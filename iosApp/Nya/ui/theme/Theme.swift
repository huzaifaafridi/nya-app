import SwiftUI

struct NyaTheme<Content: View>: View {
    private let dark: Bool? // nil follows the system
    private let content: Content
    @Environment(\.colorScheme) private var systemScheme

    init(dark: Bool? = nil, @ViewBuilder content: () -> Content) {
        self.dark = dark
        self.content = content()
    }

    var body: some View {
        let c = (dark ?? (systemScheme == .dark)) ? NyaDark : NyaLight
        content
            .environment(\.nya, c)
            .tint(c.ac)
            .preferredColorScheme(dark.map { $0 ? .dark : .light })
    }
}
