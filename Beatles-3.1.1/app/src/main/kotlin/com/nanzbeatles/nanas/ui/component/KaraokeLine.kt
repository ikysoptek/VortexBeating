/**
 * Vortex Music Project (C) 2026
 */

package com.nanzbeatles.nanas.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun KaraokeLine(
    text: String,
    fill: Float,
    style: TextStyle,
    modifier: Modifier = Modifier,
    dimAlpha: Float = 0.42f,
    brightColor: Color = Color.White,
    maxLines: Int = 3,
) {
    val clampedFill = fill.coerceIn(0f, 1f)
    Box(modifier = modifier) {
        // Lapisan dasar redup (teks yang belum dinyanyikan)
        Text(
            text = text,
            style = style.copy(color = brightColor.copy(alpha = dimAlpha)),
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
        // Lapisan terang yang terisi secara horizontal mengikuti progres vokal
        if (clampedFill > 0f) {
            Text(
                text = text,
                style = style.copy(color = brightColor),
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.drawWithContent {
                    clipRect(right = size.width * clampedFill) {
                        this@drawWithContent.drawContent()
                    }
                }
            )
        }
    }
}
