package com.waleed.onyx.grid

import kotlin.math.abs
import kotlin.math.hypot

/**
 * حاسبة شريط البحث: تكتب 25×4 أو ٢٥*٤ ويطلع الجواب. كود صافي عشان الحراس يختبرونه.
 * تفهم + − × ÷ * / ^ % والأقواس، والأرقام العربية والإنجليزية. لو مو عملية حسابية ترجع null.
 */
object Calc {
    fun normalize(s: String): String {
        val sb = StringBuilder()
        for (c in s) {
            sb.append(
                when (c) {
                    in '٠'..'٩' -> '0' + (c - '٠')
                    in '۰'..'۹' -> '0' + (c - '۰')
                    '×', 'x', 'X' -> '*'
                    '÷' -> '/'
                    '−', '–' -> '-'
                    '٫', ',' -> '.'
                    else -> c
                },
            )
        }
        return sb.toString().replace(" ", "")
    }

    /** فيه عملية حسابية فعلاً؟ (رقم وعلامة على الأقل) */
    fun looksLikeMath(s: String): Boolean {
        val n = normalize(s)
        if (n.isEmpty() || n.any { it !in "0123456789.+-*/^%()" }) return false
        return n.any { it.isDigit() } && n.drop(1).any { it in "+-*/^%" }
    }

    fun eval(s: String): Double? {
        if (!looksLikeMath(s)) return null
        val p = P(normalize(s))
        return try {
            val v = p.expr()
            if (p.i != p.s.length || v.isNaN() || v.isInfinite()) null else v
        } catch (e: Exception) {
            null
        }
    }

    /** الجواب مكتوب حلو: بدون .0 وبحد أقصى ٦ خانات بعد الفاصلة */
    fun format(v: Double): String {
        if (v == Math.rint(v) && abs(v) < 1e15) return v.toLong().toString()
        return "%.6f".format(java.util.Locale.US, v).trimEnd('0').trimEnd('.')
    }

    private class P(val s: String) {
        var i = 0
        fun peek() = if (i < s.length) s[i] else '\u0000'
        fun expr(): Double {
            var v = term()
            while (true) {
                when (peek()) {
                    '+' -> { i++; v += term() }
                    '-' -> { i++; v -= term() }
                    else -> return v
                }
            }
        }
        fun term(): Double {
            var v = power()
            while (true) {
                when (peek()) {
                    '*' -> { i++; v *= power() }
                    '/' -> { i++; val d = power(); if (d == 0.0) throw ArithmeticException(); v /= d }
                    else -> return v
                }
            }
        }
        fun power(): Double {
            val b = unary()
            if (peek() == '^') { i++; return Math.pow(b, power()) }
            return b
        }
        fun unary(): Double {
            if (peek() == '-') { i++; return -unary() }
            if (peek() == '+') { i++; return unary() }
            var v = atom()
            while (peek() == '%') { i++; v /= 100.0 }
            return v
        }
        fun atom(): Double {
            if (peek() == '(') {
                i++
                val v = expr()
                if (peek() != ')') throw IllegalStateException()
                i++
                return v
            }
            val st = i
            while (peek().isDigit() || peek() == '.') i++
            if (st == i) throw IllegalStateException()
            return s.substring(st, i).toDouble()
        }
    }
}

/** آخر التطبيقات وآخر عمليات البحث: الجديد أول، بدون تكرار، وعدد محدود */
object Recents {
    fun push(list: List<String>, item: String, max: Int): List<String> {
        if (item.isBlank()) return list.take(max)
        return (listOf(item) + list.filter { it != item }).take(max)
    }
}

/** الإيماءات بإصبعين على الشاشة الرئيسية (كتاب المواصفات، الفصل ٤) */
enum class TwoFinger { NONE, PINCH_IN, PINCH_OUT, SWIPE_UP, SWIPE_DOWN }

object GestureMath {
    /** أقل تغير بالمسافة بين الإصبعين يعتبر قرصة (dp) */
    const val PINCH_DP = 60f
    /** أقل سحبة بالإصبعين (dp) */
    const val SWIPE_DP = 70f

    /**
     * [a0] [b0] مكان الإصبعين بالبداية، و[a1] [b1] بالنهاية (dp، أزواج x,y).
     * القرصة لها الأولوية لو تغيرت المسافة أكثر من الحركة.
     */
    fun classify(a0: FloatArray, b0: FloatArray, a1: FloatArray, b1: FloatArray): TwoFinger {
        val d0 = hypot(a0[0] - b0[0], a0[1] - b0[1])
        val d1 = hypot(a1[0] - b1[0], a1[1] - b1[1])
        val dy = ((a1[1] - a0[1]) + (b1[1] - b0[1])) / 2f
        val dx = ((a1[0] - a0[0]) + (b1[0] - b0[0])) / 2f
        val pinch = d1 - d0
        if (abs(pinch) >= PINCH_DP && abs(pinch) > abs(dy)) return if (pinch < 0) TwoFinger.PINCH_IN else TwoFinger.PINCH_OUT
        if (abs(dy) >= SWIPE_DP && abs(dy) > abs(dx)) return if (dy < 0) TwoFinger.SWIPE_UP else TwoFinger.SWIPE_DOWN
        return TwoFinger.NONE
    }

    /** ضغطتين: الثانية خلال [maxMs] وقريبة من الأولى */
    const val DOUBLE_TAP_MS = 300L
    const val DOUBLE_TAP_DP = 40f
    fun isDoubleTap(lastMs: Long, lastX: Float, lastY: Float, nowMs: Long, x: Float, y: Float): Boolean =
        nowMs - lastMs in 1..DOUBLE_TAP_MS && hypot(x - lastX, y - lastY) <= DOUBLE_TAP_DP
}

/** المجلد: ترتيب التطبيقات داخله، وصفحاته (٩ بكل صفحة) */
object FolderMath {
    const val PER_PAGE = 9

    fun pages(count: Int): Int = if (count <= 0) 1 else (count + PER_PAGE - 1) / PER_PAGE

    /** أول ٩ تطبيقات تبان مصغرة على أيقونة المجلد */
    fun <T> preview(items: List<T>): List<T> = items.take(PER_PAGE)

    /** يضيف تطبيق بآخر المجلد ويرجع الترتيب الجديد (بدون تكرار) */
    fun add(order: List<Long>, id: Long): List<Long> = order.filter { it != id } + id

    fun remove(order: List<Long>, id: Long): List<Long> = order.filter { it != id }

    /** مجلد فيه تطبيق واحد أو أقل ينفك */
    fun shouldDissolve(count: Int): Boolean = count <= 1
}
