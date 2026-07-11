import Database from "@tauri-apps/plugin-sql";
import type { Database as CoreDatabase, SqlValue } from "@polyglotai/core-learning";

/**
 * The Tauri implementation of core-learning's Database port, over @tauri-apps/plugin-sql
 * (sqlx/SQLite). All core SQL uses `?` positional placeholders, which sqlx-SQLite binds in
 * order (verified by the Rust migration test) — the same style node:sqlite uses in unit tests.
 *
 * IMPORTANT — no app-level BEGIN/COMMIT here. The plugin's Rust side calls `pool.execute()`
 * per invocation (confirmed by reading tauri-plugin-sql's source), meaning every `execute`/
 * `select` call can be served by a *different* pooled connection with zero session affinity.
 * A `BEGIN` sent from here can land on one connection and simply sit there holding a lock
 * while later statements land on other connections — which is exactly what caused a real
 * "database is locked" (SQLITE_BUSY) error during onboarding's pack import. Every write in
 * core-learning is an idempotent upsert specifically so correctness never depends on
 * atomicity (plan risk 5) — so `transaction()` here is a no-op wrapper, not best-effort.
 *
 * Same reasoning applies to per-connection PRAGMAs (e.g. foreign_keys): a PRAGMA set via one
 * `execute()` call only affects the connection that happened to serve it, not the pool as a
 * whole. This plugin version exposes no pool-wide connection-setup hook, so FK enforcement is
 * not reliably active — a follow-up, not urgent (our own code never writes a dangling
 * reference; the DB-level constraint would only catch a bug we don't currently have).
 */
export class TauriDatabase implements CoreDatabase {
  private constructor(private readonly db: Database) {}

  static async connect(dbName = "polyglotai.db"): Promise<TauriDatabase> {
    // Migrations registered in Rust (lib.rs) run on load.
    const db = await Database.load(`sqlite:${dbName}`);
    return new TauriDatabase(db);
  }

  async run(sql: string, params: SqlValue[] = []): Promise<void> {
    await this.db.execute(sql, params);
  }

  async all<T = Record<string, SqlValue>>(sql: string, params: SqlValue[] = []): Promise<T[]> {
    return this.db.select<T[]>(sql, params);
  }

  async transaction<T>(fn: (tx: CoreDatabase) => Promise<T>): Promise<T> {
    return fn(this);
  }
}
