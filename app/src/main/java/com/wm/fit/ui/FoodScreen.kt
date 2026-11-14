package com.wm.fit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.wm.fit.core.Calc
import com.wm.fit.core.Diary
import com.wm.fit.core.Food
import com.wm.fit.core.Foods
import com.wm.fit.core.P
import com.wm.fit.core.Supp
import com.wm.fit.core.Supps
import com.wm.fit.core.Workout
import kotlin.math.roundToInt

/** الأكل اليوم: الأرقام مقابل هدفك، الوجبات، الموية، والمكملات. */
@Composable
fun FoodScreen(nav: Nav) {
    val d = Calc.daily()
    val t = Diary.totals()
    val eaten = Diary.eatenOn()
    val glassesTarget = (d.waterL * 4).roundToInt().coerceAtLeast(4)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Header("أكلك اليوم", sub = stageLabel())

        Card {
            val left = d.kcal - t.kcal
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${t.kcal}", style = MaterialTheme.typography.displayLarge)
                Text("من ${d.kcal} سعرة", style = MaterialTheme.typography.bodyMedium, color = C.muted,
                    modifier = Modifier.padding(bottom = 6.dp))
            }
            Bar(t.kcal / d.kcal.coerceAtLeast(1).toFloat(), if (left < 0) C.warn else C.accent)
            Text(
                if (left >= 0) "باقي لك $left سعرة" else "زدت ${-left} سعرة عن هدفك اليوم",
                style = MaterialTheme.typography.bodySmall,
                color = if (left >= 0) C.muted else C.warnText,
            )
            MacroRow("بروتين", t.p, d.protein, C.protein)
            MacroRow("كارب", t.c, d.carbs, C.carbs)
            MacroRow("دهون", t.f, d.fat, C.fat)
        }

        BigButton("أضف أكل") { nav.push(Over.AddFood) }

        if (eaten.isNotEmpty()) {
            Card {
                Text("اللي أكلته اليوم", style = MaterialTheme.typography.titleMedium)
                eaten.forEachIndexed { i, e ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                e.name + if (e.n != 1f) " × ${Workout.kg(e.n)}" else "",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                "${e.kcal} سعرة · بروتين ${e.p.roundToInt()} جم",
                                style = MaterialTheme.typography.labelSmall,
                                color = C.muted,
                            )
                        }
                        Text(
                            "✕",
                            color = C.muted,
                            modifier = Modifier.clip(CircleShape).clickable { Diary.removeEaten(i) }.padding(8.dp),
                        )
                    }
                }
            }
        }

        // الموية
        Card {
            val g = Diary.glasses()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("الموية 💧", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "$g من $glassesTarget كاسات (الكاسة 250 مل)",
                        style = MaterialTheme.typography.bodySmall,
                        color = C.muted,
                    )
                }
                OutlineButton("−") { Diary.setGlasses(g - 1) }
                OutlineButton("+", color = C.accent) { Diary.setGlasses(g + 1) }
            }
            Bar(g / glassesTarget.toFloat(), C.carbs)
        }

        // المكملات اليومية
        Card(onClick = { nav.push(Over.Supps) }) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("المكملات", style = MaterialTheme.typography.titleMedium)
                Text("›", style = MaterialTheme.typography.titleMedium, color = C.muted)
            }
            if (Supps.locked) {
                Text("مقفلة لأنك قلت عندك مرض كلى. استشر طبيبك.", style = MaterialTheme.typography.bodySmall,
                    color = C.warnText)
            } else {
                Supps.all.filter { it.daily }.forEach { s -> SuppCheck(s) }
            }
        }

        Note("أرقام الأكل تقريبية، خصوصاً الوجبات الجاهزة. لو تعرف الرقم الدقيق، أضف أكلتك بنفسك.")
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun MacroRow(label: String, have: Float, target: Int, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text("${have.roundToInt()} / $target جم", style = MaterialTheme.typography.bodySmall, color = C.muted)
        }
        Bar(have / target.coerceAtLeast(1).toFloat(), color, height = 6)
    }
}

