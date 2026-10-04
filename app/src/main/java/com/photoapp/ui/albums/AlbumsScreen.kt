package com.photoapp.ui.albums

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.photoapp.data.local.entities.AlbumEntity
import com.photoapp.data.local.entities.PhotoEntity
import com.photoapp.ui.components.EmptyState
import com.photoapp.ui.components.EmptyStateType
import com.photoapp.ui.components.MoveCopyToAlbumDialog
import com.photoapp.ui.components.PdfNameDialog
import com.photoapp.ui.components.PhotoGrid
import com.photoapp.ui.components.RenameDialog
import com.photoapp.ui.components.SelectionTopBar
import com.photoapp.ui.components.VerticalScrollbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    onPhotoClick: (Long, String) -> Unit,
    onTrashClick: () -> Unit,
    onHiddenClick: () -> Unit,
    bottomPadding: Dp = 0.dp,
    viewModel: AlbumsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showMoveDialog by remember { mutableStateOf(false) }
    var showCopyDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showPdfDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = uiState.selectedAlbum != null) {
        if (uiState.isSelectionMode) {
            viewModel.clearSelection()
        } else {
            viewModel.clearSelectedAlbum()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = uiState.selectedAlbum != null,
            transitionSpec = {
                (slideInHorizontally { it } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it } + fadeOut())
            },
            label = "albumTransition",
            modifier = Modifier.fillMaxSize()
        ) { isAlbumDetail ->
            if (isAlbumDetail && uiState.selectedAlbum != null) {
                val currentAlbumId = uiState.selectedAlbum!!.id
                // Album detail view
                AlbumDetailView(
                    album = uiState.selectedAlbum!!,
                    photos = uiState.albumPhotos,
                    selectedIds = uiState.selectedIds,
                    isSelectionMode = uiState.isSelectionMode,
                    onBack = { viewModel.clearSelectedAlbum() },
                    onPhotoClick = { photoId ->
                        if (uiState.isSelectionMode) {
                            viewModel.toggleSelection(photoId)
                        } else {
                            onPhotoClick(photoId, currentAlbumId)
                        }
                    },
                    onPhotoLongClick = { photoId ->
                        viewModel.toggleSelection(photoId)
                    },
                    onSelectionChanged = { viewModel.setSelectedIds(it) },
                    onCloseSelection = { viewModel.clearSelection() },
                    onSelectAll = { viewModel.selectAll() },
                    onDeleteSelection = { viewModel.deleteSelected() },
                    onShareSelection = { viewModel.shareSelected() },
                    onFavoriteSelection = { viewModel.favoriteSelected() },
                    onMoveToAlbumSelection = { showMoveDialog = true },
                    onCopyToAlbumSelection = { showCopyDialog = true },
                    onRenameSelection = { showRenameDialog = true },
                    onConvertToPdfSelection = { showPdfDialog = true },
                    onSetAsWallpaperSelection = {
                        viewModel.setAsWallpaperSelected { success ->
                            if (success) {
                                android.widget.Toast.makeText(context, "Wallpaper set successfully", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                android.widget.Toast.makeText(context, "Failed to set wallpaper", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onHideSelection = { viewModel.hideSelected() },
                    bottomPadding = bottomPadding
                )
            } else {
                // Album grid view
                AlbumGridView(
                    albums = uiState.albums,
                    trashCount = uiState.trashCount,
                    hiddenCount = uiState.hiddenCount,
                    onAlbumClick = { viewModel.selectAlbum(it.id) },
                    onTrashClick = onTrashClick,
                    onHiddenClick = onHiddenClick,
                    bottomPadding = bottomPadding
                )
            }
        }

        // Dialogs
        if (showMoveDialog) {
            MoveCopyToAlbumDialog(
                title = "Move to Album",
                albums = uiState.albums,
                onAlbumSelected = { albumName ->
                    showMoveDialog = false
                    viewModel.moveSelectedToAlbum(albumName)
                    android.widget.Toast.makeText(context, "Moved items to $albumName", android.widget.Toast.LENGTH_SHORT).show()
                },
                onCreateNewAlbum = { albumName ->
                    showMoveDialog = false
                    viewModel.moveSelectedToAlbum(albumName)
                    android.widget.Toast.makeText(context, "Moved items to new album $albumName", android.widget.Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showMoveDialog = false }
            )
        }

        if (showCopyDialog) {
            MoveCopyToAlbumDialog(
                title = "Copy to Album",
                albums = uiState.albums,
                onAlbumSelected = { albumName ->
                    showCopyDialog = false
                    viewModel.copySelectedToAlbum(albumName)
                    android.widget.Toast.makeText(context, "Copied items to $albumName", android.widget.Toast.LENGTH_SHORT).show()
                },
                onCreateNewAlbum = { albumName ->
                    showCopyDialog = false
                    viewModel.copySelectedToAlbum(albumName)
                    android.widget.Toast.makeText(context, "Copied items to new album $albumName", android.widget.Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showCopyDialog = false }
            )
        }

        if (showRenameDialog) {
            val initialName = if (uiState.selectedIds.size == 1) {
                uiState.albumPhotos.find { it.id == uiState.selectedIds.first() }?.name ?: ""
            } else ""
            
            RenameDialog(
                initialName = initialName,
                onRename = { newName ->
                    showRenameDialog = false
                    viewModel.renameSelected(newName)
                    android.widget.Toast.makeText(context, "Renamed selected items", android.widget.Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showRenameDialog = false }
            )
        }

        if (showPdfDialog) {
            PdfNameDialog(
                onGenerate = { pdfName ->
                    showPdfDialog = false
                    viewModel.convertSelectedToPdf(pdfName) { pdfUri ->
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumGridView(
    albums: List<AlbumEntity>,
    trashCount: Int,
    hiddenCount: Int,
    onAlbumClick: (AlbumEntity) -> Unit,
    onTrashClick: () -> Unit,
    onHiddenClick: () -> Unit,
    bottomPadding: Dp = 0.dp
) {
    val gridState = rememberLazyGridState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Albums",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 110.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = albums,
                    key = { it.id }
                ) { album ->
                    AlbumCard(
                        album = album,
                        onClick = { onAlbumClick(album) }
                    )
                }

                item(
                    key = "trash_button",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    TrashCard(
                        count = trashCount,
                        onClick = onTrashClick
                    )
                }

                item(
                    key = "hidden_button",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    HiddenCard(
                        onClick = onHiddenClick
                    )
                }
            }

            VerticalScrollbar(
                state = gridState,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(end = 4.dp),
                accentColor = MaterialTheme.colorScheme.primary,
                labelProvider = { index ->
                    albums.getOrNull(index)?.name ?: ""
                }
            )
        }
    }
}

@Composable
private fun HiddenCard(
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.VisibilityOff,
                    contentDescription = "Hidden",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Hidden Vault",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Protected pictures and videos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TrashCard(
    count: Int,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Trash",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Trash",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Recently deleted items",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun AlbumCard(
    album: AlbumEntity,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            if (album.coverPhotoUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(album.coverPhotoUri)
                        .crossfade(true)
                        .size(500)
                        .build(),
                    contentDescription = album.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = album.name,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            // Bottom Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.75f)
                            ),
                            startY = 120f
                        )
                    )
            )

            // Album title & formatted count
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp)
            ) {
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .background(
                            color = Color.Black.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = album.formattedCount,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumDetailView(
    album: AlbumEntity,
    photos: List<com.photoapp.data.local.entities.PhotoEntity>,
    selectedIds: Set<Long>,
    isSelectionMode: Boolean,
    onBack: () -> Unit,
    onPhotoClick: (Long) -> Unit,
    onPhotoLongClick: (Long) -> Unit,
    onSelectionChanged: (Set<Long>) -> Unit,
    onCloseSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onDeleteSelection: () -> Unit,
    onShareSelection: () -> Unit,
    onFavoriteSelection: () -> Unit,
    onMoveToAlbumSelection: () -> Unit,
    onCopyToAlbumSelection: () -> Unit,
    onRenameSelection: () -> Unit,
    onConvertToPdfSelection: () -> Unit,
    onSetAsWallpaperSelection: () -> Unit,
    onHideSelection: () -> Unit,
    bottomPadding: Dp = 0.dp
) {
    val onPhotoClickState = rememberUpdatedState(onPhotoClick)
    val rememberedOnPhotoClick = remember {
        { photo: PhotoEntity ->
            onPhotoClickState.value(photo.id)
        }
    }

    val onPhotoLongClickState = rememberUpdatedState(onPhotoLongClick)
    val rememberedOnPhotoLongClick = remember {
        { photo: PhotoEntity ->
            onPhotoLongClickState.value(photo.id)
        }
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                SelectionTopBar(
                    selectedCount = selectedIds.size,
                    visible = true,
                    onClose = onCloseSelection,
                    onSelectAll = onSelectAll,
                    onDelete = onDeleteSelection,
                    onShare = onShareSelection,
                    onFavorite = onFavoriteSelection,
                    onMoveToAlbum = onMoveToAlbumSelection,
                    onCopyToAlbum = onCopyToAlbumSelection,
                    onRename = onRenameSelection,
                    onConvertToPdf = onConvertToPdfSelection,
                    onSetAsWallpaper = onSetAsWallpaperSelection,
                    onHide = onHideSelection
                )
            } else {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = album.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = album.formattedCount,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    ) { paddingValues ->
        PhotoGrid(
            photos = photos,
            selectedIds = selectedIds,
            isSelectionMode = isSelectionMode,
            onPhotoClick = rememberedOnPhotoClick,
            onPhotoLongClick = rememberedOnPhotoLongClick,
            groupByDate = true,
            onSelectionChanged = onSelectionChanged,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}
