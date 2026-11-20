package com.waleed.launcher.trial

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.waleed.launcher.trial.fold.FoldTransitionHost
import com.waleed.launcher.trial.glass.GlassBackdrop
import com.waleed.launcher.trial.glass.GlassTone
import com.waleed.launcher.trial.glass.LiquidGlass
import com.waleed.launcher.trial.glass.LocalGlassBackdrop
import com.waleed.launcher.trial.glass.WallpaperLayer
import com.waleed.launcher.trial.glass.WallpaperStore
import com.waleed.launcher.trial.widgets.LocalNow
import com.waleed.launcher.trial.widgets.LocalWidgetEnv
import com.waleed.launcher.trial.widgets.LocationStore
import com.waleed.launcher.trial.widgets.Placed
import com.waleed.launcher.trial.widgets.Seconds
import com.waleed.launcher.trial.widgets.Slot
import com.waleed.launcher.trial.widgets.WConfig
import com.waleed.launcher.trial.widgets.WSize
import com.waleed.launcher.trial.widgets.WeatherStore
import com.waleed.launcher.trial.widgets.WidgetBack
import com.waleed.launcher.trial.widgets.WidgetContextMenu
import com.waleed.launcher.trial.widgets.WidgetCorner
import com.waleed.launcher.trial.widgets.WidgetEnv
import com.waleed.launcher.trial.widgets.WidgetGallery
import com.waleed.launcher.trial.widgets.WidgetSlotView
import com.waleed.launcher.trial.widgets.WidgetStore
import com.waleed.launcher.trial.widgets.WidgetStudio
import com.waleed.launcher.trial.widgets.rememberNow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private const val SIDE_COUNT = 4
private const val DOCK_COUNT = 6
private val Gold = Color(0xFFE3C285)

