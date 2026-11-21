package com.waleed.onyx.grid

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * فيزياء تقليب الصفحات (كتاب المواصفات، الفصل ٣). كود صافي بدون رسم عشان الحراس يختبرونه.
 * المكان [offset] بوحدة الصفحات: 0 = الصفحة الأولى بالضبط، 1.5 = نص الطريق بين الثانية والثالثة.
 */
object PagerPhysics {
    /** سحبة سريعة: أسرع من كذا (dp بالثانية) تقلب الصفحة حتى لو المسافة قصيرة (رقم لانشر أندرويد الأصلي) */
    const val FLING_DP_PER_SEC = 500f
    /** أو لو سحبت أكثر من 40% من الصفحة (رقم الأصلي) */
    const val SIGNIFICANT_MOVE = 0.4f
    /** المقاومة لما تسحب قبل الصفحة الأولى أو بعد الأخيرة */
    const val EDGE_RESISTANCE = 0.3f

    /** المكان وأنت تسحب: [dxPx] موجب = إصبعك راح يمين = الصفحة اللي قبل */
    fun dragOffset(startOffset: Float, dxPx: Float, pageWidthPx: Float, pageCount: Int): Float {
        val raw = startOffset - dxPx / pageWidthPx
        val max = (pageCount - 1).coerceAtLeast(0).toFloat()
        return when {
            raw < 0f -> raw * EDGE_RESISTANCE
            raw > max -> max + (raw - max) * EDGE_RESISTANCE
            else -> raw
        }
    }

    /**
     * وين تنتهي الصفحة لما تفلت إصبعك. صفحة وحدة بس كل سحبة.
     * [velocityDp] سرعة الإصبع (موجب = يمين).
     */
    fun settleTarget(startPage: Int, offset: Float, velocityDp: Float, pageCount: Int): Int {
        val last = (pageCount - 1).coerceAtLeast(0)
        val moved = offset - startPage
        val target = when {
            abs(velocityDp) >= FLING_DP_PER_SEC -> if (velocityDp < 0) startPage + 1 else startPage - 1
            abs(moved) >= SIGNIFICANT_MOVE -> if (moved > 0) startPage + 1 else startPage - 1
            else -> startPage
        }
        return target.coerceIn(0, last)
    }

    /**
     * مدة الحركة لين الصفحة: هادية تبطّئ بالنهاية. [calm] = هادية (الأساس) أو عادية.
     * كل ما كانت المسافة الباقية أقصر، كانت أقصر، بس ما تنزل تحت حد عشان ما تنط.
     */
    fun settleDurationMs(from: Float, to: Int, calm: Boolean): Int {
        val full = if (calm) 650 else 450
        val min = if (calm) 320 else 240
        val remaining = abs(to - from).coerceIn(0f, 1f)
        return (min + (full - min) * remaining).roundToInt()
    }

    /**
     * مدة الحركة بعد ما تفلت إصبعك، بحيث تبدأ بنفس سرعة إصبعك (ما توقف وتبدأ من صفر).
     * الحركة تبطّئ بالنهاية (منحنى سرعته بالبداية = ضعف المتوسط)، فالمدة = 2 × المسافة ÷ السرعة.
     * [velocityPagesPerSec] سرعة الإصبع بوحدة الصفحات، موجبة لو رايحة باتجاه الهدف.
     */
    fun flingDurationMs(from: Float, to: Int, velocityPagesPerSec: Float, calm: Boolean): Int {
        val dist = abs(to - from)
        val calmMs = settleDurationMs(from, to, calm)
        if (dist < 0.001f) return 0
        if (velocityPagesPerSec <= 0.2f) return calmMs
        val ms = (2f * dist / velocityPagesPerSec * 1000f).roundToInt()
        return ms.coerceIn(if (calm) 240 else 200, calmMs)
    }

