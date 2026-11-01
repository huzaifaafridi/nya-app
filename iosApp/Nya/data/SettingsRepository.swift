import Foundation

struct AppSettings: Hashable, Sendable {
    var theme: Int = 0 // 0 Auto, 1 Light, 2 Dark
    var quiet: Bool = false
    var mode: Int = 2 // 0 Travel, 1 Focus, 2 Wellness, 3 Creator
    var onboarded: Bool = false
}
