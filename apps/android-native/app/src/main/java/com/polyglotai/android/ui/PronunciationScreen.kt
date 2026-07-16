package com.polyglotai.android.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.polyglotai.android.AppContainer
import com.polyglotai.android.domain.ai.NeedsAccessCode
import kotlinx.coroutines.launch
import java.io.File

private enum class RecPhase { IDLE, RECORDING, TRANSCRIBING }

/** Record a phrase, transcribe it with Whisper, and compare to the target. Handles the runtime
 *  mic permission and (like the other AI screens) an inline access-code connect. */
@Suppress("DEPRECATION") // MediaRecorder() no-arg ctor is deprecated on API 31+ but needed for minSdk 24
@Composable
internal fun PronunciationScreen(
    container: AppContainer,
    modifier: Modifier,
    packId: String,
    packName: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var connected by remember { mutableStateOf(container.ai.isConnected) }
    var accessCode by remember { mutableStateOf("") }
    var busyConnect by remember { mutableStateOf(false) }

    var targets by remember { mutableStateOf<List<String>>(emptyList()) }
    var targetIdx by remember { mutableStateOf(0) }
    var language by remember { mutableStateOf<String?>(null) }

    var phase by remember { mutableStateOf(RecPhase.IDLE) }
    var transcript by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var hasMic by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val recorder = remember { mutableStateOf<MediaRecorder?>(null) }
    val audioFile = remember { File(context.cacheDir, "pron.m4a") }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasMic = granted
    }

    LaunchedEffect(packId) {
        val vocab = runCatching { container.packs.vocabulary(packId) }.getOrDefault(emptyList())
        targets = vocab.map { it.lemma }.filter { it.isNotBlank() }.take(30)
        language = runCatching { container.packs.manifest(packId).languageCode.take(2) }.getOrNull()
    }

    fun startRecording() {
        error = null; transcript = null
        try {
            val rec = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()
            rec.setAudioSource(MediaRecorder.AudioSource.MIC)
            rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            rec.setOutputFile(audioFile.absolutePath)
            rec.prepare()
            rec.start()
            recorder.value = rec
            phase = RecPhase.RECORDING
        } catch (e: Exception) {
            error = "Couldn't start recording: ${e.message}"
        }
    }

    fun stopAndTranscribe() {
        val rec = recorder.value ?: return
        runCatching { rec.stop() }
        rec.release()
        recorder.value = null
        phase = RecPhase.TRANSCRIBING
        scope.launch {
            try {
                transcript = container.ai.transcribe(audioFile, language)
            } catch (e: NeedsAccessCode) {
                connected = false
            } catch (e: Exception) {
                error = e.message ?: "Transcription failed."
            } finally {
                phase = RecPhase.IDLE
            }
        }
    }

    Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Pronunciation · $packName", style = MaterialTheme.typography.headlineSmall)
        if (!connected) {
            Text("Enter the access code to enable AI features.", style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                value = accessCode,
                onValueChange = { accessCode = it },
                label = { Text("Access code") },
                modifier = Modifier.fillMaxWidth(),
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Button(
                enabled = !busyConnect && accessCode.isNotBlank(),
                onClick = {
                    busyConnect = true; error = null
                    scope.launch {
                        val ok = runCatching { container.ai.connect(accessCode.trim()) }.getOrDefault(false)
                        busyConnect = false
                        if (ok) connected = true else error = "That code didn't work."
                    }
                },
            ) { Text(if (busyConnect) "Connecting…" else "Connect") }
        } else {
            val target = targets.getOrNull(targetIdx)
            if (target == null) {
                Text("No phrases to practice yet.", style = MaterialTheme.typography.bodyMedium)
            } else {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Say this", style = MaterialTheme.typography.labelMedium)
                        Text(target, style = MaterialTheme.typography.headlineSmall)
                    }
                }
                when (phase) {
                    RecPhase.IDLE -> Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { if (hasMic) startRecording() else micPermission.launch(Manifest.permission.RECORD_AUDIO) },
                    ) { Text(if (hasMic) "Record" else "Allow mic & record") }
                    RecPhase.RECORDING -> Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { stopAndTranscribe() },
                    ) { Text("Stop") }
                    RecPhase.TRANSCRIBING -> Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        onClick = {},
                    ) { Text("Transcribing…") }
                }
                transcript?.let { t ->
                    val ok = t.trim().equals(target.trim(), ignoreCase = true)
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                if (ok) "Sounds right ✓" else "Heard:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(t, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                TextButton(onClick = {
                    targetIdx = (targetIdx + 1) % targets.size
                    transcript = null; error = null
                }) { Text("Next phrase") }
            }
        }
        TextButton(onClick = onBack) { Text("Back") }
    }
}
