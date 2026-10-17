package uz.namozvaqti.pro.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/** Foydalanuvchi o'zi "Bajarildi" deb belgilagan namoz. Qator yo'q = belgilanmagan. */
@Entity(tableName = "prayer_log", primaryKeys = ["date", "prayer"])
data class PrayerLog(val date: String, val prayer: String, val markedAt: Long)

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val text: String,
    val createdAt: Long,
)

@Entity(tableName = "times_cache", primaryKeys = ["date", "cacheKey"])
data class TimesCache(
    val date: String,
    val cacheKey: String,
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val source: String,
    val fetchedAt: Long,
)

/** Har bir namoz uchun alohida eslatma holati: oxirgi ko'rsatilgan bosqich (takrorning oldini oladi). */
@Entity(tableName = "reminder_state", primaryKeys = ["date", "prayer"])
data class ReminderState(val date: String, val prayer: String, val firedStage: Int, val lastFiredAt: Long)

@Entity(tableName = "tahajjud")
data class TahajjudDay(
    @PrimaryKey val dayOfWeek: Int,
    val enabled: Boolean,
    val hour: Int,
    val minute: Int,
)

@Dao
interface PrayerLogDao {
    @Query("SELECT * FROM prayer_log WHERE date BETWEEN :from AND :to")
    fun observeRange(from: String, to: String): Flow<List<PrayerLog>>

    @Query("SELECT * FROM prayer_log WHERE date BETWEEN :from AND :to")
    suspend fun range(from: String, to: String): List<PrayerLog>

    @Query("SELECT COUNT(*) FROM prayer_log WHERE date = :date AND prayer = :prayer")
    suspend fun count(date: String, prayer: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: PrayerLog)

    @Query("DELETE FROM prayer_log WHERE date = :date AND prayer = :prayer")
    suspend fun delete(date: String, prayer: String)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE date BETWEEN :from AND :to ORDER BY createdAt")
    fun observeRange(from: String, to: String): Flow<List<Note>>

    @Insert
    suspend fun insert(note: Note): Long

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface TimesCacheDao {
    @Query("SELECT * FROM times_cache WHERE date = :date AND cacheKey = :key LIMIT 1")
    suspend fun get(date: String, key: String): TimesCache?

    @Query("SELECT * FROM times_cache WHERE cacheKey = :key ORDER BY fetchedAt DESC LIMIT 1")
    suspend fun latest(key: String): TimesCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TimesCache>)

    @Query("DELETE FROM times_cache WHERE date < :before")
    suspend fun deleteBefore(before: String)
}

@Dao
interface ReminderStateDao {
    @Query("SELECT * FROM reminder_state WHERE date = :date AND prayer = :prayer LIMIT 1")
    suspend fun get(date: String, prayer: String): ReminderState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: ReminderState)

    @Query("DELETE FROM reminder_state WHERE date < :before")
    suspend fun deleteBefore(before: String)
}

@Dao
interface TahajjudDao {
    @Query("SELECT * FROM tahajjud ORDER BY dayOfWeek")
    fun observeAll(): Flow<List<TahajjudDay>>

    @Query("SELECT * FROM tahajjud ORDER BY dayOfWeek")
    suspend fun all(): List<TahajjudDay>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(day: TahajjudDay)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(days: List<TahajjudDay>)
}

/**
 * MUHIM: sxema o'zgarsa version'ni oshiring va Migration qo'shing.
 * fallbackToDestructiveMigration() ATAYLAB ishlatilmagan — yangilanishda tarix o'chib ketmasligi uchun.
 */
@Database(
    entities = [PrayerLog::class, Note::class, TimesCache::class, ReminderState::class, TahajjudDay::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun prayerLog(): PrayerLogDao
    abstract fun notes(): NoteDao
    abstract fun timesCache(): TimesCacheDao
    abstract fun reminderState(): ReminderStateDao
    abstract fun tahajjud(): TahajjudDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "namoz.db").build()
    }
}
