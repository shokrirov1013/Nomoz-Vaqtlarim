package uz.namozvaqti.pro.alarm

import android.content.Context
import uz.namozvaqti.core.Prayer
import uz.namozvaqti.pro.ServiceLocator
import uz.namozvaqti.pro.data.PrayerLog
import java.time.LocalDate

/** Namozni "Bajarildi" deb belgilash/qaytarish — bazaga yozish va eslatmalarni boshqarishni birga bajaradi. */
object PrayerActions {
    suspend fun setDone(ctx: Context, date: LocalDate, prayer: Prayer, done: Boolean) {
        val sl = ServiceLocator.get(ctx)
        if (done) {
            sl.db.prayerLog().insert(PrayerLog(date.toString(), prayer.key, System.currentTimeMillis()))
            // Shu namoz uchun qolgan barcha eslatmalar darhol bekor; boshqa namozlarga tegilmaydi.
            Scheduler.cancelPrayerDay(ctx, date, prayer)
            Notifications.cancel(ctx, Notifications.reminderId(prayer))
        } else {
            sl.db.prayerLog().delete(date.toString(), prayer.key)
            // Tasodifan belgi olib tashlansa, hali kelmagan eslatmalar qayta tiklanadi.
            Scheduler.reschedulePrayer(ctx, date, prayer)
        }
        if (prayer == Prayer.FAJR) Scheduler.scheduleFajrAlarm(ctx)
    }
}
