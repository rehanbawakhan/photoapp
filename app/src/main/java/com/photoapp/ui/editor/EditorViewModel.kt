package com.photoapp.ui.editor

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photoapp.data.local.entities.PhotoEntity
import com.photoapp.data.repository.PhotoRepository
import com.photoapp.util.AiEditingEngine
import com.photoapp.util.ImageUtils
import com.photoapp.util.PhotoFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditorUiState(
    val photo: PhotoEntity? = null,
    val originalBitmap: Bitmap? = null,
    val currentBitmap: Bitmap? = null,
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val warmth: Float = 0f,
    val selectedFilter: PhotoFilter = PhotoFilter.NONE,
    val selectedTab: EditorTab = EditorTab.ADJUST,
    val rotationDegrees: Float = 0f,
    val isFlippedH: Boolean = false,
    val isFlippedV: Boolean = false,
    val hasChanges: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val isLoading: Boolean = true,
    // AI editing state
    val aiMode: AiEditMode = AiEditMode.NONE,
    val aiPreviewBitmap: Bitmap? = null,
    val isAiProcessing: Boolean = false,
    val aiError: String? = null,
    val blurRadius: Int = 15,
    val bgFill: AiEditingEngine.BackgroundFill = AiEditingEngine.BackgroundFill.TRANSPARENT
)

enum class EditorTab {
    ADJUST, FILTERS, CROP, AI
}