@Composable
private fun SuppCheck(s: Supp) {
    val on = Diary.tookToday(s.id)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { Diary.toggleSupp(s.id) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier.size(24.dp).clip(RoundedCornerShape(7.dp)).background(if (on) C.accent else C.track),
            contentAlignment = Alignment.Center,
        ) { if (on) Text("✓", color = C.onAccent, style = MaterialTheme.typography.bodySmall) }
        Text(s.ar + if (on) " · أخذته اليوم" else "", style = MaterialTheme.typography.bodyMedium)
    }
}

/** إضافة أكل: بحث في القائمة، أو أكلة جديدة. */
@Composable
fun AddFoodScreen(onBack: () -> Unit) {
    var q by remember { mutableStateOf("") }
    var group by remember { mutableStateOf<String?>(null) }
    var pick by remember { mutableStateOf<Food?>(null) }
    var newFood by remember { mutableStateOf(false) }
    val list = Foods.search(q).filter { group == null || it.group == group }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Header("أضف أكل", onBack = onBack)
        Field(q, "ابحث: رز، دجاج، تمر...", Modifier.fillMaxWidth()) { q = it }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (listOf<String?>(null) + (if (Diary.customFoods.isNotEmpty()) listOf("أكلاتي") else emptyList()) + Foods.groups)
                .forEach { g ->
                    val on = g == group
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (on) C.accentBg else C.card)
                            .border(1.dp, if (on) C.accent else C.line, RoundedCornerShape(12.dp))
                            .clickable { group = g }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) { Text(g ?: "الكل", style = MaterialTheme.typography.bodySmall, color = if (on) C.accent else C.muted) }
                }
        }
        OutlineButton("+ أكلة مو موجودة؟ أضفها بنفسك", Modifier.fillMaxWidth(), C.accent) { newFood = true }
        list.forEach { f ->
            Card(radius = 18, onClick = { pick = f }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(f.ar, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${f.portion} · ${f.kcal} سعرة · بروتين ${Workout.kg(f.p)} جم",
                            style = MaterialTheme.typography.labelSmall,
                            color = C.muted,
                        )
                    }
                    if (f.custom) {
                        Text("حذف", style = MaterialTheme.typography.labelSmall, color = C.muted,
                            modifier = Modifier.clickable { Diary.removeCustomFood(f.id) }.padding(6.dp))
                    }
                }
            }
        }
        if (list.isEmpty()) Text("ما لقيت شي. أضفها بنفسك من الزر فوق.", color = C.muted,
            style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(40.dp))
    }

    pick?.let { f -> PortionDialog(f, onClose = { pick = null }, onAdded = { pick = null; onBack() }) }
    if (newFood) NewFoodDialog { newFood = false }
}

@Composable
private fun PortionDialog(f: Food, onClose: () -> Unit, onAdded: () -> Unit) {
    var n by remember { mutableStateOf(1f) }
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = C.card,
        title = { Text(f.ar) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("الحصة: ${f.portion}", style = MaterialTheme.typography.bodySmall, color = C.muted)
                SectionLabel("كم حصة؟")
                Segmented(listOf(0.5f to "نص", 1f to "1", 1.5f to "1.5", 2f to "2", 3f to "3"), n) { n = it }
                Text(
                    "${(f.kcal * n).roundToInt()} سعرة · بروتين ${(f.p * n).roundToInt()} · " +
                        "كارب ${(f.c * n).roundToInt()} · دهون ${(f.f * n).roundToInt()} جم",
                    style = MaterialTheme.typography.bodyMedium,
                )
                BigButton("أضف") { Diary.eat(f, n); onAdded() }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("إلغاء", color = C.muted) } },
    )
}

