package com.polyglotai.android.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.text.input.VisualTransformation

/** The primary call-to-action. Champagne gold with dark-gold ink in the default world; the ai
 *  indigo under the ja pack (where kin gold stays a hairline/seal material). Flat 3px radius,
 *  no elevation — matches .btn-primary, not Material's default filled button. */
@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val c = LocalPolyColors.current
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(if (c.flat) 0.dp else 3.dp),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = c.ctaFill,
            contentColor = c.ctaInk,
            disabledContainerColor = c.surface,
            disabledContentColor = c.inkSoft,
        ),
        content = content,
    )
}

/** A secondary action — flat, thin-bordered, surfaceRaised fill. Matches the desktop app's base
 *  `button` rule, not Material's OutlinedButton (which is bolder-stroked and more rounded). */
@Composable
fun SecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val c = LocalPolyColors.current
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(if (c.flat) 0.dp else 3.dp),
        border = BorderStroke(1.dp, c.line),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = c.surfaceRaised,
            contentColor = c.ink,
            disabledContentColor = c.inkSoft,
        ),
        content = content,
    )
}

/** A flat bordered text input with the label set above the box, not Material's floating/notched
 *  label — matches the desktop app's plain `input { border: 1px solid line; border-radius: 4px }`. */
@Composable
fun PolyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val c = LocalPolyColors.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge.copy(fontFamily = PlexSans), fontWeight = FontWeight.SemiBold, color = c.ink)
        Box(
            Modifier
                .fillMaxWidth()
                .background(c.surfaceRaised, RoundedCornerShape(4.dp))
                .border(1.dp, c.line, RoundedCornerShape(4.dp))
                .padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                minLines = minLines,
                visualTransformation = visualTransformation,
                textStyle = LocalTextStyle.current.copy(color = c.ink, fontFamily = PlexSans, fontSize = 15.sp),
                cursorBrush = Brush.verticalGradient(listOf(c.indigo, c.indigo)),
                decorationBox = { inner ->
                    if (value.isEmpty() && placeholder != null) {
                        Text(placeholder, color = c.inkSoft, fontFamily = PlexSans, fontSize = 15.sp)
                    }
                    inner()
                },
            )
        }
    }
}

/** A mono-caps eyebrow label — .eyebrow: IBM Plex Mono, small, wide tracking, uppercase. */
@Composable
fun Eyebrow(text: String, color: Color = LocalPolyColors.current.inkSoft, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = color,
        letterSpacing = 1.5.sp,
        modifier = modifier,
    )
}

/** Hero surfaces (the periwinkle "due" card) carry the signature asymmetric corner — a big
 *  top-start curve in the default world, flattened to nothing in the ja pack. */
fun heroShape(c: PolyColors): Shape =
    RoundedCornerShape(topStart = c.heroTopStart, topEnd = 8.dp, bottomEnd = 8.dp, bottomStart = 8.dp)

/**
 * A periwinkle/navy hero surface with the desktop flourishes drawn behind the content: a gold
 * radial sunburst top-right, and — when [skyline] is set (the tall auth/onboarding hero) — three
 * rounded-top bars along the bottom in the pack's gold/paper/verde. In the flat ja world the corner
 * is squared and a 2px gold rule tops the card instead of the curve.
 */
@Composable
fun HeroBox(
    modifier: Modifier = Modifier,
    skyline: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = LocalPolyColors.current
    Box(
        modifier
            .clip(heroShape(c))
            .background(c.indigoFill)
            .drawBehind {
                val cx = size.width * 0.92f
                val cy = size.height * -0.06f
                val r = size.maxDimension * 0.75f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(c.gold.copy(alpha = 0.40f), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = r,
                    ),
                    radius = r,
                    center = Offset(cx, cy),
                )
                if (c.flat) {
                    drawRect(color = c.gold, size = Size(size.width, 2.dp.toPx()))
                }
                if (skyline) drawSkyline(c, size)
            },
        content = content,
    )
}

/** Texture word for the full-bleed onboarding/picker hero — ported verbatim from the desktop
 *  app's per-pack PACK_HERO_THEMES (position/size/rotation as fractions of the hero's own box). */
data class TextureWord(val text: String, val topFrac: Float, val leftFrac: Float, val sizeSp: Float, val rotateDeg: Float)

/**
 * The full-bleed hero used on the picker and per-language onboarding screens (.onboard-hero) — no
 * rounding, no clipping, fills the top of the screen and runs straight into the paper section
 * below. Carries the sunburst, an optional scattered word texture, and the skyline.
 */
@Composable
fun TextureHero(
    eyebrow: String,
    headline: String,
    body: String,
    modifier: Modifier = Modifier,
    textureWords: List<TextureWord> = emptyList(),
) {
    val c = LocalPolyColors.current
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .background(c.indigoFill)
            .drawBehind {
                val cx = size.width * 0.82f
                val cy = size.height * -0.18f
                val r = size.maxDimension * 0.85f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(c.gold.copy(alpha = 0.40f), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = r,
                    ),
                    radius = r,
                    center = Offset(cx, cy),
                )
                drawSkyline(c, size)
            }
            .padding(horizontal = 28.dp, vertical = 40.dp),
    ) {
        for (w in textureWords) {
            Text(
                w.text,
                fontFamily = LocalDisplayFamily.current,
                fontWeight = FontWeight.SemiBold,
                fontSize = w.sizeSp.sp,
                color = c.onFill.copy(alpha = 0.09f),
                maxLines = 1,
                modifier = Modifier
                    .offset(x = maxWidth * w.leftFrac, y = maxHeight * w.topFrac)
                    .graphicsLayer { rotationZ = w.rotateDeg },
            )
        }
        Column(Modifier.align(Alignment.CenterStart), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Eyebrow(eyebrow, color = c.onFill.copy(alpha = 0.65f))
            Text(
                headline,
                fontFamily = LocalDisplayFamily.current,
                fontWeight = FontWeight.Normal,
                fontSize = 44.sp,
                lineHeight = 48.sp,
                color = Color.White,
            )
            Text(
                body,
                fontFamily = PlexSans,
                fontSize = 15.sp,
                lineHeight = 23.sp,
                color = c.onFill.copy(alpha = 0.82f),
            )
        }
    }
}

