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

/// Low-level link to the headband.
@MainActor
protocol BleClient: AnyObject {
    var state: LinkState { get }
    var status: DeviceStatus? { get } // live readings while connected, nil otherwise

    func connect() async throws
    func disconnect() async
}
