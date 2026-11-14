package com.wm.fit.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.wm.fit.core.Backup
import com.wm.fit.core.P
import com.wm.fit.core.Secrets
import com.wm.fit.core.Updater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
private fun ToggleRow(text: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(on, onChange)
    }
}

/** قفل التطبيق بالبصمة. */
@Composable
fun SecurityCard() {
    val ctx = LocalContext.current
    Card(radius = 22) {
        ToggleRow("قفل التطبيق بالبصمة", P.appLock) { want ->
            if (want && !AppLock.available(ctx)) {
                Toast.makeText(ctx, "فعّل البصمة أو قفل الشاشة في جوالك أول.", Toast.LENGTH_LONG).show()
            } else {
                P.appLock = want
            }
        }
        Text(
            "يقفل أول ما تفتح التطبيق، ولو طلعت منه أكثر من دقيقة.",
            style = MaterialTheme.typography.labelSmall,
            color = C.muted,
        )
    }
}

/** المحادثة الذكية: مقفلة افتراضياً، وكل مستخدم يحط مفتاحه. */
@Composable
fun SmartChatCard(nav: Nav) {
    var warn by remember { mutableStateOf(false) }
    var keyDialog by remember { mutableStateOf(false) }
    var hasKey by remember { mutableStateOf(Secrets.hasKey()) }

    Card(radius = 22) {
        ToggleRow("المحادثة الذكية مع Claude", P.smartChat) { want -> if (want) warn = true else P.smartChat = false }
        Text(
            "مقفلة افتراضياً. لما تفعّلها، اللي تكتبه في المحادثة بس ينرسل لـ Anthropic بمفتاحك أنت.",
            style = MaterialTheme.typography.labelSmall,
            color = C.muted,
        )
        if (P.smartChat) {
            Text(
                if (hasKey) "المفتاح محفوظ ومشفّر ✓" else "ما فيه مفتاح للحين",
                style = MaterialTheme.typography.bodySmall,
                color = if (hasKey) C.accent else C.warnText,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlineButton(if (hasKey) "غيّر المفتاح" else "حط مفتاحك", Modifier.weight(1f), C.accent) { keyDialog = true }
                if (hasKey) OutlineButton("احذف المفتاح", Modifier.weight(1f), C.bad) { Secrets.clear(); hasKey = false }
            }
            OutlineButton("افتح المدرب", Modifier.fillMaxWidth()) { nav.push(Over.Coach) }
        }
    }

    if (warn) {
        AlertDialog(
            onDismissRequest = { warn = false },
            containerColor = C.card,
            title = { Text("قبل ما تفعّلها") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1. اللي تكتبه في المحادثة ينرسل لشركة Anthropic عشان Claude يرد.", style = MaterialTheme.typography.bodySmall)
                    Text("2. ملخص خطتك ما ينرسل إلا لو أنت فعّلت المشاركة، وتشوفه قبل.", style = MaterialTheme.typography.bodySmall)
                    Text("3. Anthropic ما تدرّب على بيانات الـ API افتراضياً، وتحذفها بعد فترة (تقريباً 30 يوم). تأكد من سياستها في موقعها.", style = MaterialTheme.typography.bodySmall)
                    Text("4. المفتاح يدفع من حسابك أنت في Anthropic.", style = MaterialTheme.typography.bodySmall)
                    Text("5. Claude مو طبيب.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = { P.smartChat = true; warn = false }) { Text("فهمت، فعّلها", color = C.accent) } },
            dismissButton = { TextButton(onClick = { warn = false }) { Text("لا", color = C.muted) } },
        )
    }

    if (keyDialog) {
        var k by remember { mutableStateOf("") }
        val scope = rememberCoroutineScope()
        var err by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { keyDialog = false },
            containerColor = C.card,
            title = { Text("مفتاح Claude") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "تطلعه من Claude Console (platform.claude.com) من قسم API Keys. " +
                            "لا ترسله لأحد، ولا تحطه في أي مكان غير هنا.",
                        style = MaterialTheme.typography.bodySmall,
                        color = C.muted,
                    )
                    Field(k, "sk-ant-...", Modifier.fillMaxWidth()) { k = it.trim().take(300) }
                    err?.let { Text(it, color = C.bad, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (!k.startsWith("sk-ant-")) { err = "المفتاح يبدأ بـ sk-ant-"; return@TextButton }
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) { Secrets.setKey(k) }
                        if (ok) { hasKey = true; keyDialog = false } else err = "ما قدرت أحفظه. جرّب مرة ثانية."
                    }
                }) { Text("احفظ", color = C.accent) }
            },
            dismissButton = { TextButton(onClick = { keyDialog = false }) { Text("إلغاء", color = C.muted) } },
        )
    }
}

