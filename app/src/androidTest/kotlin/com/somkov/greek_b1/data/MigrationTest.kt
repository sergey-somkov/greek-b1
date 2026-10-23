package com.somkov.greek_b1.data

import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * The database as the app builds it ([AppDatabase.build]), on the phone's SQLite. The old
 * versions are created from the schemas exported to app/schemas/.
 *
 * The test runs on the phone, not on the JVM with Robolectric: it checks the real SQLite of the
 * phone, which holds the progress data, and Robolectric would be a large dependency.
 */
class MigrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val file = context.getDatabasePath(NAME)

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = instrumentation,
        file = file,
        driver = AndroidSQLiteDriver(),
        databaseClass = AppDatabase::class,
    )

    @Before
    fun deleteDatabase() {
        context.deleteDatabase(NAME)
    }

    @Test
    fun `a new database keeps its data after a reopen`() = runBlocking<Unit> {
        AppDatabase.build(context, NAME).apply {
            placeholderDao().insert(Placeholder(1, "kept"))
            close()
        }
        AppDatabase.build(context, NAME).apply {
            assertEquals(Placeholder(1, "kept"), placeholderDao().get(1))
            close()
        }
    }

    @Test
    fun `every version migrates to the current one`() = runBlocking<Unit> {
        for (version in 1..AppDatabase.VERSION) {
            context.deleteDatabase(NAME)
            helper.createDatabase(version).close()
            helper.runMigrationsAndValidate(AppDatabase.VERSION, MIGRATIONS).close()
        }
    }

    @Test
    fun `a database without a migration fails to open and keeps its data`() = runBlocking<Unit> {
        // A version the app has no migration from: a newer one, as after a downgrade.
        helper.createDatabase(AppDatabase.VERSION).apply {
            execSQL("INSERT INTO placeholder (id, text) VALUES (1, 'kept')")
            execSQL("PRAGMA user_version = ${AppDatabase.VERSION + 1}")
            close()
        }
        val database = AppDatabase.build(context, NAME)
        assertThrows(IllegalStateException::class.java) {
            runBlocking { database.placeholderDao().get(1) }
        }
        database.close()

        AndroidSQLiteDriver().open(file.path).use { connection ->
            connection.prepare("SELECT text FROM placeholder WHERE id = 1").use { statement ->
                assertTrue(statement.step())
                assertEquals("kept", statement.getText(0))
            }
        }
    }

    private companion object {
        const val NAME = "migration-test.db"
    }
}
