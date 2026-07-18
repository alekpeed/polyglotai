package com.polyglotai.android.mastery.model

import kotlinx.serialization.Serializable

/**
 * Content models for Progressive Sentence Mastery. These are pure Kotlin (no Android deps) so the
 * engine and its unit tests can use them on the JVM without an emulator. Bundled lessons are
 * authored as JSON in assets/mastery and decoded straight into these.
 */

@Serializable
enum class UnitType { WORD, CONTRACTION, CLITIC_GROUP, FIXED_PHRASE, COMPOUND, SCRIPT_UNIT }

/** Restart strictness for a failed full-sentence attempt (spec §7). D and E are deferred. */
@Serializable
enum class RestartMode(val label: String) {
    A("Repeat sentence"),
    B("Rebuild segment"),
    C("Rebuild full"),
}

@Serializable
data class ComprehensionCheck(
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
)

@Serializable
data class LearningUnit(
    val id: String,
    val position: Int,
    val displayText: String,
    val spokenText: String = displayText,
    val translation: String,
    val unitType: UnitType = UnitType.WORD,
    val comprehension: ComprehensionCheck? = null,
    /** Optional per-unit override of the grading profile's unit threshold. */
    val gradingThreshold: Double? = null,
)

@Serializable
data class MasterySentence(
    val id: String,
    val targetText: String,
    val translation: String,
    val units: List<LearningUnit>,
    val acceptedVariants: List<String> = emptyList(),
    /** Unit indices where a clause/segment begins, used by Mode B to rebuild only the failed
     *  segment. Empty means the whole sentence is one segment (Mode B then behaves like C). */
    val segmentStarts: List<Int> = emptyList(),
    val grammarTags: List<String> = emptyList(),
    val vocabularyTags: List<String> = emptyList(),
    val difficulty: Int = 1,
)

@Serializable
data class MasteryLesson(
    val id: String,
    val title: String,
    val language: String,
    val locale: String,
    val level: String = "A1",
    val defaultRestartMode: RestartMode = RestartMode.C,
    val sentences: List<MasterySentence>,
)

/**
 * Per-language, editable grading thresholds (spec §4.4). Kept as data so a calibration tool can
 * change them without code changes. The mock grader ignores most of these; the real Azure adapter
 * will honor them.
 */
@Serializable
data class GradingProfile(
    val id: String = "default",
    val language: String = "pt",
    val locale: String = "pt-BR",
    val provider: String = "mock",
    val unitAccuracyThreshold: Double = 0.6,
    val sentenceAccuracyThreshold: Double = 0.7,
    val minimumCompleteness: Double = 1.0,
    val allowInsertions: Boolean = false,
    val allowOmissions: Boolean = false,
    val enableProsody: Boolean = false,
    val uncertainConfidenceThreshold: Double = 0.4,
)
