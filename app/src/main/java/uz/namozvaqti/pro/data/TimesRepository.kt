package uz.namozvaqti.pro.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.namozvaqti.core.AladhanSource
import uz.namozvaqti.core.CalcMethod
import uz.namozvaqti.core.IslomUzSource
import uz.namozvaqti.core.PrayerCalculator
import uz.namozvaqti.core.PrayerTimes
import uz.namozvaqti.core.UzbekCities
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.abs

data class TimesInfo(val source: String, val fetchedAt: Long?)

/**
 * Namoz vaqtlari manbai (ustuvorlik tartibida):
 *  1) islom.uz ma'lumotlari — eng yaqin shahar jadvali + foydalanuvchi uzunligi bo'yicha moslashtirish
 *     (faqat "O'zbekiston" usuli va Hanafiy Asr tanlangan bo'lsa);
 *  2) Aladhan API — aniq GPS koordinatalari bo'yicha;
 *  3) Qurilmada astronomik hisoblash (internetsiz ham ishlaydi).
 * Olingan oylik jadval bazaga saqlanadi; internet bo'lmasa oxirgi saqlangani ishlaydi.
 */
class TimesRepository(private val db: AppDatabase, private val settings: SettingsStore) {

    private fun cacheKey(s: Settings): String =
        "%.2f,%.2f|%s|%s".format(java.util.Locale.US, s.lat, s.lon, s.method.key, if (s.hanafiAsr) "h" else "s")

    /** Rejalashtirish va ekran uchun: tarmoqsiz, tezkor. */
    suspend fun timesFor(date: LocalDate): PrayerTimes {
        val s = settings.value
        db.timesCache().get(date.toString(), cacheKey(s))?.let { return it.toTimes() }
        return calculateLocally(date, s)
    }

    suspend fun info(): TimesInfo {
        val s = settings.value
        val c = db.timesCache().latest(cacheKey(s))
        return if (c != null) TimesInfo(c.source, c.fetchedAt) else TimesInfo("Qurilmada hisoblangan (oflayn)", null)
    }

    fun calculateLocally(date: LocalDate, s: Settings): PrayerTimes {
        val offset = ZoneId.systemDefault().rules.getOffset(date.atTime(12, 0)).totalSeconds / 3600.0
        return PrayerCalculator.calculate(date, s.lat, s.lon, offset, s.method, s.hanafiAsr)
    }

    /** Joriy va keyingi oy jadvalini yangilaydi. Kamida bitta oy olinsa true. */
    suspend fun refresh(): Boolean = withContext(Dispatchers.IO) {
        val s = settings.value
        val key = cacheKey(s)
        val now = LocalDate.now()
        var ok = false
        for (ym in listOf(YearMonth.from(now), YearMonth.from(now).plusMonths(1))) {
            val (times, source) = fetchMonth(s, ym) ?: continue
            db.timesCache().insertAll(times.values.map { it.toCache(key, source) })
            ok = true
        }
        db.timesCache().deleteBefore(now.minusDays(40).toString())
        ok
    }

    private fun fetchMonth(s: Settings, ym: YearMonth): Pair<Map<LocalDate, PrayerTimes>, String>? {
        fetchIslom(s, ym)?.let { return it }
        fetchAladhan(s, ym)?.let { return it }
        return null
    }

    private fun fetchIslom(s: Settings, ym: YearMonth): Pair<Map<LocalDate, PrayerTimes>, String>? {
        if (s.method != CalcMethod.UZBEKISTAN || !s.hanafiAsr) return null
        val (city, distance) = UzbekCities.nearest(s.lat, s.lon)
        if (distance > UzbekCities.MAX_CITY_DISTANCE_KM) return null
        return runCatching {
            val body = Http.get(IslomUzSource.monthUrl(city.region, ym.monthValue))
            val shift = if (s.useGps) UzbekCities.longitudeShiftMinutes(city.lon, s.lon) else 0
            val checked = IslomUzSource.parseMonth(body, ym.year, ym.monthValue)
                .filter { (date, pt) -> date.year == ym.year && date.month == ym.month && plausible(pt, s) }
                .mapValues { it.value.shiftedByMinutes(shift.toLong()) }
            if (checked.size < ym.lengthOfMonth() - 2) return null
            val note = if (shift != 0) ", joylashuvga ${if (shift > 0) "+" else ""}$shift daq. moslangan" else ""
            checked to "${IslomUzSource.NAME} — ${city.name}$note"
        }.onFailure { Log.w(TAG, "islom.uz: ${it.message}") }.getOrNull()
    }

    private fun fetchAladhan(s: Settings, ym: YearMonth): Pair<Map<LocalDate, PrayerTimes>, String>? =
        runCatching {
            val url = AladhanSource.monthUrl(
                ym.year, ym.monthValue, s.lat, s.lon, s.method, s.hanafiAsr, ZoneId.systemDefault().id,
            )
            val parsed = AladhanSource.parseMonth(Http.get(url))
                .filter { (date, pt) -> date.year == ym.year && date.month == ym.month && plausible(pt, s) }
            if (parsed.size < ym.lengthOfMonth() - 2) return null
            parsed to AladhanSource.NAME
        }.onFailure { Log.w(TAG, "aladhan: ${it.message}") }.getOrNull()

    /** Ishonchlilik tekshiruvi: tashqi vaqt qurilmada hisoblangandan 25 daqiqadan ko'p farq qilmasin. */
    private fun plausible(pt: PrayerTimes, s: Settings): Boolean {
        val local = calculateLocally(pt.date, s)
        fun close(a: LocalTime, b: LocalTime) = abs(a.toSecondOfDay() - b.toSecondOfDay()) <= 25 * 60
        return close(pt.fajr, local.fajr) && close(pt.sunrise, local.sunrise) && close(pt.dhuhr, local.dhuhr) &&
            close(pt.asr, local.asr) && close(pt.maghrib, local.maghrib) && close(pt.isha, local.isha)
    }

    private fun TimesCache.toTimes() = PrayerTimes(
        LocalDate.parse(date), LocalTime.parse(fajr), LocalTime.parse(sunrise), LocalTime.parse(dhuhr),
        LocalTime.parse(asr), LocalTime.parse(maghrib), LocalTime.parse(isha),
    )

    private fun PrayerTimes.toCache(key: String, source: String) = TimesCache(
        date.toString(), key, fajr.toString(), sunrise.toString(), dhuhr.toString(),
        asr.toString(), maghrib.toString(), isha.toString(), source, System.currentTimeMillis(),
    )

    companion object {
        private const val TAG = "TimesRepository"
    }
}
