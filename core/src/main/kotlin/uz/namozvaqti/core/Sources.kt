package uz.namozvaqti.core

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

/** Tashqi manbalar uchun URL va javob tahlilchilari (tarmoq so'rovi ilovada bajariladi). */
object IslomUzSource {
    const val NAME = "islom.uz (O'zbekiston Musulmonlari idorasi)"
    const val BASE = "https://islomapi.uz/api/monthly"

    fun monthUrl(region: String, month: Int): String =
        "$BASE?region=${java.net.URLEncoder.encode(region, "UTF-8")}&month=$month"

    /**
     * Javob: `[{region, date, month, day, weekday, hijri_date, times:{tong_saharlik, quyosh, peshin, asr, shom_iftor, hufton}}]`.
     * islom.uz yillik taqvim beradi (vaqtlar yildan yilga kun/oy bo'yicha takrorlanadi), shuning uchun sana
     * so'ralgan [year]-[month] va javobdagi `day` bo'yicha tuziladi. Buzuq kunlar tashlab yuboriladi.
     */
    fun parseMonth(body: String, year: Int, month: Int): Map<LocalDate, PrayerTimes> {
        val arr = JSONArray(body.trim())
        val out = mutableMapOf<LocalDate, PrayerTimes>()
        val length = YearMonth.of(year, month).lengthOfMonth()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val times = o.optJSONObject("times") ?: continue
            val day = dayOf(o, month) ?: continue
            if (day !in 1..length) continue
            val date = LocalDate.of(year, month, day)
            val pt = runCatching {
                PrayerTimes(
                    date = date,
                    fajr = time(times, "tong_saharlik", "bomdod", "fajr"),
                    sunrise = time(times, "quyosh", "sunrise"),
                    dhuhr = time(times, "peshin", "dhuhr"),
                    asr = time(times, "asr"),
                    maghrib = time(times, "shom_iftor", "shom", "maghrib"),
                    isha = time(times, "hufton", "xufton", "isha"),
                )
            }.getOrNull() ?: continue
            if (pt.isSane()) out[date] = pt
        }
        return out
    }

    /** Kun raqami; javobdagi oy so'ralgan oyga mos kelmasa null. */
    private fun dayOf(o: JSONObject, month: Int): Int? {
        val m = o.optInt("month", month)
        if (m != month) return null
        val day = o.optInt("day", 0)
        if (day > 0) return day
        val s = o.optString("date", "")
        if (s.length >= 10) runCatching { return LocalDate.parse(s.substring(0, 10)).dayOfMonth }
        return null
    }

    private fun time(o: JSONObject, vararg keys: String): LocalTime {
        for (k in keys) if (o.has(k)) return parseHm(o.getString(k))
        throw IllegalArgumentException("kalit yo'q: ${keys.toList()}")
    }
}

object AladhanSource {
    const val NAME = "Aladhan (aniq GPS koordinatalari bo'yicha)"

    fun monthUrl(
        year: Int, month: Int, lat: Double, lon: Double,
        method: CalcMethod, hanafiAsr: Boolean, timezone: String,
    ): String {
        val methodPart = when (method) {
            CalcMethod.MWL -> "method=3"
            CalcMethod.EGYPT -> "method=5"
            CalcMethod.KARACHI -> "method=1"
            CalcMethod.ISNA -> "method=2"
            CalcMethod.UMM_AL_QURA -> "method=4"
            CalcMethod.UZBEKISTAN -> "method=99&methodSettings=${method.fajrAngle},null,${method.ishaAngle}"
        }
        val school = if (hanafiAsr) 1 else 0
        return "https://api.aladhan.com/v1/calendar/$year/$month?latitude=$lat&longitude=$lon" +
            "&$methodPart&school=$school&timezonestring=${java.net.URLEncoder.encode(timezone, "UTF-8")}"
    }

    fun parseMonth(body: String): Map<LocalDate, PrayerTimes> {
        val data = JSONObject(body).optJSONArray("data") ?: return emptyMap()
        val out = mutableMapOf<LocalDate, PrayerTimes>()
        for (i in 0 until data.length()) {
            val o = data.optJSONObject(i) ?: continue
            val t = o.optJSONObject("timings") ?: continue
            val g = o.optJSONObject("date")?.optJSONObject("gregorian")?.optString("date") ?: continue
            val parts = g.split("-")
            if (parts.size != 3) continue
            val date = runCatching { LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt()) }.getOrNull() ?: continue
            val pt = runCatching {
                PrayerTimes(
                    date, parseHm(t.getString("Fajr")), parseHm(t.getString("Sunrise")),
                    parseHm(t.getString("Dhuhr")), parseHm(t.getString("Asr")),
                    parseHm(t.getString("Maghrib")), parseHm(t.getString("Isha")),
                )
            }.getOrNull() ?: continue
            if (pt.isSane()) out[date] = pt
        }
        return out
    }
}

/** "05:12", "5:12", "05:12 (+05)" ko'rinishlarini o'qiydi. */
fun parseHm(raw: String): LocalTime {
    val m = Regex("""(\d{1,2})[:.](\d{2})""").find(raw) ?: throw IllegalArgumentException("vaqt noto'g'ri: $raw")
    return LocalTime.of(m.groupValues[1].toInt(), m.groupValues[2].toInt())
}
