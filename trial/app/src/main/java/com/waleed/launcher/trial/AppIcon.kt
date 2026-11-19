package com.waleed.launcher.trial

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** يعرف إذا فيه أيقونة مضغوطة الحين (عشان الضغطة الطويلة على الفراغ ما تتلخبط معها) */
class PressTracker {
    var iconPressed = false
}

/** أيقونة قابلة للضغط: ضغطة تفتح، ضغطة طويلة تفتح معلومات التطبيق */
@Composable
fun AppIcon(
    app: AppEntry,
    repository: AppRepository,
    size: Dp,
    style: IconStyle,
    tracker: PressTracker? = null,
) {
    val haptics = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = if (pressed) {
            tween(durationMillis = 120)
        } else {
            spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)
        },
        label = "press",
    )

    Box(
        Modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(app.key) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tracker?.iconPressed = true
                        tryAwaitRelease()
                        pressed = false
                        tracker?.iconPressed = false
                    },
                    onTap = { repository.launch(app) },
                    onLongPress = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        repository.openAppInfo(app)
                    },
                )
            },
    ) {
        IconFace(app, repository, size, style)
    }
}

/** شكل الأيقونة حسب الستايل المختار */
@Composable
fun IconFace(app: AppEntry, repository: AppRepository, size: Dp, style: IconStyle) {
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val original: ImageBitmap? by produceState(repository.cachedIcon(app, sizePx), app.key, sizePx) {
        value = repository.icon(app, sizePx)
    }

    if (style == IconStyle.ORIGINAL) {
        original?.let { Image(it, contentDescription = app.label, modifier = Modifier.size(size)) }
        return
    }

    val glyph: ImageBitmap? by produceState(repository.cachedGlyph(app, sizePx), app.key, sizePx) {
        value = repository.glyph(app, sizePx)
    }
    val dark = style == IconStyle.GLASS_DARK

    GlassTile(size = size, dark = dark) {
        val g = glyph
        if (g != null) {
            // ظل خفيف تحت الرمز عشان يبان على الخلفيات الفاتحة
            Image(
                g,
                contentDescription = null,
                modifier = Modifier.matchParentSize().offset(y = 1.dp).alpha(0.28f),
                colorFilter = ColorFilter.tint(Color.Black),
            )
            Image(
                g,
                contentDescription = app.label,
                modifier = Modifier.matchParentSize(),
                colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.96f)),
            )
        } else {
            // التطبيق ما عنده رمز أحادي: نحط أيقونته الأصلية صغيرة داخل الزجاج
            original?.let {
                Image(
                    it,
                    contentDescription = app.label,
                    modifier = Modifier.size(size * 0.66f).align(Alignment.Center).alpha(0.94f),
                )
            }
        }
    }
}

/** مربع زجاج سائل (Liquid Glass): شفاف، لمعة فوق، وحافة مضيئة رفيعة */
@Composable
fun GlassTile(size: Dp, dark: Boolean, content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit) {
    val fill = if (dark) {
        Brush.verticalGradient(listOf(Color(0xB3232328), Color(0xD90E0E10)))
    } else {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.34f), Color.White.copy(alpha = 0.12f)))
    }
    val edge = Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = if (dark) 0.42f else 0.75f),
            Color.White.copy(alpha = 0.08f),
            Color.White.copy(alpha = if (dark) 0.22f else 0.40f),
        ),
    )
    Box(
        Modifier
            .size(size)
            .clip(SquircleShape)
            .background(if (dark) Color.Transparent else Color.Black.copy(alpha = 0.10f))
            .background(fill)
            .border(1.dp, edge, SquircleShape),
        contentAlignment = Alignment.Center,
    ) {
        // اللمعة العلوية
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = if (dark) 0.10f else 0.22f),
                        0.45f to Color.Transparent,
                    ),
                ),
        )
        content()
    }
}
