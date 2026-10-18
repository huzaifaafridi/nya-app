package com.nya.app.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import com.nya.app.data.Scene
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId

@Dao
interface MemoryDao {
    @Query("SELECT * FROM MemoryEntity WHERE uid = :uid ORDER BY createdAt DESC, id DESC")
    fun observe(uid: String): Flow<List<MemoryEntity>>

    @Query("DELETE FROM MemoryEntity WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
abstract class ChatDao {
    @Query("SELECT * FROM ChatMessageEntity WHERE uid = :uid ORDER BY createdAt, id")
    abstract fun observe(uid: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM ChatMessageEntity WHERE uid = :uid ORDER BY createdAt, id")
    abstract suspend fun history(uid: String): List<ChatMessageEntity>

    @Query("SELECT COUNT(*) FROM ChatMessageEntity WHERE uid = :uid")
    protected abstract suspend fun count(uid: String): Int

    @Insert
    abstract suspend fun insert(message: ChatMessageEntity)

    @Transaction
    open suspend fun insertIfEmpty(message: ChatMessageEntity) {
        if (count(message.uid) == 0) insert(message)
    }
}

@Dao
interface DeviceDao {
    @Query("SELECT * FROM DeviceEntity WHERE uid = :uid")
    fun observe(uid: String): Flow<DeviceEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfMissing(device: DeviceEntity)

    @Query("UPDATE DeviceEntity SET battery = :battery, storageUsedGb = :used, storageTotalGb = :total, firmware = :firmware WHERE uid = :uid")
    suspend fun updateReadings(uid: String, battery: Float, used: Float, total: Float, firmware: String)
}

/** Fills a new account with demo memories and a paired headband, once. */
@Dao
abstract class SeedDao {
    @Query("SELECT COUNT(*) FROM SeedMark WHERE uid = :uid")
    protected abstract suspend fun marks(uid: String): Int

    @Insert
    protected abstract suspend fun insertMemories(items: List<MemoryEntity>)

    @Insert
    protected abstract suspend fun insertDevice(device: DeviceEntity)

    @Insert
    protected abstract suspend fun insertMark(mark: SeedMark)

    @Transaction
    open suspend fun seed(uid: String) {
        if (marks(uid) > 0) return
        insertMemories(demoMemories(uid))
        insertDevice(DeviceEntity(uid, "NYA F1", "Wi-Fi and Bluetooth", 0.82f, 4.1f, 16f, "1.1.4", "1.2.0"))
        insertMark(SeedMark(uid))
    }

    private fun demoMemories(uid: String): List<MemoryEntity> {
        fun at(daysAgo: Long, hour: Int, minute: Int) = LocalDate.now().minusDays(daysAgo).atTime(hour, minute)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        fun memory(title: String, place: String, note: String, scene: Scene, createdAt: Long, clip: Int? = null) =
            MemoryEntity(uid = uid, title = title, place = place, note = note, scene = scene.name,
                clipSeconds = clip, mediaUri = null, createdAt = createdAt)
        return listOf(
            memory("Sunset at the pier", "Karachi",
                "Golden hour by the water. You said it felt like the day finally slowed down.", Scene.Sea, at(0, 18, 42), 8),
            memory("Beach day", "Sea View",
                "Windy, cold water, and the best chai afterwards.", Scene.Mount, at(1, 16, 15)),
            memory("Late night idea", "Home",
                "A voice note about making memories searchable by feeling.", Scene.Dusk, at(2, 23, 48)),
            memory("Morning walk", "Park",
                "30 minutes, no phone. You noticed the birds near the east gate.", Scene.Forest, at(3, 7, 5), 6),
            memory("Coffee with Ali", "Clifton",
                "Talked about the app launch. Ali suggested a softer onboarding.", Scene.Yoga, at(4, 11, 10)),
            memory("Family dinner", "Home",
                "Everyone together. Biryani, lots of laughing.", Scene.City, at(5, 20, 30))
        )
    }
}

@Database(
    entities = [MemoryEntity::class, ChatMessageEntity::class, DeviceEntity::class, SeedMark::class],
    version = 1,
    exportSchema = false
)
abstract class NyaDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun chatDao(): ChatDao
    abstract fun deviceDao(): DeviceDao
    abstract fun seedDao(): SeedDao
}
