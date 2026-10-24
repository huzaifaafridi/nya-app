package com.nya.app.data

import android.util.Log
import com.nya.app.data.local.ChatDao
import com.nya.app.data.local.ChatMessageEntity
import com.nya.app.data.local.DeviceDao
import com.nya.app.data.local.DeviceEntity
import com.nya.app.data.local.MemoryDao
import com.nya.app.data.local.MemoryEntity
import com.nya.app.data.local.ProfileDao
import com.nya.app.data.local.ProfileEntity
import com.nya.app.data.local.SeedDao
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private const val Tag = "NyaSync"
private const val FirstRetryMs = 5_000L
private const val MaxRetryMs = 5 * 60_000L

// SQLite binds at most 999 variables per statement on API 26-29
private const val DeleteChunk = 500

/**
 * Keeps Room (what the UI reads) and the cloud copy of the signed-in user in step.
 *
 * Cloud to Room runs from the start, so cached cloud data shows offline. Deleting rows because a memory is gone
 * from the cloud waits until [CloudSync.touchUser] has reached the server and, for a brand-new cloud account, the
 * one-time upload of the local rows has been issued; otherwise an early empty server snapshot would wipe them.
 */
class SyncManager(
    private val uid: Flow<String?>,
    private val cloud: CloudSync,
    private val media: MediaStore,
    private val memoryDao: MemoryDao,
    private val chatDao: ChatDao,
    private val deviceDao: DeviceDao,
    private val profileDao: ProfileDao,
    private val seedDao: SeedDao,
    private val scope: CoroutineScope
) {
    fun start() {
        scope.launch { uid.distinctUntilChanged().collectLatest { id -> if (id != null) sync(id) } }
    }

    private suspend fun sync(uid: String) = coroutineScope {
        // remote ids of the rows uploaded just now, which a server snapshot older than the upload must not delete
        val uploaded = CompletableDeferred<Set<String>>()
        launch { mirrorMemories(uid, uploaded) }
        launch { cloud.messages(uid).collect { chatDao.insertAll(it.map { doc -> doc.toEntity(uid) }) } }
        launch { cloud.device(uid).collect { doc -> if (doc != null) deviceDao.upsert(doc.toEntity(uid)) } }
        launch { mirrorProfile(uid) }
        uploaded.complete(registerWithRetry(uid))
    }

    /**
     * Refreshes the user, or for a brand-new cloud account uploads the local rows and only then creates the user,
     * so a user that exists always has its upload queued. Returns the memory ids uploaded.
     */
    private suspend fun registerWithRetry(uid: String): Set<String> {
        var wait = FirstRetryMs
        while (true) {
            try {
                if (cloud.touchUser(uid)) return emptySet()
                return upload(uid).also { cloud.createUser(uid) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(Tag, "registering the user failed, retrying in ${wait / 1000}s", e)
                delay(wait)
                wait = (wait * 2).coerceAtMost(MaxRetryMs)
            }
        }
    }

    /** Seeds the demo data if this phone has none yet, then pushes everything local; returns the memory ids pushed. */
    private suspend fun upload(uid: String): Set<String> {
        seedDao.seed(uid)
        val memories = memoryDao.all(uid)
        memories.forEach { cloud.putMemory(uid, it.toDoc(it.mediaUri?.let { uri -> media.publish(uri) })) }
        chatDao.history(uid).forEach { cloud.putMessage(uid, it.toDoc()) }
        deviceDao.get(uid)?.let { cloud.putDevice(uid, it.toDoc()) }
        profileDao.get(uid)?.let { cloud.putProfile(uid, it.toDoc()) }
        return memories.mapTo(HashSet()) { it.remoteId }
    }

    private suspend fun mirrorMemories(uid: String, uploaded: CompletableDeferred<Set<String>>) {
        cloud.memories(uid).collectLatest { snapshot ->
            memoryDao.insertAll(snapshot.items.map { it.toEntity(uid) })
            if (snapshot.fromServer) {
                val uploadedIds = uploaded.await()
                remoteIdsToDelete(memoryDao.remoteIds(uid), snapshot.items, uploadedIds)
                    .chunked(DeleteChunk).forEach { memoryDao.deleteByRemoteIds(uid, it) }
            }
        }
    }

    /** Last writer wins on updatedAt; applying a remote profile never pushes, and a push makes the next snapshot equal. */
    private suspend fun mirrorProfile(uid: String) {
        cloud.profile(uid).collect { snapshot ->
            val local = profileDao.get(uid)
            when (profileAction(local, snapshot.doc, snapshot.fromServer)) {
                ProfileAction.Apply -> snapshot.doc?.let { profileDao.upsertIfNewer(it.toEntity(uid)) }
                ProfileAction.Push -> local?.let { cloud.putProfile(uid, it.toDoc()) }
                ProfileAction.None -> Unit
            }
        }
    }
}

internal enum class ProfileAction { None, Apply, Push }

/** Only a server snapshot proves the cloud lacks or has an older profile, so only it can trigger a push. */
internal fun profileAction(local: ProfileEntity?, remote: ProfileDoc?, fromServer: Boolean): ProfileAction = when {
    remote != null && (local == null || remote.updatedAt > local.updatedAt) -> ProfileAction.Apply
    !fromServer || local == null -> ProfileAction.None
    remote == null || local.updatedAt > remote.updatedAt -> ProfileAction.Push
    else -> ProfileAction.None
}

/** Local ids the server no longer has, except the rows uploaded after that snapshot. */
internal fun remoteIdsToDelete(local: List<String>, remote: List<MemoryDoc>, uploaded: Set<String>): List<String> {
    val keep = remote.mapTo(HashSet(uploaded)) { it.remoteId }
    return local.filter { it !in keep }
}

internal fun MemoryEntity.toDoc(mediaRef: String?) =
    MemoryDoc(remoteId, title, place, note, scene, clipSeconds, mediaRef, createdAt)

internal fun MemoryDoc.toEntity(uid: String) =
    MemoryEntity(
        uid = uid, title = title, place = place, note = note, scene = scene, clipSeconds = clipSeconds,
        mediaUri = null, createdAt = createdAt, remoteId = remoteId
    )

internal fun ChatMessageEntity.toDoc() = MessageDoc(remoteId, text, mine, createdAt)

internal fun MessageDoc.toEntity(uid: String) =
    ChatMessageEntity(uid = uid, text = text, mine = mine, createdAt = createdAt, remoteId = remoteId)

internal fun DeviceEntity.toDoc() =
    DeviceDoc(
        name, link, battery, storageUsedGb, storageTotalGb, firmware, latestFirmware,
        autoSync, captureQuality, ledIndicator, lost, lostAt
    )

internal fun DeviceDoc.toEntity(uid: String) =
    DeviceEntity(
        uid, name, link, battery, storageUsedGb, storageTotalGb, firmware, latestFirmware,
        autoSync, captureQuality, ledIndicator, lost, lostAt
    )

internal fun ProfileEntity.toDoc() = ProfileDoc(name, companion, skin, gender, voice, notifications, updatedAt)

internal fun ProfileDoc.toEntity(uid: String) = ProfileEntity(uid, name, companion, skin, gender, voice, notifications, updatedAt)
