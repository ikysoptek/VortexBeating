/**
 * Vortex Music - Tema "Pastel Notebook" (Soft Neobrutalism + Paper Scrapbook).
 *
 * - Kertas buku tulis bergaris krem + garis margin merah + lubang binder
 * - Kartu pastel dengan garis tinta navy tebal & bayangan keras (tanpa blur)
 * - Selotip washi biru pastel, coretan doodle (bintang, kilau, spiral)
 * - Font: Gloria Hallelujah (judul), Patrick Hand (lirik), Plus Jakarta Sans (isi), JetBrains Mono (angka)
 */
package com.ikyvortex.vortexmusic.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ikyvortex.vortexmusic.R
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object SoftPalette {
    val Ink = Color(0xFF16204F)
    val InkDark = Color(0xFF9DB4F0)

    val Paper = Color(0xFFFBF7EA)
    val PaperLine = Color(0xFFC9DBEE)
    val Margin = Color(0xFFEF8C98)

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

    val Blue = Color(0xFF2F5BD6)
    val Coral = Color(0xFFE8684A)
    val Tape = Color(0xFFA9CCE3)
    val TapeYellow = Color(0xFFF8DE8E)

    val Pastels = listOf(Yellow, Mint, SkyBlue, Lavender, Pink, Peach)
    val PastelsDark = listOf(
        Color(0xFF4A4220), Color(0xFF1F4231), Color(0xFF1F3A55),
        Color(0xFF3A2F5C), Color(0xFF5A2E3C), Color(0xFF55392A),
    )

    private val counter = AtomicInteger(0)

    /** Indeks pastel bergilir (kuning, mint, biru, lavender, pink, peach) untuk tiap kartu. */
    @Composable
    fun rememberPastelIndex(): Int = remember { counter.getAndIncrement() % Pastels.size }

    /** Warna pastel bergilir untuk tiap kartu. */
    @Composable
    fun rememberPastel(dark: Boolean): Color {
        val i = rememberPastelIndex()
        return if (dark) PastelsDark[i] else Pastels[i]
    }

    fun ink(dark: Boolean): Color = if (dark) InkDark else Ink
}

/** Judul besar / header halaman: Gloria Hallelujah (dipakai dengan FontWeight.Bold). */
val SoftDisplayFont: FontFamily = FontFamily(Font(R.font.gloria_hallelujah))

/** Lirik lagu / karaoke: Patrick Hand (dipakai dengan Bold / ExtraBold). */
val SoftLyricFont: FontFamily = FontFamily(Font(R.font.patrick_hand))

/** Judul lagu, nama kartu, artis, subjudul, menu: Plus Jakarta Sans. */
val SoftSansFont: FontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold),
)

/** Angka durasi, XP & label badge: JetBrains Mono Bold. */
val SoftMonoFont: FontFamily = FontFamily(Font(R.font.jetbrains_mono_bold, FontWeight.Bold))

/** Kompatibilitas: nama lama untuk font judul tulisan tangan. */
val SoftHandFont: FontFamily = SoftDisplayFont

/** True saat Dynamic Island memakai tema Pastel Notebook (pil biru muda + teks navy). */
val LocalIslandSoft = staticCompositionLocalOf { false }

// ---------------------------------------------------------------------------------------------
// Primitif gambar: kilau, bintang, spiral, selotip
// ---------------------------------------------------------------------------------------------

/** Kilau 4 titik (seperti ornamen di logo). */
fun DrawScope.softSparkle(c: Offset, r: Float, color: Color) {
    val k = r * 0.2f
    val p = Path().apply {
        moveTo(c.x, c.y - r)
        lineTo(c.x + k, c.y - k)
        lineTo(c.x + r, c.y)
        lineTo(c.x + k, c.y + k)
        lineTo(c.x, c.y + r)
        lineTo(c.x - k, c.y + k)
        lineTo(c.x - r, c.y)
        lineTo(c.x - k, c.y - k)
        close()
    }
    drawPath(p, color)
}

