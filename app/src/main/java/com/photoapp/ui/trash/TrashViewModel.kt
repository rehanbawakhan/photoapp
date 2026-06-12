package com.photoapp.ui.trash

import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photoapp.data.local.entities.PhotoEntity
import com.photoapp.data.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrashUiState(
    val photos: List<PhotoEntity> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val repository: PhotoRepository
) : ViewModel() {

    private val _deleteIntentSender = MutableStateFlow<IntentSender?>(null)
    val deleteIntentSender: StateFlow<IntentSender?> = _deleteIntentSender.asStateFlow()

    private var pendingDeleteIds = emptyList<Long>()

    val uiState: StateFlow<TrashUiState> = repository.getTrashPhotos()
        .map { photos ->
            TrashUiState(
                photos = photos,
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TrashUiState()
        )

    fun restorePhoto(photoId: Long) {
        viewModelScope.launch {
            repository.restoreFromTrash(photoId)
        }
    }

    fun restoreAll() {
        viewModelScope.launch {
            repository.restoreAllFromTrash()
        }
    }

    fun permanentlyDelete(photoId: Long) {
        viewModelScope.launch {
            val ids = listOf(photoId)
            val intentSender = repository.getDeleteIntentSender(ids)
            if (intentSender != null) {
                pendingDeleteIds = ids
                _deleteIntentSender.value = intentSender
            } else {
                repository.deleteFromDatabaseMultiple(ids)
            }
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            val ids = uiState.value.photos.map { it.id }
            if (ids.isEmpty()) return@launch
            val intentSender = repository.getDeleteIntentSender(ids)
            if (intentSender != null) {
                pendingDeleteIds = ids
                _deleteIntentSender.value = intentSender
            } else {
                repository.deleteFromDatabaseMultiple(ids)
            }
        }
    }

    fun onPhotosDeletedConfirm() {
        viewModelScope.launch {
            repository.deleteFromDatabaseMultiple(pendingDeleteIds)
            pendingDeleteIds = emptyList()
            _deleteIntentSender.value = null
        }
    }

    fun clearDeleteIntentSender() {
        _deleteIntentSender.value = null
    }
}
