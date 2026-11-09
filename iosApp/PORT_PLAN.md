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
| `AppCheck.kt` (debug and release source sets) | 13 + 10 | `AppCheck.swift` | Missing | 2 | One file using `#if DEBUG`: the debug provider in Debug. Release needs App Attest (paid account), see decision D1 |

### data
| Kotlin | Lines | Swift | Status | Phase | Notes |
|---|---:|---|---|---|---|
| `data/AppConfig.kt` | 182 | — | Missing | 1 | `AppConfig`, `BleConfig`, `AiConfig`, `CompanionConfig`, `LegalConfig`, plans, features, voice styles and `parseAppConfig`, with the same defaults and limits |
| `data/AppConfigRepository.kt` | 39 | — | Missing | 1 | Live listener on Firestore `appConfig/main`. Starts as the defaults, and an error keeps the current value. Before phase 2, a stand-in that only serves defaults |
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
| `data/Models.kt` | 70 | `Models.swift` (62) | Stale | 1 | Add `Prefs.notifications`, the new `DeviceInfo` fields, `HeadbandSettings`, `MaxDeviceNameLength` and `DefaultCaptureQuality` |
| `data/ProfileRepository.kt` | 113 | `ProfileRepository.swift` (47) | Stale | 1, then 4 | Phase 1: `notifications`, `suggestName` and `MaxNameLength = 100` on the UserDefaults store. Phase 4: the per-account store, legacy import and cloud push |
| `data/SettingsRepository.kt` | 65 | `SettingsRepository.swift` (40) | Done | — | UserDefaults in place of DataStore |
| `data/UsageRepository.kt` | 67 | — | Missing | 7 | Daily count stored on the phone |
| `data/ble/BleClient.kt` | 39 | `BleClient.swift` (23) | Stale | 6 | `applySettings`, `updateFirmware` and `FirmwareStep` |
| `data/ble/NyaBleUuids.kt` | 9 | `NyaBleUuids.swift` (7) | Stale | 1 | Headband UUIDs and `NAME_PREFIX` moved into `BleConfig` |
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
| `ui/Icons.kt` | 88 | `Icons.swift` (53) | Stale | 1, then later | Phase 1: `Filter` becomes `Clear`. Other icons arrive with their screens |
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
| `viewmodel/AppViewModel.kt` | 81 | `AppViewModel.swift` (38) | Stale | 1 + 7 | Phase 1: `config` and `setNotifications`. Phase 7: `plan` |
| `viewmodel/AuthViewModel.kt` | 133 | `AuthViewModel.swift` (123) | Stale | 1 | Sets the name only for new accounts, otherwise calls `suggestName` |
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

**Totals for the 68 files in `main`:** 10 Done, 13 Stale, 6 Partial, 3 Stand-in and 36 Missing. The two `AppCheck.kt` files in the debug and release source sets are also Missing.

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

- **D1. App Check on the sideloaded iPhone build.** The `.ipa` is built in **Release**, and real attestation (App Attest) needs a paid Apple Developer account. The options are:
  - (a) *Recommended:* the CI-built `.ipa` uses the debug provider with the `APP_CHECK_DEBUG_TOKEN` secret built in. This works even once App Check is enforced. The token sits inside the `.ipa`, which is acceptable for a personal test build, and it can be revoked at any time. App Attest stays a marked TODO for the App Store build.
  - (b) Release uses App Attest only, and chat on the sideloaded app works only while AI Logic stays on Monitoring.
- **D2. Storage.** Use SwiftData (iOS 17, which matches `deploymentTarget`) as `ios-lead.md` says. It has no migrations, since no iOS users have data yet.

## Kotlin quirks to copy as they are (unless the owner says otherwise)

- (none recorded yet; added as they're found)

## Log

- Plan created on `ios-port` from `main` at `87b35d6`.
