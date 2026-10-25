package com.nya.app.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ServerException
import com.google.firebase.ai.type.content
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

private const val MaxMessages = 30

/** Reads models, prompt and companion traits from the config on every request, so remote changes apply to the next message. */
class GeminiReplyEngine(private val appConfig: AppConfigRepository) : ReplyEngine {
    override fun reply(history: List<ChatMessage>, persona: ChatPersona): Flow<String> {
        // TODO: add Firebase App Check (Play Integrity + debug provider) and enforce it for AI Logic before release.
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
        val withFallbacks = models.zipWithNext().fold(stream(models.first())) { flow, (failed, next) ->
            flow.catch {
                if (it !is QuotaExceededException && it !is ServerException) throw it
                Log.w("NyaAI", "$failed unavailable (${it::class.java.simpleName}), falling back to $next")
                emitAll(stream(next))
            }
        }
        return withFallbacks.catch {
            Log.e("NyaAI", "Gemini call failed: ${it::class.java.name}: ${it.message}", it)
            throw it
        }
    }
}

/** The configured prompt followed by the user's persona: companion personality, name and current mode. */
internal fun buildSystemPrompt(config: AppConfig, persona: ChatPersona): String {
    val trait = config.companions.getValue(persona.companion).trait
    val user = if (persona.userName.isBlank()) "" else " The user's name is ${persona.userName}."
    return "${config.ai.systemPrompt} Your personality is ${persona.companion.name}: $trait.$user " +
        "The current mode is ${ModeLabels.getOrElse(persona.mode) { ModeLabels[0] }}."
}
