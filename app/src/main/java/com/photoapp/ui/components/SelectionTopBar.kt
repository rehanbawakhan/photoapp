package com.photoapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopBar(
    selectedCount: Int,
    visible: Boolean,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onFavorite: () -> Unit,
    onMoveToAlbum: (() -> Unit)? = null,
    onCopyToAlbum: (() -> Unit)? = null,
    onRename: (() -> Unit)? = null,
    onConvertToPdf: (() -> Unit)? = null,
    onSetAsWallpaper: (() -> Unit)? = null,
    onHide: (() -> Unit)? = null,
    onUnhide: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { -it },
        exit = slideOutVertically { -it },
        modifier = modifier
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "$selectedCount selected",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close selection"
                    )
                }
            },
            actions = {
                IconButton(onClick = onSelectAll) {
                    Icon(
                        imageVector = Icons.Filled.SelectAll,
                        contentDescription = "Select all"
                    )
                }
                IconButton(onClick = onFavorite) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Favorite"
                    )
                }
                IconButton(onClick = onShare) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Share"
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete"
                    )
                }
                if (onMoveToAlbum != null || onCopyToAlbum != null || onRename != null || onConvertToPdf != null || onSetAsWallpaper != null || onHide != null || onUnhide != null) {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options"
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (onMoveToAlbum != null) {
                                DropdownMenuItem(
                                    text = { Text("Move to album") },
                                    onClick = {
                                        showMenu = false
                                        onMoveToAlbum()
                                    }
                                )
                            }
                            if (onCopyToAlbum != null) {
                                DropdownMenuItem(
                                    text = { Text("Copy to album") },
                                    onClick = {
                                        showMenu = false
                                        onCopyToAlbum()
                                    }
                                )
                            }
                            if (onRename != null) {
                                DropdownMenuItem(
                                    text = { Text("Rename") },
                                    onClick = {
                                        showMenu = false
                                        onRename()
                                    }
                                )
                            }
                            if (onConvertToPdf != null) {
                                DropdownMenuItem(
                                    text = { Text("Convert to PDF") },
                                    onClick = {
                                        showMenu = false
                                        onConvertToPdf()
                                    }
                                )
                            }
                            if (onSetAsWallpaper != null) {
                                DropdownMenuItem(
                                    text = { Text("Set as wallpaper") },
                                    onClick = {
                                        showMenu = false
                                        onSetAsWallpaper()
                                    }
                                )
                            }
                            if (onHide != null) {
                                DropdownMenuItem(
                                    text = { Text("Hide") },
                                    onClick = {
                                        showMenu = false
                                        onHide()
                                    }
                                )
                            }
                            if (onUnhide != null) {
                                DropdownMenuItem(
                                    text = { Text("Unhide") },
                                    onClick = {
                                        showMenu = false
                                        onUnhide()
                                    }
                                )
                            }
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            modifier = Modifier.statusBarsPadding()
        )
    }
}
