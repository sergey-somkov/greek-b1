package com.somkov.greek_b1.home

import com.somkov.greek_b1.progress.DayTime
import com.somkov.greek_b1.progress.today
import com.somkov.greek_b1.session.Session
import com.somkov.greek_b1.session.SessionKind
import com.somkov.greek_b1.session.SessionState
import java.time.Clock
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class TodayTest {
    /** A finished session started at the local time [start] in Cyprus, with [minutes] of work. */
    private fun session(start: String, minutes: Long, kind: SessionKind = SessionKind.STUDY): Session {
        val started = LocalDateTime.parse(start).atZone(CYPRUS)
        return Session(
            itemId = "reading.true_false.001",
            kind = kind,
            state = SessionState.FINISHED,
            timeLimitSeconds = if (kind == SessionKind.TRIAL) 750 else null,
            startedAtMillis = started.toInstant().toEpochMilli(),
            zoneOffsetSeconds = started.offset.totalSeconds,
            endedAtMillis = started.plusMinutes(minutes).toInstant().toEpochMilli(),
            activeMillis = minutes * 60_000,
            answers = null,
        )
    }

    private fun clockAt(dateTime: String): Clock = Clock.fixed(LocalDateTime.parse(dateTime).atZone(CYPRUS).toInstant(), CYPRUS)

    private val sessions = listOf(
        session("2026-11-09T22:00", 40),
        // After midnight but before 3:00: still the evening of the 9th.
        session("2026-11-10T01:30", 30, SessionKind.TRIAL),
        session("2026-11-10T09:00", 60),
        session("2026-11-10T10:00", 20, SessionKind.TRIAL),
        session("2026-11-10T11:00", 15, SessionKind.TRIAL),
    )

    @Test
    fun `today counts the sessions of today's day of study only`() {
        val state = todayState(sessions, 120, today(clockAt("2026-11-10T12:00")))
        assertEquals(TodayState(DayTime(studyMillis = 60 * 60_000, trialMillis = 35 * 60_000, trials = 2), 120), state)
    }

    @Test
    fun `until 3-00 today is the day before`() {
        val state = todayState(sessions, 120, today(clockAt("2026-11-10T02:00")))
        assertEquals(TodayState(DayTime(studyMillis = 40 * 60_000, trialMillis = 30 * 60_000, trials = 1), 120), state)
    }

    @Test
    fun `a day without sessions is empty`() {
        assertEquals(TodayState(DayTime(), 90), todayState(sessions, 90, today(clockAt("2026-11-11T12:00"))))
        assertEquals(TodayState(DayTime(), 90), todayState(emptyList(), 90, today(clockAt("2026-11-11T12:00"))))
    }
}
