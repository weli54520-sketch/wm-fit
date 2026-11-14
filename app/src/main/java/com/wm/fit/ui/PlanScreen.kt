package com.wm.fit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.wm.fit.core.Muscle
import com.wm.fit.core.P
import com.wm.fit.core.Plan
import com.wm.fit.core.Workout

/** خطة الأسبوع: كل يوم وعضلته الرئيسية والمساعدة، وليش. */
@Composable
fun PlanScreen() {
    val scheme = Plan.scheme(P.goal, P.level)
    val focus = P.focus.mapNotNull { Muscle.byId(it) }
    val week = Plan.week()
    var swapFrom by remember { mutableStateOf<Int?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stageLabel(), style = MaterialTheme.typography.bodySmall, color = C.muted)
            Text("خطة الأسبوع", style = MaterialTheme.typography.headlineMedium)
        }

        if (P.heartRisk) {
            Note(
                "خطة الحديد موقوفة لين تراجع طبيبك. مشي خفيف يومياً، وتمارين إطالة.",
                warn = true,
            )
        }

        Card {
            Text("أيامك", style = MaterialTheme.typography.titleMedium)
            DayPicker(P.trainDays) { P.trainDays = it; swapFrom = null }
        }

        // ليش الخطة كذا
        week.notes.forEach { Note(it) }

        Card {
            Text("أيام الأسبوع", style = MaterialTheme.typography.titleMedium)
            Text(
                if (swapFrom == null) "تبي تبدّل يومين؟ اضغط على يوم، بعدين على اليوم الثاني."
                else "اختر اليوم اللي تبي تبدّل معه ${Plan.dayNamesAr[swapFrom!!]}.",
                style = MaterialTheme.typography.labelSmall,
                color = if (swapFrom == null) C.muted else C.accent,
            )
            (0..6).forEach { i ->
                val s = week.days[i]
                val picked = swapFrom == i
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .then(if (picked) Modifier.border(1.5.dp, C.accent, RoundedCornerShape(14.dp)) else Modifier)
                        .then(
                            if (s != null) Modifier.clickable {
                                val from = swapFrom
                                swapFrom = when {
                                    from == null -> i
                                    from == i -> null
                                    else -> { Plan.swapDays(from, i); null }
                                }
                            } else Modifier,
                        )
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            Plan.dayNamesAr[i],
                            style = MaterialTheme.typography.titleMedium,
                            color = if (s != null) C.text else C.muted,
                        )
                        Text(
                            s?.title ?: "راحة",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (s != null) C.accent else C.muted,
                        )
                    }
                    if (s != null) {
                        s.slots.forEach { slot ->
                            Text(
                                (if (slot.main) "رئيسية: " else "مساعدة: ") +
                                    "${slot.part.ar} (${slot.part.muscle.en}) · ${slot.count} " +
                                    (if (slot.count == 1) "تمرين" else "تمارين") +
                                    if (slot.focus) " · تركيز" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (slot.main) C.soft else C.muted,
                            )
                        }
                        s.reasons.forEach {
                            Text("← $it", style = MaterialTheme.typography.labelSmall, color = C.muted)
                        }
                    }
                }
                if (i < 6) Box(Modifier.fillMaxWidth().height(1.dp).background(C.line))
            }
            if (Plan.hasSwaps()) {
                OutlineButton("رجّع ترتيب المدرب", Modifier.fillMaxWidth()) { Plan.resetSwaps(); swapFrom = null }
            }
        }

        Card {
            Text("نظام المجموعات", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("${scheme.sets}", "مجموعات", Modifier.weight(1f))
                Stat(scheme.reps, "عدّات", Modifier.weight(1f))
                Stat("${scheme.restSec} ث", "راحة", Modifier.weight(1f))
            }
            Text(scheme.note, style = MaterialTheme.typography.bodySmall, color = C.muted)
        }

        Card {
            Text("كل عضلة بالأسبوع", style = MaterialTheme.typography.titleMedium)
            val sets = Workout.weeklySets()
            Muscle.entries.forEach { m ->
                val isFocus = m in focus
                val n = sets[m] ?: 0
                val times = week.times[m] ?: 0
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "${m.ar} (${m.en})" + if (isFocus) " · تركيز" else "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isFocus) C.accent else C.text,
                        )
                        Text(
                            "$n مجموعة · " + if (times >= 2) "مرتين" else if (times == 1) "مرة" else "—",
                            style = MaterialTheme.typography.bodySmall,
                            color = C.muted,
                        )
                    }
                    Bar(n / 20f, if (isFocus) C.accent else C.muted)
                }
            }
            Text(
                "أقصى شي مرتين بالأسبوع لكل عضلة. والفايدة تقل بعد 18 إلى 20 مجموعة (ACSM 2026).",
                style = MaterialTheme.typography.labelSmall,
                color = C.muted,
            )
        }

        Note(Plan.cardioNote(P.goal))

        Spacer(Modifier.height(100.dp))
    }
}
