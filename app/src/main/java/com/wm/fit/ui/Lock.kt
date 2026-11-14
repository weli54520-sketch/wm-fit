package com.wm.fit.ui

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.wm.fit.core.P

/**
 * قفل التطبيق بالبصمة (أو رمز قفل الشاشة لو البصمة ما اشتغلت).
 * يقفل أول ما يفتح، ولو طلع منه أكثر من دقيقة.
 */
object AppLock {
    /** البصمة أو الوجه، أو رمز الجوال كبديل. يشتغل على كل الأجهزة من أندرويد 8. */
    private const val AUTH = BIOMETRIC_WEAK or DEVICE_CREDENTIAL
    private const val GRACE_MS = 60_000L

    var locked by mutableStateOf(false)
        private set
    private var stoppedAt = 0L

    fun onStart() {
        if (P.appLock && (stoppedAt == 0L || System.currentTimeMillis() - stoppedAt > GRACE_MS)) locked = true
    }

    fun onStop() {
        stoppedAt = System.currentTimeMillis()
    }

    /** الجوال فيه بصمة أو قفل شاشة؟ */
    fun available(ctx: Context): Boolean =
        BiometricManager.from(ctx).canAuthenticate(AUTH) == BiometricManager.BIOMETRIC_SUCCESS

    fun prompt(activity: FragmentActivity, onOk: () -> Unit = {}) {
        val cb = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                locked = false
                onOk()
            }
        }
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("افتح WM Fit")
            .setSubtitle("بالبصمة أو قفل الشاشة")
            .setAllowedAuthenticators(AUTH)
            .build()
        runCatching { BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), cb).authenticate(info) }
    }
}

@Composable
fun LockScreen() {
    val activity = LocalContext.current as? FragmentActivity
    LaunchedEffect(Unit) { activity?.let { AppLock.prompt(it) } }
    Column(
        Modifier
            .fillMaxSize()
            .background(C.bg)
            .systemBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text("🔒", style = MaterialTheme.typography.displayLarge)
        Text("WM Fit مقفل", style = MaterialTheme.typography.headlineMedium)
        Text(
            "افتحه بالبصمة أو قفل الشاشة.",
            style = MaterialTheme.typography.bodyMedium,
            color = C.muted,
            textAlign = TextAlign.Center,
        )
        BigButton("افتح") { activity?.let { AppLock.prompt(it) } }
    }
}
