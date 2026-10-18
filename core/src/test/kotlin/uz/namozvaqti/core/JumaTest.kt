package uz.namozvaqti.core

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JumaTest {
    @Test fun nextIsAlwaysFridayAt1315AndInTheFuture() {
        var t = LocalDateTime.of(2026, 10, 12, 0, 0)
        repeat(24 * 30) {
            val n = Juma.next(t)
            assertEquals(DayOfWeek.FRIDAY, n.dayOfWeek)
            assertEquals(LocalTime.of(13, 15), n.toLocalTime())
            assertTrue(n.isAfter(t))
            assertTrue(java.time.Duration.between(t, n).toDays() <= 7)
            t = t.plusHours(1)
        }
    }

    @Test fun exactlyAtGreetingTimeGoesToNextWeek() {
        val fri = LocalDateTime.of(2026, 10, 16, 13, 15)
        assertEquals(LocalDate.of(2026, 10, 23), Juma.next(fri).toLocalDate())
        assertEquals(LocalDate.of(2026, 10, 16), Juma.next(fri.minusMinutes(1)).toLocalDate())
    }

    @Test fun textUsesName() {
        assertEquals("Muhammed, Juma bilan tabriklayman. Juma muborak bo'lsin!", Juma.text("  Muhammed "))
        assertEquals("Juma bilan tabriklayman. Juma muborak bo'lsin!", Juma.text(""))
    }
}
