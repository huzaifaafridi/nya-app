import Foundation

@MainActor
protocol DeviceRepository: AnyObject {
    var state: LinkState { get }
    func connect() async throws
    func disconnect() async
}

/// Every account has a seeded headband, as on Android, so signing in reconnects it quietly.
@MainActor
final class SimulatedDeviceRepository: DeviceRepository {
    private let ble: BleClient

    var state: LinkState { ble.state }

    init(ble: BleClient, auth: AuthRepository) {
        self.ble = ble
        auth.onUidChange = { [weak self] id in
            Task { await self?.uidChanged(id) }
        }
        if let id = auth.uid {
            Task { await uidChanged(id) }
        }
    }

    // new account or sign-out drops the link; a known headband reconnects quietly
    private func uidChanged(_ id: String?) async {
        await ble.disconnect()
        if id == nil { return }
        try? await ble.connect()
    }

    func connect() async throws {
        try await ble.connect()
    }

    func disconnect() async {
        await ble.disconnect()
    }
}
