import SwiftUI

// status colors: lighter on dark, darker on light so the text keeps its contrast
extension NyaColors {
    var green: Color { self == NyaDark ? Color(argb: 0xFF6BCB8B) : Color(argb: 0xFF1E7A45) }
    var danger: Color { self == NyaDark ? Color(argb: 0xFFE5736A) : Color(argb: 0xFFB3382E) }
}
