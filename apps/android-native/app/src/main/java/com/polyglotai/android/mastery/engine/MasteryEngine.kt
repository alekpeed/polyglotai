package com.polyglotai.android.mastery.engine

import com.polyglotai.android.mastery.grade.GradeStatus
import com.polyglotai.android.mastery.grade.PronunciationResult
import com.polyglotai.android.mastery.model.LearningUnit
import com.polyglotai.android.mastery.model.MasteryLesson
import com.polyglotai.android.mastery.model.MasterySentence
import com.polyglotai.android.mastery.model.RestartMode

/** The lesson's position in the formal state machine (spec §5). */
enum class MasteryPhase {
    LessonLoading,
    UnitPresentation,
    UnitAudioPlaying,
    UnitWaitingForSpeech,
    UnitGrading,
    UnitResult,
    UnitComprehension,
    UnitComplete,
    SentenceReady,
    SentenceAudioPlaying,
    SentenceWaitingForSpeech,
    SentenceGrading,
    SentencePass,
    SentenceFail,
    SentenceRestarting,
    LessonComplete,
    ServiceError,
    EnvironmentError,
    Paused,
}

/** Immutable snapshot of a mastery session. The engine is a pure function over this. */
data class MasterySession(
    val lesson: MasteryLesson,
    val restartMode: RestartMode,
    val sentenceIndex: Int = 0,
    /** Index of the unit currently being learned within the sentence. */
    val unitIndex: Int = 0,
    /** How many leading units have been added to the visible, assembled sentence. */
    val assembledUnits: Int = 0,
    val phase: MasteryPhase = MasteryPhase.UnitPresentation,
    val lastResult: PronunciationResult? = null,
    val restartCount: Int = 0,
    /** Units correctly reviewed this session — a light activity metric, not the mastery signal. */
    val completedUnits: Int = 0,
    val completedSentences: Int = 0,
    private val prePausePhase: MasteryPhase? = null,
) {
    val currentSentence: MasterySentence get() = lesson.sentences[sentenceIndex]
    val currentUnit: LearningUnit? get() = currentSentence.units.getOrNull(unitIndex)
    val assembledText: String
        get() = currentSentence.units.take(assembledUnits).joinToString(" ") { it.displayText }
    val isLastSentence: Boolean get() = sentenceIndex >= lesson.sentences.size - 1
    val totalUnits: Int get() = currentSentence.units.size

    internal fun paused(from: MasteryPhase) = copy(phase = MasteryPhase.Paused, prePausePhase = from)
    internal fun resumed() = copy(phase = prePausePhase ?: MasteryPhase.UnitPresentation, prePausePhase = null)
}

/** Events the UI feeds the engine. Recording/grading/audio are performed by the UI; their outcomes
 *  arrive here as events, keeping the engine free of Android and provider code (spec §3.2). */
sealed interface MasteryEvent {
    data object PlayAudio : MasteryEvent
    data object AudioFinished : MasteryEvent
    data object RecordingCaptured : MasteryEvent
    data class UnitGraded(val result: PronunciationResult) : MasteryEvent
    data class ComprehensionAnswered(val correct: Boolean) : MasteryEvent
    data class SentenceGraded(val result: PronunciationResult) : MasteryEvent
    data object OverrideAccepted : MasteryEvent
    data object Advance : MasteryEvent
    data object Retry : MasteryEvent
    data class SelectMode(val mode: RestartMode) : MasteryEvent
    data object Pause : MasteryEvent
    data object Resume : MasteryEvent
}

/**
 * Pure reducer for Progressive Sentence Mastery. `reduce(session, event) -> session`, no side
 * effects, no Android — so the whole flow (build, one-take, restart modes, override, errors) is unit
 * testable without a mic or a network. The UI interprets each new phase to perform effects (play,
 * record, grade) and feeds the results back as events.
 */
class MasteryEngine {

    fun start(lesson: MasteryLesson, mode: RestartMode = lesson.defaultRestartMode): MasterySession =
        MasterySession(lesson = lesson, restartMode = mode, phase = MasteryPhase.UnitPresentation)

