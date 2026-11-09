import Foundation

enum Plan: String, CaseIterable, Hashable, Sendable {
    case free = "Free", plus = "Plus", pro = "Pro"
}

/// Placeholder GATT identifiers until the firmware is final.
struct BleConfig: Hashable, Sendable {
    var namePrefix: String = "NYA"
    var service: UUID = UUID(uuidString: "6e5a1c3b-8f42-4d97-a3b0-2c7d91e4f058")!
    var storage: UUID = UUID(uuidString: "6e5a1c3c-8f42-4d97-a3b0-2c7d91e4f058")!
    var firmware: UUID = UUID(uuidString: "6e5a1c3d-8f42-4d97-a3b0-2c7d91e4f058")!
    var findMy: UUID = UUID(uuidString: "6e5a1c3e-8f42-4d97-a3b0-2c7d91e4f058")!
    var transfer: UUID = UUID(uuidString: "6e5a1c3f-8f42-4d97-a3b0-2c7d91e4f058")!
}

struct AiConfig: Hashable, Sendable {
    // Tried in order; a quota or server error (e.g. high demand) on one model moves on to the next.
    var models: [String] = ["gemini-3.8-flash", "gemini-3.7-flash", "gemini-3.5-flash-lite", "gemini-3.1-flash-lite"]
    var systemPrompt: String = "You are Nya, a warm AI companion inside a wearable memory headband app. " +
        "Reply in 1-3 short, conversational sentences of plain text with no markdown, lists or emojis, " +
        "because replies may be spoken aloud. Never claim to have done actions you cannot do."
}

struct CompanionConfig: Hashable, Sendable {
    var trait: String
    var tagline: String
}

/// Pitch of a voice style on top of the TTS voice, per gender.
struct VoiceStyle: Hashable, Sendable {
    var name: String
    var femalePitch: Float
    var malePitch: Float
}

struct PlanLimits: Hashable, Sendable {
    var dailyMessages: Int
}

struct FeatureFlags: Hashable, Sendable {
    var aiChat: Bool = true
    var voiceReplies: Bool = true
    var findMy: Bool = true
    var firmwareUpdates: Bool = true
    var share: Bool = true
}

/// Empty means not set; only http/https URLs are accepted.
struct LegalConfig: Hashable, Sendable {
    var privacyUrl: String = ""
    var termsUrl: String = ""
}

/// Remote settings; every default equals the value the app shipped with, so an empty document changes nothing.
struct AppConfig: Hashable, Sendable {
    var maintenance: Bool = false
    var maintenanceMessage: String = "Nya is getting an upgrade. Please check back soon."
    var minVersionCode: Int = 1
    var ble: BleConfig = BleConfig()
    var ai: AiConfig = AiConfig()
    var companions: [Companion: CompanionConfig] = [
        .wanderer: CompanionConfig(trait: "curious about places and discoveries", tagline: "Curious about places and discoveries"),
        .muse: CompanionConfig(trait: "attuned to style, taste and inspiration", tagline: "Style, taste and inspiration"),
        .guide: CompanionConfig(trait: "calm, giving clear answers", tagline: "Calm, clear answers"),
        .yogi: CompanionConfig(trait: "mindful and steady", tagline: "Mindful and steady"),
        .creator: CompanionConfig(trait: "full of ideas and eager to make things", tagline: "Ideas and making things"),
    ]
    var voiceStyles: [VoiceStyle] = [
        VoiceStyle(name: "Calm", femalePitch: 1.1, malePitch: 0.82),
        VoiceStyle(name: "Warm", femalePitch: 1.16, malePitch: 0.87),
        VoiceStyle(name: "Bright", femalePitch: 1.26, malePitch: 0.94),
        VoiceStyle(name: "Deep", femalePitch: 1.02, malePitch: 0.72),
    ]
    var voiceRate: Float = 0.85
    var plans: [Plan: PlanLimits] = [
        .free: PlanLimits(dailyMessages: 30),
        .plus: PlanLimits(dailyMessages: 200),
        .pro: PlanLimits(dailyMessages: 1000),
    ]
    var features: FeatureFlags = FeatureFlags()
    var legal: LegalConfig = LegalConfig()
    var firmwareLatest: String = "1.2.0"

