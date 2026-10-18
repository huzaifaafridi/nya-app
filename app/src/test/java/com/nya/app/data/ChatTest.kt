@file:OptIn(ExperimentalCoroutinesApi::class)

package com.nya.app.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import androidx.lifecycle.viewModelScope
import com.nya.app.MainDispatcherRule
import com.nya.app.viewmodel.ChatViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ChatTest {
    @get:Rule val main = MainDispatcherRule()

    /** advanceUntilIdle ignores backgroundScope work, so move time explicitly. */
    private fun TestScope.settle() {
        advanceTimeBy(1_000)
        runCurrent()
    }

    private val busy = "Nya is busy right now, try again in a moment."

    private fun engine(chunks: suspend FlowCollector<String>.() -> Unit) = ReplyEngine { _, _ -> flow(chunks) }

    /** Texts after the greeting. */
    private suspend fun ChatRepository.texts() = history.first().drop(1).map { it.text }

    /** Keeps messages per account in memory, like RoomChatRepository, and resets on account switch. */
    private class AccountChat(replies: ReplyEngine, scope: CoroutineScope) : StreamingChat(replies, scope) {
        private val account = MutableStateFlow("a")
        private val messages = mutableMapOf<String, List<ChatMessage>>()
        override val history: Flow<List<ChatMessage>> get() = throw UnsupportedOperationException()

        fun textsOf(uid: String) = messages[uid].orEmpty().map { it.text }

        fun switchTo(uid: String) {
            account.value = uid
            reset()
        }

        override suspend fun saveMine(text: String): Saved {
            val uid = account.value
            val all = messages[uid].orEmpty() + ChatMessage(0, text, true)
            messages[uid] = all
            return Saved(uid, all)
        }

        override suspend fun saveNya(uid: String, text: String) {
            if (account.value == uid) messages[uid] = messages[uid].orEmpty() + ChatMessage(0, text, false)
        }

        override suspend fun persona() = ChatPersona(Companion.Muse, "", 1)
    }

    @Test
    fun replyStreamsIntoDraftThenIsSavedOnce() = runTest {
        val chat = FakeChatRepository(engine {
            emit("Hel"); delay(100); emit("lo"); delay(100)
        }, backgroundScope)
        val seen = mutableListOf<String?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { chat.draft.toList(seen) }
        chat.send("hi")
        settle()
        assertEquals(listOf(null, "", "Hel", "Hello", null), seen)
        assertEquals(listOf("hi", "Hello"), chat.texts())
    }

    @Test
    fun engineErrorShowsBusyAndSavesThePartialText() = runTest {
        val chat = FakeChatRepository(engine { emit("par"); error("boom") }, backgroundScope)
        chat.send("hi")
        settle()
        assertEquals(busy, chat.error.value)
        assertNull(chat.draft.value)
        assertEquals(listOf("hi", "par"), chat.texts())
    }

    @Test
    fun emptyReplyShowsBusyAndSavesNothing() = runTest {
        val chat = FakeChatRepository(engine { emit("  ") }, backgroundScope)
        chat.send("hi")
        settle()
        assertEquals(busy, chat.error.value)
        assertEquals(listOf("hi"), chat.texts())
    }

    @Test
    fun sendWhileReplyingIsIgnored() = runTest {
        val chat = FakeChatRepository(engine { delay(100); emit("ok") }, backgroundScope)
        chat.send("one")
        chat.send("two")
        settle()
        assertEquals(listOf("one", "ok"), chat.texts())
    }

    @Test
    fun replyIsStillSavedAfterTheViewModelIsCleared() = runTest {
        val chat = FakeChatRepository(engine { delay(500); emit("still here") }, backgroundScope)
        val vm = ChatViewModel(chat)
        vm.send("hi")
        runCurrent()
        vm.viewModelScope.cancel()
        settle()
        assertEquals(listOf("hi", "still here"), chat.texts())
    }

    /** "old" hangs and then lingers in cleanup after cancellation; anything else streams "new" and hangs. */
    private fun TestScope.accountChat() = AccountChat(
        ReplyEngine { history, _ ->
            if (history.last().text == "old") flow {
                try { awaitCancellation() } finally { withContext(NonCancellable) { delay(500) } }
            } else flow { emit("new"); awaitCancellation() }
        },
        backgroundScope
    )

    @Test
    fun switchingAccountDropsTheInFlightReplyAndItsState() = runTest {
        val chat = accountChat()
        chat.send("old")
        runCurrent()
        chat.switchTo("b")
        settle()
        assertNull(chat.draft.value)
        assertNull(chat.error.value)
        assertEquals(listOf("old"), chat.textsOf("a"))
        assertEquals(emptyList<String>(), chat.textsOf("b"))
    }

    @Test
    fun staleJobCannotClearTheNewRepliesDraft() = runTest {
        val chat = accountChat()
        chat.send("old")
        runCurrent()
        chat.switchTo("b")
        chat.send("fresh") // the old job is still winding down in its cleanup
        runCurrent()
        assertEquals("new", chat.draft.value)
        settle()
        assertEquals("new", chat.draft.value)
    }
}
