package uz.namozvaqti.core

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HijriTest {
    @Test fun knownDate() {
        // 1 Ramazon 1447 (Um al-Qura) = 2026-02-18
        val h = HijriCalendar.fromGregorian(LocalDate.of(2026, 2, 18))
        assertEquals(1447, h.year); assertEquals(9, h.month); assertEquals("Ramazon", h.monthName)
    }

    @Test fun adjustShiftsByOneDay() {
        val a = HijriCalendar.fromGregorian(LocalDate.of(2026, 10, 17), 0)
        val b = HijriCalendar.fromGregorian(LocalDate.of(2026, 10, 17), 1)
        val c = HijriCalendar.fromGregorian(LocalDate.of(2026, 10, 18), 0)
        assertEquals(c, b); assertTrue(a != b)
    }

    @Test fun roundTrip() {
        var d = LocalDate.of(2026, 1, 1)
        repeat(60) {
            val h = HijriCalendar.fromGregorian(d, -1)
            assertEquals(d, HijriCalendar.toGregorian(h.year, h.month, h.day, -1))
            d = d.plusDays(13)
        }
    }

    @Test fun monthDaysHave29Or30() {
        val days = HijriCalendar.monthDays(1448, 1)
        assertTrue(days.size in 29..30)
        assertEquals(1, days.first().first.day)
    }
}
