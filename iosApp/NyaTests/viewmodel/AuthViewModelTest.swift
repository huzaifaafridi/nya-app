import Foundation
import XCTest
@testable import Nya

/// Port of app/src/test/.../viewmodel/AuthViewModelTest.kt.
@MainActor
final class AuthViewModelTest: XCTestCase {
    /// Kotlin's FakeAuthRepository wrapped by CountingAuth. Kept in memory: the app's FakeAuthRepository saves
    /// to UserDefaults.standard, which would leave the onboarding UI test signed in.
    private final class CountingAuth: AuthRepository {
        var calls = 0
        var uid: String?
        var onUidChange: ((String?) -> Void)?
        var isSignedIn: Bool { uid != nil }

        func signIn(email: String, password: String) async throws -> AuthUser {
            calls += 1
            uid = "fake-user"
            return AuthUser(name: nameFromEmail(email), email: email, verified: true, isNew: false)
        }

        func signUp(name: String, email: String, password: String) async throws -> AuthUser {
            calls += 1
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
        func signOut() async { uid = nil }
    }

    private var auth: CountingAuth!
    private var profile: FakeProfileRepository!
    private var vm: AuthViewModel!
    private var done = 0

    override func setUp() async throws {
        auth = CountingAuth()
        profile = FakeProfileRepository()
        vm = AuthViewModel(auth: auth, profile: profile)
        done = 0
    }

    private func signUp(
        name: String = "Ada",
        email: String = "ada@nya.app",
        pass: String = "password1",
        confirm: String? = nil,
        dob: String = "09/02/2004",
        terms: Bool = true
    ) {
        vm.signUp(name: name, email: email, pass: pass, confirm: confirm ?? pass, dob: dob, terms: terms) { _ in self.done += 1 }
    }

    /// Kotlin's advanceUntilIdle: the view model runs each call in a Task and clears `busy` at the end.
    private func settle() async {
        for _ in 0..<1000 where vm.busy { await Task.yield() }
        XCTAssertFalse(vm.busy, "the auth call never finished")
    }

    private func assertRejected(_ message: String, file: StaticString = #filePath, line: UInt = #line) {
        XCTAssertEqual(message, vm.error, file: file, line: line)
        XCTAssertEqual(0, auth.calls, file: file, line: line)
        XCTAssertEqual(0, done, file: file, line: line)
    }

    func testBlankNameIsRejected() {
        signUp(name: " ")
        assertRejected("Enter your name.")
    }

    func testBlankEmailIsRejected() {
        signUp(email: "")
        assertRejected("Enter your email.")
    }

    func testShortPasswordIsRejected() {
        signUp(pass: "short12")
        assertRejected("Password must be at least 8 characters.")
    }

    func testMismatchedPasswordsAreRejected() {
        signUp(confirm: "password2")
        assertRejected("Passwords don't match.")
    }

    func testMalformedBirthDatesAreRejected() {
        for dob in ["31/02/2000", "1/1/2000", "09/02/200", "", "09/02/20000", "09-02-2004", "٠٩/٠٢/٢٠٠٤"] {
            signUp(dob: dob)
            assertRejected("Enter your date of birth as DD/MM/YYYY.")
            vm.clearError()
        }
    }

    func testUnderageUserIsRejected() {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.calendar = Calendar(identifier: .gregorian)
        f.dateFormat = "dd/MM/yyyy"
        let seventeen = Calendar(identifier: .gregorian).date(byAdding: .year, value: -17, to: Date())!
        signUp(dob: f.string(from: seventeen))
        assertRejected("You must be 18 or older to use Nya.")
    }

    func testUncheckedTermsAreRejected() {
        signUp(terms: false)
        assertRejected("Please agree to the Terms and Privacy Policy.")
    }

    func testValidSignUpCallsOnDoneAndSavesName() async {
        signUp(name: " Ada ")
        await settle()
        XCTAssertEqual(1, done)
        XCTAssertEqual(1, auth.calls)
        XCTAssertEqual("Ada", profile.profile.name)
        XCTAssertNil(vm.error)
    }

    func testSignInKeepsTheAccountName() async {
        profile.update { $0.name = "Zed" }
        vm.signIn(email: "ada@nya.app", pass: "password1") { _ in self.done += 1 }
        await settle()
        XCTAssertEqual(1, done)
        XCTAssertEqual("Zed", profile.profile.name)
    }

    func testSignInNamesAnAccountWithoutOne() async {
        vm.signIn(email: "ada@nya.app", pass: "password1") { _ in self.done += 1 }
        await settle()
        XCTAssertEqual("Ada", profile.profile.name)
    }

    func testSignUpOverwritesTheName() async {
        profile.update { $0.name = "Zed" }
        signUp(name: "Ada")
        await settle()
        XCTAssertEqual("Ada", profile.profile.name)
    }

    func testBlankSignInFieldsAreRejected() {
        vm.signIn(email: "ada@nya.app", pass: "") { _ in self.done += 1 }
        assertRejected("Enter your email and password.")
    }

    func testClearErrorRemovesTheMessage() {
        signUp(name: "")
        vm.clearError()
        XCTAssertNil(vm.error)
    }
}
