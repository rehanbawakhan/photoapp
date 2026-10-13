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
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.coroutineScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Immutable
import com.photoapp.data.local.entities.PhotoEntity
import com.photoapp.util.DateUtils
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

@Immutable
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

fun getDayGroupLabelOptimized(
    timestamp: Long,
    now: Calendar,
    yesterday: Calendar,
    photoCal: Calendar,
    cache: MutableMap<String, String>
): String {
    photoCal.timeInMillis = timestamp
    val year = photoCal.get(Calendar.YEAR)
    val dayOfYear = photoCal.get(Calendar.DAY_OF_YEAR)
    val key = "${year}_${dayOfYear}"
    
    return cache.getOrPut(key) {
        val isSameDay = now.get(Calendar.YEAR) == year && now.get(Calendar.DAY_OF_YEAR) == dayOfYear
        val isYesterday = yesterday.get(Calendar.YEAR) == year && yesterday.get(Calendar.DAY_OF_YEAR) == dayOfYear
        
        when {
            isSameDay -> "Today"
            isYesterday -> "Yesterday"
            now.get(Calendar.YEAR) == year -> dayFormatSameYear.format(Date(timestamp))
            else -> dayFormatDiffYear.format(Date(timestamp))
        }
    }
}

fun getMonthGroupLabelOptimized(
    timestamp: Long,
    photoCal: Calendar,
    cache: MutableMap<String, String>
): String {
    photoCal.timeInMillis = timestamp
    val year = photoCal.get(Calendar.YEAR)
    val month = photoCal.get(Calendar.MONTH)
    val key = "${year}_${month}"
    return cache.getOrPut(key) {
        monthYearFormatShort.format(Date(timestamp))
    }
}

fun groupPhotosByDate(photos: List<PhotoEntity>, columns: Int): List<PhotoGroup> {
    val now = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply {
        timeInMillis = now.timeInMillis
        add(Calendar.DAY_OF_YEAR, -1)
    }
    val photoCal = Calendar.getInstance()
    val dayCache = mutableMapOf<String, String>()
    val monthCache = mutableMapOf<String, String>()
    
    return if (columns <= 4) {
        photos
            .groupBy { getDayGroupLabelOptimized(it.dateTaken, now, yesterday, photoCal, dayCache) }
            .map { (label, photos) -> PhotoGroup(label, photos) }
    } else {
        photos
            .groupBy { getMonthGroupLabelOptimized(it.dateTaken, photoCal, monthCache) }
            .map { (label, photos) -> PhotoGroup(label, photos) }
    }
}

private suspend fun PointerInputScope.detectGridDragSelectAfterLongPress(
    scope: kotlinx.coroutines.CoroutineScope,
    onDragStart: (Offset) -> Unit,
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit
) {
    awaitEachGesture {
        val down = awaitFirstDown(pass = PointerEventPass.Initial)
        var isLongPress = false
        val longPressTimeout = viewConfiguration.longPressTimeoutMillis
        
        val job = scope.launch {
            delay(longPressTimeout)
            isLongPress = true
            onDragStart(down.position)
            down.consume()
        }
        
        try {
            val pointer = down.id
            while (true) {
                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                val dragChange = event.changes.firstOrNull { it.id == pointer } ?: break
                
                if (dragChange.pressed) {
                    if (isLongPress) {
                        dragChange.consume()
                        val previousPosition = dragChange.previousPosition
                        val currentPosition = dragChange.position
                        onDrag(dragChange, currentPosition - previousPosition)
                    } else {
                        val touchSlop = viewConfiguration.touchSlop
                        if ((dragChange.position - down.position).getDistance() > touchSlop) {
                            job.cancel()
                        }
                    }
                } else {
                    break
                }
            }
            job.cancel()
            if (isLongPress) {
                onDragEnd()
            }
        } catch (e: Exception) {
            job.cancel()
            onDragCancel()
        }
    }
}

