package com.photoapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectDragGestures
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.photoapp.data.local.entities.PhotoEntity
import com.photoapp.util.DateUtils
import kotlinx.coroutines.delay

data class PhotoGroup(
    val label: String,
    val photos: List<PhotoEntity>
)

private val dayFormatSameYear = SimpleDateFormat("d MMM", Locale.getDefault())
private val dayFormatDiffYear = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
private val monthYearFormatShort = SimpleDateFormat("MMM yyyy", Locale.getDefault())

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun isYesterday(now: Calendar, date: Calendar): Boolean {
    val yesterday = Calendar.getInstance().apply {
        timeInMillis = now.timeInMillis
        add(Calendar.DAY_OF_YEAR, -1)
    }
    return isSameDay(yesterday, date)
}

fun getDayGroupLabel(timestamp: Long): String {
    val now = Calendar.getInstance()
    val date = Calendar.getInstance().apply { timeInMillis = timestamp }
    
    return when {
        isSameDay(now, date) -> "Today"
        isYesterday(now, date) -> "Yesterday"
        now.get(Calendar.YEAR) == date.get(Calendar.YEAR) -> dayFormatSameYear.format(Date(timestamp))
        else -> dayFormatDiffYear.format(Date(timestamp))
    }
}

fun getMonthGroupLabel(timestamp: Long): String {
    return monthYearFormatShort.format(Date(timestamp))
}

fun groupPhotosByDate(photos: List<PhotoEntity>, columns: Int): List<PhotoGroup> {
    return if (columns <= 4) {
        photos
            .groupBy { getDayGroupLabel(it.dateTaken) }
            .map { (label, photos) -> PhotoGroup(label, photos) }
    } else {
        photos
            .groupBy { getMonthGroupLabel(it.dateTaken) }
            .map { (label, photos) -> PhotoGroup(label, photos) }
    }
}

