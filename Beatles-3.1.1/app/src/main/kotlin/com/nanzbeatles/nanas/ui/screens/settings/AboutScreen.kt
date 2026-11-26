/**
 * Vortex Music Project (C) 2026
 */

package com.nanzbeatles.nanas.ui.screens.settings

import android.net.Uri
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.nanzbeatles.nanas.BuildConfig
import com.nanzbeatles.nanas.R
import com.nanzbeatles.nanas.constants.MiniPlayerHeight
import com.nanzbeatles.nanas.constants.NavigationBarHeight
import com.nanzbeatles.nanas.streak.MusicJourneyManager
import com.nanzbeatles.nanas.streak.StreakTier
import com.nanzbeatles.nanas.ui.component.LevelEmblemIcon
import com.nanzbeatles.nanas.ui.component.StreakFlameIcon
import com.nanzbeatles.nanas.ui.utils.backToMain
import com.nanzbeatles.nanas.utils.DeveloperProfile
import com.nanzbeatles.nanas.utils.DeveloperProfileRepository
import kotlin.math.cos
import kotlin.math.sin

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val uriHandler = LocalUriHandler.current
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = if (isTablet) 48.dp else 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(92.dp))

        // 1. App Header (Logo + Styled Title "Vortex Music" + Version)
        val infiniteTransition = rememberInfiniteTransition(label = "icon_anim")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "icon_pulse"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(if (isTablet) 76.dp else 64.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(CircleShape)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.vortex_icon),
                    contentDescription = "Vortex Music Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(if (isTablet) 68.dp else 56.dp)
                        .clip(CircleShape)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column {
                Text(
                    text = "Vortex Music",
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${BuildConfig.VERSION_NAME} • ${BuildConfig.ARCHITECTURE}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2. Check for updates row
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable { navController.navigate("settings/updater") },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
            ),
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.update),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    text = stringResource(R.string.check_for_updates),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    painter = painterResource(R.drawable.arrow_forward),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // 3. Hero Developer Video Card: centang biru + Level 10 + Streak/Rekor 999
        val devProfile by DeveloperProfileRepository.state.collectAsState()
        LaunchedEffect(Unit) { DeveloperProfileRepository.pollWhileActive() }

        DeveloperHeroVideoCard(profile = devProfile)

        // 4. Social Section (WhatsApp Only)
        Text(
            text = "Social",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(top = 4.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ),
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable {
                        uriHandler.openUri("https://wa.me/6285835356933")
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF25D366).copy(alpha = 0.16f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            painter = painterResource(R.drawable.whatsapp),
                            contentDescription = "WhatsApp",
                            tint = Color(0xFF25D366),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(Modifier.width(14.dp))

                Text(
                    text = "WhatsApp",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "085835356933",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(MiniPlayerHeight + NavigationBarHeight + 24.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.about)) },
        navigationIcon = {
            com.nanzbeatles.nanas.ui.component.IconButton(
                onClick = { navController.navigateUp() },
                onLongClick = { navController.backToMain() },
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        }
    )
}

