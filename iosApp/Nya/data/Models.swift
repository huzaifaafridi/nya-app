import Foundation

// Raw values match the Kotlin enum names, which is what the Android app stores.
enum Companion: String, CaseIterable, Hashable, Sendable {
    case wanderer = "Wanderer", muse = "Muse", guide = "Guide", yogi = "Yogi", creator = "Creator"
}

let ModeLabels = ["Travel", "Focus", "Wellness", "Creator"]

enum Gender: String, CaseIterable, Hashable, Sendable {
    case female = "Female", male = "Male"
}

enum Scene: String, CaseIterable, Hashable, Sendable {
    case sea = "Sea", forest = "Forest", dusk = "Dusk", mount = "Mount", yoga = "Yoga", city = "City"
}

struct Prefs: Hashable, Sendable {
    var name: String = ""
    var companion: Companion = .muse
    var skin: UInt32 = 0xFFE0AC84 // ARGB
    var gender: Gender = .female
    var voice: Int = 0
    var theme: Int = 0 // 0 Auto, 1 Light, 2 Dark
    var quiet: Bool = false
    var mode: Int = 2 // 0 Travel, 1 Focus, 2 Wellness, 3 Creator
    var onboarded: Bool = false
}

struct Memory: Identifiable, Hashable, Sendable {
    var id: Int64
    var title: String
    var place: String
    var note: String
    var scene: Scene
    var createdAt: Int64 // epoch ms
    var clipSeconds: Int? = nil // nil = photo
    var mediaUri: String? = nil
}

struct ChatMessage: Identifiable, Hashable, Sendable {
    var id: Int64
    var text: String
    var mine: Bool
}

struct DeviceInfo: Hashable, Sendable {
    var name: String
    var link: String
    var battery: Float
    var storageUsedGb: Float
    var storageTotalGb: Float
    var firmware: String
    var latestFirmware: String?
    var state: LinkState = .disconnected
}

struct TransferItem: Hashable, Sendable {
    var title: String
    var sizeMb: Int
    var progress: Float
}
