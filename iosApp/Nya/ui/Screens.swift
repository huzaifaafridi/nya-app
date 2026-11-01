import SwiftUI

struct Page<Content: View>: View {
    @ViewBuilder let content: Content

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) { content }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(24)
        }
        .scrollBounceBehavior(.basedOnSize)
        .scrollDismissesKeyboard(.interactively)
        .clipped() // stops content scrolling under the status bar, like statusBarsPadding
        .nyaBg()
    }
}

struct Heading: View {
    let text: String
    @Environment(\.nya) private var c

    init(_ text: String) {
        self.text = text
    }

    var body: some View {
        Text(text).nyaFont(38, serif: true).foregroundStyle(c.tx).asHeading()
    }
}

/// Filled circle with Compose's default radial gradient: edge to edge across the circle.
struct GradientDot: View {
    let size: CGFloat
    let colors: [Color]

    var body: some View {
        Circle()
            .fill(RadialGradient(colors: colors, center: .center, startRadius: 0, endRadius: size / 2))
            .frame(width: size, height: size)
    }
}

// ic_apple.xml, stretched to its square like the Android vector
private let AppleLogo = parsePath("M10.055,5.538C11.138,5.538 12.496,4.806 13.305,3.830C14.037,2.945 14.571,1.709 14.571,0.473C14.571,0.305 14.556,0.137 14.525,0.000C13.320,0.046 11.870,0.809 11.001,1.831C10.314,2.609 9.688,3.830 9.688,5.081C9.688,5.264 9.719,5.447 9.734,5.508C9.811,5.523 9.933,5.538 10.055,5.538ZM6.240,24.000C7.720,24.000 8.376,23.008 10.223,23.008C12.099,23.008 12.511,23.969 14.159,23.969C15.776,23.969 16.860,22.474 17.882,21.010C19.026,19.331 19.499,17.683 19.530,17.607C19.423,17.577 16.325,16.310 16.325,12.755C16.325,9.673 18.767,8.285 18.904,8.178C17.287,5.859 14.830,5.798 14.159,5.798C12.343,5.798 10.863,6.896 9.933,6.896C8.926,6.896 7.598,5.859 6.027,5.859C3.036,5.859 0.000,8.331 0.000,12.999C0.000,15.898 1.129,18.965 2.517,20.949C3.708,22.627 4.745,24.000 6.240,24.000Z")

struct SocialRow: View {
    let onGoogle: () -> Void
    @Environment(\.nya) private var c

    var body: some View {
        let tint = c.tx
        Text("or continue with").nyaFont(13).foregroundStyle(c.sb)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 18)
        HStack(spacing: 18) {
            Canvas { ctx, size in
                ctx.scaleBy(x: size.width / 19.53, y: size.height / 24)
                ctx.fill(AppleLogo, with: .color(tint))
            }
            .frame(width: 22, height: 22)
            .frame(width: 54, height: 54)
            .background(c.sf, in: Circle())
            .overlay(Circle().strokeBorder(c.ln, lineWidth: 1))
            .pressScale(description: "Continue with Apple") {}
            .accessibilityValue("Coming soon")
            Image(c == NyaDark ? "ic_google_dark" : "ic_google_light")
                .resizable()
                .frame(width: 54, height: 54)
                .clipShape(Circle())
                .pressScale(description: "Continue with Google", onClick: onGoogle)
            Image("ic_facebook")
                .resizable()
                .frame(width: 54, height: 54)
                .clipShape(Circle())
                .pressScale(description: "Continue with Facebook") {}
                .accessibilityValue("Coming soon")
        }
        .frame(maxWidth: .infinity)
    }
}

struct TextLink: View {
    let text: String
    let onClick: () -> Void
    @Environment(\.nya) private var c

    init(_ text: String, onClick: @escaping () -> Void) {
        self.text = text
        self.onClick = onClick
    }

    var body: some View {
        Text(text).nyaFont(14).foregroundStyle(c.ac).multilineTextAlignment(.center)
            .frame(maxWidth: .infinity, minHeight: 48)
            .pressScale(onClick: onClick)
            .padding(.top, 9)
    }
}

struct WelcomeScreen: View {
    let onStart: () -> Void
    let onSignIn: () -> Void
    @Environment(\.nya) private var c

    var body: some View {
        VStack(spacing: 0) {
            CenterScroll {
                GradientDot(size: 150, colors: [c.b1, c.b2, c.sf])
                Spacer().frame(height: 28)
                Text("Nya").nyaFont(64, serif: true).foregroundStyle(c.tx)
                Text("Fashionable AI").nyaFont(15).kerning(2).foregroundStyle(c.sb)
            }
            NyaButton("Get started", onClick: onStart)
            Spacer().frame(height: 12)
            NyaOutlineButton("Sign in", onClick: onSignIn)
        }
        .padding(24)
        .nyaBg()
    }
}

