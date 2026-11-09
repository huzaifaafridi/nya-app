import SwiftUI

/// Sizes follow the iPhone's text size setting, as sp sizes do on Android.
private struct NyaFont: ViewModifier {
    @ScaledMetric private var size: CGFloat
    private let weight: Font.Weight
    private let serif: Bool

    init(size: CGFloat, weight: Font.Weight, serif: Bool) {
        _size = ScaledMetric(wrappedValue: size, relativeTo: .body)
        self.weight = weight
        self.serif = serif
    }

    func body(content: Content) -> some View {
        content.font(serif ? .system(size: size, weight: weight, design: .serif).italic() : .system(size: size, weight: weight))
    }
}

extension View {
    func nyaFont(_ size: CGFloat, weight: Font.Weight = .regular, serif: Bool = false) -> some View {
        modifier(NyaFont(size: size, weight: weight, serif: serif))
    }
}
