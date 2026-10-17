package com.nya.app.data

interface AuthRepository {
    /** Returns the display name. */
    suspend fun signIn(email: String, password: String): Result<String>
    suspend fun signUp(name: String, email: String, password: String): Result<String>
    suspend fun verify(code: String): Result<Unit>
    suspend fun sendReset(email: String): Result<Unit>
    suspend fun signOut()
}

class FakeAuthRepository : AuthRepository {
    override suspend fun signIn(email: String, password: String): Result<String> =
        Result.success(nameFromEmail(email))

    override suspend fun signUp(name: String, email: String, password: String): Result<String> =
        Result.success(name.trim().ifBlank { nameFromEmail(email) })

    override suspend fun verify(code: String) = Result.success(Unit)

    override suspend fun sendReset(email: String) = Result.success(Unit)

    override suspend fun signOut() {}

    private fun nameFromEmail(email: String) =
        email.substringBefore('@').trim().replaceFirstChar { it.uppercase() }.ifBlank { "Mercy" }
}
