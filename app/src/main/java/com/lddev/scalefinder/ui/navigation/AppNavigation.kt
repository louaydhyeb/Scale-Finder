package com.lddev.scalefinder.ui.navigation

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lddev.scalefinder.R
import com.lddev.scalefinder.ui.HomeViewModel
import com.lddev.scalefinder.ui.screens.CircleOfFifthsScreen
import com.lddev.scalefinder.ui.screens.HomeScreen
import com.lddev.scalefinder.ui.screens.QuizScreen
import com.lddev.scalefinder.ui.screens.ScaleExplorerScreen
import com.lddev.scalefinder.ui.screens.SettingsScreen
import com.lddev.scalefinder.ui.screens.TranscriptionScreen
import com.lddev.scalefinder.ui.screens.TunerScreen
import kotlin.math.roundToInt

private const val SETTINGS_ROUTE = "settings"

private const val PREFS_NAME = "scalefinder_prefs"
private const val KEY_TUTORIAL_DONE = "tutorial_done"

sealed class NavIcon {
    data class Vector(val imageVector: ImageVector) : NavIcon()

    data class Drawable(
        @DrawableRes val resId: Int,
        val tinted: Boolean = false,
    ) : NavIcon()
}

enum class AppRoute(
    val route: String,
    val labelRes: Int,
    val icon: NavIcon,
    val descriptionRes: Int,
) {
    HOME("home", R.string.nav_home, NavIcon.Drawable(R.drawable.ic_nav_home, tinted = true), R.string.tutorial_home_desc),
    TUNER("tuner", R.string.nav_tuner, NavIcon.Drawable(R.drawable.ic_nav_tuner, tinted = true), R.string.tutorial_tuner_desc),
    SCALE_EXPLORER(
        "scale_explorer",
        R.string.nav_scale_explorer,
        NavIcon.Drawable(R.drawable.ic_nav_scales, tinted = true),
        R.string.tutorial_scales_desc,
    ),
    CIRCLE_OF_FIFTHS(
        "circle_of_fifths",
        R.string.nav_circle_of_fifths,
        NavIcon.Drawable(R.drawable.ic_nav_circle, tinted = true),
        R.string.tutorial_circle_desc,
    ),
    QUIZ("quiz", R.string.nav_quiz, NavIcon.Drawable(R.drawable.ic_nav_quiz, tinted = true), R.string.tutorial_quiz_desc),
    TRANSCRIPTION(
        "transcription",
        R.string.nav_transcription,
        NavIcon.Drawable(R.drawable.ic_nav_tab, tinted = true),
        R.string.tutorial_tab_desc,
    ),
}

@Composable
fun AppNavigation(
    onToggleTheme: () -> Unit,
    isDark: Boolean,
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val itemBounds = remember { mutableStateMapOf<Int, Rect>() }
    var tutorialStep by rememberSaveable { mutableIntStateOf(-1) }

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_TUTORIAL_DONE, false)) {
            tutorialStep = 0
        }
    }

    fun finishTutorial() {
        tutorialStep = -1
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_TUTORIAL_DONE, true) }
    }

    val homeVm: HomeViewModel = viewModel()

    Box(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize()) {
            NavigationRail(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                AppRoute.entries.forEachIndexed { index, screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationRailItem(
                        modifier =
                            Modifier.onGloballyPositioned { coords ->
                                itemBounds[index] = coords.boundsInRoot()
                            },
                        icon = {
                            when (val navIcon = screen.icon) {
                                is NavIcon.Vector ->
                                    Icon(
                                        navIcon.imageVector,
                                        contentDescription = stringResource(screen.labelRes),
                                    )
                                is NavIcon.Drawable ->
                                    Icon(
                                        painter = painterResource(navIcon.resId),
                                        contentDescription = stringResource(screen.labelRes),
                                        modifier = Modifier.size(24.dp),
                                        tint =
                                            if (navIcon.tinted) {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            } else {
                                                Color.Unspecified
                                            },
                                    )
                            }
                        },
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
                        },
                    )
                }

                Spacer(Modifier.height(8.dp))

                val isSettings = currentDestination?.route == SETTINGS_ROUTE
                NavigationRailItem(
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_nav_settings),
                            contentDescription = stringResource(R.string.settings_title),
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    label = { Text(stringResource(R.string.settings_title)) },
                    selected = isSettings,
                    onClick = {
                        navController.navigate(SETTINGS_ROUTE) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }

            Scaffold { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = AppRoute.HOME.route,
                    modifier = Modifier.padding(innerPadding),
                ) {
                    composable(AppRoute.HOME.route) {
                        HomeScreen(vm = homeVm)
                    }
                    composable(AppRoute.TUNER.route) {
                        TunerScreen()
                    }
                    composable(AppRoute.SCALE_EXPLORER.route) {
                        ScaleExplorerScreen()
                    }
                    composable(AppRoute.CIRCLE_OF_FIFTHS.route) {
                        CircleOfFifthsScreen()
                    }
                    composable(AppRoute.QUIZ.route) {
                        QuizScreen()
                    }
                    composable(AppRoute.TRANSCRIPTION.route) {
                        TranscriptionScreen()
                    }
                    composable(SETTINGS_ROUTE) {
                        SettingsScreen(
                            vm = homeVm,
                            isDark = isDark,
                            onToggleTheme = onToggleTheme,
                        )
                    }
                }
            }
        }

        if (tutorialStep in AppRoute.entries.indices) {
            TutorialOverlay(
                step = tutorialStep,
                itemBounds = itemBounds,
                onNext = {
                    if (tutorialStep < AppRoute.entries.size - 1) {
                        tutorialStep++
                    } else {
                        finishTutorial()
                    }
                },
                onSkip = { finishTutorial() },
            )
        }
    }
}

