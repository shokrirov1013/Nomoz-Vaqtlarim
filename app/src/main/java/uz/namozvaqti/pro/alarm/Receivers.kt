package uz.namozvaqti.pro.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uz.namozvaqti.core.Juma
import uz.namozvaqti.core.Prayer
import uz.namozvaqti.core.PrayerTexts
import uz.namozvaqti.core.ReminderEvent
import uz.namozvaqti.core.ReminderKind
import uz.namozvaqti.core.ReminderPlanner
import uz.namozvaqti.core.VoiceKeys
import uz.namozvaqti.pro.ServiceLocator
import uz.namozvaqti.pro.data.ReminderState
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

private const val TAG = "Receivers"

/** Receiver ichida DB va tarmoq bilan ishlash uchun: goAsync + IO korutin. */
internal fun BroadcastReceiver.async(block: suspend () -> Unit) {
    val pending = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
        try {
            block()
        } catch (t: Throwable) {
            Log.e(TAG, "receiver xatosi", t)
        } finally {
            pending.finish()
        }
    }
}

/** Namoz eslatmalari (oldindan / vaqti / qayta eslatma) va kunlik yangilash. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val app = ctx.applicationContext
        when (intent.action) {
            Act.REMINDER -> async { handleReminder(app, intent) }
            Act.JUMA -> async { handleJuma(app, intent) }
            Act.DAILY -> async {
                ServiceLocator.get(app).times.refresh()
                Scheduler.rescheduleAll(app)
            }
        }
    }

    private suspend fun handleReminder(ctx: Context, intent: Intent) {
        val date = intent.getStringExtra(Extra.DATE)?.let { LocalDate.parse(it) } ?: return
        val prayer = Prayer.fromKey(intent.getStringExtra(Extra.PRAYER)) ?: return
        val stage = intent.getIntExtra(Extra.STAGE, -1).takeIf { it >= 0 } ?: return
        val at = intent.getStringExtra(Extra.AT)?.let { LocalDateTime.parse(it) } ?: return

        val sl = ServiceLocator.get(ctx)
        val s = sl.settings.value
        if (s.prayerReminders[prayer] == false) return

        val done = sl.db.prayerLog().count(date.toString(), prayer.key) > 0
        val fired = sl.db.reminderState().get(date.toString(), prayer.key)?.firedStage ?: -1
        val event = ReminderEvent(date, prayer, stage, at)
        val now = LocalDateTime.now()
        if (!ReminderPlanner.shouldDeliver(event, now, done, fired, s.policy)) {
            Log.i(TAG, "o'tkazib yuborildi: $prayer stage=$stage done=$done fired=$fired")
            if (done) Scheduler.cancelPrayerDay(ctx, date, prayer)
            return
        }
        sl.db.reminderState().upsert(ReminderState(date.toString(), prayer.key, stage, System.currentTimeMillis()))

        val text = when (event.kind) {
            ReminderKind.PRE -> PrayerTexts.preText(prayer, s.preMinutes)
            ReminderKind.MAIN -> PrayerTexts.mainText(prayer, date, s.gender, s.gentle)
            ReminderKind.FOLLOWUP -> PrayerTexts.followupText(prayer, event.followupIndex, s.policy.followups, s.gentle)
        }
        Notifications.postReminder(ctx, event, text)
        // Asosiy va qayta eslatma: to'liq ekranli oyna hamma ilovalar ustida ochiladi (ruxsat berilgan bo'lsa).
        if (event.kind != ReminderKind.PRE && s.popupReminder) ReminderActivity.launch(ctx, event, text)
        // Tayyor ovoz paketi muloyim matnlar uchun yozilgan; oldindan eslatma — faqat 5 daqiqa uchun.
        val clip = if (!s.gentle) null else when (event.kind) {
            ReminderKind.PRE -> if (s.preMinutes == VoiceKeys.PRE_MINUTES) VoiceKeys.pre(prayer) else null
            ReminderKind.MAIN -> VoiceKeys.main(prayer, date, s.gender)
            ReminderKind.FOLLOWUP -> VoiceKeys.followup(prayer, event.followupIndex, s.policy.followups)
        }
        // Ovoz xizmatini boshlab bo'lmasa, tizim ovozi bilan zaxira bildirishnoma.
        if (!SpeechService.speak(ctx, text, withVoice = s.voiceEnabled, clip = clip)) Notifications.postReminder(ctx, event, text, systemSound = true)
    }
}

/** Juma kuni 13:15: ism bilan tabrik (bildirishnoma + ovoz). */
private suspend fun handleJuma(ctx: Context, intent: Intent) {
    val s = ServiceLocator.get(ctx).settings.value
    Scheduler.scheduleJuma(ctx) // keyingi haftaga
    if (!s.jumaGreeting) return
    val at = intent.getStringExtra(Extra.AT)?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
    // Telefon o'chiq bo'lgan va juda kech qolgan tabrik aytilmaydi.
    if (at != null && Duration.between(at, LocalDateTime.now()).toMinutes() > 60) return
    val text = Juma.text(s.name)
    Notifications.postJuma(ctx, text)
    SpeechService.speak(ctx, text, withVoice = s.voiceEnabled, clip = Juma.CLIP, preName = s.name.trim())
}

/** Bomdod va Tahajjud budilnigi. */
class RingReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action != Act.RING) return
        val app = ctx.applicationContext
        val kind = RingKind.fromKey(intent.getStringExtra(Extra.KIND))
        val at = intent.getStringExtra(Extra.AT)?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
        async {
            val sl = ServiceLocator.get(app)
            val s = sl.settings.value
            // Juda kech qolgan eski budilnik (masalan, telefon o'chiq bo'lgan) ishga tushmaydi.
            if (at != null && Duration.between(at, LocalDateTime.now()).toMinutes() > 10) return@async
            when (kind) {
                RingKind.FAJR -> {
                    if (!s.fajrAlarmEnabled) return@async
                    val today = LocalDate.now()
                    if (sl.db.prayerLog().count(today.toString(), Prayer.FAJR.key) > 0) return@async
                    sl.db.reminderState().upsert(ReminderState(today.toString(), Prayer.FAJR.key, 1, System.currentTimeMillis()))
                }
                RingKind.TAHAJJUD -> {
                    // Keyingi haftalik takrorni darhol rejalashtiramiz.
                    Scheduler.scheduleTahajjud(app)
                    if (!s.tahajjudEnabled) return@async
                }
            }
            RingService.start(app, kind)
        }
    }
}

/** Bildirishnoma tugmalari: Bajarildi, To'xtatish, Keyinga surish. */
class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val app = ctx.applicationContext
        when (intent.action) {
            Act.MARK_DONE -> {
                val date = intent.getStringExtra(Extra.DATE)?.let { LocalDate.parse(it) } ?: return
                val prayer = Prayer.fromKey(intent.getStringExtra(Extra.PRAYER)) ?: return
                async { PrayerActions.setDone(app, date, prayer, true) }
            }
            Act.STOP_RING -> RingControl.stop(app)
            Act.SNOOZE_RING -> RingControl.snooze(app, RingKind.fromKey(intent.getStringExtra(Extra.KIND)))
        }
    }
}

/** Qayta yuklash, vaqt/zona o'zgarishi va ilova yangilanishidan keyin barcha rejalarni tiklaydi. */
class SystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val app = ctx.applicationContext
        Log.i(TAG, "tizim hodisasi: ${intent.action}")
        async {
            Scheduler.rescheduleAll(app)
            WorkScheduler.refreshNow(app)
        }
    }
}
