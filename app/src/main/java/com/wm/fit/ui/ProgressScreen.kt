package com.wm.fit.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.wm.fit.core.Calc
import com.wm.fit.core.Dates
import com.wm.fit.core.Diary
import com.wm.fit.core.Exercises
import com.wm.fit.core.Goal
import com.wm.fit.core.Measure
import com.wm.fit.core.P
import com.wm.fit.core.Photo
import com.wm.fit.core.Stages
import com.wm.fit.core.Store
import com.wm.fit.core.Workout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

/** التقدم: المرحلة، الوزن، الخصر، الأوزان في التمارين، والصور. */
@Composable
fun ProgressScreen(nav: Nav) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Header("تقدمك")
        StageCard()
        WeightCard()
        WaistCard()
        LiftsCard()
        PhotosCard()
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun StageCard() {
    val cur = Stages.current()
    val sug = Stages.suggestion()
    Card {
        Text("هدفك الحين", style = MaterialTheme.typography.bodySmall, color = C.muted)
        Text(stageLabel(), style = MaterialTheme.typography.titleLarge, color = C.accent)
        if (cur >= 0 && P.stageSince.isNotBlank()) {
            Text(
                "من ${Dates.daysBetween(P.stageSince, Store.today())} يوم",
                style = MaterialTheme.typography.labelSmall,
                color = C.muted,
            )
        }
        sug?.let { s ->
            Note(s.why)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BigButton("انتقل لـ${Stages.names[s.to]}", Modifier.weight(1f)) { Stages.move(s.to) }
                OutlineButton("مو الحين", Modifier.weight(1f)) { Stages.snooze() }
            }
        }
        SectionLabel("تقدر تغيّر هدفك بنفسك أي وقت")
        Segmented(
            listOf(Goal.CUT to "تنشيف", Goal.MAINTAIN to "تثبيت", Goal.BULK to "تضخيم", Goal.FITNESS to "لياقة"),
            P.goal,
        ) { g ->
            val i = Stages.goals.indexOf(g)
            if (i >= 0) Stages.move(i) else { P.goal = g; P.stageSince = Store.today() }
        }
        Text(
            "المراحل: تنشيف ← تثبيت أسبوعين ← تضخيم نظيف. أقترح الانتقال، والقرار لك.",
            style = MaterialTheme.typography.labelSmall,
            color = C.muted,
        )
    }
}

@Composable
private fun WeightCard() {
    var w by remember { mutableStateOf("") }
    val list = Diary.weights
    Card {
        Text("الوزن", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Field(w, "وزنك اليوم (كجم)", Modifier.weight(1f), number = true) {
                w = it.filter { c -> c.isDigit() || c == '.' }.take(5)
            }
            val v = w.toFloatOrNull()
            OutlineButton("سجّل", color = C.accent) {
                if (v != null && v in 30f..300f) { Diary.logWeight(v); w = "" }
            }
        }
        LineChart(list.takeLast(60), "كجم")
        weeklyChange(list)?.let { pct ->
            val target = Calc.weeklyRatePct(P.goal)
            val txt = "تغيّر وزنك آخر أسبوع: ${if (pct > 0) "+" else ""}${Workout.kg(pct)}٪"
            val judge = when {
                P.goal == Goal.CUT && pct < -1.0f -> " · أسرع من اللازم، زيد أكلك شوي عشان ما تخسر عضل."
                P.goal == Goal.CUT && pct > -0.3f -> " · أبطأ من المدى (0.5 إلى 1٪)."
                P.goal == Goal.BULK && pct > 0.6f -> " · أسرع من اللازم، أغلبه دهون. نقّص أكلك شوي."
                target != 0f -> " · ضمن المدى 👍"
                else -> ""
            }
            Text(txt + judge, style = MaterialTheme.typography.bodySmall, color = C.soft)
        }
        Text("وزّن نفسك الصبح بعد الحمام وقبل الأكل، عشان الأرقام تكون ثابتة.",
            style = MaterialTheme.typography.labelSmall, color = C.muted)
    }
}

/** نسبة تغيّر الوزن بين آخر قياس وقياس قبل 6 إلى 10 أيام. */
private fun weeklyChange(list: List<Measure>): Float? {
    val last = list.lastOrNull() ?: return null
    val prev = list.lastOrNull { Dates.daysBetween(it.date, last.date) in 6..10 } ?: return null
    return (last.v - prev.v) / prev.v * 100f
}

