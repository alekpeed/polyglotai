import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import { describe, expect, it } from "vitest";
import { NodeFsPackReader } from "../src/nodeReader.js";
import { validatePack } from "../src/validator.js";

const here = dirname(fileURLToPath(import.meta.url));
const miniPackDir = join(here, "fixtures/mini-pack");

describe("validatePack", () => {
  it("validates the mini fixture pack with no errors", async () => {
    const report = await validatePack(new NodeFsPackReader(miniPackDir));
    expect(report.errors).toEqual([]);
    expect(report.valid).toBe(true);
  });

  it("reports content volume under target for a tiny pack", async () => {
    const report = await validatePack(new NodeFsPackReader(miniPackDir));
    const vocab = report.volumeReport.find((r) => r.category === "vocabulary");
    expect(vocab?.actual).toBe(2);
    expect(vocab?.meetsTarget).toBe(false);
  });

  it("flags a dangling relatedVocabulary reference", async () => {
    const badReader = {
      async readText(path: string) {
        const reader = new NodeFsPackReader(miniPackDir);
        if (path === "grammar/core.json") {
          return JSON.stringify([
            {
              schemaVersion: 1,
              key: "grammar.bad",
              title: "Bad",
              explanationMd: "…",
              relatedVocabulary: ["vocab.does-not-exist"],
            },
          ]);
        }
        return reader.readText(path);
      },
    };
    const report = await validatePack(badReader);
    expect(report.valid).toBe(false);
    expect(report.errors.some((e) => e.includes("dangling relatedVocabulary"))).toBe(true);
  });
});
