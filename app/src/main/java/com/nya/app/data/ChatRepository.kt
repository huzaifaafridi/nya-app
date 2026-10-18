package com.nya.app.data

import com.nya.app.data.local.ChatDao
import com.nya.app.data.local.ChatMessageEntity
import com.nya.app.data.local.toMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

interface ChatRepository {
    val history: Flow<List<ChatMessage>>
    suspend fun send(text: String)
}

/** Produces Nya's answer to a conversation; the swap point for a real model. */
fun interface ReplyEngine {
    suspend fun reply(history: List<ChatMessage>): String
}

class FakeReplyEngine : ReplyEngine {
    private val replies = listOf(
        "Got it. Want me to save that as a memory?",
        "That sounds like a good moment. I can pull up similar ones from this week.",
        "I'm here. Tell me more.",
        "Noted. I'll remind you tomorrow morning."
    )

    override suspend fun reply(history: List<ChatMessage>): String {
        delay(1200)
        return replies.random()
    }
}

private const val Greeting = "Hey, I'm here. What's on your mind?"

@OptIn(ExperimentalCoroutinesApi::class)
class RoomChatRepository(
    private val dao: ChatDao,
    private val uid: Flow<String?>,
    private val replies: ReplyEngine
) : ChatRepository {
    override val history: Flow<List<ChatMessage>> = uid.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else flow {
            dao.insertIfEmpty(message(id, Greeting, mine = false))
            emitAll(dao.observe(id).map { list -> list.map { it.toMessage() } })
        }
    }

    override suspend fun send(text: String) {
        if (text.isBlank()) return
        val id = uid.first() ?: return
        dao.insert(message(id, text.trim(), mine = true))
        val answer = replies.reply(dao.history(id).map { it.toMessage() })
        dao.insert(message(id, answer, mine = false))
    }

    private fun message(uid: String, text: String, mine: Boolean) =
        ChatMessageEntity(uid = uid, text = text, mine = mine, createdAt = System.currentTimeMillis())
}

class FakeChatRepository(private val replies: ReplyEngine = FakeReplyEngine()) : ChatRepository {
    private var nextId = 1L
    private val state = MutableStateFlow(listOf(ChatMessage(nextId++, Greeting, false)))
    override val history: Flow<List<ChatMessage>> = state

    override suspend fun send(text: String) {
        if (text.isBlank()) return
        add(text.trim(), true)
        add(replies.reply(state.value), false)
    }

    private fun add(text: String, mine: Boolean) = state.update { it + ChatMessage(nextId++, text, mine) }
}
