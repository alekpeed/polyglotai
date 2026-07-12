import {
  REAL_SPEECH_LEVEL_DEFAULT_SEVERITY,
  type CorrectionStrictness,
  type LearnerGoal,
  type LearnerProfile,
  type RealSpeechLevel,
  type Severity,
} from "@polyglotai/shared-types";
import type { Database, SqlValue } from "../db/database.js";
import { newId } from "../db/ids.js";

export interface ProfileCreateInput {
  displayName: string;
  goal?: LearnerGoal;
  targetDialect?: string;
  realSpeechLevel?: RealSpeechLevel;
  correctionStrictness?: CorrectionStrictness;
  activePackId?: string | null;
}

/** Fields a learner can change after onboarding. */
export interface ProfileUpdate {
  displayName?: string;
  goal?: LearnerGoal;
  targetDialect?: string | null;
  realSpeechLevel?: RealSpeechLevel;
  slangSeverityOverride?: Severity | null;
  cefrEstimate?: LearnerProfile["cefrEstimate"];
  correctionStrictness?: CorrectionStrictness;
  activePackId?: string | null;
  settings?: Record<string, unknown>;
}

interface ProfileRow extends Record<string, SqlValue> {
  id: string;
  display_name: string;
  active_pack_id: string | null;
  goal: string | null;
  target_dialect: string | null;
  real_speech_level: string;
  slang_severity_override: number | null;
  cefr_estimate: string | null;
  correction_strictness: string;
  settings_json: string;
  schema_version: number;
  created_at: string;
  updated_at: string;
}

function rowToProfile(row: ProfileRow): LearnerProfile {
  return {
    id: row.id,
    displayName: row.display_name,
    activePackId: row.active_pack_id,
    goal: (row.goal as LearnerGoal | null) ?? undefined,
    targetDialect: row.target_dialect ?? undefined,
    realSpeechLevel: row.real_speech_level as RealSpeechLevel,
    slangSeverityOverride: (row.slang_severity_override as Severity | null) ?? undefined,
    cefrEstimate: (row.cefr_estimate as LearnerProfile["cefrEstimate"]) ?? undefined,
    correctionStrictness: row.correction_strictness as CorrectionStrictness,
    settings: JSON.parse(row.settings_json) as Record<string, unknown>,
    schemaVersion: 1,
    createdAt: row.created_at,
    updatedAt: row.updated_at,
  };
}

/** Public shape of ProfileRepo — split out so a non-SQL backend (e.g. a cloud-account
 * implementation over Supabase Postgres) can satisfy the same Repos["profiles"] type; TS
 * classes with private fields aren't structurally assignable otherwise. */
export interface IProfileRepo {
  create(input: ProfileCreateInput): Promise<LearnerProfile>;
  get(id: string): Promise<LearnerProfile | null>;
  getFirst(): Promise<LearnerProfile | null>;
  update(id: string, patch: ProfileUpdate): Promise<LearnerProfile>;
}

/**
 * The single local learner profile (spec §5.1, §6). Multi-profile is a later phase; the
 * schema already carries profile_id FKs so it stays additive (plan risk 8).
 */
export class ProfileRepo implements IProfileRepo {
  constructor(
    private readonly db: Database,
    private readonly now: () => string = () => new Date().toISOString(),
  ) {}

  async create(input: ProfileCreateInput): Promise<LearnerProfile> {
    const ts = this.now();
    const id = newId();
    await this.db.run(
      `INSERT INTO learner_profiles
         (id, display_name, active_pack_id, goal, target_dialect, real_speech_level,
          correction_strictness, settings_json, schema_version, created_at, updated_at)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        id,
        input.displayName,
        input.activePackId ?? null,
        input.goal ?? null,
        input.targetDialect ?? null,
        input.realSpeechLevel ?? "informal",
        input.correctionStrictness ?? "balanced",
        "{}",
        1,
        ts,
        ts,
      ],
    );
    const created = await this.get(id);
    if (!created) throw new Error("profile insert did not persist");
    return created;
  }

  async get(id: string): Promise<LearnerProfile | null> {
    const rows = await this.db.all<ProfileRow>("SELECT * FROM learner_profiles WHERE id = ?", [id]);
    return rows[0] ? rowToProfile(rows[0]) : null;
  }

  /** Convenience for the single-profile MVP: the oldest (and normally only) profile. */
  async getFirst(): Promise<LearnerProfile | null> {
    const rows = await this.db.all<ProfileRow>(
      "SELECT * FROM learner_profiles ORDER BY created_at, id LIMIT 1",
    );
    return rows[0] ? rowToProfile(rows[0]) : null;
  }

  async update(id: string, patch: ProfileUpdate): Promise<LearnerProfile> {
    const sets: string[] = [];
    const params: SqlValue[] = [];
    const set = (col: string, value: SqlValue) => {
      sets.push(`${col} = ?`);
      params.push(value);
    };

    if (patch.displayName !== undefined) set("display_name", patch.displayName);
    if (patch.goal !== undefined) set("goal", patch.goal);
    if (patch.targetDialect !== undefined) set("target_dialect", patch.targetDialect);
    if (patch.realSpeechLevel !== undefined) set("real_speech_level", patch.realSpeechLevel);
    if (patch.slangSeverityOverride !== undefined)
      set("slang_severity_override", patch.slangSeverityOverride);
    if (patch.cefrEstimate !== undefined) set("cefr_estimate", patch.cefrEstimate ?? null);
    if (patch.correctionStrictness !== undefined)
      set("correction_strictness", patch.correctionStrictness);
    if (patch.activePackId !== undefined) set("active_pack_id", patch.activePackId);
    if (patch.settings !== undefined) set("settings_json", JSON.stringify(patch.settings));

    set("updated_at", this.now());
    params.push(id);
    await this.db.run(`UPDATE learner_profiles SET ${sets.join(", ")} WHERE id = ?`, params);

    const updated = await this.get(id);
    if (!updated) throw new Error(`profile ${id} not found`);
    return updated;
  }
}

/**
 * The severity ceiling (1–7, spec §13) a profile is comfortable seeing: an explicit override
 * wins, otherwise the default for the chosen real-speech level (spec §6 step 6). The slang/
 * register service uses this to gate which profanity items are shown vs hidden (step 8).
 */
export function effectiveSeverityCeiling(profile: LearnerProfile): Severity {
  return profile.slangSeverityOverride ?? REAL_SPEECH_LEVEL_DEFAULT_SEVERITY[profile.realSpeechLevel];
}
