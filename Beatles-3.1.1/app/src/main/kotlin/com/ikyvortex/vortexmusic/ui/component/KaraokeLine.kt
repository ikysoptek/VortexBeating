/**
 * Vortex Music Project (C) 2026
 */

package com.ikyvortex.vortexmusic.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextLayoutResult
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
    var layoutResult by remember(text) { mutableStateOf<TextLayoutResult?>(null) }

    Box(modifier = modifier) {
        // Lapisan dasar redup (teks yang belum dinyanyikan)
        Text(
            text = text,
            style = style.copy(color = brightColor.copy(alpha = dimAlpha)),
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { layoutResult = it }
        )

        // Lapisan terang: mengisi baris demi baris secara berurutan (baris 1 habis dulu baru baris 2)
        if (clampedFill > 0f) {
            Text(
                text = text,
                style = style.copy(color = brightColor),
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.drawWithContent {
                    val layout = layoutResult
                    if (layout == null || layout.lineCount <= 1) {
                        clipRect(right = size.width * clampedFill) {
                            this@drawWithContent.drawContent()
                        }
                    } else {
                        val lineCount = layout.lineCount
                        // Hitung bobot lebar tiap baris supaya kecepatan nyala konsisten dari baris 1 ke baris berikutnya
                        val lineWidths = FloatArray(lineCount) { i ->
                            (layout.getLineRight(i) - layout.getLineLeft(i)).coerceAtLeast(1f)
                        }
                        val totalWidth = lineWidths.sum().coerceAtLeast(1f)
                        var remainingPx = clampedFill * totalWidth

                        for (i in 0 until lineCount) {
                            if (remainingPx <= 0f) break
                            val lineW = lineWidths[i]
                            val lineStart = layout.getLineLeft(i)
                            val lineTop = layout.getLineTop(i)
                            val lineBottom = layout.getLineBottom(i)
                            val fillPx = remainingPx.coerceAtMost(lineW)

                            clipRect(
                                left = lineStart,
                                top = lineTop,
                                right = lineStart + fillPx,
                                bottom = lineBottom
                            ) {
                                this@drawWithContent.drawContent()
                            }
                            remainingPx -= lineW
                        }
                    }
                }
            )
        }
    }
}
