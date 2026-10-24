package com.somkov.greek_b1.progress

import android.app.Application
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.somkov.greek_b1.GreekB1Application
import com.somkov.greek_b1.R
import com.somkov.greek_b1.ScreenScaffold
import com.somkov.greek_b1.formatMinutes
import com.somkov.greek_b1.home.EXAM_DAY
import com.somkov.greek_b1.session.PhoneClock
import com.somkov.greek_b1.session.SessionState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

// The UI is in English, so the dates are too, whatever the language of the phone.
private val dayFormat = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)
private val fullDayFormat = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.ENGLISH)
private val dateTimeFormat = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", Locale.ENGLISH)
private val monthFormat = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)

/** The scale of the heat map: red at 0%, yellow at the pass mark, green at 100%. The same in light and dark. */
private val RED = Color(0xFFD32F2F)
private val YELLOW = Color(0xFFFBC02D)
private val GREEN = Color(0xFF388E3C)

/** The width of the column of the topic names of the heat map, against 1 for a part. */
private const val TOPIC_WEIGHT = 1.7f

private val squareShape = RoundedCornerShape(4.dp)

@StringRes
private fun partName(part: String): Int = when (part) {
    "reading" -> R.string.reading
    "listening" -> R.string.listening
    "writing" -> R.string.writing
    "speaking" -> R.string.speaking
    else -> error("no part $part")
}

/** The progress: the days left until the exam, the heat map of readiness and the calendar of the time per day. */
@Composable
fun ProgressScreen(onBack: () -> Unit) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel = viewModel {
        val container = (application as GreekB1Application).container
        ProgressViewModel(container.database, container.settings, PhoneClock)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    ScreenScaffold(stringResource(R.string.progress), onBack) { innerPadding ->
        state?.let { ProgressContent(it, Modifier.padding(innerPadding)) }
    }
}

/**
 * The content of the progress screen for [state]; an empty state while nothing is recorded. A
 * tap on a cell shows its trials in a dialog, a tap on a day its minutes under the calendar.
 */
@Composable
fun ProgressContent(state: ProgressState, modifier: Modifier = Modifier) {
    // Saved as plain values, so that they survive the recreation of the activity.
    var selectedCell by rememberSaveable { mutableStateOf<List<String>?>(null) }
    var selectedDay by rememberSaveable { mutableStateOf<Long?>(null) }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        DaysLeft(state.daysUntilExam)
        if (state.isEmpty) {
            Text(stringResource(R.string.progress_empty), style = MaterialTheme.typography.bodyLarge)
        } else {
            HeatMap(state, onCell = { selectedCell = listOf(it.topic, it.part) })
            DayCalendar(state, selectedDay?.let(LocalDate::ofEpochDay), onDay = { selectedDay = it.toEpochDay() })
        }
    }
    selectedCell?.let { (topic, part) ->
        CellDialog(state, state.readiness.cell(topic, part), onDismiss = { selectedCell = null })
    }
}

@Composable
private fun DaysLeft(days: Long) {
    if (days < 0) return
    Column {
        Text(pluralStringResource(R.plurals.days, days.toInt(), days), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.countdown_exam), style = MaterialTheme.typography.titleSmall)
    }
}

/** A row per topic and a column per part, with the readiness of each part at the head of its column. */
@Composable
private fun HeatMap(state: ProgressState, onCell: (Cell) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.readiness), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
            Spacer(Modifier.weight(TOPIC_WEIGHT))
            for (part in PARTS_IN_LEARNING_ORDER) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(partName(part)),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(formatPercent(state.readiness.parts[part]), style = MaterialTheme.typography.titleSmall)
                }
            }
        }
        for (topic in state.topics) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    topic.name,
                    modifier = Modifier.weight(TOPIC_WEIGHT),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                for (part in PARTS_IN_LEARNING_ORDER) {
                    val cell = state.readiness.cell(topic.id, part)
                    val description = stringResource(R.string.readiness_cell, topic.name, stringResource(partName(part)))
                    HeatCell(cell, description, Modifier.weight(1f)) { onCell(cell) }
                }
            }
        }
        Text(stringResource(R.string.readiness_legend), style = MaterialTheme.typography.bodySmall)
    }
}

