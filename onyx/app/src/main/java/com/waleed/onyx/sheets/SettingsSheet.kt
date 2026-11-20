package com.waleed.onyx.sheets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.waleed.onyx.data.Screen
import com.waleed.onyx.data.Settings
import com.waleed.onyx.home.HomeState
import com.waleed.onyx.home.Pill
import com.waleed.onyx.ui.Motion
import com.waleed.onyx.ui.Onyx
import com.waleed.onyx.ui.smokeGlass
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val Title = TextStyle(color = Onyx.Text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
private val Head = TextStyle(color = Onyx.Muted, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
private val Note = TextStyle(color = Onyx.Muted, fontSize = 12.sp, lineHeight = 18.sp)

/** الإعدادات: لوحة من تحت، وكل تغيير ينطبق على طول */
@Composable
fun SettingsSheet(state: HomeState) {
    val s = state.settings
    val scope = rememberCoroutineScope()
    var confirmCopy by remember { mutableStateOf(false) }
    if (state.settingsOpen) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0x66000000))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { state.settingsOpen = false },
        )
    }
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible = state.settingsOpen,
            enter = fadeIn(tween(200)) + slideInVertically(tween(380)) { it },
            exit = fadeOut(tween(200)) + slideOutVertically(tween(300)) { it },
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.86f)
                        .smokeGlass(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp), strong = true)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                ) {
                    Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(38.dp, 5.dp).clip(CircleShape).background(Color(0x4DFFFFFF)))
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        BasicText("الإعدادات", style = TextStyle(color = Onyx.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold))
                        Spacer(Modifier.weight(1f))
                        Pill("تم", filled = true) { state.settingsOpen = false }
                    }
                    Column(
                        Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(start = 16.dp, end = 16.dp, bottom = bottom + 24.dp),
                    ) {
                        Section("الأيقونات") {
                            Segmented(listOf("الأصلية", "زجاج شفاف", "زجاج داكن"), s.iconStyle) { s.iconStyle = it }
                            Stepper("الحجم", "${(s.iconScale * 100).roundToInt()}٪", s.iconScale > 0.81f, s.iconScale < 1.14f,
                                onMinus = { s.iconScale = ((s.iconScale * 100).roundToInt() - 5) / 100f },
                                onPlus = { s.iconScale = ((s.iconScale * 100).roundToInt() + 5) / 100f })
                            Toggle("الأسماء تحت الأيقونات", s.labels) { s.labels = it }
                        }
                        val scr = state.screen
                        val cr = Settings.COLS_RANGE.getValue(scr)
                        val rr = Settings.ROWS_RANGE.getValue(scr)
                        Section(if (scr == Screen.INNER) "الشبكة · الشاشة الداخلية" else "الشبكة · الشاشة الخارجية") {
                            Stepper("الأعمدة", "${s.cols(scr)}", s.cols(scr) > cr.first, s.cols(scr) < cr.last,
                                onMinus = { s.setCols(scr, s.cols(scr) - 1) }, onPlus = { s.setCols(scr, s.cols(scr) + 1) })
                            Stepper("الصفوف", "${s.rows(scr)}", s.rows(scr) > rr.first, s.rows(scr) < rr.last,
                                onMinus = { s.setRows(scr, s.rows(scr) - 1) }, onPlus = { s.setRows(scr, s.rows(scr) + 1) })
                            BasicText("كل شاشة لها ترتيب مستقل. لو صغرت الشبكة، اللي ما يركب ينتقل للصفحة اللي بعدها.", style = Note, modifier = Modifier.padding(top = 6.dp))
                        }
                        Section("العمود والشريط") {
                            Toggle("العمود الجانبي", s.sideOn) { s.sideOn = it }
                            if (s.sideOn) Stepper("أيقونات العمود", "${s.sideCount}", s.sideCount > 3, s.sideCount < 5,
                                onMinus = { s.sideCount-- }, onPlus = { s.sideCount++ })
                            Stepper("أيقونات الشريط", "${s.dockCount}", s.dockCount > 4, s.dockCount < 6,
                                onMinus = { s.dockCount-- }, onPlus = { s.dockCount++ })
                        }
                        Section("الحركة") {
                            Label("السرعة")
                            Segmented(listOf("هادئة", "عادية", "سريعة"), s.speed) { s.speed = it }
                            Label("النبض")
                            Segmented(listOf("خفيف", "متوسط", "قوي"), s.bounce) { s.bounce = it }
                            Label("وضع الترتيب")
                            Segmented(listOf("الصفحة تصغر", "اهتزاز خفيف"), s.editStyle) { s.editStyle = it }
                            Toggle("اهتزاز اللمس", s.haptics) { s.haptics = it }
                            Toggle("تقليل الحركة", s.reduceMotion) { s.reduceMotion = it }
                        }
                        Section("الترتيب") {
                            Toggle("قفل الترتيب", s.lock) { s.lock = it }
                            Spacer(Modifier.height(8.dp))
                            val other = if (scr == Screen.INNER) "الخارجية" else "الداخلية"
                            if (!confirmCopy) {
                                WideButton("انسخ ترتيب هالشاشة للشاشة $other") { confirmCopy = true }
                            } else {
                                BasicText("الترتيب الحالي للشاشة $other بينمسح ويصير نسخة من هذي.", style = Note)
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(Modifier.weight(1f)) {
                                        WideButton("إيه انسخ", filled = true) {
                                            confirmCopy = false
                                            scope.launch {
                                                state.app.repo.copyLayout(scr, if (scr == Screen.INNER) Screen.OUTER else Screen.INNER)
                                                state.say("انتسخ الترتيب")
                                            }
                                        }
                                    }
                                    Box(Modifier.weight(1f)) { WideButton("لا") { confirmCopy = false } }
                                }
                            }
                        }
                        Section("عن اللانشر") {
                            BasicText("WM Onyx · الإصدار 0.1", style = Title)
                            BasicText("الأساس: الشبكة الموحدة، السحب، الشاشتين. الويدجت جاية بالنسخ الجاية.", style = Note, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    BasicText(title, style = Head, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp, start = 4.dp))
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x12FFFFFF))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
private fun Label(text: String) {
    BasicText(text, style = Head.copy(fontSize = 12.sp), modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0x14FFFFFF)).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEachIndexed { i, o ->
            val on = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) Color(0x33FFFFFF) else Color.Transparent)
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                BasicText(o, style = Title.copy(fontSize = 13.sp, color = if (on) Onyx.Text else Onyx.Muted))
            }
        }
    }
}

