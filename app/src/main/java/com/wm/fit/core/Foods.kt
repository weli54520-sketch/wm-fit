package com.wm.fit.core

/**
 * أكلة بحصتها. الأرقام لحصة واحدة.
 * [custom] = المستخدم أضافها بنفسه.
 */
data class Food(
    val id: String,
    val ar: String,
    val portion: String,
    val kcal: Int,
    val p: Float,
    val c: Float,
    val f: Float,
    val group: String,
    val custom: Boolean = false,
)

/**
 * قائمة أكلات شائعة.
 *
 * الأرقام **تقريبية**: الأكل الصافي من USDA FoodData Central، والأكلات الجاهزة
 * (شاورما، كبسة، مندي...) تختلف كثير حسب المطعم والحصة، فهي تقدير متوسط.
 * المستخدم يقدر يضيف أكلته بأرقامها الدقيقة.
 */
object Foods {

    private const val GRAIN = "نشويات"
    private const val PROTEIN = "بروتين"
    private const val DAIRY = "ألبان"
    private const val FRUIT = "فواكه"
    private const val VEG = "خضار"
    private const val MEAL = "وجبات جاهزة"
    private const val FAT = "مكسرات ودهون"
    private const val DRINK = "مشروبات وحلا"

    val groups = listOf(PROTEIN, GRAIN, DAIRY, FRUIT, VEG, MEAL, FAT, DRINK)