/** النسخة الاحتياطية: ملف مقفل بكلمة سر، تحفظه أنت. */
@Composable
fun BackupCard(onRestored: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var exportPw by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<ByteArray?>(null) }
    var importUri by remember { mutableStateOf<Uri?>(null) }
    var busy by remember { mutableStateOf(false) }

    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val data = pending
        pending = null
        if (uri != null && data != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching { ctx.contentResolver.openOutputStream(uri)?.use { it.write(data) } != null }.getOrDefault(false)
                }
                Toast.makeText(ctx, if (ok) "انحفظت النسخة ✓" else "ما قدرت أحفظ الملف.", Toast.LENGTH_LONG).show()
            }
        }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> importUri = uri }

    Card(radius = 22) {
        Text("النسخة الاحتياطية", style = MaterialTheme.typography.titleMedium)
        Text(
            "ملف تحفظه أنت (في جوالك أو أي مكان)، مقفل بكلمة سر. بدون كلمة السر ما ينفتح، " +
                "ولو نسيتها ما نقدر نرجعه. مفتاح Claude ما يدخل في النسخة.",
            style = MaterialTheme.typography.labelSmall,
            color = C.muted,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlineButton(if (busy) "لحظة..." else "احفظ نسخة", Modifier.weight(1f), C.accent) { if (!busy) exportPw = true }
            OutlineButton("استرجع نسخة", Modifier.weight(1f)) { if (!busy) open.launch(arrayOf("*/*")) }
        }
    }

    if (exportPw) {
        var pw by remember { mutableStateOf("") }
        var pw2 by remember { mutableStateOf("") }
        var photos by remember { mutableStateOf(true) }
        val ok = pw.length >= Backup.MIN_PASSWORD && pw == pw2
        AlertDialog(
            onDismissRequest = { exportPw = false },
            containerColor = C.card,
            title = { Text("كلمة سر للنسخة") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PasswordField(pw, "كلمة السر (${Backup.MIN_PASSWORD} حروف أو أكثر)") { pw = it }
                    PasswordField(pw2, "أعد كلمة السر") { pw2 = it }
                    ToggleRow("مع صور التقدم", photos) { photos = it }
                    if (pw2.isNotEmpty() && pw != pw2) Text("الكلمتين مو نفس بعض", color = C.bad, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(enabled = ok, onClick = {
                    exportPw = false
                    busy = true
                    scope.launch {
                        pending = withContext(Dispatchers.Default) { Backup.export(pw.toCharArray(), photos) }
                        busy = false
                        create.launch(Backup.fileName())
                    }
                }) { Text("كمّل", color = if (ok) C.accent else C.muted) }
            },
            dismissButton = { TextButton(onClick = { exportPw = false }) { Text("إلغاء", color = C.muted) } },
        )
    }

    importUri?.let { uri ->
        var pw by remember { mutableStateOf("") }
        var err by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { importUri = null },
            containerColor = C.card,
            title = { Text("استرجاع النسخة") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Note("بياناتك الحالية بتنمسح، وتنحط مكانها اللي في الملف.", warn = true)
                    PasswordField(pw, "كلمة السر") { pw = it }
                    err?.let { Text(it, color = C.bad, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    busy = true
                    scope.launch {
                        val r = withContext(Dispatchers.IO) {
                            val data = runCatching { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
                            if (data == null) Backup.Result.Err("ما قدرت أقرأ الملف.") else Backup.import(data, pw.toCharArray())
                        }
                        busy = false
                        when (r) {
                            is Backup.Result.Ok -> { importUri = null; onRestored() }
                            is Backup.Result.Err -> err = r.text
                        }
                    }
                }) { Text("استرجع", color = C.accent) }
            },
            dismissButton = { TextButton(onClick = { importUri = null }) { Text("إلغاء", color = C.muted) } },
        )
    }
}

@Composable
private fun PasswordField(value: String, label: String, onChange: (String) -> Unit) {
    // نعرض نقاط بدل الحروف
    androidx.compose.foundation.layout.Column(
        Modifier.fillMaxWidth(),
    ) {
        androidx.compose.material3.OutlinedTextField(
            value = value,
            onValueChange = { onChange(it.take(64)) },
            label = { Text(label) },
            singleLine = true,
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
            ),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = C.accent,
                unfocusedBorderColor = C.line,
                focusedLabelColor = C.accent,
                unfocusedLabelColor = C.muted,
                cursorColor = C.accent,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** التحديث من داخل التطبيق. */
@Composable
fun UpdateCard() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }
    var found by remember { mutableStateOf<Updater.Release?>(null) }
    var progress by remember { mutableStateOf<Float?>(null) }

    Card(radius = 22) {
        Text("التحديث", style = MaterialTheme.typography.titleMedium)
        Text("نسختك: 1.${Updater.current}", style = MaterialTheme.typography.bodySmall, color = C.muted)
        status?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = C.soft) }
        progress?.let { Bar(it) }
        val r = found
        if (r == null) {
            OutlineButton("دوّر على تحديث", Modifier.fillMaxWidth()) {
                status = "أدوّر..."
                scope.launch {
                    status = when (val c = withContext(Dispatchers.IO) { Updater.check() }) {
                        is Updater.Check.Available -> { found = c.r; "فيه تحديث جديد: ${c.r.name}" }
                        Updater.Check.UpToDate -> "عندك آخر نسخة ✓"
                        Updater.Check.NoRelease -> "ما فيه إصدار رسمي للحين."
                        is Updater.Check.Err -> c.text
                    }
                }
            }
        } else {
            BigButton(if (progress != null) "ينزل..." else "نزّل وركّب", enabled = progress == null) {
                if (!Updater.canInstall(ctx)) {
                    status = "اسمح للتطبيق يركّب تحديثات، بعدين ارجع واضغط مرة ثانية."
                    Updater.openInstallSetting(ctx)
                    return@BigButton
                }
                progress = 0f
                scope.launch {
                    val f = withContext(Dispatchers.IO) { Updater.download(ctx, r) { p -> progress = p } }
                    progress = null
                    if (f != null) Updater.install(ctx, f) else status = "ما قدرت أنزّل الملف. جرّب مرة ثانية."
                }
            }
        }
        Text(
            "ما يتصل بالنت إلا لما تضغط. والتحديث يركب فوق القديم بس لما يكون مفتاح التوقيع ثابت.",
            style = MaterialTheme.typography.labelSmall,
            color = C.muted,
        )
    }
}
