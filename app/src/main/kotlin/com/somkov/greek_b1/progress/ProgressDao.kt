package com.somkov.greek_b1.progress

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Embedded
import androidx.room3.Query
import com.somkov.greek_b1.content.CatalogTopic
import com.somkov.greek_b1.session.Session
import kotlinx.coroutines.flow.Flow

/** A trial with the title of its item, which the trials of a cell show. */
data class TrialRow(
    @Embedded val trial: Trial,
    @ColumnInfo(name = "item_title") val itemTitle: String,
)

/**
 * The queries of the progress screen: the user tables with what they need of the content tables.
 * Each one is a flow, which follows the writes of the sessions and the loads of the content.
 */
@Dao
interface ProgressDao {
    /**
     * The topics that the app offers, in the order of the catalog. The load inserts them in that
     * order into an emptied table, so the order of their rowids is the order of the catalog.
     */
    @Query("SELECT * FROM catalog_topic WHERE NOT retired ORDER BY rowid")
    fun observeTopics(): Flow<List<CatalogTopic>>

    /**
     * The trials with the topic and the part of their items and the points of their checks. A
     * trial has one check; if it ever had more, the first one ends it.
     */
    @Query(
        """
        SELECT s.id AS sessionId, s.item_id AS itemId, i.topic, i.part, s.state,
            s.started_at_millis AS startedAtMillis, s.time_limit_seconds AS timeLimitSeconds,
            s.active_millis AS activeMillis, c.points, c.max_points AS maxPoints, i.title AS item_title
        FROM session s
        JOIN content_item i ON i.id = s.item_id
        LEFT JOIN answer_check c ON c.id = (SELECT MIN(id) FROM answer_check WHERE session_id = s.id)
        WHERE s.kind = 'TRIAL'
        """,
    )
    fun observeTrials(): Flow<List<TrialRow>>

    /** Every session, for the time per day. */
    @Query("SELECT * FROM session")
    fun observeSessions(): Flow<List<Session>>
}
