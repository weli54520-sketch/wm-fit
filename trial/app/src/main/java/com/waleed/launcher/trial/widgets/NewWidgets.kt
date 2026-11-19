package com.waleed.launcher.trial.widgets

import android.app.DatePickerDialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private fun hm24(ms: Long, ad: Boolean): String {
    val t = ms.toLocal()
    val h = (t.hour % 12).let { if (it == 0) 12 else it }
    return String.format(Locale.US, "%d:%02d", h, t.minute).digits(ad)
}

// ───────────── قوس الشمس ─────────────

@Composable
fun SunArcWidget(cfg: WConfig) {
    val env = LocalWidgetEnv.current ?: return
    val coords = env.location.coords ?: return NeedLocationBox()
    val now = LocalNow.current
    val today = now.toLocal().toLocalDate()
    val day = remember(coords, today) { runCatching { PrayerCalc.day(coords.first, coords.second, today) }.getOrNull() } ?: return NeedLocationBox()
    val next = remember(coords, now / 60_000) { PrayerCalc.next(coords.first, coords.second, now) }
    val measurer = rememberTextMeasurer()
    val ad = cfg.arabicDigits
    val accent = cfg.accent.color
    val sunrise = day.times[1]
    val sunset = day.times[4]

    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val horizonY = h * 0.66f
            val left = w * 0.08f
            val right = w * 0.92f
            val arcH = h * 0.5f
            // الأفق
            drawLine(Color.White.copy(alpha = 0.25f), Offset(w * 0.03f, horizonY), Offset(w * 0.97f, horizonY), 1.5f)
            fun pointAt(frac: Float): Offset {
                val x = left + (right - left) * frac
                val y = horizonY - arcH * sin(PI * frac).toFloat()
                return Offset(x, y)
            }
            // القوس
            val path = Path()
            for (i in 0..60) {
                val p = pointAt(i / 60f)
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            drawPath(path, Color.White.copy(alpha = 0.35f), style = Stroke(2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f))))
            // الجزء اللي مشته الشمس
            val frac = ((now - sunrise).toFloat() / (sunset - sunrise)).coerceIn(0f, 1f)
            val done = Path()
            for (i in 0..60) {
                val p = pointAt(frac * i / 60f)
                if (i == 0) done.moveTo(p.x, p.y) else done.lineTo(p.x, p.y)
            }
            drawPath(done, Brush.horizontalGradient(listOf(accent.copy(alpha = 0.3f), accent), left, pointAt(frac).x + 1f), style = Stroke(3.5f, cap = StrokeCap.Round))
            // علامات الصلاة على القوس (الظهر والعصر)
            listOf(2, 3).forEach { i ->
                val f = ((day.times[i] - sunrise).toFloat() / (sunset - sunrise)).coerceIn(0f, 1f)
                val p = pointAt(f)
                drawCircle(Color.White, 3.5f, p)
                label(measurer, PrayerCalc.names[i], Offset(p.x, p.y - h * 0.09f), h * 0.075f, Color.White.copy(alpha = 0.75f), WFonts.kufi)
            }
            // الشمس أو القمر
            val isDay = now in sunrise..sunset
            if (isDay) {
                val sp = pointAt(frac)
                drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.55f), Color.Transparent), sp, h * 0.16f), h * 0.16f, sp)
                drawCircle(accent, h * 0.05f, sp)
                drawCircle(Color.White, h * 0.03f, sp)
            } else {
                val mp = Offset(w * 0.5f, horizonY + h * 0.14f)
                drawMoonPhase(mp, h * 0.06f, moonAge(now))
            }
            // الشروق والغروب تحت
            label(measurer, "الشروق ${hm24(sunrise, ad)}", Offset(left + w * 0.06f, horizonY + h * 0.12f), h * 0.07f, Color.White.copy(alpha = 0.7f), WFonts.kufi)
            label(measurer, "الغروب ${hm24(sunset, ad)}", Offset(right - w * 0.06f, horizonY + h * 0.12f), h * 0.07f, Color.White.copy(alpha = 0.7f), WFonts.kufi)
        }
        Column(Modifier.align(Alignment.TopStart).padding(14.dp)) {
            WText(PrayerCalc.names[next.index], 14.sp2(), weight = FontWeight.SemiBold, color = accent, align = TextAlign.Start)
            WText(hm24(next.time, ad), 20.sp2(), family = cfg.numFont.family, weight = FontWeight.Light, align = TextAlign.Start)
        }
    }
}

