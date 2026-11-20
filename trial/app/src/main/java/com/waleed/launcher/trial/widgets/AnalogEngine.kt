package com.waleed.launcher.trial.widgets

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** ألوان المينا: الأساس، والنصوص فوقها */
private fun dialIsLight(f: DialFace) = f in setOf(DialFace.WHITE, DialFace.SALMON, DialFace.CHAMPAGNE, DialFace.PEARL, DialFace.MARBLE)

/** محرك الساعة الكلاسيكية: كل جزء يتغير لحاله */
@Composable
fun AnalogClock(cfg: WConfig) {
    val now = LocalNow.current
    val t = now.toLocal()
    val measurer = rememberTextMeasurer()
    val veins = remember { List(8) { i -> Random(i * 37 + 11).let { r -> List(4) { Offset(r.nextFloat(), r.nextFloat()) } } } }

    val secTarget = ((now / 1000) % 86_400) * 6f
    val sec = remember { Animatable(secTarget) }
    LaunchedEffect(secTarget, cfg.seconds) {
        if (cfg.seconds == Seconds.SWEEP && secTarget > sec.value && secTarget - sec.value < 30f) {
            sec.animateTo(secTarget, tween(1000, easing = LinearEasing))
        } else {
            sec.snapTo(secTarget)
        }
    }

    val light = dialIsLight(cfg.face)
    val ink = when {
        cfg.face == DialFace.GLASS -> Color.White
        light -> Color(0xFF1C1C1E)
        else -> Color(0xFFF2F2F2)
    }

    Canvas(Modifier.fillMaxSize()) {
        val r = min(size.width, size.height) / 2f * 0.94f
        val c = center
        val dialR = if (cfg.bezel == Bezel.NONE || cfg.bezel == Bezel.RAILWAY) r else r * 0.9f

        drawBezel(cfg, c, r)
        drawFace(cfg.face, c, dialR, veins)

        // مسار الدقائق
        if (cfg.bezel == Bezel.RAILWAY) {
            drawCircle(ink.copy(alpha = 0.6f), dialR * 0.94f, c, style = Stroke(dialR * 0.006f))
            drawCircle(ink.copy(alpha = 0.6f), dialR * 0.86f, c, style = Stroke(dialR * 0.006f))
            for (k in 0 until 60) {
                val a = ang(k * 6f)
                drawLine(ink.copy(alpha = 0.7f), polar(c, dialR * 0.94f, a), polar(c, dialR * 0.86f, a), dialR * 0.008f)
            }
        }

        drawIndices(cfg, c, dialR, ink, measurer)
        drawComplication(cfg, c, dialR, ink, measurer, t, sec.value)
        if (cfg.dialText.isNotBlank()) {
            label(measurer, cfg.dialText, Offset(c.x, c.y - dialR * 0.36f), dialR * 0.15f, cfg.metal.dark.takeIf { light } ?: cfg.metal.light, WFonts.ruqaa, FontWeight.Bold)
        }

        // العقارب
        val hourA = ((t.hour % 12) + t.minute / 60f) * 30f
        val minA = (t.minute + t.second / 60f) * 6f
        drawHand(cfg.hands, cfg.metal, c, hourA, dialR * 0.5f, dialR * 0.085f, dialR)
        drawHand(cfg.hands, cfg.metal, c, minA, dialR * 0.8f, dialR * 0.062f, dialR)
        if (cfg.seconds != Seconds.OFF && cfg.comp != Comp.SUBSEC) {
            rotate(sec.value % 360f, c) {
                drawLine(Color.Black.copy(alpha = 0.25f), Offset(c.x + dialR * 0.012f, c.y + dialR * 0.22f), Offset(c.x + dialR * 0.012f, c.y - dialR * 0.84f), dialR * 0.014f, StrokeCap.Round)
                drawLine(cfg.accent.color, Offset(c.x, c.y + dialR * 0.2f), Offset(c.x, c.y - dialR * 0.86f), dialR * 0.012f, StrokeCap.Round)
                drawCircle(cfg.accent.color, dialR * 0.035f, Offset(c.x, c.y + dialR * 0.16f))
            }
        }
        drawCircle(cfg.metal.dark, dialR * 0.05f, c)
        drawCircle(cfg.metal.light, dialR * 0.03f, c)

        // انعكاس الكريستال
        drawArc(
            Brush.linearGradient(listOf(Color.White.copy(alpha = 0.12f), Color.Transparent), Offset(c.x - dialR, c.y - dialR), c),
            200f, 110f, true, Offset(c.x - dialR, c.y - dialR), Size(dialR * 2, dialR * 2),
        )
    }
}

