package com.waleed.launcher.trial.glass

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/** خلفية وليد اللي يختارها من داخل اللانشر (عشان نقدر نسوي زجاج حقيقي فوقها) */
class WallpaperStore(private val context: Context) {
    private val file = File(context.filesDir, "wallpaper.jpg")

    /** الصورة كاملة للخلفية */
    var image by mutableStateOf<ImageBitmap?>(null)
        private set

    /** نسخة صغيرة مضببة للزجاج (خفيفة جداً على البطارية) */
    var blurred by mutableStateOf<Bitmap?>(null)
        private set

    /** كم مرة صغرنا الصورة المضببة */
    var blurFactor = 1f
        private set

    var fullWidth = 1
        private set
    var fullHeight = 1
        private set

    suspend fun load() = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext
        runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()?.let { apply(it) }
    }

    suspend fun pick(uri: Uri) = withContext(Dispatchers.IO) {
        val bmp = runCatching {
            val src = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val longSide = max(info.size.width, info.size.height)
                if (longSide > 2600) {
                    val s = 2600f / longSide
                    decoder.setTargetSize((info.size.width * s).roundToInt(), (info.size.height * s).roundToInt())
                }
            }
        }.getOrNull() ?: return@withContext
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 94, it) }
        apply(bmp)
    }

    fun clear() {
        file.delete()
        image = null
        blurred = null
    }

    private fun apply(bmp: Bitmap) {
        fullWidth = bmp.width
        fullHeight = bmp.height
        val k = 14f
        val small = Bitmap.createScaledBitmap(bmp, (bmp.width / k).roundToInt().coerceAtLeast(8), (bmp.height / k).roundToInt().coerceAtLeast(8), true)
        blurFactor = bmp.width.toFloat() / small.width
        val soft = boxBlur(boxBlur(boxBlur(small, 3), 3), 3)
        blurred = soft
        image = bmp.asImageBitmap()
    }

    /** تضبيب بسيط سريع على صورة صغيرة */
    private fun boxBlur(src: Bitmap, r: Int): Bitmap {
        val w = src.width
        val h = src.height
        val px = IntArray(w * h)
        src.getPixels(px, 0, w, 0, 0, w, h)
        val tmp = IntArray(w * h)
        fun pass(input: IntArray, output: IntArray, horizontal: Boolean) {
            val len = if (horizontal) w else h
            val lines = if (horizontal) h else w
            for (line in 0 until lines) {
                for (i in 0 until len) {
                    var a = 0; var rr = 0; var g = 0; var b = 0; var n = 0
                    for (k in -r..r) {
                        val j = (i + k).coerceIn(0, len - 1)
                        val c = if (horizontal) input[line * w + j] else input[j * w + line]
                        a += c ushr 24 and 0xFF; rr += c shr 16 and 0xFF; g += c shr 8 and 0xFF; b += c and 0xFF; n++
                    }
                    val idx = if (horizontal) line * w + i else i * w + line
                    output[idx] = (a / n shl 24) or (rr / n shl 16) or (g / n shl 8) or (b / n)
                }
            }
        }
        pass(px, tmp, true)
        pass(tmp, px, false)
        return Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
    }
}
