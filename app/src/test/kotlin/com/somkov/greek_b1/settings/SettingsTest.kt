package com.somkov.greek_b1.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** The settings on a file in a temporary folder. A new store on the same file is the next start of the app. */
class SettingsTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val file: File by lazy { File(folder.root, "settings.preferences_pb") }
    private val scopes = mutableListOf<CoroutineScope>()

    /** Opens the settings on [file], after closing the store opened before, as the death of the process does. */
    private suspend fun open(): Settings {
        // Joined: the store frees its file when its scope completes.
        scopes.forEach { it.coroutineContext.job.cancelAndJoin() }
        return Settings.open(file, newScope())
    }

    private fun newScope(): CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()).also { scopes += it }

    @After
    fun closeStores() {
        scopes.forEach { it.cancel() }
    }

    @Test
    fun `the daily goal is 120 minutes by default`() = runBlocking {
        assertEquals(120, open().dailyGoalMinutes.first())
    }

    @Test
    fun `a step changes the daily goal by 15 minutes`() = runBlocking {
        val settings = open()
        settings.stepDailyGoal(1)
        assertEquals(135, settings.dailyGoalMinutes.first())
        settings.stepDailyGoal(-3)
        assertEquals(90, settings.dailyGoalMinutes.first())
    }

    @Test
    fun `the daily goal stays within its range`() = runBlocking {
        val settings = open()
        settings.stepDailyGoal(-100)
        assertEquals(15, settings.dailyGoalMinutes.first())
        settings.stepDailyGoal(100)
        assertEquals(360, settings.dailyGoalMinutes.first())
    }

    @Test
    fun `the daily goal is kept until the next start`() = runBlocking {
        open().stepDailyGoal(2)
        assertEquals(150, open().dailyGoalMinutes.first())
    }

    @Test
    fun `a stored daily goal outside the range reads as its bound`() = runBlocking {
        val store = PreferenceDataStoreFactory.create(scope = newScope(), produceFile = { file })
        store.edit { it[intPreferencesKey("daily_goal_minutes")] = 1000 }
        assertEquals(360, open().dailyGoalMinutes.first())
    }

    @Test
    fun `a corrupt file is replaced with the defaults`() = runBlocking {
        file.writeText("not a preferences file")
        val settings = open()
        assertEquals(120, settings.dailyGoalMinutes.first())
        settings.stepDailyGoal(1)
        assertEquals(135, open().dailyGoalMinutes.first())
    }
}
