package com.photoapp.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.photoapp.ui.albums.AlbumsScreen
import com.photoapp.ui.editor.EditorScreen
import com.photoapp.ui.favorites.FavoritesScreen
import com.photoapp.ui.gallery.GalleryScreen
import com.photoapp.ui.hidden.HiddenScreen
import com.photoapp.ui.search.SearchScreen
import com.photoapp.ui.settings.SettingsScreen
import com.photoapp.ui.trash.TrashScreen
import com.photoapp.ui.viewer.PhotoViewerScreen
import com.photoapp.ui.videos.VideosScreen

sealed class Screen(val route: String) {
    data object Gallery : Screen("gallery")
    data object Videos : Screen("videos")
    data object Albums : Screen("albums")
    data object Favorites : Screen("favorites")
    data object Trash : Screen("trash")
    data object Hidden : Screen("hidden")
    data object Settings : Screen("settings")
    data object Search : Screen("search")
    data object Viewer : Screen("viewer/{photoId}?albumId={albumId}&favoritesOnly={favoritesOnly}&videosOnly={videosOnly}&hiddenOnly={hiddenOnly}&externalUri={externalUri}") {
        fun createRoute(photoId: Long, albumId: String? = null, favoritesOnly: Boolean = false, videosOnly: Boolean = false, hiddenOnly: Boolean = false, externalUri: String? = null): String {
            val builder = StringBuilder("viewer/$photoId")
            val params = mutableListOf<String>()
            if (albumId != null) params.add("albumId=${android.net.Uri.encode(albumId)}")
            if (favoritesOnly) params.add("favoritesOnly=true")
            if (videosOnly) params.add("videosOnly=true")
            if (hiddenOnly) params.add("hiddenOnly=true")
            if (externalUri != null) params.add("externalUri=${android.net.Uri.encode(externalUri)}")
            if (params.isNotEmpty()) {
                builder.append("?").append(params.joinToString("&"))
            }
            return builder.toString()
        }
    }
    data object Editor : Screen("editor/{photoId}") {
        fun createRoute(photoId: Long) = "editor/$photoId"
    }
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Gallery, "Photos", Icons.Filled.Photo, Icons.Outlined.Photo),
    BottomNavItem(Screen.Videos, "Videos", Icons.Filled.Videocam, Icons.Outlined.Videocam),
    BottomNavItem(Screen.Albums, "Albums", Icons.Filled.Collections, Icons.Outlined.Collections),
    BottomNavItem(Screen.Favorites, "Favorites", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder)
)

// ─── Floating Pill Nav Bar ──────────────────────────────────────────────────

class ViewerPillState {
    var isViewerActive by androidx.compose.runtime.mutableStateOf(false)
    var showControls by androidx.compose.runtime.mutableStateOf(true)
    var isFavorite by androidx.compose.runtime.mutableStateOf(false)
    var isVideo by androidx.compose.runtime.mutableStateOf(false)
    var isHidden by androidx.compose.runtime.mutableStateOf(false)
    var onShare: () -> Unit = {}
    var onFavorite: () -> Unit = {}
    var onEdit: () -> Unit = {}
    var onDelete: () -> Unit = {}
    var onMoveToAlbum: () -> Unit = {}
    var onCopyToAlbum: () -> Unit = {}
    var onRename: () -> Unit = {}
    var onConvertToPdf: () -> Unit = {}
    var onSetAsWallpaper: () -> Unit = {}
    var onToggleHide: () -> Unit = {}
    var onDetails: () -> Unit = {}
}

val LocalViewerPillState = androidx.compose.runtime.staticCompositionLocalOf { ViewerPillState() }

