import { z } from "zod";

/** AI tutor modes — spec §14. */
export const TutorModeSchema = z.enum([
  "grammar-teacher",
  "conversation-partner",
  "pronunciation-coach",
  "slang-explainer",
  "examiner",
  "writing-editor",
  "roleplay-partner",
  "travel-coach",
  "nightlife-social-coach",
  "workplace-coach",
  "media-comprehension-coach",
]);
export type TutorMode = z.infer<typeof TutorModeSchema>;

/** AI prompt template — spec §11. */
export const AiPromptTemplateSchema = z.object({
  schemaVersion: z.literal(1),
  key: z.string(),
  mode: TutorModeSchema,
  template: z.string(),
  outputSchemaRef: z.string().optional(),
});
export type AiPromptTemplate = z.infer<typeof AiPromptTemplateSchema>;

/**
 * Structured correction output — spec §6's first-session correction system:
 * corrected version, natural version, formal version, casual version, slang/native version
 * when appropriate, grammar explanation, register explanation, pronunciation notes, and
 * future review items to seed into SRS.
 */
export const AiCorrectionSchema = z.object({
  corrected: z.string(),
  literal: z.string().optional(),
  natural: z.string().optional(),
  formal: z.string().optional(),
  casual: z.string().optional(),
  slangNative: z.string().optional(),
  grammarExplanation: z.string().optional(),
  registerExplanation: z.string().optional(),
  pronunciationNotes: z.string().optional(),
  futureReviewItemKeys: z.array(z.string()).default([]),
});
export type AiCorrection = z.infer<typeof AiCorrectionSchema>;
