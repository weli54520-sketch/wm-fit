package com.wm.fit.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** إصابة في مكان. [level] 1 خفيف، 2 متوسط، 3 قوي. */
data class Injury(val area: Area, val level: Int, val chronic: Boolean, val since: String)

/** مجموعة مسجلة: الوزن والعدّات. */
data class SetLog(val w: Float, val r: Int)

/** تمرين يوم واحد. */
data class Entry(val date: String, val sets: List<SetLog>)

/** أسباب زر "ما أقدر". */
enum class CantReason(val id: String, val ar: String) {
    PAIN("pain", "يوجعني"),
    HOW("how", "ما أعرف الطريقة"),
    BUSY("busy", "الجهاز مشغول"),
    HEAVY("heavy", "ثقيل علي"),
}

/** "كيف جسمك اليوم؟" */
object BodyCheck {
    const val GOOD = 1
    const val TIRED = 2
    const val PAIN = 3
}

/**
 * سجل التمارين والإصابات والبدائل.
 * كله داخل الجوال بس (نفس ملف الإعدادات)، وكل تغيير يحدّث الواجهة لحاله.
 */
object Store {
    private const val MAX_ENTRIES = 30

    var injuries by mutableStateOf<Map<Area, Injury>>(emptyMap())
        private set

    /** بدائل دايمة: التمرين الأصلي ← البديل اللي اختاره المستخدم. */
    var swaps by mutableStateOf<Map<String, String>>(emptyMap())
        private set

    /** بدائل لليوم بس (مثل: الجهاز مشغول). */
    var todaySwaps by mutableStateOf<Map<String, String>>(emptyMap())
        private set

    var logs by mutableStateOf<Map<String, List<Entry>>>(emptyMap())
        private set

    /** الأيام اللي خلّص فيها تمرينه (yyyy-MM-dd). */
    var done by mutableStateOf<Set<String>>(emptySet())
        private set

    /** كم مرة ضغط "ما أقدر" لكل تمرين ولكل سبب. المدرب يستخدمها بعدين. */
    var cant by mutableStateOf<Map<String, Int>>(emptyMap())
        private set

    /** جواب "كيف جسمك اليوم؟". 0 = ما جاوب اليوم. */
    var bodyToday by mutableStateOf(0)
        private set

    /** تمارين منقولة: تاريخ اليوم الجديد (yyyy-MM-dd) ← رقم يوم الجلسة الفايتة (0 = الأحد). */
    var moved by mutableStateOf<Map<String, Int>>(emptyMap())
        private set

    private var trackSince = ""

    /** تمارين فايتة قال عنها "خلاص، لا تذكرني" (تاريخ اليوم الفايت). */
    var skipped by mutableStateOf<Set<String>>(emptySet())
        private set

    private val sp get() = P.sp

    fun today(): String = day(Date())

    fun day(d: Date): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(d)

