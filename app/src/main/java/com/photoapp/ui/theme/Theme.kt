package com.photoapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.photoapp.data.settings.ThemeSettingsManager

private val DarkColorScheme = darkColorScheme(
    primary = Primary80,
    onPrimary = Primary20,
    primaryContainer = Primary30,
    onPrimaryContainer = Primary90,
    secondary = Secondary80,
    onSecondary = Secondary20,
    secondaryContainer = Secondary30,
    onSecondaryContainer = Secondary90,
    tertiary = Tertiary80,
    onTertiary = Tertiary20,
    tertiaryContainer = Tertiary30,
    onTertiaryContainer = Tertiary90,
    error = Error80,
    onError = Error20,
    errorContainer = Error30,
    onErrorContainer = Error90,
    background = Neutral6,
    onBackground = Neutral90,
    surface = Neutral6,
    onSurface = Neutral90,
    surfaceVariant = NeutralVariant30,
    onSurfaceVariant = NeutralVariant80,
    outline = NeutralVariant60,
    outlineVariant = NeutralVariant30,
    inverseSurface = Neutral90,
    inverseOnSurface = Neutral20,
    inversePrimary = Primary40,
    surfaceTint = Primary80,
    surfaceContainerLowest = Neutral4,
    surfaceContainerLow = Neutral10,
    surfaceContainer = Neutral12,
    surfaceContainerHigh = Neutral17,
    surfaceContainerHighest = Neutral22,
    scrim = Neutral0
)

private val AmoledColorScheme = darkColorScheme(
    primary = Primary80,
    onPrimary = Primary20,
    primaryContainer = Primary30,
    onPrimaryContainer = Primary90,
    secondary = Secondary80,
    onSecondary = Secondary20,
    secondaryContainer = Secondary30,
    onSecondaryContainer = Secondary90,
    tertiary = Tertiary80,
    onTertiary = Tertiary20,
    tertiaryContainer = Tertiary30,
    onTertiaryContainer = Tertiary90,
    error = Error80,
    onError = Error20,
    errorContainer = Error30,
    onErrorContainer = Error90,
    background = Color.Black,
    onBackground = Neutral90,
    surface = Color.Black,
    onSurface = Neutral90,
    surfaceVariant = Color(0xFF121212),
    onSurfaceVariant = NeutralVariant80,
    outline = Color(0xFF242424),
    outlineVariant = NeutralVariant30,
    inverseSurface = Neutral90,
    inverseOnSurface = Neutral20,
    inversePrimary = Primary40,
    surfaceTint = Primary80,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF070707),
    surfaceContainer = Color(0xFF0D0D0D),
    surfaceContainerHigh = Color(0xFF151515),
    surfaceContainerHighest = Color(0xFF1E1E1E),
    scrim = Neutral0
)

private val LightColorScheme = lightColorScheme(
    primary = Primary40,
    onPrimary = Neutral100,
    primaryContainer = Primary90,
    onPrimaryContainer = Primary10,
    secondary = Secondary40,
    onSecondary = Neutral100,
    secondaryContainer = Secondary90,
    onSecondaryContainer = Secondary10,
    tertiary = Tertiary40,
    onTertiary = Neutral100,
    tertiaryContainer = Tertiary90,
    onTertiaryContainer = Tertiary10,
    error = Error40,
    onError = Neutral100,
    errorContainer = Error95,
    onErrorContainer = Error10,
    background = Neutral98,
    onBackground = Neutral10,
    surface = Neutral98,
    onSurface = Neutral10,
    surfaceVariant = NeutralVariant90,
    onSurfaceVariant = NeutralVariant30,
    outline = NeutralVariant50,
    outlineVariant = NeutralVariant80,
    inverseSurface = Neutral20,
    inverseOnSurface = Neutral95,
    inversePrimary = Primary80,
    surfaceTint = Primary40,
    surfaceContainerLowest = Neutral100,
    surfaceContainerLow = Neutral96,
    surfaceContainer = Neutral94,
    surfaceContainerHigh = Neutral92,
    surfaceContainerHighest = Neutral90,
    scrim = Neutral0
)

