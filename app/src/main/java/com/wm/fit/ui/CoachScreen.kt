package com.wm.fit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.wm.fit.core.ClaudeChat
import com.wm.fit.core.Coach
import com.wm.fit.core.P
import com.wm.fit.core.Secrets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** المحادثة تبقى في الذاكرة بس، تنمسح لما يتقفل التطبيق. */
private object ChatMemory {
    val msgs = mutableStateListOf<ClaudeChat.Msg>()
}

/** المدرب: اقتراحاته (داخل الجوال) + المحادثة الذكية (اختيارية). */
@Composable
fun CoachScreen(nav: Nav, onBack: () -> Unit) {
    val tips = Coach.tips()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Header("المدرب", sub = "يقترح، والقرار لك", onBack = onBack)

        SectionLabel("اقتراحات (من بياناتك، داخل جوالك)")
        if (!P.coachSuggests) {
            Note("الاقتراحات مطفية. تقدر تشغّلها من الإعدادات.")
        } else if (tips.isEmpty()) {
            Note("ما عندي اقتراحات الحين. كمّل وسجّل أوزانك، وأنا أتابع التوازن والتطور.")
        }
        tips.forEach { t -> TipCard(t, nav) }

        SectionLabel("المحادثة الذكية مع Claude")
        when {
            !P.smartChat -> Note(
                "مقفلة. لو تبيها، فعّلها من الإعدادات وحط مفتاحك. " +
                    "اللي تكتبه بس ينرسل لـ Anthropic، وما ينرسل شي غيره إلا لو أنت اخترت.",
            )
            !Secrets.hasKey() -> Note("المحادثة مفعّلة بس ما فيه مفتاح. حطه من الإعدادات.", warn = true)
            else -> Chat()
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun TipCard(t: Coach.Tip, nav: Nav) {
    Card(radius = 20) {
        Text(t.title, style = MaterialTheme.typography.titleMedium)
        Text(t.why, style = MaterialTheme.typography.bodySmall, color = C.soft)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (t.canFocus) {
                OutlineButton("أضفها للتركيز", Modifier.weight(1f), C.accent) { Coach.accept(t) }
            } else {
                OutlineButton("خريطة الإصابات", Modifier.weight(1f), C.accent) { nav.injuries() }
            }
            OutlineButton("مو الحين", Modifier.weight(1f)) { Coach.dismiss(t) }
        }
    }
}

@Composable
private fun Chat() {
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var share by remember { mutableStateOf(false) }
    var showCtx by remember { mutableStateOf(false) }
    val msgs = ChatMemory.msgs

    Note("Claude مو طبيب. لو عندك ألم قوي أو مرض، راجع طبيب.")

    // مشاركة ملخص الخطة: مطفية افتراضياً، ويشوف بالضبط وش ينرسل
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(C.card)
            .clickable { share = !share }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("شارك ملخص خطتي مع Claude", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(share) { share = it }
    }
    if (share) {
        Text(
            if (showCtx) "إخفاء" else "شوف وش ينرسل بالضبط",
            style = MaterialTheme.typography.bodySmall,
            color = C.accent,
            modifier = Modifier.clickable { showCtx = !showCtx }.padding(4.dp),
        )
        if (showCtx) Note(ClaudeChat.contextSummary())
    }

    msgs.forEach { m ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.user) Arrangement.Start else Arrangement.End) {
            Text(
                m.text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (m.user) C.onAccent else C.text,
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (m.user) C.accent else C.card2)
                    .padding(12.dp),
            )
        }
    }
    if (busy) Text("Claude يكتب...", style = MaterialTheme.typography.bodySmall, color = C.muted)
    error?.let { Note(it, warn = true) }

    Field(input, "اسأل مدربك...", Modifier.fillMaxWidth()) { input = it.take(2000) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BigButton("أرسل", Modifier.weight(1f), enabled = input.isNotBlank() && !busy) {
            msgs += ClaudeChat.Msg(true, input.trim())
            input = ""
            busy = true
            error = null
            val history = msgs.toList()
            val ctx = if (share) ClaudeChat.contextSummary() else null
            scope.launch {
                val r = withContext(Dispatchers.IO) { ClaudeChat.send(history, ctx) }
                when (r) {
                    is ClaudeChat.Result.Ok -> msgs += ClaudeChat.Msg(false, r.text)
                    is ClaudeChat.Result.Err -> {
                        // نشيل سؤاله عشان المحادثة تبقى بالترتيب (سؤال، جواب)
                        if (msgs.lastOrNull()?.user == true) input = msgs.removeAt(msgs.lastIndex).text
                        error = r.text
                    }
                }
                busy = false
            }
        }
        if (msgs.isNotEmpty()) OutlineButton("امسح") { msgs.clear(); error = null }
    }
    Text(
        "النموذج: ${ClaudeChat.MODEL}. المحادثة ما تنحفظ، تنمسح لما تقفل التطبيق.",
        style = MaterialTheme.typography.labelSmall,
        color = C.muted,
    )
}
