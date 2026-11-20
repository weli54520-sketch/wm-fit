package com.waleed.onyx.grid

import kotlin.math.ceil
import kotlin.math.floor

/** حدود مقاس الويدجت بالخانات */
data class SpanLimits(val minW: Int, val maxW: Int, val minH: Int, val maxH: Int) {
    fun clampW(w: Int) = w.coerceIn(minW, maxW)
    fun clampH(h: Int) = h.coerceIn(minH, maxH)
}

/**
 * يحول مقاسات الويدجت (بالبكسل، مثل ما يعطيها التطبيق) لخانات بشبكتنا.
 * كود صافي عشان نختبره.
 */
object WidgetSpan {

    /** كم خانة يحتاج هالعرض. نسامح بشوي عشان ما يطلع خانة زيادة على الفاضي */
    fun cellsFor(px: Int, cellPx: Float, max: Int): Int {
        if (px <= 0 || cellPx <= 0f) return 1
        return ceil(px / cellPx - 0.12f).toInt().coerceIn(1, max.coerceAtLeast(1))
    }

    /** المقاس الأول لما ينحط: اللي يقترحه التطبيق، أو من أصغر مقاس له */
    fun defaultSpan(targetW: Int, targetH: Int, minWpx: Int, minHpx: Int, cellW: Float, cellH: Float, cols: Int, rows: Int): Pair<Int, Int> {
        val w = if (targetW > 0) targetW.coerceIn(1, cols) else cellsFor(minWpx, cellW, cols)
        val h = if (targetH > 0) targetH.coerceIn(1, rows) else cellsFor(minHpx, cellH, rows)
        return w to h
    }

    /**
     * أصغر وأكبر مقاس يسمح فيه التطبيق.
     * لو ما يسمح بالتكبير بالعرض أو الطول، يبقى ثابت على [curW] أو [curH].
     */
    fun limits(
        curW: Int, curH: Int,
        minResizeWpx: Int, minResizeHpx: Int, maxResizeWpx: Int, maxResizeHpx: Int,
        horizontal: Boolean, vertical: Boolean,
        cellW: Float, cellH: Float, cols: Int, rows: Int,
    ): SpanLimits {
        fun maxCells(px: Int, cell: Float, cap: Int) =
            if (px <= 0) cap else floor(px / cell + 0.12f).toInt().coerceIn(1, cap)
        val minW = if (horizontal) cellsFor(minResizeWpx, cellW, cols) else curW
        val minH = if (vertical) cellsFor(minResizeHpx, cellH, rows) else curH
        val maxW = if (horizontal) maxCells(maxResizeWpx, cellW, cols) else curW
        val maxH = if (vertical) maxCells(maxResizeHpx, cellH, rows) else curH
        return SpanLimits(
            minW.coerceAtMost(curW).coerceAtLeast(1),
            maxW.coerceAtLeast(curW).coerceAtMost(cols),
            minH.coerceAtMost(curH).coerceAtLeast(1),
            maxH.coerceAtLeast(curH).coerceAtMost(rows),
        )
    }

    /** المقاس الجديد وأنت تسحب الزاوية: [dx] و [dy] بالبكسل من بداية السحب */
    fun resized(startW: Int, startH: Int, dx: Float, dy: Float, cellW: Float, cellH: Float, x: Int, y: Int, cols: Int, rows: Int, lim: SpanLimits): Pair<Int, Int> {
        val w = (startW + Math.round(dx / cellW)).coerceIn(lim.minW, minOf(lim.maxW, cols - x).coerceAtLeast(lim.minW))
        val h = (startH + Math.round(dy / cellH)).coerceIn(lim.minH, minOf(lim.maxH, rows - y).coerceAtLeast(lim.minH))
        return w to h
    }
}
