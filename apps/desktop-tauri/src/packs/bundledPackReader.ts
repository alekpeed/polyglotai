import type { PackFileReader } from "@polyglotai/language-pack-sdk";

// Vite inlines every JSON file under packs/* at build time, keyed by resolved path — one glob
// covers every bundled pack (pt-br, ja, ...) so adding a new language pack directory is enough
// to make it available, no reader/build-config change needed per language.
const modules = import.meta.glob("../../../../packs/*/**/*.json", {
  import: "default",
  eager: true,
}) as Record<string, unknown>;

const PACKS_ROOT = "packs/";

/** packId -> { relativePath -> JSON text }, built once at module load. */
function buildPackFileMaps(): Record<string, Record<string, string>> {
  const packs: Record<string, Record<string, string>> = {};
  for (const [absPath, contents] of Object.entries(modules)) {
    const idx = absPath.indexOf(PACKS_ROOT);
    if (idx === -1) continue;
    const rel = absPath.slice(idx + PACKS_ROOT.length); // e.g. "ja/vocabulary/n5.json"
    const slash = rel.indexOf("/");
    if (slash === -1) continue;
    const packId = rel.slice(0, slash);
    const filePath = rel.slice(slash + 1); // e.g. "vocabulary/n5.json"
    (packs[packId] ??= {})[filePath] = JSON.stringify(contents);
  }
  return packs;
}

const PACK_FILES = buildPackFileMaps();

/** Every bundled pack id (directory name under packs/), sorted for a stable picker order. */
export function listBundledPackIds(): string[] {
  return Object.keys(PACK_FILES).sort();
}

/** PackFileReader backed by Vite-bundled JSON for one specific pack. */
export class BundledPackReader implements PackFileReader {
  private readonly files: Record<string, string>;

  constructor(packId: string) {
    const files = PACK_FILES[packId];
    if (!files) throw new Error(`bundled pack: unknown pack id "${packId}"`);
    this.files = files;
  }

  async readText(relativePath: string): Promise<string> {
    const text = this.files[relativePath];
    if (text === undefined) throw new Error(`bundled pack: missing file "${relativePath}"`);
    return text;
  }
}
