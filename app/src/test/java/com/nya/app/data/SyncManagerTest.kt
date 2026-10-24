@file:OptIn(ExperimentalCoroutinesApi::class)

package com.nya.app.data

import com.nya.app.data.local.ChatDao
import com.nya.app.data.local.ChatMessageEntity
import com.nya.app.data.local.DeviceDao
import com.nya.app.data.local.DeviceEntity
import com.nya.app.data.local.MemoryDao
import com.nya.app.data.local.MemoryEntity
import com.nya.app.data.local.SeedDao
import com.nya.app.data.local.SeedMark
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

private const val Uid = "u1"

private fun memory(remoteId: String, uid: String = Uid, createdAt: Long = 1L) = MemoryEntity(
    uid = uid, title = "T $remoteId", place = "P", note = "N", scene = "Sea", clipSeconds = null,
    mediaUri = null, createdAt = createdAt, remoteId = remoteId
)

private fun memoryDoc(remoteId: String) = MemoryDoc(remoteId, "T $remoteId", "P", "N", "Sea", null, null, 1L)

private val device = DeviceEntity(Uid, "NYA F1", "Wi-Fi", 0.5f, 1f, 16f, "1.0", null)

private class FakeCloud : CloudSync {
    var touch: suspend () -> Boolean = { true }
    var create: suspend () -> Unit = {}
    val calls = mutableListOf<String>()
    val memoryFlow = MutableSharedFlow<RemoteSnapshot<MemoryDoc>>(replay = 1)
    val messageFlow = MutableSharedFlow<List<MessageDoc>>(replay = 1)
    val deviceFlow = MutableSharedFlow<DeviceDoc?>(replay = 1)
    val memoryPuts = mutableListOf<MemoryDoc>()
    val memoryDeletes = mutableListOf<Pair<String, String>>()
    val messagePuts = mutableListOf<MessageDoc>()
    val devicePuts = mutableListOf<DeviceDoc>()

    override suspend fun touchUser(uid: String): Boolean {
        calls += "touch"
        return touch()
    }

    override suspend fun createUser(uid: String) {
        calls += "create"
        create()
    }

    override fun memories(uid: String): Flow<RemoteSnapshot<MemoryDoc>> = memoryFlow
    override fun messages(uid: String): Flow<List<MessageDoc>> = messageFlow
    override fun device(uid: String): Flow<DeviceDoc?> = deviceFlow
    override fun putMemory(uid: String, doc: MemoryDoc) { calls += "putMemory"; memoryPuts += doc }
    override fun deleteMemory(uid: String, remoteId: String) { calls += "deleteMemory"; memoryDeletes += uid to remoteId }
    override fun putMessage(uid: String, doc: MessageDoc) { calls += "putMessage"; messagePuts += doc }
    override fun putDevice(uid: String, doc: DeviceDoc) { devicePuts += doc }
}

private class FakeMemoryDao(private val calls: MutableList<String> = mutableListOf()) : MemoryDao {
    val rows = MutableStateFlow(emptyList<MemoryEntity>())
    val deleteBatches = mutableListOf<Int>()
    private var nextId = 1L

    val remoteIdSet get() = rows.value.map { it.remoteId }.toSet()

    override fun observe(uid: String) = rows.map { list -> list.filter { it.uid == uid } }
    override suspend fun delete(id: Long) {
        calls += "localDelete"
        rows.update { list -> list.filterNot { it.id == id } }
    }
    override suspend fun all(uid: String) = rows.value.filter { it.uid == uid }
    override suspend fun byId(id: Long) = rows.value.firstOrNull { it.id == id }
    override suspend fun remoteIds(uid: String) = all(uid).map { it.remoteId }

    override suspend fun insertAll(items: List<MemoryEntity>) = items.forEach { item ->
        if (rows.value.none { it.uid == item.uid && it.remoteId == item.remoteId }) rows.update { it + item.copy(id = nextId++) }
    }

