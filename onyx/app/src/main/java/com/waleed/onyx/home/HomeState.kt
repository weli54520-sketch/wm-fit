package com.waleed.onyx.home

import android.appwidget.AppWidgetProviderInfo
import android.os.SystemClock
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.waleed.onyx.grid.DockOrder
import com.waleed.onyx.grid.DotsMath
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
    /** مقبض تكبير الويدجت */
    data class Handle(val item: Item) : Hit()
    /** مكان فاضي بالصفحات */
    object Empty : Hit()
    /** نقاط الصفحات */
    object Dots : Hit()
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
    /** فوق الشريط وفيه مكان: وين يفسحون له (null = ما يفسحون) */
    var dockGap by mutableStateOf<Int?>(null)
    val brain = DragBrain()
}

data class Menu(val item: Item, val bounds: Rect, val slot: Slot?, val snap: ImageBitmap? = null)

/** ويدجت نكبره أو نصغره الحين بسحب الزاوية: يتبع الإصبع بالضبط، والخانات تتحدد لما تفلت */
class Resize(val item: Item, val limits: SpanLimits, startWDp: Float, startHDp: Float) {
    /** المقاس الحي (dp) تحت إصبعك */
    var liveW by mutableFloatStateOf(startWDp)
    var liveH by mutableFloatStateOf(startHDp)
    /** المقاس بالخانات اللي بينزل عليه */
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
    /** القائمة انسكرت لأن السحب بدأ: الأيقونة المرفوعة تختفي فوراً (ما تبقى نسخة باهتة) */
    var menuCut by mutableStateOf(false)
        private set
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
    /** أيقونة نزلت بالشريط: مكانها لحظة الإفلات (dp على الشاشة) عشان تنزل بنعومة من هناك */
    private val dockDropFrom = HashMap<Long, Offset>()

    fun gridItems(page: Int) = items.filter { it.container == Container.GRID && it.page == page }
    fun slotItem(slot: Slot) = items.firstOrNull { it.container == slot.container && it.page == slot.index }
    fun lastPage() = items.filter { it.container == Container.GRID }.maxOfOrNull { it.page } ?: 0

    // ───────── الشريط مثل الآيفون ─────────

    fun dockCapacity() = layout?.dockSlots?.size ?: settings.dockCount

    /** أيقونات الشريط بالترتيب (بدون فراغات) */
    fun dockItems() = items.filter { it.container == Container.DOCK }.sortedBy { it.page }.take(dockCapacity())

    /** رقم حفظ جديد لآخر الشريط (ما يتعارض مع أي أيقونة فيه) */
    private fun nextDockPage() = (items.filter { it.container == Container.DOCK }.maxOfOrNull { it.page } ?: -1) + 1

    /**
     * شكل الشريط الحين: الأيقونات بالترتيب (null = المكان اللي يفسحونه للمسحوبة)،
     * و + بالآخر لو فيه مكان وأنت بوضع الترتيب.
     */
    fun dockView(): Pair<List<Long?>, Boolean> {
        val ids = dockItems().map { it.id }
        val d = drag
        if (d != null) {
            val fromDock = d.fromSlot?.container == Container.DOCK
            return DockOrder.arrange(ids, if (fromDock) d.item.id else null, d.dockGap) to false
        }
        return ids to (editing && ids.size < dockCapacity())
    }

    /** مكان أيقونة بالشريط الحين (dp) */
    fun dockBox(id: Long): Box? {
        val g = layout ?: return null
        val (list, plus) = dockView()
        val i = list.indexOf(id)
        if (i < 0) return null
        return g.dockRow(list.size + if (plus) 1 else 0).getOrNull(i)
    }

    fun takeDockDrop(id: Long): Offset? = dockDropFrom.remove(id)

