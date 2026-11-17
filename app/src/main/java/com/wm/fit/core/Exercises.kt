package com.wm.fit.core

/** أماكن الجسم في خريطة الإصابات. المعرّف ثابت في التخزين. */
enum class Area(val id: String, val ar: String, val en: String) {
    NECK("neck", "الرقبة", "Neck"),
    SHOULDER("shoulder", "الكتف", "Shoulder"),
    CHEST("chest", "الصدر", "Chest"),
    UPPER_BACK("upper_back", "أعلى الظهر", "Upper back"),
    LOWER_BACK("lower_back", "أسفل الظهر", "Lower back"),
    ELBOW("elbow", "الكوع", "Elbow"),
    FOREARM("forearm", "الساعد", "Forearm"),
    WRIST("wrist", "الرسغ", "Wrist"),
    HIP("hip", "الورك", "Hip"),
    KNEE("knee", "الركبة", "Knee"),
    ANKLE("ankle", "الكاحل والقدم", "Ankle & foot");

    companion object {
        fun byId(id: String) = entries.firstOrNull { it.id == id }
    }
}

/** نوع الحركة، يحدد الرسمة المتحركة. */
enum class Move {
    SQUAT, GOBLET, HINGE, DEADLIFT, LUNGE, LEG_PRESS, LEG_EXT, LEG_CURL, CALF,
    HIP_THRUST, BRIDGE, BENCH, INCLINE, PUSHUP, FLY, PULLDOWN, PULLUP, ROW_SEATED,
    ROW_BENT, OHP, RAISE, FACE_PULL, REAR_FLY, CURL, PUSHDOWN, OH_EXT, PLANK,
    CRUNCH, KNEE_RAISE, CARRY, WRIST_CURL,
    LYING_CURL, HACK, DB_HINGE, STEP_UP, SL_HINGE, SEATED_CALF, SUMO, PULL_THROUGH, SL_BRIDGE,
    DB_BENCH, SEATED_PRESS, INCLINE_BAR, ROW_BAR, CHEST_ROW, SEATED_OHP, CABLE_RAISE, CABLE_OH_EXT,
    BAR_CURL, CABLE_CURL, INCLINE_CURL,
}

/**
 * تمرين واحد.
 * [stress] الأماكن اللي يضغط عليها، عشان نتجنبه أو نخففه لو فيها إصابة.
 * [inc] كم كيلو نزيد لما تتقدم. [body] بوزن الجسم (الوزن اختياري).
 * [reps] لو التمرين له عدّات خاصة (بالثواني أو الأمتار) بدل نظام الهدف.
 */
data class Ex(
    val id: String,
    val ar: String,
    val en: String,
    val muscle: Muscle,
    val compound: Boolean,
    val equip: String,
    val move: Move,
    val stress: Set<Area>,
    val steps: List<String>,
    val mistakes: List<String>,
    val inc: Float = 2.5f,
    val body: Boolean = false,
    val perSide: Boolean = false,
    val reps: String? = null,
) {
    val name: String get() = "$ar ($en)"
}

/** مكتبة التمارين: أشهر التمارين في النادي. */
object Exercises {

    private const val BAR = "بار"
    private const val DB = "دمبل"
    private const val MACHINE = "جهاز"
    private const val CABLE = "كيبل"
    private const val BODY = "وزن الجسم"

