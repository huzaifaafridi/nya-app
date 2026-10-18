package com.nya.app.data

import androidx.credentials.exceptions.NoCredentialException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException

private const val GENERIC = "Something went wrong. Please try again."

fun Throwable.authMessage(): String = when (this) {
    is FirebaseNetworkException -> "Network error. Check your connection."
    is NoCredentialException -> "No Google account on this device. Add one in Settings or sign up with email."
    is FirebaseAuthException -> when (errorCode) {
        "ERROR_INVALID_EMAIL", "ERROR_MISSING_EMAIL" -> "That email address looks invalid."
        "ERROR_INVALID_CREDENTIAL", "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND" -> "Wrong email or password."
        "ERROR_EMAIL_ALREADY_IN_USE" -> "An account with this email already exists."
        "ERROR_WEAK_PASSWORD" -> "Password is too weak. Use at least 8 characters."
        "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Try again later."
        else -> GENERIC
    }
    else -> GENERIC
}
