package uz.namozvaqti.core

import java.time.LocalDate
import java.time.LocalTime

/** muslim.uz bosh sahifasidagi "bugungi namoz vaqtlari" bloki (Toshkent). */
data class MuslimToday(val date: LocalDate, val city: String, val times: PrayerTimes)

/**
 * O'zbekiston Musulmonlari idorasi saytidan (muslim.uz) rasmiy vaqtlarni o'qish.
 * Sahifada faqat bugungi Toshkent vaqtlari bor (JSON API yo'q), shuning uchun bu ma'lumot
 * hisobni idora vaqtlariga MOSLASH (kalibrlash) uchun ishlatiladi — [Muftiyat].
 */
object MuslimUzSource {
    const val NAME = "muslim.uz (O'zbekiston Musulmonlari idorasi)"
    const val URL = "https://www.muslim.uz/oz"

    private val monthNames = mapOf(
        "январ" to 1, "феврал" to 2, "март" to 3, "апрел" to 4, "май" to 5, "июн" to 6,
        "июл" to 7, "август" to 8, "сентябр" to 9, "октябр" to 10, "ноябр" to 11, "декабр" to 12,
        "yanvar" to 1, "fevral" to 2, "mart" to 3, "aprel" to 4, "iyun" to 6,
        "iyul" to 7, "avgust" to 8, "sentabr" to 9, "oktabr" to 10, "noyabr" to 11, "dekabr" to 12,
    )

    /** Sahifa HTML'idan sana, shahar va 6 ta vaqtni oladi; tuzilma kutilganidek bo'lmasa null. */
    fun parseToday(html: String): MuslimToday? {
        val idx = html.indexOf("id=\"prayer\"")
        if (idx < 0) return null
        val block = html.substring(idx, minOf(html.length, idx + 4000))
        val times = Regex("""(?<![\d:])(\d{1,2}):(\d{2})(?![\d:])""").findAll(block).take(6).map {
            LocalTime.of(it.groupValues[1].toInt(), it.groupValues[2].toInt())
        }.toList()
        if (times.size < 6) return null
        val date = parseDate(html) ?: return null
        val city = Regex("""</i>\s*<br\s*/?>\s*([^<]+)</div>""").find(block)?.groupValues?.get(1)?.trim().orEmpty()
        val pt = PrayerTimes(date, times[0], times[1], times[2], times[3], times[4], times[5])
        if (!pt.isSane()) return null
        return MuslimToday(date, city, pt)
    }

    /** "18 Октябр, 2026" yoki "18 Oktabr, 2026". */
    private fun parseDate(html: String): LocalDate? {
        val m = Regex("""(\d{1,2})\s+([A-Za-zА-Яа-яЎўҚқҒғҲҳ]+),\s*(20\d{2})""").findAll(html)
            .firstOrNull { monthNames.keys.any { k -> it.groupValues[2].lowercase().startsWith(k) } } ?: return null
        val month = monthNames.entries.first { m.groupValues[2].lowercase().startsWith(it.key) }.value
        return runCatching { LocalDate.of(m.groupValues[3].toInt(), month, m.groupValues[1].toInt()) }.getOrNull()
    }
}

/** Idora vaqtlari bilan o'z hisobimiz o'rtasidagi farq (daqiqada), har bir vaqt uchun alohida. */
data class MuftiyatOffsets(val fajr: Int, val sunrise: Int, val dhuhr: Int, val asr: Int, val maghrib: Int, val isha: Int) {
    fun toList() = listOf(fajr, sunrise, dhuhr, asr, maghrib, isha)

    companion object {
        fun fromList(l: List<Int>) = MuftiyatOffsets(l[0], l[1], l[2], l[3], l[4], l[5])
        val ZERO = MuftiyatOffsets(0, 0, 0, 0, 0, 0)
    }
}

object Muftiyat {
    /** muslim.uz bosh sahifasi ko'rsatadigan shahar (Toshkent) markazi. */
    const val REF_LAT = 41.2995
    const val REF_LON = 69.2401
    private const val MAX_PLAUSIBLE = 12

    /**
     * 2026-10-18 da muslim.uz bilan solishtirib o'lchangan: Bomdod −3, Shom +4, Xufton +3 daqiqa (ehtiyot daqiqalari),
     * Quyosh/Peshin/Asr aynan mos. Sayt bilan sinxronlanguncha shu qiymatlar ishlatiladi.
     */
    val DEFAULT = MuftiyatOffsets(-3, 0, 0, 0, 4, 3)

    /** Idora vaqtlaridan farqni hisoblaydi; juda katta farq (tuzilma o'zgargan/noto'g'ri sahifa) rad etiladi. */
    fun offsetsFrom(official: PrayerTimes, utcOffsetHours: Double = 5.0): MuftiyatOffsets? {
        val own = PrayerCalculator.calculate(official.date, REF_LAT, REF_LON, utcOffsetHours, CalcMethod.UZBEKISTAN, true)
        fun d(a: LocalTime, b: LocalTime) = (a.toSecondOfDay() - b.toSecondOfDay()) / 60
        val o = MuftiyatOffsets(
            d(official.fajr, own.fajr), d(official.sunrise, own.sunrise), d(official.dhuhr, own.dhuhr),
            d(official.asr, own.asr), d(official.maghrib, own.maghrib), d(official.isha, own.isha),
        )
        return if (o.toList().all { kotlin.math.abs(it) <= MAX_PLAUSIBLE }) o else null
    }

    fun apply(t: PrayerTimes, o: MuftiyatOffsets): PrayerTimes = t.copy(
        fajr = t.fajr.plusMinutes(o.fajr.toLong()), sunrise = t.sunrise.plusMinutes(o.sunrise.toLong()),
        dhuhr = t.dhuhr.plusMinutes(o.dhuhr.toLong()), asr = t.asr.plusMinutes(o.asr.toLong()),
        maghrib = t.maghrib.plusMinutes(o.maghrib.toLong()), isha = t.isha.plusMinutes(o.isha.toLong()),
    )
}
