import SwiftUI

struct NyaChip: View {
    let text: String
    let on: Bool
    var hPad: CGFloat = 16
    let onClick: () -> Void
    @Environment(\.nya) private var c

    init(_ text: String, on: Bool, hPad: CGFloat = 16, onClick: @escaping () -> Void) {
        self.text = text
        self.on = on
        self.hPad = hPad
        self.onClick = onClick
    }

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: 19)
        Text(text).nyaFont(13, weight: .medium).lineLimit(1).foregroundStyle(on ? c.ac : c.tx)
            .padding(.horizontal, hPad)
            .frame(maxWidth: .infinity, minHeight: 38)
            .background(c.sf, in: shape)
            .overlay(shape.strokeBorder(on ? c.ac : c.ln, lineWidth: 1))
            .padding(3)
            .overlay(RoundedRectangle(cornerRadius: 22).strokeBorder(c.ac.opacity(on ? 0.22 : 0), lineWidth: 3))
            .frame(minHeight: 48)
            .animation(motionTween(), value: on)
            .pressScale(selected: on, onClick: onClick)
    }
}
