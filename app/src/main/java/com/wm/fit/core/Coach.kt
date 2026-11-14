package com.wm.fit.core

import org.json.JSONObject

/**
 * المدرب: يقترح لو شاف عضلة متأخرة، **والقرار للمستخدم**.
 * يعرفها من 4 أشياء:
 * 1. التوازن بين العضلات المتقابلة (صدر وظهر).
 * 2. التوقف بدون تطور 3 أسابيع أو أكثر.
 * 3. قلة المجموعات الأسبوعية (عضلة تنهمل).
 * 4. كثرة الضغط على "ما أقدر".
 * كل الحساب داخل الجوال، بدون نت.
 */
object Coach {

    data class Tip(val id: String, val muscle: Muscle?, val title: String, val why: String, val canFocus: Boolean)

    /** المجموعات المسجلة لكل عضلة آخر [days] يوم. */
    private fun loggedSets(days: Int): Map<Muscle, Int> {
        val today = Store.today()
        val out = mutableMapOf<Muscle, Int>()
        Store.logs.forEach { (id, entries) ->
            val m = Exercises.get(id)?.muscle ?: return@forEach
            entries.filter { Dates.daysBetween(it.date, today) in 0 until days }
                .forEach { out[m] = (out[m] ?: 0) + it.sets.size }
        }
        return out
    }

    private fun sessionsIn(days: Int): Int {
        val today = Store.today()
        return Store.done.count { Dates.daysBetween(it, today) in 0 until days }
    }

    /** قوة تقديرية (أقصى وزن لمرة وحدة) من الوزن والعدّات: معادلة Epley. */
    private fun e1rm(w: Float, r: Int) = w * (1f + r / 30f)

    fun tips(): List<Tip> {
        if (!P.coachSuggests) return emptyList()
        val focus = P.focus.mapNotNull { Muscle.byId(it) }
        val out = mutableListOf<Tip>()
        val s14 = loggedSets(14)

        // 1) التوازن: الصدر أكثر من الظهر بكثير
        val chest = s14[Muscle.CHEST] ?: 0
        val back = s14[Muscle.BACK] ?: 0
        if (chest >= 12 && back < chest * 2 / 3 && Muscle.BACK !in focus) {
            out += Tip(
                "balance_back", Muscle.BACK, "الظهر متأخر عن الصدر",
                "آخر أسبوعين: $chest مجموعة صدر مقابل $back ظهر. التوازن يحمي كتفك ويعدّل وقفتك.", true,
            )
        }

        // 2) التوقف: نفس القوة أو أقل 3 أسابيع
        val today = Store.today()
        val stalled = mutableSetOf<Muscle>()
        Store.logs.forEach { (id, entries) ->
            val ex = Exercises.get(id) ?: return@forEach
            if (ex.body || ex.muscle in stalled) return@forEach
            val withW = entries.filter { e -> e.sets.any { it.w > 0f } }
            val recent = withW.filter { Dates.daysBetween(it.date, today) < 21 }
            val before = withW.filter { Dates.daysBetween(it.date, today) >= 21 }
            if (recent.size < 2 || before.isEmpty()) return@forEach
            val best = { l: List<Entry> -> l.flatMap { it.sets }.maxOf { e1rm(it.w, it.r) } }
            if (best(recent) <= best(before)) stalled += ex.muscle
        }
        stalled.filter { it !in focus }.forEach { m ->
            out += Tip(
                "stall_${m.id}", m, "${m.ar} واقفة بدون تطور",
                "3 أسابيع بدون زيادة في الوزن أو العدّات. تركيز أكثر (مجموعات زيادة) يحرّكها.", true,
            )
        }

        // 3) عضلة تنهمل: يتمرن بس هالعضلة مجموعاتها قليلة
        if (sessionsIn(14) >= 4) {
            listOf(Muscle.LEGS, Muscle.CHEST, Muscle.BACK, Muscle.SHOULDERS).forEach { m ->
                val n = s14[m] ?: 0
                if (n < 8 && m !in focus && out.none { it.muscle == m }) {
                    out += Tip(
                        "low_${m.id}", m, "${m.ar} مجموعاتها قليلة",
                        "سجّلت $n مجموعات بس آخر أسبوعين. أقل من 10 بالأسبوع ما يكفي للتطور (ACSM 2026).", true,
                    )
                }
            }
        }

        // 4) "ما أقدر" كثير لنفس العضلة
        val cantByMuscle = mutableMapOf<Muscle, Int>()
        Store.cant.forEach { (k, n) ->
            val m = Exercises.get(k.substringBefore(":"))?.muscle ?: return@forEach
            cantByMuscle[m] = (cantByMuscle[m] ?: 0) + n
        }
        cantByMuscle.filter { it.value >= 3 }.forEach { (m, n) ->
            out += Tip(
                "cant_${m.id}", m, "تضغط \"ما أقدر\" كثير في ${m.ar}",
                "ضغطته $n مرات. لو فيه ألم حدده في خريطة الإصابات، ولو الطريقة صعبة شوف الشرح.", false,
            )
        }

        return out.filter { !dismissed(it.id) }
    }

    // ===== "مو الحين" =====

    private fun dismissedMap(): Map<String, String> = runCatching {
        val o = JSONObject(P.coachDismissed)
        o.keys().asSequence().associateWith { o.getString(it) }
    }.getOrDefault(emptyMap())

    /** الاقتراح المرفوض يختفي أسبوعين. */
    private fun dismissed(id: String): Boolean {
        val d = dismissedMap()[id] ?: return false
        return Dates.daysBetween(d, Store.today()) < 14
    }

    fun dismiss(t: Tip) {
        val o = JSONObject()
        dismissedMap().forEach { (k, v) -> o.put(k, v) }
        o.put(t.id, Store.today())
        P.coachDismissed = o.toString()
    }

    /** المستخدم وافق: نضيف العضلة للتركيز. */
    fun accept(t: Tip) {
        val m = t.muscle ?: return
        if (m.id !in P.focus) P.focus = P.focus + m.id
        dismiss(t)
    }
}
