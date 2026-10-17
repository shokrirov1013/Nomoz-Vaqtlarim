package uz.namozvaqti.pro

import android.content.Context
import uz.namozvaqti.pro.data.AppDatabase
import uz.namozvaqti.pro.data.SettingsStore
import uz.namozvaqti.pro.data.TimesRepository

/** Oddiy bir martalik obyektlar to'plami (ilova, xizmatlar va qabul qiluvchilar uchun umumiy). */
class ServiceLocator private constructor(context: Context) {
    val db: AppDatabase = AppDatabase.create(context)
    val settings = SettingsStore(context)
    val times = TimesRepository(db, settings)

    companion object {
        @Volatile private var instance: ServiceLocator? = null

        fun get(context: Context): ServiceLocator =
            instance ?: synchronized(this) {
                instance ?: ServiceLocator(context.applicationContext).also { instance = it }
            }
    }
}
