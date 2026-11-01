# Nya

Nya is a companion app for a smart headband. This repo holds two native apps for the same product:

| | Android | iOS |
|---|---|---|
| Location | `app/` | `iosApp/` |
| Language / UI | Kotlin, Jetpack Compose | Swift, SwiftUI |
| Source root | `app/src/main/java/com/nya/app/` | `iosApp/Nya/` |
| Project | Gradle | XcodeGen (`iosApp/project.yml`) |
| Status | Full app (the reference) | Theme, models and onboarding |

## Git workflow

- The owner works alone on this repo, so work directly on `main`.
- After each task, commit and push to `main`.
- Check that the iOS build on GitHub Actions passes, and that the Android app still builds with `gradlew`. If the Android build can't run on this computer, say why.
- If a build fails, fix it before calling the task done.
- Only create a separate branch when asked for one.

## The rule: Kotlin first, then Swift

Every feature is built and finished in Kotlin first, then ported to Swift.

- Do not start a feature in Swift. If a behaviour or value differs between the apps, Kotlin is right unless it is a bug.
- Changing a colour, model field or behaviour in Kotlin means making the same change in Swift, in the same commit where possible.
- A port copies behaviour, values and text. It uses SwiftUI idioms for the UI rather than imitating Compose.

## Parallel folders and file names

A Swift file has the same relative path and base name as its Kotlin file:

| Kotlin (under `app/src/main/java/com/nya/app/`) | Swift (under `iosApp/Nya/`) |
|---|---|
| `data/Models.kt` | `data/Models.swift` |
| `data/ble/BleClient.kt` | `data/ble/BleClient.swift` |
| `ui/Screens.kt` | `ui/Screens.swift` |
| `ui/theme/Color.kt` | `ui/theme/Color.swift` |
| `viewmodel/ChatViewModel.kt` | `viewmodel/ChatViewModel.swift` |

The one exception is the entry point: `NyaApp.swift` covers both `NyaApp.kt` and `MainActivity.kt`.

Port only what is needed. `data/ProfileRepository.swift` holds just the `Profile` struct until the repository itself is ported, so a file can be a partial port.

Base names must be unique across the Swift target, because Swift does not allow two files with the same name in one module. New files are picked up automatically, as `project.yml` includes the whole `Nya` folder.

## Porting conventions

- Keep type and property names the same as Kotlin, including short ones like `bg`, `sf` and `tx`, and top-level names like `NyaDark` and `ModeLabels`.
- Enum cases are lowerCamelCase in Swift, with raw values equal to the Kotlin names: `case sea = "Sea"`.
- Types: Kotlin `Long` is `Int64`, `Int` is `Int`, `Float` is `Float`. A `data class` is a `struct` that is `Hashable` and `Sendable`.
- Use `var` for struct fields. A `let` with a default value is left out of the generated initializer.
- ARGB values are `UInt32`, not `Int`, and colours use the same hex as Kotlin: `Color(argb: 0xFFE0AC84)`.
- `LocalNya.current` becomes `@Environment(\.nya) private var c`.
- `NyaGreen` and `NyaDanger` become `c.green` and `c.danger`.
- `Scene` in this module is our enum, so the SwiftUI protocol must be written `SwiftUI.Scene`.
- Compose `sp` sizes use `.nyaFont(size)`, so text follows the phone's text size setting.
- `Modifier.pressScale` is the `.pressScale { }` view modifier, and it goes after clip and background, not before.
- Keep SVG path strings identical to Kotlin and draw them with `parsePath`.

## Port status

Ported:

- `ui/theme/Color.kt`, `Theme.kt` and `Type.kt` (fonts only, as `nyaFont`), plus the status colours from `ui/Shared.kt`
- `data/Models.kt`
- Onboarding and its navigation: Welcome, Sign in, Sign up and Greeting (`ui/Screens.kt`), everything in `ui/Onboarding.kt`, `ui/PairFail.kt`, `ui/Help.kt`, and the navigation from `MainActivity.kt`
- What those screens use: `ui/Avatar2D.kt`, `ui/Voice.kt`, `ui/Motion.kt`, `ui/Components.kt`, `NyaOrb` and `IconBtn` from `ui/Parts.kt`, `NyaChip` from `ui/Photos.kt`, the `X*` parts of `ui/Shared.kt`, and the `Back`, `Play` and `Stop` icons
- `viewmodel/AppViewModel.kt`, `AuthViewModel.kt` and the connect part of `DeviceViewModel.kt`
- `AuthRepository.kt` (the fake), `ProfileRepository.kt`, `SettingsRepository.kt` (both on `UserDefaults`), and the connect part of `DeviceRepository.kt` and `ble/SimulatedBleClient.kt`

iOS-only stand-ins, to replace as the real thing is ported:

- `ui/Home.swift` is a placeholder with a Sign out button, not the real Home.
- Auth is `FakeAuthRepository`, so any details sign in and Google signs in as "Mercy". Android uses Firebase.
- `SimulatedDeviceRepository` replaces the Room-backed device repository.
- `ui/PathParser.swift` has no Kotlin file. It stands in for Compose's `PathParser`.

Not ported:

- `data/local/Entities.kt`: these are Room table shapes, so port them when iOS storage is chosen.
- Home, Chat, Memories, Device, Settings, Subscription and the other main screens, plus their repositories and view models.
- Colours that belong to those features, such as the ones in `Photos.kt`. Port these with that feature.

Update this list when a port lands.

## Building

Android: `./gradlew assembleDebug` to build, `./gradlew test` for unit tests.

iOS: development happens on Windows with no Mac, so Swift cannot be compiled locally. `.github/workflows/ios.yml` builds the app on a macOS runner on every push that touches `iosApp/`. Treat Swift as unverified until that workflow passes, and check it after every Swift change.

The workflow also runs `iosApp/NyaUITests/OnboardingFlowTest.swift` in a simulator, which walks the whole onboarding flow. It saves a screenshot of each screen in the `Nya-screenshots` artifact, so UI work can be checked without a Mac. A change to the flow must keep that test passing.

`Nya.xcodeproj` is generated and git-ignored; never commit it. On a Mac:

```sh
cd iosApp
xcodegen generate
open Nya.xcodeproj
```

Change project settings in `project.yml`, not in Xcode.

## Testing on iPhone

There is no paid Apple Developer account, so testing uses a free Apple ID and Sideloadly on Windows.

- `.github/workflows/ios.yml` also builds for `iphoneos` without code signing, packages an unsigned `Nya.ipa`, and uploads it as the `Nya-ipa` artifact on the Actions run.
- Sideloadly signs the `.ipa` with the free Apple ID during install, so CI needs no certificates or secrets.
- The install expires after 7 days. Reinstall with Sideloadly; the same `.ipa` still works.
- GitHub deletes artifacts after 90 days by default, and a free Apple ID allows at most 3 sideloaded apps on a device.
- The app has no icon yet, so it shows a blank one on the home screen.
