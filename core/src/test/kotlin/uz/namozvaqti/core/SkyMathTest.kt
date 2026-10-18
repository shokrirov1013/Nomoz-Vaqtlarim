package uz.namozvaqti.core

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SkyMathTest {
    private val tashkent = ZoneId.of("Asia/Tashkent")
    private val lat = 41.2995
    private val lon = 69.2401
    private fun ms(h: Int, m: Int = 0, date: LocalDate = LocalDate.of(2026, 10, 18)) =
        LocalDateTime.of(date, java.time.LocalTime.of(h, m)).atZone(tashkent).toInstant().toEpochMilli()

    @Test fun sunriseAndSunsetMatchShomTime() {
        val (rise, set) = SkyMath.sunTimes(LocalDate.of(2026, 10, 18), lat, lon, tashkent)
        assertNotNull(rise); assertNotNull(set)
        // Toshkent, 18-oktabr: quyosh ~06:38 da chiqadi va ~17:40 da botadi (Shom vaqti ~17:42).
        assertTrue(abs(rise.toSecondOfDay() - 6 * 3600 - 38 * 60) <= 6 * 60, "chiqish $rise")
        assertTrue(abs(set.toSecondOfDay() - 17 * 3600 - 40 * 60) <= 6 * 60, "botish $set")
    }

    @Test fun sunIsHighestAroundSolarNoonAndSouth() {
        val noon = SkyMath.sunPosition(ms(12, 8), lat, lon)
        assertTrue(noon.altitude in 37.0..41.0, "tush balandligi ${noon.altitude}")
        assertTrue(abs(noon.azimuth) < 6.0, "tush azimuti ${noon.azimuth}")
        val morning = SkyMath.sunPosition(ms(9), lat, lon)
        val afternoon = SkyMath.sunPosition(ms(15), lat, lon)
        assertTrue(morning.azimuth < 0 && afternoon.azimuth > 0) // sharqdan g'arbga
    }

    @Test fun sunIsBelowHorizonAtNightAndTwilightMatchesFajr() {
        assertTrue(SkyMath.sunPosition(ms(0, 30), lat, lon).altitude < -30)
        // Bomdod ~05:19: quyosh ufq ostida ~ -15° atrofida
        val fajr = SkyMath.sunPosition(ms(5, 19), lat, lon).altitude
        assertTrue(fajr in -19.0..-12.0, "bomdod paytida $fajr")
    }

    @Test fun moonPhaseOnFirstQuarterDay() {
        val p = SkyMath.moonPhase(ms(18))
        // 2026-10-18: birinchi chorak (~0.25), yarim yoritilgan
        assertTrue(abs(p.phase - 0.25) < 0.04, "faza ${p.phase}")
        assertTrue(p.fraction in 0.4..0.6, "ulush ${p.fraction}")
    }

    @Test fun firstQuarterMoonIsUpInTheEveningAndDownBeforeDawn() {
        assertTrue(SkyMath.moonPosition(ms(19), lat, lon).altitude > 10)
        assertTrue(SkyMath.moonPosition(ms(4), lat, lon).altitude < 0)
    }
}
