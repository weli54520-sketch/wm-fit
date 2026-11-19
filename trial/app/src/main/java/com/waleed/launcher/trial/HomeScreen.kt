package com.waleed.launcher.trial

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.waleed.launcher.trial.widgets.LocalNow
import com.waleed.launcher.trial.widgets.LocalWidgetEnv
import com.waleed.launcher.trial.widgets.LocationStore
import com.waleed.launcher.trial.widgets.Seconds
import com.waleed.launcher.trial.widgets.WeatherStore
import com.waleed.launcher.trial.widgets.WidgetArea
import com.waleed.launcher.trial.widgets.WidgetEditorSheet
import com.waleed.launcher.trial.widgets.WidgetEnv
import com.waleed.launcher.trial.widgets.WidgetStore
import com.waleed.launcher.trial.widgets.rememberNow
import com.waleed.launcher.trial.widgets.widgetRows
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
fun HomeScreen(
    repository: AppRepository,
    settings: LauncherSettings,
    widgetStore: WidgetStore,
    location: LocationStore,
    weather: WeatherStore,
    homePresses: StateFlow<Int>,
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var showSettings by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<EditSlot?>(null) }
    var editingWidget by remember { mutableStateOf<Int?>(null) }
    val tracker = remember { PressTracker() }
    val style = settings.iconStyle

    // نحدث القائمة كل ما ترجع للشاشة الرئيسية (لو ثبت أو حذفت تطبيق)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        scope.launch {
            val fresh = repository.loadApps()
            if (fresh.map { it.key to it.label } != apps.map { it.key to it.label }) apps = fresh
        }
    }
    // الموقع للصلاة والطقس: نطلب الإذن مرة وحدة بس
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) location.refresh()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { location.refresh() }
    val widgetEnv = remember { WidgetEnv(location, weather) { permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION) } }

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        showSettings = false
        editingWidget = null
        editing = null
    }

    val layout = remember(apps, settings.sideKeys, settings.dockKeys) {
        repository.arrange(apps, settings.sideKeys, settings.dockKeys)
    }

    // نبدل التطبيق بالخانة. لو التطبيق موجود بنفس المكان، نبدلهم ببعض
    fun pick(slot: EditSlot, chosen: AppEntry) {
        val keys = (if (slot.inSide) layout.side else layout.dock).map { it.key }.toMutableList()
        val existing = keys.indexOf(chosen.key)
        if (existing >= 0) keys[existing] = keys[slot.index]
        keys[slot.index] = chosen.key
        // لو كان بالمكان الثاني، نشيله منه
        if (slot.inSide) {
            settings.updateSide(keys)
            if (chosen.key in layout.dock.map { it.key }) {
                settings.updateDock(layout.dock.map { it.key }.map { if (it == chosen.key) slot.current.key else it })
            }
        } else {
            settings.updateDock(keys)
            if (chosen.key in layout.side.map { it.key }) {
                settings.updateSide(layout.side.map { it.key }.map { if (it == chosen.key) slot.current.key else it })
            }
        }
        editing = null
    }
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // الترتيب ثابت مثل شاشة وليد: العمود الجانبي يمين دايماً
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr, LocalWidgetEnv provides widgetEnv) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= 600.dp
            // الوقت: كل ثانية بس لو فيه عقرب ثواني ظاهر، وإلا كل دقيقة
            val needSeconds = widgetStore.slots(wide).any { it.current.variant.isAnalog && it.current.seconds != Seconds.OFF }
            val now = rememberNow(needSeconds)
            val gridColumns = if (wide) 6 else 4

            // الشبكة + خانة للعمود الجانبي (مثل ترتيب وليد)
            val sidePadding = if (wide) maxWidth * 0.05f else 16.dp
            val sideEdgeMargin = 6.dp
            val cell = (maxWidth - sidePadding - sideEdgeMargin) / (gridColumns + 1)

            // حجم الأيقونة: حوالي ٧٨٪ من الخانة، مثل نسبة الآيفون
            val iconSize = minOf(cell * 0.78f, 84.dp)

            // العمود الجانبي: بالخارجية نفس حجم الأيقونات،
            // وبالداخلية أصغر شوي عشان يتميز
            val sideIcon = if (wide) iconSize * 0.86f else iconSize
            val sideGlassWidth = sideIcon + 14.dp
            val sideGap = sideIcon * 0.24f

            // الشريط السفلي: أصغر شوي من أيقونات الشاشة، وأنحف
            val dockIcon = iconSize * 0.86f
            val dockHeight = dockIcon + 20.dp
            val dockWidth = if (wide) minOf(maxWidth * 0.66f, 640.dp) else maxWidth - 32.dp
            val dotsHeight = 22.dp

            val rowHeight = cell
            val areaHeight = maxHeight - topInset - dotsHeight - dockHeight - 14.dp - bottomInset
            val rows = (areaHeight / rowHeight).toInt().coerceIn(SIDE_COUNT, 8)
            val gridHeight = rowHeight * rows
            // نوزع الفراغ الزايد فوق وتحت عشان كل شي يجي بالنص
            val verticalGap = ((areaHeight - gridHeight) / 2).coerceAtLeast(0.dp)

            val perPage = rows * gridColumns
            // الصفحة الأولى فيها الويدجت فوق، والباقي أيقونات
            val firstCap = (rows - widgetRows(wide)).coerceAtLeast(0) * gridColumns
            val restCount = (layout.pages.size - firstCap).coerceAtLeast(0)
            val pageCount = 1 + (restCount + perPage - 1) / perPage
            val inset = (cell - iconSize) / 2
            val pagerState = rememberPagerState(pageCount = { pageCount })

            BackHandler {
                if (editingWidget != null) editingWidget = null
                else if (editing != null) editing = null
                else if (showSettings) showSettings = false
                else scope.launch { pagerState.animateScrollToPage(0) }
            }
            LaunchedEffect(Unit) {
                homePresses.drop(1).collect {
                    showSettings = false
                    editing = null
                    editingWidget = null
                    pagerState.animateScrollToPage(0)
                }
            }

            Column(Modifier.fillMaxSize()) {
                Spacer(Modifier.height(topInset + verticalGap))

                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(gridHeight)
                        .padding(start = sidePadding)
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
                        val start = if (page == 0) 0 else firstCap + (page - 1) * perPage
                        val end = if (page == 0) firstCap else start + perPage
                        val items = layout.pages.subList(
                            minOf(start, layout.pages.size),
                            minOf(end, layout.pages.size),
                        )
                        Column(Modifier.fillMaxSize()) {
                            if (page == 0) {
                                CompositionLocalProvider(LocalNow provides now) {
                                    WidgetArea(widgetStore, wide, cell, rowHeight, inset, tracker) { editingWidget = it }
                                }
                            }
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
                }

                Spacer(Modifier.height(verticalGap))
                PageDots(count = pageCount, current = pagerState.currentPage, height = dotsHeight)
                Dock(layout.dock, repository, dockIcon, style, tracker, dockHeight, dockWidth) { i, app ->
                    editing = EditSlot(inSide = false, index = i, current = app)
                }
                Spacer(Modifier.height(14.dp + bottomInset))
            }

            // العمود الجانبي: لاصق يمين الشاشة، بالنص بالضبط
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = sideEdgeMargin),
            ) {
                SideColumn(layout.side, repository, sideIcon, sideGlassWidth, sideGap, style, tracker) { i, app ->
                    editing = EditSlot(inSide = true, index = i, current = app)
                }
            }

            SettingsSheet(
                visible = showSettings,
                settings = settings,
                previewApps = layout.side.drop(1) + layout.dock,
                repository = repository,
                onClose = { showSettings = false },
            )

            AppPickerSheet(
                slot = editing,
                allApps = apps,
                repository = repository,
                style = style,
                onPick = { chosen -> editing?.let { pick(it, chosen) } },
                onClose = { editing = null },
            )

            CompositionLocalProvider(LocalNow provides now) {
                val idx = editingWidget
                WidgetEditorSheet(
                    slot = idx?.let { widgetStore.slots(wide).getOrNull(it) },
                    slotWidth = cell * (if (idx == 0) 4 else 2) - inset * 2,
                    slotHeight = rowHeight * 2 - inset * 2,
                    onChange = { newSlot -> idx?.let { widgetStore.update(wide, it, newSlot) } },
                    onClose = { editingWidget = null },
                )
            }
        }
    }
}

/** العمود الجانبي يمين: زجاج حقيقي، ٤ تطبيقات، حجمه ثابت */
@Composable
private fun SideColumn(
    apps: List<AppEntry>,
    repository: AppRepository,
    iconSize: Dp,
    glassWidth: Dp,
    gap: Dp,
    style: IconStyle,
    tracker: PressTracker,
    onEdit: (Int, AppEntry) -> Unit,
) {
    val shape = RoundedCornerShape(glassWidth / 2)
    Column(
        Modifier
            .width(glassWidth)
            .clip(shape)
            .background(GlassFill)
            .border(1.dp, GlassBorder, shape)
            .padding(vertical = 7.dp + gap / 2),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        apps.forEachIndexed { i, app ->
            AppIcon(app, repository, iconSize, style, tracker, onLongPress = { onEdit(i, app) })
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
    onEdit: (Int, AppEntry) -> Unit,
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
            apps.take(DOCK_COUNT).forEachIndexed { i, app ->
                AppIcon(app, repository, iconSize, style, tracker, onLongPress = { onEdit(i, app) })
            }
        }
    }
}
