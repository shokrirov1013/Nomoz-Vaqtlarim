package uz.namozvaqti.core

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

/** Juma kuni 13:15 dagi tabrik: "<Ism>, Juma bilan tabriklayman". */
object Juma {
    val TIME: LocalTime = LocalTime.of(13, 15)

    /** [now] dan keyingi birinchi juma 13:15 (aynan 13:15 bo'lsa — keyingi haftaga). */
    fun next(now: LocalDateTime): LocalDateTime {
        var d = now.toLocalDate()
        while (d.dayOfWeek != DayOfWeek.FRIDAY || !LocalDateTime.of(d, TIME).isAfter(now)) d = d.plusDays(1)
        return LocalDateTime.of(d, TIME)
    }

    /** Bildirishnoma va ovoz uchun matn. Ism bo'sh bo'lsa, ismsiz tabrik. */
    fun text(name: String): String {
        val n = name.trim()
        return if (n.isEmpty()) "Juma bilan tabriklayman. Juma muborak bo'lsin!" else "$n, Juma bilan tabriklayman. Juma muborak bo'lsin!"
    }

    /** Ovozda avval o'qiladigan ism (TTS), keyin tayyor "juma_greet" klipi. */
    const val CLIP = "juma_greet"
    const val TITLE = "Juma muborak!"
}
