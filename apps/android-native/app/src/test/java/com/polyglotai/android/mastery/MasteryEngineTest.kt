package com.polyglotai.android.mastery

import com.polyglotai.android.mastery.engine.MasteryEngine
import com.polyglotai.android.mastery.engine.MasteryEvent
import com.polyglotai.android.mastery.engine.MasteryPhase
import com.polyglotai.android.mastery.engine.MasterySession
import com.polyglotai.android.mastery.grade.GradeStatus
import com.polyglotai.android.mastery.grade.PronunciationResult
import com.polyglotai.android.mastery.model.ComprehensionCheck
import com.polyglotai.android.mastery.model.LearningUnit
import com.polyglotai.android.mastery.model.MasteryLesson
import com.polyglotai.android.mastery.model.MasterySentence
import com.polyglotai.android.mastery.model.RestartMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure-JVM tests for the state machine — no Android, no mic, no network (spec §15). */
class MasteryEngineTest {

    private val engine = MasteryEngine()
    private val pass = PronunciationResult(GradeStatus.PASS)
    private val fail = PronunciationResult(GradeStatus.FAIL)

    private fun lesson(mode: RestartMode = RestartMode.C, withComprehension: Boolean = false): MasteryLesson =
        MasteryLesson(
            id = "t", title = "t", language = "pt", locale = "pt-BR", defaultRestartMode = mode,
            sentences = listOf(
                MasterySentence(
                    id = "s1", targetText = "A B C", translation = "", segmentStarts = listOf(0, 2),
                    units = listOf(
                        LearningUnit("u1", 0, "A", translation = "a", comprehension = if (withComprehension) ComprehensionCheck("?", listOf("a", "b"), 0) else null),
                        LearningUnit("u2", 1, "B", translation = "b"),
                        LearningUnit("u3", 2, "C", translation = "c"),
                    ),
                ),
                MasterySentence(
                    id = "s2", targetText = "D", translation = "",
                    units = listOf(LearningUnit("u4", 0, "D", translation = "d")),
                ),
            ),
        )

    /** Drive one unit through play → record → grade with the given result. */
    private fun gradeUnit(s: MasterySession, result: PronunciationResult): MasterySession {
        var x = engine.reduce(s, MasteryEvent.PlayAudio)
        x = engine.reduce(x, MasteryEvent.AudioFinished)
        x = engine.reduce(x, MasteryEvent.RecordingCaptured)
        return engine.reduce(x, MasteryEvent.UnitGraded(result))
    }

    private fun gradeSentence(s: MasterySession, result: PronunciationResult): MasterySession {
        var x = engine.reduce(s, MasteryEvent.PlayAudio)
        x = engine.reduce(x, MasteryEvent.AudioFinished)
        x = engine.reduce(x, MasteryEvent.RecordingCaptured)
        return engine.reduce(x, MasteryEvent.SentenceGraded(result))
    }

    @Test
    fun `full happy path completes the lesson`() {
        var s = engine.start(lesson())
        assertEquals(MasteryPhase.UnitPresentation, s.phase)

        // three units of sentence 1
        s = gradeUnit(s, pass); assertEquals(MasteryPhase.UnitPresentation, s.phase); assertEquals(1, s.assembledUnits)
        s = gradeUnit(s, pass); assertEquals(2, s.assembledUnits)
        s = gradeUnit(s, pass); assertEquals(MasteryPhase.SentenceReady, s.phase); assertEquals(3, s.assembledUnits)

        s = gradeSentence(s, pass); assertEquals(MasteryPhase.SentencePass, s.phase)
        s = engine.reduce(s, MasteryEvent.Advance)
        assertEquals(MasteryPhase.UnitPresentation, s.phase); assertEquals(1, s.sentenceIndex); assertEquals(0, s.assembledUnits)

        // sentence 2 (single unit)
        s = gradeUnit(s, pass); assertEquals(MasteryPhase.SentenceReady, s.phase)
        s = gradeSentence(s, pass)
        s = engine.reduce(s, MasteryEvent.Advance)
        assertEquals(MasteryPhase.LessonComplete, s.phase)
        assertEquals(2, s.completedSentences)
    }

