import XCTest

/// Walks the whole onboarding flow in the simulator and saves a screenshot of each screen.
/// With no Mac to run the app on, this is how the flow gets checked.
@MainActor
final class OnboardingFlowTest: XCTestCase {
    private var app: XCUIApplication!

    override func setUp() async throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launch()
    }

    private func shot(_ name: String) {
        Thread.sleep(forTimeInterval: 0.6) // let the screen transition finish
        let a = XCTAttachment(screenshot: app.screenshot())
        a.name = name
        a.lifetime = .keepAlways
        add(a)
    }

    private func see(_ text: String) {
        XCTAssertTrue(app.staticTexts[text].waitForExistence(timeout: 6), "missing: \(text)")
    }

    private func tap(_ label: String) {
        let b = app.buttons[label]
        XCTAssertTrue(b.waitForExistence(timeout: 6), "missing button: \(label)")
        b.tap()
    }

    private func tapStarting(_ label: String) {
        let b = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", label)).firstMatch
        XCTAssertTrue(b.waitForExistence(timeout: 6), "missing button: \(label)")
        b.tap()
    }

    private func type(_ text: String, into field: XCUIElement) {
        field.tap()
        field.typeText(text)
    }

    func testOnboarding() {
        see("Nya")
        shot("01-welcome")

        // sign in screen: empty form error, reset password, then on to sign up
        tap("Sign in")
        see("Welcome back")
        tap("Sign in")
        see("Enter your email and password.")
        shot("02-sign-in-error")
        tap("Forgot password?")
        see("Reset password")
        shot("03-reset")
        tap("Back to sign in")
        see("Welcome back")
        tap("New here? Create account")
        see("Create account")
        shot("04-sign-up")

        type("Ada", into: app.textFields["Full name"])
        type("ada@example.com", into: app.textFields["Email"])
        type("password1", into: app.secureTextFields["Password (min 8 characters)"])
        type("password1", into: app.secureTextFields["Confirm password"])
        type("01012000", into: app.textFields["Date of birth (DD/MM/YYYY)"])
        XCTAssertEqual(app.textFields["Date of birth (DD/MM/YYYY)"].value as? String, "01/01/2000")
        app.swipeUp()
        tapStarting("I agree")
        shot("05-sign-up-filled")
        tap("Create account")

        see("Check your email")
        shot("06-verify")
        tap("Continue")

        see("Choose your companion")
        shot("07-companion")
        tapStarting("Creator")
        tap("Continue")

        see("Make it yours")
        shot("08-avatar")
        tap("Male")
        shot("09-avatar-male")
        tap("Skin tone 4")
        tap("Continue")

        see("A few permissions")
        shot("10-permissions")
        tap("Allow and continue")

        see("Find your Nya")
        shot("11-pair")
        tap("Connect")

        see("Welcome")
        see("Ada")
        shot("12-greeting")

        see("Home")
        shot("13-home")

        // onboarding is remembered across launches
        app.terminate()
        app.launch()
        see("Home")

        // signing out starts over
        tap("Sign out")
        see("Fashionable AI")
    }
}
