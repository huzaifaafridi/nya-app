package com.nya.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nya.app.NyaApp
import com.nya.app.data.Companion as Character
import com.nya.app.data.MaxNameLength
import com.nya.app.data.Profile
import com.nya.app.data.ProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class EditProfileViewModel(private val profile: ProfileRepository) : ViewModel() {
    /** Null until the current profile has loaded; later profile emissions never replace it. */
    var draft by mutableStateOf<Profile?>(null)
        private set
    var saving by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var info by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch { draft = profile.profile.first() }
    }

    fun setName(name: String) = edit { copy(name = name.take(MaxNameLength)) }

    fun setCompanion(companion: Character) = edit { copy(companion = companion) }

    fun setVoice(voice: Int) = edit { copy(voice = voice) }

    private fun edit(change: Profile.() -> Profile) {
        draft = draft?.change()
        error = null
        info = null
    }

    fun save() {
        val d = draft ?: return
        if (saving) return
        val name = d.name.trim()
        if (name.isEmpty()) {
            error = "Enter your name."
            return
        }
        if (name.length > MaxNameLength) {
            error = "Your name is too long."
            return
        }
        saving = true
        error = null
        info = null
        viewModelScope.launch {
            try {
                profile.update { it.copy(name = name, companion = d.companion, voice = d.voice) }
                draft = draft?.copy(name = name)
                info = "Profile saved"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = "Couldn't save your profile. Try again."
            } finally {
                saving = false
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { EditProfileViewModel((this[APPLICATION_KEY] as NyaApp).container.profile) }
        }
    }
}
