package com.somkov.greek_b1.diagnostics

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.somkov.greek_b1.R
import com.somkov.greek_b1.ScreenScaffold

/**
 * The capability probe of story 1, opened from the settings: what the phone offers for Greek TTS,
 * speech recognition and handwriting, to see what a system update or a new phone changes.
 */
@Composable
fun DiagnosticsScreen(onBack: () -> Unit) {
    val application = LocalContext.current.applicationContext as Application
    // Scoped to the entry of this screen on the back stack, so it is cleared when the screen closes.
    val viewModel = viewModel { DiagnosticsViewModel(application) }
    ScreenScaffold(stringResource(R.string.diagnostics), onBack) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TtsSection(viewModel.tts)
            HorizontalDivider()
            SpeechSection(viewModel.speech)
            HorizontalDivider()
            InkSection(viewModel.ink)
        }
    }
}

@Composable
private fun TtsSection(tts: TtsProbe) {
    SectionTitle("Text to speech")
    Text("«$TTS_SAMPLE»", style = MaterialTheme.typography.bodyMedium)
    tts.voices.forEach { voice ->
        OutlinedButton(onClick = { tts.speak(voice) }, modifier = Modifier.fillMaxWidth()) {
            Text("${voice.engine.substringAfterLast('.')}: ${voice.voice.name}")
        }
    }
    LogLines(tts.log)
}

@Composable
private fun SpeechSection(speech: SpeechProbe) {
    val context = LocalContext.current
    // The action that waits for the microphone permission.
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) pending?.invoke() else speech.log.add("microphone permission denied")
    }
    fun withMicrophone(action: () -> Unit) {
        val granted = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            action()
        } else {
            pending = action
            permission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    SectionTitle("Speech recognition")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { withMicrophone { speech.recognize(onDevice = true) } }) { Text("Listen on-device") }
        Button(onClick = { withMicrophone { speech.recognize(onDevice = false) } }) { Text("Listen default") }
        OutlinedButton(onClick = { speech.downloadOnDeviceModel() }) { Text("Download on-device Greek") }
    }
    LogLines(speech.log)
}

@Composable
private fun InkSection(ink: InkProbe) {
    val strokes = remember { mutableStateListOf<SnapshotStateList<InkPoint>>() }
    val toolTypes = remember { mutableSetOf<String>() }
    var padSize by remember { mutableStateOf(IntSize.Zero) }

    SectionTitle("Handwriting (ML Kit Digital Ink)")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { ink.download() }, enabled = !ink.modelReady) { Text("Download model") }
        Button(
            onClick = {
                val points = strokes.map { it.toList() }
                ink.recognize(points, padSize.width.toFloat(), padSize.height.toFloat(), toolTypes.toSet())
            },
            enabled = ink.modelReady && strokes.isNotEmpty(),
        ) { Text("Recognize") }
        OutlinedButton(onClick = { strokes.clear(); toolTypes.clear() }) { Text("Clear") }
    }
    WritingPad(
        strokes = strokes,
        onToolType = { toolTypes += it },
        modifier = Modifier.fillMaxWidth().height(220.dp).onSizeChanged { padSize = it },
    )
    if (ink.topCandidate.isNotEmpty()) {
        Text(ink.topCandidate, style = MaterialTheme.typography.headlineSmall)
    }
    LogLines(ink.log)
}

/** A pad that records strokes with their timestamps, as ML Kit expects them. */
@Composable
private fun WritingPad(
    strokes: SnapshotStateList<SnapshotStateList<InkPoint>>,
    onToolType: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lineColor = MaterialTheme.colorScheme.onSurface
    Canvas(
        modifier = modifier
            .border(1.dp, Color.Gray)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    onToolType(down.type.toString())
                    val stroke = mutableStateListOf(down.toInkPoint())
                    strokes += stroke
                    down.consume()
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        change.historical.forEach { stroke += InkPoint(it.position.x, it.position.y, it.uptimeMillis) }
                        stroke += change.toInkPoint()
                        change.consume()
                        if (!change.pressed) break
                    }
                }
            },
    ) {
        strokes.forEach { points ->
            if (points.isEmpty()) return@forEach
            val path = Path().apply {
                moveTo(points[0].x, points[0].y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(path, lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            if (points.size == 1) drawCircle(lineColor, 1.5.dp.toPx(), Offset(points[0].x, points[0].y))
        }
    }
}

private fun PointerInputChange.toInkPoint() = InkPoint(position.x, position.y, uptimeMillis)

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun LogLines(log: ProbeLog) {
    Column {
        log.lines.forEach { line ->
            Text(line, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
        }
    }
}
