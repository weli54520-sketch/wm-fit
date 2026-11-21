package com.waleed.onyx.grid

import kotlin.math.abs

/** مكان عنصر بالشبكة: العمود، الصف، العرض، الطول (بالخانات) */
data class Span(val id: Long, val x: Int, val y: Int, val w: Int = 1, val h: Int = 1) {
    fun overlaps(o: Span): Boolean = x < o.x + o.w && x + w > o.x && y < o.y + o.h && y + h > o.y
}

data class Cell(val x: Int, val y: Int)

/** نتيجة الترتيب: المكان الجديد لكل عنصر، واللي ما لقى مكان بالصفحة */
data class Reorder(val cells: Map<Long, Cell>, val overflow: List<Long>)

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

    /**
     * ترتيب الأيقونات مثل الآيفون: [moving] (خانة وحدة) ينحط بخانة [moving].x,y،
     * واللي بينه وبين أقرب فاضي يزحف خانة وحدة بترتيب القراءة.
     * الويدجت (أي عنصر أكبر من خانة) ثابتة، والزحف يتخطاها.
     * [from] = مكان الأيقونة الأصلي لو هي من نفس الصفحة: الزحف يروح باتجاهه.
     * لو الصفحة مليانة: آخر أيقونة تطلع بـ overflow (تنتقل للصفحة اللي بعدها).
     * يرجع null لو الخانة تحت ويدجت.
     */
    fun reorder(others: List<Span>, moving: Span, cols: Int, rows: Int, from: Cell? = null): Reorder? {
        if (!fits(moving.x, moving.y, 1, 1, cols, rows)) return null
        val blocked = Array(rows) { BooleanArray(cols) }
        val fixed = HashMap<Long, Cell>()
        for (o in others) {
            if (o.w == 1 && o.h == 1) continue
            fixed[o.id] = Cell(o.x, o.y)
            for (yy in o.y until minOf(rows, o.y + o.h)) for (xx in o.x until minOf(cols, o.x + o.w)) if (yy >= 0 && xx >= 0) blocked[yy][xx] = true
        }
        if (blocked[moving.y][moving.x]) return null
        // الخانات المتاحة بترتيب القراءة
        val slots = ArrayList<Cell>()
        val index = HashMap<Cell, Int>()
        for (y in 0 until rows) for (x in 0 until cols) if (!blocked[y][x]) { index[Cell(x, y)] = slots.size; slots += Cell(x, y) }
        val occ = arrayOfNulls<Long>(slots.size)
        val stray = ArrayList<Long>()
        for (o in others) {
            if (o.w != 1 || o.h != 1) continue
            val i = index[Cell(o.x, o.y)]
            if (i == null || occ[i] != null) stray += o.id else occ[i] = o.id
        }
        val b = index.getValue(Cell(moving.x, moving.y))
        val a = from?.let { index[it] }
        val overflow = ArrayList<Long>()
        if (occ[b] != null) {
            fun forwardEmpty(): Int? { for (i in b + 1 until occ.size) if (occ[i] == null) return i; return null }
            fun backwardEmpty(): Int? { for (i in b - 1 downTo 0) if (occ[i] == null) return i; return null }
            val back = a != null && a < b
            val fe: Int?
            val be: Int?
            if (back) { be = backwardEmpty(); fe = if (be == null) forwardEmpty() else null }
            else { fe = forwardEmpty(); be = if (fe == null) backwardEmpty() else null }
            when {
                fe != null -> { for (i in fe downTo b + 1) occ[i] = occ[i - 1] }
                be != null -> { for (i in be until b) occ[i] = occ[i + 1] }
                else -> {
                    // مليانة: آخر وحدة تطلع، والباقي يزحف لقدام
                    occ[occ.size - 1]?.let { overflow += it }
                    for (i in occ.size - 1 downTo b + 1) occ[i] = occ[i - 1]
                }
            }
            occ[b] = null
        }
        val cells = HashMap<Long, Cell>(fixed)
        occ.forEachIndexed { i, id -> if (id != null) cells[id] = slots[i] }
        // عناصر بمكان غلط (نادر): أول فاضي، وإلا تطلع
        for (id in stray) {
            val i = occ.indices.firstOrNull { occ[it] == null && it != b }
            if (i != null) { occ[i] = id; cells[id] = slots[i] } else overflow += id
        }
        return Reorder(cells, overflow)
    }

    /** أول مكان فاضي يكفي عنصر بهالمقاس (ترتيب القراءة) */
    /**
     * مثل [solve] بس ما يرفض أبد: الويدجت ينزل بالمكان اللي اخترته حتى لو الصفحة مليانة،
     * واللي ما لقى مكان فاضي بالصفحة يطلع بـ overflow (ينتقل للصفحة اللي بعدها).
     * يرجع null بس لو العنصر نفسه أكبر من الشبكة.
     */
    fun solveOverflow(others: List<Span>, moving: Span, cols: Int, rows: Int): Reorder? {
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
        val cells = HashMap<Long, Cell>()
        val pushed = ArrayList<Span>()
        for (o in others) {
            if (o.overlaps(moving) || !free(o.x, o.y, o.w, o.h)) pushed += o
            else { mark(o.x, o.y, o.w, o.h); cells[o.id] = Cell(o.x, o.y) }
        }
        pushed.sortWith(compareByDescending<Span> { it.w * it.h }.thenBy { it.y }.thenBy { it.x })
        val overflow = ArrayList<Long>()
        for (o in pushed) {
            var best: Cell? = null
            var bestScore = Double.MAX_VALUE
            for (y in 0..rows - o.h) for (x in 0..cols - o.w) {
                if (!free(x, y, o.w, o.h)) continue
                val score = abs(x - o.x) + abs(y - o.y) * 1.15 + (y * cols + x) * 0.001
                if (score < bestScore) { bestScore = score; best = Cell(x, y) }
            }
            if (best == null) overflow += o.id
            else { mark(best.x, best.y, o.w, o.h); cells[o.id] = best }
        }
        return Reorder(cells, overflow)
    }

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
