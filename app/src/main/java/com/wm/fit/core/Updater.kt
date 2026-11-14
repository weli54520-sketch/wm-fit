package com.wm.fit.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.wm.fit.BuildConfig
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * التحديث من داخل التطبيق.
 * ما يتصل بالنت إلا لما المستخدم يضغط "دوّر على تحديث"، ويقرأ آخر إصدار من صفحة
 * Releases في المستودع العام. الإصدارات تطلع بس من main.
 */
object Updater {
    private const val REPO = "weli54520-sketch/wm-fit"
    private const val APK = "wm-fit.apk"

    data class Release(val number: Int, val name: String, val apkUrl: String)

    sealed class Check {
        data class Available(val r: Release) : Check()
        object UpToDate : Check()
        object NoRelease : Check()
        data class Err(val text: String) : Check()
    }

    val current: Int get() = BuildConfig.VERSION_CODE

    /** يشيك آخر إصدار. يشتغل على خيط خلفي. */
    fun check(): Check = runCatching {
        val c = URL("https://api.github.com/repos/$REPO/releases/latest").openConnection() as HttpURLConnection
        c.connectTimeout = 15_000
        c.readTimeout = 15_000
        c.setRequestProperty("Accept", "application/vnd.github+json")
        if (c.responseCode == 404) return Check.NoRelease
        if (c.responseCode != 200) return Check.Err("ما قدرت أوصل لصفحة الإصدارات (${c.responseCode}).")
        val o = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
        val tag = o.getString("tag_name")
        val n = tag.removePrefix("v").toIntOrNull() ?: return Check.Err("رقم الإصدار غريب: $tag")
        val assets = o.getJSONArray("assets")
        val url = (0 until assets.length()).map { assets.getJSONObject(it) }
            .firstOrNull { it.getString("name") == APK }?.getString("browser_download_url")
            ?: return Check.Err("الإصدار ما فيه ملف التطبيق.")
        val r = Release(n, o.optString("name", tag), url)
        if (n > current) Check.Available(r) else Check.UpToDate
    }.getOrElse { Check.Err("تأكد من النت وجرّب مرة ثانية.") }

    /** ينزّل الملف. [progress] من 0 إلى 1. */
    fun download(ctx: Context, r: Release, progress: (Float) -> Unit): File? = runCatching {
        val out = File(ctx.cacheDir, "update.apk")
        val c = URL(r.apkUrl).openConnection() as HttpURLConnection
        c.connectTimeout = 15_000
        c.readTimeout = 30_000
        c.instanceFollowRedirects = true
        val total = c.contentLengthLong.coerceAtLeast(1L)
        var got = 0L
        c.inputStream.use { inp ->
            out.outputStream().use { o ->
                val buf = ByteArray(64 * 1024)
                while (true) {
                    val n = inp.read(buf)
                    if (n < 0) break
                    o.write(buf, 0, n)
                    got += n
                    progress((got.toFloat() / total).coerceIn(0f, 1f))
                }
            }
        }
        out
    }.getOrNull()

    /** هل الجوال سامح للتطبيق يركّب تحديثات؟ */
    fun canInstall(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 26 || ctx.packageManager.canRequestPackageInstalls()

    /** يفتح إعداد "السماح بالتثبيت من هذا التطبيق". */
    fun openInstallSetting(ctx: Context) {
        val i = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { ctx.startActivity(i) }
    }

    /** يفتح شاشة التثبيت من أندرويد. */
    fun install(ctx: Context, apk: File) {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", apk)
        val i = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { ctx.startActivity(i) }
    }
}
