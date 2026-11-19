package com.waleed.launcher.trial

import com.waleed.launcher.trial.widgets.Placed
import com.waleed.launcher.trial.widgets.WSize
import kotlin.math.max
import kotlin.math.min

/** صفحة وحدة: الويدجت اللي فيها، والتطبيقات تعبي الخانات الفاضية */
data class PagePlan(val widgets: List<Placed>, val apps: List<Triple<AppEntry, Int, Int>>)

fun clampWidget(w: Placed, cols: Int, rows: Int): Placed {
    val ww = min(w.size.w, cols)
    val hh = min(w.size.h, rows)
    return w.copy(x = w.x.coerceIn(0, max(0, cols - ww)), y = w.y.coerceIn(0, max(0, rows - hh)))
}

private fun occupies(w: Placed, x: Int, y: Int) = x >= w.x && x < w.x + w.size.w && y >= w.y && y < w.y + w.size.h

/** هل المقاس يركب بهالمكان بدون ما يغطي ويدجت ثاني؟ */
fun fits(cols: Int, rows: Int, others: List<Placed>, x: Int, y: Int, size: WSize): Boolean {
    if (x < 0 || y < 0 || x + size.w > cols || y + size.h > rows) return false
    for (yy in y until y + size.h) for (xx in x until x + size.w) {
        if (others.any { occupies(it, xx, yy) }) return false
    }
    return true
}

fun freeSpot(cols: Int, rows: Int, others: List<Placed>, size: WSize): Pair<Int, Int>? {
    for (y in 0..rows - size.h) for (x in 0..cols - size.w) if (fits(cols, rows, others, x, y, size)) return x to y
    return null
}

fun planPages(cols: Int, rows: Int, widgets: List<Placed>, apps: List<AppEntry>): List<PagePlan> {
    val lastWidgetPage = widgets.maxOfOrNull { it.page } ?: 0
    val pages = mutableListOf<PagePlan>()
    var ai = 0
    var p = 0
    while ((p <= lastWidgetPage || ai < apps.size) && p < 60) {
        val ws = widgets.filter { it.page == p }.map { clampWidget(it, cols, rows) }
        val placed = mutableListOf<Triple<AppEntry, Int, Int>>()
        for (y in 0 until rows) for (x in 0 until cols) {
            if (ai >= apps.size) break
            if (ws.none { occupies(it, x, y) }) placed += Triple(apps[ai++], x, y)
        }
        pages += PagePlan(ws, placed)
        p++
    }
    if (pages.isEmpty()) pages += PagePlan(emptyList(), emptyList())
    return pages
}
