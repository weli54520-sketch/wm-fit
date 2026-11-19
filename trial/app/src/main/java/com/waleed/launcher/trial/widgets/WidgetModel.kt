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
    SKY("الشمس والقمر"),
    DATE("التاريخ"),
    PRAYER("الصلاة"),
    COUNT("عدادات"),
    WEATHER("الطقس"),
    BATTERY("البطارية"),
    CONTROLS("التحكم"),
    TEXT("كتابتك"),
}

/** كل شكل من أشكال الويدجت */
enum class WVariant(val type: WType, val title: String) {
    // ساعات بعقارب (كلها تتعدل بالكامل: المينا، العقارب، المعدن، الإطار، الإضافات)
    CLOCK_BLUE(WType.CLOCK, "أزرق مشمس"),
    CLOCK_NOMOS(WType.CLOCK, "باوهاوس"),
    CLOCK_WALEED(WType.CLOCK, "سوداء وذهب"),
    CLOCK_PEARL(WType.CLOCK, "صدف"),
    CLOCK_EMERALD(WType.CLOCK, "زمردية"),
    CLOCK_SALMON(WType.CLOCK, "سلموني"),
    CLOCK_ROMAN(WType.CLOCK, "ساعة جيب"),
    CLOCK_CHRONO(WType.CLOCK, "كرونوغراف"),
    CLOCK_CARBON(WType.CLOCK, "كربون"),
    CLOCK_MARBLE(WType.CLOCK, "رخام وذهب"),
    CLOCK_LEATHER(WType.CLOCK, "جلد مخيّط"),
    CLOCK_MINIMAL(WType.CLOCK, "زجاج بسيط"),

    // ساعات رقمية وجديدة
    CLOCK_OVERSIZE(WType.CLOCK, "أرقام ضخمة"),
    CLOCK_RINGS(WType.CLOCK, "حلقات الوقت"),
    CLOCK_THIN(WType.CLOCK, "رقمية رفيعة"),
    CLOCK_BOLD(WType.CLOCK, "رقمية عريضة"),
    CLOCK_FLIP(WType.CLOCK, "قلابة"),
    CLOCK_WORDS(WType.CLOCK, "بالكلام"),
    CLOCK_HALF(WType.CLOCK, "مقصوصة"),

    SKY_SUN(WType.SKY, "قوس الشمس"),
    SKY_DAYLINE(WType.SKY, "يومك بسطر"),
    SKY_MOON(WType.SKY, "القمر والهجري"),

    DATE_DAY(WType.DATE, "اليوم"),
    DATE_DUAL(WType.DATE, "هجري وميلادي"),
    DATE_MONTH(WType.DATE, "تقويم الشهر"),
    DATE_WEEK(WType.DATE, "شريط الأسبوع"),

    PRAYER_LIST(WType.PRAYER, "كل المواقيت"),
    PRAYER_RING(WType.PRAYER, "حلقة الصلاة"),
    PRAYER_COUNTDOWN(WType.PRAYER, "عد تنازلي"),

    COUNT_SALARY(WType.COUNT, "باقي على الراتب"),
    COUNT_RAMADAN(WType.COUNT, "باقي على رمضان"),
    COUNT_EID(WType.COUNT, "باقي على العيد"),
    COUNT_CUSTOM(WType.COUNT, "مناسبتك"),

    WEATHER_SKY(WType.WEATHER, "سماء اليوم"),
    WEATHER_BIG(WType.WEATHER, "حرارة كبيرة"),
    WEATHER_HOURLY(WType.WEATHER, "ساعة بساعة"),
    WEATHER_DAYS(WType.WEATHER, "الأيام الجاية"),
    WEATHER_BARS(WType.WEATHER, "أعمدة ملونة"),

    BATTERY_LIQUID(WType.BATTERY, "بطارية سائلة"),
    BATTERY_RING(WType.BATTERY, "حلقة"),
    BATTERY_BIG(WType.BATTERY, "رقم كبير"),
    BATTERY_BAR(WType.BATTERY, "شريط"),

