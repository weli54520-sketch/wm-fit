package com.waleed.onyx.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.waleed.onyx.apps.AppCatalog
import com.waleed.onyx.apps.AppInfo
import com.waleed.onyx.ui.Squircle

/**
 * الأيقونة: الأصلية مثل ما يعطيها الجوال بالضبط (بدون خلفية أو قص)،
 * أو الرمز أبيض على زجاج شفاف، أو على زجاج داكن.
 */
@Composable
fun AppIcon(app: AppInfo?, catalog: AppCatalog, size: Dp, style: Int, modifier: Modifier = Modifier) {
    val px = with(LocalDensity.current) { size.roundToPx() }.coerceAtLeast(1)
    val glyphMode = style != 0
    val bitmap by produceState(app?.let { catalog.cachedIcon(it, px, false) }, app?.key, px) {
        if (app != null) value = catalog.icon(app, px)
    }
    val glyph by produceState(app?.let { if (glyphMode) catalog.cachedIcon(it, px, true) else null }, app?.key, px, glyphMode) {
        if (app != null && glyphMode) value = catalog.glyph(app, px)
    }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        if (style == 0) {
            bitmap?.let { Image(it, contentDescription = app?.label, modifier = Modifier.size(size)) }
        } else {
            GlassTile(size, dark = style == 2) {
                val g = glyph
                if (g != null) {
                    // ظل خفيف تحت الرمز عشان يبان على أي خلفية
                    Image(g, null, Modifier.size(size).offset(y = 1.dp).alpha(0.30f), colorFilter = ColorFilter.tint(Color.Black))
                    Image(g, app?.label, Modifier.size(size), alpha = if (style == 2) 0.9f else 0.97f)
                } else {
                    bitmap?.let { Image(it, app?.label, Modifier.size(size * 0.64f), alpha = 0.95f) }
                }
            }
        }
    }
}

/**
 * مربع الزجاج للأيقونة.
 * الشفاف: زجاج فاتح ضبابي، أساس غامق خفيف عشان يبان على الخلفيات الفاتحة، حافة مضيئة، ولمعة ناعمة جداً.
 * الداكن: زجاج مدخّن بدون لمعة، وحافة رفيعة.
 */
@Composable
fun GlassTile(size: Dp, dark: Boolean, content: @Composable BoxScope.() -> Unit) {
    val fill = if (dark) Brush.verticalGradient(listOf(Color(0xB8242429), Color(0xDE0D0D0F)))
    else Brush.verticalGradient(listOf(Color(0x5CFFFFFF), Color(0x24FFFFFF), Color(0x33FFFFFF)))
    val edge = if (dark) Brush.linearGradient(listOf(Color(0x6BFFFFFF), Color(0x14FFFFFF), Color(0x38FFFFFF)))
    else Brush.linearGradient(listOf(Color(0xD9FFFFFF), Color(0x1FFFFFFF), Color(0x80FFFFFF)))
    Box(
        Modifier
            .size(size)
            .clip(Squircle)
            .then(if (dark) Modifier else Modifier.background(Color(0x1F000000)))
            .background(fill)
            .border(if (dark) 1.dp else 1.2.dp, edge, Squircle),
        contentAlignment = Alignment.Center,
    ) {
        if (!dark) {
            // لمعة ناعمة من فوق تعطي إحساس الزجاج
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(0f to Color(0x2EFFFFFF), 0.42f to Color.Transparent)))
        }
        content()
    }
}

@Composable
fun AppLabel(text: String, width: Dp) {
    BasicText(
        text,
        modifier = Modifier.graphicsLayer { },
        style = TextStyle(
            color = Color.White,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            shadow = Shadow(Color(0x99000000), Offset(0f, 1f), 6f),
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** أيقونة مع الاسم (لو شغال من الإعدادات) */
@Composable
fun AppFace(app: AppInfo?, catalog: AppCatalog, icon: Dp, style: Int, labels: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        AppIcon(app, catalog, icon, style)
        if (labels && app != null) {
            Spacer(Modifier.height(5.dp))
            Box(Modifier.size(width = icon + 18.dp, height = 15.dp), contentAlignment = Alignment.Center) { AppLabel(app.label, icon + 18.dp) }
        }
    }
}

/** يحفظ مكان العنصر على الشاشة (للقائمة والسحب وفتح التطبيق) */
fun Modifier.trackBounds(onBounds: (androidx.compose.ui.geometry.Rect, LayoutCoordinates) -> Unit): Modifier =
    onGloballyPositioned { onBounds(it.boundsInRoot(), it) }

@Composable
fun rememberCoordsHolder(): Array<LayoutCoordinates?> = remember { arrayOfNulls(1) }
