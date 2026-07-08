import type { Database, SqlValue } from "../db/database.js";

export interface InstalledPack {
  id: string;
  name: string;
  languageCode: string;
  version: string;
  basePackId: string | null;
  installedAt: string;
}

interface PackRow extends Record<string, SqlValue> {
  id: string;
  name: string;
  language_code: string;
  version: string;
  base_pack_id: string | null;
  installed_at: string;
}

/** Reads installed packs from the DB. Write side is importPack; this is the query side. */
export class PackRegistry {
  constructor(private readonly db: Database) {}

  async list(): Promise<InstalledPack[]> {
    const rows = await this.db.all<PackRow>(
      "SELECT id, name, language_code, version, base_pack_id, installed_at FROM language_packs ORDER BY name",
    );
    return rows.map(this.toInstalledPack);
  }

  async get(id: string): Promise<InstalledPack | null> {
    const rows = await this.db.all<PackRow>(
      "SELECT id, name, language_code, version, base_pack_id, installed_at FROM language_packs WHERE id = ?",
      [id],
    );
    const row = rows[0];
    return row ? this.toInstalledPack(row) : null;
  }

  private toInstalledPack(row: PackRow): InstalledPack {
    return {
      id: row.id,
      name: row.name,
      languageCode: row.language_code,
      version: row.version,
      basePackId: row.base_pack_id,
      installedAt: row.installed_at,
    };
  }
}
