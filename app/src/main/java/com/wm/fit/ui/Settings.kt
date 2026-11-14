package com.wm.fit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import com.wm.fit.core.Exercises
import com.wm.fit.core.Muscle
import com.wm.fit.core.Store
import com.wm.fit.core.P

@Composable
fun Settings(nav: Nav, onRestart: () -> Unit) {
    var confirmWipe by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text("الإعدادات", style = MaterialTheme.typography.headlineMedium)

        SectionLabel("التركيز")
        Card(radius = 22) {
            Text(
                if (P.focus.isEmpty()) "خطة متوازنة"
                else P.focus.mapNotNull { Muscle.byId(it)?.ar }.joinToString(" · "),
                style = MaterialTheme.typography.titleMedium,
            )
            Row(
                Modifier.fillMaxWidth().heightIn(min = 44.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("المدرب يقترح عضلات متأخرة", style = MaterialTheme.typography.bodyMedium)
                Switch(P.coachSuggests) { P.coachSuggests = it }
            }
        }

        SectionLabel("الهدف والتمرين")
        Card(radius = 22) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("الهدف", style = MaterialTheme.typography.bodyMedium)
                Text(stageLabel(), style = MaterialTheme.typography.bodyMedium, color = C.accent)
            }
            Text("أيام التمرين", style = MaterialTheme.typography.bodyMedium)
            DayPicker(P.trainDays) { P.trainDays = it }
            Text(
                "عشان تغيّر هدفك أو عضلات التركيز، اضغط \"أعد أسئلة البداية\" تحت.",
                style = MaterialTheme.typography.labelSmall,
                color = C.muted,
            )
        }

        SectionLabel("التمارين")
        Card(radius = 22, onClick = { nav.injuries() }) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("خريطة الإصابات", style = MaterialTheme.typography.bodyMedium)
                Text("›", style = MaterialTheme.typography.titleMedium, color = C.muted)
            }
        }
        Card(radius = 22, onClick = { nav.library() }) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("مكتبة التمارين", style = MaterialTheme.typography.bodyMedium)
                Text("›", style = MaterialTheme.typography.titleMedium, color = C.muted)
            }
        }
        if (Store.swaps.isNotEmpty()) {
            Card(radius = 22) {
                Text("البدائل اللي اخترتها", style = MaterialTheme.typography.bodyMedium)
                Store.swaps.forEach { (from, to) ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${Exercises.get(from)?.ar ?: from} ← ${Exercises.get(to)?.ar ?: to}",
                            style = MaterialTheme.typography.bodySmall,
                            color = C.muted,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "رجّع",
                            style = MaterialTheme.typography.bodySmall,
                            color = C.accent,
                            modifier = Modifier.clickable { Store.clearSwap(from) }.padding(6.dp),
                        )
                    }
                }
            }
        }

        SectionLabel("الشكل")
        Card(radius = 22) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 44.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("اللون", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    C.accents.forEachIndexed { i, pair ->
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(pair.first)
                                .then(
                                    if (P.accent == i) Modifier.border(3.dp, C.text, CircleShape)
                                    else Modifier,
                                )
                                .clickable { P.accent = i },
                        )
                    }
                }
            }
        }

        SectionLabel("الخصوصية")
        Card(radius = 22) {
            Text("بياناتك في جوالك بس", style = MaterialTheme.typography.titleMedium)
            Text(
                "ما فيه حساب ولا خادم. التطبيق ما يرسل أي شي، والنت يستخدمه للتحديث بس. " +
                    "أوزانك وإصاباتك تنحفظ داخل جوالك. " +
                    "وقفلنا النسخة الاحتياطية التلقائية عشان بياناتك ما تطلع لسحابة جوجل.",
                style = MaterialTheme.typography.bodySmall,
                color = C.muted,
            )
        }

        Card(radius = 22, onClick = { nav.sources() }) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("من وين أرقامنا؟", style = MaterialTheme.typography.bodyMedium)
                Text("›", style = MaterialTheme.typography.titleMedium, color = C.muted)
            }
        }

        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, C.line, RoundedCornerShape(18.dp))
                .clickable { P.onboarded = false; onRestart() },
            contentAlignment = Alignment.Center,
        ) { Text("أعد أسئلة البداية", style = MaterialTheme.typography.bodyMedium) }

        Box(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, C.bad, RoundedCornerShape(18.dp))
                .clickable { confirmWipe = true },
            contentAlignment = Alignment.Center,
        ) { Text("امسح كل بياناتي", style = MaterialTheme.typography.bodyMedium, color = C.bad) }

        Text(
            "WM Fit · إصدار تجريبي · إرشادات عامة مو بديل عن الطبيب",
            style = MaterialTheme.typography.labelSmall,
            color = C.muted,
        )
        Spacer(Modifier.height(100.dp))
    }

    if (confirmWipe) {
        AlertDialog(
            onDismissRequest = { confirmWipe = false },
            containerColor = C.card,
            title = { Text("أكيد تبي تمسح كل شي؟") },
            text = { Text("بياناتك وقياساتك وإصاباتك كلها بتنمسح، وما ترجع.") },
            confirmButton = {
                TextButton(onClick = { P.wipe(); confirmWipe = false; onRestart() }) {
                    Text("امسح", color = C.bad)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmWipe = false }) { Text("خلاص لا") }
            },
        )
    }
}

/**
 * مفتاح تشغيل وإطفاء.
 * المفعّل: الدائرة على جهة النهاية (End)، يعني يسار في العربي ويمين في الإنجليزي،
 * مثل مفاتيح أندرويد نفسها.
 */
@Composable
fun Switch(on: Boolean, onChange: (Boolean) -> Unit) {
    Box(
        Modifier
            .size(56.dp, 34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(if (on) C.accent else C.track)
            .clickable { onChange(!on) }
            .padding(3.dp),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (on) C.onAccent else C.muted),
        )
    }
}
