package com.wm.fit.core

import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * كل الحسابات. الأرقام من مصادر معتمدة، وكل رقم مكتوب جنبه مصدره.
 * شاشة "المصادر" في التطبيق تعرضها للمستخدم.
 */
object Calc {

    /** كم سعرة في كيلو الدهون (تقدير شائع). */
    private const val KCAL_PER_KG_FAT = 7700f

    /**
     * السعرات وقت الراحة (BMR) بمعادلة Mifflin-St Jeor.
     * المعادلة الأدق للأشخاص الأصحاء بدون قياس نسبة دهون.
     */
    fun bmr(male: Boolean, weightKg: Float, heightCm: Int, age: Int): Float =
        10f * weightKg + 6.25f * heightCm - 5f * age + if (male) 5f else -161f

    /** السعرات اليومية الكاملة قبل تعديل الهدف. */
    fun tdee(male: Boolean, weightKg: Float, heightCm: Int, age: Int, activity: Float): Float =
        bmr(male, weightKg, heightCm, age) * activity

    /**
     * نسبة تغيّر الوزن المستهدفة بالأسبوع (من وزن الجسم).
     * تنشيف: نزول 0.7٪ (المدى الآمن 0.5 إلى 1٪، ناخذ الوسط).
     * تضخيم: زيادة 0.35٪ (Iraki 2019: 0.25 إلى 0.5٪).
     * لياقة: ثابت.
     */
    fun weeklyRatePct(goal: String): Float = when (goal) {
        Goal.CUT -> -0.7f
        Goal.BULK -> 0.35f
        else -> 0f
    }

    /**
     * السعرات المستهدفة باليوم.
     *
     * التنشيف: العجز محسوب من نسبة النزول الأسبوعية، ومحدود بـ 25٪ من مجموع سعراتك
     * عشان ما يصير قاسي ويخسرك عضل.
     * التضخيم: فائض 15٪ (Iraki 2019: 10 إلى 20٪).
     */
    fun targetKcal(goal: String, tdee: Float, weightKg: Float, male: Boolean): Int {
        val k = when (goal) {
            Goal.CUT -> {
                val fromRate = (0.007f * weightKg) * KCAL_PER_KG_FAT / 7f
                tdee - min(fromRate, tdee * 0.25f)
            }
            Goal.BULK -> tdee * 1.15f
            else -> tdee
        }
        // حد أدنى للسلامة: ما ننزل تحته مهما كان الحساب.
        val floor = if (male) 1500f else 1200f
        return max(k, floor).roundToInt()
    }

    /**
     * البروتين بالجرام باليوم (ISSN 2017).
     * تنشيف: 2.3 جم/كجم (المدى 2.3 إلى 3.1 للمتمرنين وقت العجز).
     * تضخيم وتثبيت: 1.8 جم/كجم (المدى 1.6 إلى 2.2).
     * لياقة: 1.6 جم/كجم (الحد الأدنى من مدى 1.4 إلى 2.0).
     *
     * ملاحظة سلامة: لو عنده مرض كلى، ننزله للحد المعتدل ونقوله يستشير طبيب.
     */
    fun protein(goal: String, weightKg: Float, kidneyFlag: Boolean): Int {
        val perKg = when {
            kidneyFlag -> 1.2f
            goal == Goal.CUT -> 2.3f
            goal == Goal.BULK -> 1.8f
            goal == Goal.MAINTAIN -> 1.8f
            else -> 1.6f
        }
        return (weightKg * perKg).roundToInt()
    }

    /** الدهون: 25٪ من السعرات، وما تنزل عن 0.6 جم/كجم (الحد الصحي). */
    fun fat(kcal: Int, weightKg: Float): Int =
        max(kcal * 0.25f / 9f, weightKg * 0.6f).roundToInt()

    /** الكارب = الباقي بعد البروتين والدهون. */
    fun carbs(kcal: Int, proteinG: Int, fatG: Int): Int =
        max(0f, (kcal - proteinG * 4f - fatG * 9f) / 4f).roundToInt()