    /**
     * بوضع الترتيب: لمست أيقونة وحركت. سحبة سريعة بالعرض = تقليب صفحات، غير كذا = نقل الأيقونة.
     * (مثل الآيفون: السحبة السريعة تقلب، والإمساك ثم التحريك ينقل)
     */
    const val EDIT_SWIPE_MAX_MS = 180L
    fun editTouchIsPageSwipe(elapsedMs: Long, dx: Float, dy: Float): Boolean =
        elapsedMs < EDIT_SWIPE_MAX_MS && abs(dx) > abs(dy) * 1.2f

    /** الصفحة الأقرب للمكان الحالي */
    fun nearest(offset: Float, pageCount: Int): Int = offset.roundToInt().coerceIn(0, (pageCount - 1).coerceAtLeast(0))

    /** الصفحات اللي تبان (الحالية واللي جنبها) */
    fun visible(offset: Float, pageCount: Int): IntRange {
        if (pageCount <= 0) return IntRange.EMPTY
        val a = (floor(offset).toInt() - 1).coerceAtLeast(0)
        val b = (floor(offset).toInt() + 2).coerceAtMost(pageCount - 1)
        return a..b
    }
}

/** وين الأيقونة المسحوبة الحين */
sealed class DragTarget {
    data class CellAt(val page: Int, val x: Int, val y: Int) : DragTarget()
    data class SlotAt(val container: Int, val index: Int) : DragTarget()
    /** فوق الشريط السفلي وفيه مكان: يفسحون له بمكان [index] (على طول، مثل الآيفون) */
    data class DockAt(val index: Int) : DragTarget()
    data class EdgeAt(val dir: Int) : DragTarget()
    object Nowhere : DragTarget()
}

/** قرارات المخ: وش يصير الحين */
sealed class DragEvent {
    /** الإصبع وقف على خانة كفاية: الأيقونات تزحف مرة وحدة */
    data class Commit(val cell: DragTarget.CellAt) : DragEvent()
    /** وقف فوق أيقونة بالعمود أو الشريط كفاية: تكبر شوي */
    data class SlotReady(val slot: DragTarget.SlotAt) : DragEvent()
    /** وقف عند الحافة كفاية: تقليب صفحة */
    data class Flip(val dir: Int) : DragEvent()
}

/**
 * مخ السحب: يقرر متى تزحف الأيقونات، ومتى تتقلب الصفحة، ومتى يجهز العمود.
 * ما يتحرك شي إلا لما يوقف الإصبع (كتاب المواصفات، الفصل ٥). الوقت يجي من برا عشان نختبره.
 */
class DragBrain(
    val reorderDwellMs: Long = 350,
    val slotDwellMs: Long = 200,
    val edgeDwellMs: Long = 500,
    val reflipMs: Long = 900,
) {
    var hover: DragTarget = DragTarget.Nowhere
        private set
    private var hoverSince = 0L
    var committed: DragTarget.CellAt? = null
        private set
    var slotReady: DragTarget.SlotAt? = null
        private set
    private var lastFlipAt = Long.MIN_VALUE / 2

    /** نناديه مع كل حركة إصبع، وكل شوي حتى لو الإصبع واقف */
    fun update(target: DragTarget, now: Long): DragEvent? {
        if (target != hover) {
            hover = target
            hoverSince = now
            if (target !is DragTarget.SlotAt) slotReady = null
        }
        val held = now - hoverSince
        return when (target) {
            is DragTarget.CellAt ->
                if (committed != target && held >= reorderDwellMs) { committed = target; DragEvent.Commit(target) } else null
            is DragTarget.SlotAt ->
                if (slotReady != target && held >= slotDwellMs) { slotReady = target; DragEvent.SlotReady(target) } else null
            is DragTarget.EdgeAt ->
                if (held >= edgeDwellMs && now - lastFlipAt >= reflipMs) {
                    lastFlipAt = now
                    hoverSince = now
                    committed = null
                    DragEvent.Flip(target.dir)
                } else null
            is DragTarget.DockAt -> null
            DragTarget.Nowhere -> null
        }
    }

    /** بعد تقليب الصفحة: الترتيب اللي قبل ما عاد له معنى */
    fun pageChanged() {
        committed = null
        hover = DragTarget.Nowhere
    }
}
