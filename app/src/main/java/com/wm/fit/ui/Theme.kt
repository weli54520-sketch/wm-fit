package com.wm.fit.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.wm.fit.core.P

/** ألوان التطبيق. الخلفية داكنة دايماً، واللون المميز يختاره المستخدم. */
object C {
    val bg = Color(0xFF0B0C0B)
    val card = Color(0xFF151716)
    val card2 = Color(0xFF1E211F)
    val line = Color(0x12FFFFFF)
    val text = Color(0xFFF2F4F0)
    val muted = Color(0xFF9BA39A)
    val soft = Color(0xFFDCE3D6)
    val track = Color(0xFF262A27)

    val warn = Color(0xFFFF8A3D)
    val warnText = Color(0xFFFFB27A)
    val warnBg = Color(0x1FFF8A3D)
    val bad = Color(0xFFFF6B6B)

    val protein = Color(0xFF5EE6C4)
    val carbs = Color(0xFF6FB6FF)
    val fat = Color(0xFFFFC857)

    /** الألوان المميزة اللي يختار منها المستخدم. */
    val accents = listOf(
        Color(0xFFC8FF3D) to "أخضر",
        Color(0xFFFF7A1A) to "برتقالي",
        Color(0xFF3DA5FF) to "أزرق",
        Color(0xFF5EE6C4) to "فيروزي",
    )

    val accent: Color get() = accents[P.accent.coerceIn(0, accents.lastIndex)].first
    /** نفس اللون المميز بشفافية خفيفة، للخلفيات. */
    val accentBg: Color get() = accent.copy(alpha = 0.12f)
    /** لون النص فوق اللون المميز. */
    val onAccent = Color(0xFF0B0C0B)
}

private val typo = Typography(
    displayLarge = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 19.sp),
    labelSmall = TextStyle(fontSize = 11.sp),
)

@Composable
fun WMFitTheme(content: @Composable () -> Unit) {
    // التطبيق داكن دايماً، ما يتبع وضع الجوال.
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = C.accent,
            onPrimary = C.onAccent,
            background = C.bg,
            surface = C.card,
            onBackground = C.text,
            onSurface = C.text,
            error = C.bad,
        ),
        typography = typo,
        content = content,
    )
}
