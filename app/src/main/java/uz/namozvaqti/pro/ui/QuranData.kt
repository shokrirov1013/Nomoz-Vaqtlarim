package uz.namozvaqti.pro.ui

/** Bitta oyat (yoki oyat qismi): arabcha matn va lotincha o'qilishi. */
data class Verse(val arabic: String, val translit: String)

data class Surah(
    val id: String,
    val title: String,
    val arabicTitle: String,
    val source: String,
    val verses: List<Verse>,
    val meaning: String,
    val tip: String,
)

/**
 * Qisqa suralar va Oyatal Kursi. Matn Madina mushafi imlosida. Ma'nolar qisqa va tushunarli qilib berilgan —
 * to'liq tafsir emas. Iltimos, o'qishdan oldin ishonchli mushaf bilan solishtiring.
 */
object QuranData {
    val basmala = Verse("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", "Bismillahir rohmanir rohiym")

    val all: List<Surah> = listOf(
        Surah(
            id = "kursi",
            title = "Oyatal Kursi",
            arabicTitle = "آيَةُ الْكُرْسِيِّ",
            source = "Baqara surasi, 255-oyat",
            verses = listOf(
                Verse("اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ", "Allohu laa ilaaha illaa huval hayyul qoyyum"),
                Verse("لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ", "Laa ta'xuzuhu sinatun va laa nawm"),
                Verse("لَهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ", "Lahu maa fis samaavaati va maa fil arz"),
                Verse("مَنْ ذَا الَّذِي يَشْفَعُ عِنْدَهُ إِلَّا بِإِذْنِهِ", "Man zallaziy yashfa'u 'indahu illaa bi iznih"),
                Verse("يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ", "Ya'lamu maa bayna aydiyhim va maa xolfahum"),
                Verse("وَلَا يُحِيطُونَ بِشَيْءٍ مِنْ عِلْمِهِ إِلَّا بِمَا شَاءَ", "Va laa yuhiytuuna bishay'im min 'ilmihi illaa bimaa shaa'"),
                Verse("وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ", "Vasi'a kursiyyuhus samaavaati val arz"),
                Verse("وَلَا يَئُودُهُ حِفْظُهُمَا", "Va laa ya'uuduhu hifzuhumaa"),
                Verse("وَهُوَ الْعَلِيُّ الْعَظِيمُ", "Va huval 'aliyyul 'aziym"),
            ),
            meaning = "Alloh — Undan o'zga iloh yo'q, Tirik va Qayyum zotdir. Uni mudroq ham, uyqu ham tutmaydi. " +
                "Osmonlardagi va yerdagi hamma narsa Unikidir. Uning izni bilan bo'lmasa, huzurida kim ham shafoat qila oladi? " +
                "U bandalarning oldidagi va ortidagi narsalarni biladi. Ular Uning ilmidan, U xohlagan miqdoridan boshqasini qamrab ola olmaydilar. " +
                "Uning Kursiysi osmonlar va yerni qamrab olgan. Ularni asrash Unga og'irlik qilmaydi. U Oliy va Ulug' zotdir.",
            tip = "Har farz namozdan keyin va uxlashdan oldin o'qish tavsiya etiladi.",
        ),
        Surah(
            id = "fotiha",
            title = "Fotiha surasi",
            arabicTitle = "سُورَةُ الْفَاتِحَةِ",
            source = "1-sura, 7 oyat",
            verses = listOf(
                QuranData.basmala,
                Verse("الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ", "Alhamdu lillaahi robbil 'aalamiyn"),
                Verse("الرَّحْمَٰنِ الرَّحِيمِ", "Ar-rohmaanir rohiym"),
                Verse("مَالِكِ يَوْمِ الدِّينِ", "Maaliki yavmid diyn"),
                Verse("إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ", "Iyyaaka na'budu va iyyaaka nasta'iyn"),
                Verse("اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ", "Ihdinas sirootol mustaqiym"),
                Verse(
                    "صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ",
                    "Sirootollaziyna an'amta 'alayhim, gayril mag'zuubi 'alayhim va lad-doolliyn",
                ),
            ),
            meaning = "Mehribon va rahmli Alloh nomi bilan. Hamd olamlarning Robbi Allohga xosdir. U mehribon va rahmlidir, " +
                "Qiyomat kunining egasidir. Yolg'iz Senga ibodat qilamiz va yolg'iz Sendan yordam so'raymiz. " +
                "Bizni to'g'ri yo'lga hidoyat qil — ne'mat bergan zotlaringning yo'liga, g'azabga uchraganlar va adashganlarning yo'liga emas.",
            tip = "Har rakatda o'qiladi; namozning asosiy suralaridan.",
        ),
        Surah(
            id = "ixlos",
            title = "Ixlos surasi",
            arabicTitle = "سُورَةُ الْإِخْلَاصِ",
            source = "112-sura, 4 oyat",
            verses = listOf(
                QuranData.basmala,
                Verse("قُلْ هُوَ اللَّهُ أَحَدٌ", "Qul huvallohu ahad"),
                Verse("اللَّهُ الصَّمَدُ", "Allohus samad"),
                Verse("لَمْ يَلِدْ وَلَمْ يُولَدْ", "Lam yalid va lam yuulad"),
                Verse("وَلَمْ يَكُنْ لَهُ كُفُوًا أَحَدٌ", "Va lam yakun lahuu kufuvan ahad"),
            ),
            meaning = "Ayt: «U — Alloh, yagonadir. Alloh behojatdir (hamma Unga muhtoj, U hech kimga muhtoj emas). " +
                "U tug'magan va tug'ilmagan. Va Unga hech kim teng emas.»",
            tip = "Ertalab va kechqurun uch martadan o'qish tavsiya etiladi.",
        ),
        Surah(
            id = "falaq",
            title = "Falaq surasi",
            arabicTitle = "سُورَةُ الْفَلَقِ",
            source = "113-sura, 5 oyat",
            verses = listOf(
                QuranData.basmala,
                Verse("قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ", "Qul a'uuzu birobbil falaq"),
                Verse("مِنْ شَرِّ مَا خَلَقَ", "Min sharri maa xolaq"),
                Verse("وَمِنْ شَرِّ غَاسِقٍ إِذَا وَقَبَ", "Va min sharri g'oosiqin izaa vaqob"),
                Verse("وَمِنْ شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ", "Va min sharrin naffaasaati fil 'uqod"),
                Verse("وَمِنْ شَرِّ حَاسِدٍ إِذَا حَسَدَ", "Va min sharri haasidin izaa hasad"),
            ),
            meaning = "Ayt: «Tong Robbidan panoh so'rayman — U yaratgan narsalarning yomonligidan, qorong'i bosganda tunning yomonligidan, " +
                "tugunlarga puflovchilarning yomonligidan va hasad qilgan chog'ida hasadgo'yning yomonligidan.»",
            tip = "Ertalab va kechqurun uch martadan o'qish tavsiya etiladi.",
        ),
        Surah(
            id = "nos",
            title = "Nos surasi",
            arabicTitle = "سُورَةُ النَّاسِ",
            source = "114-sura, 6 oyat",
            verses = listOf(
                QuranData.basmala,
                Verse("قُلْ أَعُوذُ بِرَبِّ النَّاسِ", "Qul a'uuzu birobbin naas"),
                Verse("مَلِكِ النَّاسِ", "Malikin naas"),
                Verse("إِلَٰهِ النَّاسِ", "Ilaahin naas"),
                Verse("مِنْ شَرِّ الْوَسْوَاسِ الْخَنَّاسِ", "Min sharril vasvaasil xonnaas"),
                Verse("الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ", "Allaziy yuvasvisu fiy suduurin naas"),
                Verse("مِنَ الْجِنَّةِ وَالنَّاسِ", "Minal jinnati van naas"),
            ),
            meaning = "Ayt: «Odamlarning Robbidan, odamlarning Podshohidan, odamlarning Ilohidan panoh so'rayman — " +
                "odamlarning ko'ksiga vasvasa soladigan, (Allohni eslanganda) yashirinib oladigan vasvasachining yomonligidan — " +
                "jinlardan ham, odamlardan ham.»",
            tip = "Ertalab va kechqurun uch martadan o'qish tavsiya etiladi.",
        ),
    )
}
