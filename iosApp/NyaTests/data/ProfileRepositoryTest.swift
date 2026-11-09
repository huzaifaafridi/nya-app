import Foundation
import XCTest
@testable import Nya

/// The parts of app/src/test/.../data/ProfileRepositoryTest.kt that apply before storage is ported (phase 4):
/// suggestName, name capping and the new notifications field. Each test uses its own UserDefaults suite,
/// never .standard, so the onboarding UI test still starts from a fresh app.
@MainActor
final class ProfileRepositoryTest: XCTestCase {
    private var suite: String!
    private var store: UserDefaults!

    override func setUp() async throws {
        suite = "ProfileRepositoryTest-\(UUID().uuidString)"
        store = UserDefaults(suiteName: suite)
    }

    override func tearDown() async throws {
        store.removePersistentDomain(forName: suite)
    }

    private func repo() -> UserDefaultsProfileRepository { UserDefaultsProfileRepository(store: store) }

    func testSuggestNameFillsABlankName() {
        let repo = repo()
        repo.suggestName("Ada")
        XCTAssertEqual("Ada", repo.profile.name)
        XCTAssertEqual("Ada", self.repo().profile.name) // saved
    }

    func testSuggestNameNeverOverwritesAName() {
        repo().update { $0.name = "Zed" }
        let repo = repo()
        repo.suggestName("Ada")
        XCTAssertEqual("Zed", repo.profile.name)
    }

    func testSuggestNameTreatsWhitespaceAsBlank() {
        repo().update { $0.name = "   " }
        let repo = repo()
        repo.suggestName("Ada")
        XCTAssertEqual("Ada", repo.profile.name)
    }

    func testLongNamesAreCappedByUpdateAndSuggestName() {
        let long = " " + String(repeating: "x", count: 150)
        repo().suggestName(long)
        XCTAssertEqual(String(repeating: "x", count: MaxNameLength), repo().profile.name)
        repo().update { $0.name = long }
        XCTAssertEqual(String(repeating: "x", count: MaxNameLength), repo().profile.name)
    }

    func testCappingNeverSplitsACharacter() {
        let name = String(repeating: "x", count: 99) + "😀"
        XCTAssertEqual(String(repeating: "x", count: 99), cappedName(name))
        XCTAssertLessThanOrEqual(cappedName(String(repeating: "😀", count: 80)).utf16.count, MaxNameLength)
    }

    func testNotificationsDefaultOnAndPersist() {
        XCTAssertTrue(repo().profile.notifications)
        repo().update { $0.notifications = false }
        XCTAssertFalse(repo().profile.notifications)
    }

    func testFakeSuggestNameMatchesKotlin() {
        let fake = FakeProfileRepository()
        fake.suggestName("Ada")
        XCTAssertEqual("Ada", fake.profile.name)
        fake.suggestName("Bo")
        XCTAssertEqual("Ada", fake.profile.name)
    }
}
