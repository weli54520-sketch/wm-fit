package com.waleed.onyx.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.waleed.onyx.OnyxApp
import com.waleed.onyx.data.Container
import com.waleed.onyx.data.Item
import com.waleed.onyx.data.Kind
import com.waleed.onyx.data.Screen
import com.waleed.onyx.grid.GeoInput
import com.waleed.onyx.grid.HomeGeometry
import com.waleed.onyx.grid.PagerPhysics
import com.waleed.onyx.sheets.AppDrawer
import com.waleed.onyx.sheets.SettingsSheet
import com.waleed.onyx.sheets.WidgetGallery
import com.waleed.onyx.ui.Motion
import com.waleed.onyx.ui.smokeGlass
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlin.math.roundToInt

/** المسافة بين الصفحة واللي جنبها (تبان بوضع الترتيب) */
private val PageGap = 12.dp

@Composable
fun HomeScreen(app: OnyxApp, homePresses: StateFlow<Int>) {
    val scope = rememberCoroutineScope()
    val state = remember { HomeState(app, scope) }
    state.haptic = LocalHapticFeedback.current
    val settings = app.settings
    val apps by app.catalog.apps.collectAsState()

    // الترتيب ثابت: العمود الجانبي يمين دايماً
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            state.density = density.density
            val inner = maxWidth >= HomeGeometry.INNER_MIN_WIDTH.dp
            val screen = if (inner) Screen.INNER else Screen.OUTER
            state.screen = screen
            val cols = settings.cols(screen)

            // ترتيب هالشاشة من قاعدة البيانات
            LaunchedEffect(screen) {
                app.repo.observe(screen).collect { if (state.drag == null) state.items = it }
            }

            val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

            // كل الأماكن من معادلة وحدة (كتاب المواصفات، الفصل ٢). الحراس يختبرونها مع كل بناء
            val g = HomeGeometry.compute(
                GeoInput(
                    width = maxWidth.value, height = maxHeight.value,
                    topInset = top.value, bottomInset = bottom.value,
                    cols = cols, rows = settings.rows(screen), sideOn = settings.sideOn, sideCount = settings.sideCount,
                    dockOn = settings.dockOn, dockCount = settings.dockCount, iconScale = settings.iconScale,
                ),
            )
            state.layout = g
            val rows = g.rows
            LaunchedEffect(screen, apps) { app.repo.sync(screen, apps) }
            LaunchedEffect(screen, cols, rows) { app.repo.reflow(screen) }

            val cellW = g.cell.dp
            val cellH = g.rowH.dp
            val icon = g.icon.dp
            with(density) { state.geo = Geo(cols, rows, cellW.toPx(), cellH.toPx()) }

            // الصفحات
            val gridItems = state.items.filter { it.container == Container.GRID }
            val pageCount = (gridItems.maxOfOrNull { it.page } ?: 0) + 1 + (if (state.editing) 1 else 0)
            val pages = state.pages
            pages.count = pageCount
            // ما نقرا مكان الصفحات وقت الرسم (كان يعيد رسم الشاشة كلها مع كل حركة إصبع)
            LaunchedEffect(pageCount) { pages.clamp() }

            BackHandler(enabled = true) {
                when {
                    state.menu != null -> state.menu = null
                    state.galleryOpen -> state.galleryOpen = false
                    state.picker != null -> { state.picker = null; state.drawerOpen = false }
                    state.drawerOpen -> state.drawerOpen = false
                    state.settingsOpen -> state.settingsOpen = false
                    state.editing -> state.exitEdit()
                    else -> state.goHome()
                }
            }
            LaunchedEffect(Unit) {
                homePresses.drop(1).collect {
                    state.closeAll()
                    state.goHome()
                }
            }
            LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { state.closeAll() }

            val shrink = state.editing && settings.editStyle == 0
            val outline by animateFloatAsState(if (shrink) 1f else 0f, Motion.smooth(settings), label = "outline")
            // الصفحات المرسومة: اللي وقفت عليها واللي جنبها، + اللي بين يديك الحين.
            // الجديدة ترتسم بعد ما توقف، مو وسط السحب (هذا كان سبب التقطيع أول كل سحبة)
            val visible by remember {
                derivedStateOf {
                    val o = pages.offset
                    val s = pages.settled
                    val a = minOf(s - 1, kotlin.math.floor(o).toInt()).coerceAtLeast(0)
                    val b = maxOf(s + 1, kotlin.math.ceil(o).toInt()).coerceAtMost(pages.count - 1)
                    a..b
                }
            }
            val currentPage by remember { derivedStateOf { pages.current } }
            val pagesW = g.pages.w.dp
            val gapPx = with(density) { PageGap.toPx() }
            val pageWpx = with(density) { pagesW.toPx() }

            // طبقة الشاشة الرئيسية: مدير لمس واحد لكل شي تحتها
            Box(Modifier.fillMaxSize().homeGestures(state)) {
                // الصفحات: من حافة الشاشة لين قبل العمود. اللي يطلع عنها ينقص (ما يمر تحت العمود)
                Box(
                    Modifier
                        .offset(x = g.pages.l.dp, y = g.pages.t.dp)
                        .size(pagesW, g.pages.h.dp)
                        .clipToBounds(),
                ) {
                    // التصغير بوضع الترتيب حول نص المنطقة، والصفحات اللي جنبها تبان من الأطراف
                    Box(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val k = state.editScale.value
                                scaleX = k; scaleY = k
                            },
                    ) {
                        for (page in visible) {
                            key(page) {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .offset { IntOffset(((page - pages.offset) * (pageWpx + gapPx)).roundToInt(), 0) },
                                ) {
                                    if (outline > 0.01f) {
                                        Box(
                                            Modifier
                                                .padding(start = (g.padStart - 8f).dp, end = 2.dp)
                                                .fillMaxSize()
                                                .drawBehind {
                                                    drawRoundRect(
                                                        Color.White.copy(alpha = 0.16f * outline),
                                                        cornerRadius = CornerRadius(30.dp.toPx()),
                                                        style = Stroke(1.dp.toPx()),
                                                    )
                                                },
                                        )
                                    }
                                    Box(Modifier.padding(start = g.padStart.dp).size(g.grid.w.dp, g.grid.h.dp)) {
                                        GhostCell(state, page, cellW, cellH, icon)
                                        for (item in state.items) {
                                            if (item.container != Container.GRID || item.page != page) continue
                                            key(item.id) {
                                                if (item.kind == Kind.WIDGET) WidgetTile(state, item, page, cellW, cellH)
                                                else GridTile(state, item, cellW, cellH, icon)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                PageDots(
                    count = pageCount,
                    current = currentPage,
                    modifier = Modifier.offset(y = g.dots.t.dp).fillMaxWidth().height(g.dots.h.dp),
                )

                // العمود الجانبي (الشريط الأساسي): منفصل، لاصق يمين، بنص منطقة الصفحات، ومسافته ثابتة
                if (g.sideOn) {
                    Box(
                        Modifier
                            .offset(x = g.side.l.dp, y = g.side.t.dp)
                            .size(g.side.w.dp, g.side.h.dp)
                            .smokeGlass(RoundedCornerShape(50), elevation = 0.dp),
                    ) {
                        for (i in 0 until g.sideCount) {
                            val b = g.sideSlots[i]
                            Box(Modifier.offset(x = (b.l - g.side.l).dp, y = (b.t - g.side.t).dp)) {
                                SlotTile(state, Slot(Container.SIDE, i), g.sideIcon.dp)
                            }
                        }
                    }
                }

                // الشريط السفلي (اختياري)
                val dock = g.dock
                if (dock != null) {
                    Box(
                        Modifier
                            .offset(x = dock.l.dp, y = dock.t.dp)
                            .size(dock.w.dp, dock.h.dp)
                            .smokeGlass(RoundedCornerShape((dock.h * 0.43f).dp)),
                    ) {
                        g.dockSlots.forEachIndexed { i, b ->
                            Box(Modifier.offset(x = (b.l - dock.l).dp, y = (b.t - dock.t).dp)) {
                                SlotTile(state, Slot(Container.DOCK, i), g.dockIcon.dp)
                            }
                        }
                    }
                }
            }

            // لمعة الحافة: الصفحة بتتقلب
            EdgeGlow(state, g.pages.t.dp, g.pages.h.dp, pagesW)
            // اللي يتسحب يطلع فوق كل شي
            DragOverlay(state, icon)

            EditBar(state, top)
            ContextMenu(state)
            AppDrawer(state, inner, icon)
            WidgetGallery(state, inner)
            SettingsSheet(state)
            ToastPill(state, (maxHeight.value - g.dots.t + 12f).dp)
        }
    }
}

/** أيقونة بالشبكة: ترسم بس. اللمس كله عند مدير اللمس */
@Composable
private fun GridTile(state: HomeState, item: Item, cellW: Dp, cellH: Dp, icon: Dp) {
    val settings = state.settings
    val density = LocalDensity.current
    val preview = state.previewCell(item.page, item.id)
    val cx = preview?.x ?: item.x
    val cy = preview?.y ?: item.y
    val target = with(density) { IntOffset((cellW * cx).roundToPx(), (cellH * cy).roundToPx()) }
    val anim = remember { Animatable(target, IntOffset.VectorConverter) }
    LaunchedEffect(target, state.dropTick) {
        state.takeDropFrom(item.id)?.let { anim.snapTo(it) }
        anim.animateTo(target, Motion.smooth(settings))
    }
    val dragged = state.drag?.item?.id == item.id
    val pressed = state.pressedKey == item.id
    val press by animateFloatAsState(if (pressed) 0.88f else 1f, pressSpec(pressed, settings), label = "press")
    // الأيقونة اللي بتطلع للصفحة اللي بعدها (الصفحة مليانة): تختفي بنعومة
    val out by animateFloatAsState(if (state.isOverflow(item.id)) 0f else 1f, Motion.smooth(settings), label = "out")
    val appInfo = state.catalog.byKey(item.appKey)
    val jiggle = state.editing && settings.editStyle == 1 && !settings.reduceMotion
    val rot = if (jiggle) {
        val t = rememberInfiniteTransition(label = "jiggle")
        t.animateFloat(-1f, 1f, infiniteRepeatable(tween(140, easing = LinearEasing), RepeatMode.Reverse), label = "j").value * (if (item.id % 2L == 0L) 1f else -1f)
    } else 0f
    Box(
        Modifier
            .offset { anim.value }
            .size(cellW, cellH)
            .graphicsLayer {
                alpha = if (dragged) 0f else out
                rotationZ = rot
                val k = press * (0.6f + 0.4f * out)
                scaleX = k; scaleY = k
            },
        contentAlignment = Alignment.Center,
    ) {
        AppFace(appInfo, state.catalog, icon, settings.iconStyle, settings.labels)
    }
}

/** ظل المكان اللي بينزل فيه العنصر: يتحرك بنعومة مع إصبعك */
@Composable
private fun GhostCell(state: HomeState, page: Int, cellW: Dp, cellH: Dp, icon: Dp) {
    val d = state.drag ?: return
    val c = d.cand ?: return
    if (d.page != page) return
    val ok = !(d.solvedFor == c && d.solution == null)
    val density = LocalDensity.current
    val widget = d.item.kind == Kind.WIDGET
    val gw = if (widget) cellW * d.item.w - WidgetMargin * 2 else icon
    val gh = if (widget) cellH * d.item.h - WidgetMargin * 2 else icon
    val target = with(density) { IntOffset((cellW * c.x + (cellW * d.item.w - gw) / 2).roundToPx(), (cellH * c.y + (cellH * d.item.h - gh) / 2).roundToPx()) }
    val anim = remember { Animatable(target, IntOffset.VectorConverter) }
    LaunchedEffect(target) { anim.animateTo(target, Motion.smooth(state.settings)) }
    Box(
        Modifier
            .offset { anim.value }
            .size(gw, gh)
            .drawBehind {
                val r = if (widget) CornerRadius(WidgetCorner.toPx()) else CornerRadius(size.minDimension * 0.31f)
                drawRoundRect(Color.White.copy(alpha = if (ok) 0.10f else 0.04f), cornerRadius = r)
                drawRoundRect(
                    if (ok) Color.White.copy(alpha = 0.5f) else Color(0xFFFF7A7A),
                    cornerRadius = r,
                    style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
                )
            },
    )
}

/** أيقونة بالعمود أو الشريط: ترسم بس */
@Composable
private fun SlotTile(state: HomeState, slot: Slot, icon: Dp) {
    val item = state.slotItem(slot)
    val dragging = state.drag
    val appInfo = item?.let { state.catalog.byKey(it.appKey) }
    val hover = dragging?.slot == slot && dragging.slotReady
    val pressed = state.pressedKey == slot
    val scale by animateFloatAsState(if (hover) 1.12f else if (pressed) 0.88f else 1f, pressSpec(pressed, state.settings), label = "slot")
    // الأيقونة اللي طالعة من هالخانة وتتسحب: مكانها فاضي
    val away = dragging != null && dragging.fromSlot == slot
    Box(
        Modifier
            .size(icon)
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (away) 0f else 1f },
        contentAlignment = Alignment.Center,
    ) {
        if (appInfo != null) AppIcon(appInfo, state.catalog, icon, state.settings.iconStyle)
        else Box(Modifier.size(icon * 0.5f).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)))
    }
}

/** العنصر المسحوب: يلحق إصبعك بالضبط، بمقاسه بالصفحة (مصغر مع وضع الترتيب) */
@Composable
private fun DragOverlay(state: HomeState, icon: Dp) {
    val d = state.drag ?: return
    val appInfo = state.catalog.byKey(d.item.appKey)
    val widget = d.item.kind == Kind.WIDGET
    val g = state.layout ?: return
    val lift by animateFloatAsState(if (d.slot != null && d.slotReady) 0.92f else if (widget) 1.03f else 1.06f, Motion.smooth(state.settings), label = "lift")
    val k = state.editScale.value
    val w = (g.cell * d.item.w).dp
    val h = (g.rowH * d.item.h).dp
    Box(
        Modifier
            .offset {
                val b = state.dragBoxDp(d)
                if (b == null) IntOffset.Zero else IntOffset((b.l * density).roundToInt(), (b.t * density).roundToInt())
            }
            .size(w * k, h * k)
            .graphicsLayer {
                scaleX = lift; scaleY = lift
                if (!widget) {
                    shadowElevation = 18.dp.toPx()
                    shape = RoundedCornerShape(percent = 31)
                }
                clip = false
            },
        contentAlignment = Alignment.Center,
    ) {
        if (widget) WidgetLifted(d.snap, state.widgetData(d.item)?.frame != false, Modifier.fillMaxSize().padding(WidgetMargin * k), k)
        else AppIcon(appInfo, state.catalog, icon * k, state.settings.iconStyle)
    }
}

@Composable
private fun PageDots(count: Int, current: Int, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        if (count > 1) {
            for (i in 0 until count) {
                val w by animateFloatAsState(if (i == current) 16f else 6f, label = "dot")
                Box(
                    Modifier
                        .padding(horizontal = 3.5.dp)
                        .size(width = w.dp, height = 6.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = if (i == current) 1f else 0.38f)),
                )
            }
        }
    }
}

