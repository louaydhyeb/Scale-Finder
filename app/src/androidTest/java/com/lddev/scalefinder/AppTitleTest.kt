package com.lddev.scalefinder

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import org.junit.Rule
import org.junit.Test

class AppTitleTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun showsScaleFinderTitle() {
        // Check for any part of the title since it uses gradient styling
        composeRule.onNodeWithText("Scale", substring = true).assertIsDisplayed()
    }
}