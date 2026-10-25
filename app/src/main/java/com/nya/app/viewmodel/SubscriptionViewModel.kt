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
import com.nya.app.data.BillingRepository
import com.nya.app.data.Plan
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SubscriptionViewModel(private val billing: BillingRepository) : ViewModel() {
    val products = billing.products
    val plan: StateFlow<Plan> = billing.plan.stateIn(viewModelScope, SharingStarted.Eagerly, Plan.Free)

    var busy by mutableStateOf(false)
        private set
    /** The plan being purchased while [busy]; null while restoring. */
    var choosing by mutableStateOf<Plan?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var info by mutableStateOf<String?>(null)
        private set

    fun choose(target: Plan) {
        if (busy || target == plan.value) return
        run("Couldn't change your plan. Please try again.", "You're now on the ${target.name} plan.", target) { billing.purchase(target) }
    }

    fun restore() {
        if (!busy) run("Couldn't restore purchases. Please try again.", "Purchases restored.", null) { billing.restore() }
    }

    private fun run(failure: String, success: String, target: Plan?, action: suspend () -> Result<Unit>) {
        busy = true
        choosing = target
        error = null
        info = null
        viewModelScope.launch {
            if (action().isSuccess) info = success else error = failure
            busy = false
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { SubscriptionViewModel((this[APPLICATION_KEY] as NyaApp).container.billing) }
        }
    }
}
