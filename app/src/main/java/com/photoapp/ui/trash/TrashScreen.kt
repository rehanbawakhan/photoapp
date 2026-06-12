package com.photoapp.ui.trash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.photoapp.data.local.entities.PhotoEntity
import com.photoapp.ui.components.EmptyState
import com.photoapp.ui.components.EmptyStateType
import com.photoapp.ui.components.VerticalScrollbar
import com.photoapp.util.DateUtils
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import android.app.Activity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    bottomPadding: Dp = 0.dp,
    viewModel: TrashViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showEmptyTrashDialog by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()

    // Intent sender launcher for permanent deletion on Android 10+
    val deleteIntentSender by viewModel.deleteIntentSender.collectAsState()
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onPhotosDeletedConfirm()
        } else {
            viewModel.clearDeleteIntentSender()
        }
    }

    LaunchedEffect(deleteIntentSender) {
        deleteIntentSender?.let { intentSender ->
            val intentSenderRequest = IntentSenderRequest.Builder(intentSender).build()
            deleteLauncher.launch(intentSenderRequest)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Trash",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (uiState.photos.isNotEmpty()) {
                        TextButton(onClick = { viewModel.restoreAll() }) {
                            Icon(
                                Icons.Default.RestoreFromTrash,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text("Restore All")
                        }
                        IconButton(onClick = { showEmptyTrashDialog = true }) {
                            Icon(
                                Icons.Default.DeleteForever,
                                contentDescription = "Empty trash",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (uiState.photos.isEmpty() && !uiState.isLoading) {
            EmptyState(
                type = EmptyStateType.TRASH,
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(bottom = bottomPadding)
            ) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = uiState.photos,
                        key = { it.id }
                    ) { photo ->
                        TrashPhotoItem(
                            photo = photo,
                            onRestore = { viewModel.restorePhoto(photo.id) },
                            onDelete = { viewModel.permanentlyDelete(photo.id) }
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
                        uiState.photos.getOrNull(index)?.let { photo ->
                            com.photoapp.ui.components.getMonthGroupLabel(photo.dateTaken)
                        } ?: ""
                    }
                )
            }
        }
    }

    // Empty trash confirmation
    if (showEmptyTrashDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashDialog = false },
            title = { Text("Empty Trash") },
            text = {
                Text(
                    "All ${uiState.photos.size} photos in trash will be permanently deleted. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.emptyTrash()
                    showEmptyTrashDialog = false
                }) {
                    Text("Empty Trash", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun TrashPhotoItem(
    photo: PhotoEntity,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val daysRemaining = photo.dateDeleted?.let {
        DateUtils.getDaysRemaining(it)
    } ?: 30

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(photo.contentUri)
                .crossfade(true)
                .size(300)
                .build(),
            contentDescription = photo.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Days remaining badge
        Badge(
            containerColor = if (daysRemaining <= 7) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
            contentColor = if (daysRemaining <= 7) {
                MaterialTheme.colorScheme.onError
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
        ) {
            Text(
                text = "${daysRemaining}d",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // Action buttons at bottom
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(
                onClick = onRestore,
                modifier = Modifier
                    .weight(1f)
            ) {
                Icon(
                    Icons.Default.RestoreFromTrash,
                    contentDescription = "Restore",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier
                    .weight(1f)
            ) {
                Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = "Delete permanently",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Permanently") },
            text = { Text("This photo will be permanently deleted and cannot be recovered.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
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
}
