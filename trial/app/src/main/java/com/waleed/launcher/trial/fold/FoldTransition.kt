package com.waleed.launcher.trial.fold

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.compose.animation.core.Spring
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
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

/** على جوال وليد الشاشتين يتبدلون عند ٩٠ تقريباً (من تجربته) */
private const val SWITCH = 90f
/** الشاشة الداخلية واضحة تماماً من هالزاوية وفوق */
private const val INNER_END = 172f

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
    if (inner) 1f - smooth((angle - SWITCH) / (INNER_END - SWITCH))
    else smooth(angle / SWITCH)

/**
 * يلف الشاشة كلها: الخلفية والأيقونات والويدجت.
 *
 * عشان تكون ٦٠ إطار بدون تعليق:
 * - ما نضبب الشاشة كل إطار. أول ما تبدأ الحركة نصور الشاشة صورة صغيرة ونضببها مرة وحدة.
 * - الحركة كلها بعدين = شفافية وتكبير لهالصورة فوق الشاشة الحقيقية (الزجاج يرتفع عن الصورة).
 * - المحتوى نفسه ثابت بمكانه، والخلفية تتحرك شوي (طبقات بسرعات مختلفة).
 * - الزاوية تنقرأ وقت الرسم بس، فالشاشة ما تنعاد بناءها مع كل حركة.
 */
@Composable
fun FoldTransitionHost(
    inner: Boolean,
    showAngle: Boolean,
    background: @Composable BoxScope.() -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val hinge = rememberHingeAngle()
    val angle = remember { Animatable(hinge?.value ?: 180f) }
    // تنعيم خفيف يلحق يدك: كل قراءة جديدة تلغي اللي قبلها وتكمل من نفس السرعة
    LaunchedEffect(hinge) {
        val h = hinge ?: return@LaunchedEffect
        snapshotFlow { h.value }.collectLatest { target ->
            angle.animateTo(target, spring(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow))
        }
    }
    fun f(): Float = if (hinge == null) 0f else frost(angle.value, inner)

    // نصور الشاشة بس وقت الحركة. وقت السكون ما فيه أي شغل زايد
    val moving by remember(inner) { derivedStateOf { f() > 0.01f } }
    val layer = rememberGraphicsLayer()
    var frostImage by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(moving, inner) {
        frostImage = null
        if (!moving) return@LaunchedEffect
        // ننتظر إطارين عشان الشاشة الجديدة (بعد التبديل) تنرسم أول
        withFrameNanos { }
        withFrameNanos { }
        val snap = runCatching { layer.toImageBitmap() }.getOrNull() ?: return@LaunchedEffect
        frostImage = withContext(Dispatchers.Default) { runCatching { makeFrost(snap) }.getOrNull() }
    }

    Box(Modifier.fillMaxSize()) {
        // الشاشة الحقيقية، ونسجلها عشان نقدر نصورها
        Box(
            Modifier.fillMaxSize().drawWithContent {
                layer.record { this@drawWithContent.drawContent() }
                drawLayer(layer)
            },
        ) {
            // الخلفية تتحرك أكثر شوي من المحتوى (Parallax)
            Box(
                Modifier.fillMaxSize().graphicsLayer {
                    val s = 1f + 0.05f * f()
                    scaleX = s
                    scaleY = s
                },
                content = background,
            )
            Box(Modifier.fillMaxSize(), content = content)
        }

        // الزجاج المضبب نفسه: يرتفع (يكبر شوي) ويخف مع الفتح
        Box(
            Modifier.fillMaxSize().drawBehind {
                val v = f()
                if (v <= 0.002f) return@drawBehind
                val img = frostImage
                if (img != null) {
                    scale(1f + 0.035f * v) {
                        drawImage(
                            img,
                            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                            alpha = (v * 1.3f).coerceAtMost(1f),
                            filterQuality = FilterQuality.Low,
                        )
                    }
                    drawRect(Color.Black.copy(alpha = 0.42f * v))
                } else {
                    // لين تجهز الصورة (إطار أو إطارين): غمقة بس
                    drawRect(Color.Black.copy(alpha = 0.8f * v))
                }
                drawRect(Color.White.copy(alpha = 0.04f * v))
                // الخارجية تختفي وهي تتضبب
                if (!inner) drawRect(Color.Black.copy(alpha = 0.45f * v))
            },
        )

        // رقم مؤقت لضبط الحركة على جوال وليد (نشيله بعد الضبط)
        if (showAngle && hinge != null) AngleBadge(hinge, inner)
    }
}

/** نصغر الصورة كثير ونضببها: رخيصة جداً، وتكبيرها بعدين يعطي نعومة الزجاج */
private fun makeFrost(snap: ImageBitmap): ImageBitmap {
    val src = snap.asAndroidBitmap().let { if (it.config == Bitmap.Config.HARDWARE) it.copy(Bitmap.Config.ARGB_8888, false) else it }
    val w = (src.width / 14).coerceAtLeast(8)
    val h = (src.height / 14).coerceAtLeast(8)
    var small = Bitmap.createScaledBitmap(src, w, h, true)
    repeat(3) { small = boxBlur(small, 2) }
    return small.asImageBitmap()
}

private fun boxBlur(src: Bitmap, r: Int): Bitmap {
    val w = src.width
    val h = src.height
    val a = IntArray(w * h)
    src.getPixels(a, 0, w, 0, 0, w, h)
    val b = IntArray(w * h)
    fun pass(input: IntArray, output: IntArray, horizontal: Boolean) {
        val len = if (horizontal) w else h
        val lines = if (horizontal) h else w
        for (line in 0 until lines) for (i in 0 until len) {
            var aa = 0; var rr = 0; var gg = 0; var bb = 0; var n = 0
            for (k in -r..r) {
                val j = (i + k).coerceIn(0, len - 1)
                val c = if (horizontal) input[line * w + j] else input[j * w + line]
                aa += c ushr 24 and 0xFF; rr += c shr 16 and 0xFF; gg += c shr 8 and 0xFF; bb += c and 0xFF; n++
            }
            output[if (horizontal) line * w + i else i * w + line] = (aa / n shl 24) or (rr / n shl 16) or (gg / n shl 8) or (bb / n)
        }
    }
    pass(a, b, true)
    pass(b, a, false)
    return Bitmap.createBitmap(a, w, h, Bitmap.Config.ARGB_8888)
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
