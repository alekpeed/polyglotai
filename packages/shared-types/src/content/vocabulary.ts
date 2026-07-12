import { z } from "zod";
import { CefrLevelSchema, RegisterSchema, VocabEntryTypeSchema } from "../enums.js";
import { ExampleSchema } from "./example.js";

/**
 * Vocabulary item — spec §11. entryType distinguishes the "300 vocabulary items" vs
 * "75 core phrases" content-volume categories (§10.1) within one shared schema, since §11
 * itself lists a single "word/phrase" field rather than two separate item types.
 */
export const VocabularyItemSchema = z.object({
  schemaVersion: z.literal(1),
  key: z.string(),
  entryType: VocabEntryTypeSchema.default("word"),
  lemma: z.string(),
  /** Reading for non-Latin-script lemmas (e.g. Japanese hiragana for a kanji lemma). */
  reading: z.string().optional(),
  /** Romanization, alongside `reading` — kana-only readings don't help a learner who can't
   * read kana yet. */
  romaji: z.string().optional(),
  translation: z.string(),
  literalMeaning: z.string().optional(),
  naturalMeaning: z.string().optional(),
  partOfSpeech: z.string().optional(),
  cefr: CefrLevelSchema.optional(),
  frequencyRank: z.number().int().positive().optional(),
  ipa: z.string().optional(),
  audioText: z.string().optional(),
  pronunciationNotes: z.string().optional(),
  gender: z.string().optional(),
  register: RegisterSchema.optional(),
  tags: z.array(z.string()).default([]),
  examples: z.array(ExampleSchema).default([]),
});
export type VocabularyItem = z.infer<typeof VocabularyItemSchema>;
