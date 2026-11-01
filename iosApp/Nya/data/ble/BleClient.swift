import Foundation

enum LinkState: String, CaseIterable, Hashable, Sendable {
    case disconnected = "Disconnected", connecting = "Connecting", connected = "Connected"
}

struct DeviceStatus: Hashable, Sendable {
    var name: String
    var battery: Float
    var storageUsedGb: Float
    var storageTotalGb: Float
    var firmware: String
}
