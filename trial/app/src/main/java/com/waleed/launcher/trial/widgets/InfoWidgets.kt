package com.waleed.launcher.trial.widgets

import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** اللي تحتاجه الويدجت من برا: الموقع، الطقس، طلب إذن الموقع */
class WidgetEnv(
    val location: LocationStore,
    val weather: WeatherStore,
    val requestLocation: () -> Unit,
)

val LocalWidgetEnv = staticCompositionLocalOf<WidgetEnv?> { null }

private fun hm(ms: Long, arabic: Boolean): String {
    val t = ms.toLocal()
    val h = (t.hour % 12).let { if (it == 0) 12 else it }
    return String.format(Locale.US, "%d:%02d", h, t.minute).digits(arabic)
}

@Composable
private fun NeedLocation() {
    val env = LocalWidgetEnv.current
    Box(Modifier.fillMaxSize().clickable { env?.requestLocation?.invoke() }, contentAlignment = Alignment.Center) {
        WText("اضغط لتحديد موقعك", 13.sp, color = Soft, maxLines = 2)
    }
}

// ───────────── التاريخ ─────────────

@Composable
fun DateWidget(cfg: WConfig) {
    val today = LocalNow.current.toLocal().toLocalDate()
    val hijri = ArabicCalendar.hijri(today)
    val ad = cfg.arabicDigits
    BoxWithConstraints(Modifier.fillMaxSize().padding(14.dp), contentAlignment = Alignment.Center) {
        val unit = min(maxWidth.value, maxHeight.value * 1.6f).dp
        val side = min(maxWidth.value, maxHeight.value).dp
        val wide = maxWidth > maxHeight * 1.4f
        when (cfg.variant) {
            WVariant.DATE_DAY -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                WText(ArabicCalendar.dayName(today.dayOfWeek), (unit * 0.27f).sp(), family = WFonts.ruqaa, weight = FontWeight.Bold, color = cfg.accent.color)
                Spacer(Modifier.height(unit * 0.03f))
                WText("${hijri.day.digits(ad)} ${hijri.monthName} ${hijri.year.digits(ad)}", (unit * 0.085f).sp(), family = WFonts.kufi)
                WText("${today.dayOfMonth.digits(ad)} ${ArabicCalendar.gregMonths[today.monthValue - 1]} ${today.year.digits(ad)}", (unit * 0.075f).sp(), color = Soft)
            }
            WVariant.DATE_DUAL -> Row(verticalAlignment = Alignment.CenterVertically) {
                DateColumn(hijri.day.digits(ad), hijri.monthName, "هـ ${hijri.year.digits(ad)}", unit, cfg, Modifier.weight(1f), accent = true)
                Box(Modifier.width(1.dp).fillMaxHeight(0.6f).background(Faint))
                DateColumn(today.dayOfMonth.digits(ad), ArabicCalendar.gregMonths[today.monthValue - 1], "م ${today.year.digits(ad)}", unit, cfg, Modifier.weight(1f), accent = false)
            }
            WVariant.DATE_RAMADAN -> {
                val (days, start) = ArabicCalendar.daysToRamadan(today)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    WText("رمضان", (unit * 0.13f).sp(), family = WFonts.ruqaa, weight = FontWeight.Bold, color = cfg.accent.color)
                    WText(days.toInt().digits(ad), (unit * 0.32f).sp(), family = cfg.numFont.family, weight = FontWeight.Light)
                    WText(if (days == 1L) "يوم باقي" else "يوم باقي · ${start.dayOfMonth.digits(ad)} ${ArabicCalendar.gregMonths[start.monthValue - 1]}", (unit * 0.07f).sp(), color = Soft)
                }
            }
            WVariant.DATE_WEEK -> WeekStrip(today, cfg, unit, wide)
            else -> MonthCalendar(today, cfg)
        }
    }
}

