# iOS port plan

Kotlin in `app/src/main/java/com/nya/app/` is the source of truth. Each Swift file has the same relative path and base name under `iosApp/Nya/` (see `CLAUDE.md`). Work happens on the `ios-port` branch, and a phase is merged to `main` only after it passes CI and ios-tester signs off.

Baseline: Kotlin at `87b35d6`. The existing Swift was ported against `1ec26c6`.

## Status key

| Status | Meaning |
|---|---|
| Done | Ported, and the Kotlin hasn't changed since |
| Stale | Ported, but the Kotlin changed afterwards (cloud sync, config, devices, billing, ...) |
| Partial | Only the parts onboarding needs are ported |
| Stand-in | An iOS placeholder that the real port replaces |
| Missing | No Swift file yet |

## Inventory: 68 Kotlin files

### App entry
| Kotlin | Lines | Swift | Status | Phase | Notes |
|---|---:|---|---|---|---|
| `MainActivity.kt` | 107 | `NyaApp.swift` | Stale | 3 | Navigation is ported. Missing: `LocalConfig`, and the update-required and maintenance gates above every screen |
| `NyaApp.kt` | 15 | `NyaApp.swift` | Stale | 2 | Missing: `FirebaseApp.configure()` and `installAppCheck()` before the container |
| `AppCheck.kt` (debug and release source sets) | 13 + 10 | `AppCheck.swift` | Missing | 2 | Debug provider in Debug and in the `Sideload` configuration only. Release (App Store and TestFlight) is reserved for App Attest. See D1 |

### data
| Kotlin | Lines | Swift | Status | Phase | Notes |
|---|---:|---|---|---|---|
| `data/AppConfig.kt` | 182 | `AppConfig.swift` | **Done (phase 1)** | 1 | `AppConfig`, `BleConfig`, `AiConfig`, `CompanionConfig`, `LegalConfig`, plans, features, voice styles and `parseAppConfig`, with the same defaults and limits |
| `data/AppConfigRepository.kt` | 39 | `AppConfigRepository.swift` | **Stand-in (phase 1)** | 1 → 2 | The protocol is ported. `DefaultAppConfigRepository` serves the built-in defaults until phase 2 adds the live Firestore listener on `appConfig/main` |
| `data/AppContainer.kt` | 48 | `AppContainer.swift` (15) | Stale | 2 → 7 | Grows as each repository lands |
| `data/AuthErrors.kt` | 21 | `AuthErrors.swift` (8) | Done | 2 | Check the Firebase error mapping once real auth is in |
| `data/AuthRepository.kt` | 43 | `AuthRepository.swift` (71) | Stand-in | 2 | The protocol is ported, and `FakeAuthRepository` stays for tests |
| `data/FirebaseAuthRepository.kt` | 63 | — | Missing | 2 | Email and password, Google, reset, sign out, and `isNew` |
| `data/GoogleSignIn.kt` | 21 | `GoogleSignIn.swift` | Missing | 2 | GoogleSignIn-iOS SDK. Cancelling returns `nil`, as on Android |
| `data/BillingRepository.kt` | 58 | — | Missing | 7 | Fake billing, the same as Android |
| `data/ChatRepository.kt` | 236 | `ChatRepository.swift` (7) | Stand-in | 4 + 5 | Storage in phase 4. Streaming, the 30 s idle watchdog, the "Nya is busy..." text and saving partial text in phase 5 |
| `data/CloudSync.kt` | 68 | — | Missing | 4 | Protocol and document types |
| `data/FirestoreCloudSync.kt` | 177 | — | Missing | 4 | Same paths and fields as `firestore.rules`, `platform: "ios"`, writes not awaited |
| `data/SyncManager.kt` | 161 | — | Missing | 4 | Same rules for reconciliation and first upload |
| `data/DeviceRepository.kt` | 173 | `DeviceRepository.swift` (41) | Partial | 6 | Only the connect flow is ported |
| `data/FirmwareCatalog.kt` | 22 | — | Missing | 6 | |
| `data/GeminiReplyEngine.kt` | 83 | — | Missing | 5 | FirebaseAI: model list from config, falls back on quota, server error or 7 s silence, same system prompt |
| `data/MediaStore.kt` | 11 | — | Missing | 4 | |
| `data/MemoryRepository.kt` | 53 | — | Missing | 4 | |
| `data/Models.kt` | 70 | `Models.swift` | **Done (phase 1)** | 1 | Add `Prefs.notifications`, the new `DeviceInfo` fields, `HeadbandSettings`, `MaxDeviceNameLength` and `DefaultCaptureQuality` |
| `data/ProfileRepository.kt` | 113 | `ProfileRepository.swift` | **Partial (phase 1 done)** | 1, then 4 | Phase 1 done: `notifications`, `suggestName`, `MaxNameLength = 100`, name capping and `FakeProfileRepository`. Phase 4: the per-account store, legacy import and cloud push |
| `data/SettingsRepository.kt` | 65 | `SettingsRepository.swift` (40) | Done | — | UserDefaults in place of DataStore |
| `data/UsageRepository.kt` | 67 | — | Missing | 7 | Daily count stored on the phone |
| `data/ble/BleClient.kt` | 39 | `BleClient.swift` (23) | Stale | 6 | `applySettings`, `updateFirmware` and `FirmwareStep` |
| `data/ble/NyaBleUuids.kt` | 9 | `NyaBleUuids.swift` | **Done (phase 1)** | 1 | Only the standard battery UUIDs are left. The headband ones live in `BleConfig`, and the simulator reads `namePrefix` from config |
| `data/ble/SimulatedBleClient.kt` | 171 | `SimulatedBleClient.swift` (48) | Partial | 6 | |
| `data/local/Entities.kt` | 94 | `local/Entities.swift` | Missing | 4 | SwiftData `@Model` classes with the same fields |
| `data/local/NyaDatabase.kt` | 209 | `local/NyaDatabase.swift` | Missing | 4 | DAOs become small query types over a `ModelContainer`. No Room migrations are needed, since iOS starts fresh |

