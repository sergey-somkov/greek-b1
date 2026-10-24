package com.somkov.greek_b1.progress

import com.somkov.greek_b1.session.SessionState

/*
 * The readiness for the exam: a cell per topic and part, from the trials only. The repeats of an
 * item (the study sessions) do not count: they show that the item was learned, not the topic.
 * Pure functions over the trials; the progress screen reads them from the user tables.
 */

/**
 * The parts of the exam in the order of learning: per topic, a part is advised once the previous
 * one is at the pass mark. What one cannot read, one cannot hear, write or say.
 */
val PARTS_IN_LEARNING_ORDER = listOf("reading", "listening", "writing", "speaking")

/** The pass mark of a part of the exam, and the readiness at which a cell advises the next part. */
const val PASS_MARK = 0.6

/** A trial with what its readiness needs: its session, the check that ended it, and its item's topic and part. */
data class Trial(
    /** The ID of the session; the later trial of a cell has the greater ID. */
    val sessionId: Long,
    val itemId: String,
    val topic: String,
    val part: String,
    val state: SessionState,
    val startedAtMillis: Long,
    /** The time of the task, copied into the session at its start. */
    val timeLimitSeconds: Int,
    /** The real time of the trial; 0 while it is open. */
    val activeMillis: Long,
    /** The points of its check: null without a check (an open or abandoned trial) or until the assessment gives them. */
    val points: Double?,
    /** The points of a full answer; null without a check. */
    val maxPoints: Double?,
) {
    /**
     * The readiness that the trial shows, 0..1: its points%, reduced in proportion to the time
     * over the time of the task (12.5 min allowed, 15 taken: 80% becomes 67%); a trial faster
     * than the time of the task earns nothing more. An abandoned trial is 0, so that abandoning
     * a bad trial never improves the readiness. Null while it is not known: an open trial, or one
     * whose points the assessment has not given yet.
     */
    val readiness: Double?
        get() = when (state) {
            SessionState.OPEN -> null
            SessionState.ABANDONED -> 0.0
            SessionState.FINISHED -> {
                if (points == null || maxPoints == null || maxPoints <= 0) {
                    null
                } else {
                    val limitMillis = timeLimitSeconds * 1000L
                    val timeFactor = if (activeMillis <= limitMillis) 1.0 else limitMillis.toDouble() / activeMillis
                    (points / maxPoints * timeFactor).coerceIn(0.0, 1.0)
                }
            }
        }
}

/** A cell of the heat map: a topic in a part of the exam. */
data class Cell(
    val topic: String,
    val part: String,
    /**
     * The readiness of the last trial of the cell whose result is known, 0..1; null (gray)
     * without one. The last trial, not the best: it is the latest measure of the topic.
     */
    val readiness: Double?,
    /**
     * Whether the app advises the cell: a reading cell always, any other one once the previous
     * part of its topic is at [PASS_MARK]. An advice, not a lock: any item can be opened.
     */
    val advised: Boolean,
    /** The trials of the cell, the latest first, the open one and those without points included. */
    val trials: List<Trial>,
)

/** The readiness for the exam: the cells of the heat map and the readiness of each part. */
data class Readiness(
    /** A cell per topic and part: by topic in the order given, then by part in [PARTS_IN_LEARNING_ORDER]. */
    val cells: List<Cell>,
    /**
     * The readiness of each part by its ID: the mean of its cells, all topics weighing the same,
     * a cell without a result counting as 0. Null while no cell of the part has a result.
     */
    val parts: Map<String, Double?>,
) {
    fun cell(topic: String, part: String): Cell = cells.single { it.topic == topic && it.part == part }
}

/**
 * The readiness of each cell of [topics] (the IDs of the topics that the app offers, in the
 * order of the catalog) from [trials]. The trials of other topics or parts (retired ones) are
 * left out.
 */
fun readiness(topics: List<String>, trials: List<Trial>): Readiness {
    val trialsOfCell = trials.sortedByDescending { it.sessionId }.groupBy { it.topic to it.part }
    val cells = topics.flatMap { topic ->
        var previous: Double? = null
        PARTS_IN_LEARNING_ORDER.mapIndexed { index, part ->
            val cellTrials = trialsOfCell[topic to part].orEmpty()
            val readiness = cellTrials.firstNotNullOfOrNull { it.readiness }
            val advised = index == 0 || (previous ?: 0.0) >= PASS_MARK
            previous = readiness
            Cell(topic, part, readiness, advised, cellTrials)
        }
    }
    val parts = PARTS_IN_LEARNING_ORDER.associateWith { part ->
        val partCells = cells.filter { it.part == part }
        if (partCells.all { it.readiness == null }) null else partCells.sumOf { it.readiness ?: 0.0 } / partCells.size
    }
    return Readiness(cells, parts)
}
