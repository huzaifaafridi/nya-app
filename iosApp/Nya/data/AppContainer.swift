import Foundation

@MainActor
final class AppContainer {
    private let ble: BleClient

    let appConfig: AppConfigRepository = DefaultAppConfigRepository()
    let auth: AuthRepository = FakeAuthRepository()
    let settings: SettingsRepository = UserDefaultsSettingsRepository()
    let profile: ProfileRepository = UserDefaultsProfileRepository()
    let device: DeviceRepository

    init() {
        ble = SimulatedBleClient { [appConfig] in appConfig.config.ble.namePrefix }
        device = SimulatedDeviceRepository(ble: ble, auth: auth)
    }
}
