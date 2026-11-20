package com.waleed.onyx.home

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.IntOffset
import com.waleed.onyx.OnyxApp
import com.waleed.onyx.apps.AppInfo
import android.appwidget.AppWidgetProviderInfo
import android.view.View
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.view.drawToBitmap
import com.waleed.onyx.data.Container
import com.waleed.onyx.data.Item
import com.waleed.onyx.data.Kind
import com.waleed.onyx.grid.Cell
import com.waleed.onyx.grid.GridEngine
import com.waleed.onyx.grid.Span
import com.waleed.onyx.grid.SpanLimits
import com.waleed.onyx.grid.WidgetSpan
import com.waleed.onyx.widget.WidgetData
import com.waleed.onyx.widget.Widgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** مقاسات الشبكة بالبكسل للشاشة الحالية */
data class Geo(val cols: Int, val rows: Int, val cellW: Float, val cellH: Float, val screenW: Float, val edgePx: Float)

/** خانة بالعمود الجانبي أو الشريط */
data class Slot(val container: Int, val index: Int)

/** عنصر يتسحب الحين */
class Drag(val item: Item, val grab: Offset, startPage: Int, val snap: ImageBitmap? = null) {
    var pointer by mutableStateOf(Offset.Zero)
    var page by mutableIntStateOf(startPage)
    var cand by mutableStateOf<Cell?>(null)
    var solution by mutableStateOf<Map<Long, Cell>?>(null)
    var slot by mutableStateOf<Slot?>(null)
    /** أيقونات تطلع من الصفحة المليانة وتنتقل للي بعدها */
    var overflow by mutableStateOf<List<Long>>(emptyList())
    /** الإصبع عند حافة الشاشة: -1 يسار، 1 يمين (للمعة اللي تبين إن الصفحة بتتقلب) */
    var edge by mutableIntStateOf(0)
}

data class Menu(val item: Item, val bounds: Rect, val slot: Slot?, val snap: ImageBitmap? = null)

/** ويدجت نكبره أو نصغره الحين بسحب الزاوية */
class Resize(val item: Item, val limits: SpanLimits) {
    var w by mutableIntStateOf(item.w)
    var h by mutableIntStateOf(item.h)
    var solution by mutableStateOf<Map<Long, Cell>?>(null)
}

/**
 * حالة الشاشة الرئيسية: وضع الترتيب، السحب، القوائم، واللوحات.
 * أي تغيير بالأماكن ينطبق على الشاشة فوراً، وبعدين ينحفظ بقاعدة البيانات.
 */
class HomeState(val app: OnyxApp, private val scope: CoroutineScope) {
    val settings get() = app.settings
    val catalog get() = app.catalog
    private val repo get() = app.repo

    var haptic: HapticFeedback? = null
    var screen by mutableIntStateOf(0)
    var items by mutableStateOf<List<Item>>(emptyList())
    var editing by mutableStateOf(false)
        private set
    var drag by mutableStateOf<Drag?>(null)
        private set
    var menu by mutableStateOf<Menu?>(null)
    var drawerOpen by mutableStateOf(false)
    var picker by mutableStateOf<Slot?>(null)
    var settingsOpen by mutableStateOf(false)
    var galleryOpen by mutableStateOf(false)
    var resize by mutableStateOf<Resize?>(null)
        private set
    var toast by mutableStateOf<String?>(null)
    var dropTick by mutableIntStateOf(0)
        private set

    /** تطبيق مضغوط الحين، عشان الضغطة الطويلة على الفاضي ما تشتغل معه */
    var tilePressed = false

    /** الأيقونة المضغوطة الحين (عشان تصغر تحت الإصبع) */
    var pressedKey by mutableStateOf<Any?>(null)
    var geo by mutableStateOf(Geo(4, 6, 1f, 1f, 1f, 1f))
    lateinit var pager: PagerState

    val pageBounds = HashMap<Int, Rect>()
    val tileBounds = HashMap<Long, Rect>()
    val slotBounds = HashMap<Slot, Rect>()
    /** شكل كل ويدجت على الشاشة، عشان نصوره لما يرتفع أو يتسحب */
    val widgetViews = HashMap<Long, View>()
    private val widgets: Widgets get() = app.widgets
    private val dropFrom = HashMap<Long, IntOffset>()
    private var edgeJob: Job? = null

