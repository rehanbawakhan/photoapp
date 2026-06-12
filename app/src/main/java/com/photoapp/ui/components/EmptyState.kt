package com.photoapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

enum class EmptyStateType(
    val icon: ImageVector,
    val title: String,
    val subtitle: String
) {
    PHOTOS(
        icon = Icons.Outlined.PhotoLibrary,
        title = "No photos found",
        subtitle = "Take some photos to see them here"
    ),
    VIDEOS(
        icon = Icons.Outlined.Videocam,
        title = "No videos found",
        subtitle = "Videos on your device will appear here"
    ),
    ALBUMS(
        icon = Icons.Outlined.Collections,
        title = "No albums yet",
        subtitle = "Your photo folders will appear here"
    ),
    FAVORITES(
        icon = Icons.Outlined.FavoriteBorder,
        title = "No favorites yet",
        subtitle = "Tap the heart icon on photos you love"
    ),
    TRASH(
        icon = Icons.Outlined.Delete,
        title = "Trash is empty",
        subtitle = "Deleted photos will appear here for 30 days"
    )
}

@Composable
fun EmptyState(
    type: EmptyStateType,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(48.dp)
    ) {
        Icon(
            imageVector = type.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = type.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = type.subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
