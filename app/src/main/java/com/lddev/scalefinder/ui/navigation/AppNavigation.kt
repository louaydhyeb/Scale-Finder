package com.lddev.scalefinder.ui.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lddev.scalefinder.R
import com.lddev.scalefinder.ui.screens.HomeScreen
import com.lddev.scalefinder.ui.screens.QuizScreen
import com.lddev.scalefinder.ui.screens.ScaleExplorerScreen
import com.lddev.scalefinder.ui.screens.TranscriptionScreen
import com.lddev.scalefinder.ui.screens.TunerScreen

enum class AppRoute(val route: String, val labelRes: Int, val icon: ImageVector) {
    HOME("home", R.string.nav_home, Icons.Default.Home),
    TUNER("tuner", R.string.nav_tuner, Icons.Default.Build),
    SCALE_EXPLORER("scale_explorer", R.string.nav_scale_explorer, Icons.Default.Search),
    QUIZ("quiz", R.string.nav_quiz, Icons.Default.CheckCircle),
    TRANSCRIPTION("transcription", R.string.nav_transcription, Icons.Default.Create);
}

@Composable
fun AppNavigation(
    onToggleTheme: () -> Unit,
    isDark: Boolean
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Row(Modifier.fillMaxSize()) {
        NavigationRail(
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            AppRoute.entries.forEach { screen ->
                val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                NavigationRailItem(
                    icon = { Icon(screen.icon, contentDescription = stringResource(screen.labelRes)) },
                    label = { Text(stringResource(screen.labelRes)) },
                    selected = selected,
                    onClick = {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }

        Scaffold { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = AppRoute.HOME.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(AppRoute.HOME.route) {
                    HomeScreen(
                        onToggleTheme = onToggleTheme,
                        isDark = isDark
                    )
                }
                composable(AppRoute.TUNER.route) {
                    TunerScreen()
                }
                composable(AppRoute.SCALE_EXPLORER.route) {
                    ScaleExplorerScreen()
                }
                composable(AppRoute.QUIZ.route) {
                    QuizScreen()
                }
                composable(AppRoute.TRANSCRIPTION.route) {
                    TranscriptionScreen()
                }
            }
        }
    }
}
