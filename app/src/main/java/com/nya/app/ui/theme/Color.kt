package com.nya.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class NyaColors(
    val bg: Color, val sf: Color, val ln: Color, val tx: Color, val sb: Color,
    val ac: Color, val b1: Color, val b2: Color, val on: Color
)

val NyaDark = NyaColors(
    Color(0xFF0B0908), Color(0xFF161210), Color(0xFF2E2822), Color(0xFFF5EFE8), Color(0xFFB8AEA2),
    Color(0xFFE8A86E), Color(0xFFEDB07A), Color(0xFFC0733A), Color(0xFF1B0F05)
)
val NyaLight = NyaColors(
    Color(0xFFFBF7F2), Color(0xFFFFFFFF), Color(0xFFE6DCD0), Color(0xFF2A1D12), Color(0xFF6B5B4B),
    Color(0xFFA5581F), Color(0xFFAD5C22), Color(0xFF8C4216), Color(0xFFFFFFFF)
)

val LocalNya = staticCompositionLocalOf { NyaDark }