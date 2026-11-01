import SwiftUI

extension Color {
    /// Takes the same 0xAARRGGBB literals as Kotlin's Color(0x...).
    init(argb: UInt32) {
        self.init(
            .sRGB,
            red: Double((argb >> 16) & 0xFF) / 255,
            green: Double((argb >> 8) & 0xFF) / 255,
            blue: Double(argb & 0xFF) / 255,
            opacity: Double(argb >> 24) / 255
        )
    }
}

struct NyaColors: Equatable, Sendable {
    let bg: Color, sf: Color, ln: Color, tx: Color, sb: Color
    let ac: Color, b1: Color, b2: Color, on: Color
}

let NyaDark = NyaColors(
    bg: Color(argb: 0xFF0B0908), sf: Color(argb: 0xFF161210), ln: Color(argb: 0xFF2E2822),
    tx: Color(argb: 0xFFF5EFE8), sb: Color(argb: 0xFFB8AEA2),
    ac: Color(argb: 0xFFE8A86E), b1: Color(argb: 0xFFEDB07A), b2: Color(argb: 0xFFC0733A),
    on: Color(argb: 0xFF1B0F05)
)
let NyaLight = NyaColors(
    bg: Color(argb: 0xFFFBF7F2), sf: Color(argb: 0xFFFFFFFF), ln: Color(argb: 0xFFE6DCD0),
    tx: Color(argb: 0xFF2A1D12), sb: Color(argb: 0xFF6B5B4B),
    ac: Color(argb: 0xFFA5581F), b1: Color(argb: 0xFFAD5C22), b2: Color(argb: 0xFF8C4216),
    on: Color(argb: 0xFFFFFFFF)
)

extension EnvironmentValues {
    @Entry var nya: NyaColors = NyaDark
}
