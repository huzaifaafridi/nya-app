# Nya Android app
- Kotlin, Jetpack Compose, Material3, package com.nya.app, min SDK 26, Android Studio, Windows (use gradlew.bat).
- Screens live in app/src/main/java/com/nya/app/ui/. Navigation: enum Screen + Crossfade in MainActivity.kt.
- Theme: LocalNya (colors bg, sf, ln, tx, sb, ac, b1, b2, on) with NyaDark / NyaLight. Never hardcode colors that exist in the theme.
- Shared UI parts: NyaIcon/NyaIcons (Icons.kt), NyaChip/NyaThumb (Photos.kt), NyaOrb/IconBtn/bubbles (Parts.kt), Avatar2D.
- Rules: minimal clean code, no dead code or unused imports, no new dependencies unless asked, do not reformat unrelated files, keep both dark and light themes working.
- Data is fake for now (UserPref, fake replies). No backend yet.