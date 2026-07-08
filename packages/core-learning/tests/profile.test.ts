import type { LearnerProfile } from "@polyglotai/shared-types";
import { beforeEach, describe, expect, it } from "vitest";
import { effectiveSeverityCeiling, ProfileRepo } from "../src/profile/profile.js";
import { FeatureFlagRegistry } from "../src/featureflags/registry.js";
import type { Database } from "../src/db/database.js";
import { createMigratedDb } from "./nodeSqliteDb.js";

const FIXED_NOW = () => "2026-07-08T00:00:00.000Z";

describe("ProfileRepo", () => {
  let db: Database;

  beforeEach(() => {
    db = createMigratedDb().database;
  });

  it("creates a profile with onboarding defaults", async () => {
    const repo = new ProfileRepo(db, FIXED_NOW);
    const p = await repo.create({ displayName: "Alek", goal: "conversation" });

    expect(p.displayName).toBe("Alek");
    expect(p.goal).toBe("conversation");
    expect(p.realSpeechLevel).toBe("informal"); // default
    expect(p.correctionStrictness).toBe("balanced"); // default
    expect(p.settings).toEqual({});
    expect(await repo.getFirst()).toEqual(p);
  });

  it("updates mutable fields and refreshes updated_at", async () => {
    const repo = new ProfileRepo(db, FIXED_NOW);
    const p = await repo.create({ displayName: "Alek" });

    const laterRepo = new ProfileRepo(db, () => "2026-09-01T00:00:00.000Z");
    const updated = await laterRepo.update(p.id, {
      realSpeechLevel: "profanity",
      cefrEstimate: "B1",
      settings: { theme: "dark" },
    });

    expect(updated.realSpeechLevel).toBe("profanity");
    expect(updated.cefrEstimate).toBe("B1");
    expect(updated.settings).toEqual({ theme: "dark" });
    expect(updated.updatedAt).toBe("2026-09-01T00:00:00.000Z");
    expect(updated.createdAt).toBe("2026-07-08T00:00:00.000Z");
  });

  it("returns null for a missing profile", async () => {
    const repo = new ProfileRepo(db);
    expect(await repo.get("nope")).toBeNull();
    expect(await repo.getFirst()).toBeNull();
  });
});

describe("effectiveSeverityCeiling", () => {
  const base: LearnerProfile = {
    id: "p1",
    displayName: "x",
    realSpeechLevel: "informal",
    correctionStrictness: "balanced",
    settings: {},
    schemaVersion: 1,
    createdAt: "2026-07-08T00:00:00.000Z",
    updatedAt: "2026-07-08T00:00:00.000Z",
  };

  it("maps real-speech level to a default severity ceiling", () => {
    expect(effectiveSeverityCeiling({ ...base, realSpeechLevel: "standard" })).toBe(1);
    expect(effectiveSeverityCeiling({ ...base, realSpeechLevel: "informal" })).toBe(3);
    expect(effectiveSeverityCeiling({ ...base, realSpeechLevel: "slang" })).toBe(5);
    expect(effectiveSeverityCeiling({ ...base, realSpeechLevel: "profanity" })).toBe(7);
  });

  it("lets an explicit override win over the level default", () => {
    expect(
      effectiveSeverityCeiling({ ...base, realSpeechLevel: "standard", slangSeverityOverride: 6 }),
    ).toBe(6);
  });
});

describe("FeatureFlagRegistry", () => {
  it("reads seeded defaults and toggles flags", async () => {
    const db = createMigratedDb().database;
    const flags = new FeatureFlagRegistry(db, FIXED_NOW);

    // Seeded by migration 0002 (plan Appendix).
    expect(await flags.isEnabled("slang_mode")).toBe(true);
    expect(await flags.isEnabled("conversation_logging")).toBe(false); // off by default (owner decision)
    expect(await flags.isEnabled("cloud_sync")).toBe(false);

    await flags.setEnabled("conversation_logging", true);
    expect(await flags.isEnabled("conversation_logging")).toBe(true);

    const all = await flags.all();
    expect(all.slang_mode).toBe(true);
    expect(all.conversation_logging).toBe(true);
  });
});
