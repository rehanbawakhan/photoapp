package com.photoapp.data.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemeSettingsManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _themeMode = MutableStateFlow(getSavedThemeMode())
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _accentColor = MutableStateFlow(getSavedAccentColor())
    val accentColor: StateFlow<String> = _accentColor.asStateFlow()

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            KEY_THEME_MODE -> _themeMode.value = getSavedThemeMode()
            KEY_ACCENT_COLOR -> _accentColor.value = getSavedAccentColor()
        }
    }

    init {
        // Observe preference changes to keep flows updated
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    private fun getSavedThemeMode(): String {
        return prefs.getString(KEY_THEME_MODE, THEME_SYSTEM) ?: THEME_SYSTEM
    }

    private fun getSavedAccentColor(): String {
        return prefs.getString(KEY_ACCENT_COLOR, ACCENT_RED) ?: ACCENT_RED
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
    }

    fun setAccentColor(color: String) {
        prefs.edit().putString(KEY_ACCENT_COLOR, color).apply()
    }

    companion object {
        private const val PREFS_NAME = "theme_settings_prefs"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_ACCENT_COLOR = "accent_color"

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
