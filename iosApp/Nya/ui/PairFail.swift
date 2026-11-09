import SwiftUI

struct PairFailScreen: View {
    let onBack: () -> Void
    @Environment(\.nya) private var c
    private let tips = [
        "Turn Bluetooth on in your phone settings",
        "Make sure the headband is charged",
        "Keep it within 1 metre of your phone",
        "Hold the button for 5 seconds to reset pairing"
    ]

    var body: some View {
        VStack(spacing: 0) {
            XTop(title: "Pairing", onBack: onBack)
            ScrollView {
                VStack(spacing: 0) {
                    Spacer().frame(height: 20)
                    Text("!").nyaFont(40, weight: .bold).foregroundStyle(c.ac)
                        .accessibilityHidden(true)
                        .frame(width: 96, height: 96)
                        .overlay(Circle().strokeBorder(c.ac, lineWidth: 3))
                    Spacer().frame(height: 22)
                    Text("Couldn't connect").nyaFont(24, weight: .semibold).foregroundStyle(c.tx)
                    Text("Let's try a few things.").nyaFont(14).foregroundStyle(c.sb)
                        .padding(.top, 6)
                        .padding(.bottom, 24)
                    XCard {
                        ForEach(Array(tips.enumerated()), id: \.offset) { i, tip in
                            HStack(alignment: .top, spacing: 0) {
                                Text("\(i + 1)").nyaFont(15, weight: .bold).foregroundStyle(c.ac).frame(width: 26, alignment: .leading)
                                Text(tip).nyaFont(15).foregroundStyle(c.tx)
                            }
                            .padding(.vertical, 8)
                        }
                    }
                }
                .padding(.horizontal, 20)
            }
            VStack(spacing: 12) {
                XBtn("Try again", onClick: onBack)
                XGhost("Get help") { SubNav.open("Help") }
            }
            .padding(20)
        }
        .nyaBg()
    }
}
