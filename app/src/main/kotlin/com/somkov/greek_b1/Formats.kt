package com.somkov.greek_b1

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/** A duration in minutes, for example "2 h", "2 h 15 min" or "45 min". */
@Composable
fun formatMinutes(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0 -> stringResource(R.string.duration_minutes, rest)
        rest == 0 -> stringResource(R.string.duration_hours, hours)
        else -> stringResource(R.string.duration_hours_minutes, hours, rest)
    }
}