enum class AiEditMode {
    NONE, BACKGROUND_BLUR, BACKGROUND_REMOVE, AUTO_ENHANCE, OBJECT_HIGHLIGHT
}

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val repository: PhotoRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val photoId: Long = savedStateHandle.get<Long>("photoId") ?: -1L
    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    init {
        loadPhoto()
    }

    private fun loadPhoto() {
        viewModelScope.launch {
            val photo = repository.getPhotoById(photoId) ?: return@launch
            val bitmap = ImageUtils.loadBitmap(context, photo.contentUri)

            _uiState.update {
                it.copy(
                    photo = photo,
                    originalBitmap = bitmap,
                    currentBitmap = bitmap,
                    isLoading = false
                )
            }
        }
    }

    fun selectTab(tab: EditorTab) {
        _uiState.update {
            it.copy(
                selectedTab = tab,
                // Clear AI preview when switching away from AI tab
                aiPreviewBitmap = if (tab != EditorTab.AI) null else it.aiPreviewBitmap,
                aiMode = if (tab != EditorTab.AI) AiEditMode.NONE else it.aiMode,
                aiError = null
            )
        }
    }

    fun setBrightness(value: Float) {
        _uiState.update { it.copy(brightness = value, hasChanges = true) }
    }

    fun setContrast(value: Float) {
        _uiState.update { it.copy(contrast = value, hasChanges = true) }
    }

    fun setSaturation(value: Float) {
        _uiState.update { it.copy(saturation = value, hasChanges = true) }
    }

    fun setWarmth(value: Float) {
        _uiState.update { it.copy(warmth = value, hasChanges = true) }
    }

    fun selectFilter(filter: PhotoFilter) {
        _uiState.update { it.copy(selectedFilter = filter, hasChanges = true) }
    }

    fun rotateLeft() {
        _uiState.update {
            it.copy(
                rotationDegrees = (it.rotationDegrees - 90f) % 360f,
                hasChanges = true
            )
        }
    }

    fun rotateRight() {
        _uiState.update {
            it.copy(
                rotationDegrees = (it.rotationDegrees + 90f) % 360f,
                hasChanges = true
            )
        }
    }

    fun flipHorizontal() {
        _uiState.update {
            it.copy(isFlippedH = !it.isFlippedH, hasChanges = true)
        }
    }

    fun flipVertical() {
        _uiState.update {
            it.copy(isFlippedV = !it.isFlippedV, hasChanges = true)
        }
    }

    // ──────────────────────────────────────────────
    // AI EDITING FUNCTIONS
    // ──────────────────────────────────────────────

    fun setBlurRadius(radius: Int) {
        _uiState.update { it.copy(blurRadius = radius.coerceIn(1, 25)) }
    }

    fun setBgFill(fill: AiEditingEngine.BackgroundFill) {
        _uiState.update { it.copy(bgFill = fill) }
    }

    fun applyBackgroundBlur() {
        val bitmap = _uiState.value.originalBitmap ?: return
        _uiState.update { it.copy(isAiProcessing = true, aiMode = AiEditMode.BACKGROUND_BLUR, aiError = null) }

        viewModelScope.launch {
            try {
                val result = AiEditingEngine.blurBackground(bitmap, _uiState.value.blurRadius)
                _uiState.update {
                    it.copy(
                        aiPreviewBitmap = result,
                        isAiProcessing = false,
                        hasChanges = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAiProcessing = false,
                        aiError = "Could not detect subject: ${e.message}"
                    )
                }
            }
        }
    }

    fun applyBackgroundRemove() {
        val bitmap = _uiState.value.originalBitmap ?: return
        _uiState.update { it.copy(isAiProcessing = true, aiMode = AiEditMode.BACKGROUND_REMOVE, aiError = null) }

        viewModelScope.launch {
            try {
                val result = AiEditingEngine.removeBackground(bitmap, _uiState.value.bgFill)
                _uiState.update {
                    it.copy(
                        aiPreviewBitmap = result,
                        isAiProcessing = false,
                        hasChanges = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAiProcessing = false,
                        aiError = "Could not detect subject: ${e.message}"
                    )
                }
            }
        }
    }

    fun applyAutoEnhance() {
        val bitmap = _uiState.value.originalBitmap ?: return
        _uiState.update { it.copy(isAiProcessing = true, aiMode = AiEditMode.AUTO_ENHANCE, aiError = null) }

        viewModelScope.launch {
            try {
                val params = AiEditingEngine.analyzeForAutoEnhance(bitmap)
                // Apply the analyzed params to the adjustment sliders
                _uiState.update {
                    it.copy(
                        brightness = params.brightness,
                        contrast = params.contrast,
                        saturation = params.saturation,
                        warmth = params.warmth,
                        isAiProcessing = false,
                        hasChanges = true,
                        aiMode = AiEditMode.AUTO_ENHANCE
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAiProcessing = false,
                        aiError = "Enhancement failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun applyObjectHighlight() {
        val bitmap = _uiState.value.originalBitmap ?: return
        _uiState.update { it.copy(isAiProcessing = true, aiMode = AiEditMode.OBJECT_HIGHLIGHT, aiError = null) }

        viewModelScope.launch {
            try {
                val result = AiEditingEngine.highlightSubject(bitmap)
                _uiState.update {
                    it.copy(
                        aiPreviewBitmap = result,
                        isAiProcessing = false,
                        hasChanges = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAiProcessing = false,
                        aiError = "Could not detect subject: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearAiPreview() {
        _uiState.update {
            it.copy(
                aiPreviewBitmap = null,
                aiMode = AiEditMode.NONE,
                aiError = null
            )
        }
    }

    fun applyAiEdit() {
        // When the user confirms an AI edit, the aiPreviewBitmap becomes the new "original"
        // for further edits or saving
        val aiBitmap = _uiState.value.aiPreviewBitmap ?: return
        _uiState.update {
            it.copy(
                currentBitmap = aiBitmap,
                originalBitmap = aiBitmap,
                aiPreviewBitmap = null,
                aiMode = AiEditMode.NONE,
                hasChanges = true
            )
        }
    }

    // ──────────────────────────────────────────────

    fun saveAsCopy() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val state = _uiState.value
            var bitmap = state.aiPreviewBitmap ?: state.originalBitmap ?: return@launch

            // Apply color adjustments
            val adjustmentMatrix = ImageUtils.createAdjustmentMatrix(
                brightness = state.brightness,
                contrast = state.contrast,
                saturation = state.saturation,
                warmth = state.warmth
            )

            // Apply filter
            if (state.selectedFilter != PhotoFilter.NONE) {
                adjustmentMatrix.postConcat(state.selectedFilter.matrix)
            }

            bitmap = ImageUtils.applyColorMatrix(bitmap, adjustmentMatrix)

            // Apply rotation
            if (state.rotationDegrees != 0f) {
                bitmap = ImageUtils.rotateBitmap(bitmap, state.rotationDegrees)
            }

            // Apply flips
            if (state.isFlippedH) {
                bitmap = ImageUtils.flipBitmap(bitmap, horizontal = true)
            }
            if (state.isFlippedV) {
                bitmap = ImageUtils.flipBitmap(bitmap, horizontal = false)
            }

            // Save
            val savedFile = ImageUtils.saveBitmap(bitmap)

            _uiState.update {
                it.copy(
                    isSaving = false,
                    isSaved = savedFile != null
                )
            }

            // Sync to pick up the new file
            if (savedFile != null) {
                repository.syncPhotos()
            }
        }
    }

    fun resetAll() {
        _uiState.update {
            it.copy(
                brightness = 0f,
                contrast = 0f,
                saturation = 0f,
                warmth = 0f,
                selectedFilter = PhotoFilter.NONE,
                rotationDegrees = 0f,
                isFlippedH = false,
                isFlippedV = false,
                hasChanges = false,
                aiPreviewBitmap = null,
                aiMode = AiEditMode.NONE,
                aiError = null
            )
        }
    }
}
