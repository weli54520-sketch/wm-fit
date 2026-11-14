package com.wm.fit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.wm.fit.core.Ex
import com.wm.fit.core.Exercises
import com.wm.fit.core.Muscle
import com.wm.fit.core.Store
import com.wm.fit.core.Workout

/** شرح التمرين: رسمة متحركة + خطوات + أخطاء شائعة + البدائل + سجلك. */
@Composable
fun ExerciseDetail(id: String, nav: Nav, onBack: () -> Unit) {
    val ex = Exercises.get(id)
    if (ex == null) {
        Header("ما لقيت التمرين", onBack = onBack)
        return
    }
    val inj = ex.stress.mapNotNull { Store.injuries[it] }
    val history = Store.logs[ex.id].orEmpty().takeLast(5).reversed()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Header(ex.ar, sub = "${ex.en} · ${ex.muscle.ar} (${ex.muscle.en})", onBack = onBack)

        Box(
            Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(C.card),
        ) { ExerciseAnim(ex.move, Modifier.fillMaxSize().padding(12.dp)) }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat(ex.equip, "الأداة", Modifier.weight(1f))
            Stat(if (ex.compound) "مركّب" else "عزل", "النوع", Modifier.weight(1f))
            Stat(if (ex.body) "—" else "+${Workout.kg(ex.inc)}", "الزيادة كجم", Modifier.weight(1f))
        }

        if (inj.isNotEmpty()) {
            Note(
                "هذا التمرين يضغط على " + inj.joinToString(" و") { it.area.ar } +
                    "، وعندك إصابة فيه. " +
                    if (Store.blocked(ex)) "موقوف لين تتحسن." else "خفّف الوزن، ووقّف لو زاد الألم.",
                warn = true,
            )
        }

        Card {
            Text("الخطوات", style = MaterialTheme.typography.titleMedium)
            ex.steps.forEachIndexed { i, s ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(C.accentBg),
                        contentAlignment = Alignment.Center,
                    ) { Text("${i + 1}", style = MaterialTheme.typography.labelSmall, color = C.accent) }
                    Text(s, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                }
            }
        }

        Card {
            Text("أخطاء شائعة", style = MaterialTheme.typography.titleMedium, color = C.warnText)
            ex.mistakes.forEach { m ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("✗", style = MaterialTheme.typography.bodyMedium, color = C.bad)
                    Text(m, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                }
            }
        }

        Text(
            "يضغط على: " + ex.stress.joinToString("، ") { it.ar },
            style = MaterialTheme.typography.bodySmall,
            color = C.muted,
        )

        if (history.isNotEmpty()) {
            Card {
                Text("سجلك", style = MaterialTheme.typography.titleMedium)
                history.forEach { e ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(e.date, style = MaterialTheme.typography.bodySmall, color = C.muted)
                        Text(
                            e.sets.joinToString("، ") {
                                if (it.w > 0f) "${Workout.kg(it.w)}×${it.r}" else "${it.r}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }

        val alts = Exercises.alternatives(ex)
        if (alts.isNotEmpty()) {
            SectionLabel("بدائل لنفس العضلة")
            alts.forEach { ExRow(it) { nav.detail(it.id) } }
        }

        Spacer(Modifier.height(40.dp))
    }
}

/** مكتبة التمارين، مقسمة حسب العضلة. */
@Composable
fun Library(nav: Nav, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Header("مكتبة التمارين", sub = "${Exercises.all.size} تمرين", onBack = onBack)
        Muscle.entries.forEach { m ->
            val list = Exercises.forMuscle(m)
            if (list.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                SectionLabel("${m.ar} (${m.en})")
                list.forEach { ExRow(it) { nav.detail(it.id) } }
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun ExRow(ex: Ex, onClick: () -> Unit) {
    Card(radius = 20, onClick = onClick) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(C.bg),
            ) { ExerciseAnim(ex.move, Modifier.fillMaxSize().padding(3.dp), play = false) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    ex.ar + if (Store.blocked(ex)) " · موقوف" else "",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (Store.blocked(ex)) C.muted else C.text,
                )
                Text("${ex.en} · ${ex.equip}", style = MaterialTheme.typography.labelSmall, color = C.muted)
            }
            Text("›", style = MaterialTheme.typography.titleMedium, color = C.muted)
        }
    }
}
