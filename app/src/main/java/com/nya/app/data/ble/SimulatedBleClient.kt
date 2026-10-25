package com.nya.app.data.ble

import com.nya.app.data.BleConfig
import com.nya.app.data.HeadbandSettings
import com.nya.app.data.TransferItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val CONNECT_MS = 1500L
private const val DRAIN_MS = 60_000L
private const val RING_MS = 8_000L
private const val MIN_BATTERY = 0.05f
private const val SETTINGS_MS = 400L
private const val DOWNLOAD_MS = 6_000L
private const val INSTALL_MS = 4_000L
private const val FIRMWARE_STEPS = 40

/**
 * Fake headband: every 3rd connection attempt fails so the pairing error path is reachable.
 * Path: auto-connect = attempt 1, Reconnect = 2 (ok), Reconnect = 3 (fails); connecting while connected doesn't count.
 */
class SimulatedBleClient(
    private val scope: CoroutineScope,
    private val namePrefix: () -> String = { BleConfig().namePrefix }
) : BleClient {
    private val _state = MutableStateFlow(LinkState.Disconnected)
    private val _status = MutableStateFlow<DeviceStatus?>(null)
    private val _ringing = MutableStateFlow(false)
    private val connectLock = Mutex()
    private val lock = Any() // guards everything below
    private var attempts = 0
    private var generation = 0
    private var battery = 0.82f
    private var name: String? = null
    private var firmware = "1.1.4"
    private var drainJob: Job? = null
    private var ringJob: Job? = null

    override val state: StateFlow<LinkState> = _state
    override val status: StateFlow<DeviceStatus?> = _status
    override val ringing: StateFlow<Boolean> = _ringing

    override suspend fun connect(): Result<Unit> = connectLock.withLock {
        val gen = synchronized(lock) {
            if (_state.value == LinkState.Connected) return@withLock Result.success(Unit)
            attempts++
            _state.value = LinkState.Connecting
            generation
        }
        try {
            delay(CONNECT_MS)
        } catch (e: CancellationException) {
            synchronized(lock) { if (generation == gen) _state.value = LinkState.Disconnected }
            throw e
        }
        synchronized(lock) {
            when {
                generation != gen -> Result.failure(IllegalStateException("Disconnected"))
                attempts % 3 == 0 -> {
                    _state.value = LinkState.Disconnected
                    Result.failure(IllegalStateException("Headband not found"))
                }
                else -> {
                    _status.value = DeviceStatus(name ?: "${namePrefix()} F1", battery, 4.1f, 16f, firmware)
                    _state.value = LinkState.Connected
                    drainJob = scope.launch {
                        while (true) {
                            delay(DRAIN_MS)
                            synchronized(lock) {
                                battery = maxOf(MIN_BATTERY, battery - 0.01f)
                                _status.update { s -> s?.copy(battery = battery) }
                            }
                        }
                    }
                    Result.success(Unit)
                }
            }
        }
    }

    override suspend fun disconnect() = synchronized(lock) {
        generation++
        drainJob?.cancel()
        ringJob?.cancel()
        _ringing.value = false
        _state.value = LinkState.Disconnected
        _status.value = null
    }

    override suspend fun ring(on: Boolean): Result<Unit> = synchronized(lock) {
        if (_state.value != LinkState.Connected) return@synchronized Result.failure(IllegalStateException("Not connected"))
        ringJob?.cancel()
        _ringing.value = on
        ringJob = if (on) scope.launch {
            delay(RING_MS)
            val self = currentCoroutineContext()[Job]
            synchronized(lock) { if (ringJob === self) _ringing.value = false }
        } else null
        Result.success(Unit)
    }

    override suspend fun applySettings(settings: HeadbandSettings): Result<Unit> {
        val gen = synchronized(lock) {
            if (_state.value != LinkState.Connected) return Result.failure(IllegalStateException("Not connected"))
            generation
        }
        delay(SETTINGS_MS)
        return synchronized(lock) {
            if (generation != gen) return@synchronized Result.failure(IllegalStateException("Disconnected"))
            name = settings.name
            _status.update { s -> s?.copy(name = settings.name) }
            Result.success(Unit)
        }
    }

    override fun updateFirmware(version: String): Flow<FirmwareStep> = flow {
        val gen = synchronized(lock) {
            check(_state.value == LinkState.Connected) { "Not connected" }
            generation
        }
        step(gen, DOWNLOAD_MS) { FirmwareStep.Downloading(it) }
        step(gen, INSTALL_MS) { FirmwareStep.Installing(it) }
        synchronized(lock) {
            check(generation == gen) { "Disconnected" }
            firmware = version
            _status.update { s -> s?.copy(firmware = version) }
        }
        emit(FirmwareStep.Done)
    }

    private suspend fun FlowCollector<FirmwareStep>.step(gen: Int, totalMs: Long, make: (Float) -> FirmwareStep) {
        for (i in 1..FIRMWARE_STEPS) {
            delay(totalMs / FIRMWARE_STEPS)
            synchronized(lock) { check(generation == gen) { "Disconnected" } }
            emit(make(i / FIRMWARE_STEPS.toFloat()))
        }
    }

    override fun transfer(): Flow<List<TransferItem>> = flow {
        if (_state.value != LinkState.Connected) {
            emit(emptyList())
            return@flow
        }
        var items = listOf(
            TransferItem("Sunset at the pier", 24, 1f),
            TransferItem("Coffee with Ali", 88, 0.6f),
            TransferItem("Morning walk", 41, 0f),
            TransferItem("Late night idea", 6, 0f)
        )
        emit(items)
        while (true) {
            val i = items.indexOfFirst { it.progress < 1f }
            if (i == -1) break
            delay(120)
            items = items.mapIndexed { j, t -> if (j == i) t.copy(progress = minOf(1f, t.progress + 0.03f)) else t }
            emit(items)
        }
    }
}