struct SignInScreen: View {
    let onSignUp: () -> Void
    let onForgot: () -> Void
    let onHome: () -> Void
    let onVerify: () -> Void
    let onGoogle: (AuthUser) -> Void
    @Environment(\.nya) private var c
    @Environment(AuthViewModel.self) private var auth
    @State private var email = ""
    @State private var pass = ""

    var body: some View {
        Page {
            Spacer().frame(height: 40)
            Heading("Welcome back")
            Spacer().frame(height: 28)
            NyaField("Email", value: $email, keyboardType: .emailAddress)
            Spacer().frame(height: 16)
            NyaField("Password", value: $pass, password: true)
            Text("Forgot password?").nyaFont(13).foregroundStyle(c.ac)
                .frame(minHeight: 48)
                .pressScale(onClick: onForgot)
            Spacer().frame(height: 10)
            NyaButton("Sign in", loading: auth.busy) {
                auth.signIn(email: email, pass: pass) { user in
                    if user.verified { onHome() } else { onVerify() }
                }
            }
            AuthMessage(error: auth.error, info: auth.info)
            SocialRow { auth.google(onDone: onGoogle) }
            TextLink("New here? Create account", onClick: onSignUp)
        }
        .onAppear { auth.clearMessages() }
        .onChange(of: email) { auth.clearError() }
        .onChange(of: pass) { auth.clearError() }
    }
}

private func dobText(_ d: String) -> String {
    d.count == 8 ? "\(d.prefix(2))/\(d.dropFirst(2).prefix(2))/\(d.dropFirst(4))" : d
}

struct SignUpScreen: View {
    let onSignIn: () -> Void
    let onVerify: () -> Void
    let onGoogle: (AuthUser) -> Void
    @Environment(\.nya) private var c
    @Environment(AuthViewModel.self) private var auth
    @State private var name = ""
    @State private var email = ""
    @State private var pass = ""
    @State private var confirm = ""
    @State private var dob = ""
    @State private var terms = false

    var body: some View {
        let dobShown = Binding(
            get: { dateShown(dob) },
            set: { dob = String($0.filter(\.isNumber).prefix(8)) }
        )
        Page {
            Spacer().frame(height: 24)
            Heading("Create account")
            Spacer().frame(height: 24)
            NyaField("Full name", value: $name, capitalize: true)
            Spacer().frame(height: 14)
            NyaField("Email", value: $email, keyboardType: .emailAddress)
            Spacer().frame(height: 14)
            NyaField("Password (min 8 characters)", value: $pass, password: true)
            Spacer().frame(height: 14)
            NyaField("Confirm password", value: $confirm, password: true)
            Spacer().frame(height: 14)
            NyaField("Date of birth (DD/MM/YYYY)", value: dobShown, keyboardType: .numberPad)
            Text("You must be 18 or older to use Nya.").nyaFont(12).foregroundStyle(c.sb).padding(.top, 6)
            HStack(spacing: 14) {
                let box = RoundedRectangle(cornerRadius: 3)
                Image(systemName: "checkmark").font(.system(size: 11, weight: .bold))
                    .foregroundStyle(terms ? c.on : .clear)
                    .frame(width: 18, height: 18)
                    .background(terms ? c.ac : .clear, in: box)
                    .overlay(box.strokeBorder(terms ? c.ac : c.sb, lineWidth: 2))
                    .frame(width: 34, height: 48)
                Text("I agree to the Terms and Privacy Policy").nyaFont(13).foregroundStyle(c.tx)
            }
            .pressScale(selected: terms) { terms.toggle() }
            .padding(.top, 14)
            Spacer().frame(height: 16)
            NyaButton("Create account", loading: auth.busy) {
                auth.signUp(name: name, email: email, pass: pass, confirm: confirm, dob: dobText(dob), terms: terms) { _ in onVerify() }
            }
            AuthMessage(error: auth.error, info: auth.info)
            SocialRow { auth.google(onDone: onGoogle) }
            TextLink("Already have an account? Sign in", onClick: onSignIn)
        }
        .onAppear { auth.clearMessages() }
        .onChange(of: [name, email, pass, confirm, dob, String(terms)]) { auth.clearError() }
    }
}

struct GreetingScreen: View {
    let name: String
    let onDone: () -> Void
    @Environment(\.nya) private var c

    var body: some View {
        CenterScroll {
            GradientDot(size: 90, colors: [c.b1, c.b2, c.sf])
            Spacer().frame(height: 24)
            Text("Welcome").nyaFont(16).kerning(2).foregroundStyle(c.sb).staggerIn(1)
            Text(name).nyaFont(44, serif: true).foregroundStyle(c.tx).staggerIn(3)
        }
        .nyaBg()
        .task {
            try? await Task.sleep(for: .seconds(2))
            if !Task.isCancelled { onDone() }
        }
    }
}

#Preview {
    NyaTheme {
        WelcomeScreen(onStart: {}, onSignIn: {})
    }
}
