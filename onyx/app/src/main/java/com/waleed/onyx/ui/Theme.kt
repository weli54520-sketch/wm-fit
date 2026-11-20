package com.waleed.onyx.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.waleed.onyx.data.Settings
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/** الألوان المتفق عليها */
object Onyx {
    val Gold = Color(0xFFE3C285)
    val Silver = Color(0xFF9DB0FF)
    val Mint = Color(0xFF9FE3CB)
    val White = Color(0xFFEDEDED)
    val Text = Color(0xFFF2F2F5)
    val Muted = Color(0x9EFFFFFF)
    val Red = Color(0xFFFF453A)
}

/** شكل الأيقونة: مربع بزوايا ناعمة متصلة (Squircle) */
object Squircle : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val n = 5.0
        val a = size.width / 2f
        val b = size.height / 2f
        val steps = 96
        for (i in 0..steps) {
            val t = 2 * Math.PI * i / steps
            val c = cos(t)
            val s = sin(t)
            val x = a + a * sign(c) * abs(c).pow(2 / n)
            val y = b + b * sign(s) * abs(s).pow(2 / n)
            if (i == 0) path.moveTo(x.toFloat(), y.toFloat()) else path.lineTo(x.toFloat(), y.toFloat())
        }
        path.close()
        return Outline.Generic(path)
    }
}

/**
 * الزجاج المدخّن: داكن وشفاف بدون لون وبدون تضبيب.
 * خلفية الجوال تبان من وراه أغمق شوي، وحافة رفيعة فاتحة. خفيف جداً على البطارية.
 */
fun Modifier.smokeGlass(shape: Shape, strong: Boolean = false, elevation: Dp = 0.dp): Modifier {
    val top = if (strong) 0x6B1C1C20 else 0x4D222226
    val bottom = if (strong) 0xC0060608 else 0x94080809
    return this
        .then(if (elevation > 0.dp) Modifier.shadow(elevation, shape, clip = false, ambientColor = Color.Black, spotColor = Color.Black) else Modifier)
        .clip(shape)
        .background(Brush.verticalGradient(listOf(Color(top.toInt()), Color(bottom.toInt()))))
        .border(
            1.dp,
            Brush.linearGradient(listOf(Color(0x5CFFFFFF), Color(0x12FFFFFF), Color(0x26FFFFFF))),
            shape,
        )
}

/** زجاج الأيقونة الشفاف (فاتح) */
fun Modifier.clearGlass(shape: Shape): Modifier = this
    .clip(shape)
    .background(Brush.verticalGradient(listOf(Color(0x3DFFFFFF), Color(0x14FFFFFF))))
    .border(1.dp, Brush.linearGradient(listOf(Color(0x8CFFFFFF), Color(0x14FFFFFF), Color(0x4DFFFFFF))), shape)

/** الحركة النابضة: تتغير من الإعدادات */
object Motion {
    fun <T> bouncy(settings: Settings): SpringSpec<T> {
        if (settings.reduceMotion) return spring(dampingRatio = 1f, stiffness = Spring.StiffnessHigh * 4)
        val damping = when (settings.bounce) { 0 -> 0.86f; 2 -> 0.58f; else -> 0.72f }
        val stiffness = when (settings.speed) { 0 -> 240f; 2 -> 620f; else -> 400f }
        return spring(dampingRatio = damping, stiffness = stiffness)
    }

    /** للوحات: بدون ارتداد */
    fun <T> smooth(settings: Settings): SpringSpec<T> =
        spring(dampingRatio = 1f, stiffness = if (settings.reduceMotion) 4000f else when (settings.speed) { 0 -> 260f; 2 -> 700f; else -> 420f })
}
