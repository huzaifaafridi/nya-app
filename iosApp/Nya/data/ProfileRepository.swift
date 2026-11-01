import Foundation

struct Profile: Hashable, Sendable {
    var name: String = ""
    var companion: Companion = .muse
    var skin: UInt32 = 0xFFE0AC84 // ARGB
    var gender: Gender = .female
    var voice: Int = 0
}