/** ضغطة الأيقونة: تصغر بسرعة (0.12 ثانية) وترجع بارتداد خفيف */
private fun pressSpec(pressed: Boolean, settings: com.waleed.onyx.data.Settings): androidx.compose.animation.core.AnimationSpec<Float> =
    if (pressed) tween(120) else Motion.bouncy(settings)

/** لمعة ناعمة بطرف منطقة الصفحات لما العنصر هناك وأنت تسحب: يعني الصفحة بتتقلب */
@Composable
private fun EdgeGlow(state: HomeState, top: Dp, height: Dp, rightEdge: Dp) {
    val d = state.drag
    val dir = d?.edge ?: 0
    val a by animateFloatAsState(if (dir != 0) 1f else 0f, Motion.smooth(state.settings), label = "edge")
    val last = remember { intArrayOf(0) }
    if (dir != 0) last[0] = dir
    if (a < 0.01f) return
    val w = HomeGeometry.EDGE_ZONE.dp
    val left = last[0] < 0
    Box(
        Modifier
            .offset(x = if (left) 0.dp else rightEdge - w, y = top)
            .size(w, height)
            .graphicsLayer { alpha = a }
            .background(
                Brush.horizontalGradient(
                    if (left) listOf(Color.White.copy(alpha = 0.22f), Color.Transparent)
                    else listOf(Color.Transparent, Color.White.copy(alpha = 0.22f)),
                ),
            ),
    )
}

/** الويدجت وهو مرفوع (يتسحب أو بالقائمة): صورته فوق زجاج، أو زجاج بس لو ما انصور */
@Composable
fun WidgetLifted(snap: ImageBitmap?, frame: Boolean, modifier: Modifier, scale: Float = 1f) {
    val shape = RoundedCornerShape(WidgetCorner * scale)
    val inset = if (frame) FramePad * scale else 0.dp
    Box(modifier.then(if (frame || snap == null) Modifier.smokeGlass(shape, strong = true, elevation = 16.dp) else Modifier)) {
        if (snap != null) {
            Image(
                snap, null,
                Modifier.fillMaxSize().padding(inset).clip(RoundedCornerShape(WidgetCorner * scale - inset)),
                contentScale = ContentScale.FillBounds,
            )
        }
    }
}
