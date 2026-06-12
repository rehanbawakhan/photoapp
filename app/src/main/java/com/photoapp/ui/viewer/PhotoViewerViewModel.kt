package com.photoapp.ui.viewer

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photoapp.data.local.entities.PhotoEntity
import com.photoapp.data.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ViewerUiState(
    val initialPhotoId: Long = -1L,
    val allPhotos: List<PhotoEntity> = emptyList(),
    val showInfo: Boolean = false,
    val showControls: Boolean = true,
    val isDeleted: Boolean = false
)

@HiltViewModel
class PhotoViewerViewModel @Inject constructor(
    private val repository: PhotoRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val initialPhotoId: Long = savedStateHandle.get<Long>("photoId") ?: -1L
    private val albumId: String? = savedStateHandle.get<String>("albumId")
    private val favoritesOnly: Boolean = savedStateHandle.get<Boolean>("favoritesOnly") ?: false
    private val videosOnly: Boolean = savedStateHandle.get<Boolean>("videosOnly") ?: false

    private val _showInfo = MutableStateFlow(false)
    private val _showControls = MutableStateFlow(true)
    private val _isDeleted = MutableStateFlow(false)

    // Load appropriate photos flow depending on the entry screen context
    private val photosFlow = when {
        favoritesOnly -> repository.getFavoritePhotos()
        albumId != null -> repository.getPhotosByBucket(albumId)
        videosOnly -> repository.getAllPhotos().map { list -> list.filter { it.mimeType.startsWith("video/") } }
        else -> repository.getAllPhotos()
    }

    val uiState: StateFlow<ViewerUiState> = combine(
        photosFlow,
        _showInfo,
        _showControls,
        _isDeleted
    ) { allPhotos, showInfo, showControls, isDeleted ->
        ViewerUiState(
            initialPhotoId = initialPhotoId,
            allPhotos = allPhotos,
            showInfo = showInfo,
            showControls = showControls,
            isDeleted = isDeleted
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ViewerUiState(initialPhotoId = initialPhotoId)
    )

    fun toggleControls() {
        _showControls.value = !_showControls.value
    }

    fun setControlsVisible(visible: Boolean) {
        _showControls.value = visible
    }

    fun toggleInfo() {
        _showInfo.value = !_showInfo.value
    }

    fun toggleFavorite(photoId: Long) {
        viewModelScope.launch {
            repository.toggleFavorite(photoId)
        }
    }

    fun deletePhoto(photoId: Long) {
        viewModelScope.launch {
            repository.moveToTrash(photoId)
        }
    }

    fun sharePhoto(photo: PhotoEntity) {
        viewModelScope.launch {
            val uri = repository.getShareUri(photo.id) ?: return@launch
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = photo.mimeType
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(
                Intent.createChooser(shareIntent, "Share media").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }
}
