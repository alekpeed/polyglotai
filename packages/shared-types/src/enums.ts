import { z } from "zod";

/** Register labels — spec §9. Every phrase/word/idiom/slang/profanity item carries one. */
export const RegisterSchema = z.enum([
  "formal",
  "neutral",
  "informal",
  "vulgar",
  "obscene",
  "offensive",
  "affectionate",
  "flirtatious",
  "sarcastic",
  "humorous",
  "childish",
  "dated",
  "internet",
  "regional",
  "professional",
  "academic",
  "street",
  "hostile",
  "taboo",
  "dangerous",
]);
export type Register = z.infer<typeof RegisterSchema>;

/** Severity scale — spec §13. 1 = harmless informal, 7 = severe taboo / socially dangerous. */
export const SeveritySchema = z.union([
  z.literal(1),
  z.literal(2),
  z.literal(3),
  z.literal(4),
  z.literal(5),
  z.literal(6),
  z.literal(7),
]);
export type Severity = z.infer<typeof SeveritySchema>;

export const LearnerShouldUseSchema = z.enum(["use", "recognize-only", "avoid"]);
export type LearnerShouldUse = z.infer<typeof LearnerShouldUseSchema>;

/** Distinguishes the "300 vocabulary items" vs "75 core phrases" categories (spec §10.1)
 * within a single vocabulary_items table / schema. */
export const VocabEntryTypeSchema = z.enum(["word", "phrase", "chunk"]);
export type VocabEntryType = z.infer<typeof VocabEntryTypeSchema>;

export const RealSpeechKindSchema = z.enum([
  "slang",
  "profanity",
  "idiom",
  "euphemism",
  "taboo",
]);
export type RealSpeechKind = z.infer<typeof RealSpeechKindSchema>;

export const LessonTypeSchema = z.enum([
  "vocabulary",
  "grammar",
  "pronunciation_drill",
  "listening_exercise",
  "dictation",
  "dialogue_analysis",
  "roleplay",
  "reading_passage",
  "writing_prompt",
  "translation_drill",
  "slang_breakdown",
  "idiom_lesson",
  "real_world_scenario",
  "review_session",
  "assessment",
]);
export type LessonType = z.infer<typeof LessonTypeSchema>;

/** First-time onboarding goal selection — spec §6 step 4. */
export const LearnerGoalSchema = z.enum([
  "travel",
  "conversation",
  "fluency",
  "tutoring",
  "professional",
  "dating_social",
  "media_comprehension",
  "custom",
]);
export type LearnerGoal = z.infer<typeof LearnerGoalSchema>;

/** First-time onboarding real-speech level selection — spec §6 step 6. Drives the default
 * severity ceiling the learner is shown by default (overridable via slang_severity_override). */
export const RealSpeechLevelSchema = z.enum([
  "standard",
  "informal",
  "slang",
  "profanity",
]);
export type RealSpeechLevel = z.infer<typeof RealSpeechLevelSchema>;

export const CorrectionStrictnessSchema = z.enum(["lenient", "balanced", "strict"]);
export type CorrectionStrictness = z.infer<typeof CorrectionStrictnessSchema>;

/** Default severity ceiling per real-speech level (§6 step 6 → §13 severity scale). */
export const REAL_SPEECH_LEVEL_DEFAULT_SEVERITY: Record<RealSpeechLevel, Severity> = {
  standard: 1,
  informal: 3,
  slang: 5,
  profanity: 7,
};

export const CefrLevelSchema = z.enum(["A1", "A2", "B1", "B2", "C1", "C2"]);
export type CefrLevel = z.infer<typeof CefrLevelSchema>;
