package com.polyglotai.android.mastery

import android.content.Context
import androidx.room.Room
import com.polyglotai.android.mastery.data.MasteryAttempt
import com.polyglotai.android.mastery.data.MasteryContentRepository
import com.polyglotai.android.mastery.data.MasteryDatabase
import com.polyglotai.android.mastery.engine.MasteryEngine
import com.polyglotai.android.mastery.engine.MasterySession
import com.polyglotai.android.mastery.grade.MockPronunciationGrader
import com.polyglotai.android.mastery.grade.PronunciationGrader
import com.polyglotai.android.mastery.grade.PronunciationResult
import com.polyglotai.android.mastery.model.GradingProfile
import com.polyglotai.android.mastery.voice.MockVoicePlaybackProvider
import com.polyglotai.android.mastery.voice.VoicePlaybackProvider

/**
 * Manual dependency wiring for the Progressive Sentence Mastery engine — kept separate from the
 * app's AppContainer so the mode stays an independent module (spec §1, §3). Milestone 1 wires the
 * mock grader and placeholder voice; swapping to Azure/OpenAI later is a one-line change here plus a
 * new adapter, with nothing downstream affected.
 */
class MasteryContainer(context: Context) {
    private val app = context.applicationContext

    private val db = Room.databaseBuilder(app, MasteryDatabase::class.java, "polyglotai_mastery.db").build()

    val content = MasteryContentRepository(app)
    val engine = MasteryEngine()

    // The replaceable seams. Milestone 3 / §11 substitute real providers here.
    val grader: PronunciationGrader = MockPronunciationGrader()
    val voice: VoicePlaybackProvider = MockVoicePlaybackProvider()

    /** Default grading profile; a calibration tool edits these per language in a later milestone. */
    val defaultProfile = GradingProfile()

    /** Persist one attempt (spec §4.5). Errors here never interrupt the lesson. */
    suspend fun logAttempt(
        sessionId: String,
        session: MasterySession,
        attemptType: String,
        result: PronunciationResult,
        manualOverride: Boolean = false,
        overrideReason: String? = null,
    ) {
        runCatching {
            db.attemptDao().insert(
                MasteryAttempt(
                    sessionId = sessionId,
                    lessonId = session.lesson.id,
                    sentenceId = session.currentSentence.id,
                    unitId = if (attemptType == "unit") session.currentUnit?.id else null,
                    attemptType = attemptType,
                    status = result.status.name,
                    recognizedText = result.recognizedText.ifBlank { null },
                    score = result.overallScore,
                    confidence = result.confidence,
                    errorTypes = result.errorTypes.joinToString(",").ifBlank { null },
                    provider = result.providerMetadata["provider"] ?: "mock",
                    restartMode = session.restartMode.name,
                    restartCount = session.restartCount,
                    manualOverride = manualOverride,
                    overrideReason = overrideReason,
                    createdAtMillis = System.currentTimeMillis(),
                ),
            )
        }
    }

    suspend fun overrideCount(): Int = runCatching { db.attemptDao().overrideCount() }.getOrDefault(0)
}
