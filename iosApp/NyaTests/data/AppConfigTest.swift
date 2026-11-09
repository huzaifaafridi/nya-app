import Foundation
import XCTest
@testable import Nya

/// Port of app/src/test/.../data/AppConfigTest.kt. The firmware catalog and system prompt cases come
/// across with those features (port phases 6 and 5).
final class AppConfigTest: XCTestCase {
    private let defaults = AppConfig()

    func testEmptyDocumentGivesAllDefaults() {
        XCTAssertEqual(defaults, parseAppConfig([:]))
    }

    func testFullValidDocumentIsParsed() {
        let uuid = "11111111-2222-3333-4444-555555555555"
        let c = parseAppConfig([
            "maintenance": true,
            "maintenanceMessage": "Back soon",
            "minVersionCode": 7,
            "firmware": ["latest": "2.0.1"],
            "ble": ["namePrefix": "ACME", "service": uuid, "transfer": uuid],
            "ai": ["models": ["m1", "m2"], "systemPrompt": "Be brief."] as [String: Any],
            "companions": ["Muse": ["trait": "bold", "tagline": "Boldly"], "Unknown": ["trait": "x"]] as [String: Any],
            "voiceStyles": [["name": "Soft", "femalePitch": 1.2, "malePitch": 0.9] as [String: Any]],
            "voiceRate": 1.0,
            "plans": ["plus": ["dailyMessages": 500]],
            "features": ["aiChat": false, "share": false],
            "legal": ["privacyUrl": "https://nya.app/privacy", "termsUrl": "http://nya.app/terms"],
        ])
        XCTAssertTrue(c.maintenance)
        XCTAssertEqual("Back soon", c.maintenanceMessage)
        XCTAssertEqual(7, c.minVersionCode)
        XCTAssertEqual("2.0.1", c.firmwareLatest)
        XCTAssertEqual("ACME", c.ble.namePrefix)
        XCTAssertEqual(UUID(uuidString: uuid), c.ble.service)
        XCTAssertEqual(defaults.ble.storage, c.ble.storage)
        XCTAssertEqual(["m1", "m2"], c.ai.models)
        XCTAssertEqual("Be brief.", c.ai.systemPrompt)
        XCTAssertEqual(CompanionConfig(trait: "bold", tagline: "Boldly"), c.companions[.muse])
        XCTAssertEqual(defaults.companions[.guide], c.companions[.guide])
        XCTAssertEqual([VoiceStyle(name: "Soft", femalePitch: 1.2, malePitch: 0.9)], c.voiceStyles)
        XCTAssertEqual(1, c.voiceRate)
        XCTAssertEqual(500, c.plans[.plus]?.dailyMessages)
        XCTAssertEqual(30, c.plans[.free]?.dailyMessages)
        XCTAssertFalse(c.features.aiChat)
        XCTAssertFalse(c.features.share)
        XCTAssertTrue(c.features.findMy)
        XCTAssertEqual("https://nya.app/privacy", c.legal.privacyUrl)
        XCTAssertEqual("http://nya.app/terms", c.legal.termsUrl)
    }

    func testBadFieldsFallBackIndividuallyAndKeepTheRest() {
        let c = parseAppConfig([
            "maintenance": "yes",
            "maintenanceMessage": 5,
            "minVersionCode": 0,
            "firmware": "1.0",
            "ble": ["namePrefix": "OK", "service": "not-a-uuid", "storage": 42] as [String: Any],
            "ai": ["models": [String](), "systemPrompt": String(repeating: "x", count: 5000)] as [String: Any],
            "voiceRate": 9.0,
            "plans": ["pro": ["dailyMessages": -1], "free": ["dailyMessages": 5]],
            "features": ["aiChat": "no", "findMy": false] as [String: Any],
            "legal": ["privacyUrl": "javascript:alert(1)", "termsUrl": "https://nya.app/terms"],
        ])
        XCTAssertFalse(c.maintenance)
        XCTAssertEqual(defaults.maintenanceMessage, c.maintenanceMessage)
        XCTAssertEqual(1, c.minVersionCode)
        XCTAssertEqual(defaults.firmwareLatest, c.firmwareLatest)
        XCTAssertEqual("OK", c.ble.namePrefix)
        XCTAssertEqual(defaults.ble.service, c.ble.service)
        XCTAssertEqual(defaults.ble.storage, c.ble.storage)
        XCTAssertEqual(defaults.ai, c.ai)
        XCTAssertEqual(defaults.voiceRate, c.voiceRate)
        XCTAssertEqual(1000, c.plans[.pro]?.dailyMessages)
        XCTAssertEqual(5, c.plans[.free]?.dailyMessages)
        XCTAssertTrue(c.features.aiChat)
        XCTAssertFalse(c.features.findMy)
        XCTAssertEqual("", c.legal.privacyUrl)
        XCTAssertEqual("https://nya.app/terms", c.legal.termsUrl)
    }

