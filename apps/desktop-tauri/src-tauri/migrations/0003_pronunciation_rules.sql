-- Adds a content table for pronunciation drills/rules (spec §11; §10.1 counts 20 of these).
-- The original §4 schema had pronunciation_attempts (user recordings) but no table for the
-- pack-authored pronunciation *content* itself — this closes that gap. Additive migration,
-- exercising the versioned-schema upgrade path (plan risk 4).

CREATE TABLE pronunciation_rules (
  id            TEXT PRIMARY KEY,
  pack_id       TEXT NOT NULL REFERENCES language_packs(id),
  item_key      TEXT NOT NULL,
  grapheme      TEXT NOT NULL,
  ipa           TEXT NOT NULL,
  description   TEXT NOT NULL,
  minimal_pairs_json TEXT NOT NULL DEFAULT '[]',
  data_json     TEXT NOT NULL DEFAULT '{}',
  schema_version INTEGER NOT NULL,
  created_at    TEXT NOT NULL,
  updated_at    TEXT NOT NULL,
  UNIQUE(pack_id, item_key)
);
CREATE INDEX idx_pronunciation_rules_pack ON pronunciation_rules(pack_id);

UPDATE schema_meta SET value = '2' WHERE key = 'db_version';
