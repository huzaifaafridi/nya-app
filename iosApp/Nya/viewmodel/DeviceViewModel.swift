import Foundation
import Observation

@MainActor @Observable
final class DeviceViewModel {
    @ObservationIgnored private let repo: DeviceRepository
    private var running = 0

    init(_ c: AppContainer) {
        repo = c.device
    }

    var connecting: Bool { running > 0 }
    var linking: Bool { repo.state == .connecting }

    func connect(onFail: @escaping () -> Void = {}, onSuccess: @escaping () -> Void = {}) {
        Task {
            running += 1
            defer { running -= 1 }
            do {
                try await repo.connect()
                onSuccess()
            } catch {
                onFail()
            }
        }
    }
}