private fun DrawScope.drawSkyline(c: PolyColors, size: Size) {
    val band = 88.dp.toPx()
    val barW = size.width / 3f
    val corner = CornerRadius(barW * 0.55f, barW * 0.55f)
    val bars = listOf(
        Triple(0f, 0.55f, c.goldFill.copy(alpha = 0.18f)),
        Triple(barW * 0.92f, 0.92f, c.onFill.copy(alpha = 0.09f)),
        Triple(barW * 1.92f, 0.42f, c.verdeFill.copy(alpha = 0.20f)),
    )
    for ((x, frac, col) in bars) {
        val h = band * frac
        drawRoundRect(
            color = col,
            topLeft = Offset(x, size.height - h),
            size = Size(barW * 1.16f, h + corner.y),
            cornerRadius = corner,
        )
    }
}

/**
 * A flat list row with a colored left accent border — .lang-picker-card. [isNew] switches the
 * accent from a solid indigo bar to a dashed gold one (unstarted languages).
 */
@Composable
fun FlatRow(
    onClick: () -> Unit,
    name: String,
    sub: String,
    modifier: Modifier = Modifier,
    isNew: Boolean = false,
) {
    val c = LocalPolyColors.current
    val accent = if (isNew) c.goldFill else c.indigoFill
    val shape = RoundedCornerShape(if (c.flat) 0.dp else 4.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surfaceRaised)
            .border(1.dp, c.line, shape)
            .drawBehind { drawRect(color = accent, size = Size(3.dp.toPx(), size.height)) }
            .clickableRipple(onClick)
            .padding(start = 20.dp, end = 16.dp, top = 15.dp, bottom = 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, fontFamily = LocalDisplayFamily.current, fontSize = 17.sp, color = c.ink)
        Text(sub, fontFamily = PlexMono, fontSize = 11.sp, color = c.inkSoft)
    }
}

/**
 * A tagged content-preview card — .shelf-card: a small colored pill, a serif heading, a
 * description. [tagIndex] cycles the pill through indigo → gold → verde (0,1,2,0,1,2…) exactly
 * like the CSS's nth-child(3n+2)/nth-child(3n) rule.
 */
@Composable
fun TagCard(
    tag: String,
    title: String,
    body: String,
    tagIndex: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val c = LocalPolyColors.current
    val (tagBg, tagInk) = when (tagIndex % 3) {
        0 -> c.indigoFill to c.onFill
        1 -> c.goldFill to c.goldInk
        else -> c.verdeFill to c.verdeInk
    }
    val shape = RoundedCornerShape(if (c.flat) 0.dp else 4.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surfaceRaised)
            .border(1.dp, c.line, shape)
            .then(if (onClick != null) Modifier.clickableRipple(onClick) else Modifier)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(tagBg)
                .padding(horizontal = 9.dp, vertical = 3.dp),
        ) {
            Text(tag.uppercase(), fontFamily = PlexMono, fontSize = 10.sp, letterSpacing = 0.8.sp, color = tagInk)
        }
        Text(title, fontFamily = LocalDisplayFamily.current, fontSize = 16.5.sp, color = c.ink)
        Text(body, fontFamily = PlexSans, fontSize = 13.sp, lineHeight = 19.sp, color = c.inkSoft)
    }
}

/** Section heading with a trailing link — .shelf-head (serif h3 + mono link on the right). */
@Composable
fun SectionHead(title: String, modifier: Modifier = Modifier, linkText: String? = null, onLinkClick: (() -> Unit)? = null) {
    val c = LocalPolyColors.current
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        Text(title, fontFamily = LocalDisplayFamily.current, fontSize = 19.sp, color = c.ink)
        if (linkText != null) {
            Text(
                linkText,
                fontFamily = PlexMono,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp,
                color = c.inkSoft,
                modifier = Modifier.then(if (onLinkClick != null) Modifier.clickableRipple(onLinkClick) else Modifier),
            )
        }
    }
}

/** Ripple-less plain clickable — the desktop app has no Material ripple anywhere; hover/press is a
 *  border-color change instead. Keeping touch feedback minimal matches that restraint. */
@Composable
private fun Modifier.clickableRipple(onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    return this.clickable(interactionSource = source, indication = null, onClick = onClick)
}

/** Faint 青海波 (seigaiha, "blue sea waves") texture behind ja screens — tiled concentric arcs at
 *  low opacity, matching the CSS stand-in pattern. A no-op when [color] is transparent. */
fun Modifier.seigaiha(color: Color): Modifier = drawBehind {
    if (color.alpha == 0f) return@drawBehind
    val unit = 44.dp.toPx()
    val stroke = 1.4.dp.toPx()
    var row = 0
    var y = 0f
    while (y < size.height + unit) {
        val shift = if (row % 2 == 0) 0f else unit
        var x = -unit + shift
        while (x < size.width + unit) {
            for (k in 1..3) {
                drawCircle(
                    color = color,
                    radius = unit * (0.42f + 0.19f * k),
                    center = Offset(x, y),
                    style = Stroke(width = stroke),
                )
            }
            x += unit
        }
        y += unit * 0.5f
        row++
    }
}
