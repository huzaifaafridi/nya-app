package com.nya.app.data.ble

import com.nya.app.data.TransferItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

enum class LinkState { Disconnected, Connecting, Connected }

data class DeviceStatus(
    val name: String,
    val battery: Float,
    val storageUsedGb: Float,
    val storageTotalGb: Float,
    val firmware: String
)

/** Low-level link to the headband. A real implementation reads the GATT characteristics in [NyaBleUuids]. */
interface BleClient {
    val state: StateFlow<LinkState>
    val status: StateFlow<DeviceStatus?> // live readings while connected, null otherwise
    val ringing: StateFlow<Boolean>

    suspend fun connect(): Result<Unit>
    suspend fun disconnect()
    suspend fun ring(on: Boolean): Result<Unit> // fails if not connected
    fun transfer(): Flow<List<TransferItem>> // media sync from the headband
}
