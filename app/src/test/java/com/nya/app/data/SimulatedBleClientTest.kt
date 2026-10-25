@file:OptIn(ExperimentalCoroutinesApi::class)

package com.nya.app.data

import com.nya.app.data.ble.FirmwareStep
import com.nya.app.data.ble.LinkState
import com.nya.app.data.ble.SimulatedBleClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SimulatedBleClientTest {
    private fun TestScope.advanceBy(ms: Long) {
        advanceTimeBy(ms)
        runCurrent()
    }

    @Test
    fun connectGoesThroughConnectingToConnectedWithStatus() = runTest {
        val ble = SimulatedBleClient(backgroundScope)
        val result = async { ble.connect() }
        runCurrent()
        assertEquals(LinkState.Connecting, ble.state.value)
        advanceBy(1499)
        assertEquals(LinkState.Connecting, ble.state.value)
        advanceBy(1)
        assertTrue(result.await().isSuccess)
        assertEquals(LinkState.Connected, ble.state.value)
        val status = ble.status.value!!
        assertTrue(status.name.startsWith(BleConfig().namePrefix))
        assertEquals(0.82f, status.battery, 1e-6f)
        assertEquals(4.1f, status.storageUsedGb, 1e-6f)
        assertEquals(16f, status.storageTotalGb, 1e-6f)
        assertEquals("1.1.4", status.firmware)
    }

    @Test
    fun everyThirdAttemptFails() = runTest {
        val ble = SimulatedBleClient(backgroundScope)
        val results = List(4) {
            ble.connect().isSuccess.also { ble.disconnect() }
        }
        assertEquals(listOf(true, true, false, true), results)
    }

    @Test
    fun failedAttemptLeavesLinkDisconnected() = runTest {
        val ble = SimulatedBleClient(backgroundScope)
        repeat(2) { ble.connect(); ble.disconnect() }
        assertTrue(ble.connect().isFailure)
        assertEquals(LinkState.Disconnected, ble.state.value)
    }

    @Test
    fun connectWhileConnectedDoesNotCountAnAttempt() = runTest {
        val ble = SimulatedBleClient(backgroundScope)
        ble.connect()
        assertTrue(ble.connect().isSuccess)
        ble.disconnect()
        assertTrue(ble.connect().isSuccess) // attempt 2
        ble.disconnect()
        assertTrue(ble.connect().isFailure) // attempt 3
    }

    @Test
    fun disconnectDuringConnectingWins() = runTest {
        val ble = SimulatedBleClient(backgroundScope)
        val result = async { ble.connect() }
        runCurrent()
        ble.disconnect()
        advanceUntilIdle()
        assertTrue(result.await().isFailure)
        assertEquals(LinkState.Disconnected, ble.state.value)
    }

    @Test
    fun batteryDropsOnePercentEveryMinute() = runTest {
        val ble = SimulatedBleClient(backgroundScope)
        ble.connect()
        advanceBy(60_000)
        assertEquals(0.81f, ble.status.value!!.battery, 1e-4f)
    }

    @Test
    fun batteryNeverDropsBelowFivePercent() = runTest {
        val ble = SimulatedBleClient(backgroundScope)
        ble.connect()
        advanceBy(100 * 60_000L)
        assertEquals(0.05f, ble.status.value!!.battery, 1e-6f)
    }

    @Test
    fun batteryLevelIsKeptAcrossReconnect() = runTest {
        val ble = SimulatedBleClient(backgroundScope)
        ble.connect()
        advanceBy(120_000)
        ble.disconnect()
        ble.connect()
        assertEquals(0.80f, ble.status.value!!.battery, 1e-4f)
    }

    @Test
    fun ringFailsWhenDisconnected() = runTest {
        val ble = SimulatedBleClient(backgroundScope)
        assertTrue(ble.ring(true).isFailure)
        assertFalse(ble.ringing.value)
    }

    @Test
    fun ringStopsByItselfAfterEightSeconds() = runTest {
        val ble = SimulatedBleClient(backgroundScope)
        ble.connect()
        ble.ring(true)
        assertTrue(ble.ringing.value)
        advanceBy(7999)
        assertTrue(ble.ringing.value)
        advanceBy(1)
        assertFalse(ble.ringing.value)
    }

    @Test
    fun secondRingIsNotCutShortByTheFirstAutoStop() = runTest {
        val ble = SimulatedBleClient(backgroundScope)
        ble.connect()
        ble.ring(true)
        advanceBy(5000)
        ble.ring(false)
        ble.ring(true)
        advanceBy(4000) // past the first ring's 8 s
        assertTrue(ble.ringing.value)
        advanceBy(4000)
        assertFalse(ble.ringing.value)
    }

    @Test
    fun applySettingsFailsWhenDisconnected() = runTest {
        val ble = SimulatedBleClient(backgroundScope)
        assertTrue(ble.applySettings(HeadbandSettings("Mine")).isFailure)
    }

    @Test
    fun applySettingsRenamesTheStatusAndKeepsTheNameAcrossReconnect() = runTest {
        val ble = SimulatedBleClient(backgroundScope)
        ble.connect()
        assertTrue(ble.applySettings(HeadbandSettings("Mine")).isSuccess)
        assertEquals("Mine", ble.status.value!!.name)
        ble.disconnect()
        ble.connect()
        assertEquals("Mine", ble.status.value!!.name)
    }

    @Test
    fun applySettingsFailsWhenTheLinkDropsMeanwhile() = runTest {
        val ble = SimulatedBleClient(backgroundScope)
        ble.connect()
        val result = async { ble.applySettings(HeadbandSettings("Mine")) }
        runCurrent()
        ble.disconnect()
        advanceUntilIdle()
        assertTrue(result.await().isFailure)
    }

    @Test
    fun firmwareUpdateDownloadsThenInstallsThenChangesFirmware() = runTest {
        val ble = SimulatedBleClient(backgroundScope)
        ble.connect()
        val steps = ble.updateFirmware("1.2.0").toList()
        assertEquals(81, steps.size)
        assertEquals(FirmwareStep.Downloading(1f / 40), steps.first())
        assertEquals(FirmwareStep.Downloading(1f), steps[39])
        assertEquals(FirmwareStep.Installing(1f / 40), steps[40])
        assertEquals(FirmwareStep.Installing(1f), steps[79])
        assertEquals(FirmwareStep.Done, steps.last())
        assertEquals("1.2.0", ble.status.value!!.firmware)
        assertEquals(10_000L, testScheduler.currentTime - 1500)
    }

    @Test
    fun firmwareStaysUpdatedAcrossReconnect() = runTest {
        val ble = SimulatedBleClient(backgroundScope)
        ble.connect()
        ble.updateFirmware("1.2.0").toList()
        ble.disconnect()
        ble.connect()
        assertEquals("1.2.0", ble.status.value!!.firmware)
    }

    @Test
    fun firmwareUpdateFailsWhenDisconnected() = runTest {
        var error: Throwable? = null
        SimulatedBleClient(backgroundScope).updateFirmware("1.2.0").catch { error = it }.toList()
        assertNotNull(error)
    }

    @Test
    fun cancellingTheFirmwareUpdateLeavesFirmwareUnchanged() = runTest {
        val ble = SimulatedBleClient(backgroundScope)
        ble.connect()
        val job = launch { ble.updateFirmware("1.2.0").collect {} }
        advanceBy(7000) // into the install phase
        job.cancel()
        advanceUntilIdle()
        assertEquals("1.1.4", ble.status.value!!.firmware)
    }

    @Test
    fun linkDroppingMidUpdateFailsIt() = runTest {
        val ble = SimulatedBleClient(backgroundScope)
        ble.connect()
        var error: Throwable? = null
        val steps = mutableListOf<FirmwareStep>()
        launch { ble.updateFirmware("1.2.0").catch { error = it }.collect { steps += it } }
        advanceBy(3000)
        ble.disconnect()
        advanceUntilIdle()
        assertNotNull(error)
        assertTrue(steps.isNotEmpty() && steps.none { it == FirmwareStep.Done })
        ble.connect()
        assertEquals("1.1.4", ble.status.value!!.firmware)
    }
}