    override suspend fun deleteByRemoteIds(uid: String, ids: List<String>) {
        deleteBatches += ids.size
        rows.update { list -> list.filterNot { it.uid == uid && it.remoteId in ids } }
    }
}

private class FakeChatDao : ChatDao() {
    val rows = MutableStateFlow(emptyList<ChatMessageEntity>())
    private var nextId = 1L

    override fun observe(uid: String) = rows.map { list -> list.filter { it.uid == uid } }
    override suspend fun history(uid: String) = rows.value.filter { it.uid == uid }
    override suspend fun count(uid: String) = history(uid).size
    override suspend fun insert(message: ChatMessageEntity) = insertAll(listOf(message))

    override suspend fun insertAll(items: List<ChatMessageEntity>) = items.forEach { item ->
        if (rows.value.none { it.uid == item.uid && it.remoteId == item.remoteId }) rows.update { it + item.copy(id = nextId++) }
    }
}

private class FakeDeviceDao : DeviceDao {
    val rows = MutableStateFlow(emptyMap<String, DeviceEntity>())

    override fun observe(uid: String) = rows.map { it[uid] }
    override suspend fun insertIfMissing(device: DeviceEntity) = rows.update { if (device.uid in it) it else it + (device.uid to device) }
    override suspend fun upsert(device: DeviceEntity) = rows.update { it + (device.uid to device) }
    override suspend fun get(uid: String) = rows.value[uid]
    override suspend fun updateReadings(uid: String, battery: Float, used: Float, total: Float, firmware: String) =
        rows.update { map ->
            map[uid]?.let { map + (uid to it.copy(battery = battery, storageUsedGb = used, storageTotalGb = total, firmware = firmware)) } ?: map
        }
}

/** Runs the real demo seeding on top of the fake DAOs. */
private class FakeSeedDao(private val memories: FakeMemoryDao, private val devices: FakeDeviceDao) : SeedDao() {
    private val marked = mutableSetOf<String>()

    override suspend fun marks(uid: String) = if (uid in marked) 1 else 0
    override suspend fun insertMemories(items: List<MemoryEntity>) = memories.insertAll(items)
    override suspend fun insertDevice(device: DeviceEntity) = devices.upsert(device)
    override suspend fun insertMark(mark: SeedMark) {
        marked += mark.uid
    }
}

private class Rig(private val scope: TestScope) {
    val uid = MutableStateFlow<String?>(Uid)
    val cloud = FakeCloud()
    val memories = FakeMemoryDao()
    val chat = FakeChatDao()
    val devices = FakeDeviceDao()

    fun start() {
        SyncManager(uid, cloud, LocalMediaStore(), memories, chat, devices, FakeSeedDao(memories, devices), scope.backgroundScope)
            .start()
        scope.runCurrent()
    }

    /** Sends a memory snapshot and lets it reach the DAOs. */
    suspend fun emit(snapshot: RemoteSnapshot<MemoryDoc>) {
        cloud.memoryFlow.emit(snapshot)
        scope.runCurrent()
    }
}

class SyncManagerTest {
    @Test
    fun remoteIdsToDeleteDropsOnlyLocalOnlyRows() {
        val remote = listOf(memoryDoc("a"), memoryDoc("b"))
        assertEquals(listOf("c"), remoteIdsToDelete(listOf("a", "b", "c"), remote, emptySet()))
        assertEquals(emptyList<String>(), remoteIdsToDelete(listOf("a", "b", "c"), remote, setOf("c")))
        assertEquals(listOf("a", "b"), remoteIdsToDelete(listOf("a", "b", "c"), emptyList(), setOf("c")))
    }

