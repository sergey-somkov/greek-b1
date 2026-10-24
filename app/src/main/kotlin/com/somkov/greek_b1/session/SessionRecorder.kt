package com.somkov.greek_b1.session

import androidx.room3.withWriteTransaction
import com.somkov.greek_b1.data.AppDatabase
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Records the sessions on the content items; the exercise screens of the parts call it. One
 * instance lives in the app container, and there is at most one open session, which the app
 * resumes at its next start ([openSession]).
 *
 * The life of a session:
 * - [start] begins a trial on a new item, or a study session on an item that has had its trial.
 * - [saveAnswers] saves the answers as they change, [check] records a check of them.
 * - A trial ends with its check ([check] finishes it) or when the user gives it up ([abandon]); a
 *   study session ends when the user returns to the list ([finish]).
 *
 * The timers:
 * - A trial runs on the clock from its start, so the screen going off and the death of the
 *   process do not stop it: its time is the end minus the start, written at the end.
 * - A study session counts only the time with its item on the screen, which the screen reports
 *   with [show] and [hide]. The time is kept in memory and saved at each call that writes the
 *   session (a hide, a save of the answers, a check, the end), so the death of the process loses
 *   only the time since the last of them. A resumed study session waits for [show].
 *
 * The calls are serialized: each one runs to its end before the next one starts. A call that
 * does not fit the state (a second start, a check without a session) is a bug of the screen and
 * throws [IllegalStateException].
 */
