package com.wm.fit.core

import java.util.Calendar
import kotlin.math.roundToInt

/**
 * تقسيم الأسبوع: كل يوم له عضلة رئيسية + عضلات مساعدة.
 *
 * القواعد:
 * - كل يوم له عضلة رئيسية تاخذ التمارين الأساسية (المركّبة) وأغلب التمارين.
 * - عضلات التركيز (اختيار المستخدم) تنلعب مرتين بالأسبوع: يوم رئيسية، ومرة
 *   رئيسية ثانية أو مساعدة في يوم يناسبها.
 * - **أقصى شي مرتين بالأسبوع لأي عضلة**، عشان ترتاح وتكبر.
 * - العضلات الباقية مرة أو مرتين حسب الأيام. مرة بالأسبوع تكفي للمحافظة عليها.
 * - ما نحط نفس العضلة أو يومين أرجل ورا بعض إذا نقدر.
 * - التمرين الأساسي ثابت كل أسبوع (عشان يزيد وزنه)، والمساعد يتغير كل 5 أسابيع.
 *
 * المستخدم يختار أيامه، والجمعة راحة في الافتراضي.
 */
object Plan {

    val dayNamesAr = listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
    val dayShortAr = listOf("أحد", "إثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة", "سبت")

    /** أقل وأكثر عدد أيام. */
    const val MIN_DAYS = 2
    const val MAX_DAYS = 6

    /** أقصى عدد مرات للعضلة بالأسبوع. */
    const val MAX_PER_WEEK = 2

    /** كم أسبوع يبقى فيه التمرين المساعد قبل ما يتغير. */
    const val BLOCK_WEEKS = 5

    /** أكثر عدد تمارين باليوم، عشان التمرين ما يطول. */
    private const val MAX_ITEMS = 7

    /**
     * الأيام الافتراضية لو المستخدم ما اختار. 0 = الأحد.
     * الجمعة راحة دايماً في الافتراضي.
     */
    fun defaultDays(perWeek: Int): List<Int> = when (perWeek.coerceIn(MIN_DAYS, MAX_DAYS)) {
        2 -> listOf(0, 3)
        3 -> listOf(0, 2, 4)
        4 -> listOf(0, 1, 3, 4)
        5 -> listOf(0, 1, 2, 3, 4)
        else -> listOf(0, 1, 2, 3, 4, 6)
    }

    fun trainingDays(): List<Int> = P.trainDays

    /**
     * جزء من الجسم ومجموعة تمارينه.
     * [key] للعدّ (أقصى مرتين): رؤوس الكتف كلها تنحسب "أكتاف".
     * أول تمرينين في [ids] هم التمارين الأساسية للعضلات الكبيرة.
     */
    enum class Part(val ar: String, val muscle: Muscle, val key: String, val ids: List<String>) {
        LEGS("أرجل", Muscle.LEGS, "legs",
            listOf("squat", "rdl", "leg_press", "leg_curl", "goblet", "lunge", "leg_ext", "calf",
                "lying_leg_curl", "hack_squat", "db_rdl", "step_up", "sl_rdl", "seated_calf")),
        GLUTES("أرداف", Muscle.GLUTES, "glutes", listOf("hip_thrust", "bulgarian", "glute_bridge", "sumo_squat", "cable_pull_through", "single_leg_bridge")),
        CHEST("صدر", Muscle.CHEST, "chest", listOf("bench", "incline_db", "cable_fly", "pushup", "db_bench", "machine_press", "incline_bar")),
        BACK("ظهر", Muscle.BACK, "back", listOf("pulldown", "cable_row", "pullup", "db_row", "deadlift", "barbell_row", "chest_row", "close_pulldown")),
        SHOULDERS("أكتاف", Muscle.SHOULDERS, "shoulders", listOf("db_ohp", "lateral", "face_pull", "rear_fly", "machine_ohp", "cable_lateral")),
        SIDE_DELT("أكتاف جانبي", Muscle.SHOULDERS, "shoulders", listOf("lateral", "cable_lateral")),
        REAR_DELT("أكتاف خلفي", Muscle.SHOULDERS, "shoulders", listOf("rear_fly", "face_pull")),
        TRICEPS("ترايسبس", Muscle.ARMS, "tri", listOf("pushdown", "oh_ext", "cable_oh_ext")),
        BICEPS("بايسبس", Muscle.ARMS, "bi", listOf("db_curl", "hammer", "ez_curl", "cable_curl", "incline_curl")),
        CORE("بطن", Muscle.CORE, "core", listOf("plank", "cable_crunch", "knee_raise")),
        FOREARMS("ساعد", Muscle.FOREARMS, "forearms", listOf("farmer", "wrist_curl")),
    }