    val all: List<Ex> = listOf(
        // ===== الأرجل =====
        Ex(
            "squat", "سكوات بالبار", "Back Squat", Muscle.LEGS, true, BAR, Move.SQUAT,
            setOf(Area.KNEE, Area.HIP, Area.LOWER_BACK),
            listOf(
                "حط البار على أعلى ظهرك، مو على رقبتك.",
                "رجولك بعرض كتوفك، وأصابعك طالعة لبرا شوي.",
                "انزل كأنك بتجلس على كرسي، وصدرك مرفوع.",
                "انزل لين فخذك يوازي الأرض أو تحت شوي، واطلع بقوة.",
            ),
            listOf(
                "ركبك تدخل لجوا وقت الطلوع.",
                "كعبك يرتفع عن الأرض.",
                "ظهرك يتقوس لقدام في النزول.",
            ),
        ),
        Ex(
            "goblet", "سكوات بالدمبل", "Goblet Squat", Muscle.LEGS, true, DB, Move.GOBLET,
            setOf(Area.KNEE, Area.HIP),
            listOf(
                "امسك الدمبل قدام صدرك بيدينك الثنتين.",
                "رجولك أعرض من كتوفك شوي.",
                "انزل بين ركبك، وكوعك يمر جوا ركبك.",
                "اطلع وأنت تدفع الأرض بكعبك.",
            ),
            listOf(
                "تنحني لقدام والدمبل يبعد عن صدرك.",
                "تنزل نص نزلة بس.",
            ),
            inc = 2f,
        ),
        Ex(
            "leg_press", "ضغط الأرجل", "Leg Press", Muscle.LEGS, true, MACHINE, Move.LEG_PRESS,
            setOf(Area.KNEE, Area.HIP),
            listOf(
                "اجلس وظهرك لاصق بالمسند كله.",
                "رجولك بنص المنصة، بعرض كتوفك.",
                "نزّل المنصة لين ركبك تقرب من صدرك.",
                "ادفع بكعبك، ولا تقفل ركبك فوق.",
            ),
            listOf(
                "أسفل ظهرك يرتفع عن المسند في آخر النزلة.",
                "تقفل ركبك بقوة فوق.",
            ),
            inc = 5f,
        ),
        Ex(
            "lunge", "الطعنات", "Walking Lunge", Muscle.LEGS, true, DB, Move.LUNGE,
            setOf(Area.KNEE, Area.HIP, Area.ANKLE),
            listOf(
                "امسك دمبل بكل يد، وأنت واقف مستقيم.",
                "خذ خطوة طويلة لقدام.",
                "انزل لين ركبتك اللي ورا تقرب من الأرض.",
                "ادفع برجلك اللي قدام وكمّل بالرجل الثانية.",
            ),
            listOf(
                "الخطوة قصيرة، فالركبة تطلع كثير قدام أصابعك.",
                "جسمك يميل يمين ويسار.",
            ),
            inc = 2f, perSide = true,
        ),
        Ex(
            "rdl", "الرفعة الرومانية", "Romanian Deadlift", Muscle.LEGS, true, BAR, Move.HINGE,
            setOf(Area.LOWER_BACK, Area.HIP),
            listOf(
                "امسك البار وأنت واقف، ركبك مثنية شوي.",
                "ارجع بوركك لورا، والبار ينزل لاصق برجولك.",
                "انزل لين تحس بشد ورا فخذك (نص الساق تقريباً).",
                "ادفع وركك لقدام وارجع واقف.",
            ),
            listOf(
                "ظهرك يتقوس بدل ما يكون مستقيم.",
                "تثني ركبك كثير فتصير سكوات.",
                "البار يبعد عن رجولك.",
            ),
        ),
        Ex(
            "leg_ext", "تمديد الأرجل", "Leg Extension", Muscle.LEGS, false, MACHINE, Move.LEG_EXT,
            setOf(Area.KNEE),
            listOf(
                "ظبط المسند عشان ركبتك تكون على محور الجهاز.",
                "المخدة تحت أسفل ساقك، فوق الكاحل.",
                "مد رجولك لين تستقيم، واثبت ثانية.",
                "نزّل على مهلك.",
            ),
            listOf(
                "ترمي الوزن بسرعة وترفع وركك عن الكرسي.",
                "تنزّل الوزن بسرعة بدون تحكم.",
            ),
        ),
        Ex(
            "leg_curl", "ثني الأرجل", "Seated Leg Curl", Muscle.LEGS, false, MACHINE, Move.LEG_CURL,
            setOf(Area.KNEE),
            listOf(
                "ركبتك على محور الجهاز، والمخدة فوق كعبك.",
                "ثبّت فخذك بالمخدة اللي فوق.",
                "اسحب كعبك لتحت ولورا لآخر مدى.",
                "رجّع على مهلك.",
            ),
            listOf(
                "ترفع وركك عن الكرسي.",
                "تنص المدى وما تكمل الحركة.",
            ),
        ),
        Ex(
            "calf", "رفع السمانة", "Calf Raise", Muscle.LEGS, false, DB, Move.CALF,
            setOf(Area.ANKLE),
            listOf(
                "اوقف على طرف درجة، وكعبك برا.",
                "نزّل كعبك لتحت لين تحس بشد.",
                "ارتفع على أصابعك لأعلى نقطة، واثبت ثانية.",
            ),
            listOf(
                "تنط بسرعة وما تثبت فوق.",
                "تثني ركبك وتدفع فيها.",
            ),
        ),

        // ===== الأرداف =====
        Ex(
            "hip_thrust", "دفع الورك", "Hip Thrust", Muscle.GLUTES, true, BAR, Move.HIP_THRUST,
            setOf(Area.HIP),
            listOf(
                "أعلى ظهرك على طرف البنش، والبار على وركك (حط مخدة).",
                "رجولك على الأرض، بعرض كتوفك.",
                "ادفع وركك لفوق لين جسمك يصير مستقيم من الكتف للركبة.",
                "اعصر الأرداف فوق ثانية، ونزّل.",
            ),
            listOf(
                "تقوّس أسفل ظهرك فوق بدل ما تعصر الأرداف.",
                "رجولك بعيدة كثير، فالشغل يروح لورا الفخذ.",
            ),
            inc = 5f,
        ),
        Ex(
            "bulgarian", "السكوات البلغاري", "Bulgarian Split Squat", Muscle.GLUTES, true, DB, Move.LUNGE,
            setOf(Area.KNEE, Area.HIP, Area.ANKLE),
            listOf(
                "رجلك اللي ورا على البنش، واللي قدام بعيدة خطوة كبيرة.",
                "امسك دمبل بكل يد.",
                "انزل مستقيم لين فخذك اللي قدام يوازي الأرض.",
                "ادفع بكعب رجلك اللي قدام.",
            ),
            listOf(
                "رجلك اللي قدام قريبة كثير من البنش.",
                "تدفع برجلك اللي ورا.",
            ),
            inc = 2f, perSide = true,
        ),
        Ex(
            "glute_bridge", "جسر الأرداف", "Glute Bridge", Muscle.GLUTES, false, BODY, Move.BRIDGE,
            setOf(Area.HIP),
            listOf(
                "انسدح على ظهرك، ركبك مثنية ورجولك على الأرض.",
                "ادفع وركك لفوق وأنت تعصر الأرداف.",
                "اثبت ثانيتين فوق، ونزّل على مهلك.",
            ),
            listOf(
                "ترفع بظهرك مو بالأرداف.",
                "تنزّل بسرعة بدون تحكم.",
            ),
            body = true,
        ),

        // ===== الصدر =====
        Ex(
            "bench", "بنش بريس بالبار", "Bench Press", Muscle.CHEST, true, BAR, Move.BENCH,
            setOf(Area.SHOULDER, Area.ELBOW, Area.WRIST, Area.CHEST),
            listOf(
                "انسدح وعيونك تحت البار، ورجولك ثابتة على الأرض.",
                "امسك البار أعرض من كتوفك شوي.",
                "شد لوحك (كتوفك لورا ولتحت).",
                "نزّل البار لأسفل صدرك، وادفعه لفوق.",
            ),
            listOf(
                "كوعك مفتوح 90 درجة، وهذا يضغط على الكتف.",
                "البار ينط على صدرك.",
                "وركك يرتفع عن البنش.",
            ),
        ),
        Ex(
            "incline_db", "ضغط مائل بالدمبل", "Incline Dumbbell Press", Muscle.CHEST, true, DB, Move.INCLINE,
            setOf(Area.SHOULDER, Area.ELBOW, Area.WRIST),
            listOf(
                "خلّ البنش مائل 30 درجة تقريباً.",
                "الدمبلات عند صدرك، وكوعك تحت يدك.",
                "ادفع لفوق لين يدينك تقرب من بعض.",
                "نزّل على مهلك لين تحس بشد في الصدر.",
            ),
            listOf(
                "البنش مائل كثير، فالشغل يروح للكتف.",
                "تخبط الدمبلات ببعض فوق.",
            ),
            inc = 2f,
        ),
        Ex(
            "pushup", "الضغط", "Push-up", Muscle.CHEST, true, BODY, Move.PUSHUP,
            setOf(Area.SHOULDER, Area.WRIST, Area.ELBOW),
            listOf(
                "يدينك تحت كتوفك، وجسمك مستقيم من الراس للكعب.",
                "انزل لين صدرك يقرب من الأرض.",
                "كوعك بزاوية 45 درجة من جسمك.",
                "ادفع الأرض وارجع فوق.",
            ),
            listOf(
                "وركك ينزل أو يرتفع.",
                "تنزل نص المسافة بس.",
            ),
            body = true,
        ),
        Ex(
            "cable_fly", "تفتيح بالكيبل", "Cable Fly", Muscle.CHEST, false, CABLE, Move.FLY,
            setOf(Area.SHOULDER),
            listOf(
                "اوقف بين الكيبلين، خطوة لقدام.",
                "يدينك مفتوحة وكوعك مثني شوي وثابت.",
                "جيب يدينك قدامك كأنك تحضن شجرة.",
                "ارجع على مهلك لين تحس بشد في الصدر.",
            ),
            listOf(
                "تثني كوعك وتفرده، فتصير ضغط.",
                "ترجع لورا زيادة وتشد الكتف.",
            ),
        ),

        // ===== الظهر =====
        Ex(
            "pulldown", "سحب أمامي", "Lat Pulldown", Muscle.BACK, true, CABLE, Move.PULLDOWN,
            setOf(Area.SHOULDER, Area.ELBOW),
            listOf(
                "امسك البار أعرض من كتوفك، وثبّت فخذك تحت المخدة.",
                "ميّل لورا شوي، وصدرك مرفوع.",
                "اسحب البار لأعلى صدرك بكوعك، مو بيدك.",
                "رجّع على مهلك لين يدك تستقيم.",
            ),
            listOf(
                "تسحب البار ورا راسك.",
                "تتمرجح بجسمك عشان تسحب.",
            ),
        ),
        Ex(
            "pullup", "العقلة", "Pull-up", Muscle.BACK, true, BODY, Move.PULLUP,
            setOf(Area.SHOULDER, Area.ELBOW, Area.WRIST),
            listOf(
                "امسك البار أعرض من كتوفك شوي.",
                "اسحب نفسك لين ذقنك يعدّي البار.",
                "انزل على مهلك لين يدك تستقيم.",
                "لو صعبة، استخدم جهاز المساعدة أو مطاط.",
            ),
            listOf(
                "تتمرجح برجولك.",
                "ما تنزل لآخر مدى.",
            ),
            body = true,
        ),
        Ex(
            "cable_row", "سحب أرضي بالكيبل", "Seated Cable Row", Muscle.BACK, true, CABLE, Move.ROW_SEATED,
            setOf(Area.LOWER_BACK, Area.ELBOW),
            listOf(
                "اجلس وركبك مثنية شوي، وظهرك مستقيم.",
                "اسحب المقبض لبطنك، وكوعك قريب من جسمك.",
                "اعصر لوحك ثانية.",
                "رجّع على مهلك لين تحس بشد في ظهرك.",
            ),
            listOf(
                "تتمرجح لقدام ولورا بظهرك.",
                "ترفع كتوفك لأذونك.",
            ),
        ),
        Ex(
            "db_row", "سحب بالدمبل", "One-arm Dumbbell Row", Muscle.BACK, true, DB, Move.ROW_BENT,
            setOf(Area.LOWER_BACK, Area.ELBOW, Area.SHOULDER),
            listOf(
                "يدك وركبتك على البنش، وظهرك مستقيم.",
                "الدمبل نازل بيدك الثانية.",
                "اسحب الدمبل لجنب وركك، وكوعك قريب من جسمك.",
                "نزّل على مهلك.",
            ),
            listOf(
                "تلف جسمك عشان ترفع.",
                "تسحب لصدرك بدل وركك.",
            ),
            inc = 2f, perSide = true,
        ),
        Ex(
            "deadlift", "الرفعة الميتة", "Deadlift", Muscle.BACK, true, BAR, Move.DEADLIFT,
            setOf(Area.LOWER_BACK, Area.HIP, Area.KNEE, Area.FOREARM),
            listOf(
                "البار فوق نص رجلك، ورجولك بعرض الورك.",
                "انزل وامسك البار، ظهرك مستقيم وصدرك مرفوع.",
                "ادفع الأرض برجولك، والبار يطلع لاصق فيها.",
                "اوقف مستقيم فوق، بدون ما تميّل لورا.",
            ),
            listOf(
                "ظهرك يتقوس.",
                "وركك يطلع قبل البار.",
                "البار يبعد عن جسمك.",
            ),
            inc = 5f,
        ),

        // ===== الأكتاف =====
        Ex(
            "db_ohp", "ضغط أكتاف بالدمبل", "Dumbbell Shoulder Press", Muscle.SHOULDERS, true, DB, Move.OHP,
            setOf(Area.SHOULDER, Area.ELBOW),
            listOf(
                "اجلس أو اوقف، والدمبلات عند كتوفك.",
                "ادفع لفوق لين يدك تستقيم تقريباً.",
                "نزّل على مهلك لمستوى ذقنك.",
            ),
            listOf(
                "تقوّس ظهرك لورا.",
                "تنزّل لتحت زيادة بسرعة.",
            ),
            inc = 2f,
        ),
        Ex(
            "lateral", "رفرفة جانبي", "Lateral Raise", Muscle.SHOULDERS, false, DB, Move.RAISE,
            setOf(Area.SHOULDER),
            listOf(
                "اوقف والدمبلات جنبك، كوعك مثني شوي.",
                "ارفع يدينك على الجنب لين مستوى كتفك.",
                "نزّل على مهلك.",
            ),
            listOf(
                "تتمرجح بجسمك عشان ترفع.",
                "ترفع فوق مستوى الكتف.",
                "وزن ثقيل كثير. هذا التمرين يبي وزن خفيف.",
            ),
            inc = 1f,
        ),
        Ex(
            "face_pull", "سحب للوجه", "Face Pull", Muscle.SHOULDERS, false, CABLE, Move.FACE_PULL,
            setOf(Area.SHOULDER),
            listOf(
                "الكيبل بمستوى وجهك، وامسك الحبل.",
                "اسحب الحبل لوجهك، وافتح يدينك على الجنب.",
                "كوعك عالي، واعصر خلف كتفك.",
                "رجّع على مهلك.",
            ),
            listOf(
                "تسحب لرقبتك أو صدرك.",
                "تميّل لورا بجسمك.",
            ),
        ),
        Ex(
            "rear_fly", "رفرفة خلفي", "Rear Delt Fly", Muscle.SHOULDERS, false, DB, Move.REAR_FLY,
            setOf(Area.SHOULDER, Area.LOWER_BACK),
            listOf(
                "انحني لقدام وظهرك مستقيم، والدمبلات تحتك.",
                "افتح يدينك على الجنب، كوعك مثني شوي.",
                "ارفع لين مستوى جسمك، ونزّل على مهلك.",
            ),
            listOf(
                "تعصر لوحك بدل ما تشغّل خلف الكتف.",
                "ترفع جسمك مع كل عدّة.",
            ),
            inc = 1f,
        ),

        // ===== الذراعين =====
        Ex(
            "db_curl", "بايسبس بالدمبل", "Dumbbell Curl", Muscle.ARMS, false, DB, Move.CURL,
            setOf(Area.ELBOW, Area.WRIST),
            listOf(
                "اوقف والدمبلات جنبك، وكفك لقدام.",
                "ارفع الدمبل وكوعك ثابت جنبك.",
                "اعصر فوق، ونزّل على مهلك لين يدك تستقيم.",
            ),
            listOf(
                "تتمرجح بظهرك.",
                "كوعك يتحرك لقدام.",
            ),
            inc = 1f,
        ),
        Ex(
            "hammer", "هامر", "Hammer Curl", Muscle.ARMS, false, DB, Move.CURL,
            setOf(Area.ELBOW, Area.FOREARM),
            listOf(
                "امسك الدمبل وكفك لجسمك (مثل المطرقة).",
                "ارفع وكوعك ثابت جنبك.",
                "نزّل على مهلك.",
            ),
            listOf(
                "ترفع بكتفك.",
                "تنزّل بسرعة.",
            ),
            inc = 1f,
        ),
        Ex(
            "pushdown", "ترايسبس بالكيبل", "Triceps Pushdown", Muscle.ARMS, false, CABLE, Move.PUSHDOWN,
            setOf(Area.ELBOW),
            listOf(
                "امسك الحبل أو البار، وكوعك لاصق جنبك.",
                "ادفع لتحت لين يدك تستقيم.",
                "رجّع لين ساعدك يوازي الأرض.",
            ),
            listOf(
                "كوعك يتحرك لقدام ولورا.",
                "تميّل بجسمك فوق الكيبل.",
            ),
        ),
        Ex(
            "oh_ext", "ترايسبس خلف الراس", "Overhead Triceps Extension", Muscle.ARMS, false, DB, Move.OH_EXT,
            setOf(Area.ELBOW, Area.SHOULDER),
            listOf(
                "امسك دمبل بيدينك فوق راسك.",
                "نزّله ورا راسك، وكوعك يأشر لفوق.",
                "ارفعه لين يدك تستقيم.",
            ),
            listOf(
                "كوعك ينفتح على الجنب.",
                "تقوّس ظهرك.",
            ),
            inc = 2f,
        ),

        // ===== البطن =====
        Ex(
            "plank", "البلانك", "Plank", Muscle.CORE, false, BODY, Move.PLANK,
            setOf(Area.LOWER_BACK, Area.SHOULDER),
            listOf(
                "ساعدك على الأرض، وكوعك تحت كتفك.",
                "جسمك مستقيم من الراس للكعب.",
                "شد بطنك وأردافك، وتنفّس عادي.",
            ),
            listOf(
                "وركك ينزل فيضغط على أسفل ظهرك.",
                "وركك يرتفع لفوق.",
                "تحبس نفَسك.",
            ),
            body = true, reps = "30-45 ثانية",
        ),
        Ex(
            "cable_crunch", "كرنش بالكيبل", "Cable Crunch", Muscle.CORE, false, CABLE, Move.CRUNCH,
            setOf(Area.LOWER_BACK, Area.KNEE),
            listOf(
                "اجلس على ركبك قدام الكيبل، وامسك الحبل جنب راسك.",
                "انحني لتحت بعضلة بطنك، كأنك تقرب صدرك من وركك.",
                "رجّع على مهلك.",
            ),
            listOf(
                "تسحب بيدينك مو ببطنك.",
                "تجلس على كعبك بدل ما تنحني.",
            ),
        ),
        Ex(
            "knee_raise", "رفع الركب معلّق", "Hanging Knee Raise", Muscle.CORE, false, BODY, Move.KNEE_RAISE,
            setOf(Area.SHOULDER, Area.WRIST, Area.LOWER_BACK),
            listOf(
                "تعلّق بالبار، ويدك مستقيمة.",
                "ارفع ركبك لصدرك بدون تمرجح.",
                "نزّل على مهلك.",
            ),
            listOf(
                "تتمرجح وتستخدم الزخم.",
                "ترفع ركبك لنص المسافة بس.",
            ),
            body = true,
        ),

        // ===== الساعد =====
        Ex(
            "farmer", "مشي المزارع", "Farmer's Walk", Muscle.FOREARMS, true, DB, Move.CARRY,
            setOf(Area.WRIST, Area.FOREARM, Area.SHOULDER),
            listOf(
                "امسك دمبل ثقيل بكل يد.",
                "اوقف مستقيم وكتوفك لورا.",
                "امشِ بخطوات قصيرة وثابتة.",
            ),
            listOf(
                "تميّل لجهة وحدة.",
                "كتوفك تنزل لقدام.",
            ),
            inc = 2f, reps = "30-40 متر",
        ),
        Ex(
            "wrist_curl", "ثني الرسغ", "Wrist Curl", Muscle.FOREARMS, false, DB, Move.WRIST_CURL,
            setOf(Area.WRIST, Area.FOREARM),
            listOf(
                "اجلس وساعدك على فخذك، وكفك لفوق.",
                "خلّ الدمبل ينزل لتحت بأصابعك.",
                "ارفع رسغك لفوق، ونزّل على مهلك.",
            ),
            listOf(
                "ترفع ساعدك عن فخذك.",
                "وزن ثقيل كثير. الرسغ يبي وزن خفيف.",
            ),
            inc = 1f,
        ),
    ) + extraExercises

    private val byId = all.associateBy { it.id }

    fun get(id: String): Ex? = byId[id]

    fun forMuscle(m: Muscle): List<Ex> = all.filter { it.muscle == m }

    /** بدائل لنفس العضلة. لو [avoid] فيه مكان، نشيل التمارين اللي تضغط عليه. */
    fun alternatives(ex: Ex, avoid: Set<Area> = emptySet()): List<Ex> =
        forMuscle(ex.muscle)
            .filter { it.id != ex.id && it.stress.none { a -> a in avoid } }
            .sortedWith(compareByDescending<Ex> { it.compound == ex.compound }.thenBy { it.stress.size })
}
