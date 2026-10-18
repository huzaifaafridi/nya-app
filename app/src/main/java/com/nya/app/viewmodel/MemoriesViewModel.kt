package com.nya.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nya.app.NyaApp
import com.nya.app.data.MemoryRepository
import kotlinx.coroutines.launch

class MemoriesViewModel(private val repo: MemoryRepository) : ViewModel() {
    private val load = Retryable(viewModelScope, repo.memories)
    val memories = load.state

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
