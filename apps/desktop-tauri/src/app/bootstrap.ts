import type { Database } from "@polyglotai/core-learning";
import { loadPack, mergePacks, type LoadedPack } from "@polyglotai/language-pack-sdk";
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

/** Loads and validates one bundled pack's full content by id (see BundledPackReader).
 *
 * Resolves `manifest.basePack` recursively: a "micro-pack" — a small situational/cultural
 * add-on scoped to one interest area within a language (e.g. a goshuin-seeker pack for
 * Japanese) — declares a basePack and only authors what's new (its own vocabulary, dialogues,
 * culture notes), inheriting the base pack's grammar/pronunciation/etc. for free via
 * mergePacks. Base and child are each independently loaded (and independently valid) before
 * merging, so a micro-pack's own content must stay self-contained — it can't cross-reference
 * the base pack's vocabulary keys, since semantic validation runs on each pack in isolation. */
export async function loadPackForId(packId: string): Promise<LoadedPack> {
  const child = await loadPack(new BundledPackReader(packId));
  if (!child.manifest.basePack) return child;
  const base = await loadPackForId(child.manifest.basePack);
  return mergePacks(base, child);
}

/** Just the manifest (name, language code, dialects, …) — enough for the language picker to
 * list a pack without paying for full content parse/validation. */
export async function loadPackManifest(packId: string): Promise<Manifest> {
  const text = await new BundledPackReader(packId).readText("manifest.json");
  return ManifestSchema.parse(JSON.parse(text));
}
