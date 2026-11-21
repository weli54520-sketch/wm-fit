package com.waleed.onyx.home

import android.appwidget.AppWidgetProviderInfo
import android.os.SystemClock
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.IntOffset
import androidx.core.view.drawToBitmap
import com.waleed.onyx.OnyxApp
import com.waleed.onyx.apps.AppInfo
import com.waleed.onyx.data.Container
import com.waleed.onyx.data.Item
import com.waleed.onyx.data.Kind
import com.waleed.onyx.grid.Box
import com.waleed.onyx.grid.Cell
import com.waleed.onyx.grid.DragBrain
import com.waleed.onyx.grid.DragEvent
import com.waleed.onyx.grid.DragTarget
import com.waleed.onyx.grid.GridEngine
import com.waleed.onyx.grid.HomeGeometry
import com.waleed.onyx.grid.PagerPhysics
import com.waleed.onyx.grid.Span
import com.waleed.onyx.grid.SpanLimits
import com.waleed.onyx.grid.WidgetSpan
import com.waleed.onyx.widget.WidgetData
import com.waleed.onyx.widget.Widgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** مقاسات الشبكة بالبكسل (للمعرض والويدجت) */
data class Geo(val cols: Int, val rows: Int, val cellW: Float, val cellH: Float)

/** خانة بالعمود الجانبي أو الشريط */
data class Slot(val container: Int, val index: Int)

/** وش تحت الإصبع لما يلمس الشاشة */
sealed class Hit {
    data class OnItem(val item: Item, val slot: Slot?) : Hit()
    data class OnEmptySlot(val slot: Slot) : Hit()
    /** مقبض تكبير الويدجت: يتعامل معه المقبض نفسه */
    object Handle : Hit()
    /** مكان فاضي بالصفحات */
    object Empty : Hit()
    /** برا الصفحات والعمود والشريط (فوق، النقاط…) */
    object Outside : Hit()
}

/** عنصر يتسحب الحين. كل الأماكن تنحسب من المقاسات، ما نقرا شي من الشاشة */
class Drag(
    val item: Item,
    /** لو السحب بدأ من العمود أو الشريط */
    val fromSlot: Slot?,
    /** مكان الإصبع داخل العنصر (dp، بدون تصغير) */
    val grabDp: Offset,
    startPage: Int,
    val snap: ImageBitmap? = null,
) {
    var pointer by mutableStateOf(Offset.Zero)
    var page by mutableIntStateOf(startPage)
    /** الخانة اللي تحت العنصر الحين (الظل يروح لها على طول) */
    var cand by mutableStateOf<Cell?>(null)
    /** الترتيب بعد ما وقف الإصبع (الأيقونات تزحف عليه) */
    var solution by mutableStateOf<Map<Long, Cell>?>(null)
    var solvedFor: Cell? = null
    var overflow by mutableStateOf<List<Long>>(emptyList())
    var slot by mutableStateOf<Slot?>(null)
    var slotReady by mutableStateOf(false)
    var edge by mutableIntStateOf(0)
    val brain = DragBrain()
}

data class Menu(val item: Item, val bounds: Rect, val slot: Slot?, val snap: ImageBitmap? = null)

/** ويدجت نكبره أو نصغره الحين بسحب الزاوية */
class Resize(val item: Item, val limits: SpanLimits) {
    var w by mutableIntStateOf(item.w)
    var h by mutableIntStateOf(item.h)
    var solution by mutableStateOf<Map<Long, Cell>?>(null)
}

/**
 * حالة الشاشة الرئيسية: الصفحات، وضع الترتيب، السحب، القوائم، واللوحات.
 * مدير واحد للمس (HomeGestures) ينادي هنا. كل مكان من HomeGeometry، وكل توقيت من DragBrain.
 */
class HomeState(val app: OnyxApp, private val scope: CoroutineScope) {
    val settings get() = app.settings
    val catalog get() = app.catalog
    private val repo get() = app.repo
    private val widgets: Widgets get() = app.widgets

