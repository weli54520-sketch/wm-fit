package com.waleed.onyx.widget

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.UserManager
import android.util.Log
import org.json.JSONObject

/** بيانات الويدجت المحفوظة بسطر العنصر */
data class WidgetData(val id: Int, val provider: String, val frame: Boolean) {
    fun encode(): String = JSONObject().put("id", id).put("provider", provider).put("frame", frame).toString()

    companion object {
        fun parse(s: String): WidgetData? = try {
            val o = JSONObject(s)
            WidgetData(o.getInt("id"), o.optString("provider"), o.optBoolean("frame", true))
        } catch (e: Exception) {
            null
        }
    }
}

/** ويدجت ننتظر الإذن أو إعداداته قبل ما ينحط بالشاشة */
class PendingWidget(val info: AppWidgetProviderInfo, val id: Int, val onDone: (Int) -> Unit)

/**
 * ويدجت التطبيقات الثانية: الإذن، الإعداد، والعرض.
 * اللانشر "مضيف" (AppWidgetHost): التطبيق يرسل شكل الويدجت، وإحنا نعرضه بالمكان اللي تختاره.
 */
class Widgets(private val context: Context) {
    val manager: AppWidgetManager = AppWidgetManager.getInstance(context)
    val host = AppWidgetHost(context, HOST_ID)
    private val users = context.getSystemService(UserManager::class.java)

    /** الشاشة الحالية، عشان نطلب الإذن ونفتح إعدادات الويدجت */
    var activity: Activity? = null
    private var pending: PendingWidget? = null

    fun startListening() = try { host.startListening() } catch (e: Exception) { Log.w(TAG, "listen", e) }
    fun stopListening() = try { host.stopListening() } catch (e: Exception) { Log.w(TAG, "stop", e) }

    /** كل الويدجت اللي بالجوال (لكل الحسابات) */
    fun providers(): List<AppWidgetProviderInfo> =
        users.userProfiles.flatMap { u -> try { manager.getInstalledProvidersForProfile(u) } catch (e: Exception) { emptyList() } }
            .filter { it.widgetCategory and AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN != 0 }

    fun info(id: Int): AppWidgetProviderInfo? = try { manager.getAppWidgetInfo(id) } catch (e: Exception) { null }

    fun delete(id: Int) = try { host.deleteAppWidgetId(id) } catch (e: Exception) { Log.w(TAG, "delete", e) }

    /** يقدر ينفتح له إعدادات بعد ما ينحط؟ */
    fun canReconfigure(info: AppWidgetProviderInfo?): Boolean =
        info?.configure != null && (info.widgetFeatures and AppWidgetProviderInfo.WIDGET_FEATURE_RECONFIGURABLE != 0)

    /** نبدأ نضيف ويدجت: الإذن، بعدين الإعداد، بعدين onDone بالرقم */
    fun begin(info: AppWidgetProviderInfo, onDone: (Int) -> Unit) {
        cancelPending()
        val id = host.allocateAppWidgetId()
        val p = PendingWidget(info, id, onDone)
        pending = p
        val ok = try { manager.bindAppWidgetIdIfAllowed(id, info.profile, info.provider, null) } catch (e: Exception) { false }
        if (ok) { configure(p); return }
        val act = activity ?: return cancelPending()
        val i = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, info.profile)
        try {
            @Suppress("DEPRECATION")
            act.startActivityForResult(i, REQ_BIND)
        } catch (e: Exception) {
            Log.w(TAG, "bind", e)
            cancelPending()
        }
    }

    private fun configure(p: PendingWidget) {
        val act = activity
        val optional = p.info.widgetFeatures and AppWidgetProviderInfo.WIDGET_FEATURE_CONFIGURATION_OPTIONAL != 0
        if (p.info.configure == null || optional || act == null) { finish(p); return }
        try {
            host.startAppWidgetConfigureActivityForResult(act, p.id, 0, REQ_CONFIG, null)
        } catch (e: Exception) {
            Log.w(TAG, "configure", e)
            finish(p)
        }
    }

    /** يفتح إعدادات ويدجت موجود */
    fun reconfigure(id: Int) {
        val act = activity ?: return
        try { host.startAppWidgetConfigureActivityForResult(act, id, 0, REQ_RECONFIG, null) } catch (e: Exception) { Log.w(TAG, "reconfigure", e) }
    }

    private fun finish(p: PendingWidget) {
        pending = null
        p.onDone(p.id)
    }

    private fun cancelPending() {
        pending?.let { delete(it.id) }
        pending = null
    }

    /** نتيجة نافذة الإذن أو الإعداد */
    fun onActivityResult(requestCode: Int, resultCode: Int): Boolean {
        val p = pending
        when (requestCode) {
            REQ_BIND -> {
                if (p == null) return true
                if (resultCode == Activity.RESULT_OK) configure(p) else cancelPending()
            }
            REQ_CONFIG -> {
                if (p == null) return true
                if (resultCode == Activity.RESULT_OK) finish(p) else cancelPending()
            }
            REQ_RECONFIG -> {}
            else -> return false
        }
        return true
    }

    companion object {
        const val HOST_ID = 2611
        private const val TAG = "OnyxWidgets"
        private const val REQ_BIND = 7101
        private const val REQ_CONFIG = 7102
        private const val REQ_RECONFIG = 7103

        fun providerKey(c: ComponentName): String = c.flattenToShortString()
    }
}
