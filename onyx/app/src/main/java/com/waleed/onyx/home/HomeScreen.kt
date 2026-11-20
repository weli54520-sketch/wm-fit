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
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.waleed.onyx.OnyxApp
import com.waleed.onyx.data.Container
import com.waleed.onyx.data.Item
import com.waleed.onyx.data.Screen
import com.waleed.onyx.sheets.AppDrawer
import com.waleed.onyx.sheets.SettingsSheet
import com.waleed.onyx.ui.Motion
import com.waleed.onyx.ui.smokeGlass
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
            val inner = maxWidth >= 600.dp
            val screen = if (inner) Screen.INNER else Screen.OUTER
            state.screen = screen
            val cols = settings.cols(screen)
            val rows = settings.rows(screen)

            // ترتيب هالشاشة من قاعدة البيانات
            LaunchedEffect(screen) {
                app.repo.observe(screen).collect { if (state.drag == null) state.items = it }
            }
            LaunchedEffect(screen, apps) { app.repo.sync(screen, apps) }
            LaunchedEffect(screen, cols, rows) { app.repo.reflow(screen) }

            val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val padStart = if (inner) 28.dp else 12.dp
            val edge = if (inner) 14.dp else 6.dp
            val sideOn = settings.sideOn
            val cellW = (maxWidth - padStart - edge - (if (inner) 10.dp else 0.dp)) / (cols + if (sideOn) 1 else 0)
            val icon = min(cellW * 0.76f, if (inner) 86.dp else 74.dp) * settings.iconScale
            val sideIcon = if (inner) icon * 0.86f else icon
            val dockIcon = icon * 0.88f
            val dockH = dockIcon + 22.dp
            val dockW = if (inner) min(maxWidth * 0.62f, 520.dp) else maxWidth - 24.dp
            val gridTop = top + 14.dp
            val dotsH = 22.dp
            val gridH = maxHeight - gridTop - dotsH - dockH - bottom - 26.dp
            val cellH = gridH / rows
            val gridW = cellW * cols

            with(density) {
                state.geo = Geo(cols, rows, cellW.toPx(), cellH.toPx(), maxWidth.toPx(), 22.dp.toPx())
            }

            val gridItems = state.items.filter { it.container == Container.GRID }
            val pageCount = (gridItems.maxOfOrNull { it.page } ?: 0) + 1 + (if (state.editing) 1 else 0)
            val pager = rememberPagerState(pageCount = { pageCount })
            state.pager = pager

            BackHandler(enabled = true) {
                when {
                    state.menu != null -> state.menu = null
                    state.picker != null -> { state.picker = null; state.drawerOpen = false }
                    state.drawerOpen -> state.drawerOpen = false
                    state.settingsOpen -> state.settingsOpen = false
                    state.editing -> state.exitEdit()
                    else -> scope.launch { pager.animateScrollToPage(0) }
                }
            }
            LaunchedEffect(Unit) {
                homePresses.drop(1).collect {
                    state.closeAll()
                    pager.animateScrollToPage(0)
                }
            }
            LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { state.closeAll() }

            val shrink = state.editing && settings.editStyle == 0
            val pageScale by animateFloatAsState(if (shrink) 0.9f else 1f, Motion.bouncy(settings), label = "pageScale")
            val outline by animateFloatAsState(if (shrink) 1f else 0f, Motion.smooth(settings), label = "outline")

            // الصفحات
            Box(
                Modifier
                    .offset(y = gridTop)
                    .fillMaxWidth()
                    .height(gridH)
                    .pointerInput(state.editing) {
                        if (!state.editing) {
                            var dy = 0f
                            detectVerticalDragGestures(
                                onDragStart = { dy = 0f },
                                onVerticalDrag = { _, d -> dy += d },
                                onDragEnd = { if (dy < -90f) state.drawerOpen = true },
                            )
                        }
                    },
            ) {
                HorizontalPager(
                    state = pager,
                    userScrollEnabled = state.drag == null,
                    beyondViewportPageCount = 3,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { scaleX = pageScale; scaleY = pageScale },
                ) { page ->
                    Box(Modifier.fillMaxSize().emptyAreaGestures(state)) {
                        if (outline > 0.01f) {
                            Box(
                                Modifier
                                    .padding(start = padStart - 8.dp, end = edge)
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
                        Box(
                            Modifier
                                .padding(start = padStart)
                                .size(gridW, gridH)
                                .onGloballyPositioned { state.pageBounds[page] = it.boundsInRoot() },
                        ) {
                            GhostCell(state, page, cellW, cellH, icon)
                            for (item in state.items) {
                                if (item.container != Container.GRID || item.page != page) continue
                                key(item.id) { GridTile(state, item, page, cellW, cellH, icon) }
                            }
                        }
                    }
                }
            }

            // النقاط تحت الصفحات
            PageDots(
                count = pageCount,
                current = pager.currentPage,
                modifier = Modifier.offset(y = gridTop + gridH + 4.dp).fillMaxWidth().height(dotsH),
            )

            // العمود الجانبي: لاصق يمين، بالنص
            if (sideOn) {
                val n = settings.sideCount
                val gap = sideIcon * 0.24f
                val sideW = sideIcon + 14.dp
                val sideH = sideIcon * n + gap * (n - 1) + 14.dp
                Box(
                    Modifier
                        .offset(x = maxWidth - edge - sideW, y = gridTop + (gridH - sideH) / 2)
                        .size(sideW, sideH)
                        .smokeGlass(RoundedCornerShape(50), elevation = 0.dp),
                ) {
                    for (i in 0 until n) {
                        val slot = Slot(Container.SIDE, i)
                        Box(Modifier.offset(x = 7.dp, y = 7.dp + (sideIcon + gap) * i)) {
                            SlotTile(state, slot, sideIcon)
                        }
                    }
                }
            } else {
                state.slotBounds.keys.removeAll { it.container == Container.SIDE }
            }

            // الشريط السفلي
            val dn = settings.dockCount
            Box(
                Modifier
                    .offset(x = (maxWidth - dockW) / 2, y = maxHeight - bottom - 14.dp - dockH)
                    .size(dockW, dockH)
                    .smokeGlass(RoundedCornerShape(dockH * 0.42f)),
            ) {
                Row(
                    Modifier.fillMaxSize().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    for (i in 0 until dn) SlotTile(state, Slot(Container.DOCK, i), dockIcon)
                }
            }
            state.slotBounds.keys.removeAll { it.container == Container.DOCK && it.index >= dn }
            state.slotBounds.keys.removeAll { it.container == Container.SIDE && it.index >= settings.sideCount }

            // اللي يتسحب يطلع فوق كل شي
            DragOverlay(state, cellW, cellH, icon)

            EditBar(state, top)
            ContextMenu(state)
            AppDrawer(state, inner, icon)
            SettingsSheet(state)
            ToastPill(state, bottom + dockH + 30.dp)
        }
    }
}

