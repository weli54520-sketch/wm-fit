package com.wm.fit.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.wm.fit.core.Move
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/*
 * رسمة متحركة لكل تمرين: شخص من خطوط يتحرك بين وضعين (A و B).
 * سويناها بأنفسنا بالكود، بدون صور ولا ملفات من برا.
 *
 * الزوايا بالدرجات: 0 = فوق، 90 = قدام (يمين)، 180 = تحت، 270 = ورا (يسار).
 * الرسمة على مربع 100×100، والأرض عند 92.
 */

private enum class Anchor { FEET, HIP, HAND }

private enum class J { SHO, HIP, HAND, HAND2, WRIST }

/** وضع الجسم. side = من الجنب، front = من قدام (للرفرفة والتفتيح). */
private class Pose(
    val front: Boolean,
    val anchor: Anchor,
    val ax: Float, val ay: Float,
    val t: Float, val hd: Float,
    val th: Float, val sh: Float, val th2: Float, val sh2: Float,
    val ua: Float, val fa: Float, val ua2: Float, val fa2: Float,
    val ft: Float, val wr: Float,
)

private fun side(
    anchor: Anchor, ax: Float, ay: Float,
    t: Float = 0f, hd: Float = 0f, th: Float = 180f, sh: Float = 180f,
    th2: Float = th, sh2: Float = sh, ua: Float = 180f, fa: Float = 180f,
    ua2: Float = ua, fa2: Float = fa, ft: Float = 90f, wr: Float = fa,
) = Pose(false, anchor, ax, ay, t, hd, th, sh, th2, sh2, ua, fa, ua2, fa2, ft, wr)

private fun front(
    anchor: Anchor, ax: Float, ay: Float,
    th: Float = 8f, ua: Float = 10f, fa: Float = 10f,
) = Pose(true, anchor, ax, ay, 0f, 0f, th, 0f, th, 0f, ua, fa, ua, fa, 90f, fa)

private sealed class Prop
private class Plate(val j: J) : Prop()
private class Db(val j: J) : Prop()
private class Cable(val j: J, val x: Float, val y: Float) : Prop()
private class Line(val x1: Float, val y1: Float, val x2: Float, val y2: Float) : Prop()

private class Anim(val a: Pose, val b: Pose, val props: List<Prop>)

private val FEET = Anchor.FEET
private val HIP = Anchor.HIP
private val HAND = Anchor.HAND

