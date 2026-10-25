# Release checklist

Everything still needed for a production Play Store release (App Store later).

## Client accounts & Firebase
- [ ] Move the Firebase project to the client's account (US region) and create a new `google-services.json`
- [ ] Register SHA-1 and SHA-256 for the debug and release (Play App Signing) keys
- [ ] Turn on App Check with Play Integrity and enforce it for Firestore and the AI API
- [ ] Put Gemini behind a Cloud Function with server-side usage limits (limits are counted on the device today)
- [ ] Add Cloud Storage for media (memory photos/clips are device-only today)
- [ ] Republish `firestore.rules` to the client project

## Signing & build
- [ ] Create the upload key and enrol in Play App Signing
- [ ] Bump `versionCode` / `versionName` in `app/build.gradle.kts`
- [ ] Build the release bundle (R8 is on) and test it on a real device

## Play Console
- [ ] Store listing, screenshots, content rating
- [ ] Data safety form matching the final privacy policy
- [ ] Target audience 18+
- [ ] Privacy policy hosted at a public URL
- [ ] **Must-do:** in-app account deletion (Google Play requires it; the app has none yet), plus a web deletion link

## Legal
- [ ] Final privacy policy and terms text (in-app text in `ui/Legal.kt` is a placeholder)
- [ ] Set `legal.privacyUrl` / `legal.termsUrl` in appConfig (see `APP_CONFIG.md`)

## Billing
- [ ] Google Play Billing behind `BillingRepository` (fake today)
- [ ] Server-owned plan (verify purchases on the server, not on the device)

## Hardware
- [ ] Real BLE UUIDs and protocol
- [ ] `RealBleClient` implementation
- [ ] Bluetooth permissions (`BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT`) checked on Android 12+ and older

## Sign-in
- [ ] Apple and Facebook sign-in with account linking (buttons removed until then)

## Data
- [x] Room schema export on (`app/schemas`, from version 4)
- [ ] Schemas 1-3 were never exported and cannot be recovered; migrations from them are covered by unit tests only
- [ ] Before each release: commit the new `app/schemas/.../<version>.json` for any version change

## Content
- [ ] Production avatars and voices

## iOS
- [ ] iOS app (App Store listing, Sign in with Apple, privacy labels)
