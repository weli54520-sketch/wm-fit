package com.wm.fit.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** أكلة مسجلة في يوم. [n] = كم حصة. */
data class Eaten(val foodId: String, val name: String, val n: Float, val kcal: Int, val p: Float, val c: Float, val f: Float)

/** قياس بتاريخ (وزن أو خصر). */
data class Measure(val date: String, val v: Float)

/** صورة تقدم: اسم الملف داخل ذاكرة التطبيق الخاصة. */
data class Photo(val date: String, val file: String)

/** مجموع يوم. */
data class Totals(val kcal: Int, val p: Float, val c: Float, val f: Float)

/**
 * يوميات الأكل والجسم: الأكل، الموية، الوزن، الخصر، الصور، والمكملات.
 *
 * الخصوصية: كله داخل الجوال. الصور في ذاكرة التطبيق الخاصة (filesDir/photos)،
 * يعني ما تطلع في معرض الصور، وما يقدر يشوفها أي تطبيق ثاني.
 */
object Diary {
    private const val KEEP_DAYS = 90

    /** أكل كل يوم: التاريخ ← الأكلات. */
    var food by mutableStateOf<Map<String, List<Eaten>>>(emptyMap())
        private set

    /** كاسات الموية لكل يوم (الكاسة 250 مل). */
    var water by mutableStateOf<Map<String, Int>>(emptyMap())
        private set

    var weights by mutableStateOf<List<Measure>>(emptyList())
        private set

    var waists by mutableStateOf<List<Measure>>(emptyList())
        private set

    var photos by mutableStateOf<List<Photo>>(emptyList())
        private set

    var customFoods by mutableStateOf<List<Food>>(emptyList())
        private set

    /** المكملات اللي أخذها كل يوم: التاريخ ← المعرّفات. */
    var supps by mutableStateOf<Map<String, Set<String>>>(emptyMap())
        private set

    /** مكان الصور. ينضبط من App. */
    var photoDir: File? = null

    private val sp get() = P.sp

    fun load() {
        food = runCatching {
            val o = JSONObject(sp.getString("food", "{}") ?: "{}")
            o.keys().asSequence().associateWith { d ->
                val a = o.getJSONArray(d)
                (0 until a.length()).map { i ->
                    val e = a.getJSONObject(i)
                    Eaten(
                        e.optString("id"), e.getString("name"), e.optDouble("n", 1.0).toFloat(),
                        e.getInt("k"), e.getDouble("p").toFloat(), e.getDouble("c").toFloat(), e.getDouble("f").toFloat(),
                    )
                }
            }
        }.getOrDefault(emptyMap())

        water = runCatching {
            val o = JSONObject(sp.getString("water", "{}") ?: "{}")
            o.keys().asSequence().associateWith { o.getInt(it) }
        }.getOrDefault(emptyMap())

        weights = readMeasures("weights")
        waists = readMeasures("waists")

        photos = runCatching {
            val a = JSONArray(sp.getString("photos", "[]") ?: "[]")
            (0 until a.length()).map { val o = a.getJSONObject(it); Photo(o.getString("d"), o.getString("f")) }
        }.getOrDefault(emptyList())

        customFoods = runCatching {
            val a = JSONArray(sp.getString("customFoods", "[]") ?: "[]")
            (0 until a.length()).map {
                val o = a.getJSONObject(it)
                Food(
                    o.getString("id"), o.getString("ar"), o.optString("portion", "حصة"), o.getInt("k"),
                    o.getDouble("p").toFloat(), o.getDouble("c").toFloat(), o.getDouble("f").toFloat(),
                    "أكلاتي", custom = true,
                )
            }
        }.getOrDefault(emptyList())

        supps = runCatching {
            val o = JSONObject(sp.getString("supps", "{}") ?: "{}")
            o.keys().asSequence().associateWith { d ->
                val a = o.getJSONArray(d)
                (0 until a.length()).map { a.getString(it) }.toSet()
            }
        }.getOrDefault(emptyMap())
    }

    private fun readMeasures(key: String): List<Measure> = runCatching {
        val a = JSONArray(sp.getString(key, "[]") ?: "[]")
        (0 until a.length()).map { val o = a.getJSONArray(it); Measure(o.getString(0), o.getDouble(1).toFloat()) }
            .sortedBy { it.date }
    }.getOrDefault(emptyList())

    private fun writeMeasures(key: String, list: List<Measure>) {
        val a = JSONArray()
        list.forEach { a.put(JSONArray().put(it.date).put(it.v.toDouble())) }
        sp.edit().putString(key, a.toString()).apply()
    }

    /** نخلي آخر 90 يوم بس من الأكل والموية والمكملات. */
    private fun <T> trim(m: Map<String, T>): Map<String, T> =
        m.keys.sorted().takeLast(KEEP_DAYS).associateWith { m.getValue(it) }

    // ===== الأكل =====

    fun eatenOn(date: String = Store.today()): List<Eaten> = food[date].orEmpty()

    fun totals(date: String = Store.today()): Totals {
        val l = eatenOn(date)
        return Totals(l.sumOf { it.kcal }, l.sumOf { it.p.toDouble() }.toFloat(),
            l.sumOf { it.c.toDouble() }.toFloat(), l.sumOf { it.f.toDouble() }.toFloat())
    }

