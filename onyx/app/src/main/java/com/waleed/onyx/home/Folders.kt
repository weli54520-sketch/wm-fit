package com.waleed.onyx.home

import android.os.Bundle
import android.util.SizeF
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.waleed.onyx.data.Item
import com.waleed.onyx.data.Kind
import com.waleed.onyx.grid.FolderMath
import com.waleed.onyx.system.Notifs
import com.waleed.onyx.ui.Onyx
import com.waleed.onyx.ui.smokeGlass
import kotlin.math.roundToInt

/**
 * زجاج سائل (مثل مجلدات الآيفون بصورة وليد): زجاج فاتح سميك، حافة لامعة،
 * انعكاس من فوق، وظل داخلي من تحت يعطيه سماكة. نرسمه بدون تضبيب عشان البطارية.
 */
fun DrawScope.liquidGlass(radius: Float, strong: Boolean = false) {
    val r = CornerRadius(radius)
    val w = size.width
    val h = size.height
    // جسم الزجاج
    drawRoundRect(
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = if (strong) 0.24f else 0.22f), Color.White.copy(alpha = if (strong) 0.12f else 0.10f), Color.White.copy(alpha = 0.16f)),
        ),
        cornerRadius = r,
    )
    // ظل داخلي من تحت (سماكة)
    drawRoundRect(
        Brush.verticalGradient(0.6f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.18f)),
        cornerRadius = r,
    )
    // انعكاس من فوق يسار
    drawRoundRect(
        Brush.radialGradient(
            listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
            center = Offset(w * 0.25f, h * 0.08f),
            radius = maxOf(w, h) * 0.65f,
        ),
        cornerRadius = r,
    )
    // الحافة الداخلية: لمعة من فوق تخف لتحت
    val inset = minOf(w, h) * 0.035f
    drawRoundRect(
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.22f))),
        topLeft = Offset(inset, inset),
        size = Size(w - inset * 2, h - inset * 2),
        cornerRadius = CornerRadius((radius - inset).coerceAtLeast(0f)),
        style = Stroke(minOf(w, h) * 0.022f),
    )
    // الحافة الخارجية
    drawRoundRect(
        Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.55f)),
            start = Offset(0f, 0f),
            end = Offset(w, h),
        ),
        cornerRadius = r,
        style = Stroke(minOf(w, h) * 0.018f + 0.6f),
    )
}

/** نقطة الإشعارات: نقطة صغيرة فخمة بدل الرقم الكبير */
@Composable
fun NotifDot(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFFFF453A))
            .drawBehind { drawCircle(Color.White.copy(alpha = 0.9f), radius = this.size.minDimension / 2f, style = Stroke(1.2.dp.toPx())) },
    )
}

/** أي عنصر بحجم أيقونة: تطبيق (مع نقطة الإشعار) أو مجلد */
@Composable
fun ItemIcon(state: HomeState, item: Item, size: Dp) {
    val notifs by Notifs.byPkg.collectAsState()
    if (item.kind == Kind.FOLDER) {
        val kids = state.folderChildren(item.id)
        val fd = state.folderData(item)
        val hasNotif = state.settings.dots && kids.any { k -> state.catalog.byKey(k.appKey)?.pkg?.let { notifs.containsKey(it) } == true }
        Box(Modifier.size(size)) {
            if (fd.cover && kids.isNotEmpty()) AppIcon(state.catalog.byKey(kids.first().appKey), state.catalog, size, state.settings.iconStyle)
            else FolderIcon(state, kids, size)
            if (hasNotif) NotifDot(size * 0.2f, Modifier.align(Alignment.TopEnd).offset(x = size * 0.04f, y = -size * 0.04f))
        }
    } else {
        val a = state.catalog.byKey(item.appKey)
        Box(Modifier.size(size)) {
            AppIcon(a, state.catalog, size, state.settings.iconStyle)
            if (state.settings.dots && a != null && notifs.containsKey(a.pkg)) {
                NotifDot(size * 0.2f, Modifier.align(Alignment.TopEnd).offset(x = size * 0.04f, y = -size * 0.04f))
            }
        }
    }
}

