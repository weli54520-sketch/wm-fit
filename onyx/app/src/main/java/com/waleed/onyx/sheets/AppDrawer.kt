package com.waleed.onyx.sheets

import android.Manifest
import android.app.Activity
import android.app.SearchManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.waleed.onyx.apps.AppInfo
import com.waleed.onyx.grid.Calc
import com.waleed.onyx.grid.Recents
import com.waleed.onyx.home.AppIcon
import com.waleed.onyx.home.AppLabel
import com.waleed.onyx.home.HomeState
import com.waleed.onyx.home.NotifDot
import com.waleed.onyx.home.Pill
import com.waleed.onyx.system.Notifs
import com.waleed.onyx.ui.Onyx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val CardTitle = TextStyle(color = Onyx.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
private val Small = TextStyle(color = Onyx.Muted, fontSize = 13.sp)

private data class Contact(val name: String, val uri: Uri)

/**
 * السحب لفوق: مثل لوحة سامسونج بصورة وليد.
 * التطبيقات المقترحة (آخر ٨ فتحتها)، آخر عمليات البحث، وكل التطبيقات بالفئات،
 * وتحت شريط بحث ذكي: تطبيقات، حاسبة، جهات اتصال، قوقل، والمتجر.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun AppDrawer(state: HomeState, inner: Boolean, icon: Dp) {
    val all by state.catalog.apps.collectAsState()
    val ctx = LocalContext.current
    var query by remember { mutableStateOf("") }
    var held by remember { mutableStateOf<AppInfo?>(null) }
    val keyboard = LocalSoftwareKeyboardController.current
    val picking = state.picker != null || state.altPicker != null
    LaunchedEffect(state.drawerOpen) {
        if (!state.drawerOpen) { query = ""; held = null }
    }
    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) {
            r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { query = it }
        }
    }
    var contactsOk by remember { mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) }
    val askContacts = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { contactsOk = it }

    AnimatedVisibility(
        visible = state.drawerOpen,
        enter = fadeIn(tween(220)) + slideInVertically(tween(380)) { it / 4 },
        exit = fadeOut(tween(200)) + slideOutVertically(tween(300)) { it / 4 },
    ) {
        val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val cols = if (inner) 7 else 4
        val hidden = state.settings.hidden
        val apps = remember(all, hidden) { all.filter { it.key !in hidden } }
        val q = query.trim()
        val calc = remember(q) { Calc.eval(q)?.let { Calc.format(it) } }
        val results = remember(q, apps) { if (q.isEmpty()) emptyList() else apps.filter { it.label.contains(q, ignoreCase = true) || it.pkg.contains(q, ignoreCase = true) } }
        val contacts by produceState(emptyList<Contact>(), q, contactsOk) {
            value = if (q.length < 2 || !contactsOk || calc != null) emptyList() else withContext(Dispatchers.IO) { searchContacts(ctx, q) }
        }
        fun open(a: AppInfo) {
            when {
                state.altPicker != null -> state.assignAlt(a)
                state.picker != null -> state.assignSlot(state.picker!!, a)
                else -> {
                    if (q.isNotEmpty()) state.settings.searches = Recents.push(state.settings.searches, q, 6)
                    state.launchApp(a)
                    state.drawerOpen = false
                }
            }
        }
        val searchFocus = remember { FocusRequester() }
        LaunchedEffect(state.searchFocus) {
            if (state.searchFocus) { searchFocus.requestFocus(); state.searchFocus = false }
        }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0xE60E0E12), Color(0xF5060608))))
                    .combinedClickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = { held = null }),
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = top + 12.dp, bottom = bottom + 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item(key = "head") {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            BasicText(
                                when {
                                    state.altPicker != null -> "اختار تطبيق الضغطتين"
                                    state.picker != null -> "اختار تطبيق"
                                    else -> "التطبيقات"
                                },
                                style = TextStyle(color = Onyx.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold),
                            )
                            Spacer(Modifier.weight(1f))
                            Pill("إغلاق") { state.drawerOpen = false; state.picker = null; state.altPicker = null }
                        }
                    }
                    if (q.isEmpty()) {
                        val recent = state.settings.recents.mapNotNull { k -> apps.firstOrNull { it.key == k } }.take(8)
                        if (recent.isNotEmpty() && !picking) {
                            item(key = "recent") {
                                Card("التطبيقات المقترحة") { AppRows(state, recent, cols, icon, ::open) { held = it } }
                            }
                        }
                        val searches = state.settings.searches
                        if (searches.isNotEmpty() && !picking) {
                            item(key = "searches") {
                                Card("عمليات البحث الأخيرة", action = "مسح الكل", onAction = { state.settings.searches = emptyList() }) {
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        for (sq in searches) {
                                            Row(
                                                Modifier
                                                    .height(38.dp)
                                                    .clip(RoundedCornerShape(50))
                                                    .background(Color(0x1FFFFFFF))
                                                    .clickable { query = sq }
                                                    .padding(start = 14.dp, end = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                BasicText(sq, style = TextStyle(color = Onyx.Text, fontSize = 14.sp, fontWeight = FontWeight.Medium))
                                                Spacer(Modifier.width(6.dp))
                                                Box(
                                                    Modifier.size(26.dp).clip(CircleShape).clickable { state.settings.searches = state.settings.searches - sq },
                                                    contentAlignment = Alignment.Center,
                                                ) { BasicText("×", style = TextStyle(color = Onyx.Muted, fontSize = 16.sp)) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (state.settings.drawerSort == 0) {
                            val groups = apps.groupBy { state.catalog.category(it) }
                            for (cat in state.catalog.categoryOrder) {
                                val list = groups[cat] ?: continue
                                item(key = "cat_$cat") { Card(cat) { AppRows(state, list, cols, icon, ::open) { held = it } } }
                            }
                        } else {
                            item(key = "all") { Card("كل التطبيقات") { AppRows(state, apps, cols, icon, ::open) { held = it } } }
                        }
                    } else {
                        if (calc != null) {
                            item(key = "calc") {
                                Card("الحاسبة") {
                                    BasicText("= $calc", style = TextStyle(color = Onyx.Text, fontSize = 34.sp, fontWeight = FontWeight.Light, textAlign = TextAlign.Left), modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                        if (results.isNotEmpty()) {
                            item(key = "res") { Card("التطبيقات") { AppRows(state, results, cols, icon, ::open) { held = it } } }
                        }
                        if (calc == null && !picking) {
                            item(key = "contacts") {
                                Card("جهات الاتصال") {
                                    if (!contactsOk) {
                                        ActionRow("اسمح بالبحث بجهات الاتصال") { askContacts.launch(Manifest.permission.READ_CONTACTS) }
                                    } else if (contacts.isEmpty()) {
                                        BasicText("ما فيه أحد بهالاسم", style = Small)
                                    } else for (c in contacts) {
                                        ActionRow(c.name) {
                                            state.settings.searches = Recents.push(state.settings.searches, q, 6)
                                            state.openSystem(Intent(Intent.ACTION_VIEW, c.uri))
                                        }
                                    }
                                }
                            }
                            item(key = "web") {
                                Card("ابحث برا") {
                                    ActionRow("قوقل: $q") {
                                        state.settings.searches = Recents.push(state.settings.searches, q, 6)
                                        val i = Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, q)
                                        if (i.resolveActivity(ctx.packageManager) != null) state.openSystem(i)
                                        else state.openSystem(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(q))))
                                    }
                                    ActionRow("المتجر: $q") {
                                        state.settings.searches = Recents.push(state.settings.searches, q, 6)
                                        state.openSystem(Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=" + Uri.encode(q))))
                                    }
                                }
                            }
                        }
                    }
                }

                // شريط البحث تحت (فوق الكيبورد)
                Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().imePadding().padding(start = 16.dp, end = 16.dp, bottom = bottom + 14.dp)) {
                    val h = held
                    if (h != null && !picking) {
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0xF21C1C21)).padding(6.dp),
                        ) {
                            BasicText(h.label, style = CardTitle.copy(fontSize = 15.sp), modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                            ActionRow("أضفه للشاشة") { held = null; state.addToHome(h) }
                            ActionRow("إخفاء التطبيق") { held = null; state.hideApp(h) }
                            ActionRow("معلومات التطبيق") { held = null; state.catalog.openInfo(h) }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xF2EEF0F6))
                            .padding(start = 20.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (query.isEmpty()) BasicText("وش تدور؟ تطبيق، حساب، أو أي شي", style = TextStyle(color = Color(0xFF7A7F8C), fontSize = 16.sp))
                            BasicTextField(
                                value = query,
                                onValueChange = { query = it },
                                singleLine = true,
                                textStyle = TextStyle(color = Color(0xFF111114), fontSize = 16.sp),
                                cursorBrush = SolidColor(Color(0xFF111114)),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = {
                                    keyboard?.hide()
                                    results.firstOrNull()?.let { open(it) }
                                }),
                                modifier = Modifier.fillMaxWidth().focusRequester(searchFocus),
                            )
                        }
                        if (query.isNotEmpty()) {
                            Box(Modifier.size(40.dp).clip(CircleShape).clickable { query = "" }, contentAlignment = Alignment.Center) {
                                BasicText("×", style = TextStyle(color = Color(0xFF555A66), fontSize = 22.sp))
                            }
                        }
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).clickable {
                                val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
                                try { voice.launch(i) } catch (_: Exception) { state.say("البحث بالصوت مو متوفر") }
                            },
                            contentAlignment = Alignment.Center,
                        ) { MicIcon() }
                    }
                }
            }
        }
    }
}

/** بطاقة زجاج مثل لوحة سامسونج */
@Composable
private fun Card(title: String, action: String? = null, onAction: () -> Unit = {}, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0x1AFFFFFF))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(title, style = CardTitle)
            Spacer(Modifier.weight(1f))
            if (action != null) BasicText(action, style = Small, modifier = Modifier.clickable(onClick = onAction))
        }
        content()
    }
}

