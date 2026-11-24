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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.nanzbeatles.nanas.BuildConfig
import com.nanzbeatles.nanas.R
import com.nanzbeatles.nanas.constants.MiniPlayerHeight
import com.nanzbeatles.nanas.constants.NavigationBarHeight
import com.nanzbeatles.nanas.ui.utils.backToMain
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

        // 3. Large Hero Developer Video Card with Badges (Check + Fire + Wings) & Fire Animated Avatar Border
        DeveloperHeroVideoCard(
            developerName = "ɪᴋʏ ᴠᴏʀᴛᴇx | 技術者",
            developerRole = "Developer • Founder Of Vortex CORPORATION",
            videoUrl = "https://files.catbox.moe/1dntyj.mp4",
            avatarUrl = "https://files.catbox.moe/21bgwe.jpg"
        )

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
private fun DeveloperHeroVideoCard(
    developerName: String,
    developerRole: String,
    videoUrl: String,
    avatarUrl: String,
) {
    val context = LocalContext.current
    val videoUri = remember(videoUrl) { Uri.parse(videoUrl) }

    val exoPlayer = remember(videoUri) {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(15_000)

        val mediaSource = ProgressiveMediaSource.Factory(httpFactory)
            .createMediaSource(MediaItem.fromUri(videoUri))

        ExoPlayer.Builder(context).build().apply {
            setMediaSource(mediaSource)
            repeatMode = Player.REPEAT_MODE_ALL
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            volume = 0f
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.stop()
            exoPlayer.clearVideoSurface()
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(370.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xFF141820))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.16f),
                shape = RoundedCornerShape(26.dp)
            )
    ) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.22f),
                            Color.Black.copy(alpha = 0.88f)
                        ),
                        startY = 250f
                    )
                )
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp, bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = developerName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Verified Checkmark Badge
                    VerifiedCheckBadge(modifier = Modifier.size(19.dp))

                    // Animated Flame Badge
                    AnimatedFlameBadge(modifier = Modifier.size(20.dp))

                    // Glowing Wings / Phoenix Crest Badge
                    AnimatedWingsBadge(modifier = Modifier.size(23.dp))
                }

                Text(
                    text = developerRole,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Circular Profile Picture with Animated Burning Fire Aura Ring
            FireAnimatedAvatarRing(
                avatarUrl = avatarUrl,
                contentDescription = developerName
            )
        }
    }
}

