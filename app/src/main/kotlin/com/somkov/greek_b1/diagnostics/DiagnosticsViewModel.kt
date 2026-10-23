package com.somkov.greek_b1.diagnostics

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.somkov.greek_b1.GreekB1Application
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** The parts of the exam, in its order. */
private val PARTS = listOf("listening", "reading", "writing", "speaking")

/** What the diagnostics screen shows of the content in the database. */
data class ContentSummary(
    /** The first 8 characters of the content version, or null before the first load. */
    val version: String?,
    /** The items that the app offers, per part in the order of the exam. */
    val itemsPerPart: List<Pair<String, Int>>,
)

/**
 * Holds the probes while the diagnostics screen is on the back stack: they start when the screen
 * opens and stop when it closes. A configuration change (dark mode, rotation) keeps them and
 * their logs. Also reads which content the database holds.
 */
class DiagnosticsViewModel(application: Application) : AndroidViewModel(application) {
    val tts = TtsProbe(application)
    val speech = SpeechProbe(application)
    val ink = InkProbe(application)

    /** Null until the first query returns; it follows a load that commits while the screen is open. */
    val content: StateFlow<ContentSummary?> = run {
        val dao = (application as GreekB1Application).container.database.contentDao()
        combine(dao.observeVersion(), dao.observeItemsPerPart()) { version, counts ->
            val byPart = counts.associate { it.part to it.count }
            ContentSummary(version?.take(8), PARTS.map { it to (byPart[it] ?: 0) })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    }

    init {
        Log.i(PROBE_TAG, "probes started")
        tts.start()
        speech.start()
        ink.start()
    }

    override fun onCleared() {
        tts.shutdown()
        speech.shutdown()
        ink.close()
        Log.i(PROBE_TAG, "probes stopped")
    }
}
