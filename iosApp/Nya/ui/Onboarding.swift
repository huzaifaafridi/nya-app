import SwiftUI

struct Para: View {
    let text: String
    @Environment(\.nya) private var c

    init(_ text: String) {
        self.text = text
    }

    var body: some View {
        Text(text).nyaFont(15).lineSpacing(4).foregroundStyle(c.sb)
    }
}

// back arrow for onboarding steps
private struct StepBack: View {
    let onBack: () -> Void

    var body: some View {
        IconBtn(d: NyaIcons.Back, contentDescription: "Back", onClick: onBack)
    }
}

struct VerifyScreen: View {
    let onVerified: () -> Void
    @Environment(AuthViewModel.self) private var auth

    var body: some View {
        Page {
            Spacer().frame(height: 40)
            Heading("Check your email")
            Spacer().frame(height: 12)
            Para("We sent a verification link to your email. Open it, then come back and tap Continue.")
            Spacer().frame(height: 32)
            NyaButton("Continue", loading: auth.busy) { auth.checkVerified(onDone: onVerified) }
            AuthMessage(error: auth.error, info: auth.info)
            TextLink("Didn't get it? Resend email") { auth.resend() }
        }
        .onAppear { auth.clearMessages() }
    }
}

struct ResetScreen: View {
    let onBack: () -> Void
    @Environment(AuthViewModel.self) private var auth
    @State private var email = ""

    var body: some View {
        Page {
            Spacer().frame(height: 40)
            Heading("Reset password")
            Spacer().frame(height: 12)
            Para("Enter your email and we'll send you a link to set a new password.")
            Spacer().frame(height: 28)
            NyaField("Email", value: $email, keyboardType: .emailAddress)
            Spacer().frame(height: 24)
            NyaButton("Send reset link", loading: auth.busy) { auth.sendReset(email: email) }
            AuthMessage(error: auth.error, info: auth.info)
            Spacer().frame(height: 16)
            NyaOutlineButton("Back to sign in", onClick: onBack)
        }
        .onAppear { auth.clearMessages() }
        .onChange(of: email) { auth.clearError() }
    }
}

struct CompanionScreen: View {
    let onContinue: () -> Void
    let onBack: () -> Void
    @Environment(\.nya) private var c
    @Environment(\.prefs) private var prefs
    @Environment(AppViewModel.self) private var app
    @State private var sel: Int?
    private let items = [
        ("Wanderer", "Curious about places and discoveries"),
        ("Muse", "Style, taste and inspiration"),
        ("Guide", "Calm, clear answers"),
        ("Yogi", "Mindful and steady"),
        ("Creator", "Ideas and making things")
    ]

    var body: some View {
        let picked = sel ?? Companion.allCases.firstIndex(of: prefs.companion) ?? 0
        let shape = RoundedRectangle(cornerRadius: 20)
        Page {
            StepBack(onBack: onBack)
            Spacer().frame(height: 20)
            Heading("Choose your companion")
            Spacer().frame(height: 12)
            Para("Each one thinks and talks a little differently. You can change this later in Settings.")
            Spacer().frame(height: 24)
            ForEach(Array(items.enumerated()), id: \.offset) { i, item in
                HStack(spacing: 14) {
                    ZStack {
                        GradientDot(size: 48, colors: [c.b1, c.b2])
                        Avatar2D(companion: Companion.allCases[i], skinTone: prefs.skin, gender: prefs.gender)
                    }
                    .frame(width: 48, height: 48)
                    .clipShape(Circle())
                    VStack(alignment: .leading, spacing: 0) {
                        Text(item.0).nyaFont(17, weight: .semibold).foregroundStyle(c.tx)
                        Text(item.1).nyaFont(13).foregroundStyle(c.sb)
                    }
                    Spacer(minLength: 0)
                }
                .padding(16)
                .background(c.sf, in: shape)
                .overlay(shape.strokeBorder(i == picked ? c.ac : c.ln, lineWidth: i == picked ? 2 : 1))
                .pressScale(selected: i == picked) { sel = i }
                .padding(.bottom, 12)
            }
            Spacer().frame(height: 12)
            NyaButton("Continue") {
                app.setCompanion(Companion.allCases[picked])
                onContinue()
            }
        }
    }
}

private let Skins: [UInt32] = [0xFFF3D5BC, 0xFFE0AC84, 0xFFC68A5E, 0xFF8D5A3B, 0xFF5A3A27]