    fun load() {
        injuries = runCatching {
            val o = JSONObject(sp.getString("injuries", "{}") ?: "{}")
            o.keys().asSequence().mapNotNull { k ->
                val a = Area.byId(k) ?: return@mapNotNull null
                val j = o.getJSONObject(k)
                a to Injury(a, j.optInt("l", 2).coerceIn(1, 3), j.optBoolean("c"), j.optString("d"))
            }.toMap()
        }.getOrDefault(emptyMap())

        swaps = readMap("swaps")
        cant = runCatching {
            val o = JSONObject(sp.getString("cant", "{}") ?: "{}")
            o.keys().asSequence().associateWith { o.optInt(it) }
        }.getOrDefault(emptyMap())

        val t = today()
        todaySwaps = if (sp.getString("todaySwapsDate", "") == t) readMap("todaySwaps") else emptyMap()
        bodyToday = if (sp.getString("bodyDate", "") == t) sp.getInt("body", 0) else 0

        logs = runCatching {
            val o = JSONObject(sp.getString("logs", "{}") ?: "{}")
            o.keys().asSequence().associateWith { k ->
                val arr = o.getJSONArray(k)
                (0 until arr.length()).map { i ->
                    val e = arr.getJSONObject(i)
                    val s = e.getJSONArray("s")
                    Entry(
                        e.getString("d"),
                        (0 until s.length()).map { n ->
                            val p = s.getJSONArray(n)
                            SetLog(p.getDouble(0).toFloat(), p.getInt(1))
                        },
                    )
                }
            }
        }.getOrDefault(emptyMap())

        // نخلي النقل والتجاهل 8 أيام، عشان التمرين الفايت ما يرجع ينبّه بعد ما نقلته
        val keep = day(Date(System.currentTimeMillis() - 8 * 86_400_000L))
        moved = readMap("moved")
            .mapValues { it.value.toIntOrNull() ?: -1 }
            .filter { it.key >= keep && it.value in 0..6 }
        // نبدأ نتابع التمارين الفايتة من أول يوم بعد هالتحديث، عشان ما ينبّه على أيام قديمة
        trackSince = sp.getString("missedSince", null) ?: today().also {
            sp.edit().putString("missedSince", it).apply()
        }
        skipped = (sp.getString("skipped", "") ?: "").split(",").filter { it >= keep && it.isNotBlank() }.toSet()

        done = (sp.getString("done", "") ?: "").split(",").filter { it.isNotBlank() }.toSet()
    }

    private fun readMap(key: String): Map<String, String> = runCatching {
        val o = JSONObject(sp.getString(key, "{}") ?: "{}")
        o.keys().asSequence().associateWith { o.getString(it) }
    }.getOrDefault(emptyMap())

    private fun writeMap(key: String, m: Map<String, String>) {
        val o = JSONObject()
        m.forEach { (k, v) -> o.put(k, v) }
        sp.edit().putString(key, o.toString()).apply()
    }

    // ===== الإصابات =====

    fun setInjury(i: Injury) {
        injuries = injuries + (i.area to i)
        saveInjuries()
    }

    fun removeInjury(a: Area) {
        injuries = injuries - a
        saveInjuries()
    }

    private fun saveInjuries() {
        val o = JSONObject()
        injuries.values.forEach {
            o.put(it.area.id, JSONObject().put("l", it.level).put("c", it.chronic).put("d", it.since))
        }
        sp.edit().putString("injuries", o.toString()).apply()
    }

    /**
     * هل التمرين ممنوع بسبب إصابة؟
     * الإصابة الجديدة المتوسطة أو القوية توقف التمارين اللي تضغط على مكانها.
     * المزمنة ما توقف شي، التطبيق يخففها بس.
     */
    fun blocked(ex: Ex): Boolean = ex.stress.any { a ->
        val i = injuries[a]
        i != null && !i.chronic && i.level >= 2
    }

    /** الإصابات اللي التمرين يضغط عليها بس ما توقفه (مزمنة أو خفيفة). */
    fun cautions(ex: Ex): List<Injury> = ex.stress.mapNotNull { injuries[it] }.filter { it.chronic || it.level < 2 }

    // ===== البدائل =====

    fun setSwap(from: String, to: String, onlyToday: Boolean) {
        if (onlyToday) {
            todaySwaps = todaySwaps + (from to to)
            writeMap("todaySwaps", todaySwaps)
            sp.edit().putString("todaySwapsDate", today()).apply()
        } else {
            swaps = swaps + (from to to)
            writeMap("swaps", swaps)
        }
    }

    fun clearSwap(from: String) {
        swaps = swaps - from
        todaySwaps = todaySwaps - from
        writeMap("swaps", swaps)
        writeMap("todaySwaps", todaySwaps)
    }

    fun countCant(exId: String, reason: CantReason) {
        val k = "$exId:${reason.id}"
        cant = cant + (k to (cant[k] ?: 0) + 1)
        val o = JSONObject()
        cant.forEach { (key, n) -> o.put(key, n) }
        sp.edit().putString("cant", o.toString()).apply()
    }

    // ===== جسمك اليوم =====

