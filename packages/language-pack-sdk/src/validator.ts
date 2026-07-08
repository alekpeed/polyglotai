import {
  DialogueSchema,
  GrammarItemSchema,
  LessonSchema,
  ManifestSchema,
  PronunciationRuleSchema,
  RealSpeechItemSchema,
  VocabularyItemSchema,
  type Dialogue,
  type GrammarItem,
  type Lesson,
  type Manifest,
  type RealSpeechItem,
  type VocabularyItem,
} from "@polyglotai/shared-types";
import { MVP_SEED_PACK_TARGET, type ContentVolumeReport } from "./contentTargets.js";
import type { PackFileReader } from "./reader.js";

export interface ValidationReport {
  valid: boolean;
  errors: string[];
  volumeReport: ContentVolumeReport[];
}

async function readJsonArray(reader: PackFileReader, path: string): Promise<unknown[]> {
  const text = await reader.readText(path);
  const parsed = JSON.parse(text);
  if (!Array.isArray(parsed)) {
    throw new Error(`${path}: expected a JSON array of items`);
  }
  return parsed;
}

/**
 * Validates a language pack directory against the shared-types Zod schemas plus semantic
 * checks (duplicate keys within a category, dangling cross-references), and reports content
 * volume against the §10.1 MVP Seed Pack Target. Takes a PackFileReader so it works from a
 * Node CLI, the Tauri app's fs plugin, or a future Android reader — see plan §2.
 */
export async function validatePack(reader: PackFileReader): Promise<ValidationReport> {
  const errors: string[] = [];
  let manifest: Manifest;

  try {
    manifest = ManifestSchema.parse(JSON.parse(await reader.readText("manifest.json")));
  } catch (err) {
    return {
      valid: false,
      errors: [`manifest.json: ${(err as Error).message}`],
      volumeReport: [],
    };
  }

  const vocabItems: VocabularyItem[] = [];
  const grammarItems: GrammarItem[] = [];
  const realSpeechItems: RealSpeechItem[] = [];
  const dialogues: Dialogue[] = [];
  const lessons: Lesson[] = [];
  let pronunciationDrillCount = 0;

  const loadCategory = async <T>(
    files: string[] | undefined,
    schema: { parse: (v: unknown) => T },
    sink: T[],
  ) => {
    for (const file of files ?? []) {
      let rows: unknown[];
      try {
        rows = await readJsonArray(reader, file);
      } catch (err) {
        errors.push(`${file}: ${(err as Error).message}`);
        continue;
      }
      for (const [i, row] of rows.entries()) {
        try {
          sink.push(schema.parse(row));
        } catch (err) {
          errors.push(`${file}[${i}]: ${(err as Error).message}`);
        }
      }
    }
  };

  await loadCategory(manifest.contents.vocabulary, VocabularyItemSchema, vocabItems);
  await loadCategory(manifest.contents.phrases, VocabularyItemSchema, vocabItems);
  await loadCategory(manifest.contents.grammar, GrammarItemSchema, grammarItems);
  await loadCategory(manifest.contents.slang, RealSpeechItemSchema, realSpeechItems);
  await loadCategory(manifest.contents.profanity, RealSpeechItemSchema, realSpeechItems);
  await loadCategory(manifest.contents.idioms, RealSpeechItemSchema, realSpeechItems);
  await loadCategory(manifest.contents.dialogues, DialogueSchema, dialogues);
  await loadCategory(manifest.contents.lessons, LessonSchema, lessons);
  await loadCategory(manifest.contents.assessments, LessonSchema, lessons);

  for (const file of manifest.contents.pronunciation ?? []) {
    try {
      const rows = await readJsonArray(reader, file);
      for (const [i, row] of rows.entries()) {
        try {
          PronunciationRuleSchema.parse(row);
          pronunciationDrillCount += 1;
        } catch (err) {
          errors.push(`${file}[${i}]: ${(err as Error).message}`);
        }
      }
    } catch (err) {
      errors.push(`${file}: ${(err as Error).message}`);
    }
  }

  // Semantic check: duplicate keys within a category.
  const checkDuplicates = (items: Array<{ key: string }>, label: string) => {
    const seen = new Set<string>();
    for (const item of items) {
      if (seen.has(item.key)) errors.push(`duplicate ${label} key: ${item.key}`);
      seen.add(item.key);
    }
  };
  checkDuplicates(vocabItems, "vocabulary");
  checkDuplicates(grammarItems, "grammar");
  checkDuplicates(realSpeechItems, "slang/profanity/idiom");
  checkDuplicates(dialogues, "dialogue");
  checkDuplicates(lessons, "lesson");

  // Semantic check: dangling references (grammar.relatedVocabulary, dialogue.keyVocabulary,
  // lesson.dialogueRef) must point at keys that actually exist in this pack.
  const vocabKeys = new Set(vocabItems.map((v) => v.key));
  const dialogueKeys = new Set(dialogues.map((d) => d.key));
  for (const g of grammarItems) {
    for (const ref of g.relatedVocabulary) {
      if (!vocabKeys.has(ref)) errors.push(`grammar ${g.key}: dangling relatedVocabulary "${ref}"`);
    }
  }
  for (const d of dialogues) {
    for (const ref of d.keyVocabulary) {
      if (!vocabKeys.has(ref)) errors.push(`dialogue ${d.key}: dangling keyVocabulary "${ref}"`);
    }
  }
  for (const l of lessons) {
    if (l.dialogueRef && !dialogueKeys.has(l.dialogueRef)) {
      errors.push(`lesson ${l.key}: dangling dialogueRef "${l.dialogueRef}"`);
    }
  }

  const vocabularyCount = vocabItems.filter((v) => v.entryType !== "phrase").length;
  const phraseCount = vocabItems.filter((v) => v.entryType === "phrase").length;
  const byLessonType = (type: Lesson["lessonType"]) =>
    lessons.filter((l) => l.lessonType === type).length;

  const actuals: Record<keyof typeof MVP_SEED_PACK_TARGET, number> = {
    vocabulary: vocabularyCount,
    phrases: phraseCount,
    grammar: grammarItems.length,
    dialogues: dialogues.length,
    slangRegister: realSpeechItems.length,
    pronunciationDrills: pronunciationDrillCount,
    listeningExercises: byLessonType("listening_exercise"),
    writingPrompts: byLessonType("writing_prompt"),
    assessments: byLessonType("assessment"),
    roleplayScenarios: byLessonType("roleplay"),
  };

  const volumeReport: ContentVolumeReport[] = Object.entries(MVP_SEED_PACK_TARGET).map(
    ([category, target]) => ({
      category: category as keyof typeof MVP_SEED_PACK_TARGET,
      target,
      actual: actuals[category as keyof typeof MVP_SEED_PACK_TARGET],
      meetsTarget: actuals[category as keyof typeof MVP_SEED_PACK_TARGET] >= target,
    }),
  );

  return { valid: errors.length === 0, errors, volumeReport };
}
