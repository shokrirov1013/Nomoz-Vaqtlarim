package uz.namozvaqti.pro.ui

import uz.namozvaqti.core.HijriCalendar
import uz.namozvaqti.core.HijriDate
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object Fmt {
    val months = listOf(
        "Yanvar", "Fevral", "Mart", "Aprel", "May", "Iyun",
        "Iyul", "Avgust", "Sentabr", "Oktabr", "Noyabr", "Dekabr",
    )
    val weekdays = listOf("Dushanba", "Seshanba", "Chorshanba", "Payshanba", "Juma", "Shanba", "Yakshanba")
    val weekdaysShort = listOf("Du", "Se", "Ch", "Pa", "Ju", "Sh", "Ya")

    private val hm = DateTimeFormatter.ofPattern("HH:mm")

    fun time(t: LocalTime): String = t.format(hm)
    fun weekday(d: DayOfWeek): String = weekdays[d.value - 1]
    fun weekdayShort(d: DayOfWeek): String = weekdaysShort[d.value - 1]

    fun gregorian(d: LocalDate): String = "${d.dayOfMonth}-${months[d.monthValue - 1]}, ${d.year}"
    fun gregorianFull(d: LocalDate): String = "${weekday(d.dayOfWeek)}, ${gregorian(d)}"
    fun hijri(h: HijriDate): String = "${h.day}-${h.monthName}, ${h.year}-yil"
    fun hijri(d: LocalDate, adjust: Int): String = hijri(HijriCalendar.fromGregorian(d, adjust))

    fun countdown(d: Duration): String {
        val s = d.seconds.coerceAtLeast(0)
        return "%02d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
    }
}