    fun setBody(v: Int) {
        bodyToday = v
        sp.edit().putInt("body", v).putString("bodyDate", today()).apply()
    }

    // ===== الأوزان =====

    /** مجموعات اليوم لتمرين. */
    fun todaySets(exId: String): List<SetLog> =
        logs[exId]?.lastOrNull { it.date == today() }?.sets ?: emptyList()

    /** آخر مرة قبل اليوم. */
    fun lastEntry(exId: String): Entry? = logs[exId]?.lastOrNull { it.date != today() }

    /** يسجّل مجموعة رقم [index] لليوم (أو يعدّلها لو موجودة). */
    fun logSet(exId: String, index: Int, s: SetLog) {
        val t = today()
        val list = logs[exId].orEmpty().toMutableList()
        val cur = list.lastOrNull()?.takeIf { it.date == t }
        val sets = cur?.sets.orEmpty().toMutableList()
        if (index < sets.size) sets[index] = s else sets += s
        if (cur != null) list[list.lastIndex] = Entry(t, sets) else list += Entry(t, sets)
        logs = logs + (exId to list.takeLast(MAX_ENTRIES))
        saveLogs()
    }

    /** يمسح آخر مجموعة اليوم (لو سجّلها بالغلط). */
    fun undoSet(exId: String) {
        val t = today()
        val list = logs[exId].orEmpty().toMutableList()
        val cur = list.lastOrNull()?.takeIf { it.date == t } ?: return
        val sets = cur.sets.dropLast(1)
        if (sets.isEmpty()) list.removeAt(list.lastIndex) else list[list.lastIndex] = Entry(t, sets)
        logs = logs + (exId to list)
        saveLogs()
    }

    private fun saveLogs() {
        val o = JSONObject()
        logs.forEach { (k, entries) ->
            val arr = JSONArray()
            entries.forEach { e ->
                val s = JSONArray()
                e.sets.forEach { s.put(JSONArray().put(it.w.toDouble()).put(it.r)) }
                arr.put(JSONObject().put("d", e.date).put("s", s))
            }
            o.put(k, arr)
        }
        sp.edit().putString("logs", o.toString()).apply()
    }

    /** ينقل جلسة يوم فايت ([dow]) ليوم ثاني ([date]). */
    fun moveWorkout(date: String, dow: Int) {
        moved = moved + (date to dow)
        writeMap("moved", moved.mapValues { it.value.toString() })
    }

    /** تاريخ يوم من هالأسبوع (0 = الأحد). */
    fun dateOfDow(dow: Int): String {
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_YEAR, dow - (c.get(Calendar.DAY_OF_WEEK) - 1))
        return day(c.time)
    }

    /**
     * آخر تمرين فاتك هالأسبوع (يوم تمرين قبل اليوم، ما خلّصته، وما نقلته ولا تجاهلته).
     * يرجّع رقم اليوم، أو null.
     */
    fun missedDow(trainDays: List<Int>, todayDow: Int): Int? = trainDays
        .filter { it < todayDow }
        .filter { d ->
            val date = dateOfDow(d)
            date >= trackSince && date !in done && date !in skipped && moved.none { (k, v) -> v == d && k >= date }
        }
        .maxOrNull()

    fun skipMissed(dow: Int) {
        skipped = skipped + dateOfDow(dow)
        sp.edit().putString("skipped", skipped.joinToString(",")).apply()
    }

    fun clearMove(date: String) {
        moved = moved - date
        writeMap("moved", moved.mapValues { it.value.toString() })
    }

    fun markDone() {
        done = done + today()
        sp.edit().putString("done", done.sorted().takeLast(400).joinToString(",")).apply()
    }

    /** كم تمرين خلّص هالأسبوع (الأسبوع يبدأ الأحد). */
    fun doneThisWeek(): Int {
        val c = Calendar.getInstance()
        val back = c.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
        val days = (0..back).map {
            val d = Calendar.getInstance()
            d.add(Calendar.DAY_OF_YEAR, -it)
            day(d.time)
        }
        return days.count { it in done }
    }
}
