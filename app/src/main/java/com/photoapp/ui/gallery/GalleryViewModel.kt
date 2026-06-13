package com.photoapp.ui.gallery

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photoapp.data.local.entities.PhotoEntity
import com.photoapp.data.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.photoapp.data.local.entities.AlbumEntity

data class GalleryUiState(
    val photos: List<PhotoEntity> = emptyList(),
    val albums: List<AlbumEntity> = emptyList(),
    val isLoading: Boolean = true,
    val selectedIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false,
    val searchQuery: String = ""
)

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val repository: PhotoRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _isLoading = MutableStateFlow(true)
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<GalleryUiState> = combine(
        repository.getAllPhotos(),
        repository.getAllAlbums(),
        _selectedIds,
        _isLoading,
        _searchQuery
    ) { photos, albums, selectedIds, isLoading, searchQuery ->
        GalleryUiState(
            photos = photos,
            albums = albums,
            isLoading = isLoading,
            selectedIds = selectedIds,
            isSelectionMode = selectedIds.isNotEmpty(),
            searchQuery = searchQuery
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GalleryUiState()
    )

    init {
        syncPhotos()
    }

    fun syncPhotos() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.syncPhotos()
                repository.syncAlbums()
                repository.cleanupExpiredTrash()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleSelection(photoId: Long) {
        _selectedIds.value = _selectedIds.value.toMutableSet().apply {
            if (contains(photoId)) remove(photoId) else add(photoId)
        }
    }

    fun setSelectedIds(ids: Set<Long>) {
        _selectedIds.value = ids
    }

    fun selectAll() {
        val allIds = uiState.value.photos.map { it.id }.toSet()
        _selectedIds.value = allIds
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val ids = _selectedIds.value.toList()
            repository.moveToTrashMultiple(ids)
            clearSelection()
        }
    }

    fun favoriteSelected() {
        viewModelScope.launch {
            val ids = _selectedIds.value.toList()
            repository.setFavoriteMultiple(ids)
            clearSelection()
        }
    }

    fun shareSelected() {
        viewModelScope.launch {
            val uris = _selectedIds.value.mapNotNull { id ->
                repository.getShareUri(id)
            }
            if (uris.isNotEmpty()) {
                val shareIntent = Intent().apply {
                    if (uris.size == 1) {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, uris.first())
                        type = "image/*"
                    } else {
                        action = Intent.ACTION_SEND_MULTIPLE
                        putParcelableArrayListExtra(
                            Intent.EXTRA_STREAM,
                            ArrayList(uris)
                        )
                        type = "image/*"
                    }
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(
                    Intent.createChooser(shareIntent, "Share photos").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
            clearSelection()
        }
    }

    fun copySelectedToAlbum(albumName: String) {
        viewModelScope.launch {
            repository.copyPhotosToAlbum(_selectedIds.value.toList(), albumName)
            clearSelection()
        }
    }

    fun moveSelectedToAlbum(albumName: String) {
        viewModelScope.launch {
            repository.movePhotosToAlbum(_selectedIds.value.toList(), albumName)
            clearSelection()
        }
    }

    fun renameSelected(newName: String) {
        viewModelScope.launch {
            val ids = _selectedIds.value.toList()
            if (ids.size == 1) {
                repository.renamePhoto(ids.first(), newName)
            } else {
                repository.renamePhotos(ids, newName)
            }
            clearSelection()
        }
    }

    fun convertSelectedToPdf(targetFileName: String, callback: (Uri?) -> Unit) {
        viewModelScope.launch {
            val pdfUri = repository.convertPhotosToPdf(_selectedIds.value.toList(), targetFileName)
            clearSelection()
            callback(pdfUri)
        }
    }

    fun setAsWallpaperSelected(callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val firstId = _selectedIds.value.firstOrNull()
            if (firstId != null) {
                val success = repository.setAsWallpaper(firstId)
                callback(success)
            } else {
                callback(false)
            }
            clearSelection()
        }
    }

    fun hideSelected() {
        viewModelScope.launch {
            repository.hidePhotos(_selectedIds.value.toList())
            clearSelection()
        }
    }
}
