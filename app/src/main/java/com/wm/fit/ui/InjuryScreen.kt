package com.wm.fit.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.wm.fit.core.Area
import com.wm.fit.core.Exercises
import com.wm.fit.core.Injury
import com.wm.fit.core.Store
import kotlin.math.min

/** شاشة خريطة الإصابات. */
@Composable
fun InjuryScreen(onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Header("خريطة الإصابات", sub = "اضغط على مكان الألم", onBack = onBack)
        InjuryMap()
        Spacer(Modifier.height(40.dp))
    }
}

/** مكان النقطة على الرسمة (مربع 100×200). */
private class Dot(val area: Area, val x: Float, val y: Float)

private val frontDots = listOf(
    Dot(Area.NECK, 50f, 30f),
    Dot(Area.SHOULDER, 33f, 40f), Dot(Area.SHOULDER, 67f, 40f),
    Dot(Area.CHEST, 50f, 52f),
    Dot(Area.ELBOW, 25f, 70f), Dot(Area.ELBOW, 75f, 70f),
    Dot(Area.FOREARM, 22f, 82f), Dot(Area.FOREARM, 78f, 82f),
    Dot(Area.WRIST, 19f, 95f), Dot(Area.WRIST, 81f, 95f),
    Dot(Area.HIP, 41f, 100f), Dot(Area.HIP, 59f, 100f),
    Dot(Area.KNEE, 42f, 140f), Dot(Area.KNEE, 58f, 140f),
    Dot(Area.ANKLE, 43f, 182f), Dot(Area.ANKLE, 57f, 182f),
)

private val backDots = listOf(
    Dot(Area.NECK, 50f, 30f),
    Dot(Area.SHOULDER, 33f, 40f), Dot(Area.SHOULDER, 67f, 40f),
    Dot(Area.UPPER_BACK, 50f, 52f),
    Dot(Area.LOWER_BACK, 50f, 86f),
    Dot(Area.ELBOW, 25f, 70f), Dot(Area.ELBOW, 75f, 70f),
    Dot(Area.HIP, 41f, 104f), Dot(Area.HIP, 59f, 104f),
    Dot(Area.KNEE, 42f, 140f), Dot(Area.KNEE, 58f, 140f),
    Dot(Area.ANKLE, 43f, 182f), Dot(Area.ANKLE, 57f, 182f),
)

internal fun levelColor(level: Int): Color = when (level) {
    1 -> C.fat
    2 -> C.warn
    else -> C.bad
}

internal fun levelAr(level: Int): String = when (level) {
    1 -> "خفيف"
    2 -> "متوسط"
    else -> "قوي"
}

/**
 * الخريطة نفسها: أمام وخلف، ونقطة لكل مكان.
 * تنستخدم في شاشة الإصابات وفي أسئلة أول مرة.
 */
