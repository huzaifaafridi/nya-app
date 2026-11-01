import Foundation

@MainActor
final class AppContainer {
    private let ble: BleClient = SimulatedBleClient()

    let auth: AuthRepository = FakeAuthRepository()
    let settings: SettingsRepository = UserDefaultsSettingsRepository()
    let profile: ProfileRepository = UserDefaultsProfileRepository()
    let device: DeviceRepository

    init() {
        device = SimulatedDeviceRepository(ble: ble, auth: auth)
    }
}