private data class Quad(
    val primary: Color,
    val primaryContainer: Color,
    val onPrimary: Color,
    val onPrimaryContainer: Color
)

private fun ColorScheme.withAccent(accent: String, isDark: Boolean): ColorScheme {
    val (primaryColor, primaryContainerColor, onPrimaryColor, onPrimaryContainerColor) = when (accent) {
        ThemeSettingsManager.ACCENT_RED -> {
            if (isDark) {
                Quad(Color(0xFFFF5252), Color(0xFF680005), Color(0xFFFFFFFF), Color(0xFFFFDAD6))
            } else {
                Quad(Color(0xFFC00010), Color(0xFFFFDAD6), Color(0xFFFFFFFF), Color(0xFF3F0002))
            }
        }
        ThemeSettingsManager.ACCENT_PURPLE -> {
            if (isDark) {
                Quad(Color(0xFFCDBFFF), Color(0xFF4520B0), Color(0xFF2D0F8A), Color(0xFFE8DEFF))
            } else {
                Quad(Color(0xFF5E35D5), Color(0xFFE8DEFF), Color(0xFFFFFFFF), Color(0xFF1B0261))
            }
        }
        ThemeSettingsManager.ACCENT_WHITE -> {
            if (isDark) {
                Quad(Color(0xFFFFFFFF), Color(0xFF2B2B2B), Color(0xFF000000), Color(0xFFFFFFFF))
            } else {
                Quad(Color(0xFF000000), Color(0xFFE0E0E0), Color(0xFFFFFFFF), Color(0xFF000000))
            }
        }
        ThemeSettingsManager.ACCENT_LIGHT_BLUE -> {
            if (isDark) {
                Quad(Color(0xFF80DEEA), Color(0xFF004D40), Color(0xFF002020), Color(0xFFB2EBF2))
            } else {
                Quad(Color(0xFF007A7A), Color(0xFFB2EBF2), Color(0xFFFFFFFF), Color(0xFF002020))
            }
        }
        ThemeSettingsManager.ACCENT_DEEP_BLUE -> {
            if (isDark) {
                Quad(Color(0xFF90CAF9), Color(0xFF0D47A1), Color(0xFF0D1B2A), Color(0xFFE3F2FD))
            } else {
                Quad(Color(0xFF1565C0), Color(0xFFE3F2FD), Color(0xFFFFFFFF), Color(0xFF0D47A1))
            }
        }
        else -> { // fallback to red
            if (isDark) {
                Quad(Color(0xFFFF5252), Color(0xFF680005), Color(0xFFFFFFFF), Color(0xFFFFDAD6))
            } else {
                Quad(Color(0xFFC00010), Color(0xFFFFDAD6), Color(0xFFFFFFFF), Color(0xFF3F0002))
            }
        }
    }
    return this.copy(
        primary = primaryColor,
        primaryContainer = primaryContainerColor,
        onPrimary = onPrimaryColor,
        onPrimaryContainer = onPrimaryContainerColor,
        surfaceTint = primaryColor
    )
}

@Composable
fun PhotoAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { ThemeSettingsManager.getInstance(context) }
    val themeMode by settingsManager.themeMode.collectAsState(initial = ThemeSettingsManager.THEME_SYSTEM)
    val accentColor by settingsManager.accentColor.collectAsState(initial = ThemeSettingsManager.ACCENT_RED)

    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeSettingsManager.THEME_LIGHT -> false
        ThemeSettingsManager.THEME_DARK, ThemeSettingsManager.THEME_AMOLED -> true
        else -> isSystemDark
    }

    val baseColorScheme = when (themeMode) {
        ThemeSettingsManager.THEME_LIGHT -> LightColorScheme
        ThemeSettingsManager.THEME_DARK -> DarkColorScheme
        ThemeSettingsManager.THEME_AMOLED -> AmoledColorScheme
        else -> if (isSystemDark) DarkColorScheme else LightColorScheme
    }

    val colorScheme = baseColorScheme.withAccent(accentColor, isDark)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !isDark
                isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
