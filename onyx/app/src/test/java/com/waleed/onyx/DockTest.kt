package com.waleed.onyx

import com.waleed.onyx.grid.DockOrder
import com.waleed.onyx.grid.DotsMath
import com.waleed.onyx.grid.GeoInput
import com.waleed.onyx.grid.HomeGeometry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** حراس الشريط السفلي مثل الآيفون، ونقاط الصفحات (كتاب المواصفات، الفصل ٢ و٥) */
class DockTest {
    private val screens = listOf(
        floatArrayOf(411f, 960f, 32f, 24f),   // الخارجية
        floatArrayOf(750f, 832f, 32f, 24f),   // الداخلية
        floatArrayOf(360f, 780f, 28f, 20f),
    )

    private fun all(): List<HomeGeometry> {
        val out = ArrayList<HomeGeometry>()
        for (s in screens) for (cols in 4..6) for (count in 3..9) for (side in listOf(true, false))
            out += HomeGeometry.compute(GeoInput(s[0], s[1], s[2], s[3], cols, 6, side, 4, true, count, 1f))
        return out
    }

    @Test
    fun dockRowIsCenteredInsideItsGlassWithoutOverlap() {
        for (g in all()) {
            val dock = g.dock!!
            for (n in 1..g.dockSlots.size) {
                val row = g.dockRow(n)
                val glass = g.dockGlass(n)!!
                assertEquals(n, row.size)
                assertTrue("الزجاج طالع من مكان الشريط", glass.inside(dock))
                for (b in row) assertTrue("أيقونة طالعة من الزجاج ($n)", b.inside(glass))
                for (k in 1 until n) {
                    assertTrue("أيقونات الشريط متداخلة", row[k].l >= row[k - 1].r - 0.01f)
                    assertEquals("المسافة بين الأيقونات تتغير", g.dockStep, row[k].cx - row[k - 1].cx, 0.01f)
                }
                val mid = (row.first().cx + row.last().cx) / 2
                assertEquals("الأيقونات مو بالنص", dock.cx, mid, 0.01f)
                assertEquals("الزجاج مو بالنص", dock.cx, glass.cx, 0.01f)
            }
        }
    }

    @Test
    fun fullDockMatchesTheOldSlotsExactly() {
        for (g in all()) {
            val full = g.dockRow(g.dockSlots.size)
            for ((a, b) in full.zip(g.dockSlots)) assertEquals(b.cx, a.cx, 0.01f)
            assertEquals(g.dock!!.w, g.dockGlass(g.dockSlots.size)!!.w, 0.01f)
        }
    }

    @Test
    fun glassShrinksWhenAnIconLeaves() {
        for (g in all()) for (n in 2..g.dockSlots.size)
            assertTrue(g.dockGlass(n - 1)!!.w < g.dockGlass(n)!!.w)
    }

    @Test
    fun insertIndexFollowsTheFinger() {
        for (g in all()) for (n in 1..g.dockSlots.size) {
            val row = g.dockRow(n)
            row.forEachIndexed { i, b -> assertEquals(i, g.dockInsertIndex(b.cx, n)) }
            assertEquals(0, g.dockInsertIndex(g.dock!!.l, n))
            assertEquals(n - 1, g.dockInsertIndex(g.dock!!.r, n))
        }
    }

    @Test
    fun dockZoneDoesNotTouchTheGrid() {
        for (g in all()) {
            val d = g.dock!!
            assertTrue(g.inDockZone(d.cx, d.cy))
            assertFalse("منطقة الشريط تدخل بالشبكة", g.inDockZone(d.cx, g.grid.b - 1f))
        }
    }

    @Test
    fun noDockMeansNoDockRow() {
        val g = HomeGeometry.compute(GeoInput(411f, 960f, 32f, 24f, 4, dockOn = false))
        assertTrue(g.dockRow(3).isEmpty())
        assertNull(g.dockGlass(3))
        assertFalse(g.inDockZone(200f, 900f))
    }

    // ───────── الترتيب ─────────

