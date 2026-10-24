package com.somkov.greek_b1.progress

import com.somkov.greek_b1.session.Session
import com.somkov.greek_b1.session.SessionKind
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

/*
 * The time per day against the daily goal: a lower threshold, not the measure of progress. A day
 * of study ends at 3:00 local time, so late practice belongs to its evening, and a session counts
 * whole to the day it started, in the time zone of the phone at its start: a trip to another time
 * zone does not move the past days.
 */

/** The hour of the local time at which a day of study ends. */
const val DAY_ENDS_AT_HOUR = 3L

/** The day of study of the local time [dateTime]: its date, or the day before until 3:00. */
fun studyDay(dateTime: LocalDateTime): LocalDate = dateTime.minusHours(DAY_ENDS_AT_HOUR).toLocalDate()

/** The day of study that [session] counts to, by the zone offset of the phone at its start. */
val Session.studyDay: LocalDate
    get() = studyDay(
        LocalDateTime.ofInstant(Instant.ofEpochMilli(startedAtMillis), ZoneOffset.ofTotalSeconds(zoneOffsetSeconds)),
    )

/** Today's day of study, by the clock of the phone in its current time zone. */
fun today(clock: Clock): LocalDate = studyDay(LocalDateTime.now(clock))

/**
 * What [read] takes from [clock], such as today's day of study, and again each time it changes.
 * It reads the clock at each full minute, which is a full minute in every time zone, so a new day
 * comes at its first minute sharp (3:00 for the day of study, midnight in Cyprus for the days
 * until the exam), and a change of the phone's time or time zone shows within a minute.
 */
fun <T> clockFlow(clock: Clock, read: (Clock) -> T): Flow<T> = flow {
    while (true) {
        emit(read(clock))
        delay(60_000 - clock.millis() % 60_000)
    }
}.distinctUntilChanged()

/** The work of one day of study. */
data class DayTime(
    /** The time of the study sessions. */
    val studyMillis: Long = 0,
    /** The time of the trials, abandoned ones included. */
    val trialMillis: Long = 0,
    /** The trials started on the day. */
    val trials: Int = 0,
) {
    val totalMillis: Long get() = studyMillis + trialMillis

    /** The whole minutes of the day. */
    val minutes: Long get() = totalMillis / 60_000

    /** The share of the daily goal of [goalMinutes] reached, 0..1. */
    fun goalShare(goalMinutes: Int): Double = (totalMillis.toDouble() / (goalMinutes * 60_000L)).coerceIn(0.0, 1.0)

    fun goalMet(goalMinutes: Int): Boolean = totalMillis >= goalMinutes * 60_000L
}

/**
 * The work of each day of study with any session, from [sessions]. A session counts its active
 * time as saved: a study session as it grows, a trial at its end, so an open trial counts 0 until
 * then.
 */
fun dailyTime(sessions: List<Session>): Map<LocalDate, DayTime> =
    sessions.groupBy { it.studyDay }.mapValues { (_, daySessions) ->
        daySessions.fold(DayTime()) { day, session ->
            when (session.kind) {
                SessionKind.STUDY -> day.copy(studyMillis = day.studyMillis + session.activeMillis)
                SessionKind.TRIAL -> day.copy(trialMillis = day.trialMillis + session.activeMillis, trials = day.trials + 1)
            }
        }
    }
