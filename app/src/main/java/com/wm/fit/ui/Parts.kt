package com.wm.fit.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wm.fit.core.Plan

/** بطاقة: الشكل الأساسي لكل المحتوى. */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    bg: Color = C.card,
    radius: Int = 24,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radius.dp))
            .background(bg)
            .border(1.dp, C.line, RoundedCornerShape(radius.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

/** الزر الأساسي: ممتلئ باللون المميز. */
@Composable
fun BigButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = C.accent,
            contentColor = C.onAccent,
            disabledContainerColor = C.track,
            disabledContentColor = C.muted,
        ),
    ) { Text(text, style = MaterialTheme.typography.titleMedium) }
}

/** زر اختيار: ينوّر لما يكون مختار. */
@Composable
fun Choice(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    sub: String? = null,
    tint: Color = C.accent,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) tint.copy(alpha = 0.12f) else C.card)
            .border(
                BorderStroke(1.5.dp, if (selected) tint else C.line),
                RoundedCornerShape(18.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) tint else C.text,
        )
        if (sub != null) {
            Text(sub, style = MaterialTheme.typography.bodySmall, color = C.muted)
        }
    }
}

/** صف أزرار صغيرة: واحد مختار. */
@Composable
fun <T> Segmented(
    options: List<Pair<T, String>>,
    selected: T,
    modifier: Modifier = Modifier,
    onSelect: (T) -> Unit,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(C.bg)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (value, label) ->
            val on = value == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) C.track else Color.Transparent)
                    .clickable { onSelect(value) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (on) C.text else C.muted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** شريط تقدّم بسيط. */
@Composable
fun Bar(fraction: Float, color: Color = C.accent, height: Int = 8) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(height.dp))
            .background(C.track),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(height.dp))
                .background(color),
        )
    }
}

/** رقم مع وصفه تحته. */
@Composable
fun Stat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(C.card)
            .border(1.dp, C.line, RoundedCornerShape(20.dp))
            .padding(vertical = 14.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelSmall, color = C.muted)
    }
}

/** صندوق ملاحظة أو تنبيه. */
@Composable
fun Note(text: String, warn: Boolean = false) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (warn) C.warnBg else C.accentBg)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = if (warn) C.warnText else C.soft,
        )
    }
}

/** عنوان قسم صغير. */
@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = C.muted,
        modifier = Modifier.padding(start = 6.dp),
    )
}

/** عنوان الشاشة مع زر رجوع. */
@Composable
fun Header(title: String, sub: String? = null, onBack: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (onBack != null) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(C.card)
                    .border(1.dp, C.line, RoundedCornerShape(14.dp))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                // في العربي السهم يأشر لليمين للرجوع
                Text("→", style = MaterialTheme.typography.titleMedium)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = C.muted)
            Text(title, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

/** زر ثانوي بإطار. */
@Composable
fun OutlineButton(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = C.text,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, if (color == C.text) C.line else color.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = MaterialTheme.typography.bodyMedium, color = color, textAlign = TextAlign.Center) }
}

/**
 * اختيار أيام التمرين: المستخدم يضغط على الأيام اللي يبيها.
 * لازم بين [Plan.MIN_DAYS] و [Plan.MAX_DAYS] أيام.
 */
@Composable
fun DayPicker(selected: List<Int>, onChange: (List<Int>) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            (0..6).forEach { d ->
                val on = d in selected
                Box(
                    Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (on) C.accent else C.bg)
                        .border(1.dp, if (on) C.accent else C.line, RoundedCornerShape(12.dp))
                        .clickable {
                            val next = if (on) selected - d else selected + d
                            if (next.size in Plan.MIN_DAYS..Plan.MAX_DAYS) onChange(next.sorted())
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        Plan.dayShortAr[d],
                        style = MaterialTheme.typography.labelSmall,
                        color = if (on) C.onAccent else C.muted,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Text(
            "${selected.size} أيام بالأسبوع. اختر من ${Plan.MIN_DAYS} إلى ${Plan.MAX_DAYS} أيام، " +
                "والباقي راحة.",
            style = MaterialTheme.typography.labelSmall,
            color = C.muted,
        )
    }
}
