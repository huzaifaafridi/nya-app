package com.nya.app.data

import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppConfigTest {
    private val defaults = AppConfig()

    @Test
    fun emptyDocumentGivesAllDefaults() {
        assertEquals(defaults, parseAppConfig(emptyMap()))
    }

    @Test
    fun fullValidDocumentIsParsed() {
        val uuid = "11111111-2222-3333-4444-555555555555"
        val c = parseAppConfig(
            mapOf(
                "maintenance" to true,
                "maintenanceMessage" to "Back soon",
                "minVersionCode" to 7L,
                "firmware" to mapOf("latest" to "2.0.1"),
                "ble" to mapOf("namePrefix" to "ACME", "service" to uuid, "transfer" to uuid),
                "ai" to mapOf("models" to listOf("m1", "m2"), "systemPrompt" to "Be brief."),
                "companions" to mapOf("Muse" to mapOf("trait" to "bold", "tagline" to "Boldly"), "Unknown" to mapOf("trait" to "x")),
                "voiceStyles" to listOf(mapOf("name" to "Soft", "femalePitch" to 1.2, "malePitch" to 0.9)),
                "voiceRate" to 1.0,
                "plans" to mapOf("plus" to mapOf("dailyMessages" to 500L)),
                "features" to mapOf("aiChat" to false, "share" to false),
                "legal" to mapOf("privacyUrl" to "https://nya.app/privacy", "termsUrl" to "http://nya.app/terms")
            )
        )
        assertTrue(c.maintenance)
        assertEquals("Back soon", c.maintenanceMessage)
        assertEquals(7, c.minVersionCode)
        assertEquals("2.0.1", c.firmwareLatest)
        assertEquals("ACME", c.ble.namePrefix)
        assertEquals(UUID.fromString(uuid), c.ble.service)
        assertEquals(defaults.ble.storage, c.ble.storage)
        assertEquals(listOf("m1", "m2"), c.ai.models)
        assertEquals("Be brief.", c.ai.systemPrompt)
        assertEquals(CompanionConfig("bold", "Boldly"), c.companions.getValue(Companion.Muse))
        assertEquals(defaults.companions.getValue(Companion.Guide), c.companions.getValue(Companion.Guide))
        assertEquals(listOf(VoiceStyle("Soft", 1.2f, 0.9f)), c.voiceStyles)
        assertEquals(1f, c.voiceRate, 0f)
        assertEquals(500, c.plans.getValue(Plan.Plus).dailyMessages)
        assertEquals(30, c.plans.getValue(Plan.Free).dailyMessages)
        assertFalse(c.features.aiChat)
        assertFalse(c.features.share)
        assertTrue(c.features.findMy)
        assertEquals("https://nya.app/privacy", c.legal.privacyUrl)
        assertEquals("http://nya.app/terms", c.legal.termsUrl)
    }

    @Test
    fun badFieldsFallBackIndividuallyAndKeepTheRest() {
        val c = parseAppConfig(
            mapOf(
                "maintenance" to "yes",
                "maintenanceMessage" to 5,
                "minVersionCode" to 0,
                "firmware" to "1.0",
                "ble" to mapOf("namePrefix" to "OK", "service" to "not-a-uuid", "storage" to 42),
                "ai" to mapOf("models" to emptyList<String>(), "systemPrompt" to "x".repeat(5000)),
                "voiceRate" to 9.0,
                "plans" to mapOf("pro" to mapOf("dailyMessages" to -1), "free" to mapOf("dailyMessages" to 5)),
                "features" to mapOf("aiChat" to "no", "findMy" to false),
                "legal" to mapOf("privacyUrl" to "javascript:alert(1)", "termsUrl" to "https://nya.app/terms")
            )
        )
        assertFalse(c.maintenance)
        assertEquals(defaults.maintenanceMessage, c.maintenanceMessage)
        assertEquals(1, c.minVersionCode)
        assertEquals(defaults.firmwareLatest, c.firmwareLatest)
        assertEquals("OK", c.ble.namePrefix)
        assertEquals(defaults.ble.service, c.ble.service)
        assertEquals(defaults.ble.storage, c.ble.storage)
        assertEquals(defaults.ai, c.ai)
        assertEquals(defaults.voiceRate, c.voiceRate, 0f)
        assertEquals(1000, c.plans.getValue(Plan.Pro).dailyMessages)
        assertEquals(5, c.plans.getValue(Plan.Free).dailyMessages)
        assertTrue(c.features.aiChat)
        assertFalse(c.features.findMy)
        assertEquals("", c.legal.privacyUrl)
        assertEquals("https://nya.app/terms", c.legal.termsUrl)
    }

    @Test
    fun modelListKeepsOnlyValidNames() {
        val c = parseAppConfig(mapOf("ai" to mapOf("models" to listOf("a", 3, "", "b"))))
        assertEquals(listOf("a", "b"), c.ai.models)
    }

    @Test
    fun numbersMayBeLongOrDouble() {
        val asLong = parseAppConfig(mapOf("minVersionCode" to 3L, "voiceRate" to 1L))
        val asDouble = parseAppConfig(mapOf("minVersionCode" to 3.0, "voiceRate" to 1.0))
        assertEquals(asLong, asDouble)
        assertEquals(3, asLong.minVersionCode)
    }

    @Test
    fun voiceStylesNeedOneToSixValidEntries() {
        fun style(i: Int) = mapOf("name" to "S$i", "femalePitch" to 1.0, "malePitch" to 1.0)
        assertEquals(defaults.voiceStyles, parseAppConfig(mapOf("voiceStyles" to emptyList<Any>())).voiceStyles)
        assertEquals(defaults.voiceStyles, parseAppConfig(mapOf("voiceStyles" to List(7, ::style))).voiceStyles)
        assertEquals(6, parseAppConfig(mapOf("voiceStyles" to List(6, ::style))).voiceStyles.size)
        val badPitch = style(0) + ("femalePitch" to 3.0)
        assertEquals(defaults.voiceStyles, parseAppConfig(mapOf("voiceStyles" to listOf(style(1), badPitch))).voiceStyles)
    }

    @Test
    fun updateRequiredComparesInstalledVersion() {
        val c = AppConfig(minVersionCode = 5)
        assertTrue(c.updateRequired(4))
        assertFalse(c.updateRequired(5))
    }

    @Test
    fun firmwareCatalogReadsLatestFromConfig() = runTest {
        val flow = MutableStateFlow(AppConfig(firmwareLatest = "1.3.0"))
        val catalog = ConfigFirmwareCatalog(object : AppConfigRepository {
            override val config: StateFlow<AppConfig> = flow
        })
        assertEquals("1.3.0", catalog.latest())
        flow.value = AppConfig(firmwareLatest = "1.4.0")
        assertEquals("1.4.0", catalog.latest())
    }

    @Test
    fun defaultPromptMatchesTheOriginalText() {
        val prompt = buildSystemPrompt(defaults, ChatPersona(Companion.Wanderer, "Sam", 0))
        assertTrue(prompt.startsWith("You are Nya, a warm AI companion inside a wearable memory headband app. "))
        assertTrue(prompt.contains("Your personality is Wanderer: curious about places and discoveries. The user's name is Sam. The current mode is Travel."))
        assertTrue(prompt.contains("Never claim to have done actions you cannot do."))
    }

    @Test
    fun promptUsesConfiguredTraitAndSkipsBlankName() {
        val config = AppConfig(
            ai = AiConfig(systemPrompt = "Custom."),
            companions = defaults.companions + (Companion.Yogi to CompanionConfig("zen", "Zen"))
        )
        assertEquals(
            "Custom. Your personality is Yogi: zen. The current mode is Focus.",
            buildSystemPrompt(config, ChatPersona(Companion.Yogi, " ", 1))
        )
    }
}
