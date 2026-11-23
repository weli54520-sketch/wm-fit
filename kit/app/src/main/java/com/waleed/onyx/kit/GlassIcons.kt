package com.waleed.onyx.kit

import android.content.ContentValues
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Shader
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.provider.MediaStore
import android.util.LruCache
import java.text.Collator
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/** تطبيق واحد بالجوال */
data class KitApp(val key: String, val label: String, val info: LauncherActivityInfo)

/**
 * يرسم أيقونات التطبيقات بزجاج Onyx (شفاف أو داكن) ويحفظها صور بالمعرض،
 * عشان وليد يركبها من Good Lock ← Theme Park.
 * نفس طريقة الرسم اللي باللانشر: الرمز الأبيض من ملفات التطبيق نفسه.
 */
class GlassIcons(private val context: Context) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val density = context.resources.displayMetrics.densityDpi
    private val cache = LruCache<String, Bitmap>(300)

    fun apps(): List<KitApp> {
        val collator = Collator.getInstance(Locale("ar"))
        return launcherApps.getActivityList(null, Process.myUserHandle())
            .filter { it.componentName.packageName != context.packageName }
            .map { KitApp(it.componentName.flattenToShortString(), it.label.toString(), it) }
            .sortedWith { a, b -> collator.compare(a.label, b.label) }
    }

    /** هل للتطبيق رمز أبيض يصلح للزجاج؟ */
    fun hasGlyph(app: KitApp): Boolean = glyph(app, 96) != null

    /**
     * أيقونة كاملة بالزجاج.
     * [white] = الرمز أبيض (لو موجود)، وإلا الأيقونة الملونة صغيرة داخل الزجاج.
     */
    fun render(app: KitApp, size: Int, dark: Boolean, white: Boolean): Bitmap {
        val k = "${app.key}|$size|$dark|$white"
        cache.get(k)?.let { return it }
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val s = size.toFloat()
        val stroke = s * 0.014f
        val path = squircle(s, stroke)

        // ١) جسم الزجاج
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        if (!dark) {
            // أساس غامق خفيف عشان يبان على الخلفيات الفاتحة
            paint.color = argb(0x1F000000)
            c.drawPath(path, paint)
            paint.shader = LinearGradient(0f, 0f, 0f, s, intArrayOf(argb(0x5CFFFFFF), argb(0x24FFFFFF), argb(0x33FFFFFF)), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
            c.drawPath(path, paint)
            // لمعة ناعمة من فوق
            paint.shader = LinearGradient(0f, 0f, 0f, s, intArrayOf(argb(0x2EFFFFFF), 0), floatArrayOf(0f, 0.42f), Shader.TileMode.CLAMP)
            c.drawPath(path, paint)
        } else {
            // مدخّن بدون لمعة
            paint.shader = LinearGradient(0f, 0f, 0f, s, argb(0xB8242429), argb(0xDE0D0D0F), Shader.TileMode.CLAMP)
            c.drawPath(path, paint)
        }

        // ٢) الرمز
        val g = if (white) glyph(app, size) else null
        if (g != null) {
            val shadow = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = PorterDuffColorFilter(android.graphics.Color.BLACK, PorterDuff.Mode.SRC_IN)
                alpha = (0.30f * 255).toInt()
            }
            c.drawBitmap(g, 0f, s * 0.012f, shadow)
            val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { alpha = if (dark) 230 else 247 }
            c.drawBitmap(g, 0f, 0f, p)
        } else {
            val d = runCatching { rawIcon(app) }.getOrNull() ?: app.info.getIcon(density)
            val inner = (s * 0.64f).toInt()
            val off = ((s - inner) / 2).toInt()
            d.mutate()
            d.setBounds(off, off, off + inner, off + inner)
            d.alpha = 242
            d.draw(c)
        }

        // ٣) الحافة المضيئة
        val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            shader = if (dark) LinearGradient(0f, 0f, s, s, intArrayOf(argb(0x6BFFFFFF), argb(0x14FFFFFF), argb(0x38FFFFFF)), null, Shader.TileMode.CLAMP)
            else LinearGradient(0f, 0f, s, s, intArrayOf(argb(0xD9FFFFFF), argb(0x1FFFFFFF), argb(0x80FFFFFF)), null, Shader.TileMode.CLAMP)
        }
        c.drawPath(path, edge)

        cache.put(k, bmp)
        return bmp
    }

    /** يحفظ الصورة بالمعرض: Pictures/WM Onyx/Clear أو Dark. يستبدل القديمة بنفس الاسم. */
    fun save(app: KitApp, dark: Boolean, white: Boolean): Boolean = runCatching {
        val bmp = render(app, 512, dark, white)
        val folder = "Pictures/WM Onyx/" + (if (dark) "Dark" else "Clear") + "/"
        val name = app.label.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifEmpty { "app" } + ".png"
        val resolver = context.contentResolver
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        // نشيل نسختنا القديمة بنفس الاسم عشان ما تتكرر
        runCatching {
            resolver.delete(
                collection,
                "${MediaStore.Images.Media.RELATIVE_PATH}=? AND ${MediaStore.Images.Media.DISPLAY_NAME}=?",
                arrayOf(folder, name),
            )
        }
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, folder)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values) ?: return false
        resolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) } ?: return false
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        true
    }.getOrDefault(false)

    /**
     * رمز التطبيق لحاله أبيض (نفس طريقة Onyx).
     * من ملفات التطبيق نفسه (قبل ثيم سامسونج). الرمز الأحادي (أندرويد ١٣) لو موجود،
     * وإلا الطبقة الأمامية لو كانت شعار مو صورة مليانة. لو ما يصلح: null.
     */
    private fun glyph(app: KitApp, size: Int): Bitmap? {
        val k = "g|${app.key}|$size"
        cache.get(k)?.let { return it }
        val adaptive = (runCatching { rawIcon(app) }.getOrNull() as? AdaptiveIconDrawable)
            ?: (app.info.getIcon(density) as? AdaptiveIconDrawable)
            ?: return null
        val mono = if (Build.VERSION.SDK_INT >= 33) adaptive.monochrome else null
        val layer = (mono ?: adaptive.foreground ?: return null).mutate()
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val inset = size / 4
        layer.setBounds(-inset, -inset, size + inset, size + inset)
        layer.colorFilter = PorterDuffColorFilter(android.graphics.Color.WHITE, PorterDuff.Mode.SRC_IN)
        layer.draw(Canvas(bmp))
        if (mono == null) {
            val px = IntArray(size * size)
            bmp.getPixels(px, 0, size, 0, 0, size, size)
            val ratio = px.count { (it ushr 24) > 128 }.toFloat() / px.size
            if (ratio > 0.42f || ratio < 0.02f) return null
        }
        cache.put(k, bmp)
        return bmp
    }

    /** الأيقونة الخام من ملفات التطبيق (بدون ثيم) */
    @Suppress("DEPRECATION")
    private fun rawIcon(app: KitApp): Drawable? {
        val pm = context.packageManager
        val ai = pm.getActivityInfo(app.info.componentName, 0)
        val id = if (ai.icon != 0) ai.icon else ai.applicationInfo.icon
        if (id == 0) return null
        val res = pm.getResourcesForApplication(ai.applicationInfo)
        return res.getDrawableForDensity(id, density, null)
    }

    private fun argb(v: Long): Int = v.toInt()

    /** مربع بزوايا ناعمة متصلة (Squircle) مثل Onyx، داخل الحافة بشوي */
    private fun squircle(size: Float, inset: Float): Path {
        val path = Path()
        val n = 5.0
        val r = size / 2f - inset / 2f
        val cx = size / 2f
        val steps = 160
        for (i in 0..steps) {
            val t = 2 * Math.PI * i / steps
            val co = cos(t)
            val si = sin(t)
            val x = cx + r * sign(co) * abs(co).pow(2 / n)
            val y = cx + r * sign(si) * abs(si).pow(2 / n)
            if (i == 0) path.moveTo(x.toFloat(), y.toFloat()) else path.lineTo(x.toFloat(), y.toFloat())
        }
        path.close()
        return path
    }
}
