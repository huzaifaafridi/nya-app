package com.nya.app.data

interface MediaStore {
    /** Returns a cloud reference for a local photo or video, or null when the media stays on the phone. */
    suspend fun publish(localUri: String): String?
}

/** Keeps media on the phone. The Cloud Storage implementation plugs in here later (Phase 14). */
class LocalMediaStore : MediaStore {
    override suspend fun publish(localUri: String): String? = null
}
