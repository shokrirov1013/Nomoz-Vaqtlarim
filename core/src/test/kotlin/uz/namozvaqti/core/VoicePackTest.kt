package uz.namozvaqti.core

import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** voicepack/phrases.json (ovoz yozish manbasi) ilovadagi muloyim matnlar bilan bir xil ekanini tekshiradi. */
class VoicePackTest {
    private val json = JSONObject(File("../voicepack/phrases.json").readText())
    private val mon = LocalDate.of(2026, 10, 12)
    private val fri = LocalDate.of(2026, 10, 16)

    @Test fun uzbekPhrasesMatchAppTexts() {
        val uz = json.getJSONObject("uz")
        for (p in Prayer.entries) {
            assertEquals(PrayerTexts.preText(p, VoiceKeys.PRE_MINUTES), uz.getString(VoiceKeys.pre(p)))
            assertEquals(PrayerTexts.mainText(p, mon, Gender.MALE, gentle = true), uz.getString(VoiceKeys.main(p, mon, Gender.MALE)))
            assertEquals(PrayerTexts.followupText(p, 1, 3, true), uz.getString(VoiceKeys.followup(p, 1, 3)))
            assertEquals(PrayerTexts.followupText(p, 2, 3, true), uz.getString(VoiceKeys.followup(p, 2, 3)))
            assertEquals(PrayerTexts.followupText(p, 3, 3, true), uz.getString(VoiceKeys.followup(p, 3, 3)))
        }
        assertEquals(PrayerTexts.mainText(Prayer.DHUHR, fri, Gender.MALE, gentle = true), uz.getString(VoiceKeys.main(Prayer.DHUHR, fri, Gender.MALE)))
    }

    @Test fun russianHasSameKeys() {
        val uz = json.getJSONObject("uz").keySet(); val ru = json.getJSONObject("ru").keySet()
        assertEquals(uz, ru)
        assertEquals(27, uz.size)
        assertTrue(json.getJSONObject("ru").keySet().all { json.getJSONObject("ru").getString(it).isNotBlank() })
    }

    @Test fun followupKeysCoverEveryIndex() {
        // 3 ta qayta eslatma: 1 -> f1, 2 -> f2, 3 -> fl; 6 tagacha sozlanganda ham kalitlar mavjud
        for (n in 1..6) for (i in 1..n) {
            val k = VoiceKeys.followup(Prayer.ASR, i, n)
            assertTrue(json.getJSONObject("uz").has(k), k)
        }
    }
}
