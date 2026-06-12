package com.photoapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.photoapp.data.local.entities.PhotoEntity
import com.photoapp.ui.theme.FavoriteRed
import androidx.compose.runtime.remember

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoThumbnail(
    photo: PhotoEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.85f else 1f,
        animationSpec = tween(200),
        label = "thumbnailScale"
    )

    val context = LocalContext.current
    val isVideo = photo.mimeType.startsWith("video/")

    val imageRequest = remember(photo.contentUri, isVideo) {
        ImageRequest.Builder(context)
            .data(photo.contentUri)
            .apply {
                if (isVideo) {
                    decoderFactory(coil.decode.VideoFrameDecoder.Factory())
                }
            }
            .crossfade(true)
            .size(300)
            .build()
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(6.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        // Image / Video frame
        AsyncImage(
            model = imageRequest,
            contentDescription = photo.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Selection overlay
        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
            )
        }

        // Selection checkbox (Simple layout block to avoid layout/animation overhead during scroll)
        if (isSelectionMode) {
            Icon(
                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                contentDescription = if (isSelected) "Selected" else "Not selected",
                tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(24.dp)
                    .then(
                        if (isSelected) {
                            Modifier.background(Color.White, CircleShape)
                        } else {
                            Modifier
                                .border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                                .background(Color.Black.copy(alpha = 0.2f), CircleShape)
                        }
                    )
            )
        }

        // Play icon indicator for videos
        if (isVideo && !isSelectionMode) {
            Icon(
                imageVector = Icons.Filled.PlayCircle,
                contentDescription = "Video",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .size(20.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
            )
        }

        // Favorite heart icon (Simple layout block to avoid animation overhead during scroll)
        if (photo.isFavorite && !isSelectionMode) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "Favorite",
                tint = FavoriteRed,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(16.dp)
            )
        }

        // Bottom gradient for depth
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.15f)
                        ),
                        startY = 200f
                    )
                )
        )
    }
}
