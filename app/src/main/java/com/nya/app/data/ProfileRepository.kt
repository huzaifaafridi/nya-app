package com.nya.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

data class Profile(
    val name: String = "",
    val companion: Companion = Companion.Muse,
    val skin: Int = 0xFFE0AC84.toInt(), // ARGB
    val gender: Gender = Gender.Female,
    val voice: Int = 0
)

interface ProfileRepository {
    val profile: Flow<Profile>
    suspend fun update(transform: (Profile) -> Profile)
}

class DataStoreProfileRepository(private val store: DataStore<Preferences>) : ProfileRepository {
    private val name = stringPreferencesKey("name")
    private val companion = intPreferencesKey("companion")
    private val skin = intPreferencesKey("skin")
    private val gender = intPreferencesKey("gender")
    private val voice = intPreferencesKey("voice")

    override val profile: Flow<Profile> = store.safeData().map { it.toProfile() }

    override suspend fun update(transform: (Profile) -> Profile) {
        store.edit { p ->
            val n = transform(p.toProfile())
            p[name] = n.name
            p[companion] = n.companion.ordinal
            p[skin] = n.skin
            p[gender] = n.gender.ordinal
            p[voice] = n.voice
        }
    }

    private fun Preferences.toProfile(): Profile {
        val d = Profile()
        return Profile(
            name = this[name] ?: d.name,
            companion = this[companion]?.let { Companion.entries.getOrNull(it) } ?: d.companion,
            skin = this[skin] ?: d.skin,
            gender = this[gender]?.let { Gender.entries.getOrNull(it) } ?: d.gender,
            voice = this[voice] ?: d.voice
        )
    }
}

class FakeProfileRepository(initial: Profile = Profile()) : ProfileRepository {
    private val state = MutableStateFlow(initial)
    override val profile: Flow<Profile> = state

    override suspend fun update(transform: (Profile) -> Profile) = state.update(transform)
}