private fun Int.sp2() = androidx.compose.ui.unit.TextUnit(this.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp)

@Composable
private fun NeedLocationBox() {
    val env = LocalWidgetEnv.current
    Box(
        Modifier.fillMaxSize().clickable { env?.requestLocation?.invoke() },
        contentAlignment = Alignment.Center,
    ) {
        WText("اضغط لتحديد موقعك", 13.sp2(), color = Soft, maxLines = 2)
    }
}

// ───────────── يومك بسطر ─────────────

@Composable
fun DayLineWidget(cfg: WConfig) {
    val env = LocalWidgetEnv.current ?: return
    val coords = env.location.coords ?: return NeedLocationBox()
    val now = LocalNow.current
    val t = now.toLocal()
    val today = t.toLocalDate()
    val day = remember(coords, today) { runCatching { PrayerCalc.day(coords.first, coords.second, today) }.getOrNull() } ?: return NeedLocationBox()
    val measurer = rememberTextMeasurer()
    val ad = cfg.arabicDigits
    val startOfDay = today.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val y = h * 0.6f
            val l = w * 0.06f
            val r = w * 0.94f
            fun xAt(ms: Long) = l + (r - l) * ((ms - startOfDay) / 86_400_000f).coerceIn(0f, 1f)
            // الليل والنهار على الشريط
            drawLine(Brush.horizontalGradient(listOf(Color(0xFF2B3A67), Color(0xFFF49BC6), Color(0xFF8EC5FC), Color(0xFFF7A867), Color(0xFF2B3A67)), l, r), Offset(l, y), Offset(r, y), h * 0.06f, StrokeCap.Round)
            for (hh in 0..24 step 3) {
                val x = l + (r - l) * hh / 24f
                drawLine(Color.White.copy(alpha = 0.3f), Offset(x, y + h * 0.06f), Offset(x, y + h * 0.1f), 1.5f)
            }
            day.times.forEachIndexed { i, ms ->
                val x = xAt(ms)
                drawCircle(Color.White, h * 0.025f, Offset(x, y))
                val up = i % 2 == 0
                label(measurer, PrayerCalc.names[i], Offset(x, if (up) y - h * 0.2f else y + h * 0.2f), h * 0.075f, Color.White.copy(alpha = 0.8f), WFonts.kufi)
                label(measurer, hm24(ms, ad), Offset(x, if (up) y - h * 0.11f else y + h * 0.29f), h * 0.065f, Color.White.copy(alpha = 0.55f), cfg.numFont.family)
            }
            val nx = xAt(now)
            drawCircle(Brush.radialGradient(listOf(cfg.accent.color.copy(alpha = 0.6f), Color.Transparent), Offset(nx, y), h * 0.14f), h * 0.14f, Offset(nx, y))
            drawCircle(cfg.accent.color, h * 0.04f, Offset(nx, y))
        }
        WText(
            "${ArabicCalendar.dayName(t.dayOfWeek)} · ${hm24(now, ad)}", 13.sp2(), color = Soft,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp),
        )
    }
}

// ───────────── القمر والهجري ─────────────

@Composable
fun MoonWidget(cfg: WConfig) {
    val now = LocalNow.current
    val age = moonAge(now)
    val h = ArabicCalendar.hijri(now.toLocal().toLocalDate())
    val ad = cfg.arabicDigits
    BoxWithConstraints(Modifier.fillMaxSize().padding(12.dp)) {
        val wide = maxWidth > maxHeight * 1.3f
        val moonSize = if (wide) maxHeight * 0.8f else min(maxWidth.value, maxHeight.value * 0.62f).dp
        val content: @Composable () -> Unit = {
            Canvas(Modifier.size(moonSize)) {
                val c = center
                val r = size.minDimension / 2 * 0.8f
                drawCircle(Brush.radialGradient(listOf(Color(0x33FFF4D6), Color.Transparent), c, r * 1.25f), r * 1.25f, c)
                drawMoonPhase(c, r, age)
            }
        }
        val texts: @Composable () -> Unit = {
            Column(horizontalAlignment = if (wide) Alignment.Start else Alignment.CenterHorizontally) {
                WText("${h.day.digits(ad)} ${h.monthName}", (moonSize * if (wide) 0.2f else 0.17f).sp(), family = WFonts.amiri, weight = FontWeight.Bold, color = cfg.accent.color)
                WText(moonPhaseName(age), (moonSize * 0.12f).sp(), color = Soft)
            }
        }
        if (wide) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) { content(); texts() }
        } else {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { content(); texts() }
        }
    }
}

