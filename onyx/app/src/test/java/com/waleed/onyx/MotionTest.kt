package com.waleed.onyx

import com.waleed.onyx.grid.DragBrain
import com.waleed.onyx.grid.DragEvent
import com.waleed.onyx.grid.DragTarget
import com.waleed.onyx.grid.GeoInput
import com.waleed.onyx.grid.HomeGeometry
import com.waleed.onyx.grid.PagerPhysics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** حراس الحركة (كتاب المواصفات، الفصول ٣ و٥ و٨) */
class MotionTest {

    // ───────── تقليب الصفحات ─────────

    @Test
    fun shortSlowSwipeGoesBack() {
        // سحبت 30% ببطء: ترجع لنفس الصفحة
        assertEquals(2, PagerPhysics.settleTarget(2, 2.3f, -100f, 5))
        assertEquals(2, PagerPhysics.settleTarget(2, 1.7f, 100f, 5))
    }

    @Test
    fun longSwipeOrFastFlingTurnsOnePageOnly() {
        assertEquals(3, PagerPhysics.settleTarget(2, 2.45f, 0f, 5))
        assertEquals(1, PagerPhysics.settleTarget(2, 1.55f, 0f, 5))
        // سحبة سريعة قصيرة
        assertEquals(3, PagerPhysics.settleTarget(2, 2.1f, -800f, 5))
        assertEquals(1, PagerPhysics.settleTarget(2, 1.9f, 800f, 5))
        // مهما كانت سريعة: صفحة وحدة بس
        assertEquals(3, PagerPhysics.settleTarget(2, 2.9f, -9000f, 5))
    }

    @Test
    fun pagesStopAtTheEnds() {
        assertEquals(0, PagerPhysics.settleTarget(0, -0.2f, 2000f, 5))
        assertEquals(4, PagerPhysics.settleTarget(4, 4.2f, -2000f, 5))
        // السحب بعد الآخر فيه مقاومة
        val over = PagerPhysics.dragOffset(4f, -400f, 400f, 5)
        assertTrue(over > 4f && over < 4.5f)
        val under = PagerPhysics.dragOffset(0f, 400f, 400f, 5)
        assertTrue(under < 0f && under > -0.5f)
    }

    @Test
    fun alwaysEndsOnAWholePage() {
        // الحراس: أي مكان + أي سرعة = صفحة كاملة
        for (o in -5..45) for (v in listOf(-3000f, -400f, 0f, 400f, 3000f)) {
            val off = o / 10f
            val start = PagerPhysics.nearest(off, 5)
            val t = PagerPhysics.settleTarget(start, off, v, 5)
            assertTrue(t in 0..4)
        }
    }

    @Test
    fun settleIsCalmAndNeverJumps() {
        assertEquals(650, PagerPhysics.settleDurationMs(1f, 2, calm = true))
        assertTrue(PagerPhysics.settleDurationMs(1.9f, 2, calm = true) >= 320)
        assertTrue(PagerPhysics.settleDurationMs(1f, 2, calm = false) < 650)
    }

    // ───────── مخ السحب ─────────

    private fun cell(x: Int, y: Int) = DragTarget.CellAt(0, x, y)

    @Test
    fun passingQuicklyOverCellsDoesNotReorder() {
        val b = DragBrain()
        var t = 0L
        // يمر على ٥ خانات، كل وحدة 120ms
        for (x in 0 until 5) {
            for (k in 0..3) { assertNull(b.update(cell(x, 0), t)); t += 40 }
        }
        assertNull(b.committed)
    }

    @Test
    fun stoppingOnACellReordersExactlyOnce() {
        val b = DragBrain()
        assertNull(b.update(cell(2, 1), 0))
        assertNull(b.update(cell(2, 1), 300))
        val e = b.update(cell(2, 1), 360)
        assertEquals(DragEvent.Commit(cell(2, 1)), e)
        // يبقى واقف: ما يتكرر
        assertNull(b.update(cell(2, 1), 900))
        assertNull(b.update(cell(2, 1), 2000))
    }

