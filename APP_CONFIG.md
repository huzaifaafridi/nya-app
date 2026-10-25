# Remote app config

Document: Firestore `appConfig/main` (public read, no writes from apps; write it with the Admin SDK).
The app listens live and Firestore keeps the last version on disk for offline starts.

**Never put secrets here** (API keys, tokens, passwords). Anyone can read this document.

**Fallback rule:** every field is optional. A missing, wrong-typed or out-of-range field falls back to its own
default (listed below); the other fields are still used. A missing document means all defaults.
Numbers may be integers or decimals.

| Field | Type | Default | Effect |
|---|---|---|---|
| `maintenance` | boolean | `false` | Blocks the app behind a maintenance screen |
| `maintenanceMessage` | string (1-200) | "Nya is getting an upgrade. Please check back soon." | Text on that screen |
| `minVersionCode` | number (>= 1) | `1` | Installed builds with a lower versionCode must update |
| `firmware.latest` | string (1-40) | `1.2.0` | Newest headband firmware offered |
| `ble.namePrefix` | string (1-40) | `NYA` | Name prefix of the headband |
| `ble.service`, `ble.storage`, `ble.firmware`, `ble.findMy`, `ble.transfer` | UUID string | placeholders | GATT identifiers; invalid ones are ignored. Placeholders until the firmware is final |
| `ai.models` | array of string | `gemini-3.8-flash`, `gemini-3.7-flash`, `gemini-3.5-flash-lite`, `gemini-3.1-flash-lite` | Models tried in order; invalid entries are skipped, empty list = default |
| `ai.systemPrompt` | string (1-4000) | Nya's identity and reply style | Start of the chat prompt; the app appends the companion personality, user name and mode |
| `companions.<Wanderer\|Muse\|Guide\|Yogi\|Creator>.trait` | string (1-200) | built-in trait | Personality phrase in the chat prompt |
| `companions.<name>.tagline` | string (1-200) | built-in tagline | Short description on the companion cards |
| `voiceStyles` | array of 1-6 `{name (1-40), femalePitch, malePitch}` | Calm, Warm, Bright, Deep | Voice choices; pitches 0.5-2.0. Any invalid entry or wrong count = whole default list. Keep the order stable: users save the position |
| `voiceRate` | number 0.5-2.0 | `0.85` | Speech speed |
| `plans.<free\|plus\|pro>.dailyMessages` | number (>= 0) | 30 / 200 / 1000 | Daily AI message limit per plan; 0 = unlimited. Counted on the device for now |
| `features.aiChat`, `voiceReplies`, `findMy`, `firmwareUpdates`, `share` | boolean | `true` | Turns a feature off |
| `legal.privacyUrl`, `legal.termsUrl` | string, http(s) URL | empty | Links to the policies; anything else is treated as empty. Settings and the sign-up text open the URL when set, otherwise the in-app placeholder text (`ui/Legal.kt`) |
