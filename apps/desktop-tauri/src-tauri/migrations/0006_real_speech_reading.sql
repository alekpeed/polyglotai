-- Adds reading/romaji columns to real_speech_items, mirroring migration 0005's addition to
-- vocabulary_items. Needed now that a non-Latin-script pack (Japanese) has slang/profanity
-- content -- pt-br's existing slang/profanity never needed these since Portuguese is
-- Latin-script and self-sufficient. Optional/NULL for existing pt-br rows.

ALTER TABLE real_speech_items ADD COLUMN reading TEXT;
ALTER TABLE real_speech_items ADD COLUMN romaji TEXT;

UPDATE schema_meta SET value = '6' WHERE key = 'db_version';
