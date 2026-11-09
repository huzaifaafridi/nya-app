package com.nya.app.data

import com.nya.app.data.local.ChatDao
import com.nya.app.data.local.ChatMessageEntity
import com.nya.app.data.local.toMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

interface ChatRepository {
    val history: Flow<List<ChatMessage>>
    /** Nya's in-progress reply: null = idle, "" = waiting for the first words, else the text streamed so far. */
    val draft: StateFlow<String?>
    val error: StateFlow<String?>
    fun send(text: String)
}

data class ChatPersona(val companion: Companion, val userName: String, val mode: Int)

/** Produces Nya's answer to a conversation as text chunks; the swap point for a real model. */
fun interface ReplyEngine {
    fun reply(history: List<ChatMessage>, persona: ChatPersona): Flow<String>
}

class FakeReplyEngine : ReplyEngine {
    private val replies = listOf(
        "Got it. Want me to save that as a memory?",
        "That sounds like a good moment. I can pull up similar ones from this week.",
        "I'm here. Tell me more.",
        "Noted. I'll remind you tomorrow morning."
    )

    override fun reply(history: List<ChatMessage>, persona: ChatPersona): Flow<String> = flow {
        delay(800)
        replies.random().split(" ").forEachIndexed { i, word ->
            if (i > 0) delay(60)
            emit(if (i == 0) word else " $word")
        }
    }
}

internal const val Greeting = "Hey, I'm here. What's on your mind?"
private const val BusyMessage = "Nya is busy right now, try again in a moment."

/** How long a reply may go without new text before it is given up and the busy error shows. */
val ReplyTimeout = 30.seconds

/**
 * Runs a reply in [scope], so it is still saved when the screen is left mid-stream. A reply that sends no new
 * text for [replyTimeout] fails like any other engine error, so the chat never stays on "typing".
 */
abstract class StreamingChat(
    private val replies: ReplyEngine,
    private val scope: CoroutineScope,
    private val replyTimeout: Duration = ReplyTimeout
) : ChatRepository {
    final override val draft = MutableStateFlow<String?>(null)
    final override val error = MutableStateFlow<String?>(null)

    /** The user's message as saved, with the account it belongs to. */
    protected class Saved(val uid: String, val conversation: List<ChatMessage>)

    private val lock = Any()
    private var job: Job? = null
    private val generation = AtomicInteger()

    /** Saves the user's message, or returns null if it cannot be saved. */
    protected abstract suspend fun saveMine(text: String): Saved?
    /** Saves Nya's reply under [uid], unless the signed-in account has changed since. */
    protected abstract suspend fun saveNya(uid: String, text: String)
    protected abstract suspend fun persona(): ChatPersona

    /** Drops any in-flight reply and its state, e.g. when another account signs in. */
    protected fun reset() {
        synchronized(lock) {
            generation.incrementAndGet()
            job?.cancel()
            job = null
        }
        draft.value = null
        error.value = null
    }

    final override fun send(text: String) {
        if (text.isBlank() || !draft.compareAndSet(null, "")) return
        error.value = null
        synchronized(lock) {
            val mine = generation.get()
            job = scope.launch {
                val answer = StringBuilder()
                var saved: Saved? = null
                try {
                    try {
                        val persona = persona()
                        saved = saveMine(text.trim()) ?: return@launch
                        replies.reply(saved.conversation, persona).collectWithIdleTimeout(replyTimeout) {
                            answer.append(it)
                            if (generation.get() == mine) draft.value = answer.toString()
                        }
                        val reply = answer.toString().trim()
                        ensureActive()
                        if (reply.isEmpty()) error.value = BusyMessage else saveNya(saved.uid, reply)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        failed(mine, saved, answer)
                    }
                } finally {
                    if (generation.get() == mine) draft.value = null
                }
            }
        }
    }

    /** Keeps whatever text arrived and shows the busy error, unless the reply has been dropped meanwhile. */
    private suspend fun failed(mine: Int, saved: Saved?, answer: StringBuilder) {
        if (generation.get() != mine) return
        val partial = answer.toString().trim()
        if (saved != null && partial.isNotEmpty()) {
            try {
                saveNya(saved.uid, partial)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (ignored: Throwable) {
                // The partial text is best effort; the busy error still shows.
            }
        }
        error.value = BusyMessage
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class RoomChatRepository(
    private val dao: ChatDao,
    private val cloud: CloudSync,
    private val uid: Flow<String?>,
    private val profile: ProfileRepository,
    private val settings: SettingsRepository,
    replies: ReplyEngine,
    scope: CoroutineScope
) : StreamingChat(replies, scope) {
    init {
        scope.launch { uid.distinctUntilChanged().drop(1).collect { reset() } }
    }

    override val history: Flow<List<ChatMessage>> = uid.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else flow {
            val greeting = message(id, Greeting, mine = false).copy(remoteId = "greeting")
            dao.insertIfEmpty(greeting)
            emitAll(dao.observe(id).map { list -> list.map { it.toMessage() } })
        }
    }

    override suspend fun saveMine(text: String): Saved? {
        val id = uid.first() ?: return null
        save(message(id, text, mine = true))
        return Saved(id, dao.history(id).map { it.toMessage() })
    }

    override suspend fun saveNya(uid: String, text: String) {
        if (this.uid.first() == uid) save(message(uid, text, mine = false))
    }

    private suspend fun save(message: ChatMessageEntity) {
        dao.insert(message)
        cloud.putMessage(message.uid, message.toDoc())
    }

    override suspend fun persona(): ChatPersona {
        val p = profile.profile.first()
        return ChatPersona(p.companion, p.name, settings.settings.first().mode)
    }

    private fun message(uid: String, text: String, mine: Boolean) =
        ChatMessageEntity(uid = uid, text = text, mine = mine, createdAt = System.currentTimeMillis())
}

class FakeChatRepository(
    replies: ReplyEngine = FakeReplyEngine(),
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : StreamingChat(replies, scope) {
    private var nextId = 1L
    private val state = MutableStateFlow(listOf(ChatMessage(nextId++, Greeting, false)))
    override val history: Flow<List<ChatMessage>> = state

    override suspend fun saveMine(text: String) = Saved("", add(text, true))

    override suspend fun saveNya(uid: String, text: String) {
        add(text, false)
    }

    override suspend fun persona() = ChatPersona(Companion.Muse, "", 1)

    private fun add(text: String, mine: Boolean): List<ChatMessage> =
        state.updateAndGet { it + ChatMessage(nextId++, text, mine) }
}

/** A reply sent no new text for the allowed time. */
class ReplyTimeoutException(timeout: Duration) : Exception("No reply text for $timeout")

/**
 * Collects in order like [Flow.collect], but fails with [ReplyTimeoutException] when [timeout] passes without a
 * new value. Unlike `Flow.timeout`, nothing is buffered, so values sent just before an error are never dropped.
 */
internal suspend fun <T> Flow<T>.collectWithIdleTimeout(timeout: Duration, action: suspend (T) -> Unit) = coroutineScope {
    val arrived = Channel<Unit>(Channel.CONFLATED)
    val watchdog = launch {
        while (true) withTimeoutOrNull(timeout) { arrived.receive() } ?: throw ReplyTimeoutException(timeout)
    }
    collect { action(it); arrived.trySend(Unit) }
    watchdog.cancel()
}