### ui
| Kotlin | Lines | Swift | Status | Phase | Notes |
|---|---:|---|---|---|---|
| `ui/Avatar2D.kt` | 323 | `Avatar2D.swift` | Done | — | |
| `ui/Capture.kt` | 118 | — | Missing | 5 | |
| `ui/Chat.kt` | 187 | — | Missing | 5 | Includes "N messages left today" |
| `ui/Components.kt` | 169 | `Components.swift` | Done | — | |
| `ui/Device.kt` | 173 | — | Missing | 6 | |
| `ui/EditProfile.kt` | 50 | — | Missing | 7 | Reuses `CompanionList` and `VoicePicker` |
| `ui/FindMy.kt` | 101 | — | Missing | 6 | Includes lost mode |
| `ui/Firmware.kt` | 94 | — | Missing | 6 | |
| `ui/Format.kt` | 24 | — | Missing | 5 | |
| `ui/Gates.kt` | 63 | — | Missing | 3 | `UpdateRequiredScreen` and `MaintenanceScreen` |
| `ui/HeadbandSettings.kt` | 99 | — | Missing | 6 | |
| `ui/Help.kt` | 55 | `Help.swift` | Done | — | |
| `ui/Home.kt` | 212 | `Home.swift` (18) | Stand-in | 5 | Currently a placeholder with a Sign out button |
| `ui/Icons.kt` | 88 | `Icons.swift` | Partial | later | Nothing to do in phase 1: `Filter` was never ported, and `Clear` arrives with Memories in phase 5. Other icons arrive with their screens |
| `ui/Legal.kt` | 81 | — | Missing | 3 | `LegalDoc`, `LegalDialog`, `openLegal`, and the Privacy and Terms screens |
| `ui/Memories.kt` | 161 | — | Missing | 5 | Search and share |
| `ui/Motion.kt` | 122 | `Motion.swift` | Done | — | |
| `ui/Onboarding.kt` | 340 | `Onboarding.swift` | Stale | 3 | `CompanionList` with taglines from config, and the public `VoicePicker(prefs, onPick)` |
| `ui/PairFail.kt` | 55 | `PairFail.swift` | Done | — | |
| `ui/Parts.kt` | 137 | `Parts.swift` (41) | Partial | 5 | Only `NyaOrb` and `IconBtn` are ported |
| `ui/Photos.kt` | 168 | `Photos.swift` (30) | Partial | 5 | Only `NyaChip`. The grid has a new layout for fewer than 5 items |
| `ui/ReadOnly.kt` | 65 | — | Missing | 6 | |
| `ui/Screens.kt` | 196 | `Screens.swift` | Stale | 3 | Facebook and Apple buttons removed, name capped at 100, Terms and Privacy become links |
| `ui/Settings.kt` | 107 | — | Missing | 7 | |
| `ui/Shared.kt` | 176 | `Shared.swift` (109) | Partial | 3 + later | `LocalConfig` and the sub-routes for each screen as it lands |
| `ui/Subscription.kt` | 66 | — | Missing | 7 | |
| `ui/Transfer.kt` | 66 | — | Missing | 5 | |
| `ui/Voice.kt` | 144 | `Voice.swift` | Stale | 3 | Voice styles, pitches and rate come from config |
| `ui/theme/Color.kt`, `Theme.kt`, `Type.kt` | 67 | same names | Done | — | |

