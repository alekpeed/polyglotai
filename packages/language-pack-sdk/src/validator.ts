import type { Lesson } from "@polyglotai/shared-types";
import { MVP_SEED_PACK_TARGET, type ContentVolumeReport } from "./contentTargets.js";
import { parsePack, semanticErrors, type ParsedPackData } from "./parse.js";
import type { PackFileReader } from "./reader.js";

export interface ValidationReport {
  valid: boolean;
  errors: string[];
  volumeReport: ContentVolumeReport[];
}

/** Content counts vs. the official §10.1 MVP Seed Pack Target (Tier 1). */
export function volumeReport(data: ParsedPackData): ContentVolumeReport[] {
  const byLessonType = (type: Lesson["lessonType"]) =>
    data.lessons.filter((l) => l.lessonType === type).length;

  const actuals: Record<keyof typeof MVP_SEED_PACK_TARGET, number> = {
    vocabulary: data.vocabulary.filter((v) => v.entryType !== "phrase").length,
    phrases: data.vocabulary.filter((v) => v.entryType === "phrase").length,
    grammar: data.grammar.length,
    dialogues: data.dialogues.length,
    slangRegister: data.realSpeech.length,
    pronunciationDrills: data.pronunciation.length,
    listeningExercises: byLessonType("listening_exercise"),
    writingPrompts: byLessonType("writing_prompt"),
    assessments: byLessonType("assessment"),
    roleplayScenarios: byLessonType("roleplay"),
  };

  return Object.entries(MVP_SEED_PACK_TARGET).map(([category, target]) => {
    const key = category as keyof typeof MVP_SEED_PACK_TARGET;
    return { category: key, target, actual: actuals[key], meetsTarget: actuals[key] >= target };
  });
}

/**
 * Validates a language pack directory: JSON-schema conformance (via parsePack), semantic
 * checks (duplicate keys, dangling cross-references), and a content-volume report against
 * §10.1. Takes a PackFileReader so it runs from a Node CLI, the Tauri fs plugin, or a future
 * Android reader (plan §2).
 */
export async function validatePack(reader: PackFileReader): Promise<ValidationReport> {
  let parsed;
  try {
    parsed = await parsePack(reader);
  } catch (err) {
    // Only an unreadable/invalid manifest reaches here; no data to volume-report.
    return { valid: false, errors: [(err as Error).message], volumeReport: [] };
  }

  const errors = [...parsed.errors, ...semanticErrors(parsed.data)];
  return { valid: errors.length === 0, errors, volumeReport: volumeReport(parsed.data) };
}
