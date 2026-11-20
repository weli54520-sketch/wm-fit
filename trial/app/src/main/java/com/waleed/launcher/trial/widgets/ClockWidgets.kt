package com.waleed.launcher.trial.widgets

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

@Composable
private fun hourText(hour24: Int): Int {
    val is24 = DateFormat.is24HourFormat(LocalContext.current)
    return if (is24) hour24 else (hour24 % 12).let { if (it == 0) 12 else it }
}

// ───────────── رقمية ─────────────

@Composable
fun DigitalClock(cfg: WConfig, bold: Boolean) {
    val now = LocalNow.current.toLocal()
    val h = hourText(now.hour)
    val time = String.format(Locale.US, "%d:%02d", h, now.minute).digits(cfg.arabicDigits)
    val sub = "${ArabicCalendar.dayName(now.dayOfWeek)}  ${now.dayOfMonth.digits(cfg.arabicDigits)} ${ArabicCalendar.gregMonths[now.monthValue - 1]}"
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val big = minOf(maxWidth / (if (bold) 2.9f else 3.1f), maxHeight * 0.48f)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            WText(
                time, big.sp(),
                family = cfg.numFont.family,
                weight = if (bold) FontWeight.Bold else FontWeight.Light,
                letterSpacing = if (bold) (-1).sp else 0.sp,
            )
            if (cfg.showDate) WText(sub, (big * 0.19f).sp(), color = if (bold) cfg.accent.color else Soft)
        }
    }
}

// ───────────── قلابة ─────────────

@Composable
fun FlipClock(cfg: WConfig) {
    val now = LocalNow.current.toLocal()
    val hh = String.format(Locale.US, "%02d", hourText(now.hour)).digits(cfg.arabicDigits)
    val mm = String.format(Locale.US, "%02d", now.minute).digits(cfg.arabicDigits)
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val cardW = minOf(maxWidth * 0.42f, maxHeight * 0.62f)
        val cardH = cardW * 1.12f
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(cardW * 0.08f)) {
                listOf(hh, mm).forEach { part ->
                    Box(
                        Modifier
                            .size(cardW, cardH)
                            .clip(RoundedCornerShape(cardW * 0.16f))
                            .background(Brush.verticalGradient(listOf(Color(0xFF2A2A2E), Color(0xFF141416)))),
                        contentAlignment = Alignment.Center,
                    ) {
                        AnimatedContent(
                            targetState = part,
                            transitionSpec = { (slideInVertically { -it / 3 } + fadeIn()) togetherWith (slideOutVertically { it / 3 } + fadeOut()) },
                            label = "flip",
                        ) { value ->
                            WText(value, (cardH * 0.62f).sp(), family = cfg.numFont.family, weight = FontWeight.Bold, shadow = false)
                        }
                        // خط القلب بالنص
                        Box(Modifier.fillMaxWidth().height(1.5.dp).background(Color.Black.copy(alpha = 0.55f)))
                    }
                }
            }
            Spacer(Modifier.height(cardH * 0.12f))
            WText(ArabicCalendar.dayName(now.dayOfWeek), (cardH * 0.16f).sp(), color = cfg.accent.color)
        }
    }
}

// ───────────── بالكلام ─────────────

@Composable
fun WordsClock(cfg: WConfig) {
    val now = LocalNow.current.toLocal()
    val (h, m) = ArabicCalendar.timeInWords(now.hour, now.minute)
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val unit = minOf(maxWidth / 6.5f, maxHeight / 3.6f)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            WText(h, unit.sp(), family = WFonts.ruqaa, weight = FontWeight.Bold, color = cfg.accent.color)
            WText(m, (unit * 0.62f).sp(), family = WFonts.amiri, maxLines = 2)
        }
    }
}

// ───────────── مقصوصة (نص قرص يدور) ─────────────

@Composable
fun HalfDialClock(cfg: WConfig) {
    val now = LocalNow.current.toLocal()
    val measurer = rememberTextMeasurer()
    val accent = cfg.accent.color
    val family = cfg.numFont.family
    val time = String.format(Locale.US, "%d:%02d", hourText(now.hour), now.minute).digits(cfg.arabicDigits)
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val r = w * 0.92f
            val c = Offset(w / 2, size.height * 0.30f + r)
            val frac = (now.hour % 12) + now.minute / 60f
            for (j in 0 until 144) {
                val pos = j / 12f
                var diff = pos - frac
                if (diff > 6) diff -= 12
                if (diff < -6) diff += 12
                val a = Math.toRadians(-90.0 + diff * 30.0)
                if (a < Math.toRadians(-180.0) || a > 0) continue
                val len = when {
                    j % 12 == 0 -> r * 0.075f
                    j % 6 == 0 -> r * 0.045f
                    else -> r * 0.025f
                }
                val p1 = Offset(c.x + (r * cos(a)).toFloat(), c.y + (r * sin(a)).toFloat())
                val p2 = Offset(c.x + ((r - len) * cos(a)).toFloat(), c.y + ((r - len) * sin(a)).toFloat())
                drawLine(Color.White.copy(alpha = if (j % 12 == 0) 0.95f else 0.45f), p1, p2, if (j % 12 == 0) 2.5f else 1.4f, StrokeCap.Round)
                if (j % 12 == 0) {
                    val hour = (j / 12).let { if (it == 0) 12 else it }
                    val label = hour.digits(cfg.arabicDigits)
                    val tl = measurer.measure(label, TextStyle(color = Color.White, fontSize = (r * 0.075f).toSp(), fontFamily = family))
                    val rr = r - r * 0.16f
                    val pc = Offset(c.x + (rr * cos(a)).toFloat(), c.y + (rr * sin(a)).toFloat())
                    drawText(tl, topLeft = Offset(pc.x - tl.size.width / 2f, pc.y - tl.size.height / 2f))
                }
            }
            // المؤشر الثابت
            val top = c.y - r
            drawLine(accent, Offset(w / 2, top - r * 0.02f), Offset(w / 2, top + r * 0.11f), 4f, StrokeCap.Round)
            val tri = Path().apply {
                moveTo(w / 2 - r * 0.025f, top - r * 0.05f)
                lineTo(w / 2 + r * 0.025f, top - r * 0.05f)
                lineTo(w / 2, top - r * 0.01f)
                close()
            }
            drawPath(tri, accent)
        }
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            WText(
                time, (maxHeight * 0.2f).sp(), family = family, weight = FontWeight.Light,
                modifier = Modifier.width(maxWidth).padding(bottom = maxHeight * 0.07f),
            )
        }
    }
}

