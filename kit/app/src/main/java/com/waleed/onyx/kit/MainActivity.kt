package com.waleed.onyx.kit

import android.graphics.Color as AColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        val icons = GlassIcons(applicationContext)
        setContent { IconsScreen(icons) }
    }
}

private val TextMain = Color(0xFFF2F2F5)
private val TextMuted = Color(0x9EFFFFFF)

/** صفحة الأيقونات: تختار الزجاج، تختار التطبيقات، وتحفظ */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IconsScreen(icons: GlassIcons) {
    val scope = rememberCoroutineScope()
    val apps by produceState(emptyList<KitApp>()) { value = withContext(Dispatchers.Default) { icons.apps() } }
    var dark by remember { mutableStateOf(true) }
    val selected = remember { mutableStateListOf<String>() }
    // التطبيقات اللي وليد يبيها ملونة بدل الرمز الأبيض
    val colored = remember { mutableStateListOf<String>() }
    var status by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            // خلفية تشبه خلفية جوال عشان الزجاج يبان
            .background(Brush.verticalGradient(listOf(Color(0xFF1B1D24), Color(0xFF07070A), Color(0xFF14110D))))
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        // العنوان
        Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            BasicText("WM Onyx", style = TextStyle(color = TextMain, fontSize = 26.sp, fontWeight = FontWeight.SemiBold))
            BasicText("الأيقونات (تجربة)", style = TextStyle(color = TextMuted, fontSize = 14.sp))
        }

        // شفاف / داكن
        Row(
            Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .height(44.dp)
                .background(Color(0x1FFFFFFF), RoundedCornerShape(22.dp))
                .padding(4.dp),
        ) {
            Segment("داكن", dark, Modifier.weight(1f)) { dark = true }
            Segment("شفاف", !dark, Modifier.weight(1f)) { dark = false }
        }

        BasicText(
            "ضغطة = اختيار · ضغطة طويلة = أبيض أو ملون",
            Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            style = TextStyle(color = TextMuted, fontSize = 12.sp),
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(82.dp),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        ) {
            items(apps, key = { it.key }) { app ->
                val white = app.key !in colored
                val isSel = app.key in selected
                Column(
                    Modifier
                        .padding(vertical = 8.dp)
                        .combinedClickable(
                            onClick = { if (isSel) selected.remove(app.key) else selected.add(app.key) },
                            onLongClick = { if (white) colored.add(app.key) else colored.remove(app.key) },
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val bmp by produceState<android.graphics.Bitmap?>(null, app.key, dark, white) {
                            value = withContext(Dispatchers.Default) { runCatching { icons.render(app, 192, dark, white) }.getOrNull() }
                        }
                        Box(Modifier.size(62.dp)) {
                            bmp?.let { Image(it.asImageBitmap(), app.label, Modifier.size(62.dp)) }
                        }
                        if (isSel) {
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .size(20.dp)
                                    .background(Color(0xFFE3C285), CircleShape)
                                    .border(1.5.dp, Color(0xFF0B0B0D), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                BasicText("✓", style = TextStyle(color = Color(0xFF0B0B0D), fontSize = 12.sp, fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    BasicText(
                        app.label,
                        Modifier.width(78.dp),
                        style = TextStyle(color = TextMain, fontSize = 11.sp, textAlign = TextAlign.Center),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        if (status.isNotEmpty()) {
            BasicText(
                status,
                Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                style = TextStyle(color = Color(0xFF9FE3CB), fontSize = 13.sp),
            )
        }

        // الأزرار تحت
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                if (selected.size == apps.size && apps.isNotEmpty()) "إلغاء الكل" else "اختر الكل",
                primary = false,
                modifier = Modifier.weight(1f),
            ) {
                if (selected.size == apps.size) selected.clear() else { selected.clear(); selected.addAll(apps.map { it.key }) }
            }
            Button(
                if (saving) "يحفظ…" else "احفظ (${selected.size})",
                primary = true,
                modifier = Modifier.weight(1.4f),
            ) {
                if (saving || selected.isEmpty()) return@Button
                saving = true
                val chosen = apps.filter { it.key in selected }
                val isDark = dark
                scope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        chosen.count { icons.save(it, isDark, it.key !in colored) }
                    }
                    saving = false
                    status = "انحفظت $ok صورة بالمعرض: Pictures/WM Onyx/" + (if (isDark) "Dark" else "Clear")
                }
            }
        }
    }
}

@Composable
private fun Segment(text: String, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .fillMaxSize()
            .background(if (on) Color(0x33FFFFFF) else Color.Transparent, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, style = TextStyle(color = if (on) TextMain else TextMuted, fontSize = 15.sp, fontWeight = FontWeight.Medium))
    }
}

@Composable
private fun Button(text: String, primary: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(50.dp)
            .background(if (primary) Color(0xFFEDEDED) else Color(0x24FFFFFF), RoundedCornerShape(25.dp))
            .border(1.dp, Color(0x3DFFFFFF), RoundedCornerShape(25.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, style = TextStyle(color = if (primary) Color(0xFF0B0B0D) else TextMain, fontSize = 15.sp, fontWeight = FontWeight.SemiBold))
    }
}
