package com.somkov.greek_b1.progress

import com.somkov.greek_b1.home.CYPRUS
import com.somkov.greek_b1.session.Session
import com.somkov.greek_b1.session.SessionKind
import com.somkov.greek_b1.session.SessionState
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// The virtual time of kotlinx-coroutines-test (testScheduler.currentTime, advanceTimeBy).
@OptIn(ExperimentalCoroutinesApi::class)
class DailyTimeTest {
    private val newYork: ZoneId = ZoneId.of("America/New_York")

    /** A session started at the local time [start] in [zone], with [minutes] of work. */
    private fun session(
        start: String,
        minutes: Long,
        kind: SessionKind = SessionKind.STUDY,
        state: SessionState = SessionState.FINISHED,
        zone: ZoneId = CYPRUS,
    ): Session {
        val started = LocalDateTime.parse(start).atZone(zone)
        return Session(
            itemId = "reading.true_false.001",
            kind = kind,
            state = state,
            timeLimitSeconds = if (kind == SessionKind.TRIAL) 750 else null,
            startedAtMillis = started.toInstant().toEpochMilli(),
            zoneOffsetSeconds = started.offset.totalSeconds,
            endedAtMillis = if (state == SessionState.OPEN) null else started.plusMinutes(minutes).toInstant().toEpochMilli(),
            activeMillis = minutes * 60_000,
            answers = null,
        )
    }

    private fun clockAt(dateTime: String, zone: ZoneId): Clock =
        Clock.fixed(LocalDateTime.parse(dateTime).atZone(zone).toInstant(), zone)

    /** A clock that starts at the local time [start] in [zone] and runs with the virtual time of the test. */
    private class VirtualClock(private val scope: TestScope, start: String, var phoneZone: ZoneId) : Clock() {
        private val startMillis = LocalDateTime.parse(start).atZone(phoneZone).toInstant().toEpochMilli()

        override fun getZone(): ZoneId = phoneZone

        override fun withZone(zone: ZoneId): Clock = throw UnsupportedOperationException()

        override fun instant(): Instant = Instant.ofEpochMilli(startMillis + scope.testScheduler.currentTime)
    }

    /** What [clockFlow] gives with [read] over [minutes] from the start of [clock], each with its local time. */
    private fun <T> TestScope.values(clock: Clock, minutes: Long, read: (Clock) -> T): List<Pair<T, LocalDateTime>> {
        val values = mutableListOf<Pair<T, LocalDateTime>>()
        clockFlow(clock, read).onEach { values += it to LocalDateTime.now(clock) }.launchIn(backgroundScope)
        advanceTimeBy(minutes * 60_000 + 1)
        return values
    }

    @Test
    fun `the day of study ends at 3-00 local time`() {
        assertEquals(LocalDate.of(2026, 11, 9), session("2026-11-10T02:59:59", 30).studyDay)
        assertEquals(LocalDate.of(2026, 11, 10), session("2026-11-10T03:00", 30).studyDay)
        assertEquals(LocalDate.of(2026, 11, 10), session("2026-11-10T23:59", 30).studyDay)
    }

    @Test
    fun `a session over 3-00 counts whole to the day it started`() {
        val days = dailyTime(listOf(session("2026-11-10T02:30", 90), session("2026-11-10T09:00", 20)))
        assertEquals(DayTime(studyMillis = 90 * 60_000), days[LocalDate.of(2026, 11, 9)])
        assertEquals(DayTime(studyMillis = 20 * 60_000), days[LocalDate.of(2026, 11, 10)])
    }

    @Test
    fun `a trip to another time zone does not move the past days`() {
        // 08:30 in Cyprus is 01:30 in New York, which would be the day before there.
        val atHome = session("2026-11-10T08:30", 40)
        val abroad = session("2026-11-10T22:00", 15, zone = newYork)
        val days = dailyTime(listOf(atHome, abroad))
        assertEquals(DayTime(studyMillis = 55 * 60_000), days[LocalDate.of(2026, 11, 10)])
        assertEquals(setOf(LocalDate.of(2026, 11, 10)), days.keys)
    }

    @Test
    fun `today follows the time zone of the phone`() {
        // The same instant: 08:30 in Cyprus, 01:30 in New York.
        assertEquals(LocalDate.of(2026, 11, 10), today(clockAt("2026-11-10T08:30", CYPRUS)))
        assertEquals(LocalDate.of(2026, 11, 9), today(clockAt("2026-11-10T01:30", newYork)))
    }

    @Test
    fun `the day of study follows the local clock across the end of summer time`() {
        // In Cyprus the clock goes back from 04:00 to 03:00 on 25 October 2026.
        assertEquals(LocalDate.of(2026, 10, 24), today(clockAt("2026-10-25T02:59", CYPRUS)))
        assertEquals(LocalDate.of(2026, 10, 25), today(clockAt("2026-10-25T03:30", CYPRUS)))
    }

