package uz.namozvaqti.core

import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PrayerCalculatorTest {
    private val tashkentLat = 41.3111
    private val tashkentLon = 69.2797

    private fun tashkent(date: LocalDate, hanafi: Boolean = true, method: CalcMethod = CalcMethod.UZBEKISTAN) =
        PrayerCalculator.calculate(date, tashkentLat, tashkentLon, 5.0, method, hanafi)

    @Test fun orderIsSaneAllYear() {
        var d = LocalDate.of(2026, 1, 1)
        while (d.year == 2026) {
            assertTrue(tashkent(d).isSane(), "tartib buzilgan: $d ${tashkent(d)}")
            d = d.plusDays(7)
        }
    }

    @Test fun solarNoonMatchesLongitudeAndEquationOfTime() {
        // Toshkent, 17-oktabr: quyosh tush vaqti ~12:08-12:12 (UTC+5, vaqt tenglamasi ~ +14 daq.)
        val t = tashkent(LocalDate.of(2026, 10, 17))
        assertTrue(t.dhuhr in LocalTime.of(12, 5)..LocalTime.of(12, 15), "peshin: ${t.dhuhr}")
        // Iyun oxiri: vaqt tenglamasi ~0, tush ~12:23
        val j = tashkent(LocalDate.of(2026, 6, 21))
        assertTrue(j.dhuhr in LocalTime.of(12, 20)..LocalTime.of(12, 30), "peshin iyun: ${j.dhuhr}")
    }

    @Test fun summerDayLongerThanWinter() {
        val s = tashkent(LocalDate.of(2026, 6, 21)); val w = tashkent(LocalDate.of(2026, 12, 21))
        assertTrue(s.maghrib.toSecondOfDay() - s.sunrise.toSecondOfDay() > w.maghrib.toSecondOfDay() - w.sunrise.toSecondOfDay() + 4 * 3600)
        // Tush ~12:25, kun ~15s04daq. => quyosh ~04:52
        assertTrue(s.sunrise in LocalTime.of(4, 45)..LocalTime.of(5, 0), "quyosh: ${s.sunrise}")
    }

    @Test fun hanafiAsrIsLater() {
        val d = LocalDate.of(2026, 10, 17)
        assertTrue(tashkent(d, true).asr.isAfter(tashkent(d, false).asr.plusMinutes(30)))
    }

    @Test fun methodsChangeFajrAndIsha() {
        val d = LocalDate.of(2026, 3, 10)
        assertTrue(tashkent(d, method = CalcMethod.MWL).fajr.isBefore(tashkent(d, method = CalcMethod.UZBEKISTAN).fajr))
    }

    @Test fun ummAlQuraIshaIs90MinutesAfterMaghrib() {
        val t = tashkent(LocalDate.of(2026, 10, 17), method = CalcMethod.UMM_AL_QURA)
        assertEquals(t.maghrib.plusMinutes(90), t.isha)
    }

    @Test fun otherTimezonesWork() {
        val t = PrayerCalculator.calculate(LocalDate.of(2026, 10, 17), 21.4225, 39.8262, 3.0, CalcMethod.UMM_AL_QURA, false)
        assertTrue(t.dhuhr in LocalTime.of(11, 50)..LocalTime.of(12, 10), "Makka peshin: ${t.dhuhr}")
    }
}