@Composable
fun InjuryMap() {
    var back by remember { mutableStateOf(false) }
    var edit by remember { mutableStateOf<Area?>(null) }
    val injuries = Store.injuries

    Segmented(listOf(false to "أمام", true to "خلف"), back) { back = it }

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(380.dp),
    ) {
        val scale = min(maxWidth.value / 100f, maxHeight.value / 200f)
        val ox = (maxWidth.value - 100f * scale) / 2f
        val oy = (maxHeight.value - 200f * scale) / 2f
        val bodyColor = C.card2

        Canvas(Modifier.fillMaxSize()) {
            val k = size.height / maxHeight.value
            fun o(x: Float, y: Float) = Offset((ox + x * scale) * k, (oy + y * scale) * k)
            val u = scale * k
            drawCircle(bodyColor, 10f * u, o(50f, 16f))
            drawLine(bodyColor, o(50f, 24f), o(50f, 34f), 7f * u, StrokeCap.Round)
            drawRoundRect(
                bodyColor,
                topLeft = o(32f, 33f),
                size = Size(36f * u, 71f * u),
                cornerRadius = CornerRadius(10f * u),
            )
            for (s in listOf(1f, -1f)) {
                fun x(v: Float) = 50f + s * (v - 50f)
                drawLine(bodyColor, o(x(34f), 38f), o(x(25f), 70f), 8f * u, StrokeCap.Round)
                drawLine(bodyColor, o(x(25f), 70f), o(x(19f), 97f), 7f * u, StrokeCap.Round)
                drawLine(bodyColor, o(x(42f), 100f), o(x(42f), 140f), 11f * u, StrokeCap.Round)
                drawLine(bodyColor, o(x(42f), 140f), o(x(43f), 184f), 9f * u, StrokeCap.Round)
                drawLine(bodyColor, o(x(43f), 186f), o(x(47f), 188f), 5f * u, StrokeCap.Round)
            }
        }

        (if (back) backDots else frontDots).forEach { d ->
            val inj = injuries[d.area]
            Box(
                Modifier
                    // absoluteOffset: ما ينعكس في العربي، عشان النقطة تجي فوق مكانها بالرسمة
                    .absoluteOffset(x = (ox + d.x * scale - 13f).dp, y = (oy + d.y * scale - 13f).dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (inj != null) levelColor(inj.level) else C.bg)
                    .border(2.dp, if (inj != null) C.text else C.accent.copy(alpha = 0.7f), CircleShape)
                    .clickable { edit = d.area },
            )
        }
    }

    // نفس الأماكن كقائمة، أسهل للضغط
    SectionLabel("أو اختر من القائمة")
    Area.entries.chunked(3).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { a ->
                val inj = injuries[a]
                Choice(
                    a.ar,
                    inj != null,
                    Modifier.weight(1f),
                    tint = if (inj != null) levelColor(inj.level) else C.accent,
                ) { edit = a }
            }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }

    if (injuries.isNotEmpty()) {
        SectionLabel("إصاباتك")
        injuries.values.sortedBy { it.area.ordinal }.forEach { i ->
            Card(radius = 20, onClick = { edit = i.area }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(Modifier.size(14.dp).clip(CircleShape).background(levelColor(i.level)))
                    Column(Modifier.weight(1f)) {
                        Text("${i.area.ar} (${i.area.en})", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${levelAr(i.level)} · ${if (i.chronic) "مزمنة" else "جديدة"} · من ${i.since}",
                            style = MaterialTheme.typography.labelSmall,
                            color = C.muted,
                        )
                    }
                }
                Text(effect(i), style = MaterialTheme.typography.bodySmall, color = C.muted)
            }
        }
    }

    Note("الخريطة عشان نعدّل تمارينك بس، مو تشخيص. لو الألم قوي أو يزيد، راجع طبيب.")

    edit?.let { a -> InjuryDialog(a, injuries[a]) { edit = null } }
}

/** وش يسوي التطبيق مع هالإصابة. */
private fun effect(i: Injury): String {
    val hit = Exercises.all.count { i.area in it.stress }
    return when {
        i.chronic -> "مزمنة: ما نوقف تمارينك. نقدّم الأخف على ${i.area.ar}، وننبهك تخفف الوزن."
        i.level >= 2 -> "نوقف $hit تمارين تضغط على ${i.area.ar} لين تتحسن، ونعطيك بدائل." +
            if (i.level == 3) " ألم قوي؟ راجع طبيب أو أخصائي علاج طبيعي." else ""
        else -> "خفيفة: نخلي التمارين، بس ننبهك تخفف الوزن ووقّف لو زاد الألم."
    }
}

@Composable
private fun InjuryDialog(area: Area, current: Injury?, onClose: () -> Unit) {
    var level by remember { mutableIntStateOf(current?.level ?: 2) }
    var chronic by remember { mutableStateOf(current?.chronic ?: false) }

    AlertDialog(
        onDismissRequest = onClose,
        containerColor = C.card,
        title = { Text("${area.ar} (${area.en})") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("قوة الألم")
                Segmented(listOf(1 to "خفيف", 2 to "متوسط", 3 to "قوي"), level) { level = it }
                SectionLabel("نوع الإصابة")
                Segmented(listOf(false to "جديدة", true to "مزمنة"), chronic) { chronic = it }
                Text(
                    effect(Injury(area, level, chronic, "")),
                    style = MaterialTheme.typography.bodySmall,
                    color = C.muted,
                )
                BigButton(if (current == null) "سجّل" else "احفظ") {
                    Store.setInjury(Injury(area, level, chronic, current?.since ?: Store.today()))
                    onClose()
                }
            }
        },
        confirmButton = {
            if (current != null) {
                TextButton(onClick = { Store.removeInjury(area); onClose() }) {
                    Text("تعافيت ✅", color = C.accent)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) { Text("إلغاء", color = C.muted) }
        },
    )
}
