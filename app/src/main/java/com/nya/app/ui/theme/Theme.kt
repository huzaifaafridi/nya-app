package com.nya.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun NyaTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val c = if (dark) NyaDark else NyaLight
    val scheme = if (dark) darkColorScheme(primary = c.ac, background = c.bg, surface = c.sf, onSurface = c.tx)
    else lightColorScheme(primary = c.ac, background = c.bg, surface = c.sf, onSurface = c.tx)
    CompositionLocalProvider(LocalNya provides c) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}