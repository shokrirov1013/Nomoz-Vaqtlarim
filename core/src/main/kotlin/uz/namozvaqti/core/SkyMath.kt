package uz.namozvaqti.core

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Quyosh va oy holati: bosh sahifadagi dinamik osmon uchun. Internet kerak emas.
 * Formulalar SunCalc kutubxonasidan (MIT litsenziya, V. Agafonkin) — pastroq aniqlikda (daqiqalar), rasm uchun yetarli.
 */
object SkyMath {
    /** [azimuth]: 0° = janub, musbat = g'arb tomon. [altitude]: ufqdan balandlik (manfiy = ufq ostida). */
    data class Position(val azimuth: Double, val altitude: Double)

    /** [phase]: 0 = yangi oy, 0.25 = birinchi chorak, 0.5 = to'lin oy, 0.75 = oxirgi chorak. [fraction]: yoritilgan ulush. */
    data class MoonPhase(val phase: Double, val fraction: Double)

    private const val RAD = PI / 180.0
    private const val DAY_MS = 86_400_000.0
    private const val J1970 = 2_440_588.0
    private const val J2000 = 2_451_545.0
    private val OBLIQUITY = RAD * 23.4397

    private fun toDays(epochMs: Long) = epochMs / DAY_MS - 0.5 + J1970 - J2000

    private fun rightAscension(l: Double, b: Double) = atan2(sin(l) * cos(OBLIQUITY) - tan(b) * sin(OBLIQUITY), cos(l))
    private fun declination(l: Double, b: Double) = asin(sin(b) * cos(OBLIQUITY) + cos(b) * sin(OBLIQUITY) * sin(l))
    private fun azimuthOf(h: Double, phi: Double, dec: Double) = atan2(sin(h), cos(h) * sin(phi) - tan(dec) * cos(phi))
    private fun altitudeOf(h: Double, phi: Double, dec: Double) = asin(sin(phi) * sin(dec) + cos(phi) * cos(dec) * cos(h))
    private fun siderealTime(d: Double, lw: Double) = RAD * (280.16 + 360.9856235 * d) - lw

    private class Eq(val ra: Double, val dec: Double, val dist: Double = 0.0)

    private fun sunEq(d: Double): Eq {
        val m = RAD * (357.5291 + 0.98560028 * d)
        val c = RAD * (1.9148 * sin(m) + 0.02 * sin(2 * m) + 0.0003 * sin(3 * m))
        val l = m + c + RAD * 102.9372 + PI
        return Eq(rightAscension(l, 0.0), declination(l, 0.0))
    }

    private fun moonEq(d: Double): Eq {
        val l0 = RAD * (218.316 + 13.176396 * d)
        val m = RAD * (134.963 + 13.064993 * d)
        val f = RAD * (93.272 + 13.22935 * d)
        val l = l0 + RAD * 6.289 * sin(m)
        val b = RAD * 5.128 * sin(f)
        return Eq(rightAscension(l, b), declination(l, b), 385_001 - 20_905 * cos(m))
    }

    fun sunPosition(epochMs: Long, latDeg: Double, lonDeg: Double): Position {
        val d = toDays(epochMs)
        val c = sunEq(d)
        val h = siderealTime(d, RAD * -lonDeg) - c.ra
        val phi = RAD * latDeg
        return Position(azimuthOf(h, phi, c.dec) / RAD, altitudeOf(h, phi, c.dec) / RAD)
    }

    private fun refraction(hRad: Double): Double {
        val h = if (hRad < 0) 0.0 else hRad
        return 0.0002967 / tan(h + 0.00312536 / (h + 0.08901179))
    }

    fun moonPosition(epochMs: Long, latDeg: Double, lonDeg: Double): Position {
        val d = toDays(epochMs)
        val c = moonEq(d)
        val h = siderealTime(d, RAD * -lonDeg) - c.ra
        val phi = RAD * latDeg
        var alt = altitudeOf(h, phi, c.dec)
        alt += refraction(alt)
        return Position(azimuthOf(h, phi, c.dec) / RAD, alt / RAD)
    }

    fun moonPhase(epochMs: Long): MoonPhase {
        val d = toDays(epochMs)
        val s = sunEq(d)
        val m = moonEq(d)
        val sdist = 149_598_000.0
        val phi = acos(sin(s.dec) * sin(m.dec) + cos(s.dec) * cos(m.dec) * cos(s.ra - m.ra))
        val inc = atan2(sdist * sin(phi), m.dist - sdist * cos(phi))
        val angle = atan2(
            cos(s.dec) * sin(s.ra - m.ra),
            sin(s.dec) * cos(m.dec) - cos(s.dec) * sin(m.dec) * cos(s.ra - m.ra),
        )
        return MoonPhase(0.5 + 0.5 * inc * (if (angle < 0) -1 else 1) / PI, (1 + cos(inc)) / 2)
    }

    /** Berilgan kun uchun quyosh chiqishi va botishi (mahalliy vaqt). Qutb tunlarida null bo'lishi mumkin. */
    fun sunTimes(date: LocalDate, latDeg: Double, lonDeg: Double, zone: ZoneId = ZoneId.systemDefault()): Pair<LocalTime?, LocalTime?> {
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        var rise: LocalTime? = null
        var set: LocalTime? = null
        var prev = sunPosition(start, latDeg, lonDeg).altitude
        for (minute in 1..1440) {
            val t = start + minute * 60_000L
            val a = sunPosition(t, latDeg, lonDeg).altitude
            if (prev < HORIZON && a >= HORIZON && rise == null) rise = LocalTime.of((minute / 60) % 24, minute % 60)
            if (prev >= HORIZON && a < HORIZON && set == null) set = LocalTime.of((minute / 60) % 24, minute % 60)
            prev = a
        }
        return rise to set
    }

    private const val HORIZON = -0.833
}
