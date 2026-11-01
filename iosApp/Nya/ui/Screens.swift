import SwiftUI

struct WelcomeScreen: View {
    @Environment(\.nya) private var c

    var body: some View {
        VStack {
            Circle()
                .fill(RadialGradient(colors: [c.b1, c.b2, c.sf], center: .center, startRadius: 0, endRadius: 75))
                .frame(width: 150, height: 150)
            Spacer().frame(height: 28)
            Text("Nya")
                .font(.system(size: 64, design: .serif).italic())
                .foregroundStyle(c.tx)
            Text("Fashionable AI")
                .font(.system(size: 15))
                .kerning(2)
                .foregroundStyle(c.sb)
        }
        .padding(24)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(c.bg)
    }
}

#Preview {
    NyaTheme {
        WelcomeScreen()
    }
}