    @Test
    fun `today moves to the next day at 3-00 sharp`() = runTest {
        val clock = VirtualClock(this, "2026-11-10T02:58:30", CYPRUS)
        assertEquals(
            listOf(
                LocalDate.of(2026, 11, 9) to LocalDateTime.parse("2026-11-10T02:58:30"),
                LocalDate.of(2026, 11, 10) to LocalDateTime.parse("2026-11-10T03:00"),
            ),
            values(clock, minutes = 120, ::today),
        )
    }

    @Test
    fun `today moves once at the end of summer time, when 3-00 comes twice`() = runTest {
        // In Cyprus the clock goes back from 04:00 to 03:00 on 25 October 2026.
        val clock = VirtualClock(this, "2026-10-25T02:30", CYPRUS)
        assertEquals(
            listOf(
                LocalDate.of(2026, 10, 24) to LocalDateTime.parse("2026-10-25T02:30"),
                LocalDate.of(2026, 10, 25) to LocalDateTime.parse("2026-10-25T03:00"),
            ),
            values(clock, minutes = 180, ::today),
        )
    }

    @Test
    fun `the days until the exam move at midnight in Cyprus, today at 3-00`() = runTest {
        val clock = VirtualClock(this, "2026-11-20T23:59:30", CYPRUS)
        assertEquals(
            listOf(
                CalendarDays(LocalDate.of(2026, 11, 20), 52) to LocalDateTime.parse("2026-11-20T23:59:30"),
                CalendarDays(LocalDate.of(2026, 11, 20), 51) to LocalDateTime.parse("2026-11-21T00:00"),
                CalendarDays(LocalDate.of(2026, 11, 21), 51) to LocalDateTime.parse("2026-11-21T03:00"),
            ),
            values(clock, minutes = 240, ::calendarDays),
        )
    }

    @Test
    fun `today follows a change of the time zone within a minute`() = runTest {
        val clock = VirtualClock(this, "2026-11-10T08:30:20", CYPRUS)
        val days = mutableListOf<Pair<LocalDate, LocalDateTime>>()
        clockFlow(clock, ::today).onEach { days += it to LocalDateTime.now(clock) }.launchIn(backgroundScope)
        advanceTimeBy(1)
        // A flight lands: 08:30 in Cyprus is 01:30 in New York, still the day before there.
        clock.phoneZone = newYork
        advanceTimeBy(60_000)
        assertEquals(
            listOf(
                LocalDate.of(2026, 11, 10) to LocalDateTime.parse("2026-11-10T08:30:20"),
                LocalDate.of(2026, 11, 9) to LocalDateTime.parse("2026-11-10T01:31"),
            ),
            days,
        )
    }

    @Test
    fun `a day adds up its study sessions and its trials`() {
        val day = dailyTime(
            listOf(
                session("2026-11-10T08:00", 25, kind = SessionKind.TRIAL),
                session("2026-11-10T08:30", 40),
                session("2026-11-10T19:00", 13, kind = SessionKind.TRIAL, state = SessionState.ABANDONED),
                session("2026-11-10T20:00", 35),
            ),
        ).getValue(LocalDate.of(2026, 11, 10))
        assertEquals(DayTime(studyMillis = 75 * 60_000, trialMillis = 38 * 60_000, trials = 2), day)
        assertEquals(113, day.minutes)
    }

    @Test
    fun `an open trial counts 0 until its end, an open study session its saved time`() {
        val day = dailyTime(
            listOf(
                session("2026-11-10T08:00", 0, kind = SessionKind.TRIAL, state = SessionState.OPEN),
                session("2026-11-10T07:00", 10, state = SessionState.OPEN),
            ),
        ).getValue(LocalDate.of(2026, 11, 10))
        assertEquals(DayTime(studyMillis = 10 * 60_000, trials = 1), day)
    }

    @Test
    fun `a day without sessions is not in the map`() {
        assertTrue(dailyTime(emptyList()).isEmpty())
    }

    @Test
    fun `the minutes against the goal`() {
        val short = DayTime(studyMillis = 119 * 60_000 + 59_999)
        assertEquals(119, short.minutes)
        assertFalse(short.goalMet(120))
        assertEquals(0.5, DayTime(studyMillis = 30 * 60_000, trialMillis = 30 * 60_000).goalShare(120), 1e-9)

        val met = DayTime(studyMillis = 100 * 60_000, trialMillis = 20 * 60_000)
        assertTrue(met.goalMet(120))
        assertEquals(1.0, met.goalShare(120), 1e-9)
        assertEquals(1.0, DayTime(studyMillis = 180 * 60_000).goalShare(120), 1e-9)
        assertEquals(0.0, DayTime().goalShare(120), 1e-9)
    }
}
