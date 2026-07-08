import { describe, expect, it } from "vitest";
import {
  DialogueSchema,
  GrammarItemSchema,
  LearnerProfileSchema,
  ManifestSchema,
  RealSpeechItemSchema,
  VocabularyItemSchema,
} from "../src/index.js";

describe("shared-types round-trip", () => {
  it("parses a manifest with dialects and content lists", () => {
    const manifest = ManifestSchema.parse({
      schemaVersion: 1,
      id: "pt-br",
      name: "Brazilian Portuguese",
      languageCode: "pt-BR",
      packVersion: "0.1.0",
      basePack: null,
      dialects: [{ id: "pt-BR-SP", name: "São Paulo" }],
      defaultDialect: "pt-BR-SP",
      cefrRange: ["A1", "B2"],
      contents: { vocabulary: ["vocabulary/a1.json"] },
    });
    expect(manifest.id).toBe("pt-br");
  });

  it("parses a vocabulary item with entryType='phrase'", () => {
    const item = VocabularyItemSchema.parse({
      schemaVersion: 1,
      key: "phrase.por-favor",
      entryType: "phrase",
      lemma: "por favor",
      translation: "please",
      register: "neutral",
    });
    expect(item.entryType).toBe("phrase");
  });

  it("parses a grammar item with drills and related vocabulary", () => {
    const item = GrammarItemSchema.parse({
      schemaVersion: 1,
      key: "grammar.present-ar",
      title: "Present tense -ar verbs",
      explanationMd: "…",
      drills: [{ prompt: "Conjugate falar for nós", answer: "falamos" }],
      relatedVocabulary: ["vocab.falar"],
    });
    expect(item.drills).toHaveLength(1);
  });

  it("parses a slang item and rejects an out-of-range severity", () => {
    const item = RealSpeechItemSchema.parse({
      schemaVersion: 1,
      key: "slang.mano",
      kind: "slang",
      phrase: "mano",
      register: "informal",
      severity: 1,
      learnerShouldUse: "use",
    });
    expect(item.severity).toBe(1);
    expect(() =>
      RealSpeechItemSchema.parse({ ...item, severity: 9 }),
    ).toThrow();
  });

  it("parses a dialogue with transcript and translation", () => {
    const dialogue = DialogueSchema.parse({
      schemaVersion: 1,
      key: "dialogue.cafe-order",
      scenario: "café",
      speakers: [{ id: "a", name: "Ana" }],
      transcript: [{ speakerId: "a", text: "Oi!" }],
      translation: [{ speakerId: "a", text: "Hi!" }],
    });
    expect(dialogue.transcript).toHaveLength(1);
  });

  it("parses a learner profile with default realSpeechLevel", () => {
    const profile = LearnerProfileSchema.parse({
      id: "p1",
      displayName: "Alek",
      schemaVersion: 1,
      createdAt: "2026-07-08T00:00:00Z",
      updatedAt: "2026-07-08T00:00:00Z",
    });
    expect(profile.realSpeechLevel).toBe("informal");
  });
});
