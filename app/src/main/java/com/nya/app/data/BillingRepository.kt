package com.nya.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

data class PlanProduct(val plan: Plan, val price: String, val perks: List<String>)

/** Sells plans. The swap point for Google Play Billing: replace the one class wired in AppContainer. */
interface BillingRepository {
    val products: List<PlanProduct>
    /** The signed-in user's current plan; Free when signed out or unknown. */
    val plan: Flow<Plan>
    suspend fun purchase(plan: Plan): Result<Unit>
    suspend fun restore(): Result<Unit>
}

/**
 * Test mode: no real charge. The chosen plan is kept per account in [store]; the server or Play
 * will own the plan later, so nothing is written to Firestore.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FakeBillingRepository(
    private val store: DataStore<Preferences>,
    private val uid: Flow<String?>,
    private val purchaseDelayMs: Long = 1_000
) : BillingRepository {
    override val products = listOf(
        PlanProduct(Plan.Free, "$0", listOf("Basic memories", "Chat with Nya", "1 device")),
        PlanProduct(Plan.Plus, "$4.99 / mo", listOf("Unlimited memories", "Voice replies", "Priority sync")),
        PlanProduct(Plan.Pro, "$9.99 / mo", listOf("Everything in Plus", "Advanced insights", "Early features"))
    )

    override val plan: Flow<Plan> = uid.flatMapLatest { id ->
        if (id == null) flowOf(Plan.Free)
        else store.safeData().map { p -> Plan.entries.firstOrNull { it.name == p[key(id)] } ?: Plan.Free }
    }

    override suspend fun purchase(plan: Plan): Result<Unit> {
        val id = uid.first() ?: return Result.failure(IllegalStateException("Not signed in"))
        delay(purchaseDelayMs)
        store.edit { it[key(id)] = plan.name }
        return Result.success(Unit)
    }

    /** The stored plan is already what [plan] reads, so there is nothing more to fetch. */
    override suspend fun restore() = Result.success(Unit)

    private fun key(uid: String) = stringPreferencesKey("plan_$uid")
}
