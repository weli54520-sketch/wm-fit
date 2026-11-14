package com.wm.fit.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.wm.fit.core.Area
import com.wm.fit.core.BodyCheck
import com.wm.fit.core.CantReason
import com.wm.fit.core.Ex
import com.wm.fit.core.Exercises
import com.wm.fit.core.Injury
import com.wm.fit.core.P
import com.wm.fit.core.Plan
import com.wm.fit.core.RestTimer
import com.wm.fit.core.SetLog
import com.wm.fit.core.Store
import com.wm.fit.core.Workout

/** تمرين اليوم: التمارين، تسجيل الأوزان، الراحة، وزر "ما أقدر". */
@Composable
fun WorkoutScreen(nav: Nav) {
    val ctx = LocalContext.current
    val days = Plan.trainingDays()
    val today = Plan.todayDow()
    var picked by remember { mutableIntStateOf(if (today in days) today else Plan.nextTrainingDay()) }
    // لو غيّر أيامه والجلسة المختارة ما صارت يوم تمرين، نرجع لأول يوم
    val dow = if (picked in days) picked else days.firstOrNull() ?: today

    val session = Plan.sessionFor(dow)
    val items = Workout.forDay(dow)
    val doneToday = Store.today() in Store.done
    var cantFor by remember { mutableStateOf<Workout.Item?>(null) }
    var summary by remember { mutableStateOf(false) }

    // إذن الإشعارات (أندرويد 13+) عشان مؤقت الراحة يطلع على شاشة القفل. نسأل مرة وحدة.
    val askNotif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val startRest: (Int) -> Unit = { sec ->
        if (Build.VERSION.SDK_INT >= 33 && !P.askedNotif &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            P.askedNotif = true
            askNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        RestTimer.start(ctx, sec)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Header(session?.title ?: "اليوم راحة", sub = "تمرين ${Plan.dayNamesAr[dow]}")

        // اختيار الجلسة: لو تبي تتمرن جلسة يوم ثاني
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            days.forEach { d ->
                val on = d == dow
                Box(
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (on) C.accentBg else C.card)
                        .border(1.dp, if (on) C.accent else C.line, RoundedCornerShape(14.dp))
                        .clickable { picked = d }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                ) {
                    Text(
                        Plan.dayNamesAr[d] + if (d == today) " · اليوم" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (on) C.accent else C.muted,
                    )
                }
            }
        }

        OutlineButton("📚 مكتبة التمارين", Modifier.fillMaxWidth()) { nav.library() }

        if (today !in days && dow != today) {
            Note("اليوم يوم راحة في خطتك. لو تبي تتمرن، هذي جلسة يوم ${Plan.dayNamesAr[dow]}.")
        }

        if (P.heartRisk) {
            Note(
                "قلت إن عندك مشكلة في القلب أو ألم صدر مع المجهود. الحديد موقوف لين تراجع طبيبك. " +
                    "لين وقتها: مشي خفيف يومياً وتمارين إطالة.",
                warn = true,
            )
        } else {
            BodyCheckCard(nav)

            if (items.isEmpty() && session != null) {
                Note(
                    "كل تمارين هالجلسة تضغط على مكان فيه إصابة. راجع خريطة الإصابات، " +
                        "أو ارتاح اليوم.",
                    warn = true,
                )
            }

            items.forEachIndexed { i, item ->
                ExerciseCard(
                    item = item,
                    nav = nav,
                    onCant = { cantFor = item },
                    onSetLogged = { last ->
                        // الراحة بعد كل مجموعة، إلا آخر مجموعة في آخر تمرين
                        if (!(last && i == items.lastIndex)) startRest(item.restSec)
                    },
                )
            }

            if (items.isNotEmpty()) {
                if (doneToday) {
                    Note("خلّصت تمرين اليوم ✅ يعطيك العافية.")
                } else {
                    BigButton("خلّصت التمرين") {
                        Store.markDone()
                        RestTimer.stop(ctx)
                        summary = true
                    }
                }
            }
        }

        Spacer(Modifier.height(190.dp))
    }

    cantFor?.let { item ->
        CantDialog(item, items.map { it.ex.id }.toSet(), nav) { cantFor = null }
    }

    if (summary) {
        val logged = items.map { Store.todaySets(it.ex.id) }
        val sets = logged.sumOf { it.size }
        val volume = logged.flatten().sumOf { (it.w * it.r).toDouble() }.toFloat()
        AlertDialog(
            onDismissRequest = { summary = false },
            containerColor = C.card,
            title = { Text("يعطيك العافية 💪") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("سجّلت $sets مجموعة في ${logged.count { it.isNotEmpty() }} تمارين.")
                    if (volume > 0f) Text("مجموع اللي رفعته: ${Workout.kg(volume)} كجم.", color = C.muted)
                    Text(
                        "الأكل والنوم الحين هم اللي يبنون العضل. نام 7 ساعات أو أكثر.",
                        style = MaterialTheme.typography.bodySmall,
                        color = C.muted,
                    )
                }
            },
            confirmButton = { TextButton(onClick = { summary = false }) { Text("تمام", color = C.accent) } },
        )
    }
}

