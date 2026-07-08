import { z } from "zod";

/** Every versioned entity carries these four fields — spec §21. */
export const EntityBaseSchema = z.object({
  id: z.string(),
  createdAt: z.string(), // ISO-8601
  updatedAt: z.string(), // ISO-8601
  schemaVersion: z.number().int().positive(),
});
export type EntityBase = z.infer<typeof EntityBaseSchema>;

/** Pack-derived content rows are keyed by (packId, itemKey) instead of a raw entity id
 * so re-importing a pack can upsert deterministically. */
export const PackContentRefSchema = z.object({
  packId: z.string(),
  itemKey: z.string(),
});
export type PackContentRef = z.infer<typeof PackContentRefSchema>;
