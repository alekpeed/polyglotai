import type { Database, SqlValue } from "../db/database.js";

/** MVP feature-flag keys (plan Appendix; all flaggable per spec §8). */
export type FeatureFlagKey =
  | "ai_conversation"
  | "slang_mode"
  | "profanity_explanations"
  | "pronunciation_recording"
  | "conversation_logging"
  | "teacher_dashboard"
  | "cloud_sync"
  | "billing"
  | "experimental_packs";

interface FlagRow extends Record<string, SqlValue> {
  key: string;
  enabled: number;
}

/**
 * Reads/writes the feature_flags table (seeded with MVP defaults by migration 0002).
 * Runtime-toggleable from Settings; unknown/unseeded keys read as disabled.
 */
export class FeatureFlagRegistry {
  constructor(
    private readonly db: Database,
    private readonly now: () => string = () => new Date().toISOString(),
  ) {}

  async isEnabled(key: FeatureFlagKey): Promise<boolean> {
    const rows = await this.db.all<FlagRow>("SELECT enabled FROM feature_flags WHERE key = ?", [key]);
    return rows[0]?.enabled === 1;
  }

  async all(): Promise<Record<string, boolean>> {
    const rows = await this.db.all<FlagRow>("SELECT key, enabled FROM feature_flags");
    return Object.fromEntries(rows.map((r) => [r.key, r.enabled === 1]));
  }

  async setEnabled(key: FeatureFlagKey, enabled: boolean): Promise<void> {
    await this.db.run(
      `INSERT INTO feature_flags (key, enabled, updated_at) VALUES (?, ?, ?)
       ON CONFLICT(key) DO UPDATE SET enabled = excluded.enabled, updated_at = excluded.updated_at`,
      [key, enabled ? 1 : 0, this.now()],
    );
  }
}