    fun eat(f: Food, n: Float) {
        val e = Eaten(f.id, f.ar, n, (f.kcal * n).toInt(), f.p * n, f.c * n, f.f * n)
        val t = Store.today()
        food = trim(food + (t to eatenOn(t) + e))
        saveFood()
    }

    fun removeEaten(index: Int) {
        val t = Store.today()
        val l = eatenOn(t).toMutableList()
        if (index !in l.indices) return
        l.removeAt(index)
        food = if (l.isEmpty()) food - t else food + (t to l)
        saveFood()
    }

    private fun saveFood() {
        val o = JSONObject()
        food.forEach { (d, l) ->
            val a = JSONArray()
            l.forEach {
                a.put(JSONObject().put("id", it.foodId).put("name", it.name).put("n", it.n.toDouble())
                    .put("k", it.kcal).put("p", it.p.toDouble()).put("c", it.c.toDouble()).put("f", it.f.toDouble()))
            }
            o.put(d, a)
        }
        sp.edit().putString("food", o.toString()).apply()
    }

    /** متوسط البروتين آخر [days] أيام سجّل فيها أكل. null لو ما سجّل. */
    fun avgProtein(days: Int = 7): Float? {
        val logged = food.keys.sorted().takeLast(days).filter { it != Store.today() }
        if (logged.size < 3) return null
        return logged.map { totals(it).p }.average().toFloat()
    }

    fun addCustomFood(ar: String, portion: String, kcal: Int, p: Float, c: Float, f: Float): Food {
        val food = Food("c" + System.currentTimeMillis(), ar, portion, kcal, p, c, f, "أكلاتي", custom = true)
        customFoods = listOf(food) + customFoods
        saveCustom()
        return food
    }

    fun removeCustomFood(id: String) {
        customFoods = customFoods.filter { it.id != id }
        saveCustom()
    }

    private fun saveCustom() {
        val a = JSONArray()
        customFoods.forEach {
            a.put(JSONObject().put("id", it.id).put("ar", it.ar).put("portion", it.portion).put("k", it.kcal)
                .put("p", it.p.toDouble()).put("c", it.c.toDouble()).put("f", it.f.toDouble()))
        }
        sp.edit().putString("customFoods", a.toString()).apply()
    }

    // ===== الموية =====

    fun glasses(date: String = Store.today()): Int = water[date] ?: 0

    fun setGlasses(n: Int) {
        water = trim(water + (Store.today() to n.coerceIn(0, 30)))
        val o = JSONObject()
        water.forEach { (d, v) -> o.put(d, v) }
        sp.edit().putString("water", o.toString()).apply()
    }

    // ===== الوزن والخصر =====

    /** يسجّل الوزن اليوم، ويحدّث وزن الحسابات. */
    fun logWeight(kg: Float) {
        val t = Store.today()
        weights = (weights.filter { it.date != t } + Measure(t, kg)).sortedBy { it.date }.takeLast(400)
        writeMeasures("weights", weights)
        P.weightKg = kg
    }

    fun logWaist(cm: Float) {
        val t = Store.today()
        waists = (waists.filter { it.date != t } + Measure(t, cm)).sortedBy { it.date }.takeLast(400)
        writeMeasures("waists", waists)
    }

    fun removeWeight(date: String) {
        weights = weights.filter { it.date != date }
        writeMeasures("weights", weights)
    }

    fun removeWaist(date: String) {
        waists = waists.filter { it.date != date }
        writeMeasures("waists", waists)
    }

    /** كم يوم من آخر قياس خصر. null = ما قاس أبد. */
    fun daysSinceWaist(): Int? = waists.lastOrNull()?.let { Dates.daysBetween(it.date, Store.today()) }

    // ===== الصور =====

    fun addPhoto(file: String) {
        photos = photos + Photo(Store.today(), file)
        savePhotos()
    }

    fun removePhoto(p: Photo) {
        photoDir?.let { File(it, p.file).delete() }
        photos = photos - p
        savePhotos()
    }

    private fun savePhotos() {
        val a = JSONArray()
        photos.forEach { a.put(JSONObject().put("d", it.date).put("f", it.file)) }
        sp.edit().putString("photos", a.toString()).apply()
    }

    /** يمسح كل الصور من الجوال (مع "امسح كل بياناتي"). */
    fun deleteAllPhotos() {
        photoDir?.listFiles()?.forEach { it.delete() }
    }

    // ===== المكملات =====

    fun tookToday(id: String): Boolean = id in supps[Store.today()].orEmpty()

    fun toggleSupp(id: String) {
        val t = Store.today()
        val cur = supps[t].orEmpty()
        supps = trim(supps + (t to if (id in cur) cur - id else cur + id))
        val o = JSONObject()
        supps.forEach { (d, s) -> o.put(d, JSONArray(s.toList())) }
        sp.edit().putString("supps", o.toString()).apply()
    }
}

/** حسابات التواريخ (yyyy-MM-dd). */
object Dates {
    private fun parse(d: String): Long? = runCatching {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(d)?.time
    }.getOrNull()

    fun daysBetween(a: String, b: String): Int {
        val x = parse(a) ?: return 0
        val y = parse(b) ?: return 0
        return ((y - x) / 86_400_000L).toInt()
    }

    /** "2026-11-14" ← "14/11". */
    fun short(d: String): String {
        val p = d.split("-")
        return if (p.size == 3) "${p[2].trimStart('0')}/${p[1].trimStart('0')}" else d
    }
}
