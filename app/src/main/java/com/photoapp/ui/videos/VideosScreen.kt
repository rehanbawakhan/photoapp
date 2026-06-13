package com.photoapp.ui.videos

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.photoapp.ui.components.EmptyState
import com.photoapp.ui.components.EmptyStateType
import com.photoapp.ui.components.PhotoGrid
import com.photoapp.ui.components.SelectionTopBar
import com.photoapp.ui.components.MoveCopyToAlbumDialog
import com.photoapp.ui.components.RenameDialog
import com.photoapp.ui.components.PdfNameDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideosScreen(
    onPhotoClick: (Long) -> Unit,
    bottomPadding: Dp = 0.dp,
    viewModel: VideosViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showMoveDialog by remember { mutableStateOf(false) }
    var showCopyDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showPdfDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = uiState.isSelectionMode) {
        viewModel.clearSelection()
    }

    Scaffold(
        topBar = {
            if (uiState.isSelectionMode) {
                SelectionTopBar(
                    selectedCount = uiState.selectedIds.size,
                    visible = true,
                    onClose = { viewModel.clearSelection() },
                    onSelectAll = { viewModel.selectAll() },
                    onDelete = { viewModel.deleteSelected() },
                    onShare = { viewModel.shareSelected() },
                    onFavorite = { viewModel.favoriteSelected() },
                    onMoveToAlbum = { showMoveDialog = true },
                    onCopyToAlbum = { showCopyDialog = true },
                    onRename = { showRenameDialog = true },
                    onConvertToPdf = { showPdfDialog = true },
                    onSetAsWallpaper = {
                        viewModel.setAsWallpaperSelected { success ->
                            if (success) {
                                android.widget.Toast.makeText(context, "Wallpaper set successfully", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                android.widget.Toast.makeText(context, "Failed to set wallpaper", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = "Videos",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(bottom = bottomPadding)
        ) {
            when {
                uiState.isLoading && uiState.videos.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                uiState.videos.isEmpty() -> {
                    EmptyState(type = EmptyStateType.VIDEOS)
                }
                else -> {
                    PullToRefreshBox(
                        isRefreshing = uiState.isLoading,
                        onRefresh = { viewModel.syncVideos() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        PhotoGrid(
                            photos = uiState.videos,
                            selectedIds = uiState.selectedIds,
                            isSelectionMode = uiState.isSelectionMode,
                            onPhotoClick = { photo ->
                                if (uiState.isSelectionMode) {
                                    viewModel.toggleSelection(photo.id)
                                } else {
                                    onPhotoClick(photo.id)
                                }
                            },
                            onPhotoLongClick = { photo ->
                                viewModel.toggleSelection(photo.id)
                            },
                            groupByDate = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
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
                    uiState.videos.find { it.id == uiState.selectedIds.first() }?.name ?: ""
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
}

