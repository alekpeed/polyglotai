import { parsePack, semanticErrors, type ParsedPackData } from "./parse.js";
import type { PackFileReader } from "./reader.js";

/** A fully validated, in-memory pack ready to import. DB-free — core-learning imports it. */
export type LoadedPack = ParsedPackData;

export class PackValidationError extends Error {
  constructor(public readonly errors: string[]) {
    super(`pack failed validation:\n  - ${errors.join("\n  - ")}`);
    this.name = "PackValidationError";
  }
}

/**
 * Loads and fully validates a pack, returning its in-memory contents. Throws
 * PackValidationError if any schema or semantic error is found — a caller that wants a report
 * instead of an exception should use validatePack. This is the entry point core-learning's
 * importer consumes (plan §3: language-pack-sdk loads/validates; core-learning writes to DB).
 */
export async function loadPack(reader: PackFileReader): Promise<LoadedPack> {
  const { data, errors: parseErrors } = await parsePack(reader);
  const errors = [...parseErrors, ...semanticErrors(data)];
  if (errors.length > 0) throw new PackValidationError(errors);
  return data;
}

/**
 * Resolves pack inheritance (spec §9 regional variants; also how a "micro-pack" — e.g. a
 * situational/cultural add-on scoped to one interest area — inherits its base language's
 * grammar/pronunciation without redefining them): child items override base items by `key`,
 * new child keys extend. The merged pack carries the child's manifest. Base and child are each
 * independently loaded (and independently valid) before this pure merge runs — the app-side
 * caller (bootstrap.ts's loadPackForId) resolves `manifest.basePack` and calls this.
 */
export function mergePacks(base: LoadedPack, child: LoadedPack): LoadedPack {
  const mergeByKey = <T extends { key: string }>(baseItems: T[], childItems: T[]): T[] => {
    const byKey = new Map<string, T>();
    for (const item of baseItems) byKey.set(item.key, item);
    for (const item of childItems) byKey.set(item.key, item);
    return [...byKey.values()];
  };

  return {
    manifest: child.manifest,
    vocabulary: mergeByKey(base.vocabulary, child.vocabulary),
    grammar: mergeByKey(base.grammar, child.grammar),
    realSpeech: mergeByKey(base.realSpeech, child.realSpeech),
    dialogues: mergeByKey(base.dialogues, child.dialogues),
    pronunciation: mergeByKey(base.pronunciation, child.pronunciation),
    lessons: mergeByKey(base.lessons, child.lessons),
    aiPrompts: mergeByKey(base.aiPrompts, child.aiPrompts),
    culture: mergeByKey(base.culture, child.culture),
  };
}
