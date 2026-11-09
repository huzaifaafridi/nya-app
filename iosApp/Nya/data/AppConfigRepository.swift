import Foundation
import Observation

@MainActor
protocol AppConfigRepository: AnyObject {
    /// Starts as the built-in defaults and follows the remote document live.
    var config: AppConfig { get }
}

/// iOS-only stand-in until Firebase lands (port phase 2): always the built-in defaults, which is also what
/// Kotlin's FirestoreAppConfigRepository serves before the first snapshot or when the listener fails.
@MainActor @Observable
final class DefaultAppConfigRepository: AppConfigRepository {
    private(set) var config: AppConfig

    init(_ config: AppConfig = AppConfig()) {
        self.config = config
    }
}
