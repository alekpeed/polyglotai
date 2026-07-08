/** Values the DB layer round-trips. JSON/arrays are stored as stringified TEXT columns. */
export type SqlValue = string | number | null;

/**
 * The `Database` port — plan §2/§3's DB-access boundary. core-learning writes SQL through
 * this interface so it stays platform-free: the Tauri app supplies an adapter over
 * `@tauri-apps/plugin-sql`, tests supply a node:sqlite adapter, and Android supplies its own
 * later. All SQL uses `?` positional placeholders (valid natively in SQLite → works across
 * sqlx and node:sqlite / better-sqlite3).
 */
export interface Database {
  /** Execute a statement that returns no rows (INSERT/UPDATE/DELETE/DDL). */
  run(sql: string, params?: SqlValue[]): Promise<void>;
  /** Execute a query and return its rows as plain objects. */
  all<T = Record<string, SqlValue>>(sql: string, params?: SqlValue[]): Promise<T[]>;
  /**
   * Run `fn` as an atomic batch where the adapter supports it. Callers rely on ordering and
   * idempotency (all writes are upserts), not on rollback — because tauri-plugin-sql's
   * connection pool can't guarantee cross-call transactions from JS (plan risk 5). Adapters
   * that CAN be atomic (node:sqlite, a single-connection SQLite) should be.
   */
  transaction<T>(fn: (tx: Database) => Promise<T>): Promise<T>;
}
