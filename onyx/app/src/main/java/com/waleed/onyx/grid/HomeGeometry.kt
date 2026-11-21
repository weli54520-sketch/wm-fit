package com.waleed.onyx.grid

import kotlin.math.floor

/** مستطيل بوحدة dp. كود صافي عشان الحراس يختبرونه بدون جوال */
data class Box(val l: Float, val t: Float, val r: Float, val b: Float) {
    val w get() = r - l
    val h get() = b - t
    val cx get() = (l + r) / 2
    val cy get() = (t + b) / 2
    fun overlaps(o: Box) = l < o.r && r > o.l && t < o.b && b > o.t
    fun inside(o: Box, eps: Float = 0.01f) = l >= o.l - eps && t >= o.t - eps && r <= o.r + eps && b <= o.b + eps
    /** نفس المستطيل مصغر حول نصه */
    fun scaled(k: Float) = Box(cx - w * k / 2, cy - h * k / 2, cx + w * k / 2, cy + h * k / 2)
}

/** اللي تحتاجه المعادلة: مقاس الشاشة والإعدادات (كله dp) */
data class GeoInput(
    val width: Float,
    val height: Float,
    val topInset: Float,
    val bottomInset: Float,
    val cols: Int,
    /** عدد الصفوف اللي يبيه وليد (ينقص لو ما يركب) */
    val rows: Int = 6,
    val sideOn: Boolean = true,
    val sideCount: Int = 4,
    val dockOn: Boolean = true,
    val dockCount: Int = 6,
    val iconScale: Float = 1f,
)

/**
 * كل أماكن الشاشة الرئيسية من معادلة وحدة (كتاب المواصفات، الفصل ٢).
 * ما نقرا أي مكان من الشاشة: كل شي ينحسب من هنا، والحراس يتأكدون إن القواعد ما تنكسر.
 */