    @Test
    fun mappingsRoundTripWithoutLocalMediaUri() {
        val entity = memory("m1").copy(clipSeconds = 8, mediaUri = "content://photo")
        val doc = entity.toDoc("cloud-ref")
        assertEquals("cloud-ref", doc.mediaRef)
        assertEquals(entity.copy(mediaUri = null), doc.toEntity(Uid))
        assertNull(entity.toDoc(null).toEntity(Uid).mediaUri)

        val message = ChatMessageEntity(uid = Uid, text = "hi", mine = true, createdAt = 5L, remoteId = "c1")
        assertEquals(message, message.toDoc().toEntity(Uid))
        assertEquals(device, device.toDoc().toEntity(Uid))
    }

    @Test
    fun newCloudAccountSeedsAndUploadsEverything() = runTest {
        val rig = Rig(this)
        rig.memories.insertAll(listOf(memory("mine")))
        rig.chat.insertAll(listOf(ChatMessageEntity(uid = Uid, text = "hi", mine = true, createdAt = 1L, remoteId = "c1")))
        rig.cloud.touch = { false }
        rig.start()

        val local = rig.memories.rows.value
        assertEquals(7, local.size)
        assertTrue(rig.memories.remoteIdSet.containsAll(listOf("mine", "demo-0")))
        assertEquals(local.map { it.remoteId }.toSet(), rig.cloud.memoryPuts.map { it.remoteId }.toSet())
        assertEquals(listOf("c1"), rig.cloud.messagePuts.map { it.remoteId })
        assertEquals(listOf("NYA F1"), rig.cloud.devicePuts.map { it.name })
        assertEquals("create", rig.cloud.calls.last())
        assertEquals(listOf("touch"), rig.cloud.calls.take(1))

        rig.emit(RemoteSnapshot(emptyList(), fromServer = true))
        assertEquals(7, rig.memories.rows.value.size)
    }

    @Test
    fun existingCloudAccountDownloadsWithoutSeedingOrUploading() = runTest {
        val rig = Rig(this)
        rig.cloud.memoryFlow.emit(RemoteSnapshot(listOf(memoryDoc("r1")), fromServer = true))
        rig.cloud.messageFlow.emit(listOf(MessageDoc("c1", "hello", false, 2L)))
        rig.cloud.deviceFlow.emit(device.toDoc())
        rig.start()

        assertEquals(setOf("r1"), rig.memories.remoteIdSet)
        assertNull(rig.memories.rows.value.single().mediaUri)
        assertEquals(listOf("hello"), rig.chat.rows.value.map { it.text })
        assertEquals(device, rig.devices.rows.value[Uid])
        assertTrue(rig.cloud.memoryPuts.isEmpty() && rig.cloud.messagePuts.isEmpty() && rig.cloud.devicePuts.isEmpty())
        assertEquals(listOf("touch"), rig.cloud.calls)
    }

    @Test
    fun onlyServerSnapshotsDeleteMissingRows() = runTest {
        val rig = Rig(this)
        rig.memories.insertAll(listOf(memory("a"), memory("b")))
        rig.start()

        rig.emit(RemoteSnapshot(listOf(memoryDoc("c")), fromServer = false))
        assertEquals(setOf("a", "b", "c"), rig.memories.remoteIdSet)

        rig.emit(RemoteSnapshot(listOf(memoryDoc("b")), fromServer = true))
        assertEquals(setOf("b"), rig.memories.remoteIdSet)
    }

    @Test
    fun largeDeletionsAreSplitIntoSmallBatches() = runTest {
        val rig = Rig(this)
        rig.memories.insertAll(List(1_200) { memory("m$it") })
        rig.start()

        rig.emit(RemoteSnapshot(emptyList(), fromServer = true))
        assertTrue(rig.memories.rows.value.isEmpty())
        assertEquals(1_200, rig.memories.deleteBatches.sum())
        assertTrue(rig.memories.deleteBatches.all { it <= 500 })
    }

