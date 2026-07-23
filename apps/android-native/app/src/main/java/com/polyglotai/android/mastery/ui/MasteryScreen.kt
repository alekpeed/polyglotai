package com.polyglotai.android.mastery.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.polyglotai.android.mastery.MasteryContainer
import com.polyglotai.android.mastery.engine.MasteryEvent
import com.polyglotai.android.mastery.engine.MasteryPhase
import com.polyglotai.android.mastery.engine.MasterySession
import com.polyglotai.android.mastery.grade.GradeStatus
import com.polyglotai.android.mastery.grade.MockPronunciationGrader
import com.polyglotai.android.mastery.grade.PronunciationResult
import com.polyglotai.android.mastery.model.MasteryLesson
import com.polyglotai.android.mastery.model.RestartMode
import com.polyglotai.android.ui.theme.Eyebrow
import com.polyglotai.android.ui.theme.LocalDisplayFamily
import com.polyglotai.android.ui.theme.LocalPolyColors
import com.polyglotai.android.ui.theme.PlexMono
import com.polyglotai.android.ui.theme.PrimaryButton
import com.polyglotai.android.ui.theme.SecondaryButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Progressive Sentence Mastery — Milestone 1 shell. Loads a bundled lesson and runs the full
 * word-by-word build → one-take → restart loop through the pure [com.polyglotai.android.mastery.engine.MasteryEngine].
 * Grading is simulated (mock behind the real grader seam); target audio is placeholder playback.
 * Everything an Azure/OpenAI adapter needs to slot into is already wired.
 */
@Composable
fun MasteryScreen(container: MasteryContainer, modifier: Modifier = Modifier, onBack: () -> Unit) {
    var lesson by remember { mutableStateOf<MasteryLesson?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    val poly = LocalPolyColors.current

    LaunchedEffect(Unit) {
        val l = runCatching { withContext(Dispatchers.IO) { container.content.firstLesson() } }.getOrNull()
        if (l == null) loadError = "No mastery lessons are bundled yet." else lesson = l
    }

    val l = lesson
    if (l == null) {
        Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Eyebrow("Sentence Mastery")
            Text(loadError ?: "Loading…", style = MaterialTheme.typography.bodyMedium, color = poly.inkSoft)
            SecondaryButton(onClick = onBack) { Text("Back") }
        }
        return
    }
    MasteryRunner(container, l, modifier, onBack)
}

