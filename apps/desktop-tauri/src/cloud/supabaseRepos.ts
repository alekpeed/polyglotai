import type { SupabaseClient } from "@supabase/supabase-js";
import { FsrsScheduler, type Scheduler } from "@polyglotai/spaced-repetition";
import {
  PackRegistry,
  type ConversationRecord,
  type Database,
  type FeatureFlagKey,
  type IConversationRepo,
  type IFeatureFlagRegistry,
  type IProfileRepo,
  type IPronunciationRepo,
  type IReviewRepo,
  type ProfileCreateInput,
  type ProfileUpdate,
  type PronunciationAttempt,
  type RecordReviewInput,
  type Repos,
  type ReviewItem,
  type ReviewItemType,
  type StoredMessage,
} from "@polyglotai/core-learning";
import type { LearnerProfile } from "@polyglotai/shared-types";

/**
 * Cloud-account Repos: profiles/flags/reviews/conversations/pronunciation are Supabase Postgres
 * tables scoped to the signed-in user via RLS (supabase/migrations/20260712120000_learner_accounts.sql).
 * `db` and `packs` stay the LOCAL SQLite/sql.js connection unchanged — pack content
 * (vocabulary/grammar/etc.) is static and bundled, never duplicated per account, so review
 * generation still enumerates content ids from the local content tables and only the review
 * state itself (review_items/review_results) lives in the cloud.
 */
export function createSupabaseRepos(client: SupabaseClient, userId: string, localDb: Database): Repos {
  return {
    db: localDb,
    profiles: new SupabaseProfileRepo(client, userId),
    flags: new SupabaseFeatureFlagRegistry(client, userId),
    packs: new PackRegistry(localDb),
    reviews: new SupabaseReviewRepo(client, userId, localDb),
    conversations: new SupabaseConversationRepo(client, userId),
    pronunciation: new SupabasePronunciationRepo(client, userId),
  };
}

interface ProfileRow {
  id: string;
  display_name: string;
  active_pack_id: string | null;
  goal: string | null;
  target_dialect: string | null;
  real_speech_level: string;
  slang_severity_override: number | null;
  cefr_estimate: string | null;
  correction_strictness: string;
  settings: Record<string, unknown> | null;
  created_at: string;
  updated_at: string;
}

function rowToProfile(row: ProfileRow): LearnerProfile {
  return {
    id: row.id,
    displayName: row.display_name,
    activePackId: row.active_pack_id,
    goal: (row.goal as LearnerProfile["goal"]) ?? undefined,
    targetDialect: row.target_dialect ?? undefined,
    realSpeechLevel: row.real_speech_level as LearnerProfile["realSpeechLevel"],
    slangSeverityOverride: (row.slang_severity_override as LearnerProfile["slangSeverityOverride"]) ?? undefined,
    cefrEstimate: (row.cefr_estimate as LearnerProfile["cefrEstimate"]) ?? undefined,
    correctionStrictness: row.correction_strictness as LearnerProfile["correctionStrictness"],
    settings: row.settings ?? {},
    schemaVersion: 1,
    createdAt: row.created_at,
    updatedAt: row.updated_at,
  };
}

class SupabaseProfileRepo implements IProfileRepo {
  constructor(
    private readonly client: SupabaseClient,
    private readonly userId: string,
  ) {}

  async create(input: ProfileCreateInput): Promise<LearnerProfile> {
    const { data, error } = await this.client
      .from("learner_profiles")
      .insert({
        user_id: this.userId,
        display_name: input.displayName,
        active_pack_id: input.activePackId ?? null,
        goal: input.goal ?? null,
        target_dialect: input.targetDialect ?? null,
        real_speech_level: input.realSpeechLevel ?? "informal",
        correction_strictness: input.correctionStrictness ?? "balanced",
        settings: {},
      })
      .select()
      .single<ProfileRow>();
    if (error || !data) throw new Error(`profile create failed: ${error?.message}`);
    return rowToProfile(data);
  }