    CONTROLS_GRID(WType.CONTROLS, "أزرار التحكم"),

    TEXT_QUOTE(WType.TEXT, "جملتك"),
    ;

    val isAnalog: Boolean get() = name in ANALOG
}

private val ANALOG = setOf(
    "CLOCK_BLUE", "CLOCK_NOMOS", "CLOCK_WALEED", "CLOCK_PEARL", "CLOCK_EMERALD", "CLOCK_SALMON",
    "CLOCK_ROMAN", "CLOCK_CHRONO", "CLOCK_CARBON", "CLOCK_MARBLE", "CLOCK_LEATHER", "CLOCK_MINIMAL",
)

/** خلفية الويدجت */
enum class Glass(val title: String) {
    LIQUID("زجاج سائل"),
    LIQUID_TINT("زجاج ملوّن"),
    DARK("زجاج داكن"),
    BLEND("بدون خلفية"),
    BLACK("أسود"),
    SUNSET("غروب"),
    AURORA("شفق"),
    NIGHT("ليل أزرق"),
    GOLD("ذهبي"),
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

enum class Seconds(val title: String) { TICK("نبضة"), SWEEP("انسيابي"), OFF("بدون") }

// ───────────── خيارات الساعة الكلاسيكية ─────────────

enum class DialFace(val title: String, val swatch: Color) {
    GLASS("زجاج", Color(0x55FFFFFF)),
    BLACK("أسود", Color(0xFF121214)),
    WHITE("أبيض أوبالين", Color(0xFFEDEBE6)),
    BLUE_SUN("أزرق مشمس", Color(0xFF16336B)),
    GREEN_SUN("أخضر مشمس", Color(0xFF1F4A3A)),
    BURGUNDY_SUN("عنابي مشمس", Color(0xFF5A1626)),
    SALMON("سلموني", Color(0xFFE9A88E)),
    CHAMPAGNE("شمبانيا", Color(0xFFD9C29A)),
    PEARL("صدف", Color(0xFFEDE7F2)),
    EMERALD("زمرد", Color(0xFF0E6B50)),
    MARBLE("رخام", Color(0xFFE8E6E1)),
    LEATHER("جلد", Color(0xFF6E4127)),
    CARBON("كربون", Color(0xFF2A2B2E)),
}

enum class Indices(val title: String) {
    BARS("عصي"), DOTS("نقاط"), NUMERALS("أرقام"), ROMAN("روماني"), QUARTERS("أرباع"), BREGUET("بريغيه"), NONE("بدون"),
}

enum class Hands(val title: String) {
    DAUPHINE("دوفين"), SWORD("سيف"), BATON("عصا"), LEAF("ورقة"), BREGUET("بريغيه"), SKELETON("مفرغة"),
}

enum class Metal(val title: String, val light: Color, val dark: Color) {
    GOLD("ذهب", Color(0xFFF3DDA6), Color(0xFFB98D45)),
    ROSE("ذهب وردي", Color(0xFFF4C7B4), Color(0xFFB9765E)),
    SILVER("فضة", Color(0xFFF4F5F7), Color(0xFF9EA3AB)),
    BLUED("فولاذ أزرق", Color(0xFF5B7FD6), Color(0xFF1C2F73)),
    BLACK("أسود", Color(0xFF3A3A3E), Color(0xFF0C0C0E)),
    WHITE("أبيض", Color(0xFFFFFFFF), Color(0xFFD9D9D9)),
}

enum class Bezel(val title: String) { NONE("بدون"), THIN("رفيع"), FLUTED("محزز"), RAILWAY("مسار دقائق") }

enum class Comp(val title: String) {
    NONE("بدون"), DATE("التاريخ"), DAY_DATE("اليوم والتاريخ"), HIJRI("هجري"), MOON("القمر"), SUBSEC("ثواني صغيرة"),
}

/** مقاسات الويدجت (عرض × طول بالخانات) */
enum class WSize(val w: Int, val h: Int, val title: String) {
    S(2, 2, "مربع صغير"),
    WIDE(4, 2, "مستطيل"),
    L(4, 4, "مربع كبير"),
    TALL(2, 4, "طولي"),
    STRIP(4, 1, "شريط عرضي"),
    PILLAR(1, 3, "عمود رفيع"),
}

/** إعدادات ويدجت واحد */
data class WConfig(
    val variant: WVariant,
    val glass: Glass = Glass.LIQUID,
    val arabicDigits: Boolean = false,
    val numFont: NumFont = NumFont.GROTESK,
    val accent: Accent = Accent.GOLD,
    val glow: Boolean = true,
    val seconds: Seconds = Seconds.TICK,
    val text: String = "اكتب جملتك هنا",
    val textFont: NumFont = NumFont.PLAYFAIR,
    val textSize: Int = 1,
    val textAlign: Int = 1,
    // الساعة الكلاسيكية
    val face: DialFace = DialFace.BLUE_SUN,
    val indices: Indices = Indices.BARS,
    val hands: Hands = Hands.DAUPHINE,
    val metal: Metal = Metal.SILVER,
    val bezel: Bezel = Bezel.THIN,
    val comp: Comp = Comp.DATE,
    val dialText: String = "",
    // الرقمية
    val showDate: Boolean = true,
    // العدادات
    val countTitle: String = "",
    val countDay: Long = 0L,
    val salaryDay: Int = 27,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("v", variant.name).put("g", glass.name).put("ad", arabicDigits).put("nf", numFont.name)
        .put("ac", accent.name).put("gl", glow).put("s", seconds.name).put("t", text)
        .put("tf", textFont.name).put("ts", textSize).put("ta", textAlign)
        .put("fa", face.name).put("in", indices.name).put("ha", hands.name).put("me", metal.name)
        .put("be", bezel.name).put("co", comp.name).put("dt", dialText).put("sd", showDate)
        .put("ct", countTitle).put("cd", countDay).put("sal", salaryDay)

    companion object {
        private inline fun <reified E : Enum<E>> e(o: JSONObject, k: String, d: E): E =
            runCatching { enumValueOf<E>(o.getString(k)) }.getOrDefault(d)

        fun fromJson(o: JSONObject): WConfig? = runCatching {
            val d = WConfig(WVariant.valueOf(o.getString("v")))
            d.copy(
                glass = e(o, "g", d.glass), arabicDigits = o.optBoolean("ad"), numFont = e(o, "nf", d.numFont),
                accent = e(o, "ac", d.accent), glow = o.optBoolean("gl", true), seconds = e(o, "s", d.seconds),
                text = o.optString("t", d.text), textFont = e(o, "tf", d.textFont), textSize = o.optInt("ts", 1),
                textAlign = o.optInt("ta", 1), face = e(o, "fa", d.face), indices = e(o, "in", d.indices),
                hands = e(o, "ha", d.hands), metal = e(o, "me", d.metal), bezel = e(o, "be", d.bezel),
                comp = e(o, "co", d.comp), dialText = o.optString("dt", ""), showDate = o.optBoolean("sd", true),
                countTitle = o.optString("ct", ""), countDay = o.optLong("cd", 0L), salaryDay = o.optInt("sal", 27),
            )
        }.getOrNull()

        /** قوالب جاهزة للساعات الكلاسيكية: تختارها وبعدين تعدل كل شي */
        fun preset(v: WVariant, base: WConfig): WConfig {
            val c = base.copy(variant = v)
            return when (v) {
                WVariant.CLOCK_BLUE -> c.copy(face = DialFace.BLUE_SUN, indices = Indices.BARS, hands = Hands.DAUPHINE, metal = Metal.SILVER, bezel = Bezel.THIN, comp = Comp.DATE, dialText = "", accent = Accent.SILVER)
                WVariant.CLOCK_NOMOS -> c.copy(face = DialFace.WHITE, indices = Indices.NUMERALS, hands = Hands.SWORD, metal = Metal.BLUED, bezel = Bezel.RAILWAY, comp = Comp.SUBSEC, dialText = "", accent = Accent.ORANGE)
                WVariant.CLOCK_WALEED -> c.copy(face = DialFace.BLACK, indices = Indices.BARS, hands = Hands.DAUPHINE, metal = Metal.GOLD, bezel = Bezel.THIN, comp = Comp.HIJRI, dialText = "وليد", accent = Accent.GOLD)
                WVariant.CLOCK_PEARL -> c.copy(face = DialFace.PEARL, indices = Indices.DOTS, hands = Hands.LEAF, metal = Metal.ROSE, bezel = Bezel.THIN, comp = Comp.MOON, dialText = "", accent = Accent.PINK)
                WVariant.CLOCK_EMERALD -> c.copy(face = DialFace.EMERALD, indices = Indices.NUMERALS, hands = Hands.DAUPHINE, metal = Metal.GOLD, bezel = Bezel.FLUTED, comp = Comp.DAY_DATE, dialText = "", accent = Accent.GOLD, numFont = NumFont.PLAYFAIR)
                WVariant.CLOCK_SALMON -> c.copy(face = DialFace.SALMON, indices = Indices.BREGUET, hands = Hands.BREGUET, metal = Metal.BLUED, bezel = Bezel.RAILWAY, comp = Comp.SUBSEC, dialText = "", accent = Accent.SILVER)
                WVariant.CLOCK_ROMAN -> c.copy(face = DialFace.WHITE, indices = Indices.ROMAN, hands = Hands.BREGUET, metal = Metal.BLUED, bezel = Bezel.RAILWAY, comp = Comp.SUBSEC, dialText = "", accent = Accent.SILVER)
                WVariant.CLOCK_CHRONO -> c.copy(face = DialFace.BLACK, indices = Indices.BARS, hands = Hands.BATON, metal = Metal.WHITE, bezel = Bezel.RAILWAY, comp = Comp.SUBSEC, dialText = "", accent = Accent.ORANGE)
                WVariant.CLOCK_CARBON -> c.copy(face = DialFace.CARBON, indices = Indices.BARS, hands = Hands.SWORD, metal = Metal.SILVER, bezel = Bezel.FLUTED, comp = Comp.DATE, dialText = "", accent = Accent.RED)
                WVariant.CLOCK_MARBLE -> c.copy(face = DialFace.MARBLE, indices = Indices.ROMAN, hands = Hands.LEAF, metal = Metal.GOLD, bezel = Bezel.THIN, comp = Comp.NONE, dialText = "", accent = Accent.GOLD, numFont = NumFont.BODONI)
                WVariant.CLOCK_LEATHER -> c.copy(face = DialFace.LEATHER, indices = Indices.NUMERALS, hands = Hands.LEAF, metal = Metal.GOLD, bezel = Bezel.NONE, comp = Comp.DAY_DATE, dialText = "", accent = Accent.GOLD, numFont = NumFont.PLAYFAIR)
                WVariant.CLOCK_MINIMAL -> c.copy(face = DialFace.GLASS, indices = Indices.QUARTERS, hands = Hands.BATON, metal = Metal.WHITE, bezel = Bezel.NONE, comp = Comp.NONE, dialText = "", accent = Accent.ORANGE)
                else -> c
            }
        }
    }
}

/** خانة ويدجت: فيها مكدس (كذا ويدجت فوق بعض) */
data class Slot(val items: List<WConfig>, val index: Int = 0) {
    val current: WConfig get() = items[index.coerceIn(0, items.size - 1)]
}

/** ويدجت محطوط بمكان: الصفحة، العمود، الصف، والمقاس */
data class Placed(val id: Long, val page: Int, val x: Int, val y: Int, val size: WSize, val slot: Slot)

/** يحفظ ويدجت كل شاشة لحالها (الخارجية والداخلية) */
class WidgetStore(context: Context) {
    private val prefs = context.getSharedPreferences("widgets_v2", Context.MODE_PRIVATE)

