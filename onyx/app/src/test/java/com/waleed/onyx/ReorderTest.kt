package com.waleed.onyx

import com.waleed.onyx.grid.Cell
import com.waleed.onyx.grid.GridEngine
import com.waleed.onyx.grid.Span
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** شبكة 4×2: الأيقونات 1..8 بترتيب القراءة */
class ReorderTest {
    private fun full(skip: Long? = null) = (0 until 8).map { i -> Span(i + 1L, i % 4, i / 4) }.filter { it.id != skip }

    @Test
    fun emptyCellNobodyMoves() {
        val others = listOf(Span(1, 0, 0), Span(2, 1, 0))
        val r = GridEngine.reorder(others, Span(9, 3, 1), 4, 2)!!
        assertEquals(Cell(0, 0), r.cells[1])
        assertEquals(Cell(1, 0), r.cells[2])
        assertTrue(r.overflow.isEmpty())
    }

    @Test
    fun movingForwardShiftsTheOnesBetweenBack() {
        // أيقونة 2 (خانة 1) تروح لخانة 4: 3،4،5 يرجعون خانة
        val r = GridEngine.reorder(full(skip = 2), Span(2, 0, 1), 4, 2, from = Cell(1, 0))!!
        assertEquals(Cell(0, 0), r.cells[1])
        assertEquals(Cell(1, 0), r.cells[3])
        assertEquals(Cell(2, 0), r.cells[4])
        assertEquals(Cell(3, 0), r.cells[5])
        assertEquals(Cell(1, 1), r.cells[6])
        assertTrue(r.overflow.isEmpty())
    }

    @Test
    fun movingBackShiftsTheOnesBetweenForward() {
        // أيقونة 7 (خانة 6) تروح لخانة 1: 2..6 يتقدمون خانة
        val r = GridEngine.reorder(full(skip = 7), Span(7, 1, 0), 4, 2, from = Cell(2, 1))!!
        assertEquals(Cell(0, 0), r.cells[1])
        assertEquals(Cell(2, 0), r.cells[2])
        assertEquals(Cell(3, 0), r.cells[3])
        assertEquals(Cell(0, 1), r.cells[4])
        assertEquals(Cell(1, 1), r.cells[5])
        assertEquals(Cell(2, 1), r.cells[6])
        assertEquals(Cell(3, 1), r.cells[8])
    }

    @Test
    fun fullPageFromAnotherPagePushesLastOut() {
        val r = GridEngine.reorder(full(), Span(99, 1, 0), 4, 2)!!
        assertEquals(listOf(8L), r.overflow)
        assertEquals(Cell(2, 0), r.cells[2])
        assertEquals(Cell(3, 1), r.cells[7])
        assertNull(r.cells[8])
    }

    @Test
    fun widgetsStayAndShiftingSkipsThem() {
        // ويدجت 2×1 بأول الصف الثاني
        val others = listOf(Span(1, 0, 0), Span(2, 1, 0), Span(3, 2, 0), Span(4, 3, 0), Span(50, 0, 1, 2, 1))
        val r = GridEngine.reorder(others, Span(9, 0, 0), 4, 2)!!
        assertEquals(Cell(0, 1), r.cells[50])
        assertEquals(Cell(1, 0), r.cells[1])
        assertEquals(Cell(2, 1), r.cells[4])
        // فوق الويدجت ما يصير
        assertNull(GridEngine.reorder(others, Span(9, 1, 1), 4, 2))
    }
}