  async get(id: string): Promise<LearnerProfile | null> {
    const { data, error } = await this.client
      .from("learner_profiles")
      .select("*")
      .eq("id", id)
      .eq("user_id", this.userId)
      .maybeSingle<ProfileRow>();
    if (error) throw new Error(`profile fetch failed: ${error.message}`);
    return data ? rowToProfile(data) : null;
  }

  /** The oldest profile on this account — used only to detect "has this account ever
   * onboarded at all"; an account can carry one profile per language pack now. */
  async getFirst(): Promise<LearnerProfile | null> {
    const { data, error } = await this.client
      .from("learner_profiles")
      .select("*")
      .eq("user_id", this.userId)
      .order("created_at", { ascending: true })
      .limit(1)
      .maybeSingle<ProfileRow>();
    if (error) throw new Error(`profile fetch failed: ${error.message}`);
    return data ? rowToProfile(data) : null;
  }

  async listAll(): Promise<LearnerProfile[]> {
    const { data, error } = await this.client
      .from("learner_profiles")
      .select("*")
      .eq("user_id", this.userId)
      .order("created_at", { ascending: true })
      .returns<ProfileRow[]>();
    if (error) throw new Error(`profiles fetch failed: ${error.message}`);
    return (data ?? []).map(rowToProfile);
  }

  async getByPackId(packId: string): Promise<LearnerProfile | null> {
    const { data, error } = await this.client
      .from("learner_profiles")
      .select("*")
      .eq("user_id", this.userId)
      .eq("active_pack_id", packId)
      .order("created_at", { ascending: true })
      .limit(1)
      .maybeSingle<ProfileRow>();
    if (error) throw new Error(`profile fetch failed: ${error.message}`);
    return data ? rowToProfile(data) : null;
  }

  async update(id: string, patch: ProfileUpdate): Promise<LearnerProfile> {
    const set: Record<string, unknown> = { updated_at: new Date().toISOString() };
    if (patch.displayName !== undefined) set.display_name = patch.displayName;
    if (patch.goal !== undefined) set.goal = patch.goal;
    if (patch.targetDialect !== undefined) set.target_dialect = patch.targetDialect;
    if (patch.realSpeechLevel !== undefined) set.real_speech_level = patch.realSpeechLevel;
    if (patch.slangSeverityOverride !== undefined) set.slang_severity_override = patch.slangSeverityOverride;
    if (patch.cefrEstimate !== undefined) set.cefr_estimate = patch.cefrEstimate ?? null;
    if (patch.correctionStrictness !== undefined) set.correction_strictness = patch.correctionStrictness;
    if (patch.activePackId !== undefined) set.active_pack_id = patch.activePackId;
    if (patch.settings !== undefined) set.settings = patch.settings;

    const { data, error } = await this.client
      .from("learner_profiles")
      .update(set)
      .eq("id", id)
      .eq("user_id", this.userId)
      .select()
      .single<ProfileRow>();
    if (error || !data) throw new Error(`profile update failed: ${error?.message}`);
    return rowToProfile(data);
  }
}

/** Matches the local build's migration-seeded defaults (0002_seed_feature_flags.sql) — a fresh
 * cloud account has zero rows in user_feature_flags, and a missing row should read the same
 * default either backend, not a surprising "everything off." */
const DEFAULT_FLAGS: Record<FeatureFlagKey, boolean> = {
  ai_conversation: true,
  slang_mode: true,
  profanity_explanations: true,
  pronunciation_recording: true,
  conversation_logging: false,
  teacher_dashboard: false,
  cloud_sync: false,
  billing: false,
  experimental_packs: false,
};

class SupabaseFeatureFlagRegistry implements IFeatureFlagRegistry {
  constructor(
    private readonly client: SupabaseClient,
    private readonly userId: string,
  ) {}

