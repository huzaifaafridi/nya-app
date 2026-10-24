package com.nya.app.data.ble

import com.nya.app.data.HeadbandSettings
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

sealed interface FirmwareStep {
    data class Downloading(val progress: Float) : FirmwareStep
    data class Installing(val progress: Float) : FirmwareStep
    data object Done : FirmwareStep
}

/** Low-level link to the headband. A real implementation reads the GATT characteristics in [NyaBleUuids]. */
interface BleClient {
    val state: StateFlow<LinkState>
    val status: StateFlow<DeviceStatus?> // live readings while connected, null otherwise
    val ringing: StateFlow<Boolean>

    suspend fun connect(): Result<Unit>
    suspend fun disconnect()
    suspend fun ring(on: Boolean): Result<Unit> // fails if not connected
    suspend fun applySettings(settings: HeadbandSettings): Result<Unit> // fails if not connected

    /** Downloads and installs [version]; fails with an exception if not connected or the link drops. Cancelling aborts the update. */
    fun updateFirmware(version: String): Flow<FirmwareStep>

    fun transfer(): Flow<List<TransferItem>> // media sync from the headband
}
