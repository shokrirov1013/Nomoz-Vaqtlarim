package uz.namozvaqti.core

import java.time.LocalDate
import kotlin.test.Test

class CalibTest {
    @Test fun print() {
        val d = LocalDate.of(2026, 10, 18)
        println("MUSLIM.UZ Toshkent 18-okt: 05:19 06:38 12:08 15:55 17:42 18:57")
        for (m in CalcMethod.entries) for (h in listOf(true, false)) {
            val t = PrayerCalculator.calculate(d, 41.2995, 69.2401, 5.0, m, h)
            println("CALIB ${m.key} hanafi=$h  ${t.fajr} ${t.sunrise} ${t.dhuhr} ${t.asr} ${t.maghrib} ${t.isha}")
        }
        for (fa in listOf(14.0, 15.0, 16.0, 17.0, 18.0)) for (ia in listOf(14.0, 15.0, 16.0, 17.0)) {
            // faqat Bomdod/Xufton burchaklarini tekshirish (Asr kerak emas)
        }
    }
}