@Composable
private fun WaistCard() {
    var cm by remember { mutableStateOf("") }
    var how by remember { mutableStateOf(false) }
    var neck by remember { mutableStateOf(if (P.neckCm > 0f) Workout.kg(P.neckCm) else "") }
    var hip by remember { mutableStateOf(if (P.hipCm > 0f) Workout.kg(P.hipCm) else "") }
    var target by remember { mutableStateOf(if (P.targetWaist > 0f) Workout.kg(P.targetWaist) else "") }
    val since = Diary.daysSinceWaist()
    val female = P.sex == "f"
    fun num(s: String) = s.filter { it.isDigit() || it == '.' }.take(5)

    Card {
        Text("الخصر (الرقم الأساسي)", style = MaterialTheme.typography.titleMedium)
        if (since == null || since >= 7) {
            Note(if (since == null) "قيس خصرك أول مرة. مرة بالأسبوع تكفي." else "صار لك $since يوم ما قست خصرك. وقت القياس الأسبوعي!")
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Field(cm, "مقاس الخصر (سم)", Modifier.weight(1f), number = true) { cm = num(it) }
            OutlineButton("سجّل", color = C.accent) {
                val v = cm.toFloatOrNull()
                if (v != null && v in 40f..200f) { Diary.logWaist(v); cm = "" }
            }
        }
        LineChart(Diary.waists.takeLast(30), "سم")
        Text(
            if (how) "إخفاء طريقة القياس" else "كيف أقيس صح؟",
            style = MaterialTheme.typography.bodySmall,
            color = C.accent,
            modifier = Modifier.clickable { how = !how }.padding(vertical = 4.dp),
        )
        if (how) {
            listOf(
                "اوقف مستقيم، وبطنك مرتاح بدون ما تشفطه.",
                "حط المتر على مستوى السرة، ملتف حول خصرك بشكل مستوي.",
                "المتر لاصق بالجلد بدون ما يضغط.",
                "اقرأ الرقم بعد ما تطلع نفَسك عادي.",
                "قيس نفس اليوم ونفس الوقت كل أسبوع (أفضل الصبح).",
            ).forEachIndexed { i, s ->
                Text("${i + 1}. $s", style = MaterialTheme.typography.bodySmall, color = C.soft)
            }
        }

        SectionLabel("هدفك وقياسات مرة وحدة")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Field(target, "الخصر المستهدف", Modifier.weight(1f), number = true) {
                target = num(it); P.targetWaist = target.toFloatOrNull() ?: 0f
            }
            Field(neck, "الرقبة (مرة وحدة)", Modifier.weight(1f), number = true) {
                neck = num(it); P.neckCm = neck.toFloatOrNull() ?: 0f
            }
            if (female) {
                Field(hip, "الورك", Modifier.weight(1f), number = true) {
                    hip = num(it); P.hipCm = hip.toFloatOrNull() ?: 0f
                }
            }
        }
        val waist = Diary.waists.lastOrNull()?.v
        val bf = waist?.let { Calc.bodyFatNavy(!female, it, P.neckCm, P.heightCm, P.hipCm) }
        Text(
            if (bf != null) "نسبة الدهون: تقريباً ${bf.roundToInt()}٪ (تقريبية، خطأها 3 إلى 4٪)"
            else "نسبة الدهون تطلع لما تسجّل الخصر والرقبة" + if (female) " والورك." else ".",
            style = MaterialTheme.typography.bodySmall,
            color = C.muted,
        )
    }
}