@Composable
private fun GridTile(state: HomeState, item: Item, page: Int, cellW: Dp, cellH: Dp, icon: Dp) {
    val settings = state.settings
    val density = LocalDensity.current
    val d = state.drag
    val preview = if (d != null && d.page == page && d.item.id != item.id) d.solution?.get(item.id) else null
    val cx = preview?.x ?: item.x
    val cy = preview?.y ?: item.y
    val target = with(density) { IntOffset((cellW * cx).roundToPx(), (cellH * cy).roundToPx()) }
    val anim = remember { Animatable(target, IntOffset.VectorConverter) }
    LaunchedEffect(target, state.dropTick) {
        state.takeDropFrom(item.id)?.let { anim.snapTo(it) }
        anim.animateTo(target, Motion.bouncy(settings))
    }
    val dragged = d?.item?.id == item.id
    val coords = rememberCoordsHolder()
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
                alpha = if (dragged) 0f else 1f
                rotationZ = rot
            }
            .trackBounds { r, c -> coords[0] = c; state.tileBounds[item.id] = r }
            .tileGestures(
                state = state,
                key = item.id,
                coords = { coords[0] },
                onTap = { if (!state.editing) state.launch(item, state.tileBounds[item.id]) },
                onLongPress = { state.openMenu(item, null) },
                canDrag = true,
                onDragStart = { p -> state.startDrag(item, p) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        AppFace(appInfo, state.catalog, icon, settings.iconStyle, settings.labels)
    }
}

