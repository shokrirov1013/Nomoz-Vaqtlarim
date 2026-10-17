package uz.namozvaqti.core

import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

data class HijriDate(val year: Int, val month: Int, val day: Int) {
    val monthName: String get() = HijriCalendar.monthNames[month - 1]
    override fun toString() = "$day-$monthName, $year"
}

/**
 * Hijriy sana — Um al-Qura taqvimi (java.time.chrono.HijrahDate), O'zbekiston uchun
 * -2..+2 kunlik moslashtirish bilan.
 */
object HijriCalendar {
    val monthNames = listOf(
        "Muharram", "Safar", "Rabiul avval", "Rabiul oxir", "Jumodul avval", "Jumodul oxir",
        "Rajab", "Sha'bon", "Ramazon", "Shavvol", "Zulqa'da", "Zulhijja",
    )

    fun fromGregorian(date: LocalDate, adjustDays: Int = 0): HijriDate {
        val h = HijrahDate.from(date.plusDays(adjustDays.toLong()))
        return HijriDate(h.get(ChronoField.YEAR), h.get(ChronoField.MONTH_OF_YEAR), h.get(ChronoField.DAY_OF_MONTH))
    }

    fun toGregorian(year: Int, month: Int, day: Int, adjustDays: Int = 0): LocalDate =
        LocalDate.from(HijrahDate.of(year, month, day)).minusDays(adjustDays.toLong())

    fun lengthOfMonth(year: Int, month: Int): Int = HijrahDate.of(year, month, 1).lengthOfMonth()

    /** Hijriy oyning har bir kuni uchun milodiy sana (taqvim katagi uchun). */
    fun monthDays(year: Int, month: Int, adjustDays: Int = 0): List<Pair<HijriDate, LocalDate>> =
        (1..lengthOfMonth(year, month)).map { d ->
            HijriDate(year, month, d) to toGregorian(year, month, d, adjustDays)
        }

    fun nextMonth(year: Int, month: Int): Pair<Int, Int> = if (month == 12) year + 1 to 1 else year to month + 1
    fun prevMonth(year: Int, month: Int): Pair<Int, Int> = if (month == 1) year - 1 to 12 else year to month - 1
}