/** A cell in the color of its readiness, gray without a result, outlined while it is not advised. */
@Composable
private fun HeatCell(cell: Cell, description: String, modifier: Modifier, onClick: () -> Unit) {
    val readiness = cell.readiness
    val color = readiness?.let(::heatColor) ?: MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (readiness == null) MaterialTheme.colorScheme.onSurfaceVariant else contentColorOn(color)
    val outline = if (cell.advised) Modifier else Modifier.border(2.dp, MaterialTheme.colorScheme.outline, squareShape)
    Box(
        modifier
            .height(36.dp)
            .clip(squareShape)
            .background(color)
            .then(outline)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (readiness != null) Text(formatPercent(readiness), style = MaterialTheme.typography.labelMedium, color = textColor)
    }
}

private fun heatColor(readiness: Double): Color =
    if (readiness < PASS_MARK) {
        lerp(RED, YELLOW, (readiness / PASS_MARK).toFloat())
    } else {
        lerp(YELLOW, GREEN, ((readiness - PASS_MARK) / (1 - PASS_MARK)).toFloat())
    }

private fun contentColorOn(background: Color): Color = if (background.luminance() > 0.4f) Color.Black else Color.White

/** The readiness of a cell, the trials of the cell the latest first, and why it is not advised yet. */
@Composable
private fun CellDialog(state: ProgressState, cell: Cell, onDismiss: () -> Unit) {
    val topicName = state.topics.firstOrNull { it.id == cell.topic }?.name ?: cell.topic
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
        title = { Text(stringResource(R.string.readiness_cell, topicName, stringResource(partName(cell.part)))) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val readiness = cell.readiness
                Text(
                    if (readiness == null) {
                        stringResource(R.string.readiness_none)
                    } else {
                        stringResource(R.string.readiness_of_cell, formatPercent(readiness))
                    },
                    style = MaterialTheme.typography.titleSmall,
                )
                if (!cell.advised) {
                    val previous = PARTS_IN_LEARNING_ORDER[PARTS_IN_LEARNING_ORDER.indexOf(cell.part) - 1]
                    Text(stringResource(R.string.readiness_not_advised, stringResource(partName(previous))))
                }
                if (cell.trials.isEmpty()) Text(stringResource(R.string.trials_none))
                for (trial in cell.trials) {
                    Column {
                        Text(state.itemTitles[trial.itemId] ?: trial.itemId, style = MaterialTheme.typography.bodyMedium)
                        Text(trialSummary(trial), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
    )
}

/** For example "Tue 20 Oct, 18:05 · 8/10 in 15:02 of 12:30 · 67%". */
@Composable
private fun trialSummary(trial: Trial): String {
    // The phone's time zone now: a trial keeps only the offset of its start, which is the same
    // unless the phone has moved since.
    val start = Instant.ofEpochMilli(trial.startedAtMillis).atZone(ZoneId.systemDefault()).format(dateTimeFormat)
    val time = formatClock(trial.activeMillis)
    val limit = formatClock(trial.timeLimitSeconds * 1000L)
    val result = when {
        trial.state == SessionState.OPEN -> stringResource(R.string.trial_open)
        trial.state == SessionState.ABANDONED -> stringResource(R.string.trial_abandoned)
        trial.points == null || trial.maxPoints == null -> stringResource(R.string.trial_unassessed, time, limit)
        else -> stringResource(R.string.trial_result, formatPoints(trial.points), formatPoints(trial.maxPoints), time, limit)
    }
    return listOfNotNull(start, result, trial.readiness?.let(::formatPercent)).joinToString(" · ")
}

/** A square per day in a row per week, from the first day of study to the day of the exam, and the minutes of the day tapped. */
@Composable
private fun DayCalendar(state: ProgressState, selected: LocalDate?, onDay: (LocalDate) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.time_per_day), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.time_per_day_goal, formatMinutes(state.goalMinutes)),
            style = MaterialTheme.typography.bodySmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (weekday in listOf("M", "T", "W", "T", "F", "S", "S")) {
                Text(
                    weekday,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                )
            }
        }
        for (week in calendarWeeks(state.firstDay, state.lastDay)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (day in week) {
                    if (day == null) {
                        Spacer(Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        DaySquare(state, day, Modifier.weight(1f)) { onDay(day) }
                    }
                }
            }
        }
        if (selected != null) {
            Text(daySummary(state, selected), style = MaterialTheme.typography.bodyMedium)
        }
        Text(stringResource(R.string.time_per_day_legend), style = MaterialTheme.typography.bodySmall)
    }
}

