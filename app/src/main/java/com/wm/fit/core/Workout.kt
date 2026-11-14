package com.wm.fit.core

import kotlin.math.roundToInt

/**
 * يبني تمارين اليوم من خطة الأسبوع (Plan).
 *
 * - العضلة الرئيسية أول: تمرينها الأساسي ثابت كل أسبوع، والباقي يتغير كل 5 أسابيع.
 * - العضلات المساعدة بعدها، بتمارين غير الأساسية عشان ما يتكرر نفس التمرين مرتين بالأسبوع.
 * - يتجنب التمارين اللي تضغط على إصابة جديدة (متوسطة أو قوية).
 * - المزمنة ما توقف التمرين، بس نقدّم الأنسب وننبّه نخفف.
 * - يطبّق البدائل اللي اختارها المستخدم من زر "ما أقدر".
 */
object Workout {

    data class Item(
        val ex: Ex,
        val sets: Int,
        val reps: String,
        val restSec: Int,
        /** التمرين الأصلي لو هذا بديل. */
        val from: Ex? = null,
        /** إصابات تخص هذا التمرين بس ما توقفه. */
        val cautions: List<Injury> = emptyList(),
        /** التمرين الأساسي لليوم (ثابت كل أسبوع). */
        val main: Boolean = false,
        /** هل هو للعضلة الرئيسية أو مساعد. */
        val primary: Boolean = true,
    )

    fun forDay(dow: Int): List<Item> {
        val session = Plan.sessionFor(dow) ?: return emptyList()
        if (P.heartRisk) return emptyList()

        val scheme = Plan.scheme(P.goal, P.level)
        val tired = Store.bodyToday == BodyCheck.TIRED
        val sets = if (tired) (scheme.sets - 1).coerceAtLeast(2) else scheme.sets
        val block = Plan.block()
        val used = mutableSetOf<String>()
        val out = mutableListOf<Item>()

        for (slot in session.slots) {
            pick(slot, block, used).forEachIndexed { idx, ex ->
                val (real, from) = applySwap(ex, used)
                if (out.any { it.ex.id == real.id }) return@forEachIndexed
                out += Item(
                    ex = real,
                    sets = sets,
                    reps = real.reps ?: scheme.reps,
                    restSec = if (real.compound) scheme.restSec else (scheme.restSec * 0.75f).roundToInt(),
                    from = from,
                    cautions = Store.cautions(real),
                    main = slot.main && idx == 0,
                    primary = slot.main,
                )
            }
        }
        return out
    }

    /**
     * يختار تمارين المكان.
     * الرئيسية: أول تمرين = الأساسي (الأول في المرة الأولى بالأسبوع، الثاني في المرة الثانية).
     * المساعدة للعضلات الكبيرة: نتجنب التمارين الأساسية عشان ما تتكرر.
     */
    private fun pick(slot: Plan.Slot, block: Int, used: MutableSet<String>): List<Ex> {
        val part = slot.part
        val pool = part.ids.mapNotNull { Exercises.get(it) }
            .filter { !Store.blocked(it) && it.id !in used }
        if (pool.isEmpty()) return emptyList()
        val big = part.ids.size > 3
        val mains = if (big) part.ids.take(2) else emptyList()
        val chosen = mutableListOf<Ex>()

        if (slot.main) {
            // الأساسي ثابت: المرة الأولى بالأسبوع الأول، والمرة الثانية الثاني
            val fixed = part.ids.take(2)
            val prefer = listOf(fixed[slot.nth % fixed.size], fixed[(slot.nth + 1) % fixed.size])
            val main = prefer.firstNotNullOfOrNull { id -> pool.firstOrNull { it.id == id } } ?: pool.first()
            chosen += main
            // أول يوم للعضلة الكبيرة: الأساسي الثاني بعده (مثل سكوات + رومانية = أمامي وخلفي)
            if (big && slot.nth == 0 && slot.count > 1) {
                pool.firstOrNull { it.id == mains[1] && it !in chosen }?.let { chosen += it }
            }
        }

        // الباقي: اللي ما فيها إصابة أول، وندوّر بينها كل 5 أسابيع
        // التمارين الثقيلة جداً ما تجي كمساعدة
        val rest = pool.filter { it !in chosen && it.id !in mains && (slot.main || it.id !in heavy) }
            .ifEmpty { pool.filter { it !in chosen } }
        val clean = rest.filter { Store.cautions(it).isEmpty() }
        val risky = rest.filter { Store.cautions(it).isNotEmpty() }
        val shift = if (clean.isEmpty()) 0 else (block + slot.nth * 2) % clean.size
        val rotated = clean.drop(shift) + clean.take(shift) + risky
        chosen += rotated.take((slot.count - chosen.size).coerceAtLeast(0))

        used += chosen.map { it.id }
        return chosen
    }

    /** تمارين ثقيلة على الجسم كله، تكون في يوم عضلتها الرئيسية بس. */
    private val heavy = setOf("deadlift", "squat")

    /** لو المستخدم اختار بديل لهذا التمرين، نرجّع البديل. */
    private fun applySwap(ex: Ex, used: MutableSet<String>): Pair<Ex, Ex?> {
        val altId = Store.todaySwaps[ex.id] ?: Store.swaps[ex.id] ?: return ex to null
        val alt = Exercises.get(altId) ?: return ex to null
        if (Store.blocked(alt)) return ex to null
        used += alt.id
        return alt to ex
    }

    /** مجموع المجموعات بالأسبوع لكل عضلة، من الخطة الفعلية. */
    fun weeklySets(): Map<Muscle, Int> {
        val total = mutableMapOf<Muscle, Int>()
        Plan.trainingDays().forEach { d ->
            forDay(d).forEach { total[it.ex.muscle] = (total[it.ex.muscle] ?: 0) + it.sets }
        }
        return total
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