data class HomeGeometry(
    val inner: Boolean,
    val width: Float,
    val height: Float,
    val cols: Int,
    val rows: Int,
    /** عرض الخانة */
    val cell: Float,
    /** طول الصف (= عرض الخانة لو الطول يكفي ٤ صفوف) */
    val rowH: Float,
    val icon: Float,
    /** يسار الشبكة داخل منطقة الصفحات */
    val padStart: Float,
    /** منطقة الصفحات: تبدأ من حافة الشاشة وتنتهي قبل العمود. اللي يطلع عنها ينقص */
    val pages: Box,
    /** الشبكة نفسها (داخل منطقة الصفحات) */
    val grid: Box,
    val dots: Box,
    val sideOn: Boolean,
    val sideCount: Int,
    val sideIcon: Float,
    val sideGap: Float,
    /** زجاج العمود */
    val side: Box,
    val sideSlots: List<Box>,
    val dockOn: Boolean,
    val dockIcon: Float,
    val dock: Box?,
    val dockSlots: List<Box>,
    /** أقصى عدد يركب بالشريط السفلي بهالشاشة */
    val dockMax: Int,
    /** المسافة بين نص أيقونة ونص اللي جنبها بالشريط (ثابتة مهما كان العدد) */
    val dockStep: Float = 0f,
) {
    // ───────── الشريط مثل الآيفون: الأيقونات متجمعة بالنص، والزجاج على قدها ─────────

    /** أماكن [n] أيقونات بالشريط، متجمعة بالنص بمسافة ثابتة */
    fun dockRow(n: Int): List<Box> {
        val d = dock ?: return emptyList()
        if (n <= 0) return emptyList()
        val first = d.cx - dockStep * (n - 1) / 2f
        return (0 until n).map { k ->
            val cx = first + dockStep * k
            Box(cx - dockIcon / 2, d.cy - dockIcon / 2, cx + dockIcon / 2, d.cy + dockIcon / 2)
        }
    }

    /** زجاج الشريط لـ [n] أيقونات (أقل شي مكان وحدة). بالعدد الأقصى = [dock] بالضبط */
    fun dockGlass(n: Int): Box? {
        val d = dock ?: return null
        val w = (dockStep * n.coerceAtLeast(1) + 16f).coerceAtMost(d.w)
        return Box(d.cx - w / 2, d.t, d.cx + w / 2, d.b)
    }

    /** وين تنحط الأيقونة لو فلتها عند [x] والشريط فيه [n] أماكن (مع مكانها) */
    fun dockInsertIndex(x: Float, n: Int): Int {
        val d = dock ?: return 0
        if (n <= 1) return 0
        val first = d.cx - dockStep * (n - 1) / 2f
        return Math.round((x - first) / dockStep).coerceIn(0, n - 1)
    }

    /** منطقة الشريط وأنت تسحب (عرض الشريط كامل، وشوي فوقه) */
    fun inDockZone(x: Float, y: Float): Boolean {
        val d = dock ?: return false
        return x >= d.l && x <= d.r && y >= d.t - 12f && y <= d.b + 12f
    }

    /** خانة بالشبكة (x,y) */
    fun cellBox(x: Int, y: Int, w: Int = 1, h: Int = 1) =
        Box(grid.l + x * cell, grid.t + y * rowH, grid.l + (x + w) * cell, grid.t + (y + h) * rowH)

    /** الأيقونة داخل الخانة (بالنص) */
    fun iconBox(x: Int, y: Int): Box {
        val c = cellBox(x, y)
        return Box(c.cx - icon / 2, c.cy - icon / 2, c.cx + icon / 2, c.cy + icon / 2)
    }

    // ───────── من الشاشة للشبكة وبالعكس (k = تصغير وضع الترتيب حول نص منطقة الصفحات) ─────────

    /** نقطة على الشاشة ← مكانها بالصفحة لو ما كانت مصغرة */
    fun toLocal(x: Float, y: Float, k: Float): Pair<Float, Float> =
        (pages.cx + (x - pages.cx) / k) to (pages.cy + (y - pages.cy) / k)

    /** مستطيل بالصفحة ← مكانه على الشاشة بعد التصغير */
    fun toScreen(b: Box, k: Float) = Box(
        pages.cx + (b.l - pages.cx) * k, pages.cy + (b.t - pages.cy) * k,
        pages.cx + (b.r - pages.cx) * k, pages.cy + (b.b - pages.cy) * k,
    )

    /** الخانة اللي ينزل فيها عنصر مقاسه w×h لو نصه عند (cx, cy) على الشاشة */
    fun cellFor(cx: Float, cy: Float, w: Int, h: Int, k: Float): Pair<Int, Int> {
        val (lx, ly) = toLocal(cx, cy, k)
        val x = Math.round((lx - grid.l) / cell - w / 2f).coerceIn(0, (cols - w).coerceAtLeast(0))
        val y = Math.round((ly - grid.t) / rowH - h / 2f).coerceIn(0, (rows - h).coerceAtLeast(0))
        return x to y
    }

    /**
     * الإصبع بمنطقة تقليب الصفحة؟ -1 يسار، 1 يمين، 0 لا.
     * اليمين: من آخر 28 بمنطقة الصفحات لين حافة الشاشة (أيقونات العمود لها الأولوية، تنفحص قبل).
     * كذا ما فيه مكان ميت: لو دفعت الأيقونة لطرف الشاشة تتقلب الصفحة.
     */
    fun edgeAt(x: Float, y: Float): Int {
        if (y < pages.t || y > pages.b) return 0
        return when {
            x < pages.l + EDGE_ZONE -> -1
            x > pages.r - EDGE_ZONE -> 1
            else -> 0
        }
    }

    /** أيقونة بالعمود تحت الإصبع: رقمها، أو null */
    fun sideSlotAt(x: Float, y: Float): Int? =
        if (!sideOn) null else sideSlots.indexOfFirst { x >= it.l && x <= it.r && y >= it.t && y <= it.b }.takeIf { it >= 0 }

    /** أيقونة بالشريط تحت الإصبع: رقمها، أو null */
    fun dockSlotAt(x: Float, y: Float): Int? =
        dockSlots.indexOfFirst { x >= it.l && x <= it.r && y >= it.t && y <= it.b }.takeIf { it >= 0 }

    /** داخل منطقة الصفحات؟ */
    fun inPages(x: Float, y: Float) = x >= pages.l && x <= pages.r && y >= pages.t && y <= pages.b

    companion object {
        /** عرض منطقة تقليب الصفحة وأنت تسحب */
        const val EDGE_ZONE = 28f
        const val INNER_MIN_WIDTH = 600f
        const val EDGE = 6f
        const val SIDE_PAD = 7f
        const val SIDE_GAP = 0.24f
        const val ICON_OF_CELL = 0.78f
        const val ICON_MAX = 84f
        const val INNER_SIDE = 0.86f
        const val DOCK_ICON = 0.86f
        const val DOTS_H = 22f
        const val MIN_ROWS = 4
        const val MAX_ROWS = 9
        /** تصغير الصفحة بوضع الترتيب */
        const val EDIT_SCALE = 0.88f

        fun compute(i: GeoInput): HomeGeometry {
            val w = i.width
            val h = i.height
            val inner = w >= INNER_MIN_WIDTH
            val cols = i.cols.coerceAtLeast(1)
            val padStart = if (inner) w * 0.05f else 16f

            // الخانة: الشبكة + خانة محجوزة للعمود
            val cell = if (i.sideOn) (w - padStart - EDGE) / (cols + 1) else (w - padStart * 2) / cols

            // الأيقونة: 78% من الخانة وأقصى 84، والحجم من الإعدادات.
            // ولازم يبقى العمود (الأيقونة + 14) أصغر من خانته بـ 2 على الأقل، عشان ما يلمس الشبكة
            val icon = minOf(minOf(cell * ICON_OF_CELL, ICON_MAX) * i.iconScale, cell - SIDE_PAD * 2 - 2.5f)
            val sideIcon = if (inner) icon * INNER_SIDE else icon
            val sideGap = sideIcon * SIDE_GAP

            // الشريط السفلي (اختياري)
            val dockIcon = icon * DOCK_ICON
            val dockH = dockIcon + 20f
            val dockW = if (inner) minOf(w * 0.66f, 640f) else w - 32f
            val dockMax = floor((dockW - 16f) / (dockIcon * 1.1f)).toInt().coerceAtLeast(3)
            val dockCount = i.dockCount.coerceIn(3, dockMax)
            val dockBottom = h - i.bottomInset - 14f
            val dock = if (i.dockOn) Box((w - dockW) / 2, dockBottom - dockH, (w + dockW) / 2, dockBottom) else null
            val dockSlots = if (dock == null) emptyList() else {
                val step = (dock.w - 16f) / dockCount
                (0 until dockCount).map { k ->
                    val cx = dock.l + 8f + step * (k + 0.5f)
                    Box(cx - dockIcon / 2, dock.cy - dockIcon / 2, cx + dockIcon / 2, dock.cy + dockIcon / 2)
                }
            }

            // المساحة للشبكة: تحت شريط الحالة، وفوق النقاط والشريط
            val areaTop = i.topInset + 8f
            val dotsBottom = (dock?.t ?: (h - i.bottomInset)) - 6f
            val dotsTop = dotsBottom - DOTS_H
            val areaBottom = dotsTop
            val areaH = (areaBottom - areaTop).coerceAtLeast(1f)

            // خانات مربعة بعدد الصفوف اللي يبيه وليد، والفراغ الزايد ينقسم فوق وتحت.
            // لو ما يركب، ينقص العدد. ولو ما يركب حتى ٤ صفوف مربعة، نقصّر الصف شوي بس
            val fit = floor(areaH / cell).toInt()
            val rows = minOf(i.rows.coerceIn(MIN_ROWS, MAX_ROWS), maxOf(fit, MIN_ROWS))
            val rowH = if (fit >= MIN_ROWS) cell else areaH / MIN_ROWS
            val gridH = rowH * rows
            val gridTop = areaTop + (areaH - gridH) / 2
            val grid = Box(padStart, gridTop, padStart + cell * cols, gridTop + gridH)
            val pages = Box(0f, gridTop, grid.r, gridTop + gridH)
            val dots = Box(0f, dotsTop, w, dotsBottom)

            // العمود: منفصل، لاصق يمين، بنص منطقة الصفحات، ومسافته ثابتة
            val sideCount = i.sideCount.coerceIn(3, 5)
            val sideW = sideIcon + SIDE_PAD * 2
            val sideH = sideIcon * sideCount + sideGap * (sideCount - 1) + (SIDE_PAD + sideGap / 2) * 2
            val sideR = w - EDGE
            val sideTop = (pages.cy - sideH / 2).coerceAtLeast(areaTop)
            val side = Box(sideR - sideW, sideTop, sideR, sideTop + sideH)
            val sideSlots = (0 until sideCount).map { k ->
                val t = side.t + SIDE_PAD + sideGap / 2 + k * (sideIcon + sideGap)
                Box(side.l + SIDE_PAD, t, side.l + SIDE_PAD + sideIcon, t + sideIcon)
            }

            return HomeGeometry(
                inner = inner, width = w, height = h, cols = cols, rows = rows, cell = cell, rowH = rowH,
                icon = icon, padStart = padStart, pages = pages, grid = grid, dots = dots,
                sideOn = i.sideOn, sideCount = sideCount, sideIcon = sideIcon, sideGap = sideGap, side = side, sideSlots = sideSlots,
                dockOn = i.dockOn, dockIcon = dockIcon, dock = dock, dockSlots = dockSlots, dockMax = dockMax,
                dockStep = if (dock == null) 0f else (dock.w - 16f) / dockCount,
            )
        }
    }
}
