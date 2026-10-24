package com.nya.app.data

import kotlinx.coroutines.delay

interface FirmwareCatalog {
    /** The newest published firmware version, or null if none could be found. */
    suspend fun latest(): String?
}

/** Fake catalog; Phase 10 moves this to appConfig. */
class SimulatedFirmwareCatalog : FirmwareCatalog {
    override suspend fun latest(): String? {
        delay(800)
        return "1.2.0"
    }
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
