package com.somkov.greek_b1.progress

import android.graphics.Bitmap
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.somkov.greek_b1.R
import com.somkov.greek_b1.ScreenScaffold
import com.somkov.greek_b1.theme.GreekB1Theme
import java.io.File
import org.junit.Rule
import org.junit.Test

/**
 * Screenshots of the progress screen on the made-up progress of [SampleProgress], for the review
 * of its look: the whole display, dialogs included, in the theme of the phone. They check
 * nothing; [ProgressScreenTest] checks the content.
 *
 * Gradle passes the folder `additionalTestOutputDir` on the phone and copies it after the run to
 * `app/build/outputs/connected_android_test_additional_output/`.
 */
class ProgressScreenshots {
    @get:Rule
    val compose = createComposeRule()

    private val sample = SampleProgress()

    private fun show(state: ProgressState) {
        compose.setContent {
            GreekB1Theme {
                ScreenScaffold(stringResource(R.string.progress), onBack = {}) { innerPadding ->
                    ProgressContent(state, Modifier.padding(innerPadding))
                }
            }
        }
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        // The window of a dialog fades in after Compose is idle.
        instrumentation.uiAutomation.waitForIdle(500, 5_000)
        val dir = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
            ?: error("No additionalTestOutputDir: run the tests with tools/test-device.sh")
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: error("No screenshot of $name")
        File(dir).mkdirs()
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun `before the first session`() {
        show(sample.emptyState())
        screenshot("progress-empty")
    }

    @Test
    fun `the heat map`() {
        show(sample.state())
        screenshot("progress-heat-map")
    }

    @Test
    fun `the trials of a cell`() {
        show(sample.state())
        compose.onNodeWithContentDescription("Home and residence, Reading").performClick()
        screenshot("progress-cell")
    }

    @Test
    fun `a cell not advised`() {
        show(sample.state())
        compose.onNodeWithContentDescription("Work and employment, Writing").performClick()
        screenshot("progress-cell-not-advised")
    }

    @Test
    fun `the calendar with a day tapped`() {
        show(sample.state())
        compose.onNodeWithContentDescription("Wed 18 Nov 2026").performScrollTo().performClick()
        // The minutes of the day, under the calendar.
        compose.onNodeWithText("Tap a day for its minutes", substring = true).performScrollTo()
        screenshot("progress-calendar")
    }
}
