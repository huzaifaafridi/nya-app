package com.nya.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

/** A day's AI message allowance. A limit of 0 means unlimited. */
data class ChatQuota(val used: Int, val limit: Int) {
    val unlimited get() = limit == 0
    val remaining get() = (limit - used).coerceAtLeast(0)
    val reached get() = !unlimited && used >= limit
}

interface UsageRepository {
    /** AI messages the signed-in user sent today (local date); starts again at local midnight. */
    fun today(): Flow<Int>
    suspend fun recordMessage()
}

/** Local only, so it can be cleared with the app data; Phase 14 moves enforcement server-side. */
@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreUsageRepository(
    private val store: DataStore<Preferences>,
    private val uid: Flow<String?>,
    private val clock: Clock = Clock.systemDefaultZone()
) : UsageRepository {
    override fun today(): Flow<Int> = uid.flatMapLatest { id ->
        if (id == null) flowOf(0)
        else combine(days(), store.safeData()) { day, p -> countOn(p[key(id)], day) }
    }

    override suspend fun recordMessage() {
        val id = uid.first() ?: return
        val day = LocalDate.now(clock)
        store.edit { it[key(id)] = "$day|${countOn(it[key(id)], day) + 1}" }
    }

    /** Emits today's date now and again at each local midnight. */
    private fun days() = flow {
        while (true) {
            val now = ZonedDateTime.now(clock)
            emit(now.toLocalDate())
            delay(Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay(now.zone)).toMillis())
        }
    }

    private fun key(uid: String) = stringPreferencesKey("usage_$uid")
}

/** The count in a stored "date|count" value, or 0 when it was saved on another day. */
internal fun countOn(stored: String?, day: LocalDate): Int {
    val (date, count) = stored?.split('|')?.takeIf { it.size == 2 } ?: return 0
    return if (date == day.toString()) count.toIntOrNull() ?: 0 else 0
}
