@file:OptIn(ExperimentalCoroutinesApi::class)

package com.nya.app.viewmodel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.nya.app.MainDispatcherRule
import com.nya.app.data.AuthRepository
import com.nya.app.data.AuthUser
import com.nya.app.data.FakeAuthRepository
import com.nya.app.data.FakeProfileRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AuthViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private class CountingAuth(private val fake: FakeAuthRepository = FakeAuthRepository()) : AuthRepository by fake {
        var calls = 0

        override suspend fun signIn(email: String, password: String): Result<AuthUser> {
            calls++
            return fake.signIn(email, password)
        }

        override suspend fun signUp(name: String, email: String, password: String): Result<AuthUser> {
            calls++
            return fake.signUp(name, email, password)
        }
    }

    private val auth = CountingAuth()
    private val profile = FakeProfileRepository()
    private val vm = AuthViewModel(auth, profile)
    private var done = 0

    private fun signUp(
        name: String = "Ada",
        email: String = "ada@nya.app",
        pass: String = "password1",
        confirm: String = pass,
        dob: String = "09/02/2004",
        terms: Boolean = true
    ) = vm.signUp(name, email, pass, confirm, dob, terms) { done++ }

    private fun assertRejected(message: String) {
        assertEquals(message, vm.error)
        assertEquals(0, auth.calls)
        assertEquals(0, done)
    }

    @Test
    fun blankNameIsRejected() {
        signUp(name = " ")
        assertRejected("Enter your name.")
    }

    @Test
    fun blankEmailIsRejected() {
        signUp(email = "")
        assertRejected("Enter your email.")
    }

    @Test
    fun shortPasswordIsRejected() {
        signUp(pass = "short12")
        assertRejected("Password must be at least 8 characters.")
    }

    @Test
    fun mismatchedPasswordsAreRejected() {
        signUp(confirm = "password2")
        assertRejected("Passwords don't match.")
    }

    @Test
    fun malformedBirthDatesAreRejected() {
        for (dob in listOf("31/02/2000", "1/1/2000", "09/02/200", "")) {
            signUp(dob = dob)
            assertRejected("Enter your date of birth as DD/MM/YYYY.")
            vm.clearError()
        }
    }

    @Test
    fun underageUserIsRejected() {
        signUp(dob = LocalDate.now().minusYears(17).format(DateTimeFormatter.ofPattern("dd/MM/uuuu")))
        assertRejected("You must be 18 or older to use Nya.")
    }

    @Test
    fun uncheckedTermsAreRejected() {
        signUp(terms = false)
        assertRejected("Please agree to the Terms and Privacy Policy.")
    }

    @Test
    fun validSignUpCallsOnDoneAndSavesName() = runTest {
        signUp(name = " Ada ")
        advanceUntilIdle()
        assertEquals(1, done)
        assertEquals(1, auth.calls)
        assertEquals("Ada", profile.profile.first().name)
        assertNull(vm.error)
    }

    @Test
    fun blankSignInFieldsAreRejected() {
        vm.signIn("ada@nya.app", "") { done++ }
        assertRejected("Enter your email and password.")
    }

    @Test
    fun clearErrorRemovesTheMessage() {
        signUp(name = "")
        vm.clearError()
        assertNull(vm.error)
    }
}
