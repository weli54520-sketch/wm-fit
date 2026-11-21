package com.waleed.onyx

import com.waleed.onyx.grid.GeoInput
import com.waleed.onyx.grid.HomeGeometry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * حراس المقاسات (كتاب المواصفات، الفصل ٨).
 * كل قاعدة تنفحص بكل التركيبات: الشاشتين، كل عدد أعمدة، كل حجم أيقونة، العمود والشريط شغالين أو لا.
 * لو أي تعديل كسر قاعدة، البناء يفشل والنسخة ما توصل وليد.
 */
class HomeGeometryTest {

    /** مقاسات Fold 7 بـ dpi 420 (نفس الجوال الوهمي) + شاشات أصغر وأكبر شوي للأمان */
    private val screens = listOf(
        // العرض، الطول، فوق، تحت
        floatArrayOf(411f, 960f, 32f, 24f),   // الخارجية
        floatArrayOf(390f, 860f, 28f, 20f),   // خارجية أصغر
        floatArrayOf(750f, 832f, 28f, 24f),   // الداخلية
        floatArrayOf(673f, 841f, 28f, 24f),   // داخلية بتكبير أكبر
        floatArrayOf(880f, 980f, 28f, 24f),   // داخلية بتكبير أصغر
    )

    private fun all(): List<HomeGeometry> {
        val out = ArrayList<HomeGeometry>()
        for (s in screens) {
            val inner = s[0] >= HomeGeometry.INNER_MIN_WIDTH
            val colRange = if (inner) 5..7 else 4..5
            for (cols in colRange) for (scale in listOf(0.8f, 0.9f, 1f, 1.1f, 1.15f))
                for (side in listOf(true, false)) for (sideCount in 3..5) for (dock in listOf(true, false))
                    for (rows in listOf(4, 6, 9))
                        out += HomeGeometry.compute(GeoInput(s[0], s[1], s[2], s[3], cols, rows, side, sideCount, dock, 8, scale))
        }
        return out
    }

    private fun say(g: HomeGeometry) = "شاشة ${g.width}×${g.height} أعمدة ${g.cols} أيقونة ${g.icon} عمود ${g.sideOn}/${g.sideCount} شريط ${g.dockOn}"

    @Test
    fun nothingPassesUnderTheSideColumn() {
        for (g in all()) {
            if (!g.sideOn) continue
            assertTrue("الصفحات تدخل تحت العمود: ${say(g)}", g.pages.r <= g.side.l - 2f)
            // وبوضع الترتيب (الصفحة مصغرة حول نصها) نفس الشي
            assertTrue("وضع الترتيب يدخل تحت العمود: ${say(g)}", !g.pages.scaled(HomeGeometry.EDIT_SCALE).overlaps(g.side))
            for (y in 0 until g.rows) for (x in 0 until g.cols)
                assertTrue("أيقونة تدخل تحت العمود: ${say(g)}", !g.iconBox(x, y).overlaps(g.side))
        }
    }

    @Test
    fun sideGlassHugsIcons() {
        for (g in all()) {
            assertEquals("زجاج العمود أعرض من المسموح: ${say(g)}", g.sideIcon + 14f, g.side.w, 0.01f)
            for (s in g.sideSlots) {
                assertEquals(g.sideIcon, s.w, 0.01f)
                assertTrue("أيقونة العمود طالعة من الزجاج: ${say(g)}", s.inside(g.side))
            }
        }
    }

    @Test
    fun sideGapDoesNotDependOnRows() {
        // نفس الشاشة بطولين مختلفين (صفوف مختلفة): المسافة والزجاج ما يتغيرون
        val a = HomeGeometry.compute(GeoInput(411f, 960f, 32f, 24f, 4, rows = 8))
        val b = HomeGeometry.compute(GeoInput(411f, 960f, 32f, 24f, 4, rows = 5))
        assertTrue(a.rows != b.rows)
        assertEquals(a.sideGap, b.sideGap, 0.01f)
        assertEquals(a.side.h, b.side.h, 0.01f)
        assertEquals(a.sideIcon * 0.24f, a.sideGap, 0.01f)
    }

    @Test
    fun outerSideIconEqualsGridIconInnerIsSmaller() {
        for (g in all()) {
            if (g.inner) assertEquals(g.icon * 0.86f, g.sideIcon, 0.01f)
            else assertEquals("أيقونة العمود بالخارجية لازم تساوي الشبكة: ${say(g)}", g.icon, g.sideIcon, 0.01f)
        }
    }

