package com.somkov.greek_b1.content

import android.content.res.AssetManager
import androidx.room3.withWriteTransaction
import com.somkov.greek_b1.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads the content of the APK into the content tables. The app runs it at each start
 * ([com.somkov.greek_b1.GreekB1Application]); the tests give it bundles of their own.
 *
 * The content version is compared with the one in the tables, and the bundle is read only when
 * they differ: then every content table is emptied and filled from the bundle, with the new
 * version, in one transaction. A load that fails rolls the transaction back, so the tables keep
 * the old content and its version, and the next start tries again. Only the content tables are
 * written; the user tables, with the progress data, refer to the content by ID and are not
 * touched.
 */
class ContentLoader(
    private val database: AppDatabase,
    /** The content version of the bundle. */
    private val readVersion: () -> String,
    /** The JSON of the bundle, read only when its version is not the loaded one. */
    private val readBundle: () -> String,
) {
    /**
     * Loads the bundle if its version is not the loaded one, and returns whether it did. One
     * load at a time: the version is compared outside the transaction.
     */
    suspend fun load(): Boolean = withContext(Dispatchers.IO) {
        val dao = database.contentDao()
        val version = readVersion()
        if (dao.version() == version) return@withContext false
        // Decoded before the transaction, which then holds the database only for the writes.
        val rows = decodeBundle(readBundle())
        database.withWriteTransaction {
            dao.deleteVersion()
            dao.deleteItems()
            dao.deleteTaskTypes()
            dao.deleteTopics()
            dao.deleteSituations()
            dao.deleteFunctions()
            dao.deleteCriteria()
            dao.insertItems(rows.items)
            dao.insertTaskTypes(rows.taskTypes)
            dao.insertTopics(rows.topics)
            dao.insertSituations(rows.situations)
            dao.insertFunctions(rows.functions)
            dao.insertCriteria(rows.criteria)
            dao.insertVersion(ContentVersion(version))
        }
        true
    }

    companion object {
        /** The paths in the assets that tools/bundle.py writes. */
        const val BUNDLE = "content/bundle.json"
        const val VERSION = "content/version.txt"

        /** The loader of the bundle in the APK's assets. */
        fun fromAssets(database: AppDatabase, assets: AssetManager) = ContentLoader(
            database,
            readVersion = { assets.open(VERSION).bufferedReader().use { it.readText().trim() } },
            readBundle = { assets.open(BUNDLE).bufferedReader().use { it.readText() } },
        )
    }
}