  async isEnabled(key: FeatureFlagKey): Promise<boolean> {
    const { data, error } = await this.client
      .from("user_feature_flags")
      .select("enabled")
      .eq("user_id", this.userId)
      .eq("key", key)
      .maybeSingle<{ enabled: boolean }>();
    if (error) throw new Error(`flag fetch failed: ${error.message}`);
    return data ? data.enabled : DEFAULT_FLAGS[key];
  }

  async all(): Promise<Record<string, boolean>> {
    const { data, error } = await this.client
      .from("user_feature_flags")
      .select("key, enabled")
      .eq("user_id", this.userId)
      .returns<{ key: string; enabled: boolean }[]>();
    if (error) throw new Error(`flags fetch failed: ${error.message}`);
    const overrides = Object.fromEntries((data ?? []).map((r) => [r.key, r.enabled]));
    return { ...DEFAULT_FLAGS, ...overrides };
  }

  async setEnabled(key: FeatureFlagKey, enabled: boolean): Promise<void> {
    const { error } = await this.client
      .from("user_feature_flags")
      .upsert(
        { user_id: this.userId, key, enabled, updated_at: new Date().toISOString() },
        { onConflict: "user_id,key" },
      );
    if (error) throw new Error(`flag update failed: ${error.message}`);
  }
}

const CONTENT_TABLE: Record<ReviewItemType, string> = {
  vocabulary: "vocabulary_items",
  grammar: "grammar_items",
  real_speech: "real_speech_items",
  grammar_ladder: "grammar_ladder_steps",
};
const ALL_ITEM_TYPES: ReviewItemType[] = ["vocabulary", "grammar", "real_speech", "grammar_ladder"];

interface ReviewItemRow {
  id: string;
  item_type: string;
  content_id: string;
  difficulty: number | null;
  stability: number | null;
  retrievability: number | null;
  state: string;
  due_at: string | null;
  last_reviewed_at: string | null;
  lapses: number;
  reps: number;
}

function rowToReviewItem(row: ReviewItemRow, profileId: string): ReviewItem {
  return {
    id: row.id,
    profileId,
    itemType: row.item_type as ReviewItemType,
    contentId: row.content_id,
    state: {
      difficulty: row.difficulty ?? 0,
      stability: row.stability ?? 0,
      retrievability: row.retrievability ?? 0,
      state: row.state as ReviewItem["state"]["state"],
      dueAt: row.due_at ?? new Date(0).toISOString(),
      lastReviewedAt: row.last_reviewed_at,
      lapses: row.lapses,
      reps: row.reps,
    },
  };
}

class SupabaseReviewRepo implements IReviewRepo {
  private readonly scheduler: Scheduler = new FsrsScheduler();

  constructor(
    private readonly client: SupabaseClient,
    private readonly userId: string,
    /** Content enumeration only (vocabulary_items/grammar_items/etc.) — the local pack content
     * tables, unrelated to where review state itself lives. */
    private readonly localDb: Database,
  ) {}

  async generateForPack(
    profileId: string,
    packId: string,
    itemTypes: ReviewItemType[] = ALL_ITEM_TYPES,
  ): Promise<number> {
    const now = new Date();
    const seed = this.scheduler.initialState(now);
    const ts = now.toISOString();
    let created = 0;

    for (const itemType of itemTypes) {
      const rows = await this.localDb.all<{ id: string }>(
        `SELECT id FROM ${CONTENT_TABLE[itemType]} WHERE pack_id = ?`,
        [packId],
      );
      if (rows.length === 0) continue;

      // Bulk existence check, then bulk insert of only what's missing — the local repo does
      // this per-row since it's all one local connection, but that would mean one network round
      // trip per content item here, so it's batched instead.
      const { data: existing, error: existingErr } = await this.client
        .from("review_items")
        .select("content_id")
        .eq("profile_id", profileId)
        .eq("item_type", itemType)
        .in(
          "content_id",
          rows.map((r) => r.id),
        )
        .returns<{ content_id: string }[]>();
      if (existingErr) throw new Error(`review lookup failed: ${existingErr.message}`);
      const have = new Set((existing ?? []).map((r) => r.content_id));

      const missing = rows.filter((r) => !have.has(r.id));
      if (missing.length === 0) continue;

      const { error: insertErr } = await this.client.from("review_items").insert(
        missing.map((r) => ({
          user_id: this.userId,
          profile_id: profileId,
          item_type: itemType,
          content_id: r.id,
          difficulty: seed.difficulty,
          stability: seed.stability,
          retrievability: seed.retrievability,
          state: seed.state,
          due_at: seed.dueAt,
          last_reviewed_at: null,
          lapses: seed.lapses,
          reps: seed.reps,
          created_at: ts,
          updated_at: ts,
        })),
      );
      if (insertErr) throw new Error(`review insert failed: ${insertErr.message}`);
      created += missing.length;
    }

    return created;
  }

