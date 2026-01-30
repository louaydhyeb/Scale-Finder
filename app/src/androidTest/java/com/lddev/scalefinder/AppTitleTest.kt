package com.lddev.scalefinder

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

/**
 * Basic UI test that launches MainActivity and checks the app title after splash.
 */
class AppTitleTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun showsScaleFinderTitle() {
        // Advance virtual time so splash's delay(1500) + animation complete
        composeRule.mainClock.advanceTimeBy(4_000)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            try {
                composeRule.onNodeWithText("Add Chord", substring = true).assertExists()
                true
            } catch (_: Exception) {
                false
            }
        }
        composeRule.onAllNodesWithText("Scale", substring = true).get(0).assertIsDisplayed()
    }
}