@Composable
private fun LiftsCard() {
    // التمارين اللي لها سجل مرتين أو أكثر، الأكثر تسجيل أول
    val ids = Store.logs.filter { (_, e) -> e.count { it.sets.any { s -> s.w > 0f } } >= 2 }
        .entries.sortedByDescending { it.value.size }.map { it.key }
    var pick by remember { mutableStateOf<String?>(null) }
    val id = pick?.takeIf { it in ids } ?: ids.firstOrNull()

    Card {
        Text("أوزانك في التمارين", style = MaterialTheme.typography.titleMedium)
        Text(
            "تمرنت ${Store.doneThisWeek()} مرات هالأسبوع، و${Store.done.size} مرة من البداية.",
            style = MaterialTheme.typography.bodySmall,
            color = C.muted,
        )
        if (id == null) {
            Text("سجّل أوزانك في تمرينين أو أكثر، ويطلع لك هنا تطورك.", style = MaterialTheme.typography.bodySmall,
                color = C.muted)
        } else {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ids.take(8).forEach { e ->
                    val on = e == id
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (on) C.accentBg else C.bg)
                            .border(1.dp, if (on) C.accent else C.line, RoundedCornerShape(12.dp))
                            .clickable { pick = e }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(Exercises.get(e)?.ar ?: e, style = MaterialTheme.typography.labelSmall,
                            color = if (on) C.accent else C.muted)
                    }
                }
            }
            val pts = Store.logs[id].orEmpty().mapNotNull { en ->
                en.sets.maxOfOrNull { it.w }?.takeIf { it > 0f }?.let { Measure(en.date, it) }
            }
            LineChart(pts, "كجم")
        }
    }
}

@Composable
private fun PhotosCard() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var open by remember { mutableStateOf<Photo?>(null) }
    var busy by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            busy = true
            scope.launch {
                val name = withContext(Dispatchers.IO) { savePhoto(ctx, uri) }
                if (name != null) Diary.addPhoto(name)
                busy = false
            }
        }
    }
    val photos = Diary.photos.sortedByDescending { it.date }

    Card {
        Text("صور التقدم (اختيارية)", style = MaterialTheme.typography.titleMedium)
        Text(
            "الصور تنحفظ داخل التطبيق بس: ما تطلع في معرض الصور، وما تطلع من جوالك.",
            style = MaterialTheme.typography.labelSmall,
            color = C.muted,
        )
        OutlineButton(if (busy) "جاري الحفظ..." else "+ أضف صورة", Modifier.fillMaxWidth(), C.accent) {
            if (!busy) picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        if (photos.size >= 2) {
            SectionLabel("أول صورة وآخر صورة")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PhotoThumb(photos.last(), Modifier.weight(1f).height(200.dp)) { open = photos.last() }
                PhotoThumb(photos.first(), Modifier.weight(1f).height(200.dp)) { open = photos.first() }
            }
        }
        if (photos.isNotEmpty()) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                photos.forEach { p -> PhotoThumb(p, Modifier.size(80.dp, 110.dp)) { open = p } }
            }
        }
    }

    open?.let { p ->
        AlertDialog(
            onDismissRequest = { open = null },
            containerColor = C.card,
            title = { Text(Dates.short(p.date)) },
            text = { PhotoThumb(p, Modifier.fillMaxWidth().height(420.dp), maxSide = 1400) {} },
            confirmButton = { TextButton(onClick = { open = null }) { Text("تمام", color = C.accent) } },
            dismissButton = {
                TextButton(onClick = { Diary.removePhoto(p); open = null }) { Text("احذفها", color = C.bad) }
            },
        )
    }
}

@Composable
private fun PhotoThumb(p: Photo, modifier: Modifier, maxSide: Int = 400, onClick: () -> Unit) {
    val img by produceState<ImageBitmap?>(null, p.file, maxSide) {
        value = withContext(Dispatchers.IO) { loadPhoto(p.file, maxSide) }
    }
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(C.bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.BottomCenter,
    ) {
        img?.let { Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        Text(
            Dates.short(p.date),
            style = MaterialTheme.typography.labelSmall,
            color = C.text,
            modifier = Modifier.background(C.bg.copy(alpha = 0.6f)).padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

/** ينسخ الصورة لذاكرة التطبيق الخاصة، بحجم أصغر (أقصى 1600 بكسل). */
private fun savePhoto(ctx: android.content.Context, uri: Uri): String? = runCatching {
    val dir = Diary.photoDir ?: return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1600) sample *= 2
    val bmp = ctx.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    } ?: return null
    val name = "p${System.currentTimeMillis()}.jpg"
    FileOutputStream(File(dir, name)).use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
    bmp.recycle()
    name
}.getOrNull()

private fun loadPhoto(file: String, maxSide: Int): ImageBitmap? = runCatching {
    val f = File(Diary.photoDir ?: return null, file)
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(f.path, bounds)
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxSide * 2) sample *= 2
    BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
}.getOrNull()
