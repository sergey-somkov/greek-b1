package com.somkov.greek_b1.session

import androidx.test.platform.app.InstrumentationRegistry
import com.somkov.greek_b1.content.CatalogTaskType
import com.somkov.greek_b1.content.ContentItem
import com.somkov.greek_b1.data.AppDatabase
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

/**
 * The recorder on a database in memory, with a clock that the tests set. A new recorder on the
 * same database is the next start of the app after the death of the process.
 */
class SessionRecorderTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val clock = TestClock(START, ATHENS)
    private lateinit var database: AppDatabase
    private lateinit var recorder: SessionRecorder

    @Before
    fun openDatabase() = runBlocking {
        database = AppDatabase.buildInMemory(context)
        database.contentDao().insertTaskTypes(listOf(TASK_TYPE))
        database.contentDao().insertItems(listOf(ITEM, ITEM.copy(id = OTHER_ITEM)))
        recorder = SessionRecorder(database, clock)
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `a new item starts a trial with the time of its task`() = runBlocking<Unit> {
        val session = recorder.start(ITEM.id)

        val expected = Session(
            id = session.id,
            itemId = ITEM.id,
            kind = SessionKind.TRIAL,
            state = SessionState.OPEN,
            timeLimitSeconds = 750,
            startedAtMillis = START.toEpochMilli(),
            zoneOffsetSeconds = 3 * 3600,
            endedAtMillis = null,
            activeMillis = 0,
            answers = null,
        )
        assertEquals(expected, session)
        assertEquals(expected, recorder.openSession())
    }

    @Test
    fun `an item after its trial starts a study session`() = runBlocking<Unit> {
        recorder.start(ITEM.id)
        recorder.check(ANSWERS, points = 4.0, maxPoints = 6.0)
        recorder.start(OTHER_ITEM)
        recorder.abandon()

        val study = recorder.start(ITEM.id)
        recorder.finish()
        val afterAbandoned = recorder.start(OTHER_ITEM)

        assertEquals(SessionKind.STUDY to null, study.kind to study.timeLimitSeconds)
        assertEquals(SessionKind.STUDY to null, afterAbandoned.kind to afterAbandoned.timeLimitSeconds)
    }

    @Test
    fun `the zone offset is the one at the start`() = runBlocking<Unit> {
        clock.zoneId = ZoneId.of("Asia/Tokyo")

        assertEquals(9 * 3600, recorder.start(ITEM.id).zoneOffsetSeconds)
    }

    @Test
    fun `the answers are saved as they change`() = runBlocking<Unit> {
        val id = recorder.start(ITEM.id).id

        recorder.saveAnswers("""{"1":"α"}""")
        recorder.saveAnswers("""{"1":"α","2":"β"}""")

        assertEquals("""{"1":"α","2":"β"}""", session(id).answers)
    }

    @Test
    fun `the check of a trial finishes it with the real time even over the limit`() = runBlocking<Unit> {
        val id = recorder.start(ITEM.id).id
        clock.advance(Duration.ofMinutes(15))

        val check = recorder.check(ANSWERS, points = 4.0, maxPoints = 6.0)

        val end = START + Duration.ofMinutes(15)
        assertEquals(
            AnswerCheck(check.id, id, end.toEpochMilli(), ANSWERS, points = 4.0, maxPoints = 6.0),
            check,
        )
        assertEquals(listOf(check), database.sessionDao().checks(id))
        val session = session(id)
        assertEquals(SessionState.FINISHED, session.state)
        assertEquals(end.toEpochMilli(), session.endedAtMillis)
        assertEquals(15 * MINUTE, session.activeMillis)
        assertEquals(ANSWERS, session.answers)
        assertNull(recorder.openSession())
    }

    @Test
    fun `the timer of a trial runs off the screen and over the death of the process`() = runBlocking<Unit> {
        recorder.show()
        val id = recorder.start(ITEM.id).id
        clock.advance(Duration.ofMinutes(2))
        recorder.hide()
        clock.advance(Duration.ofMinutes(3))
        assertEquals(5 * MINUTE, recorder.activeMillis())

        val next = SessionRecorder(database, clock)
        clock.advance(Duration.ofMinutes(4))
        assertEquals(id, next.openSession()?.id)
        assertEquals(9 * MINUTE, next.activeMillis())
        next.check(ANSWERS, points = null, maxPoints = 6.0)

        assertEquals(9 * MINUTE, session(id).activeMillis)
    }

    @Test
    fun `a study session counts only the time with its item on the screen`() = runBlocking<Unit> {
        val id = studySession()
        recorder.show()
        clock.advance(Duration.ofMinutes(5))
        recorder.hide()
        clock.advance(Duration.ofMinutes(30))
        assertEquals(5 * MINUTE, recorder.activeMillis())
        recorder.show()
        clock.advance(Duration.ofMinutes(2))
        assertEquals(7 * MINUTE, recorder.activeMillis())

        recorder.finish()

        val session = session(id)
        assertEquals(SessionState.FINISHED, session.state)
        assertEquals((START + Duration.ofMinutes(37)).toEpochMilli(), session.endedAtMillis)
        assertEquals(7 * MINUTE, session.activeMillis)
    }

    @Test
    fun `a study session started on the screen counts from its start`() = runBlocking<Unit> {
        recorder.show()
        clock.advance(Duration.ofMinutes(10))
        val id = studySession()
        clock.advance(Duration.ofMinutes(4))

        recorder.finish()

        assertEquals(4 * MINUTE, session(id).activeMillis)
    }

    @Test
    fun `a study session after the death of the process keeps the time saved and waits for the screen`() = runBlocking<Unit> {
        val id = studySession()
        recorder.show()
        clock.advance(Duration.ofMinutes(3))
        recorder.saveAnswers(ANSWERS)
        clock.advance(Duration.ofMinutes(1))

        // The process dies: the minute since the save is lost.
        val next = SessionRecorder(database, clock)
        assertEquals(3 * MINUTE, next.activeMillis())
        clock.advance(Duration.ofMinutes(60))
        assertEquals(3 * MINUTE, next.activeMillis())
        next.show()
        clock.advance(Duration.ofMinutes(2))
        next.finish()

        assertEquals(5 * MINUTE, session(id).activeMillis)
        assertEquals(ANSWERS, session(id).answers)
    }

    @Test
    fun `the time of a study session is saved when its item leaves the screen`() = runBlocking<Unit> {
        val id = studySession()
        recorder.show()
        clock.advance(Duration.ofMinutes(6))

        recorder.hide()

        assertEquals(6 * MINUTE, session(id).activeMillis)
        assertEquals(SessionState.OPEN, session(id).state)
    }

    @Test
    fun `each check of a study session is recorded and the session stays open`() = runBlocking<Unit> {
        val id = studySession()
        recorder.show()
        clock.advance(Duration.ofMinutes(1))
        val first = recorder.check("""{"1":"α"}""", points = 2.0, maxPoints = 6.0)
        clock.advance(Duration.ofMinutes(1))
        val second = recorder.check(ANSWERS, points = 6.0, maxPoints = 6.0)

        assertEquals(listOf(first, second), database.sessionDao().checks(id))
        assertEquals(listOf(2.0, 6.0), listOf(first, second).map { it.points })
        val session = session(id)
        assertEquals(SessionState.OPEN, session.state)
        assertEquals(ANSWERS, session.answers)
        assertEquals(2 * MINUTE, session.activeMillis)
    }

    @Test
    fun `an abandoned trial ends with its time`() = runBlocking<Unit> {
        val id = recorder.start(ITEM.id).id
        clock.advance(Duration.ofMinutes(8))

        recorder.abandon()

        val session = session(id)
        assertEquals(SessionState.ABANDONED, session.state)
        assertEquals(8 * MINUTE, session.activeMillis)
        assertEquals(emptyList<AnswerCheck>(), database.sessionDao().checks(id))
        assertNull(recorder.openSession())
    }

    @Test
    fun `a call that does not fit the state fails and changes nothing`() = runBlocking<Unit> {
        assertFails { recorder.saveAnswers(ANSWERS) }
        assertFails { recorder.check(ANSWERS, points = 1.0, maxPoints = 6.0) }
        assertFails { recorder.finish() }
        assertFails { recorder.abandon() }
        assertFails { recorder.start("reading.true_false.999") }

        val trial = recorder.start(ITEM.id).id
        assertFails { recorder.start(OTHER_ITEM) }
        assertFails { recorder.finish() }
        recorder.abandon()
        studySession()
        assertFails { recorder.abandon() }

        assertEquals(SessionState.ABANDONED, session(trial).state)
        assertEquals(SessionKind.STUDY, recorder.openSession()?.kind)
    }

    /** Starts a study session on [ITEM] after a trial that took no time; returns its ID. */
    private suspend fun studySession(): Long {
        if (!database.sessionDao().hasTrial(ITEM.id)) {
            recorder.start(ITEM.id)
            recorder.check(ANSWERS, points = 6.0, maxPoints = 6.0)
        }
        return recorder.start(ITEM.id).id
    }

    private suspend fun session(id: Long): Session = database.sessionDao().session(id)!!

    private fun assertFails(call: suspend () -> Unit) {
        assertThrows(IllegalStateException::class.java) { runBlocking { call() } }
    }

    /** A clock that stands still until a test moves it. */
    private class TestClock(var instant: Instant, var zoneId: ZoneId) : Clock() {
        fun advance(duration: Duration) {
            instant += duration
        }

        override fun getZone(): ZoneId = zoneId

        override fun withZone(zone: ZoneId): Clock = TestClock(instant, zone)

        override fun instant(): Instant = instant
    }

    private companion object {
        /** 2026-10-24 09:00 UTC, 12:00 in Athens. */
        val START: Instant = Instant.ofEpochMilli(1_792_832_400_000)

        val ATHENS: ZoneId = ZoneId.of("Europe/Athens")

        const val MINUTE = 60_000L

        const val ANSWERS = """{"1":"α","2":"β","3":"γ"}"""

        const val OTHER_ITEM = "reading.true_false.002"

        val TASK_TYPE = CatalogTaskType(
            id = "reading.true_false",
            part = "reading",
            greek = "Σωστό ή λάθος",
            answerFormat = "choice",
            timeSeconds = 750,
            retired = false,
        )

        val ITEM = ContentItem(
            id = "reading.true_false.001",
            part = "reading",
            taskType = "reading.true_false",
            situation = "personal.details",
            topic = "personal",
            title = "First",
            retired = false,
            body = "{}",
        )
    }
}
