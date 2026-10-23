package com.somkov.greek_b1.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.somkov.greek_b1.R
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

// The UI is in English, so the dates are too, whatever the language of the phone.
private val dateTime = DateTimeFormatter.ofPattern("EEE d MMM yyyy, HH:mm", Locale.ENGLISH)
private val date = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.ENGLISH)

/**
 * The time left until the registration opens, hidden once it has opened, and the days left until
 * the exam, hidden after it. Ticks every second while the screen is visible.
 */
@Composable
fun CountdownCard(modifier: Modifier = Modifier) {
    val now = currentTime()
    val registration = timeUntilRegistration(now)
    val examDays = daysUntilExam(now)
    if (registration == null && examDays < 0) return

    Card(modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (registration != null) {
                Countdown(
                    value = formatTimeLeft(registration),
                    label = stringResource(R.string.countdown_registration),
                    note = stringResource(R.string.countdown_registration_note, REGISTRATION_OPENS.format(dateTime)),
                )
            }
            if (examDays >= 0) {
                Countdown(
                    value = pluralStringResource(R.plurals.days, examDays.toInt(), examDays),
                    label = stringResource(R.string.countdown_exam),
                    note = stringResource(R.string.countdown_exam_note, EXAM_DAY.format(date)),
                )
            }
        }
    }
}

@Composable
private fun Countdown(value: String, label: String, note: String) {
    Column {
        Text(value, style = MaterialTheme.typography.headlineMedium)
        Text(label, style = MaterialTheme.typography.titleSmall)
        Text(note, style = MaterialTheme.typography.bodySmall)
    }
}

/** For example "9 days 22:41:30", or "22:41:30" in the last day. */
@Composable
private fun formatTimeLeft(left: Duration): String {
    val time = "%02d:%02d:%02d".format(Locale.ENGLISH, left.toHoursPart(), left.toMinutesPart(), left.toSecondsPart())
    val days = left.toDays()
    return if (days == 0L) time else pluralStringResource(R.plurals.days, days.toInt(), days) + " " + time
}

/**
 * The current time, updated at each full second while the screen is started. It stops in the
 * background, and catches up at once when the app comes back.
 */
@Composable
private fun currentTime(): Instant {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val now by produceState(Instant.now(), lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                value = Instant.now()
                delay(1000 - value.toEpochMilli() % 1000)
            }
        }
    }
    return now
}