@MainActor
private func previewVoice(_ prefs: Prefs) {
    guard let voice = VoiceEngine.shared.chosen(prefs.gender, prefs.voice) else { return }
    VoiceEngine.shared.speak("Hello, \(prefs.name)! I'm \(prefs.companion.rawValue). Nice to meet you.", voice)
}

struct AvatarScreen: View {
    let onContinue: () -> Void
    let onBack: () -> Void
    @Environment(\.nya) private var c
    @Environment(\.prefs) private var prefs
    @Environment(AppViewModel.self) private var app
    @State private var skin: Int?

    var body: some View {
        let picked = skin ?? Skins.firstIndex(of: prefs.skin) ?? 1
        VStack(spacing: 0) {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    StepBack(onBack: onBack)
                    Spacer().frame(height: 20)
                    Heading("Make it yours")
                    Spacer().frame(height: 20)
                    AvatarPreview(skin: Skins[picked])
                    Spacer().frame(height: 24)
                    Text("Gender").nyaFont(12).foregroundStyle(c.sb)
                    Spacer().frame(height: 10)
                    GenderPicker()
                    Spacer().frame(height: 20)
                    Text("Skin tone").nyaFont(12).foregroundStyle(c.sb)
                    Spacer().frame(height: 10)
                    HStack(spacing: 14) {
                        ForEach(Array(Skins.enumerated()), id: \.offset) { i, col in
                            Circle()
                                .fill(Color(argb: col))
                                .overlay(Circle().strokeBorder(i == picked ? c.ac : c.ln, lineWidth: i == picked ? 3 : 1))
                                .frame(width: 44, height: 44)
                                .pressScale(selected: i == picked, description: "Skin tone \(i + 1)") { skin = i }
                        }
                    }
                    Spacer().frame(height: 20)
                    VoicePicker()
                }
                .padding(24)
            }
            .scrollBounceBehavior(.basedOnSize)
            NyaButton("Continue") {
                app.setSkin(Skins[picked])
                onContinue()
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 16)
        }
        .nyaBg()
        .onAppear { VoiceEngine.shared.start() }
        .onDisappear { VoiceEngine.shared.stop() }
    }
}

private struct AvatarPreview: View {
    let skin: UInt32
    @Environment(\.nya) private var c
    @Environment(\.prefs) private var prefs
    @Environment(\.accessibilityReduceMotion) private var reduce

    var body: some View {
        let voice = VoiceEngine.shared
        let shape = RoundedRectangle(cornerRadius: 28)
        let key = "\(prefs.gender.rawValue)\(skin)"
        ZStack {
            NyaOrb(size: 130) {
                Avatar2D(
                    companion: prefs.companion, skinTone: skin, gender: prefs.gender,
                    state: voice.speaking ? .speaking : .idle, mouth: voice.mouth
                )
                .id(key)
                .transition(.opacity)
            }
        }
        .animation(reduce ? nil : motionTween(), value: key)
        .frame(maxWidth: .infinity, minHeight: 220, maxHeight: 220)
        .background(LinearGradient(colors: [c.sf, c.bg], startPoint: .top, endPoint: .bottom), in: shape)
        .overlay(shape.strokeBorder(c.ln, lineWidth: 1))
    }
}

private struct GenderPicker: View {
    @Environment(\.prefs) private var prefs
    @Environment(AppViewModel.self) private var app

    var body: some View {
        HStack(spacing: 2) {
            ForEach(Gender.allCases, id: \.self) { g in
                NyaChip(g.rawValue, on: prefs.gender == g) {
                    app.setGender(g)
                    var next = prefs
                    next.gender = g
                    previewVoice(next)
                }
            }
        }
    }
}

private struct VoicePicker: View {
    @Environment(\.nya) private var c
    @Environment(\.prefs) private var prefs
    @Environment(AppViewModel.self) private var app

    var body: some View {
        let voice = VoiceEngine.shared
        HStack(spacing: 0) {
            Text("Voice style").nyaFont(12).foregroundStyle(c.sb).frame(maxWidth: .infinity, alignment: .leading)
            if voice.available {
                IconBtn(
                    d: voice.speaking ? NyaIcons.Stop : NyaIcons.Play,
                    contentDescription: voice.speaking ? "Stop" : "Play voice preview", size: 36
                ) {
                    if voice.speaking { voice.stop() } else { previewVoice(prefs) }
                }
            }
        }
        Spacer().frame(height: 10)
        if voice.ready && !voice.available {
            Text("Voices aren't available on this device.").nyaFont(13).foregroundStyle(c.sb)
        }
        HStack(spacing: 2) {
            let chosen = voice.chosen(prefs.gender, prefs.voice)
            ForEach(Array(voice.options(prefs.gender).enumerated()), id: \.offset) { i, v in
                NyaChip(v.name, on: v == chosen, hPad: 6) {
                    app.setVoice(i)
                    var next = prefs
                    next.voice = i
                    previewVoice(next)
                }
            }
        }
    }
}

