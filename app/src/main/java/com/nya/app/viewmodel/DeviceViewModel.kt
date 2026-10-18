package com.nya.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nya.app.NyaApp
import com.nya.app.data.DeviceRepository

class DeviceViewModel(private val repo: DeviceRepository) : ViewModel() {
    private val load = Retryable(viewModelScope, repo.device)
    val device = load.state

    fun retry() = load.retry()

    fun transfers() = repo.transfers()

    companion object {
        val Factory = viewModelFactory {
            initializer { DeviceViewModel((this[APPLICATION_KEY] as NyaApp).container.device) }
        }
    }
}
