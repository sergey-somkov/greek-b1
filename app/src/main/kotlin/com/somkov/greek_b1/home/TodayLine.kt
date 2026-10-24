package com.somkov.greek_b1.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.somkov.greek_b1.R
import com.somkov.greek_b1.formatMinutes

/**
 * Today's minutes against the daily goal and today's trials, for example "Today: 1 h 5 min of
 * 2 h · 2 trials", with a check mark when the goal is met, as in the calendar of the progress
 * screen.
 */
@Composable
fun TodayLine(state: TodayState, modifier: Modifier = Modifier) {
    val goalMet = state.day.goalMet(state.goalMinutes)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(
                R.string.today,
                formatMinutes(state.day.minutes.toInt()),
                formatMinutes(state.goalMinutes),
                pluralStringResource(R.plurals.trials, state.day.trials, state.day.trials),
            ),
            style = MaterialTheme.typography.titleMedium,
        )
        if (goalMet) {
            Icon(
                painterResource(R.drawable.check),
                contentDescription = stringResource(R.string.today_goal_met),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