    @Test
    fun touchUserIsRetriedAndNothingIsDeletedBeforeItSucceeds() = runTest {
        val rig = Rig(this)
        var calls = 0
        rig.cloud.touch = { if (++calls == 1) throw IOException("offline") else true }
        rig.memories.insertAll(listOf(memory("a")))
        rig.cloud.memoryFlow.emit(RemoteSnapshot(emptyList(), fromServer = true))
        rig.start()

        assertEquals(1, calls)
        assertEquals(setOf("a"), rig.memories.remoteIdSet)

        advanceTimeBy(4_999)
        runCurrent()
        assertEquals(1, calls)
        assertTrue(rig.memories.deleteBatches.isEmpty())

        advanceTimeBy(1)
        runCurrent()
        assertEquals(2, calls)
        assertTrue(rig.memories.rows.value.isEmpty())
    }

    @Test
    fun failedCreateUserIsRetriedWithAHarmlessSecondUpload() = runTest {
        val rig = Rig(this)
        var creates = 0
        rig.cloud.touch = { false }
        rig.cloud.create = { if (++creates == 1) throw IOException("offline") }
        rig.memories.insertAll(listOf(memory("a")))
        rig.cloud.memoryFlow.emit(RemoteSnapshot(emptyList(), fromServer = true))
        rig.start()

        assertEquals(1, creates)
        assertEquals(7, rig.memories.rows.value.size)
        assertTrue(rig.memories.deleteBatches.isEmpty())

        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(2, creates)
        assertEquals(7, rig.memories.rows.value.size)
        assertEquals(14, rig.cloud.memoryPuts.size)
        assertEquals(7, rig.cloud.memoryPuts.map { it.remoteId }.toSet().size)
    }

    @Test
    fun deletingAMemoryRemovesItLocallyAndInTheCloud() = runTest {
        val cloud = FakeCloud()
        val dao = FakeMemoryDao()
        dao.insertAll(listOf(memory("rid")))
        RoomMemoryRepository(dao, cloud, MutableStateFlow(Uid)).delete(dao.rows.value.single().id)

        assertTrue(dao.rows.value.isEmpty())
        assertEquals(listOf(Uid to "rid"), cloud.memoryDeletes)
    }

    @Test
    fun deletingAMemoryAsksTheCloudBeforeRemovingTheRow() = runTest {
        val cloud = FakeCloud()
        val dao = FakeMemoryDao(cloud.calls)
        dao.insertAll(listOf(memory("rid")))
        RoomMemoryRepository(dao, cloud, MutableStateFlow(Uid)).delete(1L)

        assertEquals(listOf("deleteMemory", "localDelete"), cloud.calls)
    }

    @Test
    fun readingTheChatHistoryDoesNotRewriteTheGreetingInTheCloud() = runTest {
        val cloud = FakeCloud()
        val dao = FakeChatDao()
        val chat = RoomChatRepository(
            dao, cloud, MutableStateFlow(Uid), FakeProfileRepository(), FakeSettingsRepository(),
            FakeReplyEngine(), backgroundScope
        )
        repeat(2) { chat.history.first() }

        assertEquals(1, dao.rows.value.size)
        assertEquals("greeting", dao.rows.value.single().remoteId)
        assertTrue(cloud.messagePuts.isEmpty())
    }

    @Test
    fun signingOutStopsSyncing() = runTest {
        val rig = Rig(this)
        rig.start()
        rig.emit(RemoteSnapshot(listOf(memoryDoc("r1")), fromServer = true))
        assertEquals(setOf("r1"), rig.memories.remoteIdSet)

        rig.uid.value = null
        runCurrent()
        rig.emit(RemoteSnapshot(listOf(memoryDoc("r2")), fromServer = true))
        rig.cloud.messageFlow.emit(listOf(MessageDoc("c1", "late", false, 1L)))
        rig.cloud.deviceFlow.emit(device.toDoc())
        runCurrent()

        assertEquals(setOf("r1"), rig.memories.remoteIdSet)
        assertTrue(rig.chat.rows.value.isEmpty())
        assertTrue(rig.devices.rows.value.isEmpty())
    }
}