@Composable
private fun DateColumn(day: String, month: String, year: String, unit: androidx.compose.ui.unit.Dp, cfg: WConfig, modifier: Modifier, accent: Boolean) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        WText(day, (unit * 0.3f).sp(), family = cfg.numFont.family, weight = FontWeight.Light, color = if (accent) cfg.accent.color else Color.White)
        WText(month, (unit * 0.075f).sp())
        WText(year, (unit * 0.06f).sp(), color = Soft)
    }
}

@Composable
private fun WeekStrip(today: LocalDate, cfg: WConfig, unit: androidx.compose.ui.unit.Dp, wide: Boolean) {
    val start = today.minusDays((today.dayOfWeek.value % 7).toLong()) // يبدأ الأحد
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        WText(
            "${ArabicCalendar.dayName(today.dayOfWeek)} · ${ArabicCalendar.hijri(today).let { "${it.day.digits(cfg.arabicDigits)} ${it.monthName}" }}",
            (unit * 0.075f).sp(), color = Soft,
        )
        Spacer(Modifier.height(unit * 0.05f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            for (i in 0 until 7) {
                val d = start.plusDays(i.toLong())
                val isToday = d == today
                Column(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (isToday) cfg.accent.color else Color.Transparent)
                        .padding(horizontal = unit * 0.015f, vertical = unit * 0.03f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    WText(ArabicCalendar.dayLetter(d.dayOfWeek), (unit * 0.06f).sp(), color = if (isToday) Color.Black else Soft, shadow = !isToday)
                    WText(d.dayOfMonth.digits(cfg.arabicDigits), (unit * if (wide) 0.09f else 0.08f).sp(), family = cfg.numFont.family, weight = FontWeight.Medium, color = if (isToday) Color.Black else Color.White, shadow = !isToday)
                }
            }
        }
    }
}

