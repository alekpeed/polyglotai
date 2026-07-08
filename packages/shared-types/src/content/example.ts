import { z } from "zod";
import { RegisterSchema } from "../enums.js";

/** Shared shape for example sentences attached to vocabulary/grammar/real-speech items. */
export const ExampleSchema = z.object({
  text: z.string(),
  translation: z.string(),
  register: RegisterSchema.optional(),
});
export type Example = z.infer<typeof ExampleSchema>;
