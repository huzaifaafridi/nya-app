package com.nya.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nya.app.data.local.ProfileDao
import com.nya.app.data.local.ProfileEntity
import com.nya.app.data.local.toEntity
import com.nya.app.data.local.toProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Must match the name size limit in firestore.rules. */
const val MaxNameLength = 100

data class Profile(
    val name: String = "",
    val companion: Companion = Companion.Muse,
    val skin: Int = 0xFFE0AC84.toInt(), // ARGB
    val gender: Gender = Gender.Female,
    val voice: Int = 0,
    val notifications: Boolean = true
)

interface ProfileRepository {
    val profile: Flow<Profile>
    suspend fun update(transform: (Profile) -> Profile)

    /** Sets the name only when the account has none yet. */
    suspend fun suggestName(name: String)
}

private val LegacyName = stringPreferencesKey("name")
private val LegacyCompanion = intPreferencesKey("companion")
private val LegacySkin = intPreferencesKey("skin")
private val LegacyGender = intPreferencesKey("gender")
private val LegacyVoice = intPreferencesKey("voice")
private val LegacyKeys = listOf(LegacyName, LegacyCompanion, LegacySkin, LegacyGender, LegacyVoice)

/** The profile the old per-phone store held, or null when it held none. */
internal fun Preferences.toLegacyEntity(uid: String): ProfileEntity? {
    if (LegacyKeys.none { it in this }) return null
    val d = Profile()
    return Profile(
        name = this[LegacyName] ?: d.name,
        companion = this[LegacyCompanion]?.let { Companion.entries.getOrNull(it) } ?: d.companion,
        skin = this[LegacySkin] ?: d.skin,
        gender = this[LegacyGender]?.let { Gender.entries.getOrNull(it) } ?: d.gender,
        voice = this[LegacyVoice] ?: d.voice
    ).toEntity(uid, 0)
}

private fun ProfileEntity.capped() = copy(name = name.trim().take(MaxNameLength))

class RoomProfileRepository(
    private val dao: ProfileDao,
    private val cloud: CloudSync,
    private val uid: Flow<String?>,
    private val legacy: DataStore<Preferences>
) : ProfileRepository {
    private val lock = Mutex()

    @OptIn(ExperimentalCoroutinesApi::class)
    override val profile: Flow<Profile> = uid.flatMapLatest { id ->
        if (id == null) flowOf(Profile())
        else dao.observe(id).onStart { lock.withLock { importLegacy(id) } }.map { it?.toProfile() ?: Profile() }
    }

    override suspend fun update(transform: (Profile) -> Profile) =
        change(push = true) { transform(it.toProfile()).toEntity(it.uid, System.currentTimeMillis()) }

    override suspend fun suggestName(name: String) =
        change(push = false) { if (it.name.isBlank()) it.copy(name = name) else null }

    /** A suggested name is only a default, so it stays local until the user edits; [SyncManager] reconciles it. */
    private suspend fun change(push: Boolean, transform: (ProfileEntity) -> ProfileEntity?) {
        val id = uid.first() ?: return
        lock.withLock {
            importLegacy(id)
            transform(dao.get(id) ?: Profile().toEntity(id, 0))?.capped()?.let {
                dao.upsert(it)
                if (push) cloud.putProfile(id, it.toDoc())
            }
        }
    }

    /** Copies the old per-phone profile into the first account that signs in, then forgets it even if that account has one. */
    private suspend fun importLegacy(id: String) {
        val entity = legacy.safeData().first().toLegacyEntity(id) ?: return
        if (dao.get(id) == null) dao.upsert(entity.capped())
        legacy.edit { p -> LegacyKeys.forEach { p.remove(it) } }
    }
}

class FakeProfileRepository(initial: Profile = Profile()) : ProfileRepository {
    private val state = MutableStateFlow(initial)
    override val profile: Flow<Profile> = state

    override suspend fun update(transform: (Profile) -> Profile) = state.update(transform)

    override suspend fun suggestName(name: String) = state.update { if (it.name.isBlank()) it.copy(name = name) else it }
}
