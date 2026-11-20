package com.waleed.onyx.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.waleed.onyx.data.Container
import com.waleed.onyx.ui.Motion
import com.waleed.onyx.ui.Onyx
import com.waleed.onyx.ui.smokeGlass
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val Label = TextStyle(color = Onyx.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

@Composable
fun Pill(text: String, filled: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .height(36.dp)
            .then(if (filled) Modifier.clip(shape).background(Color.White) else Modifier.smokeGlass(shape, strong = true))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, style = Label.copy(color = if (filled) Color(0xFF111111) else Onyx.Text))
    }
}

/** شريط وضع الترتيب فوق */
@Composable
fun EditBar(state: HomeState, top: Dp) {
    AnimatedVisibility(
        visible = state.editing,
        enter = fadeIn() + slideInVertically { -it / 2 },
        exit = fadeOut() + slideOutVertically { -it / 2 },
        modifier = Modifier.offset(y = top + 6.dp).fillMaxWidth().padding(horizontal = 14.dp),
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill("الإعدادات") { state.settingsOpen = true }
                Spacer(Modifier.width(8.dp))
                Pill("كل التطبيقات") { state.drawerOpen = true }
                Spacer(Modifier.weight(1f))
                Pill("تم", filled = true) { state.exitEdit() }
            }
        }
    }
}

/** القائمة بعد الضغطة الطويلة: الأيقونة ترتفع والخلفية تغمق */
@Composable
fun ContextMenu(state: HomeState) {
    val m = state.menu
    val shown by animateFloatAsState(if (m != null) 1f else 0f, Motion.smooth(state.settings), label = "menu")
    val last = remember { arrayOfNulls<Menu>(1) }
    if (m != null) last[0] = m
    val menu = last[0] ?: return
    if (shown < 0.01f && m == null) return
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // تغميق الخلفية: ضغطة تسكر القائمة
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = shown }
                .background(Color(0x8C000000))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { state.menu = null },
        )
        val b = menu.bounds
        val appInfo = state.catalog.byKey(menu.item.appKey)
        // الأيقونة مرفوعة بمكانها
        with(density) {
            val iconSize = (if (menu.slot != null) b.width else minOf(b.width, b.height) * 0.76f * state.settings.iconScale).toDp()
            Box(
                Modifier
                    .offset { IntOffset(b.left.roundToInt(), b.top.roundToInt()) }
                    .size(b.width.toDp(), b.height.toDp())
                    .graphicsLayer {
                        val s = 1f + 0.08f * shown
                        scaleX = s; scaleY = s; alpha = shown
                    },
                contentAlignment = Alignment.Center,
            ) { AppIcon(appInfo, state.catalog, iconSize, state.settings.iconStyle) }

            val menuW = 230.dp
            val itemsCount = if (menu.slot != null) 2 else 3
            val menuH = (46 * itemsCount + 12).dp
            val below = b.bottom + 14.dp.toPx() + menuH.toPx() < constraints.maxHeight - 40.dp.toPx()
            val y = if (below) b.bottom + 14.dp.toPx() else b.top - 14.dp.toPx() - menuH.toPx()
            val x = (b.center.x - menuW.toPx() / 2).coerceIn(12.dp.toPx(), constraints.maxWidth - menuW.toPx() - 12.dp.toPx())
            Box(
                Modifier
                    .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                    .width(menuW)
                    .graphicsLayer {
                        alpha = shown
                        val s = 0.85f + 0.15f * shown
                        scaleX = s; scaleY = s
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, if (below) 0f else 1f)
                    }
                    .smokeGlass(RoundedCornerShape(22.dp), strong = true, elevation = 12.dp)
                    .padding(6.dp),
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Column {
                        if (menu.slot != null) {
                            MenuRow("بدّل التطبيق") { state.menu = null; state.picker = menu.slot; state.drawerOpen = true }
                            MenuRow("ترتيب الشاشة") { state.enterEdit() }
                        } else {
                            MenuRow("ترتيب الشاشة") { state.enterEdit() }
                            MenuRow("معلومات التطبيق") { state.menu = null; appInfo?.let { state.catalog.openInfo(it) } }
                            MenuRow("شيله من الشاشة", red = true) { state.menu = null; state.removeFromHome(menu.item) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuRow(text: String, red: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicText(text, style = Label.copy(fontWeight = FontWeight.Medium, color = if (red) Color(0xFFFF8A8A) else Onyx.Text))
    }
}

/** رسالة صغيرة تختفي لحالها */
@Composable
fun ToastPill(state: HomeState, bottom: Dp) {
    val msg = state.toast
    LaunchedEffect(msg) {
        if (msg != null) { delay(1800); state.toast = null }
    }
    val last = remember { arrayOfNulls<String>(1) }
    if (msg != null) last[0] = msg
    Box(Modifier.fillMaxSize().padding(bottom = bottom), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(visible = msg != null, enter = fadeIn() + scaleIn(initialScale = 0.9f), exit = fadeOut() + scaleOut(targetScale = 0.9f)) {
            Box(Modifier.smokeGlass(RoundedCornerShape(50), strong = true).padding(horizontal = 18.dp, vertical = 10.dp)) {
                BasicText(last[0] ?: "", style = Label.copy(fontWeight = FontWeight.Medium, fontSize = 13.sp))
            }
        }
    }
}
