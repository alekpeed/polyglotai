import { z } from "zod";

const MinimalPairSchema = z.object({
  a: z.string(),
  b: z.string(),
  note: z.string().optional(),
});

/** Pronunciation drill/rule — spec §11, §17 Phase 1 (recognition-level, no phoneme scoring yet). */
export const PronunciationRuleSchema = z.object({
  schemaVersion: z.literal(1),
  key: z.string(),
  grapheme: z.string(),
  ipa: z.string(),
  description: z.string(),
  minimalPairs: z.array(MinimalPairSchema).default([]),
});
export type PronunciationRule = z.infer<typeof PronunciationRuleSchema>;
