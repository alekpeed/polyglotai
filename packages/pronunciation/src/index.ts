/**
 * SpeechProvider (STT) — spec §17 Phase 1: record, transcribe, diff against target text for
 * a basic correctness score. Implemented via an OpenAI Whisper adapter in Milestone C step 14.
 */
export interface SpeechProvider {
  readonly name: string;
  transcribe(audio: Blob): Promise<string>;
}

/**
 * TTSProvider — spec §12. Implemented by OpenAiTtsProvider (providers/tts.ts). `accentHint` is
 * a natural-language steer (e.g. "native Brazilian Portuguese accent, São Paulo dialect") —
 * OpenAI's voices are English-centric by default and read foreign-language text with an
 * American accent unless explicitly told otherwise. Full per-dialect voice selection (spec §12)
 * is still Phase 4 work; this is a same-voice, prompted-accent stopgap.
 */
export interface TTSProvider {
  readonly name: string;
  synthesize(text: string, voice: { languageCode: string; accentHint?: string }): Promise<Blob>;
}

export * from "./score.js";
export * from "./providers/whisper.js";
export * from "./providers/tts.js";
