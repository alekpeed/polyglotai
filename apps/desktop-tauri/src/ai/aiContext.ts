import { useEffect, useState } from "react";
import {
  CorrectionEngine,
  DEFAULT_CONVERSATION_TEMPLATE,
  OpenAIProvider,
  renderTemplate,
  type AIProvider,
  type LearnerContext,
} from "@polyglotai/ai-orchestration";
import { OpenAiTtsProvider, WhisperProvider, type SpeechProvider, type TTSProvider } from "@polyglotai/pronunciation";
import { effectiveSeverityCeiling, type Repos } from "@polyglotai/core-learning";
import type { LoadedPack } from "@polyglotai/language-pack-sdk";
import type { LearnerProfile } from "@polyglotai/shared-types";

/** The deployed supabase/functions/openai-proxy URL (e.g.
 * "https://xxxx.supabase.co/functions/v1/openai-proxy") — the app never holds an OpenAI key
 * directly; see supabase/README.md. Set at build time; unset just means AI features are
 * unavailable in that build. */
const PROXY_BASE_URL = (import.meta.env.VITE_AI_PROXY_URL as string | undefined)?.replace(/\/$/, "");

/** Profile settings keys the AI layer reads. `openaiModel` is user-chosen (Settings screen);
 * `deviceToken` is provisioned automatically against the proxy and never shown to the user. */
export interface AiSettings {
  openaiModel?: string;
  deviceToken?: string;
}

export function readAiSettings(profile: LearnerProfile): AiSettings {
  const s = profile.settings as Record<string, unknown>;
  return {
    ...(typeof s.openaiModel === "string" && s.openaiModel ? { openaiModel: s.openaiModel } : {}),
    ...(typeof s.deviceToken === "string" && s.deviceToken ? { deviceToken: s.deviceToken } : {}),
  };
}

// Single-flight in-memory registration, shared across every hook that needs it. A single AI
// screen mounts useAiProvider/useSpeechProvider/useTtsProvider together, and each independently
// calls ensureDeviceToken on mount — without sharing one in-flight promise, each would race to
// its own /register call and (if a passcode is required) pop its own separate prompt() dialog.
// Caching only the *resolved* token (not the promise) left that window open; caching the promise
// itself closes it, since every caller in the same tick sees it already set before any of them
// await past it. Cleared on failure so a wrong/cancelled passcode doesn't permanently block retries.
let deviceTokenPromise: Promise<string | null> | null = null;

async function registerDevice(passcode: string): Promise<string | null> {
  const res = await fetch(`${PROXY_BASE_URL}/register`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ passcode }),
  });
  if (!res.ok) return null;
  const data = (await res.json()) as { token?: string };
  return data.token ?? null;
}

function ensureDeviceToken(repos: Repos, profile: LearnerProfile): Promise<string | null> {
  const existing = readAiSettings(profile).deviceToken;
  if (existing) return Promise.resolve(existing);

  if (!PROXY_BASE_URL) return Promise.resolve(null);

  if (!deviceTokenPromise) {
    deviceTokenPromise = (async () => {
      try {
        // Try with no passcode first — a no-op when the proxy has no access gate set, and the
        // common case for the desktop app. Only prompt if the proxy actually rejects it.
        let token = await registerDevice("");
        if (!token && typeof window !== "undefined") {
          const passcode = window.prompt("Enter the access code to enable AI features:");
          if (passcode) token = await registerDevice(passcode);
        }
        if (token) await repos.profiles.update(profile.id, { settings: { ...profile.settings, deviceToken: token } });
        return token;
      } catch {
        return null;
      }
    })();
    deviceTokenPromise.then((token) => {
      if (!token) deviceTokenPromise = null;
    });
  }
  return deviceTokenPromise;
}

/** Builds the chat provider via the backend proxy, or null when the proxy isn't configured
 * for this build or registration failed (e.g. offline). No OpenAI key ever reaches the client. */
