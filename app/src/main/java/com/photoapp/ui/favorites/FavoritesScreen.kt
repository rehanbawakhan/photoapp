package com.photoapp.ui.favorites

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.photoapp.ui.components.EmptyState
import com.photoapp.ui.components.EmptyStateType
import com.photoapp.ui.components.PhotoGrid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onPhotoClick: (Long) -> Unit,
    bottomPadding: Dp = 0.dp,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Favorites",
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
        if (uiState.photos.isEmpty() && !uiState.isLoading) {
            EmptyState(
                type = EmptyStateType.FAVORITES,
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            PhotoGrid(
                photos = uiState.photos,
                selectedIds = emptySet(),
                isSelectionMode = false,
                onPhotoClick = { onPhotoClick(it.id) },
                onPhotoLongClick = { },
                groupByDate = false,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(bottom = bottomPadding)
            )
        }
    }
}
