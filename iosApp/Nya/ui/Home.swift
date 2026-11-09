import SwiftUI

/// Placeholder where onboarding ends until the Home tabs are ported. Sign out stands in for
/// the Settings screen, so onboarding can be run again.
struct MainTabs: View {
    let onSignOut: () -> Void

    var body: some View {
        Page {
            Spacer().frame(height: 40)
            Heading("Home")
            Spacer().frame(height: 12)
            Para("The rest of the app hasn't been ported to iOS yet.")
            Spacer().frame(height: 32)
            NyaOutlineButton("Sign out", onClick: onSignOut)
        }
    }
}
