package com.nya.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nya.app.NyaApp
import com.nya.app.data.AppConfig
import com.nya.app.data.ChatQuota
import com.nya.app.data.ChatRepository
import com.nya.app.data.Plan
import com.nya.app.data.UsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chat: ChatRepository,
    private val config: StateFlow<AppConfig>,
    plan: Flow<Plan>,
    private val usage: UsageRepository
) : ViewModel() {
    private val load = Retryable(viewModelScope, chat.history)
    val messages = load.state
    val draft = chat.draft
    val error = chat.error

    val quota: StateFlow<ChatQuota> = combine(plan, config, usage.today()) { p, c, used ->
        ChatQuota(used, c.plans.getValue(p).dailyMessages)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ChatQuota(0, 0))

    fun retry() = load.retry()

    fun send(text: String) {
        if (!config.value.features.aiChat || quota.value.reached || text.isBlank() || draft.value != null) return
        chat.send(text)
        viewModelScope.launch { usage.recordMessage() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as NyaApp).container
                ChatViewModel(c.chat, c.appConfig.config, c.billing.plan, c.usage)
            }
        }
    }
}
