package com.waleed.onyx.home

import android.os.Bundle
import android.util.SizeF
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.waleed.onyx.data.Item
import com.waleed.onyx.ui.Motion
import com.waleed.onyx.ui.Onyx
import com.waleed.onyx.ui.smokeGlass

/** المسافة حول كل ويدجت عشان ما يلصق باللي جنبه */
val WidgetMargin = 5.dp
/** المسافة بين إطار الزجاج والويدجت نفسه */
val FramePad = 6.dp
val WidgetCorner = 26.dp

/**
 * ويدجت تطبيق ثاني على الشبكة.
 * برا وضع الترتيب: اللمس يروح للويدجت، والضغطة الطويلة قائمة (ولو سحبت بعدها ينتقل).
 * بوضع الترتيب: اللمس كله لنا (سحب)، وزاوية تكبير تحت.
 */
@Composable
fun WidgetTile(state: HomeState, item: Item, page: Int, cellW: Dp, cellH: Dp) {
    val settings = state.settings
    val density = LocalDensity.current
    val data = state.widgetData(item)
    val p = state.previewCell(page, item.id)
    val cx = p?.x ?: item.x
    val cy = p?.y ?: item.y
    val target = with(density) { IntOffset((cellW * cx).roundToPx(), (cellH * cy).roundToPx()) }
    val anim = remember { Animatable(target, IntOffset.VectorConverter) }
    LaunchedEffect(target, state.dropTick) {
        state.takeDropFrom(item.id)?.let { anim.snapTo(it) }
        anim.animateTo(target, Motion.bouncy(settings))
    }
    val r = state.resize?.takeIf { it.item.id == item.id }
    val spanW = r?.w ?: item.w
    val spanH = r?.h ?: item.h
    val boxW by animateDpAsState(cellW * spanW, Motion.bouncy(settings), label = "wW")
    val boxH by animateDpAsState(cellH * spanH, Motion.bouncy(settings), label = "wH")
    val dragged = state.drag?.item?.id == item.id
    val coords = rememberCoordsHolder()
    val frame = data?.frame != false
    val info = remember(data?.id) { data?.let { state.app.widgets.info(it.id) } }

    Box(
        Modifier
            .offset { anim.value }
            .size(boxW, boxH)
            .graphicsLayer { alpha = if (dragged) 0f else 1f }
            .trackBounds { b, c -> coords[0] = c; state.tileBounds[item.id] = b },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(WidgetMargin)
                .then(if (frame) Modifier.smokeGlass(RoundedCornerShape(WidgetCorner)) else Modifier)
                .widgetGestures(state, item) { coords[0] },
        ) {
            val inset = if (frame) FramePad else 0.dp
            // المقاس اللي نقوله للتطبيق: المساحة الفعلية للويدجت
            val contentW = cellW * spanW - WidgetMargin * 2 - inset * 2
            val contentH = cellH * spanH - WidgetMargin * 2 - inset * 2
            if (data != null && info != null) {
                AndroidView(
                    factory = { ctx ->
                        state.app.widgets.host.createView(ctx, data.id, info).also { state.widgetViews[item.id] = it }
                    },
                    onRelease = { v -> if (state.widgetViews[item.id] === v) state.widgetViews.remove(item.id) },
                    update = { v ->
                        val key = "${contentW.value.toInt()}x${contentH.value.toInt()}"
                        if (v.tag != key) {
                            v.tag = key
                            try { v.updateAppWidgetSize(Bundle(), listOf(SizeF(contentW.value, contentH.value))) } catch (_: Exception) {}
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(inset)
                        .then(if (frame) Modifier.clip(RoundedCornerShape(WidgetCorner - inset)) else Modifier),
                )
            } else {
                Box(Modifier.fillMaxSize().then(if (frame) Modifier else Modifier.smokeGlass(RoundedCornerShape(WidgetCorner))), contentAlignment = Alignment.Center) {
                    BasicText(
                        "الويدجت مو متوفر\nضغطة طويلة عشان تشيله",
                        style = TextStyle(color = Onyx.Muted, fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 18.sp),
                    )
                }
            }
        }
        // زاوية التكبير: بوضع الترتيب بس
        val showHandle = state.editing && state.drag == null && state.canResize(item)
        val handleA by animateFloatAsState(if (showHandle) 1f else 0f, Motion.smooth(settings), label = "handle")
        if (handleA > 0.01f) ResizeHandle(state, item, handleA, Modifier.align(Alignment.BottomEnd))
    }
}

/** مقبض بالزاوية: تسحبه وتكبر أو تصغر الويدجت خانة خانة */
@Composable
private fun ResizeHandle(state: HomeState, item: Item, alpha: Float, modifier: Modifier) {
    val active = state.resize?.item?.id == item.id
    val scale by animateFloatAsState(if (active) 1.25f else 1f, Motion.bouncy(state.settings), label = "hs")
    Box(
        modifier
            .size(40.dp)
            .graphicsLayer { this.alpha = alpha; scaleX = scale; scaleY = scale }
            .pointerInput(item.id) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    state.tilePressed = true
                    down.consume()
                    state.resizeStart(item)
                    var total = Offset.Zero
                    while (true) {
                        val e = awaitPointerEvent()
                        val c = e.changes.firstOrNull { it.id == down.id } ?: break
                        if (!c.pressed) { c.consume(); break }
                        total += c.position - c.previousPosition
                        c.consume()
                        state.resizeMove(total.x, total.y)
                    }
                    state.tilePressed = false
                    state.resizeEnd()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(20.dp)
                .drawBehind {
                    val rad = size.minDimension / 2
                    drawCircle(Color(0xCC111114), rad)
                    drawCircle(Color.White, rad * 0.62f)
                    drawCircle(Color.White.copy(alpha = 0.55f), rad, style = Stroke(1.dp.toPx()))
                },
        )
    }
}

