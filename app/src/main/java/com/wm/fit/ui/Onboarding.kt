package com.wm.fit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.wm.fit.core.Calc
import com.wm.fit.core.Goal
import com.wm.fit.core.Muscle
import com.wm.fit.core.P

/** أسئلة أول مرة: 7 خطوات. */
@Composable
fun Onboarding(onDone: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val steps = 7

    Column(
        Modifier
            .fillMaxSize()
            .background(C.bg)
            .systemBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // شريط الخطوات
        if (step > 0) {
            Text(
                "خطوة $step من ${steps - 1}",
                style = MaterialTheme.typography.bodySmall,
                color = C.muted,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(steps - 1) { i ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (i < step) C.accent else C.track),
                    )
                }
            }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (step) {
                0 -> StepWelcome()
                1 -> StepBasics()
                2 -> StepGoal()
                3 -> StepFocus()
                4 -> StepTraining()
                5 -> StepSafety()
                6 -> StepInjuries()
            }
        }

        val ok = when (step) {
            1 -> P.age in 13..90 && P.heightCm in 120..230 && P.weightKg in 30f..300f
            else -> true
        }
        BigButton(
            text = when (step) {
                0 -> "يلا نبدأ"
                6 -> "فهمت، ابدأ خطتي"
                1 -> if (ok) "التالي" else "عبّ عمرك وطولك ووزنك"
                else -> "التالي"
            },
            enabled = ok,
        ) {
            if (step == steps - 1) {
                P.onboarded = true
                onDone()
            } else step++
        }
    }
}

@Composable
private fun Title(text: String, sub: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text, style = MaterialTheme.typography.headlineMedium)
        if (sub != null) Text(sub, style = MaterialTheme.typography.bodyMedium, color = C.muted)
    }
}

@Composable
private fun StepWelcome() {
    Spacer(Modifier.height(24.dp))
    Text("🏋️", style = MaterialTheme.typography.displayLarge)
    Title(
        "أهلاً فيك في WM Fit",
        "مدربك الشخصي. يعرف وضعك، ويبني لك خطة، ويعدّلها معك كل أسبوع.",
    )
    Card {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("🔒", style = MaterialTheme.typography.titleLarge)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("بياناتك في جوالك بس", style = MaterialTheme.typography.titleMedium)
                Text(
                    "ما فيه حساب ولا تسجيل دخول. وزنك وقياساتك وإصاباتك تنحفظ داخل جوالك، " +
                        "وما ترسل لأي مكان. حتى النسخة الاحتياطية ملف تحفظه أنت بنفسك.",
                    style = MaterialTheme.typography.bodySmall,
                    color = C.muted,
                )
            }
        }
    }
    Note(
        "WM Fit يعطيك إرشادات عامة مبنية على أبحاث، ومو بديل عن الطبيب " +
            "أو أخصائي العلاج الطبيعي.",
    )
}

@Composable
private fun NumField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = C.accent,
            unfocusedBorderColor = C.line,
            focusedContainerColor = C.card,
            unfocusedContainerColor = C.card,
            focusedLabelColor = C.accent,
            unfocusedLabelColor = C.muted,
            cursorColor = C.accent,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun StepBasics() {
    Title("عرّفني عليك", "أحتاجها عشان أحسب سعراتك وبروتينك بدقة.")

    var name by remember { mutableStateOf(P.name) }
    var age by remember { mutableStateOf(if (P.age > 0) P.age.toString() else "") }
    var h by remember { mutableStateOf(if (P.heightCm > 0) P.heightCm.toString() else "") }
    var w by remember { mutableStateOf(if (P.weightKg > 0f) P.weightKg.toString() else "") }

    OutlinedTextField(
        value = name,
        onValueChange = { name = it; P.name = it },
        label = { Text("اسمك") },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = C.accent,
            unfocusedBorderColor = C.line,
            focusedContainerColor = C.card,
            unfocusedContainerColor = C.card,
            focusedLabelColor = C.accent,
            unfocusedLabelColor = C.muted,
            cursorColor = C.accent,
        ),
        modifier = Modifier.fillMaxWidth(),
    )

    SectionLabel("الجنس")
    Segmented(
        options = listOf("m" to "ذكر", "f" to "أنثى"),
        selected = P.sex,
        onSelect = { P.sex = it },
    )

    NumField("العمر بالسنين", age) { v ->
        age = v.filter { it.isDigit() }
        P.age = age.toIntOrNull() ?: 0
    }
    NumField("الطول بالسنتيمتر", h) { v ->
        h = v.filter { it.isDigit() }
        P.heightCm = h.toIntOrNull() ?: 0
    }
    NumField("الوزن بالكيلو", w) { v ->
        w = v.filter { it.isDigit() || it == '.' }
        P.weightKg = w.toFloatOrNull() ?: 0f
    }
}

