package com.waleed.launcher.trial.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.absoluteValue

private val EditorGold = Color(0xFFE3C285)

/** لوحة تعديل الويدجت: تختار النوع والشكل والألوان والخط */
@Composable
fun WidgetEditorSheet(
    placed: Placed?,
    cell: Dp,
    rowHeight: Dp,
    inset: Dp,
    maxCols: Int,
    onChange: (Placed) -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit,
    onClose: () -> Unit,
) {
    val slot = placed?.slot
    val noRipple = remember { MutableInteractionSource() }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(Modifier.fillMaxSize()) {
            AnimatedVisibility(slot != null, enter = fadeIn(tween(220)), exit = fadeOut(tween(200))) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable(interactionSource = noRipple, indication = null) { onClose() })
            }
            AnimatedVisibility(
                slot != null,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)) { it },
                exit = slideOutVertically(tween(220)) { it },
            ) {
                val s = slot ?: return@AnimatedVisibility
                val shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    Column(
                        Modifier
                            .widthIn(max = 700.dp)
                            .fillMaxWidth()
                            .fillMaxHeight(0.88f)
                            .clip(shape)
                            .background(Color(0xF7141416))
                            .border(1.dp, Color.White.copy(alpha = 0.14f), shape)
                            .clickable(interactionSource = noRipple, indication = null) { }
                            .verticalScroll(rememberScrollState())
                            .padding(top = 16.dp, bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
                    ) {
                        val p = placed!!
                        EditorBody(
                            s, cell * p.size.w - inset * 2, rowHeight * p.size.h - inset * 2,
                            onChange = { newSlot -> onChange(p.copy(slot = newSlot)) }, onClose = onClose,
                        )
                        Section("المقاس")
                        ChipRow {
                            WSize.entries.filter { it.w <= maxCols }.forEach { z ->
                                Chip(z.title, p.size == z) { onChange(p.copy(size = z, x = p.x.coerceAtMost(maxCols - z.w))) }
                            }
                        }
                        Section("المكان")
                        ChipRow {
                            Chip("✥ نقل الويدجت", false) { onMove() }
                            Chip("حذف الويدجت", false) { onDelete() }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorBody(slot: Slot, slotWidth: Dp, slotHeight: Dp, onChange: (Slot) -> Unit, onClose: () -> Unit) {
    val cfg = slot.current
    fun set(newCfg: WConfig) {
        onChange(slot.copy(items = slot.items.toMutableList().also { it[slot.index.coerceIn(0, it.size - 1)] = newCfg }))
    }

    // العنوان + المكدس
    Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            WText("تعديل الويدجت", 19.sp, weight = FontWeight.SemiBold, align = androidx.compose.ui.text.style.TextAlign.Start, shadow = false)
            if (slot.items.size > 1) {
                WText("ويدجت ${slot.index + 1} من ${slot.items.size} · قلّبها لفوق وتحت", 12.sp, color = Soft, align = androidx.compose.ui.text.style.TextAlign.Start, shadow = false)
            }
        }
        Chip("تم", selected = true) { onClose() }
    }
    Spacer(Modifier.height(14.dp))
    Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip("+ أضف ويدجت للمكدس", selected = false) {
            onChange(Slot(slot.items + WConfig.preset(WVariant.CLOCK_BLUE, WConfig(WVariant.CLOCK_BLUE, glass = cfg.glass)), slot.items.size))
        }
        if (slot.items.size > 1) {
            Chip("حذف هذا", selected = false) {
                val list = slot.items.toMutableList().also { it.removeAt(slot.index) }
                onChange(Slot(list, (slot.index - 1).coerceAtLeast(0)))
            }
        }
    }

    Section("النوع")
    ChipRow {
        WType.entries.forEach { t ->
            Chip(t.title, selected = cfg.variant.type == t) {
                if (cfg.variant.type != t) {
                    val v = WVariant.entries.first { it.type == t }
                    set(if (v.isAnalog) WConfig.preset(v, cfg) else cfg.copy(variant = v))
                }
            }
        }
    }

    // بطاقات الأشكال: اللي بالنص يكبر
    Section("الشكل")
    val variants = WVariant.entries.filter { it.type == cfg.variant.type }
    key(cfg.variant.type) {
        val pager = rememberPagerState(initialPage = variants.indexOf(cfg.variant).coerceAtLeast(0), pageCount = { variants.size })
        val latestCfg = rememberUpdatedState(cfg)
        val latestSet = rememberUpdatedState<(WConfig) -> Unit> { c -> set(c) }
        LaunchedEffect(pager) {
            snapshotFlow { pager.settledPage }.collect { p ->
                val v = variants.getOrNull(p) ?: return@collect
                if (v != latestCfg.value.variant) {
                    val base = latestCfg.value
                    latestSet.value(if (v.isAnalog) WConfig.preset(v, base) else base.copy(variant = v))
                }
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cardW = minOf(maxWidth * 0.6f, 300.dp)
            val scale = cardW / slotWidth
            val cardH = slotHeight * scale
            HorizontalPager(
                state = pager,
                contentPadding = PaddingValues(horizontal = (maxWidth - cardW) / 2),
                pageSpacing = 8.dp,
                modifier = Modifier.fillMaxWidth(),
            ) { page ->
                val off = ((pager.currentPage - page) + pager.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
                Column(
                    Modifier.graphicsLayer {
                        val sc = 1f - 0.16f * off
                        scaleX = sc
                        scaleY = sc
                        alpha = 1f - 0.45f * off
                    },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.size(cardW, cardH), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier.requiredSize(slotWidth, slotHeight).graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            },
                        ) {
                            val preview = cfg.copy(variant = variants[page])
                            GlassPanel(preview, Modifier.fillMaxSize()) { WidgetContent(preview) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    WText(variants[page].title, 13.sp, color = if (off < 0.5f) EditorGold else Soft, shadow = false)
                }
            }
        }
    }

    Section("الخلفية")
    ChipRow { Glass.entries.forEach { g -> Chip(g.title, cfg.glass == g) { set(cfg.copy(glass = g)) } } }

    Section("اللون")
    ChipRow {
        Accent.entries.forEach { a ->
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(a.color)
                    .border(if (cfg.accent == a) 2.5.dp else 0.dp, Color.White, CircleShape)
                    .clickable { set(cfg.copy(accent = a)) },
            )
        }
    }

    Section("الأرقام")
    ChipRow {
        Chip("English 123", !cfg.arabicDigits) { set(cfg.copy(arabicDigits = false)) }
        Chip("عربية ١٢٣", cfg.arabicDigits) { set(cfg.copy(arabicDigits = true)) }
    }

    Section("خط الأرقام")
    ChipRow { NumFont.entries.forEach { f -> Chip(f.title, cfg.numFont == f, family = f.family) { set(cfg.copy(numFont = f)) } } }

    Section("حدود رفيعة مضيئة")
    ChipRow {
        Chip("تشغيل", cfg.glow) { set(cfg.copy(glow = true)) }
        Chip("إيقاف", !cfg.glow) { set(cfg.copy(glow = false)) }
    }

    if (cfg.variant.isAnalog) {
        Section("المينا")
        ChipRow {
            DialFace.entries.forEach { f ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { set(cfg.copy(face = f)) }) {
                    Box(
                        Modifier.size(38.dp).clip(CircleShape).background(f.swatch)
                            .border(if (cfg.face == f) 2.5.dp else 1.dp, if (cfg.face == f) EditorGold else Color.White.copy(alpha = 0.2f), CircleShape),
                    )
                    WText(f.title, 10.sp, color = if (cfg.face == f) EditorGold else Soft, shadow = false)
                }
            }
        }
        Section("المعدن (العقارب والمؤشرات)")
        ChipRow {
            Metal.entries.forEach { m ->
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.07f))
                        .border(1.dp, if (cfg.metal == m) EditorGold else Color.White.copy(alpha = 0.12f), RoundedCornerShape(50))
                        .clickable { set(cfg.copy(metal = m)) }.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(16.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(m.light, m.dark))))
                    Spacer(Modifier.size(6.dp))
                    WText(m.title, 13.sp, color = if (cfg.metal == m) EditorGold else Color.White, shadow = false)
                }
            }
        }
        Section("المؤشرات")
        ChipRow { Indices.entries.forEach { v -> Chip(v.title, cfg.indices == v) { set(cfg.copy(indices = v)) } } }
        Section("العقارب")
        ChipRow { Hands.entries.forEach { v -> Chip(v.title, cfg.hands == v) { set(cfg.copy(hands = v)) } } }
        Section("الإطار")
        ChipRow { Bezel.entries.forEach { v -> Chip(v.title, cfg.bezel == v) { set(cfg.copy(bezel = v)) } } }
        Section("الإضافات على المينا")
        ChipRow { Comp.entries.forEach { v -> Chip(v.title, cfg.comp == v) { set(cfg.copy(comp = v)) } } }
        Section("كتابة على المينا")
        BasicTextField(
            value = cfg.dialText,
            onValueChange = { set(cfg.copy(dialText = it.take(14))) },
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontFamily = WFonts.ruqaa),
            cursorBrush = SolidColor(EditorGold),
            modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.08f)).padding(14.dp),
        )
        Section("عقرب الثواني")
        ChipRow { Seconds.entries.forEach { sMode -> Chip(sMode.title, cfg.seconds == sMode) { set(cfg.copy(seconds = sMode)) } } }
    }

    if (cfg.variant.type == WType.CLOCK && !cfg.variant.isAnalog) {
        Section("التاريخ مع الوقت")
        ChipRow {
            Chip("يظهر", cfg.showDate) { set(cfg.copy(showDate = true)) }
            Chip("يختفي", !cfg.showDate) { set(cfg.copy(showDate = false)) }
        }
    }

    if (cfg.variant == WVariant.COUNT_SALARY) {
        Section("يوم الراتب بالشهر")
        ChipRow {
            listOf(1, 10, 25, 26, 27, 28).forEach { d -> Chip(d.toString(), cfg.salaryDay == d) { set(cfg.copy(salaryDay = d)) } }
        }
    }
    if (cfg.variant == WVariant.COUNT_CUSTOM) {
        val ctx = LocalContext.current
        Section("اسم المناسبة")
        BasicTextField(
            value = cfg.countTitle,
            onValueChange = { set(cfg.copy(countTitle = it.take(24))) },
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontFamily = WFonts.kufi),
            cursorBrush = SolidColor(EditorGold),
            modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.08f)).padding(14.dp),
        )
        Section("التاريخ")
        ChipRow {
            Chip(if (cfg.countDay > 0) java.time.LocalDate.ofEpochDay(cfg.countDay).toString() else "اختر التاريخ", false) {
                pickDate(ctx, cfg.countDay) { d -> set(cfg.copy(countDay = d)) }
            }
        }
    }

    if (cfg.variant.type == WType.TEXT) {
        Section("جملتك")
        BasicTextField(
            value = cfg.text,
            onValueChange = { set(cfg.copy(text = it)) },
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontFamily = cfg.textFont.family),
            cursorBrush = SolidColor(EditorGold),
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .padding(14.dp),
        )
        Section("خط الكتابة")
        ChipRow { NumFont.entries.forEach { f -> Chip(f.title, cfg.textFont == f, family = f.family) { set(cfg.copy(textFont = f)) } } }
        Section("الحجم")
        ChipRow { listOf("صغير", "وسط", "كبير").forEachIndexed { i, t -> Chip(t, cfg.textSize == i) { set(cfg.copy(textSize = i)) } } }
        Section("المحاذاة")
        ChipRow { listOf("يسار", "وسط", "يمين").forEachIndexed { i, t -> Chip(t, cfg.textAlign == i) { set(cfg.copy(textAlign = i)) } } }
    }
}

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(18.dp))
    WText(
        title, 13.sp, color = Soft, shadow = false,
        align = androidx.compose.ui.text.style.TextAlign.Start,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

@Composable
private fun Chip(text: String, selected: Boolean, family: FontFamily = WFonts.kufi, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .clip(shape)
            .background(if (selected) EditorGold.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.07f))
            .border(1.dp, if (selected) EditorGold else Color.White.copy(alpha = 0.12f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        WText(text, 13.sp, family = family, color = if (selected) EditorGold else Color.White, shadow = false)
    }
}