struct PermissionsScreen: View {
    let onContinue: () -> Void
    let onBack: () -> Void
    @Environment(\.nya) private var c
    private let items = [
        ("Bluetooth", "Connect to your Nya headband"),
        ("Notifications", "Know when clips finish syncing"),
        ("Photos", "Save memories to your phone")
    ]

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: 20)
        Page {
            StepBack(onBack: onBack)
            Spacer().frame(height: 20)
            Heading("A few permissions")
            Spacer().frame(height: 12)
            Para("Nya needs these to work. You can change them any time in Settings.")
            Spacer().frame(height: 24)
            ForEach(items, id: \.0) { t, d in
                VStack(alignment: .leading, spacing: 0) {
                    Text(t).nyaFont(17, weight: .semibold).foregroundStyle(c.tx)
                    Spacer().frame(height: 4)
                    Text(d).nyaFont(14).foregroundStyle(c.sb)
                }
                .padding(18)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(c.sf, in: shape)
                .overlay(shape.strokeBorder(c.ln, lineWidth: 1))
                .padding(.bottom, 12)
            }
            Spacer().frame(height: 24)
            NyaButton("Allow and continue", onClick: onContinue)
        }
    }
}

struct PulseRing: View {
    let offset: Double
    @Environment(\.nya) private var c
    @State private var began = Date()

    var body: some View {
        TimelineView(.animation) { timeline in
            let run = timeline.date.timeIntervalSince(began) - offset
            let p = run < 0 ? 0 : run.truncatingRemainder(dividingBy: 2.4) / 2.4
            Circle()
                .strokeBorder(c.ac, lineWidth: 2)
                .frame(width: 120, height: 120)
                .scaleEffect(1 + p * 1.2)
                .opacity(1 - p)
        }
    }
}

struct PairScreen: View {
    let onConnect: () -> Void
    let onBack: () -> Void
    @Environment(\.nya) private var c
    @Environment(DeviceViewModel.self) private var vm

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: 20)
        // top, radar and bottom spread over the screen when they fit and scroll when they don't
        GeometryReader { g in
            ScrollView {
                VStack(spacing: 0) {
                    VStack(spacing: 0) {
                        HStack {
                            StepBack(onBack: onBack)
                            Spacer()
                        }
                        Text("Required").nyaFont(12).foregroundStyle(c.ac)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 5)
                            .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(c.ac, lineWidth: 1))
                    }
                    Spacer(minLength: 0)
                    ZStack {
                        PulseRing(offset: 0)
                        PulseRing(offset: 1.2)
                        GradientDot(size: 110, colors: [c.b1, c.b2, c.sf])
                    }
                    .frame(width: 320, height: 320)
                    Spacer(minLength: 0)
                    VStack(spacing: 0) {
                        Heading("Find your Nya")
                        Spacer().frame(height: 8)
                        Text("Turn on your headband and keep it close.").nyaFont(15).foregroundStyle(c.sb).multilineTextAlignment(.center)
                        Spacer().frame(height: 24)
                        HStack(spacing: 14) {
                            GradientDot(size: 40, colors: [c.b1, c.b2])
                            VStack(alignment: .leading, spacing: 0) {
                                Text("NYA F1").nyaFont(16, weight: .semibold).foregroundStyle(c.tx)
                                Text(vm.connecting || vm.linking ? "Connecting…" : "Ready to pair").nyaFont(13).foregroundStyle(c.sb)
                            }
                            Spacer(minLength: 0)
                        }
                        .padding(16)
                        .background(c.sf, in: shape)
                        .overlay(shape.strokeBorder(c.ln, lineWidth: 1))
                        Spacer().frame(height: 16)
                        NyaButton("Connect", loading: vm.connecting) {
                            vm.connect(onFail: { SubNav.open("PairFail") }, onSuccess: onConnect)
                        }
                    }
                }
                .padding(24)
                .frame(minHeight: g.size.height)
            }
            .scrollBounceBehavior(.basedOnSize)
        }
        .nyaBg()
    }
}
