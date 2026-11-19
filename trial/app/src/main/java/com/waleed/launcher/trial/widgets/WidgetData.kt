package com.waleed.launcher.trial.widgets

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.BatteryManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.util.Date

// ───────────── الوقت ─────────────

/** الوقت الحالي: يتحدث كل ثانية بس لو فيه عقرب ثواني، وإلا كل دقيقة (توفير بطارية) */
val LocalNow = androidx.compose.runtime.compositionLocalOf { System.currentTimeMillis() }

@Composable
fun rememberNow(everySecond: Boolean): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, everySecond) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                now = System.currentTimeMillis()
                val step = if (everySecond) 1000L else 60_000L
                delay(step - now % step + 5)
            }
        }
    }
    return now
}

fun Long.toLocal(): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())

// ───────────── التقويم ─────────────

object ArabicCalendar {
    val hijriMonths = listOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
        "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة",
    )
    val gregMonths = listOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر",
    )

    fun dayName(d: DayOfWeek): String = when (d) {
        DayOfWeek.SUNDAY -> "الأحد"
        DayOfWeek.MONDAY -> "الإثنين"
        DayOfWeek.TUESDAY -> "الثلاثاء"
        DayOfWeek.WEDNESDAY -> "الأربعاء"
        DayOfWeek.THURSDAY -> "الخميس"
        DayOfWeek.FRIDAY -> "الجمعة"
        DayOfWeek.SATURDAY -> "السبت"
    }

    fun dayLetter(d: DayOfWeek): String = when (d) {
        DayOfWeek.SUNDAY -> "ح"
        DayOfWeek.MONDAY -> "ن"
        DayOfWeek.TUESDAY -> "ث"
        DayOfWeek.WEDNESDAY -> "ر"
        DayOfWeek.THURSDAY -> "خ"
        DayOfWeek.FRIDAY -> "ج"
        DayOfWeek.SATURDAY -> "س"
    }

    /** التاريخ الهجري بتقويم أم القرى (مدمج بأندرويد) */
    data class Hijri(val day: Int, val month: Int, val year: Int) {
        val monthName get() = hijriMonths[month - 1]
    }

    fun hijri(date: LocalDate): Hijri {
        val h = HijrahDate.from(date)
        return Hijri(h.get(ChronoField.DAY_OF_MONTH), h.get(ChronoField.MONTH_OF_YEAR), h.get(ChronoField.YEAR))
    }

    /** كم يوم باقي على أول رمضان */
    fun daysToRamadan(today: LocalDate): Pair<Long, LocalDate> {
        val h = hijri(today)
        var target = LocalDate.from(HijrahDate.of(h.year, 9, 1))
        if (!target.isAfter(today)) target = LocalDate.from(HijrahDate.of(h.year + 1, 9, 1))
        return ChronoUnit.DAYS.between(today, target) to target
    }

    /** الوقت بالكلام: "الثامنة وإحدى وأربعون" */
    fun timeInWords(hour24: Int, minute: Int): Pair<String, String> {
        val hours = listOf(
            "الثانية عشرة", "الواحدة", "الثانية", "الثالثة", "الرابعة", "الخامسة", "السادسة",
            "السابعة", "الثامنة", "التاسعة", "العاشرة", "الحادية عشرة",
        )
        val h = hours[hour24 % 12]
        val m = when (minute) {
            0 -> "تماماً"
            15 -> "والربع"
            30 -> "والنصف"
            else -> "و" + number(minute)
        }
        return h to m
    }

    private fun number(n: Int): String {
        val ones = listOf("", "واحدة", "اثنتان", "ثلاث", "أربع", "خمس", "ست", "سبع", "ثمان", "تسع")
        val teens = listOf(
            "عشر", "إحدى عشرة", "اثنتا عشرة", "ثلاث عشرة", "أربع عشرة", "خمس عشرة",
            "ست عشرة", "سبع عشرة", "ثماني عشرة", "تسع عشرة",
        )
        val tens = listOf("", "", "عشرون", "ثلاثون", "أربعون", "خمسون")
        return when {
            n < 10 -> ones[n]
            n < 20 -> teens[n - 10]
            n % 10 == 0 -> tens[n / 10]
            else -> (if (n % 10 == 1) "إحدى" else ones[n % 10]) + " و" + tens[n / 10]
        }
    }
}

// ───────────── الموقع ─────────────

class LocationStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("location", Context.MODE_PRIVATE)

    var coords by mutableStateOf(
        if (prefs.contains("lat")) prefs.getFloat("lat", 0f).toDouble() to prefs.getFloat("lon", 0f).toDouble() else null,
    )
        private set

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** ناخذ الموقع مرة، ونحفظه. ما نتابع الموقع طول الوقت (توفير بطارية) */
    @SuppressLint("MissingPermission")
    fun refresh() {
        if (!hasPermission()) return
        val lm = context.getSystemService(LocationManager::class.java) ?: return
        val last = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER, LocationManager.GPS_PROVIDER)
            .mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
            .maxByOrNull { it.time }
        if (last != null) {
            save(last)
        } else {
            runCatching {
                lm.getCurrentLocation(LocationManager.NETWORK_PROVIDER, null, context.mainExecutor) { loc: Location? ->
                    loc?.let { save(it) }
                }
            }
        }
    }

    private fun save(l: Location) {
        coords = l.latitude to l.longitude
        prefs.edit().putFloat("lat", l.latitude.toFloat()).putFloat("lon", l.longitude.toFloat()).apply()
    }
}

// ───────────── الصلاة ─────────────