@Composable
private fun MasteryRunner(container: MasteryContainer, lesson: MasteryLesson, modifier: Modifier, onBack: () -> Unit) {
    val poly = LocalPolyColors.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val engine = container.engine
    val sessionId = remember { "sess_${System.currentTimeMillis()}" }
    var session by remember { mutableStateOf(engine.start(lesson)) }
    var overriding by remember { mutableStateOf(false) }

    val recorder = remember { com.polyglotai.android.mastery.voice.MasteryAudioRecorder(context) }
    var recording by remember { mutableStateOf(false) }
    var hasMic by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasMic = it }

    fun dispatch(e: MasteryEvent) { session = engine.reduce(session, e) }

    // Simulate a grade through the real grader seam (mock lets us pick the outcome), log it, feed the engine.
    fun grade(status: GradeStatus, sentence: Boolean) {
        overriding = false
        (container.grader as? MockPronunciationGrader)?.nextStatus = status
        scope.launch {
            val result: PronunciationResult = if (sentence) {
                container.grader.gradeSentence(null, session.currentSentence, lesson.locale, container.defaultProfile)
            } else {
                container.grader.gradeUnit(null, session.currentUnit ?: return@launch, lesson.locale, container.defaultProfile)
            }
            container.logAttempt(sessionId, session, if (sentence) "sentence" else "unit", result)
            dispatch(if (sentence) MasteryEvent.SentenceGraded(result) else MasteryEvent.UnitGraded(result))
        }
    }

    fun override(reason: String, sentence: Boolean) {
        overriding = false
        scope.launch {
            session.lastResult?.let { container.logAttempt(sessionId, session, if (sentence) "sentence" else "unit", it, manualOverride = true, overrideReason = reason) }
            dispatch(MasteryEvent.OverrideAccepted)
        }
    }

    // Placeholder audio playback for the audio phases; real TTS drops in behind container.voice.
    LaunchedEffect(session.phase, session.unitIndex, session.sentenceIndex) {
        when (session.phase) {
            MasteryPhase.UnitAudioPlaying -> {
                container.voice.play(session.currentUnit?.spokenText ?: "", lesson.locale)
                dispatch(MasteryEvent.AudioFinished)
            }
            MasteryPhase.SentenceAudioPlaying -> {
                container.voice.play(session.currentSentence.targetText, lesson.locale)
                dispatch(MasteryEvent.AudioFinished)
            }
            else -> Unit
        }
    }

    fun startRecording() {
        if (!hasMic) { micLauncher.launch(Manifest.permission.RECORD_AUDIO); return }
        if (recorder.start()) recording = true
    }
    fun stopRecording() {
        recorder.stop()
        recording = false
        dispatch(MasteryEvent.RecordingCaptured)
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Header
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Eyebrow("Sentence Mastery · ${lesson.locale}")
                Text(lesson.title, fontFamily = LocalDisplayFamily.current, fontSize = 22.sp, color = poly.ink)
            }
            Text("MODE ${session.restartMode.name}", fontFamily = PlexMono, fontSize = 11.sp, color = poly.inkSoft)
        }
        LinearProgressIndicator(
            progress = { (session.sentenceIndex.toFloat() / lesson.sentences.size).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(99.dp)),
            color = poly.indigoFill,
            trackColor = poly.line,
        )
        Text("Sentence ${session.sentenceIndex + 1} of ${lesson.sentences.size}", fontFamily = PlexMono, fontSize = 11.sp, color = poly.inkSoft)

        // The assembled sentence so far
        if (session.phase != MasteryPhase.LessonComplete) {
            AssembledPanel(session)
        }

        when (session.phase) {
            MasteryPhase.UnitPresentation, MasteryPhase.UnitAudioPlaying, MasteryPhase.UnitWaitingForSpeech,
            MasteryPhase.UnitGrading, MasteryPhase.UnitResult, MasteryPhase.UnitComprehension ->
                UnitArea(session, recording, ::dispatch, ::startRecording, ::stopRecording, ::grade,
                    overriding, { overriding = it }, { r -> override(r, false) })

            MasteryPhase.SentenceReady, MasteryPhase.SentenceAudioPlaying, MasteryPhase.SentenceWaitingForSpeech,
            MasteryPhase.SentenceGrading, MasteryPhase.SentencePass, MasteryPhase.SentenceFail ->
                SentenceArea(session, recording, ::dispatch, ::startRecording, ::stopRecording, ::grade,
                    overriding, { overriding = it }, { r -> override(r, true) })

            MasteryPhase.ServiceError, MasteryPhase.EnvironmentError -> ErrorArea(session, ::dispatch)

            MasteryPhase.LessonComplete -> CompleteArea(session, onBack)

            else -> Unit
        }

        // Restart-mode selector — recommend C, all selectable (spec §7).
        if (session.phase != MasteryPhase.LessonComplete) {
            Spacer(Modifier.height(4.dp))
            Text("RESTART MODE", fontFamily = PlexMono, fontSize = 10.sp, letterSpacing = 1.sp, color = poly.inkSoft)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RestartMode.entries.forEach { m ->
                    val active = m == session.restartMode
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(if (poly.flat) 0.dp else 4.dp))
                            .background(if (active) poly.indigoFill else poly.surfaceRaised)
                            .border(1.dp, poly.line, RoundedCornerShape(if (poly.flat) 0.dp else 4.dp))
                            .clickableNoRipple { dispatch(MasteryEvent.SelectMode(m)) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(m.name, fontFamily = PlexMono, fontSize = 12.sp, color = if (active) poly.onFill else poly.ink)
                    }
                }
            }
            Text(session.restartMode.label, fontFamily = MaterialTheme.typography.bodySmall.fontFamily, fontSize = 11.sp, color = poly.inkSoft)
            Spacer(Modifier.height(4.dp))
            Text("Back", fontFamily = PlexMono, fontSize = 12.sp, color = poly.indigo, modifier = Modifier.clickableNoRipple(onBack))
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AssembledPanel(session: MasterySession) {
    val poly = LocalPolyColors.current
    val shape = RoundedCornerShape(if (poly.flat) 0.dp else 6.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(poly.surfaceRaised).border(1.dp, poly.line, shape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Eyebrow("Building")
        val assembled = session.assembledText
        Text(
            if (assembled.isBlank()) "…" else assembled,
            fontFamily = LocalDisplayFamily.current,
            fontSize = 20.sp,
            color = poly.ink,
        )
        Text(session.currentSentence.translation, fontFamily = MaterialTheme.typography.bodySmall.fontFamily, fontSize = 12.sp, color = poly.inkSoft)
    }
}

@Composable
private fun UnitArea(
    session: MasterySession,
    recording: Boolean,
    dispatch: (MasteryEvent) -> Unit,
    startRecording: () -> Unit,
    stopRecording: () -> Unit,
    grade: (GradeStatus, Boolean) -> Unit,
    overriding: Boolean,
    setOverriding: (Boolean) -> Unit,
    override: (String) -> Unit,
) {
    val poly = LocalPolyColors.current
    val unit = session.currentUnit ?: return
    val shape = RoundedCornerShape(if (poly.flat) 0.dp else 6.dp)

    Column(
        Modifier.fillMaxWidth().clip(shape).background(poly.surfaceRaised).border(1.dp, poly.line, shape).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Eyebrow("Unit ${session.unitIndex + 1} of ${session.totalUnits}")
        Text(unit.displayText, fontFamily = LocalDisplayFamily.current, fontSize = 30.sp, color = poly.ink, textAlign = TextAlign.Center)
        Text(unit.translation, fontFamily = MaterialTheme.typography.bodyMedium.fontFamily, fontSize = 14.sp, color = poly.inkSoft)
    }

    Spacer(Modifier.height(10.dp))

    when (session.phase) {
        MasteryPhase.UnitPresentation -> PrimaryButton(onClick = { dispatch(MasteryEvent.PlayAudio) }, modifier = Modifier.fillMaxWidth()) { Text("Play ▶") }
        MasteryPhase.UnitAudioPlaying -> Text("Playing…", fontFamily = PlexMono, fontSize = 12.sp, color = poly.inkSoft)
        MasteryPhase.UnitWaitingForSpeech ->
            if (recording) PrimaryButton(onClick = stopRecording, modifier = Modifier.fillMaxWidth()) { Text("Stop ■") }
            else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton(onClick = { dispatch(MasteryEvent.PlayAudio) }, modifier = Modifier.weight(1f)) { Text("Replay") }
                PrimaryButton(onClick = startRecording, modifier = Modifier.weight(1f)) { Text("Record ●") }
            }
        MasteryPhase.UnitGrading -> SimulateRow(onGrade = { grade(it, false) })
        MasteryPhase.UnitResult -> ResultRow(session, overriding, setOverriding, override, onRetry = { dispatch(MasteryEvent.Retry) })
        MasteryPhase.UnitComprehension -> unit.comprehension?.let { c ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(c.prompt, fontFamily = MaterialTheme.typography.bodyMedium.fontFamily, fontSize = 14.sp, color = poly.ink)
                c.options.forEachIndexed { i, opt ->
                    SecondaryButton(onClick = { dispatch(MasteryEvent.ComprehensionAnswered(i == c.correctIndex)) }, modifier = Modifier.fillMaxWidth()) { Text(opt) }
                }
            }
        }
        else -> Unit
    }
}