  async listDue(profileId: string, limit = 50, itemTypes?: ReviewItemType[]): Promise<ReviewItem[]> {
    const now = new Date().toISOString();
    let query = this.client
      .from("review_items")
      .select("*")
      .eq("user_id", this.userId)
      .eq("profile_id", profileId)
      .or(`due_at.is.null,due_at.lte.${now}`)
      .order("due_at", { ascending: true, nullsFirst: true })
      .limit(limit);
    if (itemTypes?.length) query = query.in("item_type", itemTypes);
    const { data, error } = await query.returns<ReviewItemRow[]>();
    if (error) throw new Error(`due review fetch failed: ${error.message}`);
    return (data ?? []).map((r) => rowToReviewItem(r, profileId));
  }

  async countDue(profileId: string): Promise<number> {
    const now = new Date().toISOString();
    const { count, error } = await this.client
      .from("review_items")
      .select("*", { count: "exact", head: true })
      .eq("user_id", this.userId)
      .eq("profile_id", profileId)
      .or(`due_at.is.null,due_at.lte.${now}`);
    if (error) throw new Error(`due count failed: ${error.message}`);
    return count ?? 0;
  }

  async recordReview(reviewItemId: string, input: RecordReviewInput): Promise<ReviewItem> {
    const now = new Date();
    const ts = now.toISOString();

    const { data: existing, error: fetchErr } = await this.client
      .from("review_items")
      .select("*")
      .eq("id", reviewItemId)
      .eq("user_id", this.userId)
      .single<ReviewItemRow>();
    if (fetchErr || !existing) throw new Error(`review item ${reviewItemId} not found`);

    const current = rowToReviewItem(existing, "");
    const next = this.scheduler.schedule(current.state, { rating: input.rating }, now);

    const { error } = await this.client.rpc("record_review_and_result", {
      p_review_item_id: reviewItemId,
      p_difficulty: next.difficulty,
      p_stability: next.stability,
      p_retrievability: next.retrievability,
      p_state: next.state,
      p_due_at: next.dueAt,
      p_last_reviewed_at: next.lastReviewedAt,
      p_lapses: next.lapses,
      p_reps: next.reps,
      p_rating: input.rating,
      p_response_ms: input.responseMs ?? null,
      p_confidence: input.confidence ?? null,
      p_reviewed_at: ts,
    });
    if (error) throw new Error(`review record failed: ${error.message}`);

    return { ...current, state: next };
  }
}

class SupabaseConversationRepo implements IConversationRepo {
  constructor(
    private readonly client: SupabaseClient,
    private readonly userId: string,
  ) {}

  async create(profileId: string, mode: string, scenario?: string, title?: string): Promise<ConversationRecord> {
    const { data, error } = await this.client
      .from("conversations")
      .insert({ user_id: this.userId, profile_id: profileId, mode, scenario: scenario ?? null, title: title ?? null })
      .select()
      .single<{ id: string; mode: string; scenario: string | null; title: string | null; created_at: string }>();
    if (error || !data) throw new Error(`conversation create failed: ${error?.message}`);
    return {
      id: data.id,
      profileId,
      mode: data.mode,
      scenario: data.scenario,
      title: data.title,
      createdAt: data.created_at,
    };
  }

