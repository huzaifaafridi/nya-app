import Foundation
import Observation

@MainActor @Observable
final class AuthViewModel {
    @ObservationIgnored private let auth: AuthRepository
    @ObservationIgnored private let profile: ProfileRepository
    private(set) var busy = false
    private(set) var error: String?
    private(set) var info: String?

    init(auth: AuthRepository, profile: ProfileRepository) {
        self.auth = auth
        self.profile = profile
    }

    convenience init(_ c: AppContainer) {
        self.init(auth: c.auth, profile: c.profile)
    }

    func clearError() { error = nil }

    func clearMessages() {
        error = nil
        info = nil
    }

    private func launchBusy(_ block: @escaping @MainActor () async -> Void) {
        if busy { return }
        busy = true
        clearMessages()
        Task {
            await block()
            busy = false
        }
    }

    private func finish(_ call: () async throws -> AuthUser, _ onDone: (AuthUser) -> Void) async {
        do {
            let user = try await call()
            if user.isNew { profile.update { $0.name = user.name } } else { profile.suggestName(user.name) }
            onDone(user)
        } catch {
            self.error = error.authMessage()
        }
    }

    /// Kotlin's `dd/MM/uuuu` with ResolverStyle.STRICT: exactly 2, 2 and 4 ASCII digits and a real date.
    /// DateFormatter alone also takes "1/1/2000" or a 3-digit year, so the shape and a round trip are checked too.
    private func ageOf(_ dob: String) -> Int? {
        let text = dob.trimmingCharacters(in: .whitespacesAndNewlines)
        let shape = Array(text.utf8)
        let isDigit = { (b: UInt8) in b >= 0x30 && b <= 0x39 }
        guard shape.count == 10, shape[2] == 0x2F, shape[5] == 0x2F,
              [0, 1, 3, 4, 6, 7, 8, 9].allSatisfy({ isDigit(shape[$0]) }) else { return nil }
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.calendar = Calendar(identifier: .gregorian)
        f.dateFormat = "dd/MM/yyyy"
        f.isLenient = false
        guard let date = f.date(from: text), f.string(from: date) == text else { return nil }
        return Calendar(identifier: .gregorian).dateComponents([.year], from: date, to: Date()).year
    }

    private func isBlank(_ s: String) -> Bool {
        s.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    func signIn(email: String, pass: String, onDone: @escaping (AuthUser) -> Void) {
        if isBlank(email) || isBlank(pass) {
            error = "Enter your email and password."
            return
        }
        launchBusy { [self] in await finish({ try await auth.signIn(email: email, password: pass) }, onDone) }
    }

    func signUp(name: String, email: String, pass: String, confirm: String, dob: String, terms: Bool, onDone: @escaping (AuthUser) -> Void) {
        let age = ageOf(dob)
        var problem: String?
        if isBlank(name) { problem = "Enter your name." }
        else if isBlank(email) { problem = "Enter your email." }
        else if pass.utf16.count < 8 { problem = "Password must be at least 8 characters." } // Kotlin length counts UTF-16 units
        else if pass != confirm { problem = "Passwords don't match." }
        else if age == nil { problem = "Enter your date of birth as DD/MM/YYYY." }
        else if let age, age < 18 { problem = "You must be 18 or older to use Nya." }
        else if !terms { problem = "Please agree to the Terms and Privacy Policy." }
        if let problem {
            error = problem
            return
        }
        launchBusy { [self] in await finish({ try await auth.signUp(name: name, email: email, password: pass) }, onDone) }
    }

    func google(onDone: @escaping (AuthUser) -> Void) {
        launchBusy { [self] in await finish({ try await auth.signInWithGoogle(idToken: "") }, onDone) }
    }

    func checkVerified(onDone: @escaping () -> Void) {
        launchBusy { [self] in
            do {
                if try await auth.reloadVerified() { onDone() }
                else { error = "Not verified yet. Open the link in your email first." }
            } catch {
                self.error = error.authMessage()
            }
        }
    }

    func resend() {
        launchBusy { [self] in
            do {
                try await auth.resendVerification()
                info = "Verification email sent."
            } catch {
                self.error = error.authMessage()
            }
        }
    }

    func sendReset(email: String) {
        if isBlank(email) {
            error = "Enter your email."
            return
        }
        launchBusy { [self] in
            do {
                try await auth.sendReset(email: email)
                info = "Reset link sent to \(email.trimmingCharacters(in: .whitespacesAndNewlines)). Check your inbox."
            } catch {
                self.error = error.authMessage()
            }
        }
    }
}
