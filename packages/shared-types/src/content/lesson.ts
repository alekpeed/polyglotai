import { z } from "zod";
import { CefrLevelSchema, LessonTypeSchema } from "../enums.js";

const TeachStepSchema = z.object({
  type: z.literal("teach"),
  vocabRefs: z.array(z.string()).default([]),
});

const QuizStepSchema = z.object({
  type: z.literal("quiz"),
  prompt: z.string(),
  answer: z.string(),
});

const LessonStepSchema = z.discriminatedUnion("type", [TeachStepSchema, QuizStepSchema]);

/**
 * Lesson — spec §10 lesson types. lessonType's enum (see enums.ts) covers the §10.1
 * pronunciation-drill / listening / writing / roleplay / assessment content categories,
 * so those are lessons rather than separate tables.
 */
export const LessonSchema = z.object({
  schemaVersion: z.literal(1),
  key: z.string(),
  lessonType: LessonTypeSchema,
  title: z.string(),
  cefr: CefrLevelSchema.optional(),
  sequence: z.number().int().optional(),
  dialogueRef: z.string().nullable().optional(),
  body: z.object({
    steps: z.array(LessonStepSchema),
  }),
});
export type Lesson = z.infer<typeof LessonSchema>;
