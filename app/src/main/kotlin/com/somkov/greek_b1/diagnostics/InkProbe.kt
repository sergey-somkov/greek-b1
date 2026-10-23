package com.somkov.greek_b1.diagnostics

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.recognition.Ink
import com.google.mlkit.vision.digitalink.recognition.RecognitionContext
import com.google.mlkit.vision.digitalink.recognition.WritingArea
import java.io.File

/** A point of a handwritten stroke, in pixels of the writing pad, with its time in ms. */
data class InkPoint(val x: Float, val y: Float, val t: Long)

/**
 * Downloads the ML Kit Digital Ink model for Greek and recognizes handwriting with it.
 *
 * On the phone (2026-10) the Greek model (`el`) downloads once in a few seconds (22.7 MB) and then
 * works offline. Short handwritten sentences are recognized correctly, with accents and
 * punctuation, in 50–60 ms. The model gives no scores for the candidates.
 */
class InkProbe(private val context: Context) {
    val log = ProbeLog("Ink")
    var modelReady by mutableStateOf(false)
        private set
    var topCandidate by mutableStateOf("")
        private set

    private val model = DigitalInkRecognitionModel.builder(DigitalInkRecognitionModelIdentifier.EL).build()
    private val manager = RemoteModelManager.getInstance()
    private var recognizer: DigitalInkRecognizer? = null
    private var closed = false

    fun start() {
        val greek = DigitalInkRecognitionModelIdentifier.forLanguageSubtag("el")
        log.add("Greek models: ${greek.joinToString { it.languageTag }}")
        manager.isModelDownloaded(model)
            .addOnSuccessListener { downloaded ->
                log.add("model ${model.modelIdentifier.languageTag} downloaded: $downloaded")
                if (downloaded) onModelReady()
            }
            .addOnFailureListener { log.add("model check failed: $it") }
    }

    fun download() {
        // ML Kit keeps the model in the app's data directory, so the growth of that
        // directory is the size of the model.
        val sizeBefore = dataSize()
        val start = SystemClock.elapsedRealtime()
        log.add("download started, app data ${sizeBefore / 1024} KiB")
        manager.download(model, DownloadConditions.Builder().build())
            .addOnSuccessListener {
                val sizeAfter = dataSize()
                log.add(
                    "downloaded in ${SystemClock.elapsedRealtime() - start} ms, " +
                        "app data ${sizeAfter / 1024} KiB, model ${(sizeAfter - sizeBefore) / 1024} KiB"
                )
                onModelReady()
            }
            .addOnFailureListener { log.add("download failed: $it") }
    }

    fun recognize(strokes: List<List<InkPoint>>, width: Float, height: Float, toolTypes: Set<String>) {
        val current = recognizer ?: return
        val ink = Ink.builder().apply {
            strokes.filter { it.isNotEmpty() }.forEach { points ->
                val stroke = Ink.Stroke.builder()
                points.forEach { stroke.addPoint(Ink.Point.create(it.x, it.y, it.t)) }
                addStroke(stroke.build())
            }
        }.build()
        val context = RecognitionContext.builder()
            .setPreContext("")
            .setWritingArea(WritingArea(width, height))
            .build()
        val start = SystemClock.elapsedRealtime()
        current.recognize(ink, context)
            .addOnSuccessListener { result ->
                log.add(
                    "${strokes.size} strokes by $toolTypes, recognized in " +
                        "${SystemClock.elapsedRealtime() - start} ms"
                )
                // The Greek model gives no scores for the candidates.
                result.candidates.take(5).forEach { log.add("candidate \"${it.text}\", score ${it.score}") }
                topCandidate = result.candidates.firstOrNull()?.text.orEmpty()
            }
            .addOnFailureListener { log.add("recognition failed: $it") }
    }

    fun close() {
        closed = true
        recognizer?.close()
        recognizer = null
    }

    private fun onModelReady() {
        // A download or a check may end after the screen has closed.
        if (closed) return
        if (recognizer == null) {
            recognizer = DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(model).build())
        }
        modelReady = true
    }

    private fun dataSize(): Long = sizeOf(context.dataDir)

    private fun sizeOf(dir: File): Long = dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
}
