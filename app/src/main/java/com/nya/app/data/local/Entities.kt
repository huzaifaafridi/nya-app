package com.nya.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nya.app.data.ChatMessage
import com.nya.app.data.Companion
import com.nya.app.data.DefaultCaptureQuality
import com.nya.app.data.DeviceInfo
import com.nya.app.data.Gender
import com.nya.app.data.Memory
import com.nya.app.data.Profile
import com.nya.app.data.Scene
import java.util.UUID

@Entity(indices = [Index("uid"), Index("uid", "remoteId", unique = true)])
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uid: String,
    val title: String,
    val place: String,
    val note: String,
    val scene: String,
    val clipSeconds: Int?,
    val mediaUri: String?,
    val createdAt: Long,
    val remoteId: String = UUID.randomUUID().toString()
)

@Entity(indices = [Index("uid"), Index("uid", "remoteId", unique = true)])
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uid: String,
    val text: String,
    val mine: Boolean,
    val createdAt: Long,
    val remoteId: String = UUID.randomUUID().toString()
)

@Entity
data class DeviceEntity(
    @PrimaryKey val uid: String,
    val name: String,
    val link: String,
    val battery: Float,
    val storageUsedGb: Float,
    val storageTotalGb: Float,
    val firmware: String,
    val latestFirmware: String?,
    val autoSync: Boolean = true,
    val captureQuality: Int = DefaultCaptureQuality,
    val ledIndicator: Boolean = true,
    val lost: Boolean = false,
    val lostAt: Long? = null
)

/** One row per account; [updatedAt] is the epoch ms of the last user edit, 0 if never edited. */
@Entity
data class ProfileEntity(
    @PrimaryKey val uid: String,
    val name: String,
    val companion: String,
    val skin: Int,
    val gender: String,
    val voice: Int,
    val notifications: Boolean,
    val updatedAt: Long
)

@Entity
data class SeedMark(@PrimaryKey val uid: String)

fun MemoryEntity.toMemory() = Memory(
    id, title, place, note, Scene.entries.firstOrNull { it.name == scene } ?: Scene.Dusk,
    createdAt, clipSeconds, mediaUri
)

fun ChatMessageEntity.toMessage() = ChatMessage(id, text, mine)

fun DeviceEntity.toDeviceInfo() = DeviceInfo(
    name, link, battery, storageUsedGb, storageTotalGb, firmware, latestFirmware,
    autoSync = autoSync, captureQuality = captureQuality, ledIndicator = ledIndicator, lost = lost, lostAt = lostAt
)

fun ProfileEntity.toProfile(): Profile {
    val d = Profile()
    return Profile(
        name, Companion.entries.firstOrNull { it.name == companion } ?: d.companion, skin,
        Gender.entries.firstOrNull { it.name == gender } ?: d.gender, voice, notifications
    )
}

fun Profile.toEntity(uid: String, updatedAt: Long) =
    ProfileEntity(uid, name, companion.name, skin, gender.name, voice, notifications, updatedAt)
