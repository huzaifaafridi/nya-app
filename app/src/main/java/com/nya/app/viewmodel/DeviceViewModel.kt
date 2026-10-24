package com.nya.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nya.app.NyaApp
import com.nya.app.data.DeviceRepository
import com.nya.app.data.HeadbandSettings
import com.nya.app.data.TransferItem
import com.nya.app.data.ble.FirmwareStep
import com.nya.app.data.ble.LinkState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface FirmwareUiState {
    data object Idle : FirmwareUiState
    data object Checking : FirmwareUiState
    data object UpToDate : FirmwareUiState
    data class Available(val version: String) : FirmwareUiState
    data class Downloading(val progress: Float) : FirmwareUiState
    data class Installing(val progress: Float) : FirmwareUiState
    data class Done(val version: String) : FirmwareUiState
    data class Failed(val message: String) : FirmwareUiState
}

private const val Unreachable = "Couldn't reach your headband."

class DeviceViewModel(private val repo: DeviceRepository) : ViewModel() {
    private val load = Retryable(viewModelScope, repo.device)
    val device = load.state
    val ringing: StateFlow<Boolean> = repo.ringing
    private val running = MutableStateFlow(0)
    val connecting = running.map { it > 0 }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _firmware = MutableStateFlow<FirmwareUiState>(FirmwareUiState.Idle)
    val firmware = _firmware.asStateFlow()
    private var firmwareJob: Job? = null
    private var target: String? = null // the version offered by the last check

    var savingSettings by mutableStateOf(false)
        private set
    var settingsError by mutableStateOf<String?>(null)
        private set
    var settingsInfo by mutableStateOf<String?>(null)
        private set

    fun retry() = load.retry()

    fun setLost(lost: Boolean) {
        viewModelScope.launch { repo.setLost(lost) }
    }

    fun clearSettingsMessages() {
        settingsError = null
        settingsInfo = null
    }

    fun saveSettings(settings: HeadbandSettings) {
        if (savingSettings) return
        savingSettings = true
        settingsError = null
        settingsInfo = null
        viewModelScope.launch {
            try {
                if (!isConnected() && link().isFailure) {
                    settingsError = Unreachable
                    return@launch
                }
                repo.updateSettings(settings)
                    .onSuccess { settingsInfo = "Settings saved" }
                    .onFailure {
                        settingsError = if (it is IllegalArgumentException) it.message else "Couldn't save your settings. Try again."
                    }
            } finally {
                savingSettings = false
            }
        }
    }

    fun check() {
        if (isFirmwareBusy()) return
        _firmware.value = FirmwareUiState.Checking
        firmwareJob = viewModelScope.launch {
            try {
                target = repo.checkFirmware()
                _firmware.value = target?.let { FirmwareUiState.Available(it) } ?: FirmwareUiState.UpToDate
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _firmware.value = FirmwareUiState.Failed("Couldn't check for updates.")
            }
        }
    }

    fun start() {
        val version = target
        if (version == null || isFirmwareBusy()) return
        _firmware.value = FirmwareUiState.Downloading(0f)
        firmwareJob = viewModelScope.launch {
            try {
                if (!isConnected() && link().isFailure) {
                    _firmware.value = FirmwareUiState.Failed(Unreachable)
                    return@launch
                }
                repo.updateFirmware(version).collect {
                    _firmware.value = when (it) {
                        is FirmwareStep.Downloading -> FirmwareUiState.Downloading(it.progress)
                        is FirmwareStep.Installing -> FirmwareUiState.Installing(it.progress)
                        FirmwareStep.Done -> FirmwareUiState.Done(version).also { target = null }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _firmware.value = FirmwareUiState.Failed("The update didn't finish. Your headband is unchanged.")
            }
        }
    }

    /** Stops a check or an update in progress; an update can be started again afterwards. */
    fun cancel() {
        if (!isFirmwareBusy()) return
        firmwareJob?.cancel()
        _firmware.value = target?.let { FirmwareUiState.Available(it) } ?: FirmwareUiState.Idle
    }

    private fun isFirmwareBusy() = _firmware.value.let {
        it == FirmwareUiState.Checking || it is FirmwareUiState.Downloading || it is FirmwareUiState.Installing
    }

    fun connect(onFail: () -> Unit = {}, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            link().onSuccess { onSuccess() }.onFailure { onFail() }
        }
    }

    fun reconnect(onFail: () -> Unit) {
        viewModelScope.launch {
            // an attempt in flight is awaited, not restarted
            if (device.value.valueOrNull()?.state != LinkState.Connecting) repo.disconnect()
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
            emit(Loadable.Failed(Unreachable))
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
