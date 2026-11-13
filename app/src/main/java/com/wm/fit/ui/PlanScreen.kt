package com.wm.fit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wm.fit.core.Calc
import com.wm.fit.core.Muscle
import com.wm.fit.core.P
import com.wm.fit.core.Plan

/** خطة الأسبوع: أي يوم أي عضلات، وكم مجموعة. */
@Composable
fun PlanScreen() {
    val scheme = Plan.scheme(P.goal, P.level)
    val focus = P.focus.mapNotNull { Muscle.byId(it) }

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

        Card {
            Text("نظام المجموعات", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("${scheme.sets}", "مجموعات", Modifier.weight(1f))
                Stat(scheme.reps, "عدّات", Modifier.weight(1f))
                Stat("${scheme.restSec} ث", "راحة", Modifier.weight(1f))
            }
            Text(scheme.note, style = MaterialTheme.typography.bodySmall, color = C.muted)
        }

        if (P.heartRisk) {
            Note(
                "خطة الحديد موقوفة لين تراجع طبيبك. مشي خفيف يومياً، وتمارين إطالة.",
                warn = true,
            )
        }

        Card {
            Text("أيام الأسبوع", style = MaterialTheme.typography.titleMedium)
            (0..6).forEach { i ->
                val s = Plan.sessionFor(i)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        Text(
                            s.groups.joinToString("، ") { "${it.ar} (${it.en})" },
                            style = MaterialTheme.typography.bodySmall,
                            color = C.muted,
                        )
                    }
                }
                if (i < 6) Box(Modifier.fillMaxWidth().height(1.dp).background(C.line))
            }
        }

        Card {
            Text("المجموعات الأسبوعية", style = MaterialTheme.typography.titleMedium)
            Muscle.entries.forEach { m ->
                val isFocus = m in focus
                val sets = Calc.weeklySets(isFocus, P.level)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            m.ar + if (isFocus) " · تركيز" else "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isFocus) C.accent else C.text,
                        )
                        Text(
                            "$sets مجموعة",
                            style = MaterialTheme.typography.bodySmall,
                            color = C.muted,
                        )
                    }
                    Bar(sets / 20f, if (isFocus) C.accent else C.muted)
                }
            }
            Text(
                "الحد الأدنى للتضخيم تقريباً 10 مجموعات بالأسبوع لكل عضلة، والفايدة تقل بعد 18 إلى 20 " +
                    "(ACSM 2026).",
                style = MaterialTheme.typography.labelSmall,
                color = C.muted,
            )
        }

        Note(Plan.cardioNote(P.goal))

        Note(
            "التمارين نفسها وشرحها وزر \"ما أقدر\" جايين في التحديث الجاي. " +
                "هذي المرحلة الأولى: الحسابات والخطة الأسبوعية.",
        )

        Spacer(Modifier.height(100.dp))
    }
}
