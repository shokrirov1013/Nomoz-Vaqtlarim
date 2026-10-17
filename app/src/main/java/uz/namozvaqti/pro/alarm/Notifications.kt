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

object Notifications {
    const val CH_REMINDER = "reminders"
    const val CH_PLAYBACK = "playback"
    const val CH_ALARM = "alarm"
    const val CH_UPDATE = "update"

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
            vibrationPattern = longArrayOf(0, 300, 200, 300)
        })
        nm.createNotificationChannel(NotificationChannel(CH_PLAYBACK, "Ovozli xabar", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Eslatma ovozi ijro etilayotganda"
            setSound(null, null)
        })
        nm.createNotificationChannel(NotificationChannel(CH_ALARM, "Budilnik (Bomdod, Tahajjud)", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Bomdod va Tahajjud budilnigi"
            setSound(null, null)
            enableVibration(false)
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

    fun postReminder(ctx: Context, event: ReminderEvent, text: String) {
        val title = when (event.kind) {
            ReminderKind.PRE -> "${event.prayer.title} — vaqt yaqinlashdi"
            ReminderKind.MAIN -> "${event.prayer.title} vaqti"
            ReminderKind.FOLLOWUP -> "${event.prayer.title} — eslatma"
        }
        val b = NotificationCompat.Builder(ctx, CH_REMINDER)
            .setSmallIcon(R.drawable.ic_stat_namoz)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(ctx, event.prayer.ordinal))
        if (event.kind != ReminderKind.PRE) {
            val done = Intent(ctx, ActionReceiver::class.java).setAction(Act.MARK_DONE)
                .putExtra(Extra.DATE, event.date.toString()).putExtra(Extra.PRAYER, event.prayer.key)
            b.addAction(
                0, "Bajarildi",
                PendingIntent.getBroadcast(
                    ctx, ReminderEvent.requestCode(event.date, event.prayer, 9), done,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
        }
        notify(ctx, reminderId(event.prayer), b.build())
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
