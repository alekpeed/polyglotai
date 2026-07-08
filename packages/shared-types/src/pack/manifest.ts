import { z } from "zod";
import { CefrLevelSchema } from "../enums.js";

const DialectSchema = z.object({
  id: z.string(),
  name: z.string(),
});

const ContentFileListSchema = z.array(z.string()).default([]);

/**
 * Language pack manifest — spec §9. The entry point for a pack: declares versions,
 * dialect/regional-variant inheritance (basePack), and the list of content files to load.
 */
export const ManifestSchema = z.object({
  schemaVersion: z.literal(1),
  id: z.string(),
  name: z.string(),
  languageCode: z.string(), // BCP-47, e.g. "pt-BR"
  packVersion: z.string(), // semver
  basePack: z.string().nullable().default(null),
  authors: z.array(z.string()).default([]),
  license: z.string().optional(),
  dialects: z.array(DialectSchema).default([]),
  defaultDialect: z.string().optional(),
  cefrRange: z.tuple([CefrLevelSchema, CefrLevelSchema]).optional(),
  featureFlags: z.record(z.string(), z.boolean()).default({}),
  contents: z
    .object({
      vocabulary: ContentFileListSchema,
      phrases: ContentFileListSchema,
      grammar: ContentFileListSchema,
      slang: ContentFileListSchema,
      profanity: ContentFileListSchema,
      idioms: ContentFileListSchema,
      pronunciation: ContentFileListSchema,
      dialogues: ContentFileListSchema,
      lessons: ContentFileListSchema,
      assessments: ContentFileListSchema,
      aiPrompts: ContentFileListSchema,
      culture: ContentFileListSchema,
    })
    .partial(),
  checksums: z
    .object({
      algo: z.literal("sha256"),
      files: z.record(z.string(), z.string()),
    })
    .optional(),
});
export type Manifest = z.infer<typeof ManifestSchema>;
