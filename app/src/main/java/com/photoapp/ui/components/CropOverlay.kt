package com.photoapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop169
import androidx.compose.material.icons.filled.Crop32
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

enum class AspectRatioPreset(val label: String, val ratio: Float?) {
    FREE("Free", null),
    SQUARE("1:1", 1f),
    RATIO_4_3("4:3", 4f / 3f),
    RATIO_16_9("16:9", 16f / 9f),
    RATIO_3_2("3:2", 3f / 2f)
}

@Composable
fun CropControls(
    selectedAspectRatio: AspectRatioPreset,
    onAspectRatioSelected: (AspectRatioPreset) -> Unit,
    onRotateLeft: () -> Unit,
    onRotateRight: () -> Unit,
    onFlipHorizontal: () -> Unit,
    onFlipVertical: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Aspect ratio chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AspectRatioPreset.entries.forEach { preset ->
                FilterChip(
                    selected = selectedAspectRatio == preset,
                    onClick = { onAspectRatioSelected(preset) },
                    label = {
                        Text(
                            text = preset.label,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Rotate / Flip buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onRotateLeft) {
                    Icon(
                        imageVector = Icons.Default.Rotate90DegreesCcw,
                        contentDescription = "Rotate Left",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Rotate L",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onRotateRight) {
                    Icon(
                        imageVector = Icons.Default.Rotate90DegreesCw,
                        contentDescription = "Rotate Right",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Rotate R",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onFlipHorizontal) {
                    Icon(
                        imageVector = Icons.Default.FlipCameraAndroid,
                        contentDescription = "Flip Horizontal",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Flip H",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onFlipVertical) {
                    Icon(
                        imageVector = Icons.Default.FlipCameraAndroid,
                        contentDescription = "Flip Vertical",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Flip V",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CropOverlay(
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        val strokeWidth = 2.dp.toPx()
        val cornerLength = 24.dp.toPx()
        val padding = 32.dp.toPx()

        val cropRect = Rect(
            left = padding,
            top = padding,
            right = size.width - padding,
            bottom = size.height - padding
        )

        // Dark overlay outside crop area
        // Top
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset.Zero,
            size = Size(size.width, cropRect.top)
        )
        // Bottom
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset(0f, cropRect.bottom),
            size = Size(size.width, size.height - cropRect.bottom)
        )
        // Left
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset(0f, cropRect.top),
            size = Size(cropRect.left, cropRect.height)
        )
        // Right
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset(cropRect.right, cropRect.top),
            size = Size(size.width - cropRect.right, cropRect.height)
        )

        // Rule of thirds grid
        val thirdWidth = cropRect.width / 3
        val thirdHeight = cropRect.height / 3

        for (i in 1..2) {
            // Vertical lines
            drawLine(
                color = Color.White.copy(alpha = 0.4f),
                start = Offset(cropRect.left + thirdWidth * i, cropRect.top),
                end = Offset(cropRect.left + thirdWidth * i, cropRect.bottom),
                strokeWidth = 1.dp.toPx()
            )
            // Horizontal lines
            drawLine(
                color = Color.White.copy(alpha = 0.4f),
                start = Offset(cropRect.left, cropRect.top + thirdHeight * i),
                end = Offset(cropRect.right, cropRect.top + thirdHeight * i),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Crop border
        drawRect(
            color = Color.White,
            topLeft = Offset(cropRect.left, cropRect.top),
            size = Size(cropRect.width, cropRect.height),
            style = Stroke(width = strokeWidth)
        )

        // Corner handles
        val corners = listOf(
            Offset(cropRect.left, cropRect.top),
            Offset(cropRect.right, cropRect.top),
            Offset(cropRect.left, cropRect.bottom),
            Offset(cropRect.right, cropRect.bottom)
        )

        corners.forEach { corner ->
            val isLeft = corner.x == cropRect.left
            val isTop = corner.y == cropRect.top

            // Horizontal part of corner
            drawLine(
                color = Color.White,
                start = corner,
                end = Offset(
                    corner.x + if (isLeft) cornerLength else -cornerLength,
                    corner.y
                ),
                strokeWidth = 3.dp.toPx()
            )
            // Vertical part of corner
            drawLine(
                color = Color.White,
                start = corner,
                end = Offset(
                    corner.x,
                    corner.y + if (isTop) cornerLength else -cornerLength
                ),
                strokeWidth = 3.dp.toPx()
            )
        }
    }
}
