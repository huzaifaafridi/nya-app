package com.nya.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.io.IOException

data class AppSettings(
    val theme: Int = 0, // 0 Auto, 1 Light, 2 Dark
    val quiet: Boolean = false,
    val mode: Int = 2, // 0 Travel, 1 Focus, 2 Wellness, 3 Creator
    val onboarded: Boolean = false
)

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun update(transform: (AppSettings) -> AppSettings)
}

internal fun DataStore<Preferences>.safeData(): Flow<Preferences> =
    data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }

class DataStoreSettingsRepository(private val store: DataStore<Preferences>) : SettingsRepository {
    private val theme = intPreferencesKey("theme")
    private val quiet = booleanPreferencesKey("quiet")
    private val mode = intPreferencesKey("mode")
    private val onboarded = booleanPreferencesKey("onboarded")

    override val settings: Flow<AppSettings> = store.safeData().map { it.toSettings() }

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        store.edit { p ->
            val n = transform(p.toSettings())
            p[theme] = n.theme
            p[quiet] = n.quiet
            p[mode] = n.mode
            p[onboarded] = n.onboarded
        }
    }

    private fun Preferences.toSettings(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            theme = this[theme] ?: d.theme,
            quiet = this[quiet] ?: d.quiet,
            mode = this[mode] ?: d.mode,
            onboarded = this[onboarded] ?: d.onboarded
        )
    }
}

class FakeSettingsRepository(initial: AppSettings = AppSettings()) : SettingsRepository {
    private val state = MutableStateFlow(initial)
    override val settings: Flow<AppSettings> = state

    override suspend fun update(transform: (AppSettings) -> AppSettings) = state.update(transform)
}
