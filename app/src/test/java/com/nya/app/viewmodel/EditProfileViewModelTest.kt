@file:OptIn(ExperimentalCoroutinesApi::class)

package com.nya.app.viewmodel

import com.nya.app.MainDispatcherRule
import com.nya.app.data.Companion
import com.nya.app.data.FakeProfileRepository
import com.nya.app.data.Gender
import com.nya.app.data.MaxNameLength
import com.nya.app.data.Profile
import com.nya.app.data.ProfileRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EditProfileViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val initial = Profile(
        name = "Ada", companion = Companion.Guide, skin = 0x123456, gender = Gender.Male, voice = 1, notifications = false
    )

    private class CountingRepository(
        private val inner: FakeProfileRepository, private val gate: CompletableDeferred<Unit>? = null,
        private val failure: Exception? = null
    ) : ProfileRepository by inner {
        var updates = 0

        override suspend fun update(transform: (Profile) -> Profile) {
            updates++
            gate?.await()
            failure?.let { throw it }
            inner.update(transform)
        }
    }

    @Test
    fun loadsCurrentProfile() = runTest {
        val vm = EditProfileViewModel(FakeProfileRepository(initial))
        assertNull(vm.draft)
        runCurrent()
        assertEquals(initial, vm.draft)
    }

    @Test
    fun blankNameShowsErrorAndDoesNotUpdate() = runTest {
        val repo = CountingRepository(FakeProfileRepository(initial))
        val vm = EditProfileViewModel(repo)
        runCurrent()
        vm.setName("   ")
        vm.save()
        runCurrent()
        assertEquals("Enter your name.", vm.error)
        assertNull(vm.info)
        assertEquals(0, repo.updates)
    }

    @Test
    fun nameIsCappedWhileTyping() = runTest {
        val vm = EditProfileViewModel(FakeProfileRepository(initial))
        runCurrent()
        vm.setName("a".repeat(MaxNameLength + 10))
        assertEquals(MaxNameLength, vm.draft!!.name.length)
    }

    @Test
    fun saveUpdatesOnlyNameCompanionAndVoice() = runTest {
        val fake = FakeProfileRepository(initial)
        val vm = EditProfileViewModel(fake)
        runCurrent()
        vm.setName("Grace")
        vm.setCompanion(Companion.Yogi)
        vm.setVoice(2)
        vm.save()
        runCurrent()
        assertEquals(initial.copy(name = "Grace", companion = Companion.Yogi, voice = 2), fake.profile.first())
        assertEquals("Profile saved", vm.info)
        assertNull(vm.error)
        assertTrue(!vm.saving)
    }

    @Test
    fun nameIsTrimmed() = runTest {
        val fake = FakeProfileRepository(initial)
        val vm = EditProfileViewModel(fake)
        runCurrent()
        vm.setName("  Grace  ")
        vm.save()
        runCurrent()
        assertEquals("Grace", fake.profile.first().name)
        assertEquals("Grace", vm.draft!!.name)
    }

    @Test
    fun repositoryFailureShowsError() = runTest {
        val vm = EditProfileViewModel(CountingRepository(FakeProfileRepository(initial), failure = IllegalStateException("offline")))
        runCurrent()
        vm.save()
        runCurrent()
        assertEquals("Couldn't save your profile. Try again.", vm.error)
        assertNull(vm.info)
        assertTrue(!vm.saving)
    }

    @Test
    fun doubleSaveWhileSavingUpdatesOnce() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repo = CountingRepository(FakeProfileRepository(initial), gate)
        val vm = EditProfileViewModel(repo)
        runCurrent()
        vm.save()
        vm.save()
        runCurrent()
        assertTrue(vm.saving)
        gate.complete(Unit)
        runCurrent()
        assertEquals(1, repo.updates)
        assertTrue(!vm.saving)
    }

    @Test
    fun laterProfileEmissionsDoNotOverwriteTheDraft() = runTest {
        val fake = FakeProfileRepository(initial)
        val vm = EditProfileViewModel(fake)
        runCurrent()
        vm.setName("Grace")
        fake.update { it.copy(name = "Other", companion = Companion.Creator) }
        runCurrent()
        assertEquals(initial.copy(name = "Grace"), vm.draft)
    }
}
