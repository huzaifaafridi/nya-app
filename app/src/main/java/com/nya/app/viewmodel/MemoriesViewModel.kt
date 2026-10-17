package com.nya.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nya.app.NyaApp
import com.nya.app.data.Memory
import com.nya.app.data.MemoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MemoriesViewModel(memories: MemoryRepository) : ViewModel() {
    val memories: StateFlow<List<Memory>> =
        memories.memories.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    companion object {
        val Factory = viewModelFactory {
            initializer { MemoriesViewModel((this[APPLICATION_KEY] as NyaApp).container.memories) }
        }
    }
}
