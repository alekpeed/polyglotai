import { z } from "zod";
import {
  CefrLevelSchema,
  CorrectionStrictnessSchema,
  LearnerGoalSchema,
  RealSpeechLevelSchema,
  SeveritySchema,
} from "./enums.js";

/**
 * Local learner profile — spec §6 first-time onboarding flow (steps 3-6) plus §15
 * proficiency-model fields set later by the diagnostic and ongoing use.
 */
export const LearnerProfileSchema = z.object({
  id: z.string(),
  displayName: z.string(),
  activePackId: z.string().nullable().optional(),
  goal: LearnerGoalSchema.optional(),
  targetDialect: z.string().optional(),
  realSpeechLevel: RealSpeechLevelSchema.default("informal"),
  slangSeverityOverride: SeveritySchema.nullable().optional(),
  cefrEstimate: CefrLevelSchema.nullable().optional(),
  correctionStrictness: CorrectionStrictnessSchema.default("balanced"),
  settings: z.record(z.string(), z.unknown()).default({}),
  schemaVersion: z.literal(1),
  createdAt: z.string(),
  updatedAt: z.string(),
});
export type LearnerProfile = z.infer<typeof LearnerProfileSchema>;
