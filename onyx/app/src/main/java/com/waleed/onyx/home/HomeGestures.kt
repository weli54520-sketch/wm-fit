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
import com.waleed.onyx.grid.GestureMath
import com.waleed.onyx.grid.PagerPhysics
import kotlin.math.abs

private sealed class First {
    object Up : First()
    object Cancel : First()
    data class Moved(val delta: Offset) : First()
    /** إصبع ثاني نزل قبل ما نقرر: إيماءة بإصبعين */
    data class Two(val a: Offset, val b: Offset, val idB: PointerId) : First()
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
        if (state.drag != null || state.resize != null || state.menu != null || state.openFolder != null || state.popup != null) return@awaitEachGesture
        val hit = state.hit(down.position)
        // مقبض تكبير الويدجت: الويدجت يتبع الإصبع (نحسب من الشاشة، مو من المقبض اللي يتحرك)
        if (hit is Hit.Handle) {
            down.consume()
            resizeLoop(state, down.id, down.position, hit)
            return@awaitEachGesture
        }

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
        var lastTime = down.uptimeMillis
        // سرعة الإصبع من أول لمسة، بكل النقاط (حتى المجمّعة لو الجوال تأخر إطار)
        val tracker = VelocityTracker()
        tracker.addPosition(down.uptimeMillis, down.position)
        val first: First? = withTimeoutOrNull(holdMs) {
            var r: First? = null
            while (r == null) {
                val e = awaitPointerEvent(pass)
                val c = e.changes.firstOrNull { it.id == down.id }
                val other = e.changes.firstOrNull { it.id != down.id && it.pressed }
                r = when {
                    c != null && c.pressed && other != null -> First.Two(c.position, other.position, other.id)
                    c == null -> First.Cancel
                    !c.pressed -> First.Up
                    (c.position - down.position).getDistance() > slop -> First.Moved(c.position - down.position)
                    else -> null
                }
                if (c != null) {
                    track(tracker, c)
                    last = c.position
                    lastTime = c.uptimeMillis
                    if (editing && onItem != null) c.consume()
                }
            }
            r
        }
        state.pressedKey = null

