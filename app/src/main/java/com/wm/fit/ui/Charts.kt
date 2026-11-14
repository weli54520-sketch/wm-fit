package com.wm.fit.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wm.fit.core.Dates
import com.wm.fit.core.Measure
import com.wm.fit.core.Workout
import kotlin.math.abs

/**
 * رسم خط واحد عبر الوقت (وزن، خصر، أو وزن تمرين).
 * خط واحد = بدون مفتاح ألوان، العنوان يكفي. اضغط على نقطة عشان تشوف قيمتها.
 * الشبكة خفيفة، والنص بألوان النص مو بلون الخط.
 */
@Composable
fun LineChart(points: List<Measure>, unit: String, modifier: Modifier = Modifier, color: Color = C.accent) {
    if (points.size < 2) {
        Text(
            "سجّل قياسين على الأقل عشان يطلع الرسم.",
            style = MaterialTheme.typography.bodySmall,
            color = C.muted,
        )
        return
    }
    var sel by remember(points) { mutableStateOf<Int?>(null) }
    val shown = sel ?: points.lastIndex
    val p = points[shown]
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 10.sp, color = C.muted)
    val grid = C.line
    val surface = C.card

    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            (if (sel == null) "آخر قياس: " else "") + "${Dates.short(p.date)} · ${Workout.kg(p.v)} $unit",
            style = MaterialTheme.typography.bodySmall,
            color = C.soft,
        )
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(150.dp)
                .pointerInput(points) {
                    detectTapGestures { off ->
                        val left = 34.dp.toPx()
                        val w = size.width - left - 8.dp.toPx()
                        val i = ((off.x - left) / w * (points.size - 1)).let { kotlin.math.round(it).toInt() }
                        sel = i.coerceIn(0, points.lastIndex)
                    }
                },
        ) {
            val left = 34.dp.toPx()
            val right = 8.dp.toPx()
            val top = 8.dp.toPx()
            val bottom = 18.dp.toPx()
            val w = size.width - left - right
            val h = size.height - top - bottom
            val vs = points.map { it.v }
            var lo = vs.min()
            var hi = vs.max()
            if (abs(hi - lo) < 0.5f) { lo -= 1f; hi += 1f }
            val pad = (hi - lo) * 0.1f
            lo -= pad; hi += pad
            fun x(i: Int) = left + w * i / (points.size - 1)
            fun y(v: Float) = top + h * (1f - (v - lo) / (hi - lo))

            // شبكة خفيفة: 3 خطوط
            for (k in 0..2) {
                val v = lo + (hi - lo) * k / 2f
                drawLine(grid, Offset(left, y(v)), Offset(size.width - right, y(v)), 1.dp.toPx())
                drawText(measurer, Workout.kg(Math.round(v * 10) / 10f), Offset(0f, y(v) - 7.dp.toPx()), labelStyle)
            }
            drawText(measurer, Dates.short(points.first().date), Offset(left, size.height - 14.dp.toPx()), labelStyle)
            val lastLabel = Dates.short(points.last().date)
            drawText(measurer, lastLabel, Offset(size.width - right - 26.dp.toPx(), size.height - 14.dp.toPx()), labelStyle)

            // الخط
            val path = Path()
            points.forEachIndexed { i, m -> if (i == 0) path.moveTo(x(i), y(m.v)) else path.lineTo(x(i), y(m.v)) }
            drawPath(path, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

            // النقاط: لو قليلة نرسمها كلها، لو كثيرة آخر نقطة بس
            if (points.size <= 16) {
                points.forEachIndexed { i, m -> drawCircle(color, 3.dp.toPx(), Offset(x(i), y(m.v))) }
            }
            // النقطة المختارة: خط عمودي + دائرة بحلقة
            val sx = x(shown)
            if (sel != null) drawLine(grid, Offset(sx, top), Offset(sx, top + h), 1.dp.toPx())
            drawCircle(surface, 6.dp.toPx(), Offset(sx, y(p.v)))
            drawCircle(color, 4.5.dp.toPx(), Offset(sx, y(p.v)))
        }
    }
}
