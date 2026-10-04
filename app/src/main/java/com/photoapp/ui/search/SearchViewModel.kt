package com.photoapp.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photoapp.data.local.entities.PhotoEntity
import com.photoapp.data.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class SearchCategory(val label: String) {
    ALL("All"),
    IMAGES("Images"),
    VIDEOS("Videos"),
    FAVORITES("Favorites"),
    ALBUMS("Albums"),
    FOLDERS("Folders"),
    DATE_YEAR("Date / Year"),
    FILE_TYPES("File Types")
}

data class SearchUiState(
    val query: String = "",
    val activeCategory: SearchCategory = SearchCategory.ALL,
    val results: List<PhotoEntity> = emptyList(),
    val totalCount: Int = 0,
    val isSearching: Boolean = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: PhotoRepository
) : ViewModel() {

    val query = MutableStateFlow("")
    val activeCategory = MutableStateFlow(SearchCategory.ALL)

    private val allPhotos = repository.getAllPhotos()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<SearchUiState> = combine(
        allPhotos,
        query,
        activeCategory
    ) { photos, queryText, category ->
        val trimmed = queryText.trim().lowercase()

        val filtered = photos.filter { photo ->
            // Category filter
            val matchesCategory = when (category) {
                SearchCategory.ALL -> true
                SearchCategory.IMAGES -> photo.mimeType.startsWith("image/")
                SearchCategory.VIDEOS -> photo.mimeType.startsWith("video/")
                SearchCategory.FAVORITES -> photo.isFavorite
                SearchCategory.ALBUMS -> photo.bucketName != null
                SearchCategory.FOLDERS -> true
                SearchCategory.DATE_YEAR -> true
                SearchCategory.FILE_TYPES -> true
            }

            if (!matchesCategory) return@filter false

            if (trimmed.isEmpty()) return@filter true

            // Match attributes
            val nameMatch = photo.name.lowercase().contains(trimmed)
            val pathMatch = photo.path.lowercase().contains(trimmed)
            val bucketMatch = photo.bucketName?.lowercase()?.contains(trimmed) == true
            val mimeMatch = photo.mimeType.lowercase().contains(trimmed)
            val extMatch = photo.name.substringAfterLast('.', "").lowercase().contains(trimmed)

            // Date / Month / Year formatting search
            val dateTaken = Date(photo.dateTaken)
            val dateFormatFull = SimpleDateFormat("yyyy MMMM MM dd EEEE MMM", Locale.ENGLISH)
            val dateStr = dateFormatFull.format(dateTaken).lowercase()
            val dateMatch = dateStr.contains(trimmed)

            nameMatch || pathMatch || bucketMatch || mimeMatch || extMatch || dateMatch
        }

        SearchUiState(
            query = queryText,
            activeCategory = category,
            results = filtered,
            totalCount = filtered.size,
            isSearching = trimmed.isNotEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SearchUiState()
    )

    fun onQueryChange(newQuery: String) {
        query.value = newQuery
    }

    fun onCategorySelect(category: SearchCategory) {
        activeCategory.value = category
    }

    fun clearQuery() {
        query.value = ""
    }
}
