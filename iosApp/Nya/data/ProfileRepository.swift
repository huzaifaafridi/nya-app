import Foundation
import Observation

/// Must match the name size limit in firestore.rules.
let MaxNameLength = 100

struct Profile: Hashable, Sendable {
    var name: String = ""
    var companion: Companion = .muse
    var skin: UInt32 = 0xFFE0AC84 // ARGB
    var gender: Gender = .female
    var voice: Int = 0
    var notifications: Bool = true
}

@MainActor
protocol ProfileRepository: AnyObject {
    var profile: Profile { get }
    func update(_ transform: (inout Profile) -> Void)

    /// Sets the name only when the account has none yet.
    func suggestName(_ name: String)
}

/// Kotlin's `name.trim().take(MaxNameLength)`. Kotlin counts UTF-16 units; this keeps whole characters,
/// so it never splits an emoji, and the result is never longer than Kotlin's.
func cappedName(_ name: String) -> String {
    var t = name.trimmingCharacters(in: .whitespacesAndNewlines)
    while t.utf16.count > MaxNameLength { t.removeLast() }
    return t
}

private func isBlank(_ s: String) -> Bool { s.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }

/// iOS-only stand-in until storage is ported (port phase 4): one profile per phone in UserDefaults, with the
/// same keys and ordinal values as Kotlin's old DataStoreProfileRepository. Names are capped like Kotlin's
/// RoomProfileRepository.
@MainActor @Observable
final class UserDefaultsProfileRepository: ProfileRepository {
    @ObservationIgnored private let store: UserDefaults
    private(set) var profile: Profile

    init(store: UserDefaults = .standard) {
        self.store = store
        let d = Profile()
        let companions = Companion.allCases, genders = Gender.allCases
        let companion = store.object(forKey: "companion") as? Int
        let gender = store.object(forKey: "gender") as? Int
        profile = Profile(
            name: store.string(forKey: "name") ?? d.name,
            companion: companion.flatMap { companions.indices.contains($0) ? companions[$0] : nil } ?? d.companion,
            skin: (store.object(forKey: "skin") as? Int).map { UInt32(truncatingIfNeeded: $0) } ?? d.skin,
            gender: gender.flatMap { genders.indices.contains($0) ? genders[$0] : nil } ?? d.gender,
            voice: store.object(forKey: "voice") as? Int ?? d.voice,
            notifications: store.object(forKey: "notifications") as? Bool ?? d.notifications
        )
    }

    func update(_ transform: (inout Profile) -> Void) {
        transform(&profile)
        save()
    }

    func suggestName(_ name: String) {
        guard isBlank(profile.name) else { return }
        profile.name = name
        save()
    }

    private func save() {
        profile.name = cappedName(profile.name)
        store.set(profile.name, forKey: "name")
        store.set(Companion.allCases.firstIndex(of: profile.companion), forKey: "companion")
        store.set(Int(profile.skin), forKey: "skin")
        store.set(Gender.allCases.firstIndex(of: profile.gender), forKey: "gender")
        store.set(profile.voice, forKey: "voice")
        store.set(profile.notifications, forKey: "notifications")
    }
}

/// Kotlin's FakeProfileRepository, for tests.
@MainActor @Observable
final class FakeProfileRepository: ProfileRepository {
    private(set) var profile: Profile

    init(_ initial: Profile = Profile()) {
        profile = initial
    }

    func update(_ transform: (inout Profile) -> Void) { transform(&profile) }

    func suggestName(_ name: String) {
        if isBlank(profile.name) { profile.name = name }
    }
}
