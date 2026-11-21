package com.waleed.onyx.data

import com.waleed.onyx.apps.AppCatalog
import com.waleed.onyx.apps.AppInfo
import com.waleed.onyx.grid.GridEngine
import com.waleed.onyx.grid.Span
import com.waleed.onyx.widget.WidgetData
import com.waleed.onyx.widget.Widgets
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * ترتيب الشاشة: أول تشغيل، التطبيقات اللي تنزل أو تنحذف، وحفظ أي تغيير.
 * كل الكتابة تمر من هنا عشان ما يصير تعارض.
 */
class HomeRepository(private val dao: ItemDao, private val catalog: AppCatalog, private val settings: Settings, private val widgets: Widgets) {
    private val lock = Mutex()

    fun observe(screen: Int): Flow<List<Item>> = dao.observe(screen)

    /** يتأكد إن الشاشة مرتبة، ويضيف الجديد ويشيل المحذوف */
    suspend fun sync(screen: Int, apps: List<AppInfo>) = lock.withLock {
        if (apps.isEmpty()) return@withLock
        val existing = dao.all(screen)
        if (existing.isEmpty()) {
            dao.insert(seed(screen, apps))
            return@withLock
        }
        val keys = apps.map { it.key }.toSet()
        val gone = existing.filter { it.kind == Kind.APP && it.appKey !in keys }
        val present = existing.filter { it.kind == Kind.APP }.map { it.appKey }.toSet()
        val fresh = apps.filter { it.key !in present }
        val cols = settings.cols(screen)
        val rows = settings.rows(screen)
        val grid = (existing - gone.toSet()).filter { it.container == Container.GRID }
        val add = ArrayList<Item>()
        val byPage = grid.groupBy { it.page }.mapValues { e -> e.value.map { Span(it.id, it.x, it.y, it.w, it.h) }.toMutableList() }.toMutableMap()
        var page = (byPage.keys.maxOrNull() ?: 0)
        for (app in fresh) {
            while (true) {
                val list = byPage.getOrPut(page) { mutableListOf() }
                val spot = GridEngine.freeSpot(list, 1, 1, cols, rows)
                if (spot != null) {
                    list += Span(-1, spot.x, spot.y)
                    add += Item(screen = screen, container = Container.GRID, page = page, x = spot.x, y = spot.y, appKey = app.key)
                    break
                }
                page++
            }
        }
        dao.apply(insert = add, update = emptyList(), delete = gone)
    }

    /** أول تشغيل: العمود والشريط بتطبيقات وليد اليومية، والباقي بالصفحات بالترتيب */
    private fun seed(screen: Int, apps: List<AppInfo>): List<Item> {
        val used = HashSet<String>()
        val out = ArrayList<Item>()
        fun pick(container: Int, groups: List<List<String>>) {
            groups.forEachIndexed { i, pkgs ->
                val a = catalog.firstByPackages(pkgs, used) ?: return@forEachIndexed
                used += a.key
                out += Item(screen = screen, container = container, page = i, x = 0, y = 0, appKey = a.key)
            }
        }
        pick(Container.SIDE, SIDE_DEFAULT)
        pick(Container.DOCK, DOCK_DEFAULT)
        val cols = settings.cols(screen)
        val rows = settings.rows(screen)
        var i = 0
        for (a in apps) {
            if (a.key in used) continue
            val page = i / (cols * rows)
            val cell = i % (cols * rows)
            out += Item(screen = screen, container = Container.GRID, page = page, x = cell % cols, y = cell / cols, appKey = a.key)
            i++
        }
        return out
    }

    suspend fun save(update: List<Item>, delete: List<Item> = emptyList(), insert: List<Item> = emptyList()) = lock.withLock {
        dao.apply(insert = insert, update = update, delete = delete)
        releaseWidgets(delete)
    }

    /** الويدجت اللي تنشال: نرجع رقمها للنظام عشان التطبيق يوقف يحدثها */
    private fun releaseWidgets(deleted: List<Item>) {
        for (it in deleted) if (it.kind == Kind.WIDGET) WidgetData.parse(it.data)?.let { d -> widgets.delete(d.id) }
    }

