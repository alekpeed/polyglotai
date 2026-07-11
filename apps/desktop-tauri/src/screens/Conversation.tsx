import { useRef, useState, type FormEvent } from "react";
import { ConversationSession } from "@polyglotai/ai-orchestration";
import type { Repos } from "@polyglotai/core-learning";
import type { LoadedPack } from "@polyglotai/language-pack-sdk";
import type { LearnerProfile } from "@polyglotai/shared-types";
import {
  describeAccent,
  makeConversationTaskPrompt,
  makeLearnerContext,
  useAiProvider,
  useSpeechProvider,
  useTtsProvider,
} from "../ai/aiContext";
import { playAudioBlob, useVoiceRecorder } from "../ai/voice";

interface Props {
  repos: Repos;
  profile: LearnerProfile;
  pack: LoadedPack;
  onDone: () => void;
  onOpenSettings: () => void;
}

/** Adult, real-world scenarios (spec §14 tutor modes; §6 first-session conversation). */
const SCENARIOS = [
  "café",
  "bar — meeting people",
  "first date",
  "hotel check-in",
  "job interview",
  "apartment rental",
  "doctor visit",
  "texting a new friend",
  "workplace disagreement",
  "negotiating at a street market",
];

interface Bubble {
  role: "user" | "assistant";
  content: string;
}