@OptIn(UnstableApi::class)
@Composable
private fun DeveloperHeroVideoCard(profile: DeveloperProfile) {
    val developerName = profile.name
    val avatarUrl = profile.avatarUrl
    val context = LocalContext.current

    // Video lokal (res/raw/developer_bg.mp4) diutamakan: langsung jalan tanpa internet.
    val localRawId = remember {
        context.resources.getIdentifier("developer_bg", "raw", context.packageName)
    }
    val useRemote = profile.backgroundIsVideo && profile.backgroundUrl.isNotBlank()
    val playVideo = localRawId != 0 || useRemote
    val videoUri = remember(profile.backgroundUrl, localRawId) {
        if (localRawId != 0) RawResourceDataSource.buildRawResourceUri(localRawId)
        else Uri.parse(profile.backgroundUrl)
    }

    val exoPlayer = remember(videoUri, playVideo) {
        if (!playVideo) return@remember null
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(15_000)
        // DefaultDataSource: http(s) lewat httpFactory, res/raw lewat RawResourceDataSource
        val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(videoUri))

        // Decoder fallback: kalau codec utama gagal, coba decoder lain
        val renderers = DefaultRenderersFactory(context).setEnableDecoderFallback(true)
        ExoPlayer.Builder(context, renderers).build().apply {
            setMediaSource(mediaSource)
            repeatMode = Player.REPEAT_MODE_ALL
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            volume = 0f
            playWhenReady = true
            prepare()
        }
    }

    // Status video (untuk diagnosis): null = lancar
    var videoStatus by remember(exoPlayer) { mutableStateOf<String?>(null) }
    DisposableEffect(exoPlayer) {
        var retried = false
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                videoStatus = if (playbackState == Player.STATE_BUFFERING) "Memuat video…" else null
            }

            override fun onPlayerError(error: PlaybackException) {
                videoStatus = "Video gagal: ${error.errorCodeName}"
                if (!retried) {
                    retried = true
                    exoPlayer?.prepare()
                }
            }
        }
        exoPlayer?.addListener(listener)
        onDispose {
            exoPlayer?.removeListener(listener)
            exoPlayer?.stop()
            exoPlayer?.clearVideoSurface()
            exoPlayer?.release()
        }
    }

    val shape = RoundedCornerShape(30.dp)
    val borderBrush = rememberRotatingBorderBrush()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .clip(shape)
            .background(Color(0xFF0B0D12))
            .border(width = 2.dp, brush = borderBrush, shape = shape)
    ) {
        // Latar: pp sebagai placeholder sebelum video siap (atau gambar jika bukan video)
        AsyncImage(
            model = if (playVideo) avatarUrl else profile.backgroundUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (exoPlayer != null) {
            AndroidView(
                factory = { ctx ->
                    TextureView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        exoPlayer.setVideoTextureView(this)
                    }
                },
                update = { textureView ->
                    exoPlayer.setVideoTextureView(textureView)
                    if (!exoPlayer.isPlaying) {
                        exoPlayer.playWhenReady = true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Peredam gelap di bawah supaya nama terbaca
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Black.copy(alpha = 0.18f),
                            0.45f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.88f)
                        )
                    )
                )
        )

        videoStatus?.let { status ->
            Text(
                text = status,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.75f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = developerName,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    // Hanya centang biru
                    VerifiedCheckBadge(modifier = Modifier.size(22.dp))
                }

                // "Developer": font serif miring + warna gradasi, dalam pil berborder gradasi
                val pillShape = RoundedCornerShape(50)
                Box(
                    modifier = Modifier
                        .clip(pillShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(
                            1.2.dp,
                            Brush.horizontalGradient(
                                listOf(Color(0xFF38BDF8), Color(0xFFA78BFA), Color(0xFFF472B6))
                            ),
                            pillShape
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Developer",
                        style = TextStyle(
                            fontFamily = FontFamily.Serif,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            letterSpacing = 1.5.sp,
                            brush = Brush.horizontalGradient(
                                listOf(Color(0xFF7DD3FC), Color(0xFFC4B5FD), Color(0xFFF9A8D4))
                            )
                        )
                    )
                }
            }

            MinimalAnimatedAvatarRing(
                avatarUrl = avatarUrl,
                contentDescription = developerName,
                ringSize = 116.dp
            )
        }
    }
}

/** Border kartu: garis gradasi tipis (biru -> ungu -> pink) yang berputar pelan. */
@Composable
private fun rememberRotatingBorderBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "dev_border")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dev_border_angle"
    )
    val colors = remember {
        intArrayOf(
            0x00FFFFFF,
            0xFF38BDF8.toInt(),
            0xFFA78BFA.toInt(),
            0xFFF472B6.toInt(),
            0x00FFFFFF,
        )
    }
    return remember(angle) {
        object : ShaderBrush() {
            override fun createShader(size: Size): Shader {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val sweep = android.graphics.SweepGradient(cx, cy, colors, null)
                val matrix = android.graphics.Matrix().apply { setRotate(angle, cx, cy) }
                sweep.setLocalMatrix(matrix)
                return sweep
            }
        }
    }
}

@Composable
private fun MinimalAnimatedAvatarRing(
    avatarUrl: String,
    contentDescription: String,
    ringSize: androidx.compose.ui.unit.Dp = 76.dp,
) {
    // Border tipis & halus: satu garis gradasi perak-biru yang berputar pelan
    val infiniteTransition = rememberInfiniteTransition(label = "avatar_ring")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "avatar_ring_spin"
    )

    Box(
        modifier = Modifier.size(ringSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f - 1.5.dp.toPx()
            rotate(degrees = rotationAngle, pivot = center) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.0f),
                            Color.White.copy(alpha = 0.95f),
                            Color(0xFF7DD3FC),
                            Color.White.copy(alpha = 0.0f),
                        ),
                        center = center
                    ),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        Surface(
            modifier = Modifier.size(ringSize - 12.dp),
            shape = CircleShape,
            color = Color(0xFF161922)
        ) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.account),
                error = painterResource(R.drawable.account),
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }
    }
}

@Composable
private fun VerifiedCheckBadge(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val points = 16
        val outerR = w * 0.48f
        val innerR = w * 0.39f
        val starPath = Path()

        for (i in 0 until points) {
            val r = if (i % 2 == 0) outerR else innerR
            val angle = Math.toRadians((i * 360.0 / points) - 90.0)
            val x = cx + (cos(angle) * r).toFloat()
            val y = cy + (sin(angle) * r).toFloat()
            if (i == 0) starPath.moveTo(x, y) else starPath.lineTo(x, y)
        }
        starPath.close()

        drawPath(
            path = starPath,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF38BDF8), Color(0xFF2563EB))
            )
        )

        val checkPath = Path().apply {
            moveTo(w * 0.30f, h * 0.52f)
            lineTo(w * 0.44f, h * 0.66f)
            lineTo(w * 0.71f, h * 0.36f)
        }
        drawPath(
            path = checkPath,
            color = Color.White,
            style = Stroke(width = w * 0.12f, cap = StrokeCap.Round)
        )
    }
}
