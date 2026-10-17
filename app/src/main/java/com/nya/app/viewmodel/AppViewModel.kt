package com.nya.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nya.app.NyaApp
import com.nya.app.data.AuthRepository
import com.nya.app.data.Companion as CompanionType
import com.nya.app.data.Gender
import com.nya.app.data.Prefs
import com.nya.app.data.PrefsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(private val repo: PrefsRepository, private val auth: AuthRepository) : ViewModel() {
    val prefs: StateFlow<Prefs?> = repo.prefs.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private fun update(transform: (Prefs) -> Prefs) {
        viewModelScope.launch { repo.update(transform) }
    }

    fun setName(name: String) = update { it.copy(name = name) }
    fun setCompanion(companion: CompanionType) = update { it.copy(companion = companion) }
    fun setSkin(argb: Int) = update { it.copy(skin = argb) }
    fun setGender(gender: Gender) = update { it.copy(gender = gender) }
    fun setVoice(voice: Int) = update { it.copy(voice = voice) }
    fun setTheme(theme: Int) = update { it.copy(theme = theme) }
    fun setQuiet(quiet: Boolean) = update { it.copy(quiet = quiet) }
    fun setMode(mode: Int) = update { it.copy(mode = mode) }
    fun completeOnboarding() = update { it.copy(onboarded = true) }

    fun signOut() {
        viewModelScope.launch {
            auth.signOut()
            repo.update { it.copy(onboarded = false) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as NyaApp).container
                AppViewModel(c.prefs, c.auth)
            }
        }
    }
}