  async appendMessage(
    conversationId: string,
    role: StoredMessage["role"],
    content: string,
    options: { correctionJson?: string; tokens?: number } = {},
  ): Promise<void> {
    const ts = new Date().toISOString();
    const { error } = await this.client.from("ai_messages").insert({
      conversation_id: conversationId,
      user_id: this.userId,
      role,
      content,
      correction_json: options.correctionJson ?? null,
      tokens: options.tokens ?? null,
    });
    if (error) throw new Error(`message append failed: ${error.message}`);

    const { error: touchErr } = await this.client
      .from("conversations")
      .update({ updated_at: ts })
      .eq("id", conversationId)
      .eq("user_id", this.userId);
    if (touchErr) throw new Error(`conversation touch failed: ${touchErr.message}`);
  }

  async listMessages(conversationId: string): Promise<StoredMessage[]> {
    const { data, error } = await this.client
      .from("ai_messages")
      .select("id, role, content, correction_json, created_at")
      .eq("conversation_id", conversationId)
      .eq("user_id", this.userId)
      .order("created_at", { ascending: true })
      .returns<{ id: string; role: string; content: string; correction_json: string | null; created_at: string }[]>();
    if (error) throw new Error(`messages fetch failed: ${error.message}`);
    return (data ?? []).map((r) => ({
      id: r.id,
      role: r.role as StoredMessage["role"],
      content: r.content,
      correctionJson: r.correction_json,
      createdAt: r.created_at,
    }));
  }

  async listConversations(profileId: string): Promise<ConversationRecord[]> {
    const { data, error } = await this.client
      .from("conversations")
      .select("id, mode, scenario, title, created_at")
      .eq("user_id", this.userId)
      .eq("profile_id", profileId)
      .order("created_at", { ascending: false })
      .returns<{ id: string; mode: string; scenario: string | null; title: string | null; created_at: string }[]>();
    if (error) throw new Error(`conversations fetch failed: ${error.message}`);
    return (data ?? []).map((r) => ({
      id: r.id,
      profileId,
      mode: r.mode,
      scenario: r.scenario,
      title: r.title,
      createdAt: r.created_at,
    }));
  }
}

class SupabasePronunciationRepo implements IPronunciationRepo {
  constructor(
    private readonly client: SupabaseClient,
    private readonly userId: string,
  ) {}

  async record(
    profileId: string,
    input: { targetText: string; transcript?: string; score?: number; audioPath?: string },
  ): Promise<PronunciationAttempt> {
    // audioPath is accepted for interface parity but never persisted anywhere in this app —
    // audio bytes stay in-memory only, matching the local implementation's behavior.
    const { data, error } = await this.client
      .from("pronunciation_attempts")
      .insert({
        user_id: this.userId,
        profile_id: profileId,
        target_text: input.targetText,
        transcript: input.transcript ?? null,
        score: input.score ?? null,
      })
      .select()
      .single<{ id: string; target_text: string; transcript: string | null; score: number | null; created_at: string }>();
    if (error || !data) throw new Error(`pronunciation record failed: ${error?.message}`);
    return {
      id: data.id,
      targetText: data.target_text,
      transcript: data.transcript,
      score: data.score,
      createdAt: data.created_at,
    };
  }

  async listRecent(profileId: string, limit = 20): Promise<PronunciationAttempt[]> {
    const { data, error } = await this.client
      .from("pronunciation_attempts")
      .select("id, target_text, transcript, score, created_at")
      .eq("user_id", this.userId)
      .eq("profile_id", profileId)
      .order("created_at", { ascending: false })
      .limit(limit)
      .returns<{ id: string; target_text: string; transcript: string | null; score: number | null; created_at: string }[]>();
    if (error) throw new Error(`pronunciation fetch failed: ${error.message}`);
    return (data ?? []).map((r) => ({
      id: r.id,
      targetText: r.target_text,
      transcript: r.transcript,
      score: r.score,
      createdAt: r.created_at,
    }));
  }
}
