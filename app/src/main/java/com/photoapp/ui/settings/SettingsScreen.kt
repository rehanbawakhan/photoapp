package com.photoapp.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.photoapp.data.settings.ThemeSettingsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { ThemeSettingsManager.getInstance(context) }
    val themeMode by settingsManager.themeMode.collectAsState(initial = ThemeSettingsManager.THEME_SYSTEM)
    val accentColor by settingsManager.accentColor.collectAsState(initial = ThemeSettingsManager.ACCENT_RED)

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            Text(
                text = "App Theme",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    ThemeOptionRow(
                        title = "System default",
                        selected = themeMode == ThemeSettingsManager.THEME_SYSTEM,
                        onClick = { settingsManager.setThemeMode(ThemeSettingsManager.THEME_SYSTEM) }
                    )
                    ThemeOptionRow(
                        title = "Light theme",
                        selected = themeMode == ThemeSettingsManager.THEME_LIGHT,
                        onClick = { settingsManager.setThemeMode(ThemeSettingsManager.THEME_LIGHT) }
                    )
                    ThemeOptionRow(
                        title = "Dark theme",
                        selected = themeMode == ThemeSettingsManager.THEME_DARK,
                        onClick = { settingsManager.setThemeMode(ThemeSettingsManager.THEME_DARK) }
                    )
                    ThemeOptionRow(
                        title = "AMOLED Black (AMOLED displays)",
                        selected = themeMode == ThemeSettingsManager.THEME_AMOLED,
                        onClick = { settingsManager.setThemeMode(ThemeSettingsManager.THEME_AMOLED) }
                    )
                }
            }

            Text(
                text = "Accent Color",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Choose accent color for buttons and controls:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        AccentColorBubble(
                            colorName = "Red",
                            bubbleColor = Color(0xFFFF5252),
                            isSelected = accentColor == ThemeSettingsManager.ACCENT_RED,
                            onClick = { settingsManager.setAccentColor(ThemeSettingsManager.ACCENT_RED) }
                        )
                        AccentColorBubble(
                            colorName = "Purple",
                            bubbleColor = Color(0xFF9C27B0),
                            isSelected = accentColor == ThemeSettingsManager.ACCENT_PURPLE,
                            onClick = { settingsManager.setAccentColor(ThemeSettingsManager.ACCENT_PURPLE) }
                        )
                        AccentColorBubble(
                            colorName = "Sky Blue",
                            bubbleColor = Color(0xFF00BCD4),
                            isSelected = accentColor == ThemeSettingsManager.ACCENT_LIGHT_BLUE,
                            onClick = { settingsManager.setAccentColor(ThemeSettingsManager.ACCENT_LIGHT_BLUE) }
                        )
                        AccentColorBubble(
                            colorName = "Deep Blue",
                            bubbleColor = Color(0xFF1976D2),
                            isSelected = accentColor == ThemeSettingsManager.ACCENT_DEEP_BLUE,
                            onClick = { settingsManager.setAccentColor(ThemeSettingsManager.ACCENT_DEEP_BLUE) }
                        )
                        AccentColorBubble(
                            colorName = "White",
                            bubbleColor = Color.White,
                            isSelected = accentColor == ThemeSettingsManager.ACCENT_WHITE,
                            isWhite = true,
                            onClick = { settingsManager.setAccentColor(ThemeSettingsManager.ACCENT_WHITE) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "About",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "PhotoApp",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "Version 1.0.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Made By Rehan Bawakhan",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GitHub",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { uriHandler.openUri("https://github.com/rehanbawakhan") }
                                .padding(vertical = 2.dp)
                        )
                        Text(
                            text = "LinkedIn",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { uriHandler.openUri("https://www.linkedin.com/in/rehan-bawakhan-249149306/") }
                                .padding(vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "A sleek, AMOLED-optimized, fast gallery application designed for viewing and managing device photos, videos, and custom albums with ease.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Main Features",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    FeatureItem(
                        title = "✨ On-Device AI Photo Editor",
                        desc = "Harnesses Google ML Kit for 100% offline, private editing. Includes Portrait Mode (Background Blur), Background Eraser (export transparent PNGs), Color Pop (keeps subject colorful on B&W background), and one-tap histogram Auto-Enhance."
                    )

                    FeatureItem(
                        title = "🔍 Google Lens Integration",
                        desc = "Instantly scan or extract text from any photo using direct Google Lens lookups straight from the photo viewer."
                    )

                    FeatureItem(
                        title = "⚡ Dynamic Grid Zooming",
                        desc = "Pinch-to-zoom dynamically scales the photo grid from 2 up to 8 columns in a row, with smooth layouts and persistent state saving."
                    )

                    FeatureItem(
                        title = "📁 Smart Albums & Folders",
                        desc = "Effortlessly browse and organize device folders (Downloads, Camera, Screenshots) with quick, seamless navigation and state-of-the-art transitions."
                    )

                    FeatureItem(
                        title = "🎨 AMOLED Black & Accent Themes",
                        desc = "AMOLED Black optimization saves power on OLED screens. Customize your workspace with Red, Purple, Sky Blue, Deep Blue, or White accent colors."
                    )

                    FeatureItem(
                        title = "🔒 Secure Hidden Vault (Biometric)",
                        desc = "Physically relocates files to isolated secure directories to hide them from the device's stock gallery and files. Authenticate access with a custom security PIN or fingerprint/biometric credentials."
                    )

                    FeatureItem(
                        title = "👆 Google Photos Style Drag-to-Select",
                        desc = "Hold and drag across the photo grid to select lists of items. Reverts selection on swipe back, and automatically scrolls the grid when your finger reaches the top or bottom edges."
                    )
                }
            }

            Text(
                text = "Project Architecture",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp, top = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Folder Structure & Modules",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FeatureItem(
                        title = "📁 data/local",
                        desc = "Manages local SQLite storage via Room. Contains PhotoDatabase, PhotoDao, and the Room schemas for PhotoEntity & AlbumEntity. Powers key features like favorites and custom trash systems."
                    )
                    FeatureItem(
                        title = "📁 data/media",
                        desc = "Contains MediaStoreManager which queries the Android MediaStore content resolver. Synchronizes device images & videos, parses folder paths, and observes content updates in real-time."
                    )
                    FeatureItem(
                        title = "📁 data/repository",
                        desc = "Contains PhotoRepository interface and its Hilt-bound implementation. Orchestrates database reads/writes, file system modifications (trash/delete), and maps flows for the UI."
                    )
                    FeatureItem(
                        title = "📁 data/security",
                        desc = "Contains HiddenSecurityManager which handles security operations for the Vault (PIN hashing, saving, and validation) using SharedPreferences."
                    )
                    FeatureItem(
                        title = "📁 data/settings",
                        desc = "Contains ThemeSettingsManager which handles app configurations (Dark mode, AMOLED mode, custom Accent colors) via SharedPreferences."
                    )
                    FeatureItem(
                        title = "📁 di/",
                        desc = "Dagger Hilt injection module (AppModule) providing singletons like the application context, local database instance, media manager, and binding repository interfaces."
                    )
                    FeatureItem(
                        title = "📁 navigation/",
                        desc = "AppNavigation.kt defines the main NavHost, transitions, and handles query parameters (e.g., videosOnly, favoritesOnly) to filter screens dynamically."
                    )
                    FeatureItem(
                        title = "📁 ui/ & sub-packages",
                        desc = "Contains Jetpack Compose screens, layouts, and ViewModels. Structured by feature (gallery, videos, albums, favorites, trash, viewer, editor, settings)."
                    )
                    FeatureItem(
                        title = "📁 util/",
                        desc = "Houses core utilities like AiEditingEngine (handles ML Kit subject segmentation, cropping, canvas painting), MediaFormatAnalyzer (asynchronous file header parsing), and Date/Image helpers."
                    )
                }
            }

            Text(
                text = "Libraries & Dependencies",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp, top = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Core Tech Stack",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FeatureItem(
                        title = "🛠️ Jetpack Compose (BOM)",
                        desc = "The modern declarative UI toolkit used to construct all layouts, icons, animations, gestures, and custom theme systems."
                    )
                    FeatureItem(
                        title = "💉 Dagger Hilt & Hilt Navigation Compose",
                        desc = "Handles dependency injection across the codebase. Binds and injects repositories and manages lifecycle-aware ViewModels inside navigation scopes."
                    )
                    FeatureItem(
                        title = "🗄️ Room Database & SQLite",
                        desc = "Implements local persistence and caching. Stores media metadata, custom properties (isFavorite, isDeleted), and records soft-deleted items in trash."
                    )
                    FeatureItem(
                        title = "🖼️ Coil & Coil Video",
                        desc = "High-performance, image and video frame loading library. Handles memory/disk caching and asynchronous image loading."
                    )
                    FeatureItem(
                        title = "🎬 Jetpack Media3 ExoPlayer",
                        desc = "Handles low-level video decoding, buffering, and media playback control interfaces for video file playing."
                    )
                    FeatureItem(
                        title = "🤖 Google ML Kit (Subject Segmentation)",
                        desc = "Powers the offline AI photo editor. Runs neural networks locally on the device's CPU/GPU to segment people, pets, or objects for background removal."
                    )

                    FeatureItem(
                        title = "🔒 AndroidX Biometric API",
                        desc = "Handles fingerprint scanning and secure authentication integration using system hardware keychains to secure the private hidden folder."
                    )
                }
            }

            Text(
                text = "How It Works (Under the Hood)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp, top = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Data Sync & Execution Pipeline",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FeatureItem(
                        title = "🔄 Sync Pipeline",
                        desc = "On app launch or swipe-to-refresh, MediaStoreManager scans the Android system's MediaStore database. It syncs the files to the local Room database, merging any updates. Coroutines flow changes reactively to keep the UI up-to-date."
                    )
                    FeatureItem(
                        title = "🧵 Thread Management",
                        desc = "Heavy disk queries (Room, MediaStore), AI editing (ML Kit), and metadata analyses (MediaFormatAnalyzer) are offloaded to Dispatchers.IO to maintain a smooth 60fps UI on the Main thread."
                    )
                    FeatureItem(
                        title = "🎨 Reactive Theme & Accent Styling",
                        desc = "The custom ThemeSettingsManager registers a persistent listener on SharedPreferences. As theme or accent values change, state flows emit updates, causing the custom PhotoAppTheme wrapper to recompose color schemes globally."
                    )

                    FeatureItem(
                        title = "🛡️ Vault Isolation & Physical Moves",
                        desc = "Hiding a photo moves the file from external storage to secure app storage and deletes its MediaStore index, completely hiding it from device scanning. Unhiding restores it to its original folder (including DCIM/Camera)."
                    )

                    FeatureItem(
                        title = "👆 Compose Pointer Event Interception",
                        desc = "Drag-to-select intercepts touch events in Compose's PointerEventPass.Initial pass. This allows the parent layout to capture swipe gestures for selection, while preserving simple clicks on the child items."
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureItem(
    title: String,
    desc: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ThemeOptionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AccentColorBubble(
    colorName: String,
    bubbleColor: Color,
    isSelected: Boolean,
    isWhite: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        val borderModifier = if (isSelected) {
            Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
        } else if (isWhite) {
            Modifier.border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
        } else {
            Modifier
        }

        Box(
            modifier = Modifier
                .size(44.dp)
                .padding(4.dp)
                .then(borderModifier)
                .padding(3.dp)
                .clip(CircleShape)
                .background(bubbleColor),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = if (isWhite) Color.Black else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = colorName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
