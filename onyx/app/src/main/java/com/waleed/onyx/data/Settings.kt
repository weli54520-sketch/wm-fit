package com.waleed.onyx.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** إعدادات اللانشر. كل تغيير ينحفظ مباشرة وينطبق على الشاشة */
class Settings(context: Context) {
    private val p = context.getSharedPreferences("onyx_settings", Context.MODE_PRIVATE)

    private fun int(key: String, def: Int) = mutableIntStateOf(p.getInt(key, def))
    private fun bool(key: String, def: Boolean) = mutableStateOf(p.getBoolean(key, def))
    private fun float(key: String, def: Float) = mutableFloatStateOf(p.getFloat(key, def))

    // الأيقونات: 0 الأصلية، 1 زجاج شفاف، 2 زجاج داكن
    private val _iconStyle = int("iconStyle", 0)
    var iconStyle: Int
        get() = _iconStyle.intValue
        set(v) { _iconStyle.intValue = v; p.edit().putInt("iconStyle", v).apply() }

    private val _iconScale = float("iconScale", 1f)
    var iconScale: Float
        get() = _iconScale.floatValue
        set(v) { _iconScale.floatValue = v; p.edit().putFloat("iconScale", v).apply() }

    private val _labels = bool("labels", false)
    var labels: Boolean
        get() = _labels.value
        set(v) { _labels.value = v; p.edit().putBoolean("labels", v).apply() }

    private val _sideOn = bool("sideOn", true)
    var sideOn: Boolean
        get() = _sideOn.value
        set(v) { _sideOn.value = v; p.edit().putBoolean("sideOn", v).apply() }

    private val _sideCount = int("sideCount", 4)
    var sideCount: Int
        get() = _sideCount.intValue
        set(v) { _sideCount.intValue = v; p.edit().putInt("sideCount", v).apply() }

    // الشريط السفلي اختياري (العمود الجانبي هو الأساسي)
    private val _dockOn = bool("dockOn", true)
    var dockOn: Boolean
        get() = _dockOn.value
        set(v) { _dockOn.value = v; p.edit().putBoolean("dockOn", v).apply() }

    private val _dockCount = int("dockCount", 6)
    var dockCount: Int
        get() = _dockCount.intValue
        set(v) { _dockCount.intValue = v; p.edit().putInt("dockCount", v).apply() }

    // الحركة: السرعة 0 هادئة 1 عادية 2 سريعة، النبض 0 خفيف 1 متوسط 2 قوي
    private val _speed = int("speed", 1)
    var speed: Int
        get() = _speed.intValue
        set(v) { _speed.intValue = v; p.edit().putInt("speed", v).apply() }

    private val _bounce = int("bounce", 1)
    var bounce: Int
        get() = _bounce.intValue
        set(v) { _bounce.intValue = v; p.edit().putInt("bounce", v).apply() }

    // وضع الترتيب: 0 الصفحة تصغر، 1 اهتزاز خفيف
    private val _editStyle = int("editStyle", 0)
    var editStyle: Int
        get() = _editStyle.intValue
        set(v) { _editStyle.intValue = v; p.edit().putInt("editStyle", v).apply() }

    private val _haptics = bool("haptics", true)
    var haptics: Boolean
        get() = _haptics.value
        set(v) { _haptics.value = v; p.edit().putBoolean("haptics", v).apply() }

    private val _reduceMotion = bool("reduceMotion", false)
    var reduceMotion: Boolean
        get() = _reduceMotion.value
        set(v) { _reduceMotion.value = v; p.edit().putBoolean("reduceMotion", v).apply() }

    private val _lock = bool("lock", false)
    var lock: Boolean
        get() = _lock.value
        set(v) { _lock.value = v; p.edit().putBoolean("lock", v).apply() }

    // الويدجت الجديدة تنحط بإطار زجاج مدخّن؟ (كل ويدجت تقدر تغيره من قائمته)
    private val _widgetFrame = bool("widgetFrame", true)
    var widgetFrame: Boolean
        get() = _widgetFrame.value
        set(v) { _widgetFrame.value = v; p.edit().putBoolean("widgetFrame", v).apply() }

    // الشبكة لكل شاشة
    private val _outerCols = int("outerCols", 4)
    private val _outerRows = int("outerRows", 6)
    private val _innerCols = int("innerCols", 6)
    private val _innerRows = int("innerRows", 5)

    fun cols(screen: Int) = if (screen == Screen.INNER) _innerCols.intValue else _outerCols.intValue
    fun rows(screen: Int) = if (screen == Screen.INNER) _innerRows.intValue else _outerRows.intValue

    fun setCols(screen: Int, v: Int) {
        if (screen == Screen.INNER) { _innerCols.intValue = v; p.edit().putInt("innerCols", v).apply() }
        else { _outerCols.intValue = v; p.edit().putInt("outerCols", v).apply() }
    }

    fun setRows(screen: Int, v: Int) {
        if (screen == Screen.INNER) { _innerRows.intValue = v; p.edit().putInt("innerRows", v).apply() }
        else { _outerRows.intValue = v; p.edit().putInt("outerRows", v).apply() }
    }

    companion object {
        val COLS_RANGE = mapOf(Screen.OUTER to 4..5, Screen.INNER to 5..7)
        val ROWS_RANGE = mapOf(Screen.OUTER to 5..7, Screen.INNER to 4..6)
    }
}
