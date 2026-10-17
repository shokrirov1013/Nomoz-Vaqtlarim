package uz.namozvaqti.pro.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.SystemClock
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import uz.namozvaqti.pro.R
import uz.namozvaqti.pro.ServiceLocator
import java.util.Locale

/**
 * Eslatma ovozi: avval foydalanuvchi bergan "ting" mp3, keyin o'zbekcha ovozli xabar (TTS).
 * Foreground xizmat — ekran o'chiq va Doze rejimida ham to'liq ijro etiladi.
 */
class SpeechService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private class Item(val text: String, val clip: String?)
    private val queue = ArrayDeque<Item>()
    private var busy = false
    private var itemId = 0

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var voiceOk = false
    private var pendingSpeak: String? = null
    private var player: MediaPlayer? = null
    private var focus: AudioFocusRequest? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(applicationContext) { status ->
            handler.post {
                ttsReady = true
                if (status == TextToSpeech.SUCCESS) setupVoice()
                pendingSpeak?.let { pendingSpeak = null; doSpeak(it) }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val n = NotificationCompat.Builder(this, Notifications.CH_PLAYBACK)
            .setSmallIcon(R.drawable.ic_stat_namoz)
            .setContentTitle(UzText.of(this, "Namoz eslatmasi"))
            .setContentText(UzText.of(this, "Ovozli xabar ijro etilmoqda"))
            .setOngoing(true)
            .setSilent(true)
            .build()
        val type = if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        ServiceCompat.startForeground(this, Notifications.ID_PLAYBACK, n, type)

        // Bo'sh matn = faqat "ting" (ovozli xabar o'chirilgan).
        intent?.getStringExtra(Extra.TEXT)?.let { queue.addLast(Item(it, intent.getStringExtra(Extra.CLIP))) }
        if (!busy) playNext()
        return START_NOT_STICKY
    }

    private fun attrs(speech: Boolean): AudioAttributes {
        val silentOverride = ServiceLocator.get(this).settings.value.playInSilentMode
        return AudioAttributes.Builder()
            .setUsage(if (silentOverride) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .setContentType(if (speech) AudioAttributes.CONTENT_TYPE_SPEECH else AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    }

    private fun playNext() {
        val item = queue.removeFirstOrNull()
        if (item == null) {
            finishAll()
            return
        }
        val text = item.text
        busy = true
        val id = ++itemId
        requestFocus()
        // Qotib qolishdan himoya: bir xabar uchun eng ko'pi 45 soniya.
        handler.postDelayed({ if (id == itemId && busy) itemDone(id) }, 45_000)
        playChime {
            if (id != itemId) return@playChime
            when {
                text.isEmpty() -> itemDone(id)
                !playClip(item.clip, id) -> speak(text, id)
            }
        }
    }

    /**
     * "Ting" ovozi. Muloyim rejimda: ovoz 0 dan asta ko'tariladi, ~4,5 soniyada yumshoq so'nadi va keyin nutq boshlanadi.
     * Balandlik Sozlamalardagi "Eslatma ovozi balandligi"ga bog'liq.
     */
    private fun playChime(onDone: () -> Unit) {
        val st = ServiceLocator.get(this).settings.value
        val target = (st.reminderVolume / 100f).coerceIn(0.1f, 1f)
        val limitMs = if (st.gentle) 4500 else Int.MAX_VALUE
        try {
            val mp = MediaPlayer()
            player = mp
            mp.setAudioAttributes(attrs(false))
            resources.openRawResourceFd(R.raw.ting).use { mp.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            mp.prepare()
            mp.setVolume(0f, 0f)
            var finished = false
            val started = SystemClock.uptimeMillis()
            val durationMs = minOf(mp.duration, limitMs)
            fun finish() {
                if (finished) return
                finished = true
                runCatching { mp.stop() }
                mp.release()
                if (player === mp) player = null
                onDone()
            }
            val ramp = object : Runnable {
                override fun run() {
                    if (finished) return
                    val t = SystemClock.uptimeMillis() - started
                    val fadeIn = (t / 700f).coerceAtMost(1f)
                    val fadeOut = ((durationMs - t) / 1200f).coerceIn(0f, 1f)
                    val v = target * fadeIn * fadeOut
                    mp.setVolume(v, v)
                    if (t >= durationMs) finish() else handler.postDelayed(this, 60)
                }
            }
            mp.setOnCompletionListener { finish() }
            mp.setOnErrorListener { _, _, _ -> finish(); true }
            mp.start()
            handler.post(ramp)
        } catch (e: Exception) {
            Log.w(TAG, "ting ijro bo'lmadi", e)
            player?.release(); player = null
            onDone()
        }
    }

    /**
     * Tayyor ovoz paketidan (res/raw/v_<til>_<kalit>.mp3) ijro etadi. Paket yoki fayl bo'lmasa false
     * qaytaradi — shunda telefon ovozi (TTS) ishlatiladi.
     */
    private fun playClip(clip: String?, id: Int): Boolean {
        if (clip == null) return false
        val st = ServiceLocator.get(this).settings.value
        if (st.voiceSource != "uz" && st.voiceSource != "ru") return false
        val res = resources.getIdentifier("v_${st.voiceSource}_$clip", "raw", packageName)
        if (res == 0) return false
        return try {
            val mp = MediaPlayer()
            player = mp
            mp.setAudioAttributes(attrs(true))
            resources.openRawResourceFd(res).use { mp.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            mp.prepare()
            val v = (st.reminderVolume / 100f).coerceIn(0.1f, 1f)
            mp.setVolume(v, v)
            mp.setOnCompletionListener { p -> p.release(); if (player === p) player = null; handler.post { itemDone(id) } }
            mp.setOnErrorListener { p, _, _ -> p.release(); if (player === p) player = null; handler.post { itemDone(id) }; true }
            mp.start()
            true
        } catch (e: Exception) {
            Log.w(TAG, "ovoz paketi ijro bo'lmadi: ${e.message}")
            player?.release(); player = null
            false
        }
    }

    private fun speak(text: String, id: Int) {
        if (!ttsReady) {
            pendingSpeak = text
            return
        }
        doSpeak(text)
    }

    private fun doSpeak(text: String) {
        val engine = tts
        if (engine == null || !voiceOk) {
            itemDone(itemId)
            return
        }
        val id = itemId
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { handler.post { itemDone(id) } }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { handler.post { itemDone(id) } }
        })
        engine.setAudioAttributes(attrs(true))
        val st = ServiceLocator.get(this).settings.value
        engine.setSpeechRate(if (st.gentle) 0.85f else 0.92f)
        engine.setPitch(if (st.gentle) 0.95f else 1.0f)
        val params = android.os.Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, (st.reminderVolume / 100f).coerceIn(0.1f, 1f))
        }
        val r = engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, "u$id")
        if (r != TextToSpeech.SUCCESS) itemDone(id)
    }

    private fun itemDone(id: Int) {
        if (id != itemId || !busy) return
        busy = false
        handler.removeCallbacksAndMessages(null)
        playNext()
    }

    /** O'zbek ovozi; bo'lmasa sozlamadagi muqobil (turk tili o'zbek lotin imlosiga eng yaqin), keyin rus, ingliz. */
    private fun setupVoice() {
        val engine = tts ?: return
        val pref = ServiceLocator.get(this).settings.value.ttsLang
        val order = buildList {
            if (pref != "auto") add(pref)
            addAll(listOf("uz", "tr", "ru", "en"))
        }.distinct()
        for (code in order) {
            val locale = if (code == "uz") Locale("uz", "UZ") else Locale(code)
            val r = engine.isLanguageAvailable(locale)
            if (r >= TextToSpeech.LANG_AVAILABLE) {
                if (engine.setLanguage(locale) >= TextToSpeech.LANG_AVAILABLE) {
                    engine.setSpeechRate(0.92f)
                    engine.setPitch(1.0f)
                    voiceOk = true
                    Log.i(TAG, "TTS tili: $code")
                    return
                }
            }
        }
        voiceOk = false
        Log.w(TAG, "mos TTS ovozi topilmadi")
    }

    private fun requestFocus() {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attrs(true))
            .setOnAudioFocusChangeListener { }
            .build()
        focus = req
        am.requestAudioFocus(req)
    }

    private fun finishAll() {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        focus?.let { am.abandonAudioFocusRequest(it) }
        focus = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        player?.release(); player = null
        tts?.stop(); tts?.shutdown(); tts = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "SpeechService"

        /** Ovozli xabarni navbatga qo'yadi. Foreground xizmat boshlanmasa, bildirishnoma o'zi qoladi. */
        /** Doim "ting" chaladi; [withVoice] = false bo'lsa, nutq (ovozli xabar) o'qilmaydi. */
        fun speak(ctx: Context, text: String, withVoice: Boolean = true, clip: String? = null): Boolean =
            try {
                ContextCompat.startForegroundService(
                    ctx,
                    Intent(ctx, SpeechService::class.java).putExtra(Extra.TEXT, if (withVoice) text else "").putExtra(Extra.CLIP, clip),
                )
                true
            } catch (e: Exception) {
                Log.w(TAG, "xizmatni boshlab bo'lmadi: ${e.message}")
                false
            }
    }
}
