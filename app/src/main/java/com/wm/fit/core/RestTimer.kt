package com.wm.fit.core

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.wm.fit.R
import com.wm.fit.ui.MainActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * مؤقت الراحة بين المجموعات.
 * يطلع إشعار فيه عدّاد تنازلي على شاشة القفل، ولما تخلص الراحة يهتز الجوال.
 * كل شي داخل الجوال، ما فيه أي اتصال.
 */
object RestTimer {
    private const val CH_RUN = "rest"
    private const val CH_DONE = "rest_done"
    private const val ID = 7001

    /** وقت نهاية الراحة (بالملي ثانية). 0 = ما فيه مؤقت. */
    var endAt by mutableLongStateOf(0L)
        private set

    /** طول الراحة الكامل بالثواني، للشريط. */
    var total by mutableIntStateOf(0)
        private set

    private var job: Job? = null
    private val scope = MainScope()

    val running: Boolean get() = endAt > System.currentTimeMillis()

    fun remainingSec(now: Long = System.currentTimeMillis()): Int =
        if (endAt <= now) 0 else ((endAt - now + 999) / 1000).toInt()

    fun channels(c: Context) {
        val nm = c.getSystemService(NotificationManager::class.java) ?: return
        val run = NotificationChannel(CH_RUN, "مؤقت الراحة", NotificationManager.IMPORTANCE_LOW).apply {
            description = "العدّاد التنازلي بين المجموعات"
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            setShowBadge(false)
        }
        val done = NotificationChannel(CH_DONE, "خلصت الراحة", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "تنبيه لما تخلص الراحة"
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 300, 150, 300)
            setShowBadge(false)
        }
        nm.createNotificationChannels(listOf(run, done))
    }

    fun start(c: Context, sec: Int) {
        val ctx = c.applicationContext
        total = sec
        endAt = System.currentTimeMillis() + sec * 1000L
        schedule(ctx)
    }

    /** يزيد أو ينقص الراحة. */
    fun add(c: Context, sec: Int) {
        if (!running) return
        val ctx = c.applicationContext
        endAt = (endAt + sec * 1000L).coerceAtLeast(System.currentTimeMillis() + 1000L)
        total = (total + sec).coerceAtLeast(1)
        schedule(ctx)
    }

    fun stop(c: Context) {
        job?.cancel()
        endAt = 0L
        NotificationManagerCompat.from(c.applicationContext).cancel(ID)
    }

    private fun schedule(ctx: Context) {
        job?.cancel()
        notifyRunning(ctx)
        job = scope.launch {
            delay((endAt - System.currentTimeMillis()).coerceAtLeast(0L))
            endAt = 0L
            // الإشعار يهتز لحاله. لو الإشعارات مقفلة، نهز الجوال بأنفسنا.
            if (!notifyDone(ctx)) vibrate(ctx)
        }
    }

    private fun openApp(ctx: Context): PendingIntent = PendingIntent.getActivity(
        ctx, 0,
        Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    @SuppressLint("MissingPermission")
    private fun notifyRunning(ctx: Context) {
        val nm = NotificationManagerCompat.from(ctx)
        if (!nm.areNotificationsEnabled()) return
        val n = NotificationCompat.Builder(ctx, CH_RUN)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle("راحة")
            .setContentText("المجموعة الجاية بعد العدّاد")
            .setWhen(endAt)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setTimeoutAfter((endAt - System.currentTimeMillis()).coerceAtLeast(1000L))
            .setContentIntent(openApp(ctx))
            .build()
        runCatching { nm.notify(ID, n) }
    }

    @SuppressLint("MissingPermission")
    private fun notifyDone(ctx: Context): Boolean {
        val nm = NotificationManagerCompat.from(ctx)
        if (!nm.areNotificationsEnabled()) return false
        val n = NotificationCompat.Builder(ctx, CH_DONE)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle("خلصت الراحة 💪")
            .setContentText("يلا المجموعة الجاية")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setTimeoutAfter(60_000L)
            .setContentIntent(openApp(ctx))
            .build()
        return runCatching { nm.notify(ID, n) }.isSuccess
    }

    private fun vibrate(ctx: Context) {
        runCatching {
            val v: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
                ctx.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                ctx.getSystemService(Vibrator::class.java)
            }
            v?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1))
        }
    }
}
