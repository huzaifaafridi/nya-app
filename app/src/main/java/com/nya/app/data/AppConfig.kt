package com.nya.app.data

import java.util.UUID

enum class Plan { Free, Plus, Pro }

/** Placeholder GATT identifiers until the firmware is final. */
data class BleConfig(
    val namePrefix: String = "NYA",
    val service: UUID = UUID.fromString("6e5a1c3b-8f42-4d97-a3b0-2c7d91e4f058"),
    val storage: UUID = UUID.fromString("6e5a1c3c-8f42-4d97-a3b0-2c7d91e4f058"),
    val firmware: UUID = UUID.fromString("6e5a1c3d-8f42-4d97-a3b0-2c7d91e4f058"),
    val findMy: UUID = UUID.fromString("6e5a1c3e-8f42-4d97-a3b0-2c7d91e4f058"),
    val transfer: UUID = UUID.fromString("6e5a1c3f-8f42-4d97-a3b0-2c7d91e4f058")
)

data class AiConfig(
    // Tried in order; a quota or server error (e.g. high demand) on one model moves on to the next.
    val models: List<String> = listOf("gemini-3.8-flash", "gemini-3.7-flash", "gemini-3.5-flash-lite", "gemini-3.1-flash-lite"),
    val systemPrompt: String = "You are Nya, a warm AI companion inside a wearable memory headband app. " +
        "Reply in 1-3 short, conversational sentences of plain text with no markdown, lists or emojis, " +
        "because replies may be spoken aloud. Never claim to have done actions you cannot do."
)

data class CompanionConfig(val trait: String, val tagline: String)

/** Pitch of a voice style on top of the TTS voice, per gender. */
data class VoiceStyle(val name: String, val femalePitch: Float, val malePitch: Float)

data class PlanLimits(val dailyMessages: Int)

data class FeatureFlags(
    val aiChat: Boolean = true,
    val voiceReplies: Boolean = true,
    val findMy: Boolean = true,
    val firmwareUpdates: Boolean = true,
    val share: Boolean = true
)

/** Empty means not set; only http/https URLs are accepted. */
data class LegalConfig(val privacyUrl: String = "", val termsUrl: String = "")

/** Remote settings; every default equals the value the app shipped with, so an empty document changes nothing. */
data class AppConfig(
    val maintenance: Boolean = false,
    val maintenanceMessage: String = "Nya is getting an upgrade. Please check back soon.",
    val minVersionCode: Int = 1,
    val ble: BleConfig = BleConfig(),
    val ai: AiConfig = AiConfig(),
    val companions: Map<Companion, CompanionConfig> = mapOf(
        Companion.Wanderer to CompanionConfig("curious about places and discoveries", "Curious about places and discoveries"),
        Companion.Muse to CompanionConfig("attuned to style, taste and inspiration", "Style, taste and inspiration"),
        Companion.Guide to CompanionConfig("calm, giving clear answers", "Calm, clear answers"),
        Companion.Yogi to CompanionConfig("mindful and steady", "Mindful and steady"),
        Companion.Creator to CompanionConfig("full of ideas and eager to make things", "Ideas and making things")
    ),
    val voiceStyles: List<VoiceStyle> = listOf(
        VoiceStyle("Calm", 1.1f, .82f),
        VoiceStyle("Warm", 1.16f, .87f),
        VoiceStyle("Bright", 1.26f, .94f),
        VoiceStyle("Deep", 1.02f, .72f)
    ),
    val voiceRate: Float = .85f,
    val plans: Map<Plan, PlanLimits> = mapOf(
        Plan.Free to PlanLimits(30),
        Plan.Plus to PlanLimits(200),
        Plan.Pro to PlanLimits(1000)
    ),
    val features: FeatureFlags = FeatureFlags(),
    val legal: LegalConfig = LegalConfig(),
    val firmwareLatest: String = "1.2.0"
) {
    /** True when the installed build is older than the minimum the server allows. */
    fun updateRequired(installedVersionCode: Int) = installedVersionCode < minVersionCode
}

private const val MaxName = 40
private const val MaxText = 200
private const val MaxPrompt = 4000
private const val MaxUrl = 2000

/**
 * Parses the `appConfig/main` document. Pure Kotlin so it can be unit-tested; never throws.
 * Each field falls back to its own default when missing, wrong-typed or out of range.
 */
