package com.nya.app.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nya.app.NyaApp
import com.nya.app.data.AuthRepository
import com.nya.app.data.AuthUser
import com.nya.app.data.ProfileRepository
import com.nya.app.data.authMessage
import com.nya.app.data.googleIdToken
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

private val DobFormat = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)

class AuthViewModel(private val auth: AuthRepository, private val profile: ProfileRepository) : ViewModel() {
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var info by mutableStateOf<String?>(null)
        private set

    fun clearError() { error = null }

    fun clearMessages() {
        error = null
        info = null
    }

    private fun launchBusy(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        clearMessages()
        viewModelScope.launch {
            block()
            busy = false
        }
    }

    private suspend fun finish(result: Result<AuthUser>, onDone: (AuthUser) -> Unit) {
        result.onSuccess { user ->
            profile.update { it.copy(name = user.name) }
            onDone(user)
        }.onFailure { error = it.authMessage() }
    }

    private fun ageOf(dob: String) = runCatching {
        Period.between(LocalDate.parse(dob.trim(), DobFormat), LocalDate.now()).years
    }.getOrNull()

    fun signIn(email: String, pass: String, onDone: (AuthUser) -> Unit) {
        if (email.isBlank() || pass.isBlank()) {
            error = "Enter your email and password."
            return
        }
        launchBusy { finish(auth.signIn(email, pass), onDone) }
    }

    fun signUp(name: String, email: String, pass: String, confirm: String, dob: String, terms: Boolean, onDone: (AuthUser) -> Unit) {
        val age = ageOf(dob)
        val problem = when {
            name.isBlank() -> "Enter your name."
            email.isBlank() -> "Enter your email."
            pass.length < 8 -> "Password must be at least 8 characters."
            pass != confirm -> "Passwords don't match."
            age == null -> "Enter your date of birth as DD/MM/YYYY."
            age < 18 -> "You must be 18 or older to use Nya."
            !terms -> "Please agree to the Terms and Privacy Policy."
            else -> null
        }
        if (problem != null) {
            error = problem
            return
        }
        launchBusy { finish(auth.signUp(name, email, pass), onDone) }
    }

    fun google(context: Context, onDone: (AuthUser) -> Unit) {
        launchBusy {
            googleIdToken(context)
                .onSuccess { token -> if (token != null) finish(auth.signInWithGoogle(token), onDone) }
                .onFailure { error = it.authMessage() }
        }
    }

    fun checkVerified(onDone: () -> Unit) {
        launchBusy {
            auth.reloadVerified()
                .onSuccess { if (it) onDone() else error = "Not verified yet. Open the link in your email first." }
                .onFailure { error = it.authMessage() }
        }
    }

    fun resend() {
        launchBusy {
            auth.resendVerification()
                .onSuccess { info = "Verification email sent." }
                .onFailure { error = it.authMessage() }
        }
    }

    fun sendReset(email: String) {
        if (email.isBlank()) {
            error = "Enter your email."
            return
        }
        launchBusy {
            auth.sendReset(email)
                .onSuccess { info = "Reset link sent to ${email.trim()}. Check your inbox." }
                .onFailure { error = it.authMessage() }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as NyaApp).container
                AuthViewModel(c.auth, c.profile)
            }
        }
    }
}
