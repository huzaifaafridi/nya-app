package com.nya.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nya.app.NyaApp
import com.nya.app.data.ChatRepository

class ChatViewModel(private val chat: ChatRepository) : ViewModel() {
    private val load = Retryable(viewModelScope, chat.history)
    val messages = load.state
    val draft = chat.draft
    val error = chat.error

    fun retry() = load.retry()

    fun send(text: String) = chat.send(text)

    companion object {
        val Factory = viewModelFactory {
            initializer { ChatViewModel((this[APPLICATION_KEY] as NyaApp).container.chat) }
        }
    }
}
