import { z } from "zod";
import { RegisterSchema } from "../enums.js";

/** Shared shape for example sentences attached to vocabulary/grammar/real-speech items. */
export const ExampleSchema = z.object({
  text: z.string(),
  /** Reading/romanization for `text` in non-Latin-script packs — see VocabularyItem. */
  reading: z.string().optional(),
  romaji: z.string().optional(),
  translation: z.string(),
  register: RegisterSchema.optional(),
});
export type Example = z.infer<typeof ExampleSchema>;