export function Conversation({ repos, profile, pack, onDone, onOpenSettings }: Props) {
  const { value: provider, ready } = useAiProvider(repos, profile);
  const speechLanguage = pack.manifest.languageCode.split("-")[0]; // "pt-BR" -> "pt"
  const { value: speechProvider } = useSpeechProvider(repos, profile, speechLanguage);
  const { value: ttsProvider } = useTtsProvider(repos, profile);
  const accentHint = describeAccent(profile, pack);
  const recorder = useVoiceRecorder(speechProvider);
  const [scenario, setScenario] = useState<string | null>(null);
  const [bubbles, setBubbles] = useState<Bubble[]>([]);
  const [input, setInput] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [playingIndex, setPlayingIndex] = useState<number | null>(null);
  const sessionRef = useRef<ConversationSession | null>(null);
  const conversationIdRef = useRef<string | null>(null);
  const audioCacheRef = useRef<Map<number, Blob>>(new Map());

  if (!ready) return <p className="container">Connecting…</p>;

  if (!provider) {
    return (
      <main className="container">
        <h1>Conversation</h1>
        <p className="subtitle">AI features aren't available right now — check your connection and try again.</p>
        <button type="button" onClick={onOpenSettings}>
          Open Settings
        </button>
        <button type="button" className="link" onClick={onDone}>
          Back to dashboard
        </button>
      </main>
    );
  }

  async function start(chosen: string) {
    setScenario(chosen);
    setBusy(true);
    setError(null);
    const session = new ConversationSession(provider!, {
      taskPrompt: makeConversationTaskPrompt(pack, chosen),
      ctx: makeLearnerContext(profile, pack),
    });
    sessionRef.current = session;
    // Persist only when the learner opted in (conversation_logging, off by default).
    if (await repos.flags.isEnabled("conversation_logging")) {
      const record = await repos.conversations.create(profile.id, "roleplay-partner", chosen);
      conversationIdRef.current = record.id;
    }
    try {
      // The AI partner opens in character (spec: a barista/interviewer/date speaks first in
      // real life) rather than handing the learner a blank input with nothing to react to.
      const opener = await session.start();
      setBubbles([{ role: "assistant", content: opener }]);
      const convoId = conversationIdRef.current;
      if (convoId) {
        await repos.conversations.appendMessage(convoId, "assistant", opener, { tokens: session.tokensSpent });
      }
      void playBubble(0, opener);
    } catch (err) {
      setError(String(err));
    } finally {
      setBusy(false);
    }
  }

  async function handleSend(e: FormEvent) {
    e.preventDefault();
    const session = sessionRef.current;
    const text = input.trim();
    if (!session || !text || busy) return;

    setBusy(true);
    setError(null);
    setInput("");
    // The assistant bubble about to be appended will land at this index — computed from the
    // current render's `bubbles` (this send is the only mutation in flight, `busy` blocks
    // re-entrancy) rather than read back out of the setState updater, since that updater isn't
    // guaranteed to run synchronously and previously left this value stuck at -1, silently
    // skipping every auto-play.
    const replyIndex = bubbles.length + 1;
    setBubbles((b) => [...b, { role: "user", content: text }]);
    try {
      const reply = await session.send(text);
      setBubbles((b) => [...b, { role: "assistant", content: reply }]);
      const convoId = conversationIdRef.current;
      if (convoId) {
        await repos.conversations.appendMessage(convoId, "user", text);
        await repos.conversations.appendMessage(convoId, "assistant", reply, {
          tokens: session.tokensSpent,
        });
      }
      void playBubble(replyIndex, reply);
    } catch (err) {
      setError(String(err));
    } finally {
      setBusy(false);
    }
  }

  /** Speaks a bubble aloud — synthesized once per index, then cached for instant replay. */
  async function playBubble(index: number, text: string) {
    if (!ttsProvider) return;
    try {
      setPlayingIndex(index);
      let blob = audioCacheRef.current.get(index);
      if (!blob) {
        blob = await ttsProvider.synthesize(text, { languageCode: speechLanguage, accentHint });
        audioCacheRef.current.set(index, blob);
      }
      await playAudioBlob(blob);
    } catch {
      // Non-fatal — the text bubble is already on screen either way.
    } finally {
      setPlayingIndex((i) => (i === index ? null : i));
    }
  }

  async function handleMicClick() {
    if (recorder.phase === "recording") {
      const text = await recorder.stop();
      if (text) setInput(text);
    } else if (recorder.phase === "idle") {
      await recorder.start();
    }
  }

  if (!scenario) {
    return (
      <div>
        <span className="eyebrow">Conversation</span>
        <h1>Pick a scenario</h1>
        <p className="subtitle">
          Your partner speaks only {pack.manifest.name} — reply in it too. Drift into English and they'll gently
          steer you back, not switch to match you.
        </p>
        <div className="scenario-grid">
          {SCENARIOS.map((s) => (
            <button key={s} type="button" onClick={() => start(s)}>
              {s}
            </button>
          ))}
        </div>
        <button type="button" className="link" onClick={onDone}>
          Back to dashboard
        </button>
      </div>
    );
  }

  return (
    <div>
      <div className="chat-head">
        <div>
          <span className="eyebrow">Conversation</span>
          <h1>{scenario}</h1>
        </div>
      </div>
      <section className="chat">
        {bubbles.length === 0 && !busy && <p className="subtitle">Reply in Portuguese if you can!</p>}
        {bubbles.map((b, i) => (
          <div key={i} className={`bubble ${b.role}`}>
            <span>{b.content}</span>
            {b.role === "assistant" && ttsProvider && (
              <button
                type="button"
                className="bubble-play"
                onClick={() => playBubble(i, b.content)}
                disabled={playingIndex === i}
                aria-label="Play aloud"
              >
                {playingIndex === i ? "◆" : "🔊"}
              </button>
            )}
          </div>
        ))}
        {busy && <div className="bubble assistant pending">…</div>}
      </section>

      {error && <p className="error">{error}</p>}
      {recorder.error && <p className="error">{recorder.error}</p>}

      <form className="chat-input" onSubmit={handleSend}>
        <input
          value={input}
          onChange={(e) => setInput(e.currentTarget.value)}
          placeholder="Escreva aqui…"
          disabled={busy}
        />
        {speechProvider && (
          <button
            type="button"
            className={`mic-btn ${recorder.phase === "recording" ? "recording" : ""}`}
            onClick={handleMicClick}
            disabled={busy || recorder.phase === "transcribing"}
            aria-label={recorder.phase === "recording" ? "Stop recording" : "Speak your reply"}
          >
            {recorder.phase === "recording" ? "■" : recorder.phase === "transcribing" ? "…" : "🎤"}
          </button>
        )}
        <button type="submit" disabled={busy || !input.trim()}>
          Send
        </button>
      </form>

      <button type="button" className="link" onClick={onDone}>
        End conversation
      </button>
    </div>
  );
}
