package com.nya.app.data

import com.nya.app.data.ble.BleClient
import com.nya.app.data.ble.DeviceStatus
import com.nya.app.data.ble.LinkState
import com.nya.app.data.local.DeviceDao
import com.nya.app.data.local.DeviceEntity
import com.nya.app.data.local.toDeviceInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

interface DeviceRepository {
    val device: Flow<DeviceInfo?> // null = no headband paired
    val ringing: StateFlow<Boolean>
    suspend fun connect(): Result<Unit>
    suspend fun disconnect()
    suspend fun ring(on: Boolean): Result<Unit>
    fun transfers(): Flow<List<TransferItem>>
}

@OptIn(ExperimentalCoroutinesApi::class)
class RoomDeviceRepository(
    private val dao: DeviceDao,
    private val cloud: CloudSync,
    private val uid: Flow<String?>,
    private val ble: BleClient,
    scope: CoroutineScope
) : DeviceRepository {
    override val device: Flow<DeviceInfo?> = uid.flatMapLatest { id ->
        if (id == null) flowOf(null) else combine(dao.observe(id), ble.state, ble.status) { record, state, status ->
            record?.toDeviceInfo()?.withLink(state, status)
        }
    }

    override val ringing: StateFlow<Boolean> = ble.ringing

    init {
        scope.launch {
            uid.flatMapLatest { id ->
                if (id == null) emptyFlow() else ble.status.filterNotNull().map { id to it }
            }.collect { (id, s) ->
                val before = dao.get(id)
                dao.updateReadings(id, s.battery, s.storageUsedGb, s.storageTotalGb, s.firmware)
                if (before != null && before.firmware != s.firmware) {
                    cloud.putDevice(
                        id,
                        before.copy(
                            battery = s.battery, storageUsedGb = s.storageUsedGb,
                            storageTotalGb = s.storageTotalGb, firmware = s.firmware
                        ).toDoc()
                    )
                }
            }
        }
        scope.launch {
            // new account or sign-out drops the link; a known headband reconnects quietly
            uid.distinctUntilChanged().collectLatest { id ->
                ble.disconnect()
                if (id == null) return@collectLatest
                dao.observe(id).filterNotNull().first()
                ble.connect()
            }
        }
    }

    override suspend fun connect(): Result<Unit> = ble.connect().onSuccess {
        val id = uid.first() ?: return@onSuccess
        ble.status.value?.let { dao.insertIfMissing(it.toEntity(id)) }
        dao.get(id)?.let { cloud.putDevice(id, it.toDoc()) }
    }

    override suspend fun disconnect() = ble.disconnect()

    override suspend fun ring(on: Boolean) = ble.ring(on)

    override fun transfers() = ble.transfer()
}

class FakeDeviceRepository : DeviceRepository {
    override val device: Flow<DeviceInfo?> =
        MutableStateFlow(DeviceInfo("NYA F1", "Wi-Fi and Bluetooth", 0.82f, 4.1f, 16f, "1.1.4", "1.2.0"))
    override val ringing = MutableStateFlow(false)

    override suspend fun connect() = Result.success(Unit)

    override suspend fun disconnect() {}

    override suspend fun ring(on: Boolean): Result<Unit> {
        ringing.value = on
        return Result.success(Unit)
    }

    override fun transfers(): Flow<List<TransferItem>> = flowOf(emptyList())
}

private fun DeviceInfo.withLink(state: LinkState, status: DeviceStatus?) =
    if (state == LinkState.Connected && status != null) {
        copy(
            battery = status.battery, storageUsedGb = status.storageUsedGb,
            storageTotalGb = status.storageTotalGb, firmware = status.firmware, state = state
        )
    } else copy(state = state)

private fun DeviceStatus.toEntity(uid: String) =
    DeviceEntity(uid, name, "Wi-Fi and Bluetooth", battery, storageUsedGb, storageTotalGb, firmware, "1.2.0")