private val anims: Map<Move, Anim> by lazy {
    mapOf(
        Move.SQUAT to Anim(
            side(FEET, 54f, 90f, ua = 215f, fa = 20f),
            side(FEET, 54f, 90f, t = 42f, th = 100f, sh = 208f, ua = 235f, fa = 40f),
            listOf(Plate(J.SHO)),
        ),
        Move.GOBLET to Anim(
            side(FEET, 54f, 90f, ua = 165f, fa = 15f),
            side(FEET, 54f, 90f, t = 28f, th = 100f, sh = 210f, ua = 175f, fa = 35f),
            listOf(Db(J.HAND)),
        ),
        Move.HINGE to Anim(
            side(FEET, 50f, 90f),
            side(FEET, 50f, 90f, t = 78f, th = 162f, sh = 190f, ua = 178f, fa = 178f),
            listOf(Plate(J.HAND)),
        ),
        Move.DEADLIFT to Anim(
            side(FEET, 50f, 90f),
            side(FEET, 50f, 90f, t = 62f, th = 120f, sh = 208f, ua = 175f, fa = 175f),
            listOf(Plate(J.HAND)),
        ),
        Move.LUNGE to Anim(
            side(FEET, 62f, 90f, th = 168f, sh = 185f, th2 = 200f, sh2 = 192f),
            side(FEET, 62f, 90f, t = 8f, th = 98f, sh = 192f, th2 = 182f, sh2 = 258f),
            listOf(Db(J.HAND)),
        ),
        Move.LEG_PRESS to Anim(
            side(HIP, 34f, 74f, t = 295f, th = 62f, sh = 62f, ua = 150f, fa = 150f, ft = 330f),
            side(HIP, 34f, 74f, t = 295f, th = 28f, sh = 130f, ua = 150f, fa = 150f, ft = 330f),
            listOf(Line(10f, 62f, 34f, 78f), Line(30f, 80f, 44f, 80f)),
        ),
        Move.LEG_EXT to Anim(
            side(HIP, 42f, 62f, t = 352f, th = 90f, sh = 180f, ua = 185f, fa = 150f),
            side(HIP, 42f, 62f, t = 352f, th = 90f, sh = 95f, ua = 185f, fa = 150f),
            listOf(Line(34f, 65f, 62f, 65f), Line(32f, 64f, 30f, 34f)),
        ),
        Move.LEG_CURL to Anim(
            side(HIP, 42f, 62f, t = 352f, th = 90f, sh = 100f, ua = 185f, fa = 150f),
            side(HIP, 42f, 62f, t = 352f, th = 90f, sh = 205f, ua = 185f, fa = 150f),
            listOf(Line(34f, 65f, 62f, 65f), Line(32f, 64f, 30f, 34f)),
        ),
        Move.CALF to Anim(
            side(FEET, 50f, 86f, ft = 90f),
            side(FEET, 52f, 80f, ft = 140f),
            listOf(Db(J.HAND), Line(50f, 87f, 64f, 87f)),
        ),
        Move.HIP_THRUST to Anim(
            side(FEET, 74f, 90f, t = 272f, hd = -10f, th = 90f, sh = 180f, ua = 165f, fa = 110f),
            side(FEET, 74f, 90f, t = 290f, hd = -10f, th = 52f, sh = 160f, ua = 165f, fa = 110f),
            listOf(Plate(J.HIP), Line(6f, 72f, 26f, 72f)),
        ),
        Move.BRIDGE to Anim(
            side(FEET, 70f, 90f, t = 260f, th = 90f, sh = 190f, ua = 245f, fa = 270f),
            side(FEET, 70f, 90f, t = 272f, th = 60f, sh = 165f, ua = 250f, fa = 270f),
            listOf(),
        ),
        Move.BENCH to Anim(
            side(HIP, 58f, 66f, t = 270f, th = 118f, sh = 195f, ua = 2f, fa = 2f),
            side(HIP, 58f, 66f, t = 270f, th = 118f, sh = 195f, ua = 150f, fa = 8f),
            listOf(Plate(J.HAND), Line(18f, 70f, 64f, 70f), Line(24f, 70f, 24f, 90f), Line(58f, 70f, 58f, 90f)),
        ),
        Move.INCLINE to Anim(
            side(HIP, 56f, 68f, t = 305f, th = 110f, sh = 190f, ua = -10f, fa = -10f),
            side(HIP, 56f, 68f, t = 305f, th = 110f, sh = 190f, ua = 165f, fa = -5f),
            listOf(Db(J.HAND), Line(36f, 50f, 58f, 71f), Line(52f, 71f, 52f, 90f)),
        ),
        Move.PUSHUP to Anim(
            side(HAND, 24f, 90f, t = 293f, th = 113f, sh = 113f, ua = 180f, fa = 180f, ft = 165f),
            side(HAND, 24f, 90f, t = 282f, th = 103f, sh = 103f, ua = 101f, fa = 219f, ft = 165f),
            listOf(),
        ),
        Move.FLY to Anim(
            front(FEET, 50f, 90f, th = 8f, ua = 95f, fa = 100f),
            front(FEET, 50f, 90f, th = 8f, ua = 55f, fa = 25f),
            listOf(Cable(J.HAND, 96f, 30f), Cable(J.HAND2, 4f, 30f)),
        ),
        Move.PULLDOWN to Anim(
            side(HIP, 50f, 66f, t = 345f, th = 88f, sh = 178f, ua = 8f, fa = 6f),
            side(HIP, 50f, 66f, t = 340f, th = 88f, sh = 178f, ua = -155f, fa = 15f),
            listOf(Cable(J.HAND, 52f, 2f), Plate(J.HAND), Line(40f, 69f, 64f, 69f)),
        ),
        Move.PULLUP to Anim(
            side(HAND, 52f, 6f, t = 0f, th = 185f, sh = 250f, ua = 0f, fa = 0f),
            side(HAND, 52f, 6f, t = 8f, th = 170f, sh = 240f, ua = 165f, fa = 8f),
            listOf(Line(30f, 6f, 74f, 6f)),
        ),
        Move.ROW_SEATED to Anim(
            side(HIP, 36f, 64f, t = 10f, th = 84f, sh = 108f, ua = 95f, fa = 92f, ft = 0f),
            side(HIP, 36f, 64f, t = 355f, th = 84f, sh = 108f, ua = 215f, fa = 82f, ft = 0f),
            listOf(Cable(J.HAND, 96f, 62f), Line(24f, 67f, 46f, 67f)),
        ),
        Move.ROW_BENT to Anim(
            side(FEET, 34f, 90f, t = 82f, th = 165f, sh = 185f, ua = 180f, fa = 180f, ua2 = 180f, fa2 = 180f),
            side(FEET, 34f, 90f, t = 82f, th = 165f, sh = 185f, ua = 300f, fa = 180f, ua2 = 180f, fa2 = 180f),
            listOf(Db(J.HAND), Line(52f, 66f, 86f, 66f), Line(80f, 66f, 80f, 92f)),
        ),
        Move.OHP to Anim(
            side(FEET, 50f, 90f, ua = 150f, fa = 10f),
            side(FEET, 50f, 90f, ua = 2f, fa = 2f),
            listOf(Db(J.HAND)),
        ),
        Move.RAISE to Anim(
            front(FEET, 50f, 90f, ua = 12f, fa = 14f),
            front(FEET, 50f, 90f, ua = 86f, fa = 90f),
            listOf(Db(J.HAND), Db(J.HAND2)),
        ),
        Move.FACE_PULL to Anim(
            side(FEET, 46f, 90f, t = 355f, ua = 80f, fa = 78f),
            side(FEET, 46f, 90f, t = 355f, ua = 250f, fa = 40f),
            listOf(Cable(J.HAND, 98f, 22f)),
        ),
        Move.REAR_FLY to Anim(
            side(FEET, 42f, 90f, t = 72f, th = 168f, sh = 188f, ua = 180f, fa = 178f),
            side(FEET, 42f, 90f, t = 72f, th = 168f, sh = 188f, ua = 262f, fa = 262f),
            listOf(Db(J.HAND)),
        ),
        Move.CURL to Anim(
            side(FEET, 50f, 90f, ua = 182f, fa = 178f),
            side(FEET, 50f, 90f, ua = 176f, fa = 25f),
            listOf(Db(J.HAND)),
        ),
        Move.PUSHDOWN to Anim(
            side(FEET, 46f, 90f, t = 8f, ua = 178f, fa = 95f),
            side(FEET, 46f, 90f, t = 8f, ua = 180f, fa = 178f),
            listOf(Cable(J.HAND, 62f, 4f)),
        ),
        Move.OH_EXT to Anim(
            side(FEET, 50f, 90f, ua = 4f, fa = 4f),
            side(FEET, 50f, 90f, ua = 6f, fa = -160f),
            listOf(Db(J.HAND)),
        ),
        Move.PLANK to Anim(
            side(HAND, 12f, 90f, t = 281f, th = 101f, sh = 101f, ua = 180f, fa = 270f, ft = 170f),
            side(HAND, 12f, 90f, t = 280f, th = 100f, sh = 102f, ua = 180f, fa = 270f, ft = 170f),
            listOf(),
        ),
        Move.CRUNCH to Anim(
            side(HIP, 50f, 69f, t = 10f, th = 180f, sh = 270f, ua = 35f, fa = 200f, ft = 180f),
            side(HIP, 50f, 69f, t = 100f, hd = 30f, th = 178f, sh = 270f, ua = 130f, fa = 290f, ft = 180f),
            listOf(Cable(J.HAND, 64f, 2f)),
        ),
        Move.KNEE_RAISE to Anim(
            side(HAND, 52f, 3f, th = 172f, sh = 218f, ua = 0f, fa = 0f),
            side(HAND, 52f, 3f, t = -5f, th = 88f, sh = 175f, ua = 0f, fa = 0f),
            listOf(Line(30f, 3f, 74f, 3f)),
        ),
        Move.CARRY to Anim(
            side(HIP, 50f, 50f, th = 160f, sh = 182f, th2 = 200f, sh2 = 205f),
            side(HIP, 50f, 50f, th = 200f, sh = 205f, th2 = 160f, sh2 = 182f),
            listOf(Db(J.HAND)),
        ),
        Move.WRIST_CURL to Anim(
            side(HIP, 40f, 62f, t = 28f, th = 90f, sh = 180f, ua = 150f, fa = 92f, wr = 150f),
            side(HIP, 40f, 62f, t = 28f, th = 90f, sh = 180f, ua = 150f, fa = 92f, wr = 40f),
            listOf(Db(J.WRIST), Line(30f, 65f, 52f, 65f)),
        ),
    )
}

