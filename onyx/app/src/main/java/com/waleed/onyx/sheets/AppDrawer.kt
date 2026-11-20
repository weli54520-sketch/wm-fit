package com.waleed.onyx.sheets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.waleed.onyx.home.AppFace
import com.waleed.onyx.home.HomeState
import com.waleed.onyx.home.Pill
import com.waleed.onyx.ui.Onyx

/** كل التطبيقات: بحث، وضغطة تفتح، وضغطة طويلة تضيف للشاشة. ومنها نختار تطبيق للعمود والشريط */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawer(state: HomeState, inner: Boolean, icon: Dp) {
    val apps by state.catalog.apps.collectAsState()
    var query by remember { mutableStateOf("") }
    LaunchedEffect(state.drawerOpen) { if (!state.drawerOpen) query = "" }
    AnimatedVisibility(
        visible = state.drawerOpen,
        enter = fadeIn(tween(220)) + slideInVertically(tween(380)) { it / 3 },
        exit = fadeOut(tween(200)) + slideOutVertically(tween(300)) { it / 3 },
    ) {
        val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0xE6101014), Color(0xF2060608))))
                    .combinedClickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                    .padding(top = top + 10.dp),
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    BasicText(
                        if (state.picker != null) "اختار تطبيق" else "كل التطبيقات",
                        style = TextStyle(color = Onyx.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    )
                    Spacer(Modifier.weight(1f))
                    Pill("إغلاق") { state.drawerOpen = false; state.picker = null }
                }
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier
                        .padding(horizontal = 18.dp)
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x1AFFFFFF))
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (query.isEmpty()) BasicText("ابحث عن تطبيق", style = TextStyle(color = Onyx.Muted, fontSize = 15.sp))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Onyx.Text, fontSize = 15.sp),
                        cursorBrush = SolidColor(Onyx.Text),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                val list = if (query.isBlank()) apps else apps.filter { it.label.contains(query.trim(), ignoreCase = true) }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (inner) 7 else 4),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 18.dp, bottom = bottom + 24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(list, key = { it.key }) { a ->
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .combinedClickable(
                                    onClick = {
                                        val p = state.picker
                                        if (p != null) state.assignSlot(p, a)
                                        else { state.catalog.launch(a, null); state.drawerOpen = false }
                                    },
                                    onLongClick = { if (state.picker == null) state.addToHome(a) },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            AppFace(a, state.catalog, icon * 0.92f, state.settings.iconStyle, labels = true)
                        }
                    }
                }
            }
        }
    }
}

