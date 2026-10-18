package com.nya.app.data

import com.nya.app.data.local.DeviceDao
import com.nya.app.data.local.SeedDao
import com.nya.app.data.local.toDeviceInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

interface DeviceRepository {
    val device: Flow<DeviceInfo?> // null = no headband paired
    fun transfers(): Flow<List<TransferItem>>
}

@OptIn(ExperimentalCoroutinesApi::class)
class RoomDeviceRepository(
    private val dao: DeviceDao,
    private val seed: SeedDao,
    uid: Flow<String?>
) : DeviceRepository {
    override val device: Flow<DeviceInfo?> = uid.flatMapLatest { id ->
        if (id == null) flowOf(null) else flow {
            seed.seed(id)
            emitAll(dao.observe(id).map { it?.toDeviceInfo() })
        }
    }

    override fun transfers() = simulatedTransfers()
}

class FakeDeviceRepository : DeviceRepository {
    override val device: Flow<DeviceInfo?> =
        MutableStateFlow(DeviceInfo("NYA F1", "Wi-Fi and Bluetooth", 0.82f, 4.1f, 16f, "1.1.4", "1.2.0"))

    override fun transfers() = simulatedTransfers()
}

private fun simulatedTransfers(): Flow<List<TransferItem>> = flow {
    var items = listOf(
        TransferItem("Sunset at the pier", 24, 1f),
        TransferItem("Coffee with Ali", 88, 0.6f),
        TransferItem("Morning walk", 41, 0f),
        TransferItem("Late night idea", 6, 0f)
    )
    emit(items)
    while (true) {
        val i = items.indexOfFirst { it.progress < 1f }
        if (i == -1) break
        delay(120)
        items = items.mapIndexed { j, t -> if (j == i) t.copy(progress = minOf(1f, t.progress + 0.03f)) else t }
        emit(items)
    }
}
