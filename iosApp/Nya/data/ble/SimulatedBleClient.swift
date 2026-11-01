import Foundation
import Observation

private let CONNECT_MS = 1500

struct BleError: Error {
    let message: String
}

/// Fake headband: every 3rd connection attempt fails so the pairing error path is reachable.
/// Ring, transfer and battery drain come across with their screens.
@MainActor @Observable
final class SimulatedBleClient: BleClient {
    private(set) var state: LinkState = .disconnected
    private(set) var status: DeviceStatus?
    @ObservationIgnored private var attempts = 0
    @ObservationIgnored private var generation = 0
    @ObservationIgnored private var inFlight: Task<Void, Never>?
    @ObservationIgnored private let battery: Float = 0.82

    func connect() async throws {
        // one attempt at a time, like the Kotlin connectLock
        while let running = inFlight { await running.value }
        if state == .connected { return }
        attempts += 1
        state = .connecting
        let gen = generation
        let wait = Task<Void, Never> { try? await Task.sleep(for: .milliseconds(CONNECT_MS)) }
        inFlight = wait
        await wait.value
        inFlight = nil
        if generation != gen {
            throw BleError(message: "Disconnected")
        }
        if attempts % 3 == 0 {
            state = .disconnected
            throw BleError(message: "Headband not found")
        }
        status = DeviceStatus(name: "\(NyaBleUuids.NAME_PREFIX) F1", battery: battery, storageUsedGb: 4.1, storageTotalGb: 16, firmware: "1.1.4")
        state = .connected
    }

    func disconnect() async {
        generation += 1
        state = .disconnected
        status = nil
    }
}
