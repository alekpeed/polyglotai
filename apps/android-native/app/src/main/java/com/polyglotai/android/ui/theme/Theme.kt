package com.polyglotai.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Indigo = Color(0xFF4B4BE0)
private val IndigoDark = Color(0xFF9A9AF5)

private val LightColors = lightColorScheme(
    primary = Indigo,
)
private val DarkColors = darkColorScheme(
    primary = IndigoDark,
)

@Composable
fun PolyglotTheme(useDark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (useDark) DarkColors else LightColors,
        content = content,
    )
}
