package com.nya.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nya.app.NyaApp
import com.nya.app.data.DeviceRepository
import com.nya.app.data.TransferItem
import com.nya.app.data.ble.LinkState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DeviceViewModel(private val repo: DeviceRepository) : ViewModel() {
    private val load = Retryable(viewModelScope, repo.device)
    val device = load.state
    val ringing: StateFlow<Boolean> = repo.ringing
    private val running = MutableStateFlow(0)
    val connecting = running.map { it > 0 }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        // quiet reconnect to a known headband; a failure just leaves it disconnected
        viewModelScope.launch {
            val known = device.first { it !is Loadable.Loading }.valueOrNull()
            if (known?.state == LinkState.Disconnected) link()
        }
    }

    fun retry() = load.retry()

    fun connect(onFail: () -> Unit = {}, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            link().onSuccess { onSuccess() }.onFailure { onFail() }
        }
    }

    fun reconnect(onFail: () -> Unit) {
        viewModelScope.launch {
            repo.disconnect()
            link().onFailure { onFail() }
        }
    }

    fun ring(on: Boolean) {
        viewModelScope.launch {
            if (on && !isConnected() && link().isFailure) return@launch
            repo.ring(on)
        }
    }

    fun transfers(): Flow<Loadable<List<TransferItem>>> = flow {
        if (!isConnected() && link().isFailure) {
            emit(Loadable.Failed("Couldn't reach your headband."))
            return@flow
        }
        emitAll(repo.transfers().map { Loadable.Ready(it) })
    }

    private suspend fun link(): Result<Unit> {
        running.update { it + 1 }
        try {
            return repo.connect()
        } finally {
            running.update { it - 1 }
        }
    }

    private fun isConnected() = device.value.valueOrNull()?.state == LinkState.Connected

    companion object {
        val Factory = viewModelFactory {
            initializer { DeviceViewModel((this[APPLICATION_KEY] as NyaApp).container.device) }
        }
    }
}
