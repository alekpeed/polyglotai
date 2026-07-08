/**
 * Deterministic id for pack-derived content rows: stable across re-imports so that
 * review_items.content_id references survive a pack update (plan §4). item_key is unique
 * within a pack, so (packId, itemKey) uniquely identifies the row.
 *
 * User-owned rows (profiles, review items, conversations) get random UUIDv7s instead — they
 * have no natural key and must be globally unique.
 */
export function contentId(packId: string, itemKey: string): string {
  return `${packId}::${itemKey}`;
}
