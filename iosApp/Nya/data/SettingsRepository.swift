import Foundation
import Observation

struct AppSettings: Hashable, Sendable {
    var theme: Int = 0 // 0 Auto, 1 Light, 2 Dark
    var quiet: Bool = false
    var mode: Int = 2 // 0 Travel, 1 Focus, 2 Wellness, 3 Creator
    var onboarded: Bool = false
}

@MainActor
protocol SettingsRepository: AnyObject {
    var settings: AppSettings { get }
    func update(_ transform: (inout AppSettings) -> Void)
}

@MainActor @Observable
final class UserDefaultsSettingsRepository: SettingsRepository {
    @ObservationIgnored private let store = UserDefaults.standard
    private(set) var settings: AppSettings

    init() {
        let store = UserDefaults.standard
        let d = AppSettings()
        settings = AppSettings(
            theme: store.object(forKey: "theme") as? Int ?? d.theme,
            quiet: store.object(forKey: "quiet") as? Bool ?? d.quiet,
            mode: store.object(forKey: "mode") as? Int ?? d.mode,
            onboarded: store.object(forKey: "onboarded") as? Bool ?? d.onboarded
        )
    }

    func update(_ transform: (inout AppSettings) -> Void) {
        transform(&settings)
        store.set(settings.theme, forKey: "theme")
        store.set(settings.quiet, forKey: "quiet")
        store.set(settings.mode, forKey: "mode")
        store.set(settings.onboarded, forKey: "onboarded")
    }
}
