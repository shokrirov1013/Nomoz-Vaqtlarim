package uz.namozvaqti.core

import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MuslimUzTest {
    // muslim.uz bosh sahifasidan (2026-10-18) haqiqiy parcha
    private val html = """
<div id="prayer" class="text-center d-flex align-items-center">
<div class="float-left"><div class="px-3"><i class="fas fa-map-marked-alt"></i><br>Тошкент шаҳри</div></div>
<div class="float-right d-flex align-items-center">
<div class="flex-column px-3 py-1 border-right border-left"><div>Бомдод</div><div>05:19</div></div>
<div class="flex-column px-3 py-1 border-right border-left"><div>Қуёш</div><div>06:38</div></div>
<div class="flex-column px-3 py-1 border-right border-left"><div>Пешин</div><div>12:08</div></div>
<div class="flex-column px-3 py-1 border-right border-left"><div>Аср</div><div>15:55</div></div>
<div class="flex-column px-3 py-1 border-right border-left"><div>Шом</div><div>17:42</div></div>
<div class="flex-column px-3 py-1 border-right border-left"><div>Хуфтон</div><div>18:57</div></div>
</div></div>
<div class="namaz_name">
18 Октябр, 2026, 7 Жумадул аввал, 1448<!-- x -->
</div>"""

    @Test fun parsesOfficialBlock() {
        val t = assertNotNull(MuslimUzSource.parseToday(html))
        assertEquals(LocalDate.of(2026, 10, 18), t.date)
        assertEquals("Тошкент шаҳри", t.city)
        assertEquals(LocalTime.of(5, 19), t.times.fajr)
        assertEquals(LocalTime.of(15, 55), t.times.asr)
        assertEquals(LocalTime.of(18, 57), t.times.isha)
    }

    @Test fun rejectsBrokenPages() {
        assertNull(MuslimUzSource.parseToday("<html>Just a moment...</html>"))
        assertNull(MuslimUzSource.parseToday("<div id=\"prayer\">05:19 06:38</div> 18 Октябр, 2026"))
    }

    @Test fun calibrationMatchesOfficialTimes() {
        val official = assertNotNull(MuslimUzSource.parseToday(html)).times
        val o = assertNotNull(Muftiyat.offsetsFrom(official))
        assertEquals(Muftiyat.DEFAULT, o) // Bomdod -3, Shom +4, Xufton +3; qolganlari aynan mos
        // Toshkent uchun kalibrlangan hisob idora vaqtlariga teng
        val own = PrayerCalculator.calculate(official.date, Muftiyat.REF_LAT, Muftiyat.REF_LON, 5.0, CalcMethod.UZBEKISTAN, true)
        assertEquals(official, Muftiyat.apply(own, o))
    }

    @Test fun absurdDifferenceIsRejected() {
        val official = assertNotNull(MuslimUzSource.parseToday(html)).times
        assertNull(Muftiyat.offsetsFrom(official.copy(maghrib = LocalTime.of(19, 30))))
    }
}
