package com.somkov.greek_b1.session

import androidx.sqlite.SQLiteException
import androidx.test.platform.app.InstrumentationRegistry
import com.somkov.greek_b1.data.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

/** The queries of the user tables, on the database as the app builds it and the phone's SQLite. */
class SessionDaoTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: AppDatabase
    private lateinit var dao: SessionDao

    @Before
    fun openDatabase() {
        context.deleteDatabase(NAME)
        database = AppDatabase.build(context, NAME)
        dao = database.sessionDao()
    }

    @After
    fun deleteDatabase() {
        database.close()
        context.deleteDatabase(NAME)
    }

    @Test
    fun `a session is read back as it was inserted`() = runBlocking<Unit> {
        val id = dao.insertSession(TRIAL)

        assertEquals(TRIAL.copy(id = id), dao.session(id))
    }

    @Test
    fun `an open session takes its answers and its active time`() = runBlocking<Unit> {
        val id = dao.insertSession(STUDY)

        assertEquals(1, dao.saveAnswers(id, """{"1":"α"}"""))
        assertEquals(1, dao.saveActiveTime(id, 5_000))

        assertEquals(STUDY.copy(id = id, answers = """{"1":"α"}""", activeMillis = 5_000), dao.session(id))
    }

    @Test
    fun `an ended session never changes`() = runBlocking<Unit> {
        val id = dao.insertSession(TRIAL)
        assertEquals(1, dao.end(id, SessionState.FINISHED, endedAtMillis = START + 600_000, activeMillis = 600_000))
        val ended = TRIAL.copy(
            id = id,
            state = SessionState.FINISHED,
            endedAtMillis = START + 600_000,
            activeMillis = 600_000,
        )
        assertEquals(ended, dao.session(id))

        assertEquals(0, dao.saveAnswers(id, """{"1":"β"}"""))
        assertEquals(0, dao.saveActiveTime(id, 700_000))
        assertEquals(0, dao.end(id, SessionState.ABANDONED, endedAtMillis = START + 700_000, activeMillis = 700_000))
        assertEquals(ended, dao.session(id))
    }

    @Test
    fun `the open session is the one not ended`() = runBlocking<Unit> {
        assertNull(dao.openSession())
        val trial = dao.insertSession(TRIAL)
        dao.end(trial, SessionState.ABANDONED, endedAtMillis = START + 1_000, activeMillis = 1_000)
        assertNull(dao.openSession())

        val study = dao.insertSession(STUDY)

        assertEquals(study, dao.openSession()?.id)
    }

    @Test
    fun `the checks of a session are read in their order`() = runBlocking<Unit> {
        val id = dao.insertSession(STUDY)
        val first = AnswerCheck(sessionId = id, checkedAtMillis = START + 1_000, answers = "{}", points = 2.5, maxPoints = 6.0)
        val second = first.copy(checkedAtMillis = START + 2_000, points = 6.0)
        val writing = first.copy(checkedAtMillis = START + 3_000, points = null)

        val ids = listOf(first, second, writing).map { dao.insertCheck(it) }

        assertEquals(
            listOf(first.copy(id = ids[0]), second.copy(id = ids[1]), writing.copy(id = ids[2])),
            dao.checks(id),
        )
        assertEquals(emptyList<AnswerCheck>(), dao.checks(id + 1))
    }

    @Test
    fun `a check needs its session`() = runBlocking<Unit> {
        val check = AnswerCheck(sessionId = 1, checkedAtMillis = START, answers = "{}", points = 0.0, maxPoints = 5.0)

        assertThrows(SQLiteException::class.java) {
            runBlocking { dao.insertCheck(check) }
        }
    }

    private companion object {
        const val NAME = "session-dao-test.db"

        /** 2026-10-24 09:00 UTC. */
        const val START = 1_792_832_400_000

        val TRIAL = Session(
            itemId = "reading.true_false.001",
            kind = SessionKind.TRIAL,
            state = SessionState.OPEN,
            timeLimitSeconds = 750,
            startedAtMillis = START,
            zoneOffsetSeconds = 3 * 3600,
            endedAtMillis = null,
            activeMillis = 0,
            answers = null,
        )

        val STUDY = TRIAL.copy(kind = SessionKind.STUDY, timeLimitSeconds = null)
    }
}
