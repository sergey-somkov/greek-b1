package com.somkov.greek_b1.diagnostics

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel

/**
 * Holds the probes while the diagnostics screen is on the back stack: they start when the screen
 * opens and stop when it closes. A configuration change (dark mode, rotation) keeps them and
 * their logs.
 */
class DiagnosticsViewModel(application: Application) : AndroidViewModel(application) {
    val tts = TtsProbe(application)
    val speech = SpeechProbe(application)
    val ink = InkProbe(application)

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
