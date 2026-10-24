package com.nya.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nya.app.data.local.ProfileDao
import com.nya.app.data.local.ProfileEntity
import com.nya.app.data.local.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

internal class FakeProfileDao : ProfileDao() {
    val rows = MutableStateFlow(emptyMap<String, ProfileEntity>())

    override fun observe(uid: String): Flow<ProfileEntity?> = rows.map { it[uid] }
    override suspend fun get(uid: String) = rows.value[uid]
    override suspend fun upsert(profile: ProfileEntity) { rows.value += profile.uid to profile }
}

private class FakeStore(initial: Preferences) : DataStore<Preferences> {
    private val state = MutableStateFlow(initial)
    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        state.value = transform(state.value)
        return state.value
    }
}

class ProfileRepositoryTest {
    private val dao = FakeProfileDao()
    private val uid = MutableStateFlow<String?>("a")
    private val cloud = FakeCloud()
    private val noLegacy = FakeStore(mutablePreferencesOf())

    private fun repo(store: DataStore<Preferences> = noLegacy) = RoomProfileRepository(dao, cloud, uid, store)

    @Test
    fun signedOutShowsDefaultsAndIgnoresUpdates() = runTest {
        uid.value = null
        val repo = repo()
        repo.update { it.copy(name = "Ada") }
        repo.suggestName("Ada")
        assertEquals(Profile(), repo.profile.first())
        assertTrue(dao.rows.value.isEmpty())
    }

    @Test
    fun updateSetsUpdatedAt() = runTest {
        val before = System.currentTimeMillis()
        val repo = repo()
        repo.update { it.copy(name = "Ada", notifications = false) }
        val row = dao.rows.value.getValue("a")
        assertEquals("Ada", row.name)
        assertTrue(row.updatedAt >= before)
        assertEquals(Profile(name = "Ada", notifications = false), repo.profile.first())
    }

    @Test
    fun updatePushesTheRowButSuggestNameDoesNot() = runTest {
        val repo = repo()
        repo.suggestName("Ada")
        assertTrue(cloud.profilePuts.isEmpty())
        repo.update { it.copy(voice = 4) }
        assertEquals(listOf(dao.rows.value.getValue("a").toDoc()), cloud.profilePuts)
        assertEquals("Ada", cloud.profilePuts.single().name)
    }

    @Test
    fun suggestNameFillsABlankNameAndKeepsUpdatedAt() = runTest {
        val repo = repo()
        repo.suggestName("Ada")
        assertEquals("Ada", repo.profile.first().name)
        assertEquals(0L, dao.rows.value.getValue("a").updatedAt)
    }

    @Test
    fun suggestNameNeverOverwritesAName() = runTest {
        dao.upsert(Profile(name = "Zed").toEntity("a", 42))
        repo().suggestName("Ada")
        val row = dao.rows.value.getValue("a")
        assertEquals("Zed", row.name)
        assertEquals(42L, row.updatedAt)
    }

    @Test
    fun longNamesAreCappedByUpdateAndSuggestName() = runTest {
        val long = " " + "x".repeat(150)
        repo().suggestName(long)
        assertEquals("x".repeat(MaxNameLength), dao.rows.value.getValue("a").name)
        repo().update { it.copy(name = long) }
        assertEquals("x".repeat(MaxNameLength), dao.rows.value.getValue("a").name)
    }

    @Test
    fun upsertIfNewerKeepsANewerLocalRow() = runTest {
        dao.upsertIfNewer(Profile(name = "New").toEntity("a", 10))
        dao.upsertIfNewer(Profile(name = "Old").toEntity("a", 5))
        assertEquals("New", dao.rows.value.getValue("a").name)
        dao.upsertIfNewer(Profile(name = "Newer").toEntity("a", 11))
        assertEquals("Newer", dao.rows.value.getValue("a").name)
    }

    @Test
    fun profileIsPerAccount() = runTest {
        val repo = repo()
        repo.update { it.copy(name = "Ada") }
        uid.value = "b"
        assertEquals(Profile(), repo.profile.first())
        repo.update { it.copy(name = "Bo") }
        uid.value = "a"
        assertEquals("Ada", repo.profile.first().name)
    }

    @Test
    fun legacyProfileIsImportedOnceAndCleared() = runTest {
        val store = FakeStore(mutablePreferencesOf(
            stringPreferencesKey("name") to "Old", intPreferencesKey("companion") to Companion.Guide.ordinal,
            intPreferencesKey("gender") to Gender.Male.ordinal, intPreferencesKey("theme") to 2
        ))
        val repo = repo(store)
        assertEquals(Profile(name = "Old", companion = Companion.Guide, gender = Gender.Male), repo.profile.first())
        assertEquals(0L, dao.rows.value.getValue("a").updatedAt)
        assertEquals(setOf(intPreferencesKey("theme")), store.data.first().asMap().keys)
        uid.value = "b"
        assertEquals(Profile(), repo.profile.first())
        assertNull(dao.rows.value["b"])
    }

    @Test
    fun legacyKeysAreClearedEvenWhenTheAccountAlreadyHasAProfile() = runTest {
        dao.upsert(Profile(name = "Cloud").toEntity("a", 5))
        val store = FakeStore(mutablePreferencesOf(
            stringPreferencesKey("name") to "Old", intPreferencesKey("theme") to 2
        ))
        assertEquals("Cloud", repo(store).profile.first().name)
        assertEquals(setOf(intPreferencesKey("theme")), store.data.first().asMap().keys)
        assertEquals(5L, dao.rows.value.getValue("a").updatedAt)
        assertTrue(cloud.profilePuts.isEmpty())
    }

    @Test
    fun noLegacyKeysMeansNothingToImport() {
        assertNull(mutablePreferencesOf(intPreferencesKey("theme") to 1).toLegacyEntity("a"))
    }

    @Test
    fun unknownLegacyEnumsFallBackToDefaults() {
        val entity = mutablePreferencesOf(intPreferencesKey("companion") to 99).toLegacyEntity("a")
        assertEquals(Profile().toEntity("a", 0), entity)
    }
}
