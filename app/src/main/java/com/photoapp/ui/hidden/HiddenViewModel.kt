package com.photoapp.ui.hidden

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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HiddenUiState(
    val photos: List<PhotoEntity> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class HiddenViewModel @Inject constructor(
    private val repository: PhotoRepository,
    private val securityManager: com.photoapp.data.security.HiddenSecurityManager
) : ViewModel() {

    private val _deleteIntentSender = MutableStateFlow<IntentSender?>(null)
    val deleteIntentSender: StateFlow<IntentSender?> = _deleteIntentSender.asStateFlow()

    private var pendingDeleteIds = emptyList<Long>()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _isLoading = MutableStateFlow(false)

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated = _isAuthenticated.asStateFlow()

    fun isPinSet(): Boolean = securityManager.isPinSet()

    fun savePin(pin: String) {
        securityManager.savePin(pin)
        _isAuthenticated.value = true
    }

    fun verifyPin(pin: String): Boolean {
        val success = securityManager.verifyPin(pin)
        if (success) {
            _isAuthenticated.value = true
        }
        return success
    }

    fun setAuthenticated(auth: Boolean) {
        _isAuthenticated.value = auth
    }

    val uiState: StateFlow<HiddenUiState> = combine(
        repository.getHiddenPhotos(),
        _selectedIds,
        _isLoading
    ) { photos, selectedIds, isLoading ->
        HiddenUiState(
            photos = photos,
            selectedIds = selectedIds,
            isSelectionMode = selectedIds.isNotEmpty(),
            isLoading = isLoading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HiddenUiState()
    )

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

    fun unhideSelected() {
        viewModelScope.launch {
            repository.unhidePhotos(_selectedIds.value.toList())
            clearSelection()
        }
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val ids = _selectedIds.value.toList()
            if (ids.isEmpty()) return@launch
            val intentSender = repository.getDeleteIntentSender(ids)
            if (intentSender != null) {
                pendingDeleteIds = ids
                _deleteIntentSender.value = intentSender
            } else {
                repository.deleteFromDatabaseMultiple(ids)
                clearSelection()
            }
        }
    }

    fun onPhotosDeletedConfirm() {
        viewModelScope.launch {
            repository.deleteFromDatabaseMultiple(pendingDeleteIds)
            pendingDeleteIds = emptyList()
            _deleteIntentSender.value = null
            clearSelection()
        }
    }

    fun clearDeleteIntentSender() {
        _deleteIntentSender.value = null
    }
}
