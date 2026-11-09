import Foundation
import Observation

@MainActor @Observable
final class AuthViewModel {
    @ObservationIgnored private let auth: AuthRepository
    @ObservationIgnored private let profile: ProfileRepository
    private(set) var busy = false
    private(set) var error: String?
    private(set) var info: String?

    init(_ c: AppContainer) {
        auth = c.auth
        profile = c.profile
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
            profile.update { $0.name = user.name }
            onDone(user)
        } catch {
            self.error = error.authMessage()
        }
    }

    private func ageOf(_ dob: String) -> Int? {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.calendar = Calendar(identifier: .gregorian)
        f.dateFormat = "dd/MM/yyyy"
        f.isLenient = false
        guard let date = f.date(from: dob.trimmingCharacters(in: .whitespaces)) else { return nil }
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
        else if pass.count < 8 { problem = "Password must be at least 8 characters." }
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
