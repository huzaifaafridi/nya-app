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

// Tried in order; a quota or server error (e.g. high demand) on one model moves on to the next.
private const val MODEL_PRIMARY = "gemini-3.8-flash"
private const val MODEL_FALLBACK_1 = "gemini-3.7-flash"
private const val MODEL_FALLBACK_2 = "gemini-3.5-flash-lite"
private const val MODEL_FALLBACK_3 = "gemini-3.1-flash-lite"
private val MODELS = listOf(MODEL_PRIMARY, MODEL_FALLBACK_1, MODEL_FALLBACK_2, MODEL_FALLBACK_3)
private const val MaxMessages = 30

class GeminiReplyEngine : ReplyEngine {
    override fun reply(history: List<ChatMessage>, persona: ChatPersona): Flow<String> {
        // TODO: add Firebase App Check (Play Integrity + debug provider) and enforce it for AI Logic before release.
        val system = content { text(systemPrompt(persona)) }
        val turns = history.takeLast(MaxMessages).dropWhile { !it.mine }
            .map { content(if (it.mine) "user" else "model") { text(it.text) } }
        fun stream(name: String) = Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(name, systemInstruction = system)
            .generateContentStream(turns)
            .map { it.text.orEmpty() }
            .filter { it.isNotEmpty() }
        val withFallbacks = MODELS.zipWithNext().fold(stream(MODELS.first())) { flow, (failed, next) ->
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

    private fun systemPrompt(persona: ChatPersona): String {
        val trait = when (persona.companion) {
            Companion.Wanderer -> "curious about places and discoveries"
            Companion.Muse -> "attuned to style, taste and inspiration"
            Companion.Guide -> "calm, giving clear answers"
            Companion.Yogi -> "mindful and steady"
            Companion.Creator -> "full of ideas and eager to make things"
        }
        val user = if (persona.userName.isBlank()) "" else " The user's name is ${persona.userName}."
        return "You are Nya, a warm AI companion inside a wearable memory headband app. " +
            "Your personality is ${persona.companion.name}: $trait.$user " +
            "The current mode is ${ModeLabels.getOrElse(persona.mode) { ModeLabels[0] }}. " +
            "Reply in 1-3 short, conversational sentences of plain text with no markdown, lists or emojis, " +
            "because replies may be spoken aloud. Never claim to have done actions you cannot do."
    }
}
