import SwiftUI

@main
struct NyaApp: App {
    // SwiftUI.Scene, because data/Models.swift has its own Scene enum
    var body: some SwiftUI.Scene {
        WindowGroup {
            NyaTheme {
                WelcomeScreen()
            }
        }
    }
}
