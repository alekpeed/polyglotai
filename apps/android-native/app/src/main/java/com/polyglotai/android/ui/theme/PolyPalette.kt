package com.polyglotai.android.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Appearance override (Settings). SYSTEM follows the device; the rest pin a palette. Mirrors the
 *  desktop app's data-theme = light | dark | classic. */
enum class AppTheme(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark"), CLASSIC("Classic") }

/** Per-language visual world — the desktop app's data-pack. Each pack that has a bespoke identity
 *  gets its own token set; everything else (pt-br + its micro-packs) uses the default Brazil-derived
 *  palette. The five worlds:
 *    DEFAULT — Brasil · Concreto & Ipê (warm paper, periwinkle-indigo, champagne gold, mint)
 *    JA      — 藍と墨 ai to sumi (washi + sumi ink, one ai-indigo accent, kin-gold hairlines, flat)
 *    FR      — Café de Paris (cream paper, near-black ink, bright terrace red, absinthe green)
 *    ES_MX   — Cal y Rosa (limewash paper, rosa mexicano magenta, jade; sunlit, generous rounding)
 *    RU      — two palette variants (see [PackVariant]): Gzhel (cobalt-on-porcelain, default) and
 *              Hermitage (salon green + gilt on warm marble) */
enum class Pack { DEFAULT, JA, FR, ES_MX, RU }

/** A named palette variant within a pack world. Most worlds expose a single look; RU offers two.
 *  DEFAULT is each world's primary palette; the rest are alternates the learner can pick in Settings.
 *  The chosen variant is a device preference (see SettingsStore) applied only to the world it belongs
 *  to — an unrelated pack falls back to its own DEFAULT. */
enum class PackVariant { DEFAULT, HERMITAGE }

/** A selectable variant and the label shown in the Settings picker. */
data class VariantOption(val variant: PackVariant, val label: String)

/** The palette variants a world offers, most-preferred first (first = default). Empty for worlds
 *  with a single look — Settings hides the picker in that case. */
fun variantsForPack(pack: Pack): List<VariantOption> = when (pack) {
    Pack.RU -> listOf(
        VariantOption(PackVariant.DEFAULT, "Gzhel"),
        VariantOption(PackVariant.HERMITAGE, "Hermitage"),
    )
    else -> emptyList()
}

/** True design tokens ported verbatim from apps/desktop-tauri/src/App.css. These are the single
 *  source of truth for color — not Material's stock scheme. Names match the CSS custom properties.
 *  The slot semantics (reused across every world): `indigo*` = the primary brand accent (text
 *  emphasis / soft tint / solid fill), `gold*` = the secondary accent (hairline + decorative color
 *  + a soft fill and its ink), `verde*` = the positive/"correct" accent. */
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
    /** big asymmetric top-start corner on hero surfaces (radius-curve). 0 for the flat worlds. */
    val heroTopStart: Dp,
    /** primary call-to-action fill + ink. Champagne gold in the default world; the bespoke worlds
     *  move the CTA onto their solid primary fill (like ja), with light ink. */
    val ctaFill: Color,
    val ctaInk: Color,
    /** the flat worlds (ja, ru): no shadow/curve, cards square off and heroes get a 2px secondary
     *  rule instead of the corner. */
    val flat: Boolean,
    /** faint background texture tint (ja's 青海波 waves). Transparent = no texture drawn. */
    val seigaiha: Color,
) {
    /** On the deep primary fill (sidebar, hero) text is always this near-white. */
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

// ---- FR · Café de Paris (black, red & cream). Warm cream paper + near-black bistro ink, a bold
//      bright terrace red as the primary accent + call-to-action, and absinthe green as the quiet
//      secondary / "correct" hue. Crisp editorial, soft-shadowed. (Dark is a derived first pass;
//      light leads.) The red is the whole point — bold, not muted.
private val FrLight = PolyColors(
    ink = Color(0xFF111112), inkSoft = Color(0xFF585858),
    paper = Color(0xFFF5F1E9), surface = Color(0xFFECE6D6), surfaceRaised = Color(0xFFFCF9F1),
    line = Color(0xFFE7E0D1), gold = Color(0xFF6F8A50), goldInk = Color(0xFF33421F),
    indigo = Color(0xFFA81810), indigoSoft = Color(0xFFFBD9D3), indigoFill = Color(0xFFCF2317),
    goldFill = Color(0xFFDBE4C8), verdeFill = Color(0xFFBCD39A), verdeInk = Color(0xFF33421F),
    heat = Heat, hasShadow = true, heroTopStart = 10.dp,
    ctaFill = Color(0xFFE42618), ctaInk = Color(0xFFFCF0EC), flat = false, seigaiha = Color.Transparent,
)

private val FrDark = PolyColors(
    ink = Color(0xFFF1ECE2), inkSoft = Color(0xFFB0AAA0),
    paper = Color(0xFF17140F), surface = Color(0xFF1F1B15), surfaceRaised = Color(0xFF241F18),
    line = Color(0xFF34302A), gold = Color(0xFF9FB579), goldInk = Color(0xFF141308),
    indigo = Color(0xFFF06A5E), indigoSoft = Color(0xFF2A1614), indigoFill = Color(0xFFCF2317),
    goldFill = Color(0xFF33421F), verdeFill = Color(0xFF5F8F4A), verdeInk = Color(0xFFEEF4E2),
    heat = Heat, hasShadow = true, heroTopStart = 10.dp,
    ctaFill = Color(0xFFE42618), ctaInk = Color(0xFFFCF0EC), flat = false, seigaiha = Color.Transparent,
)

// ---- ES_MX · Cal y Rosa (limewash & rose). Warm lime-washed sand paper + warm near-black ink, a
//      rosa mexicano magenta primary and a jade secondary. Sunlit and friendly: soft shadow,
//      generous corner. Two-hue system (magenta + jade/green) kept clean against the calm paper.
private val EsMxLight = PolyColors(
    ink = Color(0xFF2A1E1C), inkSoft = Color(0xFF6C5C55),
    paper = Color(0xFFF8F1E7), surface = Color(0xFFEFE6D5), surfaceRaised = Color(0xFFFFFDF7),
    line = Color(0xFFE5D9C5), gold = Color(0xFF0E8A7A), goldInk = Color(0xFF0A4A42),
    indigo = Color(0xFFC11B6B), indigoSoft = Color(0xFFFBE1EE), indigoFill = Color(0xFFA5165C),
    goldFill = Color(0xFFBFE3DC), verdeFill = Color(0xFFA9D89B), verdeInk = Color(0xFF1F5A2A),
    heat = Heat, hasShadow = true, heroTopStart = 40.dp,
    ctaFill = Color(0xFFA5165C), ctaInk = Color(0xFFFFF3F8), flat = false, seigaiha = Color.Transparent,
)

private val EsMxDark = PolyColors(
    ink = Color(0xFFF6EBE4), inkSoft = Color(0xFFC3AEA5),
    paper = Color(0xFF1A1210), surface = Color(0xFF221715), surfaceRaised = Color(0xFF271A17),
    line = Color(0xFF3A2A25), gold = Color(0xFF3FB5A2), goldInk = Color(0xFFF6EBE4),
    indigo = Color(0xFFE86BA3), indigoSoft = Color(0xFF331520), indigoFill = Color(0xFFA5165C),
    goldFill = Color(0xFF12463F), verdeFill = Color(0xFF4E9B57), verdeInk = Color(0xFFEAF7EA),
    heat = Heat, hasShadow = true, heroTopStart = 40.dp,
    ctaFill = Color(0xFFA5165C), ctaInk = Color(0xFFFFF3F8), flat = false, seigaiha = Color.Transparent,
)

// ---- RU · Gzhel (cobalt on porcelain) — the DEFAULT Russian world. Cool porcelain paper + deep
//      cobalt-navy ink, a hand-painted cobalt-blue primary and a warm gilt secondary. Soft, rounded,
//      lightly shadowed — glazed blue-and-white folk ceramic. verde is a quiet teal. Light leads.
private val RuGzhelLight = PolyColors(
    ink = Color(0xFF1B2C4D), inkSoft = Color(0xFF5C6D8F),
    paper = Color(0xFFEAEFF3), surface = Color(0xFFDDE6F2), surfaceRaised = Color(0xFFF8FAFB),
    line = Color(0xFFCBD7EF), gold = Color(0xFFB68A2E), goldInk = Color(0xFF5B4519),
    indigo = Color(0xFF2B4C9B), indigoSoft = Color(0xFFE6ECF7), indigoFill = Color(0xFF182F61),
    goldFill = Color(0xFFEFE2C2), verdeFill = Color(0xFFA9D8CC), verdeInk = Color(0xFF12463F),
    heat = Heat, hasShadow = true, heroTopStart = 26.dp,
    ctaFill = Color(0xFF182F61), ctaInk = Color(0xFFF4F7FB), flat = false, seigaiha = Color.Transparent,
)

private val RuGzhelDark = PolyColors(
    ink = Color(0xFFE7EDF6), inkSoft = Color(0xFF9FB0CC),
    paper = Color(0xFF0E1626), surface = Color(0xFF141F33), surfaceRaised = Color(0xFF17233B),
    line = Color(0xFF26324E), gold = Color(0xFFD8B768), goldInk = Color(0xFF17233B),
    indigo = Color(0xFF7EA0E0), indigoSoft = Color(0xFF172239), indigoFill = Color(0xFF2B4C9B),
    goldFill = Color(0xFF3A3018), verdeFill = Color(0xFF3E8F7A), verdeInk = Color(0xFFE8F5F0),
    heat = Heat, hasShadow = true, heroTopStart = 26.dp,
    ctaFill = Color(0xFF2B4C9B), ctaInk = Color(0xFFEAF1FB), flat = false, seigaiha = Color.Transparent,
)

// ---- RU · Hermitage (salon green + gilt) — the alternate Russian world. Warm marble paper + slate
//      ink, an imperial salon-green primary and a rich gilt secondary. Ornate and framed, softly
//      shadowed — a gilded museum hall. verde folds into the green family. Light leads.
private val RuHermitageLight = PolyColors(
    ink = Color(0xFF232830), inkSoft = Color(0xFF5C6168),
    paper = Color(0xFFF5F3EA), surface = Color(0xFFECE7D6), surfaceRaised = Color(0xFFFAF8F0),
    line = Color(0xFFDDD6C1), gold = Color(0xFFB48A34), goldInk = Color(0xFF5B4519),
    indigo = Color(0xFF3F7A73), indigoSoft = Color(0xFFE3F0ED), indigoFill = Color(0xFF2E6E67),
    goldFill = Color(0xFFECDCAE), verdeFill = Color(0xFFBCDCD6), verdeInk = Color(0xFF204A44),
    heat = Heat, hasShadow = true, heroTopStart = 12.dp,
    ctaFill = Color(0xFF2E6E67), ctaInk = Color(0xFFF4F2E8), flat = false, seigaiha = Color.Transparent,
)

private val RuHermitageDark = PolyColors(
    ink = Color(0xFFECEFE8), inkSoft = Color(0xFFA7AD9F),
    paper = Color(0xFF10130F), surface = Color(0xFF171A15), surfaceRaised = Color(0xFF1C211A),
    line = Color(0xFF2C322A), gold = Color(0xFFD8B768), goldInk = Color(0xFF1C211A),
    indigo = Color(0xFF5AA79E), indigoSoft = Color(0xFF16241F), indigoFill = Color(0xFF2E6E67),
    goldFill = Color(0xFF3A2F18), verdeFill = Color(0xFF3E8F6E), verdeInk = Color(0xFFEAF6F0),
    heat = Heat, hasShadow = true, heroTopStart = 12.dp,
    ctaFill = Color(0xFF2E6E67), ctaInk = Color(0xFFF4F2E8), flat = false, seigaiha = Color.Transparent,
)

/** Resolve the active token set from the appearance override, the language pack, and (for SYSTEM)
 *  the device's dark-mode flag — mirroring the CSS cascade of :root → theme → pack. CLASSIC is a
 *  default-world-only variant; the bespoke worlds treat it as their light palette. */
fun resolvePolyColors(
    theme: AppTheme,
    pack: Pack,
    systemDark: Boolean,
    variant: PackVariant = PackVariant.DEFAULT,
): PolyColors {
    val dark = when (theme) {
        AppTheme.DARK -> true
        AppTheme.LIGHT, AppTheme.CLASSIC -> false
        AppTheme.SYSTEM -> systemDark
    }
    return when (pack) {
        Pack.JA -> if (dark) JaDark else JaLight
        Pack.FR -> if (dark) FrDark else FrLight
        Pack.ES_MX -> if (dark) EsMxDark else EsMxLight
        Pack.RU -> when (variant) {
            PackVariant.HERMITAGE -> if (dark) RuHermitageDark else RuHermitageLight
            else -> if (dark) RuGzhelDark else RuGzhelLight
        }
        Pack.DEFAULT -> when {
            dark -> DefaultDark
            theme == AppTheme.CLASSIC -> Classic
            else -> DefaultLight
        }
    }
}

/** The active language's visual world, derived from the pack id (base packs and their micro-packs
 *  share a world: "ja"/"ja-izakaya" → JA, "ru"/"ru-banya" → RU, "es-mx"/"es-mx-…" → ES_MX). */
fun packForId(packId: String?): Pack = when {
    packId == null -> Pack.DEFAULT
    packId.startsWith("ja") -> Pack.JA
    packId.startsWith("fr") -> Pack.FR
    packId.startsWith("es-mx") -> Pack.ES_MX
    packId.startsWith("ru") -> Pack.RU
    else -> Pack.DEFAULT
}

val LocalPolyColors = staticCompositionLocalOf { DefaultLight }