/** ظل المكان اللي بينزل فيه العنصر */
@Composable
private fun GhostCell(state: HomeState, page: Int, cellW: Dp, cellH: Dp, icon: Dp) {
    val d = state.drag ?: return
    val c = d.cand ?: return
    if (d.page != page || d.slot != null) return
    val ok = d.solution != null
    val density = LocalDensity.current
    val target = with(density) { IntOffset((cellW * c.x + (cellW - icon) / 2).roundToPx(), (cellH * c.y + (cellH - icon) / 2).roundToPx()) }
    val anim = remember { Animatable(target, IntOffset.VectorConverter) }
    LaunchedEffect(target) { anim.animateTo(target, Motion.smooth(state.settings)) }
    Box(
        Modifier
            .offset { anim.value }
            .size(icon)
            .drawBehind {
                val r = CornerRadius(size.minDimension * 0.31f)
                drawRoundRect(Color.White.copy(alpha = if (ok) 0.10f else 0.04f), cornerRadius = r)
                drawRoundRect(
                    if (ok) Color.White.copy(alpha = 0.5f) else Color(0xFFFF7A7A),
                    cornerRadius = r,
                    style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
                )
            },
    )
}

/** أيقونة بالعمود أو الشريط: ضغطة تفتح، ضغطة طويلة قائمة "بدّل التطبيق" */
@Composable
private fun SlotTile(state: HomeState, slot: Slot, icon: Dp) {
    val item = state.slotItem(slot)
    val appInfo = item?.let { state.catalog.byKey(it.appKey) }
    val coords = rememberCoordsHolder()
    val hover = state.drag?.slot == slot
    val scale by animateFloatAsState(if (hover) 1.12f else 1f, Motion.bouncy(state.settings), label = "slotHover")
    Box(
        Modifier
            .size(icon)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .trackBounds { r, c -> coords[0] = c; state.slotBounds[slot] = r }
            .tileGestures(
                state = state,
                key = slot,
                coords = { coords[0] },
                onTap = {
                    if (item != null) state.launch(item, state.slotBounds[slot]) else { state.picker = slot; state.drawerOpen = true }
                },
                onLongPress = {
                    if (item != null) state.openMenu(item, slot) else { state.picker = slot; state.drawerOpen = true }
                },
                canDrag = false,
                onDragStart = {},
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (appInfo != null) AppIcon(appInfo, state.catalog, icon, state.settings.iconStyle)
        else Box(Modifier.size(icon * 0.5f).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)))
    }
}

@Composable
private fun DragOverlay(state: HomeState, cellW: Dp, cellH: Dp, icon: Dp) {
    val d = state.drag ?: return
    val density = LocalDensity.current
    val s = state.dragScale()
    val appInfo = state.catalog.byKey(d.item.appKey)
    val lift by animateFloatAsState(if (d.slot != null) 0.9f else 1.08f, Motion.bouncy(state.settings), label = "lift")
    Box(
        Modifier
            .offset {
                IntOffset((d.pointer.x - d.grab.x * s).roundToInt(), (d.pointer.y - d.grab.y * s).roundToInt())
            }
            .size(cellW * s, cellH * s)
            .graphicsLayer {
                scaleX = lift; scaleY = lift
                shadowElevation = with(density) { 18.dp.toPx() }
                shape = RoundedCornerShape(percent = 31)
                clip = false
            },
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(appInfo, state.catalog, icon * s, state.settings.iconStyle)
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
