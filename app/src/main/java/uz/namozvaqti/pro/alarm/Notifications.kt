package uz.namozvaqti.pro.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import uz.namozvaqti.core.Prayer
import uz.namozvaqti.core.ReminderEvent
import uz.namozvaqti.core.ReminderKind
import uz.namozvaqti.pro.MainActivity
import uz.namozvaqti.pro.R

/** Bildirishnoma matnlari ham foydalanuvchi tanlagan yozuvda (lotin/kirill) chiqadi. */
object UzText {
    fun of(ctx: Context, text: String): String =
        uz.namozvaqti.core.UzbekScript.display(text, uz.namozvaqti.pro.ServiceLocator.get(ctx).settings.value.cyrillic)
}

object Notifications {
    const val CH_REMINDER = "reminders_v2"
    const val CH_PLAYBACK = "playback"
    const val CH_ALARM = "alarm"
    const val CH_UPDATE = "update"
    // Zaxira kanallar: foreground xizmat boshlanmasa, tizimning o'z ovozi chaladi.
    const val CH_REMINDER_SOUND = "reminders_sound"
    const val CH_ALARM_SOUND = "alarm_sound"

    const val ID_PLAYBACK = 900
    const val ID_RING = 901
    const val ID_UPDATE = 902

    fun reminderId(prayer: Prayer) = 1000 + prayer.ordinal

    fun createChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Tovushni xizmat o'zi chaladi (ting.mp3 + ovozli xabar), shuning uchun kanal ovozsiz.
        nm.createNotificationChannel(NotificationChannel(CH_REMINDER, "Namoz eslatmalari", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Namoz vaqti va qayta eslatmalar"
            setSound(null, null)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 160, 140, 160)
        })
        nm.deleteNotificationChannel("reminders")
        nm.createNotificationChannel(NotificationChannel(CH_PLAYBACK, "Ovozli xabar", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Eslatma ovozi ijro etilayotganda"
            setSound(null, null)
        })
        nm.createNotificationChannel(NotificationChannel(CH_ALARM, "Budilnik (Bomdod, Tahajjud)", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Bomdod va Tahajjud budilnigi"
            setSound(null, null)
            enableVibration(false)
        })
        nm.createNotificationChannel(NotificationChannel(CH_REMINDER_SOUND, "Namoz eslatmalari (tizim ovozi)", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Ovozli xabar ishlamasa, zaxira sifatida tizim ovozi chaladi"
            enableVibration(true)
        })
        nm.createNotificationChannel(NotificationChannel(CH_ALARM_SOUND, "Budilnik (tizim ovozi)", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Budilnik xizmati boshlanmasa, zaxira sifatida tizim budilnigi chaladi"
            setSound(
                android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM),
                android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_ALARM).build(),
            )
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 700, 500, 700, 500, 700)
        })
        nm.createNotificationChannel(NotificationChannel(CH_UPDATE, "Yangilanishlar", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Ilovaning yangi versiyasi"
        })
    }

    fun openAppIntent(ctx: Context, requestCode: Int = 0): PendingIntent =
        PendingIntent.getActivity(
            ctx, requestCode, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    fun postReminder(ctx: Context, event: ReminderEvent, text: String, systemSound: Boolean = false) {
        val title = when (event.kind) {
            ReminderKind.PRE -> "${event.prayer.title} — vaqt yaqinlashdi"
            ReminderKind.MAIN -> "${event.prayer.title} vaqti"
            ReminderKind.FOLLOWUP -> "${event.prayer.title} — eslatma"
        }
        val b = NotificationCompat.Builder(ctx, if (systemSound) CH_REMINDER_SOUND else CH_REMINDER)
            .setSmallIcon(R.drawable.ic_stat_namoz)
            .setContentTitle(UzText.of(ctx, title))
            .setContentText(UzText.of(ctx, text))
            .setStyle(NotificationCompat.BigTextStyle().bigText(UzText.of(ctx, text)))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(ctx, event.prayer.ordinal))
        if (event.kind != ReminderKind.PRE) {
            val done = Intent(ctx, ActionReceiver::class.java).setAction(Act.MARK_DONE)
                .putExtra(Extra.DATE, event.date.toString()).putExtra(Extra.PRAYER, event.prayer.key)
            b.addAction(
                0, UzText.of(ctx, "Bajarildi"),
                PendingIntent.getBroadcast(
                    ctx, ReminderEvent.requestCode(event.date, event.prayer, 9), done,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
        }
        notify(ctx, reminderId(event.prayer), b.build())
    }

    /** Budilnik xizmatini boshlab bo'lmaganda: to'liq ekranli bildirishnoma + tizim budilnik ovozi. */
    fun postRingFallback(ctx: Context, kind: RingKind) {
        val st = uz.namozvaqti.pro.ServiceLocator.get(ctx).settings.value
        val text = RingTexts.of(kind, st.gentle, st.gender)
        val full = PendingIntentFactory.alarmScreen(ctx, kind)
        val n = NotificationCompat.Builder(ctx, CH_ALARM_SOUND)
            .setSmallIcon(R.drawable.ic_stat_namoz)
            .setContentTitle(UzText.of(ctx, kind.title))
            .setContentText(UzText.of(ctx, text))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setFullScreenIntent(full, true)
            .setContentIntent(full)
            .build()
        notify(ctx, ID_RING, n)
    }

    fun notify(ctx: Context, id: Int, n: android.app.Notification) {
        try {
            val nm = NotificationManagerCompat.from(ctx)
            if (nm.areNotificationsEnabled()) nm.notify(id, n)
        } catch (_: SecurityException) {
        }
    }

    fun cancel(ctx: Context, id: Int) = NotificationManagerCompat.from(ctx).cancel(id)
}
