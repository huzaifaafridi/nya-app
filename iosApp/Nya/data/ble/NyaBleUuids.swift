import Foundation

/// Bluetooth SIG standard identifiers. The headband's own (placeholder) ones are the defaults of [BleConfig].
enum NyaBleUuids {
    static let BATTERY_SERVICE = UUID(uuidString: "0000180f-0000-1000-8000-00805f9b34fb")!
    static let BATTERY_LEVEL = UUID(uuidString: "00002a19-0000-1000-8000-00805f9b34fb")!
}