### viewmodel
| Kotlin | Lines | Swift | Status | Phase | Notes |
|---|---:|---|---|---|---|
| `viewmodel/AppViewModel.kt` | 81 | `AppViewModel.swift` | **Partial (phase 1 done)** | 1 + 7 | Phase 1 done: `config`, `notifications` in `prefs`, and `setNotifications`. Phase 7: `plan` |
| `viewmodel/AuthViewModel.kt` | 133 | `AuthViewModel.swift` | **Done (phase 1)** | 1 | Sets the name only for new accounts, otherwise calls `suggestName`. The date of birth check is now as strict as Kotlin's (see quirks) |
| `viewmodel/ChatViewModel.kt` | 52 | — | Missing | 5 | Quota, and the `aiChat` feature flag |
| `viewmodel/DeviceViewModel.kt` | 192 | `DeviceViewModel.swift` (28) | Partial | 6 | |
| `viewmodel/EditProfileViewModel.kt` | 82 | — | Missing | 7 | |
| `viewmodel/Loadable.kt` | 44 | — | Missing | 4 | `Retryable` |
| `viewmodel/MemoriesViewModel.kt` | 62 | — | Missing | 5 | |
| `viewmodel/MemoryBrowse.kt` | 47 | — | Missing | 5 | |
| `viewmodel/SubscriptionViewModel.kt` | 58 | — | Missing | 7 | |

### Swift-only
| Swift | Notes |
|---|---|
| `ui/PathParser.swift` | Stands in for Compose's `PathParser`. Keep it |

**Totals for the 68 files in `main`, after phase 1:** 14 Done, 7 Stale, 9 Partial, 4 Stand-in and 34 Missing. At the start: 10 Done, 13 Stale, 6 Partial, 3 Stand-in and 36 Missing. The two `AppCheck.kt` files in the debug and release source sets are also Missing.

## Tests

iOS has one UI test (`OnboardingFlowTest`) and no unit-test target. In phase 1 I'll add a `NyaTests` unit-test target to `project.yml` and the CI workflow. Each Kotlin unit test is then ported with its feature:

| Kotlin test | Phase |
|---|---|
| `data/AppConfigTest.kt` | 1 |
| `viewmodel/AuthViewModelTest.kt` | 1 (`suggestName`) + 2 |
| `data/ProfileRepositoryTest.kt` | 1 + 4 |
| `data/SyncManagerTest.kt` | 4 |
| `data/ChatTest.kt`, `data/FailIfSilentTest.kt` | 5 |
| `viewmodel/MemoriesViewModelTest.kt`, `viewmodel/MemoryBrowseTest.kt` | 5 |
| `ui/DateTransformationTest.kt` | 3 (the date of birth field) |
| `data/DeviceRepositoryTest.kt`, `data/SimulatedBleClientTest.kt`, `viewmodel/DeviceViewModelTest.kt` | 6 |
| `data/PlanUsageTest.kt`, `viewmodel/EditProfileViewModelTest.kt` | 7 |

