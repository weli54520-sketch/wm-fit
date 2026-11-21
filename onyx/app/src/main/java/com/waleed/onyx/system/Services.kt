package com.waleed.onyx.system

import android.accessibilityservice.AccessibilityService
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** آخر إشعار لكل تطبيق (للنقطة على الأيقونة، ونصه بالضغطة الطويلة) */
data class AppNotif(val pkg: String, val title: String, val text: String, val count: Int, val intent: PendingIntent?)

object Notifs {
    private val _byPkg = MutableStateFlow<Map<String, AppNotif>>(emptyMap())
    val byPkg: StateFlow<Map<String, AppNotif>> = _byPkg

    fun set(list: List<StatusBarNotification>) {
        val m = HashMap<String, AppNotif>()
        for (sbn in list) {
            if (sbn.isOngoing || !sbn.isClearable) continue
            val n = sbn.notification ?: continue
            if (n.flags and android.app.Notification.FLAG_GROUP_SUMMARY != 0) continue
            val ex = n.extras
            val title = ex?.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString().orEmpty()
            val text = ex?.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString().orEmpty()
            val prev = m[sbn.packageName]
            // الأحدث يفوز بالنص، والعدد يتجمع
            if (prev == null || sbn.postTime >= 0) {
                m[sbn.packageName] = AppNotif(sbn.packageName, title, text, (prev?.count ?: 0) + 1, n.contentIntent ?: prev?.intent)
            }
        }
        _byPkg.value = m
    }

    fun granted(c: Context): Boolean {
        val s = Settings.Secure.getString(c.contentResolver, "enabled_notification_listeners") ?: return false
        val me = ComponentName(c, NotifService::class.java).flattenToString()
        return s.split(':').any { it == me }
    }
}

/** يسمع الإشعارات (بعد ما توافق مرة وحدة بإعدادات الجوال) */
class NotifService : NotificationListenerService() {
    private fun refresh() {
        try { Notifs.set(activeNotifications?.toList() ?: emptyList()) } catch (_: Exception) {}
    }
    override fun onListenerConnected() = refresh()
    override fun onNotificationPosted(sbn: StatusBarNotification?) = refresh()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()
    override fun onListenerDisconnected() { Notifs.set(emptyList()) }
}

/**
 * قفل الشاشة بالضغطتين. أندرويد ما يسمح للانشر يقفل الشاشة إلا عن طريق "إمكانية الوصول"،
 * وهذي الخدمة ما تقرا ولا تسوي أي شي ثاني.
 */
class LockService : AccessibilityService() {
    override fun onServiceConnected() { instance = this }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onDestroy() { if (instance === this) instance = null; super.onDestroy() }

    companion object {
        @Volatile var instance: LockService? = null
        fun lock(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) == true
    }
}
