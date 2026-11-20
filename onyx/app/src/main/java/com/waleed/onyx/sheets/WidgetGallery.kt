package com.waleed.onyx.sheets

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import com.waleed.onyx.grid.WidgetSpan
import com.waleed.onyx.home.HomeState
import com.waleed.onyx.home.Pill
import com.waleed.onyx.ui.Onyx
import com.waleed.onyx.ui.smokeGlass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale

/** ويدجت تطبيق واحد بالمعرض */
private class WidgetOption(val info: AppWidgetProviderInfo, val label: String, val w: Int, val h: Int)

/** تطبيق وكل الويدجت حقته */
private class WidgetGroup(val pkg: String, val appLabel: String, val icon: ImageBitmap?, val options: List<WidgetOption>)

/** التطبيقات اللي يستخدمها وليد تطلع أول */
private val PREFERRED = listOf(
    "com.samsung.android.calendar",
    "com.sec.android.daemonapp", // طقس سامسونج
    "com.samsung.android.weather",
    "com.spotify.music",
)

/** معرض الويدجت: كل التطبيقات اللي عندها ويدجت، وضغطة تضيفه للصفحة الحالية */
@Composable
fun WidgetGallery(state: HomeState, inner: Boolean) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var query by remember { mutableStateOf("") }
    LaunchedEffect(state.galleryOpen) { if (!state.galleryOpen) query = "" }
    val geo = state.geo
    val groups by produceState<List<WidgetGroup>?>(null, state.galleryOpen, geo.cols, geo.rows) {
        if (state.galleryOpen) value = withContext(Dispatchers.IO) { loadGroups(context, state, geo.cellW, geo.cellH, geo.cols, geo.rows) }
    }
    val cellW = with(density) { geo.cellW.toDp() }
    val cellH = with(density) { geo.cellH.toDp() }

    AnimatedVisibility(
        visible = state.galleryOpen,
        enter = fadeIn(tween(220)) + slideInVertically(tween(380)) { it / 3 },
        exit = fadeOut(tween(200)) + slideOutVertically(tween(300)) { it / 3 },
    ) {
        val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0xE6101014), Color(0xF2060608))))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .padding(top = top + 10.dp),
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    BasicText("الويدجت", style = TextStyle(color = Onyx.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold))
                    Spacer(Modifier.weight(1f))
                    Pill("إغلاق") { state.galleryOpen = false }
                }
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier
                        .padding(horizontal = 18.dp)
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x1AFFFFFF))
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (query.isEmpty()) BasicText("ابحث عن تطبيق أو ويدجت", style = TextStyle(color = Onyx.Muted, fontSize = 15.sp))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Onyx.Text, fontSize = 15.sp),
                        cursorBrush = SolidColor(Onyx.Text),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                val all = groups
                if (all == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        BasicText("نجيب الويدجت…", style = TextStyle(color = Onyx.Muted, fontSize = 14.sp))
                    }
                    return@Column
                }
                val q = query.trim()
                val list = if (q.isEmpty()) all else all.mapNotNull { g ->
                    if (g.appLabel.contains(q, ignoreCase = true)) g
                    else g.options.filter { it.label.contains(q, ignoreCase = true) }.takeIf { it.isNotEmpty() }?.let { WidgetGroup(g.pkg, g.appLabel, g.icon, it) }
                }
                if (list.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        BasicText("ما لقينا ويدجت", style = TextStyle(color = Onyx.Muted, fontSize = 14.sp))
                    }
                    return@Column
                }
                LazyColumn(
                    contentPadding = PaddingValues(top = 14.dp, bottom = bottom + 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(list, key = { it.pkg }) { g ->
                        Column {
                            Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                                g.icon?.let { Image(it, null, Modifier.size(26.dp)) }
                                Spacer(Modifier.width(10.dp))
                                BasicText(g.appLabel, style = TextStyle(color = Onyx.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold))
                                Spacer(Modifier.width(8.dp))
                                BasicText("${g.options.size}", style = TextStyle(color = Onyx.Muted, fontSize = 13.sp))
                            }
                            Spacer(Modifier.height(10.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 18.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(g.options, key = { it.info.provider.flattenToShortString() + it.info.profile.hashCode() }) { o ->
                                    WidgetCard(state, o, cellW, cellH, if (inner) 170.dp else 140.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetCard(state: HomeState, o: WidgetOption, cellW: Dp, cellH: Dp, previewH: Dp) {
    val context = LocalContext.current
    val natW = cellW * o.w
    val natH = cellH * o.h
    val cardW = (previewH * (natW / natH)).coerceIn(110.dp, 300.dp)
    val preview by produceState<ImageBitmap?>(null, o.info.provider) {
        value = withContext(Dispatchers.IO) { previewImage(context, o.info) }
    }
    Column(Modifier.width(cardW)) {
        Box(
            Modifier
                .size(cardW, previewH)
                .smokeGlass(RoundedCornerShape(22.dp))
                .clip(RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center,
        ) {
            val img = preview
            when {
                img != null -> Image(img, o.label, Modifier.fillMaxSize().padding(10.dp))
                o.info.previewLayout != 0 -> LayoutPreview(o.info, natW, natH, cardW - 12.dp, previewH - 12.dp)
                else -> {
                    val icon by produceState<ImageBitmap?>(null, o.info.provider) {
                        value = withContext(Dispatchers.IO) { o.info.loadIcon(context, 0)?.let { safeBitmap(it, 160) } }
                    }
                    icon?.let { Image(it, o.label, Modifier.size(56.dp)) }
                }
            }
            // طبقة فوق المعاينة: الضغطة لنا، والمعاينة ما تستقبل لمس
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable { state.addWidget(o.info) },
            )
        }
        Spacer(Modifier.height(6.dp))
        BasicText(
            o.label,
            style = TextStyle(color = Onyx.Text, fontSize = 13.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        BasicText("${o.w}×${o.h}", style = TextStyle(color = Onyx.Muted, fontSize = 12.sp))
    }
}

/** بعض التطبيقات (مثل سامسونج) ما تعطي صورة، تعطي شكل الويدجت نفسه. نرسمه ونصغره */
@Composable
private fun LayoutPreview(info: AppWidgetProviderInfo, natW: Dp, natH: Dp, maxW: Dp, maxH: Dp) {
    val scale = minOf(maxW / natW, maxH / natH, 1f)
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { ctx ->
                AppWidgetHostView(ctx).apply {
                    try {
                        val i = info.clone()
                        i.initialLayout = info.previewLayout
                        setAppWidget(-1, i)
                        updateAppWidget(null)
                    } catch (_: Exception) {}
                }
            },
            modifier = Modifier
                .requiredSize(natW, natH)
                .graphicsLayer { scaleX = scale; scaleY = scale },
        )
    }
}

private fun safeBitmap(d: Drawable, max: Int): ImageBitmap? = try {
    val w = d.intrinsicWidth.takeIf { it > 0 } ?: max
    val h = d.intrinsicHeight.takeIf { it > 0 } ?: max
    val k = minOf(1f, max.toFloat() / maxOf(w, h))
    d.toBitmap((w * k).toInt().coerceAtLeast(1), (h * k).toInt().coerceAtLeast(1)).asImageBitmap()
} catch (_: Exception) {
    null
}

private fun previewImage(context: Context, info: AppWidgetProviderInfo): ImageBitmap? {
    if (info.previewImage == 0) return null
    val d = try { info.loadPreviewImage(context, 0) } catch (_: Exception) { null } ?: return null
    return safeBitmap(d, 640)
}

private fun loadGroups(context: Context, state: HomeState, cellW: Float, cellH: Float, cols: Int, rows: Int): List<WidgetGroup> {
    val pm = context.packageManager
    val collator = Collator.getInstance(Locale("ar"))
    return state.app.widgets.providers()
        .groupBy { it.provider.packageName }
        .map { (pkg, infos) ->
            val appLabel = try { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() } catch (_: Exception) { infos.first().loadLabel(pm) ?: pkg }
            val icon = try { safeBitmap(pm.getApplicationIcon(pkg), 96) } catch (_: Exception) { null }
            val options = infos.map { i ->
                val (w, h) = WidgetSpan.defaultSpan(i.targetCellWidth, i.targetCellHeight, i.minWidth, i.minHeight, cellW, cellH, cols, rows)
                WidgetOption(i, i.loadLabel(pm) ?: appLabel, w, h)
            }.sortedWith(compareBy<WidgetOption> { it.w * it.h }.thenBy { it.label })
            WidgetGroup(pkg, appLabel, icon, options)
        }
        .sortedWith(
            compareBy<WidgetGroup> { g -> PREFERRED.indexOf(g.pkg).let { if (it < 0) Int.MAX_VALUE else it } }
                .thenComparator { a, b -> collator.compare(a.appLabel, b.appLabel) },
        )
}
