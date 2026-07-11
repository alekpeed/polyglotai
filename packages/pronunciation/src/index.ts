/**
 * SpeechProvider (STT) — spec §17 Phase 1: record, transcribe, diff against target text for
 * a basic correctness score. Implemented via an OpenAI Whisper adapter in Milestone C step 14.
 */
export interface SpeechProvider {
  readonly name: string;
  transcribe(audio: Blob): Promise<string>;
}

/**
 * TTSProvider — spec §12. Implemented by OpenAiTtsProvider (providers/tts.ts). Dialect-specific
 * voice selection (§12) is still Phase 4 work; the current adapter uses one fixed voice.
 */
export interface TTSProvider {
  readonly name: string;
  synthesize(text: string, voice: { languageCode: string }): Promise<Blob>;
}

export * from "./score.js";
export * from "./providers/whisper.js";
export * from "./providers/tts.js";
