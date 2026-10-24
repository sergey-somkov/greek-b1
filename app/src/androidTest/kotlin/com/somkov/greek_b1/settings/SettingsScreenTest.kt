package com.somkov.greek_b1.settings

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.somkov.greek_b1.GreekB1Application
import com.somkov.greek_b1.MainActivity
import com.somkov.greek_b1.R
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * The daily goal on the settings screen. The tests change the settings of the test copy of the
 * app, so each one starts and ends at the default goal.
 */
class SettingsScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val settings: Settings
        get() = (compose.activity.application as GreekB1Application).container.settings

    private fun string(@StringRes id: Int): String = compose.activity.getString(id)

    @Before
    @After
    fun resetDailyGoal() = runBlocking {
        val minutes = settings.dailyGoalMinutes.first()
        settings.stepDailyGoal((Settings.DAILY_GOAL_DEFAULT - minutes) / Settings.DAILY_GOAL_STEP)
    }

    @Test
    fun `the buttons change the daily goal by 15 minutes`() {
        openSettings()
        compose.onNodeWithText("2 h").assertIsDisplayed()
        compose.onNodeWithContentDescription(string(R.string.daily_goal_more)).performClick()
        compose.onNodeWithText("2 h 15 min").assertIsDisplayed()
        compose.onNodeWithContentDescription(string(R.string.daily_goal_less)).performClick()
        compose.onNodeWithContentDescription(string(R.string.daily_goal_less)).performClick()
        compose.onNodeWithText("1 h 45 min").assertIsDisplayed()
    }

    @Test
    fun `the daily goal survives the recreation of the activity`() {
        openSettings()
        compose.onNodeWithContentDescription(string(R.string.daily_goal_more)).performClick()
        compose.onNodeWithText("2 h 15 min").assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("2 h 15 min").assertIsDisplayed()
    }

    @Test
    fun `a button is disabled at its end of the range`() {
        runBlocking { settings.stepDailyGoal(-100) }
        openSettings()
        compose.onNodeWithText("15 min").assertIsDisplayed()
        compose.onNodeWithContentDescription(string(R.string.daily_goal_less)).assertIsNotEnabled()
        compose.onNodeWithContentDescription(string(R.string.daily_goal_more)).assertIsEnabled()
        runBlocking { settings.stepDailyGoal(100) }
        compose.onNodeWithText("6 h").assertIsDisplayed()
        compose.onNodeWithContentDescription(string(R.string.daily_goal_less)).assertIsEnabled()
        compose.onNodeWithContentDescription(string(R.string.daily_goal_more)).assertIsNotEnabled()
    }

    private fun openSettings() {
        compose.onNodeWithText(string(R.string.settings)).performClick()
        compose.onNodeWithText(string(R.string.daily_goal)).assertIsDisplayed()
    }
}