@Composable
private fun NewFoodDialog(onClose: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var portion by remember { mutableStateOf("حصة") }
    var k by remember { mutableStateOf("") }
    var p by remember { mutableStateOf("") }
    var c by remember { mutableStateOf("") }
    var f by remember { mutableStateOf("") }
    val kcal = k.toIntOrNull()
    val ok = name.isNotBlank() && kcal != null && kcal in 0..5000
    fun num(s: String) = s.filter { it.isDigit() || it == '.' }.take(6)

    AlertDialog(
        onDismissRequest = onClose,
        containerColor = C.card,
        title = { Text("أكلة جديدة") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(name, "اسم الأكلة", Modifier.fillMaxWidth()) { name = it.take(40) }
                Field(portion, "الحصة (مثل: صحن، 100 جم)", Modifier.fillMaxWidth()) { portion = it.take(30) }
                Field(k, "السعرات", Modifier.fillMaxWidth(), number = true) { k = num(it) }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Field(p, "بروتين جم", Modifier.weight(1f), number = true) { p = num(it) }
                    Field(c, "كارب جم", Modifier.weight(1f), number = true) { c = num(it) }
                    Field(f, "دهون جم", Modifier.weight(1f), number = true) { f = num(it) }
                }
                Text("تلقى الأرقام على علبة الأكل (جدول القيمة الغذائية).", style = MaterialTheme.typography.labelSmall,
                    color = C.muted)
                BigButton("احفظها", enabled = ok) {
                    Diary.addCustomFood(name.trim(), portion.trim().ifBlank { "حصة" }, kcal!!,
                        p.toFloatOrNull() ?: 0f, c.toFloatOrNull() ?: 0f, f.toFloatOrNull() ?: 0f)
                    onClose()
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("إلغاء", color = C.muted) } },
    )
}

/** حقل كتابة بسيط بنفس شكل التطبيق. */
@Composable
fun Field(value: String, label: String, modifier: Modifier = Modifier, number: Boolean = false, onChange: (String) -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(C.bg)
            .border(1.dp, C.line, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = C.muted)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = C.text),
            cursorBrush = SolidColor(C.accent),
            keyboardOptions = KeyboardOptions(keyboardType = if (number) KeyboardType.Decimal else KeyboardType.Text),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** شاشة المكملات: درجة الدليل، الجرعة حسب وزنك، والمصدر. */
@Composable
fun SuppsScreen(onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Header("المكملات", sub = "أكل صحي أول، والمكمل يكمّل بس", onBack = onBack)

        if (Supps.locked) {
            Note("قلت إن عندك مرض في الكلى، فقفلنا المكملات. استشر طبيبك قبل أي مكمل.", warn = true)
        } else {
            if (P.sfMeds) Note("تاخذ أدوية دايمة: اسأل طبيبك أو الصيدلي قبل أي مكمل، خصوصاً الكافيين.", warn = true)
            Note("رأينا: أكل صحي + كرياتين، وبروتين لو أكلك ما يكفي. والباقي حسب حالتك.")
            (0..2).forEach { g ->
                SectionLabel(Supps.gradeAr[g])
                Supps.all.filter { it.grade == g }.forEach { s ->
                    Card(radius = 20) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(s.ar, style = MaterialTheme.typography.titleMedium,
                                    color = if (g == 2) C.muted else C.text)
                                Text(s.en, style = MaterialTheme.typography.labelSmall, color = C.muted)
                            }
                            Text(
                                Supps.gradeAr[g],
                                style = MaterialTheme.typography.labelSmall,
                                color = when (g) { 0 -> C.accent; 1 -> C.fat; else -> C.bad },
                            )
                        }
                        Text(s.why, style = MaterialTheme.typography.bodySmall, color = C.soft)
                        if (g < 2) Text(Supps.dose(s), style = MaterialTheme.typography.bodyMedium, color = C.accent)
                        Text("المصدر: ${s.source}", style = MaterialTheme.typography.labelSmall, color = C.muted)
                        if (s.daily) SuppCheck(s)
                    }
                }
            }
        }
        Note("استشر طبيبك لو عندك مرض أو تاخذ أدوية.")
        Spacer(Modifier.height(40.dp))
    }
}
