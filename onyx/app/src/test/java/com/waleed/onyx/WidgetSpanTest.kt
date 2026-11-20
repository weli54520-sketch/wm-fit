package com.waleed.onyx

import com.waleed.onyx.grid.SpanLimits
import com.waleed.onyx.grid.WidgetSpan
import org.junit.Assert.assertEquals
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
