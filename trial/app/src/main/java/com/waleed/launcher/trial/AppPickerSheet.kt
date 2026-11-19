package com.waleed.launcher.trial

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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.Collator

private val Gold = Color(0xFFE3C285)

/** وش اللي نبدله: العمود الجانبي أو الشريط، وأي خانة */
data class EditSlot(val inSide: Boolean, val index: Int, val current: AppEntry)

/** قائمة تختار منها تطبيق بدال اللي ضغطت عليه */
@Composable
fun AppPickerSheet(
    slot: EditSlot?,
    allApps: List<AppEntry>,
    repository: AppRepository,
    style: IconStyle,
    onPick: (AppEntry) -> Unit,
    onClose: () -> Unit,
) {
    val noRipple = remember { MutableInteractionSource() }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(slot) { query = "" }
    val sorted = remember(allApps) {
        val collator = Collator.getInstance()
        allApps.sortedWith { a, b -> collator.compare(a.label, b.label) }
    }
    val shown = if (query.isBlank()) sorted else sorted.filter { it.label.contains(query.trim(), ignoreCase = true) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(Modifier.fillMaxSize()) {
            AnimatedVisibility(slot != null, enter = fadeIn(tween(220)), exit = fadeOut(tween(200))) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(interactionSource = noRipple, indication = null) { onClose() },
                )
            }
            AnimatedVisibility(
                slot != null,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)) { it },
                exit = slideOutVertically(tween(220)) { it },
            ) {
                val shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    Column(
                        Modifier
                            .widthIn(max = 680.dp)
                            .fillMaxWidth()
                            .fillMaxHeight(0.72f)
                            .clip(shape)
                            .background(Color(0xF7161618))
                            .border(1.dp, Color.White.copy(alpha = 0.14f), shape)
                            .clickable(interactionSource = noRipple, indication = null) { }
                            .padding(horizontal = 18.dp)
                            .padding(top = 18.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BasicText(
                                "اختار تطبيق بداله",
                                modifier = Modifier.weight(1f),
                                style = TextStyle(color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                            )
                            slot?.let { s ->
                                BasicText(
                                    "معلومات التطبيق",
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(50))
                                        .clickable {
                                            repository.openAppInfo(s.current)
                                            onClose()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = TextStyle(color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp),
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))

                        // البحث
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                        ) {
                            if (query.isEmpty()) {
                                BasicText("ابحث عن تطبيق…", style = TextStyle(color = Color.White.copy(alpha = 0.4f), fontSize = 15.sp))
                            }
                            BasicTextField(
                                value = query,
                                onValueChange = { query = it },
                                singleLine = true,
                                textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                                cursorBrush = SolidColor(Gold),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Spacer(Modifier.height(12.dp))

                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 78.dp),
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                bottom = 20.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                            ),
                        ) {
                            items(shown, key = { it.key }) { app ->
                                val isCurrent = app.key == slot?.current?.key
                                Column(
                                    Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .then(if (isCurrent) Modifier.border(1.dp, Gold, RoundedCornerShape(16.dp)) else Modifier)
                                        .clickable { onPick(app) }
                                        .padding(vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    IconFace(app, repository, 52.dp, style)
                                    Spacer(Modifier.height(6.dp))
                                    BasicText(
                                        app.label,
                                        modifier = Modifier.width(72.dp),
                                        style = TextStyle(color = Color.White, fontSize = 11.sp, textAlign = TextAlign.Center),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
