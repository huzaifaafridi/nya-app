import SwiftUI

enum Screen: Int {
    case welcome, signIn, signUp, verify, reset, companion, avatar, permissions, pair, greeting, home, sub
}

// what's on screen; sub screens are keyed by name and stack depth so push/pop animate too
struct Dest: Hashable {
    let screen: Screen
    let sub: String
    let depth: Int

    // forward = later in the flow, or one level deeper into sub screens
    func isAfter(_ o: Dest) -> Bool {
        screen == .sub && o.screen == .sub ? depth >= o.depth : screen.rawValue > o.screen.rawValue
    }
}

@main
struct NyaApp: App {
    @State private var container = AppContainer()

    // SwiftUI.Scene, because data/Models.swift has its own Scene enum
    var body: some SwiftUI.Scene {
        WindowGroup {
            MainActivity(container: container)
        }
    }
}

struct MainActivity: View {
    @State private var app: AppViewModel
    @State private var auth: AuthViewModel
    @State private var device: DeviceViewModel
    @State private var s: Screen
    @State private var sub = ""
    @State private var subStack: [String] = []
    @State private var returnTo = Screen.home
    @State private var forward = true
    @Environment(\.accessibilityReduceMotion) private var reduce

    init(container: AppContainer) {
        let app = AppViewModel(container)
        _app = State(initialValue: app)
        _auth = State(initialValue: AuthViewModel(container))
        _device = State(initialValue: DeviceViewModel(container))
        _s = State(initialValue: app.prefs.onboarded && app.isSignedIn ? .home : .welcome)
    }

    private func dest(_ screen: Screen, _ sub: String, _ depth: Int) -> Dest {
        Dest(screen: screen, sub: screen == .sub ? sub : "", depth: screen == .sub ? depth : 0)
    }

    private func go(_ next: Screen) {
        let target = dest(next, sub, subStack.count)
        forward = target.isAfter(dest(s, sub, subStack.count))
        withAnimation(reduce ? nil : motionTween(Motion.LONG)) { s = next }
    }

    private func openSub(_ name: String) {
        forward = true
        withAnimation(reduce ? nil : motionTween(Motion.LONG)) {
            if s == .sub { subStack.append(sub) } else { returnTo = s }
            sub = name
            s = .sub
        }
    }

    private func closeSub() {
        forward = false
        withAnimation(reduce ? nil : motionTween(Motion.LONG)) {
            if let last = subStack.popLast() { sub = last } else { s = returnTo }
        }
    }

    var body: some View {
        let prefs = app.prefs
        NyaTheme(dark: prefs.theme == 1 ? false : prefs.theme == 2 ? true : nil) {
            ZStack {
                screen(prefs)
                    .id(dest(s, sub, subStack.count))
                    .transition(screenTransition(forward: forward, reduce: reduce))
            }
        }
        .environment(\.prefs, prefs)
        .environment(app)
        .environment(auth)
        .environment(device)
        .onAppear { SubNav.open = openSub }
    }

    @ViewBuilder
    private func screen(_ prefs: Prefs) -> some View {
        switch s {
        case .welcome:
            WelcomeScreen(onStart: { go(.signUp) }, onSignIn: { go(.signIn) })
        case .signIn:
            SignInScreen(
                onSignUp: { go(.signUp) }, onForgot: { go(.reset) }, onHome: { go(.greeting) },
                onVerify: { go(.verify) }, onGoogle: { go($0.isNew ? .companion : .greeting) }
            )
        case .signUp:
            SignUpScreen(
                onSignIn: { go(.signIn) }, onVerify: { go(.verify) },
                onGoogle: { go($0.isNew ? .companion : .greeting) }
            )
        case .verify:
            VerifyScreen { go(.companion) }
        case .reset:
            ResetScreen { go(.signIn) }
        case .companion:
            CompanionScreen(onContinue: { go(.avatar) }, onBack: { go(.verify) })
        case .avatar:
            AvatarScreen(onContinue: { go(.permissions) }, onBack: { go(.companion) })
        case .permissions:
            PermissionsScreen(onContinue: { go(.pair) }, onBack: { go(.avatar) })
        case .pair:
            PairScreen(onConnect: { go(.greeting) }, onBack: { go(.permissions) })
        case .greeting:
            GreetingScreen(name: prefs.name) {
                app.completeOnboarding()
                go(.home)
            }
        case .home:
            MainTabs {
                app.signOut()
                subStack = []
                go(.welcome)
            }
        case .sub:
            SubRouter(name: sub, onBack: closeSub)
        }
    }
}
