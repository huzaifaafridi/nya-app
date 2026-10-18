package com.nya.app.data

import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) : AuthRepository {
    override val isSignedIn get() = auth.currentUser != null

    override suspend fun signIn(email: String, password: String) = runCatching {
        auth.signInWithEmailAndPassword(email.trim(), password).await().toAuthUser()
    }

    override suspend fun signUp(name: String, email: String, password: String) = runCatching {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val user = result.user!!
        user.updateProfile(userProfileChangeRequest { displayName = name.trim() }).await()
        user.sendEmailVerification().await()
        result.toAuthUser()
    }

    override suspend fun signInWithGoogle(idToken: String) = runCatching {
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await().toAuthUser()
    }

    override suspend fun reloadVerified() = runCatching {
        val user = auth.currentUser!!
        user.reload().await()
        user.isEmailVerified
    }

    override suspend fun resendVerification() = runCatching {
        auth.currentUser!!.sendEmailVerification().await()
        Unit
    }

    override suspend fun sendReset(email: String) = runCatching {
        auth.sendPasswordResetEmail(email.trim()).await()
        Unit
    }

    override suspend fun signOut() = auth.signOut()

    private fun AuthResult.toAuthUser() = user!!.toAuthUser(additionalUserInfo?.isNewUser == true)

    private fun FirebaseUser.toAuthUser(isNew: Boolean): AuthUser {
        val mail = email.orEmpty()
        return AuthUser(displayName?.takeIf { it.isNotBlank() } ?: nameFromEmail(mail), mail, isEmailVerified, isNew)
    }
}