object PrayerCalc {
    val names = listOf("الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء")

    data class Day(val times: List<Long>)

    /** مواقيت اليوم بطريقة أم القرى. برمضان العشاء بعد المغرب بساعتين */
    fun day(lat: Double, lon: Double, date: LocalDate): Day {
        val params = CalculationMethod.UMM_AL_QURA.parameters
        val ramadan = ArabicCalendar.hijri(date).month == 9
        if (ramadan) params.ishaInterval = 120
        val d = Date.from(date.atStartOfDay(ZoneId.systemDefault()).plusHours(12).toInstant())
        val pt = PrayerTimes(Coordinates(lat, lon), DateComponents.from(d), params)
        return Day(listOf(pt.fajr, pt.sunrise, pt.dhuhr, pt.asr, pt.maghrib, pt.isha).map { it.time })
    }

    /** الصلاة الجاية (بدون الشروق) */
    data class Next(val index: Int, val time: Long, val previous: Long)

    fun next(lat: Double, lon: Double, now: Long): Next {
        val today = now.toLocal().toLocalDate()
        val t = day(lat, lon, today).times
        val prayerIdx = listOf(0, 2, 3, 4, 5)
        for ((k, i) in prayerIdx.withIndex()) {
            if (t[i] > now) {
                val prev = if (k == 0) day(lat, lon, today.minusDays(1)).times[5] else t[prayerIdx[k - 1]]
                return Next(i, t[i], prev)
            }
        }
        val tomorrow = day(lat, lon, today.plusDays(1)).times
        return Next(0, tomorrow[0], t[5])
    }
}

// ───────────── الطقس (Open-Meteo: مجاني بدون مفتاح) ─────────────

data class Weather(
    val temp: Int,
    val code: Int,
    val hourly: List<Triple<Int, Int, Int>>, // ساعة، حرارة، حالة
    val daily: List<Daily>,
    val fetchedAt: Long,
)

data class Daily(val date: LocalDate, val min: Int, val max: Int, val code: Int)

class WeatherStore(context: Context) {
    private val prefs = context.getSharedPreferences("weather", Context.MODE_PRIVATE)
    var data by mutableStateOf(prefs.getString("json", null)?.let { runCatching { parse(JSONObject(it), prefs.getLong("at", 0)) }.getOrNull() })
        private set

    /** نحدث كل ٣٠ دقيقة بالكثير */
    suspend fun refreshIfNeeded(lat: Double, lon: Double) {
        val d = data
        if (d != null && System.currentTimeMillis() - d.fetchedAt < 30 * 60_000) return
        val json = withContext(Dispatchers.IO) {
            runCatching {
                val url = URL(
                    "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                        "&current=temperature_2m,weather_code&hourly=temperature_2m,weather_code" +
                        "&daily=temperature_2m_max,temperature_2m_min,weather_code&timezone=auto&forecast_days=6",
                )
                val c = url.openConnection() as HttpURLConnection
                c.connectTimeout = 8000
                c.readTimeout = 8000
                c.inputStream.bufferedReader().use { it.readText() }.also { c.disconnect() }
            }.getOrNull()
        } ?: return
        val now = System.currentTimeMillis()
        runCatching { parse(JSONObject(json), now) }.getOrNull()?.let {
            data = it
            prefs.edit().putString("json", json).putLong("at", now).apply()
        }
    }

    private fun parse(o: JSONObject, at: Long): Weather {
        val cur = o.getJSONObject("current")
        val h = o.getJSONObject("hourly")
        val ht = h.getJSONArray("time")
        val htemp = h.getJSONArray("temperature_2m")
        val hcode = h.getJSONArray("weather_code")
        val nowHour = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0)
        val hourly = mutableListOf<Triple<Int, Int, Int>>()
        for (i in 0 until ht.length()) {
            val t = LocalDateTime.parse(ht.getString(i))
            if (!t.isBefore(nowHour) && hourly.size < 8) {
                hourly += Triple(t.hour, Math.round(htemp.getDouble(i)).toInt(), hcode.getInt(i))
            }
        }
        val d = o.getJSONObject("daily")
        val dt = d.getJSONArray("time")
        val daily = (0 until dt.length()).map { i ->
            Daily(
                LocalDate.parse(dt.getString(i)),
                Math.round(d.getJSONArray("temperature_2m_min").getDouble(i)).toInt(),
                Math.round(d.getJSONArray("temperature_2m_max").getDouble(i)).toInt(),
                d.getJSONArray("weather_code").getInt(i),
            )
        }
        return Weather(Math.round(cur.getDouble("temperature_2m")).toInt(), cur.getInt("weather_code"), hourly, daily, at)
    }

    companion object {
        fun describe(code: Int): String = when (code) {
            0 -> "صافي"
            1, 2 -> "غائم جزئياً"
            3 -> "غائم"
            45, 48 -> "ضباب"
            in 51..67, in 80..82 -> "ممطر"
            in 71..77, 85, 86 -> "ثلج"
            in 95..99 -> "عاصفة رعدية"
            else -> "—"
        }
    }
}

// ───────────── البطارية ─────────────

data class BatteryState(val level: Int, val charging: Boolean)

@Composable
fun rememberBattery(): BatteryState {
    val context = LocalContext.current
    var state by remember { mutableStateOf(BatteryState(0, false)) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
                val status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                state = BatteryState(
                    level * 100 / scale,
                    status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL,
                )
            }
        }
        ContextCompat.registerReceiver(context, receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
            ?.let { receiver.onReceive(context, it) }
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    return state
}