@Composable
private fun FloatingPillNavBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onItemClick: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewerPillState = LocalViewerPillState.current
    val isViewerMode = viewerPillState.isViewerActive
    val surfaceColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
    val isLight = (surfaceColor.red * 0.299f + surfaceColor.green * 0.587f + surfaceColor.blue * 0.114f) > 0.5f
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current

    val pillColor = if (isLight) {
        androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)
    } else {
        Color(0xCC1C1C1E)
    }

    val pillBorderColor = if (isLight) {
        androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    } else {
        Color.White.copy(alpha = 0.18f)
    }

    val density = LocalDensity.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Surface(
            shape = CircleShape,
            color = pillColor,
            border = androidx.compose.foundation.BorderStroke(1.dp, pillBorderColor),
            shadowElevation = 0.dp,
            modifier = Modifier
                .graphicsLayer {
                    shadowElevation = with(density) { 8.dp.toPx() }
                    shape = CircleShape
                    clip = true
                }
                .clip(CircleShape)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    alignment = Alignment.Center
                )
        ) {
            androidx.compose.animation.AnimatedContent(
                targetState = isViewerMode,
                transitionSpec = {
                    (fadeIn(tween(220)) + scaleIn(initialScale = 0.92f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center))
                        .togetherWith(fadeOut(tween(180)) + scaleOut(targetScale = 0.92f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center))
                },
                contentAlignment = Alignment.Center,
                label = "nav_pill_morph"
            ) { inViewer ->
                if (!inViewer) {
                    // Home Nav Items (Photos, Videos, Albums, Favorites)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        items.forEach { item ->
                            val isSelected = currentRoute == item.screen.route

                            val circleSize by animateDpAsState(
                                targetValue = if (isSelected) 54.dp else 46.dp,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                label = "circleSize_${item.label}"
                            )

                            val selectedBg = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer
                            val unselectedBg = Color.Transparent

                            val circleBg by animateColorAsState(
                                targetValue = if (isSelected) selectedBg else unselectedBg,
                                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                label = "circleBg_${item.label}"
                            )

                            val selectedTint = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer
                            val unselectedTint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant

                            val iconTint by animateColorAsState(
                                targetValue = if (isSelected) selectedTint else unselectedTint,
                                animationSpec = tween(durationMillis = 180),
                                label = "iconTint_${item.label}"
                            )

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(circleSize)
                                    .background(circleBg, shape = CircleShape)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        try {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        } catch (e: Exception) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                        onItemClick(item)
                                    }
                            ) {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label,
                                    tint = iconTint,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Viewer Action Items (Share, Favorite, Edit, Delete, More)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        // Share
                        androidx.compose.material3.IconButton(onClick = { viewerPillState.onShare() }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Favorite
                        androidx.compose.material3.IconButton(onClick = { viewerPillState.onFavorite() }) {
                            Icon(
                                imageVector = if (viewerPillState.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (viewerPillState.isFavorite) Color(0xFFFF5252) else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Edit
                        androidx.compose.material3.IconButton(onClick = { viewerPillState.onEdit() }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Delete
                        androidx.compose.material3.IconButton(onClick = { viewerPillState.onDelete() }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Overflow Menu
                        var showOverflowMenu by remember { androidx.compose.runtime.mutableStateOf(false) }
                        Box {
                            androidx.compose.material3.IconButton(onClick = { showOverflowMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            androidx.compose.material3.DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { androidx.compose.material3.Text("Move to album") },
                                    onClick = {
                                        showOverflowMenu = false
                                        viewerPillState.onMoveToAlbum()
                                    }
                                )
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { androidx.compose.material3.Text("Copy to album") },
                                    onClick = {
                                        showOverflowMenu = false
                                        viewerPillState.onCopyToAlbum()
                                    }
                                )
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { androidx.compose.material3.Text("Rename") },
                                    onClick = {
                                        showOverflowMenu = false
                                        viewerPillState.onRename()
                                    }
                                )
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { androidx.compose.material3.Text("Convert to PDF") },
                                    onClick = {
                                        showOverflowMenu = false
                                        viewerPillState.onConvertToPdf()
                                    }
                                )
                                if (!viewerPillState.isVideo) {
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { androidx.compose.material3.Text("Set as wallpaper") },
                                        onClick = {
                                            showOverflowMenu = false
                                            viewerPillState.onSetAsWallpaper()
                                        }
                                    )
                                }
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { androidx.compose.material3.Text(if (viewerPillState.isHidden) "Unhide" else "Hide") },
                                    onClick = {
                                        showOverflowMenu = false
                                        viewerPillState.onToggleHide()
                                    }
                                )
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { androidx.compose.material3.Text("Details") },
                                    onClick = {
                                        showOverflowMenu = false
                                        viewerPillState.onDetails()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── App Navigation Host ────────────────────────────────────────────────────

// How much bottom padding screens should add so their last row isn't hidden under the pill
private val NAV_PILL_BOTTOM_PADDING = 100.dp

@Composable
fun AppNavigation(
    externalUri: String? = null
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    val showBottomBar = currentRoute in bottomNavItems.map { it.screen.route }
    val viewerPillState = remember { ViewerPillState() }

    // Handle deep link from external apps (Open With)
    LaunchedEffect(externalUri) {
        if (!externalUri.isNullOrEmpty()) {
            navController.navigate("viewer/0?externalUri=${android.net.Uri.encode(externalUri)}")
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalViewerPillState provides viewerPillState) {
        // Box overlay: pill sits on top of content with zero reserved space below
        Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Screen.Gallery.route,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn(tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) + fadeOut(tween(300)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut(tween(300)) }
        ) {
            // ── Top-level destinations ──

            composable(Screen.Gallery.route) {
                GalleryScreen(
                    onPhotoClick = { photoId ->
                        navController.navigate(Screen.Viewer.createRoute(photoId))
                    },
                    onSearchClick = {
                        navController.navigate(Screen.Search.route)
                    },
                    onSettingsClick = {
                        navController.navigate(Screen.Settings.route)
                    },
                    bottomPadding = 0.dp
                )
            }

            composable(Screen.Videos.route) {
                VideosScreen(
                    onPhotoClick = { photoId ->
                        navController.navigate(
                            Screen.Viewer.createRoute(
                                photoId = photoId,
                                videosOnly = true
                            )
                        )
                    },
                    bottomPadding = 0.dp
                )
            }

            composable(Screen.Albums.route) {
                AlbumsScreen(
                    onPhotoClick = { photoId, albumId ->
                        navController.navigate(
                            Screen.Viewer.createRoute(
                                photoId = photoId,
                                albumId = albumId
                            )
                        )
                    },
                    onTrashClick = {
                        navController.navigate(Screen.Trash.route)
                    },
                    onHiddenClick = {
                        navController.navigate(Screen.Hidden.route)
                    },
                    bottomPadding = 0.dp
                )
            }

            composable(Screen.Favorites.route) {
                FavoritesScreen(
                    onPhotoClick = { photoId ->
                        navController.navigate(
                            Screen.Viewer.createRoute(
                                photoId = photoId,
                                favoritesOnly = true
                            )
                        )
                    },
                    bottomPadding = 0.dp
                )
            }

            composable(Screen.Trash.route) {
                TrashScreen(
                    bottomPadding = 0.dp
                )
            }

            composable(Screen.Hidden.route) {
                HiddenScreen(
                    onPhotoClick = { photoId ->
                        navController.navigate(
                            Screen.Viewer.createRoute(
                                photoId = photoId,
                                hiddenOnly = true
                            )
                        )
                    },
                    onBack = { navController.navigateUp() },
                    bottomPadding = 0.dp
                )
            }

            // ── Detail destinations ──

            composable(
                route = Screen.Viewer.route,
                arguments = listOf(
                    navArgument("photoId") { type = NavType.LongType },
                    navArgument("albumId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("favoritesOnly") {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                    navArgument("videosOnly") {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                    navArgument("hiddenOnly") {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                    navArgument("externalUri") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                ),
                enterTransition = {
                    fadeIn(tween(250)) + scaleIn(initialScale = 0.93f, animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow))
                },
                exitTransition = {
                    fadeOut(tween(200)) + scaleOut(targetScale = 0.95f, animationSpec = tween(200))
                },
                popEnterTransition = {
                    fadeIn(tween(250))
                },
                popExitTransition = {
                    fadeOut(tween(200)) + scaleOut(targetScale = 0.95f, animationSpec = tween(200))
                }
            ) {
                PhotoViewerScreen(
                    onBack = { navController.navigateUp() },
                    onEdit = { photoId ->
                        navController.navigate(Screen.Editor.createRoute(photoId))
                    }
                )
            }

            composable(
                route = Screen.Editor.route,
                arguments = listOf(
                    navArgument("photoId") { type = NavType.LongType }
                ),
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Up,
                        tween(300)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Down,
                        tween(300)
                    )
                }
            ) {
                EditorScreen(
                    onBack = { navController.navigateUp() }
                )
            }

            composable(
                route = Screen.Settings.route,
                enterTransition = { fadeIn(tween(300)) },
                exitTransition = { fadeOut(tween(300)) }
            ) {
                SettingsScreen(
                    onBack = { navController.navigateUp() }
                )
            }

            composable(
                route = Screen.Search.route,
                enterTransition = { fadeIn(tween(300)) },
                exitTransition = { fadeOut(tween(300)) }
            ) {
                SearchScreen(
                    onBack = { navController.navigateUp() },
                    onPhotoClick = { photoId ->
                        navController.navigate(Screen.Viewer.createRoute(photoId))
                    }
                )
            }
        }

        // Pill overlaid directly on top with smooth spring slide & fade transition
        AnimatedVisibility(
            visible = if (viewerPillState.isViewerActive) viewerPillState.showControls else showBottomBar,
            enter = slideInVertically(
                initialOffsetY = { it * 2 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(animationSpec = tween(250)),
            exit = slideOutVertically(
                targetOffsetY = { it * 2 },
                animationSpec = tween(220)
            ) + fadeOut(animationSpec = tween(180)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            FloatingPillNavBar(
                items = bottomNavItems,
                currentRoute = currentRoute,
                onItemClick = { item ->
                    if (currentRoute != item.screen.route) {
                        navController.navigate(item.screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
        }
    }
}
