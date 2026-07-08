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

/**
 * Fresh id for user-owned rows (profiles, review items, conversations) that have no natural
 * key. Uses Web Crypto's randomUUID (available in Node and the Tauri webview). The plan calls
 * for UUIDv7's sortability; v4 is the MVP stand-in behind this single helper so it can be
 * swapped without touching call sites.
 */
export function newId(): string {
  return crypto.randomUUID();
}
