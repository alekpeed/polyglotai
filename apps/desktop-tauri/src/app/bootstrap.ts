import type { Database } from "@polyglotai/core-learning";
import { loadPack, type LoadedPack } from "@polyglotai/language-pack-sdk";
import { ManifestSchema, type Manifest } from "@polyglotai/shared-types";
import { TauriDatabase } from "../db/tauriDatabase";
import { BundledPackReader, listBundledPackIds } from "../packs/bundledPackReader";

export interface AppBootstrap {
  db: Database;
}

/**
 * App startup: open the SQLite DB (migrations run in Rust on load). This is the same for every
 * account model — local-only or cloud-synced — since pack content is always local (see
 * cloud/supabaseRepos.ts). Loading a specific pack's content is a separate step (loadPackForId)
 * because which pack(s) matter depends on which profile(s) exist, decided in App.tsx.
 */
export async function bootstrap(): Promise<AppBootstrap> {
  const db = await TauriDatabase.connect();
  return { db };
}

/** Every pack id bundled with this build — the choices offered on the language picker. */
export { listBundledPackIds };

/** Loads and validates one bundled pack's full content by id (see BundledPackReader). */
export async function loadPackForId(packId: string): Promise<LoadedPack> {
  return loadPack(new BundledPackReader(packId));
}

/** Just the manifest (name, language code, dialects, …) — enough for the language picker to
 * list a pack without paying for full content parse/validation. */
export async function loadPackManifest(packId: string): Promise<Manifest> {
  const text = await new BundledPackReader(packId).readText("manifest.json");
  return ManifestSchema.parse(JSON.parse(text));
}
