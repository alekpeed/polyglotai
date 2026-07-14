import type { LearnerProfile } from "@polyglotai/shared-types";
import type { SqlValue } from "../db/database.js";
import type { Repos } from "../repos.js";

export interface DashboardData {
  profile: LearnerProfile;
  activePackName: string | null;
  dueCount: number;
  totals: {
    vocabulary: number;
    grammar: number;
    realSpeech: number;
    dialogues: number;
  };
  /** Consecutive days (ending today, or yesterday if today has no review yet) with at least
   * one recorded review — real, derived from review_results, not a separately-tracked stat. */
  streakDays: number;
  /** Whether each of the last 7 calendar days (oldest first, today last) had a review. */
  streakLast7: boolean[];
  /** Total graded reviews this profile has ever done (review_results rows). */
  lifetimeReviews: number;
  /** Share of those reviews recalled well (rated Good/Easy, i.e. rating ≥ 3), as a 0–100
   * percentage — null until there's at least one review to divide by. */
  recallRate: number | null;
  /** The learner's daily review target (profile setting `dailyGoal`, defaulting to 20). */
  dailyGoal: number;
  /** Reviews completed today (local calendar day of `now`), counted toward the daily goal. */
  reviewsToday: number;
}

/** Default daily review target when a profile hasn't set its own. */
export const DEFAULT_DAILY_GOAL = 20;

/** Reads the profile's chosen daily review goal from settings, falling back to the default. */
export function dailyGoalOf(profile: LearnerProfile): number {
  const g = (profile.settings as Record<string, unknown>).dailyGoal;
  return typeof g === "number" && g > 0 ? Math.round(g) : DEFAULT_DAILY_GOAL;
}

async function count(repos: Repos, table: string, packId: string): Promise<number> {
  const rows = await repos.db.all<{ n: number }>(
    `SELECT COUNT(*) AS n FROM ${table} WHERE pack_id = ?`,
    [packId as SqlValue],
  );
  return rows[0]?.n ?? 0;
}

function toIsoDate(d: Date): string {
  return d.toISOString().slice(0, 10);
}

async function reviewedDays(repos: Repos, profileId: string): Promise<Set<string>> {
  const rows = await repos.db.all<{ d: string }>(
    `SELECT DISTINCT date(rr.reviewed_at) AS d
       FROM review_results rr
       JOIN review_items ri ON ri.id = rr.review_item_id
      WHERE ri.profile_id = ?`,
    [profileId as SqlValue],
  );
  return new Set(rows.map((r) => r.d));
}

/** Lifetime review count + how many were recalled well (rating ≥ 3 = Good/Easy). One aggregate
 * pass over this profile's review_results. */
async function reviewStats(repos: Repos, profileId: string): Promise<{ total: number; recalled: number }> {
  const rows = await repos.db.all<{ total: number; recalled: number }>(
    `SELECT COUNT(*) AS total,
            SUM(CASE WHEN rr.rating >= 3 THEN 1 ELSE 0 END) AS recalled
       FROM review_results rr
       JOIN review_items ri ON ri.id = rr.review_item_id
      WHERE ri.profile_id = ?`,
    [profileId as SqlValue],
  );
  return { total: Number(rows[0]?.total ?? 0), recalled: Number(rows[0]?.recalled ?? 0) };
}

/** Count of reviews this profile completed on a given local calendar day (ISO yyyy-mm-dd). */
async function reviewsOnDay(repos: Repos, profileId: string, isoDay: string): Promise<number> {
  const rows = await repos.db.all<{ n: number }>(
    `SELECT COUNT(*) AS n
       FROM review_results rr
       JOIN review_items ri ON ri.id = rr.review_item_id
      WHERE ri.profile_id = ? AND date(rr.reviewed_at) = ?`,
    [profileId as SqlValue, isoDay as SqlValue],
  );
  return Number(rows[0]?.n ?? 0);
}

function streakFrom(days: Set<string>, today: Date): number {
  if (days.size === 0) return 0;
  const cursor = new Date(today);
  if (!days.has(toIsoDate(cursor))) cursor.setUTCDate(cursor.getUTCDate() - 1);
  let streak = 0;
  while (days.has(toIsoDate(cursor))) {
    streak += 1;
    cursor.setUTCDate(cursor.getUTCDate() - 1);
  }
  return streak;
}

function last7From(days: Set<string>, today: Date): boolean[] {
  const out: boolean[] = [];
  for (let i = 6; i >= 0; i--) {
    const d = new Date(today);
    d.setUTCDate(today.getUTCDate() - i);
    out.push(days.has(toIsoDate(d)));
  }
  return out;
}

/** Aggregates the home dashboard (spec §5.1): profile, active pack, due count, content totals,
 * and review streak. `now` is injected for deterministic tests. */
export async function loadDashboard(
  repos: Repos,
  profileId: string,
  now: () => Date = () => new Date(),
): Promise<DashboardData> {
  const profile = await repos.profiles.get(profileId);
  if (!profile) throw new Error(`profile ${profileId} not found`);

  const packId = profile.activePackId ?? null;
  const activePack = packId ? await repos.packs.get(packId) : null;
  const dueCount = await repos.reviews.countDue(profileId);

  const totals = packId
    ? {
        vocabulary: await count(repos, "vocabulary_items", packId),
        grammar: await count(repos, "grammar_items", packId),
        realSpeech: await count(repos, "real_speech_items", packId),
        dialogues: await count(repos, "dialogues", packId),
      }
    : { vocabulary: 0, grammar: 0, realSpeech: 0, dialogues: 0 };

  const days = await reviewedDays(repos, profileId);
  const today = now();
  const stats = await reviewStats(repos, profileId);

  return {
    profile,
    activePackName: activePack?.name ?? null,
    dueCount,
    totals,
    streakDays: streakFrom(days, today),
    streakLast7: last7From(days, today),
    lifetimeReviews: stats.total,
    recallRate: stats.total ? Math.round((stats.recalled / stats.total) * 100) : null,
    dailyGoal: dailyGoalOf(profile),
    reviewsToday: await reviewsOnDay(repos, profileId, toIsoDate(today)),
  };
}