    var outer by mutableStateOf(load("outer") ?: defaultOuter())
        private set
    var inner by mutableStateOf(load("inner") ?: defaultInner())
        private set

    fun list(wide: Boolean): List<Placed> = if (wide) inner else outer

    fun replace(wide: Boolean, list: List<Placed>) {
        if (wide) inner = list else outer = list
        save(if (wide) "inner" else "outer", list)
    }

    fun update(wide: Boolean, id: Long, f: (Placed) -> Placed) =
        replace(wide, list(wide).map { if (it.id == id) f(it) else it })

    fun remove(wide: Boolean, id: Long) = replace(wide, list(wide).filterNot { it.id == id })

    fun add(wide: Boolean, p: Placed) = replace(wide, list(wide) + p)

    fun newId(): Long = System.currentTimeMillis()

    private fun load(key: String): List<Placed>? = runCatching {
        val arr = JSONArray(prefs.getString(key, null) ?: return null)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val items = o.getJSONArray("items").let { a -> (0 until a.length()).mapNotNull { WConfig.fromJson(a.getJSONObject(it)) } }
            Placed(
                o.getLong("id"), o.optInt("page"), o.optInt("x"), o.optInt("y"),
                runCatching { WSize.valueOf(o.getString("size")) }.getOrDefault(WSize.S),
                Slot(items.ifEmpty { listOf(WConfig(WVariant.CLOCK_BLUE)) }, o.optInt("index", 0)),
            )
        }
    }.getOrNull()