    /**
     * مكان في اليوم: جزء + كم تمرين.
     * [main] = العضلة الرئيسية (أول تمرين أساسي وثابت).
     * [nth] = ترتيب هذا الظهور للعضلة بالأسبوع (للتنويع بين المرتين).
     */
    data class Slot(val part: Part, val count: Int, val main: Boolean, val focus: Boolean, val nth: Int)

    /** جلسة يوم واحد. */
    data class Session(
        val title: String,
        val primary: Muscle,
        val slots: List<Slot>,
        /** ليش اليوم كذا، بكلام بسيط. */
        val reasons: List<String>,
    ) {
        val groups: List<Muscle> get() = slots.map { it.part.muscle }.distinct()
    }

    /** خطة الأسبوع كاملة. */
    data class Week(val days: Map<Int, Session>, val notes: List<String>, val times: Map<Muscle, Int>)

    private var cacheKey = ""
    private var cache: Week? = null

    /** خطة هالأسبوع. تنحسب مرة وحدة لين يتغير شي في الإعدادات. */
    fun week(): Week {
        val days = trainingDays()
        val focus = P.focus.mapNotNull { Muscle.byId(it) }
        val key = "$days|$focus|${P.level}|${P.goal}|${P.daySwapsRaw}"
        cache?.let { if (key == cacheKey) return it }
        val w = build(days, focus, swapsFor(days, focus))
        cache = w
        cacheKey = key
        return w
    }

    /** جلسة هذا اليوم، أو null لو يوم راحة. 0 = الأحد. */
    fun sessionFor(dow: Int): Session? = week().days[dow]

    // ===== بناء الخطة =====

    private val canPrimary = listOf(
        Muscle.LEGS, Muscle.GLUTES, Muscle.CHEST, Muscle.BACK, Muscle.SHOULDERS, Muscle.ARMS,
    )
    private val base = listOf(Muscle.LEGS, Muscle.CHEST, Muscle.BACK, Muscle.SHOULDERS)
    private val lower = setOf(Muscle.LEGS, Muscle.GLUTES)

    /** المفاتيح اللي يمثلها كل عضلة لما تكون رئيسية. */
    private fun keysOf(m: Muscle): List<String> = when (m) {
        Muscle.ARMS -> listOf("bi", "tri")
        else -> listOf(Part.entries.first { it.muscle == m }.key)
    }

    private fun muscleOfKey(k: String): Muscle = Part.entries.first { it.key == k }.muscle

    /** أنسب يوم (حسب عضلته الرئيسية) لكل عضلة مساعدة، بالترتيب. */
    private val hosts: Map<String, List<Muscle>> = mapOf(
        "legs" to listOf(Muscle.GLUTES, Muscle.SHOULDERS, Muscle.ARMS, Muscle.BACK, Muscle.CHEST),
        "glutes" to listOf(Muscle.LEGS, Muscle.SHOULDERS, Muscle.ARMS, Muscle.BACK, Muscle.CHEST),
        "chest" to listOf(Muscle.SHOULDERS, Muscle.ARMS, Muscle.BACK, Muscle.LEGS, Muscle.GLUTES),
        "back" to listOf(Muscle.SHOULDERS, Muscle.ARMS, Muscle.CHEST, Muscle.LEGS, Muscle.GLUTES),
        "shoulders" to listOf(Muscle.CHEST, Muscle.BACK, Muscle.ARMS, Muscle.LEGS, Muscle.GLUTES),
        "tri" to listOf(Muscle.CHEST, Muscle.SHOULDERS, Muscle.LEGS, Muscle.GLUTES, Muscle.BACK),
        "bi" to listOf(Muscle.BACK, Muscle.SHOULDERS, Muscle.LEGS, Muscle.GLUTES, Muscle.CHEST),
        "core" to listOf(Muscle.LEGS, Muscle.GLUTES, Muscle.SHOULDERS, Muscle.ARMS, Muscle.BACK, Muscle.CHEST),
        "forearms" to listOf(Muscle.BACK, Muscle.ARMS, Muscle.LEGS, Muscle.GLUTES),
    )

