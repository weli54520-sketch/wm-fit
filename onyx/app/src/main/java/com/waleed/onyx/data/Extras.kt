package com.waleed.onyx.data

import org.json.JSONObject

/** بيانات المجلد: اسمه، وهل هو "غطاء" (يبان مثل أول تطبيق فيه) */
data class FolderData(val name: String, val cover: Boolean = false) {
    fun encode(): String = JSONObject().put("name", name).put("cover", cover).toString()

    companion object {
        fun parse(s: String): FolderData = try {
            val o = JSONObject(s)
            FolderData(o.optString("name", "مجلد"), o.optBoolean("cover", false))
        } catch (e: Exception) {
            FolderData("مجلد")
        }
    }
}

/** بيانات أيقونة التطبيق: التطبيق الثاني اللي ينفتح بالضغطتين */
data class AppData(val alt: String = "") {
    fun encode(): String = if (alt.isEmpty()) "" else JSONObject().put("alt", alt).toString()

    companion object {
        fun parse(s: String): AppData = try {
            if (s.isBlank()) AppData() else AppData(JSONObject(s).optString("alt", ""))
        } catch (e: Exception) {
            AppData()
        }
    }
}

/** وش تسوي الإيماءة */
object GestureAction {
    const val NONE = 0
    const val LOCK = 1
    const val NOTIFICATIONS = 2
    const val QUICK = 3
    const val DRAWER = 4
    const val SEARCH = 5
    const val EDIT = 6
    const val SETTINGS = 7
    const val CAMERA = 8

    val labels = listOf("ولا شي", "قفل الشاشة", "الإشعارات", "الإعدادات السريعة", "كل التطبيقات", "البحث", "ترتيب الشاشة", "إعدادات Onyx", "الكاميرا")
}
