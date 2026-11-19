package com.waleed.launcher.trial

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Process
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator

/** تطبيق واحد على الجوال */
class AppEntry(val info: LauncherActivityInfo) {
    val key: String = info.componentName.flattenToString()
    val label: String = info.label?.toString() ?: info.componentName.packageName
    val packageName: String = info.componentName.packageName
}

class AppRepository(private val context: Context) {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val iconCache = LruCache<String, ImageBitmap>(300)

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

    /** الشريط السفلي: ٤ تطبيقات */
    private val dockPackages = listOf(
        "com.whatsapp",
        "org.telegram.messenger",
        "com.google.android.youtube",
        "com.anthropic.claude",
    )

    suspend fun loadApps(): List<AppEntry> = withContext(Dispatchers.IO) {
        launcherApps.getActivityList(null, Process.myUserHandle())
            .filter { it.componentName.packageName != context.packageName }
            .map { AppEntry(it) }
    }

    /** نقسم التطبيقات: الشريط السفلي + الصفحات */
    fun arrange(apps: List<AppEntry>): Pair<List<AppEntry>, List<AppEntry>> {
        val byPackage = apps.groupBy { it.packageName }
        val dock = mutableListOf<AppEntry>()
        for (pkg in dockPackages + dailyPackages) {
            if (dock.size == 4) break
            byPackage[pkg]?.firstOrNull()?.let { if (it !in dock) dock += it }
        }
        val rest = apps.filter { it !in dock }
        val collator = Collator.getInstance()
        val daily = dailyPackages.mapNotNull { pkg -> rest.firstOrNull { it.packageName == pkg } }
        val others = rest.filter { it !in daily }.sortedWith { a, b -> collator.compare(a.label, b.label) }
        return dock to (daily + others)
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
     * الأيقونة كاملة تعبي المربع مثل الآيفون.
     * نرسم طبقات الأيقونة الأصلية بنفسنا، والقص بشكل Squircle يصير بالواجهة.
     */
    private fun renderFullIcon(drawable: Drawable, size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (drawable is AdaptiveIconDrawable) {
            // طبقات أندرويد أكبر من الجزء الظاهر بـ ٥٠٪، فنوسعها ربع من كل جهة
            val inset = size / 4
            drawable.background?.let {
                it.setBounds(-inset, -inset, size + inset, size + inset)
                it.draw(canvas)
            }
            drawable.foreground?.let {
                it.setBounds(-inset, -inset, size + inset, size + inset)
                it.draw(canvas)
            }
        } else {
            // أيقونة قديمة: خلفية بيضاء والشعار بالنص
            canvas.drawColor(android.graphics.Color.WHITE)
            val pad = (size * 0.12f).toInt()
            drawable.setBounds(pad, pad, size - pad, size - pad)
            drawable.draw(canvas)
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
