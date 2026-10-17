package uz.namozvaqti.core

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MiscTest {
    @Test fun lastThirdOfNight() {
        val t = PrayerTimes(LocalDate.of(2026, 10, 17), LocalTime.of(5, 0), LocalTime.of(6, 0),
            LocalTime.of(12, 0), LocalTime.of(16, 0), LocalTime.of(18, 0), LocalTime.of(19, 30))
        // 18:00 -> 05:00 = 11 soat; 2/3 = 7:20 -> 01:20
        assertEquals(LocalDateTime.of(2026, 10, 18, 1, 20), Night.lastThirdStart(t, LocalTime.of(5, 0)))
    }

    @Test fun tahajjudNextOccurrence() {
        val sched = mapOf(1 to LocalTime.of(3, 30), 5 to LocalTime.of(4, 0)) // du, ju
        val sat = LocalDateTime.of(2026, 10, 17, 10, 0)
        assertEquals(LocalDateTime.of(2026, 10, 19, 3, 30), TahajjudRules.nextOccurrence(sat, sched))
        val fri = LocalDateTime.of(2026, 10, 16, 3, 0)
        assertEquals(LocalDateTime.of(2026, 10, 16, 4, 0), TahajjudRules.nextOccurrence(fri, sched))
        assertNull(TahajjudRules.nextOccurrence(sat, emptyMap()))
    }

    @Test fun statsAreOnlyWhatUserMarked() {
        val a = LocalDate.of(2026, 10, 10)
        val done = setOf(a to Prayer.FAJR, a to Prayer.ASR, a.plusDays(1) to Prayer.FAJR, a.minusDays(30) to Prayer.ISHA)
        val s = StatsCalculator.compute(a, a.plusDays(6), done)
        assertEquals(7, s.days); assertEquals(3, s.done); assertEquals(35 - 3, s.unmarked)
        assertEquals(2, s.perPrayer[Prayer.FAJR]); assertEquals(0, s.perPrayer[Prayer.ISHA])
        assertEquals(9, s.percentDone)
        assertEquals(0, StatsCalculator.compute(a, a.minusDays(1), done).slots)
    }

    @Test fun nearestCityAndShift() {
        val (c, d) = UzbekCities.nearest(41.35, 69.30)
        assertEquals("Toshkent", c.name); assertTrue(d < 10)
        assertEquals(-4, UzbekCities.longitudeShiftMinutes(69.0, 70.0))
        assertEquals(0, UzbekCities.longitudeShiftMinutes(69.2797, 69.2797))
    }

    @Test fun parseIslomUz() {
        // islomapi.uz /api/monthly haqiqiy tuzilishi (namozvatqiapi modeli): times.shom_iftor
        val body = """[{"region":"Toshkent","date":"2023-10-17T00:00:00.000Z","month":10,"day":17,"weekday":"Seshanba","hijri_date":"x",
          "times":{"tong_saharlik":"05:21","quyosh":"06:39","peshin":"12:09","asr":"16:12","shom_iftor":"17:39","hufton":"18:56"}},
          {"month":10,"day":18,"times":{"tong_saharlik":"05:22","quyosh":"06:40","peshin":"12:09","asr":"16:11","shom_iftor":"17:37","hufton":"18:55"}},
          {"month":10,"day":19,"times":{"tong_saharlik":"99:99"}},
          {"month":11,"day":1,"times":{"tong_saharlik":"05:30","quyosh":"06:50","peshin":"12:10","asr":"16:00","shom_iftor":"17:20","hufton":"18:40"}},
          {"month":10,"day":32,"times":{"tong_saharlik":"05:30","quyosh":"06:50","peshin":"12:10","asr":"16:00","shom_iftor":"17:20","hufton":"18:40"}}]"""
        val m = IslomUzSource.parseMonth(body, 2026, 10)
        assertEquals(2, m.size) // yil ahamiyatsiz, boshqa oy / buzuq / 32-kun tashlanadi
        assertEquals(LocalTime.of(12, 9), m[LocalDate.of(2026, 10, 17)]!!.dhuhr)
        assertEquals(LocalTime.of(17, 39), m[LocalDate.of(2026, 10, 17)]!!.maghrib)
        assertEquals(LocalTime.of(5, 22), m[LocalDate.of(2026, 10, 18)]!!.fajr)
    }

    @Test fun parseAladhan() {
        val body = """{"data":[{"timings":{"Fajr":"05:21 (+05)","Sunrise":"06:39 (+05)","Dhuhr":"12:09 (+05)",
          "Asr":"16:12 (+05)","Maghrib":"17:39 (+05)","Isha":"18:56 (+05)"},
          "date":{"gregorian":{"date":"17-10-2026"}}}]}"""
        val m = AladhanSource.parseMonth(body)
        assertEquals(LocalTime.of(18, 56), m[LocalDate.of(2026, 10, 17)]!!.isha)
        assertTrue(AladhanSource.monthUrl(2026, 10, 41.3, 69.2, CalcMethod.UZBEKISTAN, true, "Asia/Tashkent").contains("school=1"))
    }
}

class VersionsTest {
    @Test fun compare() {
        assertTrue(Versions.isNewer("v1.0.1", "1.0.0"))
        assertTrue(Versions.isNewer("1.10.0", "1.9.9"))
        assertTrue(Versions.isNewer("2.0", "1.9.9"))
        assertTrue(!Versions.isNewer("1.0.0", "1.0.0"))
        assertTrue(!Versions.isNewer("v1.0.0", "1.0.1"))
        assertTrue(!Versions.isNewer("abc", "1.0.0"))
        assertTrue(!Versions.isNewer("1.0.0-beta", "1.0.0"))
    }
}