    val all: List<Food> = listOf(
        // ===== بروتين =====
        Food("chicken_breast", "صدر دجاج مشوي", "100 جم", 165, 31f, 0f, 3.6f, PROTEIN),
        Food("chicken_quarter", "ربع دجاج مشوي بالجلد", "ربع", 400, 40f, 0f, 26f, PROTEIN),
        Food("egg", "بيض", "حبة كبيرة", 72, 6.3f, 0.4f, 4.8f, PROTEIN),
        Food("egg_white", "بياض بيض", "حبة", 17, 3.6f, 0.2f, 0f, PROTEIN),
        Food("beef", "لحم بقري قليل الدهن", "100 جم مطبوخ", 220, 26f, 0f, 12f, PROTEIN),
        Food("lamb", "لحم غنم", "100 جم مطبوخ", 290, 25f, 0f, 21f, PROTEIN),
        Food("tuna_water", "تونة بالماء", "علبة (120 جم)", 130, 29f, 0f, 1f, PROTEIN),
        Food("tuna_oil", "تونة بالزيت", "علبة (120 جم)", 230, 27f, 0f, 13f, PROTEIN),
        Food("fish", "سمك مشوي (هامور)", "100 جم", 120, 24f, 0f, 1.5f, PROTEIN),
        Food("salmon", "سلمون", "100 جم", 210, 22f, 0f, 13f, PROTEIN),
        Food("shrimp", "روبيان مطبوخ", "100 جم", 100, 24f, 0f, 0.3f, PROTEIN),
        Food("liver", "كبدة", "100 جم", 175, 27f, 5f, 5f, PROTEIN),
        Food("foul", "فول مدمس بدون زيت", "كوب", 190, 13f, 33f, 1f, PROTEIN),
        Food("foul_oil", "صحن فول بالزيت", "صحن", 320, 14f, 35f, 14f, PROTEIN),
        Food("lentils", "عدس مطبوخ", "كوب", 230, 18f, 40f, 0.8f, PROTEIN),
        Food("chickpeas", "حمص مسلوق", "كوب", 270, 14.5f, 45f, 4f, PROTEIN),
        Food("whey", "بروتين واي", "سكوب (30 جم)", 120, 24f, 3f, 1.5f, PROTEIN),

        // ===== نشويات =====
        Food("rice", "رز أبيض مطبوخ", "كوب", 205, 4.3f, 45f, 0.4f, GRAIN),
        Food("rice_kabsa", "رز كبسة (بالزيت)", "كوب", 280, 6f, 45f, 8f, GRAIN),
        Food("samoli", "خبز صامولي", "حبة", 160, 5f, 30f, 2f, GRAIN),
        Food("arabic_bread", "خبز عربي بر", "رغيف", 165, 6f, 33f, 1.5f, GRAIN),
        Food("toast_white", "توست أبيض", "شريحة", 75, 2.5f, 14f, 1f, GRAIN),
        Food("toast_brown", "توست أسمر", "شريحة", 70, 3.5f, 12f, 1f, GRAIN),
        Food("oats", "شوفان", "نص كوب جاف (40 جم)", 150, 5f, 27f, 2.5f, GRAIN),
        Food("pasta", "مكرونة مطبوخة", "كوب", 220, 8f, 43f, 1.3f, GRAIN),
        Food("potato", "بطاطس مسلوقة", "حبة وسط", 150, 4f, 34f, 0.2f, GRAIN),
        Food("fries", "بطاطس مقلية", "صحن وسط", 365, 4f, 48f, 17f, GRAIN),
        Food("cornflakes", "كورن فليكس", "كوب", 100, 2f, 24f, 0.2f, GRAIN),

        // ===== ألبان =====
        Food("milk_full", "حليب كامل الدسم", "كوب", 150, 8f, 12f, 8f, DAIRY),
        Food("milk_low", "حليب قليل الدسم", "كوب", 105, 8.5f, 12f, 2.5f, DAIRY),
        Food("laban", "لبن كامل الدسم", "كوب", 150, 8f, 11f, 8f, DAIRY),
        Food("greek_yogurt", "زبادي يوناني قليل الدسم", "علبة (170 جم)", 100, 17f, 6f, 0.7f, DAIRY),
        Food("cheddar", "جبنة شيدر", "شريحة", 80, 5f, 0.3f, 6.6f, DAIRY),
        Food("feta", "جبنة بيضاء", "30 جم", 75, 4f, 1f, 6f, DAIRY),
        Food("labneh", "لبنة", "ملعقتين كبار", 50, 2.5f, 1.5f, 4f, DAIRY),
        Food("cottage", "جبنة قريش قليلة الدسم", "نص كوب", 90, 12f, 4f, 2.5f, DAIRY),

        // ===== فواكه =====
        Food("dates", "تمر", "3 حبات", 70, 0.6f, 18f, 0f, FRUIT),
        Food("banana", "موز", "حبة وسط", 105, 1.3f, 27f, 0.4f, FRUIT),
        Food("apple", "تفاح", "حبة وسط", 95, 0.5f, 25f, 0.3f, FRUIT),
        Food("orange", "برتقال", "حبة وسط", 62, 1.2f, 15f, 0.2f, FRUIT),
        Food("grapes", "عنب", "كوب", 104, 1f, 27f, 0.2f, FRUIT),
        Food("watermelon", "حبحب", "كوب مكعبات", 46, 0.9f, 11.5f, 0.2f, FRUIT),

        // ===== خضار =====
        Food("salad", "سلطة خضار بدون صوص", "صحن", 35, 1.5f, 7f, 0.3f, VEG),
        Food("salad_oil", "سلطة بزيت زيتون", "صحن", 155, 1.5f, 7f, 14f, VEG),
        Food("cucumber", "خيار", "حبة", 30, 1.3f, 7f, 0.2f, VEG),
        Food("tomato", "طماطم", "حبة", 22, 1f, 5f, 0.2f, VEG),
        Food("hummus", "حمص بطحينة", "ملعقتين كبار", 50, 2.4f, 4f, 3f, VEG),
        Food("lentil_soup", "شوربة عدس", "صحن", 180, 10f, 28f, 3f, VEG),

        // ===== وجبات جاهزة (تقدير متوسط) =====
        Food("shawarma_chicken", "شاورما دجاج", "ساندويتش", 450, 25f, 40f, 20f, MEAL),
        Food("shawarma_meat", "شاورما لحم", "ساندويتش", 500, 25f, 40f, 26f, MEAL),
        Food("burger", "برجر لحم", "ساندويتش", 500, 25f, 40f, 25f, MEAL),
        Food("pizza", "بيتزا جبن", "شريحة كبيرة", 285, 12f, 36f, 10f, MEAL),
        Food("falafel", "فلافل", "4 حبات", 225, 9f, 21f, 12f, MEAL),
        Food("sambosa", "سمبوسة لحم مقلية", "حبة", 150, 5f, 12f, 9f, MEAL),
        Food("kabsa", "كبسة دجاج", "صحن (رز + ربع دجاج)", 750, 40f, 85f, 25f, MEAL),
        Food("mandi", "مندي لحم", "صحن", 900, 45f, 85f, 40f, MEAL),
        Food("mutabbaq", "مطبق", "حبة", 400, 15f, 35f, 22f, MEAL),

        // ===== مكسرات ودهون =====
        Food("almonds", "لوز", "حفنة (28 جم)", 165, 6f, 6f, 14f, FAT),
        Food("peanuts", "فول سوداني", "حفنة (28 جم)", 160, 7f, 5f, 14f, FAT),
        Food("peanut_butter", "زبدة فول سوداني", "ملعقة كبيرة", 95, 3.5f, 3.5f, 8f, FAT),
        Food("olive_oil", "زيت زيتون", "ملعقة كبيرة", 120, 0f, 0f, 14f, FAT),
        Food("avocado", "أفوكادو", "نص حبة", 120, 1.5f, 6f, 11f, FAT),

        // ===== مشروبات وحلا =====
        Food("tea_sugar", "شاي بسكر", "كاسة (ملعقة سكر)", 35, 0f, 9f, 0f, DRINK),
        Food("arabic_coffee", "قهوة عربية", "فنجال", 2, 0f, 0f, 0f, DRINK),
        Food("soda", "مشروب غازي", "علبة", 140, 0f, 37f, 0f, DRINK),
        Food("orange_juice", "عصير برتقال", "كوب", 110, 2f, 26f, 0.5f, DRINK),
        Food("kunafa", "كنافة", "قطعة", 400, 7f, 45f, 22f, DRINK),
        Food("chocolate", "شوكولاتة", "بار (45 جم)", 235, 3f, 26f, 13f, DRINK),
        Food("honey", "عسل", "ملعقة كبيرة", 64, 0f, 17f, 0f, DRINK),
    )

    /** كل الأكلات: القائمة + اللي أضافها المستخدم. */
    fun everything(): List<Food> = Diary.customFoods + all

    fun search(q: String): List<Food> {
        val t = q.trim()
        val list = everything()
        if (t.isEmpty()) return list
        return list.filter { it.ar.contains(t) || it.group.contains(t) }
    }
}
