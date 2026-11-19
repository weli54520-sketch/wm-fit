package com.waleed.launcher.trial

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.concurrent.ConcurrentHashMap

/** تطبيق واحد على الجوال */
class AppEntry(val info: LauncherActivityInfo) {
    val key: String = info.componentName.flattenToString()
    val label: String = info.label?.toString() ?: info.componentName.packageName
    val packageName: String = info.componentName.packageName
}

class AppRepository(private val context: Context) {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val iconCache = LruCache<String, ImageBitmap>(300)
    private val glyphCache = LruCache<String, ImageBitmap>(300)
    private val noGlyph = ConcurrentHashMap.newKeySet<String>()

    /** تطبيقات وليد اليومية بالترتيب (تطلع أول شي) */
    private val dailyPackages = listOf(
        "com.anthropic.claude",
        "com.google.android.youtube",
        "org.telegram.messenger",
        "com.whatsapp",
        "com.samsung.android.app.notes",
        "com.google.android.apps.bard",
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill",
        "com.android.vending",
        "com.twitter.android",
        "com.pinterest",
        "com.openai.chatgpt",
    )

    /** العمود الجانبي يمين: ٤ تطبيقات (نفس ترتيب وليد) */
    private val sidePackages = listOf(
        "com.samsung.android.dialer",
        "com.whatsapp",
        "com.google.android.youtube",
        "com.google.android.apps.bard",
    )

    /** الشريط السفلي: ٦ تطبيقات (نفس ترتيب وليد) */
    private val dockPackages = listOf(
        "com.twitter.android",
        "com.zhiliaoapp.musically",
        "com.anthropic.claude",
        "com.samsung.android.app.notes",
        "com.android.vending",
        "com.pinterest",
    )

    suspend fun loadApps(): List<AppEntry> = withContext(Dispatchers.IO) {
        launcherApps.getActivityList(null, Process.myUserHandle())
            .filter { it.componentName.packageName != context.packageName }
            .map { AppEntry(it) }
    }

    class HomeLayout(val side: List<AppEntry>, val dock: List<AppEntry>, val pages: List<AppEntry>)

    /** نقسم التطبيقات: العمود الجانبي + الشريط السفلي + الصفحات */
    fun arrange(apps: List<AppEntry>): HomeLayout {
        val used = mutableSetOf<AppEntry>()
        fun pick(packages: List<String>, count: Int): List<AppEntry> {
            val result = mutableListOf<AppEntry>()
            for (pkg in packages + dailyPackages) {
                if (result.size == count) break
                // تيك توك له اسمين حسب الدولة
                val alt = if (pkg == "com.zhiliaoapp.musically") "com.ss.android.ugc.trill" else pkg
                val app = apps.firstOrNull { (it.packageName == pkg || it.packageName == alt) && it !in used }
                if (app != null) { result += app; used += app }
            }
            return result
        }
        val side = pick(sidePackages, 4)
        val dock = pick(dockPackages, 6)
        val rest = apps.filter { it !in used }
        val collator = Collator.getInstance()
        val daily = dailyPackages.mapNotNull { pkg -> rest.firstOrNull { it.packageName == pkg } }
        val others = rest.filter { it !in daily }.sortedWith { a, b -> collator.compare(a.label, b.label) }
        return HomeLayout(side, dock, daily + others)
    }

    fun cachedIcon(app: AppEntry, sizePx: Int): ImageBitmap? = iconCache.get("${app.key}@$sizePx")

    suspend fun icon(app: AppEntry, sizePx: Int): ImageBitmap {
        val cacheKey = "${app.key}@$sizePx"
        iconCache.get(cacheKey)?.let { return it }
        return withContext(Dispatchers.Default) {
            val drawable = app.info.getIcon(context.resources.displayMetrics.densityDpi)
            val bitmap = renderFullIcon(drawable, sizePx).asImageBitmap()
            iconCache.put(cacheKey, bitmap)
            bitmap
        }
    }

    /**
     * نرسم الأيقونة مثل ما يعطيها الجوال بالضبط، بدون أي خلفية أو قص.
     * كذا تطلع أيقونات Theme Park لو سامسونج تعطيها للانشر.
     */
    private fun renderFullIcon(drawable: Drawable, size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        return bitmap
    }

    fun cachedGlyph(app: AppEntry, sizePx: Int): ImageBitmap? = glyphCache.get("${app.key}@$sizePx")

    /**
     * رمز التطبيق بلون واحد (للأيقونات الزجاجية Liquid Glass).
     * نستخدم الرمز الأحادي الرسمي (Monochrome) لو التطبيق يوفره،
     * وإلا نطلع شكل الشعار من الطبقة الأمامية. لو ما ينفع نرجع null.
     */
    suspend fun glyph(app: AppEntry, sizePx: Int): ImageBitmap? {
        val cacheKey = "${app.key}@$sizePx"
        glyphCache.get(cacheKey)?.let { return it }
        if (app.key in noGlyph) return null
        return withContext(Dispatchers.Default) {
            val bitmap = runCatching { buildGlyph(app, sizePx) }.getOrNull()
            if (bitmap == null) {
                noGlyph += app.key
                null
            } else {
                bitmap.asImageBitmap().also { glyphCache.put(cacheKey, it) }
            }
        }
    }

    /** الأيقونة الخام من ملفات التطبيق نفسه (بدون ثيم سامسونج) */
    private fun rawIcon(app: AppEntry): Drawable? {
        val pm = context.packageManager
        @Suppress("DEPRECATION")
        val activityInfo = pm.getActivityInfo(app.info.componentName, 0)
        val iconId = activityInfo.iconResource
        if (iconId == 0) return null
        val resources = pm.getResourcesForApplication(activityInfo.applicationInfo)
        return resources.getDrawableForDensity(iconId, context.resources.displayMetrics.densityDpi, null)
    }

    private fun buildGlyph(app: AppEntry, size: Int): Bitmap? {
        val raw = rawIcon(app) as? AdaptiveIconDrawable ?: return null
        val mono = if (Build.VERSION.SDK_INT >= 33) raw.monochrome else null
        val layer = mono ?: raw.foreground ?: return null
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val inset = size / 4
        layer.setBounds(-inset, -inset, size + inset, size + inset)
        layer.draw(canvas)
        if (mono == null) {
            // لو الطبقة الأمامية مليانة (مو شعار) ما تنفع كرمز
            val pixels = IntArray(size * size)
            bitmap.getPixels(pixels, 0, size, 0, 0, size, size)
            val solid = pixels.count { (it ushr 24) > 128 }
            val ratio = solid.toFloat() / pixels.size
            if (ratio > 0.42f || ratio < 0.02f) return null
        }
        return bitmap
    }

    fun launch(app: AppEntry) {
        runCatching { launcherApps.startMainActivity(app.info.componentName, app.info.user, null, null) }
    }

    fun openAppInfo(app: AppEntry) {
        runCatching { launcherApps.startAppDetailsActivity(app.info.componentName, app.info.user, null, null) }
    }
}
