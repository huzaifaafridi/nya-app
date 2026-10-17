package com.nya.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nya.app.NyaApp
import com.nya.app.data.ChatMessage
import com.nya.app.data.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(private val chat: ChatRepository) : ViewModel() {
    val messages: StateFlow<List<ChatMessage>> =
        chat.history.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _typing = MutableStateFlow(false)
    val typing: StateFlow<Boolean> = _typing

    fun send(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _typing.value = true
            try {
                chat.send(text)
            } finally {
                _typing.value = false
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { ChatViewModel((this[APPLICATION_KEY] as NyaApp).container.chat) }
        }
    }
}
