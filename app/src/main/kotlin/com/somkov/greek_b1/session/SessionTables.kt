package com.somkov.greek_b1.session

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query

/*
 * The user tables: the sessions on the content items and the checks of their answers. They hold
 * the user's progress, which exists only on the phone (until the backup of story 18). The rules:
 * - They are never rewritten. The content load does not touch them, and no code deletes their
 *   rows. A session changes only while it is open (its answers, its active time, its end): the
 *   updates of [SessionDao] match open sessions only. A check is only inserted.
 * - They refer to the content by its stable IDs, not by its rows: an item that is no longer
 *   offered stays in content_item as retired, so its ID still finds it. No foreign keys to the
 *   content tables, which the load rewrites as a whole. The part, the topic and the task type of
 *   a session come from its item.
 * - What the result of a session depends on and the catalog may change later is copied into the
 *   session: the time of the task of a trial.
 * - The times are UTC, in milliseconds since the epoch. A session keeps the zone offset of the
 *   phone at its start, so its day (which ends at 3:00 local time) stays the same after a trip
 *   to another time zone.
 * - The kinds and the states are stored by the names of their constants: renaming one is a
 *   change of the data and needs a migration, as a change of the schema does (see MIGRATIONS).
 */

enum class SessionKind {
    /** The first sight of a new item, under exam conditions; its timer runs on the clock. */
    TRIAL,

    /** Any work on an item after its trial; its timer counts the time with the item on the screen. */
    STUDY,
}

enum class SessionState {
    /** In progress, or interrupted and resumed at the next start of the app. */
    OPEN,

    /** Ended by the user: a trial when its answers are checked, a study session on the return to the list. */
    FINISHED,

    /** A trial that the user gave up; it counts as 0% for the readiness of its cell. */
    ABANDONED,
}

/** A session: the work on one content item, from its start to its end. */
@Entity(tableName = "session", indices = [Index("item_id"), Index("started_at_millis")])
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** The ID of the item in content_item. */
    @ColumnInfo(name = "item_id") val itemId: String,
    val kind: SessionKind,
    val state: SessionState,
    /**
     * The time of the task of a trial, from catalog_task_type at its start; null for a study
     * session. Copied here so that a later change of the catalog keeps the readiness of the past
     * trials, which were taken against the old time.
     */
    @ColumnInfo(name = "time_limit_seconds") val timeLimitSeconds: Int?,
    @ColumnInfo(name = "started_at_millis") val startedAtMillis: Long,
    /** The offset of the phone's time zone from UTC at the start, in seconds. */
    @ColumnInfo(name = "zone_offset_seconds") val zoneOffsetSeconds: Int,
    /** Null while the session is open. */
    @ColumnInfo(name = "ended_at_millis") val endedAtMillis: Long?,
    /**
     * The time of work on the item. For a trial, the time on the clock from its start to its end,
     * written at the end. For a study session, the time with the item on the screen, saved as it
     * grows, so the death of the process loses only the time since the last save.
     */
    @ColumnInfo(name = "active_millis") val activeMillis: Long,
    /**
     * The current answers, saved as they change: a JSON object in the recorded answer of the
     * answer format of the task type (content/catalog/task_types.yaml), written and read by the
     * screen of the part. Null before the first answer.
     */
    val answers: String?,
)

/**
 * A check of the answers of a session: the one at the end of a trial, and each one that the user
 * asks for in a study session. The review of mistakes (story 16) reads the mistakes from them.
 */
@Entity(
    tableName = "answer_check",
    foreignKeys = [ForeignKey(entity = Session::class, parentColumns = ["id"], childColumns = ["session_id"])],
    indices = [Index("session_id")],
)
data class AnswerCheck(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "checked_at_millis") val checkedAtMillis: Long,
    /** The answers as they were checked, in the format of [Session.answers]. */
    val answers: String,
    /**
     * The points of the answers; null while they are not known. The app scores reading and
     * listening itself; writing and speaking get their points from the LLM assessment (story 9).
     */
    val points: Double?,
    /** The points of a full answer, so that the percentage needs no catalog. */
    @ColumnInfo(name = "max_points") val maxPoints: Double,
)

/** The queries of the user tables. It grows with the screens. */
@Dao
interface SessionDao {
    /** Returns the ID of the new session. */
    @Insert
    suspend fun insertSession(session: Session): Long

    @Query("SELECT * FROM session WHERE id = :id")
    suspend fun session(id: Long): Session?

    /** Whether the item has had its trial, finished or abandoned or still open. */
    @Query("SELECT EXISTS (SELECT 1 FROM session WHERE item_id = :itemId AND kind = 'TRIAL')")
    suspend fun hasTrial(itemId: String): Boolean

    /** The open session, which the app resumes at its start; [SessionRecorder] keeps at most one. */
    @Query("SELECT * FROM session WHERE state = 'OPEN' ORDER BY id DESC LIMIT 1")
    suspend fun openSession(): Session?

    // The updates of an open session; each returns the number of rows it changed, 0 when the
    // session is not open.

    @Query("UPDATE session SET answers = :answers WHERE id = :id AND state = 'OPEN'")
    suspend fun saveAnswers(id: Long, answers: String): Int

    @Query("UPDATE session SET active_millis = :activeMillis WHERE id = :id AND state = 'OPEN'")
    suspend fun saveActiveTime(id: Long, activeMillis: Long): Int

    /** Ends an open session as [state], FINISHED or ABANDONED. */
    @Query(
        """
        UPDATE session SET state = :state, ended_at_millis = :endedAtMillis, active_millis = :activeMillis
        WHERE id = :id AND state = 'OPEN'
        """,
    )
    suspend fun end(id: Long, state: SessionState, endedAtMillis: Long, activeMillis: Long): Int

    /** Returns the ID of the new check. */
    @Insert
    suspend fun insertCheck(check: AnswerCheck): Long

    /** The checks of a session, in the order they were made. */
    @Query("SELECT * FROM answer_check WHERE session_id = :sessionId ORDER BY id")
    suspend fun checks(sessionId: Long): List<AnswerCheck>
}