@Composable
private fun Toggle(text: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(44.dp).clickable { onChange(!value) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(text, style = Title, modifier = Modifier.weight(1f))
        val x by animateFloatAsState(if (value) 1f else 0f, label = "toggle")
        Box(
            Modifier
                .size(50.dp, 30.dp)
                .clip(CircleShape)
                .background(if (value) Color(0xFF34C759) else Color(0x33FFFFFF)),
        ) {
            // بالعربي: التشغيل يسار
            Box(
                Modifier
                    .offset(x = 3.dp + (20.dp * x))
                    .align(Alignment.CenterStart)
                    .padding(start = 0.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.White),
            )
        }
    }
}

@Composable
private fun Stepper(text: String, value: String, canMinus: Boolean, canPlus: Boolean, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
        BasicText(text, style = Title, modifier = Modifier.weight(1f))
        StepBtn("−", canMinus, onMinus)
        BasicText(value, style = Title, modifier = Modifier.width(56.dp).padding(horizontal = 4.dp))
        StepBtn("+", canPlus, onPlus)
    }
}

@Composable
private fun StepBtn(t: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color(if (enabled) 0x29FFFFFF else 0x0FFFFFFF))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(t, style = Title.copy(fontSize = 18.sp, color = if (enabled) Onyx.Text else Onyx.Muted))
    }
}

@Composable
private fun WideButton(text: String, filled: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (filled) Color.White else Color(0x1FFFFFFF))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, style = Title.copy(color = if (filled) Color(0xFF111111) else Onyx.Text))
    }
}