@Composable
private fun ActionRow(text: String, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicText(text, style = TextStyle(color = Onyx.Text, fontSize = 15.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** التطبيقات صفوف بعدد ثابت: ضغطة تفتح، ضغطة طويلة خيارات */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppRows(state: HomeState, list: List<AppInfo>, cols: Int, icon: Dp, onOpen: (AppInfo) -> Unit, onHold: (AppInfo) -> Unit) {
    val notifs by Notifs.byPkg.collectAsState()
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        for (row in list.chunked(cols)) {
            Row(Modifier.fillMaxWidth()) {
                for (i in 0 until cols) {
                    val a = row.getOrNull(i)
                    Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                        if (a != null) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .combinedClickable(onClick = { onOpen(a) }, onLongClick = { state.buzz(); onHold(a) })
                                    .padding(4.dp),
                            ) {
                                Box(Modifier.size(icon)) {
                                    AppIcon(a, state.catalog, icon, state.settings.iconStyle)
                                    if (state.settings.dots && notifs.containsKey(a.pkg)) NotifDot(icon * 0.2f, Modifier.align(Alignment.TopEnd))
                                }
                                Spacer(Modifier.height(6.dp))
                                Box(Modifier.width(icon + 18.dp).height(16.dp), contentAlignment = Alignment.Center) { AppLabel(a.label, icon + 18.dp) }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** ميكروفون بخطوط */
@Composable
private fun MicIcon() {
    Box(
        Modifier.size(22.dp).drawBehind {
            val w = size.width
            val h = size.height
            val st = Stroke(2.dp.toPx(), cap = StrokeCap.Round)
            val c = Color(0xFF222630)
            drawRoundRect(c, topLeft = Offset(w * 0.34f, 0f), size = androidx.compose.ui.geometry.Size(w * 0.32f, h * 0.62f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.16f))
            drawArc(c, 0f, 180f, false, topLeft = Offset(w * 0.18f, h * 0.22f), size = androidx.compose.ui.geometry.Size(w * 0.64f, h * 0.56f), style = st)
            drawLine(c, Offset(w / 2, h * 0.78f), Offset(w / 2, h * 0.96f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        },
    )
}

private fun searchContacts(ctx: android.content.Context, q: String): List<Contact> {
    val out = ArrayList<Contact>()
    try {
        ctx.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.LOOKUP_KEY, ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),
            "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} LIKE ?",
            arrayOf("%$q%"),
            "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC",
        )?.use { c ->
            while (c.moveToNext() && out.size < 5) {
                val id = c.getLong(0)
                val key = c.getString(1)
                val name = c.getString(2) ?: continue
                out += Contact(name, ContactsContract.Contacts.getLookupUri(id, key))
            }
        }
    } catch (_: Exception) {}
    return out
}