    @Test
    fun cellsAreSquareWhenTheyFit() {
        for (g in all()) {
            assertTrue(g.rows in 4..9)
            if (g.rowH >= g.cell - 0.01f) assertEquals(g.cell, g.rowH, 0.01f)
            assertTrue("الصف أطول من الخانة: ${say(g)}", g.rowH <= g.cell + 0.01f)
        }
    }

    @Test
    fun everythingStaysOnScreenAndApart() {
        for (g in all()) {
            val screen = com.waleed.onyx.grid.Box(0f, 0f, g.width, g.height)
            assertTrue("الشبكة برا الشاشة: ${say(g)}", g.grid.inside(screen))
            assertTrue("الشبكة داخل منطقة الصفحات", g.grid.inside(g.pages))
            assertTrue("الشبكة فوق النقاط: ${say(g)}", g.grid.b <= g.dots.t + 0.01f)
            if (g.sideOn) {
                assertTrue("العمود برا الشاشة: ${say(g)}", g.side.inside(screen))
                g.dock?.let { assertTrue("العمود يدخل بالشريط: ${say(g)}", !g.side.overlaps(it)) }
            }
            g.dock?.let { d ->
                assertTrue("الشريط برا الشاشة: ${say(g)}", d.inside(screen))
                assertTrue("النقاط تدخل بالشريط: ${say(g)}", g.dots.b <= d.t + 0.01f)
                assertTrue(g.dockSlots.size <= g.dockMax)
                for (s in g.dockSlots) assertTrue("أيقونة طالعة من الشريط: ${say(g)}", s.inside(d))
                for (k in 1 until g.dockSlots.size) assertTrue("أيقونات الشريط متداخلة", g.dockSlots[k].l >= g.dockSlots[k - 1].r)
            }
            for (y in 0 until g.rows) for (x in 0 until g.cols)
                assertTrue("أيقونة طالعة من خانتها: ${say(g)}", g.iconBox(x, y).inside(g.cellBox(x, y)))
        }
    }

    @Test
    fun allIconsAreOneSize() {
        for (g in all()) {
            assertTrue("الأيقونة أكبر من 84×الحجم: ${say(g)}", g.icon <= 84f * 1.15f + 0.01f)
            assertTrue(g.icon > 30f)
        }
    }

    @Test
    fun rowsFollowTheChoiceAndShrinkOnlyIfTheyDontFit() {
        val six = HomeGeometry.compute(GeoInput(411f, 960f, 32f, 24f, 4, rows = 6))
        assertEquals(6, six.rows)
        assertEquals(six.cell, six.rowH, 0.01f)
        // الفراغ الزايد ينقسم فوق وتحت بالتساوي
        val area = six.dots.t - (32f + 8f)
        assertEquals((area - six.grid.h) / 2, six.grid.t - (32f + 8f), 0.01f)
        // شاشة قصيرة: ٨ ما تركب، ينقص
        val short = HomeGeometry.compute(GeoInput(411f, 700f, 32f, 24f, 4, rows = 8))
        assertTrue(short.rows < 8 && short.rows >= 4)
    }

    @Test
    fun dockIsOptionalAndGridGrowsWithoutIt() {
        val on = HomeGeometry.compute(GeoInput(411f, 960f, 32f, 24f, 4, dockOn = true))
        val off = HomeGeometry.compute(GeoInput(411f, 960f, 32f, 24f, 4, dockOn = false))
        assertTrue(off.dock == null && off.dockSlots.isEmpty())
        assertTrue(off.rows >= on.rows)
        assertTrue(off.grid.h >= on.grid.h - 0.01f)
    }

    @Test
    fun foldNumbersMatchTheBook() {
        // الخارجية: (411 − 16 − 6) ÷ 5 = 77.8، الأيقونة 78% = 60.7
        val o = HomeGeometry.compute(GeoInput(411f, 960f, 32f, 24f, 4))
        assertEquals(77.8f, o.cell, 0.05f)
        assertEquals(60.68f, o.icon, 0.05f)
        assertTrue(!o.inner)
        val i = HomeGeometry.compute(GeoInput(750f, 832f, 28f, 24f, 6))
        assertTrue(i.inner)
        assertEquals(37.5f, i.padStart, 0.01f)
    }
}
