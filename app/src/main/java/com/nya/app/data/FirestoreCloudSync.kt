package com.nya.app.data

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private const val Tag = "NyaSync"
private const val MaxMessages = 500L

class FirestoreCloudSync(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) : CloudSync {
    private fun user(uid: String) = db.collection("users").document(uid)

    override suspend fun touchUser(uid: String): Boolean {
        val ref = user(uid)
        if (!ref.get(Source.SERVER).await().exists()) return false
        ref.update("lastActiveAt", FieldValue.serverTimestamp(), "platform", "android").await()
        return true
    }

    override suspend fun createUser(uid: String) {
        user(uid).set(
            mapOf(
                "createdAt" to FieldValue.serverTimestamp(),
                "lastActiveAt" to FieldValue.serverTimestamp(),
                "plan" to "free",
                "platform" to "android"
            )
        ).await()
    }

    override fun memories(uid: String): Flow<RemoteSnapshot<MemoryDoc>> = listen(user(uid).collection("memories")) { snap ->
        RemoteSnapshot(snap.documents.mapNotNull { it.runCatching { toMemoryDoc() }.getOrNull() }, !snap.metadata.isFromCache)
    }

    override fun messages(uid: String): Flow<List<MessageDoc>> =
        listen(user(uid).collection("messages").orderBy("createdAt").limitToLast(MaxMessages)) { snap ->
            snap.documents.mapNotNull { it.runCatching { toMessageDoc() }.getOrNull() }
        }

    override fun device(uid: String): Flow<DeviceDoc?> = callbackFlow {
        val registration = user(uid).collection("devices").document("main").addSnapshotListener { snap, e ->
            if (e != null) {
                Log.w(Tag, "device listener failed", e)
                close()
            } else trySend(snap?.runCatching { toDeviceDoc() }?.getOrNull())
        }
        awaitClose { registration.remove() }
    }

    override fun putMemory(uid: String, doc: MemoryDoc) = write(
        user(uid).collection("memories").document(doc.remoteId).set(
            mapOf(
                "title" to doc.title, "place" to doc.place, "note" to doc.note, "scene" to doc.scene,
                "clipSeconds" to doc.clipSeconds, "mediaRef" to doc.mediaRef, "createdAt" to doc.createdAt
            )
        )
    )

    override fun deleteMemory(uid: String, remoteId: String) =
        write(user(uid).collection("memories").document(remoteId).delete())

    override fun putMessage(uid: String, doc: MessageDoc) = write(
        user(uid).collection("messages").document(doc.remoteId)
            .set(mapOf("text" to doc.text, "mine" to doc.mine, "createdAt" to doc.createdAt))
    )

    override fun putDevice(uid: String, doc: DeviceDoc) = write(
        user(uid).collection("devices").document("main").set(
            mapOf(
                "name" to doc.name, "link" to doc.link, "battery" to doc.battery,
                "storageUsedGb" to doc.storageUsedGb, "storageTotalGb" to doc.storageTotalGb,
                "firmware" to doc.firmware, "latestFirmware" to doc.latestFirmware
            )
        )
    )

    /** A failing listener (e.g. permission denied) is logged and ends the flow quietly. */
    private fun <T> listen(query: Query, map: (QuerySnapshot) -> T): Flow<T> = callbackFlow {
        val registration = query.addSnapshotListener { snap, e ->
            if (e != null) {
                Log.w(Tag, "listener failed", e)
                close()
            } else if (snap != null) trySend(map(snap))
        }
        awaitClose { registration.remove() }
    }

    /** Not awaited: offline the write is queued by Firestore and would never complete here. */
    private fun write(task: Task<Void>) {
        task.addOnFailureListener { Log.w(Tag, "write failed", it) }
    }
}

private fun DocumentSnapshot.toMemoryDoc(): MemoryDoc? {
    return MemoryDoc(
        remoteId = id,
        title = getString("title") ?: return null,
        place = getString("place") ?: "",
        note = getString("note") ?: "",
        scene = getString("scene") ?: return null,
        clipSeconds = getLong("clipSeconds")?.toInt(),
        mediaRef = getString("mediaRef"),
        createdAt = getLong("createdAt") ?: return null
    )
}

private fun DocumentSnapshot.toMessageDoc(): MessageDoc? {
    return MessageDoc(
        remoteId = id,
        text = getString("text") ?: return null,
        mine = getBoolean("mine") ?: return null,
        createdAt = getLong("createdAt") ?: return null
    )
}

private fun DocumentSnapshot.toDeviceDoc(): DeviceDoc? {
    if (!exists()) return null
    return DeviceDoc(
        name = getString("name") ?: return null,
        link = getString("link") ?: return null,
        battery = getDouble("battery")?.toFloat() ?: return null,
        storageUsedGb = getDouble("storageUsedGb")?.toFloat() ?: return null,
        storageTotalGb = getDouble("storageTotalGb")?.toFloat() ?: return null,
        firmware = getString("firmware") ?: return null,
        latestFirmware = getString("latestFirmware")
    )
}
