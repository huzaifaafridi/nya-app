package com.nya.app.viewmodel

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nya.app.NyaApp
import com.nya.app.data.AppConfig
import com.nya.app.data.AppConfigRepository
import com.nya.app.data.AppSettings
import com.nya.app.data.AuthRepository
import com.nya.app.data.Companion as CompanionType
import com.nya.app.data.Gender
import com.nya.app.data.Prefs
import com.nya.app.data.Profile
import com.nya.app.data.ProfileRepository
import com.nya.app.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(
    private val settings: SettingsRepository,
    private val profile: ProfileRepository,
    private val auth: AuthRepository,
    appConfig: AppConfigRepository
) : ViewModel() {
    val config: StateFlow<AppConfig> = appConfig.config

    val prefs: StateFlow<Prefs?> = combine(settings.settings, profile.profile) { s, p ->
        Prefs(p.name, p.companion, p.skin, p.gender, p.voice, p.notifications, s.theme, s.quiet, s.mode, s.onboarded)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settings.update(transform) }
    }

    private fun updateProfile(transform: (Profile) -> Profile) {
        viewModelScope.launch { profile.update(transform) }
    }

    fun setCompanion(companion: CompanionType) = updateProfile { it.copy(companion = companion) }
    fun setSkin(argb: Int) = updateProfile { it.copy(skin = argb) }
    fun setGender(gender: Gender) = updateProfile { it.copy(gender = gender) }
    fun setVoice(voice: Int) = updateProfile { it.copy(voice = voice) }
    fun setNotifications(on: Boolean) = updateProfile { it.copy(notifications = on) }
    fun setTheme(theme: Int) = updateSettings { it.copy(theme = theme) }
    fun setQuiet(quiet: Boolean) = updateSettings { it.copy(quiet = quiet) }
    fun setMode(mode: Int) = updateSettings { it.copy(mode = mode) }
    fun completeOnboarding() = updateSettings { it.copy(onboarded = true) }

    val isSignedIn get() = auth.isSignedIn

    fun signOut(context: Context) {
        val appContext = context.applicationContext
        viewModelScope.launch {
            auth.signOut()
            runCatching { CredentialManager.create(appContext).clearCredentialState(ClearCredentialStateRequest()) }
            settings.update { it.copy(onboarded = false) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as NyaApp).container
                AppViewModel(c.settings, c.profile, c.auth, c.appConfig)
            }
        }
    }
}
