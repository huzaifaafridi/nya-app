import SwiftUI

/// glowing ring orb
struct NyaOrb<Content: View>: View {
    let size: CGFloat
    var glow = true
    @ViewBuilder let content: Content
    @Environment(\.nya) private var c

    var body: some View {
        ZStack {
            RadialGradient(colors: [c.sf, c.bg], center: .center, startRadius: 0, endRadius: size / 2)
            content
        }
        .frame(width: size, height: size)
        .clipShape(Circle())
        .overlay(Circle().strokeBorder(c.ac, lineWidth: 1.5))
        .background {
            if glow {
                Circle().fill(c.ac.opacity(0.08)).frame(width: size + 68, height: size + 68)
                Circle().fill(c.ac.opacity(0.16)).frame(width: size + 20, height: size + 20)
            }
        }
    }
}

struct IconBtn: View {
    let d: String
    let contentDescription: String
    var size: CGFloat = 42
    var onClick: () -> Void = {}
    @Environment(\.nya) private var c

    var body: some View {
        NyaIcon(d: d, tint: c.tx, iconSize: 20)
            .frame(width: size, height: size)
            .background(c.sf, in: Circle())
            .overlay(Circle().strokeBorder(c.ln, lineWidth: 1))
            .pressScale(description: contentDescription, onClick: onClick)
    }
}