        when (first) {
            is First.Two -> {
                state.pressedKey = null
                twoFingerLoop(state, down.id, first.idB, first.a, first.b)
                state.pages.settle(state.calm())
            }
            First.Up -> when {
                // النقاط: تروح للصفحة اللي ضغطت نقطتها
                hit is Hit.Dots -> state.dotsGo(last.x, fast = false)
                // ضغطتين على مكان فاضي = إيماءة (قفل الشاشة بالأساس)
                hit is Hit.Empty && !pagesWereMoving -> state.emptyTap(down.position)
                pagesWereMoving -> state.pages.settle(state.calm())
                else -> state.tap(hit)
            }
            First.Cancel -> state.pages.settle(state.calm())
            is First.Moved -> {
                val d = first.delta
                when {
                    // السحب على النقاط: تتنقل بين الصفحات مع إصبعك (مثل الآيفون)
                    hit is Hit.Dots && abs(d.x) >= abs(d.y) -> dotsScrub(state, down.id, last)
                    // بوضع الترتيب: سحبة سريعة بالعرض فوق الأيقونات = تقليب صفحات (مثل الآيفون)
                    editing && onItem != null && onItem.slot == null && state.inPages(down.position) &&
                        PagerPhysics.editTouchIsPageSwipe(lastTime - down.uptimeMillis, d.x, d.y) ->
                        pageSwipe(state, down.id, last, tracker)
                    // بوضع الترتيب: إمساك وتحريك = نقل الأيقونة
                    editing && onItem != null -> {
                        // نمسكه من مكان اللمسة الأولى بالضبط، وبعدين يلحق الإصبع (بدون ما يتأخر عنه)
                        state.startDrag(onItem.item, onItem.slot, down.position)
                        state.dragMove(last)
                        dragLoop(state, down.id)
                    }
                    abs(d.x) > abs(d.y) && state.inPages(down.position) -> pageSwipe(state, down.id, last, tracker)
                    // المكدس: السحب لفوق وتحت يتنقل بين الويدجت
                    isWidget && !editing && state.isStack(onItem!!.item) -> stackSwipe(state, down.id, down.position, onItem.item)
                    // الويدجت بالوضع العادي: السحب الطولي له (مثل قائمة التقويم)
                    isWidget && !editing -> {}
                    else -> verticalSwipe(state, down.id, down.position, if (editing) null else onItem)
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
                    // مكان فاضي، أو برا الصفحات (فوق، النقاط، حول الشريط): وضع الترتيب (أو الإعدادات لو مقفل)
                    Hit.Empty, Hit.Dots, Hit.Outside -> state.longPressEmpty()
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

/** السحب على النقاط: الصفحة اللي تحت إصبعك */
private suspend fun AwaitPointerEventScope.dotsScrub(state: HomeState, id: PointerId, start: Offset) {
    state.dotsGo(start.x, fast = true)
    while (true) {
        val e = awaitPointerEvent(PointerEventPass.Initial)
        val c = e.changes.firstOrNull { it.id == id } ?: break
        c.consume()
        if (!c.pressed) break
        state.dotsGo(c.position.x, fast = true)
    }
}

/**
 * السحب: اللمسة كلها لنا لين ترفع إصبعك.
 * وإصبع ثاني ينزل وأنت ماسك = يقلب الصفحات (مثل الآيفون)، والأيقونة تبقى تحت إصبعك الأول.
 */
private suspend fun AwaitPointerEventScope.dragLoop(state: HomeState, id: PointerId) {
    var second: PointerId? = null
    var secondStart = Offset.Zero
    var tracker2 = VelocityTracker()
    while (true) {
        val e = awaitPointerEvent(PointerEventPass.Initial)
        val c = e.changes.firstOrNull { it.id == id }
        // الإصبع الثاني
        val s2 = second
        if (s2 == null) {
            val o = e.changes.firstOrNull { it.id != id && it.pressed }
            if (o != null) {
                second = o.id
                secondStart = o.position
                tracker2 = VelocityTracker()
                track(tracker2, o)
                o.consume()
                state.dragPagingStart()
            }
        } else {
            val o = e.changes.firstOrNull { it.id == s2 }
            if (o == null || !o.pressed) {
                if (o != null) track(tracker2, o)
                state.dragPagingEnd(tracker2.calculateVelocity().x)
                second = null
            } else {
                track(tracker2, o)
                o.consume()
                state.dragPagingMove(o.position.x - secondStart.x)
            }
        }
        if (c == null) break
        c.consume()
        if (!c.pressed) break
        state.dragMove(c.position)
    }
    if (second != null) state.dragPagingEnd(tracker2.calculateVelocity().x)
    state.dragEnd()
}

/** إيماءة بإصبعين: قرصة أو سحب لفوق/تحت */
private suspend fun AwaitPointerEventScope.twoFingerLoop(state: HomeState, idA: PointerId, idB: PointerId, a0: Offset, b0: Offset) {
    var a1 = a0
    var b1 = b0
    while (true) {
        val e = awaitPointerEvent(PointerEventPass.Initial)
        val ca = e.changes.firstOrNull { it.id == idA }
        val cb = e.changes.firstOrNull { it.id == idB }
        e.changes.forEach { it.consume() }
        if (ca != null) a1 = ca.position
        if (cb != null) b1 = cb.position
        if (ca == null || cb == null || !ca.pressed || !cb.pressed) break
    }
    // ننتظر الإصبع الثاني يرتفع عشان ما يبدأ شي ثاني
    while (true) {
        val e = awaitPointerEvent(PointerEventPass.Initial)
        e.changes.forEach { it.consume() }
        if (e.changes.none { it.pressed }) break
    }
    val d = state.density
    val kind = GestureMath.classify(
        floatArrayOf(a0.x / d, a0.y / d), floatArrayOf(b0.x / d, b0.y / d),
        floatArrayOf(a1.x / d, a1.y / d), floatArrayOf(b1.x / d, b1.y / d),
    )
    state.twoFinger(kind)
}

/** المكدس: سحبة لفوق = اللي بعده، لتحت = اللي قبله */
private suspend fun AwaitPointerEventScope.stackSwipe(state: HomeState, id: PointerId, start: Offset, item: com.waleed.onyx.data.Item) {
    var dy = 0f
    while (true) {
        val e = awaitPointerEvent(PointerEventPass.Initial)
        val c = e.changes.firstOrNull { it.id == id } ?: break
        c.consume()
        dy = c.position.y - start.y
        if (!c.pressed) break
    }
    if (kotlin.math.abs(dy) > 36f * state.density) state.stackShift(item, if (dy < 0) 1 else -1)
}

/**
 * تقليب الصفحات بإصبعك: الصفحة تلحق إصبعك من أول ما يتعدى حد اللمس (بدون قفزة)،
 * وإذا فلت تكمل بنفس سرعة إصبعك لين صفحة كاملة.
 */
private suspend fun AwaitPointerEventScope.pageSwipe(state: HomeState, id: PointerId, start: Offset, tracker: VelocityTracker) {
    val pages = state.pages
    pages.stop()
    val startOffset = pages.offset
    val startPage = PagerPhysics.nearest(startOffset, pages.count)
    val width = state.pageWidthPx()
    while (true) {
        val e = awaitPointerEvent(PointerEventPass.Initial)
        val c = e.changes.firstOrNull { it.id == id } ?: break
        c.consume()
        track(tracker, c)
        // مكان الإصبع الأخير (حتى لحظة الرفع) يحدد الصفحة
        pages.offset = PagerPhysics.dragOffset(startOffset, c.position.x - start.x, width, pages.count)
        if (!c.pressed) break
    }
    // السرعة من الإصبع نفسه (بكسل بالثانية، موجب = يمين)
    val vPx = tracker.calculateVelocity().x
    val target = PagerPhysics.settleTarget(startPage, pages.offset, vPx / state.density, pages.count)
    // سرعة الإصبع باتجاه الهدف بوحدة الصفحات (الصفحة الجاية = الإصبع رايح يسار)
    val toward = when {
        target > pages.offset -> -vPx / width
        target < pages.offset -> vPx / width
        else -> 0f
    }
    android.util.Log.d("OnyxPages", "release offset=${"%.2f".format(pages.offset)} v=${vPx.toInt()}px/s target=$target")
    pages.animateTo(target, PagerPhysics.flingDurationMs(pages.offset, target, toward, state.calm()))
}

/** يضيف كل نقاط الإصبع للسرعة: المجمّعة (لو الجوال تأخر) والأخيرة */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
private fun track(tracker: VelocityTracker, c: androidx.compose.ui.input.pointer.PointerInputChange) {
    for (h in c.historical) tracker.addPosition(h.uptimeMillis, h.position)
    tracker.addPosition(c.uptimeMillis, c.position)
}

/** تكبير الويدجت بالمقبض: المقاس يتبع إصبعك، والخانات تتحدد لما تفلت */
private suspend fun AwaitPointerEventScope.resizeLoop(state: HomeState, id: PointerId, start: Offset, hit: Hit.Handle) {
    state.resizeStart(hit.item)
    if (state.resize == null) return
    while (true) {
        val e = awaitPointerEvent(PointerEventPass.Initial)
        val c = e.changes.firstOrNull { it.id == id } ?: break
        c.consume()
        if (!c.pressed) break
        state.resizeDrag(c.position.x - start.x, c.position.y - start.y)
    }
    state.resizeEnd()
}

/**
 * السحب لفوق = كل التطبيقات، لتحت = الإشعارات (من أي مكان).
 * لو بدأ على أيقونة بالعمود أو الشريط: لفوق = ويدجتها المنبثق، أو أول تطبيق بالمجلد.
 */
private suspend fun AwaitPointerEventScope.verticalSwipe(state: HomeState, id: PointerId, start: Offset, on: Hit.OnItem?) {
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
    // على أيقونة بالعمود أو الشريط بس (بالصفحات السحب لفوق يفتح الدرج دايماً، حتى لو الصفحة مليانة)
    if (on != null && on.slot != null && dy < -40f * d) {
        when (on.item.kind) {
            Kind.APP -> { state.showPopup(on.item); return }
            Kind.FOLDER -> { state.folderSwipe(on.item); return }
        }
    }
    when {
        dy < -60f * d -> state.drawerOpen = true
        dy > 48f * d -> state.openNotifications()
    }
}
