package com.somkov.greek_b1.settings

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.somkov.greek_b1.GreekB1Application
import com.somkov.greek_b1.R
import com.somkov.greek_b1.Screen
import com.somkov.greek_b1.ScreenScaffold
import com.somkov.greek_b1.formatMinutes

/** The settings, and the diagnostics below them. */
@Composable
fun SettingsScreen(onOpen: (Screen) -> Unit, onBack: () -> Unit) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel = viewModel { SettingsViewModel((application as GreekB1Application).container.settings) }
    val dailyGoal by viewModel.dailyGoalMinutes.collectAsStateWithLifecycle()
    ScreenScaffold(stringResource(R.string.settings), onBack) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            DailyGoal(dailyGoal, viewModel::stepDailyGoal)
            ListItem(
                headlineContent = { Text(stringResource(R.string.diagnostics)) },
                supportingContent = { Text(stringResource(R.string.diagnostics_summary)) },
                modifier = Modifier.clickable { onOpen(Screen.Diagnostics) },
            )
        }
    }
}

/**
 * The daily goal with buttons to change it by a step, since the user prefers visible buttons;
 * a button is disabled at its end of the range. Empty until the store is read.
 */
@Composable
private fun DailyGoal(minutes: Int?, onStep: (Int) -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.daily_goal)) },
        supportingContent = { Text(if (minutes == null) "" else formatMinutes(minutes)) },
        trailingContent = {
            Row {
                IconButton(
                    onClick = { onStep(-1) },
                    enabled = minutes != null && minutes > Settings.DAILY_GOAL_RANGE.first,
                ) {
                    Icon(painterResource(R.drawable.remove), contentDescription = stringResource(R.string.daily_goal_less))
                }
                IconButton(
                    onClick = { onStep(1) },
                    enabled = minutes != null && minutes < Settings.DAILY_GOAL_RANGE.last,
                ) {
                    Icon(painterResource(R.drawable.add), contentDescription = stringResource(R.string.daily_goal_more))
                }
            }
        },
    )
}
