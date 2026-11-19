package com.waleed.launcher.trial.widgets

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject

/** أنواع الويدجت */
enum class WType(val title: String) {
    CLOCK("الساعات"),
    DATE("التاريخ"),
    PRAYER("الصلاة"),
    WEATHER("الطقس"),
    BATTERY("البطارية"),
    CONTROLS("التحكم"),
    TEXT("كتابتك"),
}

/** كل شكل من أشكال الويدجت */
enum class WVariant(val type: WType, val title: String) {
    CLOCK_THIN(WType.CLOCK, "رقمية رفيعة"),
    CLOCK_BOLD(WType.CLOCK, "رقمية عريضة"),
    CLOCK_NOMOS(WType.CLOCK, "عقارب كلاسيك"),
    CLOCK_MINIMAL(WType.CLOCK, "عقارب بسيطة"),
    CLOCK_FLIP(WType.CLOCK, "قلابة"),
    CLOCK_WORDS(WType.CLOCK, "بالكلام"),
    CLOCK_HALF(WType.CLOCK, "مقصوصة"),
    CLOCK_ROMAN(WType.CLOCK, "رومانية"),
    CLOCK_WALEED(WType.CLOCK, "سوداء وذهب"),
    CLOCK_CHRONO(WType.CLOCK, "كرونوغراف"),
    CLOCK_EMERALD(WType.CLOCK, "زمردية"),
    CLOCK_LEATHER(WType.CLOCK, "جلد مخيّط"),
    CLOCK_MARBLE(WType.CLOCK, "رخام وذهب"),

    DATE_DAY(WType.DATE, "اليوم"),
    DATE_DUAL(WType.DATE, "هجري وميلادي"),
    DATE_MONTH(WType.DATE, "تقويم الشهر"),
    DATE_RAMADAN(WType.DATE, "باقي على رمضان"),
    DATE_WEEK(WType.DATE, "شريط الأسبوع"),

    PRAYER_LIST(WType.PRAYER, "كل المواقيت"),
    PRAYER_RING(WType.PRAYER, "حلقة الصلاة"),
    PRAYER_COUNTDOWN(WType.PRAYER, "عد تنازلي"),

    WEATHER_BIG(WType.WEATHER, "حرارة كبيرة"),
    WEATHER_HOURLY(WType.WEATHER, "ساعة بساعة"),
    WEATHER_DAYS(WType.WEATHER, "الأيام الجاية"),
    WEATHER_BARS(WType.WEATHER, "أعمدة ملونة"),

    BATTERY_RING(WType.BATTERY, "حلقة"),
    BATTERY_BIG(WType.BATTERY, "رقم كبير"),
    BATTERY_BAR(WType.BATTERY, "شريط"),

    CONTROLS_GRID(WType.CONTROLS, "أزرار التحكم"),

    TEXT_QUOTE(WType.TEXT, "جملتك"),
    ;

    val isAnalog: Boolean
        get() = this in setOf(CLOCK_NOMOS, CLOCK_MINIMAL, CLOCK_ROMAN, CLOCK_WALEED, CLOCK_CHRONO, CLOCK_EMERALD, CLOCK_LEATHER, CLOCK_MARBLE)
}

/** خلفية الويدجت */
enum class Glass(val title: String) {
    DARK("زجاج داكن"),
    CLEAR("زجاج شفاف"),
    BLEND("متلائم مع الخلفية"),
}

/** الألوان المميزة المتفق عليها */
enum class Accent(val color: Color, val title: String) {
    WHITE(Color(0xFFEDEDED), "أبيض"),
    GOLD(Color(0xFFE3C285), "ذهبي"),
    SILVER(Color(0xFF9DB0FF), "أزرق فضي"),
    MINT(Color(0xFF9FE3CB), "نعناعي"),
    PINK(Color(0xFFF49BC6), "وردي"),
    ORANGE(Color(0xFFF7A867), "برتقالي"),
    RED(Color(0xFFFF7A7A), "أحمر ليلي"),
}

/** عقرب الثواني */
enum class Seconds(val title: String) {
    TICK("نبضة"),
    SWEEP("انسيابي"),
    OFF("بدون"),
}

