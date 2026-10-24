package com.somkov.greek_b1.data

import android.content.Context
import androidx.room3.AutoMigration
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.somkov.greek_b1.content.CatalogCriterion
import com.somkov.greek_b1.content.CatalogFunction
import com.somkov.greek_b1.content.CatalogSituation
import com.somkov.greek_b1.content.CatalogTaskType
import com.somkov.greek_b1.content.CatalogTopic
import com.somkov.greek_b1.content.ContentDao
import com.somkov.greek_b1.content.ContentItem
import com.somkov.greek_b1.content.ContentVersion
import com.somkov.greek_b1.progress.ProgressDao
import com.somkov.greek_b1.session.AnswerCheck
import com.somkov.greek_b1.session.Session
import com.somkov.greek_b1.session.SessionDao

/**
 * The app's database: the content tables (story 4) and the user tables with the progress data
 * (story 5 on). It is never dropped; see [MIGRATIONS] for the rules of a schema change.
 *
 * Room 3 (`androidx.room3`), the latest stable line, rather than Room 2: KSP only, suspending
 * DAOs and a `SQLiteDriver`. Starting on Room 2 would mean a migration of every DAO later.
 */
@Database(
    entities = [
        ContentItem::class,
        CatalogTaskType::class,
        CatalogTopic::class,
        CatalogSituation::class,
        CatalogFunction::class,
        CatalogCriterion::class,
        ContentVersion::class,
        Session::class,
        AnswerCheck::class,
    ],
    version = AppDatabase.VERSION,
    autoMigrations = [
        AutoMigration(from = 1, to = 2, spec = DropPlaceholder::class),
        // The time of a task, a new column of catalog_task_type; the user tables.
        AutoMigration(from = 2, to = 3),
    ],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contentDao(): ContentDao

    abstract fun sessionDao(): SessionDao

    abstract fun progressDao(): ProgressDao

    companion object {
        /** The version of the schema. Raised with every change of the schema, see [MIGRATIONS]. */
        const val VERSION = 3

        const val FILE_NAME = "greek-b1.db"

        /**
         * Builds the database without opening it; Room opens the file on the first query. The
         * tests build theirs here too, under another [name] or with [buildInMemory], so they test
         * the app's configuration.
         *
         * The driver is the phone's own SQLite (the framework's): no native library in the APK.
         * There is no destructive fallback: without a migration, opening the database fails.
         */
        fun build(context: Context, name: String = FILE_NAME): AppDatabase =
            configure(Room.databaseBuilder<AppDatabase>(context, name))

        /** Builds a database in memory, for the tests that need no file; it is gone when closed. */
        fun buildInMemory(context: Context): AppDatabase = configure(Room.inMemoryDatabaseBuilder<AppDatabase>(context))

        private fun configure(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase =
            builder
                .setDriver(AndroidSQLiteDriver())
                .addMigrations(*MIGRATIONS.toTypedArray())
                .build()
    }
}