@Composable
private fun MonthCalendar(today: LocalDate, cfg: WConfig) {
    val first = today.withDayOfMonth(1)
    val offset = first.dayOfWeek.value % 7
    val days = today.lengthOfMonth()
    val hFirst = ArabicCalendar.hijri(first)
    val hLast = ArabicCalendar.hijri(today.withDayOfMonth(days))
    val ad = cfg.arabicDigits
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val cellW = maxWidth / 7
        val rows = (offset + days + 6) / 7
        val cellH = (maxHeight * 0.84f) / rows
        val fs = min(cellW.value * 0.42f, cellH.value * 0.55f).dp
        val headerH = maxHeight * 0.16f
        Column {
            Row(Modifier.fillMaxWidth().height(headerH), verticalAlignment = Alignment.CenterVertically) {
                WText(ArabicCalendar.gregMonths[today.monthValue - 1], (fs * 1.1f).sp(), weight = FontWeight.SemiBold, color = cfg.accent.color, align = TextAlign.Start, modifier = Modifier.weight(1f))
                WText(
                    if (hFirst.month == hLast.month) hFirst.monthName else "${hFirst.monthName} – ${hLast.monthName}",
                    (fs * 0.85f).sp(), color = Soft, align = TextAlign.End, modifier = Modifier.weight(1.4f),
                )
            }
            for (row in 0 until rows) {
                Row(Modifier.height(cellH)) {
                    for (col in 0 until 7) {
                        val dayNum = row * 7 + col - offset + 1
                        Box(Modifier.width(cellW).fillMaxHeight(), contentAlignment = Alignment.Center) {
                            if (dayNum in 1..days) {
                                val d = first.withDayOfMonth(dayNum)
                                val isToday = d == today
                                if (isToday) Box(Modifier.size(min(cellW.value, cellH.value).dp * 0.92f).clip(CircleShape).background(cfg.accent.color))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    WText(dayNum.digits(ad), fs.sp(), family = cfg.numFont.family, weight = FontWeight.Medium, color = if (isToday) Color.Black else Color.White, shadow = !isToday)
                                    WText(ArabicCalendar.hijri(d).day.digits(ad), (fs * 0.62f).sp(), color = if (isToday) Color.Black.copy(alpha = 0.7f) else Faint, shadow = false)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ───────────── الصلاة ─────────────

@Composable
fun PrayerWidget(cfg: WConfig) {
    val env = LocalWidgetEnv.current ?: return
    val coords = env.location.coords ?: return NeedLocation()
    val now = LocalNow.current
    val ad = cfg.arabicDigits
    val today = now.toLocal().toLocalDate()
    val day = remember(coords, today) { runCatching { PrayerCalc.day(coords.first, coords.second, today) }.getOrNull() } ?: return NeedLocation()
    val next = remember(coords, now / 60_000) { PrayerCalc.next(coords.first, coords.second, now) }
    val left = ((next.time - now) / 60_000).coerceAtLeast(0)
    val leftText = String.format(Locale.US, "%d:%02d", left / 60, left % 60).digits(ad)

    BoxWithConstraints(Modifier.fillMaxSize().padding(14.dp), contentAlignment = Alignment.Center) {
        val unit = min(maxWidth.value, maxHeight.value * 1.6f).dp
        val side = min(maxWidth.value, maxHeight.value).dp
        val wide = maxWidth > maxHeight * 1.4f
        when (cfg.variant) {
            WVariant.PRAYER_RING -> Box(contentAlignment = Alignment.Center) {
                val progress = ((now - next.previous).toFloat() / (next.time - next.previous).coerceAtLeast(1)).coerceIn(0f, 1f)
                Canvas(Modifier.size(side)) {
                    val sw = size.minDimension * 0.07f
                    drawCircle(Color.White.copy(alpha = 0.12f), size.minDimension / 2 - sw, style = Stroke(sw))
                    drawArc(cfg.accent.color, -90f, 360f * progress, false, Offset(sw, sw), Size(size.width - sw * 2, size.height - sw * 2), style = Stroke(sw, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    WText(PrayerCalc.names[next.index], (unit * 0.11f).sp(), weight = FontWeight.SemiBold, color = cfg.accent.color)
                    WText(hm(next.time, ad), (unit * 0.13f).sp(), family = cfg.numFont.family)
                    WText("بعد $leftText", (unit * 0.06f).sp(), color = Soft)
                }
            }
            WVariant.PRAYER_COUNTDOWN -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                WText("باقي على ${PrayerCalc.names[next.index]}", (unit * 0.08f).sp(), color = Soft)
                WText(leftText, (unit * 0.3f).sp(), family = cfg.numFont.family, weight = FontWeight.Light, color = cfg.accent.color)
                WText(hm(next.time, ad), (unit * 0.07f).sp(), color = Soft)
            }
            else -> {
                val idx = if (wide) listOf(0, 1, 2, 3, 4, 5) else listOf(0, 2, 3, 4, 5)
                if (wide) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        idx.forEach { i -> PrayerCell(i, day.times[i], i == next.index, cfg, unit, Modifier.weight(1f)) }
                    }
                } else {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.SpaceEvenly) {
                        idx.forEach { i ->
                            val active = i == next.index
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(if (active) cfg.accent.color.copy(alpha = 0.22f) else Color.Transparent).padding(horizontal = 8.dp, vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                WText(PrayerCalc.names[i], (unit * 0.075f).sp(), color = if (active) cfg.accent.color else Color.White, align = TextAlign.Start, modifier = Modifier.weight(1f))
                                WText(hm(day.times[i], ad), (unit * 0.075f).sp(), family = cfg.numFont.family, color = if (active) cfg.accent.color else Soft)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrayerCell(i: Int, time: Long, active: Boolean, cfg: WConfig, unit: androidx.compose.ui.unit.Dp, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(if (active) cfg.accent.color.copy(alpha = 0.2f) else Color.Transparent).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WText(PrayerCalc.names[i], (unit * 0.045f).sp(), color = if (active) cfg.accent.color else Soft)
        Spacer(Modifier.height(4.dp))
        WText(hm(time, cfg.arabicDigits), (unit * 0.06f).sp(), family = cfg.numFont.family, weight = FontWeight.Medium, color = if (active) cfg.accent.color else Color.White)
    }
}

// ───────────── الطقس ─────────────

private fun tempColor(t: Int): Color {
    val stops = listOf(5 to Color(0xFF9DB0FF), 18 to Color(0xFF9FE3CB), 28 to Color(0xFFE3C285), 40 to Color(0xFFF7A867))
    if (t <= stops.first().first) return stops.first().second
    for (k in 1 until stops.size) {
        val (t1, c1) = stops[k - 1]
        val (t2, c2) = stops[k]
        if (t <= t2) return lerp(c1, c2, (t - t1).toFloat() / (t2 - t1))
    }
    return stops.last().second
}

@Composable
fun WeatherWidget(cfg: WConfig) {
    val env = LocalWidgetEnv.current ?: return
    val coords = env.location.coords ?: return NeedLocation()
    LaunchedEffect(coords) { env.weather.refreshIfNeeded(coords.first, coords.second) }
    val w = env.weather.data
    if (w == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { WText("جاري تحميل الطقس…", 13.sp, color = Soft) }
        return
    }
    val ad = cfg.arabicDigits
    BoxWithConstraints(Modifier.fillMaxSize().padding(14.dp), contentAlignment = Alignment.Center) {
        val unit = min(maxWidth.value, maxHeight.value * 1.6f).dp
        val side = min(maxWidth.value, maxHeight.value).dp
        val wide = maxWidth > maxHeight * 1.4f
        when (cfg.variant) {
            WVariant.WEATHER_BIG -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                WeatherGlyph(w.code, Modifier.size(unit * 0.22f), cfg.accent.color)
                WText("${w.temp.digits(ad)}°", (unit * 0.3f).sp(), family = cfg.numFont.family, weight = FontWeight.Light)
                WText(WeatherStore.describe(w.code), (unit * 0.075f).sp())
                w.daily.firstOrNull()?.let { WText("${it.max.digits(ad)}° / ${it.min.digits(ad)}°", (unit * 0.065f).sp(), color = Soft) }
            }
            WVariant.WEATHER_DAYS -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
                val days = w.daily.take(if (wide) 5 else 4)
                val lo = days.minOf { it.min }
                val hi = days.maxOf { it.max }.coerceAtLeast(lo + 1)
                days.forEachIndexed { i, d ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WText(if (i == 0) "اليوم" else ArabicCalendar.dayName(d.date.dayOfWeek), (unit * 0.06f).sp(), align = TextAlign.Start, modifier = Modifier.width(unit * 0.28f))
                        WeatherGlyph(d.code, Modifier.size(unit * 0.08f), cfg.accent.color)
                        Spacer(Modifier.width(8.dp))
                        WText(d.min.digits(ad), (unit * 0.055f).sp(), color = Soft, family = cfg.numFont.family)
                        Canvas(Modifier.weight(1f).height(unit * 0.03f).padding(horizontal = 6.dp)) {
                            val y = size.height / 2
                            drawLine(Color.White.copy(alpha = 0.12f), Offset(0f, y), Offset(size.width, y), size.height, StrokeCap.Round)
                            val a = (d.min - lo).toFloat() / (hi - lo) * size.width
                            val b = (d.max - lo).toFloat() / (hi - lo) * size.width
                            drawLine(Brush.horizontalGradient(listOf(tempColor(d.min), tempColor(d.max)), a, b), Offset(a, y), Offset(b.coerceAtLeast(a + 1), y), size.height, StrokeCap.Round)
                        }
                        WText(d.max.digits(ad), (unit * 0.055f).sp(), family = cfg.numFont.family)
                    }
                }
            }
            WVariant.WEATHER_BARS -> {
                val items = w.hourly.take(if (wide) 8 else 5)
                val lo = items.minOf { it.second } - 2
                val hi = items.maxOf { it.second }.coerceAtLeast(lo + 3)
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                    items.forEach { (hour, temp, _) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Bottom) {
                            WText("${temp.digits(ad)}°", (unit * 0.055f).sp(), family = cfg.numFont.family)
                            val frac = 0.25f + 0.75f * (temp - lo).toFloat() / (hi - lo)
                            Box(
                                Modifier
                                    .padding(vertical = 4.dp)
                                    .width(unit * 0.07f)
                                    .fillMaxHeight(0.55f * frac)
                                    .clip(RoundedCornerShape(50))
                                    .background(Brush.verticalGradient(listOf(tempColor(temp), tempColor(temp).copy(alpha = 0.35f)))),
                            )
                            WText(hour.digits(ad), (unit * 0.05f).sp(), color = Soft)
                        }
                    }
                }
            }
            else -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WText("${w.temp.digits(ad)}°", (unit * 0.16f).sp(), family = cfg.numFont.family, weight = FontWeight.Light)
                    Spacer(Modifier.width(10.dp))
                    WText(WeatherStore.describe(w.code), (unit * 0.065f).sp(), color = Soft)
                    Spacer(Modifier.weight(1f))
                    WeatherGlyph(w.code, Modifier.size(unit * 0.12f), cfg.accent.color)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    w.hourly.take(if (wide) 6 else 4).forEach { (hour, temp, code) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            WText(hour.digits(ad), (unit * 0.05f).sp(), color = Soft)
                            WeatherGlyph(code, Modifier.size(unit * 0.08f).padding(vertical = 2.dp), Color.White)
                            WText("${temp.digits(ad)}°", (unit * 0.06f).sp(), family = cfg.numFont.family)
                        }
                    }
                }
            }
        }
    }
}

/** رموز طقس مرسومة بخطوط رفيعة (بدون إيموجي) */
@Composable
fun WeatherGlyph(code: Int, modifier: Modifier, accent: Color) {
    Canvas(modifier) {
        val s = size.minDimension
        val sw = s * 0.07f
        fun sun(c: Offset, r: Float) {
            drawCircle(accent, r, c, style = Stroke(sw))
            for (k in 0 until 8) {
                val a = Math.toRadians(k * 45.0)
                drawLine(accent, Offset(c.x + (r * 1.45f * cos(a)).toFloat(), c.y + (r * 1.45f * sin(a)).toFloat()), Offset(c.x + (r * 1.85f * cos(a)).toFloat(), c.y + (r * 1.85f * sin(a)).toFloat()), sw, StrokeCap.Round)
            }
        }
        fun cloud(color: Color) {
            val p = Path().apply {
                moveTo(s * 0.22f, s * 0.72f)
                cubicTo(s * 0.02f, s * 0.72f, s * 0.04f, s * 0.44f, s * 0.26f, s * 0.47f)
                cubicTo(s * 0.3f, s * 0.22f, s * 0.66f, s * 0.2f, s * 0.7f, s * 0.44f)
                cubicTo(s * 0.96f, s * 0.42f, s * 0.98f, s * 0.72f, s * 0.76f, s * 0.72f)
                close()
            }
            drawPath(p, color, style = Stroke(sw, cap = StrokeCap.Round))
        }
        when (code) {
            0 -> sun(center, s * 0.2f)
            1, 2 -> { sun(Offset(s * 0.62f, s * 0.32f), s * 0.13f); cloud(Color.White) }
            in 51..67, in 80..82 -> {
                cloud(Color.White)
                for (k in 0..2) drawLine(accent, Offset(s * (0.32f + k * 0.18f), s * 0.8f), Offset(s * (0.28f + k * 0.18f), s * 0.95f), sw, StrokeCap.Round)
            }
            in 71..77, 85, 86 -> { cloud(Color.White); for (k in 0..2) drawCircle(accent, sw * 0.8f, Offset(s * (0.3f + k * 0.2f), s * 0.88f)) }
            in 95..99 -> {
                cloud(Color.White)
                val b = Path().apply { moveTo(s * 0.52f, s * 0.7f); lineTo(s * 0.42f, s * 0.86f); lineTo(s * 0.54f, s * 0.86f); lineTo(s * 0.46f, s * 1f) }
                drawPath(b, accent, style = Stroke(sw, cap = StrokeCap.Round))
            }
            45, 48 -> for (k in 0..2) drawLine(Color.White, Offset(s * 0.15f, s * (0.38f + k * 0.15f)), Offset(s * 0.85f, s * (0.38f + k * 0.15f)), sw, StrokeCap.Round)
            else -> cloud(Color.White)
        }
    }
}

// ───────────── البطارية ─────────────

@Composable
fun BatteryWidget(cfg: WConfig) {
    val b = rememberBattery()
    val ad = cfg.arabicDigits
    val color = if (b.level <= 15 && !b.charging) Color(0xFFFF7A7A) else if (b.charging) Color(0xFF9FE3CB) else cfg.accent.color
    BoxWithConstraints(Modifier.fillMaxSize().padding(14.dp), contentAlignment = Alignment.Center) {
        val unit = min(maxWidth.value, maxHeight.value * 1.6f).dp
        val side = min(maxWidth.value, maxHeight.value).dp
        when (cfg.variant) {
            WVariant.BATTERY_RING -> Box(contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(side)) {
                    val sw = size.minDimension * 0.08f
                    drawCircle(Color.White.copy(alpha = 0.12f), size.minDimension / 2 - sw, style = Stroke(sw))
                    drawArc(color, -90f, 3.6f * b.level, false, Offset(sw, sw), Size(size.width - sw * 2, size.height - sw * 2), style = Stroke(sw, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    WText("${b.level.digits(ad)}٪", (unit * 0.17f).sp(), family = cfg.numFont.family, weight = FontWeight.Light)
                    WText(if (b.charging) "يشحن" else "البطارية", (unit * 0.06f).sp(), color = Soft)
                }
            }
            WVariant.BATTERY_BAR -> Column(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.Bottom) {
                    WText(if (b.charging) "يشحن" else "البطارية", (unit * 0.075f).sp(), color = Soft, align = TextAlign.Start, modifier = Modifier.weight(1f))
                    WText("${b.level.digits(ad)}٪", (unit * 0.12f).sp(), family = cfg.numFont.family)
                }
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().height(unit * 0.09f).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.12f))) {
                    Box(Modifier.fillMaxWidth(b.level / 100f).fillMaxHeight().clip(RoundedCornerShape(50)).background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.6f), color))))
                }
            }
            else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                WText("${b.level.digits(ad)}٪", (unit * 0.3f).sp(), family = cfg.numFont.family, weight = FontWeight.Light, color = color)
                WText(if (b.charging) "يشحن الحين" else "متبقي", (unit * 0.07f).sp(), color = Soft)
            }
        }
    }
}

