import Foundation

private let GENERIC = "Something went wrong. Please try again."

// the Firebase-specific messages come across with real auth
extension Error {
    func authMessage() -> String { GENERIC }
}
