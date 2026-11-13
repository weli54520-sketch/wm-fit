package com.wm.fit.core

import java.util.Calendar

/**
 * تقسيم الأسبوع: أي يوم تتمرن، وأي عضلات.
 *
 * الأساس (ACSM 2026): كل عضلة مرتين بالأسبوع على الأقل، عشان كذا التقسيمات
 * كلها تكرر العضلة مرتين أو أكثر، ما فيه "يوم لكل عضلة مرة بالأسبوع".
 * الجمعة والسبت راحة افتراضياً (عطلة السعودية).
 */
object Plan {

    val dayNamesAr = listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
    val dayShortAr = listOf("أحد", "إثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة", "سبت")

    /** أيام التمرين في الأسبوع. 0 = الأحد. */
    fun trainingDays(perWeek: Int): List<Int> = when (perWeek) {
        3 -> listOf(0, 2, 4)
        5 -> listOf(0, 1, 2, 4, 5)
        else -> listOf(0, 1, 3, 4)
    }

    /** جلسة تمرين واحدة. */
    data class Session(val title: String, val groups: List<Muscle>)

    private fun splits(perWeek: Int): List<Session> = when (perWeek) {
        3 -> listOf(
            Session("جسم كامل أ", listOf(Muscle.LEGS, Muscle.CHEST, Muscle.BACK, Muscle.CORE)),
            Session("جسم كامل ب", listOf(Muscle.LEGS, Muscle.SHOULDERS, Muscle.BACK, Muscle.ARMS)),
            Session("جسم كامل ج", listOf(Muscle.LEGS, Muscle.GLUTES, Muscle.CHEST, Muscle.CORE)),
        )
        5 -> listOf(
            Session("دفع", listOf(Muscle.CHEST, Muscle.SHOULDERS, Muscle.ARMS)),
            Session("سحب", listOf(Muscle.BACK, Muscle.ARMS, Muscle.FOREARMS)),
            Session("أرجل", listOf(Muscle.LEGS, Muscle.GLUTES, Muscle.CORE)),
            Session("علوي", listOf(Muscle.CHEST, Muscle.BACK, Muscle.SHOULDERS)),
            Session("سفلي", listOf(Muscle.LEGS, Muscle.GLUTES, Muscle.CORE)),
        )
        else -> listOf(
            Session("علوي", listOf(Muscle.CHEST, Muscle.BACK, Muscle.SHOULDERS, Muscle.ARMS)),
            Session("سفلي", listOf(Muscle.LEGS, Muscle.GLUTES, Muscle.CORE)),
            Session("علوي", listOf(Muscle.BACK, Muscle.CHEST, Muscle.SHOULDERS, Muscle.ARMS)),
            Session("سفلي", listOf(Muscle.LEGS, Muscle.GLUTES, Muscle.CORE)),
        )
    }

    /** جلسة هذا اليوم، أو null لو يوم راحة. 0 = الأحد. */
    fun sessionFor(dow: Int, perWeek: Int = P.daysPerWeek): Session? {
        val days = trainingDays(perWeek)
        val i = days.indexOf(dow)
        if (i < 0) return null
        val s = splits(perWeek)
        val base = s[i % s.size]
        // عضلة التركيز تنضاف لأول جلسة ما هي فيها، عشان تاخذ مجموعات أكثر
        val focus = P.focus.mapNotNull { Muscle.byId(it) }
        val extra = focus.firstOrNull { it !in base.groups && (i % 2 == 0) }
        val groups = if (extra != null) base.groups + extra else base.groups
        val title = if (focus.isNotEmpty() && focus.any { it in groups })
            base.title + " · تركيز" else base.title
        return Session(title, groups)
    }

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
        Goal.BULK -> Scheme(
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
        else -> "150 دقيقة كارديو متوسط بالأسبوع، أو 75 دقيقة شديد."
    }
}