// ───────────── التحكم ─────────────

private enum class Ctl(val title: String) { TORCH("فلاش"), SILENT("صامت"), WIFI("واي فاي"), BT("بلوتوث"), DND("لا تزعج"), SAVER("توفير") }

@Composable
fun ControlsWidget(cfg: WConfig) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }
    val camera = remember { context.getSystemService(CameraManager::class.java) }
    val torchId = remember {
        runCatching { camera.cameraIdList.firstOrNull { camera.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true } }.getOrNull()
    }
    var torchOn by remember { mutableStateOf(false) }
    DisposableEffect(torchId) {
        val cb = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                if (cameraId == torchId) torchOn = enabled
            }
        }
        runCatching { camera.registerTorchCallback(cb, null) }
        onDispose { runCatching { camera.unregisterTorchCallback(cb) } }
    }
    val audio = remember { context.getSystemService(AudioManager::class.java) }
    val notif = remember { context.getSystemService(NotificationManager::class.java) }
    val states = remember(refresh, torchOn) {
        mapOf(
            Ctl.TORCH to torchOn,
            Ctl.SILENT to (audio.ringerMode != AudioManager.RINGER_MODE_NORMAL),
            Ctl.WIFI to (runCatching { context.getSystemService(WifiManager::class.java).isWifiEnabled }.getOrNull() ?: false),
            Ctl.BT to (runCatching { context.getSystemService(BluetoothManager::class.java).adapter?.isEnabled }.getOrNull() ?: false),
            Ctl.DND to (notif.currentInterruptionFilter > NotificationManager.INTERRUPTION_FILTER_ALL),
            Ctl.SAVER to context.getSystemService(PowerManager::class.java).isPowerSaveMode,
        )
    }

    fun open(action: String) = runCatching { context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }

    fun toggle(c: Ctl) {
        when (c) {
            Ctl.TORCH -> torchId?.let { runCatching { camera.setTorchMode(it, !torchOn) } }
            Ctl.SILENT -> runCatching {
                audio.ringerMode = if (audio.ringerMode == AudioManager.RINGER_MODE_NORMAL) AudioManager.RINGER_MODE_VIBRATE else AudioManager.RINGER_MODE_NORMAL
            }
            Ctl.WIFI -> open(Settings.Panel.ACTION_WIFI)
            Ctl.BT -> open(Settings.ACTION_BLUETOOTH_SETTINGS)
            Ctl.DND -> if (notif.isNotificationPolicyAccessGranted) {
                notif.setInterruptionFilter(if (states[Ctl.DND] == true) NotificationManager.INTERRUPTION_FILTER_ALL else NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            } else {
                open(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            }
            Ctl.SAVER -> open(Settings.ACTION_BATTERY_SAVER_SETTINGS)
        }
        refresh++
    }

    BoxWithConstraints(Modifier.fillMaxSize().padding(12.dp)) {
        val wide = maxWidth > maxHeight * 1.4f
        val cols = if (wide) 6 else 3
        val rows = if (wide) 1 else 2
        val gap = 8.dp
        val tile = min(((maxWidth - gap * (cols - 1)) / cols).value, ((maxHeight - gap * (rows - 1)) / rows).value).dp
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(gap, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
            Ctl.entries.chunked(cols).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { c ->
                        val on = states[c] == true
                        Column(
                            Modifier.size(tile).clip(RoundedCornerShape(tile * 0.3f))
                                .background(if (on) cfg.accent.color else Color.White.copy(alpha = 0.12f))
                                .clickable { toggle(c) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            ControlGlyph(c, Modifier.size(tile * 0.4f), if (on) Color.Black else Color.White)
                            if (tile > 64.dp) WText(c.title, (tile * 0.13f).sp(), color = if (on) Color.Black else Soft, shadow = false)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlGlyph(c: Ctl, modifier: Modifier, color: Color) {
    Canvas(modifier) {
        val s = size.minDimension
        val sw = s * 0.09f
        val st = Stroke(sw, cap = StrokeCap.Round)
        when (c) {
            Ctl.TORCH -> {
                val p = Path().apply { moveTo(s * 0.28f, s * 0.12f); lineTo(s * 0.72f, s * 0.12f); lineTo(s * 0.62f, s * 0.4f); lineTo(s * 0.62f, s * 0.9f); lineTo(s * 0.38f, s * 0.9f); lineTo(s * 0.38f, s * 0.4f); close() }
                drawPath(p, color, style = st)
                drawLine(color, Offset(s * 0.5f, s * 0.52f), Offset(s * 0.5f, s * 0.64f), sw, StrokeCap.Round)
            }
            Ctl.SILENT -> {
                val p = Path().apply { moveTo(s * 0.2f, s * 0.72f); lineTo(s * 0.8f, s * 0.72f); cubicTo(s * 0.7f, s * 0.6f, s * 0.74f, s * 0.2f, s * 0.5f, s * 0.18f); cubicTo(s * 0.26f, s * 0.2f, s * 0.3f, s * 0.6f, s * 0.2f, s * 0.72f) }
                drawPath(p, color, style = st)
                drawLine(color, Offset(s * 0.42f, s * 0.86f), Offset(s * 0.58f, s * 0.86f), sw, StrokeCap.Round)
                drawLine(color, Offset(s * 0.15f, s * 0.12f), Offset(s * 0.85f, s * 0.88f), sw, StrokeCap.Round)
            }
            Ctl.WIFI -> {
                for (k in 1..3) {
                    val r = s * 0.14f * k + s * 0.04f
                    drawArc(color, 225f, 90f, false, Offset(s * 0.5f - r, s * 0.82f - r), Size(r * 2, r * 2), style = st)
                }
                drawCircle(color, sw * 0.8f, Offset(s * 0.5f, s * 0.82f))
            }
            Ctl.BT -> {
                val p = Path().apply { moveTo(s * 0.3f, s * 0.32f); lineTo(s * 0.68f, s * 0.66f); lineTo(s * 0.5f, s * 0.84f); lineTo(s * 0.5f, s * 0.16f); lineTo(s * 0.68f, s * 0.34f); lineTo(s * 0.3f, s * 0.68f) }
                drawPath(p, color, style = st)
            }
            Ctl.DND -> {
                val a = Path().apply { addOval(androidx.compose.ui.geometry.Rect(s * 0.15f, s * 0.15f, s * 0.85f, s * 0.85f)) }
                val b = Path().apply { addOval(androidx.compose.ui.geometry.Rect(s * 0.38f, s * 0.05f, s * 1.0f, s * 0.67f)) }
                drawPath(Path.combine(PathOperation.Difference, a, b), color)
            }
            Ctl.SAVER -> {
                drawRoundRect(color, Offset(s * 0.3f, s * 0.2f), Size(s * 0.4f, s * 0.7f), androidx.compose.ui.geometry.CornerRadius(s * 0.08f), style = Stroke(sw))
                drawLine(color, Offset(s * 0.42f, s * 0.12f), Offset(s * 0.58f, s * 0.12f), sw, StrokeCap.Round)
                drawLine(color, Offset(s * 0.5f, s * 0.42f), Offset(s * 0.5f, s * 0.68f), sw, StrokeCap.Round)
                drawLine(color, Offset(s * 0.37f, s * 0.55f), Offset(s * 0.63f, s * 0.55f), sw, StrokeCap.Round)
            }
        }
    }
}

// ───────────── كتابتك ─────────────

@Composable
fun TextWidget(cfg: WConfig) {
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp), contentAlignment = Alignment.Center) {
        val maxW = constraints.maxWidth
        val maxH = constraints.maxHeight
        val scale = listOf(0.75f, 1f, 1.25f)[cfg.textSize.coerceIn(0, 2)]
        val align = listOf(TextAlign.Start, TextAlign.Center, TextAlign.End)[cfg.textAlign.coerceIn(0, 2)]
        val text = cfg.text.ifBlank { "اكتب جملتك" }
        // نكبر الخط لين يعبي المكان بدون فراغ
        val fontSize = remember(text, maxW, maxH, cfg.textFont, scale) {
            var fs = 44f * scale
            while (fs > 9f) {
                val r = measurer.measure(
                    text,
                    TextStyle(fontSize = fs.sp, fontFamily = cfg.textFont.family, fontWeight = FontWeight.SemiBold),
                    constraints = Constraints(maxWidth = maxW),
                )
                if (r.size.height <= maxH * 0.92f) break
                fs -= 1f
            }
            fs
        }
        WText(
            text, fontSize.sp, family = cfg.textFont.family, weight = FontWeight.SemiBold,
            color = if (cfg.accent == Accent.WHITE) Color.White else cfg.accent.color,
            align = align, maxLines = 6, modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Suppress("unused")
private fun unusedAbs() = abs(0) + sin(0.0).toInt()

@Suppress("unused")
private fun DrawScope.unusedDraw() = Unit

@Suppress("unused")
private fun Context.unusedCtx() = Unit
