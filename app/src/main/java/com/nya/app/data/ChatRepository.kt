package com.nya.app.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

interface ChatRepository {
    val history: Flow<List<ChatMessage>>
    suspend fun send(text: String)
}

class FakeChatRepository : ChatRepository {
    private val replies = listOf(
        "Got it. Want me to save that as a memory?",
        "That sounds like a good moment. I can pull up similar ones from this week.",
        "I'm here. Tell me more.",
        "Noted. I'll remind you tomorrow morning."
    )
    private var nextId = 1L
    private val state = MutableStateFlow(listOf(ChatMessage(nextId++, "Hey, I'm here. What's on your mind?", false)))
    override val history: Flow<List<ChatMessage>> = state

    override suspend fun send(text: String) {
        if (text.isBlank()) return
        add(text.trim(), true)
        delay(1200)
        add(replies.random(), false)
    }

    private fun add(text: String, mine: Boolean) = state.update { it + ChatMessage(nextId++, text, mine) }
}
