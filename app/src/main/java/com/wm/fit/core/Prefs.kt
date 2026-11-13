package com.wm.fit.core

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlin.reflect.KProperty

/**
 * كل إعدادات التطبيق وبيانات المستخدم.
 *
 * الخصوصية: كل شي هنا ينحفظ في ذاكرة التطبيق داخل الجوال بس (SharedPreferences).
 * ما نرسل أي شي لأي خادم، وما نسمح بنسخة احتياطية تلقائية (شوف data_rules.xml).
 */
object P {
    internal lateinit var sp: SharedPreferences
        private set

    fun init(c: Context) {
        sp = c.getSharedPreferences("wmfit", Context.MODE_PRIVATE)
    }

    // ===== هل خلّص أسئلة أول مرة؟ =====
    var onboarded by Pref("onboarded", false)

    // ===== بياناتك =====
    var name by Pref("name", "")
    /** "m" ذكر أو "f" أنثى */
    var sex by Pref("sex", "m")
    var age by Pref("age", 0)
    var heightCm by Pref("heightCm", 0)
    var weightKg by Pref("weightKg", 0f)

    /** الهدف: cut تنشيف، bulk تضخيم، fitness لياقة */
    var goal by Pref("goal", Goal.CUT)
    /** معامل النشاط اليومي بدون التمرين */
    var activity by Pref("activity", 1.55f)
    var daysPerWeek by Pref("daysPerWeek", 4)
    /** beg مبتدئ، mid متوسط، adv متقدم */
    var level by Pref("level", "beg")

    /** العضلات اللي تبي تركز عليها، مفصولة بفاصلة. فاضي = خطة متوازنة. */
    var focusRaw by Pref("focus", "")
    var focus: List<String>
        get() = focusRaw.split(",").filter { it.isNotBlank() }
        set(v) { focusRaw = v.joinToString(",") }

    /** المدرب يقترح عضلات متأخرة */
    var coachSuggests by Pref("coachSuggests", true)

    // ===== أسئلة السلامة =====
    var sfHeart by Pref("sfHeart", false)
    var sfChestPain by Pref("sfChestPain", false)
    var sfKidney by Pref("sfKidney", false)
    var sfMeds by Pref("sfMeds", false)
    var sfSurgery by Pref("sfSurgery", false)

    /** فيه خطر على القلب = نوقف الحديد الثقيل لين يراجع طبيب */
    val heartRisk: Boolean get() = sfHeart || sfChestPain
    val anySafetyFlag: Boolean get() = sfHeart || sfChestPain || sfKidney || sfMeds || sfSurgery

    // ===== المرحلة =====
    /** 0 = تنشيف، 1 = تثبيت، 2 = تضخيم نظيف. تتغير بموافقة المستخدم بس. */
    var stage by Pref("stage", 0)
    /** الخصر المستهدف بالسنتيمتر. 0 = ما حدده. */
    var targetWaist by Pref("targetWaist", 0f)
    var neckCm by Pref("neckCm", 0f)

    // ===== الشكل =====
    /** 0 أخضر، 1 برتقالي، 2 أزرق، 3 فيروزي */
    var accent by Pref("accent", 0)
    /** "ar" أو "en" */
    var lang by Pref("lang", "ar")

    // ===== الخصوصية =====
    /** المحادثة الحرة مع Claude. مقفلة افتراضياً، والمستخدم يفعّلها بنفسه بمفتاحه. */
    var smartChat by Pref("smartChat", false)
    /** قفل التطبيق بالبصمة */
    var appLock by Pref("appLock", false)

    /** يمسح كل شي. */
    fun wipe() {
        sp.edit().clear().apply()
        Pref.reloadAll()
    }
}

object Goal {
    const val CUT = "cut"
    const val BULK = "bulk"
    const val FITNESS = "fitness"
}

/** عضلات التركيز. المعرّف ثابت في التخزين، والاسم للعرض. */
enum class Muscle(val id: String, val ar: String, val en: String) {
    LEGS("legs", "الأرجل", "Legs"),
    GLUTES("glutes", "الأرداف", "Glutes"),
    CHEST("chest", "الصدر", "Chest"),
    BACK("back", "الظهر", "Back"),
    SHOULDERS("shoulders", "الأكتاف", "Shoulders"),
    ARMS("arms", "الذراعين", "Arms"),
    CORE("core", "البطن", "Core"),
    FOREARMS("forearms", "الساعد", "Forearms");

    companion object {
        fun byId(id: String) = entries.firstOrNull { it.id == id }
    }
}

/** مفتاح محفوظ، يحدّث الواجهة لحاله لما يتغير. */
class Pref<T>(private val key: String, private val def: T) {
    private val state: MutableState<T> = mutableStateOf(def)
    private var loaded = false

    init { all += this }

    @Suppress("UNCHECKED_CAST")
    private fun read(): T = when (def) {
        is Boolean -> P.sp.getBoolean(key, def) as T
        is Int -> P.sp.getInt(key, def) as T
        is Float -> P.sp.getFloat(key, def) as T
        is String -> (P.sp.getString(key, def) ?: def) as T
        else -> def
    }

    private fun ensure() {
        if (!loaded) { state.value = read(); loaded = true }
    }

    internal fun reload() { loaded = false }

    operator fun getValue(thisRef: Any?, p: KProperty<*>): T {
        ensure()
        return state.value
    }

    operator fun setValue(thisRef: Any?, p: KProperty<*>, v: T) {
        ensure()
        state.value = v
        P.sp.edit().apply {
            when (v) {
                is Boolean -> putBoolean(key, v)
                is Int -> putInt(key, v)
                is Float -> putFloat(key, v)
                is String -> putString(key, v)
            }
        }.apply()
    }

    companion object {
        private val all = mutableListOf<Pref<*>>()
        fun reloadAll() = all.forEach { it.reload() }
    }
}