    var haptic: HapticFeedback? = null
    var density = 1f
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

    /** الأيقونة المضغوطة الحين (تصغر تحت الإصبع) */
    var pressedKey by mutableStateOf<Any?>(null)
    var geo by mutableStateOf(Geo(4, 6, 1f, 1f))
    /** المقاسات كاملة بوحدة dp */
    var layout: HomeGeometry? = null

    val pages = PagesState(scope)
    /** تصغير الصفحة بوضع الترتيب (1 = عادي) */
    val editScale = Animatable(1f)

    /** شكل كل ويدجت على الشاشة، عشان نصوره لما يرتفع أو يتسحب */
    val widgetViews = HashMap<Long, View>()
    private val dropFrom = HashMap<Long, IntOffset>()

    fun gridItems(page: Int) = items.filter { it.container == Container.GRID && it.page == page }
    fun slotItem(slot: Slot) = items.firstOrNull { it.container == slot.container && it.page == slot.index }
    fun lastPage() = items.filter { it.container == Container.GRID }.maxOfOrNull { it.page } ?: 0

    /** أيقونة بتطلع من الصفحة المليانة وأنت تسحب */
    fun isOverflow(id: Long): Boolean = drag?.overflow?.contains(id) == true

    /** المكان المؤقت لعنصر وأنت تسحب أو تكبر ويدجت (بعد ما يوقف الإصبع) */
    fun previewCell(page: Int, id: Long): Cell? {
        val d = drag
        if (d != null && d.page == page && d.item.id != id) return d.solution?.get(id)
        val r = resize
        if (r != null && r.item.page == page && r.item.id != id) return r.solution?.get(id)
        return null
    }

