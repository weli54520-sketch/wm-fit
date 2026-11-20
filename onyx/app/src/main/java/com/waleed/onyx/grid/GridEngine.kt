package com.waleed.onyx.grid

import kotlin.math.abs

/** مكان عنصر بالشبكة: العمود، الصف، العرض، الطول (بالخانات) */
data class Span(val id: Long, val x: Int, val y: Int, val w: Int = 1, val h: Int = 1) {
    fun overlaps(o: Span): Boolean = x < o.x + o.w && x + w > o.x && y < o.y + o.h && y + h > o.y
}

data class Cell(val x: Int, val y: Int)

/**
 * محرك الشبكة: كود صافي بدون رسم.
 * يقرر وين يركب العنصر، مين يتزحزح، ووين أقرب مكان فاضي.
 */
object GridEngine {

    fun fits(x: Int, y: Int, w: Int, h: Int, cols: Int, rows: Int): Boolean =
        x >= 0 && y >= 0 && x + w <= cols && y + h <= rows

    /**
     * نحط [moving] بمكانه الجديد، وكل اللي يتصادم معه يروح لأقرب مكان فاضي.
     * الحساب دايماً من الأماكن الأصلية [others]، فما يتلخبط الترتيب وأنت تتردد.
     * يرجع الأماكن الجديدة للباقين، أو null لو ما فيه مكان يكفي.
     */
    fun solve(others: List<Span>, moving: Span, cols: Int, rows: Int): Map<Long, Cell>? {
        if (!fits(moving.x, moving.y, moving.w, moving.h, cols, rows)) return null
        val taken = Array(rows) { BooleanArray(cols) }
        fun free(x: Int, y: Int, w: Int, h: Int): Boolean {
            if (!fits(x, y, w, h, cols, rows)) return false
            for (yy in y until y + h) for (xx in x until x + w) if (taken[yy][xx]) return false
            return true
        }
        fun mark(x: Int, y: Int, w: Int, h: Int) {
            for (yy in y until y + h) for (xx in x until x + w) taken[yy][xx] = true
        }

        mark(moving.x, moving.y, moving.w, moving.h)
        val result = HashMap<Long, Cell>()
        val pushed = ArrayList<Span>()
        for (o in others) {
            if (o.overlaps(moving)) {
                pushed += o
            } else {
                if (!free(o.x, o.y, o.w, o.h)) return null
                mark(o.x, o.y, o.w, o.h)
                result[o.id] = Cell(o.x, o.y)
            }
        }
        // الكبير يتحرك أول، وبعدين حسب ترتيب القراءة
        pushed.sortWith(compareByDescending<Span> { it.w * it.h }.thenBy { it.y }.thenBy { it.x })
        for (o in pushed) {
            var best: Cell? = null
            var bestScore = Double.MAX_VALUE
            for (y in 0..rows - o.h) for (x in 0..cols - o.w) {
                if (!free(x, y, o.w, o.h)) continue
                val score = abs(x - o.x) + abs(y - o.y) * 1.15 + (y * cols + x) * 0.001
                if (score < bestScore) { bestScore = score; best = Cell(x, y) }
            }
            val b = best ?: return null
            mark(b.x, b.y, o.w, o.h)
            result[o.id] = b
        }
        return result
    }

    /** أول مكان فاضي يكفي عنصر بهالمقاس (ترتيب القراءة) */
    fun freeSpot(items: List<Span>, w: Int, h: Int, cols: Int, rows: Int): Cell? {
        for (y in 0..rows - h) for (x in 0..cols - w) {
            val t = Span(-1, x, y, w, h)
            if (items.none { it.overlaps(t) }) return Cell(x, y)
        }
        return null
    }

    /** الخانة اللي ينزل فيها عنصر، من مكان زاويته اللي فوق بالبكسل */
    fun cellAt(px: Float, py: Float, cellW: Float, cellH: Float, w: Int, h: Int, cols: Int, rows: Int): Cell {
        val x = Math.round(px / cellW).coerceIn(0, (cols - w).coerceAtLeast(0))
        val y = Math.round(py / cellH).coerceIn(0, (rows - h).coerceAtLeast(0))
        return Cell(x, y)
    }

    /**
     * بعد ما يتغير عدد الأعمدة أو الصفوف: كل عنصر يبقى بمكانه لو يركب،
     * وإلا يروح لأقرب فاضي بنفس الصفحة، وإلا للصفحة اللي بعدها.
     * يرجع (الصفحة، المكان) لكل عنصر.
     */
    fun reflow(pages: List<List<Span>>, cols: Int, rows: Int): Map<Long, Pair<Int, Cell>> {
        val out = HashMap<Long, Pair<Int, Cell>>()
        val placed = ArrayList<ArrayList<Span>>()
        pages.forEachIndexed { pageIndex, items ->
            for (s0 in items.sortedWith(compareBy<Span> { it.y }.thenBy { it.x })) {
                val w = s0.w.coerceAtMost(cols)
                val h = s0.h.coerceAtMost(rows)
                var s = s0.copy(x = s0.x.coerceIn(0, cols - w), y = s0.y.coerceIn(0, rows - h), w = w, h = h)
                var p = pageIndex
                while (true) {
                    while (placed.size <= p) placed.add(ArrayList())
                    val list = placed[p]
                    if (list.none { it.overlaps(s) }) break
                    val spot = freeSpot(list, w, h, cols, rows)
                    if (spot != null) { s = s.copy(x = spot.x, y = spot.y); break }
                    p++
                }
                placed[p].add(s)
                out[s.id] = p to Cell(s.x, s.y)
            }
        }
        return out
    }
}
