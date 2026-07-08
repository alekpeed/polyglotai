import { describe, expect, it } from "vitest";
import type { AIProvider } from "../src/index.js";

describe("ai-orchestration scaffold", () => {
  it("exposes an AIProvider interface shape usable by adapters", async () => {
    const fakeProvider: AIProvider = {
      name: "fake",
      complete: async () => "ok",
    };
    await expect(
      fakeProvider.complete({ systemPrompt: "", messages: [] }),
    ).resolves.toBe("ok");
  });
});
