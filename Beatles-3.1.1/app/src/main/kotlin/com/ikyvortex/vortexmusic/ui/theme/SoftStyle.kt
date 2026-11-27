/**
 * Vortex Music - Tema "Pastel Notebook" (Soft Neobrutalism):
 * kertas buku bergaris, kartu pastel, garis tinta navy tebal, bayangan keras.
 */
package com.ikyvortex.vortexmusic.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import java.util.concurrent.atomic.AtomicInteger

object SoftPalette {
    val Ink = Color(0xFF16204F)
    val InkDark = Color(0xFF9DB4F0)

    val Paper = Color(0xFFF6F8FC)
    val PaperLine = Color(0xFFD3E1F0)
    val Margin = Color(0xFFF2A3AE)

    val PaperDark = Color(0xFF131A33)
    val PaperLineDark = Color(0xFF212B4D)
    val MarginDark = Color(0xFF7A4560)

    val Yellow = Color(0xFFFFEFB0)
    val Mint = Color(0xFFCFEFD3)
    val SkyBlue = Color(0xFFCDE5F7)
    val Lavender = Color(0xFFE2D5F5)
    val Pink = Color(0xFFFFD3DB)
    val Peach = Color(0xFFFFDCC2)
    val Teal = Color(0xFFA8D8DA)
    val Cream = Color(0xFFFFF6D6)

    val Pastels = listOf(Yellow, Mint, SkyBlue, Lavender, Pink, Peach)
    val PastelsDark = listOf(
        Color(0xFF4A4220), Color(0xFF1F4231), Color(0xFF1F3A55),
        Color(0xFF3A2F5C), Color(0xFF5A2E3C), Color(0xFF55392A),
    )

    private val counter = AtomicInteger(0)

    /** Warna pastel bergilir (kuning, mint, biru, lavender, pink, peach) untuk tiap kartu. */
    @Composable
    fun rememberPastel(dark: Boolean): Color {
        val i = remember { counter.getAndIncrement() }
        val list = if (dark) PastelsDark else Pastels
        return list[i % list.size]
    }

    fun ink(dark: Boolean): Color = if (dark) InkDark else Ink
}

/** Font tulisan tangan bawaan Android ("casual") untuk judul besar. */
val SoftHandFont: FontFamily = FontFamily(
    android.graphics.Typeface.create("casual", android.graphics.Typeface.NORMAL)
)

/** Kertas buku bergaris + garis margin merah di kiri. */
@Composable
fun SoftNotebookBackground(isDark: Boolean, modifier: Modifier = Modifier) {
    val paper = if (isDark) SoftPalette.PaperDark else SoftPalette.Paper
    val line = if (isDark) SoftPalette.PaperLineDark else SoftPalette.PaperLine
    val margin = if (isDark) SoftPalette.MarginDark else SoftPalette.Margin
    Box(modifier = modifier.fillMaxSize().background(paper)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 30.dp.toPx()
            var y = step
            while (y < size.height) {
                drawLine(
                    color = line,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.2.dp.toPx(),
                )
                y += step
            }
            val mx = 22.dp.toPx()
            drawLine(
                color = margin,
                start = Offset(mx, 0f),
                end = Offset(mx, size.height),
                strokeWidth = 1.6.dp.toPx(),
            )
        }
    }
}
