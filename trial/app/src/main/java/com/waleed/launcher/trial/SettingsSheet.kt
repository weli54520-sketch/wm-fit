package com.waleed.launcher.trial

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Gold = Color(0xFFE3C285)

/** لوحة الإعدادات: تطلع من تحت لما تضغط ضغطة طويلة على مكان فاضي */
@Composable
fun SettingsSheet(
    visible: Boolean,
    settings: LauncherSettings,
    previewApps: List<AppEntry>,
    repository: AppRepository,
    hasWallpaper: Boolean,
    onPickWallpaper: () -> Unit,
    onClearWallpaper: () -> Unit,
    onAddWidget: () -> Unit,
    onClose: () -> Unit,
) {
    val noRipple = remember { MutableInteractionSource() }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(Modifier.fillMaxSize()) {
            AnimatedVisibility(visible, enter = fadeIn(tween(220)), exit = fadeOut(tween(200))) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable(interactionSource = noRipple, indication = null) { onClose() },
                )
            }
            AnimatedVisibility(
                visible,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) { it },
                exit = slideOutVertically(tween(220)) { it },
            ) {
                val shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    Column(
                        Modifier
                            .widthIn(max = 640.dp)
                            .fillMaxWidth()
                            .clip(shape)
                            .background(Color(0xF2161618))
                            .border(1.dp, Color.White.copy(alpha = 0.14f), shape)
                            .clickable(interactionSource = noRipple, indication = null) { }
                            .padding(horizontal = 20.dp)
                            .padding(top = 12.dp, bottom = 20.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
                    ) {
                        Box(
                            Modifier
                                .align(Alignment.CenterHorizontally)
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.25f))
                                .widthIn(min = 40.dp)
                                .height(5.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                        BasicText(
                            "الشاشة",
                            style = TextStyle(color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SheetButton("+ إضافة ويدجت", Modifier.weight(1f), onAddWidget)
                            SheetButton("اختيار خلفية", Modifier.weight(1f), onPickWallpaper)
                        }
                        if (hasWallpaper) {
                            Spacer(Modifier.height(8.dp))
                            SheetButton("رجوع لخلفية الجوال", Modifier.fillMaxWidth(), onClearWallpaper)
                        } else {
                            Spacer(Modifier.height(8.dp))
                            BasicText(
                                "اختر خلفيتك من هنا عشان يشتغل الزجاج السائل الحقيقي",
                                style = TextStyle(color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp),
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                        BasicText(
                            "شكل الأيقونات",
                            style = TextStyle(color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            IconStyle.entries.forEach { style ->
                                StyleOption(
                                    style = style,
                                    selected = settings.iconStyle == style,
                                    previewApps = previewApps,
                                    repository = repository,
                                    modifier = Modifier.weight(1f),
                                    onClick = { settings.updateIconStyle(style) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StyleOption(
    style: IconStyle,
    selected: Boolean,
    previewApps: List<AppEntry>,
    repository: AppRepository,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    val borderWidth by animateDpAsState(if (selected) 1.5.dp else 1.dp, label = "border")
    // خلفية رمادية مثل خلفية وليد عشان يشوف الزجاج على حقيقته
    Column(
        modifier
            .clip(shape)
            .background(Color(0xFF8E8E93))
            .border(borderWidth, if (selected) Gold else Color.White.copy(alpha = 0.15f), shape)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            previewApps.take(2).forEach { app -> IconFace(app, repository, 34.dp, style) }
        }
        Spacer(Modifier.height(10.dp))
        BasicText(
            style.title,
            style = TextStyle(
                color = if (selected) Gold else Color.White,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

@Composable
private fun SheetButton(text: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color(0xFFE3C285).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, style = TextStyle(color = Color(0xFFE3C285), fontSize = 14.sp, fontWeight = FontWeight.Medium))
    }
}
