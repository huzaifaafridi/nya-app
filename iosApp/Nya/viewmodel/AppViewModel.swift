import Foundation
import Observation

@MainActor @Observable
final class AppViewModel {
    @ObservationIgnored private let settings: SettingsRepository
    @ObservationIgnored private let profile: ProfileRepository
    @ObservationIgnored private let auth: AuthRepository

    init(_ c: AppContainer) {
        settings = c.settings
        profile = c.profile
        auth = c.auth
    }

    var prefs: Prefs {
        let s = settings.settings, p = profile.profile
        return Prefs(
            name: p.name, companion: p.companion, skin: p.skin, gender: p.gender, voice: p.voice,
            theme: s.theme, quiet: s.quiet, mode: s.mode, onboarded: s.onboarded
        )
    }

    func setCompanion(_ companion: Companion) { profile.update { $0.companion = companion } }
    func setSkin(_ argb: UInt32) { profile.update { $0.skin = argb } }
    func setGender(_ gender: Gender) { profile.update { $0.gender = gender } }
    func setVoice(_ voice: Int) { profile.update { $0.voice = voice } }
    func completeOnboarding() { settings.update { $0.onboarded = true } }

    var isSignedIn: Bool { auth.isSignedIn }

    func signOut() {
        Task {
            await auth.signOut()
            settings.update { $0.onboarded = false }
        }
    }
}
