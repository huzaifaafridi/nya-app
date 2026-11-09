import SwiftUI

enum NyaIcons {
    static let Back = "M15 5l-7 7 7 7"
    static let Play = "M8 5l11 7-11 7z"
    static let Stop = "M7 7h10v10H7z"
}

struct NyaIcon: View {
    let d: String
    var tint: Color
    var iconSize: CGFloat = 24
    var strokeW: CGFloat = 1.6

    var body: some View {
        let path = parsePath(d)
        let tint = tint, strokeW = strokeW
        Canvas { ctx, size in
            ctx.scaleBy(x: size.width / 24, y: size.width / 24)
            ctx.stroke(path, with: .color(tint), style: StrokeStyle(lineWidth: strokeW, lineCap: .round, lineJoin: .round))
        }
        .frame(width: iconSize, height: iconSize)
    }
}

// background color + warm glow at the top of the screen, like the design
private struct NyaBg: ViewModifier {
    @Environment(\.nya) private var c

    func body(content: Content) -> some View {
        let glow = c == NyaDark ? Color(argb: 0x3DE8A86E) : Color(argb: 0x33BE6E32)
        content.background {
            Canvas { ctx, size in
                ctx.fill(
                    Path(CGRect(origin: .zero, size: size)),
                    with: .radialGradient(
                        Gradient(colors: [glow, .clear]),
                        center: CGPoint(x: size.width / 2, y: 13),
                        startRadius: 0, endRadius: size.width * 0.85
                    )
                )
            }
            .background(c.bg)
            .ignoresSafeArea()
        }
    }
}

extension View {
    func nyaBg() -> some View {
        modifier(NyaBg())
    }
}
