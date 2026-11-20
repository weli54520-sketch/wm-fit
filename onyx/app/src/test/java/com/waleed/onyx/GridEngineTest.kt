package com.waleed.onyx

import com.waleed.onyx.grid.Cell
import com.waleed.onyx.grid.GridEngine
import com.waleed.onyx.grid.Span
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GridEngineTest {

    @Test
    fun movingIntoEmptyCellKeepsEveryoneElse() {
        val others = listOf(Span(1, 0, 0), Span(2, 1, 0))
        val r = GridEngine.solve(others, Span(9, 3, 3), 4, 6)!!
        assertEquals(Cell(0, 0), r[1])
        assertEquals(Cell(1, 0), r[2])
    }

    @Test
    fun occupiedCellPushesTheAppToNearestFreeCell() {
        val others = listOf(Span(1, 0, 0), Span(2, 1, 0))
        val r = GridEngine.solve(others, Span(9, 1, 0), 4, 6)!!
        assertEquals(Cell(0, 0), r[1])
        val moved = r[2]!!
        assertTrue(moved != Cell(1, 0))
        assertEquals(1, kotlin.math.abs(moved.x - 1) + kotlin.math.abs(moved.y - 0))
    }

    @Test
    fun noRoomReturnsNull() {
        val others = (0 until 4).map { Span(it.toLong(), it, 0) }
        assertNull(GridEngine.solve(others, Span(9, 0, 0), 4, 1))
    }

    @Test
    fun outsideGridIsRejected() {
        assertNull(GridEngine.solve(emptyList(), Span(9, 3, 0, 2, 1), 4, 6))
    }

    @Test
    fun wideItemPushesSeveralApps() {
        val others = listOf(Span(1, 0, 0), Span(2, 1, 0), Span(3, 2, 0), Span(4, 3, 0))
        val r = GridEngine.solve(others, Span(9, 0, 0, 4, 2), 4, 6)
        assertNotNull(r)
        r!!.values.forEach { assertTrue(it.y >= 2) }
        assertEquals(4, r.values.toSet().size)
    }

    @Test
    fun freeSpotFindsFirstHoleInReadingOrder() {
        val items = listOf(Span(1, 0, 0), Span(2, 1, 0))
        assertEquals(Cell(2, 0), GridEngine.freeSpot(items, 1, 1, 4, 6))
        assertEquals(Cell(2, 0), GridEngine.freeSpot(items, 2, 2, 4, 6))
    }

    @Test
    fun cellAtRoundsAndClamps() {
        assertEquals(Cell(1, 2), GridEngine.cellAt(110f, 190f, 100f, 100f, 1, 1, 4, 6))
        assertEquals(Cell(3, 5), GridEngine.cellAt(900f, 900f, 100f, 100f, 1, 1, 4, 6))
        assertEquals(Cell(0, 0), GridEngine.cellAt(-50f, -50f, 100f, 100f, 1, 1, 4, 6))
    }

    @Test
    fun reflowMovesOverflowToNextPage() {
        // 5 أعمدة صارت 4: آخر عنصر بالصف ما يركب، يروح لأقرب فاضي
        val page = (0 until 5).map { Span(it.toLong(), it, 0) } + Span(10, 0, 1)
        val r = GridEngine.reflow(listOf(page), 4, 1)
        assertEquals(6, r.size)
        val pageCounts = r.values.groupingBy { it.first }.eachCount()
        assertEquals(4, pageCounts[0])
        assertEquals(2, pageCounts[1])
    }
}
