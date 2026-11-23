/**
 * Vortex Music Project (C) 2026
 */

package com.nanzbeatles.nanas.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.os.Bundle
import android.widget.RemoteViews
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import com.nanzbeatles.nanas.MainActivity
import com.nanzbeatles.nanas.R
import com.nanzbeatles.nanas.db.MusicDatabase
import com.nanzbeatles.nanas.ui.utils.resolveArtworkUrl
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NanzBeatlesWidgetManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase
) {
    private val imageLoader by lazy {
        ImageLoader.Builder(context)
            .crossfade(false)
            .build()
    }

    private var cachedArtworkUri: String? = null
    private var cachedAlbumArt: Bitmap? = null
    private var cachedGlassBackdropFull: Bitmap? = null
    private var cachedGlassBackdropWide: Bitmap? = null
    private var cachedGlassBackdropSquare: Bitmap? = null

    suspend fun updateWidgets(
        title: String,
        artist: String,
        artworkUri: String?,
        isPlaying: Boolean,
        isLiked: Boolean,
        duration: Long = 0,
        currentPosition: Long = 0
    ) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val normalizedArtworkUri = resolveArtworkUrl(artworkUri)

        val albumArt: Bitmap?

        if (normalizedArtworkUri != null && normalizedArtworkUri == cachedArtworkUri && cachedAlbumArt != null) {
            albumArt = cachedAlbumArt
        } else {
            albumArt = normalizedArtworkUri?.let { loadAlbumArt(it, 360) }
            cachedArtworkUri = normalizedArtworkUri
            cachedAlbumArt = albumArt
            cachedGlassBackdropFull = renderLiquidGlassBackdrop(albumArt, width = 640, height = 310, cornerRadius = 68f)
            cachedGlassBackdropWide = renderLiquidGlassBackdrop(albumArt, width = 680, height = 164, cornerRadius = 82f)
            cachedGlassBackdropSquare = renderLiquidGlassBackdrop(albumArt, width = 340, height = 340, cornerRadius = 72f)
        }

        val glassBackdropFull = cachedGlassBackdropFull
            ?: renderLiquidGlassBackdrop(albumArt, width = 640, height = 310, cornerRadius = 68f).also {
                cachedGlassBackdropFull = it
            }
        val glassBackdropWide = cachedGlassBackdropWide
            ?: renderLiquidGlassBackdrop(albumArt, width = 680, height = 164, cornerRadius = 82f).also {
                cachedGlassBackdropWide = it
            }
        val glassBackdropSquare = cachedGlassBackdropSquare
            ?: renderLiquidGlassBackdrop(albumArt, width = 340, height = 340, cornerRadius = 72f).also {
                cachedGlassBackdropSquare = it
            }

        // 1. Update main music player widgets
        val componentName = ComponentName(context, MusicWidgetReceiver::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
        if (widgetIds.isNotEmpty()) {
            widgetIds.forEach { widgetId ->
                val options = appWidgetManager.getAppWidgetOptions(widgetId)
                val views = createRemoteViewsForSize(
                    options,
                    title,
                    artist,
                    albumArt,
                    isPlaying,
                    isLiked,
                    duration,
                    currentPosition
                )
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }

        // 2. Update compact square widgets
        val compactSquareComponentName = ComponentName(context, CompactSquareWidgetReceiver::class.java)
        val compactSquareWidgetIds = appWidgetManager.getAppWidgetIds(compactSquareComponentName)
        if (compactSquareWidgetIds.isNotEmpty()) {
            val compactSquareViews = createCompactSquareRemoteViews(albumArt, isPlaying)
            compactSquareWidgetIds.forEach { widgetId ->
                appWidgetManager.updateAppWidget(widgetId, compactSquareViews)
            }
        }

        // 3. Update compact wide widgets
        val compactWideComponentName = ComponentName(context, CompactWideWidgetReceiver::class.java)
        val compactWideWidgetIds = appWidgetManager.getAppWidgetIds(compactWideComponentName)
        if (compactWideWidgetIds.isNotEmpty()) {
            val compactWideViews = createCompactWideRemoteViews(title, artist, albumArt, isPlaying, isLiked)
            compactWideWidgetIds.forEach { widgetId ->
                appWidgetManager.updateAppWidget(widgetId, compactWideViews)
            }
        }

        // 4. Update Liquid Glass Player (4x2 / Resizable) widgets
        val liquidGlassComponentName = ComponentName(context, LiquidGlassWidgetReceiver::class.java)
        val liquidGlassWidgetIds = appWidgetManager.getAppWidgetIds(liquidGlassComponentName)
        if (liquidGlassWidgetIds.isNotEmpty()) {
            liquidGlassWidgetIds.forEach { widgetId ->
                val options = appWidgetManager.getAppWidgetOptions(widgetId)
                val views = createLiquidGlassRemoteViewsForSize(
                    options = options,
                    title = title,
                    artist = artist,
                    albumArt = albumArt,
                    backdropFull = glassBackdropFull,
                    backdropWide = glassBackdropWide,
                    backdropSquare = glassBackdropSquare,
                    isPlaying = isPlaying,
                    isLiked = isLiked,
                    duration = duration,
                    currentPosition = currentPosition
                )
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }

        // 5. Update Liquid Glass Pill (4x1 Wide) widgets
        val liquidGlassWideComponentName = ComponentName(context, LiquidGlassWideWidgetReceiver::class.java)
        val liquidGlassWideWidgetIds = appWidgetManager.getAppWidgetIds(liquidGlassWideComponentName)
        if (liquidGlassWideWidgetIds.isNotEmpty()) {
            val wideViews = createLiquidGlassWideRemoteViews(
                title = title,
                artist = artist,
                albumArt = albumArt,
                backdropWide = glassBackdropWide,
                isPlaying = isPlaying,
                isLiked = isLiked,
                duration = duration,
                currentPosition = currentPosition
            )
            liquidGlassWideWidgetIds.forEach { widgetId ->
                appWidgetManager.updateAppWidget(widgetId, wideViews)
            }
        }
    }

    private fun createLiquidGlassRemoteViewsForSize(
        options: Bundle,
        title: String,
        artist: String,
        albumArt: Bitmap?,
        backdropFull: Bitmap,
        backdropWide: Bitmap,
        backdropSquare: Bitmap,
        isPlaying: Boolean,
        isLiked: Boolean,
        duration: Long,
        currentPosition: Long
    ): RemoteViews {
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)

        return when {
            minWidth in 1..179 && minHeight in 1..99 -> {
                createLiquidGlassSquareRemoteViews(title, albumArt, backdropSquare, isPlaying, duration, currentPosition)
            }
            minWidth >= 180 && minHeight in 1..89 -> {
                createLiquidGlassWideRemoteViews(title, artist, albumArt, backdropWide, isPlaying, isLiked, duration, currentPosition)
            }
            else -> {
                createLiquidGlassFullRemoteViews(title, artist, albumArt, backdropFull, isPlaying, isLiked, duration, currentPosition)
            }
        }
    }

    private fun createLiquidGlassFullRemoteViews(
        title: String,
        artist: String,
        albumArt: Bitmap?,
        backdropFull: Bitmap,
        isPlaying: Boolean,
        isLiked: Boolean,
        duration: Long,
        currentPosition: Long
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_liquid_glass)

        views.setImageViewBitmap(R.id.widget_glass_backdrop, backdropFull)
        views.setTextViewText(R.id.widget_glass_song_title, title)
        views.setTextViewText(R.id.widget_glass_artist_name, artist)

        val artBitmap = if (albumArt != null) {
            getRoundedCornerBitmap(albumArt, 48f)
        } else {
            getRoundedDefaultIcon(48f)
        }
        views.setImageViewBitmap(R.id.widget_glass_album_art, artBitmap)

        // Bright white solid circular Play/Pause button with pure black icon
        val playPauseIcon = if (isPlaying) R.drawable.ic_widget_pause_black else R.drawable.ic_widget_play_black
        views.setImageViewResource(R.id.widget_glass_play_pause, playPauseIcon)

        val likeIcon = if (isLiked) R.drawable.ic_widget_heart_nav else R.drawable.ic_widget_heart_outline_nav
        views.setImageViewResource(R.id.widget_glass_like_button, likeIcon)

        val progress = if (duration > 0) {
            ((currentPosition.toDouble() / duration.toDouble()) * 10000).toInt().coerceIn(0, 10000)
        } else 0
        views.setProgressBar(R.id.widget_glass_progress, 10000, progress, false)

        val openAppIntent = getOpenAppIntent()
        views.setOnClickPendingIntent(R.id.widget_glass_album_art, openAppIntent)
        views.setOnClickPendingIntent(R.id.widget_glass_open_app, openAppIntent)
        views.setOnClickPendingIntent(R.id.widget_glass_play_pause, getPlayPauseIntent())
        views.setOnClickPendingIntent(R.id.widget_glass_previous, getPreviousIntent())
        views.setOnClickPendingIntent(R.id.widget_glass_next, getNextIntent())
        views.setOnClickPendingIntent(R.id.widget_glass_like_button, getLikeIntent())

        return views
    }

    private fun createLiquidGlassWideRemoteViews(
        title: String,
        artist: String,
        albumArt: Bitmap?,
        backdropWide: Bitmap,
        isPlaying: Boolean,
        isLiked: Boolean,
        duration: Long,
        currentPosition: Long
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_liquid_glass_wide)

        views.setImageViewBitmap(R.id.widget_glass_wide_backdrop, backdropWide)
        views.setTextViewText(R.id.widget_glass_wide_song_title, title)
        views.setTextViewText(R.id.widget_glass_wide_artist_name, artist)

        val progressFraction = if (duration > 0) {
            (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        } else 0f

        val discBitmap = renderCircularGlassDisc(
            sourceArt = albumArt ?: getRoundedDefaultIcon(0f),
            progressFraction = progressFraction
        )
        views.setImageViewBitmap(R.id.widget_glass_wide_album_art, discBitmap)

        val playPauseIcon = if (isPlaying) R.drawable.ic_widget_pause_black else R.drawable.ic_widget_play_black
        views.setImageViewResource(R.id.widget_glass_wide_play_pause, playPauseIcon)

        val likeIcon = if (isLiked) R.drawable.ic_widget_heart_nav else R.drawable.ic_widget_heart_outline_nav
        views.setImageViewResource(R.id.widget_glass_wide_like_button, likeIcon)

        val progress = (progressFraction * 10000).toInt().coerceIn(0, 10000)
        views.setProgressBar(R.id.widget_glass_wide_progress, 10000, progress, false)

        views.setOnClickPendingIntent(R.id.widget_glass_wide_album_art, getOpenAppIntent())
        views.setOnClickPendingIntent(R.id.widget_glass_wide_play_pause, getPlayPauseIntent())
        views.setOnClickPendingIntent(R.id.widget_glass_wide_previous, getPreviousIntent())
        views.setOnClickPendingIntent(R.id.widget_glass_wide_next, getNextIntent())
        views.setOnClickPendingIntent(R.id.widget_glass_wide_like_button, getLikeIntent())

        return views
    }

    private fun createLiquidGlassSquareRemoteViews(
        title: String,
        albumArt: Bitmap?,
        backdropSquare: Bitmap,
        isPlaying: Boolean,
        duration: Long,
        currentPosition: Long
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_liquid_glass_square)

        views.setImageViewBitmap(R.id.widget_glass_square_backdrop, backdropSquare)
        views.setTextViewText(R.id.widget_glass_square_title, title)

        val progressFraction = if (duration > 0) {
            (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        } else 0f
        val discBitmap = renderCircularGlassDisc(
            sourceArt = albumArt ?: getRoundedDefaultIcon(0f),
            progressFraction = progressFraction
        )
        views.setImageViewBitmap(R.id.widget_glass_square_album_art, discBitmap)

        val playPauseIcon = if (isPlaying) R.drawable.ic_widget_pause_black else R.drawable.ic_widget_play_black
        views.setImageViewResource(R.id.widget_glass_square_play_pause, playPauseIcon)

        views.setOnClickPendingIntent(R.id.widget_glass_square_album_art, getOpenAppIntent())
        views.setOnClickPendingIntent(R.id.widget_glass_square_play_pause, getPlayPauseIntent())
        views.setOnClickPendingIntent(R.id.widget_glass_square_previous, getPreviousIntent())
        views.setOnClickPendingIntent(R.id.widget_glass_square_next, getNextIntent())

        return views
    }

    /**
     * Renders a vivid, bright 3D Liquid Glass backdrop that dynamically adapts to the
     * currently playing song's album artwork (high-visibility blurred cover + bright glass sheen).
     */
    private fun renderLiquidGlassBackdrop(
        albumArt: Bitmap?,
        width: Int,
        height: Int,
        cornerRadius: Float
    ): Bitmap {
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val inset = 3f
        val rect = RectF(inset, inset, width - inset, height - inset)
        val clipPath = Path().apply {
            addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
        }

        canvas.save()
        canvas.clipPath(clipPath)

        // 1. Base frosted glass gradient (bright translucent tint when no song is playing)
        val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(
                    Color.argb(145, 38, 48, 68),
                    Color.argb(120, 24, 34, 52),
                    Color.argb(140, 18, 24, 38)
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(rect, basePaint)

        // 2. Vivid & bright blurred album artwork layer (dynamically follows current song)
        if (albumArt != null) {
            try {
                // Multi-step smooth blur scaling so cover colors & shapes remain vibrant & recognizable
                val step1 = Bitmap.createScaledBitmap(albumArt, 64, 48, true)
                val step2 = Bitmap.createScaledBitmap(step1, 32, 24, true)
                val blurred = Bitmap.createScaledBitmap(step2, width, height, true)
                val artPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                    alpha = 215
                }
                canvas.drawBitmap(blurred, 0f, 0f, artPaint)
                if (blurred != step2) blurred.recycle()
                if (step2 != step1) step2.recycle()
                if (step1 != albumArt) step1.recycle()

                // Balanced readability scrim so white text & controls stay crisp without darkening the cover
                val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        0f, 0f, 0f, height.toFloat(),
                        intArrayOf(
                            Color.argb(36, 10, 14, 24),
                            Color.argb(58, 8, 12, 20),
                            Color.argb(92, 6, 10, 18)
                        ),
                        floatArrayOf(0f, 0.5f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(rect, scrimPaint)
            } catch (_: Exception) {
            }
        }

        // 3. Top bright curved glass specular reflection
        val topDomeHeight = height * 0.44f
        val topDomePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, inset, 0f, topDomeHeight,
                intArrayOf(
                    Color.argb(105, 255, 255, 255),
                    Color.argb(32, 255, 255, 255),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(inset, inset, width - inset, topDomeHeight, topDomePaint)

        // 4. Bottom crystal water caustic shimmer
        val causticTop = height * 0.65f
        val causticPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, causticTop, width.toFloat(), height.toFloat(),
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.argb(46, 255, 255, 255),
                    Color.argb(42, 140, 235, 255)
                ),
                floatArrayOf(0f, 0.6f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(inset, causticTop, width - inset, height - inset, causticPaint)

        // 5. Inner 3D bent-glass thickness ring
        val innerRimRect = RectF(inset + 3.5f, inset + 3.5f, width - inset - 3.5f, height - inset - 3.5f)
        val innerRimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(
                    Color.argb(75, 255, 255, 255),
                    Color.argb(32, 160, 240, 255),
                    Color.argb(18, 255, 255, 255),
                    Color.argb(55, 255, 255, 255)
                ),
                floatArrayOf(0f, 0.35f, 0.7f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(
            innerRimRect,
            (cornerRadius - 3.5f).coerceAtLeast(8f),
            (cornerRadius - 3.5f).coerceAtLeast(8f),
            innerRimPaint
        )

        canvas.restore()

        // 6. Gleaming Bright Crystal Rim Border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3.6f
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(
                    Color.argb(250, 255, 255, 255),
                    Color.argb(210, 165, 243, 252),
                    Color.argb(135, 255, 255, 255),
                    Color.argb(190, 224, 231, 255),
                    Color.argb(245, 255, 255, 255)
                ),
                floatArrayOf(0f, 0.28f, 0.52f, 0.78f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

        // 7. Top & bottom razor-sharp specular glint lines
        val glintWidth = width * 0.76f
        val glintStartX = (width - glintWidth) / 2f
        val topGlintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3.2f
            strokeCap = Paint.Cap.ROUND
            shader = LinearGradient(
                glintStartX, 0f, glintStartX + glintWidth, 0f,
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.argb(255, 255, 255, 255),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawLine(glintStartX, inset + 1.4f, glintStartX + glintWidth, inset + 1.4f, topGlintPaint)

        return output
    }

    private fun renderCircularGlassDisc(
        sourceArt: Bitmap,
        progressFraction: Float
    ): Bitmap {
        val size = 180
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val center = size / 2f

        val artInset = 14f
        val artDiameter = (size - artInset * 2).toInt()
        val squareArt = getRoundedCornerBitmap(sourceArt, artDiameter / 2f)
        val scaledArt = Bitmap.createScaledBitmap(squareArt, artDiameter, artDiameter, true)
        val artPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(scaledArt, artInset, artInset, artPaint)
        if (scaledArt != squareArt) scaledArt.recycle()
        if (squareArt != sourceArt) squareArt.recycle()

        val innerRimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.4f
            color = Color.argb(150, 255, 255, 255)
        }
        canvas.drawCircle(center, center, artDiameter / 2f, innerRimPaint)

        val strokeW = 8f
        val arcInset = strokeW / 2f + 2f
        val arcRect = RectF(arcInset, arcInset, size - arcInset, size - arcInset)
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokeW
            color = Color.argb(75, 255, 255, 255)
        }
        canvas.drawArc(arcRect, 0f, 360f, false, trackPaint)

        if (progressFraction > 0.005f) {
            val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = strokeW
                strokeCap = Paint.Cap.ROUND
                shader = LinearGradient(
                    0f, 0f, size.toFloat(), size.toFloat(),
                    intArrayOf(
                        Color.WHITE,
                        Color.rgb(125, 240, 255),
                        Color.WHITE
                    ),
                    floatArrayOf(0f, 0.55f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawArc(
                arcRect,
                -90f,
                (progressFraction * 360f).coerceIn(6f, 360f),
                false,
                progressPaint
            )
        }

        return output
    }

    private fun createRemoteViewsForSize(
        options: Bundle,
        title: String,
        artist: String,
        albumArt: Bitmap?,
        isPlaying: Boolean,
        isLiked: Boolean,
        duration: Long,
        currentPosition: Long
    ): RemoteViews {
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)

        return when {
            minWidth < 180 && minHeight < 100 -> {
                createCompactSquareRemoteViews(albumArt, isPlaying)
            }
            minWidth >= 180 && minHeight < 100 -> {
                createCompactWideRemoteViews(title, artist, albumArt, isPlaying, isLiked)
            }
            else -> {
                createRemoteViews(title, artist, albumArt, isPlaying, isLiked, duration, currentPosition)
            }
        }
    }

    private fun createRemoteViews(
        title: String,
        artist: String,
        albumArt: Bitmap?,
        isPlaying: Boolean,
        isLiked: Boolean,
        duration: Long = 0,
        currentPosition: Long = 0
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_music_player)

        views.setTextViewText(R.id.widget_song_title, title)
        views.setTextViewText(R.id.widget_artist_name, artist)

        if (albumArt != null) {
            val roundedAlbumArt = getRoundedCornerBitmap(albumArt, 48f)
            views.setImageViewBitmap(R.id.widget_album_art, roundedAlbumArt)
        } else {
            views.setImageViewBitmap(R.id.widget_album_art, getRoundedDefaultIcon(48f))
        }

        val playPauseIcon = if (isPlaying) R.drawable.ic_widget_pause_dark else R.drawable.ic_widget_play_dark
        views.setImageViewResource(R.id.widget_play_pause, playPauseIcon)

        val likeIcon = if (isLiked) R.drawable.ic_widget_heart_nav else R.drawable.ic_widget_heart_outline_nav
        views.setImageViewResource(R.id.widget_like_button, likeIcon)

        if (duration > 0) {
            val level = ((currentPosition.toDouble() / duration.toDouble()) * 10000).toInt()
            views.setInt(R.id.widget_progress_fill, "setImageLevel", level)
        } else {
            views.setInt(R.id.widget_progress_fill, "setImageLevel", 0)
        }

        views.setOnClickPendingIntent(R.id.widget_album_art, getOpenAppIntent())
        views.setOnClickPendingIntent(R.id.widget_play_pause, getPlayPauseIntent())
        views.setOnClickPendingIntent(R.id.widget_previous, getPreviousIntent())
        views.setOnClickPendingIntent(R.id.widget_next, getNextIntent())
        views.setOnClickPendingIntent(R.id.widget_like_button, getLikeIntent())

        return views
    }

    private suspend fun loadAlbumArt(artworkUri: String, size: Int = 360): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(artworkUri)
                    .size(size, size)
                    .allowHardware(false)
                    .crossfade(300)
                    .build()
                val result = imageLoader.execute(request)
                result.image?.toBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun getRoundedCornerBitmap(bitmap: Bitmap, cornerRadius: Float): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val xOffset = (bitmap.width - size) / 2
        val yOffset = (bitmap.height - size) / 2
        val squareBitmap = Bitmap.createBitmap(bitmap, xOffset, yOffset, size, size)

        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            shader = BitmapShader(squareBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

        if (squareBitmap != bitmap) {
            squareBitmap.recycle()
        }

        return output
    }

    private fun createCompactSquareRemoteViews(
        albumArt: Bitmap?,
        isPlaying: Boolean
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_compact_square)

        if (albumArt != null) {
            val roundedAlbumArt = getRoundedCornerBitmap(albumArt, 0f)
            views.setImageViewBitmap(R.id.widget_compact_album_art, roundedAlbumArt)
        } else {
            views.setImageViewBitmap(R.id.widget_compact_album_art, getRoundedDefaultIcon(0f))
        }

        val playPauseIcon = if (isPlaying) R.drawable.ic_widget_pause_low else R.drawable.ic_widget_play_low
        views.setImageViewResource(R.id.widget_compact_play_pause, playPauseIcon)

        views.setOnClickPendingIntent(R.id.widget_compact_album_art, getOpenAppIntent())
        views.setOnClickPendingIntent(R.id.widget_compact_play_container, getCompactSquarePlayPauseIntent())
        views.setOnClickPendingIntent(R.id.widget_compact_previous, getCompactSquarePreviousIntent())
        views.setOnClickPendingIntent(R.id.widget_compact_next, getCompactSquareNextIntent())

        return views
    }

    private fun createCompactWideRemoteViews(
        title: String,
        artist: String,
        albumArt: Bitmap?,
        isPlaying: Boolean,
        isLiked: Boolean
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_compact_wide)

        views.setTextViewText(R.id.widget_wide_song_title, title)
        views.setTextViewText(R.id.widget_wide_artist_name, artist)

        if (albumArt != null) {
            val roundedAlbumArt = getRoundedCornerBitmap(albumArt, 48f)
            views.setImageViewBitmap(R.id.widget_wide_album_art, roundedAlbumArt)
        } else {
            views.setImageViewBitmap(R.id.widget_wide_album_art, getRoundedDefaultIcon(48f))
        }

        val playPauseIcon = if (isPlaying) R.drawable.ic_widget_pause_low else R.drawable.ic_widget_play_low
        views.setImageViewResource(R.id.widget_wide_play_pause, playPauseIcon)

        val likeIcon = if (isLiked) R.drawable.ic_widget_heart_nav else R.drawable.ic_widget_heart_outline_nav
        views.setImageViewResource(R.id.widget_wide_like_button, likeIcon)

        views.setOnClickPendingIntent(R.id.widget_wide_album_art, getOpenAppIntent())
        views.setOnClickPendingIntent(R.id.widget_wide_play_pause, getPlayPauseIntent())
        views.setOnClickPendingIntent(R.id.widget_wide_like_button, getLikeIntent())

        return views
    }

    private fun getRoundedDefaultIcon(cornerRadius: Float): Bitmap {
        val drawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_widget_music_logo)
            ?: context.packageManager.getApplicationIcon(context.packageName)
        val size = 300
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        return getRoundedCornerBitmap(bitmap, cornerRadius)
    }

    private fun getOpenAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getPlayPauseIntent(): PendingIntent {
        val intent = Intent(context, MusicWidgetReceiver::class.java).apply {
            action = MusicWidgetReceiver.ACTION_PLAY_PAUSE
        }
        return PendingIntent.getBroadcast(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getLikeIntent(): PendingIntent {
        val intent = Intent(context, MusicWidgetReceiver::class.java).apply {
            action = MusicWidgetReceiver.ACTION_LIKE
        }
        return PendingIntent.getBroadcast(
            context,
            2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getPreviousIntent(): PendingIntent {
        val intent = Intent(context, MusicWidgetReceiver::class.java).apply {
            action = MusicWidgetReceiver.ACTION_PREVIOUS
        }
        return PendingIntent.getBroadcast(
            context,
            9,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getNextIntent(): PendingIntent {
        val intent = Intent(context, MusicWidgetReceiver::class.java).apply {
            action = MusicWidgetReceiver.ACTION_NEXT
        }
        return PendingIntent.getBroadcast(
            context,
            10,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getCompactSquarePlayPauseIntent(): PendingIntent {
        val intent = Intent(context, CompactSquareWidgetReceiver::class.java).apply {
            action = CompactSquareWidgetReceiver.ACTION_COMPACT_SQUARE_PLAY_PAUSE
        }
        return PendingIntent.getBroadcast(
            context,
            6,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getCompactWidePlayPauseIntent(): PendingIntent {
        val intent = Intent(context, CompactWideWidgetReceiver::class.java).apply {
            action = CompactWideWidgetReceiver.ACTION_COMPACT_WIDE_PLAY_PAUSE
        }
        return PendingIntent.getBroadcast(
            context,
            7,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getCompactWideLikeIntent(): PendingIntent {
        val intent = Intent(context, CompactWideWidgetReceiver::class.java).apply {
            action = CompactWideWidgetReceiver.ACTION_COMPACT_WIDE_LIKE
        }
        return PendingIntent.getBroadcast(
            context,
            8,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getCompactSquarePreviousIntent(): PendingIntent {
        val intent = Intent(context, CompactSquareWidgetReceiver::class.java).apply {
            action = CompactSquareWidgetReceiver.ACTION_COMPACT_SQUARE_PREVIOUS
        }
        return PendingIntent.getBroadcast(
            context,
            12,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getCompactSquareNextIntent(): PendingIntent {
        val intent = Intent(context, CompactSquareWidgetReceiver::class.java).apply {
            action = CompactSquareWidgetReceiver.ACTION_COMPACT_SQUARE_NEXT
        }
        return PendingIntent.getBroadcast(
            context,
            13,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
