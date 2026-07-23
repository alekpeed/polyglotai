package com.polyglotai.android.mastery.grade

import com.polyglotai.android.mastery.model.GradingProfile
import com.polyglotai.android.mastery.model.LearningUnit
import com.polyglotai.android.mastery.model.MasterySentence
import java.io.File

/**
 * Normalized grading vocabulary. Everything downstream of a grader depends only on these types —
 * never on Azure's, OpenAI's, or any provider's native response shape (spec §3.6). Swapping the
 * provider means writing a new adapter that produces a [PronunciationResult]; nothing else changes.
 */

enum class GradeStatus { PASS, FAIL, UNCERTAIN, ENVIRONMENT_ERROR, SERVICE_ERROR }

/** Per-unit detail inside a sentence grade — which unit, what was heard, its score, pass flag. */
data class UnitScore(
    val unitId: String,
    val recognized: String,
    val score: Double,
    val ok: Boolean,
)

data class PronunciationResult(
    val status: GradeStatus,
    val recognizedText: String = "",
    val overallScore: Double = 0.0,
    val confidence: Double = 0.0,
    val unitResults: List<UnitScore> = emptyList(),
    val errorTypes: List<String> = emptyList(),
    val providerMetadata: Map<String, String> = emptyMap(),
) {
    val isError: Boolean get() = status == GradeStatus.SERVICE_ERROR || status == GradeStatus.ENVIRONMENT_ERROR
}

/**
 * The one seam the real graders slot into. The engine and UI hold a [PronunciationGrader] and never
 * know which provider is behind it. Milestone 1 ships [MockPronunciationGrader]; Milestone 3 adds an
 * Azure adapter that talks to the backend gateway and returns the same [PronunciationResult].
 */
interface PronunciationGrader {
    suspend fun gradeUnit(audio: File?, expected: LearningUnit, locale: String, profile: GradingProfile): PronunciationResult

    suspend fun gradeSentence(audio: File?, expected: MasterySentence, locale: String, profile: GradingProfile): PronunciationResult
}

/**
 * Stand-in grader for the structural build. It does not evaluate audio — it returns a configurable
 * result so the whole loop (including the fail/restart/override branches) can be exercised end to
 * end before any real speech grading exists. The UI drives the outcome via simulate controls; this
 * default returns PASS so an un-driven path still flows.
 */
class MockPronunciationGrader(var nextStatus: GradeStatus = GradeStatus.PASS) : PronunciationGrader {
    override suspend fun gradeUnit(audio: File?, expected: LearningUnit, locale: String, profile: GradingProfile): PronunciationResult =
        result(nextStatus, expected.spokenText)

    override suspend fun gradeSentence(audio: File?, expected: MasterySentence, locale: String, profile: GradingProfile): PronunciationResult =
        result(nextStatus, expected.targetText)

    private fun result(status: GradeStatus, text: String): PronunciationResult {
        val pass = status == GradeStatus.PASS
        return PronunciationResult(
            status = status,
            recognizedText = if (pass) text else "",
            overallScore = if (pass) 0.95 else 0.3,
            confidence = if (status == GradeStatus.UNCERTAIN) 0.35 else 0.9,
            providerMetadata = mapOf("provider" to "mock"),
        )
    }
}
