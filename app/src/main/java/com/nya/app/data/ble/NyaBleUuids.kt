package com.nya.app.data.ble

import java.util.UUID

/** Bluetooth SIG standard identifiers. The headband's own (placeholder) ones are the defaults of [com.nya.app.data.BleConfig]. */
object NyaBleUuids {
    val BATTERY_SERVICE: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
    val BATTERY_LEVEL: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
}
