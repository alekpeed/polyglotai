import type { Database } from "@polyglotai/core-learning";
import { loadPack, type LoadedPack } from "@polyglotai/language-pack-sdk";
import { TauriDatabase } from "../db/tauriDatabase";
import { BundledPackReader } from "../packs/bundledPackReader";

export interface AppBootstrap {
  db: Database;
  pack: LoadedPack;
}

/**
 * App startup: open the SQLite DB (migrations run in Rust on load) and validate + load the
 * bundled seed pack. This is the same for every account model — local-only or cloud-synced —
 * since pack content is always local (see cloud/supabaseRepos.ts). Which Repos implementation
 * to wire over this `db`, and which profile (if any) is active, is decided in App.tsx based on
 * auth state.
 */
export async function bootstrap(): Promise<AppBootstrap> {
  const db = await TauriDatabase.connect();
  const pack = await loadPack(new BundledPackReader());
  return { db, pack };
}
