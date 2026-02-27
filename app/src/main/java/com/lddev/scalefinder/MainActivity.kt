package com.lddev.scalefinder

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.lddev.scalefinder.ui.navigation.AppNavigation
import com.lddev.scalefinder.ui.screens.SplashScreen
import com.lddev.scalefinder.ui.theme.ScaleFinderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        enableEdgeToEdge()

        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDark by rememberSaveable { mutableStateOf(systemDark) }
            var showSplash by rememberSaveable { mutableStateOf(true) }

            ScaleFinderTheme(darkTheme = isDark) {
                if (showSplash) {
                    SplashScreen(
                        darkTheme = isDark,
                        onSplashComplete = { showSplash = false },
                    )
                } else {
                    AppNavigation(
                        onToggleTheme = { isDark = !isDark },
                        isDark = isDark,
                    )
                }
            }
        }
    }
}
