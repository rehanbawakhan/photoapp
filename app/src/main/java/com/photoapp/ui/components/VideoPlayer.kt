package com.photoapp.ui.components

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.util.Rational
import android.view.LayoutInflater
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import kotlin.math.abs
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.photoapp.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
            .size(40.dp)
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
            modifier = Modifier.size(22.dp)
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
    val activity = context as? Activity
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var duration by remember { mutableLongStateOf(0L) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var isMuted by remember { mutableStateOf(false) }
    var isVideoVisible by remember { mutableStateOf(false) }
    var hasPlaybackError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // PiP State
    var isInPipMode by remember { mutableStateOf(false) }

    LaunchedEffect(activity) {
        while (true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
                isInPipMode = activity.isInPictureInPictureMode
            }
            delay(250)
        }
    }

    // VLC Gesture HUD states
    var isLocked by remember { mutableStateOf(false) }
    var hudText by remember { mutableStateOf("") }
    var showHud by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }

    // Track selector dialog states
    var showTrackDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    // Double tap seek ripple state
    var seekRippleText by remember { mutableStateOf("") }
    var showSeekRipple by remember { mutableStateOf(false) }

    // Pinch-to-zoom & Pan states
    var videoScale by remember { mutableFloatStateOf(1f) }
    var videoOffset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var volumeAccumulator by remember { mutableFloatStateOf(0f) }

    fun enterPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
            try {
                val builder = PictureInPictureParams.Builder()
                val w = containerSize.width.coerceAtLeast(100)
                val h = containerSize.height.coerceAtLeast(100)
                val rawRatio = w.toFloat() / h.toFloat()
                val clampedRatio = rawRatio.coerceIn(0.42f, 2.38f)
                val num = (clampedRatio * 1000).toInt()
                val den = 1000
                builder.setAspectRatio(Rational(num, den))
                activity.enterPictureInPictureMode(builder.build())
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    activity.enterPictureInPictureMode()
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
    }

    // Manage player lifecycle when active page changes
    LaunchedEffect(isActivePage, uri) {
        if (!isActivePage) {
            exoPlayer?.pause()
            exoPlayer?.release()
            exoPlayer = null
            isVideoVisible = false
            hasPlaybackError = false
            activity?.let { act ->
                val lp = act.window.attributes
                lp.screenBrightness = android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                act.window.attributes = lp
            }
            return@LaunchedEffect
        }

        if (exoPlayer == null) {
            hasPlaybackError = false
            isVideoVisible = false

            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(2500, 15000, 1500, 2000)
                .build()

            val player = ExoPlayer.Builder(context)
                .setLoadControl(loadControl)
                .build()
                .apply {
                    val mediaItem = MediaItem.fromUri(uri)
                    setMediaItem(mediaItem)
                    repeatMode = Player.REPEAT_MODE_ONE
                    playWhenReady = true
                    prepare()
                }

            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        isVideoVisible = true
                        duration = player.duration.coerceAtLeast(0L)
                        isPlaying = player.isPlaying
                    }
                }

                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }

                override fun onPlayerError(error: PlaybackException) {
                    hasPlaybackError = true
                    errorMessage = error.localizedMessage ?: "Failed to play video"
                }
            })

            exoPlayer = player
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer?.release()
            exoPlayer = null
        }
    }

    // Sync position every 200ms
    LaunchedEffect(isPlaying, exoPlayer) {
        while (isPlaying) {
            exoPlayer?.let { p ->
                currentPosition = p.currentPosition.coerceAtLeast(0L)
                duration = p.duration.coerceAtLeast(0L)
            }
            delay(200)
        }
    }

    val coroutineScope = rememberCoroutineScope()
    fun showTemporaryHud(text: String) {
        hudText = text
        showHud = true
        coroutineScope.launch {
            delay(1200)
            showHud = false
        }
    }

    fun showTemporarySeek(text: String) {
        seekRippleText = text
        showSeekRipple = true
        coroutineScope.launch {
            delay(700)
            showSeekRipple = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // 1. Error Display
        if (hasPlaybackError) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(text = "⚠️", style = MaterialTheme.typography.displaySmall)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // 2. Base PlayerView
        val player = exoPlayer
        if (player != null) {
            AndroidView(
                factory = { ctx ->
                    val view = LayoutInflater.from(ctx).inflate(R.layout.view_video_player, null)
                    val playerView = view as PlayerView
                    playerView.player = player
                    playerView.resizeMode = if (isInPipMode) androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM else androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                    playerView
                },
                update = { playerView ->
                    playerView.player = exoPlayer
                    playerView.resizeMode = if (isInPipMode) androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM else androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = if (isInPipMode) 1f else videoScale,
                        scaleY = if (isInPipMode) 1f else videoScale,
                        translationX = if (isInPipMode) 0f else videoOffset.x,
                        translationY = if (isInPipMode) 0f else videoOffset.y,
                        alpha = if (isVideoVisible) 1f else 0f
                    )
            )
        }

        // 3. Gesture & VLC-style Controls Overlay
        if (!hasPlaybackError && !isInPipMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { containerSize = it }
                    .pointerInput(videoScale, isLocked) {
                        if (isLocked || videoScale <= 1.05f) return@pointerInput
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (videoScale * zoom).coerceIn(1f, 4f)
                            videoScale = newScale
                            if (newScale > 1f) {
                                val maxOffsetX = (containerSize.width * (newScale - 1f)) / 2f
                                val maxOffsetY = (containerSize.height * (newScale - 1f)) / 2f
                                videoOffset = Offset(
                                    x = (videoOffset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                    y = (videoOffset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                )
                            } else {
                                videoOffset = Offset.Zero
                            }
                        }
                    }
                    .pointerInput(showControls, isLocked) {
                        detectTapGestures(
                            onDoubleTap = { offset ->
                                if (isLocked) return@detectTapGestures
                                val width = containerSize.width.toFloat()
                                val p = exoPlayer ?: return@detectTapGestures
                                when {
                                    offset.x < width * 0.35f -> {
                                        p.seekTo((p.currentPosition - 10000).coerceAtLeast(0L))
                                        showTemporarySeek("-10s ⏪")
                                    }
                                    offset.x > width * 0.65f -> {
                                        p.seekTo((p.currentPosition + 10000).coerceAtMost(duration))
                                        showTemporarySeek("⏩ +10s")
                                    }
                                    else -> {
                                        if (videoScale > 1.2f) {
                                            videoScale = 1f
                                            videoOffset = Offset.Zero
                                        } else {
                                            videoScale = 2.0f
                                            videoOffset = Offset.Zero
                                        }
                                    }
                                }
                            },
                            onTap = {
                                if (!isLocked) {
                                    onControllerVisibilityChanged(!showControls)
                                }
                            }
                        )
                    }
                    .pointerInput(isLocked, videoScale) {
                        if (isLocked) return@pointerInput

                        if (videoScale > 1.2f) {
                            // When zoomed in, allow 2D panning
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val maxOffsetX = (containerSize.width * (videoScale - 1f)) / 2f
                                val maxOffsetY = (containerSize.height * (videoScale - 1f)) / 2f
                                videoOffset = Offset(
                                    x = (videoOffset.x + dragAmount.x).coerceIn(-maxOffsetX, maxOffsetX),
                                    y = (videoOffset.y + dragAmount.y).coerceIn(-maxOffsetY, maxOffsetY)
                                )
                            }
                        } else {
                            // Custom pointer scope: consume ONLY vertical drags for Brightness/Sound
                            // Let horizontal drags pass through completely so HorizontalPager can swipe next/prev video!
                            awaitPointerEventScope {
                                while (true) {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    var dragDirection = 0 // 0 = Unknown, 1 = Vertical, 2 = Horizontal
                                    val initialX = down.position.x
                                    val initialY = down.position.y
                                    var lastY = down.position.y

                                    do {
                                        val event = awaitPointerEvent()
                                        val changes = event.changes
                                        if (changes.size > 1) break
                                        val change = changes.firstOrNull { it.id == down.id } ?: break

                                        if (change.pressed) {
                                            val dx = abs(change.position.x - initialX)
                                            val dy = abs(change.position.y - initialY)

                                            if (dragDirection == 0 && (dx > 18f || dy > 18f)) {
                                                if (dy > dx * 1.3f) {
                                                    dragDirection = 1 // Vertical drag
                                                } else if (dx > dy * 1.3f) {
                                                    dragDirection = 2 // Horizontal drag
                                                }
                                            }

                                            if (dragDirection == 1) {
                                                change.consume()
                                                val width = containerSize.width.toFloat()
                                                val height = containerSize.height.toFloat()
                                                val deltaY = lastY - change.position.y

                                                if (width > 0f && height > 0f) {
                                                    if (initialX < width * 0.5f) {
                                                        // Left half: Brightness
                                                        activity?.let { act ->
                                                            val lp = act.window.attributes
                                                            val currentBrightness = if (lp.screenBrightness < 0f) 0.5f else lp.screenBrightness
                                                            val newBrightness = (currentBrightness + deltaY / (height * 0.5f)).coerceIn(0.05f, 1.0f)
                                                            lp.screenBrightness = newBrightness
                                                            act.window.attributes = lp
                                                            showTemporaryHud("☀️ Brightness: ${(newBrightness * 100).toInt()}%")
                                                        }
                                                    } else {
                                                        // Right half: Volume / Sound
                                                        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                                        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                                        val deltaVol = (deltaY / (height * 0.35f)) * maxVol.toFloat()
                                                        volumeAccumulator += deltaVol
                                                        if (abs(volumeAccumulator) >= 1.0f) {
                                                            val step = volumeAccumulator.toInt()
                                                            volumeAccumulator -= step.toFloat()
                                                            val newVol = (currentVol + step).coerceIn(0, maxVol)
                                                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                                            showTemporaryHud("🔊 Volume: ${(newVol * 100 / maxVol)}%")
                                                        }
                                                    }
                                                }
                                                lastY = change.position.y
                                            }
                                        }
                                    } while (event.changes.any { it.pressed })
                                }
                            }
                        }
                    }
            )
        }

        // 4. Poster Thumbnail while video initializes
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
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // 5. Gesture HUD Overlay (Brightness / Volume Text Pill)
        AnimatedVisibility(
            visible = showHud && !isInPipMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.8f),
                tonalElevation = 6.dp
            ) {
                Text(
                    text = hudText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }

        // 6. Double Tap Seek Ripple Text Indicator
        AnimatedVisibility(
            visible = showSeekRipple && !isInPipMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.75f)
            ) {
                Text(
                    text = seekRippleText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp)
                )
            }
        }

        // 7. Floating Lock Indicator (Shown only when controls are locked)
        AnimatedVisibility(
            visible = isLocked && !isInPipMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
        ) {
            IconButton(
                onClick = {
                    isLocked = false
                    onControllerVisibilityChanged(true)
                    showTemporaryHud("🔓 Controls Unlocked")
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.75f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Unlock Controls",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // 8. Player Controls Bar (Only when unlocked and showControls == true)
        AnimatedVisibility(
            visible = showControls && !isLocked && !isInPipMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 165.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                // Control Pill Container
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .background(Color.Black.copy(alpha = 0.85f), shape = CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    // Rewind 10s
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                val p = exoPlayer ?: return@clickable
                                p.seekTo((p.currentPosition - 10000).coerceAtLeast(0L))
                            }
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "-10s",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Play/Pause
                    MiniIconButton(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        onClick = {
                            val p = exoPlayer ?: return@MiniIconButton
                            if (p.isPlaying) p.pause() else p.play()
                            isPlaying = p.isPlaying
                        }
                    )

                    // Forward 10s
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                val p = exoPlayer ?: return@clickable
                                p.seekTo((p.currentPosition + 10000).coerceAtMost(duration))
                            }
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+10s",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = "${formatTime(currentPosition)} / ${formatTime(duration)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // Mute / Unmute
                    MiniIconButton(
                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        onClick = {
                            val p = exoPlayer ?: return@MiniIconButton
                            val newVolume = if (isMuted) 1f else 0f
                            p.volume = newVolume
                            isMuted = newVolume == 0f
                        }
                    )

                    // Speed button
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { showSpeedDialog = true }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${playbackSpeed}x",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD54F)
                        )
                    }

                    // Audio & Subtitle Tracks button
                    MiniIconButton(
                        imageVector = Icons.Default.Subtitles,
                        contentDescription = "Audio & Subtitles",
                        onClick = { showTrackDialog = true }
                    )

                    // Lock controls button
                    MiniIconButton(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = "Lock Controls",
                        onClick = {
                            isLocked = true
                            onControllerVisibilityChanged(false)
                            showTemporaryHud("🔒 Controls Locked")
                        }
                    )

                    // Picture-in-Picture (PiP) button
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        MiniIconButton(
                            imageVector = Icons.Default.PictureInPicture,
                            contentDescription = "Picture in Picture",
                            onClick = { enterPip() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // High-Contrast Seekbar Canvas
                var isDragging by remember { mutableStateOf(false) }
                var dragProgress by remember { mutableFloatStateOf(0f) }

                val progress = if (isDragging) dragProgress else {
                    if (duration > 0) currentPosition.toFloat() / duration else 0f
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .padding(horizontal = 16.dp)
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
                        color = Color.White.copy(alpha = 0.4f),
                        start = Offset(0f, centerY),
                        end = Offset(width, centerY),
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    val progressX = width * progress
                    drawLine(
                        color = Color(0xFFFF5252),
                        start = Offset(0f, centerY),
                        end = Offset(progressX, centerY),
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    drawCircle(
                        color = Color.White,
                        radius = 6.dp.toPx(),
                        center = Offset(progressX, centerY)
                    )
                    drawCircle(
                        color = Color(0xFFFF5252),
                        radius = 4.dp.toPx(),
                        center = Offset(progressX, centerY)
                    )
                }
            }
        }
    }

    // Playback Speed Selection Dialog
    if (showSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Playback Speed", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    playbackSpeed = speed
                                    exoPlayer?.playbackParameters = PlaybackParameters(speed)
                                    showSpeedDialog = false
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            RadioButton(
                                selected = playbackSpeed == speed,
                                onClick = {
                                    playbackSpeed = speed
                                    exoPlayer?.playbackParameters = PlaybackParameters(speed)
                                    showSpeedDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "${speed}x", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Audio & Subtitles Dialog
    if (showTrackDialog) {
        val playerInstance = exoPlayer
        AlertDialog(
            onDismissRequest = { showTrackDialog = false },
            title = { Text("Audio & Subtitle Tracks", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(text = "Audio Tracks", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "• Default Stereo Track", style = MaterialTheme.typography.bodyMedium)

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = "Subtitles", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                playerInstance?.trackSelectionParameters = playerInstance?.trackSelectionParameters
                                    ?.buildUpon()
                                    ?.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                    ?.build() ?: TrackSelectionParameters.DEFAULT_WITHOUT_CONTEXT
                                showTrackDialog = false
                            }
                            .padding(vertical = 6.dp)
                    ) {
                        Text("• Subtitles Enabled", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                playerInstance?.trackSelectionParameters = playerInstance?.trackSelectionParameters
                                    ?.buildUpon()
                                    ?.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                                    ?.build() ?: TrackSelectionParameters.DEFAULT_WITHOUT_CONTEXT
                                showTrackDialog = false
                            }
                            .padding(vertical = 6.dp)
                    ) {
                        Text("• Off", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTrackDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
