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
import com.waleed.onyx.data.Container
import com.waleed.onyx.data.Item
import com.waleed.onyx.grid.Cell
import com.waleed.onyx.grid.GridEngine
import com.waleed.onyx.grid.Span
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
class Drag(val item: Item, val grab: Offset, startPage: Int) {
    var pointer by mutableStateOf(Offset.Zero)
    var page by mutableIntStateOf(startPage)
    var cand by mutableStateOf<Cell?>(null)
    var solution by mutableStateOf<Map<Long, Cell>?>(null)
    var slot by mutableStateOf<Slot?>(null)
}

data class Menu(val item: Item, val bounds: Rect, val slot: Slot?)

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
    private val dropFrom = HashMap<Long, IntOffset>()
    private var edgeJob: Job? = null

    fun gridItems(page: Int) = items.filter { it.container == Container.GRID && it.page == page }
    fun slotItem(slot: Slot) = items.firstOrNull { it.container == slot.container && it.page == slot.index }
    fun lastPage() = items.filter { it.container == Container.GRID }.maxOfOrNull { it.page } ?: 0

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
        scope.launch { repo.compactPages(screen) }
    }

    fun closeAll() {
        menu = null
        drawerOpen = false
        picker = null
        settingsOpen = false
        exitEdit()
    }

    // ───────── السحب ─────────

    fun startDrag(item: Item, pointer: Offset) {
        if (settings.lock) { say("الترتيب مقفل من الإعدادات"); return }
        if (item.container != Container.GRID) return
        if (!editing) enterEdit()
        val b = tileBounds[item.id] ?: return
        val scale = (b.width / geo.cellW).coerceAtLeast(0.01f)
        drag = Drag(item, Offset((pointer.x - b.left) / scale, (pointer.y - b.top) / scale), item.page).also { it.pointer = pointer }
        menu = null
        buzz()
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
        val slot = slotBounds.entries.firstOrNull { it.value.inflate(geo.cellW * 0.12f).contains(pointer) }?.key
        if (slot != null) {
            if (d.slot != slot) buzz()
            d.slot = slot; d.cand = null; d.solution = null
            return
        }
        d.slot = null
        edgeCheck(d)
        val tl = localTopLeft(d, d.page) ?: return
        val c = GridEngine.cellAt(tl.x, tl.y, geo.cellW, geo.cellH, d.item.w, d.item.h, geo.cols, geo.rows)
        if (c == d.cand) return
        d.cand = c
        val others = gridItems(d.page).filter { it.id != d.item.id }.map { Span(it.id, it.x, it.y, it.w, it.h) }
        d.solution = GridEngine.solve(others, Span(d.item.id, c.x, c.y, d.item.w, d.item.h), geo.cols, geo.rows)
    }

    private fun edgeCheck(d: Drag) {
        val dir = when {
            d.pointer.x < geo.edgePx -> -1
            d.pointer.x > geo.screenW - geo.edgePx -> 1
            else -> 0
        }
        if (dir == 0) { edgeJob?.cancel(); edgeJob = null; return }
        if (edgeJob?.isActive == true) return
        edgeJob = scope.launch {
            delay(520)
            val np = d.page + dir
            if (drag === d && np >= 0 && np < pager.pageCount) {
                d.cand = null; d.solution = null; d.page = np
                buzz()
                pager.animateScrollToPage(np)
                delay(60)
                edgeJob = null
                dragMove(d.pointer)
            }
        }
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
                for (o in gridItems(d.page)) {
                    if (o.id == it0.id) continue
                    val q = sol[o.id] ?: continue
                    if (q.x != o.x || q.y != o.y) upd += o.copy(x = q.x, y = q.y)
                }
                upd += it0.copy(container = Container.GRID, page = d.page, x = c.x, y = c.y)
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
        menu = Menu(item, b ?: return, slot)
        buzz()
    }

    fun removeFromHome(item: Item) {
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