fun ang(deg: Float): Double = Math.toRadians(deg - 90.0)
fun polar(c: Offset, rad: Float, a: Double) = Offset(c.x + (rad * cos(a)).toFloat(), c.y + (rad * sin(a)).toFloat())

private fun DrawScope.drawBezel(cfg: WConfig, c: Offset, r: Float) {
    when (cfg.bezel) {
        Bezel.THIN -> {
            drawCircle(Brush.sweepGradient(listOf(cfg.metal.light, cfg.metal.dark, cfg.metal.light, cfg.metal.dark, cfg.metal.light), c), r, c)
        }
        Bezel.FLUTED -> {
            drawCircle(cfg.metal.dark, r, c)
            for (k in 0 until 90) {
                val a = ang(k * 4f)
                drawLine(if (k % 2 == 0) cfg.metal.light else cfg.metal.dark.copy(alpha = 0.6f), polar(c, r * 0.99f, a), polar(c, r * 0.9f, a), r * 0.035f)
            }
        }
        else -> Unit
    }
}

private fun DrawScope.drawFace(f: DialFace, c: Offset, r: Float, veins: List<List<Offset>>) {
    fun sun(base: Color, hi: Color) {
        drawCircle(Brush.radialGradient(listOf(hi, base), c, r), r, c)
        // أشعة الشمس المصقولة
        val stops = List(48) { if (it % 2 == 0) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.10f) }
        drawCircle(Brush.sweepGradient(stops + stops.first(), c), r, c)
    }
    when (f) {
        DialFace.GLASS -> drawCircle(Color.White.copy(alpha = 0.08f), r, c, style = Stroke(r * 0.012f))
        DialFace.BLACK -> drawCircle(Brush.radialGradient(listOf(Color(0xFF26262A), Color(0xFF050506)), c, r), r, c)
        DialFace.WHITE -> drawCircle(Brush.radialGradient(listOf(Color(0xFFFBFAF7), Color(0xFFDCD8CF)), c, r), r, c)
        DialFace.BLUE_SUN -> sun(Color(0xFF0D2350), Color(0xFF2E5AA8))
        DialFace.GREEN_SUN -> sun(Color(0xFF12352A), Color(0xFF2F6E57))
        DialFace.BURGUNDY_SUN -> sun(Color(0xFF3A0C17), Color(0xFF8A2A40))
        DialFace.SALMON -> drawCircle(Brush.radialGradient(listOf(Color(0xFFF6C3AE), Color(0xFFDB8F73)), c, r), r, c)
        DialFace.CHAMPAGNE -> sun(Color(0xFFB99B66), Color(0xFFEAD8AF))
        DialFace.PEARL -> {
            drawCircle(Color(0xFFF4F1F6), r, c)
            drawCircle(
                Brush.sweepGradient(
                    listOf(Color(0x66F9C6D8), Color(0x66BDE7DD), Color(0x66C9C2F2), Color(0x66F3E4BD), Color(0x66F9C6D8)), c,
                ),
                r, c,
            )
            drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.6f), Color.Transparent), c, r * 0.8f), r, c)
        }
        DialFace.EMERALD -> sun(Color(0xFF042A1F), Color(0xFF13805F))
        DialFace.MARBLE -> {
            drawCircle(Brush.radialGradient(listOf(Color(0xFFF7F6F3), Color(0xFFD9D7D1)), c, r), r, c)
            val clip = Path().apply { addOval(Rect(c, r)) }
            clipPath(clip) {
                veins.forEachIndexed { i, pts ->
                    val m = { o: Offset -> Offset(c.x - r + o.x * 2 * r, c.y - r + o.y * 2 * r) }
                    val p = Path()
                    val a = m(pts[0]); val b = m(pts[1]); val d = m(pts[2]); val e = m(pts[3])
                    p.moveTo(a.x, a.y)
                    p.cubicTo(b.x, b.y, d.x, d.y, e.x, e.y)
                    drawPath(p, Color(0xFF8C8A84).copy(alpha = if (i % 2 == 0) 0.25f else 0.12f), style = Stroke(r * (0.004f + i * 0.0016f)))
                }
            }
        }
        DialFace.LEATHER -> {
            drawCircle(Brush.radialGradient(listOf(Color(0xFF8E5A37), Color(0xFF3A2112)), c, r), r, c)
            drawCircle(Color(0xFFF1E4CC).copy(alpha = 0.75f), r * 0.93f, c, style = Stroke(r * 0.012f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(r * 0.05f, r * 0.035f))))
        }
        DialFace.CARBON -> {
            drawCircle(Color(0xFF1A1B1E), r, c)
            val clip = Path().apply { addOval(Rect(c, r)) }
            clipPath(clip) {
                val cell = r / 9f
                var y = c.y - r
                var row = 0
                while (y < c.y + r) {
                    var x = c.x - r
                    var col = 0
                    while (x < c.x + r) {
                        val horizontal = (row + col) % 2 == 0
                        drawRect(
                            Brush.linearGradient(
                                listOf(Color(0xFF3A3C41), Color(0xFF1B1C1F)),
                                Offset(x, y), if (horizontal) Offset(x + cell, y) else Offset(x, y + cell),
                            ),
                            Offset(x + 0.6f, y + 0.6f), Size(cell - 1.2f, cell - 1.2f),
                        )
                        x += cell; col++
                    }
                    y += cell; row++
                }
            }
            drawCircle(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)), c, r), r, c)
        }
    }
    // ظل داخلي يعطي عمق
    if (f != DialFace.GLASS) drawCircle(Brush.radialGradient(0.8f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.28f), center = c, radius = r), r, c)
}