@Composable
private fun StepGoal() {
    Title("وش هدفك؟", "أبني الخطة عليه، وتقدر تغيّره أي وقت.")

    val goals = listOf(
        Triple(Goal.CUT, "تنشيف", "تنزل دهون وتحافظ على عضلك. بروتين عالي وحديد ثقيل."),
        Triple(Goal.BULK, "تضخيم", "تبني عضل بزيادة أكل بسيطة ومحسوبة."),
        Triple(Goal.FITNESS, "لياقة", "قلب أقوى ونفَس أطول، مع تمارين قوة تحافظ على عضلك."),
    )
    goals.forEach { (id, label, desc) ->
        val rate = Calc.weeklyRatePct(id)
        val rateText = when {
            rate < 0 -> "نزول 0.5 إلى 1٪ من وزنك بالأسبوع"
            rate > 0 -> "زيادة 0.25 إلى 0.5٪ من وزنك بالأسبوع"
            else -> "وزنك ثابت، والتركيز على الأداء"
        }
        Choice(
            text = label,
            sub = "$desc\n$rateText",
            selected = P.goal == id,
            modifier = Modifier.fillMaxWidth(),
        ) { P.goal = id }
    }

    Note(
        when (P.goal) {
            Goal.CUT -> "لو عندك عضل ودهون زايدة، هذا الصح: تنشيف، بعدين تثبيت، بعدين تضخيم نظيف. " +
                "وأسألك قبل كل انتقال."
            Goal.BULK -> "الزيادة السريعة أغلبها دهون. عشان كذا الفائض بسيط: 15٪ فوق احتياجك."
            else -> "أكلك يكون على احتياجك بالضبط، بدون عجز ولا فائض."
        },
    )
}

@Composable
private fun StepFocus() {
    Title("وش تبي تطوّر؟", "اختر عضلة أو أكثر. أزيد مجموعاتها وأتابع تطورها.")

    var sel by remember { mutableStateOf(P.focus.toSet()) }
    val subs = mapOf(
        Muscle.LEGS to "أمامي، خلفي، سمانة",
        Muscle.GLUTES to "للقوة والثبات",
        Muscle.CHEST to "علوي، وسط",
        Muscle.BACK to "عرض، سماكة",
        Muscle.SHOULDERS to "3 رؤوس",
        Muscle.ARMS to "بايسبس، ترايسبس",
        Muscle.CORE to "الجذع والثبات",
        Muscle.FOREARMS to "قوة القبضة",
    )

    Muscle.entries.chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { m ->
                Choice(
                    text = "${m.ar} · ${m.en}",
                    sub = subs[m],
                    selected = m.id in sel,
                    modifier = Modifier.weight(1f),
                ) {
                    sel = if (m.id in sel) sel - m.id else sel + m.id
                    P.focus = sel.toList()
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }

    Choice(
        text = "ولا شي — أبي خطة متوازنة",
        selected = sel.isEmpty(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        sel = emptySet()
        P.focus = emptyList()
    }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(C.card)
            .border(1.dp, C.line, RoundedCornerShape(16.dp))
            .clickable { P.coachSuggests = !P.coachSuggests }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(if (P.coachSuggests) C.accent else C.track),
            contentAlignment = Alignment.Center,
        ) {
            if (P.coachSuggests) Text("✓", color = C.onAccent, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            "لو لاحظت عضلة متأخرة، اقترح عليّ. والقرار لي.",
            style = MaterialTheme.typography.bodySmall,
            color = C.soft,
        )
    }
}

@Composable
private fun StepTraining() {
    Title("تمرينك", "كم يوم تقدر تلتزم؟ الصدق هنا أهم من الطموح.")

    SectionLabel("وش الأيام اللي تروح فيها النادي؟")
    DayPicker(P.trainDays) { P.trainDays = it }

    SectionLabel("مستواك في الحديد")
    Segmented(
        options = listOf("beg" to "مبتدئ", "mid" to "متوسط", "adv" to "متقدم"),
        selected = P.level,
        onSelect = { P.level = it },
    )

    SectionLabel("نشاطك اليومي غير التمرين")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            1.375f to ("قليل" to "دوام مكتبي، وأمشي شوي"),
            1.55f to ("متوسط" to "أتحرك كثير باليوم"),
            1.725f to ("عالي" to "شغل بدني أو مشي طويل"),
        ).forEach { (v, t) ->
            Choice(
                text = t.first,
                sub = t.second,
                selected = P.activity == v,
                modifier = Modifier.fillMaxWidth(),
            ) { P.activity = v }
        }
    }

    val sets = Calc.weeklySets(P.focus.isNotEmpty(), P.level)
    Note("خطتك: $sets مجموعة بالأسبوع لكل عضلة، وكل عضلة تتمرن مرتين بالأسبوع على الأقل.")
}