/** "كيف جسمك اليوم؟" قبل التمرين. */
@Composable
private fun BodyCheckCard(nav: Nav) {
    val v = Store.bodyToday
    if (v == 0) {
        Card {
            Text("كيف جسمك اليوم؟", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Choice("تمام 💪", false, Modifier.weight(1f)) { Store.setBody(BodyCheck.GOOD) }
                Choice("تعبان 😮‍💨", false, Modifier.weight(1f)) { Store.setBody(BodyCheck.TIRED) }
                Choice("فيه ألم 🤕", false, Modifier.weight(1f)) {
                    Store.setBody(BodyCheck.PAIN)
                    nav.injuries()
                }
            }
        }
    } else {
        val text = when (v) {
            BodyCheck.TIRED -> "جسمك تعبان اليوم، فنقصنا مجموعة من كل تمرين."
            BodyCheck.PAIN -> "قلت فيه ألم. حدده في خريطة الإصابات، وأنا أعدّل التمارين."
            else -> "جسمك تمام اليوم. يلا!"
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (v == BodyCheck.GOOD) C.accentBg else C.warnBg)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color = if (v == BodyCheck.GOOD) C.soft else C.warnText,
                modifier = Modifier.weight(1f),
            )
            Text(
                "غيّر",
                style = MaterialTheme.typography.bodySmall,
                color = C.accent,
                modifier = Modifier.clickable { Store.setBody(0) }.padding(6.dp),
            )
        }
        if (v == BodyCheck.PAIN && Store.injuries.isEmpty()) {
            OutlineButton("افتح خريطة الإصابات", Modifier.fillMaxWidth(), C.warn) { nav.injuries() }
        }
    }
}

/** وحدة العدّات: عدّات، ثواني، أو أمتار. */
internal fun repsUnit(reps: String): String = when {
    "ثانية" in reps -> "ثانية"
    "متر" in reps -> "متر"
    else -> "عدّات"
}

@Composable
private fun ExerciseCard(
    item: Workout.Item,
    nav: Nav,
    onCant: () -> Unit,
    onSetLogged: (last: Boolean) -> Unit,
) {
    val ex = item.ex
    val logged = Store.todaySets(ex.id)
    val hint = Workout.hint(item)
    val allDone = logged.size >= item.sets

    Card(bg = if (allDone) C.card2 else C.card) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(C.bg)
                    .clickable { nav.detail(ex.id) },
            ) { ExerciseAnim(ex.move, Modifier.fillMaxSize().padding(4.dp), play = false) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    when {
                        item.main -> "الأساسي · ثابت كل أسبوع"
                        item.primary -> "رئيسية"
                        else -> "مساعدة"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (item.primary) C.accent else C.muted,
                )
                Text(ex.ar + if (allDone) " ✅" else "", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${ex.en} · ${ex.muscle.ar} · ${ex.equip}",
                    style = MaterialTheme.typography.labelSmall,
                    color = C.muted,
                )
            }
        }

        item.from?.let { from ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "بديل عن ${from.ar}",
                    style = MaterialTheme.typography.labelSmall,
                    color = C.muted,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "رجّع الأصلي",
                    style = MaterialTheme.typography.labelSmall,
                    color = C.accent,
                    modifier = Modifier.clickable { Store.clearSwap(from.id) }.padding(4.dp),
                )
            }
        }

        Text(
            "${item.sets} مجموعات × ${item.reps}" + (if (ex.perSide) " لكل جهة" else "") +
                " · راحة ${item.restSec} ث",
            style = MaterialTheme.typography.bodyMedium,
            color = C.soft,
        )

        if (item.cautions.isNotEmpty()) {
            Note(
                "عندك إصابة في " + item.cautions.joinToString(" و") { injuryLabel(it) } +
                    ". خفّف الوزن، ووقّف لو زاد الألم.",
                warn = true,
            )
        }

        hint?.let { Text(it.text, style = MaterialTheme.typography.bodySmall, color = C.accent) }

        // المجموعات
        val unit = repsUnit(item.reps)
        repeat(item.sets) { i ->
            when {
                i < logged.size -> LoggedRow(i, logged[i], unit, isLast = i == logged.lastIndex) {
                    Store.undoSet(ex.id)
                }
                i == logged.size -> InputRow(
                    index = i,
                    ex = ex,
                    unit = unit,
                    weight0 = logged.lastOrNull()?.w ?: hint?.weight,
                    reps0 = logged.lastOrNull()?.r ?: Workout.topReps(item.reps),
                ) { s ->
                    Store.logSet(ex.id, i, s)
                    onSetLogged(i == item.sets - 1)
                }
                else -> Text(
                    "مجموعة ${i + 1}",
                    style = MaterialTheme.typography.bodySmall,
                    color = C.muted,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlineButton("الشرح", Modifier.weight(1f)) { nav.detail(ex.id) }
            OutlineButton("ما أقدر", Modifier.weight(1f), C.warn, onCant)
        }
    }
}