ios-tester extends the UI tests and CI screenshots in each phase.

## Phases (each passes CI and tester review before the next)

1. **Foundations.** `AppConfig` and its parsing, `AppConfigRepository` (defaults-only until phase 2), the Models changes, `Profile.notifications`, `suggestName`, `MaxNameLength`, the `NyaBleUuids` move to `BleConfig`, the `Icons` changes, the `AppViewModel` and `AuthViewModel` changes, and the `NyaTests` target. **Needs nothing from the owner.**
2. **Firebase on iOS.** SPM packages in `project.yml`: FirebaseAuth, FirebaseFirestore, FirebaseAI, FirebaseAppCheck and GoogleSignIn. Then `FirebaseApp.configure()`, `AppCheck.swift`, `FirebaseAuthRepository`, `GoogleSignIn`, and the live Firestore listener for `AppConfigRepository`. CI writes `GoogleService-Info.plist` from a secret. **Needs O1–O4.**
3. **Onboarding parity.** Screens (social and legal), `Legal`, `Gates`, `CompanionList` with taglines from config, `VoicePicker` and voice styles from config, and `LocalConfig`.
4. **Storage and sync.** SwiftData entities and stores, the Memory, Chat, Profile and Media stores, `CloudSync`, `FirestoreCloudSync`, `SyncManager` and `Retryable`.
5. **Main screens.** Home, Chat with `GeminiReplyEngine`, Memories, Capture, Photos, Transfer, and the rest of `Parts` and `Format`.
6. **Device.** `BleClient`, `SimulatedBleClient`, `DeviceRepository`, `FirmwareCatalog`, `DeviceViewModel`, and the Device, HeadbandSettings, Firmware, FindMy and lost mode, and ReadOnly screens.
7. **Account.** Settings, EditProfile, Subscription with fake billing, `UsageRepository`, and the plan in `AppViewModel`.
8. **Cleanup.** Remove the stand-ins, update the port status in `CLAUDE.md`, and mark this plan done.

## Things the owner must do

| # | When | What |
|---|---|---|
| O1 | Before phase 2 | Firebase console → project `nyaa-9ddc0` → Add app → iOS, with bundle ID **`com.nya.app`**. The App Store ID is optional |
| O2 | Before phase 2 | Check that Google is enabled under Authentication → Sign-in method (it already is for Android). **Then** download `GoogleService-Info.plist`, so it contains `CLIENT_ID` and `REVERSED_CLIENT_ID`. Don't commit it, as with `google-services.json` |
| O3 | Before phase 2 | GitHub → repo Settings → Secrets and variables → Actions → New secret **`GOOGLE_SERVICE_INFO_PLIST`**, whose value is the plist encoded in base64 (PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("GoogleService-Info.plist"))`) |
| O4 | Before phase 2 | Firebase console → App Check → register the iOS app (App Attest can be chosen but won't work without a paid account). Manage debug tokens → add a token for CI and the sideloaded build, then save the same value as GitHub secret **`APP_CHECK_DEBUG_TOKEN`**. Not needed while AI Logic stays on Monitoring, but needed before it is Enforced |
| O5 | Optional | Install the GitHub CLI (`winget install GitHub.cli`, then `gh auth login`) so CI can be watched with `gh run watch`. Without it I poll the public Actions API, which works because the repo's runs are readable |

## Decisions for the owner

- **D1. App Check (decided: option a).** The sideloaded `.ipa` uses the debug provider with the `APP_CHECK_DEBUG_TOKEN` secret built in, and it must never be able to ship. How phase 2 does it:
  - `project.yml` gets a third build configuration, **`Sideload`**, based on Release. Only Debug and Sideload set the Swift flag `NYA_APPCHECK_DEBUG`, and only Sideload gets the token, through a build setting that CI passes in from the secret.
  - `AppCheck.swift` installs `AppCheckDebugProviderFactory` only under `#if NYA_APPCHECK_DEBUG`. The `Release` configuration, which App Store and TestFlight archives use, has neither the flag nor the token, so the debug provider code is not even compiled in.
  - CI builds the sideloaded `.ipa` with `-configuration Sideload`. A CI check fails the build if `NYA_APPCHECK_DEBUG` or the token ever appears in the Release settings.
  - **TODO before any App Store or TestFlight release: switch Release to App Attest** (`AppAttestProvider`, with the DeviceCheck fallback for older iOS). This needs a paid Apple Developer account and the App Attest capability. Until then, Release has no App Check provider, so chat works on it only while AI Logic is on Monitoring.
