package uz.namozvaqti.core

import java.time.LocalDate

data class PeriodStats(
    val days: Int,
    val done: Int,
    val perPrayer: Map<Prayer, Int>,
) {
    val slots: Int get() = days * Prayer.entries.size

    /** Belgilanmagan = "o'qilmagan" degani emas; shunchaki foydalanuvchi belgilamagan. */
    val unmarked: Int get() = slots - done
    val percentDone: Int get() = if (slots == 0) 0 else Math.round(done * 100f / slots)
}

object StatsCalculator {
    fun compute(from: LocalDate, to: LocalDate, done: Set<Pair<LocalDate, Prayer>>): PeriodStats {
        if (to.isBefore(from)) return PeriodStats(0, 0, Prayer.entries.associateWith { 0 })
        val days = (to.toEpochDay() - from.toEpochDay() + 1).toInt()
        val inRange = done.filter { !it.first.isBefore(from) && !it.first.isAfter(to) }
        return PeriodStats(
            days = days,
            done = inRange.size,
            perPrayer = Prayer.entries.associateWith { p -> inRange.count { it.second == p } },
        )
    }
}