    fun gridItems(page: Int) = items.filter { it.container == Container.GRID && it.page == page }
    fun slotItem(slot: Slot) = items.firstOrNull { it.container == slot.container && it.page == slot.index }
    fun lastPage() = items.filter { it.container == Container.GRID }.maxOfOrNull { it.page } ?: 0

    /** المكان المؤقت لعنصر وأنت تسحب أو تكبر ويدجت (قبل ما تفلت) */
    /** أيقونة بتطلع من الصفحة وأنت تسحب (الصفحة مليانة) */
    fun isOverflow(id: Long): Boolean = drag?.overflow?.contains(id) == true

    fun previewCell(page: Int, id: Long): Cell? {
        val d = drag
        if (d != null && d.page == page && d.item.id != id) return d.solution?.get(id)
        val r = resize
        if (r != null && r.item.page == page && r.item.id != id) return r.solution?.get(id)
        return null
    }

    fun buzz() {
        if (settings.haptics) haptic?.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun say(msg: String) { toast = msg }

    // ───────── وضع الترتيب ─────────

    fun enterEdit() {
        if (editing) return
        if (settings.lock) { say("الترتيب مقفل من الإعدادات"); return }
        menu = null
        editing = true
        buzz()
    }

    fun exitEdit() {
        if (!editing) return
        editing = false
        drag = null
        resize = null
        scope.launch { repo.compactPages(screen) }
    }

    fun closeAll() {
        menu = null
        drawerOpen = false
        picker = null
        settingsOpen = false
        galleryOpen = false
        exitEdit()
    }

    // ───────── السحب ─────────

    fun startDrag(item: Item, pointer: Offset) {
        if (settings.lock) { say("الترتيب مقفل من الإعدادات"); return }
        if (item.container != Container.GRID) return
        if (!editing) enterEdit()
        val b = tileBounds[item.id] ?: return
        val scale = (b.width / (geo.cellW * item.w)).coerceAtLeast(0.01f)
        val snap = if (item.kind == Kind.WIDGET) snapshot(item.id) else null
        drag = Drag(item, Offset((pointer.x - b.left) / scale, (pointer.y - b.top) / scale), item.page, snap).also { it.pointer = pointer }
        menu = null
        buzz()
        settlePager()
    }

    /** لو الصفحة واقفة بالنص لأي سبب، ترجع لمكانها بنعومة */
    fun settlePager() {
        if (!::pager.isInitialized) return
        if (pager.currentPageOffsetFraction != 0f && !pager.isScrollInProgress) {
            scope.launch { pager.animateScrollToPage(pager.currentPage) }
        }
    }

    private fun pageScale(page: Int): Float {
        val r = pageBounds[page] ?: return 1f
        return (r.width / (geo.cols * geo.cellW)).coerceAtLeast(0.01f)
    }

    /** مكان زاوية العنصر داخل الصفحة (بكسل بدون تصغير) */
    private fun localTopLeft(d: Drag, page: Int): Offset? {
        val r = pageBounds[page] ?: return null
        val s = pageScale(page)
        return Offset((d.pointer.x - d.grab.x * s - r.left) / s, (d.pointer.y - d.grab.y * s - r.top) / s)
    }

    fun dragScale(): Float = drag?.let { pageScale(it.page) } ?: 1f

    fun dragMove(pointer: Offset) {
        val d = drag ?: return
        d.pointer = pointer
        // حافة الشاشة لها الأولوية (حتى لو فوق العمود الجانبي): تقلب الصفحة
        if (edgeCheck(d)) return
        // الويدجت ما ينحط بالعمود أو الشريط
        val slot = if (d.item.kind == Kind.WIDGET) null
        else slotBounds.entries.firstOrNull { it.value.contains(pointer) }?.key
        if (slot != null) {
            if (d.slot != slot) buzz()
            d.slot = slot; d.cand = null; d.solution = null; d.overflow = emptyList()
            return
        }
        d.slot = null
        val tl = localTopLeft(d, d.page) ?: return
        val c = GridEngine.cellAt(tl.x, tl.y, geo.cellW, geo.cellH, d.item.w, d.item.h, geo.cols, geo.rows)
        if (c == d.cand && d.solution != null) return
        d.cand = c
        val others = gridItems(d.page).filter { it.id != d.item.id }.map { Span(it.id, it.x, it.y, it.w, it.h) }
        val moving = Span(d.item.id, c.x, c.y, d.item.w, d.item.h)
        if (d.item.w == 1 && d.item.h == 1) {
            // الأيقونات تزحف بالترتيب مثل الآيفون
            val from = if (d.page == d.item.page && d.item.container == Container.GRID) Cell(d.item.x, d.item.y) else null
            val r = GridEngine.reorder(others, moving, geo.cols, geo.rows, from)
            if (r == null) return // فوق ويدجت: نخلي آخر مكان صح
            d.solution = r.cells
            d.overflow = r.overflow
        } else {
            d.solution = GridEngine.solve(others, moving, geo.cols, geo.rows)
            d.overflow = emptyList()
        }
    }

    /** يرجع true لو الإصبع بحافة الشاشة (والصفحة بتتقلب بعد شوي) */
    private fun edgeCheck(d: Drag): Boolean {
        val np0 = d.page
        val dir = when {
            d.pointer.x < geo.edgePx && np0 > 0 -> -1
            d.pointer.x > geo.screenW - geo.edgePx && np0 + 1 < pager.pageCount -> 1
            else -> 0
        }
        if (dir == 0) { edgeJob?.cancel(); edgeJob = null; d.edge = 0; return false }
        if (d.edge != dir) {
            d.edge = dir
            // الأيقونة ما تنحط بالصفحة اللي بنطلع منها
            d.slot = null; d.cand = null; d.solution = null; d.overflow = emptyList()
        }
        if (edgeJob?.isActive == true) return true
        edgeJob = scope.launch {
            delay(420)
            val np = d.page + dir
            if (drag === d && np >= 0 && np < pager.pageCount) {
                d.cand = null; d.solution = null; d.overflow = emptyList(); d.page = np
                buzz()
                pager.animateScrollToPage(np)
                edgeJob = null
                d.edge = 0
                dragMove(d.pointer)
            }
        }
        return true
    }

    fun dragEnd() {
        val d = drag ?: return
        edgeJob?.cancel(); edgeJob = null
        val it0 = d.item
        val slot = d.slot
        val upd = ArrayList<Item>()
        if (slot != null) {
            val target = slotItem(slot)
            upd += it0.copy(container = slot.container, page = slot.index, x = 0, y = 0)
            if (target != null && target.id != it0.id) upd += target.copy(container = it0.container, page = it0.page, x = it0.x, y = it0.y)
        } else {
            val c = d.cand
            val sol = d.solution
            if (c != null && sol != null) {
                localTopLeft(d, d.page)?.let { dropFrom[it0.id] = IntOffset(it.x.roundToInt(), it.y.roundToInt()) }
                val placed = HashMap<Long, Item>()
                for (o in gridItems(d.page)) {
                    if (o.id == it0.id || o.id in d.overflow) continue
                    val q = sol[o.id] ?: continue
                    if (q.x != o.x || q.y != o.y) placed[o.id] = o.copy(x = q.x, y = q.y)
                }
                placed[it0.id] = it0.copy(container = Container.GRID, page = d.page, x = c.x, y = c.y)
                // اللي طلع من الصفحة المليانة: أول مكان فاضي بالصفحات اللي بعدها
                for (id in d.overflow) {
                    val o = items.firstOrNull { it.id == id } ?: continue
                    var p = d.page + 1
                    while (true) {
                        val taken = items.filter { it.container == Container.GRID && it.id != id }
                            .map { placed[it.id] ?: it }
                            .filter { it.page == p }
                            .map { Span(it.id, it.x, it.y, it.w, it.h) }
                        val spot = GridEngine.freeSpot(taken, 1, 1, geo.cols, geo.rows)
                        if (spot != null) { placed[id] = o.copy(page = p, x = spot.x, y = spot.y); break }
                        p++
                    }
                }
                upd += placed.values
                buzz()
            } else {
                // ما فيه مكان: يرجع لمكانه بنعومة
                localTopLeft(d, it0.page)?.let { dropFrom[it0.id] = IntOffset(it.x.roundToInt(), it.y.roundToInt()) }
            }
        }
        applyLocal(upd)
        dropTick++
        drag = null
        if (upd.isNotEmpty()) scope.launch { repo.save(upd) }
        settlePager()
    }

    /** السحب لتحت: يفتح لوحة الإشعارات مثل سامسونج */
    fun openNotifications() {
        try {
            val sb = app.getSystemService("statusbar")
            Class.forName("android.app.StatusBarManager").getMethod("expandNotificationsPanel").invoke(sb)
        } catch (e: Exception) {
            say("ما قدرت أفتح الإشعارات")
        }
    }

    fun takeDropFrom(id: Long): IntOffset? = dropFrom.remove(id)

    private fun applyLocal(upd: List<Item>) {
        if (upd.isEmpty()) return
        val map = upd.associateBy { it.id }
        items = items.map { map[it.id] ?: it }
    }

    // ───────── القوائم ─────────

    fun openMenu(item: Item, slot: Slot?) {
        val b = if (slot != null) slotBounds[slot] else tileBounds[item.id]
        val snap = if (item.kind == Kind.WIDGET) snapshot(item.id) else null
        menu = Menu(item, b ?: return, slot, snap)
        buzz()
    }

    /** صورة للويدجت مثل ما هو الحين. بعض الويدجت ما تنصور، فنرسم مكانها زجاج بس */
    private fun snapshot(id: Long): ImageBitmap? {
        val v = widgetViews[id] ?: return null
        if (v.width <= 0 || v.height <= 0) return null
        return try { v.drawToBitmap().asImageBitmap() } catch (e: Exception) { null }
    }

    // ───────── الويدجت ─────────

    fun widgetData(item: Item): WidgetData? = WidgetData.parse(item.data)

    /** من المعرض: نطلب الإذن والإعداد، وبعدين نحطه بأول مكان يكفيه */
    fun addWidget(info: AppWidgetProviderInfo) {
        val g = geo
        val (w, h) = WidgetSpan.defaultSpan(info.targetCellWidth, info.targetCellHeight, info.minWidth, info.minHeight, g.cellW, g.cellH, g.cols, g.rows)
        val scr = screen
        val frame = settings.widgetFrame
        galleryOpen = false
        widgets.begin(info) { id ->
            if (scr != screen) { widgets.delete(id); say("الشاشة تغيرت، جرب مرة ثانية"); return@begin }
            var page = pager.currentPage.coerceAtMost(lastPage())
            var spot: Cell?
            while (true) {
                spot = GridEngine.freeSpot(gridItems(page).map { Span(it.id, it.x, it.y, it.w, it.h) }, w, h, geo.cols, geo.rows)
                if (spot != null) break
                if (page > lastPage()) { spot = Cell(0, 0); break }
                page++
            }
            val s = spot!!
            val data = WidgetData(id, Widgets.providerKey(info.provider), frame).encode()
            val row = Item(screen = scr, container = Container.GRID, page = page, x = s.x, y = s.y, w = w, h = h, kind = Kind.WIDGET, data = data)
            scope.launch {
                repo.save(emptyList(), insert = listOf(row))
                // ننتظر الصفحة الجديدة تطلع قبل ما نروح لها
                delay(150)
                pager.animateScrollToPage(page)
            }
            say("انضاف الويدجت")
        }
    }

    fun toggleFrame(item: Item) {
        val d = widgetData(item) ?: return
        val u = item.copy(data = d.copy(frame = !d.frame).encode())
        applyLocal(listOf(u))
        scope.launch { repo.save(listOf(u)) }
    }

    fun reconfigure(item: Item) {
        widgetData(item)?.let { widgets.reconfigure(it.id) }
    }

    fun canReconfigure(item: Item): Boolean = widgetData(item)?.let { widgets.canReconfigure(widgets.info(it.id)) } == true

    fun canResize(item: Item): Boolean {
        val info = widgetData(item)?.let { widgets.info(it.id) } ?: return false
        return info.resizeMode != AppWidgetProviderInfo.RESIZE_NONE
    }

    fun resizeStart(item: Item) {
        if (settings.lock) { say("الترتيب مقفل من الإعدادات"); return }
        val info = widgetData(item)?.let { widgets.info(it.id) } ?: return
        val g = geo
        val lim = WidgetSpan.limits(
            item.w, item.h,
            info.minResizeWidth, info.minResizeHeight, info.maxResizeWidth, info.maxResizeHeight,
            info.resizeMode and AppWidgetProviderInfo.RESIZE_HORIZONTAL != 0,
            info.resizeMode and AppWidgetProviderInfo.RESIZE_VERTICAL != 0,
            g.cellW, g.cellH, g.cols, g.rows,
        )
        resize = Resize(item, lim).also { it.solution = emptyMap() }
        buzz()
    }

    /** [dx] [dy] بالبكسل من بداية السحب */
    fun resizeMove(dx: Float, dy: Float) {
        val r = resize ?: return
        val g = geo
        val (w, h) = WidgetSpan.resized(r.item.w, r.item.h, dx, dy, g.cellW, g.cellH, r.item.x, r.item.y, g.cols, g.rows, r.limits)
        if (w == r.w && h == r.h) return
        val others = gridItems(r.item.page).filter { it.id != r.item.id }.map { Span(it.id, it.x, it.y, it.w, it.h) }
        val sol = GridEngine.solve(others, Span(r.item.id, r.item.x, r.item.y, w, h), g.cols, g.rows) ?: return
        r.w = w; r.h = h; r.solution = sol
        buzz()
    }

    fun resizeEnd() {
        val r = resize ?: return
        resize = null
        val sol = r.solution ?: return
        if (r.w == r.item.w && r.h == r.item.h) return
        val upd = ArrayList<Item>()
        for (o in gridItems(r.item.page)) {
            if (o.id == r.item.id) continue
            val q = sol[o.id] ?: continue
            if (q.x != o.x || q.y != o.y) upd += o.copy(x = q.x, y = q.y)
        }
        upd += r.item.copy(w = r.w, h = r.h)
        applyLocal(upd)
        scope.launch { repo.save(upd) }
    }

    fun removeFromHome(item: Item) {
        if (item.kind == Kind.WIDGET) {
            items = items.filter { it.id != item.id }
            scope.launch { repo.save(emptyList(), delete = listOf(item)) }
            say("انشال الويدجت")
            return
        }
        val u = item.copy(container = Container.HIDDEN)
        applyLocal(listOf(u))
        scope.launch { repo.save(listOf(u)) }
        say("انشال من الشاشة، تلقاه بكل التطبيقات")
    }

    /** من كل التطبيقات: يرجع التطبيق للشاشة بأول مكان فاضي */
    fun addToHome(appInfo: AppInfo) {
        val row = items.firstOrNull { it.appKey == appInfo.key }
        if (row != null && row.container != Container.HIDDEN) { say("التطبيق موجود بالشاشة"); return }
        var page = pager.currentPage.coerceAtMost(lastPage())
        var spot: Cell?
        while (true) {
            spot = GridEngine.freeSpot(gridItems(page).map { Span(it.id, it.x, it.y, it.w, it.h) }, 1, 1, geo.cols, geo.rows)
            if (spot != null) break
            page++
        }
        val s = spot!!
        if (row != null) {
            val u = row.copy(container = Container.GRID, page = page, x = s.x, y = s.y)
            applyLocal(listOf(u))
            scope.launch { repo.save(listOf(u)) }
        } else {
            scope.launch { repo.save(emptyList(), insert = listOf(Item(screen = screen, container = Container.GRID, page = page, x = s.x, y = s.y, appKey = appInfo.key))) }
        }
        drawerOpen = false
        say("انضاف ${appInfo.label}")
        scope.launch { pager.animateScrollToPage(page) }
    }

    /** يحط تطبيق بخانة بالعمود أو الشريط. لو التطبيق موجود بمكان ثاني، يتبادلون */
    fun assignSlot(slot: Slot, appInfo: AppInfo) {
        val target = slotItem(slot)
        val chosen = items.firstOrNull { it.appKey == appInfo.key }
        val upd = ArrayList<Item>()
        val ins = ArrayList<Item>()
        when {
            chosen != null -> {
                upd += chosen.copy(container = slot.container, page = slot.index, x = 0, y = 0)
                if (target != null && target.id != chosen.id) upd += target.copy(container = chosen.container, page = chosen.page, x = chosen.x, y = chosen.y)
            }
            target != null -> upd += target.copy(appKey = appInfo.key)
            else -> ins += Item(screen = screen, container = slot.container, page = slot.index, x = 0, y = 0, appKey = appInfo.key)
        }
        applyLocal(upd)
        scope.launch { repo.save(upd, insert = ins) }
        picker = null
        drawerOpen = false
    }

    fun launch(item: Item, bounds: Rect?) {
        val a = catalog.byKey(item.appKey) ?: return
        val r = bounds?.let { android.graphics.Rect(it.left.toInt(), it.top.toInt(), it.right.toInt(), it.bottom.toInt()) }
        catalog.launch(a, r)
    }
}