@Composable
private fun SentenceArea(
    session: MasterySession,
    recording: Boolean,
    dispatch: (MasteryEvent) -> Unit,
    startRecording: () -> Unit,
    stopRecording: () -> Unit,
    grade: (GradeStatus, Boolean) -> Unit,
    overriding: Boolean,
    setOverriding: (Boolean) -> Unit,
    override: (String) -> Unit,
) {
    val poly = LocalPolyColors.current
    val shape = RoundedCornerShape(if (poly.flat) 0.dp else 6.dp)
    val sentence = session.currentSentence

    Column(
        Modifier.fillMaxWidth().clip(shape).background(poly.surfaceRaised).border(1.dp, poly.line, shape).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Eyebrow("Full sentence — one take")
        Text(sentence.targetText, fontFamily = LocalDisplayFamily.current, fontSize = 24.sp, color = poly.ink)
        Text(sentence.translation, fontFamily = MaterialTheme.typography.bodySmall.fontFamily, fontSize = 12.sp, color = poly.inkSoft)
    }

    Spacer(Modifier.height(10.dp))

    when (session.phase) {
        MasteryPhase.SentenceReady -> PrimaryButton(onClick = { dispatch(MasteryEvent.PlayAudio) }, modifier = Modifier.fillMaxWidth()) { Text("Play sentence ▶") }
        MasteryPhase.SentenceAudioPlaying -> Text("Playing…", fontFamily = PlexMono, fontSize = 12.sp, color = poly.inkSoft)
        MasteryPhase.SentenceWaitingForSpeech ->
            if (recording) PrimaryButton(onClick = stopRecording, modifier = Modifier.fillMaxWidth()) { Text("Stop ■") }
            else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton(onClick = { dispatch(MasteryEvent.PlayAudio) }, modifier = Modifier.weight(1f)) { Text("Replay") }
                PrimaryButton(onClick = startRecording, modifier = Modifier.weight(1f)) { Text("Record ●") }
            }
        MasteryPhase.SentenceGrading -> SimulateRow(onGrade = { grade(it, true) })
        MasteryPhase.SentencePass -> {
            Text("Passed ✓", fontFamily = PlexMono, fontSize = 13.sp, color = poly.heat[0])
            PrimaryButton(onClick = { dispatch(MasteryEvent.Advance) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (session.isLastSentence) "Finish lesson →" else "Next sentence →")
            }
        }
        MasteryPhase.SentenceFail -> {
            Text("Failed — restarting per Mode ${session.restartMode.name}", fontFamily = PlexMono, fontSize = 12.sp, color = poly.heat[5])
            if (overriding) OverrideReasons(override)
            else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton(onClick = { setOverriding(true) }, modifier = Modifier.weight(1f)) { Text("Override") }
                PrimaryButton(onClick = { dispatch(MasteryEvent.Retry) }, modifier = Modifier.weight(1f)) { Text("Restart") }
            }
        }
        else -> Unit
    }
}

