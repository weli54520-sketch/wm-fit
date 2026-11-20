package com.waleed.launcher.trial.widgets

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

val Soft = Color.White.copy(alpha = 0.62f)
val Faint = Color.White.copy(alpha = 0.32f)
val TextShadow = Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 1.5f), 6f)

@Composable
fun Dp.sp(): TextUnit = with(LocalDensity.current) { this@sp.toSp() }

/** نص موحد للويدجت: بظل خفيف عشان يبان فوق أي خلفية */
@Composable
fun WText(
    text: String,
    size: TextUnit,
    modifier: Modifier = Modifier,
    family: FontFamily = WFonts.kufi,
    weight: FontWeight = FontWeight.Normal,
    color: Color = Color.White,
    align: TextAlign = TextAlign.Center,
    maxLines: Int = 1,
    letterSpacing: TextUnit = 0.sp,
    shadow: Boolean = true,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(
            color = color,
            fontSize = size,
            fontFamily = family,
            fontWeight = weight,
            textAlign = align,
            letterSpacing = letterSpacing,
            shadow = if (shadow) TextShadow else null,
        ),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        softWrap = maxLines > 1,
    )
}
