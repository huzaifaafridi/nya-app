package com.nya.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.io.IOException

interface PrefsRepository {
    val prefs: Flow<Prefs>
    suspend fun update(transform: (Prefs) -> Prefs)
}

class DataStorePrefsRepository(private val store: DataStore<Preferences>) : PrefsRepository {
    private val name = stringPreferencesKey("name")
    private val companion = intPreferencesKey("companion")
    private val skin = intPreferencesKey("skin")
    private val gender = intPreferencesKey("gender")
    private val voice = intPreferencesKey("voice")
    private val theme = intPreferencesKey("theme")
    private val quiet = booleanPreferencesKey("quiet")
    private val mode = intPreferencesKey("mode")
    private val onboarded = booleanPreferencesKey("onboarded")

    override val prefs: Flow<Prefs> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it.toPrefs() }

    override suspend fun update(transform: (Prefs) -> Prefs) {
        store.edit { p ->
            val n = transform(p.toPrefs())
            p[name] = n.name
            p[companion] = n.companion.ordinal
            p[skin] = n.skin
            p[gender] = n.gender.ordinal
            p[voice] = n.voice
            p[theme] = n.theme
            p[quiet] = n.quiet
            p[mode] = n.mode
            p[onboarded] = n.onboarded
        }
    }

    private fun Preferences.toPrefs(): Prefs {
        val d = Prefs()
        return Prefs(
            name = this[name] ?: d.name,
            companion = this[companion]?.let { Companion.entries.getOrNull(it) } ?: d.companion,
            skin = this[skin] ?: d.skin,
            gender = this[gender]?.let { Gender.entries.getOrNull(it) } ?: d.gender,
            voice = this[voice] ?: d.voice,
            theme = this[theme] ?: d.theme,
            quiet = this[quiet] ?: d.quiet,
            mode = this[mode] ?: d.mode,
            onboarded = this[onboarded] ?: d.onboarded
        )
    }
}

class FakePrefsRepository(initial: Prefs = Prefs()) : PrefsRepository {
    private val state = MutableStateFlow(initial)
    override val prefs: Flow<Prefs> = state

    override suspend fun update(transform: (Prefs) -> Prefs) = state.update(transform)
}
