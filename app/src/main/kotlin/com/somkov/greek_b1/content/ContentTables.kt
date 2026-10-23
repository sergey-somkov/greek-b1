package com.somkov.greek_b1.content

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

/*
 * The content tables: the items and the catalog of content/, as the bundle in the assets has
 * them (tools/bundle.py). The app rewrites them from the bundle when its content version
 * changes, so they hold nothing of the user's. The user tables refer to their IDs, not to their
 * rows, and an item or an entry that is no longer needed stays here as retired.
 *
 * Only what the app filters, counts and lists by is a column; the rest of an item is the JSON
 * object `body`, so a change of the item format needs no migration. The catalog tables have the
 * IDs and the names; the rest of the catalog (the descriptors of the criteria, the points of the
 * task types) is added when a feature needs it. No foreign keys: the validator checks the
 * references before the build, and the tables are rewritten as a whole.
 *
 * [ContentLoader] rewrites them; [ContentVersion] says which bundle they hold.
 */

/** An item: one exam task, `content/items/<part>/<type>/<id>.yaml`. */
@Entity(tableName = "content_item")
data class ContentItem(
    /** `<task type>.<NNN>`, for example `reading.true_false.001`. */
    @PrimaryKey val id: String,
    /** The part of the exam: `listening`, `reading`, `writing` or `speaking`. */
    val part: String,
    @ColumnInfo(name = "task_type") val taskType: String,
    val situation: String,
    /** The topic of [situation]. */
    val topic: String,
    val title: String,
    /** A retired item is no longer offered, but it stays for the results that refer to it. */
    val retired: Boolean,
    /** The other fields of the item, a JSON object decoded by the screen of its part. */
    val body: String,
)

@Entity(tableName = "catalog_task_type")
data class CatalogTaskType(
    /** `<part>.<type>`, for example `reading.true_false`. */
    @PrimaryKey val id: String,
    val part: String,
    val greek: String,
    /** The ID of the answer format: `choice`, `assign`, `grid`, `short_text`, `text`, ... */
    @ColumnInfo(name = "answer_format") val answerFormat: String,
    val retired: Boolean,
)

@Entity(tableName = "catalog_topic")
data class CatalogTopic(
    @PrimaryKey val id: String,
    val name: String,
    val greek: String,
    val retired: Boolean,
)

@Entity(tableName = "catalog_situation")
data class CatalogSituation(
    /** `<topic>.<situation>`, for example `personal.details`. */
    @PrimaryKey val id: String,
    val topic: String,
    val name: String,
    val greek: String,
    val retired: Boolean,
)

@Entity(tableName = "catalog_function")
data class CatalogFunction(
    @PrimaryKey val id: String,
    val name: String,
    val greek: String,
    val retired: Boolean,
)

/** A criterion of the assessment; its ID is unique within its part only (`grammar`). */
@Entity(tableName = "catalog_criterion", primaryKeys = ["part", "id"])
data class CatalogCriterion(
    /** `writing` or `speaking`. */
    val part: String,
    val id: String,
    val name: String,
    val greek: String,
    val retired: Boolean,
)

/**
 * The content version of the bundle in the tables (content/version.txt of the assets): one row,
 * written in the same transaction as the content, so it never names content that is not there.
 */
@Entity(tableName = "content_version")
data class ContentVersion(
    @PrimaryKey val version: String,
)

/** The number of items of a part of the exam. */
data class PartCount(val part: String, val count: Int)

/** The queries of the content tables. It grows with the screens. */
@Dao
interface ContentDao {
    @Query("SELECT version FROM content_version")
    suspend fun version(): String?

    /** The loaded content version, or null before the first load; it changes when a load commits. */
    @Query("SELECT version FROM content_version")
    fun observeVersion(): Flow<String?>

    /** The items that the app offers (not retired), per part; a part without items is left out. */
    @Query("SELECT part, COUNT(*) AS count FROM content_item WHERE NOT retired GROUP BY part")
    fun observeItemsPerPart(): Flow<List<PartCount>>

    @Query("SELECT * FROM content_item ORDER BY id")
    suspend fun items(): List<ContentItem>

    @Query("SELECT * FROM content_item WHERE id = :id")
    suspend fun item(id: String): ContentItem?

    // The load: it deletes every row of the content tables and inserts the bundle's.

    @Query("DELETE FROM content_version")
    suspend fun deleteVersion()

    @Query("DELETE FROM content_item")
    suspend fun deleteItems()

    @Query("DELETE FROM catalog_task_type")
    suspend fun deleteTaskTypes()

    @Query("DELETE FROM catalog_topic")
    suspend fun deleteTopics()

    @Query("DELETE FROM catalog_situation")
    suspend fun deleteSituations()

    @Query("DELETE FROM catalog_function")
    suspend fun deleteFunctions()

    @Query("DELETE FROM catalog_criterion")
    suspend fun deleteCriteria()

    @Insert
    suspend fun insertVersion(version: ContentVersion)

    @Insert
    suspend fun insertItems(items: List<ContentItem>)

    @Insert
    suspend fun insertTaskTypes(taskTypes: List<CatalogTaskType>)

    @Insert
    suspend fun insertTopics(topics: List<CatalogTopic>)

    @Insert
    suspend fun insertSituations(situations: List<CatalogSituation>)

    @Insert
    suspend fun insertFunctions(functions: List<CatalogFunction>)

    @Insert
    suspend fun insertCriteria(criteria: List<CatalogCriterion>)
}