/** إعدادات ويدجت واحد */
data class WConfig(
    val variant: WVariant,
    val glass: Glass = Glass.DARK,
    val arabicDigits: Boolean = false,
    val numFont: NumFont = NumFont.GROTESK,
    val accent: Accent = Accent.GOLD,
    val glow: Boolean = true,
    val seconds: Seconds = Seconds.TICK,
    val text: String = "The only way to do great work is to love what you do",
    val textFont: NumFont = NumFont.PLAYFAIR,
    val textSize: Int = 1,
    val textAlign: Int = 1,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("v", variant.name).put("g", glass.name).put("ad", arabicDigits).put("nf", numFont.name)
        .put("ac", accent.name).put("gl", glow).put("s", seconds.name).put("t", text)
        .put("tf", textFont.name).put("ts", textSize).put("ta", textAlign)

    companion object {
        fun fromJson(o: JSONObject): WConfig? = runCatching {
            WConfig(
                variant = WVariant.valueOf(o.getString("v")),
                glass = Glass.valueOf(o.optString("g", "DARK")),
                arabicDigits = o.optBoolean("ad", false),
                numFont = NumFont.valueOf(o.optString("nf", "GROTESK")),
                accent = Accent.valueOf(o.optString("ac", "GOLD")),
                glow = o.optBoolean("gl", true),
                seconds = Seconds.valueOf(o.optString("s", "TICK")),
                text = o.optString("t", ""),
                textFont = NumFont.valueOf(o.optString("tf", "PLAYFAIR")),
                textSize = o.optInt("ts", 1),
                textAlign = o.optInt("ta", 1),
            )
        }.getOrNull()
    }
}

/** خانة ويدجت: فيها مكدس (كذا ويدجت فوق بعض) */
data class Slot(val items: List<WConfig>, val index: Int = 0) {
    val current: WConfig get() = items[index.coerceIn(0, items.size - 1)]
}

/** يحفظ ويدجت كل شاشة لحالها (الخارجية والداخلية) */
class WidgetStore(context: Context) {
    private val prefs = context.getSharedPreferences("widgets", Context.MODE_PRIVATE)

    var outer by mutableStateOf(load("outer") ?: defaultOuter())
        private set
    var inner by mutableStateOf(load("inner") ?: defaultInner())
        private set

    fun slots(wide: Boolean): List<Slot> = if (wide) inner else outer

    fun update(wide: Boolean, slotIndex: Int, slot: Slot) {
        val list = slots(wide).toMutableList()
        if (slotIndex !in list.indices) return
        list[slotIndex] = slot
        if (wide) inner = list else outer = list
        save(if (wide) "inner" else "outer", list)
    }

    private fun load(key: String): List<Slot>? = runCatching {
        val arr = JSONArray(prefs.getString(key, null) ?: return null)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val items = o.getJSONArray("items").let { a -> (0 until a.length()).mapNotNull { WConfig.fromJson(a.getJSONObject(it)) } }
            Slot(items.ifEmpty { listOf(WConfig(WVariant.CLOCK_NOMOS)) }, o.optInt("index", 0))
        }
    }.getOrNull()

    private fun save(key: String, slots: List<Slot>) {
        val arr = JSONArray()
        slots.forEach { s ->
            arr.put(JSONObject().put("index", s.index).put("items", JSONArray().apply { s.items.forEach { put(it.toJson()) } }))
        }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    private fun defaultOuter() = listOf(
        Slot(listOf(WConfig(WVariant.TEXT_QUOTE), WConfig(WVariant.WEATHER_HOURLY), WConfig(WVariant.PRAYER_LIST))),
        Slot(listOf(WConfig(WVariant.DATE_DAY), WConfig(WVariant.DATE_DUAL), WConfig(WVariant.BATTERY_RING))),
        Slot(listOf(WConfig(WVariant.CLOCK_NOMOS), WConfig(WVariant.CLOCK_WALEED), WConfig(WVariant.PRAYER_RING))),
    )

    private fun defaultInner() = listOf(
        Slot(listOf(WConfig(WVariant.TEXT_QUOTE), WConfig(WVariant.DATE_MONTH), WConfig(WVariant.WEATHER_DAYS))),
        Slot(listOf(WConfig(WVariant.CLOCK_NOMOS), WConfig(WVariant.DATE_DAY), WConfig(WVariant.CONTROLS_GRID))),
    )
}

/** أرقام عربية (١٢٣) أو إنجليزية (123) */
fun String.digits(arabic: Boolean): String =
    if (!arabic) this else map { if (it in '0'..'9') '٠' + (it - '0') else it }.joinToString("")

fun Int.digits(arabic: Boolean): String = toString().digits(arabic)