    /** بعد أي تغيير: الشريط يرجع متجمع 0،1،2… (ما يبقى فراغ بالنص) */
    private fun compactDock(upd: MutableList<Item>) {
        val map = LinkedHashMap<Long, Item>()
        for (u in upd) map[u.id] = u
        val merged = items.map { map[it.id] ?: it }
        val dock = merged.filter { it.container == Container.DOCK }
            .sortedWith(compareBy({ it.page }, { if (map.containsKey(it.id)) 0 else 1 }))
        dock.forEachIndexed { i, o -> if (o.page != i) map[o.id] = o.copy(page = i) }
        upd.clear()
        upd += map.values
    }

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
        if (g.dock != null) {
            val (list, plus) = dockView()
            val row = g.dockRow(list.size + if (plus) 1 else 0)
            val i = row.indexOfFirst { d.x >= it.l && d.x <= it.r && d.y >= it.t && d.y <= it.b }
            if (i >= 0) {
                val id = list.getOrNull(i)
                val item = id?.let { x -> items.firstOrNull { it.id == x } }
                return if (item != null) Hit.OnItem(item, Slot(Container.DOCK, item.page))
                else Hit.OnEmptySlot(Slot(Container.DOCK, nextDockPage()))
            }
        }
        if (!g.inPages(d.x, d.y)) {
            if (d.y >= g.dots.t - 6f && d.y <= g.dots.b + 4f && DotsMath.onRow(d.x, g.width / 2f, pages.count)) return Hit.Dots
            return Hit.Outside
        }
        val page = pages.current
        val (lx, ly) = g.toLocal(d.x, d.y, k())
        for (it in gridItems(page)) {
            val b = g.cellBox(it.x, it.y, it.w, it.h)
            if (lx >= b.l && lx <= b.r && ly >= b.t && ly <= b.b) {
                if (editing && it.kind == Kind.WIDGET && canResize(it)) {
                    // مقبض التكبير بالزاوية اللي تحت (40 مربع)
                    if (lx >= b.r - 44f && ly >= b.b - 44f) return Hit.Handle(it)
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
            val b = if (slot.container == Container.SIDE) g.sideSlots.getOrNull(slot.index) else dockBox(item.id)
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

    /** النقاط: الصفحة اللي تحت الإصبع (ضغطة أو سحب) */
    fun dotsGo(xPx: Float, fast: Boolean) {
        val g = layout ?: return
        val i = DotsMath.indexAt(xPx / density, g.width / 2f, pages.count)
        if (i == pages.current && !pages.animating) return
        if (pages.animating && i == pages.target) return
        android.util.Log.d("OnyxPages", "dots go=$i")
        if (fast) buzzLight()
        pages.animateTo(i, if (fast) 260 else PagerPhysics.settleDurationMs(pages.offset, i, calm()))
    }

    private fun buzzLight() {
        if (settings.haptics) haptic?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
            val b = (if (slot.container == Container.SIDE) g.sideSlots.getOrNull(slot.index) else dockBox(item.id)) ?: return
            Offset(g.cell / 2 + (f.x - b.cx), g.rowH / 2 + (f.y - b.cy))
        }
        val snap = if (item.kind == Kind.WIDGET) snapshot(item.id) else null
        if (!editing) enterEdit()
        if (!editing) return
        pages.settle(calm())
        val d = Drag(item, slot, grab, pages.current, snap).also { it.pointer = pointer }
        if (menu != null) menuCut = true
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
            if (g.inDockZone(cx, cy)) {
                val dock = dockItems()
                val fromDock = d.fromSlot?.container == Container.DOCK
                // فيه مكان (أو من الشريط نفسه): يفسحون له تحت إصبعك مثل الآيفون
                if (DockOrder.canInsert(dock.size, dockCapacity(), fromDock))
                    return DragTarget.DockAt(g.dockInsertIndex(cx, DockOrder.slotsWhileHovering(dock.size, fromDock)))
                // مليان: فوق أيقونة = تبديل
                val i = g.dockRow(dock.size).indexOfFirst { cx >= it.l && cx <= it.r }
                return if (i >= 0) DragTarget.SlotAt(Container.DOCK, dock[i].page) else DragTarget.Nowhere
            }
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
        d.dockGap = (t as? DragTarget.DockAt)?.index
        when (t) {
            is DragTarget.DockAt -> { d.slot = null; d.cand = null; d.edge = 0 }
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
        var t = targetFor(d)
        // فلت عند طرف الصفحة: ينزل بالصفحة اللي قدامك بأقرب خانة (ما يضيع النقل)
        if (t is DragTarget.EdgeAt && g != null) {
            val b = dragBoxDp(d)
            if (b != null) {
                val cx = b.cx.coerceIn(g.grid.l + 1f, g.grid.r - 1f)
                val (x, y) = g.cellFor(cx, b.cy, d.item.w, d.item.h, k())
                t = DragTarget.CellAt(d.page, x, y)
            }
        }
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
        fun rememberDockDrop() {
            if (box != null) dockDropFrom[it0.id] = Offset(box.cx, box.cy)
        }
        when (t) {
            is DragTarget.DockAt -> if (it0.kind == Kind.APP) {
                val order = DockOrder.drop(dockItems().map { it.id }, it0.id, t.index)
                order.forEachIndexed { i, id ->
                    val o = if (id == it0.id) it0 else items.first { it.id == id }
                    val u = if (id == it0.id) o.copy(container = Container.DOCK, page = i, x = 0, y = 0) else o.copy(page = i)
                    if (u != o) upd += u
                }
                rememberDockDrop()
                android.util.Log.d("OnyxDock", "drop at=${t.index} order=${order.joinToString(",")}")
            }
            is DragTarget.SlotAt -> {
                val slot = Slot(t.container, t.index)
                if (slot != d.fromSlot && it0.kind == Kind.APP) {
                    val target = slotItem(slot)
                    upd += it0.copy(container = slot.container, page = slot.index, x = 0, y = 0)
                    if (target != null && target.id != it0.id) upd += target.copy(container = it0.container, page = it0.page, x = it0.x, y = it0.y)
                    if (slot.container == Container.DOCK) rememberDockDrop()
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
        compactDock(upd)
        applyLocal(upd)
        dropTick++
        drag = null
        logDock()
        if (upd.isNotEmpty()) { buzz(); scope.launch { repo.save(upd) } }
    }

    fun takeDropFrom(id: Long): IntOffset? = dropFrom.remove(id)

    /** سجل للجوال الوهمي: الشريط بعد كل تغيير (الترتيب، وهل فيه فراغ) */
    private fun logDock() {
        val all = items.filter { it.container == Container.DOCK }.sortedBy { it.page }
        android.util.Log.d("OnyxDock", "dock pages=${all.joinToString(",") { it.page.toString() }} apps=${all.joinToString(",") { it.appKey.substringBefore('/') }}")
    }

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
        menuCut = false
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

    /** خلفية الويدجت نفسه شفافة (الأسود يختفي ويبان الزجاج والخلفية) */
    fun toggleClear(item: Item) {
        val d = widgetData(item) ?: return
        val u = item.copy(data = d.copy(clear = !d.clear).encode())
        applyLocal(listOf(u))
        scope.launch { repo.save(listOf(u)) }
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
        val L = layout ?: return
        resize = Resize(item, lim, item.w * L.cell, item.h * L.rowH).also { it.solution = emptyMap() }
        buzz()
    }

    /** [dxPx] [dyPx] من بداية السحب (بكسل الشاشة): الويدجت يتبع الإصبع، والخانات بتأخير 60% */
    fun resizeDrag(dxPx: Float, dyPx: Float) {
        val r = resize ?: return
        val g = layout ?: return
        val k = k()
        val s = WidgetSpan.liveResize(
            r.item.w, r.item.h, r.w, r.h, dxPx / (density * k), dyPx / (density * k),
            g.cell, g.rowH, r.item.x, r.item.y, g.cols, g.rows, r.limits,
        )
        r.liveW = s.wDp
        r.liveH = s.hDp
        android.util.Log.d("OnyxResize", "live=${s.wDp.roundToInt()}x${s.hDp.roundToInt()}")
        if (s.w == r.w && s.h == r.h) return
        val others = gridItems(r.item.page).filter { it.id != r.item.id }.map { Span(it.id, it.x, it.y, it.w, it.h) }
        val sol = GridEngine.solve(others, Span(r.item.id, r.item.x, r.item.y, s.w, s.h), g.cols, g.rows)
        if (sol == null) { android.util.Log.d("OnyxResize", "noroom cells=${s.w}x${s.h}"); return }
        r.w = s.w; r.h = s.h; r.solution = sol
        android.util.Log.d("OnyxResize", "cells=${s.w}x${s.h}")
    }

    fun resizeEnd() {
        val r = resize ?: return
        android.util.Log.d("OnyxResize", "end cells=${r.w}x${r.h} from=${r.item.w}x${r.item.h}")
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
        val upd = mutableListOf(item.copy(container = Container.HIDDEN))
        compactDock(upd)
        applyLocal(upd)
        scope.launch { repo.save(upd) }
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
        compactDock(upd)
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
