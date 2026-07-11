// Web-preview-only shim for @tauri-apps/plugin-sql (design/demo QA build — see
// vite.preview.config.ts). The real app talks to SQLite through Tauri's native IPC bridge,
// which only exists inside an actual Tauri window; a browser tab has no such bridge. This
// swaps in an in-browser SQLite (sql.js/WASM), migrated with the real migration files, and
// persisted to localStorage so a visitor's demo data survives a page reload — but it is
// never wired into the real desktop build (vite.config.ts has no reference to this file).
import initSqlJs, { type Database as SqlJsDb } from "sql.js";
import sqlWasmUrl from "sql.js/dist/sql-wasm.wasm?url";
import m1 from "../../src-tauri/migrations/0001_initial.sql?raw";
import m2 from "../../src-tauri/migrations/0002_seed_feature_flags.sql?raw";
import m3 from "../../src-tauri/migrations/0003_pronunciation_rules.sql?raw";
import m4 from "../../src-tauri/migrations/0004_grammar_ladders.sql?raw";

type SqlValue = string | number | boolean | null;

const STORAGE_KEY = "polyglotai-preview-db-v1";

// The wasm import above is inlined as a base64 data: URI (vite.preview.config.ts sets
// assetsInlineLimit sky-high). Handing that URI to sql.js's `locateFile` makes it `fetch()`
// the wasm — which sandboxed hosts (e.g. Claude Artifacts) block, failing with "both async
// and sync fetching of the wasm failed". Decoding the data URI synchronously into bytes and
// passing `wasmBinary` skips fetch entirely.
function dataUriToBytes(dataUri: string): Uint8Array {
  const binary = atob(dataUri.slice(dataUri.indexOf(",") + 1));
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
  return bytes;
}

let sqlJsPromise: Promise<Awaited<ReturnType<typeof initSqlJs>>> | null = null;
function getSqlJs() {
  sqlJsPromise ??= initSqlJs({ wasmBinary: dataUriToBytes(sqlWasmUrl).buffer as ArrayBuffer });
  return sqlJsPromise;
}

function loadSaved(): Uint8Array | undefined {
  try {
    const b64 = localStorage.getItem(STORAGE_KEY);
    if (!b64) return undefined;
    const binary = atob(b64);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
    return bytes;
  } catch {
    return undefined;
  }
}

function save(db: SqlJsDb): void {
  try {
    const bytes = db.export();
    let binary = "";
    for (let i = 0; i < bytes.length; i++) binary += String.fromCharCode(bytes[i]!);
    localStorage.setItem(STORAGE_KEY, btoa(binary));
  } catch {
    // Preview-only convenience; if storage is full/unavailable the demo still works in-memory.
  }
}

export default class Database {
  private constructor(private readonly db: SqlJsDb) {}

  static async load(_connString: string): Promise<Database> {
    const SQL = await getSqlJs();
    const saved = loadSaved();
    if (saved) return new Database(new SQL.Database(saved));

    const db = new SQL.Database();
    for (const migration of [m1, m2, m3, m4]) db.exec(migration);
    save(db);
    return new Database(db);
  }

  async execute(sql: string, params: SqlValue[] = []): Promise<{ rowsAffected: number; lastInsertId?: number }> {
    this.db.run(sql, params as (string | number | Uint8Array | null)[]);
    save(this.db);
    return { rowsAffected: this.db.getRowsModified() };
  }

  async select<T = Record<string, SqlValue>>(sql: string, params: SqlValue[] = []): Promise<T[]> {
    const stmt = this.db.prepare(sql);
    stmt.bind(params as (string | number | Uint8Array | null)[]);
    const rows: T[] = [];
    while (stmt.step()) rows.push(stmt.getAsObject() as T);
    stmt.free();
    return rows;
  }
}
