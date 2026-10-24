package com.somkov.greek_b1.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.somkov.greek_b1.content.CatalogTopic
import com.somkov.greek_b1.data.AppDatabase
import com.somkov.greek_b1.home.EXAM_DAY
import com.somkov.greek_b1.home.daysUntilExam
import com.somkov.greek_b1.session.Session
import com.somkov.greek_b1.settings.Settings
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** What the progress screen shows. */
data class ProgressState(
    /** The topics of the heat map, in the order of the catalog. */
    val topics: List<CatalogTopic>,
    val readiness: Readiness,
    /** The titles of the items of the trials, by item ID. */
    val itemTitles: Map<String, String>,
    /** The work of each day of study with any session. */
    val days: Map<LocalDate, DayTime>,
    /** The daily goal of today, which the past days are shown against too. */
    val goalMinutes: Int,
    val today: LocalDate,
    val daysUntilExam: Long,
) {
    /** Nothing is recorded yet: the screen shows its empty state. */
    val isEmpty: Boolean get() = days.isEmpty()

    /** The first day of the calendar: the first day of study with a session. */
    val firstDay: LocalDate get() = minOf(days.keys.minOrNull() ?: today, today)

    /** The last day of the calendar: the day of the exam, or today after it. */
    val lastDay: LocalDate get() = maxOf(EXAM_DAY, today)
}

/** Today's day of study and the days left until the exam, which the clock gives. */
data class CalendarDays(val today: LocalDate, val daysUntilExam: Long)

fun calendarDays(clock: Clock): CalendarDays = CalendarDays(today(clock), daysUntilExam(Instant.now(clock)))

/** The state of the progress screen from the rows of the database, the daily goal and the days of the clock. */
fun progressState(
    topics: List<CatalogTopic>,
    trials: List<TrialRow>,
    sessions: List<Session>,
    goalMinutes: Int,
    days: CalendarDays,
): ProgressState = ProgressState(
    topics = topics,
    readiness = readiness(topics.map { it.id }, trials.map { it.trial }),
    itemTitles = trials.associate { it.trial.itemId to it.itemTitle },
    days = dailyTime(sessions),
    goalMinutes = goalMinutes,
    today = days.today,
    daysUntilExam = days.daysUntilExam,
)

/**
 * The progress from the user tables and the settings; it follows each write while the screen is
 * shown, and moves today at 3:00 and the days left at midnight in Cyprus.
 */
class ProgressViewModel(database: AppDatabase, settings: Settings, clock: Clock) : ViewModel() {
    /** Null until the first query returns. */
    val state: StateFlow<ProgressState?> = run {
        val dao = database.progressDao()
        combine(
            dao.observeTopics(),
            dao.observeTrials(),
            dao.observeSessions(),
            settings.dailyGoalMinutes,
            clockFlow(clock, ::calendarDays),
        ) { topics, trials, sessions, goalMinutes, days ->
            progressState(topics, trials, sessions, goalMinutes, days)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    }
}
