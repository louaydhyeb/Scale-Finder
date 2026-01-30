package com.lddev.scalefinder

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the main HomeScreen. They launch MainActivity and wait for the
 * splash screen to finish before asserting on content.
 */
class HomeScreenUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun waitForHomeScreen() {
        // Splash uses delay(1500) + animation (~1.3s). In Compose test the virtual clock
        // doesn't advance by itself for coroutine delay, so advance time to finish splash.
        composeRule.mainClock.advanceTimeBy(4_000)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            try {
                composeRule.onNodeWithText("Add Chord", substring = true).assertExists()
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    @Test
    fun appTitle_isDisplayedAfterSplash() {
        waitForHomeScreen()
        // Multiple nodes contain "Scale" (title, "Scale Suggestions"); use first displayed
        composeRule.onAllNodesWithText("Scale", substring = true).get(0).assertIsDisplayed()
        composeRule.onNodeWithText("Finder", substring = true).assertIsDisplayed()
    }

    @Test
    fun addChordButton_isDisplayedAndHasContentDescription() {
        waitForHomeScreen()
        composeRule.onNodeWithContentDescription("Add chord").assertIsDisplayed()
        composeRule.onNodeWithText("Add Chord", substring = true).assertIsDisplayed()
    }

    @Test
    fun chordProgressionSection_isDisplayed() {
        waitForHomeScreen()
        composeRule.onNodeWithText("Chord Progression", substring = true).assertIsDisplayed()
    }

    @Test
    fun scaleSuggestionsSection_isDisplayed() {
        waitForHomeScreen()
        composeRule.onNodeWithText("Scale Suggestions", substring = true).performScrollTo()
        composeRule.onNodeWithText("Scale Suggestions", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Add chords to see suggestions", substring = true).assertIsDisplayed()
    }

    @Test
    fun quickPresetsSection_isDisplayed() {
        waitForHomeScreen()
        composeRule.onNodeWithText("Quick Presets", substring = true).performScrollTo()
        composeRule.onNodeWithText("Quick Presets", substring = true).assertIsDisplayed()
    }

    @Test
    fun applyPopPreset_showsChordChips() {
        waitForHomeScreen()
        composeRule.onNodeWithText("I–V–vi–IV", substring = true).performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 2000) {
            try {
                composeRule.onNodeWithText("CMaj", substring = true).assertExists()
                true
            } catch (_: Exception) {
                false
            }
        }
        composeRule.onNodeWithText("CMaj", substring = true).assertIsDisplayed()
    }

    @Test
    fun fretboardCard_isDisplayed() {
        waitForHomeScreen()
        composeRule.onNodeWithContentDescription("Fretboard card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Fretboard", substring = true).assertIsDisplayed()
    }

    @Test
    fun metronomeSection_isDisplayed() {
        waitForHomeScreen()
        composeRule.onNodeWithContentDescription("Start metronome").performScrollTo()
        composeRule.onNodeWithContentDescription("Start metronome").assertIsDisplayed()
    }
}
