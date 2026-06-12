package com.photoapp.ui.components

import android.graphics.ColorMatrix as AndroidColorMatrix
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.photoapp.util.PhotoFilter

@Composable
fun FilterSelector(
    photoUri: String,
    selectedFilter: PhotoFilter,
    onFilterSelected: (PhotoFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PhotoFilter.entries.forEach { filter ->
            FilterItem(
                photoUri = photoUri,
                filter = filter,
                isSelected = filter == selectedFilter,
                onClick = { onFilterSelected(filter) }
            )
        }
    }
}

@Composable
private fun FilterItem(
    photoUri: String,
    filter: PhotoFilter,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            val colorMatrix = filter.matrix
            val composeMatrix = ColorMatrix(colorMatrix.array)

            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photoUri)
                    .crossfade(true)
                    .size(128)
                    .build(),
                contentDescription = filter.displayName,
                contentScale = ContentScale.Crop,
                colorFilter = if (filter != PhotoFilter.NONE) {
                    ColorFilter.colorMatrix(composeMatrix)
                } else null,
                modifier = Modifier.size(64.dp)
            )
        }

        Text(
            text = filter.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