/**
 * لمسات الويدجت. نسمع اللمسة قبل الويدجت (Initial) بدون ما ناخذها،
 * فالأزرار اللي داخله تشتغل عادي. لو صارت ضغطة طويلة، ناخذ اللمسة منه ونفتح القائمة.
 */
private fun Modifier.widgetGestures(state: HomeState, item: Item, coords: () -> LayoutCoordinates?): Modifier = pointerInput(item.id) {
    fun root(p: Offset): Offset = coords()?.localToRoot(p) ?: p
    val pass = PointerEventPass.Initial
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = pass)
        state.tilePressed = true
        val editing = state.editing
        // بوضع الترتيب الويدجت ما يستقبل شي
        if (editing) down.consume()
        val slop = viewConfiguration.touchSlop
        val holdMs = maxOf(viewConfiguration.longPressTimeoutMillis, 450L)
        var lastPos = down.position
        val first: Int? = withTimeoutOrNull(holdMs) {
            var res = -1
            while (res < 0) {
                val e = awaitPointerEvent(pass)
                val c = e.changes.firstOrNull { it.id == down.id }
                res = when {
                    c == null -> 3
                    !c.pressed -> 0
                    (c.position - down.position).getDistance() > slop -> 2
                    else -> -1
                }
                if (c != null) {
                    lastPos = c.position
                    if (editing) c.consume()
                }
            }
            res
        }
        when (first) {
            0, 3 -> { state.tilePressed = false; return@awaitEachGesture }
            2 -> {
                if (editing) state.startDrag(item, root(lastPos))
                else { state.tilePressed = false; return@awaitEachGesture }
            }
            null -> {
                if (editing) state.startDrag(item, root(lastPos)) else state.openMenu(item, null)
            }
        }
        var dragging = state.drag != null
        while (true) {
            val e = awaitPointerEvent(pass)
            val c = e.changes.firstOrNull { it.id == down.id } ?: break
            // من هنا اللمسة لنا: الويدجت يوصله إلغاء
            c.consume()
            if (!c.pressed) break
            if (!dragging && (c.position - down.position).getDistance() > slop * 1.5f) {
                state.startDrag(item, root(c.position))
                dragging = state.drag != null
            }
            if (dragging) state.dragMove(root(c.position))
        }
        state.tilePressed = false
        if (dragging) state.dragEnd()
    }
}