// أطوال أجزاء الجسم
private const val TORSO = 26f
private const val NECK = 9f
private const val THIGH = 21f
private const val SHIN = 20f
private const val UARM = 14f
private const val FARM = 13f
private const val FOOT = 6f
private const val WRIST = 4f
private const val HEAD = 6f

private class P2(val x: Float, val y: Float) {
    operator fun plus(o: P2) = P2(x + o.x, y + o.y)
    operator fun minus(o: P2) = P2(x - o.x, y - o.y)
}

private fun vec(a: Float, l: Float): P2 {
    val r = a * PI.toFloat() / 180f
    return P2(sin(r) * l, -cos(r) * l)
}

private fun lerp(a: Float, b: Float, k: Float) = a + (b - a) * k

private fun mix(a: Pose, b: Pose, k: Float) = Pose(
    a.front, a.anchor,
    lerp(a.ax, b.ax, k), lerp(a.ay, b.ay, k),
    lerp(a.t, b.t, k), lerp(a.hd, b.hd, k),
    lerp(a.th, b.th, k), lerp(a.sh, b.sh, k), lerp(a.th2, b.th2, k), lerp(a.sh2, b.sh2, k),
    lerp(a.ua, b.ua, k), lerp(a.fa, b.fa, k), lerp(a.ua2, b.ua2, k), lerp(a.fa2, b.fa2, k),
    lerp(a.ft, b.ft, k), lerp(a.wr, b.wr, k),
)

