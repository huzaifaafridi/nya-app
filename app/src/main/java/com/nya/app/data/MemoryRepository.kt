package com.nya.app.data

import com.nya.app.data.local.MemoryDao
import com.nya.app.data.local.toMemory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

interface MemoryRepository {
    val memories: Flow<List<Memory>>
    suspend fun delete(id: Long)
}

@OptIn(ExperimentalCoroutinesApi::class)
class RoomMemoryRepository(
    private val dao: MemoryDao,
    private val cloud: CloudSync,
    uid: Flow<String?>
) : MemoryRepository {
    override val memories: Flow<List<Memory>> = uid.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else dao.observe(id).map { list -> list.map { it.toMemory() } }
    }

    override suspend fun delete(id: Long) {
        val memory = dao.byId(id) ?: return
        // first, so a snapshot arriving in between cannot bring the row back
        cloud.deleteMemory(memory.uid, memory.remoteId)
        dao.delete(id)
    }
}

class FakeMemoryRepository : MemoryRepository {
    private val now = System.currentTimeMillis()
    private val state = MutableStateFlow(
        listOf(
            Memory(1, "Sunset at the pier", "Karachi",
                "Golden hour by the water. You said it felt like the day finally slowed down.", Scene.Sea, now, 8),
            Memory(2, "Beach day", "Sea View",
                "Windy, cold water, and the best chai afterwards.", Scene.Mount, now - DayMs)
        )
    )
    override val memories: Flow<List<Memory>> = state

    override suspend fun delete(id: Long) = state.update { list -> list.filterNot { it.id == id } }

    private companion object {
        const val DayMs = 24 * 60 * 60 * 1000L
    }
}
