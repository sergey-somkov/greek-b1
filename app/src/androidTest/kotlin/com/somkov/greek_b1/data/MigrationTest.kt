package com.somkov.greek_b1.data

import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.platform.app.InstrumentationRegistry
import com.somkov.greek_b1.content.ContentItem
import com.somkov.greek_b1.session.AnswerCheck
import com.somkov.greek_b1.session.Session
import com.somkov.greek_b1.session.SessionKind
import com.somkov.greek_b1.session.SessionState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        val item = ContentItem(
            id = "reading.true_false.001",
            part = "reading",
            taskType = "reading.true_false",
            situation = "personal.details",
            topic = "personal",
            title = "Kept",
            retired = false,
            body = "{}",
        )
        val session = Session(
            id = 1,
            itemId = item.id,
            kind = SessionKind.TRIAL,
            state = SessionState.OPEN,
            timeLimitSeconds = 750,
            startedAtMillis = 1_792_832_400_000,
            zoneOffsetSeconds = 3 * 3600,
            endedAtMillis = null,
            activeMillis = 0,
            answers = """{"1":"α"}""",
        )
        val check = AnswerCheck(
            id = 1,
            sessionId = session.id,
            checkedAtMillis = 1_792_833_000_000,
            answers = """{"1":"α"}""",
            points = 1.0,
            maxPoints = 5.0,
        )
        AppDatabase.build(context, NAME).apply {
            contentDao().insertItems(listOf(item))
            sessionDao().insertSession(session)
            sessionDao().insertCheck(check)
            close()
        }
        AppDatabase.build(context, NAME).apply {
            assertEquals(item, contentDao().item(item.id))
            assertEquals(session, sessionDao().session(session.id))
            assertEquals(listOf(check), sessionDao().checks(session.id))
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
    fun `version 1 loses the placeholder table`() = runBlocking<Unit> {
        helper.createDatabase(1).apply {
            execSQL("INSERT INTO placeholder (id, text) VALUES (1, 'dropped')")
            close()
        }
        helper.runMigrationsAndValidate(AppDatabase.VERSION, MIGRATIONS).use { connection ->
            val query = "SELECT name FROM sqlite_master WHERE name = 'placeholder'"
            connection.prepare(query).use { statement ->
                assertFalse(statement.step())
            }
        }
    }

    @Test
    fun `version 2 keeps its content and gets the user tables`() = runBlocking<Unit> {
        helper.createDatabase(2).apply {
            execSQL(
                "INSERT INTO catalog_task_type (id, part, greek, answer_format, retired) " +
                    "VALUES ('reading.true_false', 'reading', 'Σωστό ή λάθος', 'choice', 0)",
            )
            close()
        }
        helper.runMigrationsAndValidate(AppDatabase.VERSION, MIGRATIONS).use { connection ->
            // The time of the task is 0 until the next content load writes it.
            val query = "SELECT id, time_seconds FROM catalog_task_type"
            connection.prepare(query).use { statement ->
                assertTrue(statement.step())
                assertEquals("reading.true_false", statement.getText(0))
                assertEquals(0L, statement.getLong(1))
            }
            connection.prepare("SELECT COUNT(*) FROM session").use { statement ->
                assertTrue(statement.step())
                assertEquals(0L, statement.getLong(0))
            }
        }
    }

    @Test
    fun `a database without a migration fails to open and keeps its data`() = runBlocking<Unit> {
        // A version the app has no migration from: a newer one, as after a downgrade.
        helper.createDatabase(AppDatabase.VERSION).apply {
            execSQL(
                "INSERT INTO catalog_topic (id, name, greek, retired) VALUES ('personal', 'kept', '', 0)",
            )
            execSQL("PRAGMA user_version = ${AppDatabase.VERSION + 1}")
            close()
        }
        val database = AppDatabase.build(context, NAME)
        assertThrows(IllegalStateException::class.java) {
            runBlocking { database.contentDao().item("reading.true_false.001") }
        }
        database.close()

        AndroidSQLiteDriver().open(file.path).use { connection ->
            val query = "SELECT name FROM catalog_topic WHERE id = 'personal'"
            connection.prepare(query).use { statement ->
                assertTrue(statement.step())
                assertEquals("kept", statement.getText(0))
            }
        }
    }

    private companion object {
        const val NAME = "migration-test.db"
    }
}
