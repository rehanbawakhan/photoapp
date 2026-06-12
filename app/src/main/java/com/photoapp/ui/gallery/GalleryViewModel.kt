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

data class GalleryUiState(
    val photos: List<PhotoEntity> = emptyList(),
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
        _selectedIds,
        _isLoading,
        _searchQuery
    ) { photos, selectedIds, isLoading, searchQuery ->
        GalleryUiState(
            photos = photos,
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
}
