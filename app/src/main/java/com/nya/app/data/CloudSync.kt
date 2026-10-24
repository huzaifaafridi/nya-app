package com.nya.app.data

import kotlinx.coroutines.flow.Flow

data class MemoryDoc(
    val remoteId: String,
    val title: String,
    val place: String,
    val note: String,
    val scene: String,
    val clipSeconds: Int?,
    val mediaRef: String?,
    val createdAt: Long
)

data class MessageDoc(val remoteId: String, val text: String, val mine: Boolean, val createdAt: Long)

data class DeviceDoc(
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

data class ProfileDoc(
    val name: String,
    val companion: String,
    val skin: Int,
    val gender: String,
    val voice: Int,
    val notifications: Boolean,
    val updatedAt: Long
)

/** [doc] is null when the profile is missing; [fromServer] is false while it comes from the on-device cache. */
data class ProfileSnapshot(val doc: ProfileDoc?, val fromServer: Boolean)

/** [fromServer] is false while the items come from the on-device cache and may be out of date. */
data class RemoteSnapshot<T>(val items: List<T>, val fromServer: Boolean)

/** The per-user cloud copy of the app's data; the swap point for a different backend. */
interface CloudSync {
    /** Refreshes the user's activity and returns true, or returns false if the user does not exist yet. Throws if the server cannot be reached. */
    suspend fun touchUser(uid: String): Boolean

    /** Creates the user; issued after the first upload, so an existing user implies that upload was queued. */
    suspend fun createUser(uid: String)

    fun memories(uid: String): Flow<RemoteSnapshot<MemoryDoc>>
    fun messages(uid: String): Flow<List<MessageDoc>>
    fun device(uid: String): Flow<DeviceDoc?>
    fun profile(uid: String): Flow<ProfileSnapshot>

    // Writes are queued and retried by the backend, so they never wait for the network.
    fun putMemory(uid: String, doc: MemoryDoc)
    fun deleteMemory(uid: String, remoteId: String)
    fun putMessage(uid: String, doc: MessageDoc)
    fun putDevice(uid: String, doc: DeviceDoc)
    fun putProfile(uid: String, doc: ProfileDoc)
}
