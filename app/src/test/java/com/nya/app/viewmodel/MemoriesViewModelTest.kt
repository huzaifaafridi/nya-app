@file:OptIn(ExperimentalCoroutinesApi::class)

package com.nya.app.viewmodel

import com.nya.app.MainDispatcherRule
import com.nya.app.data.FakeMemoryRepository
import com.nya.app.data.Memory
import com.nya.app.data.MemoryRepository
import com.nya.app.data.Scene
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.ZoneOffset

class MemoriesViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private class FlakyRepository(var failing: Boolean) : MemoryRepository {
        override val memories: Flow<List<Memory>> = flow {
            if (failing) error("offline")
            emit(emptyList())
        }

        override suspend fun delete(id: Long) = Unit
    }

    @Test
    fun loadsFromLoadingToReady() = runTest {
        val vm = MemoriesViewModel(FakeMemoryRepository())
        assertEquals(Loadable.Loading, vm.memories.value)
        runCurrent()
        assertEquals(2, (vm.memories.value as Loadable.Ready).value.size)
    }

    @Test
    fun emptyRepositoryIsReadyWithEmptyList() = runTest {
        val vm = MemoriesViewModel(object : MemoryRepository {
            override val memories = flowOf(emptyList<Memory>())
            override suspend fun delete(id: Long) = Unit
        })
        runCurrent()
        assertEquals(Loadable.Ready(emptyList<Memory>()), vm.memories.value)
    }

    @Test
    fun failingSourceShowsFailedThenRetryRecovers() = runTest {
        val repo = FlakyRepository(failing = true)
        val vm = MemoriesViewModel(repo)
        runCurrent()
        assertEquals(Loadable.Failed("Couldn't load your data."), vm.memories.value)
        repo.failing = false
        vm.retry()
        runCurrent()
        assertTrue(vm.memories.value is Loadable.Ready)
    }

    @Test
    fun deleteRemovesTheMemory() = runTest {
        val vm = MemoriesViewModel(FakeMemoryRepository())
        runCurrent()
        vm.delete(1)
        runCurrent()
        assertEquals(listOf(2L), (vm.memories.value as Loadable.Ready).value.map { it.id })
    }

    @Test
    fun visibleCombinesQueryAndFilter() = runTest {
        val now = 1_700_000_000_000L // fixed, so the days never depend on the clock
        val repo = object : MemoryRepository {
            override val memories = flowOf(listOf(
                Memory(1, "Sunset", "Karachi", "Golden hour", Scene.Sea, now, 8),
                Memory(2, "Beach day", "Sea View", "Windy", Scene.Mount, now - 24 * 60 * 60 * 1000L)
            ))
            override suspend fun delete(id: Long) = Unit
        }
        val vm = MemoriesViewModel(repo, { now }, { ZoneOffset.UTC })
        runCurrent()
        fun labels() = (vm.visible.value as Loadable.Ready).value.map { it.label }
        assertEquals(listOf("TODAY", "YESTERDAY"), labels())
        vm.setQuery("beach")
        runCurrent()
        assertEquals(listOf("YESTERDAY"), labels())
        vm.setFilter(MemoryFilter.Clips)
        runCurrent()
        assertEquals(emptyList<String>(), labels())
        assertEquals("beach", vm.query.value)
        assertEquals(MemoryFilter.Clips, vm.filter.value)
    }
}