    /** الجزء المناسب للعضلة المساعدة حسب يومها. مثلاً الأكتاف مع الصدر = جانبي. */
    private fun secondaryPart(key: String, host: Muscle): Part = when (key) {
        "legs" -> Part.LEGS
        "glutes" -> Part.GLUTES
        "chest" -> Part.CHEST
        "back" -> Part.BACK
        "shoulders" -> when (host) {
            Muscle.CHEST -> Part.SIDE_DELT
            Muscle.BACK -> Part.REAR_DELT
            else -> Part.SHOULDERS
        }
        "tri" -> Part.TRICEPS
        "bi" -> Part.BICEPS
        "core" -> Part.CORE
        else -> Part.FOREARMS
    }

    /** أزواج الأيام اللي ورا بعض (بالمؤشر). السبت والأحد ورا بعض بعد. */
    private fun adjacent(days: List<Int>): List<Pair<Int, Int>> {
        val out = mutableListOf<Pair<Int, Int>>()
        for (i in 0 until days.size - 1) if (days[i + 1] - days[i] == 1) out += i to i + 1
        if (days.size > 2 && days.first() + 7 - days.last() == 1) out += days.lastIndex to 0
        return out
    }

    private fun <T> permutations(list: List<T>): List<List<T>> {
        if (list.size <= 1) return listOf(list)
        val out = LinkedHashSet<List<T>>()
        list.indices.forEach { i ->
            val rest = list.toMutableList().also { it.removeAt(i) }
            permutations(rest).forEach { out += listOf(list[i]) + it }
        }
        return out.toList()
    }

    /** العضلات الرئيسية لكل يوم، قبل الترتيب. */
    private fun primaries(n: Int, focus: List<Muscle>): List<Muscle> {
        val fp = focus.filter { it in canPrimary }
        val prim = (fp + base).distinct().take(n).toMutableList()
        // أيام زيادة: عضلات التركيز يوم ثاني أول، بعدين الأرجل والظهر والصدر
        val fill = fp + listOf(Muscle.LEGS, Muscle.BACK, Muscle.CHEST, Muscle.SHOULDERS, Muscle.GLUTES, Muscle.ARMS)
        for (m in fill) {
            if (prim.size >= n) break
            if (prim.count { it == m } < MAX_PER_WEEK) prim += m
        }
        for (m in fill) {
            if (prim.size >= n) break
            if (prim.count { it == m } < MAX_PER_WEEK) prim += m
        }
        return prim
    }

