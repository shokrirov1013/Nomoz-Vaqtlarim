package uz.namozvaqti.pro

import android.app.Application
import uz.namozvaqti.pro.alarm.Notifications
import uz.namozvaqti.pro.alarm.WorkScheduler

class NamozApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        WorkScheduler.ensure(this)
    }
}
