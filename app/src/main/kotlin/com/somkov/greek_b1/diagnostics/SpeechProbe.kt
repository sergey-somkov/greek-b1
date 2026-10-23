package com.somkov.greek_b1.diagnostics

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.speech.ModelDownloadListener
import android.speech.RecognitionListener
import android.speech.RecognitionService
import android.speech.RecognitionSupport
import android.speech.RecognitionSupportCallback
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

private const val GREEK_TAG = "el-GR"

/**
 * Checks whether the on-device and the default recognizers support Greek, downloads the
 * on-device Greek model, and recognizes a short phrase.
 *
 * On the phone (2026-10) there is no Greek: the on-device recognizer (Android System
 * Intelligence) offers 30 languages without Greek, and recognition and the model download
 * fail with "language not supported". The default recognizer (Google) fails the same way,
 * cannot check support itself, and reports the language list of the on-device one.
 */
class SpeechProbe(private val context: Context) {
    val log = ProbeLog("STT")

    private val mainExecutor = context.mainExecutor
    private var recognizer: SpeechRecognizer? = null
    private var stopped = false

    fun start() {
        log.add("recognition available: ${SpeechRecognizer.isRecognitionAvailable(context)}")
        log.add("on-device recognition available: ${SpeechRecognizer.isOnDeviceRecognitionAvailable(context)}")
        val services = context.packageManager
            .queryIntentServices(Intent(RecognitionService.SERVICE_INTERFACE), 0)
        log.add("recognition services: ${services.joinToString { it.serviceInfo.packageName + "/" + it.serviceInfo.name }}")
        // One check at a time, so that their lines in the log do not mix.
        checkSupport(onDevice = true) { checkSupport(onDevice = false) }
    }

    private fun checkSupport(onDevice: Boolean, then: () -> Unit = {}) {
        if (stopped) return
        val name = recognizerName(onDevice)
        val checker = createRecognizer(onDevice)
        checker.checkRecognitionSupport(greekIntent(onDevice), mainExecutor, object : RecognitionSupportCallback {
            override fun onSupportResult(support: RecognitionSupport) {
                log.add("$name: installed on-device ${support.installedOnDeviceLanguages}")
                log.add("$name: pending on-device ${support.pendingOnDeviceLanguages}")
                log.add("$name: supported on-device ${greekOf(support.supportedOnDeviceLanguages)}")
                log.add("$name: online ${greekOf(support.onlineLanguages)}")
                checker.destroy()
                then()
            }

            override fun onError(error: Int) {
                log.add("$name: support check failed: ${errorName(error)}")
                checker.destroy()
                then()
            }
        })
    }

    fun downloadOnDeviceModel() {
        val downloader = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        val start = SystemClock.elapsedRealtime()
        log.add("on-device: model download requested")
        downloader.triggerModelDownload(greekIntent(onDevice = true), mainExecutor, object : ModelDownloadListener {
            override fun onProgress(completedPercent: Int) {
                log.add("on-device: download $completedPercent%")
            }

            override fun onSuccess() {
                log.add("on-device: model downloaded in ${SystemClock.elapsedRealtime() - start} ms")
                downloader.destroy()
                checkSupport(onDevice = true)
            }

            override fun onScheduled() {
                log.add("on-device: download scheduled by the system")
                downloader.destroy()
            }

            override fun onError(error: Int) {
                log.add("on-device: download failed: ${errorName(error)}")
                downloader.destroy()
            }
        })
    }

    fun recognize(onDevice: Boolean) {
        recognizer?.destroy()
        val name = recognizerName(onDevice)
        val current = createRecognizer(onDevice)
        recognizer = current
        var speechEnd = 0L
        current.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                log.add("$name: speak now")
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                speechEnd = SystemClock.elapsedRealtime()
            }

            override fun onPartialResults(partialResults: Bundle) {
                val partial = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!partial.isNullOrEmpty() && partial[0].isNotBlank()) log.add("$name: partial \"${partial[0]}\"")
            }

            override fun onResults(results: Bundle) {
                val delay = if (speechEnd > 0) SystemClock.elapsedRealtime() - speechEnd else -1
                val texts = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                val scores = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                log.add("$name: ${texts.size} results, $delay ms after the end of speech")
                texts.forEachIndexed { i, text ->
                    log.add("$name: result \"$text\", confidence ${scores?.getOrNull(i)}")
                }
                current.destroy()
            }

            override fun onError(error: Int) {
                log.add("$name: recognition failed: ${errorName(error)}")
                current.destroy()
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        current.startListening(greekIntent(onDevice))
    }

    fun shutdown() {
        stopped = true
        recognizer?.destroy()
        recognizer = null
    }

    private fun createRecognizer(onDevice: Boolean): SpeechRecognizer =
        if (onDevice) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            SpeechRecognizer.createSpeechRecognizer(context)
        }

    private fun greekIntent(onDevice: Boolean) = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, GREEK_TAG)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        // The default recognizer may go online; this asks it to stay on the device.
        if (!onDevice) putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
    }

    private fun recognizerName(onDevice: Boolean) = if (onDevice) "on-device" else "default"

    private fun greekOf(languages: List<String>): String {
        val greek = languages.filter { it.startsWith("el") }
        return "${languages.size} languages, Greek: ${greek.ifEmpty { "none" }}, all: $languages"
    }

    private fun errorName(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "network timeout"
        SpeechRecognizer.ERROR_NETWORK -> "network"
        SpeechRecognizer.ERROR_AUDIO -> "audio"
        SpeechRecognizer.ERROR_SERVER -> "server"
        SpeechRecognizer.ERROR_CLIENT -> "client"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "speech timeout"
        SpeechRecognizer.ERROR_NO_MATCH -> "no match"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "recognizer busy"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "insufficient permissions"
        SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "too many requests"
        SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "server disconnected"
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "language not supported"
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "language unavailable"
        SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT -> "cannot check support"
        SpeechRecognizer.ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS -> "cannot listen to download events"
        else -> "error $error"
    }
}
