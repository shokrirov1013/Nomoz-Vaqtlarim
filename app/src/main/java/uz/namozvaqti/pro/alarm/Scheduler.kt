package uz.namozvaqti.pro.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.namozvaqti.core.Juma
import uz.namozvaqti.core.Prayer
import uz.namozvaqti.core.ReminderEvent
import uz.namozvaqti.core.ReminderPlanner
import uz.namozvaqti.core.ReminderPolicy
import uz.namozvaqti.core.TahajjudRules
import uz.namozvaqti.pro.MainActivity
import uz.namozvaqti.pro.ServiceLocator
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Barcha AlarmManager rejalari shu yerda.
 * Har bir alarm (sana + namoz + bosqich) uchun o'zgarmas requestCode ishlatiladi, shuning uchun
 * qayta rejalashtirish eskisini almashtiradi va takroriy alarm hosil bo'lmaydi.
 */
object Scheduler {
    private const val TAG = "Scheduler"
    private const val CODE_FAJR_BASE = 1_100_000_000
    private const val CODE_TAHAJJUD = 1_200_000_000
    private const val CODE_SNOOZE = 1_300_000_000
    private const val CODE_DAILY = 1_400_000_000
    private const val CODE_JUMA = 1_500_000_000

    private fun am(ctx: Context) = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private fun millis(at: LocalDateTime) = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private const val IMM = PendingIntent.FLAG_IMMUTABLE

    // ---------------------------------------------------------------- umumiy
    suspend fun rescheduleAll(ctx: Context) = withContext(Dispatchers.IO) {
        val sl = ServiceLocator.get(ctx)
        val s = sl.settings.value
        if (!s.onboardingDone) return@withContext
        val now = LocalDateTime.now()
        val today = LocalDate.now()
        // Kechagi kun ham: Xufton qayta eslatmalari yarim tundan o'tib ketishi mumkin.
        for (offset in -1L..1L) {
            val date = today.plusDays(offset)
            val times = sl.times.timesFor(date)
            for (p in Prayer.entries) scheduleDayPrayer(ctx, date, p, times.of(p), now)
        }
        scheduleFajrAlarm(ctx)
        scheduleTahajjud(ctx)
        scheduleJuma(ctx)
        scheduleDaily(ctx)
        sl.db.reminderState().deleteBefore(today.minusDays(3).toString())
        Log.i(TAG, "rescheduled at $now")
    }

    /** Bitta namozning bugungi/ertangi qolgan eslatmalarini qayta qo'yadi (belgi bekor qilinganda ham ishlatiladi). */
    suspend fun reschedulePrayer(ctx: Context, date: LocalDate, prayer: Prayer) = withContext(Dispatchers.IO) {
        val sl = ServiceLocator.get(ctx)
        scheduleDayPrayer(ctx, date, prayer, sl.times.timesFor(date).of(prayer), LocalDateTime.now())
    }

    private suspend fun scheduleDayPrayer(ctx: Context, date: LocalDate, prayer: Prayer, time: LocalTime, now: LocalDateTime) {
        val sl = ServiceLocator.get(ctx)
        val s = sl.settings.value
        cancelPrayerDay(ctx, date, prayer)
        if (s.prayerReminders[prayer] == false) return
        val done = sl.db.prayerLog().count(date.toString(), prayer.key) > 0
        val fired = sl.db.reminderState().get(date.toString(), prayer.key)?.firedStage ?: -1
        var events = ReminderPlanner.schedulable(ReminderPlanner.plan(date, prayer, time, s.policy), now, done, fired, s.policy)
        // Bomdod budilnigi yoqilgan bo'lsa, Bomdod vaqtidagi asosiy eslatma o'rniga budilnik chaladi.
        if (prayer == Prayer.FAJR && s.fajrAlarmEnabled) events = events.filter { it.stage != 1 }
        events.forEach { scheduleReminder(ctx, it) }
    }

    fun cancelPrayerDay(ctx: Context, date: LocalDate, prayer: Prayer) {
        for (stage in 0..ReminderPolicy.MAX_STAGE) {
            val pi = reminderPi(ctx, date, prayer, stage, null, PendingIntent.FLAG_NO_CREATE) ?: continue
            am(ctx).cancel(pi)
            pi.cancel()
        }
    }

    // ---------------------------------------------------------------- namoz eslatmalari
    private fun reminderPi(ctx: Context, date: LocalDate, prayer: Prayer, stage: Int, at: LocalDateTime?, flag: Int): PendingIntent? {
        val intent = Intent(ctx, ReminderReceiver::class.java).setAction(Act.REMINDER)
            .putExtra(Extra.DATE, date.toString()).putExtra(Extra.PRAYER, prayer.key).putExtra(Extra.STAGE, stage)
        if (at != null) intent.putExtra(Extra.AT, at.toString())
        return PendingIntent.getBroadcast(ctx, ReminderEvent.requestCode(date, prayer, stage), intent, flag or IMM)
    }

