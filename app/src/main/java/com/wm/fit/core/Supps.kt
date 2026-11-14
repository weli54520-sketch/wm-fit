package com.wm.fit.core

import kotlin.math.roundToInt

/**
 * مكمل واحد ودرجة دليله.
 * [grade]: 0 = دليل قوي، 1 = حسب حالتك، 2 = ما ننصح فيه.
 * [daily] = يطلع له زر "أخذته اليوم".
 */
data class Supp(
    val id: String,
    val ar: String,
    val en: String,
    val grade: Int,
    val why: String,
    val source: String,
    val daily: Boolean,
)

/**
 * قسم المكملات. الرأي: أكل صحي + كرياتين، وبروتين لو أكلك ما يكفي.
 * مرض الكلى يقفل القسم كله، والأدوية الدايمة تطلع تنبيه.
 */
object Supps {
    val all = listOf(
        Supp("creatine", "كرياتين مونوهيدرات", "Creatine monohydrate", 0,
            "يزيد القوة والأداء في الحديد، ومن أكثر المكملات المدروسة وأأمنها للأصحاء.",
            "ISSN 2017", true),
        Supp("whey", "بروتين واي", "Whey protein", 0,
            "أكل مو سحر. يسهّل توصل لبروتينك لو أكلك ما يكفي.",
            "ISSN 2017", true),
        Supp("caffeine", "كافيين", "Caffeine", 0,
            "يحسّن الأداء والتركيز في التمرين.",
            "ISSN 2021", false),
        Supp("vitd", "فيتامين د", "Vitamin D", 1,
            "مفيد لو عندك نقص. لا تاخذه بدون تحليل دم.",
            "حسب تحليل الدم", false),
        Supp("omega3", "أوميغا 3", "Omega-3", 1,
            "لو ما تاكل سمك دهني (مثل السلمون) مرتين بالأسبوع.",
            "حسب أكلك", false),
        Supp("bcaa", "BCAA", "BCAA", 2,
            "لو بروتينك كافي، ما يضيف شي. فلوس على الفاضي.",
            "ISSN 2017", false),
        Supp("burners", "حارقات الدهون", "Fat burners", 2,
            "أغلبها كافيين غالي، وبعضها فيه مواد خطرة على القلب.",
            "مراجعات السلامة", false),
        Supp("testo", "رافعات التستوستيرون", "Testosterone boosters", 2,
            "ما فيه دليل إنها تزيد العضل عند الأصحاء.",
            "مراجعات علمية", false),
    )

    val locked: Boolean get() = P.sfKidney

    /** الجرعة أو النصيحة حسب وزنك وأكلك. */
    fun dose(s: Supp): String {
        val w = P.weightKg
        return when (s.id) {
            "creatine" -> "3 إلى 5 جم يومياً، أي وقت باليوم. ما يحتاج فترة تحميل."
            "whey" -> {
                val target = Calc.daily().protein
                val avg = Diary.avgProtein()
                when {
                    avg == null -> "سجّل أكلك 3 أيام، وأقولك تحتاجه أو لا."
                    avg < target - 20 -> "ناقصك تقريباً ${(target - avg).roundToInt()} جم باليوم. سكوب (24 جم) يكمّلها."
                    else -> "أكلك يكفي (${avg.roundToInt()} من $target جم). ما تحتاجه."
                }
            }
            "caffeine" -> if (P.heartRisk) "لا، لين تراجع طبيبك." else {
                // 3 إلى 6 مجم/كجم (ISSN 2021)، وما نتجاوز الحد العام 400 مجم باليوم
                val lo = (w * 3).roundToInt().coerceAtMost(400)
                val hi = (w * 6).roundToInt().coerceAtMost(400)
                "$lo إلى $hi مجم قبل التمرين بساعة. ابدأ بالأقل. والحد العام للأصحاء 400 مجم باليوم " +
                    "(كوب القهوة تقريباً 80 إلى 100 مجم)."
            }
            "vitd" -> "الجرعة من طبيبك بعد التحليل."
            "omega3" -> "اسأل الطبيب أو الصيدلي عن الجرعة."
            else -> "ما ننصح فيه."
        }
    }

    val gradeAr = listOf("دليل قوي", "حسب حالتك", "ما ننصح فيه")
}

/**
 * مراحل الهدف: تنشيف ← تثبيت أسبوعين ← تضخيم نظيف.
 * التطبيق يقترح الانتقال، وما ينتقل إلا بموافقة المستخدم. ويقدر يغيّر بنفسه أي وقت.
 */
object Stages {
    val names = listOf("تنشيف", "تثبيت", "تضخيم نظيف")
    val goals = listOf(Goal.CUT, Goal.MAINTAIN, Goal.BULK)

    const val MAINTAIN_DAYS = 14

    fun current(): Int = goals.indexOf(P.goal)

    data class Suggest(val to: Int, val why: String)

    fun suggestion(): Suggest? {
        val s = when (current()) {
            0 -> {
                val w = Diary.waists.lastOrNull()
                if (P.targetWaist > 0f && w != null && w.v <= P.targetWaist)
                    Suggest(1, "وصلت لخصرك المستهدف (${Workout.kg(P.targetWaist)} سم). " +
                        "أقترح أسبوعين تثبيت، بعدين تضخيم نظيف.")
                else null
            }
            1 -> {
                val days = if (P.stageSince.isBlank()) 0 else Dates.daysBetween(P.stageSince, Store.today())
                if (days >= MAINTAIN_DAYS) Suggest(2, "خلّصت $days يوم تثبيت. جاهز للتضخيم النظيف؟") else null
            }
            else -> null
        } ?: return null
        // "مو الحين" يخفي الاقتراح أسبوع
        val d = P.stageSnooze
        if (d.isNotBlank() && Dates.daysBetween(d, Store.today()) < 7) return null
        return s
    }

    fun move(to: Int) {
        P.goal = goals[to.coerceIn(0, 2)]
        P.stage = to
        P.stageSince = Store.today()
        P.stageSnooze = ""
    }

    fun snooze() {
        P.stageSnooze = Store.today()
    }
}
