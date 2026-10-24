package com.somkov.greek_b1.progress

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.somkov.greek_b1.theme.GreekB1Theme
import org.junit.Rule
import org.junit.Test

/**
 * The content of the progress screen on made-up progress ([SampleProgress]), shown in an empty
 * activity: the database of the app is not touched. The texts are the ones of strings.xml.
 */
class ProgressScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val sample = SampleProgress()

    private fun show(state: ProgressState) {
        compose.setContent { GreekB1Theme { ProgressContent(state) } }
    }

    @Test
    fun `before the first session only the days left and a note are shown`() {
        show(sample.emptyState())

        compose.onNodeWithText("52 days").assertIsDisplayed()
        compose.onNodeWithText("Nothing is recorded yet", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Readiness").assertDoesNotExist()
        compose.onNodeWithText("Time per day").assertDoesNotExist()
    }

    @Test
    fun `the heat map shows the last trial of each cell and the readiness of each part`() {
        show(sample.state())

        compose.onNodeWithText("52 days").assertIsDisplayed()
        // Reading: home 80% (its last trial, not the first one at 20%), personal 0% (abandoned), work none.
        compose.onNodeWithText("80%").assertIsDisplayed()
        compose.onNodeWithText("0%").assertIsDisplayed()
        compose.onNodeWithText("27%").assertIsDisplayed()
        // Listening: 50% in 9 min of 7.5 is 42%, and its part 14%.
        compose.onNodeWithText("42%").assertIsDisplayed()
        compose.onNodeWithText("14%").assertIsDisplayed()
    }

    @Test
    fun `a tap on a cell shows its trials the latest first`() {
        show(sample.state())

        compose.onNodeWithContentDescription("Home and residence, Reading").performClick()
        compose.onNodeWithText("Readiness 80%").assertIsDisplayed()
        compose.onNodeWithText("Ψάχνοντας διαμέρισμα").assertIsDisplayed()
        compose.onNodeWithText("8/10 in 10:00 of 12:30 · 80%", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Το καινούργιο σπίτι").assertIsDisplayed()
        compose.onNodeWithText("4/10 in 25:00 of 12:30 · 20%", substring = true).assertIsDisplayed()

        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithText("Readiness 80%").assertDoesNotExist()
    }

    @Test
    fun `a cell after a part below the pass mark says it is not advised`() {
        show(sample.state())

        compose.onNodeWithContentDescription("Work and employment, Writing").performClick()
        compose.onNodeWithText("No result yet").assertIsDisplayed()
        compose.onNodeWithText("Not advised yet: Listening of this topic is below 60%.").assertIsDisplayed()
        compose.onNodeWithText("in 30:00 of 35:00, not assessed yet", substring = true).assertIsDisplayed()
    }

    @Test
    fun `a tap on a day shows its minutes`() {
        show(sample.state())

        compose.onNodeWithContentDescription("Wed 18 Nov 2026").performScrollTo().performClick()
        compose.onNodeWithText("Wed 18 Nov: 2 h 5 min of 2 h\nStudy 1 h 40 min, trials 25 min (1 trial)").assertExists()

        // The trial after 3:00 counts to the evening before.
        compose.onNodeWithContentDescription("Thu 19 Nov 2026").performClick()
        compose.onNodeWithText("Thu 19 Nov: 33 min of 2 h\nStudy 30 min, trials 3 min (1 trial)").assertExists()

        compose.onNodeWithContentDescription("Fri 20 Nov 2026").performClick()
        compose.onNodeWithText("Fri 20 Nov: 49 min of 2 h\nStudy 0 min, trials 49 min (3 trials)").assertExists()
    }

    @Test
    fun `a tap on a future day shows the days from it to the exam`() {
        show(sample.state())

        compose.onNodeWithContentDescription("Fri 1 Jan 2027").performScrollTo().performClick()
        compose.onNodeWithText("Fri 1 Jan: 10 days before the exam").assertExists()

        compose.onNodeWithContentDescription("Mon 11 Jan 2027").performScrollTo().performClick()
        compose.onNodeWithText("Mon 11 Jan: the day of the exam").assertExists()
    }
}
