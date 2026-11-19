package com.waleed.launcher.trial.widgets

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.waleed.launcher.trial.glass.GlassTone
import com.waleed.launcher.trial.glass.LiquidGlass

private val MenuGold = Color(0xFFE3C285)
private val Danger = Color(0xFFFF7A7A)

/**
 * القائمة اللي تطلع لما تضغط ضغطة طويلة على الويدجت (مثل الآيفون):
 * المقاسات بضغطة، تعديل، ترتيب الشاشة، المكدس، حذف.
 */
@Composable
fun WidgetContextMenu(
    placed: Placed?,
    anchor: Rect?,
    screenWidthPx: Float,
    screenHeightPx: Float,
    maxCols: Int,
    canFit: (WSize) -> Boolean,
    onSize: (WSize) -> Unit,
    onEdit: () -> Unit,
    onStudio: () -> Unit,
    onArrange: () -> Unit,
    onToggleRotate: () -> Unit,
    onRemoveFromStack: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val visible = placed != null && anchor != null
    val appear by animateFloatAsState(if (visible) 1f else 0f, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow), label = "menu")
    var lastPlaced by remember { mutableStateOf(placed) }
    var lastAnchor by remember { mutableStateOf(anchor) }
    if (placed != null) lastPlaced = placed
    if (anchor != null) lastAnchor = anchor
    if (appear < 0.01f && !visible) return
    val p = lastPlaced ?: return
    val a = lastAnchor ?: return
    val noRipple = remember { MutableInteractionSource() }
    val density = LocalDensity.current
    var menuH by remember { mutableIntStateOf(0) }
    val menuW = with(density) { 270.dp.toPx() }
    val gap = with(density) { 10.dp.toPx() }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = appear }
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(interactionSource = noRipple, indication = null) { onDismiss() },
        ) {
            // تحت الويدجت لو فيه مكان، وإلا فوقه
            val below = a.bottom + gap + menuH < screenHeightPx * 0.94f
            val y = if (below) a.bottom + gap else a.top - gap - menuH
            val x = (a.center.x - menuW / 2).coerceIn(gap, screenWidthPx - menuW - gap)
            Box(
                Modifier
                    .offset { IntOffset(x.toInt(), y.toInt()) }
                    .width(270.dp)
                    .onSizeChanged { menuH = it.height }
                    .graphicsLayer {
                        val s = 0.85f + 0.15f * appear
                        scaleX = s
                        scaleY = s
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, if (below) 0f else 1f)
                    }
                    .clickable(interactionSource = noRipple, indication = null) { },
            ) {
                LiquidGlass(Modifier.fillMaxWidth(), 24.dp, tone = GlassTone.DARK) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                        // المقاسات: أشكال تضغطها وتتغير على طول
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            WSize.entries.filter { it.w <= maxCols }.forEach { z ->
                                val current = p.size == z
                                val ok = current || canFit(z)
                                Box(
                                    Modifier
                                        .size((z.w * 8 + 4).dp, (z.h * 8 + 4).dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(if (current) MenuGold.copy(alpha = 0.35f) else Color.White.copy(alpha = if (ok) 0.12f else 0.04f))
                                        .border(1.5.dp, if (current) MenuGold else Color.White.copy(alpha = if (ok) 0.6f else 0.15f), RoundedCornerShape(5.dp))
                                        .clickable(enabled = ok && !current) { onSize(z) },
                                )
                            }
                        }
                        Divider()
                        MenuRow("تعديل الويدجت", "✎", onEdit)
                        MenuRow("الاستوديو (تصميم كامل)", "◐", onStudio)
                        MenuRow("ترتيب الشاشة", "⊞", onArrange)
                        if (p.slot.items.size > 1) {
                            Divider()
                            MenuRow(if (p.slot.autoRotate) "التقليب التلقائي: شغال" else "التقليب التلقائي: مطفي", "↻", onToggleRotate)
                            MenuRow("شيل هذا من المكدس", "−", onRemoveFromStack)
                        }
                        Divider()
                        MenuRow("حذف الويدجت", "✕", onDelete, color = Danger)
                    }
                }
            }
        }
    }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp).background(Color.White.copy(alpha = 0.08f)))
}

@Composable
private fun MenuRow(text: String, icon: String, onClick: () -> Unit, color: Color = Color.White) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WText(text, 15.sp, color = color, align = TextAlign.Start, shadow = false, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(10.dp))
        WText(icon, 16.sp, color = color.copy(alpha = 0.8f), shadow = false, weight = FontWeight.Medium)
    }
}
