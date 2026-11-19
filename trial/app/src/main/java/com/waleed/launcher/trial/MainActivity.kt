package com.waleed.launcher.trial

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.waleed.launcher.trial.widgets.LocationStore
import com.waleed.launcher.trial.widgets.WeatherStore
import com.waleed.launcher.trial.widgets.WidgetStore
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    // كل ما تضغط زر الهوم وأنت بالشاشة الرئيسية، نرجع للصفحة الأولى
    private val homePresses = MutableStateFlow(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        val repository = AppRepository(applicationContext)
        val settings = LauncherSettings(applicationContext)
        val widgets = WidgetStore(applicationContext)
        val location = LocationStore(applicationContext)
        val weather = WeatherStore(applicationContext)
        setContent {
            HomeScreen(
                repository = repository,
                settings = settings,
                widgetStore = widgets,
                location = location,
                weather = weather,
                homePresses = homePresses,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        homePresses.value = homePresses.value + 1
    }
}