// ───────────── حلقات الوقت ─────────────

@Composable
fun RingsClock(cfg: WConfig) {
    val now = LocalNow.current
    val t = now.toLocal()
    val ad = cfg.arabicDigits
    val dayFrac = (t.hour * 60 + t.minute) / 1440f
    val week = ((t.dayOfWeek.value % 7) + dayFrac) / 7f
    val month = ((t.dayOfMonth - 1) + dayFrac) / t.toLocalDate().lengthOfMonth()
    val year = ((t.dayOfYear - 1) + dayFrac) / t.toLocalDate().lengthOfYear()
    val rings = listOf(dayFrac to Accent.GOLD.color, week to Accent.MINT.color, month to Accent.SILVER.color, year to Accent.PINK.color)
    BoxWithConstraints(Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.Center) {
        val side = min(maxWidth.value, maxHeight.value).dp
        Canvas(Modifier.size(side)) {
            val sw = size.minDimension * 0.065f
            rings.forEachIndexed { i, (f, col) ->
                val inset = sw * 0.6f + i * sw * 1.45f
                val s = Size(size.width - inset * 2, size.height - inset * 2)
                drawArc(col.copy(alpha = 0.16f), 0f, 360f, false, Offset(inset, inset), s, style = Stroke(sw))
                drawArc(Brush.sweepGradient(listOf(col.copy(alpha = 0.5f), col), center), -90f, 360f * f, false, Offset(inset, inset), s, style = Stroke(sw, cap = StrokeCap.Round))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            WText(String.format(Locale.US, "%d:%02d", (t.hour % 12).let { if (it == 0) 12 else it }, t.minute).digits(ad), (side * 0.17f).sp(), family = cfg.numFont.family, weight = FontWeight.Light)
            WText("${(year * 100).toInt().digits(ad)}٪ من السنة", (side * 0.06f).sp(), color = Soft)
        }
    }
}

// ───────────── أرقام ضخمة مقصوصة ─────────────

@Composable
fun OversizeClock(cfg: WConfig) {
    val t = LocalNow.current.toLocal()
    val ad = cfg.arabicDigits
    val hh = String.format(Locale.US, "%02d", (t.hour % 12).let { if (it == 0) 12 else it }).digits(ad)
    val mm = String.format(Locale.US, "%02d", t.minute).digits(ad)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val big = maxHeight * 1.05f
        val mh = maxHeight
        WText(
            hh, big.sp(), family = cfg.numFont.family, weight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.92f), shadow = false, align = TextAlign.Start,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 6.dp).width(maxWidth * 1.2f),
        )
        Column(Modifier.align(Alignment.TopEnd).padding(14.dp), horizontalAlignment = Alignment.End) {
            WText(mm, (mh * 0.32f).sp(), family = cfg.numFont.family, weight = FontWeight.Light, color = cfg.accent.color)
            if (cfg.showDate) WText(ArabicCalendar.dayName(t.dayOfWeek), (mh * 0.09f).sp(), color = Soft)
        }
    }
}

// ───────────── بطارية سائلة ─────────────