/** الاستوديو مفتوح لويدجت معين، أو لتصميم جديد */
private data class StudioState(val widgetId: Long?, val cfg: WConfig)

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
    val tracker = remember { PressTracker() }
    val style = settings.iconStyle

    // اللوحات والأوضاع
    var showSettings by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<EditSlot?>(null) }   // تبديل تطبيق بالعمود أو الشريط
    var arrange by remember { mutableStateOf(false) }             // وضع الترتيب
    var menuFor by remember { mutableStateOf<Long?>(null) }       // قائمة الضغطة الطويلة
    var flipped by remember { mutableStateOf<Long?>(null) }       // الويدجت المقلوب للتعديل السريع
    var galleryOpen by remember { mutableStateOf(false) }
    var studio by remember { mutableStateOf<StudioState?>(null) }
    val bounds = remember { mutableStateMapOf<Long, Rect>() }

    fun closeAll() {
        showSettings = false
        editing = null
        arrange = false
        menuFor = null
        flipped = null
        galleryOpen = false
        studio = null
    }

    // نحدث القائمة كل ما ترجع للشاشة الرئيسية (لو ثبت أو حذفت تطبيق)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        scope.launch {
            val fresh = repository.loadApps()
            if (fresh.map { it.key to it.label } != apps.map { it.key to it.label }) apps = fresh
        }
        location.refresh()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { closeAll() }

    // الموقع للصلاة والطقس: نطلب الإذن مرة وحدة بس
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) location.refresh()
    }
    val widgetEnv = remember { WidgetEnv(location, weather) { permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION) } }

    // الخلفية من داخل اللانشر (للزجاج السائل الحقيقي)
    val wallpaperPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { wallpaper.pick(uri) }
    }
    LaunchedEffect(Unit) { wallpaper.load() }

    val layout = remember(apps, settings.sideKeys, settings.dockKeys) {
        repository.arrange(apps, settings.sideKeys, settings.dockKeys)
    }

    // نبدل التطبيق بالخانة. لو التطبيق موجود بنفس المكان، نبدلهم ببعض
    fun pick(slot: EditSlot, chosen: AppEntry) {
        val keys = (if (slot.inSide) layout.side else layout.dock).map { it.key }.toMutableList()
        val existing = keys.indexOf(chosen.key)
        if (existing >= 0) keys[existing] = keys[slot.index]
        keys[slot.index] = chosen.key
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
            val screenW = constraints.maxWidth.toFloat()
            val screenH = constraints.maxHeight.toFloat()
            val blurred = wallpaper.blurred
            val backdrop = remember(blurred, screenW, screenH) {
                blurred?.let { GlassBackdrop(it, wallpaper.blurFactor, screenW, screenH, wallpaper.fullWidth, wallpaper.fullHeight) }
            }
            val screenMaxW = maxWidth
            val screenMaxH = maxHeight
            // انتقال الطي بالزجاج المضبب: يلف الشاشة كلها (الخلفية + الأيقونات + الويدجت)
            FoldTransitionHost(
                inner = screenMaxW >= 600.dp,
                showAngle = true,
                background = { WallpaperLayer(wallpaper) },
            ) {
            CompositionLocalProvider(LocalGlassBackdrop provides backdrop) {
                val wide = screenMaxW >= 600.dp
                val widgets = widgetStore.list(wide)
                // الوقت: كل ثانية بس لو فيه عقرب ثواني ظاهر، وإلا كل دقيقة
                val needSeconds = widgets.any { it.slot.current.variant.isAnalog && it.slot.current.seconds != Seconds.OFF }
                val now = rememberNow(needSeconds)
                val gridColumns = if (wide) 6 else 4

                // الشبكة + خانة للعمود الجانبي (مثل ترتيب وليد)
                val sidePadding = if (wide) screenMaxW * 0.05f else 16.dp
                val sideEdgeMargin = 6.dp
                val cell = (screenMaxW - sidePadding - sideEdgeMargin) / (gridColumns + 1)
                // حجم الأيقونة: حوالي ٧٨٪ من الخانة، مثل نسبة الآيفون
                val iconSize = minOf(cell * 0.78f, 84.dp)
                // العمود الجانبي: بالخارجية نفس حجم الأيقونات، وبالداخلية أصغر شوي عشان يتميز
                val sideIcon = if (wide) iconSize * 0.86f else iconSize
                val sideGlassWidth = sideIcon + 14.dp
                val sideGap = sideIcon * 0.24f
                // الشريط السفلي: أصغر شوي من أيقونات الشاشة، وأنحف
                val dockIcon = iconSize * 0.86f
                val dockHeight = dockIcon + 20.dp
                val dockWidth = if (wide) minOf(screenMaxW * 0.66f, 640.dp) else screenMaxW - 32.dp
                val dotsHeight = 22.dp

                val rowHeight = cell
                val areaHeight = screenMaxH - topInset - dotsHeight - dockHeight - 14.dp - bottomInset
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

                // السحب والتكبير بوضع الترتيب
                var dragId by remember { mutableStateOf<Long?>(null) }
                var dragOffset by remember { mutableStateOf(Offset.Zero) }
                var resizeId by remember { mutableStateOf<Long?>(null) }
                var resizeDelta by remember { mutableStateOf(Offset.Zero) }

                fun othersOnPage(w: Placed) = widgets.filter { it.page == w.page && it.id != w.id }

                /** أقرب مقاس مسموح للمقاس اللي سحبته، ويركب بمكانه */
                fun resizeTarget(w: Placed, delta: Offset): WSize? {
                    val cw = ((w.size.w * cellPx + delta.x) / cellPx).roundToInt().coerceIn(1, gridColumns)
                    val ch = ((w.size.h * rowPx + delta.y) / rowPx).roundToInt().coerceIn(1, rows)
                    return WSize.entries
                        .filter { it.w <= gridColumns && it.h <= rows }
                        .sortedBy { abs(it.w - cw) + abs(it.h - ch) }
                        .firstOrNull { z -> fits(gridColumns, rows, othersOnPage(w), w.x.coerceAtMost(gridColumns - z.w), w.y.coerceAtMost(rows - z.h), z) }
                }

                fun applySize(w: Placed, z: WSize) {
                    widgetStore.update(wide, w.id) { it.copy(size = z, x = it.x.coerceAtMost(gridColumns - z.w), y = it.y.coerceAtMost(rows - z.h)) }
                }

                fun addWidget(cfg: WConfig, size: WSize) {
                    val page = pagerState.currentPage
                    var target = page
                    var spot = freeSpot(gridColumns, rows, widgets.filter { it.page == page }, size)
                    while (spot == null && target < plans.size + 1) {
                        target++
                        spot = freeSpot(gridColumns, rows, widgets.filter { it.page == target }, size)
                    }
                    val (x, y) = spot ?: (0 to 0)
                    widgetStore.add(wide, Placed(widgetStore.newId(), target, x, y, size, Slot(listOf(cfg))))
                    galleryOpen = false
                    if (target != page) scope.launch { pagerState.animateScrollToPage(target) }
                }

                BackHandler {
                    when {
                        studio != null -> studio = null
                        galleryOpen -> galleryOpen = false
                        menuFor != null -> menuFor = null
                        flipped != null -> flipped = null
                        showSettings -> showSettings = false
                        editing != null -> editing = null
                        arrange -> arrange = false
                        else -> scope.launch { pagerState.animateScrollToPage(0) }
                    }
                }
                LaunchedEffect(Unit) {
                    homePresses.drop(1).collect {
                        closeAll()
                        pagerState.animateScrollToPage(0)
                    }
                }

                // اهتزاز خفيف بوضع الترتيب (يشتغل بس وهو مفتوح)
                val wobble = if (arrange) {
                    val t = rememberInfiniteTransition(label = "wobble")
                    t.animateFloat(-0.8f, 0.8f, infiniteRepeatable(tween(150, easing = LinearEasing), RepeatMode.Reverse), label = "w").value
                } else {
                    0f
                }

                Column(Modifier.fillMaxSize()) {
                    Spacer(Modifier.height(topInset + verticalGap))

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(gridHeight)
                            // ضغطة طويلة على مكان فاضي = وضع الترتيب (مثل الآيفون)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { if (arrange && dragId == null) arrange = false },
                                    onLongPress = {
                                        if (!tracker.iconPressed && !arrange) {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            flipped = null
                                            arrange = true
                                        }
                                    },
                                )
                            },
                    ) {
                        // الصفحات تبدأ من حافة الشاشة نفسها، عشان ما تنقص الأيقونات وأنت تقلب
                        HorizontalPager(
                            state = pagerState,
                            userScrollEnabled = dragId == null && resizeId == null,
                            modifier = Modifier.width(sidePadding + cell * gridColumns).height(gridHeight),
                        ) { page ->
                            val plan = plans[page]
                            Box(Modifier.padding(start = sidePadding).size(cell * gridColumns, gridHeight)) {
                                plan.apps.forEach { (app, x, y) ->
                                    Box(Modifier.offset(cell * x, rowHeight * y).size(cell, rowHeight), contentAlignment = Alignment.Center) {
                                        AppIcon(app, repository, iconSize, style, tracker)
                                    }
                                }
                                plan.widgets.forEach { w -> key(w.id) {
                                    val isDragging = dragId == w.id
                                    val isFlipped = flipped == w.id
                                    val flip by animateFloatAsState(
                                        if (isFlipped) 180f else 0f,
                                        spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
                                        label = "flip",
                                    )
                                    Box(
                                        Modifier
                                            .zIndex(if (isDragging || isFlipped) 3f else 1f)
                                            .offset(cell * w.x, rowHeight * w.y)
                                            .offset { if (isDragging) IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) else IntOffset.Zero }
                                            .size(cell * w.size.w, rowHeight * w.size.h)
                                            .padding(inset)
                                            .onGloballyPositioned { bounds[w.id] = it.boundsInRoot() }
                                            .graphicsLayer {
                                                rotationZ = if (arrange && !isDragging) (if (w.id % 2L == 0L) wobble else -wobble) else 0f
                                                rotationY = flip
                                                cameraDistance = 14f * density.density
                                                val s = if (isDragging) 1.05f else 1f
                                                scaleX = s
                                                scaleY = s
                                            },
                                    ) {
                                        if (flip <= 90f) {
                                            CompositionLocalProvider(LocalNow provides now) {
                                                WidgetSlotView(
                                                    w.slot, Modifier.fillMaxSize(), tracker,
                                                    interactive = !arrange,
                                                    onIndexChange = { i -> widgetStore.update(wide, w.id) { it.copy(slot = it.slot.copy(index = i)) } },
                                                    onLongPress = {
                                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        menuFor = w.id
                                                    },
                                                )
                                            }
                                        } else {
                                            // ظهر الويدجت: تعديل سريع
                                            Box(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }) {
                                                LiquidGlass(Modifier.fillMaxSize(), WidgetCorner, tone = GlassTone.DARK) {
                                                    WidgetBack(
                                                        cfg = w.slot.current,
                                                        designs = widgetStore.designs,
                                                        onChange = { c -> widgetStore.update(wide, w.id) { it.copy(slot = it.slot.withCurrent(c)) } },
                                                        onStudio = {
                                                            flipped = null
                                                            studio = StudioState(w.id, w.slot.current)
                                                        },
                                                        onDone = { flipped = null },
                                                    )
                                                }
                                            }
                                        }

                                        if (arrange) {
                                            // السحب: تنقل الويدجت، ولو حطيته فوق ويدجت بنفس المقاس يصيرون مكدس
                                            Box(
                                                Modifier
                                                    .matchParentSize()
                                                    .pointerInput(w.id, plan) {
                                                        detectDragGestures(
                                                            onDragStart = {
                                                                dragId = w.id
                                                                dragOffset = Offset.Zero
                                                            },
                                                            onDrag = { change, d ->
                                                                change.consume()
                                                                dragOffset += d
                                                            },
                                                            onDragEnd = {
                                                                val nx = ((w.x * cellPx + dragOffset.x) / cellPx).roundToInt()
                                                                val ny = ((w.y * rowPx + dragOffset.y) / rowPx).roundToInt()
                                                                val others = othersOnPage(w)
                                                                val target = others.firstOrNull { it.x == nx && it.y == ny && it.size == w.size }
                                                                if (target != null) {
                                                                    widgetStore.replace(
                                                                        wide,
                                                                        widgets.filterNot { it.id == w.id }.map {
                                                                            if (it.id == target.id) it.copy(slot = it.slot.copy(items = it.slot.items + w.slot.items)) else it
                                                                        },
                                                                    )
                                                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                } else if (fits(gridColumns, rows, others, nx, ny, w.size)) {
                                                                    widgetStore.update(wide, w.id) { it.copy(x = nx, y = ny) }
                                                                }
                                                                dragId = null
                                                                dragOffset = Offset.Zero
                                                            },
                                                            onDragCancel = {
                                                                dragId = null
                                                                dragOffset = Offset.Zero
                                                            },
                                                        )
                                                    },
                                            )
                                            // زر الحذف
                                            Box(
                                                Modifier
                                                    .align(Alignment.TopStart)
                                                    .offset((-8).dp, (-8).dp)
                                                    .size(26.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xF0303034))
                                                    .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                                                    .clickable { widgetStore.remove(wide, w.id) },
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                BasicText("−", style = TextStyle(color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold))
                                            }
                                            // مقبض المقاس بالزاوية: اسحبه تكبر وتصغر
                                            Box(
                                                Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .offset(8.dp, 8.dp)
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(Gold)
                                                    .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                                                    .pointerInput(w.id, plan) {
                                                        detectDragGestures(
                                                            onDragStart = {
                                                                resizeId = w.id
                                                                resizeDelta = Offset.Zero
                                                            },
                                                            onDrag = { change, d ->
                                                                change.consume()
                                                                resizeDelta += d
                                                            },
                                                            onDragEnd = {
                                                                resizeTarget(w, resizeDelta)?.let { applySize(w, it) }
                                                                resizeId = null
                                                                resizeDelta = Offset.Zero
                                                            },
                                                            onDragCancel = {
                                                                resizeId = null
                                                                resizeDelta = Offset.Zero
                                                            },
                                                        )
                                                    },
                                            )
                                        }
                                    }

                                    // إطار يوريك المقاس الجديد وأنت تسحب المقبض
                                    if (resizeId == w.id) {
                                        resizeTarget(w, resizeDelta)?.let { z ->
                                            Box(
                                                Modifier
                                                    .zIndex(4f)
                                                    .offset(cell * w.x.coerceAtMost(gridColumns - z.w), rowHeight * w.y.coerceAtMost(rows - z.h))
                                                    .size(cell * z.w, rowHeight * z.h)
                                                    .padding(inset)
                                                    .border(2.dp, Gold, RoundedCornerShape(WidgetCorner)),
                                            )
                                        }
                                    }
                                } }
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
                Box(Modifier.align(Alignment.CenterEnd).padding(end = sideEdgeMargin)) {
                    SideColumn(layout.side, repository, sideIcon, sideGlassWidth, sideGap, style, tracker) { i, app ->
                        editing = EditSlot(inSide = true, index = i, current = app)
                    }
                }

                // شريط وضع الترتيب فوق
                if (arrange) {
                    Row(
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = topInset + 6.dp, start = 12.dp, end = 12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TopPill("+ ويدجت") { galleryOpen = true }
                        Spacer(Modifier.width(8.dp))
                        TopPill("الإعدادات") { showSettings = true }
                        Spacer(Modifier.weight(1f))
                        TopPill("تم", filled = true) { arrange = false }
                    }
                }

                // قائمة الضغطة الطويلة على الويدجت
                val menuPlaced = menuFor?.let { id -> widgets.firstOrNull { it.id == id } }
                WidgetContextMenu(
                    placed = menuPlaced,
                    anchor = menuFor?.let { bounds[it] },
                    screenWidthPx = screenW,
                    screenHeightPx = screenH,
                    maxCols = gridColumns,
                    canFit = { z ->
                        menuPlaced != null && fits(
                            gridColumns, rows, othersOnPage(menuPlaced),
                            menuPlaced.x.coerceAtMost(gridColumns - z.w), menuPlaced.y.coerceAtMost(rows - z.h), z,
                        )
                    },
                    onSize = { z -> menuPlaced?.let { applySize(it, z) } },
                    onEdit = {
                        flipped = menuFor
                        menuFor = null
                    },
                    onStudio = {
                        menuPlaced?.let { studio = StudioState(it.id, it.slot.current) }
                        menuFor = null
                    },
                    onArrange = {
                        menuFor = null
                        arrange = true
                    },
                    onToggleRotate = {
                        menuPlaced?.let { p -> widgetStore.update(wide, p.id) { it.copy(slot = it.slot.copy(autoRotate = !it.slot.autoRotate)) } }
                    },
                    onRemoveFromStack = {
                        menuPlaced?.let { p ->
                            val items = p.slot.items.toMutableList().also { it.removeAt(p.slot.index.coerceIn(0, it.size - 1)) }
                            widgetStore.update(wide, p.id) { it.copy(slot = it.slot.copy(items = items, index = (p.slot.index - 1).coerceAtLeast(0))) }
                        }
                        menuFor = null
                    },
                    onDelete = {
                        menuFor?.let { widgetStore.remove(wide, it) }
                        menuFor = null
                    },
                    onDismiss = { menuFor = null },
                )

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
                    onOpenStudio = {
                        showSettings = false
                        studio = StudioState(null, WConfig.preset(com.waleed.launcher.trial.widgets.WVariant.CLOCK_BLUE, WConfig(com.waleed.launcher.trial.widgets.WVariant.CLOCK_BLUE)))
                    },
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
                    WidgetGallery(
                        visible = galleryOpen,
                        designs = widgetStore.designs,
                        maxCols = gridColumns,
                        cell = cell,
                        rowHeight = rowHeight,
                        inset = inset,
                        onAdd = { cfg, size -> addWidget(cfg, size) },
                        onClose = { galleryOpen = false },
                    )

                    val st = studio
                    val studioPlaced = st?.widgetId?.let { id -> widgets.firstOrNull { it.id == id } }
                    val studioSize = studioPlaced?.size ?: WSize.S
                    WidgetStudio(
                        cfg = st?.cfg,
                        previewWidth = cell * studioSize.w - inset * 2,
                        previewHeight = rowHeight * studioSize.h - inset * 2,
                        onChange = { c ->
                            studio = st?.copy(cfg = c)
                            st?.widgetId?.let { id -> widgetStore.update(wide, id) { it.copy(slot = it.slot.withCurrent(c)) } }
                        },
                        onSaveDesign = { c -> widgetStore.addDesign(c) },
                        onClose = { studio = null },
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun TopPill(text: String, filled: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (filled) Gold else Color(0xE6161618))
            .border(1.dp, Gold.copy(alpha = if (filled) 1f else 0.6f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        BasicText(text, style = TextStyle(color = if (filled) Color.Black else Gold, fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
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
