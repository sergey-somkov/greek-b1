package com.somkov.greek_b1

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import org.junit.Rule
import org.junit.Test

/**
 * Walks through all the screens: each one opens from its tile on home and goes back to home, and
 * the diagnostics open from the settings and go back to them.
 */
class NavigationTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    // The tile on home and the top bar of the screen show the same title.
    private val screens = listOf(
        R.string.reading,
        R.string.listening,
        R.string.writing,
        R.string.speaking,
        R.string.progress,
        R.string.settings,
    )

    private fun string(@StringRes id: Int): String = compose.activity.getString(id)

    @Test
    fun `every screen goes back to home with the back arrow`() {
        for (screen in screens) {
            open(screen)
            compose.onNodeWithContentDescription(string(R.string.back)).performClick()
            assertHome()
        }
    }

    @Test
    fun `every screen goes back to home with the system back`() {
        for (screen in screens) {
            open(screen)
            Espresso.pressBack()
            assertHome()
        }
    }

    @Test
    fun `diagnostics goes back to settings both ways`() {
        open(R.string.settings)
        openDiagnostics()
        compose.onNodeWithContentDescription(string(R.string.back)).performClick()
        assertSettings()
        openDiagnostics()
        Espresso.pressBack()
        assertSettings()
    }

    private fun openDiagnostics() {
        compose.onNodeWithText(string(R.string.diagnostics)).performClick()
        compose.onNodeWithText(string(R.string.diagnostics)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.settings)).assertDoesNotExist()
    }

    private fun assertSettings() {
        compose.onNodeWithText(string(R.string.settings)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.diagnostics_summary)).assertIsDisplayed()
    }

    private fun open(@StringRes screen: Int) {
        assertHome()
        compose.onNodeWithText(string(screen)).performClick()
        compose.onNodeWithText(string(screen)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.app_name)).assertDoesNotExist()
        compose.onNodeWithContentDescription(string(R.string.back)).assertIsDisplayed()
    }

    private fun assertHome() {
        compose.onNodeWithText(string(R.string.app_name)).assertIsDisplayed()
        compose.onNodeWithContentDescription(string(R.string.back)).assertDoesNotExist()
    }
}
