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
            WText(sub, (big * 0.19f).sp(), color = if (bold) cfg.accent.color else Soft)
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

// ───────────── عقارب ─────────────

private enum class Numerals { NONE, QUARTERS, FULL, ROMAN }

private data class DialTheme(
    val face: Brush?,
    val index: Color,
    val numeral: Color,
    val hands: Color,
    val second: Color,
    val numerals: Numerals,
    val minuteTrack: Boolean = true,
    val stitch: Color? = null,
    val subdials: Boolean = false,
    val label: String? = null,
    val marble: Boolean = false,
    val rim: Color? = null,
    val numeralFont: FontFamily? = null,
    val onlyQuarterBars: Boolean = false,
)

private val Gold = Color(0xFFE3C285)
private val GoldDeep = Color(0xFFC9A45C)

private fun themeFor(cfg: WConfig): DialTheme = when (cfg.variant) {
    WVariant.CLOCK_MINIMAL -> DialTheme(null, Color.White, Color.White, Color.White, cfg.accent.color, Numerals.NONE, minuteTrack = false, onlyQuarterBars = true)
    WVariant.CLOCK_ROMAN -> DialTheme(
        Brush.radialGradient(listOf(Color(0xFFF7F2E6), Color(0xFFE0D3B6))), Color(0xFF1A1A1A), Color(0xFF1A1A1A),
        Color(0xFF1E3A8A), Color(0xFF1A1A1A), Numerals.ROMAN, rim = GoldDeep, numeralFont = WFonts.bodoni,
    )
    WVariant.CLOCK_WALEED -> DialTheme(
        Brush.radialGradient(listOf(Color(0xFF1E1E21), Color(0xFF050506))), Gold, Gold, Gold, Gold, Numerals.NONE,
        label = "وليد", rim = GoldDeep,
    )
    WVariant.CLOCK_CHRONO -> DialTheme(
        Brush.radialGradient(listOf(Color(0xFF34373C), Color(0xFF16171A))), Color.White, Color.White, Color.White,
        Color(0xFFF7A867), Numerals.NONE, subdials = true, rim = Color(0xFF8A8F98),
    )
    WVariant.CLOCK_EMERALD -> DialTheme(
        Brush.radialGradient(listOf(Color(0xFF13805F), Color(0xFF052A20))), Gold, Gold, Gold, Gold, Numerals.FULL,
        rim = GoldDeep, numeralFont = WFonts.playfair,
    )
    WVariant.CLOCK_LEATHER -> DialTheme(
        Brush.radialGradient(listOf(Color(0xFF8A5634), Color(0xFF3A2112))), Color(0xFFF1E4CC), Color(0xFFF1E4CC),
        Color(0xFFF1E4CC), Color(0xFFF1E4CC), Numerals.QUARTERS, stitch = Color(0xFFF1E4CC), numeralFont = WFonts.playfair,
    )
    WVariant.CLOCK_MARBLE -> DialTheme(
        Brush.radialGradient(listOf(Color(0xFFF6F5F2), Color(0xFFD8D6D0))), GoldDeep, GoldDeep, GoldDeep, Color(0xFF1A1A1A),
        Numerals.QUARTERS, marble = true, rim = GoldDeep, numeralFont = WFonts.bodoni,
    )
    else -> DialTheme(null, Color.White, Color.White, Color.White, cfg.accent.color, Numerals.FULL)
}

