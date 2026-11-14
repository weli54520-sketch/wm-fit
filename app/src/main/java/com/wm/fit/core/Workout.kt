package com.wm.fit.core

import kotlin.math.roundToInt

/**
 * يبني تمارين اليوم من خطة الأسبوع.
 *
 * - عدد التمارين لكل عضلة من المجموعات الأسبوعية (ACSM 2026) مقسومة على أيامها.
 * - يتجنب التمارين اللي تضغط على إصابة جديدة (متوسطة أو قوية).
 * - المزمنة ما توقف التمرين، بس نقدّم الأنسب وننبّه نخفف.
 * - يطبّق البدائل اللي اختارها المستخدم من زر "ما أقدر".
 */
object Workout {

    /** أكثر عدد تمارين باليوم، عشان التمرين ما يطول. */
    private const val MAX_ITEMS = 8

    data class Item(
        val ex: Ex,
        val sets: Int,
        val reps: String,
        val restSec: Int,
        /** التمرين الأصلي لو هذا بديل. */
        val from: Ex? = null,
        /** إصابات تخص هذا التمرين بس ما توقفه. */
        val cautions: List<Injury> = emptyList(),
    )

    fun forDay(dow: Int): List<Item> {
        val session = Plan.sessionFor(dow) ?: return emptyList()
        if (P.heartRisk) return emptyList()

        val scheme = Plan.scheme(P.goal, P.level)
        val tired = Store.bodyToday == BodyCheck.TIRED
        val sets = if (tired) (scheme.sets - 1).coerceAtLeast(2) else scheme.sets
        val focus = P.focus.mapNotNull { Muscle.byId(it) }

        // عضلات التركيز أول
        val groups = session.groups.sortedByDescending { it in focus }
        val picked = mutableListOf<Item>()
        val used = mutableSetOf<String>()

        val perMuscle = groups.map { m ->
            val weekly = Calc.weeklySets(m in focus, P.level)
            val (hits, nth) = Plan.muscleHits(m, dow)
            val perSession = weekly.toFloat() / hits
            val n = (perSession / scheme.sets).roundToInt().coerceIn(1, 3)
            Triple(m, n, nth)
        }

        // نختار المركبة أول لكل العضلات، بعدين العزل
        val chosen = perMuscle.associate { (m, n, nth) -> m to choose(m, n, nth, used) }
        val ordered = chosen.values.flatten().sortedByDescending { it.compound }

        for (ex in ordered) {
            if (picked.size >= MAX_ITEMS) break
            val (real, from) = applySwap(ex, used)
            if (picked.any { it.ex.id == real.id }) continue
            picked += Item(
                ex = real,
                sets = sets,
                reps = real.reps ?: scheme.reps,
                restSec = if (real.compound) scheme.restSec else (scheme.restSec * 0.75f).roundToInt(),
                from = from,
                cautions = Store.cautions(real),
            )
        }
        return picked
    }

    /**
     * ترتيب الاختيار لكل عضلة: نخلط أنواع الحركة عشان كل يوم ياخذ نوعين مختلفين.
     * مثلاً الأرجل: سكوات (أمامي) + رومانية (خلفي)، واليوم الثاني: ضغط أرجل + ثني أرجل.
     */
    private val order = listOf(
        "squat", "rdl", "leg_press", "leg_curl", "goblet", "lunge", "leg_ext", "calf",
        "hip_thrust", "bulgarian", "glute_bridge",
        "bench", "cable_fly", "incline_db", "pushup",
        "pulldown", "cable_row", "pullup", "db_row", "deadlift",
        "db_ohp", "lateral", "face_pull", "rear_fly",
        "db_curl", "pushdown", "hammer", "oh_ext",
        "plank", "cable_crunch", "knee_raise",
        "farmer", "wrist_curl",
    ).withIndex().associate { (i, id) -> id to i }

    /** يختار [n] تمارين للعضلة، ويدوّر بينها حسب ترتيب اليوم عشان تتنوع. */
    private fun choose(m: Muscle, n: Int, nth: Int, used: MutableSet<String>): List<Ex> {
        val pool = Exercises.forMuscle(m)
            .filter { !Store.blocked(it) && it.id !in used }
            .sortedWith(
                // اللي فيها إصابة مزمنة أو خفيفة تتأخر، والباقي بالترتيب المخلوط
                compareBy<Ex> { Store.cautions(it).size }
                    .thenBy { order[it.id] ?: Int.MAX_VALUE },
            )
        if (pool.isEmpty()) return emptyList()
        val out = (0 until n.coerceAtMost(pool.size)).map { pool[(nth * n + it) % pool.size] }.distinct()
        used += out.map { it.id }
        return out
    }

    /** لو المستخدم اختار بديل لهذا التمرين، نرجّع البديل. */
    private fun applySwap(ex: Ex, used: MutableSet<String>): Pair<Ex, Ex?> {
        val altId = Store.todaySwaps[ex.id] ?: Store.swaps[ex.id] ?: return ex to null
        val alt = Exercises.get(altId) ?: return ex to null
        if (Store.blocked(alt)) return ex to null
        used += alt.id
        return alt to ex
    }

    /** أعلى رقم في مدى العدّات، مثل "6-10" = 10. */
    fun topReps(reps: String): Int? =
        Regex("""(\d+)\s*-\s*(\d+)""").find(reps)?.groupValues?.get(2)?.toIntOrNull()

    fun lowReps(reps: String): Int? =
        Regex("""(\d+)\s*-\s*(\d+)""").find(reps)?.groupValues?.get(1)?.toIntOrNull()

    /**
     * اقتراح الوزن من آخر مرة.
     * لو كل المجموعات وصلت أعلى المدى: زيد. لو تحت أقل المدى: خفف. غير كذا: نفس الوزن.
     */
    data class Hint(val text: String, val weight: Float?)

    fun hint(item: Item): Hint? {
        val last = Store.lastEntry(item.ex.id) ?: return null
        if (last.sets.isEmpty()) return null
        val w = last.sets.maxOf { it.w }
        val summary = "آخر مرة: " + (if (w > 0f) "${kg(w)} كجم × " else "") +
            last.sets.joinToString("، ") { "${it.r}" }
        val top = topReps(item.reps)
        val low = lowReps(item.reps)
        if (item.ex.body || top == null || low == null) return Hint(summary, if (w > 0f) w else null)
        val all = last.sets.size >= item.sets && last.sets.all { it.r >= top }
        val weak = last.sets.count { it.r < low } >= 2
        return when {
            all -> Hint("$summary. وصلت فوق المدى، جرّب ${kg(w + item.ex.inc)} كجم.", w + item.ex.inc)
            weak -> {
                val lower = (w * 0.9f / item.ex.inc).roundToInt() * item.ex.inc
                Hint("$summary. كان ثقيل، جرّب ${kg(lower)} كجم.", lower)
            }
            else -> Hint("$summary. خلك على نفس الوزن، وحاول تزيد عدّة.", w)
        }
    }

    fun kg(w: Float): String = if (w == w.roundToInt().toFloat()) "${w.roundToInt()}" else
        ((w * 10).roundToInt() / 10f).toString()
}
