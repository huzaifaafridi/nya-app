import SwiftUI

let faqs = [
    ("How do I pair my headband?", "Open Device, tap Pairing trouble? and follow the steps. Keep the headband within 1 metre and make sure Bluetooth is on."),
    ("How do I charge it?", "Place the headband on a Qi wireless charger. The red light shows charging, green means ready."),
    ("Where are my memories stored?", "Memories sync from your headband to your account, so you can see them on any device you sign in on."),
    ("Can I use Nya without the headband?", "You can chat with Nya, but capturing new memories needs the headband connected."),
    ("How do I delete a memory?", "Open a memory and tap Delete. It's removed from your account.")
]

struct HelpScreen: View {
    let onBack: () -> Void
    @Environment(\.nya) private var c
    @State private var open = -1

    var body: some View {
        VStack(spacing: 0) {
            XTop(title: "Help & support", onBack: onBack)
            ScrollView {
                VStack(spacing: 12) {
                    ForEach(Array(faqs.enumerated()), id: \.offset) { i, f in
                        XCard {
                            HStack(spacing: 0) {
                                Text(f.0).nyaFont(15).foregroundStyle(c.tx).frame(maxWidth: .infinity, alignment: .leading)
                                Text(open == i ? "–" : "+").nyaFont(20).foregroundStyle(c.ac).accessibilityHidden(true)
                            }
                            if open == i {
                                Text(f.1).nyaFont(14).lineSpacing(4).foregroundStyle(c.sb).padding(.top, 10)
                            }
                        }
                        .pressScale(selected: open == i) { open = open == i ? -1 : i }
                        .accessibilityValue(open == i ? "Expanded" : "Collapsed")
                    }
                }
                .padding(.horizontal, 20)
            }
            XBtn("Contact support") {}
                .padding(20)
        }
        .nyaBg()
    }
}
