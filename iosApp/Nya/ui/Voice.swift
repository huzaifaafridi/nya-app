import AVFoundation
import Observation

struct VoiceOption: Equatable {
    let name: String
    let voice: String? // AVSpeechSynthesisVoice identifier
    let pitch: Float
}

private let Styles = ["Calm", "Warm", "Bright", "Deep"]

// pitch for each style on top of the gender base, so all four sound clearly different
private let Pitches: [Gender: [Float]] = [
    .female: [1.1, 1.16, 1.26, 1.02],
    .male: [0.82, 0.87, 0.94, 0.72]
]

// fixed, comfortable pace for every style
private let RATE: Float = 0.85

/// the app's single speech synthesizer
@MainActor @Observable
final class VoiceEngine: NSObject, AVSpeechSynthesizerDelegate {
    static let shared = VoiceEngine()

    private(set) var ready = false
    private(set) var available = false
    private var byGender: [Gender: [VoiceOption]] = [:]
    private(set) var speaking = false

    /// true while a word is being spoken, closed between words
    private(set) var mouth = false

    @ObservationIgnored private let synth = AVSpeechSynthesizer()
    @ObservationIgnored private var closing: Task<Void, Never>?

    /// the 4 styles for a gender
    func options(_ gender: Gender) -> [VoiceOption] { byGender[gender] ?? [] }

    func chosen(_ gender: Gender, _ voice: Int) -> VoiceOption? {
        let all = options(gender)
        return all.indices.contains(voice) ? all[voice] : all.first
    }

    func start() {
        if ready { return }
        synth.delegate = self
        // otherwise the ring/silent switch mutes the preview
        try? AVAudioSession.sharedInstance().setCategory(.playback, mode: .spokenAudio)
        byGender = stylesOf()
        available = !byGender.isEmpty
        ready = true
    }

    func speak(_ text: String, _ voice: VoiceOption) {
        synth.stopSpeaking(at: .immediate)
        let u = AVSpeechUtterance(string: text)
        if let id = voice.voice { u.voice = AVSpeechSynthesisVoice(identifier: id) }
        u.pitchMultiplier = voice.pitch
        u.rate = AVSpeechUtteranceDefaultSpeechRate * RATE
        synth.speak(u)
    }

    func stop() {
        synth.stopSpeaking(at: .immediate)
    }

    // per gender: an English voice of that gender, else one shared US English voice;
    // the gender and style difference then comes from pitch
    private func stylesOf() -> [Gender: [VoiceOption]] {
        let english = AVSpeechSynthesisVoice.speechVoices()
            .filter { $0.language.hasPrefix("en") }
            .sorted { $0.identifier < $1.identifier }
        guard let shared = english.first(where: { $0.language == "en-US" }) ?? english.first else { return [:] }
        var out: [Gender: [VoiceOption]] = [:]
        for g in Gender.allCases {
            let wanted: AVSpeechSynthesisVoiceGender = g == .female ? .female : .male
            let voice = english.first { $0.gender == wanted } ?? shared
            out[g] = Styles.enumerated().map { i, name in
                VoiceOption(name: name, voice: voice.identifier, pitch: Pitches[g]?[i] ?? 1)
            }
        }
        return out
    }

    private func started() {
        speaking = true
    }

    private func ended() {
        speaking = synth.isSpeaking
        if !speaking {
            closing?.cancel()
            mouth = false
        }
    }

    private func word(length: Int) {
        let openMs = min(max(length * 40, 80), 260) // longer words keep the mouth open longer
        mouth = true
        closing?.cancel()
        closing = Task {
            try? await Task.sleep(for: .milliseconds(openMs))
            if !Task.isCancelled { mouth = false }
        }
    }

    nonisolated func speechSynthesizer(_ s: AVSpeechSynthesizer, didStart u: AVSpeechUtterance) {
        Task { @MainActor in VoiceEngine.shared.started() }
    }

    nonisolated func speechSynthesizer(_ s: AVSpeechSynthesizer, didFinish u: AVSpeechUtterance) {
        Task { @MainActor in VoiceEngine.shared.ended() }
    }

    nonisolated func speechSynthesizer(_ s: AVSpeechSynthesizer, didCancel u: AVSpeechUtterance) {
        Task { @MainActor in VoiceEngine.shared.ended() }
    }

    nonisolated func speechSynthesizer(_ s: AVSpeechSynthesizer, willSpeakRangeOfSpeechString r: NSRange, utterance u: AVSpeechUtterance) {
        let length = r.length
        Task { @MainActor in VoiceEngine.shared.word(length: length) }
    }
}
