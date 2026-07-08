import { z } from "zod";
import { CefrLevelSchema } from "../enums.js";

const SpeakerSchema = z.object({
  id: z.string(),
  name: z.string(),
  role: z.string().optional(),
});

const TranscriptLineSchema = z.object({
  speakerId: z.string(),
  text: z.string(),
});

/** Dialogue — first-class content entity per spec §11, one of the 10 §10.1 MVP categories. */
export const DialogueSchema = z.object({
  schemaVersion: z.literal(1),
  key: z.string(),
  scenario: z.string(),
  speakers: z.array(SpeakerSchema).min(1),
  targetLevel: CefrLevelSchema.optional(),
  regionDialect: z.string().optional(),
  formality: z.enum(["formal", "informal"]).optional(),
  transcript: z.array(TranscriptLineSchema).min(1),
  translation: z.array(TranscriptLineSchema).min(1),
  keyVocabulary: z.array(z.string()).default([]),
  grammarNotes: z.string().optional(),
  slangRegisterNotes: z.string().optional(),
  audioGenerationInstructions: z.string().nullable().optional(),
});
export type Dialogue = z.infer<typeof DialogueSchema>;
