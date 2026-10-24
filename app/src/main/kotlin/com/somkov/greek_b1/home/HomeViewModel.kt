package com.somkov.greek_b1.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.somkov.greek_b1.data.AppDatabase
import com.somkov.greek_b1.progress.DayTime
import com.somkov.greek_b1.progress.dailyTime
import com.somkov.greek_b1.progress.clockFlow
import com.somkov.greek_b1.progress.today
import com.somkov.greek_b1.session.Session
import com.somkov.greek_b1.settings.Settings
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** What home shows of today: the work of today's day of study against the daily goal. */
data class TodayState(val day: DayTime, val goalMinutes: Int)

/** The work of the day of study [today] from [sessions], against the goal of [goalMinutes]. */
fun todayState(sessions: List<Session>, goalMinutes: Int, today: LocalDate): TodayState =
    TodayState(dailyTime(sessions)[today] ?: DayTime(), goalMinutes)

/**
 * Today on home, from the sessions and the daily goal; it follows each write while home is shown,
 * and moves to the next day at 3:00.
 */
class HomeViewModel(database: AppDatabase, settings: Settings, clock: Clock) : ViewModel() {
    /** Null until the first query returns. */
    val today: StateFlow<TodayState?> =
        combine(database.progressDao().observeSessions(), settings.dailyGoalMinutes, clockFlow(clock, ::today)) {
                sessions, goalMinutes, today ->
            todayState(sessions, goalMinutes, today)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