class SessionRecorder(
    private val database: AppDatabase,
    /** The time and the time zone; the tests set them. */
    private val clock: Clock,
) {
    private val sessions = database.sessionDao()
    private val content = database.contentDao()
    private val mutex = Mutex()

    /**
     * Since when the item of a study session has been on the screen, counted into the session's
     * time from then; null while it is not. Reset to the time of the start and of the end of a
     * session, so that one session never counts the time of another.
     */
    private var visibleSince: Instant? = null

    /** The open session, or null; the app resumes it at its start. */
    suspend fun openSession(): Session? = mutex.withLock { sessions.openSession() }

    /**
     * The time of work on the open session until now, for the timer on the screen: from its start
     * for a trial, the time on the screen for a study session; 0 without an open session.
     */
    suspend fun activeMillis(): Long = mutex.withLock {
        val session = sessions.openSession() ?: return@withLock 0
        when (session.kind) {
            SessionKind.TRIAL -> trialMillis(session, clock.instant())
            SessionKind.STUDY -> session.activeMillis + visibleMillis(clock.instant())
        }
    }

    /**
     * Starts a session on the item: a trial if the item has never had one, a study session
     * otherwise. The item must be in the content tables, and no other session may be open.
     */
    suspend fun start(itemId: String): Session = mutex.withLock {
        val now = clock.instant()
        val session = database.withWriteTransaction {
            check(sessions.openSession() == null) { "a session is open already" }
            val item = checkNotNull(content.item(itemId)) { "no item $itemId" }
            val kind = if (sessions.hasTrial(itemId)) SessionKind.STUDY else SessionKind.TRIAL
            val timeLimitSeconds = when (kind) {
                SessionKind.TRIAL -> checkNotNull(content.taskType(item.taskType)) { "no task type ${item.taskType}" }.timeSeconds
                SessionKind.STUDY -> null
            }
            val session = Session(
                itemId = itemId,
                kind = kind,
                state = SessionState.OPEN,
                timeLimitSeconds = timeLimitSeconds,
                startedAtMillis = now.toEpochMilli(),
                zoneOffsetSeconds = clock.zone.rules.getOffset(now).totalSeconds,
                endedAtMillis = null,
                activeMillis = 0,
                answers = null,
            )
            session.copy(id = sessions.insertSession(session))
        }
        if (visibleSince != null) visibleSince = now
        session
    }

    /** The item of the open session came on the screen: the timer of a study session runs. */
    suspend fun show() = mutex.withLock {
        if (visibleSince == null) visibleSince = clock.instant()
    }

    /** The item of the open session left the screen: the timer of a study session stops. */
    suspend fun hide() = mutex.withLock {
        val session = sessions.openSession()
        if (session != null) saveStudyTime(session, clock.instant())
        visibleSince = null
    }

    /** Saves the current answers of the open session, in the format of [Session.answers]. */
    suspend fun saveAnswers(answers: String) = mutex.withLock {
        val session = requireOpen()
        database.withWriteTransaction {
            sessions.saveAnswers(session.id, answers)
            saveStudyTime(session, clock.instant())
        }
    }

    /**
     * Records a check of [answers] with their [points] (null until the assessment gives them) out
     * of [maxPoints], and saves the answers. A trial has one check, which finishes it; a study
     * session stays open.
     */
    suspend fun check(answers: String, points: Double?, maxPoints: Double): AnswerCheck = mutex.withLock {
        val session = requireOpen()
        val now = clock.instant()
        database.withWriteTransaction {
            val check = AnswerCheck(
                sessionId = session.id,
                checkedAtMillis = now.toEpochMilli(),
                answers = answers,
                points = points,
                maxPoints = maxPoints,
            )
            val id = sessions.insertCheck(check)
            sessions.saveAnswers(session.id, answers)
            when (session.kind) {
                SessionKind.TRIAL -> end(session, SessionState.FINISHED, now)
                SessionKind.STUDY -> saveStudyTime(session, now)
            }
            check.copy(id = id)
        }
    }

    /** Ends the open study session: the user returned to the list. A trial ends with its [check]. */
    suspend fun finish() = mutex.withLock {
        val session = requireOpen()
        check(session.kind == SessionKind.STUDY) { "a trial ends with its check" }
        end(session, SessionState.FINISHED, clock.instant())
    }

    /** Ends the open trial as abandoned: the user gave it up. */
    suspend fun abandon() = mutex.withLock {
        val session = requireOpen()
        check(session.kind == SessionKind.TRIAL) { "only a trial can be abandoned" }
        end(session, SessionState.ABANDONED, clock.instant())
    }

    private suspend fun requireOpen(): Session = checkNotNull(sessions.openSession()) { "no open session" }

    /** Ends [session] at [now] with its final time; the screen may still show the item. */
    private suspend fun end(session: Session, state: SessionState, now: Instant) {
        val activeMillis = when (session.kind) {
            SessionKind.TRIAL -> trialMillis(session, now)
            SessionKind.STUDY -> session.activeMillis + visibleMillis(now)
        }
        sessions.end(session.id, state, endedAtMillis = now.toEpochMilli(), activeMillis = activeMillis)
        if (visibleSince != null) visibleSince = now
    }

    /** Adds the time on the screen since the last save to a study session. */
    private suspend fun saveStudyTime(session: Session, now: Instant) {
        if (session.kind != SessionKind.STUDY || visibleSince == null) return
        sessions.saveActiveTime(session.id, session.activeMillis + visibleMillis(now))
        visibleSince = now
    }

    private fun visibleMillis(now: Instant): Long =
        visibleSince?.let { (now.toEpochMilli() - it.toEpochMilli()).coerceAtLeast(0) } ?: 0

    // Never negative, even if the clock of the phone is set back.
    private fun trialMillis(session: Session, now: Instant): Long =
        (now.toEpochMilli() - session.startedAtMillis).coerceAtLeast(0)
}

/**
 * The clock of the phone in its current time zone. [Clock.systemDefaultZone] keeps the zone that
 * was current when it was made, but the app lives on across a trip to another time zone, and
 * Android updates the default zone of a running app when the phone's zone changes.
 */
object PhoneClock : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()

    override fun withZone(zone: ZoneId): Clock = Clock.system(zone)

    override fun instant(): Instant = Instant.now()
}
