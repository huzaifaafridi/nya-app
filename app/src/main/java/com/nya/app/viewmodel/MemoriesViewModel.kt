package com.nya.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nya.app.NyaApp
import com.nya.app.data.MemoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

class MemoriesViewModel(
    private val repo: MemoryRepository,
    private val now: () -> Long = System::currentTimeMillis,
    private val zone: () -> ZoneId = ZoneId::systemDefault
) : ViewModel() {
    private val load = Retryable(viewModelScope, repo.memories)
    val memories = load.state

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _filter = MutableStateFlow(MemoryFilter.All)
    val filter: StateFlow<MemoryFilter> = _filter

    /** [memories] narrowed by [query] and [filter], grouped by day. */
    val visible: StateFlow<Loadable<List<DaySection>>> =
        combine(memories, _query, _filter) { state, q, f ->
            state.map { all ->
                val z = zone()
                val t = now()
                groupByDay(filterMemories(all, q, f, t, z), Instant.ofEpochMilli(t).atZone(z).toLocalDate(), z)
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, Loadable.Loading)

    fun setQuery(value: String) {
        _query.value = value
    }

    fun setFilter(value: MemoryFilter) {
        _filter.value = value
    }

    fun retry() = load.retry()

    fun delete(id: Long) {
        viewModelScope.launch { repo.delete(id) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { MemoriesViewModel((this[APPLICATION_KEY] as NyaApp).container.memories) }
        }
    }
}