    internal fun build(days: List<Int>, focus: List<Muscle>, swaps: List<Pair<Int, Int>> = emptyList()): Week {
        val n = days.size
        if (n == 0) return Week(emptyMap(), emptyList(), emptyMap())
        val pairs = adjacent(days)

        // 1) ترتيب الأيام: نفس العضلة أو الأرجل ورا بعض = نقاط سلبية
        val order = permutations(primaries(n, focus)).minByOrNull { o ->
            pairs.sumOf { (a, b) ->
                (if (o[a] == o[b]) 10 else 0) + (if (o[a] in lower && o[b] in lower) 6 else 0)
            }
        }!!.toMutableList()

        // تبديل المستخدم بين يومين
        swaps.forEach { (a, b) ->
            val i = days.indexOf(a)
            val j = days.indexOf(b)
            if (i >= 0 && j >= 0) order[i] = order[j].also { order[j] = order[i] }
        }

        // 2) كم مرة كل عضلة بالأسبوع
        val focusKeys = focus.flatMap { keysOf(it) }.toSet()
        val count = mutableMapOf<String, Int>()
        val present = Array(n) { mutableSetOf<String>() }
        order.forEachIndexed { i, m -> keysOf(m).forEach { k -> count[k] = (count[k] ?: 0) + 1; present[i] += k } }
        val secs = Array(n) { mutableListOf<String>() }
        val maxSec = if (n <= 2) 4 else 3

        fun need(k: String): Int = when {
            k in focusKeys -> MAX_PER_WEEK
            k == "forearms" -> 0
            n <= 2 && k in setOf("legs", "chest", "back") -> 2
            else -> 1
        }

        fun neighbours(i: Int) = pairs.filter { it.first == i || it.second == i }
            .map { if (it.first == i) it.second else it.first }

        fun place(k: String): Boolean {
            val pref = hosts.getValue(k)
            val best = (0 until n)
                .filter { k !in present[it] && secs[it].size < maxSec && order[it] in pref }
                .minByOrNull { i ->
                    pref.indexOf(order[i]) * 10 + secs[i].size * 4 +
                        neighbours(i).count { k in present[it] } * 6
                } ?: return false
            secs[best] += k
            present[best] += k
            count[k] = (count[k] ?: 0) + 1
            return true
        }

        // 3) نوزع العضلات المساعدة: التركيز أول، بعدين الكبيرة، بعدين الصغيرة
        val keys = listOf("legs", "chest", "back", "shoulders", "glutes", "tri", "bi", "core", "forearms")
        val ordered = keys.filter { it in focusKeys } + keys.filter { it !in focusKeys }
        repeat(MAX_PER_WEEK) {
            for (k in ordered) {
                if ((count[k] ?: 0) < need(k) && (count[k] ?: 0) < MAX_PER_WEEK) place(k)
            }
        }
        // يوم الأرجل لازم فيه أرداف (مع الفخذ الخلفي)، عشان يشتغلون مع بعض
        for (i in 0 until n) {
            if (order[i] == Muscle.LEGS && "glutes" !in present[i] &&
                (count["glutes"] ?: 0) < MAX_PER_WEEK && secs[i].size < maxSec
            ) {
                secs[i] += "glutes"
                present[i] += "glutes"
                count["glutes"] = (count["glutes"] ?: 0) + 1
            }
        }
        // يوم فيه عضلة وحدة بس: نضيف له عضلة كبيرة مساعدة عشان توصل مرتين بالأسبوع.
        // جزء واحد بس، عشان اليوم ما يتشتت.
        for (i in 0 until n) {
            if (secs[i].isNotEmpty() || order[i] == Muscle.ARMS) continue
            val k = listOf("legs", "back", "chest", "glutes", "shoulders").firstOrNull { k ->
                (count[k] ?: 0) < MAX_PER_WEEK && k !in present[i] && order[i] in hosts.getValue(k) &&
                    neighbours(i).none { k in present[it] }
            } ?: continue
            secs[i] += k
            present[i] += k
            count[k] = (count[k] ?: 0) + 1
        }

        // الساعد: إضافة خفيفة في يوم ظهر فيه مكان
        if ((count["forearms"] ?: 0) == 0) {
            (0 until n).firstOrNull { order[it] == Muscle.BACK && secs[it].size < 2 }?.let {
                secs[it] += "forearms"; present[it] += "forearms"; count["forearms"] = 1
            }
        }

        // 4) كم تمرين لكل جزء
        val scheme = scheme(P.goal, P.level)
        fun totalEx(m: Muscle): Int =
            (Calc.weeklySets(m in focus, P.level).toFloat() / scheme.sets).roundToInt().coerceIn(4, 8)

        val nthSeen = mutableMapOf<String, Int>()
        val sessions = mutableMapOf<Int, Session>()
        days.forEachIndexed { i, dow ->
            val m = order[i]
            val slots = mutableListOf<Slot>()
            val isFocus = m in focus

            if (m == Muscle.ARMS) {
                listOf(Part.BICEPS, Part.TRICEPS).forEach { p ->
                    val nth = nthSeen[p.key] ?: 0
                    nthSeen[p.key] = nth + 1
                    slots += Slot(p, 3, true, isFocus, nth)
                }
            } else {
                val k = keysOf(m).first()
                val p = Part.entries.first { it.key == k && it.muscle == m }
                val prim = order.count { it == m }
                val secN = count[k]!! - prim
                val secEach = if (isFocus) 2 else 1
                // قرار المالك: يوم العضلة الرئيسية فيه 4 إلى 5 تمارين لها
                val each = ((totalEx(m) - secN * secEach).toFloat() / prim).roundToInt().coerceIn(4, 5)
                val nth = nthSeen[k] ?: 0
                nthSeen[k] = nth + 1
                slots += Slot(p, each, true, isFocus, nth)
            }

            secs[i].sortedByDescending { it in focusKeys }.forEach { k ->
                val mk = muscleOfKey(k)
                val f = mk in focus
                val big = k in setOf("legs", "glutes", "chest", "back", "shoulders")
                val prim = order.count { it == mk }
                val c = when {
                    k == "glutes" && m == Muscle.LEGS -> 2
                    big && prim == 0 -> (totalEx(mk).toFloat() / count[k]!!).roundToInt().coerceIn(1, 2)
                    f -> 2
                    else -> 1
                }
                val nth = nthSeen[k] ?: 0
                nthSeen[k] = nth + 1
                slots += Slot(secondaryPart(k, m), c, false, f, nth)
            }

            // ما نتجاوز الحد: ننقص المساعد اللي مو تركيز، بعدين الرئيسية
            while (slots.sumOf { it.count } > MAX_ITEMS) {
                val j = listOf(
                    slots.indexOfLast { !it.main && !it.focus && it.count > 1 },
                    slots.indexOfFirst { it.main && it.count > 4 },
                    slots.indexOfLast { !it.main && it.focus && it.count > 1 },
                    slots.indexOfLast { !it.main && !it.focus },
                ).firstOrNull { it >= 0 } ?: break
                if (slots[j].count > 1) slots[j] = slots[j].copy(count = slots[j].count - 1)
                else slots.removeAt(j)
            }

            sessions[dow] = Session(
                title = slots.joinToString(" + ") { it.part.ar },
                primary = m,
                slots = slots,
                reasons = reasons(m, isFocus, slots),
            )
        }

        val times = Muscle.entries.associateWith { mu -> keysOf(mu).maxOf { count[it] ?: 0 } }
        return Week(sessions, weekNotes(focus, times, n), times)
    }

