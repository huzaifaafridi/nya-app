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
import com.nya.app.NyaApp
import com.nya.app.data.Gender
import com.nya.app.data.VoiceStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.concurrent.thread

class VoiceOption(val name: String, val voice: Voice?, val pitch: Float)

/** the app's single TextToSpeech, created on first use from the application context */
object VoiceEngine {
    private var tts: TextToSpeech? = null
    private val main = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main)

    var ready by mutableStateOf(false)
        private set
    var available by mutableStateOf(false)
        private set
    private var voices by mutableStateOf(emptyMap<Gender, Voice?>()) // the TTS voice per gender; empty = unavailable
    private var styles by mutableStateOf(emptyList<VoiceStyle>())
    private var rate = 1f
    private var configJob: Job? = null
    var speaking by mutableStateOf(false)
        private set

    private val mouthState = mutableStateOf(false)

    /** true while a word is being spoken, closed between words; read it in the draw phase only */
    val mouth: State<Boolean> = mouthState

    /** the configured styles for a gender */
    fun options(gender: Gender): List<VoiceOption> {
        if (gender !in voices) return emptyList()
        val voice = voices[gender]
        return styles.map { VoiceOption(it.name, voice, if (gender == Gender.Female) it.femalePitch else it.malePitch) }
    }

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
        configJob = scope.launch {
            (context.applicationContext as NyaApp).container.appConfig.config.collect { styles = it.voiceStyles; rate = it.voiceRate }
        }
        tts = TextToSpeech(context.applicationContext) { status ->
            val t = tts
            if (status == TextToSpeech.SUCCESS && t != null) {
                t.setOnUtteranceProgressListener(listener)
                thread {
                    val found = voicesOf(t)
                    main.post { voices = found; available = found.isNotEmpty(); ready = true }
                }
            } else ready = true
        }
    }

    fun speak(text: String, voice: VoiceOption) {
        val t = tts ?: return
        if (voice.voice != null) t.setVoice(voice.voice) else t.setLanguage(Locale.US)
        t.setPitch(voice.pitch)
        t.setSpeechRate(rate)
        t.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nya")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        configJob?.cancel()
        main.removeCallbacksAndMessages(null)
        ready = false
        available = false
        voices = emptyMap()
        speaking = false
        mouthState.value = false
    }

    // per gender: an offline English voice whose name or features mention that gender, else one shared
    // US English voice (or the engine default); the gender and style difference then comes from pitch
    private fun voicesOf(t: TextToSpeech): Map<Gender, Voice?> {
        val english = t.voices.orEmpty()
            .filter {
                it.locale.language == "en" && !it.isNetworkConnectionRequired &&
                    TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features
            }
            .sortedBy { it.name }
        if (english.isEmpty() && t.isLanguageAvailable(Locale.US) < TextToSpeech.LANG_AVAILABLE) return emptyMap()
        val shared = english.firstOrNull { it.locale == Locale.US } ?: english.firstOrNull()
        return Gender.entries.associateWith { g -> english.firstOrNull { mentions(it, g) } ?: shared }
    }

    private fun mentions(v: Voice, g: Gender): Boolean {
        val text = (v.name + v.features.joinToString()).lowercase()
        return if (g == Gender.Female) "female" in text else "male" in text.replace("female", "")
    }
}
