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
import kotlinx.coroutines.launch

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
        app.widgets.activity = this
        maybeTestFill(intent)
        setContent { HomeScreen(app, homePresses) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        homePresses.value = homePresses.value + 1
        maybeTestFill(intent)
    }

    /** للجوال الوهمي بس: adb يطلب صفحات مليانة عشان نختبر التقليب والنقل */
    private fun maybeTestFill(i: Intent?) {
        // للجوال الوهمي: يشغل أو يطفي "قفل الترتيب" (عشان نختبر إنه ما يحبس الإعدادات)
        i?.getStringExtra("onyx_test_lock")?.let { v ->
            if (v.startsWith("wm-emu-2026:")) {
                (application as OnyxApp).settings.lock = v.endsWith(":on")
                i?.removeExtra("onyx_test_lock")
            }
        }
        if (i?.getStringExtra("onyx_test_fill") != "wm-emu-2026") return
        i.removeExtra("onyx_test_fill")
        val app = application as OnyxApp
        kotlinx.coroutines.MainScope().launch { app.repo.testFill(3) }
    }

    // الويدجت تتحدث بس والشاشة ظاهرة (توفير بطارية)
    override fun onStart() {
        super.onStart()
        (application as OnyxApp).widgets.startListening()
    }

    override fun onStop() {
        super.onStop()
        (application as OnyxApp).widgets.stopListening()
    }

    override fun onDestroy() {
        val w = (application as OnyxApp).widgets
        if (w.activity === this) w.activity = null
        super.onDestroy()
    }

    // نتيجة نافذة إذن الويدجت أو إعداداته
    @Deprecated("AppWidgetHost uses request codes")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        (application as OnyxApp).widgets.onActivityResult(requestCode, resultCode)
    }

    override fun onResume() {
        super.onResume()
        (application as OnyxApp).catalog.refresh()
    }
}
