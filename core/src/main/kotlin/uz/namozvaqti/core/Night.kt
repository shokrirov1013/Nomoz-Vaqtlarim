package uz.namozvaqti.core

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

object Night {
    /** Tunning oxirgi uchdan biri boshlanishi: Shomdan keyingi Bomdodgacha. */
    fun lastThirdStart(today: PrayerTimes, tomorrowFajr: LocalTime): LocalDateTime {
        val start = LocalDateTime.of(today.date, today.maghrib)
        val end = LocalDateTime.of(today.date.plusDays(1), tomorrowFajr)
        val night = Duration.between(start, end)
        return start.plus(night.multipliedBy(2).dividedBy(3))
    }
}

object TahajjudRules {
    const val DEFAULT_HOUR = 3
    const val DEFAULT_MINUTE = 30

    /** Keyingi ishga tushish vaqti: [days] — Java DayOfWeek raqamlari (1=Dushanba..7=Yakshanba). */
    fun nextOccurrence(now: LocalDateTime, schedule: Map<Int, LocalTime>): LocalDateTime? {
        var best: LocalDateTime? = null
        for (offset in 0..7) {
            val d: LocalDate = now.toLocalDate().plusDays(offset.toLong())
            val t = schedule[d.dayOfWeek.value] ?: continue
            val dt = LocalDateTime.of(d, t)
            if (dt.isAfter(now) && (best == null || dt.isBefore(best))) best = dt
        }
        return best
    }
}
