package com.waleed.onyx.home

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.dp
import com.waleed.onyx.data.Kind
import com.waleed.onyx.grid.PagerPhysics
import kotlin.math.abs

private sealed class First {
    object Up : First()
    object Cancel : First()
    data class Moved(val delta: Offset) : First()
}

/**
 * مدير اللمس الوحيد للشاشة الرئيسية (كتاب المواصفات، الفصل ٤).
 * يسمع اللمسة قبل أي شي ثاني (Initial)، وما ياخذها إلا لما يقرر: تقليب صفحة، سحب، قائمة.
 * كذا الويدجت يستقبل الضغطات العادية، واللمسة ما تنقسم بين شيين أبد.
 */
fun Modifier.homeGestures(state: HomeState): Modifier = pointerInput(state) {
    val pass = PointerEventPass.Initial
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = pass)
        if (state.drag != null || state.resize != null || state.menu != null) return@awaitEachGesture
        val hit = state.hit(down.position)
        if (hit is Hit.Handle) return@awaitEachGesture

        val slop = viewConfiguration.touchSlop
        val holdMs = maxOf(viewConfiguration.longPressTimeoutMillis, 450L)
        val dragStart = 20.dp.toPx()
        val editing = state.editing
        val onItem = hit as? Hit.OnItem
        val isWidget = onItem?.item?.kind == Kind.WIDGET
        // لو الصفحات تتحرك: اللمسة توقفها بس، وما تفتح شي
        val pagesWereMoving = state.pages.animating
        if (pagesWereMoving) state.pages.stop()
        // بوضع الترتيب الويدجت ما يستقبل شي
        if (editing && onItem != null) down.consume()
        if (!editing && onItem != null && !isWidget && !pagesWereMoving) state.pressedKey = onItem.slot ?: onItem.item.id

        var last = down.position
        val first: First? = withTimeoutOrNull(holdMs) {
            var r: First? = null
            while (r == null) {
                val e = awaitPointerEvent(pass)
                val c = e.changes.firstOrNull { it.id == down.id }
                r = when {
                    c == null -> First.Cancel
                    !c.pressed -> First.Up
                    (c.position - down.position).getDistance() > slop -> First.Moved(c.position - down.position)
                    else -> null
                }
                if (c != null) {
                    last = c.position
                    if (editing && onItem != null) c.consume()
                }
            }
            r
        }
        state.pressedKey = null

        when (first) {
            First.Up -> {
                if (pagesWereMoving) state.pages.settle(state.calm()) else state.tap(hit)
            }
            First.Cancel -> state.pages.settle(state.calm())
            is First.Moved -> {
                val d = first.delta
                when {
                    // بوضع الترتيب: لمس أيقونة وتحريك = سحب على طول
                    editing && onItem != null -> {
                        // نمسكه من مكان اللمسة الأولى بالضبط، وبعدين يلحق الإصبع (بدون ما يتأخر عنه)
                        state.startDrag(onItem.item, onItem.slot, down.position)
                        state.dragMove(last)
                        dragLoop(state, down.id)
                    }
                    abs(d.x) > abs(d.y) && state.inPages(down.position) -> pageSwipe(state, down.id, down.position)
                    // الويدجت بالوضع العادي: السحب الطولي له (مثل قائمة التقويم)
                    isWidget && !editing -> {}
                    else -> verticalSwipe(state, down.id, down.position)
                }
                if (state.drag == null) state.pages.settle(state.calm())
            }
            null -> {
                // ضغطة طويلة
                if (editing) {
                    if (onItem != null) {
                        state.startDrag(onItem.item, onItem.slot, down.position)
                        state.dragMove(last)
                        dragLoop(state, down.id)
                    }
                } else when (hit) {
                    is Hit.OnItem -> {
                        state.openMenu(hit.item, hit.slot)
                        afterLongPress(state, down.id, last, hit, dragStart)
                    }
                    is Hit.OnEmptySlot -> state.openPicker(hit.slot)
                    Hit.Empty -> { state.buzz(); state.enterEdit() }
                    else -> {}
                }
            }
        }
    }
}

/** بعد الضغطة الطويلة: القائمة ظاهرة. لو حرّكت 20 يبدأ السحب، ولو رفعت تبقى القائمة */
private suspend fun AwaitPointerEventScope.afterLongPress(state: HomeState, id: PointerId, at: Offset, hit: Hit.OnItem, dragStart: Float) {
    while (true) {
        val e = awaitPointerEvent(PointerEventPass.Initial)
        val c = e.changes.firstOrNull { it.id == id } ?: return
        c.consume()
        if (!c.pressed) return
        if ((c.position - at).getDistance() > dragStart) {
            // نمسكه من مكان الضغطة الطويلة، وبعدين ينتقل لمكان الإصبع
            state.startDrag(hit.item, hit.slot, at)
            if (state.drag != null) {
                state.dragMove(c.position)
                dragLoop(state, id)
            }
            return
        }
    }
}

/** السحب: اللمسة كلها لنا لين ترفع إصبعك */
private suspend fun AwaitPointerEventScope.dragLoop(state: HomeState, id: PointerId) {
    while (true) {
        val e = awaitPointerEvent(PointerEventPass.Initial)
        val c = e.changes.firstOrNull { it.id == id }
        if (c == null) break
        c.consume()
        if (!c.pressed) break
        state.dragMove(c.position)
    }
    state.dragEnd()
}

/** تقليب الصفحات بإصبعك: الصفحة تلحق إصبعك، وإذا فلت تنتهي على صفحة كاملة */
private suspend fun AwaitPointerEventScope.pageSwipe(state: HomeState, id: PointerId, start: Offset) {
    val pages = state.pages
    pages.stop()
    val startOffset = pages.offset
    val startPage = PagerPhysics.nearest(startOffset, pages.count)
    val width = state.pageWidthPx()
    val tracker = VelocityTracker()
    while (true) {
        val e = awaitPointerEvent(PointerEventPass.Initial)
        val c = e.changes.firstOrNull { it.id == id } ?: break
        c.consume()
        tracker.addPosition(c.uptimeMillis, c.position)
        if (!c.pressed) break
        pages.offset = PagerPhysics.dragOffset(startOffset, c.position.x - start.x, width, pages.count)
    }
    val v = tracker.calculateVelocity().x / state.density
    val target = PagerPhysics.settleTarget(startPage, pages.offset, v, pages.count)
    pages.animateTo(target, PagerPhysics.settleDurationMs(pages.offset, target, state.calm()))
}

/** السحب لفوق = كل التطبيقات، لتحت = الإشعارات (من أي مكان) */
private suspend fun AwaitPointerEventScope.verticalSwipe(state: HomeState, id: PointerId, start: Offset) {
    var dy = 0f
    var dx = 0f
    while (true) {
        val e = awaitPointerEvent(PointerEventPass.Initial)
        val c = e.changes.firstOrNull { it.id == id } ?: return
        c.consume()
        dy = c.position.y - start.y
        dx = c.position.x - start.x
        if (!c.pressed) break
    }
    if (state.editing || abs(dx) > abs(dy)) return
    val d = state.density
    when {
        dy < -60f * d -> state.drawerOpen = true
        dy > 48f * d -> state.openNotifications()
    }
}