@Composable
private fun TutorialOverlay(
    step: Int,
    itemBounds: Map<Int, Rect>,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    val bounds = itemBounds[step] ?: return
    val routes = AppRoute.entries
    val total = routes.size
    val density = LocalDensity.current
    val padPx = with(density) { 6.dp.toPx() }
    val cornerPx = with(density) { 10.dp.toPx() }
    val primary = MaterialTheme.colorScheme.primary

    var overlayHeight by remember { mutableIntStateOf(0) }
    var cardHeight by remember { mutableIntStateOf(0) }

    Box(Modifier.fillMaxSize().onSizeChanged { overlayHeight = it.height }) {
        // Layer 1: invisible tap-absorber so taps on the dark area
        // don't leak through to the NavigationRail behind
        Box(
            Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { },
                ),
        )

        // Layer 2: dark overlay with cutout (visual only, no pointer handling)
        Canvas(
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
        ) {
            drawRect(Color.Black.copy(alpha = 0.72f))

            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(bounds.left - padPx, bounds.top - padPx),
                size = Size(bounds.width + padPx * 2, bounds.height + padPx * 2),
                cornerRadius = CornerRadius(cornerPx),
                blendMode = BlendMode.Clear,
            )
        }

        // Layer 3: highlight border
        Box(
            modifier =
                Modifier
                    .graphicsLayer {
                        translationX = bounds.left - padPx
                        translationY = bounds.top - padPx
                    },
        ) {
            val w = with(density) { (bounds.width + padPx * 2).toDp() }
            val h = with(density) { (bounds.height + padPx * 2).toDp() }
            Box(
                Modifier
                    .size(w, h)
                    .border(2.dp, primary, RoundedCornerShape(10.dp)),
            )
        }

        // Layer 4 (top): tooltip card — receives all touch events first
        Card(
            modifier =
                Modifier
                    .onSizeChanged { cardHeight = it.height }
                    .offset {
                        val marginPx = 12.dp.toPx()
                        val idealY = bounds.top
                        val maxY =
                            if (overlayHeight > 0 && cardHeight > 0) {
                                (overlayHeight - cardHeight - marginPx).coerceAtLeast(0f)
                            } else {
                                idealY
                            }
                        IntOffset(
                            x = (bounds.right + 20.dp.toPx()).roundToInt(),
                            y = idealY.roundToInt().coerceIn(marginPx.roundToInt(), maxY.roundToInt()),
                        )
                    }
                    .widthIn(max = 280.dp),
            shape = RoundedCornerShape(14.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(routes[step].labelRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = primary,
                )
                Text(
                    text = stringResource(routes[step].descriptionRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.tutorial_step, step + 1, total),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (step < total - 1) {
                            TextButton(onClick = onSkip) {
                                Text(stringResource(R.string.tutorial_skip))
                            }
                            Button(onClick = onNext) {
                                Text(stringResource(R.string.tutorial_next))
                            }
                        } else {
                            Button(onClick = onNext) {
                                Text(stringResource(R.string.tutorial_done))
                            }
                        }
                    }
                }
            }
        }
    }
}
