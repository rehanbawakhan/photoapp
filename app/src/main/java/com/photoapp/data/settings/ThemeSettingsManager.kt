package com.photoapp.data.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemeSettingsManager private constructor(context: Context) : SharedPreferences.OnSharedPreferenceChangeListener {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _themeMode = MutableStateFlow(getSavedThemeMode())
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _accentColor = MutableStateFlow(getSavedAccentColor())
    val accentColor: StateFlow<String> = _accentColor.asStateFlow()

    private val _photoGridColumns = MutableStateFlow(getSavedPhotoGridColumns())
    val photoGridColumns: StateFlow<Int> = _photoGridColumns.asStateFlow()

    private val _videoGridColumns = MutableStateFlow(getSavedVideoGridColumns())
    val videoGridColumns: StateFlow<Int> = _videoGridColumns.asStateFlow()

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            KEY_THEME_MODE -> _themeMode.value = getSavedThemeMode()
            KEY_ACCENT_COLOR -> _accentColor.value = getSavedAccentColor()
            KEY_PHOTO_GRID_COLUMNS -> _photoGridColumns.value = getSavedPhotoGridColumns()
            KEY_VIDEO_GRID_COLUMNS -> _videoGridColumns.value = getSavedVideoGridColumns()
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(this)
    }

    private fun getSavedThemeMode(): String {
        return prefs.getString(KEY_THEME_MODE, THEME_SYSTEM) ?: THEME_SYSTEM
    }

    private fun getSavedAccentColor(): String {
        return prefs.getString(KEY_ACCENT_COLOR, ACCENT_RED) ?: ACCENT_RED
    }

    private fun getSavedPhotoGridColumns(): Int {
        return prefs.getInt(KEY_PHOTO_GRID_COLUMNS, 3).coerceIn(2, 8)
    }

    private fun getSavedVideoGridColumns(): Int {
        return prefs.getInt(KEY_VIDEO_GRID_COLUMNS, 3).coerceIn(2, 8)
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
        _themeMode.value = mode
    }

    fun setAccentColor(color: String) {
        prefs.edit().putString(KEY_ACCENT_COLOR, color).apply()
        _accentColor.value = color
    }

    fun setPhotoGridColumns(count: Int) {
        val validCount = count.coerceIn(2, 8)
        prefs.edit().putInt(KEY_PHOTO_GRID_COLUMNS, validCount).apply()
        _photoGridColumns.value = validCount
    }

    fun setVideoGridColumns(count: Int) {
        val validCount = count.coerceIn(2, 8)
        prefs.edit().putInt(KEY_VIDEO_GRID_COLUMNS, validCount).apply()
        _videoGridColumns.value = validCount
    }

    companion object {
        private const val PREFS_NAME = "theme_settings_prefs"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_ACCENT_COLOR = "accent_color"
        private const val KEY_PHOTO_GRID_COLUMNS = "photo_grid_columns"
        private const val KEY_VIDEO_GRID_COLUMNS = "video_grid_columns"

        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_AMOLED = "amoled"

        const val ACCENT_WHITE = "white"
        const val ACCENT_RED = "red"
        const val ACCENT_PURPLE = "purple"
        const val ACCENT_LIGHT_BLUE = "lightblue"
        const val ACCENT_DEEP_BLUE = "deepblue"

        @Volatile
        private var INSTANCE: ThemeSettingsManager? = null

        fun getInstance(context: Context): ThemeSettingsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ThemeSettingsManager(context).also { INSTANCE = it }
            }
        }
    }
}
