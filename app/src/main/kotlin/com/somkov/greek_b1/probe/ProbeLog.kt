package com.somkov.greek_b1.probe

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.mutableStateListOf

const val PROBE_TAG = "GreekB1Probe"

/**
 * One section of the probe report. Each line goes to logcat at once and to the screen
 * on the main thread, since the platform callbacks come on binder and worker threads.
 */
class ProbeLog(private val section: String) {
    val lines = mutableStateListOf<String>()

    private val mainHandler = Handler(Looper.getMainLooper())

    fun add(line: String) {
        Log.i(PROBE_TAG, "$section: $line")
        if (Looper.myLooper() == Looper.getMainLooper()) {
            lines += line
        } else {
            mainHandler.post { lines += line }
        }
    }
}