    /** الاهتزاز: الضغطة الطويلة، بداية السحب، النزول، تقليب الصفحة. بس */
    fun buzz() {
        if (settings.haptics) haptic?.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun say(msg: String) { toast = msg }

    private fun k() = editScale.value
    private fun dp(p: Offset) = Offset(p.x / density, p.y / density)
    private fun px(b: Box) = Rect(b.l * density, b.t * density, b.r * density, b.b * density)

    // ───────── وش تحت الإصبع ─────────

    fun hit(p: Offset): Hit {
        val g = layout ?: return Hit.Outside
        val d = dp(p)
        g.sideSlotAt(d.x, d.y)?.let { i ->
            val s = Slot(Container.SIDE, i)
            return slotItem(s)?.let { Hit.OnItem(it, s) } ?: Hit.OnEmptySlot(s)
        }
        g.dockSlotAt(d.x, d.y)?.let { i ->
            val s = Slot(Container.DOCK, i)
            return slotItem(s)?.let { Hit.OnItem(it, s) } ?: Hit.OnEmptySlot(s)
        }
        if (!g.inPages(d.x, d.y)) return Hit.Outside
        val page = pages.current
        val (lx, ly) = g.toLocal(d.x, d.y, k())
        for (it in gridItems(page)) {
            val b = g.cellBox(it.x, it.y, it.w, it.h)
            if (lx >= b.l && lx <= b.r && ly >= b.t && ly <= b.b) {
                if (editing && it.kind == Kind.WIDGET && canResize(it)) {
                    // مقبض التكبير بالزاوية اللي تحت (40 مربع)
                    if (lx >= b.r - 40f && ly >= b.b - 40f) return Hit.Handle
                }
                return Hit.OnItem(it, null)
            }
        }
        return Hit.Empty
    }

    fun inPages(p: Offset): Boolean {
        val g = layout ?: return false
        val d = dp(p)
        return g.inPages(d.x, d.y)
    }

    /** عرض الصفحة بالبكسل (للسحب) */
    fun pageWidthPx(): Float = (layout?.pages?.w ?: 1f) * density

    /** مكان العنصر على الشاشة (بكسل) */
    fun itemRect(item: Item, slot: Slot?): Rect? {
        val g = layout ?: return null
        if (slot != null) {
            val b = if (slot.container == Container.SIDE) g.sideSlots.getOrNull(slot.index) else g.dockSlots.getOrNull(slot.index)
            return b?.let { px(it) }
        }
        return px(g.toScreen(g.cellBox(item.x, item.y, item.w, item.h), k()))
    }

    // ───────── الضغط ─────────

    fun tap(hit: Hit) {
        when (hit) {
            is Hit.OnItem -> if (!editing && hit.item.kind == Kind.APP) launch(hit.item, itemRect(hit.item, hit.slot))
            is Hit.OnEmptySlot -> openPicker(hit.slot)
            else -> {}
        }
    }

    fun openPicker(slot: Slot) {
        picker = slot
        drawerOpen = true
    }

    // ───────── وضع الترتيب ─────────

    fun enterEdit() {
        if (editing) return
        if (settings.lock) { say("الترتيب مقفل من الإعدادات"); return }
        menu = null
        editing = true
        if (settings.editStyle == 0) scope.launch { editScale.animateTo(HomeGeometry.EDIT_SCALE, smooth()) }
    }

    fun exitEdit() {
        if (!editing) return
        editing = false
        drag = null
        resize = null
        scope.launch { editScale.animateTo(1f, smooth()) }
        scope.launch { repo.compactPages(screen) }
    }

    private fun <T> smooth() = spring<T>(dampingRatio = 1f, stiffness = if (settings.reduceMotion) 4000f else Spring.StiffnessMediumLow)

    fun closeAll() {
        menu = null
        drawerOpen = false
        picker = null
        settingsOpen = false
        galleryOpen = false
        exitEdit()
    }

    fun goHome() {
        pages.animateTo(0, PagerPhysics.settleDurationMs(pages.offset, 0, calm()))
    }

    fun calm() = settings.speed != 2

    // ───────── السحب ─────────

    fun startDrag(item: Item, slot: Slot?, pointer: Offset) {
        if (settings.lock) { say("الترتيب مقفل من الإعدادات"); return }
        if (item.kind == Kind.WIDGET && slot != null) return
        val g = layout ?: return
        val k = k()
        val f = dp(pointer)
        val grab = if (slot == null) {
            val b = g.toScreen(g.cellBox(item.x, item.y, item.w, item.h), k)
            Offset((f.x - b.l) / k, (f.y - b.t) / k)
        } else {
            // من العمود أو الشريط: نمسكه من نص خانة عادية
            val b = (if (slot.container == Container.SIDE) g.sideSlots[slot.index] else g.dockSlots[slot.index])
            Offset(g.cell / 2 + (f.x - b.cx), g.rowH / 2 + (f.y - b.cy))
        }
        val snap = if (item.kind == Kind.WIDGET) snapshot(item.id) else null
        if (!editing) enterEdit()
        if (!editing) return
        pages.settle(calm())
        val d = Drag(item, slot, grab, pages.current, snap).also { it.pointer = pointer }
        drag = d
        menu = null
        buzz()
        // نشيك كل ٥٠ms حتى لو الإصبع واقف (عشان الزحف والتقليب بعد الوقفة)
        scope.launch {
            while (drag === d) {
                tick(d)
                delay(50)
            }
        }
    }

    fun dragMove(pointer: Offset) {
        val d = drag ?: return
        d.pointer = pointer
        tick(d)
    }

    /** مكان العنصر المسحوب على الشاشة (dp): الزاوية اللي فوق + المقاس، بالتصغير الحالي */
    fun dragBoxDp(d: Drag): Box? {
        val g = layout ?: return null
        val k = k()
        val f = dp(d.pointer)
        val l = f.x - d.grabDp.x * k
        val t = f.y - d.grabDp.y * k
        return Box(l, t, l + g.cell * d.item.w * k, t + g.rowH * d.item.h * k)
    }

    private fun targetFor(d: Drag): DragTarget {
        val g = layout ?: return DragTarget.Nowhere
        val b = dragBoxDp(d) ?: return DragTarget.Nowhere
        val cx = b.cx
        val cy = b.cy
        if (d.item.kind != Kind.WIDGET) {
            g.sideSlotAt(cx, cy)?.let { return DragTarget.SlotAt(Container.SIDE, it) }
            g.dockSlotAt(cx, cy)?.let { return DragTarget.SlotAt(Container.DOCK, it) }
        }
        val e = g.edgeAt(cx, cy)
        if (e == -1 && d.page > 0) return DragTarget.EdgeAt(-1)
        if (e == 1 && d.page + 1 < pages.count) return DragTarget.EdgeAt(1)
        if (cx >= g.pages.l && cx <= g.pages.r && cy >= g.pages.t - g.rowH / 2 && cy <= g.pages.b + g.rowH / 2) {
            val (x, y) = g.cellFor(cx, cy, d.item.w, d.item.h, k())
            return DragTarget.CellAt(d.page, x, y)
        }
        return DragTarget.Nowhere
    }

    private var lastLogged: DragTarget? = null

    private fun tick(d: Drag) {
        if (drag !== d || pages.animating) return
        val t = targetFor(d)
        // سجل للجوال الوهمي: وين الإصبع ووش تحته (يساعدنا نشوف الأخطاء بدون تخمين)
        if (t != lastLogged) {
            lastLogged = t
            val b = dragBoxDp(d)
            android.util.Log.d("OnyxDrag", "pointer=${d.pointer.x.roundToInt()},${d.pointer.y.roundToInt()}px center=${b?.cx?.roundToInt()},${b?.cy?.roundToInt()}dp size=${b?.w?.roundToInt()}x${b?.h?.roundToInt()}dp kind=${d.item.kind} k=${"%.2f".format(k())} page=${d.page} target=$t")
        }
        when (t) {
            is DragTarget.CellAt -> { d.cand = Cell(t.x, t.y); d.slot = null; d.edge = 0 }
            is DragTarget.SlotAt -> { d.slot = Slot(t.container, t.index); d.cand = null; d.edge = 0 }
            is DragTarget.EdgeAt -> { d.edge = t.dir; d.slot = null; d.cand = null }
            DragTarget.Nowhere -> { d.slot = null; d.cand = null; d.edge = 0 }
        }
        if (t !is DragTarget.SlotAt) d.slotReady = false
        when (val e = d.brain.update(t, SystemClock.uptimeMillis())) {
            is DragEvent.Commit -> solveFor(d, Cell(e.cell.x, e.cell.y))
            is DragEvent.SlotReady -> d.slotReady = true
            is DragEvent.Flip -> {
                val np = (d.page + e.dir).coerceIn(0, pages.count - 1)
                if (np != d.page) {
                    d.page = np
                    d.solution = null; d.solvedFor = null; d.overflow = emptyList(); d.cand = null
                    d.brain.pageChanged()
                    pages.animateTo(np, PagerPhysics.settleDurationMs(pages.offset, np, true))
                    buzz()
                }
            }
            null -> {}
        }
    }

    /** الترتيب لو نزل العنصر بخانة [c] (ما يتغير شي إلا بعد الوقفة أو الإفلات) */
    private fun solveFor(d: Drag, c: Cell) {
        val g = layout ?: return
        val others = gridItems(d.page).filter { it.id != d.item.id }.map { Span(it.id, it.x, it.y, it.w, it.h) }
        val moving = Span(d.item.id, c.x, c.y, d.item.w, d.item.h)
        d.solvedFor = c
        if (d.item.w == 1 && d.item.h == 1) {
            val from = if (d.fromSlot == null && d.page == d.item.page) Cell(d.item.x, d.item.y) else null
            val r = GridEngine.reorder(others, moving, g.cols, g.rows, from)
            d.solution = r?.cells
            d.overflow = r?.overflow ?: emptyList()
        } else {
            d.solution = GridEngine.solve(others, moving, g.cols, g.rows)
            d.overflow = emptyList()
        }
    }

    fun dragEnd() {
        val d = drag ?: return
        val g = layout
        val t = targetFor(d)
        android.util.Log.d("OnyxDrag", "drop pointer=${d.pointer.x.roundToInt()},${d.pointer.y.roundToInt()}px target=$t")
        lastLogged = null
        val it0 = d.item
        val upd = ArrayList<Item>()
        // مكان العنصر لحظة الإفلات (عشان ينزل بنعومة من هناك)
        val box = dragBoxDp(d)
        fun rememberDropFrom() {
            if (g == null || box == null) return
            val (lx, ly) = g.toLocal(box.l, box.t, k())
            dropFrom[it0.id] = IntOffset(((lx - g.grid.l) * density).roundToInt(), ((ly - g.grid.t) * density).roundToInt())
        }
        when (t) {
            is DragTarget.SlotAt -> {
                val slot = Slot(t.container, t.index)
                if (slot != d.fromSlot && it0.kind == Kind.APP) {
                    val target = slotItem(slot)
                    upd += it0.copy(container = slot.container, page = slot.index, x = 0, y = 0)
                    if (target != null && target.id != it0.id) upd += target.copy(container = it0.container, page = it0.page, x = it0.x, y = it0.y)
                }
            }
            is DragTarget.CellAt -> {
                val c = Cell(t.x, t.y)
                if (d.solvedFor != c) solveFor(d, c)
                val sol = d.solution
                if (sol != null && g != null) {
                    rememberDropFrom()
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
                            val spot = GridEngine.freeSpot(taken, 1, 1, g.cols, g.rows)
                            if (spot != null) { placed[id] = o.copy(page = p, x = spot.x, y = spot.y); break }
                            p++
                        }
                    }
                    upd += placed.values
                } else if (d.fromSlot == null && d.page == it0.page) rememberDropFrom()
            }
            else -> if (d.fromSlot == null && d.page == it0.page) rememberDropFrom()
        }
        applyLocal(upd)
        dropTick++
        drag = null
        if (upd.isNotEmpty()) { buzz(); scope.launch { repo.save(upd) } }
    }

    fun takeDropFrom(id: Long): IntOffset? = dropFrom.remove(id)

    private fun applyLocal(upd: List<Item>) {
        if (upd.isEmpty()) return
        val map = upd.associateBy { it.id }
        items = items.map { map[it.id] ?: it }
    }

    /** السحب لتحت: يفتح لوحة الإشعارات حق سامسونج (والسحبة الثانية داخل اللوحة نفسها) */
    fun openNotifications() {
        try {
            val sb = app.getSystemService("statusbar")
            Class.forName("android.app.StatusBarManager").getMethod("expandNotificationsPanel").invoke(sb)
        } catch (e: Exception) {
            say("ما قدرت أفتح الإشعارات")
        }
    }

    // ───────── القوائم ─────────

    fun openMenu(item: Item, slot: Slot?) {
        val b = itemRect(item, slot) ?: return
        val snap = if (item.kind == Kind.WIDGET) snapshot(item.id) else null
        menu = Menu(item, b, slot, snap)
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
            var page = pages.current.coerceAtMost(lastPage())
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
                delay(150)
                pages.animateTo(page, PagerPhysics.settleDurationMs(pages.offset, page, calm()))
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
        buzz()
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
        var page = pages.current.coerceAtMost(lastPage())
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
        scope.launch {
            delay(150)
            pages.animateTo(page, PagerPhysics.settleDurationMs(pages.offset, page, calm()))
        }
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
