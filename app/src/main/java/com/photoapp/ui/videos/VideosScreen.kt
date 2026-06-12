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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.photoapp.ui.components.EmptyState
import com.photoapp.ui.components.EmptyStateType
import com.photoapp.ui.components.PhotoGrid
import com.photoapp.ui.components.SelectionTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideosScreen(
    onPhotoClick: (Long) -> Unit,
    bottomPadding: Dp = 0.dp,
    viewModel: VideosViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
                    onFavorite = { viewModel.favoriteSelected() }
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
        }
    }
}
