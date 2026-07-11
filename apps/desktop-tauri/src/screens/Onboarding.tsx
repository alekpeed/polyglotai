import { useState, type CSSProperties, type FormEvent } from "react";
import { runOnboarding, type Repos } from "@polyglotai/core-learning";
import type { LoadedPack } from "@polyglotai/language-pack-sdk";
import type { LearnerGoal, LearnerProfile, RealSpeechLevel } from "@polyglotai/shared-types";

const GOALS: { value: LearnerGoal; label: string }[] = [
  { value: "conversation", label: "Conversation" },
  { value: "travel", label: "Travel" },
  { value: "fluency", label: "Fluency" },
  { value: "professional", label: "Professional" },
  { value: "dating_social", label: "Dating / social" },
  { value: "media_comprehension", label: "Media comprehension" },
  { value: "tutoring", label: "Tutoring" },
  { value: "custom", label: "Custom" },
];

// Spec §6 step 6: how much real speech to surface (drives the profanity/severity gate).
const LEVELS: { value: RealSpeechLevel; label: string; hint: string }[] = [
  { value: "standard", label: "Standard only", hint: "Textbook-safe. No slang, no surprises." },
  { value: "informal", label: "Informal included", hint: "How people actually talk — casual, still comfortable." },
  { value: "slang", label: "Slang included", hint: "Street-level Portuguese, softly labeled by register." },
  { value: "profanity", label: "Profanity explained", hint: "Everything, explained honestly — full severity labels included." },
];

// Real slang from the pack, scattered as decorative "concrete poetry" texture — not filler
// words, the actual thing the app teaches.
const TEXTURE_WORDS: { text: string; top: string; left: string; size: string; rotate: number }[] = [
  { text: "mano", top: "8%", left: "56%", size: "4.4rem", rotate: -8 },
  { text: "beleza", top: "20%", left: "2%", size: "3.1rem", rotate: 5 },
  { text: "valeu", top: "70%", left: "58%", size: "3.6rem", rotate: -4 },
  { text: "da hora", top: "48%", left: "10%", size: "2.4rem", rotate: 10 },
  { text: "rolê", top: "4%", left: "6%", size: "2.8rem", rotate: -12 },
  { text: "maneiro", top: "80%", left: "6%", size: "2.2rem", rotate: 6 },
  { text: "caramba", top: "58%", left: "78%", size: "2.6rem", rotate: 8 },
];

interface Props {
  repos: Repos;
  pack: LoadedPack;
  onComplete: (profile: LearnerProfile) => void;
}

export function Onboarding({ repos, pack, onComplete }: Props) {
  const [name, setName] = useState("");
  const [goal, setGoal] = useState<LearnerGoal>("conversation");
  const [level, setLevel] = useState<RealSpeechLevel>("informal");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const profile = await runOnboarding(repos, {
        displayName: name.trim() || "Learner",
        goal,
        realSpeechLevel: level,
        pack,
      });
      onComplete(profile);
    } catch (err) {
      setError(String(err));
      setBusy(false);
    }
  }

  const levelHint = LEVELS.find((l) => l.value === level)?.hint;

  return (
    <div className="onboard-shell">
      <aside className="onboard-hero">
        <div className="onboard-hero-texture" aria-hidden="true">
          {TEXTURE_WORDS.map((w) => {
            const style: CSSProperties = {
              top: w.top,
              left: w.left,
              fontSize: w.size,
              transform: `rotate(${w.rotate}deg)`,
            };
            return (
              <span key={w.text} style={style}>
                {w.text}
              </span>
            );
          })}
        </div>
        <div className="onboard-sunburst" aria-hidden="true" />
        <div className="onboard-hero-content">
          <span className="eyebrow">PolyglotAI · Brasil</span>
          <h1 className="onboard-headline">Fala aí.</h1>
          <p>
            Learning <strong>{pack.manifest.name}</strong> — real speech, not sanitized textbook language. Slang,
            register, and yes, the swear words too, all labeled honestly.
          </p>
        </div>
        <div className="onboard-skyline" aria-hidden="true">
          <span className="a1" />
          <span className="a2" />
          <span className="a3" />
        </div>
      </aside>

      <main className="onboard-form-panel">
        <form className="onboarding onboard-form" onSubmit={handleSubmit}>
          <h2 className="onboard-title">Let's set you up</h2>

          <div className="onboard-step">
            <span className="onboard-step-num mono">01</span>
            <label>
              What should we call you?
              <input value={name} onChange={(e) => setName(e.currentTarget.value)} placeholder="Your name" autoFocus />
            </label>
          </div>

          <div className="onboard-step">
            <span className="onboard-step-num mono">02</span>
            <label>
              Why are you learning?
              <select value={goal} onChange={(e) => setGoal(e.currentTarget.value as LearnerGoal)}>
                {GOALS.map((g) => (
                  <option key={g.value} value={g.value}>
                    {g.label}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <div className="onboard-step">
            <span className="onboard-step-num mono">03</span>
            <label>
              How real do you want it?
              <select value={level} onChange={(e) => setLevel(e.currentTarget.value as RealSpeechLevel)}>
                {LEVELS.map((l) => (
                  <option key={l.value} value={l.value}>
                    {l.label}
                  </option>
                ))}
              </select>
              {levelHint && <span className="onboard-hint">{levelHint}</span>}
            </label>
          </div>

          {error && <p className="error">{error}</p>}
          <button type="submit" className="btn-primary" disabled={busy}>
            {busy ? "Setting up…" : "Start learning"} <span className="arrow">→</span>
          </button>
        </form>
      </main>
    </div>
  );
}
