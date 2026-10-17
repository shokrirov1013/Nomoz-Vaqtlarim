package uz.namozvaqti.pro.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.namozvaqti.core.AladhanSource
import uz.namozvaqti.core.CalcMethod
import uz.namozvaqti.core.IslomUzSource
import uz.namozvaqti.core.MuftiyatOffsets
import uz.namozvaqti.core.Muftiyat
import uz.namozvaqti.core.MuslimUzSource
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
 *  "O'zbekiston" usuli + Hanafiy Asr:
 *    1) islom.uz ma'lumotlari (islomapi.uz orqali) — ishlasa, eng yaqin shahar jadvali + uzunlik bo'yicha moslash;
 *    2) muslim.uz (O'zbekiston Musulmonlari idorasi) bilan sinxronlangan hisob — joylashuv uchun aniq hisob,
 *       idora vaqtlariga (Bomdod/Shom/Xufton "ehtiyot" daqiqalari) moslangan;
 *  Boshqa usullar: Aladhan API, keyin qurilmada hisoblash (internetsiz ham ishlaydi).
 * Olingan oylik jadval bazaga saqlanadi; internet bo'lmasa oxirgi saqlangani ishlaydi.
 */
class TimesRepository(
    private val db: AppDatabase,
    private val settings: SettingsStore,
    context: android.content.Context,
) {
    private val calib = context.getSharedPreferences("muftiyat_calibration", android.content.Context.MODE_PRIVATE)

    private fun storedOffsets(): MuftiyatOffsets? =
        calib.getString("offsets", null)?.split(",")?.mapNotNull { it.toIntOrNull() }
            ?.takeIf { it.size == 6 }?.let { MuftiyatOffsets.fromList(it) }

    private fun offsets(): MuftiyatOffsets = storedOffsets() ?: Muftiyat.DEFAULT

    /** "O'zbekiston" usuli + Hanafiy Asr bo'lsa hisob idora (muslim.uz) vaqtlariga moslanadi. */
    private fun followsMuftiyat(s: Settings) = s.method == CalcMethod.UZBEKISTAN && s.hanafiAsr

    /**
     * muslim.uz bosh sahifasidagi bugungi rasmiy vaqtlarni oladi va o'z hisobimiz bilan farqni (har bir vaqt uchun
     * daqiqada) saqlaydi. Sayt ishlamasa, oxirgi saqlangan (yoki o'lchangan standart) farq ishlatiladi.
     */
    private fun calibrateWithMuslimUz(): Boolean = runCatching {
        val html = Http.get(MuslimUzSource.URL, timeoutMs = 12_000, accept = "text/html")
        val today = MuslimUzSource.parseToday(html) ?: return false
        val zone = ZoneId.of("Asia/Tashkent")
        val o = Muftiyat.offsetsFrom(today.times, zone.rules.getOffset(today.date.atTime(12, 0)).totalSeconds / 3600.0)
            ?: return false
        calib.edit().putString("offsets", o.toList().joinToString(",")).putString("date", today.date.toString())
            .putLong("fetchedAt", System.currentTimeMillis()).apply()
        true
    }.onFailure { Log.w(TAG, "muslim.uz: ${it.message}") }.getOrDefault(false)


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
        val base = PrayerCalculator.calculate(date, s.lat, s.lon, offset, s.method, s.hanafiAsr)
        return if (followsMuftiyat(s)) Muftiyat.apply(base, offsets()) else base
    }

    /** Joriy va keyingi oy jadvalini yangilaydi. Kamida bitta oy olinsa true. */
    suspend fun refresh(): Boolean = withContext(Dispatchers.IO) {
        val s = settings.value
        val key = cacheKey(s)
        val now = LocalDate.now()
        var ok = false
        // Avval idora (muslim.uz) bilan sinxronlaymiz: hisobni rasmiy vaqtlarga moslash uchun.
        val synced = followsMuftiyat(s) && calibrateWithMuslimUz()
        for (ym in listOf(YearMonth.from(now), YearMonth.from(now).plusMonths(1))) {
            val (times, source) = fetchMonth(s, ym, synced) ?: continue
            db.timesCache().insertAll(times.values.map { it.toCache(key, source) })
            ok = true
        }
        db.timesCache().deleteBefore(now.minusDays(40).toString())
        ok
    }

    private fun fetchMonth(s: Settings, ym: YearMonth, synced: Boolean): Pair<Map<LocalDate, PrayerTimes>, String>? {
        if (followsMuftiyat(s)) {
            fetchIslom(s, ym)?.let { return it }
            return muftiyatMonth(s, ym, synced)
        }
        fetchAladhan(s, ym)?.let { return it }
        return null
    }

    /** Joylashuvingiz uchun aniq hisob, idora (muslim.uz) vaqtlariga moslangan. Internet bo'lmasa ham ishlaydi. */
    private fun muftiyatMonth(s: Settings, ym: YearMonth, synced: Boolean): Pair<Map<LocalDate, PrayerTimes>, String> {
        val days = (1..ym.lengthOfMonth()).associate { d ->
            val date = ym.atDay(d)
            date to calculateLocally(date, s)
        }
        val source = when {
            synced -> "${MuslimUzSource.NAME} bilan sinxronlangan hisob — ${s.locationLabel}"
            storedOffsets() != null -> "Hisob, muslim.uz vaqtlariga moslangan (${calib.getString("date", "")}) — ${s.locationLabel}"
            else -> "Hisob, muslim.uz vaqtlariga moslangan (taxminiy) — ${s.locationLabel}"
        }
        return days to source
    }

    private fun fetchIslom(s: Settings, ym: YearMonth): Pair<Map<LocalDate, PrayerTimes>, String>? {
        if (s.method != CalcMethod.UZBEKISTAN || !s.hanafiAsr) return null
        val (city, distance) = UzbekCities.nearest(s.lat, s.lon)
        if (distance > UzbekCities.MAX_CITY_DISTANCE_KM) return null
        return runCatching {
            val body = Http.get(IslomUzSource.monthUrl(city.region, ym.monthValue), timeoutMs = 8_000)
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
