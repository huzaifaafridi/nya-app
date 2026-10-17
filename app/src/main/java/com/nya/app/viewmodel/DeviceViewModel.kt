package com.nya.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nya.app.NyaApp
import com.nya.app.data.DeviceInfo
import com.nya.app.data.DeviceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class DeviceViewModel(private val repo: DeviceRepository) : ViewModel() {
    val device: StateFlow<DeviceInfo?> = repo.device.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun transfers() = repo.transfers()

    companion object {
        val Factory = viewModelFactory {
            initializer { DeviceViewModel((this[APPLICATION_KEY] as NyaApp).container.device) }
        }
    }
}