    private fun scheduleReminder(ctx: Context, e: ReminderEvent) {
        val pi = reminderPi(ctx, e.date, e.prayer, e.stage, e.at, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        val manager = am(ctx)
        // Vaqti hozirgina o'tib ketgan (masalan, telefon qayta yoqilgan) eslatma darhol ishga tushadi.
        val t = maxOf(millis(e.at), System.currentTimeMillis() + 2_000)
        val canExact = Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms()
        try {
            if (canExact) manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi)
            else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi)
        } catch (se: SecurityException) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi)
        }
    }

    // ---------------------------------------------------------------- Bomdod budilnigi
    private fun fajrCode(date: LocalDate) = CODE_FAJR_BASE + (date.toEpochDay() % 100_000).toInt()

    private fun ringPi(ctx: Context, code: Int, kind: RingKind, at: LocalDateTime?, date: LocalDate?, flag: Int): PendingIntent? {
        val intent = Intent(ctx, RingReceiver::class.java).setAction(Act.RING).putExtra(Extra.KIND, kind.key)
        if (at != null) intent.putExtra(Extra.AT, at.toString())
        if (date != null) intent.putExtra(Extra.DATE, date.toString())
        return PendingIntent.getBroadcast(ctx, code, intent, flag or IMM)
    }

    private fun showPi(ctx: Context) =
        PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java), IMM or PendingIntent.FLAG_UPDATE_CURRENT)

    private fun setClock(ctx: Context, at: LocalDateTime, pi: PendingIntent) {
        try {
            am(ctx).setAlarmClock(AlarmManager.AlarmClockInfo(millis(at), showPi(ctx)), pi)
        } catch (se: SecurityException) {
            am(ctx).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis(at), pi)
        }
    }

    suspend fun scheduleFajrAlarm(ctx: Context) = withContext(Dispatchers.IO) {
        val sl = ServiceLocator.get(ctx)
        val s = sl.settings.value
        val today = LocalDate.now()
        val now = LocalDateTime.now()
        for (offset in -1L..2L) cancelRing(ctx, fajrCode(today.plusDays(offset)), RingKind.FAJR)
        if (!s.fajrAlarmEnabled) return@withContext
        for (offset in 0L..1L) {
            val date = today.plusDays(offset)
            val at = LocalDateTime.of(date, sl.times.timesFor(date).fajr)
            val done = sl.db.prayerLog().count(date.toString(), Prayer.FAJR.key) > 0
            if (at.isAfter(now) && !done) {
                setClock(ctx, at, ringPi(ctx, fajrCode(date), RingKind.FAJR, at, date, PendingIntent.FLAG_UPDATE_CURRENT)!!)
            }
        }
    }

    // ---------------------------------------------------------------- Tahajjud
    suspend fun scheduleTahajjud(ctx: Context) = withContext(Dispatchers.IO) {
        val sl = ServiceLocator.get(ctx)
        val s = sl.settings.value
        cancelRing(ctx, CODE_TAHAJJUD, RingKind.TAHAJJUD)
        if (!s.tahajjudEnabled) return@withContext
        val schedule = sl.db.tahajjud().all().filter { it.enabled }.associate { it.dayOfWeek to LocalTime.of(it.hour, it.minute) }
        val next = TahajjudRules.nextOccurrence(LocalDateTime.now(), schedule) ?: return@withContext
        setClock(ctx, next, ringPi(ctx, CODE_TAHAJJUD, RingKind.TAHAJJUD, next, next.toLocalDate(), PendingIntent.FLAG_UPDATE_CURRENT)!!)
    }

    // ---------------------------------------------------------------- Juma tabrigi (har juma 13:15)
    private fun jumaPi(ctx: Context, at: LocalDateTime?, flag: Int): PendingIntent? {
        val intent = Intent(ctx, ReminderReceiver::class.java).setAction(Act.JUMA)
        if (at != null) intent.putExtra(Extra.AT, at.toString())
        return PendingIntent.getBroadcast(ctx, CODE_JUMA, intent, flag or IMM)
    }

    /** Keyingi juma 13:15 ga alarm qo'yadi (o'chirilgan bo'lsa bekor qiladi). Har safar ishga tushganda keyingi haftaga qayta qo'yiladi. */
    fun scheduleJuma(ctx: Context) {
        jumaPi(ctx, null, PendingIntent.FLAG_NO_CREATE)?.let { am(ctx).cancel(it); it.cancel() }
        val s = ServiceLocator.get(ctx).settings.value
        if (!s.onboardingDone || !s.jumaGreeting) return
        val at = Juma.next(LocalDateTime.now())
        val pi = jumaPi(ctx, at, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        val manager = am(ctx)
        val t = millis(at)
        try {
            if (canScheduleExact(ctx)) manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi)
            else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi)
        } catch (se: SecurityException) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi)
        }
    }

    // ---------------------------------------------------------------- keyinga surish
    fun snooze(ctx: Context, kind: RingKind, minutes: Int) {
        val at = LocalDateTime.now().plusMinutes(minutes.toLong())
        val code = CODE_SNOOZE + kind.ordinal
        setClock(ctx, at, ringPi(ctx, code, kind, at, LocalDate.now(), PendingIntent.FLAG_UPDATE_CURRENT)!!)
    }

    private fun cancelRing(ctx: Context, code: Int, kind: RingKind) {
        val pi = ringPi(ctx, code, kind, null, null, PendingIntent.FLAG_NO_CREATE) ?: return
        am(ctx).cancel(pi)
        pi.cancel()
    }

    // ---------------------------------------------------------------- kunlik yangilash (yarim tundan keyin)
    private fun scheduleDaily(ctx: Context) {
        val next = LocalDate.now().plusDays(1).atTime(0, 3)
        val pi = PendingIntent.getBroadcast(
            ctx, CODE_DAILY, Intent(ctx, ReminderReceiver::class.java).setAction(Act.DAILY),
            PendingIntent.FLAG_UPDATE_CURRENT or IMM,
        )
        am(ctx).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis(next), pi)
    }

    fun canScheduleExact(ctx: Context): Boolean = Build.VERSION.SDK_INT < 31 || am(ctx).canScheduleExactAlarms()
}
