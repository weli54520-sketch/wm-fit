package com.waleed.onyx.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.waleed.onyx.data.Container
import com.waleed.onyx.data.Kind
import com.waleed.onyx.ui.Motion
import com.waleed.onyx.ui.Onyx
import com.waleed.onyx.ui.smokeGlass
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val Label = TextStyle(color = Onyx.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

@Composable
fun Pill(text: String, filled: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .height(36.dp)
            .then(if (filled) Modifier.clip(shape).background(Color.White) else Modifier.smokeGlass(shape, strong = true))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, style = Label.copy(color = if (filled) Color(0xFF111111) else Onyx.Text))
    }
}

/** شريط وضع الترتيب فوق */
@Composable
fun EditBar(state: HomeState, top: Dp) {
    AnimatedVisibility(
        visible = state.editing,
        enter = fadeIn() + slideInVertically { -it / 2 },
        exit = fadeOut() + slideOutVertically { -it / 2 },
        modifier = Modifier.offset(y = top + 6.dp).fillMaxWidth().padding(horizontal = 14.dp),
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill("الإعدادات") { state.settingsOpen = true }
                Spacer(Modifier.width(8.dp))
                Pill("التطبيقات") { state.drawerOpen = true }
                Spacer(Modifier.width(8.dp))
                Pill("+ ويدجت") { state.galleryOpen = true }
                Spacer(Modifier.weight(1f))
                Pill("تم", filled = true) { state.exitEdit() }
            }
        }
    }
}

/** سطر بالقائمة */
private class Row0(val text: String, val red: Boolean = false, val action: () -> Unit)

/**
 * القائمة بعد الضغطة الطويلة: الأيقونة ترتفع والخلفية تغمق.
 * فوق: نص آخر إشعار (لو فيه)، واختصارات التطبيق (مثل "رسالة جديدة")، وتحتها الخيارات.
 */
