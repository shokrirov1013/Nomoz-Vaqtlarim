package uz.namozvaqti.core

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** Besh vaqt namoz. Quyosh chiqishi namoz emas, shuning uchun bu yerda yo'q. */
enum class Prayer(val key: String, val title: String) {
    FAJR("fajr", "Bomdod"),
    DHUHR("dhuhr", "Peshin"),
    ASR("asr", "Asr"),
    MAGHRIB("maghrib", "Shom"),
    ISHA("isha", "Xufton");

    companion object {
        fun fromKey(key: String?): Prayer? = entries.firstOrNull { it.key == key }
    }
}

enum class Gender(val key: String) {
    MALE("male"), FEMALE("female");

    companion object {
        fun fromKey(key: String?): Gender = entries.firstOrNull { it.key == key } ?: MALE
    }
}

data class PrayerTimes(
    val date: LocalDate,
    val fajr: LocalTime,
    val sunrise: LocalTime,
    val dhuhr: LocalTime,
    val asr: LocalTime,
    val maghrib: LocalTime,
    val isha: LocalTime,
) {
    fun of(prayer: Prayer): LocalTime = when (prayer) {
        Prayer.FAJR -> fajr
        Prayer.DHUHR -> dhuhr
        Prayer.ASR -> asr
        Prayer.MAGHRIB -> maghrib
        Prayer.ISHA -> isha
    }

    fun shiftedByMinutes(minutes: Long): PrayerTimes = copy(
        fajr = fajr.plusMinutes(minutes),
        sunrise = sunrise.plusMinutes(minutes),
        dhuhr = dhuhr.plusMinutes(minutes),
        asr = asr.plusMinutes(minutes),
        maghrib = maghrib.plusMinutes(minutes),
        isha = isha.plusMinutes(minutes),
    )

    /** Vaqtlar kun ichida o'sib borishini tekshiradi (buzuq manba ma'lumotini rad etish uchun). */
    fun isSane(): Boolean =
        fajr < sunrise && sunrise < dhuhr && dhuhr < asr && asr < maghrib && maghrib < isha
}

/** Ovozli xabar matnlari (talab bo'yicha aynan shu so'zlar). */
object PrayerTexts {
    fun isFridayMale(prayer: Prayer, date: LocalDate, gender: Gender): Boolean =
        prayer == Prayer.DHUHR && date.dayOfWeek == DayOfWeek.FRIDAY && gender == Gender.MALE

    fun mainText(prayer: Prayer, date: LocalDate, gender: Gender): String = when {
        isFridayMale(prayer, date, gender) -> "Juma vaqti bo'ldi."
        prayer == Prayer.FAJR -> "Bomdod vaqti bo'ldi. Namozga turing."
        else -> "${prayer.title} vaqti bo'ldi, kechiktirmang."
    }

    fun preText(prayer: Prayer, minutes: Int): String =
        "${prayer.title} namoziga ${minutesWord(minutes)} qoldi. Iltimos, o'qib oling."

    /**
     * [index] 1 dan [total] gacha. Oxirgisi doim "oxirgi eslatma".
     * Standart 3 ta: unutmang -> hali belgilanmagan -> oxirgi eslatma.
     */
    fun followupText(prayer: Prayer, index: Int, total: Int): String = when {
        index >= total -> "${prayer.title} namozi uchun oxirgi eslatma."
        index % 2 == 1 -> "${prayer.title} namozini bajarishni unutmang."
        else -> "${prayer.title} namozi hali belgilanmagan. Eslatmani tekshiring."
    }

    private fun minutesWord(m: Int): String {
        val words = mapOf(
            1 to "bir", 2 to "ikki", 3 to "uch", 4 to "to'rt", 5 to "besh",
            10 to "o'n", 15 to "o'n besh", 20 to "yigirma", 30 to "o'ttiz",
        )
        return "${words[m] ?: m.toString()} daqiqa"
    }
}
