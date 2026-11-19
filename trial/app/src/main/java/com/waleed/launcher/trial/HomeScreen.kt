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
import com.waleed.launcher.trial.widgets.WidgetEditorSheet
import com.waleed.launcher.trial.widgets.WidgetEnv
import com.waleed.launcher.trial.widgets.WidgetStore
import com.waleed.launcher.trial.widgets.rememberNow
import com.waleed.launcher.trial.widgets.Placed
import com.waleed.launcher.trial.widgets.Slot
import com.waleed.launcher.trial.widgets.WConfig
import com.waleed.launcher.trial.widgets.WSize
import com.waleed.launcher.trial.widgets.WVariant
import com.waleed.launcher.trial.widgets.WidgetCorner
import com.waleed.launcher.trial.widgets.WidgetSlotView
import com.waleed.launcher.trial.glass.GlassBackdrop
import com.waleed.launcher.trial.glass.LiquidGlass
import com.waleed.launcher.trial.glass.LocalGlassBackdrop
import com.waleed.launcher.trial.glass.WallpaperLayer
import com.waleed.launcher.trial.glass.WallpaperStore
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
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


@Composable
fun HomeScreen(
    repository: AppRepository,
    settings: LauncherSettings,
    widgetStore: WidgetStore,
    location: LocationStore,
    weather: WeatherStore,
    wallpaper: WallpaperStore,
    homePresses: StateFlow<Int>,
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var showSettings by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<EditSlot?>(null) }
    var editingWidget by remember { mutableStateOf<Long?>(null) }
    var moving by remember { mutableStateOf<Long?>(null) }
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
        moving = null
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

    // الخلفية من داخل اللانشر (للزجاج السائل الحقيقي)
    val wallpaperPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { wallpaper.pick(uri) }
    }
    LaunchedEffect(Unit) { wallpaper.load() }

    // الترتيب ثابت مثل شاشة وليد: العمود الجانبي يمين دايماً
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr, LocalWidgetEnv provides widgetEnv) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val screenW = constraints.maxWidth.toFloat()
            val screenH = constraints.maxHeight.toFloat()
            val blurred = wallpaper.blurred
            val backdrop = remember(blurred, screenW, screenH) {
                blurred?.let { GlassBackdrop(it, wallpaper.blurFactor, screenW, screenH, wallpaper.fullWidth, wallpaper.fullHeight) }
            }
            WallpaperLayer(wallpaper)

            CompositionLocalProvider(LocalGlassBackdrop provides backdrop) {
            val wide = maxWidth >= 600.dp
            val widgets = widgetStore.list(wide)
            // الوقت: كل ثانية بس لو فيه عقرب ثواني ظاهر، وإلا كل دقيقة
            val needSeconds = widgets.any { it.slot.current.variant.isAnalog && it.slot.current.seconds != Seconds.OFF }
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
            val rows = (areaHeight / rowHeight).toInt().coerceIn(SIDE_COUNT, 9)
            val gridHeight = rowHeight * rows
            // نوزع الفراغ الزايد فوق وتحت عشان كل شي يجي بالنص
            val verticalGap = ((areaHeight - gridHeight) / 2).coerceAtLeast(0.dp)
            val inset = (cell - iconSize) / 2

            val plans = remember(widgets, layout, gridColumns, rows) { planPages(gridColumns, rows, widgets, layout.pages) }
            val pagerState = rememberPagerState(pageCount = { plans.size })
            val density = LocalDensity.current
            val cellPx = with(density) { cell.toPx() }
            val rowPx = with(density) { rowHeight.toPx() }

            BackHandler {
                when {
                    moving != null -> moving = null
                    editingWidget != null -> editingWidget = null
                    editing != null -> editing = null
                    showSettings -> showSettings = false
                    else -> scope.launch { pagerState.animateScrollToPage(0) }
                }
            }
            LaunchedEffect(Unit) {
                homePresses.drop(1).collect {
                    showSettings = false
                    editing = null
                    editingWidget = null
                    moving = null
                    pagerState.animateScrollToPage(0)
                }
            }

            fun addWidget() {
                val page = pagerState.currentPage
                var target = page
                var spot = freeSpot(gridColumns, rows, widgets.filter { it.page == page }, WSize.S)
                while (spot == null && target < plans.size + 1) {
                    target++
                    spot = freeSpot(gridColumns, rows, widgets.filter { it.page == target }, WSize.S)
                }
                val (x, y) = spot ?: (0 to 0)
                val id = widgetStore.newId()
                widgetStore.add(wide, Placed(id, target, x, y, WSize.S, Slot(listOf(WConfig.preset(WVariant.CLOCK_BLUE, WConfig(WVariant.CLOCK_BLUE))))))
                showSettings = false
                editingWidget = id
                if (target != page) scope.launch { pagerState.animateScrollToPage(target) }
            }

            Column(Modifier.fillMaxSize()) {
                Spacer(Modifier.height(topInset + verticalGap))

                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(gridHeight)
                        // ضغطة طويلة على مكان فاضي = الإعدادات
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = {
                                    if (!tracker.iconPressed && moving == null) {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        showSettings = true
                                    }
                                },
                            )
                        },
                ) {
                    // الصفحات تبدأ من حافة الشاشة نفسها، عشان ما تنقص الأيقونات وأنت تقلب
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = moving == null,
                        modifier = Modifier.width(sidePadding + cell * gridColumns).height(gridHeight),
                    ) { page ->
                        val plan = plans[page]
                        Box(Modifier.padding(start = sidePadding).size(cell * gridColumns, gridHeight)) {
                            plan.apps.forEach { (app, x, y) ->
                                Box(Modifier.offset(cell * x, rowHeight * y).size(cell, rowHeight), contentAlignment = Alignment.Center) {
                                    AppIcon(app, repository, iconSize, style, tracker)
                                }
                            }
                            plan.widgets.forEach { w ->
                                val isMoving = moving == w.id
                                var drag by remember(w.id, isMoving) { mutableStateOf(Offset.Zero) }
                                Box(
                                    Modifier
                                        .zIndex(if (isMoving) 2f else 1f)
                                        .offset(cell * w.x, rowHeight * w.y)
                                        .offset { IntOffset(drag.x.roundToInt(), drag.y.roundToInt()) }
                                        .size(cell * w.size.w, rowHeight * w.size.h)
                                        .padding(inset),
                                ) {
                                    CompositionLocalProvider(LocalNow provides now) {
                                        WidgetSlotView(
                                            w.slot, Modifier.fillMaxSize(), tracker,
                                            onIndexChange = { i -> widgetStore.update(wide, w.id) { it.copy(slot = it.slot.copy(index = i)) } },
                                            onEdit = { if (moving == null) editingWidget = w.id },
                                        )
                                    }
                                    if (isMoving) {
                                        // وضع النقل: اسحب الويدجت لمكانه
                                        Box(
                                            Modifier
                                                .matchParentSize()
                                                .border(2.dp, Color(0xFFE3C285), RoundedCornerShape(WidgetCorner))
                                                .pointerInput(w.id) {
                                                    detectDragGestures(
                                                        onDrag = { change, d ->
                                                            change.consume()
                                                            drag += d
                                                        },
                                                        onDragEnd = {
                                                            val nx = ((w.x * cellPx + drag.x) / cellPx).roundToInt()
                                                            val ny = ((w.y * rowPx + drag.y) / rowPx).roundToInt()
                                                            val others = widgets.filter { it.page == w.page && it.id != w.id }
                                                            if (fits(gridColumns, rows, others, nx, ny, w.size)) {
                                                                widgetStore.update(wide, w.id) { it.copy(x = nx, y = ny) }
                                                            }
                                                            drag = Offset.Zero
                                                        },
                                                    )
                                                },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(verticalGap))
                PageDots(count = plans.size, current = pagerState.currentPage, height = dotsHeight)
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

            // شريط وضع النقل
            moving?.let { id ->
                val w = widgets.firstOrNull { it.id == id }
                Row(
                    Modifier.align(Alignment.TopCenter).padding(top = topInset + 6.dp)
                        .clip(RoundedCornerShape(50)).background(Color(0xEE161618))
                        .border(1.dp, Color(0xFFE3C285).copy(alpha = 0.6f), RoundedCornerShape(50))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    fun movePage(delta: Int) {
                        if (w == null) return
                        val target = (w.page + delta).coerceAtLeast(0)
                        val spot = freeSpot(gridColumns, rows, widgets.filter { it.page == target && it.id != id }, w.size) ?: return
                        widgetStore.update(wide, id) { it.copy(page = target, x = spot.first, y = spot.second) }
                        scope.launch { pagerState.animateScrollToPage(target) }
                    }
                    MoveChip("◀ صفحة") { movePage(-1) }
                    MoveChip("اسحبه لمكانه") { }
                    MoveChip("صفحة ▶") { movePage(1) }
                    MoveChip("تم ✓") { moving = null }
                }
            }

            SettingsSheet(
                visible = showSettings,
                settings = settings,
                previewApps = layout.side.drop(1) + layout.dock,
                repository = repository,
                hasWallpaper = wallpaper.image != null,
                onPickWallpaper = {
                    showSettings = false
                    wallpaperPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onClearWallpaper = { wallpaper.clear() },
                onAddWidget = { addWidget() },
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
                val id = editingWidget
                WidgetEditorSheet(
                    placed = id?.let { i -> widgets.firstOrNull { it.id == i } },
                    cell = cell,
                    rowHeight = rowHeight,
                    inset = inset,
                    maxCols = gridColumns,
                    onChange = { p -> widgetStore.update(wide, p.id) { _ -> clampWidget(p, gridColumns, rows) } },
                    onDelete = {
                        id?.let { widgetStore.remove(wide, it) }
                        editingWidget = null
                    },
                    onMove = {
                        moving = id
                        editingWidget = null
                    },
                    onClose = { editingWidget = null },
                )
            }
            }
        }
    }
}

@Composable
private fun MoveChip(text: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        BasicText(text, style = TextStyle(color = Color(0xFFE3C285), fontSize = 13.sp, fontWeight = FontWeight.Medium))
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
    LiquidGlass(Modifier.width(glassWidth), glassWidth / 2) {
        Column(
            Modifier.width(glassWidth).padding(vertical = 7.dp + gap / 2),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            apps.forEachIndexed { i, app ->
                AppIcon(app, repository, iconSize, style, tracker, onLongPress = { onEdit(i, app) })
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
    onEdit: (Int, AppEntry) -> Unit,
) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        LiquidGlass(Modifier.width(width).height(height), height * 0.36f) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                apps.take(DOCK_COUNT).forEachIndexed { i, app ->
                    AppIcon(app, repository, iconSize, style, tracker, onLongPress = { onEdit(i, app) })
                }
            }
        }
    }
}
