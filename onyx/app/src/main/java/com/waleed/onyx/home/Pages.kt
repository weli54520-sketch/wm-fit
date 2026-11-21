package com.waleed.onyx.home

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.waleed.onyx.grid.PagerPhysics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * تبطّئ بالنهاية بدون ارتداد (حركة "صفحات" بالكتاب).
 * سرعتها بالبداية = ضعف المتوسط، فتكمل بسرعة إصبعك بدل ما توقف وتبدأ من صفر.
 */
val Decelerate = CubicBezierEasing(0.33f, 0.66f, 0.66f, 1f)

/**
 * الصفحات: نكتبها بنفسنا بدل الأداة الجاهزة، عشان نتحكم بالسرعة وما تعلق بالنص أبد.
 * [offset] بوحدة الصفحات. أي حركة تنتهي دايماً على صفحة كاملة.
 */
class PagesState(private val scope: CoroutineScope) {
    var offset by mutableFloatStateOf(0f)
    var count by mutableIntStateOf(1)
    var animating by mutableStateOf(false)
        private set
    /** آخر صفحة وقفت عليها (يتغير بس لما توقف، عشان ما نرسم صفحات جديدة وسط الحركة) */
    var settled by mutableIntStateOf(0)
        private set
    /** الصفحة اللي رايحين لها الحين */
    var target by mutableIntStateOf(0)
        private set
    private var job: Job? = null

    /** الصفحة الحالية (الأقرب) */
    val current: Int get() = PagerPhysics.nearest(offset, count)

    fun stop() {
        job?.cancel()
        job = null
        animating = false
    }

    /** حركة لين صفحة [page] خلال [durationMs] */
    fun animateTo(page: Int, durationMs: Int) {
        stop()
        val target = page.coerceIn(0, (count - 1).coerceAtLeast(0)).toFloat()
        this.target = target.toInt()
        val from = offset
        if (from == target) { settled = target.toInt(); return }
        animating = true
        val mine = scope.launch {
            animate(from, target, animationSpec = tween(durationMs, easing = Decelerate)) { v, _ -> offset = v }
        }
        job = mine
        mine.invokeOnCompletion {
            if (job === mine) {
                job = null
                animating = false
                // مهما صار: ننتهي على صفحة كاملة
                if (offset != target && it == null) offset = target
                if (it == null) settled = target.toInt()
            }
        }
    }

    /** لو وقفت بالنص لأي سبب: ترجع لأقرب صفحة */
    fun settle(calm: Boolean) {
        if (animating) return
        val t = current
        if (offset != t.toFloat()) animateTo(t, PagerPhysics.settleDurationMs(offset, t, calm))
    }

    /**
     * لما ينقص عدد الصفحات (مثلاً طلعت من وضع الترتيب وأنت على الصفحة الفاضية):
     * ترجع للصفحة اللي قبلها بحركة ناعمة، مو قفزة.
     */
    fun clamp() {
        val max = (count - 1).coerceAtLeast(0)
        if (offset > max || (animating && target > max)) {
            android.util.Log.d("OnyxPages", "clamp animate from=${"%.2f".format(offset)} to=$max")
            animateTo(max, PagerPhysics.settleDurationMs(offset, max, true))
        }
    }
}
