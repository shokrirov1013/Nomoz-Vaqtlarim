package uz.namozvaqti.pro

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.namozvaqti.core.City
import uz.namozvaqti.core.Gender
import uz.namozvaqti.core.Prayer
import uz.namozvaqti.core.PrayerTimes
import uz.namozvaqti.core.TahajjudRules
import uz.namozvaqti.pro.alarm.PrayerActions
import uz.namozvaqti.pro.alarm.Scheduler
import uz.namozvaqti.pro.alarm.WorkScheduler
import uz.namozvaqti.pro.data.LocationHelper
import uz.namozvaqti.pro.data.Note
import uz.namozvaqti.pro.data.PrayerLog
import uz.namozvaqti.pro.data.Settings
import uz.namozvaqti.pro.data.TahajjudDay
import uz.namozvaqti.pro.data.TimesInfo
import uz.namozvaqti.pro.update.CheckResult
import uz.namozvaqti.pro.update.ReleaseInfo
import uz.namozvaqti.pro.update.UpdateManager
import java.time.LocalDate
import java.time.LocalDateTime

sealed interface UpdateUi {
    data object Idle : UpdateUi
    data object Checking : UpdateUi
    data object UpToDate : UpdateUi
    data class Available(val release: ReleaseInfo) : UpdateUi
    data class Downloading(val release: ReleaseInfo, val progress: Float) : UpdateUi
    data class Error(val message: String) : UpdateUi
}

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx get() = getApplication<Application>()
    private val sl = ServiceLocator.get(app)

    val settings: StateFlow<Settings> = sl.settings.flow

    /** Har soniyada yangilanadi (teskari sanoq uchun). */
    val now = MutableStateFlow(LocalDateTime.now())
    private val refreshTick = MutableStateFlow(0)

    private val timesKey = combine(now.map { it.toLocalDate() }.distinctUntilChanged(), settings, refreshTick) { d, s, t ->
        Triple(d, listOf(s.lat, s.lon, s.method, s.hanafiAsr), t)
    }.distinctUntilChanged()

    val todayTimes: StateFlow<PrayerTimes?> = timesKey.mapLatest { (d, _, _) -> sl.times.timesFor(d) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val tomorrowTimes: StateFlow<PrayerTimes?> = timesKey.mapLatest { (d, _, _) -> sl.times.timesFor(d.plusDays(1)) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val timesInfo: StateFlow<TimesInfo?> = timesKey.mapLatest { sl.times.info() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val tahajjud: StateFlow<List<TahajjudDay>> = sl.db.tahajjud().observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _update = MutableStateFlow<UpdateUi>(UpdateUi.Idle)
    val update: StateFlow<UpdateUi> = _update

    init {
        viewModelScope.launch {
            while (true) {
                now.value = LocalDateTime.now()
                delay(1000)
            }
        }
        viewModelScope.launch(Dispatchers.IO) { ensureTahajjudDefaults() }
    }

    private suspend fun ensureTahajjudDefaults() {
        if (sl.db.tahajjud().all().size < 7) {
            sl.db.tahajjud().upsertAll(
                (1..7).map { TahajjudDay(it, false, TahajjudRules.DEFAULT_HOUR, TahajjudRules.DEFAULT_MINUTE) },
            )
        }
    }

    // ------------------------------------------------------------------ ishga tushish
    fun onAppStart() {
        viewModelScope.launch(Dispatchers.IO) {
            Scheduler.rescheduleAll(ctx)
            if (sl.times.refresh()) {
                refreshTick.value++
                Scheduler.rescheduleAll(ctx)
            }
        }
        WorkScheduler.ensure(ctx)
    }

    // ------------------------------------------------------------------ sozlamalar
    fun updateSettings(reschedule: Boolean = true, block: (Settings) -> Settings) {
        sl.settings.update(block)
        if (reschedule) viewModelScope.launch(Dispatchers.IO) { Scheduler.rescheduleAll(ctx) }
    }

    /** Joylashuv yoki hisoblash usuli o'zgarganda: vaqtlarni qayta olib, eslatmalarni yangilaydi. */
    fun refreshTimes() {
        viewModelScope.launch(Dispatchers.IO) {
            refreshTick.value++
            sl.times.refresh()
            refreshTick.value++
            Scheduler.rescheduleAll(ctx)
        }
    }

    fun completeOnboarding(name: String, gender: Gender) {
        updateSettings(reschedule = false) { it.copy(name = name.trim(), gender = gender, onboardingDone = true) }
        onAppStart()
    }

    /** GPS orqali joylashuvni aniqlaydi. Natija: shahar nomi yoki null (aniqlanmadi). */
    suspend fun useGpsLocation(): String? {
        val loc = LocationHelper.current(ctx) ?: return null
        val label = LocationHelper.label(ctx, loc.latitude, loc.longitude)
        updateSettings(reschedule = false) { it.copy(useGps = true, lat = loc.latitude, lon = loc.longitude, locationLabel = label) }
        refreshTimes()
        return label
    }

    fun selectCity(city: City) {
        updateSettings(reschedule = false) {
            it.copy(useGps = false, manualCity = city.name, lat = city.lat, lon = city.lon, locationLabel = city.name)
        }
        refreshTimes()
    }

    // ------------------------------------------------------------------ namoz belgilari
    fun setDone(date: LocalDate, prayer: Prayer, done: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { PrayerActions.setDone(ctx, date, prayer, done) }
    }

    fun logsInRange(from: LocalDate, to: LocalDate): Flow<List<PrayerLog>> =
        sl.db.prayerLog().observeRange(from.toString(), to.toString())

    fun doneSet(from: LocalDate, to: LocalDate): Flow<Set<Pair<LocalDate, Prayer>>> =
        logsInRange(from, to).map { list ->
            list.mapNotNull { l -> Prayer.fromKey(l.prayer)?.let { LocalDate.parse(l.date) to it } }.toSet()
        }

    suspend fun timesFor(date: LocalDate): PrayerTimes = sl.times.timesFor(date)

    // ------------------------------------------------------------------ eslatmalar (taqvim)
    fun notes(from: LocalDate, to: LocalDate): Flow<List<Note>> = sl.db.notes().observeRange(from.toString(), to.toString())

    fun addNote(date: LocalDate, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) { sl.db.notes().insert(Note(date = date.toString(), text = text.trim(), createdAt = System.currentTimeMillis())) }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { sl.db.notes().delete(id) }
    }

    // ------------------------------------------------------------------ Tahajjud
    fun setTahajjudDay(day: TahajjudDay) {
        viewModelScope.launch(Dispatchers.IO) {
            sl.db.tahajjud().upsert(day)
            Scheduler.scheduleTahajjud(ctx)
        }
    }

    fun setTahajjudDays(days: List<TahajjudDay>) {
        viewModelScope.launch(Dispatchers.IO) {
            sl.db.tahajjud().upsertAll(days)
            Scheduler.scheduleTahajjud(ctx)
        }
    }

    fun rescheduleTahajjud() {
        viewModelScope.launch(Dispatchers.IO) { Scheduler.scheduleTahajjud(ctx) }
    }

    // ------------------------------------------------------------------ yangilanish
    fun checkUpdate() {
        if (_update.value is UpdateUi.Checking || _update.value is UpdateUi.Downloading) return
        _update.value = UpdateUi.Checking
        viewModelScope.launch(Dispatchers.IO) {
            _update.value = when (val r = UpdateManager.check()) {
                is CheckResult.Available -> UpdateUi.Available(r.release)
                CheckResult.UpToDate -> UpdateUi.UpToDate
                is CheckResult.Failed -> UpdateUi.Error(r.reason)
            }
        }
    }

    /** Yuklab oladi, imzoni tekshiradi va tizim o'rnatish oynasini ochadi. */
    fun downloadAndInstall(release: ReleaseInfo) {
        if (_update.value is UpdateUi.Downloading) return
        _update.value = UpdateUi.Downloading(release, 0f)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = UpdateManager.download(ctx, release) { p -> _update.value = UpdateUi.Downloading(release, p) }
                val error = UpdateManager.install(ctx, file)
                _update.value = if (error == null) UpdateUi.Available(release) else UpdateUi.Error(error)
            } catch (e: Exception) {
                _update.value = UpdateUi.Error(e.message ?: "Yuklab bo'lmadi")
            }
        }
    }
}