    private fun save(key: String, list: List<Placed>) {
        val arr = JSONArray()
        list.forEach { p ->
            arr.put(
                JSONObject().put("id", p.id).put("page", p.page).put("x", p.x).put("y", p.y).put("size", p.size.name)
                    .put("index", p.slot.index).put("items", JSONArray().apply { p.slot.items.forEach { put(it.toJson()) } }),
            )
        }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    private fun clock(v: WVariant) = WConfig.preset(v, WConfig(v))

    private fun defaultOuter() = listOf(
        Placed(1, 0, 0, 0, WSize.WIDE, Slot(listOf(WConfig(WVariant.SKY_SUN), WConfig(WVariant.WEATHER_SKY), WConfig(WVariant.SKY_DAYLINE)))),
        Placed(2, 0, 0, 2, WSize.S, Slot(listOf(clock(WVariant.CLOCK_BLUE), clock(WVariant.CLOCK_WALEED), clock(WVariant.CLOCK_PEARL)))),
        Placed(3, 0, 2, 2, WSize.S, Slot(listOf(WConfig(WVariant.SKY_MOON), WConfig(WVariant.CLOCK_RINGS), WConfig(WVariant.BATTERY_LIQUID)))),
    )

    private fun defaultInner() = listOf(
        Placed(11, 0, 0, 0, WSize.WIDE, Slot(listOf(WConfig(WVariant.SKY_SUN), WConfig(WVariant.WEATHER_SKY), WConfig(WVariant.DATE_MONTH)))),
        Placed(12, 0, 4, 0, WSize.S, Slot(listOf(clock(WVariant.CLOCK_EMERALD), clock(WVariant.CLOCK_NOMOS), WConfig(WVariant.SKY_MOON)))),
    )
}

/** أرقام عربية (١٢٣) أو إنجليزية (123) */
fun String.digits(arabic: Boolean): String =
    if (!arabic) this else map { if (it in '0'..'9') '٠' + (it - '0') else it }.joinToString("")

fun Int.digits(arabic: Boolean): String = toString().digits(arabic)
