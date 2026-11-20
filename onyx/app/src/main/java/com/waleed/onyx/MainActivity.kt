package com.waleed.onyx

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.waleed.onyx.home.HomeScreen
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    // كل ما تضغط زر الهوم وأنت بالشاشة الرئيسية: نسكر اللوحات ونرجع للصفحة الأولى
    private val homePresses = MutableStateFlow(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        val app = application as OnyxApp
        setContent { HomeScreen(app, homePresses) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        homePresses.value = homePresses.value + 1
    }

    override fun onResume() {
        super.onResume()
        (application as OnyxApp).catalog.refresh()
    }
}