/** أيقونة المجلد: زجاج سائل وفيه ٩ تطبيقات صغار (٣×٣) */
@Composable
fun FolderIcon(state: HomeState, kids: List<Item>, size: Dp) {
    val mini = size * 0.215f
    val gap = size * 0.05f
    Box(
        Modifier
            .size(size)
            .graphicsLayer {
                shadowElevation = 6.dp.toPx()
                shape = RoundedCornerShape(percent = 27)
                clip = false
            }
            .drawBehind { liquidGlass(this.size.minDimension * 0.27f) },
        contentAlignment = Alignment.Center,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            val preview = FolderMath.preview(kids)
            for (row in 0 until 3) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    for (col in 0 until 3) {
                        val k = preview.getOrNull(row * 3 + col)
                        Box(Modifier.size(mini)) {
                            if (k != null) AppIcon(state.catalog.byKey(k.appKey), state.catalog, mini, state.settings.iconStyle)
                        }
                    }
                }
            }
        }
    }
}

/**
 * المجلد المفتوح: يكبر من مكانه لمربع زجاج سائل بالنص، والخلفية تتضبب (أو تغمق من الإعدادات).
 * ضغطة على تطبيق تفتحه، ضغطة طويلة تطلعه، والاسم ينكتب بضغطة عليه.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderOverlay(state: HomeState) {
    val f = state.openFolder
    val p by animateFloatAsState(if (f != null) 1f else 0f, spring(dampingRatio = 0.78f, stiffness = 380f), label = "folder")
    val last = remember { arrayOfNulls<Item>(1) }
    if (f != null) last[0] = f
    val folder = last[0] ?: return
    if (p < 0.01f && f == null) return
    val anchor = remember(folder.id) { state.itemRect(folder, state.slotOf(folder)) }
    val kids = state.folderChildren(folder.id)
    var name by remember(folder.id) { mutableStateOf(state.folderData(folder).name) }
    var editing by remember(folder.id) { mutableStateOf(state.folderRename) }
    var held by remember(folder.id) { mutableStateOf<Item?>(null) }
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val W = constraints.maxWidth.toFloat()
        val H = constraints.maxHeight.toFloat()
        val dark = state.settings.folderBg == 1
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = p.coerceIn(0f, 1f) }
                .background(Color.Black.copy(alpha = if (dark) 0.62f else 0.22f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    if (editing) { state.renameFolder(folder, name); editing = false } else state.closeFolder()
                },
        )
        val inner = maxWidth > 600.dp
        val panelW = minOf(maxWidth - 48.dp, if (inner) 420.dp else 340.dp)
        val icon = (panelW - 48.dp) / 3 * 0.66f
        val cellH = icon + 30.dp
        val panelH = cellH * 3 + 64.dp
        val pw = with(density) { panelW.toPx() }
        val ph = with(density) { panelH.toPx() }
        val left = (W - pw) / 2
        val top = (H - ph) / 2
        // يكبر من مكان أيقونة المجلد
        val ax = anchor?.center?.x ?: (W / 2)
        val ay = anchor?.center?.y ?: (H / 2)
        val s = 0.2f + 0.8f * p
        val tx = (ax - (left + pw / 2)) * (1 - p)
        val ty = (ay - (top + ph / 2)) * (1 - p)
        Column(
            Modifier
                .offset { IntOffset(left.roundToInt(), (top - with(density) { 64.dp.toPx() }).roundToInt()) }
                .width(panelW)
                .graphicsLayer { translationX = tx; translationY = ty; scaleX = s; scaleY = s; alpha = p.coerceIn(0f, 1f) },
        ) {
            // الاسم
            Box(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 6.dp), contentAlignment = Alignment.CenterStart) {
                if (editing) {
                    val fr = remember { FocusRequester() }
                    LaunchedEffect(Unit) { fr.requestFocus() }
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it.take(24) },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Right),
                        cursorBrush = SolidColor(Color.White),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { state.renameFolder(folder, name); editing = false }),
                        modifier = Modifier.fillMaxWidth().focusRequester(fr),
                    )
                } else {
                    BasicText(
                        state.folderData(folder).name,
                        style = TextStyle(color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Right),
                        modifier = Modifier.fillMaxWidth().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { editing = true },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .width(panelW)
                    .height(panelH)
                    .graphicsLayer { shadowElevation = 18.dp.toPx(); shape = RoundedCornerShape(38.dp); clip = false }
                    .drawBehind { liquidGlass(38.dp.toPx(), strong = true) }
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
            ) {
                val pagesCount = FolderMath.pages(kids.size)
                val pager = rememberPagerState { pagesCount }
                Column(Modifier.fillMaxSize().padding(top = 18.dp, bottom = 12.dp)) {
                    HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth().weight(1f)) { page ->
                        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.Top) {
                            val chunk = kids.drop(page * FolderMath.PER_PAGE).take(FolderMath.PER_PAGE)
                            for (row in 0 until 3) {
                                Row(Modifier.fillMaxWidth().height(cellH)) {
                                    for (col in 0 until 3) {
                                        val k = chunk.getOrNull(row * 3 + col)
                                        Box(Modifier.weight(1f).height(cellH), contentAlignment = Alignment.TopCenter) {
                                            if (k != null) {
                                                val a = state.catalog.byKey(k.appKey)
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(16.dp))
                                                        .combinedClickable(
                                                            onClick = { state.closeFolder(); state.launch(k, null) },
                                                            onLongClick = { state.buzz(); held = k },
                                                        ),
                                                ) {
                                                    ItemIcon(state, k, icon)
                                                    Spacer(Modifier.height(5.dp))
                                                    BasicText(
                                                        a?.label ?: "",
                                                        style = TextStyle(color = Color.White, fontSize = 11.sp, textAlign = TextAlign.Center),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.width(icon + 16.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (pagesCount > 1) {
                        Row(Modifier.fillMaxWidth().height(14.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            for (i in 0 until pagesCount) {
                                Box(Modifier.padding(horizontal = 4.dp).size(7.dp).clip(CircleShape).background(Color.White.copy(alpha = if (i == pager.currentPage) 1f else 0.4f)))
                            }
                        }
                    }
                }
            }
            // ضغطة طويلة على تطبيق داخل المجلد: خياراته
            val h = held
            if (h != null) {
                Spacer(Modifier.height(12.dp))
                Column(
                    Modifier.fillMaxWidth().smokeGlass(RoundedCornerShape(20.dp), strong = true).padding(6.dp),
                ) {
                    FolderRow("طلّعه للشاشة") { held = null; state.removeFromFolder(h) }
                    FolderRow("معلومات التطبيق") { held = null; state.catalog.byKey(h.appKey)?.let { state.catalog.openInfo(it) } }
                    FolderRow("إلغاء") { held = null }
                }
            }
        }
    }
}

@Composable
private fun FolderRow(text: String, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicText(text, style = TextStyle(color = Onyx.Text, fontSize = 14.sp, fontWeight = FontWeight.Medium))
    }
}

/** الويدجت المنبثق: يطلع فوق الأيقونة بزجاج مدخّن، وضغطة برا تسكره */
@Composable
fun PopupOverlay(state: HomeState) {
    val pop = state.popup ?: return
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0x66000000))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { state.popup = null },
        )
        val W = constraints.maxWidth.toFloat()
        val H = constraints.maxHeight.toFloat()
        // المقاس الأساسي حق الويدجت (بكسل) ← dp، ومحصور بالشاشة
        val wDp = with(density) { pop.info.minWidth.toDp() }.coerceIn(240.dp, maxWidth - 32.dp)
        val hDp = with(density) { pop.info.minHeight.toDp() }.coerceIn(120.dp, 360.dp)
        val wPx = with(density) { wDp.toPx() }
        val hPx = with(density) { hDp.toPx() }
        val x = (pop.anchor.center.x - wPx / 2).coerceIn(with(density) { 16.dp.toPx() }, W - wPx - with(density) { 16.dp.toPx() })
        val above = pop.anchor.top - hPx - with(density) { 14.dp.toPx() }
        val y = (if (above > with(density) { 40.dp.toPx() }) above else pop.anchor.bottom + with(density) { 14.dp.toPx() })
            .coerceIn(with(density) { 40.dp.toPx() }, (H - hPx - with(density) { 24.dp.toPx() }).coerceAtLeast(0f))
        val shown by animateFloatAsState(1f, spring(dampingRatio = 0.8f, stiffness = 500f), label = "popup")
        Box(
            Modifier
                .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                .size(wDp, hDp)
                .graphicsLayer { alpha = shown; scaleX = 0.9f + 0.1f * shown; scaleY = 0.9f + 0.1f * shown }
                .smokeGlass(RoundedCornerShape(28.dp), strong = true, elevation = 12.dp)
                .padding(8.dp)
                .clip(RoundedCornerShape(22.dp)),
        ) {
            androidx.compose.runtime.key(pop.id) {
                AndroidView(
                    factory = { ctx -> state.app.widgets.host.createView(ctx, pop.id, pop.info) },
                    update = { v ->
                        try { v.updateAppWidgetSize(Bundle(), listOf(SizeF(wDp.value - 16f, hDp.value - 16f))) } catch (_: Exception) {}
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
