package com.waleed.launcher.trial.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BackGold = Color(0xFFE3C285)

/**
 * ظهر الويدجت لما ينقلب (مثل "تعديل الويدجت" بالآيفون):
 * خيارات سريعة وقليلة بس. التصميم الكامل بالاستوديو.
 */
@Composable
fun WidgetBack(
    cfg: WConfig,
    designs: List<WConfig>,
    onChange: (WConfig) -> Unit,
    onStudio: () -> Unit,
    onDone: () -> Unit,
) {
    val variants = WVariant.entries.filter { it.type == cfg.variant.type }
    fun cycleVariant(step: Int) {
        val i = (variants.indexOf(cfg.variant) + step + variants.size) % variants.size
        val v = variants[i]
        onChange(if (v.isAnalog) WConfig.preset(v, cfg) else cfg.copy(variant = v))
    }
    fun cycleGlass(step: Int) {
        val all = Glass.entries
        onChange(cfg.copy(glass = all[(all.indexOf(cfg.glass) + step + all.size) % all.size]))
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(Modifier.fillMaxSize().background(Color(0xF0141416))) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Cycler("الشكل", cfg.variant.title, { cycleVariant(-1) }, { cycleVariant(1) })
                Cycler("الخلفية", cfg.glass.title, { cycleGlass(-1) }, { cycleGlass(1) })
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Accent.entries.forEach { a ->
                        Box(
                            Modifier.size(22.dp).clip(CircleShape).background(a.color)
                                .border(if (cfg.accent == a) 2.dp else 0.dp, Color.White, CircleShape)
                                .clickable { onChange(cfg.copy(accent = a)) },
                        )
                    }
                }
                if (designs.isNotEmpty()) {
                    WText("تصاميمي", 11.sp, color = Soft, shadow = false, align = androidx.compose.ui.text.style.TextAlign.Start, modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        designs.forEachIndexed { i, d ->
                            SmallPill("${i + 1} · ${d.variant.title}") { onChange(d) }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SmallPill("الاستوديو", onClick = onStudio)
                    SmallPill("تم ✓", onClick = onDone, selected = true)
                }
            }
        }
    }
}

@Composable
private fun Cycler(label: String, value: String, onPrev: () -> Unit, onNext: () -> Unit) {
    Column {
        WText(label, 11.sp, color = Soft, shadow = false, align = androidx.compose.ui.text.style.TextAlign.Start, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(3.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.08f)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WText("‹", 18.sp, color = BackGold, shadow = false, modifier = Modifier.clickable(onClick = onPrev).padding(horizontal = 12.dp, vertical = 4.dp))
            WText(value, 13.sp, weight = FontWeight.Medium, shadow = false, modifier = Modifier.weight(1f))
            WText("›", 18.sp, color = BackGold, shadow = false, modifier = Modifier.clickable(onClick = onNext).padding(horizontal = 12.dp, vertical = 4.dp))
        }
    }
}

@Composable
private fun SmallPill(text: String, selected: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(50))
            .background(if (selected) BackGold.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f))
            .border(1.dp, if (selected) BackGold else Color.White.copy(alpha = 0.14f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        WText(text, 12.sp, color = if (selected) BackGold else Color.White, shadow = false)
    }
}
