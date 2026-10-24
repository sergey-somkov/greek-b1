package com.somkov.greek_b1.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The settings that the settings screen shows and changes. */
class SettingsViewModel(private val settings: Settings) : ViewModel() {
    /** Null until the store is read. */
    val dailyGoalMinutes: StateFlow<Int?> =
        settings.dailyGoalMinutes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun stepDailyGoal(steps: Int) {
        viewModelScope.launch { settings.stepDailyGoal(steps) }
    }
}