    @Test
    fun edgeFlipsAfterHalfSecondAndNotFasterThan900() {
        val b = DragBrain()
        val right = DragTarget.EdgeAt(1)
        assertNull(b.update(right, 0))
        assertNull(b.update(right, 450))
        assertEquals(DragEvent.Flip(1), b.update(right, 520))
        // يبقى عند الحافة: التقليب الثاني بعد 900 من الأول
        assertNull(b.update(right, 1000))
        assertNull(b.update(right, 1400))
        assertEquals(DragEvent.Flip(1), b.update(right, 1430))
    }

    @Test
    fun leavingTheEdgeCancelsTheFlip() {
        val b = DragBrain()
        b.update(DragTarget.EdgeAt(-1), 0)
        b.update(cell(1, 1), 300)
        assertNull(b.update(cell(1, 1), 300))
        // رجع للحافة: يبدأ العد من جديد
        assertNull(b.update(DragTarget.EdgeAt(-1), 400))
        assertNull(b.update(DragTarget.EdgeAt(-1), 800))
        assertEquals(DragEvent.Flip(-1), b.update(DragTarget.EdgeAt(-1), 900))
    }

    @Test
    fun slotGetsReadyAfter200() {
        val b = DragBrain()
        val s = DragTarget.SlotAt(1, 2)
        assertNull(b.update(s, 0))
        assertNull(b.update(s, 150))
        assertEquals(DragEvent.SlotReady(s), b.update(s, 210))
        assertNull(b.update(s, 500))
    }

    // ───────── وين الإصبع (من المقاسات) ─────────

    private val outer = HomeGeometry.compute(GeoInput(411f, 960f, 32f, 24f, 4))
    private val inner = HomeGeometry.compute(GeoInput(750f, 832f, 28f, 24f, 6, rows = 5))

    @Test
    fun cellUnderIconCenterIsTheRightOne() {
        for (g in listOf(outer, inner)) for (k in listOf(1f, HomeGeometry.EDIT_SCALE)) {
            for (y in 0 until g.rows) for (x in 0 until g.cols) {
                val b = g.toScreen(g.cellBox(x, y), k)
                assertEquals(x to y, g.cellFor(b.cx, b.cy, 1, 1, k))
            }
        }
    }

    @Test
    fun noGridCellCenterIsInsideAPageFlipZone() {
        // حط أيقونة بأي خانة ما يقلب الصفحة بالغلط (نص الأيقونة بعيد عن الحافة)
        for (g in listOf(outer, inner)) for (k in listOf(1f, HomeGeometry.EDIT_SCALE)) {
            for (y in 0 until g.rows) for (x in 0 until g.cols) {
                val b = g.toScreen(g.cellBox(x, y), k)
                assertEquals(0, g.edgeAt(b.cx, b.cy))
            }
        }
    }

    @Test
    fun pageFlipZonesAndSideSlotsDoNotOverlap() {
        for (g in listOf(outer, inner)) {
            for (s in g.sideSlots) {
                assertEquals(0, g.edgeAt(s.cx, s.cy))
                assertTrue("العمود يدخل بمنطقة التقليب", s.l > g.pages.r + 1f)
            }
            assertEquals(1, g.edgeAt(g.pages.r - 5f, g.pages.cy))
            assertEquals(-1, g.edgeAt(5f, g.pages.cy))
            assertEquals(0, g.sideSlotAt(g.pages.r - 5f, g.pages.cy) ?: 0)
        }
    }

    @Test
    fun slotsAreFoundUnderTheFinger() {
        for (g in listOf(outer, inner)) {
            g.sideSlots.forEachIndexed { i, s -> assertEquals(i, g.sideSlotAt(s.cx, s.cy)) }
            g.dockSlots.forEachIndexed { i, s -> assertEquals(i, g.dockSlotAt(s.cx, s.cy)) }
            assertNull(g.sideSlotAt(g.grid.cx, g.grid.cy))
        }
    }

    @Test
    fun widgetCellKeepsItInsideTheGrid() {
        val g = outer
        val (x, y) = g.cellFor(g.pages.r, g.grid.b, 4, 2, 1f)
        assertTrue(x + 4 <= g.cols && y + 2 <= g.rows)
    }
}