export async function makeProvider(repos: Repos, profile: LearnerProfile): Promise<AIProvider | null> {
  if (!PROXY_BASE_URL) return null;
  const token = await ensureDeviceToken(repos, profile);
  if (!token) return null;
  const { openaiModel } = readAiSettings(profile);
  return new OpenAIProvider({ apiKey: token, baseUrl: PROXY_BASE_URL, ...(openaiModel ? { model: openaiModel } : {}) });
}

/** Same proxy, Whisper transcription endpoint — used by the Pronunciation screen. `language`
 * is a BCP-47-ish hint (e.g. "pt") that improves transcription accuracy. */
export async function makeSpeechProvider(
  repos: Repos,
  profile: LearnerProfile,
  language?: string,
): Promise<SpeechProvider | null> {
  if (!PROXY_BASE_URL) return null;
  const token = await ensureDeviceToken(repos, profile);
  if (!token) return null;
  return new WhisperProvider({ apiKey: token, baseUrl: PROXY_BASE_URL, ...(language ? { language } : {}) });
}

/** Same proxy, TTS endpoint — used to speak AI turns aloud in Conversation/Live Interpreter. */
export async function makeTtsProvider(repos: Repos, profile: LearnerProfile): Promise<TTSProvider | null> {
  if (!PROXY_BASE_URL) return null;
  const token = await ensureDeviceToken(repos, profile);
  if (!token) return null;
  return new OpenAiTtsProvider({ apiKey: token, baseUrl: PROXY_BASE_URL });
}

interface AsyncResource<T> {
  value: T | null;
  ready: boolean;
}

/** Resolves an AI/speech provider once per (repos, profile) identity and exposes it as React
 * state, so screens don't each duplicate the async registration dance. */
function useAsyncProvider<T>(factory: () => Promise<T | null>, deps: unknown[]): AsyncResource<T> {
  const [state, setState] = useState<AsyncResource<T>>({ value: null, ready: false });
  useEffect(() => {
    let alive = true;
    setState({ value: null, ready: false });
    factory().then((value) => {
      if (alive) setState({ value, ready: true });
    });
    return () => {
      alive = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);
  return state;
}

export function useAiProvider(repos: Repos, profile: LearnerProfile): AsyncResource<AIProvider> {
  return useAsyncProvider(() => makeProvider(repos, profile), [repos, profile.id]);
}

export function useSpeechProvider(
  repos: Repos,
  profile: LearnerProfile,
  language?: string,
): AsyncResource<SpeechProvider> {
  return useAsyncProvider(() => makeSpeechProvider(repos, profile, language), [repos, profile.id, language]);
}

export function useTtsProvider(repos: Repos, profile: LearnerProfile): AsyncResource<TTSProvider> {
  return useAsyncProvider(() => makeTtsProvider(repos, profile), [repos, profile.id]);
}

/** Learner context for prompts (spec §14) straight from the profile + pack. */
export function makeLearnerContext(profile: LearnerProfile, pack: LoadedPack): LearnerContext {
  return {
    targetLanguage: pack.manifest.name,
    dialect: profile.targetDialect ?? pack.manifest.defaultDialect,
    cefrEstimate: profile.cefrEstimate ?? undefined,
    severityCeiling: effectiveSeverityCeiling(profile),
    correctionStrictness: profile.correctionStrictness,
  };
}

function packTemplate(pack: LoadedPack, key: string): string | undefined {
  return pack.aiPrompts.find((p) => p.key === key)?.template;
}

/** Correction engine wired to the pack's correction template when it ships one (spec §11). */
export function makeCorrectionEngine(provider: AIProvider, pack: LoadedPack): CorrectionEngine {
  const template = packTemplate(pack, "prompt.tutor.correction");
  return new CorrectionEngine(provider, template ? { template } : {});
}

/** Conversation task prompt: pack template if present, built-in default otherwise. */
export function makeConversationTaskPrompt(pack: LoadedPack, scenario: string): string {
  const template = packTemplate(pack, "prompt.tutor.conversation") ?? DEFAULT_CONVERSATION_TEMPLATE;
  return renderTemplate(template, { targetLanguage: pack.manifest.name, scenario });
}
