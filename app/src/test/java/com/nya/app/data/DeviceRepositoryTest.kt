@file:OptIn(ExperimentalCoroutinesApi::class)

package com.nya.app.data

import com.nya.app.data.ble.FirmwareStep
import com.nya.app.data.ble.SimulatedBleClient
import com.nya.app.data.local.DeviceEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val Uid = "u1"

private class Catalog(var latest: String?) : FirmwareCatalog {
    override suspend fun latest() = latest
}

class DeviceRepositoryTest {
    private val dao = FakeDeviceDao()
    private val cloud = FakeCloud()
    private val catalog = Catalog("1.2.0")

    private fun TestScope.repo(signedIn: Boolean = true, row: DeviceEntity? = DeviceEntity(Uid, "NYA F1", "Wi-Fi", 0.82f, 4.1f, 16f, "1.1.4", "1.2.0")) =
        RoomDeviceRepository(dao, cloud, flowOf(if (signedIn) Uid else null), SimulatedBleClient(backgroundScope), catalog, backgroundScope) { 1234L }
            .also {
                if (row != null) dao.rows.value = mapOf(Uid to row)
                runCurrent() // starts the quiet auto-connect, which drops any earlier link
            }

    private val row get() = dao.rows.value.getValue(Uid)

    @Test
    fun newerVersionsAreComparedNumerically() {
        assertTrue(isNewerVersion("1.10.0", "1.9.0"))
        assertTrue(isNewerVersion("1.2.0", "1.1.4"))
        assertTrue(isNewerVersion("1.2.1", "1.2"))
        assertFalse(isNewerVersion("1.2.0", "1.2"))
        assertFalse(isNewerVersion("1.1.4", "1.2.0"))
    }

    @Test
    fun checkFirmwareStoresTheNewerVersionAndPushesIt() = runTest {
        val repo = repo(row = DeviceEntity(Uid, "NYA F1", "Wi-Fi", 0.5f, 1f, 16f, "1.1.4", null))
        assertEquals("1.2.0", repo.checkFirmware())
        assertEquals("1.2.0", row.latestFirmware)
        assertEquals("1.2.0", cloud.devicePuts.last().latestFirmware)
    }

    @Test
    fun checkFirmwareClearsLatestWhenUpToDate() = runTest {
        val repo = repo()
        catalog.latest = "1.1.4"
        assertNull(repo.checkFirmware())
        assertNull(row.latestFirmware)
        assertNull(cloud.devicePuts.last().latestFirmware)
    }

    @Test
    fun checkFirmwareFailsWithoutAPairedHeadband() = runTest {
        val repo = repo(row = null)
        assertTrue(runCatching { repo.checkFirmware() }.isFailure)
        assertTrue(cloud.devicePuts.isEmpty())
    }

    @Test
    fun setLostUpdatesTheRowAndPushes() = runTest {
        val repo = repo()
        repo.setLost(true)
        assertTrue(row.lost)
        assertEquals(1234L, row.lostAt)
        assertEquals(true to 1234L, cloud.devicePuts.last().let { it.lost to it.lostAt })
        repo.setLost(false)
        assertFalse(row.lost)
        assertNull(row.lostAt)
        assertNull(cloud.devicePuts.last().lostAt)
    }

    @Test
    fun setLostDoesNothingWhenSignedOutOrWithoutDevice() = runTest {
        repo(signedIn = false).setLost(true)
        assertFalse(row.lost)
        dao.rows.value = emptyMap()
        repo(row = null).setLost(true)
        assertTrue(cloud.devicePuts.isEmpty())
    }

    @Test
    fun updateSettingsSavesTrimmedNameAndPushesWhenConnected() = runTest {
        val repo = repo()
        repo.connect()
        assertTrue(repo.updateSettings(HeadbandSettings("  Mine ", false, 2, false)).isSuccess)
        assertEquals(Triple("Mine", false, 2), Triple(row.name, row.autoSync, row.captureQuality))
        assertFalse(row.ledIndicator)
        assertEquals("Mine", cloud.devicePuts.last().name)
        assertEquals(2, cloud.devicePuts.last().captureQuality)
    }

    @Test
    fun updateSettingsRejectsBlankAndTooLongNamesAndBadQuality() = runTest {
        val repo = repo()
        repo.connect()
        val puts = cloud.devicePuts.size
        listOf(
            HeadbandSettings("   "),
            HeadbandSettings("x".repeat(MaxDeviceNameLength + 1)),
            HeadbandSettings("Mine", captureQuality = 3)
        ).forEach {
            assertTrue(repo.updateSettings(it).exceptionOrNull() is IllegalArgumentException)
        }
        assertEquals("NYA F1", row.name)
        assertEquals(puts, cloud.devicePuts.size)
        assertTrue(repo.updateSettings(HeadbandSettings("x".repeat(MaxDeviceNameLength))).isSuccess)
        assertEquals("x".repeat(MaxDeviceNameLength), row.name)
    }

    @Test
    fun updateSettingsSavesNothingWhenTheHeadbandIsUnreachable() = runTest {
        val repo = repo()
        val before = row
        assertTrue(repo.updateSettings(HeadbandSettings("Mine", false, 2, false)).isFailure)
        assertEquals(before, row)
        assertTrue(cloud.devicePuts.isEmpty())
    }

    @Test
    fun finishedFirmwareUpdateClearsLatestAndPushes() = runTest {
        val repo = repo()
        repo.connect()
        val steps = repo.updateFirmware("1.2.0").toList()
        runCurrent()
        assertEquals(FirmwareStep.Done, steps.last())
        assertNull(row.latestFirmware)
        assertEquals("1.2.0", row.firmware)
        assertEquals(null to "1.2.0", cloud.devicePuts.last().let { it.latestFirmware to it.firmware })
    }

    @Test
    fun readingsNeverOverwriteTheCustomNameOrSettings() = runTest {
        val repo = repo(row = DeviceEntity(Uid, "Mine", "Wi-Fi", 0.5f, 1f, 16f, "1.1.4", null, autoSync = false, captureQuality = 2, ledIndicator = false))
        repo.connect()
        runCurrent()
        assertEquals(Triple("Mine", false, 2), Triple(row.name, row.autoSync, row.captureQuality))
        assertFalse(row.ledIndicator)
    }

    @Test
    fun deviceDocRoundTripsTheNewFields() {
        val entity = DeviceEntity(Uid, "Mine", "Wi-Fi", 0.5f, 1f, 16f, "1.2.0", null, false, 2, false, true, 99L)
        assertEquals(entity.toDoc().toEntity(Uid), entity)
        assertTrue(entity.toDoc().lost)
    }
}
