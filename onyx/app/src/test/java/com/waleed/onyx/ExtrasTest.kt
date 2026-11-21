package com.waleed.onyx

import com.waleed.onyx.grid.Calc
import com.waleed.onyx.grid.DragBrain
import com.waleed.onyx.grid.DragEvent
import com.waleed.onyx.grid.DragTarget
import com.waleed.onyx.grid.FolderMath
import com.waleed.onyx.grid.GestureMath
import com.waleed.onyx.grid.GridEngine
import com.waleed.onyx.grid.Recents
import com.waleed.onyx.grid.Span
import com.waleed.onyx.grid.TwoFinger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** حراس 0.4: الويدجت بصفحة مليانة، الحاسبة، آخر التطبيقات، الإيماءات، المجلدات */
class ExtrasTest {

    private fun fullPage(cols: Int, rows: Int): List<Span> {
        val out = ArrayList<Span>()
        var id = 1L
        for (y in 0 until rows) for (x in 0 until cols) out += Span(id++, x, y)
        return out
    }

    @Test
    fun widgetLandsWhereYouDropItEvenOnAFullPage() {
        val cols = 4; val rows = 6
        val icons = fullPage(cols, rows)
        val w = Span(999, 1, 2, 2, 2)
        val r = GridEngine.solveOverflow(icons, w, cols, rows)
        assertNotNull(r)
        r!!
        // ٤ أيقونات تحت الويدجت ما لقت مكان: تطلع للصفحة اللي بعدها
        assertEquals(4, r.overflow.size)
        val placed = r.cells.entries.map { Span(it.key, it.value.x, it.value.y) }
        for (p in placed) assertFalse("أيقونة تحت الويدجت", p.overlaps(w))
        for (i in placed.indices) for (j in i + 1 until placed.size) assertFalse("أيقونتين بنفس الخانة", placed[i].overlaps(placed[j]))
        assertEquals("أيقونة ضاعت", icons.size, placed.size + r.overflow.size)
    }

    @Test
    fun widgetOnARoomyPageMovesNothingAway() {
        val icons = listOf(Span(1, 0, 0), Span(2, 1, 0), Span(3, 2, 2))
        val r = GridEngine.solveOverflow(icons, Span(9, 2, 2, 2, 2), 4, 6)!!
        assertTrue(r.overflow.isEmpty())
        assertEquals(3, r.cells.size)
        assertEquals(0, r.cells[1L]!!.x)
    }

    @Test
    fun widgetBiggerThanTheGridIsRefused() {
        assertNull(GridEngine.solveOverflow(emptyList(), Span(9, 0, 0, 5, 2), 4, 6))
        assertNull(GridEngine.solveOverflow(emptyList(), Span(9, 3, 0, 2, 2), 4, 6))
    }

    @Test
    fun calculatorUnderstandsArabicAndSigns() {
        assertEquals("100", Calc.format(Calc.eval("25×4")!!))
        assertEquals("100", Calc.format(Calc.eval("٢٥*٤")!!))
        assertEquals("7", Calc.format(Calc.eval("1+2*3")!!))
        assertEquals("9", Calc.format(Calc.eval("(1+2)*3")!!))
        assertEquals("2.5", Calc.format(Calc.eval("10÷4")!!))
        assertEquals("1024", Calc.format(Calc.eval("2^10")!!))
        assertEquals("15", Calc.format(Calc.eval("150*10%")!!))
        assertEquals("-3", Calc.format(Calc.eval("-1-2")!!))
        assertNull("قسمة على صفر", Calc.eval("5/0"))
        assertNull("اسم تطبيق مو حساب", Calc.eval("youtube"))
        assertNull("رقم لحاله مو حساب", Calc.eval("2026"))
        assertNull("ناقص", Calc.eval("3+"))
        assertNull(Calc.eval("(2+3"))
    }

    @Test
    fun recentsKeepNewestFirstWithoutDuplicates() {
        var l = emptyList<String>()
        for (k in listOf("a", "b", "c", "a")) l = Recents.push(l, k, 8)
        assertEquals(listOf("a", "c", "b"), l)
        for (i in 0 until 20) l = Recents.push(l, "x$i", 8)
        assertEquals(8, l.size)
        assertEquals("x19", l.first())
        assertEquals(l, Recents.push(l, "  ", 8))
    }

    @Test
    fun twoFingerGesturesAreTellApart() {
        fun f(vararg v: Float) = v
        assertEquals(TwoFinger.PINCH_IN, GestureMath.classify(f(100f, 300f), f(300f, 500f), f(170f, 370f), f(230f, 430f)))
        assertEquals(TwoFinger.PINCH_OUT, GestureMath.classify(f(180f, 380f), f(220f, 420f), f(100f, 300f), f(300f, 500f)))
        assertEquals(TwoFinger.SWIPE_DOWN, GestureMath.classify(f(150f, 200f), f(250f, 200f), f(150f, 320f), f(250f, 320f)))
        assertEquals(TwoFinger.SWIPE_UP, GestureMath.classify(f(150f, 400f), f(250f, 400f), f(150f, 300f), f(250f, 300f)))
        assertEquals(TwoFinger.NONE, GestureMath.classify(f(150f, 400f), f(250f, 400f), f(160f, 410f), f(255f, 405f)))
    }

    @Test
    fun doubleTapNeedsToBeQuickAndClose() {
        assertTrue(GestureMath.isDoubleTap(1000, 100f, 100f, 1200, 110f, 105f))
        assertFalse(GestureMath.isDoubleTap(1000, 100f, 100f, 1400, 100f, 100f))
        assertFalse(GestureMath.isDoubleTap(1000, 100f, 100f, 1100, 200f, 100f))
        assertFalse(GestureMath.isDoubleTap(1000, 100f, 100f, 1000, 100f, 100f))
    }

    @Test
    fun folderOrderAndPages() {
        assertEquals(1, FolderMath.pages(0))
        assertEquals(1, FolderMath.pages(9))
        assertEquals(2, FolderMath.pages(10))
        assertEquals(listOf(1L, 2L, 3L), FolderMath.add(listOf(1L, 3L, 2L).filter { it != 3L } + 3L, 3L))
        assertEquals(listOf(1L, 2L), FolderMath.add(listOf(1L, 2L), 2L))
        assertEquals(listOf(2L), FolderMath.remove(listOf(1L, 2L), 1L))
        assertTrue(FolderMath.shouldDissolve(1))
        assertFalse(FolderMath.shouldDissolve(2))
        assertEquals(9, FolderMath.preview((1..20).toList()).size)
    }

    @Test
    fun mergeWaitsForTheFingerToRest() {
        val b = DragBrain()
        val t = DragTarget.MergeAt(5)
        assertNull(b.update(t, 0))
        assertNull(b.update(t, 200))
        assertTrue(b.update(t, 320) is DragEvent.MergeReady)
        assertNull("مرة وحدة بس", b.update(t, 400))
        assertNull(b.update(DragTarget.Nowhere, 450))
        assertNull(b.mergeReady)
    }
}