/** المفاصل والخطوط اللي تنرسم. */
private class Body(val joints: Map<J, P2>, val head: P2, val segs: List<Pair<P2, P2>>)

private fun sideBody(p: Pose): Body {
    val hip: P2
    val sho: P2
    when (p.anchor) {
        Anchor.FEET -> {
            val ankle = P2(p.ax, p.ay)
            val knee = ankle - vec(p.sh, SHIN)
            hip = knee - vec(p.th, THIGH)
            sho = hip + vec(p.t, TORSO)
        }
        Anchor.HIP -> {
            hip = P2(p.ax, p.ay)
            sho = hip + vec(p.t, TORSO)
        }
        Anchor.HAND -> {
            val hand = P2(p.ax, p.ay)
            val elb = hand - vec(p.fa, FARM)
            sho = elb - vec(p.ua, UARM)
            hip = sho - vec(p.t, TORSO)
        }
    }
    val knee = hip + vec(p.th, THIGH)
    val ankle = knee + vec(p.sh, SHIN)
    val toe = ankle + vec(p.ft, FOOT)
    val knee2 = hip + vec(p.th2, THIGH)
    val ankle2 = knee2 + vec(p.sh2, SHIN)
    val toe2 = ankle2 + vec(p.ft, FOOT)
    val elb = sho + vec(p.ua, UARM)
    val hand = elb + vec(p.fa, FARM)
    val elb2 = sho + vec(p.ua2, UARM)
    val hand2 = elb2 + vec(p.fa2, FARM)
    val wrist = hand + vec(p.wr, WRIST)
    val head = sho + vec(p.t + p.hd, NECK)
    return Body(
        mapOf(J.SHO to sho, J.HIP to hip, J.HAND to hand, J.HAND2 to hand2, J.WRIST to wrist),
        head,
        listOf(
            // الرجل واليد البعيدة أول، عشان القريبة تطلع فوقها
            hip to knee2, knee2 to ankle2, ankle2 to toe2, sho to elb2, elb2 to hand2,
            sho to hip, hip to knee, knee to ankle, ankle to toe, sho to elb, elb to hand, hand to wrist,
        ),
    )
}

