-- Adds reading/romaji columns to vocabulary_items for non-Latin-script packs (e.g. Japanese
-- kana/kanji lemma + hiragana reading + romanization). Optional/NULL for existing packs whose
-- lemma is already Latin-script and self-sufficient (pt-br). Examples and dialogue transcript
-- lines get the same two fields, but those live in existing *_json columns, so no migration is
-- needed there — the new optional fields just ride through the JSON blob once the pack schema
-- (shared-types) grows them.

ALTER TABLE vocabulary_items ADD COLUMN reading TEXT;
ALTER TABLE vocabulary_items ADD COLUMN romaji TEXT;

UPDATE schema_meta SET value = '5' WHERE key = 'db_version';
