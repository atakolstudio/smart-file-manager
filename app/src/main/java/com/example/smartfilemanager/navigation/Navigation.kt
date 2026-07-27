package com.example.smartfilemanager.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.smartfilemanager.R
import com.example.smartfilemanager.ui.screens.apps.AppsScreen
import com.example.smartfilemanager.ui.screens.favorites.FavoritesScreen
import com.example.smartfilemanager.ui.screens.files.FilesScreen
import com.example.smartfilemanager.ui.screens.home.HomeScreen
import com.example.smartfilemanager.ui.screens.preview.PreviewScreen
import com.example.smartfilemanager.ui.screens.recyclebin.RecycleBinScreen
import com.example.smartfilemanager.ui.screens.settings.SettingsScreen

private data class BottomNavItem(
    val screen: Screen,
    val labelRes: Int,
    val icon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, R.string.nav_home, Icons.Filled.Home),
    BottomNavItem(Screen.Files, R.string.nav_files, Icons.Filled.Folder),
    BottomNavItem(Screen.Favorites, R.string.nav_favorites, Icons.Filled.Star),
    BottomNavItem(Screen.Settings, R.string.nav_settings, Icons.Filled.Settings)
)

/**
 * Uygulamanın ana gezinme iskeleti: alt gezinme çubuğu + NavHost.
 * onRequestPermission, üst katmandaki (MainActivity) izin isteme akışını tetikler.
 */
@Composable
fun SmartFileManagerNavHost(
    onRequestPermission: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    Scaffold(
        bottomBar = { AppBottomBar(navController) }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues),
            enterTransition = {
                slideInHorizontally(animationSpec = tween(300)) { it / 4 } + fadeIn(tween(300))
            },
            exitTransition = {
                slideOutHorizontally(animationSpec = tween(300)) { -it / 4 } + fadeOut(tween(300))
            },
            popEnterTransition = {
                slideInHorizontally(animationSpec = tween(300)) { -it / 4 } + fadeIn(tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(animationSpec = tween(300)) { it / 4 } + fadeOut(tween(300))
            }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToFolder = { path ->
                        navController.navigate(Screen.filesRouteWithPath(path))
                    },
                    onNavigateToApps = {
                        navController.navigate(Screen.Apps.route)
                    },
                    onRequestPermission = onRequestPermission
                )
            }

            composable(
                route = "${Screen.Files.route}?${Screen.FILES_PATH_ARG}={${Screen.FILES_PATH_ARG}}",
                arguments = listOf(
                    navArgument(Screen.FILES_PATH_ARG) {
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                val path = backStackEntry.arguments?.getString(Screen.FILES_PATH_ARG)
                FilesScreen(
                    path = path?.ifEmpty { null },
                    onOpenFolder = { folderPath ->
                        navController.navigate(Screen.filesRouteWithPath(folderPath))
                    },
                    onOpenPreview = { filePath ->
                        navController.navigate(Screen.previewRouteWithPath(filePath))
                    }
                )
            }

            composable(
                route = "${Screen.Preview.route}?${Screen.PREVIEW_PATH_ARG}={${Screen.PREVIEW_PATH_ARG}}",
                arguments = listOf(
                    navArgument(Screen.PREVIEW_PATH_ARG) {
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                val path = backStackEntry.arguments?.getString(Screen.PREVIEW_PATH_ARG)
                PreviewScreen(
                    path = path?.ifEmpty { null },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Favorites.route) {
                FavoritesScreen(
                    onOpenFolder = { folderPath ->
                        navController.navigate(Screen.filesRouteWithPath(folderPath))
                    },
                    onOpenPreview = { filePath ->
                        navController.navigate(Screen.previewRouteWithPath(filePath))
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToRecycleBin = { navController.navigate(Screen.RecycleBin.route) }
                )
            }

            composable(Screen.RecycleBin.route) {
                RecycleBinScreen(onBack = { navController.popBackStack() })
            }

            composable(Screen.Apps.route) {
                AppsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun AppBottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        bottomNavItems.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
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
                icon = { Icon(imageVector = item.icon, contentDescription = null) },
                label = { Text(text = stringResource(id = item.labelRes)) }
            )
        }
    }
}
