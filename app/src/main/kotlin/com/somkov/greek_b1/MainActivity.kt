package com.somkov.greek_b1

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.somkov.greek_b1.probe.InkProbe
import com.somkov.greek_b1.probe.ProbeScreen
import com.somkov.greek_b1.probe.SpeechProbe
import com.somkov.greek_b1.probe.TtsProbe

private const val TAG = "GreekB1"

class MainActivity : ComponentActivity() {
    private lateinit var tts: TtsProbe
    private lateinit var speech: SpeechProbe
    private lateinit var ink: InkProbe

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "MainActivity created")
        tts = TtsProbe(this).also { it.start() }
        speech = SpeechProbe(this).also { it.start() }
        ink = InkProbe(this).also { it.start() }
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Scaffold { innerPadding ->
                    ProbeScreen(tts, speech, ink, Modifier.padding(innerPadding))
                }
            }
        }
    }

    override fun onDestroy() {
        tts.shutdown()
        speech.shutdown()
        ink.close()
        super.onDestroy()
    }
}
