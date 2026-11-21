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

    // ───────── 0.4 ─────────

    private fun strList(key: String): MutableList<String> =
        (p.getString(key, "") ?: "").split('\n').filter { it.isNotBlank() }.toMutableList()

    private val _recents = mutableStateOf(strList("recents").toList())
    /** آخر التطبيقات اللي فتحتها (الجديد أول) */
    var recents: List<String>
        get() = _recents.value
        set(v) { _recents.value = v; p.edit().putString("recents", v.joinToString("\n")).apply() }

    private val _searches = mutableStateOf(strList("searches").toList())
    /** آخر عمليات البحث */
    var searches: List<String>
        get() = _searches.value
        set(v) { _searches.value = v; p.edit().putString("searches", v.joinToString("\n")).apply() }

    private val _hidden = mutableStateOf(strList("hidden").toSet())
    /** التطبيقات المخفية (ما تبان بالدرج ولا البحث) */
    var hidden: Set<String>
        get() = _hidden.value
        set(v) { _hidden.value = v; p.edit().putString("hidden", v.joinToString("\n")).apply() }

    // نقطة الإشعارات على الأيقونة
    private val _dots = bool("dots", true)
    var dots: Boolean
        get() = _dots.value
        set(v) { _dots.value = v; p.edit().putBoolean("dots", v).apply() }

    // الدرج: 0 بالفئات، 1 أبجدي
    private val _drawerSort = int("drawerSort", 0)
    var drawerSort: Int
        get() = _drawerSort.intValue
        set(v) { _drawerSort.intValue = v; p.edit().putInt("drawerSort", v).apply() }

    // خلفية المجلد المفتوح: 0 تضبيب، 1 تغميق
    private val _folderBg = int("folderBg", 0)
    var folderBg: Int
        get() = _folderBg.intValue
        set(v) { _folderBg.intValue = v; p.edit().putInt("folderBg", v).apply() }

    // الإيماءات (GestureAction)
    private val _gDouble = int("gDouble", GestureAction.LOCK)
    private val _gPinchIn = int("gPinchIn", GestureAction.EDIT)
    private val _gPinchOut = int("gPinchOut", GestureAction.DRAWER)
    private val _gTwoUp = int("gTwoUp", GestureAction.SEARCH)
    private val _gTwoDown = int("gTwoDown", GestureAction.QUICK)

    fun gesture(name: String): Int = when (name) {
        "double" -> _gDouble.intValue
        "pinchIn" -> _gPinchIn.intValue
        "pinchOut" -> _gPinchOut.intValue
        "twoUp" -> _gTwoUp.intValue
        "twoDown" -> _gTwoDown.intValue
        else -> GestureAction.NONE
    }

    fun setGesture(name: String, v: Int) {
        when (name) {
            "double" -> _gDouble.intValue = v
            "pinchIn" -> _gPinchIn.intValue = v
            "pinchOut" -> _gPinchOut.intValue = v
            "twoUp" -> _gTwoUp.intValue = v
            "twoDown" -> _gTwoDown.intValue = v
            else -> return
        }
        p.edit().putInt("g" + name.replaceFirstChar { it.uppercase() }, v).apply()
    }

    /** ويدجت منبثق لكل تطبيق: رقم الويدجت المحفوظ (عشان ما نطلب الإذن كل مرة) */
    fun popupId(pkg: String): Int = p.getInt("popup_$pkg", -1)
    fun setPopupId(pkg: String, id: Int) { p.edit().putInt("popup_$pkg", id).apply() }

    /** كل الإعدادات (للنسخة الاحتياطية) */
    fun exportAll(): Map<String, *> = p.all

    fun importAll(m: Map<String, Any?>) {
        val e = p.edit()
        for ((k, v) in m) when (v) {
            is Boolean -> e.putBoolean(k, v)
            is Int -> e.putInt(k, v)
            is Long -> e.putInt(k, v.toInt())
            is Float -> e.putFloat(k, v)
            is Double -> e.putFloat(k, v.toFloat())
            is String -> e.putString(k, v)
        }
        e.apply()
    }

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
        val ROWS_RANGE = mapOf(Screen.OUTER to 5..8, Screen.INNER to 4..6)
    }
}