    /** الموية باللتر: 35 مل لكل كيلو. */
    fun waterL(weightKg: Float): Float = weightKg * 0.035f

    /** مؤشر كتلة الجسم. */
    fun bmi(weightKg: Float, heightCm: Int): Float =
        if (heightCm <= 0) 0f else weightKg / ((heightCm / 100f) * (heightCm / 100f))

    /**
     * نسبة الدهون التقريبية بمعادلة البحرية الأمريكية (الخصر والرقبة والطول).
     *
     * مهم: خطأ هذي المعادلة 3 إلى 4٪ (Hodgdon و Friedl 1999)، وما تتابع
     * التغيّر القصير بدقة. عشان كذا الرقم الأساسي عندنا هو مقاس الخصر نفسه،
     * وهذي تنعرض كـ "تقريبية" بس.
     *
     * ترجع null لو القياسات ناقصة أو غير منطقية.
     */
    fun bodyFatNavy(male: Boolean, waistCm: Float, neckCm: Float, heightCm: Int, hipCm: Float = 0f): Float? {
        if (heightCm <= 0 || waistCm <= 0f || neckCm <= 0f) return null
        val h = heightCm.toFloat()
        val v = if (male) {
            val d = waistCm - neckCm
            if (d <= 0f) return null
            495f / (1.0324f - 0.19077f * log10(d) + 0.15456f * log10(h)) - 450f
        } else {
            if (hipCm <= 0f) return null
            val d = waistCm + hipCm - neckCm
            if (d <= 0f) return null
            495f / (1.29579f - 0.35004f * log10(d) + 0.22100f * log10(h)) - 450f
        }
        return if (v.isNaN() || v < 2f || v > 70f) null else v
    }

    /**
     * كم أسبوع تقريباً للهدف، بناءً على نسبة التغيّر الأسبوعية.
     * ترجع null لو الهدف ما له اتجاه (لياقة) أو الأرقام ما تتفق.
     */
    fun weeksToTarget(goal: String, current: Float, target: Float): Int? {
        val rate = weeklyRatePct(goal)
        if (rate == 0f) return null
        val diff = target - current
        if (diff == 0f) return 0
        if ((diff > 0) != (rate > 0)) return null
        val perWeek = current * rate / 100f
        return kotlin.math.ceil(kotlin.math.abs(diff / perWeek)).toInt().coerceAtMost(260)
    }

    /**
     * عدد المجموعات الأسبوعية لكل عضلة (ACSM 2026).
     * الحد الأدنى للتضخيم تقريباً 10 مجموعات بالأسبوع، والفايدة تقل بعد 18 إلى 20.
     * عضلة التركيز تاخذ أكثر، وما نتجاوز 20 أبداً.
     */
    fun weeklySets(isFocus: Boolean, level: String): Int {
        val base = when (level) {
            "beg" -> 10
            "mid" -> 13
            else -> 15
        }
        return (if (isFocus) base + 5 else base).coerceIn(8, 20)
    }

    /** ملخص كل أرقام اليوم، جاهز للعرض. */
    data class Daily(
        val tdee: Int,
        val kcal: Int,
        val protein: Int,
        val carbs: Int,
        val fat: Int,
        val waterL: Float,
        val bmi: Float,
    )

    fun daily(): Daily {
        val male = P.sex == "m"
        val w = P.weightKg
        val t = tdee(male, w, P.heightCm, P.age, P.activity)
        val kcal = targetKcal(P.goal, t, w, male)
        val pr = protein(P.goal, w, P.sfKidney)
        val ft = fat(kcal, w)
        return Daily(
            tdee = t.roundToInt(),
            kcal = kcal,
            protein = pr,
            carbs = carbs(kcal, pr, ft),
            fat = ft,
            waterL = waterL(w),
            bmi = bmi(w, P.heightCm),
        )
    }
}
