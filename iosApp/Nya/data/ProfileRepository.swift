import Foundation
import Observation

struct Profile: Hashable, Sendable {
    var name: String = ""
    var companion: Companion = .muse
    var skin: UInt32 = 0xFFE0AC84 // ARGB
    var gender: Gender = .female
    var voice: Int = 0
}

@MainActor
protocol ProfileRepository: AnyObject {
    var profile: Profile { get }
    func update(_ transform: (inout Profile) -> Void)
}

/// Same keys and ordinal values as Kotlin's DataStoreProfileRepository.
@MainActor @Observable
final class UserDefaultsProfileRepository: ProfileRepository {
    @ObservationIgnored private let store = UserDefaults.standard
    private(set) var profile: Profile

    init() {
        let store = UserDefaults.standard
        let d = Profile()
        let companions = Companion.allCases, genders = Gender.allCases
        let companion = store.object(forKey: "companion") as? Int
        let gender = store.object(forKey: "gender") as? Int
        profile = Profile(
            name: store.string(forKey: "name") ?? d.name,
            companion: companion.flatMap { companions.indices.contains($0) ? companions[$0] : nil } ?? d.companion,
            skin: (store.object(forKey: "skin") as? Int).map { UInt32(truncatingIfNeeded: $0) } ?? d.skin,
            gender: gender.flatMap { genders.indices.contains($0) ? genders[$0] : nil } ?? d.gender,
            voice: store.object(forKey: "voice") as? Int ?? d.voice
        )
    }

    func update(_ transform: (inout Profile) -> Void) {
        transform(&profile)
        store.set(profile.name, forKey: "name")
        store.set(Companion.allCases.firstIndex(of: profile.companion), forKey: "companion")
        store.set(Int(profile.skin), forKey: "skin")
        store.set(Gender.allCases.firstIndex(of: profile.gender), forKey: "gender")
        store.set(profile.voice, forKey: "voice")
    }
}