    private fun reasons(m: Muscle, isFocus: Boolean, slots: List<Slot>): List<String> {
        val out = mutableListOf<String>()
        out += if (isFocus) "${m.ar} رئيسية اليوم لأنك اخترتها تركيز، وتبدأ فيها وأنت نشيط."
        else "${m.ar} رئيسية اليوم: التمارين الأساسية لها."
        slots.filter { !it.main && it.focus }.forEach {
            out += "${it.part.ar} مساعدة اليوم، عشان ${it.part.muscle.ar} تنلعب مرتين بالأسبوع."
        }
        return out
    }

    private fun weekNotes(focus: List<Muscle>, times: Map<Muscle, Int>, n: Int): List<String> {
        val out = mutableListOf<String>()
        if (focus.isNotEmpty()) {
            out += focus.joinToString(" و") { it.ar } +
                ": مرتين بالأسبوع لأنك اخترتها تركيز. وما نزيد عن مرتين، عشان العضلة ترتاح وتكبر."
        }
        val once = listOf(Muscle.LEGS, Muscle.CHEST, Muscle.BACK, Muscle.SHOULDERS)
            .filter { it !in focus && times[it] == 1 }
        if (once.isNotEmpty()) {
            out += once.joinToString(" و") { it.ar } + ": مرة بالأسبوع، تكفي عشان تحافظ عليها" +
                if (focus.isNotEmpty()) "، والتركيز رايح للي اخترتها." else ". زيد أيامك لو تبيها مرتين."
        }
        if (n >= 2) out += "التمرين الأساسي ثابت كل أسبوع عشان تزيد وزنه، والمساعد يتغير كل $BLOCK_WEEKS أسابيع."
        return out
    }