- **D2. Storage (decided).** SwiftData (iOS 17, which matches `deploymentTarget`). There are no migrations, since no iOS users have data yet.

## Kotlin quirks to copy as they are (unless the owner says otherwise)

- **Config numbers:** Kotlin's `Number.toLong()` turns NaN into 0, so a NaN `plans.*.dailyMessages` becomes a limit of 0, because 0 is in range. Swift copies this, and `AppConfigTest.testNonFiniteAndHugeNumbersNeverTrap` pins it.
- **Name cap:** Kotlin's `take(100)` counts UTF-16 units and can cut an emoji in half. Swift keeps whole characters, so the result is never longer than Kotlin's and never broken. Intentional, and it doesn't affect the Firestore rule.
- **String lengths:** Kotlin `length` counts UTF-16 units. Swift must use `.utf16.count` wherever Kotlin compares a length (config text limits, the 8-character password). `.count` counts characters and differs for emoji.
- **Config floats:** `voiceRate` and the voice pitches are converted to Float before the range check, so `2.0000001` is accepted as `2`. Whole numbers are accepted too.
- **Config sections:** an unknown key in `companions` is ignored, and a bad trait or tagline falls back one field at a time. `voiceStyles` is the one all-or-nothing field, so a saved voice index keeps its meaning. Every text field is trimmed first, then length-checked, and a blank or oversized value falls back.
- **Dictionaries have no order:** `companions` and `plans` are Swift dictionaries, but Kotlin maps keep insertion order. Any UI that lists them (phases 5–7) must iterate `Companion.allCases` or `Plan.allCases`, never the dictionary.
- **Name prefix read when needed:** the simulator reads `config.ble.namePrefix` through a closure at connect time, as Kotlin does. Keep it that way when phase 2 swaps in the Firestore repository.
- **Date of birth edge cases (not copied):** Kotlin accepts the year `0000` and trims a slightly different set of control characters. Swift rejects year 0, because the Gregorian round trip fails. Too obscure to matter.
- **`FakeProfileRepository` doesn't cap names.** Only the real store (Room on Android, the UserDefaults stand-in on iOS) caps them.

## Port bugs found and fixed

- Phase 1: Swift's date of birth check accepted `1/1/2000` and three-digit years such as `09/02/200`. Kotlin's `dd/MM/uuuu` STRICT rejects both. Fixed with an exact-shape check plus a round trip. `AuthViewModelTest.testMalformedBirthDatesAreRejected` covers it.
- Phase 1 (ios-tester): Swift counted the password length in characters, so 4 emoji were rejected. Kotlin counts UTF-16 units and accepts them. Fixed with `.utf16.count`. `AuthViewModelTest.testPasswordLengthCountsUtf16UnitsLikeKotlin` covers it.

## Log

- Plan created on `ios-port` from `main` at `87b35d6`.
- Phase 1 written: `AppConfig`, the `AppConfigRepository` stand-in, the Models and Profile changes, `NyaBleUuids`, the simulator's name prefix from config, the App and Auth view models, and the `NyaTests` target with `AppConfigTest`, `ProfileRepositoryTest` and `AuthViewModelTest`.
- Phase 1: CI passed ([run 42622849774](https://github.com/huzaifaafridi/nya-app/actions/runs/42622849774)). ios-tester signed off, with one fix (password length). Quirks recorded above.