    @Test
    fun arrangeClosesTheGapWhenTheIconLeaves() {
        val ids = listOf(1L, 2L, 3L, 4L)
        assertEquals(listOf<Long?>(1L, 3L, 4L), DockOrder.arrange(ids, 2L, null))
        assertEquals(listOf<Long?>(1L, null, 3L, 4L), DockOrder.arrange(ids, 2L, 1))
        assertEquals(listOf<Long?>(1L, 2L, 3L, 4L, null), DockOrder.arrange(ids, 9L, 99))
        assertEquals(listOf<Long?>(null, 1L, 2L, 3L, 4L), DockOrder.arrange(ids, 9L, -3))
    }

    @Test
    fun dropNeverLosesOrDuplicatesAnIcon() {
        val ids = listOf(10L, 20L, 30L, 40L, 50L)
        for (dragged in ids + 99L) for (at in -1..7) {
            val r = DockOrder.drop(ids, dragged, at)
            assertEquals("أيقونة مكررة", r.size, r.toSet().size)
            assertTrue("أيقونة ضاعت", r.containsAll(ids))
            assertTrue(dragged in r)
            assertEquals(at.coerceIn(0, r.size - 1), r.indexOf(dragged))
        }
    }

    @Test
    fun reorderInsideTheDock() {
        assertEquals(listOf(2L, 3L, 1L, 4L), DockOrder.drop(listOf(1L, 2L, 3L, 4L), 1L, 2))
        assertEquals(listOf(4L, 1L, 2L, 3L), DockOrder.drop(listOf(1L, 2L, 3L, 4L), 4L, 0))
    }

    @Test
    fun insertOnlyWhenThereIsRoom() {
        assertTrue(DockOrder.canInsert(5, 6, false))
        assertFalse(DockOrder.canInsert(6, 6, false))
        assertTrue("من الشريط نفسه يقدر يغير الترتيب حتى لو مليان", DockOrder.canInsert(6, 6, true))
        assertEquals(6, DockOrder.slotsWhileHovering(6, true))
        assertEquals(6, DockOrder.slotsWhileHovering(5, false))
    }

    @Test
    fun hoverLayoutAlwaysFitsTheDock() {
        for (g in all()) {
            val cap = g.dockSlots.size
            for (size in 0..cap) for (fromDock in listOf(true, false)) {
                if (fromDock && size == 0) continue
                if (!DockOrder.canInsert(size, cap, fromDock)) continue
                val n = DockOrder.slotsWhileHovering(size, fromDock)
                assertTrue("الشريط يطلع أكبر من مكانه", n <= cap)
                for (b in g.dockRow(n)) assertTrue(b.inside(g.dock!!))
            }
        }
    }

    // ───────── النقاط ─────────

    @Test
    fun eachDotTapGoesToItsPage() {
        for (count in 2..12) {
            val cx = 205f
            for (i in 0 until count) assertEquals(i, DotsMath.indexAt(DotsMath.dotCenter(i, cx, count), cx, count))
            assertEquals(0, DotsMath.indexAt(-500f, cx, count))
            assertEquals(count - 1, DotsMath.indexAt(5000f, cx, count))
            assertTrue(DotsMath.onRow(DotsMath.dotCenter(0, cx, count) - DotsMath.SLOT / 2 - 5f, cx, count))
            assertFalse(DotsMath.onRow(cx + DotsMath.rowWidth(count) / 2 + DotsMath.HIT_PAD + 1f, cx, count))
            assertEquals("النقاط مو بالنص", cx, (DotsMath.dotCenter(0, cx, count) + DotsMath.dotCenter(count - 1, cx, count)) / 2, 0.01f)
        }
        assertFalse("صفحة وحدة: ما فيه نقاط", DotsMath.onRow(205f, 205f, 1))
    }

    @Test
    fun dotsFitOnTheScreen() {
        for (g in all()) {
            val maxPages = 12
            assertTrue(abs(DotsMath.rowWidth(maxPages)) + DotsMath.HIT_PAD * 2 <= g.width)
        }
    }
}