    @Test
    fun `unit fail sends back to the same unit`() {
        var s = engine.start(lesson())
        s = gradeUnit(s, fail)
        assertEquals(MasteryPhase.UnitResult, s.phase)
        s = engine.reduce(s, MasteryEvent.Retry)
        assertEquals(MasteryPhase.UnitPresentation, s.phase)
        assertEquals(0, s.unitIndex); assertEquals(0, s.assembledUnits)
    }

    @Test
    fun `wrong comprehension repeats the unit without adding it`() {
        var s = engine.start(lesson(withComprehension = true))
        s = gradeUnit(s, pass)
        assertEquals(MasteryPhase.UnitComprehension, s.phase)
        s = engine.reduce(s, MasteryEvent.ComprehensionAnswered(false))
        assertEquals(MasteryPhase.UnitPresentation, s.phase); assertEquals(0, s.assembledUnits)
        // correct answer adds it
        s = gradeUnit(s, pass)
        s = engine.reduce(s, MasteryEvent.ComprehensionAnswered(true))
        assertEquals(1, s.assembledUnits)
    }

    @Test
    fun `mode C wipes the whole sentence on failure`() {
        var s = engine.start(lesson(RestartMode.C))
        s = gradeUnit(s, pass); s = gradeUnit(s, pass); s = gradeUnit(s, pass)
        s = gradeSentence(s, fail)
        assertEquals(MasteryPhase.SentenceFail, s.phase)
        s = engine.reduce(s, MasteryEvent.Retry)
        assertEquals(MasteryPhase.UnitPresentation, s.phase); assertEquals(0, s.unitIndex); assertEquals(0, s.assembledUnits)
        assertEquals(1, s.restartCount)
    }

    @Test
    fun `mode A keeps the assembled sentence on failure`() {
        var s = engine.start(lesson(RestartMode.A))
        s = gradeUnit(s, pass); s = gradeUnit(s, pass); s = gradeUnit(s, pass)
        s = gradeSentence(s, fail)
        s = engine.reduce(s, MasteryEvent.Retry)
        assertEquals(MasteryPhase.SentenceReady, s.phase); assertEquals(3, s.assembledUnits)
    }

    @Test
    fun `mode B rebuilds from the last segment start`() {
        var s = engine.start(lesson(RestartMode.B)) // segmentStarts = [0, 2]
        s = gradeUnit(s, pass); s = gradeUnit(s, pass); s = gradeUnit(s, pass)
        s = gradeSentence(s, fail)
        s = engine.reduce(s, MasteryEvent.Retry)
        assertEquals(MasteryPhase.UnitPresentation, s.phase); assertEquals(2, s.unitIndex); assertEquals(2, s.assembledUnits)
    }

    @Test
    fun `override accepts a failed unit and advances`() {
        var s = engine.start(lesson())
        s = gradeUnit(s, fail)
        assertEquals(MasteryPhase.UnitResult, s.phase)
        s = engine.reduce(s, MasteryEvent.OverrideAccepted)
        assertEquals(1, s.assembledUnits)
    }

    @Test
    fun `environment error is not a learner failure`() {
        var s = engine.start(lesson())
        s = gradeUnit(s, PronunciationResult(GradeStatus.ENVIRONMENT_ERROR))
        assertEquals(MasteryPhase.EnvironmentError, s.phase)
        assertEquals(0, s.assembledUnits)
        s = engine.reduce(s, MasteryEvent.Retry)
        assertEquals(MasteryPhase.UnitPresentation, s.phase)
    }

    @Test
    fun `pause and resume restores the prior phase`() {
        var s = engine.start(lesson())
        s = engine.reduce(s, MasteryEvent.PlayAudio)
        s = engine.reduce(s, MasteryEvent.AudioFinished)
        assertEquals(MasteryPhase.UnitWaitingForSpeech, s.phase)
        s = engine.reduce(s, MasteryEvent.Pause)
        assertEquals(MasteryPhase.Paused, s.phase)
        s = engine.reduce(s, MasteryEvent.Resume)
        assertEquals(MasteryPhase.UnitWaitingForSpeech, s.phase)
    }

    @Test
    fun `selecting a mode updates it without losing progress`() {
        var s = engine.start(lesson(RestartMode.C))
        s = gradeUnit(s, pass)
        s = engine.reduce(s, MasteryEvent.SelectMode(RestartMode.A))
        assertEquals(RestartMode.A, s.restartMode)
        assertEquals(1, s.assembledUnits)
        assertTrue(s.phase == MasteryPhase.UnitPresentation)
    }
}