    fun reduce(s: MasterySession, event: MasteryEvent): MasterySession {
        // Global events valid from most phases.
        when (event) {
            is MasteryEvent.SelectMode -> return s.copy(restartMode = event.mode)
            is MasteryEvent.Pause -> return if (s.phase == MasteryPhase.Paused) s else s.paused(s.phase)
            is MasteryEvent.Resume -> return if (s.phase == MasteryPhase.Paused) s.resumed() else s
            else -> Unit
        }

        return when (s.phase) {
            MasteryPhase.UnitPresentation -> when (event) {
                is MasteryEvent.PlayAudio -> s.copy(phase = MasteryPhase.UnitAudioPlaying)
                else -> s
            }

            MasteryPhase.UnitAudioPlaying -> when (event) {
                is MasteryEvent.AudioFinished -> s.copy(phase = MasteryPhase.UnitWaitingForSpeech)
                else -> s
            }

            MasteryPhase.UnitWaitingForSpeech -> when (event) {
                is MasteryEvent.RecordingCaptured -> s.copy(phase = MasteryPhase.UnitGrading)
                is MasteryEvent.PlayAudio -> s.copy(phase = MasteryPhase.UnitAudioPlaying)
                else -> s
            }

            MasteryPhase.UnitGrading -> when (event) {
                is MasteryEvent.UnitGraded -> applyUnitGrade(s, event.result)
                else -> s
            }

            MasteryPhase.UnitResult -> when (event) {
                // Fail/uncertain result screen: retry the unit, or accept via override.
                is MasteryEvent.Retry -> s.copy(phase = MasteryPhase.UnitPresentation, lastResult = null)
                is MasteryEvent.OverrideAccepted -> afterUnitPassed(s)
                else -> s
            }

            MasteryPhase.UnitComprehension -> when (event) {
                is MasteryEvent.ComprehensionAnswered ->
                    if (event.correct) advanceUnit(s)
                    // Wrong meaning: repeat the unit from the top (comprehension must pass to add it).
                    else s.copy(phase = MasteryPhase.UnitPresentation, lastResult = null)
                else -> s
            }

            MasteryPhase.SentenceReady -> when (event) {
                is MasteryEvent.PlayAudio -> s.copy(phase = MasteryPhase.SentenceAudioPlaying)
                else -> s
            }

            MasteryPhase.SentenceAudioPlaying -> when (event) {
                is MasteryEvent.AudioFinished -> s.copy(phase = MasteryPhase.SentenceWaitingForSpeech)
                else -> s
            }

            MasteryPhase.SentenceWaitingForSpeech -> when (event) {
                is MasteryEvent.RecordingCaptured -> s.copy(phase = MasteryPhase.SentenceGrading)
                is MasteryEvent.PlayAudio -> s.copy(phase = MasteryPhase.SentenceAudioPlaying)
                else -> s
            }

            MasteryPhase.SentenceGrading -> when (event) {
                is MasteryEvent.SentenceGraded -> applySentenceGrade(s, event.result)
                else -> s
            }

            MasteryPhase.SentencePass -> when (event) {
                is MasteryEvent.Advance -> advanceSentence(s)
                else -> s
            }

            MasteryPhase.SentenceFail -> when (event) {
                is MasteryEvent.Retry, is MasteryEvent.Advance -> applyRestart(s)
                is MasteryEvent.OverrideAccepted -> advanceSentence(s.copy(phase = MasteryPhase.SentencePass))
                else -> s
            }

            MasteryPhase.ServiceError, MasteryPhase.EnvironmentError -> when (event) {
                // Technical/environment failures never count as learner failure — just retry the step.
                is MasteryEvent.Retry -> s.copy(
                    phase = if (s.assembledUnits >= s.totalUnits) MasteryPhase.SentenceReady else MasteryPhase.UnitPresentation,
                    lastResult = null,
                )
                else -> s
            }

            MasteryPhase.LessonComplete, MasteryPhase.Paused, MasteryPhase.LessonLoading,
            MasteryPhase.UnitComplete, MasteryPhase.SentenceRestarting -> s
        }
    }

