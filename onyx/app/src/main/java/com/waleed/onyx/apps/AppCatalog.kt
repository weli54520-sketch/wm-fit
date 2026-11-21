package com.waleed.onyx.apps

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.UserManager
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale

/** تطبيق واحد يفتح من الشاشة الرئيسية */
data class AppInfo(
    val key: String,
    val label: String,
    val pkg: String,
    val component: ComponentName,
    val user: UserHandle,
    val info: LauncherActivityInfo,
)

/**
 * كل التطبيقات اللي بالجوال، وأيقوناتها، وفتحها.
 * الأيقونة تجي من الجوال نفسه، فأيقونات Theme Park تطلع مثل ما هي.
 */
class AppCatalog(private val context: Context) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val collator = Collator.getInstance(Locale("ar"))

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded

    private val originals = LruCache<String, ImageBitmap>(400)
    private val glyphs = LruCache<String, ImageBitmap>(400)

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = refresh()
        override fun onPackageAdded(packageName: String, user: UserHandle) = refresh()
        override fun onPackageChanged(packageName: String, user: UserHandle) {
            invalidate(packageName); refresh()
        }
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = refresh()
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = refresh()
    }

    init {
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        refresh()
    }

    fun keyOf(info: LauncherActivityInfo): String =
        "${info.componentName.flattenToShortString()}#${userManager.getSerialNumberForUser(info.user)}"

    fun refresh() {
        scope.launch {
            val list = withContext(Dispatchers.IO) {
                launcherApps.profiles.flatMap { user ->
                    runCatching { launcherApps.getActivityList(null, user) }.getOrDefault(emptyList())
                }.filter { it.componentName.packageName != context.packageName }
                    .map {
                        AppInfo(keyOf(it), it.label.toString(), it.componentName.packageName, it.componentName, it.user, it)
                    }
                    .sortedWith { a, b -> collator.compare(a.label, b.label) }
            }
            _apps.value = list
            _loaded.value = true
        }
    }

    fun byKey(key: String): AppInfo? = _apps.value.firstOrNull { it.key == key }

    /** الحزم بالترتيب: أول واحد موجود يفوز */
    fun firstByPackages(packages: List<String>, exclude: Set<String>): AppInfo? {
        for (p in packages) {
            val a = _apps.value.firstOrNull { it.pkg == p && it.key !in exclude }
            if (a != null) return a
        }
        return null
    }

    private fun invalidate(pkg: String) {
        originals.snapshot().keys.filter { it.startsWith(pkg) }.forEach { originals.remove(it) }
        glyphs.snapshot().keys.filter { it.startsWith(pkg) }.forEach { glyphs.remove(it) }
    }

    fun cachedIcon(app: AppInfo, size: Int, glyph: Boolean): ImageBitmap? =
        (if (glyph) glyphs else originals).get("${app.key}@$size")

    /** الأيقونة الأصلية كاملة مثل ما يعطيها الجوال، بدون قص أو خلفية */
    suspend fun icon(app: AppInfo, size: Int): ImageBitmap = withContext(Dispatchers.Default) {
        val k = "${app.key}@$size"
        originals.get(k) ?: run {
            val d = app.info.getIcon(context.resources.displayMetrics.densityDpi)
            val bmp = draw(d, size, Rect(0, 0, size, size))
            bmp.also { originals.put(k, it) }
        }
    }

    /**
     * رمز التطبيق لحاله للزجاج الشفاف والداكن.
     * ناخذه من ملفات التطبيق نفسه (قبل ثيم سامسونج)، لأن أيقونة Theme Park مو متكيفة.
     * نستخدم الرمز الأحادي (أندرويد ١٣) لو موجود، وإلا الطبقة الأمامية لو كانت شعار مو مليانة.
     * لو ما فيه رمز صالح نرجع null، والأيقونة الأصلية تنعرض صغيرة داخل الزجاج.
     */
    suspend fun glyph(app: AppInfo, size: Int): ImageBitmap? = withContext(Dispatchers.Default) {
        val k = "${app.key}@$size"
        glyphs.get(k) ?: run {
            val adaptive = (runCatching { rawIcon(app) }.getOrNull() as? AdaptiveIconDrawable)
                ?: (app.info.getIcon(context.resources.displayMetrics.densityDpi) as? AdaptiveIconDrawable)
                ?: return@run null
            val mono = if (Build.VERSION.SDK_INT >= 33) adaptive.monochrome else null
            val layer = (mono ?: adaptive.foreground ?: return@run null).mutate()
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val inset = size / 4
            layer.setBounds(-inset, -inset, size + inset, size + inset)
            layer.colorFilter = PorterDuffColorFilter(android.graphics.Color.WHITE, PorterDuff.Mode.SRC_IN)
            layer.draw(Canvas(bmp))
            if (mono == null) {
                // الطبقة الأمامية لو مليانة (صورة مو شعار) ما تنفع كرمز
                val px = IntArray(size * size)
                bmp.getPixels(px, 0, size, 0, 0, size, size)
                val ratio = px.count { (it ushr 24) > 128 }.toFloat() / px.size
                if (ratio > 0.42f || ratio < 0.02f) return@run null
            }
            bmp.asImageBitmap().also { glyphs.put(k, it) }
        }
    }

    /** الأيقونة الخام من ملفات التطبيق (بدون ثيم) */
    @Suppress("DEPRECATION")
    private fun rawIcon(app: AppInfo): Drawable? {
        val pm = context.packageManager
        val ai = pm.getActivityInfo(app.component, 0)
        val id = if (ai.icon != 0) ai.icon else ai.applicationInfo.icon
        if (id == 0) return null
        val res = pm.getResourcesForApplication(ai.applicationInfo)
        return res.getDrawableForDensity(id, context.resources.displayMetrics.densityDpi, null)
    }

    private fun draw(d: Drawable, size: Int, bounds: Rect): ImageBitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        d.bounds = bounds
        d.draw(c)
        return bmp.asImageBitmap()
    }

    /**
     * يفتح التطبيق. [from] = الشاشة الحالية: التطبيق يكبر من مكان الأيقونة وحجمها لين يعبي الشاشة
     * (حركة النظام على كرت الشاشة، مثل الآيفون). لو سامسونج فرضت حركتها، تمشي حركتها.
     */
    fun launch(app: AppInfo, source: Rect?, from: android.view.View? = null) {
        val opts = if (source != null && from != null && source.width() > 0) {
            runCatching {
                android.app.ActivityOptions.makeScaleUpAnimation(from, source.left, source.top, source.width(), source.height()).toBundle()
            }.getOrNull()
        } else null
        runCatching { launcherApps.startMainActivity(app.component, app.user, source, opts) }
            .onFailure { runCatching { launcherApps.startMainActivity(app.component, app.user, source, null) } }
    }

    fun openInfo(app: AppInfo) {
        runCatching { launcherApps.startAppDetailsActivity(app.component, app.user, null, null) }
    }

    // ───────── اختصارات التطبيق (الضغطة الطويلة) ─────────

    /** اختصارات التطبيق (مثل "رسالة جديدة"). تشتغل بس لما Onyx هو اللانشر الأساسي */
    fun shortcuts(app: AppInfo): List<android.content.pm.ShortcutInfo> = try {
        if (!launcherApps.hasShortcutHostPermission()) emptyList() else {
            val q = LauncherApps.ShortcutQuery()
                .setPackage(app.pkg)
                .setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
                )
            (launcherApps.getShortcuts(q, app.user) ?: emptyList())
                .filter { it.isEnabled }
                .sortedWith(compareBy({ if (it.isDynamic) 1 else 0 }, { it.rank }))
                .take(4)
        }
    } catch (e: Exception) {
        emptyList()
    }

    fun shortcutIcon(s: android.content.pm.ShortcutInfo, size: Int): ImageBitmap? = try {
        launcherApps.getShortcutIconDrawable(s, context.resources.displayMetrics.densityDpi)?.let { draw(it, size, Rect(0, 0, size, size)) }
    } catch (e: Exception) {
        null
    }

    fun startShortcut(s: android.content.pm.ShortcutInfo) {
        runCatching { launcherApps.startShortcut(s, null, null) }
    }

    // ───────── الفئات (ترتيب الدرج) ─────────

    fun category(app: AppInfo): String {
        val c = runCatching { app.info.applicationInfo.category }.getOrDefault(-1)
        return when (c) {
            android.content.pm.ApplicationInfo.CATEGORY_SOCIAL -> "التواصل"
            android.content.pm.ApplicationInfo.CATEGORY_GAME -> "الألعاب"
            android.content.pm.ApplicationInfo.CATEGORY_AUDIO -> "الصوت والموسيقى"
            android.content.pm.ApplicationInfo.CATEGORY_VIDEO -> "الفيديو"
            android.content.pm.ApplicationInfo.CATEGORY_IMAGE -> "الصور"
            android.content.pm.ApplicationInfo.CATEGORY_NEWS -> "الأخبار"
            android.content.pm.ApplicationInfo.CATEGORY_MAPS -> "الخرائط والتنقل"
            android.content.pm.ApplicationInfo.CATEGORY_PRODUCTIVITY -> "الإنتاجية"
            else -> if (app.pkg.startsWith("com.samsung") || app.pkg.startsWith("com.sec") || app.pkg.startsWith("com.android") || app.pkg.startsWith("com.google.android")) "الجوال" else "أخرى"
        }
    }

    /** ترتيب الفئات بالدرج */
    val categoryOrder = listOf("التواصل", "الإنتاجية", "الصور", "الفيديو", "الصوت والموسيقى", "الخرائط والتنقل", "الأخبار", "الألعاب", "الجوال", "أخرى")
}