    // ===== تبديل الأيام =====

    private fun sig(days: List<Int>, focus: List<Muscle>) = "$days$focus".hashCode().toString()

    private fun swapsFor(days: List<Int>, focus: List<Muscle>): List<Pair<Int, Int>> {
        val raw = P.daySwapsRaw
        val parts = raw.split("|")
        if (parts.size != 2 || parts[0] != sig(days, focus)) return emptyList()
        return parts[1].split(",").mapNotNull {
            val ab = it.split(":")
            val a = ab.getOrNull(0)?.toIntOrNull()
            val b = ab.getOrNull(1)?.toIntOrNull()
            if (a != null && b != null) a to b else null
        }
    }

    /** يبدّل جلستين بين يومين. ينمسح لحاله لو تغيرت الأيام أو التركيز. */
    fun swapDays(a: Int, b: Int) {
        val days = trainingDays()
        val focus = P.focus.mapNotNull { Muscle.byId(it) }
        val list = swapsFor(days, focus) + (a to b)
        P.daySwapsRaw = sig(days, focus) + "|" + list.joinToString(",") { "${it.first}:${it.second}" }
    }

    fun hasSwaps(): Boolean = swapsFor(trainingDays(), P.focus.mapNotNull { Muscle.byId(it) }).isNotEmpty()

    fun resetSwaps() {
        P.daySwapsRaw = ""
    }

    // ===== الوقت =====

    /** رقم اليوم اليوم. 0 = الأحد. */
    fun todayDow(): Int = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1

    fun todaySession(): Session? = sessionFor(todayDow())

    /** أول يوم تمرين جاي بعد اليوم. */
    fun nextTrainingDay(): Int {
        val t = todayDow()
        for (k in 1..7) {
            val d = (t + k) % 7
            if (sessionFor(d) != null) return d
        }
        return t
    }

    /** رقم فترة الـ 5 أسابيع الحالية. التمارين المساعدة تتغير لما يتغير. */
    fun block(): Int {
        val week = (System.currentTimeMillis() / 86_400_000L + 4) / 7
        return (week / BLOCK_WEEKS).toInt()
    }

    /**
     * نظام المجموعات والعدّات حسب الهدف.
     * القوة: حمل ثقيل 80٪ أو أكثر من أقصى وزن، 2 إلى 3 مجموعات (ACSM 2026).
     * التضخيم: مدى واسع من الأحمال يشتغل لو الجهد كافي، وما يلزم الوصول للفشل.
     */
    data class Scheme(val sets: Int, val reps: String, val restSec: Int, val note: String)

    fun scheme(goal: String, level: String): Scheme = when (goal) {
        Goal.CUT -> Scheme(
            sets = if (level == "beg") 3 else 4,
            reps = "10-15",
            restSec = 75,
            note = "وقت التنشيف نحافظ على الأوزان قدر الإمكان، هي اللي تحمي عضلك.",
        )
        Goal.BULK, Goal.MAINTAIN -> Scheme(
            sets = if (level == "beg") 3 else 4,
            reps = "6-10",
            restSec = 120,
            note = "خلّ آخر عدّتين صعبة، بدون ما توصل للفشل كل مجموعة.",
        )
        else -> Scheme(
            sets = 3,
            reps = "8-12",
            restSec = 90,
            note = "مع الكارديو، تمارين القوة تحافظ على عضلك.",
        )
    }

    /** الكارديو المقترح بالأسبوع. */
    fun cardioNote(goal: String): String = when (goal) {
        Goal.CUT -> "امشِ 8 إلى 10 آلاف خطوة يومياً. أسهل من الكارديو الشديد وأقل تعب على رجولك."
        Goal.BULK -> "كارديو خفيف 10 دقايق للإحماء بس، عشان ما ياكل من سعراتك."
        Goal.MAINTAIN -> "كمّل على 7 إلى 8 آلاف خطوة يومياً، وأكلك على احتياجك بالضبط."
        else -> "150 دقيقة كارديو متوسط بالأسبوع، أو 75 دقيقة شديد."
    }
}
