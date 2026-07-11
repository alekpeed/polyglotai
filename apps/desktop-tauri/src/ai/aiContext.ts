import { useEffect, useState } from "react";
import {
  CorrectionEngine,
  DEFAULT_CONVERSATION_TEMPLATE,
  OpenAIProvider,
  renderTemplate,
  type AIProvider,
  type LearnerContext,
} from "@polyglotai/ai-orchestration";
import { WhisperProvider, type SpeechProvider } from "@polyglotai/pronunciation";
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

// Cached in-memory so every screen visited in one session reuses the same registration instead
// of re-hitting /register each time — the `profile` prop screens hold is a snapshot from launch
// and won't reflect a token persisted mid-session by a different screen.
let deviceTokenCache: string | null = null;

async function ensureDeviceToken(repos: Repos, profile: LearnerProfile): Promise<string | null> {
  if (deviceTokenCache) return deviceTokenCache;

  const existing = readAiSettings(profile).deviceToken;
  if (existing) {
    deviceTokenCache = existing;
    return existing;
  }

  if (!PROXY_BASE_URL) return null;
  try {
    const res = await fetch(`${PROXY_BASE_URL}/register`, { method: "POST" });
    if (!res.ok) return null;
    const data = (await res.json()) as { token?: string };
    if (!data.token) return null;
    deviceTokenCache = data.token;
    await repos.profiles.update(profile.id, { settings: { ...profile.settings, deviceToken: data.token } });
    return data.token;
  } catch {
    return null;
  }
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
