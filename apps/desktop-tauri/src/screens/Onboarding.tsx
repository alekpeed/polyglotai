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

interface TextureWord {
  text: string;
  top: string;
  left: string;
  size: string;
  rotate: number;
}

/** Per-pack hero identity — copy and decorative texture, distinct per language the way the
 * palette is (see App.css "Pack theme" blocks). Falls back to a neutral generic hero for any
 * pack that hasn't been given its own treatment yet. */
interface HeroTheme {
  eyebrow: string;
  headline: string;
  blurb: string;
  textureWords: TextureWord[];
}

const PACK_HERO_THEMES: Record<string, HeroTheme> = {
  "pt-br": {
    eyebrow: "PolyglotAI · Brasil",
    headline: "Fala aí.",
    blurb:
      "Real speech, not sanitized textbook language. Slang, register, and yes, the swear words too, all labeled honestly.",
    textureWords: [
      { text: "mano", top: "8%", left: "56%", size: "4.4rem", rotate: -8 },
      { text: "beleza", top: "20%", left: "2%", size: "3.1rem", rotate: 5 },
      { text: "valeu", top: "70%", left: "58%", size: "3.6rem", rotate: -4 },
      { text: "da hora", top: "48%", left: "10%", size: "2.4rem", rotate: 10 },
      { text: "rolê", top: "4%", left: "6%", size: "2.8rem", rotate: -12 },
      { text: "maneiro", top: "80%", left: "6%", size: "2.2rem", rotate: 6 },
      { text: "caramba", top: "58%", left: "78%", size: "2.6rem", rotate: 8 },
    ],
  },
  ja: {
    eyebrow: "PolyglotAI · 日本語",
    headline: "話そう。",
    blurb: "Kanji, kana, and the readings that actually help — not just romanized word lists.",
    textureWords: [
      { text: "やばい", top: "8%", left: "54%", size: "4rem", rotate: -8 },
      { text: "頑張って", top: "20%", left: "2%", size: "2.6rem", rotate: 5 },
      { text: "マジで", top: "70%", left: "58%", size: "3.4rem", rotate: -4 },
      { text: "すごい", top: "48%", left: "8%", size: "2.8rem", rotate: 10 },
      { text: "元気", top: "4%", left: "8%", size: "3rem", rotate: -12 },
      { text: "かわいい", top: "80%", left: "6%", size: "2.2rem", rotate: 6 },
      { text: "大丈夫", top: "58%", left: "76%", size: "2.4rem", rotate: 8 },
    ],
  },
};

const DEFAULT_HERO_THEME: HeroTheme = {
  eyebrow: "PolyglotAI",
  headline: "Let's go.",
  blurb: "Real, native content — not sanitized textbook language.",
  textureWords: [],
};

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

  const theme = PACK_HERO_THEMES[pack.manifest.id] ?? DEFAULT_HERO_THEME;
  // The pilot Japanese pack has no slang/profanity content yet (see packs/ja) — asking for a
  // real-speech comfort level would be a setting with nothing behind it, so the step (and its
  // numbering) only appears for packs that actually have real-speech items.
  const hasRealSpeech = pack.realSpeech.length > 0;

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const profile = await runOnboarding(repos, {
        displayName: name.trim() || "Learner",
        goal,
        realSpeechLevel: hasRealSpeech ? level : undefined,
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
          {theme.textureWords.map((w) => {
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
          <span className="eyebrow">{theme.eyebrow}</span>
          <h1 className="onboard-headline">{theme.headline}</h1>
          <p>
            Learning <strong>{pack.manifest.name}</strong> — {theme.blurb}
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

          {hasRealSpeech && (
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
          )}

          {error && <p className="error">{error}</p>}
          <button type="submit" className="btn-primary" disabled={busy}>
            {busy ? "Setting up…" : "Start learning"} <span className="arrow">→</span>
          </button>
        </form>
      </main>
    </div>
  );
}
