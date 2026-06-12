package com.photoapp.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photoapp.data.local.entities.PhotoEntity
import com.photoapp.data.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesUiState(
    val photos: List<PhotoEntity> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: PhotoRepository
) : ViewModel() {

    val uiState: StateFlow<FavoritesUiState> = repository.getFavoritePhotos()
        .map { photos ->
            FavoritesUiState(
                photos = photos,
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FavoritesUiState()
        )

    fun removeFavorite(photoId: Long) {
        viewModelScope.launch {
            repository.toggleFavorite(photoId)
        }
    }
}
