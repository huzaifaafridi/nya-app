import SwiftUI

// shared motion tokens: short, FastOutSlowIn or springs, nothing flashy
enum Motion {
    static let MED = 0.25
    static let LONG = 0.35
    static let STAGGER = 0.06
}

func motionTween(_ seconds: Double = Motion.MED) -> Animation {
    .timingCurve(0.4, 0, 0.2, 1, duration: seconds)
}

/// fade + 24pt slide + slight scale; forward comes in from the right, back from the left
func screenTransition(forward: Bool, reduce: Bool) -> AnyTransition {
    if reduce { return .identity }
    let d: CGFloat = forward ? 24 : -24
    return .asymmetric(
        insertion: .opacity.combined(with: .offset(x: d)).combined(with: .scale(scale: 0.98)),
        removal: .opacity.combined(with: .offset(x: -d)).combined(with: .scale(scale: 0.98))
    )
}

/// A subtle press effect instead of the system highlight: scales to 0.96 while pressed and springs back.
private struct PressScale: ButtonStyle {
    @Environment(\.accessibilityReduceMotion) private var reduce

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .contentShape(Rectangle())
            .scaleEffect(configuration.isPressed && !reduce ? 0.96 : 1)
            .animation(.spring(response: 0.3, dampingFraction: 0.6), value: configuration.isPressed)
    }
}

extension View {
    /// [selected] and [description] are what VoiceOver announces; use [description] for icon-only targets.
    @ViewBuilder
    func pressScale(selected: Bool = false, description: String? = nil, onClick: @escaping () -> Void) -> some View {
        let button = Button(action: onClick) { self }
            .buttonStyle(PressScale())
            .accessibilityAddTraits(selected ? .isSelected : [])
        if let description {
            button.accessibilityLabel(description)
        } else {
            button
        }
    }

    /// list/card entrance: fade in and rise 16pt, staggered by index
    func staggerIn(_ index: Int) -> some View {
        modifier(StaggerIn(index: index))
    }
}

private struct StaggerIn: ViewModifier {
    let index: Int
    @Environment(\.accessibilityReduceMotion) private var reduce
    @State private var shown = false

    func body(content: Content) -> some View {
        content
            .opacity(shown || reduce ? 1 : 0)
            .offset(y: shown || reduce ? 0 : 16)
            .onAppear {
                withAnimation(motionTween(Motion.LONG).delay(Double(index) * Motion.STAGGER)) { shown = true }
            }
    }
}