@Composable
private fun ResultRow(
    session: MasterySession,
    overriding: Boolean,
    setOverriding: (Boolean) -> Unit,
    override: (String) -> Unit,
    onRetry: () -> Unit,
) {
    val poly = LocalPolyColors.current
    val uncertain = session.lastResult?.status == GradeStatus.UNCERTAIN
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text(if (uncertain) "Unclear — try again or accept" else "Not recognized — try again", fontFamily = PlexMono, fontSize = 12.sp, color = poly.heat[5])
        if (overriding) OverrideReasons(override)
        else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton(onClick = { setOverriding(true) }, modifier = Modifier.weight(1f)) { Text("Override") }
            PrimaryButton(onClick = onRetry, modifier = Modifier.weight(1f)) { Text("Retry") }
        }
    }
}

@Composable
private fun OverrideReasons(override: (String) -> Unit) {
    val poly = LocalPolyColors.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        Text("Why accept? (logged)", fontFamily = PlexMono, fontSize = 11.sp, color = poly.inkSoft)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SecondaryButton(onClick = { override("ai_error") }, modifier = Modifier.weight(1f)) { Text("AI error", fontSize = 12.sp) }
            SecondaryButton(onClick = { override("acceptable_accent") }, modifier = Modifier.weight(1f)) { Text("Accent", fontSize = 12.sp) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SecondaryButton(onClick = { override("microphone") }, modifier = Modifier.weight(1f)) { Text("Mic", fontSize = 12.sp) }
            SecondaryButton(onClick = { override("other") }, modifier = Modifier.weight(1f)) { Text("Other", fontSize = 12.sp) }
        }
    }
}

/** Milestone-1 stand-in for real grading: pick the outcome so every branch is reachable. */
@Composable
private fun SimulateRow(onGrade: (GradeStatus) -> Unit) {
    val poly = LocalPolyColors.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        Text("SIMULATE GRADE (mock)", fontFamily = PlexMono, fontSize = 10.sp, letterSpacing = 1.sp, color = poly.inkSoft)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PrimaryButton(onClick = { onGrade(GradeStatus.PASS) }, modifier = Modifier.weight(1f)) { Text("Pass") }
            SecondaryButton(onClick = { onGrade(GradeStatus.FAIL) }, modifier = Modifier.weight(1f)) { Text("Fail") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SecondaryButton(onClick = { onGrade(GradeStatus.UNCERTAIN) }, modifier = Modifier.weight(1f)) { Text("Uncertain", fontSize = 12.sp) }
            SecondaryButton(onClick = { onGrade(GradeStatus.ENVIRONMENT_ERROR) }, modifier = Modifier.weight(1f)) { Text("Noise", fontSize = 12.sp) }
        }
    }
}

@Composable
private fun ErrorArea(session: MasterySession, dispatch: (MasteryEvent) -> Unit) {
    val poly = LocalPolyColors.current
    val env = session.phase == MasteryPhase.EnvironmentError
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            if (env) "Environment problem — not counted against you." else "Service problem — not counted against you.",
            fontFamily = PlexMono, fontSize = 12.sp, color = poly.inkSoft,
        )
        PrimaryButton(onClick = { dispatch(MasteryEvent.Retry) }, modifier = Modifier.fillMaxWidth()) { Text("Try again") }
    }
}

@Composable
private fun CompleteArea(session: MasterySession, onBack: () -> Unit) {
    val poly = LocalPolyColors.current
    Column(Modifier.fillMaxWidth().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Lesson complete", fontFamily = LocalDisplayFamily.current, fontSize = 26.sp, color = poly.ink)
        Text(
            "${session.completedSentences} sentences · ${session.completedUnits} units · ${session.restartCount} restarts",
            fontFamily = PlexMono, fontSize = 12.sp, color = poly.inkSoft,
        )
        PrimaryButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Done") }
    }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)
