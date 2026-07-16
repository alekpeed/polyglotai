package com.polyglotai.android.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.unit.dp

/** The primary call-to-action. Champagne gold with dark-gold ink in the default world; the ai
 *  indigo under the ja pack (where kin gold stays a hairline/seal material). */
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
        shape = RoundedCornerShape(if (c.flat) 0.dp else 6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = c.ctaFill,
            contentColor = c.ctaInk,
            disabledContainerColor = c.surface,
            disabledContentColor = c.inkSoft,
        ),
        content = content,
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
                // sunburst — a soft gold glow off the top-right corner
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
                    // flat washi world: a crisp gold top rule stands in for the curve
                    drawRect(color = c.gold, size = Size(size.width, 2.dp.toPx()))
                }
                if (skyline) {
                    val band = 88.dp.toPx()
                    val barW = size.width / 3f
                    val corner = CornerRadius(barW * 0.55f, barW * 0.55f)
                    // heights + colors from .onboard-skyline .a1/.a2/.a3
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
                            // extend below the clip so only the top corners round
                            size = Size(barW * 1.16f, h + corner.y),
                            cornerRadius = corner,
                        )
                    }
                }
            },
        content = content,
    )
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
