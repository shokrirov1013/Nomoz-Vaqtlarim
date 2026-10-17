package uz.namozvaqti.pro.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import uz.namozvaqti.pro.R
import uz.namozvaqti.pro.ServiceLocator
import uz.namozvaqti.pro.data.SOUND_BUILTIN
import uz.namozvaqti.pro.data.SOUND_DEFAULT_ALARM

/** Hozir jiringlayotgan budilnik (yo'q bo'lsa null). Alarm ekrani shuni kuzatadi. */
object RingState {
    private val _active = MutableStateFlow<RingKind?>(null)
    val active: StateFlow<RingKind?> get() = _active
    internal fun set(kind: RingKind?) { _active.value = kind }
}

object RingControl {
    fun stop(ctx: Context) {
        ctx.stopService(Intent(ctx, RingService::class.java))
        Notifications.cancel(ctx, Notifications.ID_RING)
    }

    fun snooze(ctx: Context, kind: RingKind) {
        val minutes = ServiceLocator.get(ctx).settings.value.snoozeMinutes
        Scheduler.snooze(ctx, kind, minutes)
        stop(ctx)
    }
}

/** Budilnik: takrorlanuvchi ovoz + vibratsiya; ekran qulflangan bo'lsa ham to'liq ekran oynasi. */
class RingService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var wake: PowerManager.WakeLock? = null
    private var kind = RingKind.FAJR

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        kind = RingKind.fromKey(intent?.getStringExtra(Extra.KIND))
        val s = ServiceLocator.get(this).settings.value

        val stop = PendingIntentFactory.broadcast(this, 1, Act.STOP_RING, kind)
        val snooze = PendingIntentFactory.broadcast(this, 2, Act.SNOOZE_RING, kind)
        val full = PendingIntentFactory.alarmScreen(this, kind)
        val n = NotificationCompat.Builder(this, Notifications.CH_ALARM)
            .setSmallIcon(R.drawable.ic_stat_namoz)
            .setContentTitle(kind.title)
            .setContentText(if (kind == RingKind.FAJR) "Bomdod vaqti bo'ldi. Namozga turing." else "Tahajjud vaqti bo'ldi.")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setFullScreenIntent(full, true)
            .setContentIntent(full)
            .addAction(0, "To'xtatish", stop)
            .addAction(0, "Keyinga surish (${s.snoozeMinutes} daq.)", snooze)
            .build()
        val type = if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        ServiceCompat.startForeground(this, Notifications.ID_RING, n, type)

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wake = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "namoz:ring").apply { acquire(5 * 60_000L) }

        startSound(if (kind == RingKind.FAJR) s.fajrSound else s.tahajjudSound)
        if (if (kind == RingKind.FAJR) s.fajrVibration else s.tahajjudVibration) startVibration()
        RingState.set(kind)
        // Hech kim to'xtatmasa, 3 daqiqadan keyin o'zi to'xtaydi.
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ stopSelf() }, 3 * 60_000L)
        return START_NOT_STICKY
    }

    private fun startSound(sound: String) {
        player?.release()
        player = null
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        fun build(source: String): MediaPlayer? = try {
            MediaPlayer().apply {
                setAudioAttributes(attrs)
                when (source) {
                    SOUND_BUILTIN -> resources.openRawResourceFd(R.raw.ting).use {
                        setDataSource(it.fileDescriptor, it.startOffset, it.length)
                    }
                    SOUND_DEFAULT_ALARM -> setDataSource(this@RingService, defaultAlarmUri())
                    else -> setDataSource(this@RingService, Uri.parse(source))
                }
                isLooping = true
                prepare()
            }
        } catch (e: Exception) {
            Log.w(TAG, "ovoz ochilmadi ($source): ${e.message}")
            null
        }
        val mp = build(sound) ?: build(SOUND_BUILTIN)
        player = mp
        mp?.start()
    }

    private fun startVibration() {
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 700, 500), 0)
        val v: Vibrator? = if (Build.VERSION.SDK_INT >= 31)
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        else @Suppress("DEPRECATION") (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
        v?.vibrate(effect)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        player?.runCatching { stop() }
        player?.release(); player = null
        val v: Vibrator? = if (Build.VERSION.SDK_INT >= 31)
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        else @Suppress("DEPRECATION") (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
        v?.cancel()
        wake?.takeIf { it.isHeld }?.release()
        RingState.set(null)
        super.onDestroy()
    }

    companion object {
        private const val TAG = "RingService"

        fun defaultAlarmUri(): Uri =
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        fun start(ctx: Context, kind: RingKind) {
            try {
                ContextCompat.startForegroundService(
                    ctx, Intent(ctx, RingService::class.java).putExtra(Extra.KIND, kind.key),
                )
            } catch (e: Exception) {
                Log.w(TAG, "budilnik xizmatini boshlab bo'lmadi: ${e.message}")
                Notifications.postRingFallback(ctx, kind)
            }
        }
    }
}

internal object PendingIntentFactory {
    fun broadcast(ctx: Context, code: Int, action: String, kind: RingKind): android.app.PendingIntent =
        android.app.PendingIntent.getBroadcast(
            ctx, code + kind.ordinal * 10,
            Intent(ctx, ActionReceiver::class.java).setAction(action).putExtra(Extra.KIND, kind.key),
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT,
        )

    fun alarmScreen(ctx: Context, kind: RingKind): android.app.PendingIntent =
        android.app.PendingIntent.getActivity(
            ctx, 50 + kind.ordinal,
            Intent(ctx, AlarmActivity::class.java).putExtra(Extra.KIND, kind.key)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT,
        )
}
