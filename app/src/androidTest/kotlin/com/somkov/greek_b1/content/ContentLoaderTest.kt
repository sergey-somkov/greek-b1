package com.somkov.greek_b1.content

import androidx.room3.useReaderConnection
import androidx.test.platform.app.InstrumentationRegistry
import com.somkov.greek_b1.data.AppDatabase
import com.somkov.greek_b1.session.AnswerCheck
import com.somkov.greek_b1.session.Session
import com.somkov.greek_b1.session.SessionKind
import com.somkov.greek_b1.session.SessionState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The load of the content into the database as the app builds it, on the phone's SQLite: small
 * bundles written here, and the bundle of the APK's assets.
 */
class ContentLoaderTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: AppDatabase

    @Before
    fun openDatabase() {
        context.deleteDatabase(NAME)
        database = AppDatabase.build(context, NAME)
    }

    @After
    fun deleteDatabase() {
        database.close()
        context.deleteDatabase(NAME)
    }

    @Test
    fun `the first load fills the empty tables`() = runBlocking<Unit> {
        assertTrue(loader("1", FIRST).load())

        assertEquals("1", database.contentDao().version())
        assertEquals(
            listOf(
                "reading.true_false.001|reading|reading.true_false|personal.details|personal|First|0|{\"text\":\"Κείμενο\"}",
                "reading.true_false.002|reading|reading.true_false|personal.details|personal|Second|0|{\"text\":\"Κείμενο\"}",
            ),
            rows("content_item"),
        )
        assertEquals(listOf("reading.true_false|reading|Σωστό ή λάθος|choice|750|0"), rows("catalog_task_type"))
        assertEquals(listOf("personal|Personal identification|Προσωπικά στοιχεία|0"), rows("catalog_topic"))
        assertEquals(
            listOf("personal.details|personal|Personal details|Προσωπικά στοιχεία|0"),
            rows("catalog_situation"),
        )
        assertEquals(listOf("give_information|Give information|Δίνω πληροφορίες|0"), rows("catalog_function"))
        assertEquals(
            listOf("speaking|grammar|Grammar|Γραμματική|0", "writing|grammar|Grammar|Γραμματική|0"),
            rows("catalog_criterion"),
        )
    }

    @Test
    fun `a new version replaces the content`() = runBlocking<Unit> {
        loader("1", FIRST).load()
        val second = bundle(
            topic = "Personal identification and family",
            "reading.true_false.001" to "First, corrected",
            "reading.true_false.003" to "Third",
        )

        assertTrue(loader("2", second).load())

        assertEquals("2", database.contentDao().version())
        assertEquals(
            listOf("reading.true_false.001|First, corrected", "reading.true_false.003|Third"),
            database.contentDao().items().map { "${it.id}|${it.title}" },
        )
        assertEquals(listOf("personal|Personal identification and family|Προσωπικά στοιχεία|0"), rows("catalog_topic"))
        // The other tables hold the rows of the second bundle only, not two copies.
        assertEquals(1, rows("catalog_situation").size)
        assertEquals(2, rows("catalog_criterion").size)
    }

    @Test
    fun `a new version keeps the user tables`() = runBlocking<Unit> {
        loader("1", FIRST).load()
        val dao = database.sessionDao()
        val sessionId = dao.insertSession(
            Session(
                itemId = "reading.true_false.001",
                kind = SessionKind.TRIAL,
                state = SessionState.FINISHED,
                timeLimitSeconds = 750,
                startedAtMillis = 1_792_832_400_000,
                zoneOffsetSeconds = 3 * 3600,
                endedAtMillis = 1_792_833_000_000,
                activeMillis = 600_000,
                answers = "{}",
            ),
        )
        dao.insertCheck(
            AnswerCheck(sessionId = sessionId, checkedAtMillis = 1_792_833_000_000, answers = "{}", points = 3.0, maxPoints = 5.0),
        )
        val before = USER_TABLES.map { rows(it) }

        assertTrue(loader("2", bundle(topic = "Changed", "reading.true_false.001" to "First, corrected")).load())

        assertEquals(before, USER_TABLES.map { rows(it) })
    }

    @Test
    fun `the same version is not loaded again`() = runBlocking<Unit> {
        loader("1", FIRST).load()
        val before = rows("content_item")

        val again = ContentLoader(database, readVersion = { "1" }, readBundle = { error("the bundle was read") })
        assertFalse(again.load())

        assertEquals(before, rows("content_item"))
    }

    @Test
    fun `a failed load keeps the old content`() = runBlocking<Unit> {
        loader("1", FIRST).load()
        val before = TABLES.map { rows(it) }
        // Two items with one ID: the second insert of the items fails, after the deletes.
        val broken = bundle(
            topic = "Changed",
            "reading.true_false.003" to "Third",
            "reading.true_false.003" to "Third again",
        )

        assertThrows(Exception::class.java) {
            runBlocking { loader("2", broken).load() }
        }

        assertEquals("1", database.contentDao().version())
        assertEquals(before, TABLES.map { rows(it) })
    }

    @Test
    fun `the bundle of the APK loads`() = runBlocking<Unit> {
        assertTrue(ContentLoader.fromAssets(database, context.assets).load())

        val version = context.assets.open(ContentLoader.VERSION).bufferedReader().use { it.readText().trim() }
        assertEquals(version, database.contentDao().version())
        for (table in TABLES) {
            assertNotEquals("$table is empty", 0, rows(table).size)
        }
    }

    @Test
    fun `the diagnostics see the version and the items per part`() = runBlocking<Unit> {
        val dao = database.contentDao()
        assertEquals(null, dao.observeVersion().first())
        assertEquals(emptyList<PartCount>(), dao.observeItemsPerPart().first())

        loader("1", FIRST).load()

        assertEquals("1", dao.observeVersion().first())
        assertEquals(listOf(PartCount("reading", 2)), dao.observeItemsPerPart().first())
    }

    private fun loader(version: String, bundle: String) =
        ContentLoader(database, readVersion = { version }, readBundle = { bundle })

    /** The rows of a table in the order of its key, each as its columns joined by `|`. */
    private suspend fun rows(table: String): List<String> = database.useReaderConnection { connection ->
        connection.usePrepared("SELECT * FROM $table ORDER BY 1, 2") { statement ->
            buildList {
                while (statement.step()) {
                    add((0 until statement.getColumnCount()).joinToString("|") { statement.getText(it) })
                }
            }
        }
    }

    private companion object {
        const val NAME = "content-loader-test.db"

        val TABLES = listOf(
            "content_item",
            "catalog_task_type",
            "catalog_topic",
            "catalog_situation",
            "catalog_function",
            "catalog_criterion",
        )

        val USER_TABLES = listOf("session", "answer_check")

        val FIRST = bundle(
            topic = "Personal identification",
            "reading.true_false.001" to "First",
            "reading.true_false.002" to "Second",
        )

        /**
         * A bundle in the format of tools/bundle.py with one entry of each kind of the catalog
         * (a criterion in both parts), the name of the topic, and the items by ID and title.
         */
        fun bundle(topic: String, vararg items: Pair<String, String>): String {
            val itemsJson = items.joinToString(",") { (id, title) ->
                """
                {"id": "$id", "part": "reading", "task_type": "reading.true_false",
                 "situation": "personal.details", "topic": "personal", "title": "$title",
                 "retired": false, "body": {"text": "Κείμενο"}}
                """
            }
            val criterion = """{"id": "grammar", "retired": false, "name": "Grammar", "greek": "Γραμματική"}"""
            return """
                {"catalog": {
                  "task_types": {
                    "answer_formats": [{"id": "choice", "retired": false}],
                    "task_types": [{"id": "reading.true_false", "retired": false, "part": "reading",
                                    "greek": "Σωστό ή λάθος", "answer": "choice", "points": "5",
                                    "minutes": 12.5}]},
                  "topics": {
                    "topics": [{"id": "personal", "retired": false, "name": "$topic",
                                "greek": "Προσωπικά στοιχεία",
                                "situations": [{"id": "personal.details", "retired": false,
                                                "name": "Personal details", "greek": "Προσωπικά στοιχεία"}]}],
                    "samples": []},
                  "functions": {
                    "functions": [{"id": "give_information", "retired": false, "name": "Give information",
                                   "greek": "Δίνω πληροφορίες", "mostly_in": ["writing.formal"]}]},
                  "criteria": {"writing": [$criterion], "speaking": [$criterion]}},
                 "items": [$itemsJson]}
            """
        }
    }
}
