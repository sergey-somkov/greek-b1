package com.somkov.greek_b1.home

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CountdownTest {
    private val registration = Instant.parse("2026-11-02T07:00:00Z")

    /** The exam day at a given time in Cyprus. */
    private fun examDayAt(hour: Int, minute: Int): Instant =
        EXAM_DAY.atTime(LocalTime.of(hour, minute)).atZone(CYPRUS).toInstant()

    @Test
    fun `registration opens at 09-00 winter time in Cyprus`() {
        // Summer time in Cyprus ends on 2026-10-25, so 09:00 on 2 November is UTC+2, not UTC+3.
        assertEquals(registration, REGISTRATION_OPENS.atZone(CYPRUS).toInstant())
    }

    @Test
    fun `time until registration counts across the end of summer time`() {
        // 12:00 in Cyprus (UTC+3): 9 days and 21 hours on the clock, plus the hour gained on 25 October.
        val now = Instant.parse("2026-10-23T09:00:00Z")
        assertEquals(Duration.ofDays(9).plusHours(22), timeUntilRegistration(now))
    }

    @Test
    fun `time until registration in its last second`() {
        assertEquals(Duration.ofMillis(1), timeUntilRegistration(registration.minusMillis(1)))
    }

    @Test
    fun `time until registration is null once it has opened`() {
        assertNull(timeUntilRegistration(registration))
        assertNull(timeUntilRegistration(registration.plusSeconds(1)))
    }

    @Test
    fun `days until exam on the day before, the day itself and the day after`() {
        assertEquals(1, daysUntilExam(examDayAt(0, 0).minusMillis(1)))
        assertEquals(0, daysUntilExam(examDayAt(0, 0)))
        assertEquals(0, daysUntilExam(examDayAt(23, 59)))
        assertEquals(-1, daysUntilExam(examDayAt(0, 0).plus(Duration.ofDays(1))))
    }

    @Test
    fun `days until exam follow the calendar in Cyprus, not in UTC`() {
        // 22:30 UTC on 23 October is 01:30 on 24 October in Cyprus (UTC+3).
        val now = Instant.parse("2026-10-23T22:30:00Z")
        assertEquals(EXAM_DAY.toEpochDay() - LocalDate.of(2026, 10, 24).toEpochDay(), daysUntilExam(now))
    }
}
