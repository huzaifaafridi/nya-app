# Nya

Nya is a companion app for a smart headband. This repo holds two native apps for the same product:

| | Android | iOS |
|---|---|---|
| Location | `app/` | `iosApp/` |
| Language / UI | Kotlin, Jetpack Compose | Swift, SwiftUI |
| Source root | `app/src/main/java/com/nya/app/` | `iosApp/Nya/` |
| Project | Gradle | XcodeGen (`iosApp/project.yml`) |
| Status | Full app (the reference) | Theme, models and a starter screen |

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

## Port status

Ported:

- `ui/theme/Color.kt` and `Theme.kt`, plus the status colours from `ui/Shared.kt`
- `data/Models.kt`
- Model types from `AuthRepository.kt`, `ChatRepository.kt`, `ProfileRepository.kt`, `SettingsRepository.kt` and `ble/BleClient.kt`
- The top of `WelcomeScreen` (logo and title, no buttons yet)

Not ported:

- `data/local/Entities.kt`: these are Room table shapes, so port them when iOS storage is chosen.
- `ui/theme/Type.kt`, and all repositories, view models, navigation and other screens.
- Colours that belong to one feature, such as those in `Avatar2D.kt` and `Photos.kt`, and the skin tones in `Onboarding.kt`. Port these with that feature.

Update this list when a port lands.

## Building

Android: `./gradlew assembleDebug` to build, `./gradlew test` for unit tests.

iOS: development happens on Windows with no Mac, so Swift cannot be compiled locally. `.github/workflows/ios.yml` builds the app on a macOS runner on every push that touches `iosApp/`. Treat Swift as unverified until that workflow passes, and check it after every Swift change.

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
