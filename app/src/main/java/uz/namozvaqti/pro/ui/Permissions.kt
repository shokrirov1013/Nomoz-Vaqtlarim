package uz.namozvaqti.pro.ui

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import uz.namozvaqti.pro.alarm.Scheduler
import uz.namozvaqti.pro.data.LocationHelper

object Perms {
    fun notifications(ctx: Context): Boolean = NotificationManagerCompat.from(ctx).areNotificationsEnabled()

    fun exactAlarms(ctx: Context): Boolean = Scheduler.canScheduleExact(ctx)

    fun location(ctx: Context): Boolean = LocationHelper.hasPermission(ctx)

    fun batteryUnrestricted(ctx: Context): Boolean =
        (ctx.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(ctx.packageName)

    fun fullScreen(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 34 || (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).canUseFullScreenIntent()

    fun needsNotificationRuntimePermission(ctx: Context): Boolean =
        Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

    fun openExactAlarmSettings(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 31) {
            ctx.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    fun openNotificationSettings(ctx: Context) {
        ctx.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    fun requestIgnoreBattery(ctx: Context) {
        ctx.startActivity(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    fun openFullScreenSettings(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 34) {
            ctx.startActivity(
                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${ctx.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    fun openTtsSettings(ctx: Context) {
        val intents = listOf(
            Intent("com.android.settings.TTS_SETTINGS"),
            Intent(android.speech.tts.TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA),
            Intent(Settings.ACTION_SETTINGS),
        )
        for (i in intents) {
            try {
                ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return
            } catch (_: Exception) {
            }
        }
    }
}
