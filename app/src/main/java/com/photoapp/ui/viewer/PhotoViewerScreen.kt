package com.photoapp.ui.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.border
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import com.photoapp.ui.components.MoveCopyToAlbumDialog
import com.photoapp.ui.components.RenameDialog
import com.photoapp.ui.components.PdfNameDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.DisposableEffect
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.photoapp.ui.theme.FavoriteRed
import com.photoapp.util.DateUtils
import com.photoapp.ui.components.VideoPlayer
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoViewerScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: PhotoViewerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val allPhotos = uiState.allPhotos
    val pageScales = remember { mutableStateMapOf<Int, Float>() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var showCopyDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showPdfDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val density = LocalDensity.current
    val defaultBottomBarHeight = with(density) { 140.dp.roundToPx() }
    var bottomBarHeightPx by remember { mutableStateOf(defaultBottomBarHeight) }

    var hasLoadedOnce by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.showControls) {
        val activity = context as? android.app.Activity
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            if (uiState.showControls) {
                controller.show(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val activity = context as? android.app.Activity
            val window = activity?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Auto back if list becomes empty after loading (e.g. items are deleted)
    LaunchedEffect(allPhotos) {
        if (allPhotos.isNotEmpty()) {
            hasLoadedOnce = true
        } else if (hasLoadedOnce) {
            onBack()
        }
    }

    if (allPhotos.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    // Determine initial page index once the media list is loaded
    val initialIndex = remember(allPhotos) {
        val index = allPhotos.indexOfFirst { it.id == uiState.initialPhotoId }
        if (index >= 0) index else 0
    }

    val pagerState = rememberPagerState(initialPage = initialIndex) {
        allPhotos.size
    }

    val currentPhoto = allPhotos.getOrNull(pagerState.currentPage)

    if (currentPhoto == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text("Photo not found", color = Color.White)
        }
        return
    }

    val isVideo = currentPhoto.mimeType.startsWith("video/")

    val thumbnailListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        if (allPhotos.isNotEmpty() && pagerState.currentPage < allPhotos.size) {
            thumbnailListState.animateScrollToItem(pagerState.currentPage)
        }
    }

    var mediaInfo by remember { mutableStateOf<com.photoapp.util.MediaFormatAnalyzer.MediaInfo?>(null) }
    LaunchedEffect(currentPhoto) {
        mediaInfo = null
        if (currentPhoto != null) {
            mediaInfo = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.photoapp.util.MediaFormatAnalyzer.analyze(
                    context = context,
                    uriString = currentPhoto.uri,
                    path = currentPhoto.path,
                    isVideo = currentPhoto.mimeType.startsWith("video/"),
                    width = currentPhoto.width,
                    height = currentPhoto.height
                )
            }
        }
    }

    val swipeDismissOffsetY = remember { Animatable(0f) }
    val localDensity = LocalDensity.current
    val maxSwipeDistance = remember(localDensity) { with(localDensity) { 400.dp.toPx() } }
    val dragFraction = if (swipeDismissOffsetY.value > 0f) {
        (swipeDismissOffsetY.value / maxSwipeDistance).coerceIn(0f, 1f)
    } else {
        0f
    }
    val bgAlpha = 1f - dragFraction
    val scaleFraction = 1f - (dragFraction * 0.12f)
    val currentScale = pageScales[pagerState.currentPage] ?: 1f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = bgAlpha))
            .pointerInput(currentScale) {
                if (currentScale == 1f) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var isVerticalDrag: Boolean? = null
                            var lastY = down.position.y
                            val velocityTracker = VelocityTracker()
                            
                            do {
                                val event = awaitPointerEvent()
                                val changes = event.changes
                                val change = changes.firstOrNull { it.id == down.id }
                                
                                if (change != null && change.pressed) {
                                    val currentPos = change.position
                                    velocityTracker.addPosition(change.uptimeMillis, currentPos)
                                    
                                    if (isVerticalDrag == null) {
                                        val totalDrag = currentPos - down.position
                                        if (totalDrag.getDistance() > 15f) {
                                            if (Math.abs(totalDrag.y) > Math.abs(totalDrag.x)) {
                                                if (totalDrag.y > 0f) {
                                                    isVerticalDrag = true
                                                } else {
                                                    isVerticalDrag = false
                                                    viewModel.toggleInfo()
                                                }
                                            }
                                        }
                                    }
                                    
                                    if (isVerticalDrag == true) {
                                        change.consume()
                                        val deltaY = currentPos.y - lastY
                                        coroutineScope.launch {
                                            swipeDismissOffsetY.snapTo(
                                                (swipeDismissOffsetY.value + deltaY).coerceAtLeast(0f)
                                            )
                                        }
                                    }
                                    lastY = currentPos.y
                                }
                            } while (changes.any { it.pressed })
                            
                            if (isVerticalDrag == true) {
                                val velocity = velocityTracker.calculateVelocity().y
                                if (swipeDismissOffsetY.value > maxSwipeDistance * 0.35f || velocity > 1000f) {
                                    onBack()
                                } else {
                                    coroutineScope.launch {
                                        swipeDismissOffsetY.animateTo(0f, spring())
                                    }
                                }
                            }
                        }
                    }
                }
            }
    ) {
        // Horizontal Pager for swiping (scrollable only if current page is not zoomed in and not swiping down)
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = currentScale == 1f && swipeDismissOffsetY.value == 0f,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    translationY = swipeDismissOffsetY.value,
                    scaleX = scaleFraction,
                    scaleY = scaleFraction
                )
        ) { pageIndex ->
            val photo = allPhotos.getOrNull(pageIndex)
            if (photo != null) {
                val itemIsVideo = photo.mimeType.startsWith("video/")
                if (itemIsVideo) {
                    val isActivePage = pagerState.currentPage == pageIndex
                    VideoPlayer(
                        uri = photo.contentUri,
                        showControls = uiState.showControls,
                        bottomBarHeightPx = bottomBarHeightPx,
                        isActivePage = isActivePage,
                        onControllerVisibilityChanged = { visible ->
                            viewModel.setControlsVisible(visible)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    var scale by remember { mutableStateOf(1f) }
                    var offset by remember { mutableStateOf(Offset.Zero) }
                    var size by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

                    val isCurrentPage = pagerState.currentPage == pageIndex
                    LaunchedEffect(isCurrentPage) {
                        if (!isCurrentPage) {
                            scale = 1f
                            offset = Offset.Zero
                        }
                    }

                    LaunchedEffect(scale) {
                        pageScales[pageIndex] = scale
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { size = it }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = { centroid ->
                                        if (scale > 1f) {
                                            scale = 1f
                                            offset = Offset.Zero
                                        } else {
                                            scale = 2.5f
                                            offset = Offset.Zero
                                        }
                                    },
                                    onTap = {
                                        viewModel.toggleControls()
                                    }
                                )
                            }
                            .pointerInput(Unit) {
                        awaitPointerEventScope {
                            var downPosition = Offset.Zero
                            var isPanning = false
                            while (true) {
                                val event = awaitPointerEvent()
                                val changes = event.changes
                                val numPointers = changes.size

                                val hasDown = changes.any { it.changedToDown() }
                                if (hasDown) {
                                    downPosition = changes.firstOrNull { it.pressed }?.position ?: Offset.Zero
                                    isPanning = false
                                }

                                if (numPointers >= 2) {
                                    changes.forEach { it.consume() }

                                    val p1 = changes[0].position
                                    val p2 = changes[1].position
                                    val currentDist = (p1 - p2).getDistance()

                                    val prevP1 = changes[0].previousPosition
                                    val prevP2 = changes[1].previousPosition
                                    val prevDist = (prevP1 - prevP2).getDistance()

                                    if (prevDist > 0f && currentDist > 0f) {
                                        val zoomFactor = currentDist / prevDist
                                        val newScale = (scale * zoomFactor).coerceIn(1f, 5f)
                                        val maxTx = (size.width * newScale - size.width) / 2f
                                        val maxTy = (size.height * newScale - size.height) / 2f

                                        scale = newScale
                                        val currentCentroid = (p1 + p2) / 2f
                                        val prevCentroid = (prevP1 + prevP2) / 2f
                                        val pan = currentCentroid - prevCentroid
                                        offset = Offset(
                                            x = (offset.x + pan.x).coerceIn(-maxTx, maxTx),
                                            y = (offset.y + pan.y).coerceIn(-maxTy, maxTy)
                                        )
                                    }
                                } else if (numPointers == 1 && scale > 1f) {
                                    val change = changes[0]
                                    if (change.pressed) {
                                        if (!isPanning) {
                                            val dist = (change.position - downPosition).getDistance()
                                            if (dist > 15f) {
                                                isPanning = true
                                            }
                                        }

                                        if (isPanning) {
                                            change.consume()
                                            if (change.position != change.previousPosition) {
                                                val pan = change.position - change.previousPosition
                                                val maxTx = (size.width * scale - size.width) / 2f
                                                val maxTy = (size.height * scale - size.height) / 2f
                                                offset = Offset(
                                                    x = (offset.x + pan.x).coerceIn(-maxTx, maxTx),
                                                    y = (offset.y + pan.y).coerceIn(-maxTy, maxTy)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(photo.contentUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = photo.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                )
                        )
                    }
                }
            }
        }

        // Top bar
        AnimatedVisibility(
            visible = uiState.showControls,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer(alpha = bgAlpha)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentPhoto.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    mediaInfo?.let { info ->
                        val tags = mutableListOf<Pair<String, Pair<Color, Color>>>()
                        
                        info.hdrTag?.let { hdr ->
                            val displayHdr = when {
                                hdr.contains("Dolby") -> "Dolby Vision"
                                hdr.contains("HDR10") -> "HDR10"
                                hdr.contains("HLG") -> "HLG"
                                else -> "HDR"
                            }
                            val color = if (hdr.contains("Dolby")) Color(0xFFFFB300) else Color(0xFFFF5722)
                            val textColor = if (hdr.contains("Dolby")) Color.Black else Color.White
                            tags.add(displayHdr to (color to textColor))
                        }
                        
                        info.resolutionTag?.let { res ->
                            val displayRes = when {
                                res.contains("8K") -> "8K"
                                res.contains("4K") -> "4K"
                                res.contains("1080") -> "1080p"
                                res.contains("720") -> "720p"
                                else -> res
                            }
                            val color = when {
                                displayRes.contains("8K") -> Color(0xFF00BCD4)
                                displayRes.contains("4K") -> Color(0xFF3F51B5)
                                else -> Color(0xFF757575)
                            }
                            tags.add(displayRes to (color to Color.White))
                        }
                        
                        info.extraTags.forEach { tag ->
                            val color = if (tag.contains("fps") || tag.contains("Slow")) Color(0xFF4CAF50) else Color(0xFF9C27B0)
                            tags.add(tag to (color to Color.White))
                        }

                        if (tags.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                tags.forEach { (tag, colors) ->
                                    MediaBadge(text = tag, containerColor = colors.first, contentColor = colors.second)
                                }
                            }
                        }
                    }
                }

                if (!isVideo) {
                    IconButton(
                        onClick = { launchGoogleLens(context, currentPhoto.contentUri) },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CenterFocusStrong,
                            contentDescription = "Google Lens",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Bottom action bar (includes Thumbnail strip and Action bar)
        AnimatedVisibility(
            visible = uiState.showControls,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .graphicsLayer(alpha = bgAlpha)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .onGloballyPositioned { coordinates ->
                        if (coordinates.size.height > 0) {
                            bottomBarHeightPx = coordinates.size.height
                        }
                    }
            ) {
                // 1. Thumbnail Strip
                LazyRow(
                    state = thumbnailListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    itemsIndexed(allPhotos) { index, photo ->
                        val isSelected = index == pagerState.currentPage
                        val borderModifier = if (isSelected) {
                            Modifier.border(2.dp, Color.White, RoundedCornerShape(4.dp))
                        } else {
                            Modifier
                        }

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .then(borderModifier)
                                .clickable {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(photo.contentUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Thumbnail $index",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // 2. Bottom Action bar Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Share
                    IconButton(onClick = { viewModel.sharePhoto(currentPhoto) }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }



                    // Favorite
                    IconButton(onClick = { viewModel.toggleFavorite(currentPhoto.id) }) {
                        Icon(
                            imageVector = if (currentPhoto.isFavorite) Icons.Filled.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (currentPhoto.isFavorite) FavoriteRed else Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Edit
                    IconButton(onClick = {
                        if (isVideo) {
                            android.widget.Toast.makeText(context, "Video editing is not supported yet", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            onEdit(currentPhoto.id)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = if (isVideo) Color.White.copy(alpha = 0.5f) else Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Delete
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // More (3-dot menu)
                    var showOverflowMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Move to album") },
                                onClick = {
                                    showOverflowMenu = false
                                    showMoveDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Copy to album") },
                                onClick = {
                                    showOverflowMenu = false
                                    showCopyDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                onClick = {
                                    showOverflowMenu = false
                                    showRenameDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Convert to PDF") },
                                onClick = {
                                    showOverflowMenu = false
                                    showPdfDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Set as wallpaper") },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.setAsWallpaper(currentPhoto.id) { success ->
                                        if (success) {
                                            android.widget.Toast.makeText(context, "Wallpaper set successfully", android.widget.Toast.LENGTH_SHORT).show()
                                        } else {
                                            android.widget.Toast.makeText(context, "Failed to set wallpaper", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Info bottom sheet
        if (uiState.showInfo) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.toggleInfo() },
                sheetState = rememberModalBottomSheetState(),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                MediaInfoContent(photo = currentPhoto)
            }
        }

        // Delete confirmation dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(if (isVideo) "Delete Video" else "Delete Photo") },
                text = { Text(if (isVideo) "Move this video to trash?" else "Move this photo to trash?") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deletePhoto(currentPhoto.id)
                        showDeleteDialog = false
                    }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Move to Album Dialog
        if (showMoveDialog) {
            MoveCopyToAlbumDialog(
                title = "Move to Album",
                albums = uiState.albums,
                onAlbumSelected = { albumName ->
                    showMoveDialog = false
                    viewModel.movePhotoToAlbum(currentPhoto.id, albumName)
                    android.widget.Toast.makeText(context, "Moved to $albumName", android.widget.Toast.LENGTH_SHORT).show()
                },
                onCreateNewAlbum = { albumName ->
                    showMoveDialog = false
                    viewModel.movePhotoToAlbum(currentPhoto.id, albumName)
                    android.widget.Toast.makeText(context, "Moved to new album $albumName", android.widget.Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showMoveDialog = false }
            )
        }

        // Copy to Album Dialog
        if (showCopyDialog) {
            MoveCopyToAlbumDialog(
                title = "Copy to Album",
                albums = uiState.albums,
                onAlbumSelected = { albumName ->
                    showCopyDialog = false
                    viewModel.copyPhotoToAlbum(currentPhoto.id, albumName)
                    android.widget.Toast.makeText(context, "Copied to $albumName", android.widget.Toast.LENGTH_SHORT).show()
                },
                onCreateNewAlbum = { albumName ->
                    showCopyDialog = false
                    viewModel.copyPhotoToAlbum(currentPhoto.id, albumName)
                    android.widget.Toast.makeText(context, "Copied to new album $albumName", android.widget.Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showCopyDialog = false }
            )
        }

        // Rename Dialog
        if (showRenameDialog) {
            RenameDialog(
                initialName = currentPhoto.name,
                onRename = { newName ->
                    showRenameDialog = false
                    viewModel.renamePhoto(currentPhoto.id, newName)
                    android.widget.Toast.makeText(context, "Renamed to $newName", android.widget.Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showRenameDialog = false }
            )
        }

        // PDF Dialog
        if (showPdfDialog) {
            PdfNameDialog(
                onGenerate = { pdfName ->
                    showPdfDialog = false
                    viewModel.convertPhotosToPdf(listOf(currentPhoto.id), pdfName) { pdfUri ->
                        if (pdfUri != null) {
                            android.widget.Toast.makeText(context, "PDF saved to Documents/PhotoApp", android.widget.Toast.LENGTH_LONG).show()
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(android.content.Intent.EXTRA_STREAM, pdfUri)
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(android.content.Intent.createChooser(intent, "Share PDF").apply {
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        } else {
                            android.widget.Toast.makeText(context, "Failed to generate PDF", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onDismiss = { showPdfDialog = false }
            )
        }
    }
}

@Composable
private fun MediaInfoContent(
    photo: com.photoapp.data.local.entities.PhotoEntity
) {
    val context = LocalContext.current
    var info by remember(photo) { mutableStateOf<com.photoapp.util.MediaFormatAnalyzer.MediaInfo?>(null) }
    LaunchedEffect(photo) {
        info = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            com.photoapp.util.MediaFormatAnalyzer.analyze(
                context = context,
                uriString = photo.uri,
                path = photo.path,
                isVideo = photo.mimeType.startsWith("video/"),
                width = photo.width,
                height = photo.height
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Details",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        InfoRow(label = "Filename", value = photo.name)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        info?.let { mediaInfo ->
            val tags = mutableListOf<String>()
            mediaInfo.hdrTag?.let { tags.add(it) }
            mediaInfo.resolutionTag?.let { tags.add(it) }
            tags.addAll(mediaInfo.extraTags)

            if (tags.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Attributes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(100.dp)
                    )
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tags.forEach { tag ->
                            val color = when {
                                tag.contains("Dolby") -> Color(0xFFFFB300)
                                tag.contains("HDR") -> Color(0xFFFF5722)
                                tag.contains("8K") -> Color(0xFF00BCD4)
                                tag.contains("4K") -> Color(0xFF3F51B5)
                                tag.contains("fps") || tag.contains("Slow") -> Color(0xFF4CAF50)
                                else -> Color(0xFF9C27B0)
                            }
                            val textColor = if (tag.contains("Dolby")) Color.Black else Color.White
                            MediaBadge(text = tag, containerColor = color, contentColor = textColor)
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
        }

        InfoRow(label = "Date Added", value = DateUtils.formatDateTime(photo.dateAdded))
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        InfoRow(label = "File Size", value = photo.formattedSize)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        InfoRow(label = "Resolution", value = photo.resolution)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        InfoRow(label = "Type", value = photo.mimeType)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        InfoRow(label = "Path", value = photo.path)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun launchGoogleLens(context: android.content.Context, imageUri: android.net.Uri) {
    // Try 1: Send image to Google app – it routes to Lens internally
    // This is how OEM gallery apps (Samsung, Realme, OnePlus) launch Lens
    try {
        val googleAppIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(android.content.Intent.EXTRA_STREAM, imageUri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setPackage("com.google.android.googlequicksearchbox")
        }
        context.startActivity(googleAppIntent)
        return
    } catch (_: Exception) { }

    // Try 2: Standalone Google Lens app (some devices)
    try {
        val lensAppIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(android.content.Intent.EXTRA_STREAM, imageUri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setPackage("com.google.ar.lens")
        }
        context.startActivity(lensAppIntent)
        return
    } catch (_: Exception) { }

    android.widget.Toast.makeText(
        context,
        "Google Lens is not available on this device",
        android.widget.Toast.LENGTH_SHORT
    ).show()
}

@Composable
private fun MediaBadge(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Box(
        modifier = Modifier
            .background(containerColor, shape = RoundedCornerShape(3.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            lineHeight = 10.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}
