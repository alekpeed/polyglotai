package com.polyglotai.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.polyglotai.android.R

// The real faces from the desktop build (apps/desktop-tauri/src/assets/fonts), converted to ttf.
val Fraunces = FontFamily(Font(R.font.fraunces))
val PlexSans = FontFamily(Font(R.font.plex_sans))
val PlexMono = FontFamily(Font(R.font.plex_mono))
val NotoSerifJp = FontFamily(
    Font(R.font.noto_serif_jp_medium, FontWeight.Medium),
    Font(R.font.noto_serif_jp_bold, FontWeight.Bold),
)

/** The active display face — Fraunces in the default world, Noto Serif JP (mincho) under the ja
 *  pack. Consumed where a heading or numeral is set explicitly. */
val LocalDisplayFamily = staticCompositionLocalOf { Fraunces }

/**
 * Type roles mirroring the desktop app: IBM Plex Sans for body/titles, the [display] serif for
 * display + headlines, IBM Plex Mono for eyebrows and labels (the distinctive mono caps).
 */
fun polyTypography(display: FontFamily): Typography {
    val b = Typography()
    return b.copy(
        displayLarge = b.displayLarge.copy(fontFamily = display),
        displayMedium = b.displayMedium.copy(fontFamily = display),
        displaySmall = b.displaySmall.copy(fontFamily = display),
        headlineLarge = b.headlineLarge.copy(fontFamily = display),
        headlineMedium = b.headlineMedium.copy(fontFamily = display),
        headlineSmall = b.headlineSmall.copy(fontFamily = display),
        titleLarge = b.titleLarge.copy(fontFamily = display),
        titleMedium = b.titleMedium.copy(fontFamily = PlexSans),
        titleSmall = b.titleSmall.copy(fontFamily = PlexSans),
        bodyLarge = b.bodyLarge.copy(fontFamily = PlexSans),
        bodyMedium = b.bodyMedium.copy(fontFamily = PlexSans),
        bodySmall = b.bodySmall.copy(fontFamily = PlexSans),
        labelLarge = b.labelLarge.copy(fontFamily = PlexMono),
        labelMedium = b.labelMedium.copy(fontFamily = PlexMono),
        labelSmall = b.labelSmall.copy(fontFamily = PlexMono),
    )
}
