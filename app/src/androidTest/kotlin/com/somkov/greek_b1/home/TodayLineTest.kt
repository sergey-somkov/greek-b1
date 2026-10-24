package com.somkov.greek_b1.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.somkov.greek_b1.progress.SampleProgress
import com.somkov.greek_b1.theme.GreekB1Theme
import org.junit.Rule
import org.junit.Test

/**
 * The line of today on home, on the made-up progress of [SampleProgress], shown in an empty
 * activity: the database of the app is not touched. The texts are the ones of strings.xml.
 */
class TodayLineTest {
    @get:Rule
    val compose = createComposeRule()

    private val sample = SampleProgress()

    private fun show(state: TodayState) {
        compose.setContent { GreekB1Theme { TodayLine(state) } }
    }

    @Test
    fun `today shows its minutes against the goal and its trials`() {
        // Fri 20 Nov: three trials; the trial abandoned at 1:00 counts to the evening before.
        show(sample.today())

        compose.onNodeWithText("Today: 49 min of 2 h · 3 trials").assertIsDisplayed()
        compose.onNodeWithContentDescription("Daily goal met").assertDoesNotExist()
    }

    @Test
    fun `a met goal has a check mark`() {
        show(sample.today(goalMinutes = 45))

        compose.onNodeWithText("Today: 49 min of 45 min · 3 trials").assertIsDisplayed()
        compose.onNodeWithContentDescription("Daily goal met").assertIsDisplayed()
    }

    @Test
    fun `a day without sessions shows zero`() {
        show(sample.emptyToday())

        compose.onNodeWithText("Today: 0 min of 2 h · 0 trials").assertIsDisplayed()
    }
}
