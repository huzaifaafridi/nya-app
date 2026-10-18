package com.nya.app.data

data class AuthUser(val name: String, val email: String, val verified: Boolean, val isNew: Boolean)

interface AuthRepository {
    val isSignedIn: Boolean
    suspend fun signIn(email: String, password: String): Result<AuthUser>
    suspend fun signUp(name: String, email: String, password: String): Result<AuthUser>
    suspend fun signInWithGoogle(idToken: String): Result<AuthUser>
    suspend fun reloadVerified(): Result<Boolean>
    suspend fun resendVerification(): Result<Unit>
    suspend fun sendReset(email: String): Result<Unit>
    suspend fun signOut()
}

fun nameFromEmail(email: String) =
    email.substringBefore('@').trim().replaceFirstChar { it.uppercase() }.ifBlank { "Mercy" }

class FakeAuthRepository : AuthRepository {
    override val isSignedIn = true

    override suspend fun signIn(email: String, password: String) =
        Result.success(AuthUser(nameFromEmail(email), email, verified = true, isNew = false))

    override suspend fun signUp(name: String, email: String, password: String) =
        Result.success(AuthUser(name.trim().ifBlank { nameFromEmail(email) }, email, verified = true, isNew = true))

    override suspend fun signInWithGoogle(idToken: String) =
        Result.success(AuthUser(nameFromEmail(""), "", verified = true, isNew = false))

    override suspend fun reloadVerified() = Result.success(true)

    override suspend fun resendVerification() = Result.success(Unit)

    override suspend fun sendReset(email: String) = Result.success(Unit)

    override suspend fun signOut() {}
}
