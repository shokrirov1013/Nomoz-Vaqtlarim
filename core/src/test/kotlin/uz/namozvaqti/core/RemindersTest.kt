package uz.namozvaqti.core

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RemindersTest {
    private val date = LocalDate.of(2026, 10, 16) // juma
    private val policy = ReminderPolicy()
    private val dhuhr = LocalTime.of(12, 25)
    private val events = ReminderPlanner.plan(date, Prayer.DHUHR, dhuhr, policy)

    @Test fun planIsPreMainAndExactlyThreeFollowups() {
        assertEquals(5, events.size)
        assertEquals(listOf(0, 1, 2, 3, 4), events.map { it.stage })
        assertEquals(
            listOf("12:20", "12:25", "12:35", "12:45", "12:55"),
            events.map { it.at.toLocalTime().toString() },
        )
        assertEquals(3, events.count { it.kind == ReminderKind.FOLLOWUP })
    }

    @Test fun markedDoneCancelsEverything() {
        val now = LocalDateTime.of(date, LocalTime.of(12, 26))
        assertEquals(emptyList(), ReminderPlanner.pending(events, now, done = true))
        events.forEach { assertFalse(ReminderPlanner.shouldDeliver(it, it.at, true, -1, policy)) }
    }

    @Test fun pendingOnlyFutureEvents() {
        val now = LocalDateTime.of(date, LocalTime.of(12, 36))
        assertEquals(listOf(3, 4), ReminderPlanner.pending(events, now, false).map { it.stage })
    }

    @Test fun recentMissedReminderIsRestoredAfterReboot() {
        // Telefon 12:27 da qayta yondi: 12:25 (asosiy) hali kechikish oynasida -> tiklanadi, 12:20 (oldindan) esa yo'q.
        val now = LocalDateTime.of(date, LocalTime.of(12, 27))
        val s = ReminderPlanner.schedulable(events, now, false, -1, policy)
        assertEquals(listOf(1, 2, 3, 4), s.map { it.stage })
        // Asosiy allaqachon ko'rsatilgan bo'lsa, qayta tiklanmaydi.
        assertEquals(listOf(2, 3, 4), ReminderPlanner.schedulable(events, now, false, 1, policy).map { it.stage })
        // Belgilangan namoz uchun hech narsa.
        assertEquals(emptyList(), ReminderPlanner.schedulable(events, now, true, -1, policy))
        // Juda kech: eski eslatmalar tashlanadi, faqat kelajak qoladi.
        val late = LocalDateTime.of(date, LocalTime.of(12, 50))
        // 12:45 (3-bosqich) hali oynada, 12:55 (4) kelajakda; 12:20/12:25/12:35 tashlanadi.
        assertEquals(listOf(3, 4), ReminderPlanner.schedulable(events, late, false, -1, policy).map { it.stage })
    }

    @Test fun onlyOneRecentEventIsRestored() {
        val now = LocalDateTime.of(date, LocalTime.of(12, 36)) // 12:35 (1-qayta) ham, 12:25 ham oynada emas/oynada
        val s = ReminderPlanner.schedulable(events, now, false, -1, policy)
        assertEquals(1, s.count { !it.at.isAfter(now) })
    }

    @Test fun followupDisabledGivesNoFollowups() {
        val p = policy.copy(followupEnabled = false)
        assertEquals(listOf(0, 1), ReminderPlanner.plan(date, Prayer.ASR, LocalTime.of(16, 0), p).map { it.stage })
    }

    @Test fun customIntervalAndCount() {
        val p = policy.copy(followupIntervalMinutes = 15, followupCount = 2, preEnabled = false)
        val e = ReminderPlanner.plan(date, Prayer.ASR, LocalTime.of(16, 0), p)
        assertEquals(listOf("16:00", "16:15", "16:30"), e.map { it.at.toLocalTime().toString() })
    }

    @Test fun duplicateDeliveryIsBlocked() {
        val e = events[2]
        assertTrue(ReminderPlanner.shouldDeliver(e, e.at.plusSeconds(5), false, 1, policy))
        assertFalse(ReminderPlanner.shouldDeliver(e, e.at.plusSeconds(6), false, 2, policy))
        assertFalse(ReminderPlanner.shouldDeliver(e, e.at.plusSeconds(6), false, 3, policy))
    }

    @Test fun staleAlarmIsDropped() {
        val e = events[2]
        assertFalse(ReminderPlanner.shouldDeliver(e, e.at.plusMinutes(30), false, 1, policy))
        assertTrue(ReminderPlanner.shouldDeliver(e, e.at.plusMinutes(2), false, 1, policy))
    }

    @Test fun requestCodesUniquePerDayPrayerStage() {
        val codes = mutableSetOf<Int>()
        for (dayOffset in 0..3) for (p in Prayer.entries) for (s in 0..ReminderPolicy.MAX_STAGE) {
            assertTrue(codes.add(ReminderEvent.requestCode(date.plusDays(dayOffset.toLong()), p, s)))
        }
    }

    @Test fun otherDaysDoNotCollide() {
        val today = ReminderEvent.requestCode(date, Prayer.DHUHR, 1)
        val tomorrow = ReminderEvent.requestCode(date.plusDays(1), Prayer.DHUHR, 1)
        assertTrue(today != tomorrow)
    }

    @Test fun textsMatchSpec() {
        val fri = LocalDate.of(2026, 10, 16); val mon = LocalDate.of(2026, 10, 12)
        assertEquals("Bomdod vaqti bo'ldi. Namozga turing.", PrayerTexts.mainText(Prayer.FAJR, mon, Gender.MALE))
        assertEquals("Peshin vaqti bo'ldi, kechiktirmang.", PrayerTexts.mainText(Prayer.DHUHR, mon, Gender.MALE))
        assertEquals("Asr vaqti bo'ldi, kechiktirmang.", PrayerTexts.mainText(Prayer.ASR, mon, Gender.FEMALE))
        assertEquals("Shom vaqti bo'ldi, kechiktirmang.", PrayerTexts.mainText(Prayer.MAGHRIB, mon, Gender.FEMALE))
        assertEquals("Xufton vaqti bo'ldi, kechiktirmang.", PrayerTexts.mainText(Prayer.ISHA, mon, Gender.MALE))
        assertEquals("Juma vaqti bo'ldi.", PrayerTexts.mainText(Prayer.DHUHR, fri, Gender.MALE))
        assertEquals("Peshin vaqti bo'ldi, kechiktirmang.", PrayerTexts.mainText(Prayer.DHUHR, fri, Gender.FEMALE))
        assertEquals("Asr vaqti bo'ldi, kechiktirmang.", PrayerTexts.mainText(Prayer.ASR, fri, Gender.MALE))
        assertEquals("Peshin namozini bajarishni unutmang.", PrayerTexts.followupText(Prayer.DHUHR, 1, 3))
        assertEquals("Peshin namozi hali belgilanmagan. Eslatmani tekshiring.", PrayerTexts.followupText(Prayer.DHUHR, 2, 3))
        assertEquals("Peshin namozi uchun oxirgi eslatma.", PrayerTexts.followupText(Prayer.DHUHR, 3, 3))
        assertEquals("Asr namoziga besh daqiqa qoldi. Iltimos, o'qib oling.", PrayerTexts.preText(Prayer.ASR, 5))
    }
}
