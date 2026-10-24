package com.somkov.greek_b1.settings

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The settings of the app, in a Preferences DataStore: a file of its own, not the database. One
 * instance lives in the app container, since DataStore allows one open store per file in a
 * process.
 *
 * A setting that was never changed reads as its default, which is not written to the file. A
 * stored value outside the range of its setting reads as the nearest bound, so a narrower range
 * in a later version needs no migration.
 */
class Settings(private val store: DataStore<Preferences>) {
    /** The daily goal in minutes; it follows each change. */
    val dailyGoalMinutes: Flow<Int> = store.data.map { dailyGoal(it) }

    /** Changes the daily goal by [steps] of [DAILY_GOAL_STEP] minutes, within [DAILY_GOAL_RANGE]. */
    suspend fun stepDailyGoal(steps: Int) {
        // Read and written in one edit, so that quick taps do not lose a step.
        store.edit { it[DAILY_GOAL] = (dailyGoal(it) + steps * DAILY_GOAL_STEP).coerceIn(DAILY_GOAL_RANGE) }
    }

    private fun dailyGoal(preferences: Preferences): Int =
        (preferences[DAILY_GOAL] ?: DAILY_GOAL_DEFAULT).coerceIn(DAILY_GOAL_RANGE)

    companion object {
        /** 2 hours: the goal of the scenario of use (2–3 hours a day in the app). */
        const val DAILY_GOAL_DEFAULT = 120
        const val DAILY_GOAL_STEP = 15
        val DAILY_GOAL_RANGE = 15..360

        private val DAILY_GOAL = intPreferencesKey("daily_goal_minutes")

        /**
         * Opens the settings in [file]: the app passes its own file, the tests a temporary one. A
         * corrupt file is replaced with the defaults: the settings are cheap to set again, and
         * the progress data is in the database. The store works in [scope]; a second store on
         * the same file needs the first one's scope cancelled.
         */
        fun open(file: File, scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())): Settings =
            Settings(
                PreferenceDataStoreFactory.create(
                    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
                    scope = scope,
                    produceFile = { file },
                ),
            )
    }
}
