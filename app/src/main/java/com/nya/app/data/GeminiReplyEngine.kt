package com.nya.app.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ServerException
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val MaxMessages = 30

/**
 * How long one model may take to send its first text before the next model is tried. Short enough that every
 * fallback fits inside [ReplyTimeout]: a model that is out of quota can hang without answering instead of failing.
 */
val ModelFirstTextTimeout = 7.seconds

/** A model sent no text within [ModelFirstTextTimeout]. */
class ModelStalledException(model: String, timeout: Duration) : Exception("$model sent no text within $timeout")

/** Reads models, prompt and companion traits from the config on every request, so remote changes apply to the next message. */
class GeminiReplyEngine(private val appConfig: AppConfigRepository) : ReplyEngine {
    override fun reply(history: List<ChatMessage>, persona: ChatPersona): Flow<String> {
        val config = appConfig.config.value
        val models = config.ai.models
        val system = content { text(buildSystemPrompt(config, persona)) }
        val turns = history.takeLast(MaxMessages).dropWhile { !it.mine }
            .map { content(if (it.mine) "user" else "model") { text(it.text) } }
        fun stream(name: String) = Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(name, systemInstruction = system)
            .generateContentStream(turns)
            .map { it.text.orEmpty() }
            .filter { it.isNotEmpty() }
            .failIfSilent(ModelFirstTextTimeout) { ModelStalledException(name, it) }
        val withFallbacks = models.zipWithNext().fold(stream(models.first())) { flow, (failed, next) ->
            flow.catch {
                if (it !is QuotaExceededException && it !is ServerException && it !is ModelStalledException) throw it
                Log.w("NyaAI", "$failed unavailable (${it::class.java.simpleName}), falling back to $next")
                emitAll(stream(next))
            }
        }
        return withFallbacks.catch {
            if (it !is CancellationException) Log.e("NyaAI", "Gemini call failed: ${it::class.java.name}: ${it.message}", it)
            throw it
        }
    }
}

/** Passes values through, but fails with [error] if the first one has not arrived within [timeout]. */
internal fun <T> Flow<T>.failIfSilent(timeout: Duration, error: (Duration) -> Throwable): Flow<T> = flow {
    coroutineScope {
        val watchdog = launch {
            delay(timeout)
            throw error(timeout)
        }
        collect {
            watchdog.cancel()
            emit(it)
        }
        watchdog.cancel()
    }
}

/** The configured prompt followed by the user's persona: companion personality, name and current mode. */
internal fun buildSystemPrompt(config: AppConfig, persona: ChatPersona): String {
    val trait = config.companions.getValue(persona.companion).trait
    val user = if (persona.userName.isBlank()) "" else " The user's name is ${persona.userName}."
    return "${config.ai.systemPrompt} Your personality is ${persona.companion.name}: $trait.$user " +
        "The current mode is ${ModeLabels.getOrElse(persona.mode) { ModeLabels[0] }}."
}
