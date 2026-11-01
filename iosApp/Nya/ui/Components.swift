import SwiftUI

struct NyaButton: View {
    let text: String
    let onClick: () -> Void
    var loading = false
    @Environment(\.nya) private var c

    init(_ text: String, loading: Bool = false, onClick: @escaping () -> Void) {
        self.text = text
        self.loading = loading
        self.onClick = onClick
    }

    var body: some View {
        ZStack {
            if loading {
                ProgressView().tint(c.on).accessibilityLabel("Loading")
            } else {
                Text(text).nyaFont(16, weight: .semibold).foregroundStyle(c.on).multilineTextAlignment(.center)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
        .frame(maxWidth: .infinity, minHeight: 54)
        .background(LinearGradient(colors: [c.b1, c.b2], startPoint: .top, endPoint: .bottom))
        .clipShape(RoundedRectangle(cornerRadius: 27))
        .pressScale { if !loading { onClick() } }
    }
}

struct AuthMessage: View {
    let error: String?
    let info: String?
    @Environment(\.nya) private var c

    var body: some View {
        if let error {
            Text(error).nyaFont(13).foregroundStyle(c.danger).padding(.top, 12)
        } else if let info {
            Text(info).nyaFont(13).foregroundStyle(c.sb).padding(.top, 12)
        }
    }
}

/// Fills its space and centres [content]; scrolls instead when the content is taller (big fonts, small phones).
struct CenterScroll<Content: View>: View {
    @ViewBuilder let content: Content

    var body: some View {
        GeometryReader { g in
            ScrollView {
                VStack(spacing: 0) { content }
                    .frame(maxWidth: .infinity, minHeight: g.size.height)
            }
            .scrollBounceBehavior(.basedOnSize)
        }
    }
}

struct NyaOutlineButton: View {
    let text: String
    let onClick: () -> Void
    @Environment(\.nya) private var c

    init(_ text: String, onClick: @escaping () -> Void) {
        self.text = text
        self.onClick = onClick
    }

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: 27)
        Text(text).nyaFont(14, weight: .medium).foregroundStyle(c.tx).multilineTextAlignment(.center)
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
            .frame(maxWidth: .infinity, minHeight: 54)
            .background(c.sf, in: shape)
            .overlay(shape.strokeBorder(c.ln, lineWidth: 1))
            .pressScale(onClick: onClick)
    }
}

struct NyaField: View {
    let label: String
    @Binding var value: String
    var password = false
    var keyboardType: UIKeyboardType = .default
    var capitalize = false
    @Environment(\.nya) private var c
    @FocusState private var focused: Bool

    init(_ label: String, value: Binding<String>, password: Bool = false, keyboardType: UIKeyboardType = .default, capitalize: Bool = false) {
        self.label = label
        _value = value
        self.password = password
        self.keyboardType = keyboardType
        self.capitalize = capitalize
    }

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: 16)
        VStack(alignment: .leading, spacing: 0) {
            Text(label).nyaFont(12).foregroundStyle(c.sb).padding(.bottom, 6)
            Group {
                if password {
                    SecureField("", text: $value)
                } else {
                    TextField("", text: $value)
                }
            }
            .focused($focused)
            .accessibilityLabel(label)
            .nyaFont(16)
            .foregroundStyle(c.tx)
            .tint(c.ac)
            .keyboardType(keyboardType)
            // anything else lets iOS swap the field for its Automatic Strong Password cover
            .textContentType(password ? .oneTimeCode : nil)
            .textInputAutocapitalization(capitalize ? .words : .never)
            .autocorrectionDisabled()
            .padding(.horizontal, 16)
            .frame(minHeight: 56)
            .background(c.sf, in: shape)
            .overlay(shape.strokeBorder(focused ? c.ac : c.ln, lineWidth: focused ? 2 : 1))
        }
    }
}

/// Shows a digits-only string as DD/MM/YYYY; slashes appear only once more digits follow them.
func dateShown(_ digits: String) -> String {
    var shown = ""
    for (i, ch) in digits.enumerated() {
        if i == 2 || i == 4 { shown.append("/") }
        shown.append(ch)
    }
    return shown
}
