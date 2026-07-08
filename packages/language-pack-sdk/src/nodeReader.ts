import { readFile } from "node:fs/promises";
import { join } from "node:path";
import type { PackFileReader } from "./reader.js";

/** Node fs-based PackFileReader — used by the CLI validator and CI, not by the Tauri app. */
export class NodeFsPackReader implements PackFileReader {
  constructor(private readonly root: string) {}

  async readText(relativePath: string): Promise<string> {
    return readFile(join(this.root, relativePath), "utf-8");
  }
}
