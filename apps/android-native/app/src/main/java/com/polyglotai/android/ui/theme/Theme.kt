package com.polyglotai.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

/**
 * The app's real design system, ported from apps/desktop-tauri/src/App.css. Resolves the token set
 * for the chosen appearance + language pack, exposes it as [LocalPolyColors], and maps it onto a
 * Material 3 scheme so stock components (cards, surfaces, fields) inherit the palette instead of
 * Material's generic defaults.
 */
@Composable
fun PolyglotTheme(
    theme: AppTheme = AppTheme.SYSTEM,
    pack: Pack = Pack.DEFAULT,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val c = resolvePolyColors(theme, pack, systemDark)
    val dark = when (theme) {
        AppTheme.DARK -> true
        AppTheme.LIGHT, AppTheme.CLASSIC -> false
        AppTheme.SYSTEM -> systemDark
    }

    // indigo is the interactive / text-emphasis accent; champagne gold is reserved for the primary
    // call-to-action (see PrimaryButton). Card + surface tokens all map to surfaceRaised so Material
    // cards read as raised paper on the paper background.
    val base = if (dark) darkColorScheme() else lightColorScheme()
    val scheme = base.copy(
        primary = c.indigo,
        onPrimary = c.onFill,
        primaryContainer = c.indigoSoft,
        onPrimaryContainer = c.indigo,
        secondary = c.gold,
        onSecondary = c.goldInk,
        secondaryContainer = c.goldFill,
        onSecondaryContainer = c.goldInk,
        tertiary = c.verdeFill,
        onTertiary = c.verdeInk,
        background = c.paper,
        onBackground = c.ink,
        surface = c.surfaceRaised,
        onSurface = c.ink,
        surfaceVariant = c.surface,
        onSurfaceVariant = c.inkSoft,
        surfaceContainerLowest = c.surfaceRaised,
        surfaceContainerLow = c.surfaceRaised,
        surfaceContainer = c.surfaceRaised,
        surfaceContainerHigh = c.surface,
        surfaceContainerHighest = c.surface,
        outline = c.line,
        outlineVariant = c.line,
        error = c.heat[5],
        onError = Color(0xFFFFFFFF),
        errorContainer = c.heat[5].copy(alpha = 0.14f),
        onErrorContainer = c.heat[5],
    )

    CompositionLocalProvider(LocalPolyColors provides c) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
