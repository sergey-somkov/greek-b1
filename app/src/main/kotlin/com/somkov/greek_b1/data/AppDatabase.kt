package com.somkov.greek_b1.data

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver

/**
 * The app's database: the content tables (story 4) and the user tables with the progress data
 * (story 5 on). It is never dropped; see [MIGRATIONS] for the rules of a schema change.
 *
 * Room 3 (`androidx.room3`), the latest stable line, rather than Room 2: KSP only, suspending
 * DAOs and a `SQLiteDriver`. Starting on Room 2 would mean a migration of every DAO later.
 */
@Database(entities = [Placeholder::class], version = AppDatabase.VERSION)
abstract class AppDatabase : RoomDatabase() {
    abstract fun placeholderDao(): PlaceholderDao

    companion object {
        /** The version of the schema. Raised with every change of the schema, see [MIGRATIONS]. */
        const val VERSION = 1

        const val FILE_NAME = "greek-b1.db"

        /**
         * Builds the database without opening it; Room opens the file on the first query. The
         * tests build theirs here too, under another [name], so they test the app's configuration.
         *
         * The driver is the phone's own SQLite (the framework's): no native library in the APK.
         * There is no destructive fallback: without a migration, opening the database fails.
         */
        fun build(context: Context, name: String = FILE_NAME): AppDatabase =
            Room.databaseBuilder<AppDatabase>(context, name)
                .setDriver(AndroidSQLiteDriver())
                .addMigrations(*MIGRATIONS.toTypedArray())
                .build()
    }
}
