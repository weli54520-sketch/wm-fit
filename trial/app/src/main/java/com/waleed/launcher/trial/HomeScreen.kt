package com.waleed.launcher.trial

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

private const val LABEL_HEIGHT_DP = 22
private const val ROW_GAP_DP = 20

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

    val (dock, pageApps) = remember(apps) { repository.arrange(apps) }

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // الشاشة الداخلية عريضة، الخارجية ضيقة. كل وحدة لها ترتيب
        val wide = maxWidth >= 600.dp
        val columns = if (wide) 6 else 4
        val sidePadding = if (wide) 56.dp else 20.dp
        val cellWidth = (maxWidth - sidePadding * 2) / columns
        val iconSize = minOf(if (wide) 74.dp else 64.dp, cellWidth * 0.80f)

        val dockHeight = iconSize + 28.dp
        val dockArea = dockHeight + 12.dp + bottomInset
        val dotsArea = 26.dp
        val gridHeight = maxHeight - topInset - 18.dp - dockArea - dotsArea
        val rowHeight = iconSize + LABEL_HEIGHT_DP.dp + ROW_GAP_DP.dp
        val rows = (gridHeight / rowHeight).toInt().coerceIn(3, 7)
        val perPage = rows * columns
        val pageCount = maxOf(1, (pageApps.size + perPage - 1) / perPage)

        val pagerState = rememberPagerState(pageCount = { pageCount })

        BackHandler { scope.launch { pagerState.animateScrollToPage(0) } }
        LaunchedEffect(Unit) {
            homePresses.drop(1).collect { pagerState.animateScrollToPage(0) }
        }

        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(topInset + 18.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().height(gridHeight),
            ) { page ->
                val start = page * perPage
                val items = pageApps.subList(
                    minOf(start, pageApps.size),
                    minOf(start + perPage, pageApps.size),
                )
                Column(
                    Modifier.fillMaxSize().padding(horizontal = sidePadding),
                    verticalArrangement = Arrangement.spacedBy(ROW_GAP_DP.dp),
                ) {
                    items.chunked(columns).forEach { rowItems ->
                        Row(Modifier.fillMaxWidth()) {
                            rowItems.forEach { app ->
                                Box(Modifier.width(cellWidth), contentAlignment = Alignment.TopCenter) {
                                    AppIcon(app, repository, iconSize, showLabel = true)
                                }
                            }
                        }
                    }
                }
            }

            PageDots(count = pageCount, current = pagerState.currentPage, height = dotsArea)

            Dock(
                apps = dock,
                repository = repository,
                iconSize = iconSize,
                height = dockHeight,
                wide = wide,
            )
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
                val active = index == current
                val color by animateColorAsState(
                    if (active) Color.White else Color.White.copy(alpha = 0.35f),
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

/** الشريط السفلي: مثل الآيفون، زجاج بزوايا ناعمة لاصق تحت */
@Composable
private fun Dock(
    apps: List<AppEntry>,
    repository: AppRepository,
    iconSize: Dp,
    height: Dp,
    wide: Boolean,
) {
    val shape = RoundedCornerShape(32.dp)
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            Modifier
                .padding(horizontal = 10.dp)
                .then(if (wide) Modifier.widthIn(max = 560.dp) else Modifier)
                .fillMaxWidth()
                .height(height)
                .clip(shape)
                .background(Color.White.copy(alpha = 0.14f))
                .border(1.dp, Color.White.copy(alpha = 0.22f), shape)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            apps.forEach { app -> AppIcon(app, repository, iconSize, showLabel = false) }
        }
    }
    Spacer(
        Modifier.height(12.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
    )
}

/** أيقونة واحدة: الشعار الأصلي كامل، Squircle، ظل خفيف، وضغطة نابضة */
@Composable
private fun AppIcon(app: AppEntry, repository: AppRepository, size: Dp, showLabel: Boolean) {
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
    val shadowSize by animateDpAsState(if (pressed) 2.dp else 6.dp, label = "shadow")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
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
        Box(
            Modifier
                .size(size)
                .shadow(
                    elevation = shadowSize,
                    shape = SquircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.35f),
                    spotColor = Color.Black.copy(alpha = 0.55f),
                )
                .clip(SquircleShape)
                .background(Color(0xFF1C1C1E)),
        ) {
            icon?.let {
                Image(
                    bitmap = it,
                    contentDescription = app.label,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds,
                )
            }
        }
        if (showLabel) {
            Spacer(Modifier.height(6.dp))
            BasicText(
                text = app.label,
                modifier = Modifier.width(size + 18.dp).height((LABEL_HEIGHT_DP - 6).dp),
                style = TextStyle(
                    color = Color.White,
                    fontSize = 11.5.sp,
                    textAlign = TextAlign.Center,
                    shadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, 1f), 4f),
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