private fun frontBody(p: Pose): Body {
    val joints = mutableMapOf<J, P2>()
    val segs = mutableListOf<Pair<P2, P2>>()
    var sho1 = P2(0f, 0f)
    var hip1 = P2(0f, 0f)
    for (s in listOf(1f, -1f)) {
        val r = p.th * PI.toFloat() / 180f
        val ankle = P2(p.ax + s * (5f + sin(r) * 41f), p.ay)
        val hip = P2(p.ax + s * 5f, p.ay - cos(r) * 41f)
        val knee = P2((ankle.x + hip.x) / 2f, (ankle.y + hip.y) / 2f)
        val sho = P2(p.ax + s * 9f, hip.y - TORSO)
        val ru = p.ua * PI.toFloat() / 180f
        val rf = p.fa * PI.toFloat() / 180f
        val elb = P2(sho.x + s * sin(ru) * UARM, sho.y + cos(ru) * UARM)
        val hand = P2(elb.x + s * sin(rf) * FARM, elb.y + cos(rf) * FARM)
        val toe = P2(ankle.x + s * 4f, ankle.y)
        segs += listOf(hip to knee, knee to ankle, ankle to toe, sho to elb, elb to hand)
        if (s > 0) {
            joints[J.SHO] = sho; joints[J.HIP] = hip; joints[J.HAND] = hand; joints[J.WRIST] = hand
            sho1 = sho; hip1 = hip
        } else {
            joints[J.HAND2] = hand
            segs += listOf(sho1 to sho, hip1 to hip)
            segs += P2(p.ax, sho.y) to P2(p.ax, hip.y)
        }
    }
    val head = P2(p.ax, joints.getValue(J.SHO).y - NECK)
    return Body(joints, head, segs)
}

/**
 * الرسمة المتحركة للتمرين. تتحرك ذهاب وإياب كل ثانيتين ونص تقريباً.
 * [play] = false: صورة ثابتة (للقوائم، عشان ما تتحرك عشرات الرسمات مع بعض).
 */
@Composable
fun ExerciseAnim(move: Move, modifier: Modifier = Modifier, color: Color = C.accent, play: Boolean = true) {
    val anim = anims[move] ?: return
    val k = if (play) {
        val tr = rememberInfiniteTransition(label = "ex")
        val phase by tr.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
            label = "phase",
        )
        // سهل-سريع-سهل: يوقف شوي في كل طرف
        (1f - cos(phase * 2f * PI.toFloat())) / 2f
    } else 1f
    val propColor = C.muted
    val lineColor = C.track
    val plateColor = C.soft

    Canvas(modifier) {
        val s = min(size.width, size.height) / 100f
        val ox = (size.width - 100f * s) / 2f
        val oy = (size.height - 100f * s) / 2f
        fun o(p: P2) = Offset(ox + p.x * s, oy + p.y * s)
        fun o(x: Float, y: Float) = Offset(ox + x * s, oy + y * s)

        val pose = mix(anim.a, anim.b, k)
        val body = if (pose.front) frontBody(pose) else sideBody(pose)

        // الأرض
        drawLine(lineColor, o(4f, 92f), o(96f, 92f), strokeWidth = 1.2f * s)
        // الأجهزة الثابتة (بنش، كرسي، بار العقلة)
        anim.props.forEach {
            if (it is Line) drawLine(propColor, o(it.x1, it.y1), o(it.x2, it.y2), 2.2f * s, StrokeCap.Round)
        }
        anim.props.forEach {
            if (it is Cable) drawLine(propColor, o(body.joints.getValue(it.j)), o(it.x, it.y), 0.7f * s)
        }
        // الجسم
        body.segs.forEach { (a, b) -> drawLine(color, o(a), o(b), 3.2f * s, StrokeCap.Round) }
        drawCircle(color, HEAD * s, o(body.head))
        // الأوزان
        anim.props.forEach {
            when (it) {
                is Plate -> drawCircle(plateColor, 7f * s, o(body.joints.getValue(it.j)), style = Stroke(1.6f * s))
                is Db -> {
                    val c = o(body.joints.getValue(it.j))
                    drawRoundRect(
                        plateColor,
                        topLeft = Offset(c.x - 4f * s, c.y - 2f * s),
                        size = Size(8f * s, 4f * s),
                        cornerRadius = CornerRadius(1f * s),
                    )
                }
                else -> Unit
            }
        }
    }
}
