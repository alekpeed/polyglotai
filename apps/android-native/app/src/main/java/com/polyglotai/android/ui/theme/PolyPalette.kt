package com.polyglotai.android.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Appearance override (Settings). SYSTEM follows the device; the rest pin a palette. Mirrors the
 *  desktop app's data-theme = light | dark | classic. */
enum class AppTheme(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark"), CLASSIC("Classic") }

/** Per-language visual world — the desktop app's data-pack. Japanese gets 藍と墨 (ai to sumi);
 *  every other language uses the default Brazil-derived palette. */
enum class Pack { DEFAULT, JA }

/** True design tokens ported verbatim from apps/desktop-tauri/src/App.css. These are the single
 *  source of truth for color — not Material's stock scheme. Names match the CSS custom properties. */
@Immutable
data class PolyColors(
    val ink: Color,
    val inkSoft: Color,
    val paper: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val line: Color,
    val gold: Color,
    val goldInk: Color,
    val indigo: Color,
    val indigoSoft: Color,
    val indigoFill: Color,
    val goldFill: Color,
    val verdeFill: Color,
    val verdeInk: Color,
    /** severity / register ramp — deliberately separate from the brand accent */
    val heat: List<Color>,
    val hasShadow: Boolean,
    /** big asymmetric top-start corner on hero surfaces (radius-curve). 0 for the flat ja world. */
    val heroTopStart: Dp,
    /** primary call-to-action fill + ink. Champagne gold in the default world; in ja the CTA moves
     *  to the ai indigo (kin gold is reserved for hairlines and the seal). */
    val ctaFill: Color,
    val ctaInk: Color,
    /** the flat washi world (ja): no shadow/curve, cards get a gold top rule, seigaiha behind. */
    val flat: Boolean,
    /** faint 青海波 wave tint for the ja background; transparent elsewhere. */
    val seigaiha: Color,
) {
    /** On the periwinkle/navy fill (sidebar, hero) text is always this near-white. */
    val onFill: Color get() = Color(0xFFF2F4F9)
}

// severity ramp — constant across every theme (defined once in :root)
private val Heat = listOf(
    Color(0xFF6B8F5E), Color(0xFF96923F), Color(0xFFC99A3E), Color(0xFFD9803D),
    Color(0xFFC05A3D), Color(0xFFA13A34), Color(0xFF7A2530),
)

private val DefaultLight = PolyColors(
    ink = Color(0xFF14181B), inkSoft = Color(0xFF4B5259),
    paper = Color(0xFFFAFAF7), surface = Color(0xFFEFF0EA), surfaceRaised = Color(0xFFFFFFFF),
    line = Color(0xFFDCDDD5), gold = Color(0xFFE2A431), goldInk = Color(0xFF5A3E0C),
    indigo = Color(0xFF263A66), indigoSoft = Color(0xFFEAEDF4), indigoFill = Color(0xFF7C93C9),
    goldFill = Color(0xFFEDD09C), verdeFill = Color(0xFF78DCA0), verdeInk = Color(0xFF0F4D28),
    heat = Heat, hasShadow = true, heroTopStart = 56.dp,
    ctaFill = Color(0xFFEDD09C), ctaInk = Color(0xFF5A3E0C), flat = false, seigaiha = Color.Transparent,
)

private val DefaultDark = PolyColors(
    ink = Color(0xFFF1EFE9), inkSoft = Color(0xFFB7BAB6),
    paper = Color(0xFF14171A), surface = Color(0xFF1D2124), surfaceRaised = Color(0xFF23272B),
    line = Color(0xFF33383C), gold = Color(0xFFE9B34D), goldInk = Color(0xFF2A1D06),
    indigo = Color(0xFF7C93C9), indigoSoft = Color(0xFF202A3E), indigoFill = Color(0xFF263A66),
    goldFill = Color(0xFFE9B34D), verdeFill = Color(0xFF1E8A4A), verdeInk = Color(0xFFEAFAF0),
    heat = Heat, hasShadow = true, heroTopStart = 56.dp,
    ctaFill = Color(0xFFE9B34D), ctaInk = Color(0xFF2A1D06), flat = false, seigaiha = Color.Transparent,
)

// Classic — light surfaces, but every -fill token pinned to its original richer shade.
private val Classic = DefaultLight.copy(
    indigoFill = Color(0xFF263A66), goldFill = Color(0xFFE2A431),
    verdeFill = Color(0xFF1E8A4A), verdeInk = Color(0xFFEAFAF0),
    ctaFill = Color(0xFFE2A431), ctaInk = Color(0xFF5A3E0C),
)

private val JaLight = PolyColors(
    ink = Color(0xFF1C1712), inkSoft = Color(0xFF574F43),
    paper = Color(0xFFE7DFCE), surface = Color(0xFFDDD3BD), surfaceRaised = Color(0xFFF2ECDD),
    line = Color(0xFFCFC4AC), gold = Color(0xFF927639), goldInk = Color(0xFF4A3B1A),
    indigo = Color(0xFF22384C), indigoSoft = Color(0xFFDFE6EA), indigoFill = Color(0xFF22384C),
    goldFill = Color(0xFFCDBB92), verdeFill = Color(0xFF22384C), verdeInk = Color(0xFFF2ECDD),
    heat = Heat, hasShadow = false, heroTopStart = 0.dp,
    ctaFill = Color(0xFF22384C), ctaInk = Color(0xFFF2ECDD), flat = true, seigaiha = Color(0x0D22384C),
)

private val JaDark = PolyColors(
    ink = Color(0xFFECE3D1), inkSoft = Color(0xFFA89E8B),
    paper = Color(0xFF14110C), surface = Color(0xFF100D09), surfaceRaised = Color(0xFF1D1913),
    line = Color(0xFF322C22), gold = Color(0xFFC2A15E), goldInk = Color(0xFFECE3D1),
    indigo = Color(0xFF8BB6D6), indigoSoft = Color(0xFF1C2830), indigoFill = Color(0xFF22384C),
    goldFill = Color(0xFF4A3F2C), verdeFill = Color(0xFF22384C), verdeInk = Color(0xFFF2ECDD),
    heat = Heat, hasShadow = false, heroTopStart = 0.dp,
    ctaFill = Color(0xFF22384C), ctaInk = Color(0xFFF2ECDD), flat = true, seigaiha = Color(0x0F8BB6D6),
)

/** Resolve the active token set from the appearance override, the language pack, and (for SYSTEM)
 *  the device's dark-mode flag — mirroring the CSS cascade of :root → theme → pack. */
fun resolvePolyColors(theme: AppTheme, pack: Pack, systemDark: Boolean): PolyColors {
    val dark = when (theme) {
        AppTheme.DARK -> true
        AppTheme.LIGHT, AppTheme.CLASSIC -> false
        AppTheme.SYSTEM -> systemDark
    }
    return when (pack) {
        Pack.JA -> if (dark) JaDark else JaLight
        Pack.DEFAULT -> when {
            dark -> DefaultDark
            theme == AppTheme.CLASSIC -> Classic
            else -> DefaultLight
        }
    }
}

/** The active language's pack world. Japanese pack ids ("ja", "ja-izakaya", …) get 藍と墨. */
fun packForId(packId: String?): Pack =
    if (packId != null && packId.startsWith("ja")) Pack.JA else Pack.DEFAULT

val LocalPolyColors = staticCompositionLocalOf { DefaultLight }
