package com.waleed.onyx

import com.waleed.onyx.grid.SpanLimits
import com.waleed.onyx.grid.WidgetSpan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetSpanTest {

    @Test
    fun cellsRoundUpButForgiveTinyOverflow() {
        assertEquals(1, WidgetSpan.cellsFor(100, 100f, 4))
        assertEquals(1, WidgetSpan.cellsFor(110, 100f, 4))
        assertEquals(2, WidgetSpan.cellsFor(150, 100f, 4))
        assertEquals(4, WidgetSpan.cellsFor(900, 100f, 4))
        assertEquals(1, WidgetSpan.cellsFor(0, 100f, 4))
    }

    @Test
    fun targetCellsWinOverPixels() {
        assertEquals(4 to 2, WidgetSpan.defaultSpan(4, 2, 50, 50, 100f, 100f, 4, 6))
        assertEquals(4 to 2, WidgetSpan.defaultSpan(6, 2, 50, 50, 100f, 100f, 4, 6))
        assertEquals(2 to 1, WidgetSpan.defaultSpan(0, 0, 180, 90, 100f, 100f, 4, 6))
    }

    @Test
    fun fixedAxisStaysAtCurrentSize() {
        val l = WidgetSpan.limits(2, 2, 100, 100, 0, 0, horizontal = true, vertical = false, cellW = 100f, cellH = 100f, cols = 4, rows = 6)
        assertEquals(SpanLimits(1, 4, 2, 2), l)
    }

    @Test
    fun resizeSnapsToCellsAndStaysInsideGrid() {
        val l = SpanLimits(1, 4, 1, 4)
        assertEquals(3 to 2, WidgetSpan.resized(2, 2, 120f, 20f, 100f, 100f, 0, 0, 4, 6, l))
        // ما يطلع برا الشبكة
        assertEquals(2 to 2, WidgetSpan.resized(2, 2, 500f, 0f, 100f, 100f, 2, 0, 4, 6, l))
        // ما يصغر عن الحد
        assertEquals(1 to 1, WidgetSpan.resized(2, 2, -900f, -900f, 100f, 100f, 0, 0, 4, 6, l))
    }
}

class LiveResizeTest {
    private val lim = SpanLimits(1, 4, 1, 4)

    @Test
    fun followsTheFingerContinuously() {
        // كل حركة صغيرة تغير المقاس الحي، بدون قفزات
        var last = -1f
        for (d in 0..150 step 5) {
            val s = WidgetSpan.liveResize(2, 2, 2, 2, d.toFloat(), d.toFloat(), 80f, 80f, 0, 0, 4, 6, lim)
            assertTrue(s.wDp > last)
            assertEquals(160f + d, s.wDp, 0.01f)
            last = s.wDp
        }
    }

    @Test
    fun widthAndHeightChangeTogether() {
        val s = WidgetSpan.liveResize(2, 2, 2, 2, 60f, 60f, 80f, 80f, 0, 0, 4, 6, lim)
        assertEquals(s.wDp, s.hDp, 0.01f)
        assertEquals(s.w, s.h)
    }

    @Test
    fun cellsChangeOnlyPast60Percent() {
        // 2.5 خانة: يبقى 2. 2.65: يصير 3. ويرجع 2 بس تحت 2.4
        assertEquals(2, WidgetSpan.snap(2.5f, 2, 1, 4))
        assertEquals(3, WidgetSpan.snap(2.65f, 2, 1, 4))
        assertEquals(3, WidgetSpan.snap(2.5f, 3, 1, 4))
        assertEquals(2, WidgetSpan.snap(2.35f, 3, 1, 4))
    }

    @Test
    fun neverGoesOutsideLimitsOrGrid() {
        val s = WidgetSpan.liveResize(2, 2, 2, 2, 900f, -900f, 80f, 80f, 1, 0, 4, 6, lim)
        assertEquals(3 * 80f, s.wDp, 0.01f) // ما يطلع برا الشبكة (من العمود 1)
        assertEquals(80f, s.hDp, 0.01f)
        assertEquals(3, s.w)
        assertEquals(1, s.h)
    }
}