    /// True when the installed build is older than the minimum the server allows.
    func updateRequired(_ installedVersionCode: Int) -> Bool { installedVersionCode < minVersionCode }
}

private let MaxName = 40
private let MaxText = 200
private let MaxPrompt = 4000
private let MaxUrl = 2000

/// Parses the `appConfig/main` document. Pure Swift so it can be unit-tested; never traps.
/// Each field falls back to its own default when missing, wrong-typed or out of range.
func parseAppConfig(_ data: [String: Any]) -> AppConfig {
    let d = AppConfig()
    return AppConfig(
        maintenance: data.bool("maintenance", d.maintenance),
        maintenanceMessage: data.str("maintenanceMessage", d.maintenanceMessage, MaxText),
        minVersionCode: data.int("minVersionCode", d.minVersionCode, 1),
        ble: parseBle(data.section("ble")),
        ai: parseAi(data.section("ai")),
        companions: parseCompanions(data.section("companions"), d.companions),
        voiceStyles: parseVoiceStyles(data["voiceStyles"], d.voiceStyles),
        voiceRate: data.float("voiceRate", d.voiceRate, 0.5, 2),
        plans: parsePlans(data.section("plans"), d.plans),
        features: parseFeatures(data.section("features")),
        legal: parseLegal(data.section("legal")),
        firmwareLatest: data.section("firmware").str("latest", d.firmwareLatest, MaxName)
    )
}

// MARK: - Value checks matching Kotlin's `as?` casts

// Firestore on iOS hands booleans and numbers over as NSNumber. Kotlin sees a Boolean as no Number and a
// number as no Boolean, so the two are told apart here instead of relying on Swift's bridging.
private func isBoolean(_ value: Any) -> Bool {
    guard let n = value as? NSNumber else { return false }
    return CFGetTypeID(n) == CFBooleanGetTypeID()
}

private func number(_ value: Any?) -> NSNumber? {
    guard let value, !isBoolean(value), let n = value as? NSNumber else { return nil }
    return n
}

/// Kotlin's `Number.toLong()`: truncates doubles, NaN becomes 0 and out-of-range values clamp.
private func toLong(_ n: NSNumber) -> Int64 {
    guard CFNumberIsFloatType(n as CFNumber) else { return n.int64Value }
    let v = n.doubleValue
    if v.isNaN { return 0 }
    if v >= 9.223372036854775807e18 { return .max }
    if v <= -9.223372036854775808e18 { return .min }
    return Int64(v.rounded(.towardZero))
}

/// Kotlin `String.trim()` and `length` (UTF-16 code units).
private func trimmed(_ s: String) -> String { s.trimmingCharacters(in: .whitespacesAndNewlines) }

private extension Optional where Wrapped == [String: Any] {
    func section(_ key: String) -> [String: Any]? { self?[key] as? [String: Any] }

    func str(_ key: String, _ fallback: String, _ max: Int) -> String {
        guard let raw = self?[key] as? String else { return fallback }
        let t = trimmed(raw)
        return !t.isEmpty && t.utf16.count <= max ? t : fallback
    }

    func bool(_ key: String, _ fallback: Bool) -> Bool {
        guard let value = self?[key], isBoolean(value), let n = value as? NSNumber else { return fallback }
        return n.boolValue
    }

    // Kotlin Int is 32-bit, so the upper bound is Int32.max, as in `min..Int.MAX_VALUE`.
    func int(_ key: String, _ fallback: Int, _ min: Int) -> Int {
        guard let n = number(self?[key]) else { return fallback }
        let v = toLong(n)
        return v >= Int64(min) && v <= Int64(Int32.max) ? Int(v) : fallback
    }

    func float(_ key: String, _ fallback: Float, _ min: Float, _ max: Float) -> Float {
        guard let n = number(self?[key]) else { return fallback }
        let v = n.floatValue
        return v >= min && v <= max ? v : fallback
    }

    /// Only the canonical lowercase form is accepted, like Kotlin's `UUID.fromString(...).toString() == text`.
    func uuid(_ key: String, _ fallback: UUID) -> UUID {
        guard let raw = self?[key] as? String else { return fallback }
        let text = trimmed(raw).lowercased()
        guard let id = UUID(uuidString: text), id.uuidString.lowercased() == text else { return fallback }
        return id
    }

