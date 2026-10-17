package uz.namozvaqti.pro.alarm

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import uz.namozvaqti.pro.ServiceLocator
import uz.namozvaqti.pro.update.UpdateWorker
import java.util.concurrent.TimeUnit

/** Namoz vaqtlarini yangilaydi va eslatmalarni qayta rejalashtiradi (alarmlar yo'qolgan bo'lsa tiklaydi). */
class RefreshWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val sl = ServiceLocator.get(applicationContext)
        if (!sl.settings.value.onboardingDone) return Result.success()
        sl.times.refresh()
        Scheduler.rescheduleAll(applicationContext)
        return Result.success()
    }
}

object WorkScheduler {
    fun ensure(ctx: Context) {
        val wm = WorkManager.getInstance(ctx)
        wm.enqueueUniquePeriodicWork(
            "refresh-periodic", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<RefreshWorker>(6, TimeUnit.HOURS).build(),
        )
        wm.enqueueUniquePeriodicWork(
            "update-check", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<UpdateWorker>(12, TimeUnit.HOURS).build(),
        )
    }

    fun refreshNow(ctx: Context) {
        WorkManager.getInstance(ctx).enqueueUniqueWork(
            "refresh-now", ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<RefreshWorker>().build(),
        )
    }
}
