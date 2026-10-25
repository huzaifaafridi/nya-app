package com.nya.app.data

interface FirmwareCatalog {
    /** The newest published firmware version, or null if none could be found. */
    suspend fun latest(): String?
}

/** Latest firmware version published in the remote config. */
class ConfigFirmwareCatalog(private val appConfig: AppConfigRepository) : FirmwareCatalog {
    override suspend fun latest(): String? = appConfig.config.value.firmwareLatest
}

/** Compares dotted versions numerically, so "1.10.0" is newer than "1.9.0". */
fun isNewerVersion(candidate: String, current: String): Boolean {
    val a = candidate.split('.').map { it.toIntOrNull() ?: 0 }
    val b = current.split('.').map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(a.size, b.size)) {
        val d = a.getOrElse(i) { 0 }.compareTo(b.getOrElse(i) { 0 })
        if (d != 0) return d > 0
    }
    return false
}
