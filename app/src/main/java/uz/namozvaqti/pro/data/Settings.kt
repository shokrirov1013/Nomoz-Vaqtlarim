package uz.namozvaqti.pro.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import uz.namozvaqti.core.CalcMethod
import uz.namozvaqti.core.Gender
import uz.namozvaqti.core.Prayer
import uz.namozvaqti.core.ReminderPolicy

/** Ovoz manbai: ilovaning "ting" ovozi, tizim budilnigi yoki tanlangan tizim ringtoni URI'si. */
const val SOUND_BUILTIN = "builtin"
const val SOUND_DEFAULT_ALARM = "default_alarm"

data class Settings(
    val onboardingDone: Boolean = false,
    val name: String = "",
    val gender: Gender = Gender.MALE,

    val useGps: Boolean = true,
    val lat: Double = 41.3111,
    val lon: Double = 69.2797,
    val locationLabel: String = "Toshkent",
    val manualCity: String = "Toshkent",

    val method: CalcMethod = CalcMethod.UZBEKISTAN,
    val hanafiAsr: Boolean = true,
    val hijriAdjust: Int = 0,

    val prayerReminders: Map<Prayer, Boolean> = Prayer.entries.associateWith { true },
    val preEnabled: Boolean = true,
    val preMinutes: Int = 5,
    val followupEnabled: Boolean = true,
    val followupIntervalMinutes: Int = 10,
    val followupCount: Int = 3,
    val playInSilentMode: Boolean = false,
    /** Muloyim ohang: yumshoq ovoz, qisqa ting, muloyim matn, asta kuchayadigan budilnik. */
    val gentle: Boolean = true,
    val reminderVolume: Int = 70,
    val ttsLang: String = "auto",

    val fajrAlarmEnabled: Boolean = false,
    val fajrSound: String = SOUND_BUILTIN,
    val fajrVibration: Boolean = true,
    val snoozeMinutes: Int = 5,

    val tahajjudEnabled: Boolean = false,
    val tahajjudSound: String = SOUND_BUILTIN,
    val tahajjudVibration: Boolean = true,

    val autoUpdateCheck: Boolean = true,
    val autoDownloadUpdate: Boolean = true,
) {
    val policy: ReminderPolicy
        get() = ReminderPolicy(preEnabled, preMinutes, followupEnabled, followupIntervalMinutes, followupCount)
}

class SettingsStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _flow = MutableStateFlow(load())
    val flow: StateFlow<Settings> get() = _flow
    val value: Settings get() = _flow.value

    @Synchronized
    fun update(block: (Settings) -> Settings) {
        val next = block(_flow.value)
        save(next)
        _flow.value = next
    }

    private fun load(): Settings {
        val d = Settings()
        val p = prefs
        return Settings(
            onboardingDone = p.getBoolean("onboardingDone", d.onboardingDone),
            name = p.getString("name", d.name) ?: d.name,
            gender = Gender.fromKey(p.getString("gender", null)),
            useGps = p.getBoolean("useGps", d.useGps),
            lat = p.getString("latS", null)?.toDoubleOrNull() ?: d.lat,
            lon = p.getString("lonS", null)?.toDoubleOrNull() ?: d.lon,
            locationLabel = p.getString("locationLabel", d.locationLabel) ?: d.locationLabel,
            manualCity = p.getString("manualCity", d.manualCity) ?: d.manualCity,
            method = CalcMethod.fromKey(p.getString("method", null)),
            hanafiAsr = p.getBoolean("hanafiAsr", d.hanafiAsr),
            hijriAdjust = p.getInt("hijriAdjust", d.hijriAdjust),
            prayerReminders = Prayer.entries.associateWith { p.getBoolean("remind_${it.key}", true) },
            preEnabled = p.getBoolean("preEnabled", d.preEnabled),
            preMinutes = p.getInt("preMinutes", d.preMinutes),
            followupEnabled = p.getBoolean("followupEnabled", d.followupEnabled),
            followupIntervalMinutes = p.getInt("followupInterval", d.followupIntervalMinutes),
            followupCount = p.getInt("followupCount", d.followupCount),
            playInSilentMode = p.getBoolean("playInSilentMode", d.playInSilentMode),
            gentle = p.getBoolean("gentle", d.gentle),
            reminderVolume = p.getInt("reminderVolume", d.reminderVolume),
            ttsLang = p.getString("ttsLang", d.ttsLang) ?: d.ttsLang,
            fajrAlarmEnabled = p.getBoolean("fajrAlarmEnabled", d.fajrAlarmEnabled),
            fajrSound = p.getString("fajrSound", d.fajrSound) ?: d.fajrSound,
            fajrVibration = p.getBoolean("fajrVibration", d.fajrVibration),
            snoozeMinutes = p.getInt("snoozeMinutes", d.snoozeMinutes),
            tahajjudEnabled = p.getBoolean("tahajjudEnabled", d.tahajjudEnabled),
            tahajjudSound = p.getString("tahajjudSound", d.tahajjudSound) ?: d.tahajjudSound,
            tahajjudVibration = p.getBoolean("tahajjudVibration", d.tahajjudVibration),
            autoUpdateCheck = p.getBoolean("autoUpdateCheck", d.autoUpdateCheck),
            autoDownloadUpdate = p.getBoolean("autoDownloadUpdate", d.autoDownloadUpdate),
        )
    }

    private fun save(s: Settings) {
        prefs.edit().apply {
            putBoolean("onboardingDone", s.onboardingDone)
            putString("name", s.name)
            putString("gender", s.gender.key)
            putBoolean("useGps", s.useGps)
            putString("latS", s.lat.toString())
            putString("lonS", s.lon.toString())
            putString("locationLabel", s.locationLabel)
            putString("manualCity", s.manualCity)
            putString("method", s.method.key)
            putBoolean("hanafiAsr", s.hanafiAsr)
            putInt("hijriAdjust", s.hijriAdjust)
            Prayer.entries.forEach { putBoolean("remind_${it.key}", s.prayerReminders[it] ?: true) }
            putBoolean("preEnabled", s.preEnabled)
            putInt("preMinutes", s.preMinutes)
            putBoolean("followupEnabled", s.followupEnabled)
            putInt("followupInterval", s.followupIntervalMinutes)
            putInt("followupCount", s.followupCount)
            putBoolean("playInSilentMode", s.playInSilentMode)
            putBoolean("gentle", s.gentle)
            putInt("reminderVolume", s.reminderVolume)
            putString("ttsLang", s.ttsLang)
            putBoolean("fajrAlarmEnabled", s.fajrAlarmEnabled)
            putString("fajrSound", s.fajrSound)
            putBoolean("fajrVibration", s.fajrVibration)
            putInt("snoozeMinutes", s.snoozeMinutes)
            putBoolean("tahajjudEnabled", s.tahajjudEnabled)
            putString("tahajjudSound", s.tahajjudSound)
            putBoolean("tahajjudVibration", s.tahajjudVibration)
            putBoolean("autoUpdateCheck", s.autoUpdateCheck)
            putBoolean("autoDownloadUpdate", s.autoDownloadUpdate)
        }.apply()
    }
}