/** Bintang 5 sudut berupa garis coretan tangan. */
fun DrawScope.softStarOutline(c: Offset, r: Float, color: Color, stroke: Float) {
    val p = Path()
    for (i in 0 until 5) {
        val a = (-PI / 2 + i * 4 * PI / 5).toFloat()
        val x = c.x + r * cos(a)
        val y = c.y + r * sin(a)
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    drawPath(p, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
}

/** Spiral kecil coretan tangan. */
fun DrawScope.softSpiral(c: Offset, r: Float, color: Color, stroke: Float) {
    val p = Path()
    val turns = 2.6f
    val steps = 48
    for (i in 0..steps) {
        val t = i / steps.toFloat()
        val a = (t * turns * 2 * PI).toFloat()
        val rr = r * t
        val x = c.x + rr * cos(a)
        val y = c.y + rr * sin(a)
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    drawPath(p, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
}

/** Garis bergelombang (garis bawah coretan). */
fun DrawScope.softSquiggle(start: Offset, width: Float, amp: Float, color: Color, stroke: Float) {
    val p = Path()
    val steps = 24
    for (i in 0..steps) {
        val t = i / steps.toFloat()
        val x = start.x + width * t
        val y = start.y + amp * sin((t * 5 * PI).toFloat())
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    drawPath(p, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
}

/** Selotip washi: strip semi-transparan miring dengan ujung bergerigi & garis tipis. */
fun DrawScope.softWashiTape(center: Offset, w: Float, h: Float, angleDeg: Float, color: Color) {
    rotate(degrees = angleDeg, pivot = center) {
        val left = center.x - w / 2f
        val top = center.y - h / 2f
        val zig = h / 4f
        val p = Path().apply {
            moveTo(left, top)
            lineTo(left + w, top)
            lineTo(left + w - zig, top + h * 0.25f)
            lineTo(left + w, top + h * 0.5f)
            lineTo(left + w - zig, top + h * 0.75f)
            lineTo(left + w, top + h)
            lineTo(left, top + h)
            lineTo(left + zig, top + h * 0.75f)
            lineTo(left, top + h * 0.5f)
            lineTo(left + zig, top + h * 0.25f)
            close()
        }
        drawPath(p, color.copy(alpha = 0.82f))
        drawLine(
            Color.White.copy(alpha = 0.35f),
            Offset(left + zig * 2, top + h * 0.3f), Offset(left + w - zig * 2, top + h * 0.3f), h * 0.08f,
        )
        drawLine(
            Color.White.copy(alpha = 0.25f),
            Offset(left + zig * 2, top + h * 0.7f), Offset(left + w - zig * 2, top + h * 0.7f), h * 0.08f,
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Kertas buku bergaris
// ---------------------------------------------------------------------------------------------

/** Kertas buku bergaris + garis margin merah + lubang binder + coretan doodle halus. */
@Composable
fun SoftNotebookBackground(isDark: Boolean, modifier: Modifier = Modifier) {
    val paper = if (isDark) SoftPalette.PaperDark else SoftPalette.Paper
    val line = if (isDark) SoftPalette.PaperLineDark else SoftPalette.PaperLine
    val margin = if (isDark) SoftPalette.MarginDark else SoftPalette.Margin
    Box(modifier = modifier.fillMaxSize().background(paper)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Gradasi lembut di atas kertas supaya tidak terlalu flat
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        if (isDark) Color.White.copy(alpha = 0.03f) else Color.White.copy(alpha = 0.55f),
                        Color.Transparent,
                    ),
                    startY = 0f,
                    endY = 220.dp.toPx(),
                ),
                size = Size(size.width, 220.dp.toPx()),
            )
            // Garis kertas
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
            // Garis margin merah
            val mx = 24.dp.toPx()
            drawLine(
                color = margin,
                start = Offset(mx, 0f),
                end = Offset(mx, size.height),
                strokeWidth = 1.8.dp.toPx(),
            )
            // Lubang binder
            val holeFill = if (isDark) Color(0xFF0C1124) else Color(0xFFE9E2CD)
            val holeEdge = if (isDark) Color(0xFF2B3566) else Color(0xFFCFC6AB)
            listOf(0.17f, 0.50f, 0.83f).forEach { f ->
                val c = Offset(10.dp.toPx(), size.height * f)
                drawCircle(holeFill, radius = 5.dp.toPx(), center = c)
                drawCircle(holeEdge, radius = 5.dp.toPx(), center = c, style = Stroke(1.2.dp.toPx()))
            }
            // Doodle halus di pinggir kanan & kiri (tidak mengganggu teks)
            val a = if (isDark) 0.38f else 0.85f
            val blue = Color(0xFF9FCBF2).copy(alpha = a)
            val pink = Color(0xFFF6AFC0).copy(alpha = a)
            val gold = Color(0xFFF7CB5E).copy(alpha = a)
            val ink = SoftPalette.ink(isDark).copy(alpha = if (isDark) 0.25f else 0.28f)
            val w = size.width
            val h = size.height
            softSparkle(Offset(w * 0.93f, h * 0.07f), 11.dp.toPx(), pink)
            softSparkle(Offset(w * 0.86f, h * 0.045f), 6.dp.toPx(), gold)
            softSparkle(Offset(w * 0.95f, h * 0.30f), 8.dp.toPx(), blue)
            softStarOutline(Offset(w * 0.90f, h * 0.46f), 7.dp.toPx(), ink, 1.4.dp.toPx())
            softSpiral(Offset(w * 0.93f, h * 0.62f), 9.dp.toPx(), ink, 1.3.dp.toPx())
            softSparkle(Offset(w * 0.91f, h * 0.78f), 10.dp.toPx(), gold)
            softSparkle(Offset(w * 0.07f, h * 0.30f), 7.dp.toPx(), blue)
            softSparkle(Offset(w * 0.075f, h * 0.67f), 9.dp.toPx(), pink)
            softStarOutline(Offset(w * 0.08f, h * 0.93f), 7.dp.toPx(), ink, 1.4.dp.toPx())
            softSparkle(Offset(w * 0.88f, h * 0.95f), 7.dp.toPx(), blue)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Modifier Soft Neobrutalism
// ---------------------------------------------------------------------------------------------

/**
 * Soft Neobrutalism: border tinta tebal, sudut membulat, bayangan keras (tanpa blur) di kanan-bawah,
 * dan (opsional) selotip washi di tepi atas.
 */
fun Modifier.softNeo(
    fill: Color,
    ink: Color,
    corner: Dp = 22.dp,
    border: Dp = 3.dp,
    shadow: Dp = 5.dp,
    tape: Boolean = false,
    tapeColor: Color = SoftPalette.Tape,
    tapeAngle: Float = -3f,
): Modifier {
    val shape = RoundedCornerShape(corner)
    return this
        .drawBehind {
            val off = shadow.toPx()
            drawRoundRect(
                color = ink,
                topLeft = Offset(off, off),
                size = size,
                cornerRadius = CornerRadius(corner.toPx()),
            )
        }
        .drawWithContent {
            drawContent()
            if (tape && size.width > 130.dp.toPx() && size.height > 70.dp.toPx()) {
                softWashiTape(
                    center = Offset(size.width * 0.5f, 0f),
                    w = 46.dp.toPx(),
                    h = 14.dp.toPx(),
                    angleDeg = tapeAngle,
                    color = tapeColor,
                )
            }
        }
        .clip(shape)
        .background(fill, shape)
        .border(width = border, color = ink, shape = shape)
}
