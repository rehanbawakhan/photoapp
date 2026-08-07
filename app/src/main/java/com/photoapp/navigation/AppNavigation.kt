package com.photoapp.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Photo
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.photoapp.ui.map.PhotosMapScreen
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
    data object Map : Screen("map")
    data object Viewer : Screen("viewer/{photoId}?albumId={albumId}&favoritesOnly={favoritesOnly}&videosOnly={videosOnly}&hiddenOnly={hiddenOnly}") {
        fun createRoute(photoId: Long, albumId: String? = null, favoritesOnly: Boolean = false, videosOnly: Boolean = false, hiddenOnly: Boolean = false): String {
            val builder = StringBuilder("viewer/$photoId")
            val params = mutableListOf<String>()
            if (albumId != null) params.add("albumId=$albumId")
            if (favoritesOnly) params.add("favoritesOnly=true")
            if (videosOnly) params.add("videosOnly=true")
            if (hiddenOnly) params.add("hiddenOnly=true")
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

@Composable
private fun FloatingPillNavBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onItemClick: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    // 70% opaque — dark but translucent so photos bleed through underneath
    val pillColor = Color(0xB51C1C1E)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 36.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(50),
                    ambientColor = Color.Black.copy(alpha = 0.7f),
                    spotColor = Color.Black.copy(alpha = 0.7f)
                )
                .background(pillColor, shape = RoundedCornerShape(50))
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.screen.route

                // Animate the indicator circle size — active tab gets slightly larger
                val circleSize by animateDpAsState(
                    targetValue = if (isSelected) 54.dp else 46.dp,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "circleSize_${item.label}"
                )

                // Active tab background: a noticeably darker circle inside the pill
                val circleBg by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFF3A3A3C) else Color.Transparent,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "circleBg_${item.label}"
                )

                // Icon tint: bright white for active, muted grey for inactive
                val iconTint by animateColorAsState(
                    targetValue = if (isSelected) Color.White else Color(0xFF8E8E93),
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
                        ) { onItemClick(item) }
                ) {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

// ─── App Navigation Host ────────────────────────────────────────────────────

// How much bottom padding screens should add so their last row isn't hidden under the pill
private val NAV_PILL_BOTTOM_PADDING = 100.dp

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    val showBottomBar = currentRoute in bottomNavItems.map { it.screen.route }

    // Box overlay: pill sits on top of content with zero reserved space below
    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Screen.Gallery.route,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { fadeIn(tween(300)) },
            exitTransition = { fadeOut(tween(300)) }
        ) {
            // ── Top-level destinations ──

            composable(Screen.Gallery.route) {
                GalleryScreen(
                    onPhotoClick = { photoId ->
                        navController.navigate(Screen.Viewer.createRoute(photoId))
                    },
                    onSettingsClick = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onMapClick = {
                        navController.navigate(Screen.Map.route)
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
                    }
                ),
                enterTransition = { fadeIn(tween(300)) },
                exitTransition = { fadeOut(tween(300)) }
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
                route = Screen.Map.route,
                enterTransition = { fadeIn(tween(300)) },
                exitTransition = { fadeOut(tween(300)) }
            ) {
                PhotosMapScreen(
                    onBack = { navController.navigateUp() },
                    onPhotoClick = { photoId ->
                        navController.navigate(Screen.Viewer.createRoute(photoId))
                    }
                )
            }
        }

        // Pill overlaid directly on top — truly floating, zero reserved space below
        if (showBottomBar) {
            FloatingPillNavBar(
                items = bottomNavItems,
                currentRoute = currentRoute,
                onItemClick = { item ->
                    navController.navigate(item.screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