    private fun applyUnitGrade(s: MasterySession, r: PronunciationResult): MasterySession = when (r.status) {
        GradeStatus.PASS -> afterUnitPassed(s.copy(lastResult = r))
        GradeStatus.FAIL, GradeStatus.UNCERTAIN -> s.copy(phase = MasteryPhase.UnitResult, lastResult = r)
        GradeStatus.SERVICE_ERROR -> s.copy(phase = MasteryPhase.ServiceError, lastResult = r)
        GradeStatus.ENVIRONMENT_ERROR -> s.copy(phase = MasteryPhase.EnvironmentError, lastResult = r)
    }

    /** A passed unit either runs its comprehension check (if any) or is added straight away. */
    private fun afterUnitPassed(s: MasterySession): MasterySession =
        if (s.currentUnit?.comprehension != null) s.copy(phase = MasteryPhase.UnitComprehension)
        else advanceUnit(s)

    /** Add the current unit to the assembled sentence and move on, or open the full-sentence phase. */
    private fun advanceUnit(s: MasterySession): MasterySession {
        val nextUnit = s.unitIndex + 1
        val assembled = nextUnit
        val completed = s.completedUnits + 1
        return if (nextUnit >= s.totalUnits) {
            s.copy(phase = MasteryPhase.SentenceReady, unitIndex = nextUnit, assembledUnits = assembled, completedUnits = completed, lastResult = null)
        } else {
            s.copy(phase = MasteryPhase.UnitPresentation, unitIndex = nextUnit, assembledUnits = assembled, completedUnits = completed, lastResult = null)
        }
    }

    private fun applySentenceGrade(s: MasterySession, r: PronunciationResult): MasterySession = when (r.status) {
        GradeStatus.PASS -> s.copy(phase = MasteryPhase.SentencePass, lastResult = r)
        GradeStatus.FAIL, GradeStatus.UNCERTAIN -> s.copy(phase = MasteryPhase.SentenceFail, lastResult = r)
        GradeStatus.SERVICE_ERROR -> s.copy(phase = MasteryPhase.ServiceError, lastResult = r)
        GradeStatus.ENVIRONMENT_ERROR -> s.copy(phase = MasteryPhase.EnvironmentError, lastResult = r)
    }

    private fun advanceSentence(s: MasterySession): MasterySession {
        val done = s.completedSentences + 1
        return if (s.isLastSentence) {
            s.copy(phase = MasteryPhase.LessonComplete, completedSentences = done, lastResult = null)
        } else {
            s.copy(
                phase = MasteryPhase.UnitPresentation,
                sentenceIndex = s.sentenceIndex + 1,
                unitIndex = 0,
                assembledUnits = 0,
                completedSentences = done,
                lastResult = null,
            )
        }
    }

    /** Apply the selected restart mode to a failed sentence (spec §7). */
    private fun applyRestart(s: MasterySession): MasterySession {
        val restarted = s.restartCount + 1
        return when (s.restartMode) {
            // A — keep the whole assembled sentence, just re-attempt the one-take.
            RestartMode.A -> s.copy(phase = MasteryPhase.SentenceReady, restartCount = restarted, lastResult = null)
            // B — rebuild only the last authored segment; falls back to full rebuild if none authored.
            RestartMode.B -> {
                val start = s.currentSentence.segmentStarts.lastOrNull { it < s.totalUnits } ?: 0
                s.copy(phase = MasteryPhase.UnitPresentation, unitIndex = start, assembledUnits = start, restartCount = restarted, lastResult = null)
            }
            // C — wipe it, rebuild from the first unit. The strict default.
            RestartMode.C -> s.copy(phase = MasteryPhase.UnitPresentation, unitIndex = 0, assembledUnits = 0, restartCount = restarted, lastResult = null)
        }
    }
}
