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
import com.waleed.launcher.trial.glass.GlassTone
import com.waleed.launcher.trial.glass.LiquidGlass

val WidgetCorner = 28.dp

/** خلفية الويدجت: زجاج سائل حقيقي، أو تدرجات فخمة */
@Composable
fun GlassPanel(cfg: WConfig, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val shape = RoundedCornerShape(WidgetCorner)
    when (cfg.glass) {
        Glass.LIQUID, Glass.LIQUID_TINT, Glass.DARK -> LiquidGlass(
            modifier,
            WidgetCorner,
            tone = when (cfg.glass) {
                Glass.LIQUID -> GlassTone.CLEAR
                Glass.LIQUID_TINT -> GlassTone.TINT
                else -> GlassTone.DARK
            },
            tint = cfg.accent.color,
            rim = cfg.glow,
            content = content,
        )
        else -> {
            val bg: Brush? = when (cfg.glass) {
                Glass.BLACK -> Brush.verticalGradient(listOf(Color(0xFF141416), Color(0xFF050506)))
                Glass.SUNSET -> Brush.linearGradient(listOf(Color(0xFF2E1A47), Color(0xFFB0476B), Color(0xFFF7A867)))
                Glass.AURORA -> Brush.linearGradient(listOf(Color(0xFF0B1026), Color(0xFF3B2A6B), Color(0xFF2E8C7A)))
                Glass.NIGHT -> Brush.verticalGradient(listOf(Color(0xFF0A1430), Color(0xFF16336B)))
                Glass.GOLD -> Brush.linearGradient(listOf(Color(0xFF3A2C14), Color(0xFF9C7A3C), Color(0xFF2A1F0E)))
                else -> null
            }
            val border = if (cfg.glow) {
                Modifier.border(
                    1.dp,
                    Brush.linearGradient(listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.32f))),
                    shape,
                )
            } else {
                Modifier
            }
            Box(
                modifier.clip(shape).then(if (bg != null) Modifier.background(bg) else Modifier).then(border),
                content = content,
            )
        }
    }
}

@Composable
fun WidgetContent(cfg: WConfig) {
    when (cfg.variant) {
        WVariant.CLOCK_THIN -> DigitalClock(cfg, bold = false)
        WVariant.CLOCK_BOLD -> DigitalClock(cfg, bold = true)
        WVariant.CLOCK_FLIP -> FlipClock(cfg)
        WVariant.CLOCK_WORDS -> WordsClock(cfg)
        WVariant.CLOCK_HALF -> HalfDialClock(cfg)
        WVariant.CLOCK_OVERSIZE -> OversizeClock(cfg)
        WVariant.CLOCK_RINGS -> RingsClock(cfg)
        WVariant.SKY_SUN -> SunArcWidget(cfg)
        WVariant.SKY_DAYLINE -> DayLineWidget(cfg)
        WVariant.SKY_MOON -> MoonWidget(cfg)
        WVariant.WEATHER_SKY -> WeatherSkyWidget(cfg)
        WVariant.BATTERY_LIQUID -> LiquidBattery(cfg)
        else -> when (cfg.variant.type) {
            WType.CLOCK -> AnalogClock(cfg)
            WType.DATE -> DateWidget(cfg)
            WType.PRAYER -> PrayerWidget(cfg)
            WType.COUNT -> CountWidget(cfg)
            WType.WEATHER -> WeatherWidget(cfg)
            WType.BATTERY -> BatteryWidget(cfg)
            WType.CONTROLS -> ControlsWidget(cfg)
            WType.TEXT -> TextWidget(cfg)
            WType.SKY -> MoonWidget(cfg)
        }
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

