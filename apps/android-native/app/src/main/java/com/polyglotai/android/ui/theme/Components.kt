package com.polyglotai.android.ui.theme

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** The primary call-to-action: champagne gold with dark-gold ink, matching the desktop app's
 *  .btn-primary. Distinct from Material's `primary` (indigo, used for accents/links). */
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
        shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = c.goldFill,
            contentColor = c.goldInk,
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