private fun DrawScope.drawIndices(cfg: WConfig, c: Offset, r: Float, ink: Color, m: TextMeasurer) {
    val inset = if (cfg.bezel == Bezel.RAILWAY) r * 0.84f else r * 0.93f
    for (h in 0 until 12) {
        val a = ang(h * 30f)
        val quarter = h % 3 == 0
        when (cfg.indices) {
            Indices.NONE -> Unit
            Indices.BARS, Indices.QUARTERS -> {
                if (cfg.indices == Indices.QUARTERS && !quarter) continue
                val len = if (quarter) r * 0.17f else r * 0.12f
                val w = if (quarter) r * 0.055f else r * 0.038f
                appliedBar(polar(c, inset, a), polar(c, inset - len, a), w, cfg.metal)
                if (quarter && h == 0 && cfg.indices == Indices.BARS) {
                    // مؤشر مزدوج عند ١٢
                    val off = r * 0.045f
                    val perp = a + PI / 2
                    val s = Offset((off * cos(perp)).toFloat(), (off * sin(perp)).toFloat())
                    appliedBar(polar(c, inset, a) + s, polar(c, inset - len, a) + s, w * 0.8f, cfg.metal)
                    appliedBar(polar(c, inset, a) - s, polar(c, inset - len, a) - s, w * 0.8f, cfg.metal)
                }
            }
            Indices.DOTS -> {
                val p = polar(c, inset - r * 0.06f, a)
                val rr = if (quarter) r * 0.05f else r * 0.034f
                drawCircle(cfg.metal.dark, rr, p)
                drawCircle(Brush.radialGradient(listOf(cfg.metal.light, cfg.metal.dark), p - Offset(rr * 0.3f, rr * 0.3f), rr * 1.2f), rr * 0.85f, p)
            }
            Indices.NUMERALS, Indices.ROMAN, Indices.BREGUET -> {
                val hour = if (h == 0) 12 else h
                val txt = when (cfg.indices) {
                    Indices.ROMAN -> listOf("XII", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI")[h]
                    else -> hour.digits(cfg.arabicDigits)
                }
                val family = when (cfg.indices) {
                    Indices.ROMAN -> WFonts.bodoni
                    Indices.BREGUET -> WFonts.playfair
                    else -> cfg.numFont.family
                }
                val fs = if (cfg.indices == Indices.ROMAN) r * 0.15f else r * 0.19f
                val pos = polar(c, inset - r * 0.16f, a)
                val col = if (cfg.face == DialFace.GLASS) Color.White else if (dialIsLight(cfg.face)) cfg.metal.dark else cfg.metal.light
                label(m, txt, pos, fs, col, family, if (cfg.indices == Indices.BREGUET) FontWeight.Normal else FontWeight.Medium)
                // نقاط دقائق بين الأرقام
                if (cfg.indices == Indices.BREGUET) drawCircle(ink.copy(alpha = 0.7f), r * 0.012f, polar(c, inset, ang(h * 30f + 15f)))
            }
        }
    }
    // علامات الدقائق الصغيرة
    if (cfg.bezel != Bezel.RAILWAY && cfg.indices != Indices.NONE) {
        for (k in 0 until 60) {
            if (k % 5 == 0) continue
            val a = ang(k * 6f)
            drawLine(ink.copy(alpha = 0.35f), polar(c, r * 0.95f, a), polar(c, r * 0.92f, a), r * 0.007f)
        }
    }
}

/** مؤشر بارز بلمعة (مثل الساعات الحقيقية) */
private fun DrawScope.appliedBar(a: Offset, b: Offset, w: Float, metal: Metal) {
    drawLine(Color.Black.copy(alpha = 0.3f), a + Offset(w * 0.15f, w * 0.25f), b + Offset(w * 0.15f, w * 0.25f), w, StrokeCap.Butt)
    drawLine(metal.dark, a, b, w, StrokeCap.Butt)
    drawLine(metal.light, a, b, w * 0.45f, StrokeCap.Butt)
}

private fun DrawScope.drawHand(style: Hands, metal: Metal, c: Offset, angle: Float, len: Float, w: Float, r: Float) {
    fun body(p: Path, shadow: Boolean) {
        if (shadow) translate(r * 0.018f, r * 0.03f) { drawPath(p, Color.Black.copy(alpha = 0.32f)) }
    }
    rotate(angle, c) {
        val x = c.x
        val y = c.y
        when (style) {
            Hands.DAUPHINE -> {
                val left = Path().apply { moveTo(x, y - len); lineTo(x - w, y - len * 0.08f); lineTo(x, y + len * 0.12f); close() }
                val right = Path().apply { moveTo(x, y - len); lineTo(x + w, y - len * 0.08f); lineTo(x, y + len * 0.12f); close() }
                body(Path().apply { addPath(left); addPath(right) }, true)
                drawPath(left, metal.light)
                drawPath(right, metal.dark)
            }
            Hands.SWORD, Hands.SKELETON -> {
                val hw = w * 0.55f
                val p = Path().apply {
                    moveTo(x, y - len); lineTo(x + hw, y - len * 0.85f); lineTo(x + hw, y + len * 0.1f)
                    lineTo(x - hw, y + len * 0.1f); lineTo(x - hw, y - len * 0.85f); close()
                }
                if (style == Hands.SKELETON) {
                    body(p, true)
                    drawPath(p, metal.light, style = Stroke(w * 0.22f))
                } else {
                    body(p, true)
                    drawPath(p, metal.dark)
                    drawLine(metal.light, Offset(x, y - len * 0.9f), Offset(x, y + len * 0.05f), hw * 0.7f)
                }
            }
            Hands.BATON -> {
                translate(r * 0.018f, r * 0.03f) { drawLine(Color.Black.copy(alpha = 0.3f), Offset(x, y + len * 0.12f), Offset(x, y - len), w * 0.7f, StrokeCap.Round) }
                drawLine(metal.dark, Offset(x, y + len * 0.12f), Offset(x, y - len), w * 0.7f, StrokeCap.Round)
                drawLine(metal.light, Offset(x, y + len * 0.1f), Offset(x, y - len * 0.98f), w * 0.3f, StrokeCap.Round)
            }
            Hands.LEAF -> {
                val p = Path().apply {
                    moveTo(x, y + len * 0.12f)
                    cubicTo(x - w * 1.2f, y - len * 0.25f, x - w * 0.9f, y - len * 0.7f, x, y - len)
                    cubicTo(x + w * 0.9f, y - len * 0.7f, x + w * 1.2f, y - len * 0.25f, x, y + len * 0.12f)
                    close()
                }
                body(p, true)
                drawPath(p, Brush.horizontalGradient(listOf(metal.light, metal.dark), x - w, x + w))
            }
            Hands.BREGUET -> {
                translate(r * 0.018f, r * 0.03f) { drawLine(Color.Black.copy(alpha = 0.3f), Offset(x, y + len * 0.1f), Offset(x, y - len), w * 0.3f, StrokeCap.Round) }
                drawLine(metal.dark, Offset(x, y + len * 0.1f), Offset(x, y - len * 0.62f), w * 0.32f, StrokeCap.Round)
                val ringC = Offset(x, y - len * 0.72f)
                drawCircle(metal.dark, w * 0.75f, ringC, style = Stroke(w * 0.28f))
                drawLine(metal.dark, Offset(x, y - len * 0.82f), Offset(x, y - len), w * 0.22f, StrokeCap.Round)
            }
        }
    }
}

private fun DrawScope.drawComplication(cfg: WConfig, c: Offset, r: Float, ink: Color, m: TextMeasurer, t: java.time.LocalDateTime, secDeg: Float) {
    val light = dialIsLight(cfg.face)
    val windowBg = if (light) Color.White else Color(0xFF0E0E10)
    val windowInk = if (light) Color(0xFF1C1C1E) else Color.White
    fun window(at: Offset, text: String, wf: Float) {
        val h = r * 0.17f
        val w = r * wf
        drawRoundRect(cfg.metal.dark, Offset(at.x - w / 2 - r * 0.012f, at.y - h / 2 - r * 0.012f), Size(w + r * 0.024f, h + r * 0.024f), CornerRadius(r * 0.03f))
        drawRoundRect(windowBg, Offset(at.x - w / 2, at.y - h / 2), Size(w, h), CornerRadius(r * 0.025f))
        label(m, text, at, r * 0.12f, windowInk, WFonts.grotesk, FontWeight.Medium)
    }
    val ad = cfg.arabicDigits
    when (cfg.comp) {
        Comp.NONE -> Unit
        Comp.DATE -> window(Offset(c.x + r * 0.58f, c.y), t.dayOfMonth.digits(ad), 0.22f)
        Comp.DAY_DATE -> {
            window(Offset(c.x + r * 0.6f, c.y), t.dayOfMonth.digits(ad), 0.2f)
            label(m, ArabicCalendar.dayName(t.dayOfWeek), Offset(c.x, c.y + r * 0.42f), r * 0.13f, ink.copy(alpha = 0.85f), WFonts.kufi, FontWeight.Medium)
        }
        Comp.HIJRI -> {
            val h = ArabicCalendar.hijri(t.toLocalDate())
            label(m, "${h.day.digits(ad)} ${h.monthName}", Offset(c.x, c.y + r * 0.42f), r * 0.11f, ink.copy(alpha = 0.85f), WFonts.kufi, FontWeight.Medium)
        }
        Comp.MOON -> {
            val mc = Offset(c.x, c.y + r * 0.45f)
            val mr = r * 0.16f
            drawCircle(Color(0xFF0C1E44), mr, mc)
            drawMoonPhase(mc, mr * 0.7f, moonAge(System.currentTimeMillis()))
            drawCircle(cfg.metal.dark, mr, mc, style = Stroke(r * 0.015f))
        }
        Comp.SUBSEC -> {
            val sc = Offset(c.x, c.y + r * 0.45f)
            val sr = r * 0.2f
            drawCircle(Color.Black.copy(alpha = if (light) 0.06f else 0.25f), sr, sc)
            drawCircle(ink.copy(alpha = 0.5f), sr, sc, style = Stroke(r * 0.006f))
            for (k in 0 until 12) {
                val a = ang(k * 30f)
                drawLine(ink.copy(alpha = 0.6f), polar(sc, sr, a), polar(sc, sr * 0.8f, a), r * 0.007f)
            }
            val a = ang(secDeg % 360f)
            drawLine(cfg.accent.color, sc, polar(sc, sr * 0.9f, a), r * 0.012f, StrokeCap.Round)
            drawCircle(cfg.accent.color, r * 0.02f, sc)
        }
    }
}

fun DrawScope.label(m: TextMeasurer, text: String, at: Offset, size: Float, color: Color, family: FontFamily, weight: FontWeight = FontWeight.Medium) {
    val layout = m.measure(text, TextStyle(color = color, fontSize = size.toSp(), fontFamily = family, fontWeight = weight))
    drawText(layout, topLeft = Offset(at.x - layout.size.width / 2f, at.y - layout.size.height / 2f))
}

// ───────────── القمر ─────────────

private const val SYNODIC = 29.530588853

/** عمر القمر بالأيام من آخر محاق */
fun moonAge(ms: Long): Double {
    val days = (ms - 947_182_440_000L) / 86_400_000.0
    return ((days % SYNODIC) + SYNODIC) % SYNODIC
}

fun moonPhaseName(age: Double): String = when {
    age < 1.8 -> "محاق"
    age < 6.4 -> "هلال"
    age < 8.9 -> "تربيع أول"
    age < 13.8 -> "أحدب متزايد"
    age < 15.8 -> "بدر"
    age < 21.0 -> "أحدب متناقص"
    age < 23.5 -> "تربيع أخير"
    age < 28.0 -> "هلال آخر الشهر"
    else -> "محاق"
}

/** نرسم القمر الحقيقي: الجزء المضيء حسب عمره */
fun DrawScope.drawMoonPhase(c: Offset, r: Float, age: Double) {
    val phi = 2 * PI * age / SYNODIC
    val k = cos(phi).toFloat()
    val waxing = age < SYNODIC / 2
    drawCircle(Color(0xFF2B2F3A), r, c)
    val half = Path().apply {
        addArc(Rect(c, r), if (waxing) -90f else 90f, 180f)
        close()
    }
    val ell = Path().apply { addOval(Rect(c.x - r * abs(k), c.y - r, c.x + r * abs(k), c.y + r)) }
    val lit = if (k > 0) Path.combine(PathOperation.Difference, half, ell) else Path.combine(PathOperation.Union, half, ell)
    drawPath(lit, Brush.radialGradient(listOf(Color(0xFFFFF8E7), Color(0xFFE6DCC2)), c, r))
    // فوهات خفيفة
    clipPath(lit) {
        listOf(Offset(-0.3f, -0.2f) to 0.18f, Offset(0.25f, 0.3f) to 0.12f, Offset(0.1f, -0.45f) to 0.09f, Offset(-0.15f, 0.42f) to 0.1f).forEach { (o, s) ->
            drawCircle(Color(0xFFC9BFA5).copy(alpha = 0.45f), r * s, Offset(c.x + o.x * r, c.y + o.y * r))
        }
    }
    drawCircle(Color(0xFFFFF4D6).copy(alpha = 0.12f), r * 1.25f, c)
}
