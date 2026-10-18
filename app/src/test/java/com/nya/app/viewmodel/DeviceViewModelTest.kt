@file:OptIn(ExperimentalCoroutinesApi::class)

package com.nya.app.viewmodel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.nya.app.MainDispatcherRule
import com.nya.app.data.DeviceInfo
import com.nya.app.data.DeviceRepository
import com.nya.app.data.ble.BleClient
import com.nya.app.data.ble.LinkState
import com.nya.app.data.ble.SimulatedBleClient
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

    private class BleDeviceRepository(private val ble: BleClient) : DeviceRepository {
        override val device = ble.state.map { DeviceInfo("NYA F1", "", 0.82f, 4.1f, 16f, "1.1.4", null, it) }
        override val ringing = ble.ringing

        override suspend fun connect() = ble.connect()

        override suspend fun disconnect() = ble.disconnect()

        override suspend fun ring(on: Boolean) = ble.ring(on)

        override fun transfers() = ble.transfer()
    }

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
}
