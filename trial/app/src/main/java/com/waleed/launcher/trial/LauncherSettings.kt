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

    /** تطبيقات العمود الجانبي والشريط اللي اختارها وليد (فاضية = الافتراضي) */
    var sideKeys by mutableStateOf(load("side_apps"))
        private set
    var dockKeys by mutableStateOf(load("dock_apps"))
        private set

    private fun load(name: String): List<String> =
        prefs.getString(name, null)?.split("|")?.filter { it.isNotBlank() } ?: emptyList()

    fun updateSide(keys: List<String>) {
        sideKeys = keys
        prefs.edit().putString("side_apps", keys.joinToString("|")).apply()
    }

    fun updateDock(keys: List<String>) {
        dockKeys = keys
        prefs.edit().putString("dock_apps", keys.joinToString("|")).apply()
    }

    fun updateIconStyle(style: IconStyle) {
        iconStyle = style
        prefs.edit().putString("icon_style", style.id).apply()
    }
}
