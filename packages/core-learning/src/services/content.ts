import type { Register, Severity } from "@polyglotai/shared-types";
import type { SqlValue } from "../db/database.js";
import type { Repos } from "../repos.js";

export interface VocabularyEntry {
  key: string;
  entryType: string;
  lemma: string;
  reading: string | null;
  romaji: string | null;
  translation: string;
  register: string | null;
  cefr: string | null;
  tags: string[];
}

export interface GrammarEntry {
  key: string;
  title: string;
  cefr: string | null;
  explanationMd: string;
}

export interface RealSpeechEntry {
  key: string;
  kind: string;
  phrase: string;
  reading: string | null;
  romaji: string | null;
  natural: string | null;
  register: Register;
  severity: Severity;
  learnerShouldUse: string | null;
  culturalWarning: string | null;
  /** Whether this item is within the learner's chosen real-speech comfort ceiling (spec §6/§13). */
  withinComfort: boolean;
}

export async function listVocabulary(repos: Repos, packId: string): Promise<VocabularyEntry[]> {
  const rows = await repos.db.all<{
    item_key: string;
    entry_type: string;
    lemma: string;
    reading: string | null;
    romaji: string | null;
    translation: string;
    register: string | null;
    cefr: string | null;
    tags_json: string;
  }>(
    "SELECT item_key, entry_type, lemma, reading, romaji, translation, register, cefr, tags_json FROM vocabulary_items WHERE pack_id = ? ORDER BY entry_type, lemma",
    [packId as SqlValue],
  );
  return rows.map((r) => ({
    key: r.item_key,
    entryType: r.entry_type,
    lemma: r.lemma,
    reading: r.reading,
    romaji: r.romaji,
    translation: r.translation,
    register: r.register,
    cefr: r.cefr,
    tags: JSON.parse(r.tags_json) as string[],
  }));
}

export async function listGrammar(repos: Repos, packId: string): Promise<GrammarEntry[]> {
  const rows = await repos.db.all<{
    item_key: string;
    title: string;
    cefr: string | null;
    explanation_md: string;
  }>("SELECT item_key, title, cefr, explanation_md FROM grammar_items WHERE pack_id = ? ORDER BY cefr, title", [
    packId as SqlValue,
  ]);
  return rows.map((r) => ({ key: r.item_key, title: r.title, cefr: r.cefr, explanationMd: r.explanation_md }));
}

/**
 * Lists slang/profanity/idiom items, tagging each with whether it falls within the learner's
 * severity ceiling. Items are returned ordered by severity so the library reads from mild to
 * strong; the screen shows within-comfort items in full and locks the rest (teaching the
 * control/judgment goal of spec §3, not hiding the language's existence).
 */
export async function listRealSpeech(
  repos: Repos,
  packId: string,
  ceiling: Severity,
): Promise<RealSpeechEntry[]> {
  const rows = await repos.db.all<{
    item_key: string;
    kind: string;
    phrase: string;
    reading: string | null;
    romaji: string | null;
    natural: string | null;
    register: string;
    severity: number;
    learner_should_use: string | null;
    cultural_warning: string | null;
  }>(
    "SELECT item_key, kind, phrase, reading, romaji, natural, register, severity, learner_should_use, cultural_warning FROM real_speech_items WHERE pack_id = ? ORDER BY severity, phrase",
    [packId as SqlValue],
  );
  return rows.map((r) => ({
    key: r.item_key,
    kind: r.kind,
    phrase: r.phrase,
    reading: r.reading,
    romaji: r.romaji,
    natural: r.natural,
    register: r.register as Register,
    severity: r.severity as Severity,
    learnerShouldUse: r.learner_should_use,
    culturalWarning: r.cultural_warning,
    withinComfort: r.severity <= ceiling,
  }));
}
