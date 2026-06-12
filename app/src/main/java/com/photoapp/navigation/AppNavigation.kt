package com.photoapp.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import com.photoapp.ui.trash.TrashScreen
import com.photoapp.ui.viewer.PhotoViewerScreen
import com.photoapp.ui.settings.SettingsScreen
import com.photoapp.ui.videos.VideosScreen
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Videocam

sealed class Screen(val route: String) {
    data object Gallery : Screen("gallery")
    data object Videos : Screen("videos")
    data object Albums : Screen("albums")
    data object Favorites : Screen("favorites")
    data object Trash : Screen("trash")
    data object Settings : Screen("settings")
    data object Viewer : Screen("viewer/{photoId}?albumId={albumId}&favoritesOnly={favoritesOnly}&videosOnly={videosOnly}") {
        fun createRoute(photoId: Long, albumId: String? = null, favoritesOnly: Boolean = false, videosOnly: Boolean = false): String {
            val builder = StringBuilder("viewer/$photoId")
            val params = mutableListOf<String>()
            if (albumId != null) params.add("albumId=$albumId")
            if (favoritesOnly) params.add("favoritesOnly=true")
            if (videosOnly) params.add("videosOnly=true")
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

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Show bottom bar only on top-level destinations
    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.screen.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == item.screen.route
                        } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Gallery.route,
            modifier = Modifier,
            enterTransition = {
                fadeIn(tween(300))
            },
            exitTransition = {
                fadeOut(tween(300))
            }
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
                    bottomPadding = paddingValues.calculateBottomPadding()
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
                    bottomPadding = paddingValues.calculateBottomPadding()
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
                    bottomPadding = paddingValues.calculateBottomPadding()
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
                    bottomPadding = paddingValues.calculateBottomPadding()
                )
            }

            composable(Screen.Trash.route) {
                TrashScreen(
                    bottomPadding = paddingValues.calculateBottomPadding()
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
                    }
                ),
                enterTransition = {
                    fadeIn(tween(300))
                },
                exitTransition = {
                    fadeOut(tween(300))
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
                enterTransition = {
                    fadeIn(tween(300))
                },
                exitTransition = {
                    fadeOut(tween(300))
                }
            ) {
                SettingsScreen(
                    onBack = { navController.navigateUp() }
                )
            }
        }
    }
}
