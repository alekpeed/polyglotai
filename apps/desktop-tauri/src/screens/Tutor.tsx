import { useState, type FormEvent } from "react";
import type { AiCorrection } from "@polyglotai/shared-types";
import type { LearnerProfile } from "@polyglotai/shared-types";
import type { Repos } from "@polyglotai/core-learning";
import type { LoadedPack } from "@polyglotai/language-pack-sdk";
import { makeCorrectionEngine, makeLearnerContext, useAiProvider } from "../ai/aiContext";

interface Props {
  repos: Repos;
  profile: LearnerProfile;
  pack: LoadedPack;
  onDone: () => void;
  onOpenSettings: () => void;
}

const FIELD_LABELS: Array<[keyof AiCorrection, string]> = [
  ["corrected", "Corrected"],
  ["literal", "Literal meaning"],
  ["natural", "Natural version"],
  ["formal", "Formal"],
  ["casual", "Casual"],
  ["slangNative", "Slang / native"],
  ["grammarExplanation", "Grammar"],
  ["registerExplanation", "Register"],
  ["pronunciationNotes", "Pronunciation"],
];

export function Tutor({ repos, profile, pack, onDone, onOpenSettings }: Props) {
  const { value: provider, ready } = useAiProvider(repos, profile);
  const [text, setText] = useState("");
  const [busy, setBusy] = useState(false);
  const [correction, setCorrection] = useState<AiCorrection | null>(null);
  const [error, setError] = useState<string | null>(null);

  if (!ready) return <p className="container">Connecting…</p>;

  if (!provider) {
    return (
      <main className="container">
        <h1>AI Tutor</h1>
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

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!text.trim() || !provider) return;
    setBusy(true);
    setError(null);
    setCorrection(null);
    try {
      const engine = makeCorrectionEngine(provider, pack);
      setCorrection(await engine.correct(text.trim(), makeLearnerContext(profile, pack)));
    } catch (err) {
      setError(String(err));
    } finally {
      setBusy(false);
    }
  }

  const corrected = correction?.corrected;
  const otherFields = FIELD_LABELS.filter(([key]) => key !== "corrected");

  return (
    <div>
      <span className="eyebrow">AI Tutor</span>
      <h1>Write it, get it corrected</h1>
      <p className="subtitle">Write a sentence in {pack.manifest.name}; get the full correction.</p>

      <form className="tutor-card" onSubmit={handleSubmit}>
        <textarea
          value={text}
          onChange={(e) => setText(e.currentTarget.value)}
          rows={3}
          placeholder="Eu sou cansado hoje…"
        />
        <button type="submit" className="btn-primary" disabled={busy || !text.trim()}>
          {busy ? "Correcting…" : "Correct it"} <span className="arrow">→</span>
        </button>
      </form>

      {error && <p className="error">{error}</p>}

      {correction && (
        <div className="correction-result">
          {corrected && (
            <div className="correction-hero">
              <span className="eyebrow">Corrected</span>
              <div className="correction-hero-text">{corrected}</div>
            </div>
          )}
          <div className="correction-grid">
            {otherFields.map(([key, label]) => {
              const value = correction[key];
              if (!value || (Array.isArray(value) && value.length === 0)) return null;
              return (
                <div key={key} className="correction-tile">
                  <span className="correction-label">{label}</span>
                  <span className="correction-value">{String(value)}</span>
                </div>
              );
            })}
          </div>
        </div>
      )}

      <button type="button" className="link" onClick={onDone}>
        Back to dashboard
      </button>
    </div>
  );
}