internal fun injuryLabel(i: Injury): String =
    i.area.ar + if (i.chronic) " (مزمنة)" else " (خفيفة)"

@Composable
private fun LoggedRow(index: Int, s: SetLog, unit: String, isLast: Boolean, onUndo: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(C.accentBg)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "✓ مجموعة ${index + 1}: " + (if (s.w > 0f) "${Workout.kg(s.w)} كجم × " else "") + "${s.r} $unit",
            style = MaterialTheme.typography.bodySmall,
            color = C.soft,
            modifier = Modifier.weight(1f),
        )
        if (isLast) {
            Text(
                "تراجع",
                style = MaterialTheme.typography.labelSmall,
                color = C.muted,
                modifier = Modifier.clickable(onClick = onUndo).padding(4.dp),
            )
        }
    }
}

@Composable
private fun InputRow(
    index: Int,
    ex: Ex,
    unit: String,
    weight0: Float?,
    reps0: Int?,
    onLog: (SetLog) -> Unit,
) {
    var w by remember(ex.id, index) { mutableStateOf(weight0?.let { Workout.kg(it) } ?: "") }
    var r by remember(ex.id, index) { mutableStateOf(reps0?.toString() ?: "") }
    val reps = r.toIntOrNull() ?: 0
    val weight = w.toFloatOrNull() ?: 0f
    val ok = reps > 0 && (ex.body || weight > 0f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("${index + 1}", style = MaterialTheme.typography.titleMedium, color = C.accent)
        SmallField(w, if (ex.body) "وزن إضافي" else "كجم", Modifier.weight(1f), decimal = true) { v ->
            w = v.filter { it.isDigit() || it == '.' }.take(6)
        }
        SmallField(r, unit, Modifier.weight(1f), decimal = false) { v -> r = v.filter { it.isDigit() }.take(3) }
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (ok) C.accent else C.track)
                .clickable(enabled = ok) { onLog(SetLog(weight, reps)) },
            contentAlignment = Alignment.Center,
        ) { Text("✓", style = MaterialTheme.typography.titleMedium, color = if (ok) C.onAccent else C.muted) }
    }
}

