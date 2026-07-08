import type { SqlValue } from "./database.js";

export type Row = Record<string, SqlValue>;

/**
 * Builds an idempotent upsert: INSERT ... ON CONFLICT(<conflict>) DO UPDATE SET ... using `?`
 * placeholders. On conflict, every column is updated to the incoming value EXCEPT those in
 * `immutable` (typically the id, the conflict-target columns, and created_at — things that
 * must survive re-import so review history keyed by content id stays valid; plan §4).
 */
export function buildUpsert(
  table: string,
  row: Row,
  conflict: string[],
  immutable: string[],
): { sql: string; params: SqlValue[] } {
  const cols = Object.keys(row);
  if (cols.length === 0) throw new Error(`buildUpsert(${table}): empty row`);

  const placeholders = cols.map(() => "?").join(", ");
  const noUpdate = new Set([...conflict, ...immutable]);
  const updates = cols
    .filter((c) => !noUpdate.has(c))
    .map((c) => `${c} = excluded.${c}`)
    .join(", ");

  const conflictClause =
    updates.length > 0
      ? `ON CONFLICT(${conflict.join(", ")}) DO UPDATE SET ${updates}`
      : `ON CONFLICT(${conflict.join(", ")}) DO NOTHING`;

  const sql = `INSERT INTO ${table} (${cols.join(", ")}) VALUES (${placeholders}) ${conflictClause}`;
  return { sql, params: cols.map((c) => row[c] ?? null) };
}

/**
 * Deletes rows of `table` for `packId` whose item_key is NOT in `keepKeys` — removing content
 * that a pack dropped between versions. Runs AFTER upserting current items, so at no point are
 * current rows missing (safe ordering under best-effort atomicity).
 */
export function buildDeleteStale(
  table: string,
  packId: string,
  keepKeys: string[],
): { sql: string; params: SqlValue[] } {
  if (keepKeys.length === 0) {
    return { sql: `DELETE FROM ${table} WHERE pack_id = ?`, params: [packId] };
  }
  const placeholders = keepKeys.map(() => "?").join(", ");
  return {
    sql: `DELETE FROM ${table} WHERE pack_id = ? AND item_key NOT IN (${placeholders})`,
    params: [packId, ...keepKeys],
  };
}