fun parseAppConfig(data: Map<String, Any?>): AppConfig {
    val d = AppConfig()
    return AppConfig(
        maintenance = data.bool("maintenance", d.maintenance),
        maintenanceMessage = data.str("maintenanceMessage", d.maintenanceMessage, MaxText),
        minVersionCode = data.int("minVersionCode", d.minVersionCode, 1),
        ble = parseBle(data.map("ble")),
        ai = parseAi(data.map("ai")),
        companions = parseCompanions(data.map("companions"), d.companions),
        voiceStyles = parseVoiceStyles(data["voiceStyles"], d.voiceStyles),
        voiceRate = data.float("voiceRate", d.voiceRate, .5f, 2f),
        plans = parsePlans(data.map("plans"), d.plans),
        features = parseFeatures(data.map("features")),
        legal = parseLegal(data.map("legal")),
        firmwareLatest = data.map("firmware").str("latest", d.firmwareLatest, MaxName)
    )
}

@Suppress("UNCHECKED_CAST")
private fun Map<String, Any?>?.map(key: String) = this?.get(key) as? Map<String, Any?>

private fun Map<String, Any?>?.str(key: String, default: String, max: Int) =
    (this?.get(key) as? String)?.trim()?.takeIf { it.isNotEmpty() && it.length <= max } ?: default

private fun Map<String, Any?>?.bool(key: String, default: Boolean) = this?.get(key) as? Boolean ?: default

private fun Map<String, Any?>?.int(key: String, default: Int, min: Int) =
    (this?.get(key) as? Number)?.toLong()?.takeIf { it in min..Int.MAX_VALUE }?.toInt() ?: default

private fun Map<String, Any?>?.float(key: String, default: Float, min: Float, max: Float) =
    (this?.get(key) as? Number)?.toFloat()?.takeIf { it in min..max } ?: default

private fun Map<String, Any?>?.uuid(key: String, default: UUID): UUID {
    val text = (this?.get(key) as? String)?.trim()?.lowercase() ?: return default
    return runCatching { UUID.fromString(text) }.getOrNull()?.takeIf { it.toString() == text } ?: default
}

private fun Map<String, Any?>?.url(key: String) =
    str(key, "", MaxUrl).takeIf { it.startsWith("https://") || it.startsWith("http://") } ?: ""

private fun parseBle(m: Map<String, Any?>?): BleConfig {
    val d = BleConfig()
    return BleConfig(
        namePrefix = m.str("namePrefix", d.namePrefix, MaxName),
        service = m.uuid("service", d.service),
        storage = m.uuid("storage", d.storage),
        firmware = m.uuid("firmware", d.firmware),
        findMy = m.uuid("findMy", d.findMy),
        transfer = m.uuid("transfer", d.transfer)
    )
}

private fun parseAi(m: Map<String, Any?>?): AiConfig {
    val d = AiConfig()
    val models = (m?.get("models") as? List<*>).orEmpty()
        .mapNotNull { (it as? String)?.trim()?.takeIf { name -> name.isNotEmpty() && name.length <= MaxName * 2 } }
    return AiConfig(models.ifEmpty { d.models }, m.str("systemPrompt", d.systemPrompt, MaxPrompt))
}

private fun parseCompanions(m: Map<String, Any?>?, defaults: Map<Companion, CompanionConfig>) =
    defaults.mapValues { (companion, d) ->
        val c = m.map(companion.name)
        CompanionConfig(c.str("trait", d.trait, MaxText), c.str("tagline", d.tagline, MaxText))
    }

// all or nothing: a saved voice index must keep meaning the same style
private fun parseVoiceStyles(raw: Any?, default: List<VoiceStyle>): List<VoiceStyle> {
    val items = raw as? List<*> ?: return default
    if (items.size !in 1..6) return default
    return items.map { item ->
        @Suppress("UNCHECKED_CAST")
        val m = item as? Map<String, Any?>
        val name = m.str("name", "", MaxName)
        val female = m.float("femalePitch", 0f, .5f, 2f)
        val male = m.float("malePitch", 0f, .5f, 2f)
        if (name.isEmpty() || female == 0f || male == 0f) return default
        VoiceStyle(name, female, male)
    }
}

private fun parsePlans(m: Map<String, Any?>?, defaults: Map<Plan, PlanLimits>) =
    defaults.mapValues { (plan, d) ->
        PlanLimits(m.map(plan.name.lowercase()).int("dailyMessages", d.dailyMessages, 0))
    }

private fun parseFeatures(m: Map<String, Any?>?): FeatureFlags {
    val d = FeatureFlags()
    return FeatureFlags(
        aiChat = m.bool("aiChat", d.aiChat),
        voiceReplies = m.bool("voiceReplies", d.voiceReplies),
        findMy = m.bool("findMy", d.findMy),
        firmwareUpdates = m.bool("firmwareUpdates", d.firmwareUpdates),
        share = m.bool("share", d.share)
    )
}

private fun parseLegal(m: Map<String, Any?>?) = LegalConfig(m.url("privacyUrl"), m.url("termsUrl"))
