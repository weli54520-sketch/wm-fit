package com.waleed.launcher.trial.widgets

import android.content.Intent
import android.provider.AlarmClock
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.waleed.launcher.trial.PressTracker

val WidgetCorner = 28.dp

/** الزجاج حول الويدجت: داكن، شفاف، أو متلائم مع الخلفية + حدود رفيعة مضيئة */
@Composable
fun GlassPanel(cfg: WConfig, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val shape = RoundedCornerShape(WidgetCorner)
    val halo = if (cfg.glow) {
        Modifier.drawBehind {
            val r = WidgetCorner.toPx()
            drawRoundRect(
                Color.White.copy(alpha = 0.07f), Offset(-2.5f, -2.5f), Size(size.width + 5f, size.height + 5f),
                CornerRadius(r + 2.5f), style = Stroke(3f),
            )
        }
    } else {
        Modifier
    }
    val bg = when (cfg.glass) {
        Glass.DARK -> Modifier.background(Brush.verticalGradient(listOf(Color(0xD91C1C1F), Color(0xF00A0A0B))))
        Glass.CLEAR -> Modifier
            .background(Color.Black.copy(alpha = 0.16f))
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.06f))))
        Glass.BLEND -> Modifier
    }
    val border = when {
        cfg.glow -> Modifier.border(
            1.dp,
            Brush.linearGradient(listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.32f))),
            shape,
        )
        cfg.glass == Glass.BLEND -> Modifier
        else -> Modifier.border(1.dp, Color.White.copy(alpha = 0.12f), shape)
    }
    Box(modifier.then(halo).clip(shape).then(bg).then(border)) {
        if (cfg.glass != Glass.BLEND) {
            // لمعة خفيفة فوق
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0f to Color.White.copy(alpha = 0.07f), 0.4f to Color.Transparent),
                ),
            )
        }
        content()
    }
}

@Composable
fun WidgetContent(cfg: WConfig) {
    when (cfg.variant.type) {
        WType.CLOCK -> when (cfg.variant) {
            WVariant.CLOCK_THIN -> DigitalClock(cfg, bold = false)
            WVariant.CLOCK_BOLD -> DigitalClock(cfg, bold = true)
            WVariant.CLOCK_FLIP -> FlipClock(cfg)
            WVariant.CLOCK_WORDS -> WordsClock(cfg)
            WVariant.CLOCK_HALF -> HalfDialClock(cfg)
            else -> AnalogClock(cfg)
        }
        WType.DATE -> DateWidget(cfg)
        WType.PRAYER -> PrayerWidget(cfg)
        WType.WEATHER -> WeatherWidget(cfg)
        WType.BATTERY -> BatteryWidget(cfg)
        WType.CONTROLS -> ControlsWidget(cfg)
        WType.TEXT -> TextWidget(cfg)
    }
}

/** خانة واحدة: مكدس ويدجت تقلبه لفوق وتحت */
@Composable
fun WidgetSlotView(
    slot: Slot,
    modifier: Modifier,
    tracker: PressTracker,
    onIndexChange: (Int) -> Unit,
    onEdit: () -> Unit,
) {
    val context = LocalContext.current
    val pager = rememberPagerState(initialPage = slot.index.coerceIn(0, slot.items.size - 1), pageCount = { slot.items.size })
    val latestIndex = rememberUpdatedState(slot.index)
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.collect { if (it != latestIndex.value) onIndexChange(it) }
    }
    LaunchedEffect(slot.index, slot.items.size) {
        val target = slot.index.coerceIn(0, slot.items.size - 1)
        if (pager.currentPage != target) pager.animateScrollToPage(target)
    }

    fun open(cfg: WConfig) {
        val intent = when (cfg.variant.type) {
            WType.CLOCK -> Intent(AlarmClock.ACTION_SHOW_ALARMS)
            WType.DATE -> Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR)
            WType.BATTERY -> Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
            WType.TEXT -> null
            else -> null
        }
        if (intent == null) {
            if (cfg.variant.type == WType.TEXT) onEdit()
            return
        }
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    Box(modifier) {
        VerticalPager(state = pager, modifier = Modifier.fillMaxSize(), pageSpacing = 14.dp, beyondViewportPageCount = 0) { page ->
            val cfg = slot.items[page]
            GlassPanel(
                cfg,
                Modifier
                    .fillMaxSize()
                    .pointerInput(cfg) {
                        detectTapGestures(
                            onPress = {
                                tracker.iconPressed = true
                                tryAwaitRelease()
                                tracker.iconPressed = false
                            },
                            onTap = { open(cfg) },
                            onLongPress = { onEdit() },
                        )
                    },
            ) { WidgetContent(cfg) }
        }
        if (slot.items.size > 1) {
            Column(
                Modifier.align(Alignment.CenterEnd).padding(end = 7.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                repeat(slot.items.size) { i ->
                    Box(
                        Modifier.size(4.dp).clip(CircleShape)
                            .background(if (i == pager.currentPage) Color.White else Color.White.copy(alpha = 0.3f)),
                    )
                }
            }
        }
    }
}

/** منطقة الويدجت فوق الأيقونات بالصفحة الأولى */
@Composable
fun WidgetArea(
    store: WidgetStore,
    wide: Boolean,
    cell: Dp,
    rowHeight: Dp,
    inset: Dp,
    tracker: PressTracker,
    onEdit: (Int) -> Unit,
) {
    val slots = store.slots(wide)

    @Composable
    fun SlotBox(i: Int, cols: Int) {
        val slot = slots.getOrNull(i) ?: return
        WidgetSlotView(
            slot,
            Modifier.size(cell * cols, rowHeight * 2).padding(inset),
            tracker,
            onIndexChange = { store.update(wide, i, slot.copy(index = it)) },
            onEdit = { onEdit(i) },
        )
    }

    if (wide) {
        Row(Modifier.height(rowHeight * 2)) {
            SlotBox(0, 4)
            SlotBox(1, 2)
        }
    } else {
        Column {
            Row(Modifier.height(rowHeight * 2)) { SlotBox(0, 4) }
            Row(Modifier.height(rowHeight * 2)) {
                SlotBox(1, 2)
                SlotBox(2, 2)
            }
        }
    }
}

/** كم صف ياخذ الويدجت بالصفحة الأولى */
fun widgetRows(wide: Boolean) = if (wide) 2 else 4

@Suppress("unused")
private val unusedW = Modifier.width(0.dp)