@Composable
private fun FireAnimatedAvatarRing(
    avatarUrl: String,
    contentDescription: String,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fire_avatar_ring")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fire_spin"
    )
    val flickerPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fire_flicker"
    )

    Box(
        modifier = Modifier.size(88.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.minDimension / 2f

            // Outer ambient fire glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFF6D00).copy(alpha = 0.55f * flickerPulse.coerceAtMost(1f)),
                        Color(0xFFFF1744).copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = outerRadius
                ),
                radius = outerRadius,
                center = center
            )

            // Rotating multi-layered burning flame ring + ember sparks
            rotate(degrees = rotationAngle, pivot = center) {
                val ringRadius = outerRadius - 5.dp.toPx()
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFFFFEA00),
                            Color(0xFFFF9100),
                            Color(0xFFFF1744),
                            Color(0xFFD500F9),
                            Color(0xFF00E5FF),
                            Color(0xFFFF9100),
                            Color(0xFFFFEA00)
                        ),
                        center = center
                    ),
                    radius = ringRadius,
                    center = center,
                    style = Stroke(width = 3.6.dp.toPx(), cap = StrokeCap.Round)
                )

                // Orbiting flame sparks around the border
                for (i in 0 until 6) {
                    val angleRad = Math.toRadians((i * 60.0))
                    val sparkDist = ringRadius + (if (i % 2 == 0) 2.5.dp.toPx() else -1.dp.toPx())
                    val sx = center.x + (cos(angleRad) * sparkDist).toFloat()
                    val sy = center.y + (sin(angleRad) * sparkDist).toFloat()
                    drawCircle(
                        color = if (i % 2 == 0) Color(0xFFFFEA00) else Color(0xFFFF5252),
                        radius = (if (i % 2 == 0) 2.3.dp.toPx() else 1.7.dp.toPx()) * flickerPulse,
                        center = Offset(sx, sy)
                    )
                }
            }
        }

        // Inner circular profile image
        Surface(
            modifier = Modifier.size(72.dp),
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

@Composable
private fun AnimatedFlameBadge(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "flame_badge")
    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame_scale"
    )

    Canvas(
        modifier = modifier.graphicsLayer {
            scaleX = scalePulse
            scaleY = scalePulse
        }
    ) {
        val w = size.width
        val h = size.height

        val outerFlame = Path().apply {
            moveTo(w * 0.50f, h * 0.04f)
            cubicTo(w * 0.82f, h * 0.30f, w * 0.92f, h * 0.64f, w * 0.72f, h * 0.88f)
            cubicTo(w * 0.60f, h * 0.98f, w * 0.40f, h * 0.98f, w * 0.28f, h * 0.88f)
            cubicTo(w * 0.08f, h * 0.64f, w * 0.18f, h * 0.32f, w * 0.42f, h * 0.16f)
            cubicTo(w * 0.40f, h * 0.28f, w * 0.48f, h * 0.26f, w * 0.50f, h * 0.04f)
            close()
        }
        drawPath(
            path = outerFlame,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFEA00), Color(0xFFFF6D00), Color(0xFFFF1744))
            )
        )

        val innerCore = Path().apply {
            moveTo(w * 0.50f, h * 0.38f)
            cubicTo(w * 0.68f, h * 0.54f, w * 0.68f, h * 0.82f, w * 0.50f, h * 0.90f)
            cubicTo(w * 0.32f, h * 0.82f, w * 0.32f, h * 0.54f, w * 0.50f, h * 0.38f)
            close()
        }
        drawPath(
            path = innerCore,
            color = Color(0xFFFFF9C4)
        )
    }
}

@Composable
private fun AnimatedWingsBadge(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "wings_badge")
    val wingFlap by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wing_flap"
    )

    Canvas(
        modifier = modifier.graphicsLayer {
            rotationZ = wingFlap
        }
    ) {
        val w = size.width
        val h = size.height
        val wingBrush = Brush.linearGradient(
            colors = listOf(Color(0xFFFFF59D), Color(0xFFFFB300), Color(0xFFFF6F00))
        )

        // Left wing
        val leftWing = Path().apply {
            moveTo(w * 0.46f, h * 0.56f)
            cubicTo(w * 0.28f, h * 0.22f, w * 0.06f, h * 0.16f, w * 0.02f, h * 0.32f)
            cubicTo(w * 0.12f, h * 0.42f, w * 0.08f, h * 0.54f, w * 0.16f, h * 0.64f)
            cubicTo(w * 0.24f, h * 0.72f, w * 0.36f, h * 0.74f, w * 0.46f, h * 0.66f)
            close()
        }
        drawPath(path = leftWing, brush = wingBrush)

        // Right wing
        val rightWing = Path().apply {
            moveTo(w * 0.54f, h * 0.56f)
            cubicTo(w * 0.72f, h * 0.22f, w * 0.94f, h * 0.16f, w * 0.98f, h * 0.32f)
            cubicTo(w * 0.88f, h * 0.42f, w * 0.92f, h * 0.54f, w * 0.84f, h * 0.64f)
            cubicTo(w * 0.76f, h * 0.72f, w * 0.64f, h * 0.74f, w * 0.54f, h * 0.66f)
            close()
        }
        drawPath(path = rightWing, brush = wingBrush)

        // Center crest gem
        drawCircle(
            color = Color(0xFFFFFDE7),
            radius = w * 0.09f,
            center = Offset(w * 0.50f, h * 0.60f)
        )
    }
}
