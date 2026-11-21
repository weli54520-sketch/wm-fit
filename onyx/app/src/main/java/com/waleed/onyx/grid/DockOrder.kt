package com.waleed.onyx.grid

/**
 * ترتيب الشريط السفلي مثل الآيفون (كتاب المواصفات، الفصل ٥). كود صافي عشان الحراس يختبرونه.
 * الشريط = قائمة مرتبة بدون فراغات. null = المكان اللي يفسحونه للأيقونة المسحوبة.
 */
object DockOrder {
    /**
     * شكل الشريط وأنت تسحب: [ids] الشريط الحين، [dragged] اللي بيدك (لو كان منه ينشال من مكانه)،
     * [gapAt] وين يفسحون له (null = ما يفسحون، يعني الباقي متجمع).
     */
    fun arrange(ids: List<Long>, dragged: Long?, gapAt: Int?): List<Long?> {
        val rest: MutableList<Long?> = ids.filter { it != dragged }.toMutableList()
        if (gapAt != null) rest.add(gapAt.coerceIn(0, rest.size), null)
        return rest
    }

    /** الشريط بعد ما تفلت [dragged] بمكان [at] (من الشريط نفسه أو من برا) */
    fun drop(ids: List<Long>, dragged: Long, at: Int): List<Long> {
        val rest = ids.filter { it != dragged }.toMutableList()
        rest.add(at.coerceIn(0, rest.size), dragged)
        return rest
    }

    /** يفسحون؟ لو الأيقونة من الشريط نفسه، أو فيه مكان */
    fun canInsert(size: Int, capacity: Int, fromDock: Boolean): Boolean = fromDock || size < capacity

    /** عدد الأماكن وأنت فوق الشريط (مع المكان اللي يفسحونه) */
    fun slotsWhileHovering(size: Int, fromDock: Boolean): Int = size - (if (fromDock) 1 else 0) + 1
}

/** نقاط الصفحات: كل نقطة بخانة عرضها ثابت، فالضغط والسحب يعرف أي صفحة بالضبط */
object DotsMath {
    /** عرض خانة النقطة (dp) */
    const val SLOT = 18f
    /** مساحة لمس زيادة يمين ويسار الصف */
    const val HIT_PAD = 14f

    fun rowWidth(count: Int) = SLOT * count
    fun left(centerX: Float, count: Int) = centerX - rowWidth(count) / 2f

    /** نص نقطة الصفحة [i] */
    fun dotCenter(i: Int, centerX: Float, count: Int) = left(centerX, count) + SLOT * (i + 0.5f)

    /** فوق النقاط؟ (بالعرض بس، الطول يفحصه اللي يناديه) */
    fun onRow(x: Float, centerX: Float, count: Int): Boolean {
        if (count <= 1) return false
        val l = left(centerX, count)
        return x >= l - HIT_PAD && x <= l + rowWidth(count) + HIT_PAD
    }

    /** أي صفحة تحت الإصبع (لو برا الصف: الأقرب، عشان السحب ما يوقف) */
    fun indexAt(x: Float, centerX: Float, count: Int): Int {
        if (count <= 1) return 0
        return ((x - left(centerX, count)) / SLOT).toInt().coerceIn(0, count - 1)
    }
}