@Composable
private fun StepSafety() {
    Title("سلامتك أول", "أسئلة سريعة عشان خطتك تكون آمنة. إجاباتك تبقى في جوالك.")

    data class Q(val text: String, val get: () -> Boolean, val set: (Boolean) -> Unit)
    val qs = listOf(
        Q("عندك مشكلة في القلب، أو ضغط مو مضبوط؟", { P.sfHeart }, { P.sfHeart = it }),
        Q("يجيك ألم صدر أو دوخة مع المجهود؟", { P.sfChestPain }, { P.sfChestPain = it }),
        Q("عندك مرض في الكلى؟", { P.sfKidney }, { P.sfKidney = it }),
        Q("تاخذ أدوية بشكل دايم؟", { P.sfMeds }, { P.sfMeds = it }),
        Q("سويت عملية أو جاتك إصابة قوية آخر 6 شهور؟", { P.sfSurgery }, { P.sfSurgery = it }),
    )

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(C.card)
            .border(1.dp, C.line, RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp),
    ) {
        qs.forEachIndexed { i, q ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 64.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    q.text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                YesNo(q.get(), q.set)
            }
            if (i < qs.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(C.line))
        }
    }

    val verdict = when {
        P.heartRisk -> "راجع طبيبك قبل تبدأ. لين وقتها، أعطيك مشي وتمارين خفيفة بس." to true
        P.sfKidney -> "أقفل قسم المكملات، وأخلي البروتين على الحد المعتدل. استشر طبيبك عنه." to true
        P.sfSurgery -> "نبدأ بأوزان أخف، وحدد مكان الإصابة على خريطة الجسم." to true
        P.sfMeds -> "اسأل طبيبك أو الصيدلي قبل أي مكمل، خصوصاً الكافيين." to true
        else -> "ممتاز. تقدر تبدأ خطتك كاملة." to false
    }
    Note(verdict.first, warn = verdict.second)
}

@Composable
private fun StepInjuries() {
    Title(
        "عندك إصابة أو ألم؟",
        "اضغط على المكان، وحدد قوة الألم وهل هي جديدة أو مزمنة. لو ما عندك شي، اضغط التالي.",
    )
    InjuryMap()
}

@Composable
private fun YesNo(value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .width(112.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(C.bg)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(false to "لا", true to "إيه").forEach { (v, label) ->
            val on = v == value
            Box(
                Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        when {
                            on && v -> C.warn
                            on -> C.track
                            else -> Color.Transparent
                        },
                    )
                    .clickable { onChange(v) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        on && v -> C.onAccent
                        on -> C.text
                        else -> C.muted
                    },
                )
            }
        }
    }
}
