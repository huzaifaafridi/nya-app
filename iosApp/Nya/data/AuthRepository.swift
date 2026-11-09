import Foundation

struct AuthUser: Hashable, Sendable {
    var name: String
    var email: String
    var verified: Bool
    var isNew: Bool
}

@MainActor
protocol AuthRepository: AnyObject {
    var isSignedIn: Bool { get }
    var uid: String? { get }
    var onUidChange: ((String?) -> Void)? { get set }
    func signIn(email: String, password: String) async throws -> AuthUser
    func signUp(name: String, email: String, password: String) async throws -> AuthUser
    func signInWithGoogle(idToken: String) async throws -> AuthUser
    func reloadVerified() async throws -> Bool
    func resendVerification() async throws
    func sendReset(email: String) async throws
    func signOut() async
}

func nameFromEmail(_ email: String) -> String {
    let name = (email.split(separator: "@", omittingEmptySubsequences: false).first ?? "")
        .trimmingCharacters(in: .whitespacesAndNewlines)
    return name.isEmpty ? "Mercy" : name.prefix(1).uppercased() + name.dropFirst()
}

/// No Firebase on iOS yet, so any details sign in. Unlike the Kotlin fake it remembers the
/// session and starts signed out, because the iOS app really runs on it.
@MainActor
final class FakeAuthRepository: AuthRepository {
    private let store = UserDefaults.standard
    var onUidChange: ((String?) -> Void)?

    var isSignedIn: Bool { uid != nil }
    var uid: String? {
        get { store.string(forKey: "uid") }
        set {
            store.set(newValue, forKey: "uid")
            onUidChange?(newValue)
        }
    }

    func signIn(email: String, password: String) async throws -> AuthUser {
        uid = "fake-user"
        return AuthUser(name: nameFromEmail(email), email: email, verified: true, isNew: false)
    }

    func signUp(name: String, email: String, password: String) async throws -> AuthUser {
        uid = "fake-user"
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        return AuthUser(name: trimmed.isEmpty ? nameFromEmail(email) : trimmed, email: email, verified: true, isNew: true)
    }

    func signInWithGoogle(idToken: String) async throws -> AuthUser {
        uid = "fake-user"
        return AuthUser(name: nameFromEmail(""), email: "", verified: true, isNew: false)
    }

    func reloadVerified() async throws -> Bool { true }

    func resendVerification() async throws {}

    func sendReset(email: String) async throws {}

    func signOut() async {
        uid = nil
    }
}
