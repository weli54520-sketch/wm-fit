package com.wm.fit.core

import java.util.Calendar

/**
 * تقسيم الأسبوع: أي يوم تتمرن، وأي عضلات.
 *
 * الأساس (ACSM 2026): كل عضلة مرتين بالأسبوع على الأقل، عشان كذا التقسيمات
 * كلها تكرر العضلة مرتين أو أكثر، ما فيه "يوم لكل عضلة مرة بالأسبوع".
 * المستخدم يختار أيامه، والجمعة راحة في الافتراضي.
 */
object Plan {

    val dayNamesAr = listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
    val dayShortAr = listOf("أحد", "إثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة", "سبت")

    /**
     * الأيام الافتراضية لو المستخدم ما اختار. 0 = الأحد.
     * الجمعة راحة دايماً في الافتراضي، والمستخدم يقدر يغيّر أيامه من الإعدادات.
     */
    fun defaultDays(perWeek: Int): List<Int> = when (perWeek.coerceIn(MIN_DAYS, MAX_DAYS)) {
        2 -> listOf(0, 3)
        3 -> listOf(0, 2, 4)
        4 -> listOf(0, 1, 3, 4)
        5 -> listOf(0, 1, 2, 3, 4)
        else -> listOf(0, 1, 2, 3, 4, 6)
    }

    /** أقل وأكثر عدد أيام. يومين = كل عضلة مرتين بجسم كامل. */
    const val MIN_DAYS = 2
    const val MAX_DAYS = 6

    /** أيام التمرين اللي اختارها المستخدم. */
    fun trainingDays(): List<Int> = P.trainDays

    /** جلسة تمرين واحدة. */
    data class Session(val title: String, val groups: List<Muscle>)

    private fun splits(perWeek: Int): List<Session> = when (perWeek) {
        2 -> listOf(
            Session("جسم كامل أ", listOf(Muscle.LEGS, Muscle.CHEST, Muscle.BACK, Muscle.SHOULDERS, Muscle.CORE)),
            Session("جسم كامل ب", listOf(Muscle.GLUTES, Muscle.BACK, Muscle.CHEST, Muscle.ARMS, Muscle.LEGS)),
        )
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
        6 -> listOf(
            Session("دفع", listOf(Muscle.CHEST, Muscle.SHOULDERS, Muscle.ARMS)),
            Session("سحب", listOf(Muscle.BACK, Muscle.ARMS, Muscle.FOREARMS)),
            Session("أرجل", listOf(Muscle.LEGS, Muscle.GLUTES, Muscle.CORE)),
            Session("دفع", listOf(Muscle.SHOULDERS, Muscle.CHEST, Muscle.ARMS)),
            Session("سحب", listOf(Muscle.BACK, Muscle.ARMS, Muscle.FOREARMS)),
            Session("أرجل", listOf(Muscle.GLUTES, Muscle.LEGS, Muscle.CORE)),
        )
        else -> listOf(
            Session("علوي", listOf(Muscle.CHEST, Muscle.BACK, Muscle.SHOULDERS, Muscle.ARMS)),
            Session("سفلي", listOf(Muscle.LEGS, Muscle.GLUTES, Muscle.CORE)),
            Session("علوي", listOf(Muscle.BACK, Muscle.CHEST, Muscle.SHOULDERS, Muscle.ARMS)),
            Session("سفلي", listOf(Muscle.LEGS, Muscle.GLUTES, Muscle.CORE)),
        )
    }

    /** جلسة هذا اليوم، أو null لو يوم راحة. 0 = الأحد. */
    fun sessionFor(dow: Int, days: List<Int> = trainingDays()): Session? {
        val i = days.indexOf(dow)
        if (i < 0) return null
        val s = splits(days.size)
        val base = s[i % s.size]
        // عضلة التركيز تنضاف لكل جلسة ثانية ما هي فيها، عشان تاخذ مجموعات أكثر
        val focus = P.focus.mapNotNull { Muscle.byId(it) }
        val extra = focus.firstOrNull { it !in base.groups && (i % 2 == 0) }
        val groups = if (extra != null) base.groups + extra else base.groups
        val title = if (focus.isNotEmpty() && focus.any { it in groups })
            base.title + " · تركيز" else base.title
        return Session(title, groups)
    }

    /** كم مرة العضلة تتمرن بالأسبوع، وترتيب هذا اليوم بينها (للتنويع بين الجلسات). */
    fun muscleHits(m: Muscle, dow: Int): Pair<Int, Int> {
        val days = trainingDays()
        val with = days.filter { sessionFor(it, days)?.groups?.contains(m) == true }
        return with.size.coerceAtLeast(1) to with.indexOf(dow).coerceAtLeast(0)
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
