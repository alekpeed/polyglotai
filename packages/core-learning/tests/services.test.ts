import type { LoadedPack } from "@polyglotai/language-pack-sdk";
import type { Manifest, VocabularyItem } from "@polyglotai/shared-types";
import { describe, expect, it } from "vitest";
import { createRepos } from "../src/repos.js";
import { runOnboarding } from "../src/services/onboarding.js";
import { loadDashboard } from "../src/services/dashboard.js";
import { createMigratedDb } from "./nodeSqliteDb.js";

const T0 = new Date("2026-07-08T00:00:00.000Z");

function seedPack(): LoadedPack {
  const manifest: Manifest = {
    schemaVersion: 1,
    id: "pt-br",
    name: "Brazilian Portuguese",
    languageCode: "pt-BR",
    packVersion: "0.1.0",
    basePack: null,
    authors: [],
    dialects: [],
    featureFlags: {},
    contents: {},
  };
  const vocab: VocabularyItem[] = ["agua", "cafe", "pao"].map((k) => ({
    schemaVersion: 1,
    key: `vocab.${k}`,
    entryType: "word",
    lemma: k,
    translation: k,
    tags: [],
    examples: [],
  }));
  return {
    manifest,
    vocabulary: vocab,
    grammar: [],
    realSpeech: [],
    dialogues: [],
    pronunciation: [],
    lessons: [],
    aiPrompts: [],
    culture: [],
  };
}

describe("runOnboarding", () => {
  it("installs the pack, creates the profile, and seeds a due queue", async () => {
    const repos = createRepos(createMigratedDb().database, () => T0);
    const profile = await runOnboarding(repos, {
      displayName: "Alek",
      goal: "conversation",
      realSpeechLevel: "slang",
      pack: seedPack(),
    });

    expect(profile.displayName).toBe("Alek");
    expect(profile.activePackId).toBe("pt-br");
    expect(profile.realSpeechLevel).toBe("slang");

    // Pack installed and review items seeded (3 vocab -> 3 due).
    expect(await repos.packs.get("pt-br")).not.toBeNull();
    expect(await repos.reviews.countDue(profile.id)).toBe(3);
  });
});

describe("loadDashboard", () => {
  it("summarizes profile, active pack, due count, and content totals", async () => {
    const repos = createRepos(createMigratedDb().database, () => T0);
    const profile = await runOnboarding(repos, { displayName: "Alek", pack: seedPack() });

    const data = await loadDashboard(repos, profile.id, () => T0);
    expect(data.profile.id).toBe(profile.id);
    expect(data.activePackName).toBe("Brazilian Portuguese");
    expect(data.dueCount).toBe(3);
    expect(data.totals.vocabulary).toBe(3);
    expect(data.totals.grammar).toBe(0);
  });

  it("computes a real review streak from review_results, not a fabricated stat", async () => {
    const repos = createRepos(createMigratedDb().database, () => T0);
    const profile = await runOnboarding(repos, { displayName: "Alek", pack: seedPack() });

    const zero = await loadDashboard(repos, profile.id, () => T0);
    expect(zero.streakDays).toBe(0);
    expect(zero.streakLast7.every((d) => d === false)).toBe(true);

    const [item] = await repos.reviews.listDue(profile.id);
    const dayIso = (offsetDays: number) => new Date(T0.getTime() + offsetDays * 86_400_000).toISOString();
    for (const offset of [-2, -1, 0]) {
      await repos.db.run(
        `INSERT INTO review_results (id, review_item_id, rating, reviewed_at, schema_version, created_at)
         VALUES (?, ?, ?, ?, ?, ?)`,
        [`r-${offset}`, item!.id, 3, dayIso(offset), 1, dayIso(offset)],
      );
    }

    const withStreak = await loadDashboard(repos, profile.id, () => T0);
    expect(withStreak.streakDays).toBe(3);
    expect(withStreak.streakLast7.slice(-3)).toEqual([true, true, true]);
    expect(withStreak.streakLast7.slice(0, 4)).toEqual([false, false, false, false]);
  });

  it("reports lifetime review count and recall rate (rating >= 3 counts as recalled well)", async () => {
    const repos = createRepos(createMigratedDb().database, () => T0);
    const profile = await runOnboarding(repos, { displayName: "Alek", pack: seedPack() });

    const zero = await loadDashboard(repos, profile.id, () => T0);
    expect(zero.lifetimeReviews).toBe(0);
    expect(zero.recallRate).toBeNull();

    const [item] = await repos.reviews.listDue(profile.id);
    const iso = (n: number) => new Date(T0.getTime() + n * 1000).toISOString();
    const ratings = [4, 3, 3, 1]; // 3 of 4 recalled well → 75%
    let i = 0;
    for (const rating of ratings) {
      await repos.db.run(
        `INSERT INTO review_results (id, review_item_id, rating, reviewed_at, schema_version, created_at)
         VALUES (?, ?, ?, ?, ?, ?)`,
        [`s-${i}`, item!.id, rating, iso(i), 1, iso(i)],
      );
      i += 1;
    }

    const data = await loadDashboard(repos, profile.id, () => T0);
    expect(data.lifetimeReviews).toBe(4);
    expect(data.recallRate).toBe(75);
  });
});