@Composable
fun ContextMenu(state: HomeState) {
    val m = state.menu
    val shown by animateFloatAsState(if (m != null) 1f else 0f, Motion.smooth(state.settings), label = "menu")
    val last = remember { arrayOfNulls<Menu>(1) }
    if (m != null) last[0] = m
    val menu = last[0] ?: return
    if (shown < 0.01f && m == null) return
    val density = LocalDensity.current
    val item = menu.item
    val appInfo = state.catalog.byKey(item.appKey)
    val isWidget = item.kind == Kind.WIDGET
    val isFolder = item.kind == Kind.FOLDER
    val shortcuts by androidx.compose.runtime.produceState(emptyList<android.content.pm.ShortcutInfo>(), item.id) {
        value = if (appInfo != null && item.kind == Kind.APP) kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { state.catalog.shortcuts(appInfo) } else emptyList()
    }
    val notifs by com.waleed.onyx.system.Notifs.byPkg.collectAsState()
    val notif = appInfo?.let { notifs[it.pkg] }?.takeIf { item.kind == Kind.APP }

    val rows = ArrayList<Row0>()
    when {
        isWidget -> {
            val canResize = state.canResize(item)
            val canConfig = state.canReconfigure(item)
            val framed = state.widgetData(item)?.frame != false
            val clear = state.widgetData(item)?.clear != false
            if (canResize) rows += Row0("المقاس") { state.enterEdit(); state.say("اسحب الزاوية اللي تحت عشان تكبره أو تصغره") }
            rows += Row0(if (framed) "شيل الإطار الزجاجي" else "حط إطار زجاجي") { state.menu = null; state.toggleFrame(item) }
            rows += Row0(if (clear) "رجّع خلفية الويدجت" else "خلفية شفافة كاملة") { state.menu = null; state.toggleClear(item) }
            if (canConfig) rows += Row0("إعدادات الويدجت") { state.menu = null; state.reconfigure(item) }
            if (state.isStack(item)) rows += Row0("طلّعه من المكدس") { state.unstack(item) }
            rows += Row0("ترتيب الشاشة") { state.enterEdit() }
            rows += Row0("الإعدادات") { state.menu = null; state.settingsOpen = true }
            rows += Row0(if (state.isStack(item)) "شيل المكدس كامل" else "شيل الويدجت", red = true) { state.menu = null; state.removeFromHome(item) }
        }
        isFolder -> {
            val cover = state.folderData(item).cover
            rows += Row0("افتح المجلد") { state.openFolderUi(item) }
            rows += Row0("سمّ المجلد") { state.openFolderUi(item, rename = true) }
            rows += Row0(if (cover) "رجّعه مجلد عادي" else "غطاء (يبان مثل أول تطبيق)") { state.toggleCover(item) }
            rows += Row0("فك المجلد") { state.dissolveFolder(item) }
            rows += Row0("ترتيب الشاشة") { state.enterEdit() }
            rows += Row0("الإعدادات") { state.menu = null; state.settingsOpen = true }
            rows += Row0("شيل المجلد", red = true) { state.menu = null; state.removeFromHome(item) }
        }
        else -> {
            val alt = com.waleed.onyx.data.AppData.parse(item.data).alt
            if (menu.slot != null) rows += Row0("بدّل التطبيق") { state.menu = null; state.picker = menu.slot; state.drawerOpen = true }
            rows += Row0(if (alt.isEmpty()) "تطبيق للضغطتين" else "ضغطتين: ${state.catalog.byKey(alt)?.label ?: "—"} (غيّر)") { state.startAltPicker(item) }
            if (alt.isNotEmpty()) rows += Row0("شيل تطبيق الضغطتين") { state.clearAlt(item) }
            rows += Row0("الويدجت المنبثق") { state.menu = null; state.showPopup(item) }
            rows += Row0("ترتيب الشاشة") { state.enterEdit() }
            rows += Row0("الإعدادات") { state.menu = null; state.settingsOpen = true }
            rows += Row0("معلومات التطبيق") { state.menu = null; appInfo?.let { state.catalog.openInfo(it) } }
            if (appInfo != null) rows += Row0("إخفاء التطبيق") { state.hideApp(appInfo) }
            if (menu.slot == null) rows += Row0("شيله من الشاشة", red = true) { state.menu = null; state.removeFromHome(item) }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // تغميق الخلفية: ضغطة تسكر القائمة
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = shown }
                .background(Color(0x8C000000))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { state.menu = null },
        )
        val b = menu.bounds
        // الأيقونة (أو الويدجت) مرفوعة بمكانها. لو السحب بدأ: تختفي فوراً (الأيقونة اللي بيدك هي الوحيدة)
        val cut = m == null && state.menuCut
        with(density) {
            val iconSize = (if (menu.slot != null) b.width else minOf(b.width, b.height) * 0.76f * state.settings.iconScale).toDp()
            Box(
                Modifier
                    .offset { IntOffset(b.left.roundToInt(), b.top.roundToInt()) }
                    .size(b.width.toDp(), b.height.toDp())
                    .graphicsLayer {
                        val s = 1f + (if (isWidget) 0.03f else 0.08f) * shown
                        scaleX = s; scaleY = s; alpha = if (cut) 0f else shown
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (isWidget) WidgetLifted(menu.snap, state.widgetData(item)?.frame != false, Modifier.fillMaxSize().padding(WidgetMargin))
                else ItemIcon(state, item, iconSize)
            }

            val menuW = 250.dp
            val notifH = if (notif != null) 74 else 0
            val menuH = (46 * rows.size + 44 * shortcuts.size + notifH + 12 + (if (shortcuts.isNotEmpty()) 8 else 0)).dp
            val below = b.bottom + 14.dp.toPx() + menuH.toPx() < constraints.maxHeight - 40.dp.toPx()
            val y = (if (below) b.bottom + 14.dp.toPx() else b.top - 14.dp.toPx() - menuH.toPx())
                .coerceIn(40.dp.toPx(), (constraints.maxHeight - menuH.toPx() - 24.dp.toPx()).coerceAtLeast(40.dp.toPx()))
            val x = (b.center.x - menuW.toPx() / 2).coerceIn(12.dp.toPx(), constraints.maxWidth - menuW.toPx() - 12.dp.toPx())
            Box(
                Modifier
                    .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                    .width(menuW)
                    .graphicsLayer {
                        alpha = shown
                        val s = 0.85f + 0.15f * shown
                        scaleX = s; scaleY = s
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, if (below) 0f else 1f)
                    }
                    .smokeGlass(RoundedCornerShape(22.dp), strong = true, elevation = 12.dp)
                    .padding(6.dp),
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Column {
                        if (notif != null) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .height(68.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0x14FFFFFF))
                                    .clickable {
                                        state.menu = null
                                        try { notif.intent?.send() } catch (_: Exception) { appInfo?.let { state.launchApp(it) } }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            ) {
                                BasicText(notif.title.ifEmpty { appInfo?.label ?: "" }, style = Label.copy(fontSize = 13.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                BasicText(notif.text, style = Label.copy(fontWeight = FontWeight.Normal, fontSize = 12.sp, color = Onyx.Muted), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                        for (sc in shortcuts) ShortcutRow(state, sc)
                        if (shortcuts.isNotEmpty()) Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp).background(Color(0x22FFFFFF)))
                        for (r in rows) MenuRow(r.text, r.red, r.action)
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortcutRow(state: HomeState, sc: android.content.pm.ShortcutInfo) {
    val px = with(LocalDensity.current) { 22.dp.roundToPx() }
    val icon by androidx.compose.runtime.produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, sc.id) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { state.catalog.shortcutIcon(sc, px) }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { state.menu = null; state.catalog.startShortcut(sc) }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(22.dp)) { icon?.let { androidx.compose.foundation.Image(it, null, Modifier.size(22.dp)) } }
        Spacer(Modifier.width(10.dp))
        BasicText((sc.shortLabel ?: "").toString(), style = Label.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun MenuRow(text: String, red: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicText(text, style = Label.copy(fontWeight = FontWeight.Medium, color = if (red) Color(0xFFFF8A8A) else Onyx.Text))
    }
}

/** رسالة صغيرة تختفي لحالها */
@Composable
fun ToastPill(state: HomeState, bottom: Dp) {
    val msg = state.toast
    LaunchedEffect(msg) {
        if (msg != null) { delay(1800); state.toast = null }
    }
    val last = remember { arrayOfNulls<String>(1) }
    if (msg != null) last[0] = msg
    Box(Modifier.fillMaxSize().padding(bottom = bottom), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(visible = msg != null, enter = fadeIn() + scaleIn(initialScale = 0.9f), exit = fadeOut() + scaleOut(targetScale = 0.9f)) {
            Box(Modifier.smokeGlass(RoundedCornerShape(50), strong = true).padding(horizontal = 18.dp, vertical = 10.dp)) {
                BasicText(last[0] ?: "", style = Label.copy(fontWeight = FontWeight.Medium, fontSize = 13.sp))
            }
        }
    }
}
