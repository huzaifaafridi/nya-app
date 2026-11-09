package com.nya.app.data

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class FailIfSilentTest {
    @Test
    fun silentFlowFailsAtTheTimeout() = runTest {
        val result = runCatching {
            flow<String> { awaitCancellation() }
                .failIfSilent(ModelFirstTextTimeout) { ModelStalledException("m", it) }
                .toList()
        }
        assertTrue(result.exceptionOrNull() is ModelStalledException)
        assertEquals(ModelFirstTextTimeout.inWholeMilliseconds, currentTime)
    }

    @Test
    fun onceTextArrivesSlowerChunksAreNotCutOff() = runTest {
        val out = flow {
            delay(ModelFirstTextTimeout - 1.seconds); emit("a")
            delay(ModelFirstTextTimeout * 2); emit("b")
        }.failIfSilent(ModelFirstTextTimeout) { ModelStalledException("m", it) }.toList()
        assertEquals(listOf("a", "b"), out)
    }

    @Test
    fun upstreamErrorsPassThroughUnchanged() = runTest {
        val result = runCatching {
            flow<String> { emit("a"); error("boom") }
                .failIfSilent(ModelFirstTextTimeout) { ModelStalledException("m", it) }
                .toList()
        }
        assertEquals("boom", result.exceptionOrNull()?.message)
    }

    @Test
    fun everyFallbackFitsInsideTheReplyTimeout() {
        // four configured models by default; all may stall before one answers
        assertTrue(ModelFirstTextTimeout * AppConfig().ai.models.size < ReplyTimeout)
    }
}
