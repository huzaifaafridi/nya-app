import SwiftUI

@MainActor
enum SubNav {
    static var open: (String) -> Void = { _ in }
}

extension EnvironmentValues {
    @Entry var prefs = Prefs()
}

// status colors: lighter on dark, darker on light so the text keeps its contrast
extension NyaColors {
    var green: Color { self == NyaDark ? Color(argb: 0xFF6BCB8B) : Color(argb: 0xFF1E7A45) }
    var danger: Color { self == NyaDark ? Color(argb: 0xFFE5736A) : Color(argb: 0xFFB3382E) }
}

extension View {
    func asHeading() -> some View {
        accessibilityAddTraits(.isHeader)
    }
}

struct XTop: View {
    let title: String
    let onBack: () -> Void
    @Environment(\.nya) private var c

    var body: some View {
        HStack(spacing: 0) {
            Text("‹").nyaFont(30).foregroundStyle(c.tx)
                .frame(minWidth: 48, minHeight: 48)
                .pressScale(description: "Back", onClick: onBack)
            Text(title).nyaFont(20, weight: .semibold).foregroundStyle(c.tx).asHeading()
            Spacer(minLength: 0)
        }
        .padding(EdgeInsets(top: 12, leading: 2, bottom: 12, trailing: 20))
    }
}

struct XBtn: View {
    let text: String
    let onClick: () -> Void
    @Environment(\.nya) private var c

    init(_ text: String, onClick: @escaping () -> Void) {
        self.text = text
        self.onClick = onClick
    }

    var body: some View {
        Text(text).nyaFont(16, weight: .semibold).foregroundStyle(c.on).multilineTextAlignment(.center)
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
            .frame(maxWidth: .infinity, minHeight: 54)
            .background(LinearGradient(colors: [c.b1, c.b2], startPoint: .leading, endPoint: .trailing))
            .clipShape(RoundedRectangle(cornerRadius: 27))
            .pressScale(onClick: onClick)
    }
}

struct XGhost: View {
    let text: String
    let onClick: () -> Void
    @Environment(\.nya) private var c

    init(_ text: String, onClick: @escaping () -> Void) {
        self.text = text
        self.onClick = onClick
    }

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: 27)
        Text(text).nyaFont(16).foregroundStyle(c.tx).multilineTextAlignment(.center)
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
            .frame(maxWidth: .infinity, minHeight: 54)
            .overlay(shape.strokeBorder(c.ln, lineWidth: 1))
            .pressScale(onClick: onClick)
    }
}

struct XCard<Content: View>: View {
    @ViewBuilder let content: Content
    @Environment(\.nya) private var c

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: 20)
        VStack(alignment: .leading, spacing: 0) { content }
            .padding(18)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(c.sf, in: shape)
            .overlay(shape.strokeBorder(c.ln, lineWidth: 1))
    }
}

// every extra screen gets one line added here as we build it
struct SubRouter: View {
    let name: String
    let onBack: () -> Void

    var body: some View {
        switch name {
        case "Help": HelpScreen(onBack: onBack)
        case "PairFail": PairFailScreen(onBack: onBack)
        default: EmptyView()
        }
    }
}
