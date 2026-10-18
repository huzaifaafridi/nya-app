package com.nya.app.data

enum class Companion { Wanderer, Muse, Guide, Yogi, Creator }

enum class Gender { Female, Male }

enum class Scene { Sea, Forest, Dusk, Mount, Yoga, City }

data class Prefs(
    val name: String = "",
    val companion: Companion = Companion.Muse,
    val skin: Int = 0xFFE0AC84.toInt(), // ARGB
    val gender: Gender = Gender.Female,
    val voice: Int = 0,
    val theme: Int = 0, // 0 Auto, 1 Light, 2 Dark
    val quiet: Boolean = false,
    val mode: Int = 2, // 0 Travel, 1 Focus, 2 Wellness, 3 Creator
    val onboarded: Boolean = false
)

data class Memory(
    val id: Long,
    val title: String,
    val place: String,
    val note: String,
    val scene: Scene,
    val createdAt: Long, // epoch ms
    val clipSeconds: Int? = null, // null = photo
    val mediaUri: String? = null
)

data class ChatMessage(val id: Long, val text: String, val mine: Boolean)

data class DeviceInfo(
    val name: String,
    val link: String,
    val battery: Float,
    val storageUsedGb: Float,
    val storageTotalGb: Float,
    val firmware: String,
    val latestFirmware: String?
)

data class TransferItem(val title: String, val sizeMb: Int, val progress: Float)