@Composable
fun LiquidBattery(cfg: WConfig) {
    val b = rememberBattery()
    val ad = cfg.arabicDigits
    val col = if (b.level <= 15 && !b.charging) Color(0xFFFF7A7A) else if (b.charging) Color(0xFF9FE3CB) else cfg.accent.color
    BoxWithConstraints(Modifier.fillMaxSize().padding(14.dp), contentAlignment = Alignment.Center) {
        val vertical = maxHeight >= maxWidth * 0.8f
        Canvas(Modifier.fillMaxSize()) {
            val tw = if (vertical) size.width * 0.42f else size.width * 0.9f
            val th = if (vertical) size.height * 0.92f else size.height * 0.45f
            val tl = Offset((size.width - tw) / 2, (size.height - th) / 2)
            val corner = CornerRadius(min(tw, th) * 0.3f)
            val tube = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(tl.x, tl.y, tl.x + tw, tl.y + th, corner)) }
            drawPath(tube, Color.White.copy(alpha = 0.08f))
            clipPath(tube) {
                val level = b.level / 100f
                if (vertical) {
                    val top = tl.y + th * (1 - level)
                    val wave = Path().apply {
                        moveTo(tl.x, top)
                        var x = 0f
                        while (x <= tw) {
                            lineTo(tl.x + x, top + sin(x / tw * 2 * PI * 1.5).toFloat() * th * 0.012f)
                            x += tw / 30
                        }
                        lineTo(tl.x + tw, tl.y + th); lineTo(tl.x, tl.y + th); close()
                    }
                    drawPath(wave, Brush.verticalGradient(listOf(col, col.copy(alpha = 0.55f)), top, tl.y + th))
                    if (b.charging) listOf(0.3f to 0.75f, 0.6f to 0.85f, 0.45f to 0.92f).forEach { (fx, fy) ->
                        drawCircle(Color.White.copy(alpha = 0.35f), tw * 0.04f, Offset(tl.x + tw * fx, tl.y + th * fy), style = Stroke(1.5f))
                    }
                } else {
                    drawRect(Brush.horizontalGradient(listOf(col.copy(alpha = 0.55f), col), tl.x, tl.x + tw * level), tl, Size(tw * level, th))
                }
                // لمعة الأنبوب
                drawRect(Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.25f), Color.Transparent, Color.White.copy(alpha = 0.08f)), tl.x, tl.x + tw), tl, Size(tw, th))
            }
            drawPath(tube, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.7f), Color.White.copy(alpha = 0.1f), Color.White.copy(alpha = 0.4f)), tl, tl + Offset(tw, th)), style = Stroke(1.5f))
        }
        WText("${b.level.digits(ad)}٪", (if (vertical) maxWidth * 0.16f else maxHeight * 0.22f).sp(), family = cfg.numFont.family, weight = FontWeight.SemiBold)
    }
}

// ───────────── سماء الطقس ─────────────

private fun skyColors(hour: Int): List<Color> = when (hour) {
    in 5..6 -> listOf(Color(0xFF2B3A67), Color(0xFFC86B98), Color(0xFFF7A867))
    in 7..15 -> listOf(Color(0xFF2E6BC6), Color(0xFF6FA8E8), Color(0xFFB9DBF7))
    in 16..18 -> listOf(Color(0xFF2E1A47), Color(0xFFB0476B), Color(0xFFF7A867))
    else -> listOf(Color(0xFF03050C), Color(0xFF0E1A33), Color(0xFF1E2E52))
}

