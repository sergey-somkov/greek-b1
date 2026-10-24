package com.somkov.greek_b1.progress

import com.somkov.greek_b1.content.CatalogTopic
import com.somkov.greek_b1.home.CYPRUS
import com.somkov.greek_b1.home.TodayState
import com.somkov.greek_b1.home.todayState
import com.somkov.greek_b1.session.Session
import com.somkov.greek_b1.session.SessionKind
import com.somkov.greek_b1.session.SessionState
import java.time.Clock
import java.time.LocalDateTime

/**
 * Made-up progress for the tests of the progress screen and of today on home: a few days of
 * sessions until "today", Fri 20 Nov 2026 at noon in Cyprus, 52 days before the exam. It never
 * reaches a real database.
 */
class SampleProgress {
    val clock: Clock = Clock.fixed(LocalDateTime.parse("2026-11-20T12:00").atZone(CYPRUS).toInstant(), CYPRUS)

    val topics = listOf(
        CatalogTopic("personal", "Personal identity", "Ταυτότητα", retired = false),
        CatalogTopic("home", "Home and residence", "Κατοικία", retired = false),
        CatalogTopic("work", "Work and employment", "Εργασία", retired = false),
    )

    private val sessions = mutableListOf<Session>()
    private val trials = mutableListOf<TrialRow>()

    init {
        // Wed 18 Nov: a trial and a long study session, over the goal of 2 hours.
        trial("2026-11-18T09:00", "home", "reading", "Το καινούργιο σπίτι", minutes = 25.0, points = 4.0)
        study("2026-11-18T10:00", minutes = 100)
        // Thu 19 Nov: a short study session, and a trial abandoned after 3:00, which counts to the evening before.
        study("2026-11-19T20:00", minutes = 30)
        trial("2026-11-20T01:00", "personal", "reading", "Γνωριμία", minutes = 3.0, state = SessionState.ABANDONED)
        // Fri 20 Nov, today: three trials.
        trial("2026-11-20T09:00", "home", "reading", "Ψάχνοντας διαμέρισμα", minutes = 10.0, points = 8.0)
        trial("2026-11-20T09:30", "home", "listening", "Στο μεσιτικό γραφείο", minutes = 9.0, points = 5.0, limitMinutes = 7.5)
        trial("2026-11-20T10:00", "work", "writing", "Αίτηση για δουλειά", minutes = 30.0, points = null, limitMinutes = 35.0)
    }

    fun state(goalMinutes: Int = 120): ProgressState = progressState(topics, trials, sessions, goalMinutes, calendarDays(clock))

    /** The state before the first session. */
    fun emptyState(): ProgressState = progressState(topics, emptyList(), emptyList(), 120, calendarDays(clock))

    /** Today on home. */
    fun today(goalMinutes: Int = 120): TodayState = todayState(sessions, goalMinutes, today(clock))

    /** Today on home before the first session. */
    fun emptyToday(): TodayState = todayState(emptyList(), 120, today(clock))

    private fun session(start: String, kind: SessionKind, state: SessionState, minutes: Double, limitMinutes: Double?): Session {
        val started = LocalDateTime.parse(start).atZone(CYPRUS)
        val session = Session(
            id = sessions.size + 1L,
            itemId = "item.${sessions.size + 1}",
            kind = kind,
            state = state,
            timeLimitSeconds = limitMinutes?.let { (it * 60).toInt() },
            startedAtMillis = started.toInstant().toEpochMilli(),
            zoneOffsetSeconds = started.offset.totalSeconds,
            endedAtMillis = started.toInstant().toEpochMilli() + (minutes * 60_000).toLong(),
            activeMillis = (minutes * 60_000).toLong(),
            answers = null,
        )
        sessions += session
        return session
    }

    private fun study(start: String, minutes: Int) {
        session(start, SessionKind.STUDY, SessionState.FINISHED, minutes.toDouble(), limitMinutes = null)
    }

    private fun trial(
        start: String,
        topic: String,
        part: String,
        title: String,
        minutes: Double,
        points: Double? = null,
        state: SessionState = SessionState.FINISHED,
        limitMinutes: Double = 12.5,
    ) {
        val session = session(start, SessionKind.TRIAL, state, minutes, limitMinutes)
        val checked = state == SessionState.FINISHED
        val trial = Trial(
            sessionId = session.id,
            itemId = session.itemId,
            topic = topic,
            part = part,
            state = state,
            startedAtMillis = session.startedAtMillis,
            timeLimitSeconds = session.timeLimitSeconds!!,
            activeMillis = session.activeMillis,
            points = points,
            maxPoints = if (checked) 10.0 else null,
        )
        trials += TrialRow(trial, title)
    }
}
