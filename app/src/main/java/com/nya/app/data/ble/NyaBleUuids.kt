package com.nya.app.data.ble

import java.util.UUID

/**
 * PLACEHOLDERS: replace with the firmware's real values once the headband exists.
 * Kept for the real BLE client; the simulator only uses [NAME_PREFIX].
 */
object NyaBleUuids {
    const val NAME_PREFIX = "NYA"

    val SERVICE: UUID = UUID.fromString("6e5a1c3b-8f42-4d97-a3b0-2c7d91e4f058")
    val STORAGE: UUID = UUID.fromString("6e5a1c3c-8f42-4d97-a3b0-2c7d91e4f058")
    val FIRMWARE: UUID = UUID.fromString("6e5a1c3d-8f42-4d97-a3b0-2c7d91e4f058")
    val FIND_MY: UUID = UUID.fromString("6e5a1c3e-8f42-4d97-a3b0-2c7d91e4f058")
    val TRANSFER: UUID = UUID.fromString("6e5a1c3f-8f42-4d97-a3b0-2c7d91e4f058")

    val BATTERY_SERVICE: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
    val BATTERY_LEVEL: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
}
