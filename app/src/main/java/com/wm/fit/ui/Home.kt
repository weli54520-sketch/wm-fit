package com.wm.fit.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wm.fit.core.Calc
import com.wm.fit.core.Goal
import com.wm.fit.core.Muscle
import com.wm.fit.core.P
import com.wm.fit.core.Plan
import com.wm.fit.core.Store
import com.wm.fit.core.Coach
import com.wm.fit.core.Stages
import com.wm.fit.core.Diary
import java.util.Calendar
import kotlin.math.roundToInt

@Composable
fun Home(nav: Nav) {
    val d = Calc.daily()
    val session = Plan.todaySession()
    val dow = Plan.todayDow()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        // ترحيب
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(greeting(), style = MaterialTheme.typography.bodySmall, color = C.muted)
            Text(
                if (session != null) "جاهز لتمرين اليوم؟" else "اليوم راحة",
                style = MaterialTheme.typography.headlineMedium,
            )
        }

        // تنبيه السلامة لو فيه خطر على القلب
        if (P.heartRisk) {
            Note(
                "قلت إن عندك مشكلة في القلب أو ألم صدر مع المجهود. راجع طبيبك قبل الحديد الثقيل. " +
                    "لين وقتها خطتك مشي وتمارين خفيفة.",
                warn = true,
            )
        }

        // تمرين اليوم
        Card(onClick = if (session != null && !P.heartRisk) nav.workout else null) {
            if (session != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    WeekRing()
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            stageLabel(),
                            style = MaterialTheme.typography.bodySmall,
                            color = C.accent,
                        )
                        Text(session.title, style = MaterialTheme.typography.titleLarge)
                        Text(
                            session.groups.joinToString("، ") { it.ar },
                            style = MaterialTheme.typography.bodySmall,
                            color = C.muted,
                        )
                    }
                }
                if (!P.heartRisk) {
                    BigButton(if (Store.today() in Store.done) "خلّصته ✅ شوف التمرين" else "ابدأ التمرين") {
                        nav.workout()
                    }
                }
            } else {
                Text("اليوم راحة 😴", style = MaterialTheme.typography.titleLarge)
                Text(
                    "الراحة جزء من الخطة، العضل يكبر وقت الراحة. " +
                        "تمرينك الجاي يوم ${Plan.dayNamesAr[Plan.nextTrainingDay()]}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = C.muted,
                )
            }
        }

        // المدرب: أول اقتراح
        Coach.tips().firstOrNull()?.let { t ->
            Card(onClick = { nav.coach() }) {
                Text("المدرب يقترح", style = MaterialTheme.typography.bodySmall, color = C.accent)
                Text(t.title, style = MaterialTheme.typography.titleMedium)
                Text(t.why, style = MaterialTheme.typography.bodySmall, color = C.muted)
            }
        }
        Stages.suggestion()?.let { s ->
            Card {
                Text("اقتراح المرحلة", style = MaterialTheme.typography.bodySmall, color = C.accent)
                Text(s.why, style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BigButton("انتقل لـ${Stages.names[s.to]}", Modifier.weight(1f)) { Stages.move(s.to) }
                    OutlineButton("مو الحين", Modifier.weight(1f)) { Stages.snooze() }
                }
            }
        }

        // أرقام اليوم
        Card {
            Text("أرقامك اليومية", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("${d.kcal}", "سعرة", Modifier.weight(1f))
                Stat("${d.protein} جم", "بروتين", Modifier.weight(1f))
                Stat("${d.carbs} جم", "كارب", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("${d.fat} جم", "دهون", Modifier.weight(1f))
                Stat(fmt1(d.waterL) + " لتر", "موية", Modifier.weight(1f))
                Stat(fmt1(d.bmi), "مؤشر الكتلة", Modifier.weight(1f))
            }
            Text(
                "أكلت اليوم ${Diary.totals().kcal} من ${d.kcal} سعرة. مجموع سعراتك بدون تعديل الهدف: ${d.tdee}",
                style = MaterialTheme.typography.labelSmall,
                color = C.muted,
            )
        }

        // الإصابات
        Card(onClick = { nav.injuries() }) {
            Text("إصاباتك", style = MaterialTheme.typography.bodySmall, color = C.muted)
            Text(
                if (Store.injuries.isEmpty()) "ما فيه إصابات مسجلة"
                else Store.injuries.values.joinToString(" · ") { "${it.area.ar} (${levelAr(it.level)})" },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "عندك ألم؟ حدده على الخريطة، وأنا أعدّل التمارين.",
                style = MaterialTheme.typography.bodySmall,
                color = C.muted,
            )
        }

        // التركيز
        Card {
            Text(
                if (P.focus.isEmpty()) "خطة متوازنة" else "تركيزك (اختيارك)",
                style = MaterialTheme.typography.bodySmall,
                color = C.muted,
            )
            Text(
                if (P.focus.isEmpty()) "كل العضلات تاخذ نفس الاهتمام"
                else P.focus.mapNotNull { Muscle.byId(it)?.ar }.joinToString(" · "),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                if (P.focus.isEmpty()) "كل يوم له عضلة رئيسية، والباقي مساعدة."
                else "عضلات التركيز تنلعب مرتين بالأسبوع (أقصى شي)، وتاخذ يوم رئيسي لها.",
                style = MaterialTheme.typography.bodySmall,
                color = C.muted,
            )
        }

        // الأسبوع
        Card(onClick = { nav.plan() }) {
            Text("أسبوعك", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (0..6).forEach { i ->
                    val s = Plan.sessionFor(i)
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(C.bg)
                            .then(
                                if (i == dow) Modifier.border(
                                    1.5.dp, C.accent, RoundedCornerShape(10.dp),
                                ) else Modifier,
                            )
                            .padding(vertical = 8.dp, horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            Plan.dayShortAr[i],
                            style = MaterialTheme.typography.labelSmall,
                            color = if (i == dow) C.text else C.muted,
                            textAlign = TextAlign.Center,
                        )
                        Text(if (s != null) "💪" else "😴", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Spacer(Modifier.height(100.dp))
    }
}

/** دائرة: كم يوم تمرنت هالأسبوع من أصل أيام خطتك. */
@Composable
private fun WeekRing() {
    val total = P.daysPerWeek
    val done = Store.doneThisWeek().coerceAtMost(total)
    val accent = C.accent
    Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = 8.dp.toPx()
            val inset = sw / 2
            val box = Size(size.width - sw, size.height - sw)
            val at = Offset(inset, inset)
            drawArc(
                color = C.track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = at,
                size = box,
                style = Stroke(sw),
            )
            if (total > 0) {
                drawArc(
                    color = accent,
                    startAngle = -90f,
                    sweepAngle = 360f * done / total,
                    useCenter = false,
                    topLeft = at,
                    size = box,
                    style = Stroke(sw, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$done/$total", style = MaterialTheme.typography.titleLarge)
            Text("الأسبوع", style = MaterialTheme.typography.labelSmall, color = C.muted)
        }
    }
}

private fun greeting(): String {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val part = when (h) {
        in 4..11 -> "صباح الخير"
        in 12..16 -> "مساء الخير"
        else -> "مساء الخير"
    }
    return if (P.name.isNotBlank()) "$part، ${P.name}" else part
}

internal fun stageLabel(): String = when (P.goal) {
    Goal.CUT -> "مرحلة التنشيف"
    Goal.BULK -> "مرحلة التضخيم"
    Goal.MAINTAIN -> "مرحلة التثبيت"
    else -> "مرحلة اللياقة"
}

internal fun fmt1(v: Float): String = (v * 10).roundToInt().let { "${it / 10}.${it % 10}" }
