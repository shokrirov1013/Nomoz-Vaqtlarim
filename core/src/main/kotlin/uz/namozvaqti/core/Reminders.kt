package uz.namozvaqti.core

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class ReminderPolicy(
    val preEnabled: Boolean = true,
    val preMinutes: Int = 5,
    val followupEnabled: Boolean = true,
    val followupIntervalMinutes: Int = 10,
    val followupCount: Int = 3,
) {
    val followups: Int get() = if (followupEnabled) followupCount.coerceIn(0, MAX_FOLLOWUPS) else 0

    companion object {
        const val MAX_FOLLOWUPS = 6
        const val MAX_STAGE = MAX_FOLLOWUPS + 1
    }
}

enum class ReminderKind { PRE, MAIN, FOLLOWUP }

/**
 * Bitta rejalashtirilgan eslatma.
 * stage: 0 = oldindan (5 daq. oldin), 1 = namoz vaqti, 2.. = qayta eslatmalar (1-, 2-, 3-).
 */
data class ReminderEvent(
    val date: LocalDate,
    val prayer: Prayer,
    val stage: Int,
    val at: LocalDateTime,
) {
    val kind: ReminderKind
        get() = when (stage) {
            0 -> ReminderKind.PRE
            1 -> ReminderKind.MAIN
            else -> ReminderKind.FOLLOWUP
        }

    /** 1 dan boshlanadigan qayta eslatma tartibi (faqat FOLLOWUP uchun). */
    val followupIndex: Int get() = stage - 1

    /** Sana + namoz + bosqich bo'yicha o'zgarmas kod: bir xil eslatma ikki marta qo'yilmaydi. */
    val requestCode: Int get() = requestCode(date, prayer, stage)

    companion object {
        fun requestCode(date: LocalDate, prayer: Prayer, stage: Int): Int =
            (date.toEpochDay() % 100_000).toInt() * 100 + prayer.ordinal * 10 + stage
    }
}

object ReminderPlanner {
    /** Bir namoz uchun barcha eslatmalar (oldindan + asosiy + qayta eslatmalar). */
    fun plan(date: LocalDate, prayer: Prayer, time: LocalTime, policy: ReminderPolicy): List<ReminderEvent> {
        val main = LocalDateTime.of(date, time)
        val events = mutableListOf<ReminderEvent>()
        if (policy.preEnabled && policy.preMinutes > 0) {
            events += ReminderEvent(date, prayer, 0, main.minusMinutes(policy.preMinutes.toLong()))
        }
        events += ReminderEvent(date, prayer, 1, main)
        for (i in 1..policy.followups) {
            events += ReminderEvent(date, prayer, 1 + i, main.plusMinutes(policy.followupIntervalMinutes.toLong() * i))
        }
        return events
    }

    /** Hali kelmagan eslatmalar (bajarilgan namoz uchun hech narsa qolmaydi). */
    fun pending(events: List<ReminderEvent>, now: LocalDateTime, done: Boolean): List<ReminderEvent> =
        if (done) emptyList() else events.filter { it.at.isAfter(now) }

    /**
     * Rejalashtirish uchun: kelajakdagi eslatmalar + (telefon o'chiq/qayta yonayotgan bo'lsa) yaqinda o'tib ketgan
     * eng so'nggi bitta eslatma. Bir vaqtda ikkita eski eslatma bir-biriga urilib ketmasligi uchun faqat eng so'nggisi olinadi.
     */
    fun schedulable(
        events: List<ReminderEvent>,
        now: LocalDateTime,
        done: Boolean,
        firedStage: Int,
        policy: ReminderPolicy,
    ): List<ReminderEvent> {
        if (done) return emptyList()
        val future = events.filter { it.at.isAfter(now) && it.stage > firedStage }
        val recent = events
            .filter { !it.at.isAfter(now) && shouldDeliver(it, now, false, firedStage, policy) }
            .maxByOrNull { it.stage }
        return listOfNotNull(recent) + future
    }

    /**
     * Alarm ishga tushganda: haqiqatan ham ko'rsatish kerakmi?
     * - namoz belgilangan bo'lsa — yo'q;
     * - shu bosqich (yoki keyingisi) allaqachon ko'rsatilgan bo'lsa — yo'q (takror oldi olinadi);
     * - juda kech qolgan eski alarm (masalan, telefon o'chiq bo'lgan) — yo'q.
     */
    fun shouldDeliver(
        event: ReminderEvent,
        now: LocalDateTime,
        done: Boolean,
        firedStage: Int,
        policy: ReminderPolicy,
    ): Boolean {
        if (done) return false
        if (event.stage <= firedStage) return false
        val lateWindow = when (event.kind) {
            ReminderKind.PRE -> (policy.preMinutes - 1).coerceAtLeast(1)
            else -> (policy.followupIntervalMinutes - 1).coerceAtLeast(2)
        }
        return !now.isAfter(event.at.plusMinutes(lateWindow.toLong()))
    }
}