@Composable
fun AnalogClock(cfg: WConfig) {
    val now = LocalNow.current
    val t = now.toLocal()
    val theme = remember(cfg) { themeFor(cfg) }
    val measurer = rememberTextMeasurer()
    val numeralFont = theme.numeralFont ?: cfg.numFont.family
    val veins = remember { List(7) { Random(it * 31 + 7).let { r -> List(4) { Offset(r.nextFloat(), r.nextFloat()) } } } }

    // عقرب الثواني: نبضة، أو انسيابي مثل الساعات الأوتوماتيك
    val secTarget = ((now / 1000) % 86_400) * 6f
    val sec = remember { Animatable(secTarget) }
    LaunchedEffect(secTarget, cfg.seconds) {
        if (cfg.seconds == Seconds.SWEEP && secTarget > sec.value) {
            sec.animateTo(secTarget, tween(1000, easing = LinearEasing))
        } else {
            sec.snapTo(secTarget)
        }
    }

    Canvas(Modifier.fillMaxSize()) {
        val r = min(size.width, size.height) / 2f * 0.92f
        val c = center
        // القرص
        theme.face?.let { drawCircle(it, r, c) } ?: drawCircle(Color.White.copy(alpha = 0.10f), r, c, style = Stroke(1.2f))
        if (theme.marble) drawMarble(veins, c, r)
        theme.rim?.let { drawCircle(it, r, c, style = Stroke(r * 0.025f)) }
        theme.stitch?.let {
            drawCircle(it.copy(alpha = 0.8f), r * 0.93f, c, style = Stroke(r * 0.012f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(r * 0.05f, r * 0.035f))))
        }
        if (theme.subdials) {
            listOf(-1, 1).forEach { side ->
                val sc = Offset(c.x + side * r * 0.42f, c.y + r * 0.05f)
                drawCircle(Color.Black.copy(alpha = 0.35f), r * 0.2f, sc)
                drawCircle(Color.White.copy(alpha = 0.55f), r * 0.2f, sc, style = Stroke(r * 0.008f))
                for (k in 0 until 12) {
                    val a = Math.toRadians(k * 30.0 - 90)
                    drawLine(
                        Color.White.copy(alpha = 0.6f),
                        Offset(sc.x + (r * 0.2f * cos(a)).toFloat(), sc.y + (r * 0.2f * sin(a)).toFloat()),
                        Offset(sc.x + (r * 0.16f * cos(a)).toFloat(), sc.y + (r * 0.16f * sin(a)).toFloat()),
                        r * 0.008f,
                    )
                }
                val a2 = Math.toRadians((if (side < 0) t.second * 6.0 else t.minute * 6.0) - 90)
                drawLine(theme.second, sc, Offset(sc.x + (r * 0.17f * cos(a2)).toFloat(), sc.y + (r * 0.17f * sin(a2)).toFloat()), r * 0.012f, StrokeCap.Round)
            }
        }
        // مسار الدقائق والمؤشرات
        for (k in 0 until 60) {
            val a = Math.toRadians(k * 6.0 - 90)
            val isHour = k % 5 == 0
            val isQuarter = k % 15 == 0
            if (!isHour && !theme.minuteTrack) continue
            if (theme.onlyQuarterBars && !isQuarter) continue
            val outer = r * 0.95f
            val len = when {
                isQuarter -> r * 0.14f
                isHour -> r * 0.09f
                else -> r * 0.035f
            }
            val wdt = when {
                isQuarter -> r * 0.03f
                isHour -> r * 0.02f
                else -> r * 0.007f
            }
            val numberedHour = isHour && (theme.numerals == Numerals.FULL || theme.numerals == Numerals.ROMAN || (theme.numerals == Numerals.QUARTERS && isQuarter))
            if (numberedHour && theme.numerals != Numerals.FULL && theme.numerals != Numerals.ROMAN) {
                // الأرقام مكان المؤشر
            } else {
                drawLine(
                    theme.index.copy(alpha = if (isHour) 1f else 0.55f),
                    Offset(c.x + (outer * cos(a)).toFloat(), c.y + (outer * sin(a)).toFloat()),
                    Offset(c.x + ((outer - len) * cos(a)).toFloat(), c.y + ((outer - len) * sin(a)).toFloat()),
                    wdt, StrokeCap.Round,
                )
            }
            if (numberedHour) {
                val hour = (k / 5).let { if (it == 0) 12 else it }
                val label = if (theme.numerals == Numerals.ROMAN) roman(hour) else hour.digits(cfg.arabicDigits)
                val fs = if (theme.numerals == Numerals.FULL) r * 0.15f else r * 0.2f
                val tr = if (theme.numerals == Numerals.QUARTERS) r * 0.78f else r * 0.70f
                drawLabel(measurer, label, Offset(c.x + (tr * cos(a)).toFloat(), c.y + (tr * sin(a)).toFloat()), fs, theme.numeral, numeralFont)
            }
        }
        theme.label?.let { drawLabel(measurer, it, Offset(c.x, c.y + r * 0.38f), r * 0.17f, theme.numeral, WFonts.ruqaa) }

        // العقارب مع ظل خفيف
        val hourAngle = ((t.hour % 12) + t.minute / 60f) * 30f
        val minuteAngle = (t.minute + t.second / 60f) * 6f
        hand(hourAngle, r * 0.5f, r * 0.055f, theme.hands, c, r)
        hand(minuteAngle, r * 0.76f, r * 0.038f, theme.hands, c, r)
        if (cfg.seconds != Seconds.OFF) {
            rotate(sec.value % 360f, c) {
                drawLine(theme.second, Offset(c.x, c.y + r * 0.18f), Offset(c.x, c.y - r * 0.86f), r * 0.012f, StrokeCap.Round)
            }
        }
        drawCircle(theme.hands, r * 0.045f, c)
        drawCircle(if (cfg.seconds != Seconds.OFF) theme.second else theme.hands, r * 0.022f, c)
    }
}

private fun DrawScope.hand(angle: Float, length: Float, width: Float, color: Color, c: Offset, r: Float) {
    rotate(angle, c) {
        drawLine(Color.Black.copy(alpha = 0.28f), Offset(c.x + r * 0.012f, c.y + r * 0.1f + r * 0.02f), Offset(c.x + r * 0.012f, c.y - length + r * 0.02f), width, StrokeCap.Round)
        drawLine(color, Offset(c.x, c.y + r * 0.1f), Offset(c.x, c.y - length), width, StrokeCap.Round)
    }
}

private fun DrawScope.drawLabel(m: TextMeasurer, text: String, at: Offset, size: Float, color: Color, family: FontFamily) {
    val layout = m.measure(text, TextStyle(color = color, fontSize = size.toSp(), fontFamily = family, fontWeight = FontWeight.Medium))
    drawText(layout, topLeft = Offset(at.x - layout.size.width / 2f, at.y - layout.size.height / 2f))
}

private fun DrawScope.drawMarble(veins: List<List<Offset>>, c: Offset, r: Float) {
    veins.forEachIndexed { i, pts ->
        val p = Path()
        val map = { o: Offset -> Offset(c.x - r + o.x * 2 * r, c.y - r + o.y * 2 * r) }
        val a = map(pts[0])
        p.moveTo(a.x, a.y)
        val b = map(pts[1]); val d = map(pts[2]); val e = map(pts[3])
        p.cubicTo(b.x, b.y, d.x, d.y, e.x, e.y)
        drawPath(p, Color(0xFF8C8A84).copy(alpha = if (i % 2 == 0) 0.22f else 0.12f), style = Stroke(r * (0.004f + i * 0.0015f)))
    }
    drawCircle(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.08f)), c, r), r, c)
}

private fun roman(n: Int): String = listOf("XII", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI")[n % 12]

@Suppress("unused")
private val unusedSize = Size.Zero
