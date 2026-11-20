package com.waleed.launcher.trial.fold

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/*
 * انتقال الطي بالزجاج المضبب (Hinge-driven frosted-glass fold transition)
 *
 * - الحركة تمشي مع زاوية المفصل نفسها، مو مع مؤقت.
 * - الفتح: الخارجية تتضبب وتختفي، والداخلية تبدأ مضببة وغامقة وتتوضح لين الفتح الكامل.
 * - السكر: نفس الشي بالعكس (لأن كل شي محسوب من الزاوية).
 * - الحساس يشتغل بس والشاشة شغالة واللانشر ظاهر (من ON_START لين ON_STOP).
 */

/** لا شي يصير تحت هالزاوية بالشاشة الداخلية (تقريباً وقت ما تشتغل) */
private const val INNER_START = 25f
/** الشاشة الداخلية واضحة تماماً من هالزاوية وفوق */
private const val INNER_END = 172f
/** الخارجية تختفي بالكامل عند هالزاوية */
private const val COVER_END = 60f

private const val MAX_BLUR_DP = 26f
private const val MAX_DIM = 0.5f

/** زاوية المفصل الحالية (٠ مسكر، ١٨٠ مفتوح) أو null لو الجهاز ما فيه حساس مفصل */
@Composable
fun rememberHingeAngle(): State<Float>? {
    val context = LocalContext.current
    val manager = remember { context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager }
    val sensor = remember { if (Build.VERSION.SDK_INT >= 30) manager?.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE) else null }
    if (manager == null || sensor == null) return null

    val angle = remember { mutableFloatStateOf(180f) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, sensor) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                angle.floatValue = e.values.firstOrNull() ?: return
            }
            override fun onAccuracyChanged(s: Sensor?, accuracy: Int) = Unit
        }
        var registered = false
        fun start() {
            if (!registered) registered = manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
        fun stop() {
            if (registered) manager.unregisterListener(listener)
            registered = false
        }
        // الشاشة تطفى = اللانشر يوقف (ON_STOP) = نوقف الحساس
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> start()
                Lifecycle.Event.ON_STOP -> stop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) start()
        onDispose {
            lifecycle.removeObserver(observer)
            stop()
        }
    }
    return angle
}

private fun smooth(x: Float): Float {
    val t = x.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/**
 * كم "الزجاج" مضبب الحين: ٠ = واضح تماماً، ١ = مضبب كامل.
 * على الداخلية: يقل مع الفتح. على الخارجية: يزيد مع الفتح.
 */
private fun frost(angle: Float, inner: Boolean): Float =
    if (inner) 1f - smooth((angle - INNER_START) / (INNER_END - INNER_START))
    else smooth(angle / COVER_END)

/**
 * يلف الشاشة كلها: الخلفية والأيقونات والويدجت.
 * [background] الخلفية (تتحرك أكثر)، و[content] المحتوى (ثابت تقريباً). هذا اللي يعطي إحساس الطبقات.
 */
@Composable
fun FoldTransitionHost(
    inner: Boolean,
    showAngle: Boolean,
    background: @Composable BoxScope.() -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val hinge = rememberHingeAngle()
    val density = LocalDensity.current
    // تنعيم خفيف عشان الحركة تكون ٦٠ إطار بدون رجفة، ومع ذلك تلحق سرعة يدك
    val angle by animateFloatAsState(
        hinge?.value ?: 180f,
        spring(dampingRatio = 1f, stiffness = Spring.StiffnessMedium),
        label = "hinge",
    )
    val canBlur = Build.VERSION.SDK_INT >= 31

    // كل القراءات داخل graphicsLayer: تتحدث بالرسم بس، بدون إعادة بناء الشاشة
    fun Modifier.frosted(depth: Float): Modifier = graphicsLayer {
        val f = if (hinge == null) 0f else frost(angle, inner)
        val blurPx = with(density) { (MAX_BLUR_DP * f * depth).dp.toPx() }
        renderEffect = if (canBlur && blurPx > 0.5f) {
            android.graphics.RenderEffect
                .createBlurEffect(blurPx, blurPx, android.graphics.Shader.TileMode.CLAMP)
                .asComposeRenderEffect()
        } else {
            null
        }
        // الطبقات تتحرك بسرعات مختلفة (Parallax): الخلفية أكثر، والمحتوى أقل
        val s = 1f + f * 0.05f * depth
        scaleX = s
        scaleY = s
        // الخارجية تختفي وهي تتضبب
        alpha = if (!inner) 1f - 0.9f * f else 1f
    }

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().frosted(depth = 1.4f), content = background)
        Box(Modifier.fillMaxSize().frosted(depth = 0.6f), content = content)
        // طبقة الزجاج نفسه: عتامة تخف مع الفتح
        Box(
            Modifier.fillMaxSize().drawWithContent {
                val f = if (hinge == null) 0f else frost(angle, inner)
                if (f > 0.001f) drawRect(Color.Black.copy(alpha = MAX_DIM * f))
                if (f > 0.001f) drawRect(Color.White.copy(alpha = 0.05f * f))
            },
        )
        // رقم مؤقت لضبط الحركة على جوال وليد (نشيله بعد الضبط)
        if (showAngle && hinge != null) AngleBadge(hinge, inner)
    }
}

/** نقرأ الزاوية هنا بس، عشان باقي الشاشة ما تنعاد بناءها مع كل حركة */
@Composable
private fun BoxScope.AngleBadge(hinge: State<Float>, inner: Boolean) {
    BasicText(
        "∠ ${hinge.value.toInt()}° · ${if (inner) "داخلية" else "خارجية"}",
        style = TextStyle(color = Color.White, fontSize = 12.sp),
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(start = 12.dp, bottom = 120.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
