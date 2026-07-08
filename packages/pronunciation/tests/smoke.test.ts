import { describe, expect, it } from "vitest";
import type { SpeechProvider, TTSProvider } from "../src/index.js";

describe("pronunciation scaffold", () => {
  it("exposes SpeechProvider and TTSProvider interface shapes usable by adapters", async () => {
    const fakeSpeech: SpeechProvider = {
      name: "fake-stt",
      transcribe: async () => "transcript",
    };
    const fakeTts: TTSProvider = {
      name: "fake-tts",
      synthesize: async () => new Blob(),
    };
    await expect(fakeSpeech.transcribe(new Blob())).resolves.toBe("transcript");
    expect(fakeTts.name).toBe("fake-tts");
  });
});
