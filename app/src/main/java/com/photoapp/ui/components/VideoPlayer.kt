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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
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

    // ExoPlayer is nullable — only allocated when this page is active
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    var playWhenReady by remember { mutableStateOf(false) }
    var isFirstFrameRendered by remember { mutableStateOf(false) }
    var hasPlaybackError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }

    // KEY LIFECYCLE FIX:
    // Create and prepare the player only when this page is active.
    // Fully RELEASE (not just pause) when page becomes inactive to free
    // hardware codec slots and native heap — critical for heavy HDR videos.
    LaunchedEffect(isActivePage, uri) {
        if (isActivePage) {
            hasPlaybackError = false
            isFirstFrameRendered = false
            playWhenReady = true

            // Build ExoPlayer with conservative buffer limits to prevent OOM on HDR content.
            // Default buffers can grow to 512MB+ for HDR — we cap at 32MB target per player.
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    /* minBufferMs */ 15_000,
                    /* maxBufferMs */ 30_000,
                    /* bufferForPlaybackMs */ 2_500,
                    /* bufferForPlaybackAfterRebufferMs */ 5_000
                )
                .setTargetBufferBytes(32 * 1024 * 1024) // 32 MB cap
                .build()

            val player = ExoPlayer.Builder(context)
                .setLoadControl(loadControl)
                .build()
                .apply {
                    setMediaItem(MediaItem.fromUri(uri))
                    prepare()
                    repeatMode = Player.REPEAT_MODE_ONE
                    playWhenReady = true
                    play()
                }

            player.addListener(object : Player.Listener {
                override fun onRenderedFirstFrame() {
                    isFirstFrameRendered = true
                }

                override fun onPlayerError(error: PlaybackException) {
                    hasPlaybackError = true
                    errorMessage = when (error.errorCode) {
                        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ->
                            "Could not initialize video decoder.\nThis format may not be supported."
                        PlaybackException.ERROR_CODE_DECODING_FAILED ->
                            "Video decoding failed.\nThe file may be corrupt or unsupported."
                        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                            "Video file not found."
                        else -> "Playback error (${error.errorCode})."
                    }
                }
            })

            exoPlayer = player
        } else {
            // Full release when not active — frees MediaCodec sessions and native memory
            playWhenReady = false
            isFirstFrameRendered = false
            isPlaying = false
            exoPlayer?.apply {
                stop()
                release()
            }
            exoPlayer = null
        }
    }

    // Ensure full cleanup if the composable is removed from composition entirely
    DisposableEffect(uri) {
        onDispose {
            exoPlayer?.apply {
                stop()
                release()
            }
            exoPlayer = null
        }
    }

    // Poll current position and playback state while active
    LaunchedEffect(exoPlayer, playWhenReady) {
        val player = exoPlayer
        if (player != null && playWhenReady) {
            while (true) {
                currentPosition = player.currentPosition
                duration = player.duration.coerceAtLeast(0L)
                isPlaying = player.isPlaying
                isMuted = player.volume == 0f
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

    val isVideoVisible = playWhenReady && isFirstFrameRendered && !hasPlaybackError

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 1. Playback error state — shown instead of crashing
        if (hasPlaybackError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = "⚠️",
                        style = MaterialTheme.typography.displaySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 2. Base PlayerView — only rendered when player is alive
        val player = exoPlayer
        if (player != null) {
            AndroidView(
                factory = { ctx ->
                    val view = LayoutInflater.from(ctx).inflate(R.layout.view_video_player, null)
                    val playerView = view as PlayerView
                    playerView.player = player
                    playerView
                },
                update = { playerView ->
                    // Re-attach player reference in case of recomposition
                    playerView.player = exoPlayer
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(alpha = if (isVideoVisible) 1f else 0f)
            )
        }

        // 3. Clickable overlay to toggle controls (only when playing)
        if (playWhenReady && !hasPlaybackError) {
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

        // 4. Poster Thumbnail — shown smoothly while video decoder initializes
        if (!isVideoVisible && !hasPlaybackError) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
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
            }
        }

        // 5. Custom compact floating video controls overlay
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), shape = CircleShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    MiniIconButton(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        onClick = {
                            val p = exoPlayer ?: return@MiniIconButton
                            if (p.isPlaying) p.pause() else p.play()
                            isPlaying = p.isPlaying
                        }
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "${formatTime(currentPosition)}/${formatTime(duration)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    MiniIconButton(
                        imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        onClick = {
                            val p = exoPlayer ?: return@MiniIconButton
                            val newVolume = if (isMuted) 1f else 0f
                            p.volume = newVolume
                            isMuted = newVolume == 0f
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom compact seekbar Canvas
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
                                    val p = exoPlayer ?: return@detectTapGestures
                                    if (duration > 0) {
                                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                        p.seekTo((fraction * duration).toLong())
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
                                    exoPlayer?.seekTo((dragProgress * duration).toLong())
                                },
                                onDragCancel = {
                                    isDragging = false
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val newProgress = (dragProgress + dragAmount.x / size.width).coerceIn(0f, 1f)
                                    dragProgress = newProgress
                                    exoPlayer?.seekTo((newProgress * duration).toLong())
                                }
                            )
                        }
                ) {
                    val width = size.width
                    val height = size.height
                    val centerY = height / 2f

                    drawLine(
                        color = Color.White.copy(alpha = 0.3f),
                        start = Offset(0f, centerY),
                        end = Offset(width, centerY),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    val progressX = width * progress
                    drawLine(
                        color = Color.White,
                        start = Offset(0f, centerY),
                        end = Offset(progressX, centerY),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = Offset(progressX, centerY)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
