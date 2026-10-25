@file:OptIn(ExperimentalCoroutinesApi::class)

package com.nya.app.data

import androidx.datastore.preferences.core.mutablePreferencesOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

private class MutableClock(var now: Instant) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId): Clock = this
    override fun instant(): Instant = now
}

class PlanUsageTest {
    private val uid = MutableStateFlow<String?>("a")
    private val store = FakeStore(mutablePreferencesOf())
    private val clock = MutableClock(Instant.parse("2026-10-25T10:00:00Z"))

    @Test
    fun quotaCountsDownAndStopsAtTheLimit() {
        assertEquals(10, ChatQuota(20, 30).remaining)
        assertFalse(ChatQuota(29, 30).reached)
        assertTrue(ChatQuota(30, 30).reached)
        assertEquals(0, ChatQuota(35, 30).remaining)
    }

    @Test
    fun zeroLimitIsUnlimited() {
        assertTrue(ChatQuota(5000, 0).unlimited)
        assertFalse(ChatQuota(5000, 0).reached)
    }

    @Test
    fun aHigherPlanRaisesTheLimit() {
        val plans = AppConfig().plans
        val used = 30
        assertTrue(ChatQuota(used, plans.getValue(Plan.Free).dailyMessages).reached)
        assertFalse(ChatQuota(used, plans.getValue(Plan.Plus).dailyMessages).reached)
    }

    @Test
    fun storedCountOnlyCountsOnItsOwnDay() {
        val day = LocalDate.of(2026, 10, 25)
        assertEquals(3, countOn("2026-10-25|3", day))
        assertEquals(0, countOn("2026-10-24|3", day))
        assertEquals(0, countOn(null, day))
        assertEquals(0, countOn("garbage", day))
    }

    @Test
    fun usageResetsOnANewDay() = runTest {
        val usage = DataStoreUsageRepository(store, uid, clock)
        usage.recordMessage()
        usage.recordMessage()
        assertEquals(2, usage.today().first())
        clock.now = Instant.parse("2026-10-26T00:00:01Z")
        assertEquals(0, usage.today().first())
        usage.recordMessage()
        assertEquals(1, usage.today().first())
    }

    @Test
    fun usageIsPerAccount() = runTest {
        val usage = DataStoreUsageRepository(store, uid, clock)
        usage.recordMessage()
        uid.value = "b"
        assertEquals(0, usage.today().first())
        uid.value = "a"
        assertEquals(1, usage.today().first())
        uid.value = null
        usage.recordMessage()
        assertEquals(0, usage.today().first())
    }

    @Test
    fun usageRollsOverAtMidnightWhileObserved() = runTest {
        clock.now = Instant.parse("2026-10-25T23:59:00Z")
        val usage = DataStoreUsageRepository(store, uid, clock)
        usage.recordMessage()
        val seen = mutableListOf<Int>()
        val job = backgroundScope.launch { usage.today().collect { seen += it } }
        advanceTimeBy(1_000)
        assertEquals(listOf(1), seen)
        clock.now = Instant.parse("2026-10-26T00:00:00Z")
        advanceTimeBy(60_000)
        assertEquals(listOf(1, 0), seen)
        job.cancel()
    }

    @Test
    fun billingKeepsAPlanPerAccount() = runTest {
        val billing = FakeBillingRepository(store, uid, purchaseDelayMs = 0)
        assertEquals(Plan.Free, billing.plan.first())
        assertTrue(billing.purchase(Plan.Pro).isSuccess)
        assertEquals(Plan.Pro, billing.plan.first())
        uid.value = "b"
        assertEquals(Plan.Free, billing.plan.first())
        uid.value = "a"
        assertTrue(billing.restore().isSuccess)
        assertEquals(Plan.Pro, billing.plan.first())
        assertTrue(billing.purchase(Plan.Free).isSuccess)
        assertEquals(Plan.Free, billing.plan.first())
    }

    @Test
    fun billingIsFreeAndRefusesPurchasesWhenSignedOut() = runTest {
        val billing = FakeBillingRepository(store, uid, purchaseDelayMs = 0)
        uid.value = null
        assertEquals(Plan.Free, billing.plan.first())
        assertTrue(billing.purchase(Plan.Plus).isFailure)
    }
}