private fun LazyGridState.getItemPhotoIndexAtOffset(
    offset: Offset,
    photos: List<PhotoEntity>
): Int? {
    val layoutInfo = layoutInfo
    val visibleItemsInfo = layoutInfo.visibleItemsInfo
    val x = offset.x
    val y = offset.y
    val item = visibleItemsInfo.find { item ->
        x >= item.offset.x && x <= item.offset.x + item.size.width &&
        y >= item.offset.y && y <= item.offset.y + item.size.height
    } ?: return null

    val key = item.key
    if (key is Long) {
        val idx = photos.indexOfFirst { it.id == key }
        return if (idx != -1) idx else null
    }
    return null
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
    columns: Int = 4,
    onSelectionChanged: ((Set<Long>) -> Unit)? = null
) {
    val gridState = rememberLazyGridState()
    var gridColumns by remember(columns) { mutableIntStateOf(columns) }
    LaunchedEffect(columns) {
        gridColumns = columns
    }
    val scope = rememberCoroutineScope()

    val onLongClickAction = remember(onSelectionChanged, onPhotoLongClick) {
        { p: PhotoEntity ->
            if (onSelectionChanged == null) {
                onPhotoLongClick(p)
            }
        }
    }

    val currentPhotosState = rememberUpdatedState(photos)
    val currentSelectedIdsState = rememberUpdatedState(selectedIds)

    var dragStartPhotoIndex by remember { mutableStateOf<Int?>(null) }
    var dragCurrentPhotoIndex by remember { mutableStateOf<Int?>(null) }
    var dragSelectMode by remember { mutableStateOf(true) }
    var initialSelectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var currentDragPosition by remember { mutableStateOf<Offset?>(null) }

    fun updateDragSelection(pointerOffset: Offset) {
        val startIdx = dragStartPhotoIndex ?: return
        val currentPhotos = currentPhotosState.value
        val currentIdx = gridState.getItemPhotoIndexAtOffset(pointerOffset, currentPhotos) ?: dragCurrentPhotoIndex ?: startIdx
        dragCurrentPhotoIndex = currentIdx

        val newSelection = initialSelectedIds.toMutableSet().apply {
            val range = if (startIdx <= currentIdx) startIdx..currentIdx else currentIdx..startIdx
            for (i in range) {
                if (i in currentPhotos.indices) {
                    val id = currentPhotos[i].id
                    if (dragSelectMode) add(id) else remove(id)
                }
            }
        }
        onSelectionChanged?.invoke(newSelection)
    }

    LaunchedEffect(currentDragPosition) {
        val pos = currentDragPosition
        if (pos != null && onSelectionChanged != null && dragStartPhotoIndex != null) {
            val viewportHeight = gridState.layoutInfo.viewportSize.height
            if (viewportHeight > 0) {
                var scrollSpeed = 0f
                if (pos.y < 150f) {
                    scrollSpeed = -20f * ((150f - pos.y) / 150f).coerceIn(0.2f, 1.5f)
                } else if (pos.y > viewportHeight - 150f) {
                    scrollSpeed = 20f * ((pos.y - (viewportHeight - 150f)) / 150f).coerceIn(0.2f, 1.5f)
                }
                
                if (scrollSpeed != 0f) {
                    while (currentDragPosition != null) {
                        gridState.scrollBy(scrollSpeed)
                        currentDragPosition?.let { updateDragSelection(it) }
                        delay(16)
                    }
                }
            }
        }
    }

    val dragSelectModifier = if (onSelectionChanged != null) {
        Modifier.pointerInput(Unit) {
            detectGridDragSelectAfterLongPress(
                scope = scope,
                onDragStart = { startOffset ->
                    val currentPhotos = currentPhotosState.value
                    val currentSelectedIds = currentSelectedIdsState.value
                    val photoIndex = gridState.getItemPhotoIndexAtOffset(startOffset, currentPhotos)
                    if (photoIndex != null) {
                        dragStartPhotoIndex = photoIndex
                        dragCurrentPhotoIndex = photoIndex
                        val startPhotoId = currentPhotos[photoIndex].id
                        dragSelectMode = startPhotoId !in currentSelectedIds
                        initialSelectedIds = currentSelectedIds
                        currentDragPosition = startOffset
                        
                        val newSelection = currentSelectedIds.toMutableSet().apply {
                            if (dragSelectMode) add(startPhotoId) else remove(startPhotoId)
                        }
                        onSelectionChanged(newSelection)
                    }
                },
                onDragEnd = {
                    dragStartPhotoIndex = null
                    dragCurrentPhotoIndex = null
                    currentDragPosition = null
                },
                onDragCancel = {
                    dragStartPhotoIndex = null
                    dragCurrentPhotoIndex = null
                    currentDragPosition = null
                },
                onDrag = { change, _ ->
                    change.consume()
                    currentDragPosition = change.position
                    updateDragSelection(change.position)
                }
            )
        }
    } else {
        Modifier
    }

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
                contentPadding = PaddingValues(start = 2.dp, end = 2.dp, top = 2.dp, bottom = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxSize().then(zoomModifier).then(dragSelectModifier)
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
                            onClick = onPhotoClick,
                            onLongClick = onLongClickAction
                        )
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(gridColumns),
                contentPadding = PaddingValues(start = 2.dp, end = 2.dp, top = 2.dp, bottom = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxSize().then(zoomModifier).then(dragSelectModifier)
            ) {
                items(
                    items = photos,
                    key = { it.id }
                ) { photo ->
                    PhotoThumbnail(
                        photo = photo,
                        isSelected = photo.id in selectedIds,
                        isSelectionMode = isSelectionMode,
                        onClick = onPhotoClick,
                        onLongClick = onLongClickAction
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

private val ScrollHandleWidth = 28.dp
private val ScrollHandleHeight = 52.dp
private val ScrollHandleTouchWidth = 44.dp
private val ScrollHandleTouchHeight = 64.dp

/**
 * Fast-scroll handle in the style of iOS Photos / Samsung One UI Gallery:
 * a pill with up/down chevrons that slides in from the edge while the list moves,
 * can be grabbed and dragged to jump through the timeline, shows a date bubble
 * while dragging and ticks (haptics) every time the section label changes.
 */
@Composable
fun VerticalScrollbar(
    state: LazyGridState,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    labelProvider: ((Int) -> String)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val currentLabelProvider by rememberUpdatedState(labelProvider)

    var isDragging by remember { mutableStateOf(false) }
    var isScrolling by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var trackHeightPx by remember { mutableIntStateOf(0) }
    var scrollJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // Reveal while the list moves, hide again shortly after it settles
    LaunchedEffect(state) {
        snapshotFlow { state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset }
            .drop(1)
            .collectLatest {
                isScrolling = true
                delay(1500)
                isScrolling = false
            }
    }

    // Only worth showing for long lists (more than ~2 screens of content)
    val isLongList by remember(state) {
        derivedStateOf {
            val layoutInfo = state.layoutInfo
            val visibleCount = layoutInfo.visibleItemsInfo.size
            visibleCount > 0 && layoutInfo.totalItemsCount > visibleCount * 2
        }
    }

    // Smooth 0..1 position of the list, including the partial offset of the first row
    val listFraction by remember(state) {
        derivedStateOf {
            val layoutInfo = state.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            when {
                visibleItems.isEmpty() -> 0f
                !state.canScrollBackward -> 0f
                !state.canScrollForward -> 1f
                else -> {
                    val first = visibleItems.first()
                    val itemsInFirstRow = visibleItems.count { it.row == first.row }.coerceAtLeast(1)
                    val rowProgress = if (first.size.height > 0) {
                        state.firstVisibleItemScrollOffset.toFloat() / first.size.height
                    } else 0f
                    val position = state.firstVisibleItemIndex + rowProgress * itemsInFirstRow
                    val scrollableItems = (layoutInfo.totalItemsCount - visibleItems.size).coerceAtLeast(1)
                    (position / scrollableItems).coerceIn(0f, 1f)
                }
            }
        }
    }

    val currentLabel by remember(state) {
        derivedStateOf { currentLabelProvider?.invoke(state.firstVisibleItemIndex).orEmpty() }
    }

    // Haptic tick each time the dragged-to section changes
    LaunchedEffect(currentLabel) {
        if (isDragging && currentLabel.isNotEmpty()) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    val handleScale by animateFloatAsState(
        targetValue = if (isDragging) 1.12f else 1f,
        animationSpec = tween(150),
        label = "scrollHandleScale"
    )

    Box(
        modifier = modifier.onSizeChanged { trackHeightPx = it.height }
    ) {
        AnimatedVisibility(
            visible = isLongList && (isScrolling || isDragging),
            enter = slideInHorizontally(tween(220)) { it } + fadeIn(tween(220)),
            exit = slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset {
                    val handleHeightPx = ScrollHandleTouchHeight.toPx()
                    val range = (trackHeightPx - handleHeightPx).coerceAtLeast(0f)
                    val fraction = if (isDragging) dragFraction else listFraction
                    IntOffset(0, (range * fraction).roundToInt())
                }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Date bubble next to the handle while dragging
                AnimatedVisibility(
                    visible = isDragging && currentLabel.isNotEmpty(),
                    enter = fadeIn(tween(150)) + scaleIn(tween(150), initialScale = 0.85f),
                    exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.85f)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .shadow(6.dp, RoundedCornerShape(50))
                            .background(
                                color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.95f),
                                shape = RoundedCornerShape(50)
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = currentLabel,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            maxLines = 1
                        )
                    }
                }

                // Grabbable handle (touch area is larger than the visible pill)
                Box(
                    contentAlignment = Alignment.CenterEnd,
                    modifier = Modifier
                        .size(ScrollHandleTouchWidth, ScrollHandleTouchHeight)
                        .pointerInput(state) {
                            detectVerticalDragGestures(
                                onDragStart = {
                                    dragFraction = listFraction
                                    isDragging = true
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDragEnd = { isDragging = false },
                                onDragCancel = { isDragging = false },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    val range = (trackHeightPx - size.height).toFloat()
                                    val totalItems = state.layoutInfo.totalItemsCount
                                    if (range > 0f && totalItems > 0) {
                                        dragFraction = (dragFraction + dragAmount / range).coerceIn(0f, 1f)
                                        val targetIndex = (dragFraction * (totalItems - 1)).roundToInt()
                                        scrollJob?.cancel()
                                        scrollJob = coroutineScope.launch {
                                            state.scrollToItem(targetIndex)
                                        }
                                    }
                                }
                            )
                        }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = handleScale
                                scaleY = handleScale
                            }
                            .size(ScrollHandleWidth, ScrollHandleHeight)
                            .shadow(4.dp, RoundedCornerShape(50))
                            .background(
                                color = if (isDragging) accentColor else MaterialTheme.colorScheme.surfaceContainerHighest,
                                shape = RoundedCornerShape(50)
                            )
                    ) {
                        val chevronTint = if (isDragging) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowUp,
                            contentDescription = null,
                            tint = chevronTint,
                            modifier = Modifier.size(18.dp)
                        )
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = chevronTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
