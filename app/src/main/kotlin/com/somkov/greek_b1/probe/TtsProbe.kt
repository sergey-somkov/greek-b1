package com.somkov.greek_b1.probe

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.compose.runtime.mutableStateListOf
import java.util.Locale

private val GREEK = Locale.forLanguageTag("el-GR")

const val TTS_SAMPLE =
    "Καλημέρα σας. Θα ήθελα να κλείσω ένα ραντεβού με τον γιατρό για την Πέμπτη, " +
        "στις δέκα και μισή το πρωί. Είναι δυνατόν;"

/** A Greek voice and the engine instance that speaks it. */
class ProbeVoice(val engine: String, val tts: TextToSpeech, val voice: Voice)

/** Lists the TTS engines and their Greek voices, and speaks a sample sentence with each voice. */
class TtsProbe(private val context: Context) {
    val log = ProbeLog("TTS")
    val voices = mutableStateListOf<ProbeVoice>()

    private val instances = mutableListOf<TextToSpeech>()
    private val utteranceStarts = mutableMapOf<String, Long>()

    fun start() {
        // An instance of the default engine, only to list the engines.
        lateinit var lister: TextToSpeech
        lister = TextToSpeech(context) { status ->
            if (status != TextToSpeech.SUCCESS) {
                log.add("default engine failed to start: status $status")
                return@TextToSpeech
            }
            // The engine set in the system settings, and the one the app gets as the default.
            // They differ on Samsung phones: Samsung TTS is a "private engine" that other apps
            // cannot see or bind to, and TextToSpeech falls back to the next engine. A <queries>
            // entry for the Samsung package does not help.
            val configured = Settings.Secure.getString(context.contentResolver, Settings.Secure.TTS_DEFAULT_SYNTH)
            log.add("configured default engine: $configured")
            log.add("default engine: ${lister.defaultEngine}")
            val engines = lister.engines
            log.add("engines: ${engines.joinToString { "${it.name} (${it.label})" }}")
            lister.shutdown()
            engines.forEach { openEngine(it.name) }
        }
    }

    private fun openEngine(engine: String) {
        lateinit var tts: TextToSpeech
        tts = TextToSpeech(context, { status ->
            if (status != TextToSpeech.SUCCESS) {
                log.add("$engine: failed to start: status $status")
                return@TextToSpeech
            }
            instances += tts
            tts.setOnUtteranceProgressListener(progressListener(engine))
            reportEngine(engine, tts)
        }, engine)
    }

    private fun reportEngine(engine: String, tts: TextToSpeech) {
        log.add("$engine: el-GR availability: ${availability(tts.isLanguageAvailable(GREEK))}")
        val all = tts.voices.orEmpty()
        val greek = all.filter { it.locale.language == "el" }.sortedBy { it.name }
        log.add("$engine: ${all.size} voices, ${greek.size} Greek")
        greek.forEach { voice ->
            log.add(
                "$engine: voice ${voice.name}, locale ${voice.locale.toLanguageTag()}, " +
                    "quality ${voice.quality}, latency ${voice.latency}, " +
                    "network required ${voice.isNetworkConnectionRequired}, " +
                    "features ${voice.features.sorted()}"
            )
            voices += ProbeVoice(engine, tts, voice)
        }
    }

    fun speak(probeVoice: ProbeVoice) {
        instances.forEach { it.stop() }
        val tts = probeVoice.tts
        val setVoice = tts.setVoice(probeVoice.voice)
        val id = "${probeVoice.engine}/${probeVoice.voice.name}/${SystemClock.elapsedRealtime()}"
        utteranceStarts[id] = SystemClock.elapsedRealtime()
        val result = tts.speak(TTS_SAMPLE, TextToSpeech.QUEUE_FLUSH, null, id)
        log.add("speak ${probeVoice.voice.name}: setVoice $setVoice, speak $result")
    }

    fun shutdown() {
        instances.forEach { it.shutdown() }
        instances.clear()
    }

    private fun progressListener(engine: String) = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String) {
            log.add("$engine: started after ${elapsed(utteranceId)} ms")
        }

        override fun onDone(utteranceId: String) {
            log.add("$engine: done after ${elapsed(utteranceId)} ms")
        }

        override fun onStop(utteranceId: String, interrupted: Boolean) {
            log.add("$engine: stopped after ${elapsed(utteranceId)} ms")
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String) {
            log.add("$engine: error after ${elapsed(utteranceId)} ms")
        }

        override fun onError(utteranceId: String, errorCode: Int) {
            log.add("$engine: error $errorCode after ${elapsed(utteranceId)} ms")
        }
    }

    private fun elapsed(utteranceId: String): Long =
        SystemClock.elapsedRealtime() - (utteranceStarts[utteranceId] ?: 0)

    private fun availability(code: Int): String = when (code) {
        TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE -> "country and variant available"
        TextToSpeech.LANG_COUNTRY_AVAILABLE -> "country available"
        TextToSpeech.LANG_AVAILABLE -> "language available"
        TextToSpeech.LANG_MISSING_DATA -> "missing data"
        TextToSpeech.LANG_NOT_SUPPORTED -> "not supported"
        else -> "code $code"
    }
}
