package com.somkov.greek_b1.home

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

// The dates of the countdown on home. They change once or twice, and a new build costs less than
// a settings UI.

/** The time zone of the registration and the exam: both are in Nicosia. */
val CYPRUS: ZoneId = ZoneId.of("Europe/Nicosia")

/** The UCY registration opens at this time in Cyprus; the B1 places are usually gone within hours. */
val REGISTRATION_OPENS: LocalDateTime = LocalDateTime.of(2026, 11, 2, 9, 0)

/**
 * The day of the exam. Until the exact date is known (each candidate gets it after the registration
 * closes), the first day of the session, 11–29 January: the worst case. When it changes, change
 * the note `countdown_exam_note` in strings.xml too.
 */
val EXAM_DAY: LocalDate = LocalDate.of(2027, 1, 11)

/** The time left until the registration opens, or null once it has opened. */
fun timeUntilRegistration(now: Instant): Duration? {
    val left = Duration.between(now, REGISTRATION_OPENS.atZone(CYPRUS).toInstant())
    return if (left > Duration.ZERO) left else null
}

/**
 * The days left until the exam by the calendar in Cyprus, wherever the phone is: 1 on the day
 * before, 0 on the day itself, negative after it.
 */
fun daysUntilExam(now: Instant): Long = ChronoUnit.DAYS.between(now.atZone(CYPRUS).toLocalDate(), EXAM_DAY)
