package com.waleed.launcher.trial.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.absoluteValue

/** قسم بالمعرض: نوع ويدجت، أو "تصاميمي" */
private sealed class GalleryTab(val title: String) {
    data object Mine : GalleryTab("تصاميمي")
    data class Of(val type: WType) : GalleryTab(type.title)
}

/**
 * معرض الويدجت (زر + بوضع الترتيب):
 * تختار القسم، تقلب البطاقات (اللي بالنص يكبر)، تختار المقاس، وتضيف.
 */
@Composable
fun WidgetGallery(
    visible: Boolean,
    designs: List<WConfig>,
    maxCols: Int,
    cell: Dp,
    rowHeight: Dp,
    inset: Dp,
    onAdd: (WConfig, WSize) -> Unit,
    onClose: () -> Unit,
) {
    var tab by remember { mutableStateOf<GalleryTab>(GalleryTab.Of(WType.CLOCK)) }
    var size by remember { mutableStateOf(WSize.S) }
    var chosen by remember { mutableStateOf<WConfig?>(null) }

    BottomSheet(visible = visible, heightFraction = 0.86f, onClose = onClose) {
        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            WText("إضافة ويدجت", 19.sp, weight = FontWeight.SemiBold, align = TextAlign.Start, shadow = false, modifier = Modifier.weight(1f))
            Chip("إغلاق", false) { onClose() }
        }

        Spacer(Modifier.height(10.dp))
        ChipRow {
            if (designs.isNotEmpty()) Chip(GalleryTab.Mine.title, tab == GalleryTab.Mine) { tab = GalleryTab.Mine }
            WType.entries.forEach { t -> Chip(t.title, tab == GalleryTab.Of(t)) { tab = GalleryTab.Of(t) } }
        }

        val options: List<WConfig> = when (val t = tab) {
            GalleryTab.Mine -> designs
            is GalleryTab.Of -> WVariant.entries.filter { it.type == t.type }.map { v ->
                if (v.isAnalog) WConfig.preset(v, WConfig(v)) else WConfig(v)
            }
        }

        Spacer(Modifier.height(18.dp))
        key(tab, size) {
            val pager = rememberPagerState(pageCount = { options.size })
            chosen = options.getOrNull(pager.currentPage)
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                val slotW = cell * size.w - inset * 2
                val slotH = rowHeight * size.h - inset * 2
                val cardW = minOf(maxWidth * 0.62f, 320.dp)
                val scale = minOf(cardW / slotW, (maxHeight * 0.78f) / slotH)
                val cardH = slotH * scale
                HorizontalPager(
                    state = pager,
                    contentPadding = PaddingValues(horizontal = (maxWidth - slotW * scale) / 2),
                    pageSpacing = 10.dp,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    val off = ((pager.currentPage - page) + pager.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
                    Column(
                        Modifier.fillMaxSize().graphicsLayer {
                            val sc = 1f - 0.18f * off
                            scaleX = sc
                            scaleY = sc
                            alpha = 1f - 0.5f * off
                        },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(Modifier.size(slotW * scale, cardH), contentAlignment = Alignment.Center) {
                            Box(Modifier.requiredSize(slotW, slotH).graphicsLayer { scaleX = scale; scaleY = scale }) {
                                val cfg = options[page]
                                GlassPanel(cfg, Modifier.fillMaxSize()) { WidgetContent(cfg) }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        WText(options[page].variant.title, 14.sp, color = if (off < 0.5f) EditorGold else Soft, shadow = false)
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        ChipRow { WSize.entries.filter { it.w <= maxCols }.forEach { z -> Chip(z.title, size == z) { size = z } } }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Chip("+ إضافة للشاشة", true) { chosen?.let { onAdd(it, size) } }
        }
    }
}
