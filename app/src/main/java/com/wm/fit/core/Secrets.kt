package com.wm.fit.core

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager

/**
 * مفتاح Claude الخاص بالمستخدم.
 *
 * - المفتاح **ما يدخل الكود أبداً**. المستخدم يحطه بنفسه داخل التطبيق.
 * - يتشفّر بـ Tink (AES-256-GCM)، ومفتاح التشفير نفسه محفوظ في Android Keystore
 *   (داخل شريحة الأمان في الجوال، ما يطلع منها).
 * - ملف منفصل عن باقي البيانات، وما يدخل في النسخة الاحتياطية.
 */
object Secrets {
    private const val KEYSET_NAME = "wmfit_keyset"
    private const val KEYSET_PREFS = "wmfit_keyset_prefs"
    private const val MASTER_KEY = "android-keystore://wmfit_master"
    private const val PREFS = "wmfit_secret"
    private const val K_API = "anthropic_key"
    private val AD = "wmfit:anthropic".toByteArray()

    private lateinit var app: Context
    private var aead: Aead? = null
    private val sp: SharedPreferences get() = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun init(c: Context) {
        app = c.applicationContext
    }

    /** نجهّز التشفير أول ما نحتاجه (Keystore ممكن ياخذ وقت). */
    @Synchronized
    private fun crypto(): Aead {
        aead?.let { return it }
        AeadConfig.register()
        val handle = AndroidKeysetManager.Builder()
            .withSharedPref(app, KEYSET_NAME, KEYSET_PREFS)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(MASTER_KEY)
            .build()
            .keysetHandle
        return handle.getPrimitive(RegistryConfiguration.get(), Aead::class.java).also { aead = it }
    }

    fun hasKey(): Boolean = sp.contains(K_API)

    /** يحفظ المفتاح مشفّر. ترجع false لو صار خطأ. */
    fun setKey(key: String): Boolean = runCatching {
        val enc = crypto().encrypt(key.trim().toByteArray(), AD)
        sp.edit().putString(K_API, Base64.encodeToString(enc, Base64.NO_WRAP)).apply()
    }.isSuccess

    /** يفك تشفير المفتاح وقت الإرسال بس. */
    fun key(): String? = runCatching {
        val b64 = sp.getString(K_API, null) ?: return null
        String(crypto().decrypt(Base64.decode(b64, Base64.NO_WRAP), AD))
    }.getOrNull()

    /** آخر 4 حروف بس، عشان المستخدم يعرف أي مفتاح محفوظ. */
    fun hint(): String? = key()?.takeLast(4)

    fun clear() {
        sp.edit().clear().apply()
    }
}
