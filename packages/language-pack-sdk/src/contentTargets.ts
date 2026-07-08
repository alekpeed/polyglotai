/**
 * Official MVP Seed Pack Target — spec §10.1, restated in §23. This is Tier 1; §10.2-§10.6
 * are post-MVP roadmap targets and are NOT checked here.
 */
export const MVP_SEED_PACK_TARGET = {
  vocabulary: 300,
  phrases: 75,
  grammar: 25,
  dialogues: 25,
  slangRegister: 20, // slang + profanity + idioms combined (plan §6)
  pronunciationDrills: 20,
  listeningExercises: 10,
  writingPrompts: 10,
  assessments: 10,
  roleplayScenarios: 5,
} as const;

export type ContentCategory = keyof typeof MVP_SEED_PACK_TARGET;

export interface ContentVolumeReport {
  category: ContentCategory;
  target: number;
  actual: number;
  meetsTarget: boolean;
}
