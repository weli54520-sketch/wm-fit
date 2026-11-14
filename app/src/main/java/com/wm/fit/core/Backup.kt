package com.wm.fit.core

import android.util.Base64
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.SecureRandom
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * النسخة الاحتياطية: ملف يحفظه المستخدم بنفسه، مقفل بكلمة سر.
 *
 * - التشفير: AES-256-GCM، والمفتاح من كلمة السر بـ PBKDF2 (HMAC-SHA256).
 * - بدون كلمة السر الملف ما ينفتح. لو نسيها، ما فيه طريقة نرجعه.
 * - مفتاح Claude **ما يدخل** في النسخة (محفوظ في ملف ثاني مشفّر بالجوال).
 */
object Backup {
    private val MAGIC = "WMFIT1".toByteArray()
    private const val ITER = 120_000
    const val MIN_PASSWORD = 6

    sealed class Result {
        object Ok : Result()
        data class Err(val text: String) : Result()
    }

    private fun key(pw: CharArray, salt: ByteArray): SecretKeySpec {
        val f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val k = f.generateSecret(PBEKeySpec(pw, salt, ITER, 256)).encoded
        return SecretKeySpec(k, "AES")
    }

    /** يجهّز ملف النسخة (مضغوط ومشفّر). */
    fun export(pw: CharArray, photos: Boolean): ByteArray {
        val prefs = JSONObject()
        P.sp.all.forEach { (k, v) ->
            val o = when (v) {
                is Boolean -> JSONObject().put("t", "b").put("v", v)
                is Int -> JSONObject().put("t", "i").put("v", v)
                is Long -> JSONObject().put("t", "l").put("v", v)
                is Float -> JSONObject().put("t", "f").put("v", v.toDouble())
                is String -> JSONObject().put("t", "s").put("v", v)
                else -> null
            }
            if (o != null) prefs.put(k, o)
        }
        val root = JSONObject().put("v", 1).put("prefs", prefs)
        if (photos) {
            val ph = JSONObject()
            Diary.photos.forEach { p ->
                val f = File(Diary.photoDir ?: return@forEach, p.file)
                if (f.exists()) ph.put(p.file, Base64.encodeToString(f.readBytes(), Base64.NO_WRAP))
            }
            root.put("photos", ph)
        }
        val plain = ByteArrayOutputStream().also { bo ->
            GZIPOutputStream(bo).use { it.write(root.toString().toByteArray()) }
        }.toByteArray()

        val rnd = SecureRandom()
        val salt = ByteArray(16).also { rnd.nextBytes(it) }
        val iv = ByteArray(12).also { rnd.nextBytes(it) }
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, key(pw, salt), GCMParameterSpec(128, iv))
        c.updateAAD(MAGIC)
        return MAGIC + salt + iv + c.doFinal(plain)
    }

    /** يسترجع النسخة. يمسح البيانات الحالية ويحط اللي في الملف. */
    fun import(data: ByteArray, pw: CharArray): Result {
        if (data.size < MAGIC.size + 28 || !data.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) {
            return Result.Err("هذا مو ملف نسخة احتياطية من WM Fit.")
        }
        val salt = data.copyOfRange(MAGIC.size, MAGIC.size + 16)
        val iv = data.copyOfRange(MAGIC.size + 16, MAGIC.size + 28)
        val body = data.copyOfRange(MAGIC.size + 28, data.size)
        val plain = runCatching {
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.DECRYPT_MODE, key(pw, salt), GCMParameterSpec(128, iv))
            c.updateAAD(MAGIC)
            c.doFinal(body)
        }.getOrElse { return Result.Err("كلمة السر غلط.") }

        return runCatching {
            val json = GZIPInputStream(ByteArrayInputStream(plain)).use { String(it.readBytes()) }
            val root = JSONObject(json)
            val prefs = root.getJSONObject("prefs")
            val e = P.sp.edit().clear()
            prefs.keys().forEach { k ->
                val o = prefs.getJSONObject(k)
                when (o.getString("t")) {
                    "b" -> e.putBoolean(k, o.getBoolean("v"))
                    "i" -> e.putInt(k, o.getInt("v"))
                    "l" -> e.putLong(k, o.getLong("v"))
                    "f" -> e.putFloat(k, o.getDouble("v").toFloat())
                    "s" -> e.putString(k, o.getString("v"))
                }
            }
            e.apply()
            root.optJSONObject("photos")?.let { ph ->
                Diary.deleteAllPhotos()
                val dir = Diary.photoDir
                if (dir != null) ph.keys().forEach { name ->
                    // اسم الملف بس، بدون مسارات
                    val safe = File(name).name
                    File(dir, safe).writeBytes(Base64.decode(ph.getString(name), Base64.NO_WRAP))
                }
            }
            Pref.reloadAll()
            Store.load()
            Diary.load()
            Result.Ok
        }.getOrElse { Result.Err("الملف خربان أو ناقص.") }
    }

    /** اسم الملف المقترح. */
    fun fileName(): String = "wmfit-${Store.today()}.wmfit"
}
