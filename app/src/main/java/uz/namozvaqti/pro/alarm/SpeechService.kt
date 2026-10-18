package uz.namozvaqti.pro.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
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
 * Eslatma ovozi: avval foydalanuvchi bergan "ting" mp3, keyin ovozli xabar (tayyor ovoz paketi yoki telefon ovozi).
 * Foreground xizmat — ekran o'chiq va Doze rejimida ham to'liq ijro etiladi.
 *
 * Ovoz HAR QANDAY holatda eshitilishi kerak (jim rejim, ovoz 0, naushnik):
 *  - naushnik/Bluetooth ulangan bo'lsa MEDIA oqimi ishlatiladi (ALARM/NOTIFICATION oqimi naushnikka yo'nalmasdan jim qolardi),
 *    aks holda ALARM oqimi (jim rejimda ham chaladi);
 *  - tanlangan oqim ovozi 0 bo'lsa vaqtincha ko'tariladi va tugagach qaytariladi;
 *  - audio fokus qisqa muddatga olinadi: musiqa to'xtab, eslatmadan keyin o'zi davom etadi.
 */
class SpeechService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private class Item(val text: String, val clip: String?, val preName: String)
    private val queue = ArrayDeque<Item>()
    private var busy = false
    private var itemId = 0

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var voiceOk = false
    private var pendingSpeak: Pair<String, () -> Unit>? = null
    private var player: MediaPlayer? = null
    private var focus: AudioFocusRequest? = null

    private var headset = false
    private var savedVolume: Pair<Int, Int>? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(applicationContext) { status ->
            handler.post {
                ttsReady = true
                if (status == TextToSpeech.SUCCESS) setupVoice()
                pendingSpeak?.let { (text, done) -> pendingSpeak = null; speakText(text, done) }
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
        intent?.getStringExtra(Extra.TEXT)?.let {
            queue.addLast(Item(it, intent.getStringExtra(Extra.CLIP), intent.getStringExtra(Extra.NAME).orEmpty()))
        }
        if (!busy) playNext()
        return START_NOT_STICKY
    }

    // ------------------------------------------------------------------ audio holati

    /** Simli/Bluetooth/USB naushnik yoki eshitish apparati ulanganmi (qo'ng'iroq uchun SCO hisoblanmaydi). */
    private fun headsetConnected(): Boolean {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return try {
            am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { d ->
                when (d.type) {
                    AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_USB_HEADSET -> true
                    else -> Build.VERSION.SDK_INT >= 31 && d.type in 26..27 // BLE headset / BLE speaker
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun stream() = if (headset) AudioManager.STREAM_MUSIC else AudioManager.STREAM_ALARM

    private fun attrs(speech: Boolean): AudioAttributes =
        AudioAttributes.Builder()
            .setUsage(if (headset) AudioAttributes.USAGE_MEDIA else AudioAttributes.USAGE_ALARM)
            .setContentType(if (speech) AudioAttributes.CONTENT_TYPE_SPEECH else AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

    /** Oqim ovozi butunlay o'chiq (0) bo'lsa eslatma jim qolmasligi uchun vaqtincha o'rtacha darajaga ko'taradi. */
    private fun ensureAudible() {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val st = stream()
        try {
            val cur = am.getStreamVolume(st)
            if (cur == 0) {
                val max = am.getStreamMaxVolume(st)
                if (savedVolume == null) savedVolume = st to cur
                am.setStreamVolume(st, maxOf(1, (max * 0.4f).toInt()), 0)
            }
        } catch (e: Exception) {
            Log.w(TAG, "ovozni ko'tarib bo'lmadi: ${e.message}")
        }
    }

    private fun restoreVolume() {
        val saved = savedVolume ?: return
        savedVolume = null
        try {
            (getSystemService(Context.AUDIO_SERVICE) as AudioManager).setStreamVolume(saved.first, saved.second, 0)
        } catch (_: Exception) {
        }
    }

    private fun requestFocus() {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        focus?.let { am.abandonAudioFocusRequest(it) }
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attrs(true))
            .setOnAudioFocusChangeListener { }
            .build()
        focus = req
        am.requestAudioFocus(req)
    }

    // ------------------------------------------------------------------ navbat

    private fun playNext() {
        val item = queue.removeFirstOrNull()
        if (item == null) {
            finishAll()
            return
        }
        busy = true
        val id = ++itemId
        headset = headsetConnected()
        ensureAudible()
        requestFocus()
        // Qotib qolishdan himoya: bir xabar uchun eng ko'pi 45 soniya.
        handler.postDelayed({ if (id == itemId && busy) itemDone(id) }, 45_000)
        playChime {
            if (id != itemId) return@playChime
            if (item.text.isEmpty()) itemDone(id) else playVoice(item, id)
        }
    }

    /** Ovozli xabar: tayyor klip bo'lsa (oldidan ism TTS bilan), bo'lmasa to'liq matn telefon ovozi bilan. */
    private fun playVoice(item: Item, id: Int) {
        val res = clipResource(item.clip)
        if (res == 0) {
            speakText(item.text) { itemDone(id) }
            return
        }
        val clip = { if (id == itemId && !playClip(res, id)) speakText(item.text) { itemDone(id) } }
        if (item.preName.isNotBlank()) speakText(item.preName) { clip() } else clip()
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

    /** Tayyor ovoz paketi fayli (res/raw/v_<til>_<kalit>.mp3) resursi; paket bo'lmasa yoki telefon ovozi tanlangan bo'lsa 0. */
    private fun clipResource(clip: String?): Int {
        if (clip == null) return 0
        val st = ServiceLocator.get(this).settings.value
        if (st.voiceSource != "uz" && st.voiceSource != "ru") return 0
        return resources.getIdentifier("v_${st.voiceSource}_$clip", "raw", packageName)
    }

    private fun playClip(res: Int, id: Int): Boolean {
        val st = ServiceLocator.get(this).settings.value
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

    // ------------------------------------------------------------------ telefon ovozi (TTS)

    private fun speakText(text: String, whenDone: () -> Unit) {
        if (!ttsReady) {
            pendingSpeak = text to whenDone
            return
        }
        val engine = tts
        if (engine == null || !voiceOk) {
            whenDone()
            return
        }
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { handler.post { whenDone() } }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { handler.post { whenDone() } }
        })
        engine.setAudioAttributes(attrs(true))
        val st = ServiceLocator.get(this).settings.value
        engine.setSpeechRate(if (st.gentle) 0.85f else 0.92f)
        engine.setPitch(if (st.gentle) 0.95f else 1.0f)
        val params = android.os.Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, (st.reminderVolume / 100f).coerceIn(0.1f, 1f))
        }
        val r = engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, "u${itemId}_${SystemClock.uptimeMillis()}")
        if (r != TextToSpeech.SUCCESS) whenDone()
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

    private fun finishAll() {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        focus?.let { am.abandonAudioFocusRequest(it) }
        focus = null
        restoreVolume()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        player?.release(); player = null
        tts?.stop(); tts?.shutdown(); tts = null
        restoreVolume()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "SpeechService"

        /**
         * Ovozli xabarni navbatga qo'yadi. Doim "ting" chaladi; [withVoice] = false bo'lsa nutq o'qilmaydi.
         * [preName] bo'sh bo'lmasa, tayyor klipdan oldin shu ism telefon ovozi bilan o'qiladi (Juma tabrigi).
         * Foreground xizmat boshlanmasa, bildirishnoma o'zi qoladi.
         */
        fun speak(ctx: Context, text: String, withVoice: Boolean = true, clip: String? = null, preName: String = ""): Boolean =
            try {
                ContextCompat.startForegroundService(
                    ctx,
                    Intent(ctx, SpeechService::class.java)
                        .putExtra(Extra.TEXT, if (withVoice) text else "")
                        .putExtra(Extra.CLIP, clip)
                        .putExtra(Extra.NAME, preName),
                )
                true
            } catch (e: Exception) {
                Log.w(TAG, "xizmatni boshlab bo'lmadi: ${e.message}")
                false
            }
    }
}