@Composable
fun WeatherSkyWidget(cfg: WConfig) {
    val env = LocalWidgetEnv.current ?: return
    val coords = env.location.coords ?: return NeedLocationBox()
    LaunchedEffect(coords) { env.weather.refreshIfNeeded(coords.first, coords.second) }
    val t = LocalNow.current.toLocal()
    val w = env.weather.data
    val ad = cfg.arabicDigits
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.verticalGradient(skyColors(t.hour)))
            if (t.hour !in 5..18) {
                listOf(0.1f to 0.2f, 0.3f to 0.12f, 0.55f to 0.25f, 0.8f to 0.15f, 0.7f to 0.4f, 0.2f to 0.45f).forEach { (x, y) ->
                    drawCircle(Color.White.copy(alpha = 0.6f), 1.5f, Offset(size.width * x, size.height * y))
                }
            }
        }
        if (w == null) {
            WText("جاري تحميل الطقس…", 13.sp2(), color = Soft, modifier = Modifier.align(Alignment.Center))
        } else {
            BoxWithConstraints(Modifier.fillMaxSize().padding(14.dp)) {
                val u = min(maxWidth.value, maxHeight.value * 1.6f).dp
                Column(Modifier.align(Alignment.TopStart)) {
                    WText("${w.temp.digits(ad)}°", (u * 0.24f).sp(), family = cfg.numFont.family, weight = FontWeight.Light, align = TextAlign.Start)
                    WText(WeatherStore.describe(w.code), (u * 0.07f).sp(), align = TextAlign.Start)
                    w.daily.firstOrNull()?.let { WText("${it.max.digits(ad)}° / ${it.min.digits(ad)}°", (u * 0.06f).sp(), color = Soft, align = TextAlign.Start) }
                }
                WeatherGlyph(w.code, Modifier.align(Alignment.TopEnd).size(u * 0.2f), cfg.accent.color)
                if (maxWidth > maxHeight * 1.3f) {
                    Row(Modifier.align(Alignment.BottomEnd), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        w.hourly.drop(1).take(4).forEach { (hour, temp, code) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                WText(hour.digits(ad), (u * 0.05f).sp(), color = Soft)
                                WeatherGlyph(code, Modifier.size(u * 0.08f), Color.White)
                                WText("${temp.digits(ad)}°", (u * 0.055f).sp(), family = cfg.numFont.family)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ───────────── عدادات ─────────────

private data class Target(val title: String, val date: LocalDate?)

private fun salaryDate(today: LocalDate, day: Int): LocalDate {
    fun forMonth(m: LocalDate): LocalDate {
        var d = m.withDayOfMonth(day.coerceIn(1, m.lengthOfMonth()))
        // إذا صادف جمعة أو سبت ينزل الخميس اللي قبله
        while (d.dayOfWeek == DayOfWeek.FRIDAY || d.dayOfWeek == DayOfWeek.SATURDAY) d = d.minusDays(1)
        return d
    }
    val thisMonth = forMonth(today)
    return if (!thisMonth.isBefore(today)) thisMonth else forMonth(today.plusMonths(1).withDayOfMonth(1))
}

private fun hijriTarget(today: LocalDate, month: Int, day: Int): LocalDate {
    val y = ArabicCalendar.hijri(today).year
    var d = LocalDate.from(HijrahDate.of(y, month, day))
    if (d.isBefore(today)) d = LocalDate.from(HijrahDate.of(y + 1, month, day))
    return d
}

@Composable
fun CountWidget(cfg: WConfig) {
    val today = LocalNow.current.toLocal().toLocalDate()
    val ad = cfg.arabicDigits
    val target = remember(cfg, today) {
        when (cfg.variant) {
            WVariant.COUNT_SALARY -> Target("الراتب", salaryDate(today, cfg.salaryDay))
            WVariant.COUNT_RAMADAN -> Target("رمضان", hijriTarget(today, 9, 1))
            WVariant.COUNT_EID -> {
                val fitr = hijriTarget(today, 10, 1)
                val adha = hijriTarget(today, 12, 10)
                if (fitr.isBefore(adha)) Target("عيد الفطر", fitr) else Target("عيد الأضحى", adha)
            }
            else -> Target(cfg.countTitle.ifBlank { "مناسبتك" }, if (cfg.countDay > 0) LocalDate.ofEpochDay(cfg.countDay) else null)
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize().padding(14.dp), contentAlignment = Alignment.Center) {
        val u = min(maxWidth.value, maxHeight.value * 1.6f).dp
        val d = target.date
        if (d == null) {
            WText("ضغطة طويلة ← حدد التاريخ", (u * 0.07f).sp(), color = Soft, maxLines = 2)
            return@BoxWithConstraints
        }
        val days = ChronoUnit.DAYS.between(today, d)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            WText("باقي على ${target.title}", (u * 0.075f).sp(), color = Soft)
            WText(if (days == 0L) "اليوم" else days.toInt().digits(ad), (u * 0.3f).sp(), family = cfg.numFont.family, weight = FontWeight.Light, color = cfg.accent.color)
            if (days > 0) WText(if (days == 1L) "يوم" else "يوم · ${d.dayOfMonth.digits(ad)} ${ArabicCalendar.gregMonths[d.monthValue - 1]}", (u * 0.06f).sp(), color = Soft)
        }
    }
}

/** نافذة اختيار تاريخ المناسبة */
fun pickDate(context: android.content.Context, initial: Long, onPick: (Long) -> Unit) {
    val d = if (initial > 0) LocalDate.ofEpochDay(initial) else LocalDate.now().plusDays(30)
    DatePickerDialog(context, { _, y, m, day -> onPick(LocalDate.of(y, m + 1, day).toEpochDay()) }, d.year, d.monthValue - 1, d.dayOfMonth).show()
}