    /** بعد ما تتغير الشبكة: كل شي يبقى بمكانه لو يركب، والباقي ينتقل */
    suspend fun reflow(screen: Int) = lock.withLock {
        val all = dao.all(screen)
        val grid = all.filter { it.container == Container.GRID }
        val maxPage = grid.maxOfOrNull { it.page } ?: 0
        val pages = (0..maxPage).map { p -> grid.filter { it.page == p }.map { Span(it.id, it.x, it.y, it.w, it.h) } }
        val r = GridEngine.reflow(pages, settings.cols(screen), settings.rows(screen))
        val upd = grid.mapNotNull { it0 ->
            val (p, c) = r[it0.id] ?: return@mapNotNull null
            if (p == it0.page && c.x == it0.x && c.y == it0.y) null else it0.copy(page = p, x = c.x, y = c.y)
        }
        dao.apply(emptyList(), upd, emptyList())
    }

    /** يشيل الصفحات الفاضية بالنص ويرص الأرقام */
    suspend fun compactPages(screen: Int) = lock.withLock {
        val grid = dao.all(screen).filter { it.container == Container.GRID }
        val used = grid.map { it.page }.distinct().sorted()
        val map = used.withIndex().associate { (i, p) -> p to i }
        val upd = grid.filter { map[it.page] != it.page }.map { it.copy(page = map.getValue(it.page)) }
        dao.apply(emptyList(), upd, emptyList())
    }

    /**
     * وضع اختبار للجوال الوهمي بس: ينسخ أيقونات الشبكة كذا مرة عشان تصير صفحات كثيرة ومليانة.
     * ينطلب بأمر adb مع كلمة سر، وما يوصل له وليد من الواجهة.
     */
    suspend fun testFill(copies: Int) = lock.withLock {
        for (screen in listOf(Screen.OUTER, Screen.INNER)) {
            val existing = dao.all(screen)
            val apps = existing.filter { it.kind == Kind.APP && it.container == Container.GRID }
            if (apps.isEmpty()) continue
            val cols = settings.cols(screen)
            val rows = settings.rows(screen)
            val byPage = existing.filter { it.container == Container.GRID }.groupBy { it.page }
                .mapValues { e -> e.value.map { Span(it.id, it.x, it.y, it.w, it.h) }.toMutableList() }.toMutableMap()
            val add = ArrayList<Item>()
            var page = 0
            repeat(copies) {
                for (a in apps) {
                    while (true) {
                        val list = byPage.getOrPut(page) { mutableListOf() }
                        val spot = GridEngine.freeSpot(list, 1, 1, cols, rows)
                        if (spot != null) { list += Span(-1, spot.x, spot.y); add += a.copy(id = 0, page = page, x = spot.x, y = spot.y); break }
                        page++
                    }
                }
            }
            dao.apply(insert = add, update = emptyList(), delete = emptyList())
        }
    }

    /**
     * ينسخ ترتيب التطبيقات لشاشة الثانية. الويدجت ما تنتسخ (كل ويدجت له رقم واحد عند النظام)،
     * وويدجت الشاشة الثانية تبقى، واللي يتصادم معها ينتقل لأقرب فاضي.
     */
    suspend fun copyLayout(from: Int, to: Int) = lock.withLock {
        val src = dao.all(from).filter { it.kind == Kind.APP }
        val old = dao.all(to).filter { it.kind == Kind.APP }
        val copy = src.map { it.copy(id = 0, screen = to) }
        dao.apply(insert = copy, update = emptyList(), delete = old)
    }

    companion object {
        // الهاتف، واتساب، يوتيوب، تيليجرام، + واحد
        val SIDE_DEFAULT = listOf(
            listOf("com.samsung.android.dialer", "com.google.android.dialer", "com.android.dialer"),
            listOf("com.whatsapp", "com.whatsapp.w4b"),
            listOf("com.google.android.youtube"),
            listOf("org.telegram.messenger", "org.telegram.messenger.web"),
            listOf("com.google.android.gm", "com.samsung.android.messaging"),
        )

        // X، تيك توك، Claude، نوت، بلاي، بنترست
        val DOCK_DEFAULT = listOf(
            listOf("com.twitter.android"),
            listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill"),
            listOf("com.anthropic.claude"),
            listOf("com.samsung.android.app.notes"),
            listOf("com.android.vending"),
            listOf("com.pinterest"),
        )
    }
}
