package com.waleed.onyx.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.waleed.onyx.apps.AppCatalog
import com.waleed.onyx.apps.AppInfo
import com.waleed.onyx.ui.Squircle
import com.waleed.onyx.ui.clearGlass
import com.waleed.onyx.ui.smokeGlass

/**
 * الأيقونة: الأصلية مثل ما يعطيها الجوال بالضبط (بدون خلفية أو قص)،
 * أو الرمز أبيض على زجاج شفاف، أو على زجاج داكن.
 */
@Composable
fun AppIcon(app: AppInfo?, catalog: AppCatalog, size: Dp, style: Int, modifier: Modifier = Modifier) {
    val px = with(LocalDensity.current) { size.roundToPx() }.coerceAtLeast(1)
    val glyphMode = style != 0
    val bitmap by produceState(app?.let { catalog.cachedIcon(it, px, false) }, app?.key, px) {
        if (app != null) value = catalog.icon(app, px)
    }
    val glyph by produceState(app?.let { if (glyphMode) catalog.cachedIcon(it, px, true) else null }, app?.key, px, glyphMode) {
        if (app != null && glyphMode) value = catalog.glyph(app, px)
    }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        when {
            style == 0 -> bitmap?.let { Image(it, contentDescription = app?.label, modifier = Modifier.size(size)) }
            else -> {
                val glass = if (style == 1) Modifier.clearGlass(Squircle) else Modifier.smokeGlass(Squircle, strong = true)
                Box(Modifier.size(size).then(glass), contentAlignment = Alignment.Center) {
                    val g = glyph
                    if (g != null) {
                        Image(g, contentDescription = app?.label, modifier = Modifier.size(size))
                    } else {
                        bitmap?.let { Image(it, contentDescription = app?.label, modifier = Modifier.size(size * 0.7f)) }
                    }
                }
            }
        }
    }
}

@Composable
fun AppLabel(text: String, width: Dp) {
    BasicText(
        text,
        modifier = Modifier.graphicsLayer { },
        style = TextStyle(
            color = Color.White,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            shadow = Shadow(Color(0x99000000), Offset(0f, 1f), 6f),
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** أيقونة مع الاسم (لو شغال من الإعدادات) */
@Composable
fun AppFace(app: AppInfo?, catalog: AppCatalog, icon: Dp, style: Int, labels: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        AppIcon(app, catalog, icon, style)
        if (labels && app != null) {
            Spacer(Modifier.height(5.dp))
            Box(Modifier.size(width = icon + 18.dp, height = 15.dp), contentAlignment = Alignment.Center) { AppLabel(app.label, icon + 18.dp) }
        }
    }
}

/** ضغطة طويلة على مكان فاضي = وضع الترتيب. ضغطة عادية بوضع الترتيب = خروج */
fun Modifier.emptyAreaGestures(state: HomeState): Modifier = pointerInput(state) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        // الأيقونة تستقبل اللمسة قبلنا: لو بدأت على أيقونة نتركها لها
        val onTile = state.tilePressed
        val slop = viewConfiguration.touchSlop
        val res: Int? = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            var r = -1
            while (r < 0) {
                val e = awaitPointerEvent()
                val c = e.changes.firstOrNull { it.id == down.id }
                r = when {
                    c == null -> 2
                    !c.pressed -> 0
                    (c.position - down.position).getDistance() > slop -> 2
                    else -> -1
                }
            }
            r
        }
        if (onTile) return@awaitEachGesture
        when (res) {
            null -> if (!state.editing) state.enterEdit()
            0 -> if (state.editing) state.exitEdit()
            else -> {}
        }
    }
}

/**
 * لمسات الأيقونة بالشبكة:
 * ضغطة = يفتح التطبيق. ضغطة طويلة = القائمة، ولو سحبت بعدها يبدأ النقل.
 * بوضع الترتيب: السحب يبدأ على طول.
 */
fun Modifier.tileGestures(
    state: HomeState,
    key: Any,
    coords: () -> LayoutCoordinates?,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    canDrag: Boolean,
    onDragStart: (Offset) -> Unit,
): Modifier = pointerInput(key, canDrag) {
    fun root(p: Offset): Offset = coords()?.localToRoot(p) ?: p
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        state.tilePressed = true
        val slop = viewConfiguration.touchSlop
        val editing = state.editing
        var lastPos = down.position
        val first: Int? = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            var r = -1
            while (r < 0) {
                val e = awaitPointerEvent()
                val c = e.changes.firstOrNull { it.id == down.id }
                r = when {
                    c == null -> 3
                    !c.pressed -> 0
                    (c.position - down.position).getDistance() > slop -> 2
                    else -> -1
                }
                if (c != null) {
                    lastPos = c.position
                    // بوضع الترتيب نمسك اللمسة عشان الصفحات ما تتقلب بدال السحب
                    if (editing && canDrag && c.pressed) c.consume()
                }
            }
            r
        }
        var dragging = false
        when (first) {
            0 -> { state.tilePressed = false; onTap(); return@awaitEachGesture }
            3 -> { state.tilePressed = false; return@awaitEachGesture }
            2 -> {
                if (editing && canDrag) { onDragStart(root(lastPos)); dragging = state.drag != null }
                else { state.tilePressed = false; return@awaitEachGesture }
            }
            null -> {
                if (editing && canDrag) { onDragStart(root(lastPos)); dragging = state.drag != null }
                else onLongPress()
            }
        }
        while (true) {
            val e = awaitPointerEvent()
            val c = e.changes.firstOrNull { it.id == down.id } ?: break
            if (!c.pressed) break
            if (!dragging && canDrag && (c.position - down.position).getDistance() > slop * 1.5f) {
                onDragStart(root(c.position))
                dragging = state.drag != null
            }
            if (dragging) {
                c.consume()
                state.dragMove(root(c.position))
            }
        }
        state.tilePressed = false
        if (dragging) state.dragEnd()
    }
}

/** يحفظ مكان العنصر على الشاشة (للقائمة والسحب وفتح التطبيق) */
fun Modifier.trackBounds(onBounds: (androidx.compose.ui.geometry.Rect, LayoutCoordinates) -> Unit): Modifier =
    onGloballyPositioned { onBounds(it.boundsInRoot(), it) }

@Composable
fun rememberCoordsHolder(): Array<LayoutCoordinates?> = remember { arrayOfNulls(1) }
