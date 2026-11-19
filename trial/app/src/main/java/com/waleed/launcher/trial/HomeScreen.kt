package com.waleed.launcher.trial

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

private const val SIDE_COUNT = 4
private const val DOCK_COUNT = 6

// زجاج فاتح شفاف مثل العمود اللي في شاشة وليد
private val GlassFill = Color.White.copy(alpha = 0.18f)
private val GlassBorder = Color.White.copy(alpha = 0.28f)

@Composable
fun HomeScreen(repository: AppRepository, homePresses: StateFlow<Int>) {
    val scope = rememberCoroutineScope()
    var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }

    // نحدث القائمة كل ما ترجع للشاشة الرئيسية (لو ثبت أو حذفت تطبيق)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        scope.launch {
            val fresh = repository.loadApps()
            if (fresh.map { it.key to it.label } != apps.map { it.key to it.label }) apps = fresh
        }
    }

    val layout = remember(apps) { repository.arrange(apps) }
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // الترتيب ثابت مثل صورة وليد: العمود الجانبي يمين دايماً
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= 600.dp
            val gridColumns = if (wide) 6 else 4
            val sidePadding = if (wide) 48.dp else 14.dp
            val contentWidth = maxWidth - sidePadding * 2
            val cell = contentWidth / (gridColumns + 1)

            val dockInnerPadding = 10.dp
            val dockWidth = if (wide) minOf(contentWidth, 620.dp) else contentWidth
            val dockCell = (dockWidth - dockInnerPadding * 2) / DOCK_COUNT
            val iconSize = minOf(cell * 0.84f, dockCell * 0.9f, 80.dp)

            val rowHeight = cell
            val dockHeight = iconSize + 22.dp
            val dotsHeight = 24.dp
            val gridHeight = maxHeight - topInset - 16.dp - dotsHeight - dockHeight - 12.dp - bottomInset
            val rows = (gridHeight / rowHeight).toInt().coerceIn(SIDE_COUNT, 9)
            val perPage = rows * gridColumns
            val pageCount = maxOf(1, (layout.pages.size + perPage - 1) / perPage)
            val pagerState = rememberPagerState(pageCount = { pageCount })

            BackHandler { scope.launch { pagerState.animateScrollToPage(0) } }
            LaunchedEffect(Unit) {
                homePresses.drop(1).collect { pagerState.animateScrollToPage(0) }
            }

            Column(Modifier.fillMaxSize()) {
                Spacer(Modifier.height(topInset + 16.dp))

                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(gridHeight)
                        .padding(horizontal = sidePadding),
                ) {
                    // الصفحات: تتقلب يمين ويسار
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.width(cell * gridColumns).height(gridHeight),
                    ) { page ->
                        val start = page * perPage
                        val items = layout.pages.subList(
                            minOf(start, layout.pages.size),
                            minOf(start + perPage, layout.pages.size),
                        )
                        Column(Modifier.fillMaxSize()) {
                            items.chunked(gridColumns).forEach { rowItems ->
                                Row(Modifier.height(rowHeight)) {
                                    rowItems.forEach { app ->
                                        Box(Modifier.size(cell, rowHeight), contentAlignment = Alignment.Center) {
                                            AppIcon(app, repository, iconSize)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // العمود الجانبي: ثابت ما يتقلب مع الصفحات
                    SideColumn(layout.side, repository, iconSize, cell, rowHeight)
                }

                PageDots(count = pageCount, current = pagerState.currentPage, height = dotsHeight)

                Dock(layout.dock, repository, iconSize, dockHeight, dockWidth, dockInnerPadding)

                Spacer(Modifier.height(12.dp + bottomInset))
            }
        }
    }
}

/** العمود الجانبي يمين: زجاج حقيقي، ٤ تطبيقات */
@Composable
private fun SideColumn(apps: List<AppEntry>, repository: AppRepository, iconSize: Dp, cell: Dp, rowHeight: Dp) {
    val glassWidth = iconSize + 16.dp
    val shape = RoundedCornerShape(glassWidth / 2)
    Box(Modifier.width(cell), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .width(glassWidth)
                .height(rowHeight * SIDE_COUNT)
                .clip(shape)
                .background(GlassFill)
                .border(1.dp, GlassBorder, shape),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            apps.forEach { app ->
                Box(Modifier.height(rowHeight), contentAlignment = Alignment.Center) {
                    AppIcon(app, repository, iconSize)
                }
            }
        }
    }
}

@Composable
private fun PageDots(count: Int, current: Int, height: Dp) {
    Row(
        Modifier.fillMaxWidth().height(height),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (count > 1) {
            repeat(count) { index ->
                val color by animateColorAsState(
                    if (index == current) Color.White else Color.White.copy(alpha = 0.4f),
                    animationSpec = tween(250),
                    label = "dot",
                )
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(color),
                )
            }
        }
    }
}

/** الشريط السفلي: زجاج بزوايا ناعمة، ٦ تطبيقات */
@Composable
private fun Dock(
    apps: List<AppEntry>,
    repository: AppRepository,
    iconSize: Dp,
    height: Dp,
    width: Dp,
    innerPadding: Dp,
) {
    val shape = RoundedCornerShape(32.dp)
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            Modifier
                .widthIn(max = width)
                .fillMaxWidth()
                .height(height)
                .clip(shape)
                .background(GlassFill)
                .border(1.dp, GlassBorder, shape)
                .padding(horizontal = innerPadding),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            apps.forEach { app -> AppIcon(app, repository, iconSize) }
        }
    }
}

/** أيقونة واحدة: مثل ما يعطيها الجوال بالضبط، مع ضغطة نابضة */
@Composable
private fun AppIcon(app: AppEntry, repository: AppRepository, size: Dp) {
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val haptics = LocalHapticFeedback.current
    val icon: ImageBitmap? by produceState(repository.cachedIcon(app, sizePx), app.key, sizePx) {
        value = repository.icon(app, sizePx)
    }

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
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { repository.launch(app) },
                    onLongPress = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        repository.openAppInfo(app)
                    },
                )
            },
    ) {
        icon?.let {
            Image(bitmap = it, contentDescription = app.label, modifier = Modifier.fillMaxSize())
        }
    }
}
