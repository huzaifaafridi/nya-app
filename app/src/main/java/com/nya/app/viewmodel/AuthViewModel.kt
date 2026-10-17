package com.nya.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nya.app.NyaApp
import com.nya.app.data.AuthRepository
import com.nya.app.data.PrefsRepository
import kotlinx.coroutines.launch

class AuthViewModel(private val auth: AuthRepository, private val prefs: PrefsRepository) : ViewModel() {
    fun signIn(email: String, pass: String, onDone: () -> Unit) {
        viewModelScope.launch {
            auth.signIn(email, pass).onSuccess { name ->
                prefs.update { it.copy(name = name) }
                onDone()
            }
        }
    }

    fun signUp(name: String, email: String, pass: String, onDone: () -> Unit) {
        viewModelScope.launch {
            auth.signUp(name, email, pass).onSuccess { displayName ->
                prefs.update { it.copy(name = displayName) }
                onDone()
            }
        }
    }

    fun verify(code: String, onDone: () -> Unit) {
        viewModelScope.launch { auth.verify(code).onSuccess { onDone() } }
    }

    fun sendReset(email: String, onDone: () -> Unit) {
        viewModelScope.launch { auth.sendReset(email).onSuccess { onDone() } }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as NyaApp).container
                AuthViewModel(c.auth, c.prefs)
            }
        }
    }
}
