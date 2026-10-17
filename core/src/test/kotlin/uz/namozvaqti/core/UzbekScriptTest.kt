package uz.namozvaqti.core

import kotlin.test.Test
import kotlin.test.assertEquals

class UzbekScriptTest {
    private fun c(s: String) = UzbekScript.latinToCyrillic(s)

    @Test fun prayerNames() {
        assertEquals("Бомдод", c("Bomdod")); assertEquals("Пешин", c("Peshin")); assertEquals("Аср", c("Asr"))
        assertEquals("Шом", c("Shom")); assertEquals("Хуфтон", c("Xufton")); assertEquals("Қуёш", c("Quyosh"))
        assertEquals("Жума", c("Juma")); assertEquals("Таҳажжуд", c("Tahajjud"))
    }

    @Test fun specialLetters() {
        assertEquals("Ўзбекистон", c("O'zbekiston")); assertEquals("Ўзбекистон", c("Oʻzbekiston"))
        assertEquals("тоғ", c("tog'")); assertEquals("маъно", c("ma'no")); assertEquals("Шофеъий", c("Shofe'iy"))
        assertEquals("Янгиланиш", c("Yangilanish")); assertEquals("ҳаёт", c("hayot")); assertEquals("тайёр", c("tayyor"))
        assertEquals("Эслатма", c("Eslatma")); assertEquals("Белгиланмаган", c("Belgilanmagan"))
        assertEquals("Ҳижрий", c("Hijriy")); assertEquals("батарея", c("batareya")); assertEquals("Аллоҳ", c("Alloh"))
    }

    @Test fun sentencesAndPunctuation() {
        assertEquals("Пешин вақти кирди. Марҳамат, намозингизни ўқиб олинг.", c("Peshin vaqti kirdi. Marhamat, namozingizni o'qib oling."))
        assertEquals("Асосий: 5 дақиқа", c("Asosiy: 5 daqiqa"))
    }

    @Test fun abbreviationsAndNamesStayLatin() {
        assertEquals("GPS орқали аниқлаш", c("GPS orqali aniqlash"))
        assertEquals("GitHub: shokrirov1013/Nomoz-Vaqtlarim", c("GitHub: shokrirov1013/Nomoz-Vaqtlarim"))
        assertEquals("islom.uz жадвали", c("islom.uz jadvali"))
        assertEquals("Wi-Fi'да", c("Wi-Fi'da"))
        assertEquals("ISNA (15° / 15°)", c("ISNA (15° / 15°)"))
    }

    @Test fun latinModeIsUntouched() {
        assertEquals("Bomdod vaqti", UzbekScript.display("Bomdod vaqti", false))
        assertEquals("Бомдод вақти", UzbekScript.display("Bomdod vaqti", true))
    }

    @Test fun quranTransliterationBecomesCyrillic() {
        assertEquals("Аллоҳу лаа илааҳа иллаа ҳувал ҳайюл қойюм", c("Allohu laa ilaaha illaa huval hayyul qoyyum"))
        assertEquals("Қул ҳуваллоҳу аҳад", c("Qul huvallohu ahad"))
    }
}