@Composable
private fun SmallField(
    value: String,
    label: String,
    modifier: Modifier,
    decimal: Boolean,
    onChange: (String) -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(C.bg)
            .border(1.dp, C.line, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = C.muted)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium.copy(color = C.text, textAlign = TextAlign.Start),
            cursorBrush = SolidColor(C.accent),
            keyboardOptions = KeyboardOptions(
                keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * زر "ما أقدر": يسأل ليش، ويعطي بديل لنفس العضلة، ويتذكر الاختيار.
 * - يوجعني: يسجّل الألم في خريطة الإصابات.
 * - ما أعرف الطريقة: يفتح الشرح، أو بديل أسهل.
 * - الجهاز مشغول: بديل لليوم بس.
 * - ثقيل: نخفف الوزن، أو بديل.
 */
@Composable
private fun CantDialog(item: Workout.Item, inToday: Set<String>, nav: Nav, onClose: () -> Unit) {
    val ex = item.ex
    var reason by remember { mutableStateOf<CantReason?>(null) }
    var area by remember { mutableStateOf<Area?>(null) }
    var level by remember { mutableIntStateOf(2) }
    var chronic by remember { mutableStateOf(false) }
    var painSaved by remember { mutableStateOf(false) }
    val original = item.from ?: ex

    AlertDialog(
        onDismissRequest = onClose,
        containerColor = C.card,
        title = { Text(if (reason == null) "ليش ما تقدر؟" else reason!!.ar) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when (reason) {
                    null -> {
                        Text(ex.name, style = MaterialTheme.typography.bodySmall, color = C.muted)
                        CantReason.entries.forEach { r ->
                            Choice(r.ar, false, Modifier.fillMaxWidth()) {
                                Store.countCant(original.id, r)
                                reason = r
                            }
                        }
                    }

                    CantReason.PAIN -> {
                        if (!painSaved) {
                            Text("وين يوجعك؟", style = MaterialTheme.typography.bodyMedium)
                            ex.stress.chunked(2).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { a ->
                                        Choice(a.ar, area == a, Modifier.weight(1f)) { area = a }
                                    }
                                    if (row.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                            OutlineButton("مكان ثاني؟ افتح الخريطة", Modifier.fillMaxWidth()) {
                                onClose(); nav.injuries()
                            }
                            area?.let { a ->
                                SectionLabel("قوة الألم")
                                Segmented(listOf(1 to "خفيف", 2 to "متوسط", 3 to "قوي"), level) { level = it }
                                SectionLabel("نوع الإصابة")
                                Segmented(listOf(false to "جديدة", true to "مزمنة"), chronic) { chronic = it }
                                BigButton("سجّل") {
                                    Store.setInjury(Injury(a, level, chronic, Store.today()))
                                    painSaved = true
                                }
                            }
                        } else {
                            val a = area!!
                            if (Store.blocked(ex)) {
                                Note(
                                    "سجّلت الألم في ${a.ar}، وشلت التمارين اللي تضغط عليه لين تتحسن. " +
                                        "تقدر تعدّله من خريطة الإصابات." +
                                        if (level == 3) " ألم قوي؟ راجع طبيب أو أخصائي علاج طبيعي." else "",
                                )
                                BigButton("تمام", onClick = onClose)
                            } else {
                                Note("سجّلته. اختر بديل ما يضغط على ${a.ar}، أو كمّل بوزن أخف.")
                                AltList(original, setOf(a), inToday, onlyToday = false, onDone = onClose)
                            }
                        }
                    }

                    CantReason.HOW -> {
                        BigButton("شوف الشرح") { onClose(); nav.detail(ex.id) }
                        SectionLabel("أو جرّب بديل")
                        AltList(original, emptySet(), inToday, onlyToday = false, onDone = onClose)
                    }

                    CantReason.BUSY -> {
                        Text(
                            "بديل لليوم بس. المرة الجاية يرجع التمرين الأصلي.",
                            style = MaterialTheme.typography.bodySmall,
                            color = C.muted,
                        )
                        AltList(original, emptySet(), inToday, onlyToday = true, onDone = onClose)
                    }

                    CantReason.HEAVY -> {
                        Note("نزّل الوزن 10 إلى 20٪ وكمّل. المهم الحركة تكون صح.")
                        BigButton("تمام، بنزّل الوزن", onClick = onClose)
                        SectionLabel("أو جرّب بديل")
                        AltList(original, emptySet(), inToday, onlyToday = false, onDone = onClose)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) { Text("إلغاء", color = C.muted) }
        },
    )
}

/** قائمة البدائل لنفس العضلة. */
@Composable
private fun AltList(ex: Ex, avoid: Set<Area>, inToday: Set<String>, onlyToday: Boolean, onDone: () -> Unit) {
    val alts = Exercises.alternatives(ex, avoid).filter { !Store.blocked(it) && it.id !in inToday }
    if (alts.isEmpty()) {
        Text("ما فيه بديل مناسب للحين.", style = MaterialTheme.typography.bodySmall, color = C.muted)
        return
    }
    alts.take(4).forEach { alt ->
        Choice(
            text = alt.ar,
            sub = "${alt.en} · ${alt.equip}",
            selected = false,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Store.setSwap(ex.id, alt.id, onlyToday)
            onDone()
        }
    }
}
