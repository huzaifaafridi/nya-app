package com.nya.app.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nya.app.data.Gender
import java.util.Locale
import kotlin.concurrent.thread

class VoiceOption(val name: String, val voice: Voice?, val pitch: Float)

private val Styles = listOf("Calm", "Warm", "Bright", "Deep")

// pitch for each style on top of the gender base, so all four sound clearly different
private val Pitches = mapOf(
    Gender.Female to floatArrayOf(1.1f, 1.16f, 1.26f, 1.02f),
    Gender.Male to floatArrayOf(.82f, .87f, .94f, .72f)
)

// fixed, comfortable pace for every style
private const val RATE = .85f

/** the app's single TextToSpeech, created on first use from the application context */
object VoiceEngine {
    private var tts: TextToSpeech? = null
    private val main = Handler(Looper.getMainLooper())

    var ready by mutableStateOf(false)
        private set
    var available by mutableStateOf(false)
        private set
    private var byGender by mutableStateOf(emptyMap<Gender, List<VoiceOption>>())
    var speaking by mutableStateOf(false)
        private set

    private val mouthState = mutableStateOf(false)

    /** true while a word is being spoken, closed between words; read it in the draw phase only */
    val mouth: State<Boolean> = mouthState

    /** the 4 styles for a gender */
    fun options(gender: Gender): List<VoiceOption> = byGender[gender].orEmpty()

    fun chosen(gender: Gender, voice: Int): VoiceOption? =
        options(gender).let { it.getOrNull(voice) ?: it.firstOrNull() }

    @Volatile private var openMs = 110L

    // prebuilt so word callbacks don't allocate
    private val close = Runnable { mouthState.value = false }
    private val open = Runnable {
        mouthState.value = true
        main.removeCallbacks(close)
        main.postDelayed(close, openMs)
    }
    private val started = Runnable { speaking = true }
    private val ended = Runnable {
        speaking = false
        main.removeCallbacks(close)
        mouthState.value = false
    }

    private val listener = object : UtteranceProgressListener() {
        override fun onStart(id: String?) { main.post(started) }
        override fun onDone(id: String?) { main.post(ended) }
        @Deprecated("Deprecated in Java")
        override fun onError(id: String?) { main.post(ended) }
        override fun onStop(id: String?, interrupted: Boolean) { main.post(ended) }
        override fun onRangeStart(id: String?, start: Int, end: Int, frame: Int) {
            openMs = ((end - start) * 40L).coerceIn(80L, 260L) // longer words keep the mouth open longer
            main.post(open)
        }
    }

    fun init(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            val t = tts
            if (status == TextToSpeech.SUCCESS && t != null) {
                t.setOnUtteranceProgressListener(listener)
                thread {
                    val styles = stylesOf(t)
                    main.post { byGender = styles; available = styles.isNotEmpty(); ready = true }
                }
            } else ready = true
        }
    }

    fun speak(text: String, voice: VoiceOption) {
        val t = tts ?: return
        if (voice.voice != null) t.setVoice(voice.voice) else t.setLanguage(Locale.US)
        t.setPitch(voice.pitch)
        t.setSpeechRate(RATE)
        t.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nya")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        main.removeCallbacksAndMessages(null)
        ready = false
        available = false
        byGender = emptyMap()
        speaking = false
        mouthState.value = false
    }

    // per gender: an offline English voice whose name or features mention that gender, else one shared
    // US English voice (or the engine default); the gender and style difference then comes from pitch
    private fun stylesOf(t: TextToSpeech): Map<Gender, List<VoiceOption>> {
        val english = t.voices.orEmpty()
            .filter {
                it.locale.language == "en" && !it.isNetworkConnectionRequired &&
                    TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features
            }
            .sortedBy { it.name }
        if (english.isEmpty() && t.isLanguageAvailable(Locale.US) < TextToSpeech.LANG_AVAILABLE) return emptyMap()
        val shared = english.firstOrNull { it.locale == Locale.US } ?: english.firstOrNull()
        return Gender.entries.associateWith { g ->
            val voice = english.firstOrNull { mentions(it, g) } ?: shared
            Styles.mapIndexed { i, name -> VoiceOption(name, voice, Pitches.getValue(g)[i]) }
        }
    }

    private fun mentions(v: Voice, g: Gender): Boolean {
        val text = (v.name + v.features.joinToString()).lowercase()
        return if (g == Gender.Female) "female" in text else "male" in text.replace("female", "")
    }
}
