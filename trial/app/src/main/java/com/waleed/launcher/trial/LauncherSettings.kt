package com.waleed.launcher.trial

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** أشكال الأيقونات اللي يختار منها وليد */
enum class IconStyle(val id: String, val title: String) {
    ORIGINAL("original", "الأصلية"),
    GLASS_CLEAR("glass_clear", "زجاج شفاف"),
    GLASS_DARK("glass_dark", "زجاج داكن"),
}

/** الإعدادات تنحفظ بالجوال، وما تضيع لو سكرت اللانشر أو حدثته */
class LauncherSettings(context: Context) {
    private val prefs = context.getSharedPreferences("launcher_settings", Context.MODE_PRIVATE)

    var iconStyle by mutableStateOf(
        IconStyle.entries.firstOrNull { it.id == prefs.getString("icon_style", null) } ?: IconStyle.ORIGINAL,
    )
        private set

    fun updateIconStyle(style: IconStyle) {
        iconStyle = style
        prefs.edit().putString("icon_style", style.id).apply()
    }
}
