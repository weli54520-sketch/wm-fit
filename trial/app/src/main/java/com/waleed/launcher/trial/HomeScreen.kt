package com.waleed.launcher.trial

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
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

// زجاج فاتح شفاف للعمود والشريط
private val GlassFill = Color.White.copy(alpha = 0.16f)
private val GlassBorder = Color.White.copy(alpha = 0.30f)

@Composable
fun HomeScreen(repository: AppRepository, settings: LauncherSettings, homePresses: StateFlow<Int>) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var showSettings by remember { mutableStateOf(false) }
    val tracker = remember { PressTracker() }
    val style = settings.iconStyle

    // نحدث القائمة كل ما ترجع للشاشة الرئيسية (لو ثبت أو حذفت تطبيق)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        scope.launch {
            val fresh = repository.loadApps()
            if (fresh.map { it.key to it.label } != apps.map { it.key to it.label }) apps = fresh
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { showSettings = false }

    val layout = remember(apps) { repository.arrange(apps) }
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // الترتيب ثابت مثل شاشة وليد: العمود الجانبي يمين دايماً
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= 600.dp
            val gridColumns = if (wide) 6 else 4

            // الهوامش: أوسع شوي عشان التصميم يتنفس
            val sidePadding = if (wide) maxWidth * 0.06f else 18.dp
            val contentWidth = maxWidth - sidePadding * 2
            val cell = contentWidth / (gridColumns + 1)

            // حجم الأيقونة: حوالي ٧٨٪ من الخانة، مثل نسبة الآيفون
            val iconSize = minOf(cell * 0.78f, 84.dp)

            // الشريط السفلي: أصغر شوي من أيقونات الشاشة، وأنحف
            val dockIcon = iconSize * 0.86f
            val dockHeight = dockIcon + 20.dp
            val dockWidth = if (wide) minOf(contentWidth * 0.72f, 640.dp) else contentWidth
            val dotsHeight = 22.dp

            val rowHeight = cell
            val areaHeight = maxHeight - topInset - dotsHeight - dockHeight - 14.dp - bottomInset
            val rows = (areaHeight / rowHeight).toInt().coerceIn(SIDE_COUNT, 8)
            val gridHeight = rowHeight * rows
            // نوزع الفراغ الزايد فوق وتحت عشان كل شي يجي بالنص
            val verticalGap = ((areaHeight - gridHeight) / 2).coerceAtLeast(0.dp)

            val perPage = rows * gridColumns
            val pageCount = maxOf(1, (layout.pages.size + perPage - 1) / perPage)
            val pagerState = rememberPagerState(pageCount = { pageCount })

            BackHandler {
                if (showSettings) showSettings = false
                else scope.launch { pagerState.animateScrollToPage(0) }
            }
            LaunchedEffect(Unit) {
                homePresses.drop(1).collect {
                    showSettings = false
                    pagerState.animateScrollToPage(0)
                }
            }

            Column(Modifier.fillMaxSize()) {
                Spacer(Modifier.height(topInset + verticalGap))

                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(gridHeight)
                        .padding(horizontal = sidePadding)
                        // ضغطة طويلة على مكان فاضي = الإعدادات
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = {
                                    if (!tracker.iconPressed) {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        showSettings = true
                                    }
                                },
                            )
                        },
                ) {
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
                                            AppIcon(app, repository, iconSize, style, tracker)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    SideColumn(layout.side, repository, iconSize, style, tracker, cell, rowHeight)
                }

                Spacer(Modifier.height(verticalGap))
                PageDots(count = pageCount, current = pagerState.currentPage, height = dotsHeight)
                Dock(layout.dock, repository, dockIcon, style, tracker, dockHeight, dockWidth)
                Spacer(Modifier.height(14.dp + bottomInset))
            }

            SettingsSheet(
                visible = showSettings,
                settings = settings,
                previewApps = layout.side.drop(1) + layout.dock,
                repository = repository,
                onClose = { showSettings = false },
            )
        }
    }
}

/** العمود الجانبي يمين: زجاج حقيقي، ٤ تطبيقات */
@Composable
private fun SideColumn(
    apps: List<AppEntry>,
    repository: AppRepository,
    iconSize: Dp,
    style: IconStyle,
    tracker: PressTracker,
    cell: Dp,
    rowHeight: Dp,
) {
    val glassWidth = iconSize + 14.dp
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
                    AppIcon(app, repository, iconSize, style, tracker)
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
                        .padding(horizontal = 3.dp)
                        .size(6.dp)
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
    style: IconStyle,
    tracker: PressTracker,
    height: Dp,
    width: Dp,
) {
    val shape = RoundedCornerShape(height * 0.36f)
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            Modifier
                .width(width)
                .height(height)
                .clip(shape)
                .background(GlassFill)
                .border(1.dp, GlassBorder, shape)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            apps.take(DOCK_COUNT).forEach { app -> AppIcon(app, repository, iconSize, style, tracker) }
        }
    }
}
