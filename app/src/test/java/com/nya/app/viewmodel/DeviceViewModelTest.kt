@file:OptIn(ExperimentalCoroutinesApi::class)

package com.nya.app.viewmodel

import com.nya.app.MainDispatcherRule
import com.nya.app.data.DeviceInfo
import com.nya.app.data.DeviceRepository
import com.nya.app.data.HeadbandSettings
import com.nya.app.data.ble.BleClient
import com.nya.app.data.ble.FirmwareStep
import com.nya.app.data.ble.LinkState
import com.nya.app.data.ble.SimulatedBleClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DeviceViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private inner class BleDeviceRepository(private val ble: BleClient) : DeviceRepository {
        override val device = ble.state.map { DeviceInfo("NYA F1", "", 0.82f, 4.1f, 16f, "1.1.4", null, it) }
        override val ringing = ble.ringing

        override suspend fun connect() = ble.connect()

        override suspend fun disconnect() = ble.disconnect()

        override suspend fun ring(on: Boolean) = ble.ring(on)

        override fun transfers() = ble.transfer()

        override suspend fun updateSettings(settings: HeadbandSettings) =
            if (settings.name.isBlank()) Result.failure(IllegalArgumentException("Enter a name.")) else ble.applySettings(settings)

        override suspend fun setLost(lost: Boolean) {
            lostCalls += lost
        }

        override suspend fun checkFirmware() = latest

        override fun updateFirmware(version: String) = ble.updateFirmware(version)
    }

    private var latest: String? = "1.2.0"
    private val lostCalls = mutableListOf<Boolean>()

    private fun TestScope.viewModel() = DeviceViewModel(BleDeviceRepository(SimulatedBleClient(backgroundScope)))

    private fun DeviceViewModel.linkState() = (device.value as Loadable.Ready).value?.state

    @Test
    fun connectingIsTrueWhileAConnectIsInFlight() = runTest {
        val vm = viewModel()
        vm.connect()
        runCurrent()
        assertTrue(vm.connecting.value)
        advanceTimeBy(1500)
        runCurrent()
        assertFalse(vm.connecting.value)
    }

    @Test
    fun reconnectWhileConnectingWaitsForTheAttemptInsteadOfRestartingIt() = runTest {
        val vm = viewModel()
        var connected = false
        var failed = false
        vm.connect(onSuccess = { connected = true })
        runCurrent()
        assertEquals(LinkState.Connecting, vm.linkState())
        vm.reconnect { failed = true }
        advanceUntilIdle()
        assertTrue(connected)
        assertFalse(failed)
        assertEquals(LinkState.Connected, vm.linkState())
    }

    private fun TestScope.advanceBy(ms: Long) {
        advanceTimeBy(ms)
        runCurrent()
    }

    @Test
    fun checkFindsAnUpdateOrReportsUpToDate() = runTest {
        val vm = viewModel()
        vm.check()
        runCurrent()
        assertEquals(FirmwareUiState.Available("1.2.0"), vm.firmware.value)
        latest = null
        vm.check()
        runCurrent()
        assertEquals(FirmwareUiState.UpToDate, vm.firmware.value)
    }

    @Test
    fun checkingAgainReplacesASettledResultButNeverAnUpdateInProgress() = runTest {
        val vm = viewModel()
        vm.check()
        runCurrent()
        vm.start()
        advanceBy(1500 + 3000 + 3000 + 2000 + 2000)
        assertEquals(FirmwareUiState.Done("1.2.0"), vm.firmware.value)
        latest = null
        vm.check()
        runCurrent()
        assertEquals(FirmwareUiState.UpToDate, vm.firmware.value)
        latest = "1.3.0"
        vm.check()
        runCurrent()
        vm.start()
        advanceBy(3000)
        vm.check()
        advanceBy(0)
        assertEquals(FirmwareUiState.Downloading(0.5f), vm.firmware.value)
    }

    @Test
    fun updateRunsThroughDownloadingInstallingDone() = runTest {
        val vm = viewModel()
        vm.check()
        runCurrent()
        vm.start()
        advanceBy(1500 + 3000) // connect, then half of the download
        assertEquals(FirmwareUiState.Downloading(0.5f), vm.firmware.value)
        advanceBy(3000 + 2000)
        assertEquals(FirmwareUiState.Installing(0.5f), vm.firmware.value)
        advanceBy(2000)
        assertEquals(FirmwareUiState.Done("1.2.0"), vm.firmware.value)
    }

    @Test
    fun startFailsWhenTheHeadbandCannotBeReached() = runTest {
        val ble = SimulatedBleClient(backgroundScope)
        val vm = DeviceViewModel(BleDeviceRepository(ble))
        vm.check()
        runCurrent()
        repeat(2) { ble.connect(); ble.disconnect() } // the next attempt fails
        vm.start()
        advanceBy(1500)
        assertEquals(FirmwareUiState.Failed("Couldn't reach your headband."), vm.firmware.value)
    }

    @Test
    fun cancelReturnsToAvailableAndAllowsAnotherStart() = runTest {
        val vm = viewModel()
        vm.check()
        runCurrent()
        vm.start()
        advanceBy(1500 + 2000)
        vm.cancel()
        advanceBy(10_000)
        assertEquals(FirmwareUiState.Available("1.2.0"), vm.firmware.value)
        vm.start()
        advanceBy(10_000)
        assertEquals(FirmwareUiState.Done("1.2.0"), vm.firmware.value)
    }

    @Test
    fun savingSettingsReportsSuccessAndValidationErrors() = runTest {
        val vm = viewModel()
        vm.saveSettings(HeadbandSettings(" "))
        advanceBy(5000)
        assertEquals("Enter a name.", vm.settingsError)
        assertFalse(vm.savingSettings)
        vm.saveSettings(HeadbandSettings("Mine"))
        advanceBy(5000)
        assertEquals("Settings saved", vm.settingsInfo)
        assertEquals(null, vm.settingsError)
    }

    @Test
    fun clearingSettingsMessagesRemovesTheOldResult() = runTest {
        val vm = viewModel()
        vm.saveSettings(HeadbandSettings(" "))
        advanceBy(5000)
        vm.clearSettingsMessages()
        assertEquals(null, vm.settingsError)
        vm.saveSettings(HeadbandSettings("Mine"))
        advanceBy(5000)
        vm.clearSettingsMessages()
        assertEquals(null, vm.settingsInfo)
    }

    @Test
    fun setLostPassesThrough() = runTest {
        val vm = viewModel()
        vm.setLost(true)
        runCurrent()
        assertEquals(listOf(true), lostCalls)
    }
}
