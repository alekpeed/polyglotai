import type { TTSProvider } from "../index.js";

export interface OpenAiTtsProviderOptions {
  apiKey: string;
  model?: string;
  /** OpenAI voice name (e.g. "nova", "alloy") — voices aren't language-specific; the model
   * detects the spoken language from the input text itself. */
  voice?: string;
  baseUrl?: string;
  fetchFn?: typeof fetch;
}

/**
 * OpenAI TTS adapter (spec §12 TTSProvider). Defaults to gpt-4o-mini-tts, OpenAI's
 * cost-efficient tier (~$0.015/min) — plenty for spoken tutor/interpreter lines played back
 * over a laptop speaker; the HD tier isn't worth 2x the cost for this use case.
 */
export class OpenAiTtsProvider implements TTSProvider {
  readonly name = "openai-tts";
  private readonly apiKey: string;
  private readonly model: string;
  private readonly voice: string;
  private readonly baseUrl: string;
  private readonly fetchFn: typeof fetch;

  constructor(options: OpenAiTtsProviderOptions) {
    if (!options.apiKey) throw new Error("OpenAiTtsProvider requires an apiKey");
    this.apiKey = options.apiKey;
    this.model = options.model ?? "gpt-4o-mini-tts";
    this.voice = options.voice ?? "nova";
    this.baseUrl = options.baseUrl ?? "https://api.openai.com/v1";
    // Wrap (not just reference) global fetch — native fetch throws "Illegal invocation" when
    // called as a method of this instance (this.fetchFn(...)); the arrow calls it bare.
    this.fetchFn = options.fetchFn ?? ((input, init) => fetch(input, init));
  }

  async synthesize(text: string, voice?: { languageCode: string; accentHint?: string }): Promise<Blob> {
    // gpt-4o-mini-tts (unlike tts-1/tts-1-hd) accepts a free-text "instructions" field that
    // steers delivery — without it, the model defaults to reading foreign-language text in an
    // American accent rather than a native one. Kept deliberately short: gpt-4o-mini-tts
    // generates audio more like an LLM than a classical TTS engine, and a longer instructions
    // string competing with the input text was observed to cut playback off mid-sentence.
    const instructions = voice?.accentHint ? `Native ${voice.accentHint} accent.` : undefined;
    const response = await this.fetchFn(`${this.baseUrl}/audio/speech`, {
      method: "POST",
      headers: { "content-type": "application/json", authorization: `Bearer ${this.apiKey}` },
      body: JSON.stringify({
        model: this.model,
        voice: this.voice,
        input: text,
        response_format: "mp3",
        ...(instructions ? { instructions } : {}),
      }),
    });

    if (!response.ok) {
      const body = (await response.text()).slice(0, 400);
      throw new Error(`TTS request failed (${response.status}): ${body}`);
    }

    return response.blob();
  }
}
