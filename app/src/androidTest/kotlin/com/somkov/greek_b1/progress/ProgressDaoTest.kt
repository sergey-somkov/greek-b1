package com.somkov.greek_b1.progress

import androidx.test.platform.app.InstrumentationRegistry
import com.somkov.greek_b1.content.CatalogTopic
import com.somkov.greek_b1.content.ContentItem
import com.somkov.greek_b1.data.AppDatabase
import com.somkov.greek_b1.session.AnswerCheck
import com.somkov.greek_b1.session.Session
import com.somkov.greek_b1.session.SessionKind
import com.somkov.greek_b1.session.SessionState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** The queries of the progress screen, on a database in memory. */
class ProgressDaoTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: AppDatabase
    private lateinit var dao: ProgressDao

    @Before
    fun openDatabase() {
        database = AppDatabase.buildInMemory(context)
        dao = database.progressDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `the topics are in the order of the catalog without the retired ones`() = runBlocking<Unit> {
        val topics = listOf(topic("work"), topic("personal", retired = true), topic("home"))
        database.contentDao().insertTopics(topics)

        assertEquals(listOf("work", "home"), dao.observeTopics().first().map { it.id })
    }

    @Test
    fun `the trials have their item and their first check and the study sessions are left out`() = runBlocking<Unit> {
        database.contentDao().insertItems(listOf(ITEM))
        val sessions = database.sessionDao()
        val finished = sessions.insertSession(TRIAL.copy(state = SessionState.FINISHED, activeMillis = 600_000))
        sessions.insertCheck(AnswerCheck(sessionId = finished, checkedAtMillis = START, answers = "{}", points = 4.0, maxPoints = 6.0))
        sessions.insertCheck(AnswerCheck(sessionId = finished, checkedAtMillis = START + 1, answers = "{}", points = 6.0, maxPoints = 6.0))
        val abandoned = sessions.insertSession(TRIAL.copy(state = SessionState.ABANDONED, activeMillis = 60_000))
        sessions.insertSession(TRIAL.copy(kind = SessionKind.STUDY, timeLimitSeconds = null))

        val expected = listOf(
            TrialRow(
                Trial(
                    sessionId = finished,
                    itemId = ITEM.id,
                    topic = "home",
                    part = "reading",
                    state = SessionState.FINISHED,
                    startedAtMillis = START,
                    timeLimitSeconds = 750,
                    activeMillis = 600_000,
                    points = 4.0,
                    maxPoints = 6.0,
                ),
                itemTitle = "Home",
            ),
            TrialRow(
                Trial(
                    sessionId = abandoned,
                    itemId = ITEM.id,
                    topic = "home",
                    part = "reading",
                    state = SessionState.ABANDONED,
                    startedAtMillis = START,
                    timeLimitSeconds = 750,
                    activeMillis = 60_000,
                    points = null,
                    maxPoints = null,
                ),
                itemTitle = "Home",
            ),
        )
        assertEquals(expected, dao.observeTrials().first().sortedBy { it.trial.sessionId })
        assertEquals(3, dao.observeSessions().first().size)
    }

    private fun topic(id: String, retired: Boolean = false) = CatalogTopic(id, name = id, greek = id, retired = retired)

    private companion object {
        const val START = 1_792_832_400_000

        val ITEM = ContentItem(
            id = "reading.true_false.001",
            part = "reading",
            taskType = "reading.true_false",
            situation = "home.housing",
            topic = "home",
            title = "Home",
            retired = false,
            body = "{}",
        )

        val TRIAL = Session(
            itemId = ITEM.id,
            kind = SessionKind.TRIAL,
            state = SessionState.OPEN,
            timeLimitSeconds = 750,
            startedAtMillis = START,
            zoneOffsetSeconds = 2 * 3600,
            endedAtMillis = null,
            activeMillis = 0,
            answers = null,
        )
    }
}
