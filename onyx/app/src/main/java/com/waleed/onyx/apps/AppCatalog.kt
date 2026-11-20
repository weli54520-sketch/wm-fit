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
     * رمز التطبيق لحاله (أبيض) للزجاج الشفاف والداكن.
     * نستخدم الرمز الأحادي من أندرويد ١٣ لو موجود، وإلا الطبقة الأمامية.
     * لو الأيقونة مو متكيفة، نرجع null ونعرض الأصلية أصغر.
     */
    suspend fun glyph(app: AppInfo, size: Int): ImageBitmap? = withContext(Dispatchers.Default) {
        val k = "${app.key}@$size"
        glyphs.get(k) ?: run {
            val d = app.info.getIcon(context.resources.displayMetrics.densityDpi)
            val adaptive = d as? AdaptiveIconDrawable ?: return@run null
            val layer: Drawable = (if (Build.VERSION.SDK_INT >= 33) adaptive.monochrome else null)
                ?: adaptive.foreground ?: return@run null
            val m = layer.mutate()
            m.colorFilter = PorterDuffColorFilter(android.graphics.Color.WHITE, PorterDuff.Mode.SRC_IN)
            // الطبقة ١٠٨ والجزء الظاهر ٧٢: نكبرها شوي عشان الرمز يعبي
            val pad = (size * 0.25f).toInt()
            draw(m, size, Rect(-pad, -pad, size + pad, size + pad)).also { glyphs.put(k, it) }
        }
    }

    private fun draw(d: Drawable, size: Int, bounds: Rect): ImageBitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        d.bounds = bounds
        d.draw(c)
        return bmp.asImageBitmap()
    }

    fun launch(app: AppInfo, source: Rect?) {
        runCatching { launcherApps.startMainActivity(app.component, app.user, source, null) }
    }

    fun openInfo(app: AppInfo) {
        runCatching { launcherApps.startAppDetailsActivity(app.component, app.user, null, null) }
    }
}
