package uz.namozvaqti.core

import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

/** Namoz vaqtini hisoblash usullari (Bomdod va Xufton burchaklari). */
enum class CalcMethod(
    val key: String,
    val label: String,
    val fajrAngle: Double,
    val ishaAngle: Double,
    val ishaAfterMaghribMinutes: Int = 0,
) {
    UZBEKISTAN("uz", "O'zbekiston (Bomdod 15°, Xufton 15°)", 15.0, 15.0),
    MWL("mwl", "Muslim World League (18° / 17°)", 18.0, 17.0),
    EGYPT("egypt", "Misr (19.5° / 17.5°)", 19.5, 17.5),
    KARACHI("karachi", "Karachi (18° / 18°)", 18.0, 18.0),
    ISNA("isna", "ISNA (15° / 15°)", 15.0, 15.0),
    UMM_AL_QURA("umm", "Umm al-Qura (18.5° / Shomdan 90 daq.)", 18.5, 0.0, 90);

    companion object {
        fun fromKey(key: String?): CalcMethod = entries.firstOrNull { it.key == key } ?: UZBEKISTAN
    }
}

/**
 * Astronomik hisoblash (PrayTimes algoritmi asosida). Internet bo'lmaganda ham ishlaydi.
 * [utcOffsetHours] — shu sanadagi mahalliy vaqt zonasi siljishi (soat).
 */
object PrayerCalculator {

    fun calculate(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        utcOffsetHours: Double,
        method: CalcMethod = CalcMethod.UZBEKISTAN,
        hanafiAsr: Boolean = true,
    ): PrayerTimes {
        val jd = julianDate(date.year, date.monthValue, date.dayOfMonth) - longitude / (15.0 * 24.0)
        val asrFactor = if (hanafiAsr) 2.0 else 1.0

        fun midDay(t: Double): Double = fixHour(12.0 - sunPosition(jd + t).second)

        fun angleTime(angle: Double, t: Double, ccw: Boolean): Double {
            val decl = sunPosition(jd + t).first
            val noon = midDay(t)
            val a = (-sinD(angle) - sinD(decl) * sinD(latitude)) / (cosD(decl) * cosD(latitude))
            val h = arcCosD(a.coerceIn(-1.0, 1.0)) / 15.0
            return noon + if (ccw) -h else h
        }

        fun asrTime(t: Double): Double {
            val decl = sunPosition(jd + t).first
            val angle = -arcCotD(asrFactor + tanD(abs(latitude - decl)))
            return angleTime(angle, t, ccw = false)
        }

        // Ikki marta iteratsiya: vaqtlar (soatda) kunning boshlang'ich taxminidan aniqlashtiriladi.
        // MUHIM: har bir hisobga vaqt kun ULUSHI (soat/24) sifatida uzatiladi.
        var fajr = 5.0; var sunrise = 6.0; var dhuhr = 12.0
        var asr = 13.0; var sunset = 18.0; var isha = 18.0
        repeat(2) {
            fajr = angleTime(method.fajrAngle, fajr / 24, ccw = true)
            sunrise = angleTime(0.833, sunrise / 24, ccw = true)
            dhuhr = midDay(dhuhr / 24)
            asr = asrTime(asr / 24)
            sunset = angleTime(0.833, sunset / 24, ccw = false)
            isha = if (method.ishaAfterMaghribMinutes > 0) sunset
            else angleTime(method.ishaAngle, isha / 24, ccw = false)
        }
        val adjust = utcOffsetHours - longitude / 15.0
        fun fin(h: Double) = toTime(h + adjust)
        val ishaFinal = if (method.ishaAfterMaghribMinutes > 0)
            fin(isha).plusMinutes(method.ishaAfterMaghribMinutes.toLong()) else fin(isha)

        return PrayerTimes(date, fin(fajr), fin(sunrise), fin(dhuhr), fin(asr), fin(sunset), ishaFinal)
    }

    private fun toTime(hours: Double): LocalTime {
        val minutes = (fixHour(hours) * 60.0).roundToInt() % (24 * 60)
        return LocalTime.of(minutes / 60, minutes % 60)
    }

    fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year; var m = month
        if (m <= 2) { y -= 1; m += 12 }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    /** (og'ish darajada, vaqt tenglamasi soatda) */
    private fun sunPosition(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sinD(g) + 0.020 * sinD(2 * g))
        val e = 23.439 - 0.00000036 * d
        val ra = fixHour(arcTanD2(cosD(e) * sinD(l), cosD(l)) / 15.0)
        val eqt = q / 15.0 - ra
        val decl = arcSinD(sinD(e) * sinD(l))
        return decl to eqt
    }

    private fun rad(d: Double) = Math.toRadians(d)
    private fun deg(r: Double) = Math.toDegrees(r)
    private fun sinD(d: Double) = sin(rad(d))
    private fun cosD(d: Double) = cos(rad(d))
    private fun tanD(d: Double) = tan(rad(d))
    private fun arcSinD(x: Double) = deg(asin(x))
    private fun arcCosD(x: Double) = deg(acos(x))
    private fun arcTanD2(y: Double, x: Double) = deg(atan2(y, x))
    private fun arcCotD(x: Double) = deg(atan(1.0 / x))
    private fun fixAngle(a: Double) = fix(a, 360.0)
    private fun fixHour(a: Double) = fix(a, 24.0)
    private fun fix(a: Double, mode: Double): Double {
        val r = a - mode * floor(a / mode)
        return if (r < 0) r + mode else r
    }
}