/**
 * A day of the calendar: gray without work, from pale to full in the color of the theme up to
 * the goal, with a check mark when the goal is met; outlined in the future, today in bold.
 */
@Composable
private fun DaySquare(state: ProgressState, day: LocalDate, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val dayTime = state.days[day]
    val future = day > state.today
    val goalMet = dayTime != null && dayTime.goalMet(state.goalMinutes)
    val background = when {
        future -> Color.Transparent
        dayTime == null || dayTime.totalMillis == 0L -> colors.surfaceVariant
        // The palest step stays apart from the gray of a day without work.
        else -> lerp(colors.surfaceVariant, colors.primary, 0.3f + 0.7f * dayTime.goalShare(state.goalMinutes).toFloat())
    }
    val textColor = if (future || background == colors.surfaceVariant) colors.onSurfaceVariant else contentColorOn(background)
    val border = when {
        day == state.today -> Modifier.border(2.dp, colors.onSurface, squareShape)
        day == EXAM_DAY -> Modifier.border(2.dp, colors.tertiary, squareShape)
        future -> Modifier.border(1.dp, colors.outlineVariant, squareShape)
        else -> Modifier
    }
    val label = when {
        day == EXAM_DAY -> stringResource(R.string.exam)
        day.dayOfMonth == 1 || day == state.firstDay -> day.format(monthFormat)
        else -> null
    }
    val description = day.format(fullDayFormat)
    Box(
        modifier
            .aspectRatio(1f)
            .clip(squareShape)
            .background(background)
            .then(border)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (label != null) Text(label, style = MaterialTheme.typography.labelSmall, color = textColor)
            Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.labelMedium, color = textColor)
        }
        if (goalMet) {
            Icon(
                painterResource(R.drawable.check),
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).size(12.dp),
            )
        }
    }
}

/**
 * For a past day or today: "Tue 20 Oct: 1 h 35 min of 2 h", and the study and the trials in it.
 * For a future day: the days from it to the exam.
 */
@Composable
private fun daySummary(state: ProgressState, day: LocalDate): String {
    val date = day.format(dayFormat)
    if (day > state.today) {
        val days = ChronoUnit.DAYS.between(day, EXAM_DAY)
        return when {
            days > 0 -> stringResource(R.string.day_before_exam, date, pluralStringResource(R.plurals.days, days.toInt(), days))
            days == 0L -> stringResource(R.string.day_of_exam, date)
            else -> date
        }
    }
    val dayTime = state.days[day] ?: DayTime()
    val total = stringResource(R.string.day_minutes, date, formatMinutes(dayTime.minutes.toInt()), formatMinutes(state.goalMinutes))
    if (dayTime.totalMillis == 0L) return total
    val split = stringResource(
        R.string.day_split,
        formatMinutes((dayTime.studyMillis / 60_000).toInt()),
        formatMinutes((dayTime.trialMillis / 60_000).toInt()),
        pluralStringResource(R.plurals.trials, dayTime.trials, dayTime.trials),
    )
    return total + "\n" + split
}

/** For example "67%"; a dash without a value. */
private fun formatPercent(share: Double?): String = share?.let { "${(it * 100).roundToInt()}%" } ?: "–"

/** For example "8" or "7.5". */
private fun formatPoints(points: Double): String =
    if (points % 1.0 == 0.0) points.toLong().toString() else "%.1f".format(Locale.ENGLISH, points)

/** For example "12:30" for 12 min 30 s. */
private fun formatClock(millis: Long): String {
    val seconds = millis / 1000
    return "%d:%02d".format(Locale.ENGLISH, seconds / 60, seconds % 60)
}
