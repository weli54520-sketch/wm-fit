package com.waleed.launcher.trial.glass

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** معلومات الخلفية على الشاشة: عشان كل قطعة زجاج تعرف وش اللي وراها بالضبط */
class GlassBackdrop(
    val blurred: Bitmap,
    val blurFactor: Float,
    val screenW: Float,
    val screenH: Float,
    fullW: Int,
    fullH: Int,
) {
    // نفس حساب ContentScale.Crop
    val scale: Float = maxOf(screenW / fullW, screenH / fullH)
    val offX: Float = (screenW - fullW * scale) / 2f
    val offY: Float = (screenH - fullH * scale) / 2f

    val bitmapShader: BitmapShader = BitmapShader(blurred, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
        setLocalMatrix(Matrix().apply {
            setScale(scale * blurFactor, scale * blurFactor)
            postTranslate(offX, offY)
        })
    }
}

val LocalGlassBackdrop = staticCompositionLocalOf<GlassBackdrop?> { null }

/** كود الزجاج السائل: يضبب اللي وراه، ويثنيه عند الحواف مثل العدسة، ويفصل الألوان شوي */
private const val GLASS_AGSL = """
uniform shader bg;
uniform float2 size;
uniform float2 origin;
uniform float radius;
uniform float strength;
uniform float sat;

half4 sample(float2 p) { return bg.eval(p); }

half4 main(float2 p) {
    float2 c = size * 0.5;
    float2 q = abs(p - c) - (c - radius);
    float d = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
    float edge = clamp(1.0 + d / (min(c.x, c.y) * 0.85), 0.0, 1.0);
    float2 dir = normalize(p - c + float2(0.0001, 0.0001));
    float bend = pow(edge, 2.6) * strength;
    float2 base = origin + p - dir * bend;
    half3 col;
    col.r = sample(base - dir * bend * 0.10).r;
    col.g = sample(base).g;
    col.b = sample(base + dir * bend * 0.10).b;
    float l = dot(col, half3(0.299, 0.587, 0.114));
    col = mix(half3(l), col, sat);
    return half4(col, 1.0);
}
"""

/** نوع الزجاج */
enum class GlassTone { CLEAR, TINT, DARK }

/**
 * سطح زجاج سائل حقيقي.
 * لو فيه خلفية مختارة من اللانشر: تضبيب + انثناء + لمعة.
 * لو ما فيه: زجاج شفاف بسيط.
 */
@Composable
fun LiquidGlass(
    modifier: Modifier,
    corner: Dp,
    tone: GlassTone = GlassTone.CLEAR,
    tint: Color = Color.White,
    rim: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val backdrop = LocalGlassBackdrop.current
    val shape = RoundedCornerShape(corner)
    var origin by remember { mutableStateOf(Offset.Zero) }
    val runtime = remember { if (Build.VERSION.SDK_INT >= 33) RuntimeShader(GLASS_AGSL) else null }

    val glassLayer = if (backdrop != null) {
        Modifier
            .onGloballyPositioned { origin = it.positionInRoot() }
            .drawBehind {
                val rs = runtime
                if (rs != null && Build.VERSION.SDK_INT >= 33) {
                    rs.setInputShader("bg", backdrop.bitmapShader)
                    rs.setFloatUniform("size", size.width, size.height)
                    rs.setFloatUniform("origin", origin.x, origin.y)
                    rs.setFloatUniform("radius", corner.toPx())
                    rs.setFloatUniform("strength", size.minDimension * 0.16f)
                    rs.setFloatUniform("sat", 1.6f)
                    drawRect(ShaderBrush(rs))
                } else {
                    // أجهزة أقدم: تضبيب بدون انثناء
                    val m = Matrix()
                    backdrop.bitmapShader.getLocalMatrix(m)
                    val shifted = BitmapShader(backdrop.blurred, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
                        setLocalMatrix(Matrix(m).apply { postTranslate(-origin.x, -origin.y) })
                    }
                    drawRect(ShaderBrush(shifted))
                }
            }
    } else {
        Modifier.drawBehind { drawRect(Color.Black.copy(alpha = 0.14f)) }
    }

    val toneColor = when (tone) {
        GlassTone.CLEAR -> Color.White.copy(alpha = if (backdrop != null) 0.06f else 0.16f)
        GlassTone.TINT -> tint.copy(alpha = 0.20f)
        GlassTone.DARK -> Color.Black.copy(alpha = if (backdrop != null) 0.42f else 0.62f)
    }

    Box(
        modifier
            .clip(shape)
            .then(glassLayer)
            .drawWithContent {
                drawRect(toneColor)
                // لمعة فوق: الضوء جاي من فوق يسار
                drawRect(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.10f), 0.35f to Color.Transparent))
                drawContent()
                if (rim) {
                    val r = corner.toPx()
                    // حافة مضيئة رفيعة، أقوى فوق يسار وتحت يمين مثل الزجاج الحقيقي
                    drawRoundRect(
                        Brush.linearGradient(
                            0f to Color.White.copy(alpha = 0.75f),
                            0.35f to Color.White.copy(alpha = 0.10f),
                            0.65f to Color.White.copy(alpha = 0.06f),
                            1f to Color.White.copy(alpha = 0.40f),
                            start = Offset.Zero, end = Offset(size.width, size.height),
                        ),
                        cornerRadius = CornerRadius(r), style = Stroke(1.2.dp.toPx()),
                    )
                    // ظل داخلي خفيف تحت يعطي سماكة
                    drawRoundRect(
                        Brush.verticalGradient(0.7f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.18f)),
                        cornerRadius = CornerRadius(r),
                    )
                }
            },
        content = content,
    )
}

/** الخلفية نفسها مرسومة من اللانشر */
@Composable
fun WallpaperLayer(store: WallpaperStore) {
    val img = store.image ?: return
    Image(img, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
}