@Composable
fun PhotoGrid(
    photos: List<PhotoEntity>,
    selectedIds: Set<Long>,
    isSelectionMode: Boolean,
    onPhotoClick: (PhotoEntity) -> Unit,
    onPhotoLongClick: (PhotoEntity) -> Unit,
    groupByDate: Boolean = true,
    modifier: Modifier = Modifier,
    columns: Int = 4
) {
    val gridState = rememberLazyGridState()
    var gridColumns by rememberSaveable { mutableIntStateOf(columns) }

    // Gesture detection for pinch-to-zoom columns (runs in Initial pass to override scrolling)
    val zoomModifier = Modifier.pointerInput(gridColumns) {
        awaitPointerEventScope {
            var accumulatedZoom = 1f
            var isZooming = false

            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val changeList = event.changes

                if (changeList.size >= 2) {
                    isZooming = true
                    // Consume touch changes so the grid doesn't scroll during a pinch zoom
                    changeList.forEach { it.consume() }

                    val p1 = changeList[0].position
                    val p2 = changeList[1].position
                    val currentDist = (p1 - p2).getDistance()

                    val prevP1 = changeList[0].previousPosition
                    val prevP2 = changeList[1].previousPosition
                    val prevDist = (prevP1 - prevP2).getDistance()

                    if (prevDist > 0f && currentDist > 0f) {
                        val zoom = currentDist / prevDist
                        accumulatedZoom *= zoom

                        // Zooming in (pinch open) -> fewer columns (larger photos)
                        // Zooming out (pinch closed) -> more columns (smaller photos)
                        if (accumulatedZoom > 1.3f) {
                            if (gridColumns > 2) {
                                gridColumns -= 1
                                accumulatedZoom = 1f
                            }
                        } else if (accumulatedZoom < 0.7f) {
                            if (gridColumns < 8) {
                                gridColumns += 1
                                accumulatedZoom = 1f
                            }
                        }
                    }
                } else {
                    if (isZooming) {
                        accumulatedZoom = 1f
                        isZooming = false
                    }
                }
            }
        }
    }

    Box(modifier = modifier) {
        val groups = if (groupByDate) {
            remember(photos, gridColumns) {
                groupPhotosByDate(photos, gridColumns)
            }
        } else {
            emptyList()
        }

        val flatItemLabels = if (groupByDate) {
            remember(groups) {
                val list = mutableListOf<String>()
                groups.forEach { group ->
                    list.add(group.label) // Header label
                    group.photos.forEach { _ ->
                        list.add(group.label) // Photo labels
                    }
                }
                list
            }
        } else {
            emptyList()
        }

        if (groupByDate) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(gridColumns),
                contentPadding = PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxSize().then(zoomModifier)
            ) {
                groups.forEach { group ->
                    // Date header
                    item(
                        key = "header_${group.label}",
                        span = { GridItemSpan(maxLineSpan) }
                    ) {
                        if (gridColumns <= 4) {
                            Text(
                                text = group.label,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 12.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = Color(0xFF222222).copy(alpha = 0.85f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = Color(0xFF333333),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = group.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Photos in this group
                    items(
                        items = group.photos,
                        key = { it.id }
                    ) { photo ->
                        PhotoThumbnail(
                            photo = photo,
                            isSelected = photo.id in selectedIds,
                            isSelectionMode = isSelectionMode,
                            onClick = { onPhotoClick(photo) },
                            onLongClick = { onPhotoLongClick(photo) }
                        )
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(gridColumns),
                contentPadding = PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxSize().then(zoomModifier)
            ) {
                items(
                    items = photos,
                    key = { it.id }
                ) { photo ->
                    PhotoThumbnail(
                        photo = photo,
                        isSelected = photo.id in selectedIds,
                        isSelectionMode = isSelectionMode,
                        onClick = { onPhotoClick(photo) },
                        onLongClick = { onPhotoLongClick(photo) }
                    )
                }
            }
        }

        VerticalScrollbar(
            state = gridState,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(end = 4.dp),
            accentColor = MaterialTheme.colorScheme.primary,
            labelProvider = if (groupByDate) {
                { index -> flatItemLabels.getOrNull(index) ?: "" }
            } else {
                { index -> photos.getOrNull(index)?.let { getMonthGroupLabel(it.dateTaken) } ?: "" }
            }
        )
    }
}

private data class ScrollbarData(
    val showScrollbar: Boolean,
    val totalItems: Int,
    val thumbHeightPercent: Float,
    val scrollPercent: Float,
    val firstVisibleIndex: Int
)

@Composable
fun VerticalScrollbar(
    state: LazyGridState,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    labelProvider: ((Int) -> String)? = null
) {
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    // Keep track of scrollbar active drag state and visibility
    var isDragging by remember { mutableStateOf(false) }
    var isScrolling by remember { mutableStateOf(false) }

    LaunchedEffect(state.isScrollInProgress, isDragging) {
        if (state.isScrollInProgress || isDragging) {
            isScrolling = true
        } else {
            delay(1500)
            isScrolling = false
        }
    }

    // Compute layout properties reactively inside derivedStateOf to trigger recomposition on scroll changes
    val scrollbarData by remember(state) {
        derivedStateOf {
            val layoutInfo = state.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) {
                ScrollbarData(
                    showScrollbar = false,
                    totalItems = 0,
                    thumbHeightPercent = 1f,
                    scrollPercent = 0f,
                    firstVisibleIndex = 0
                )
            } else {
                val totalItems = layoutInfo.totalItemsCount
                val firstVisibleIndex = visibleItems.first().index
                val lastVisibleIndex = visibleItems.last().index
                val visibleCount = lastVisibleIndex - firstVisibleIndex + 1
                val showScrollbar = visibleCount < totalItems

                val thumbHeightPercent = (visibleCount.toFloat() / totalItems.toFloat()).coerceIn(0.1f, 0.9f)
                val scrollPercent = firstVisibleIndex.toFloat() / (totalItems - visibleCount).coerceAtLeast(1)

                ScrollbarData(
                    showScrollbar = showScrollbar,
                    totalItems = totalItems,
                    thumbHeightPercent = thumbHeightPercent,
                    scrollPercent = scrollPercent,
                    firstVisibleIndex = firstVisibleIndex
                )
            }
        }
    }

    if (!scrollbarData.showScrollbar) return

    val currentScrollLabel by remember(scrollbarData.firstVisibleIndex, labelProvider) {
        derivedStateOf {
            if (labelProvider == null) ""
            else {
                labelProvider(scrollbarData.firstVisibleIndex)
            }
        }
    }

    val thumbHeightPercent = scrollbarData.thumbHeightPercent
    val scrollPercent = scrollbarData.scrollPercent

    AnimatedVisibility(
        visible = isScrolling,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .width(180.dp) // Touch grab width + space for bubble
                .fillMaxHeight()
                .padding(vertical = 4.dp)
        ) {
            // Drag handling container aligned to the right (overlaying track and thumb)
            var scrollJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(36.dp) // Larger touch target
                    .fillMaxHeight()
                    .pointerInput(state) {
                        detectDragGestures(
                            onDragStart = { isDragging = true },
                            onDragEnd = { isDragging = false },
                            onDragCancel = { isDragging = false },
                            onDrag = { change, _ ->
                                change.consume()
                                val y = change.position.y
                                val height = size.height
                                if (height > 0) {
                                    val touchPercent = (y / height).coerceIn(0f, 1f)
                                    val totalItems = scrollbarData.totalItems
                                    val targetIndex = (touchPercent * totalItems).toInt().coerceIn(0, totalItems - 1)
                                    scrollJob?.cancel()
                                    scrollJob = coroutineScope.launch {
                                        state.scrollToItem(targetIndex, 0)
                                    }
                                }
                            }
                        )
                    }
            )

            // Track (Centered inside the touch target area)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(2.dp)
                    )
            )

            // Thumb
            Box(
                modifier = Modifier
                    .fillMaxHeight(thumbHeightPercent)
                    .width(6.dp)
                    .align(Alignment.TopEnd)
                    .padding(end = 15.dp)
                    .graphicsLayer {
                        val parentHeight = size.height / thumbHeightPercent.coerceAtLeast(0.01f)
                        val thumbHeight = size.height
                        val maxTranslation = parentHeight - thumbHeight
                        translationY = maxTranslation * scrollPercent
                    }
                    .background(
                        color = accentColor,
                        shape = RoundedCornerShape(3.dp)
                    )
            )

            // Date/Label Bubble
            if (currentScrollLabel.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .graphicsLayer {
                            val parentHeight = size.height / thumbHeightPercent.coerceAtLeast(0.01f)
                            val thumbHeight = size.height
                            val maxTranslation = parentHeight - thumbHeight
                            translationY = maxTranslation * scrollPercent + (thumbHeight / 2) - 18.dp.toPx()
                        }
                        .padding(end = 36.dp) // Positioned to the left of the scroll handle
                        .background(
                            color = Color(0xFF1E1E1E).copy(alpha = 0.9f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = Color(0xFF333333),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = currentScrollLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
