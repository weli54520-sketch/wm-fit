package com.wm.fit.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wm.fit.core.P

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { WMFitTheme { Root() } }
    }
}

private enum class Tab(val label: String, val icon: String) {
    HOME("الرئيسية", "🏠"),
    PLAN("الخطة", "📋"),
    SETTINGS("الإعدادات", "⚙️"),
}

@Composable
private fun Root() {
    // nonce يخلي الواجهة تعيد البناء بعد المسح أو إعادة الأسئلة
    var nonce by remember { mutableIntStateOf(0) }
    var tab by remember { mutableStateOf(Tab.HOME) }
    var sources by remember { mutableStateOf(false) }

    key(nonce) {
        if (!P.onboarded) {
            Onboarding { nonce++ }
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(C.bg)
                    .systemBarsPadding(),
            ) {
                when {
                    sources -> Sources { sources = false }
                    tab == Tab.HOME -> Home { tab = Tab.PLAN }
                    tab == Tab.PLAN -> PlanScreen()
                    else -> Settings(
                        onRestart = { nonce++; tab = Tab.HOME },
                        onSources = { sources = true },
                    )
                }

                if (!sources) {
                    BottomBar(tab) { tab = it }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.BottomBar(current: Tab, onSelect: (Tab) -> Unit) {
    Row(
        Modifier
            .align(Alignment.BottomCenter)
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .fillMaxWidth()
            .height(68.dp)
            .clip(RoundedCornerShape(34.dp))
            .background(Color(0xE0222523))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(34.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tab.entries.forEach { t ->
            val on = t == current
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onSelect(t) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(t.icon, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(3.dp))
                Text(
                    t.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (on) C.accent else C.muted,
                )
            }
        }
    }
}
