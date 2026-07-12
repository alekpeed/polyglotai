import { z } from "zod";
import { CefrLevelSchema } from "../enums.js";

/**
 * A short freeform cultural/etiquette note — distinct from Lesson (which is quiz/drill-shaped).
 * Reference reading, not SRS content: no schedule, no grading. Rendered straight from the
 * in-memory LoadedPack (see language-pack-sdk), never imported into the review DB.
 */
export const CultureNoteSchema = z.object({
  schemaVersion: z.literal(1),
  key: z.string(),
  title: z.string(),
  bodyMd: z.string(),
  tags: z.array(z.string()).default([]),
  cefr: CefrLevelSchema.optional(),
});
export type CultureNote = z.infer<typeof CultureNoteSchema>;
