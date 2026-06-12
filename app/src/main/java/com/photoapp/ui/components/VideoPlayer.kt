package com.photoapp.ui.components

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import android.view.LayoutInflater
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.photoapp.R


private fun formatTime(ms: Long): String {
    val totalSecs = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSecs / 60
    val seconds = totalSecs % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Composable
private fun MiniIconButton(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    uri: Uri,
    showControls: Boolean,
    bottomBarHeightPx: Int,
    isActivePage: Boolean,
    onControllerVisibilityChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var playWhenReady by remember { mutableStateOf(false) }
    var isFirstFrameRendered by remember { mutableStateOf(false) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(uri)
            setMediaItem(mediaItem)
            prepare()
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                isFirstFrameRendered = true
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }

    // Synchronize play state with page active status (Auto-Play / Auto-Pause)
    LaunchedEffect(isActivePage) {
        if (isActivePage) {
            playWhenReady = true
            exoPlayer.playWhenReady = true
            exoPlayer.play()
        } else {
            playWhenReady = false
            isFirstFrameRendered = false // Reset rendering status when swiping away
            exoPlayer.playWhenReady = false
            exoPlayer.pause()
        }
    }

    // Poll the current position and playing status from the player
    LaunchedEffect(exoPlayer, playWhenReady) {
        if (playWhenReady) {
            while (true) {
                currentPosition = exoPlayer.currentPosition
                duration = exoPlayer.duration.coerceAtLeast(0L)
                isPlaying = exoPlayer.isPlaying
                isMuted = exoPlayer.volume == 0f
                kotlinx.coroutines.delay(200)
            }
        }
    }

    // Auto-hide controls when video is playing
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            kotlinx.coroutines.delay(3500)
            onControllerVisibilityChanged(false)
        }
    }

    val isVideoVisible = playWhenReady && isFirstFrameRendered

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 1. Base PlayerView (Always active to prevent black screen flashing/delays)
        AndroidView(
            factory = { ctx ->
                val view = LayoutInflater.from(ctx).inflate(R.layout.view_video_player, null)
                val playerView = view as PlayerView
                playerView.player = exoPlayer
                playerView
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = if (isVideoVisible) 1f else 0f)
        )

        // 2. Clickable transparent overlay to toggle controls (only visible during/after play initialization)
        if (playWhenReady) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onControllerVisibilityChanged(!showControls)
                    }
            )
        }

        // 3. Static Cover Thumbnail overlay (Only shown before player is loaded/ready)
        if (!isVideoVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        playWhenReady = true
                        exoPlayer.playWhenReady = true
                        exoPlayer.play()
                    },
                contentAlignment = Alignment.Center
            ) {
                // Video thumbnail
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(uri)
                        .decoderFactory(coil.decode.VideoFrameDecoder.Factory())
                        .crossfade(true)
                        .build(),
                    contentDescription = "Video Thumbnail",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                // Play icon overlay (compact 56.dp bubble)
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play Video",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        // 4. Custom compact floating video controls overlay
        AnimatedVisibility(
            visible = isVideoVisible && showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = with(LocalDensity.current) { bottomBarHeightPx.toDp() })
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Floating Pill controls (highly compact styling)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), shape = CircleShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    // Play/Pause button
                    MiniIconButton(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        onClick = {
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                            } else {
                                exoPlayer.play()
                            }
                            isPlaying = exoPlayer.isPlaying
                        }
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Time display
                    Text(
                        text = "${formatTime(currentPosition)}/${formatTime(duration)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Mute/Unmute button
                    MiniIconButton(
                        imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        onClick = {
                            val newVolume = if (isMuted) 1f else 0f
                            exoPlayer.volume = newVolume
                            isMuted = newVolume == 0f
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom compact seekbar Canvas (ultra thin, no extra padding/margins)
                var isDragging by remember { mutableStateOf(false) }
                var dragProgress by remember { mutableFloatStateOf(0f) }

                val progress = if (isDragging) dragProgress else {
                    if (duration > 0) currentPosition.toFloat() / duration else 0f
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .padding(horizontal = 24.dp)
                        .pointerInput(duration) {
                            detectTapGestures(
                                onTap = { offset ->
                                    if (duration > 0) {
                                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                        exoPlayer.seekTo((fraction * duration).toLong())
                                    }
                                }
                            )
                        }
                        .pointerInput(duration) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    isDragging = true
                                    dragProgress = (offset.x / size.width).coerceIn(0f, 1f)
                                },
                                onDragEnd = {
                                    isDragging = false
                                    exoPlayer.seekTo((dragProgress * duration).toLong())
                                },
                                onDragCancel = {
                                    isDragging = false
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val newProgress = (dragProgress + dragAmount.x / size.width).coerceIn(0f, 1f)
                                    dragProgress = newProgress
                                    exoPlayer.seekTo((newProgress * duration).toLong())
                                }
                            )
                        }
                ) {
                    val width = size.width
                    val height = size.height
                    val centerY = height / 2f

                    // Draw background track line
                    drawLine(
                        color = Color.White.copy(alpha = 0.3f),
                        start = Offset(0f, centerY),
                        end = Offset(width, centerY),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Draw progress track line
                    val progressX = width * progress
                    drawLine(
                        color = Color.White,
                        start = Offset(0f, centerY),
                        end = Offset(progressX, centerY),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Draw tiny thumb circle
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = Offset(progressX, centerY)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Pause playback when navigating away / scrolling
        DisposableEffect(Unit) {
            onDispose {
                exoPlayer.pause()
            }
        }
    }
}