    func testModelListKeepsOnlyValidNames() {
        let c = parseAppConfig(["ai": ["models": ["a", 3, "", "b"] as [Any]]])
        XCTAssertEqual(["a", "b"], c.ai.models)
    }

    func testNumbersMayBeLongOrDouble() {
        let asLong = parseAppConfig(["minVersionCode": Int64(3), "voiceRate": Int64(1)])
        let asDouble = parseAppConfig(["minVersionCode": 3.0, "voiceRate": 1.0])
        XCTAssertEqual(asLong, asDouble)
        XCTAssertEqual(3, asLong.minVersionCode)
    }

    func testVoiceStylesNeedOneToSixValidEntries() {
        func style(_ i: Int) -> [String: Any] { ["name": "S\(i)", "femalePitch": 1.0, "malePitch": 1.0] }
        XCTAssertEqual(defaults.voiceStyles, parseAppConfig(["voiceStyles": [Any]()]).voiceStyles)
        XCTAssertEqual(defaults.voiceStyles, parseAppConfig(["voiceStyles": (0..<7).map(style)]).voiceStyles)
        XCTAssertEqual(6, parseAppConfig(["voiceStyles": (0..<6).map(style)]).voiceStyles.count)
        var badPitch = style(0)
        badPitch["femalePitch"] = 3.0
        XCTAssertEqual(defaults.voiceStyles, parseAppConfig(["voiceStyles": [style(1), badPitch]]).voiceStyles)
    }

    func testUpdateRequiredComparesInstalledVersion() {
        var c = AppConfig()
        c.minVersionCode = 5
        XCTAssertTrue(c.updateRequired(4))
        XCTAssertFalse(c.updateRequired(5))
    }

    // iOS only: Firestore hands booleans and numbers over as NSNumber. Kotlin never treats one as the other.
    func testBooleansAndNumbersAreNotMixedUp() {
        let c = parseAppConfig([
            "maintenance": NSNumber(value: 1),
            "minVersionCode": NSNumber(value: true),
            "voiceRate": NSNumber(value: true),
            "features": ["aiChat": NSNumber(value: 0)],
        ])
        XCTAssertFalse(c.maintenance)
        XCTAssertEqual(defaults.minVersionCode, c.minVersionCode)
        XCTAssertEqual(defaults.voiceRate, c.voiceRate)
        XCTAssertTrue(c.features.aiChat)
        XCTAssertTrue(parseAppConfig(["maintenance": NSNumber(value: true)]).maintenance)
    }

    // iOS only: Swift traps on Int64(.infinity); Kotlin's toLong clamps, and NaN becomes 0.
    func testNonFiniteAndHugeNumbersNeverTrap() {
        let c = parseAppConfig([
            "minVersionCode": Double.infinity,
            "voiceRate": Double.nan,
            "plans": ["free": ["dailyMessages": Double.nan], "plus": ["dailyMessages": 1e30], "pro": ["dailyMessages": 12.9]],
        ])
        XCTAssertEqual(defaults.minVersionCode, c.minVersionCode)
        XCTAssertEqual(defaults.voiceRate, c.voiceRate)
        XCTAssertEqual(0, c.plans[.free]?.dailyMessages) // Kotlin quirk: NaN.toLong() == 0, which is in range
        XCTAssertEqual(200, c.plans[.plus]?.dailyMessages)
        XCTAssertEqual(12, c.plans[.pro]?.dailyMessages)
        XCTAssertEqual(defaults.minVersionCode, parseAppConfig(["minVersionCode": Int64(Int32.max) + 1]).minVersionCode)
    }

    // iOS only: UUIDs must be in canonical form, as Kotlin's toString() round trip requires.
    func testUuidsAcceptCanonicalFormInAnyCaseOnly() {
        let c = parseAppConfig(["ble": [
            "service": " 11111111-2222-3333-4444-555555555555 ",
            "storage": "11111111-2222-3333-4444-55555555555G",
            "firmware": "AAAAAAAA-2222-3333-4444-555555555555",
        ]])
        XCTAssertEqual(UUID(uuidString: "11111111-2222-3333-4444-555555555555"), c.ble.service)
        XCTAssertEqual(defaults.ble.storage, c.ble.storage)
        XCTAssertEqual(UUID(uuidString: "aaaaaaaa-2222-3333-4444-555555555555"), c.ble.firmware)
    }
}
