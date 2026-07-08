/**
 * Abstract file access for a pack directory. Kept separate from any concrete filesystem API
 * so the validator/loader core stays platform-free (usable from a Node CLI, from the Tauri
 * app via its fs plugin, or later from Android) — see plan §2 boundary rules.
 */
export interface PackFileReader {
  readText(relativePath: string): Promise<string>;
}
