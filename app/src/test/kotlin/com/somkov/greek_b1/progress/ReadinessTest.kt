package com.somkov.greek_b1.progress

import com.somkov.greek_b1.session.SessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadinessTest {
    private val topics = listOf("personal", "home", "work")

    private var nextId = 1L

    /** A trial of a reading task (12.5 min) started after the ones made before it, finished in time by default. */
    private fun trial(
        topic: String = "home",
        part: String = "reading",
        points: Double? = 8.0,
        maxPoints: Double? = 10.0,
        state: SessionState = SessionState.FINISHED,
        minutes: Double = 10.0,
    ): Trial {
        val id = nextId++
        return Trial(
            sessionId = id,
            itemId = "$part.item.%03d".format(id),
            topic = topic,
            part = part,
            state = state,
            startedAtMillis = 1_700_000_000_000 + id * 3_600_000,
            timeLimitSeconds = 750,
            activeMillis = (minutes * 60_000).toLong(),
            points = points,
            maxPoints = maxPoints,
        )
    }

    private fun assertReadiness(expected: Double?, actual: Double?) {
        if (expected == null) assertNull(actual) else assertEquals(expected, actual!!, 1e-9)
    }

    @Test
    fun `a trial in time shows its points`() {
        assertReadiness(0.8, trial(minutes = 12.5).readiness)
        assertReadiness(0.8, trial(minutes = 1.0).readiness)
    }

    @Test
    fun `a trial over its time shrinks in proportion`() {
        // 12.5 min allowed, 15 taken: 80% becomes 67%.
        assertReadiness(0.8 * 12.5 / 15, trial(minutes = 15.0).readiness)
        assertReadiness(0.4, trial(minutes = 25.0).readiness)
    }

    @Test
    fun `an abandoned trial is 0`() {
        assertReadiness(0.0, trial(state = SessionState.ABANDONED, points = null, maxPoints = null).readiness)
    }

    @Test
    fun `an open trial and a trial without points are not known`() {
        assertReadiness(null, trial(state = SessionState.OPEN, points = null, maxPoints = null, minutes = 0.0).readiness)
        assertReadiness(null, trial(part = "writing", points = null, maxPoints = 25.0).readiness)
    }

    @Test
    fun `a cell without a trial is gray`() {
        val cell = readiness(topics, emptyList()).cell("home", "reading")
        assertNull(cell.readiness)
        assertTrue(cell.trials.isEmpty())
    }

    @Test
    fun `a cell shows its last trial, not its best`() {
        val best = trial(points = 10.0)
        val last = trial(points = 5.0)
        val cell = readiness(topics, listOf(last, best)).cell("home", "reading")
        assertReadiness(0.5, cell.readiness)
        assertEquals(listOf(last, best), cell.trials)
    }

    @Test
    fun `an abandoned last trial makes its cell 0`() {
        val trials = listOf(trial(points = 9.0), trial(state = SessionState.ABANDONED, points = null, maxPoints = null))
        assertReadiness(0.0, readiness(topics, trials).cell("home", "reading").readiness)
    }

    @Test
    fun `an open last trial and one without points leave the cell at the last known result`() {
        val trials = listOf(
            trial(part = "writing", points = 15.0, maxPoints = 25.0),
            trial(part = "writing", points = null, maxPoints = 25.0),
            trial(part = "writing", state = SessionState.OPEN, points = null, maxPoints = null, minutes = 0.0),
        )
        val cell = readiness(topics, trials).cell("home", "writing")
        assertReadiness(0.6, cell.readiness)
        assertEquals(3, cell.trials.size)
    }

    @Test
    fun `the trials of other topics and parts are left out`() {
        val result = readiness(topics, listOf(trial(topic = "retired_topic"), trial(part = "retired_part")))
        assertEquals(topics.size * PARTS_IN_LEARNING_ORDER.size, result.cells.size)
        assertTrue(result.cells.all { it.readiness == null && it.trials.isEmpty() })
    }

    @Test
    fun `the cells are by topic, then by part in the order of learning`() {
        val cells = readiness(topics, emptyList()).cells
        assertEquals(topics.flatMap { topic -> PARTS_IN_LEARNING_ORDER.map { topic to it } }, cells.map { it.topic to it.part })
        assertEquals(listOf("reading", "listening", "writing", "speaking"), PARTS_IN_LEARNING_ORDER)
    }

    @Test
    fun `a part is the mean of its cells, a cell without a result counting as 0`() {
        val trials = listOf(trial(topic = "personal", points = 9.0), trial(topic = "home", points = 6.0))
        assertReadiness((0.9 + 0.6 + 0.0) / 3, readiness(topics, trials).parts["reading"])
    }

    @Test
    fun `a part without any result is gray, one with an abandoned trial is 0`() {
        val parts = readiness(topics, listOf(trial(part = "listening", state = SessionState.ABANDONED))).parts
        assertReadiness(null, parts["reading"])
        assertReadiness(0.0, parts["listening"])
        assertReadiness(null, parts["writing"])
        assertReadiness(null, parts["speaking"])
    }

    @Test
    fun `reading is always advised`() {
        assertTrue(readiness(topics, emptyList()).cells.filter { it.part == "reading" }.all { it.advised })
    }

    @Test
    fun `a part is advised once the previous part of its topic is at the pass mark`() {
        val result = readiness(
            topics,
            listOf(
                trial(topic = "personal", points = 6.0),
                trial(topic = "home", points = 5.9),
                trial(topic = "work", points = 6.0, minutes = 13.0),
            ),
        )
        assertTrue(result.cell("personal", "listening").advised)
        assertFalse(result.cell("home", "listening").advised)
        // 60% in points, but over the time of the task.
        assertFalse(result.cell("work", "listening").advised)
    }

    @Test
    fun `a part is not advised while the previous part has no result`() {
        val result = readiness(topics, listOf(trial(part = "listening", points = 10.0)))
        assertFalse(result.cell("home", "listening").advised)
        assertTrue(result.cell("home", "writing").advised)
        // Writing has no points until the assessment, so speaking is not advised yet.
        assertFalse(result.cell("home", "speaking").advised)
    }

    @Test
    fun `the chain follows the last trial`() {
        val trials = listOf(trial(points = 10.0), trial(points = 3.0))
        assertFalse(readiness(topics, trials).cell("home", "listening").advised)
    }
}
