/**
 * Vortex Music Project (C) 2026
 */

package com.nanzbeatles.nanas.ui.screens.settings

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.nanzbeatles.nanas.BuildConfig
import com.nanzbeatles.nanas.R
import com.nanzbeatles.nanas.constants.MiniPlayerHeight
import com.nanzbeatles.nanas.constants.NavigationBarHeight
import com.nanzbeatles.nanas.ui.utils.backToMain

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val uriHandler = LocalUriHandler.current
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() > 0.5f

    val gradientColors = listOf(
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDarkTheme) 0.3f else 0.5f),
        MaterialTheme.colorScheme.surface
    )

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(gradientColors))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (isTablet) 48.dp else 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(104.dp))

            // App Header (Logo + Name + Version)
            val infiniteTransition = rememberInfiniteTransition(label = "icon_anim")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1400, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "icon_pulse"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isTablet) 84.dp else 68.dp)
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
                            .size(if (isTablet) 76.dp else 60.dp)
                            .clip(CircleShape)
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Vortex Music",
                        style = if (isTablet) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${BuildConfig.VERSION_NAME} • ${BuildConfig.ARCHITECTURE}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Version Badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = buildString {
                            append(BuildConfig.ARCHITECTURE.uppercase())
                            append(" • ")
                            append(
                                when {
                                    BuildConfig.CAST_AVAILABLE -> "GMS"
                                    BuildConfig.ARCHITECTURE != "universal" -> "STANDALONE"
                                    else -> "PRO"
                                }
                            )
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (BuildConfig.DEBUG) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = if (BuildConfig.DEBUG) "DEBUG" else "RELEASE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (BuildConfig.DEBUG) MaterialTheme.colorScheme.onErrorContainer
                        else MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            // ==================== Developer Profile Video Card (Catbox URLs) ====================
            DeveloperHeroVideoCard(
                developerName = "ɪᴋʏ ᴠᴏʀᴛᴇx | 技術者",
                developerRole = "Developer",
                videoUrl = "https://files.catbox.moe/1dntyj.mp4",
                avatarUrl = "https://files.catbox.moe/21bgwe.jpg"
            )

            // ==================== Social Section (WhatsApp Only) ====================
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
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
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
                        modifier = Modifier.size(42.dp)
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

            // Description Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    text = "Vortex Music adalah pemutar musik kaya fitur untuk Android yang menghadirkan pengalaman musik terbaik dengan dukungan streaming, pemutaran lokal, integrasi YouTube, dan banyak lagi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Extra bottom padding to avoid mini player blocking content
            Spacer(Modifier.height(MiniPlayerHeight + NavigationBarHeight + 24.dp))
        }
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
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(275.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF0E1015))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.16f),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        // Full-card Looping Video Background from Catbox URL
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Bottom dark scrim so white name & role text are crystal clear
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.25f),
                            Color.Black.copy(alpha = 0.82f)
                        ),
                        startY = 120f
                    )
                )
        )

        // Bottom Overlay: Left (Name + Developer) | Right (Circular Profile Picture)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = developerName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = developerRole,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.80f),
                    maxLines = 1
                )
            }

            // Circular Profile Avatar from Catbox URL on Bottom-Right
            Surface(
                modifier = Modifier.size(68.dp),
                shape = CircleShape,
                color = Color(0xFF161922),
                border = BorderStroke(2.dp, Color.White.copy(alpha = 0.85f)),
                shadowElevation = 6.dp
            ) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = developerName,
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
}

private fun Color.luminance(): Float {
    val r = red
    val g = green
    val b = blue
    return 0.299f * r + 0.587f * g + 0.114f * b
}