    func url(_ key: String) -> String {
        let s = str(key, "", MaxUrl)
        return s.hasPrefix("https://") || s.hasPrefix("http://") ? s : ""
    }
}

private extension Dictionary where Key == String, Value == Any {
    func section(_ key: String) -> [String: Any]? { Optional(self).section(key) }
    func str(_ key: String, _ fallback: String, _ max: Int) -> String { Optional(self).str(key, fallback, max) }
    func bool(_ key: String, _ fallback: Bool) -> Bool { Optional(self).bool(key, fallback) }
    func int(_ key: String, _ fallback: Int, _ min: Int) -> Int { Optional(self).int(key, fallback, min) }
    func float(_ key: String, _ fallback: Float, _ min: Float, _ max: Float) -> Float {
        Optional(self).float(key, fallback, min, max)
    }
}

// MARK: - Sections

private func parseBle(_ m: [String: Any]?) -> BleConfig {
    let d = BleConfig()
    return BleConfig(
        namePrefix: m.str("namePrefix", d.namePrefix, MaxName),
        service: m.uuid("service", d.service),
        storage: m.uuid("storage", d.storage),
        firmware: m.uuid("firmware", d.firmware),
        findMy: m.uuid("findMy", d.findMy),
        transfer: m.uuid("transfer", d.transfer)
    )
}

private func parseAi(_ m: [String: Any]?) -> AiConfig {
    let d = AiConfig()
    let models = ((m?["models"] as? [Any]) ?? []).compactMap { item -> String? in
        guard let raw = item as? String else { return nil }
        let name = trimmed(raw)
        return !name.isEmpty && name.utf16.count <= MaxName * 2 ? name : nil
    }
    return AiConfig(models: models.isEmpty ? d.models : models, systemPrompt: m.str("systemPrompt", d.systemPrompt, MaxPrompt))
}

private func parseCompanions(_ m: [String: Any]?, _ defaults: [Companion: CompanionConfig]) -> [Companion: CompanionConfig] {
    defaults.reduce(into: [:]) { result, entry in
        let (companion, d) = entry
        let c = m.section(companion.rawValue)
        result[companion] = CompanionConfig(trait: c.str("trait", d.trait, MaxText), tagline: c.str("tagline", d.tagline, MaxText))
    }
}

// all or nothing: a saved voice index must keep meaning the same style
private func parseVoiceStyles(_ raw: Any?, _ fallback: [VoiceStyle]) -> [VoiceStyle] {
    guard let items = raw as? [Any], (1...6).contains(items.count) else { return fallback }
    var styles: [VoiceStyle] = []
    for item in items {
        let m = item as? [String: Any]
        let name = m.str("name", "", MaxName)
        let female = m.float("femalePitch", 0, 0.5, 2)
        let male = m.float("malePitch", 0, 0.5, 2)
        if name.isEmpty || female == 0 || male == 0 { return fallback }
        styles.append(VoiceStyle(name: name, femalePitch: female, malePitch: male))
    }
    return styles
}

private func parsePlans(_ m: [String: Any]?, _ defaults: [Plan: PlanLimits]) -> [Plan: PlanLimits] {
    defaults.reduce(into: [:]) { result, entry in
        let (plan, d) = entry
        result[plan] = PlanLimits(dailyMessages: m.section(plan.rawValue.lowercased()).int("dailyMessages", d.dailyMessages, 0))
    }
}

private func parseFeatures(_ m: [String: Any]?) -> FeatureFlags {
    let d = FeatureFlags()
    return FeatureFlags(
        aiChat: m.bool("aiChat", d.aiChat),
        voiceReplies: m.bool("voiceReplies", d.voiceReplies),
        findMy: m.bool("findMy", d.findMy),
        firmwareUpdates: m.bool("firmwareUpdates", d.firmwareUpdates),
        share: m.bool("share", d.share)
    )
}

private func parseLegal(_ m: [String: Any]?) -> LegalConfig {
    LegalConfig(privacyUrl: m.url("privacyUrl"), termsUrl: m.url("termsUrl"))
}
