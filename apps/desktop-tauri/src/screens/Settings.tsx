import { useEffect, useState, type FormEvent } from "react";
import type { Repos } from "@polyglotai/core-learning";
import type { LearnerProfile, RealSpeechLevel } from "@polyglotai/shared-types";
import { readAiSettings } from "../ai/aiContext";
import { signOut } from "../auth/authContext";
import { supabase } from "../auth/supabaseClient";
import { getStoredTheme, setTheme, type ThemePreference } from "../theme";

interface Props {
  repos: Repos;
  profile: LearnerProfile;
  onSaved: (profile: LearnerProfile) => void;
  onDone: () => void;
  onSwitchLanguage: () => void;
}

const LEVELS: { value: RealSpeechLevel; label: string; heat: 1 | 2 | 3 | 4; hint: string }[] = [
  { value: "standard", label: "Standard only", heat: 1, hint: "Textbook-safe. No slang, no surprises." },
  { value: "informal", label: "Informal included", heat: 2, hint: "How people actually talk — casual, still comfortable." },
  { value: "slang", label: "Slang included", heat: 3, hint: "Street-level Portuguese, softly labeled by register." },
  { value: "profanity", label: "Profanity explained", heat: 4, hint: "Everything, explained honestly — full severity labels included." },
];

const STRICTNESS: { value: "lenient" | "balanced" | "strict"; label: string; hint: string }[] = [
  { value: "lenient", label: "Lenient", hint: "Correct only errors that block understanding." },
  { value: "balanced", label: "Balanced", hint: "Correct meaningful errors, note small recurring ones." },
  { value: "strict", label: "Strict", hint: "Correct every error, including minor ones." },
];

const THEMES: { value: ThemePreference; label: string; hint: string }[] = [
  { value: "system", label: "Match system", hint: "Follows your OS's light/dark setting." },
  { value: "light", label: "Light", hint: "Always light, regardless of OS setting." },
  { value: "dark", label: "Dark", hint: "Always dark, regardless of OS setting." },
  { value: "classic", label: "Classic", hint: "The original navy-blue look, before the periwinkle update." },
];

export function Settings({ repos, profile, onSaved, onDone, onSwitchLanguage }: Props) {
  const initial = readAiSettings(profile);
  const [model, setModel] = useState(initial.openaiModel ?? "gpt-5.6-luna");
  const [level, setLevel] = useState<RealSpeechLevel>(profile.realSpeechLevel);
  const [strictness, setStrictness] = useState(profile.correctionStrictness);
  const [logging, setLogging] = useState(false);
  const [saved, setSaved] = useState(false);
  const [themePref, setThemePref] = useState<ThemePreference>(getStoredTheme());

  function handleThemeChange(pref: ThemePreference) {
    setTheme(pref);
    setThemePref(pref);
  }

  useEffect(() => {
    repos.flags.isEnabled("conversation_logging").then(setLogging);
  }, [repos]);

  async function handleSave(e: FormEvent) {
    e.preventDefault();
    const updated = await repos.profiles.update(profile.id, {
      realSpeechLevel: level,
      correctionStrictness: strictness,
      settings: { ...profile.settings, openaiModel: model.trim() },
    });
    await repos.flags.setEnabled("conversation_logging", logging);
    onSaved(updated);
    setSaved(true);
  }

  return (
    <div>
      <span className="eyebrow">Account</span>
      <h1>Settings</h1>

      <div className="settings-list">
        <section className="settings-card">
          <div className="settings-card-head">
            <h3>Language</h3>
            <p>Switch to another language you've started, or pick up a new one.</p>
          </div>
          <button type="button" onClick={onSwitchLanguage}>
            Switch language
          </button>
        </section>

        <section className="settings-card">
          <div className="settings-card-head">
            <h3>Appearance</h3>
            <p>Applies immediately — no need to save.</p>
          </div>
          <div className="pill-select">
            {THEMES.map((t) => (
              <button
                key={t.value}
                type="button"
                className={`neutral ${themePref === t.value ? "active" : ""}`}
                onClick={() => handleThemeChange(t.value)}
              >
                {t.label}
              </button>
            ))}
            <span className="hint">{THEMES.find((t) => t.value === themePref)?.hint}</span>
          </div>
        </section>

        {supabase && (
          <section className="settings-card">
            <div className="settings-card-head">
              <h3>Account</h3>
              <p>Signed in — your progress syncs to this account automatically.</p>
            </div>
            <button type="button" onClick={() => void signOut()}>
              Sign out
            </button>
          </section>
        )}
      </div>

      <form onSubmit={handleSave}>
        <div className="settings-list">
          <section className="settings-card">
            <div className="settings-card-head">
              <h3>AI model</h3>
              <p>AI features run through PolyglotAI's shared backend — no API key needed here.</p>
            </div>
            <input type="text" value={model} onChange={(e) => setModel(e.currentTarget.value)} />
          </section>

          <section className="settings-card">
            <div className="settings-card-head">
              <h3>Real-speech level</h3>
              <p>How much slang, register, and profanity the library and tutor surface — mild to full severity.</p>
            </div>
            <div className="pill-select">
              {LEVELS.map((l) => (
                <button
                  key={l.value}
                  type="button"
                  data-heat={l.heat}
                  className={level === l.value ? "active" : ""}
                  onClick={() => setLevel(l.value)}
                >
                  {l.label}
                </button>
              ))}
              <span className="hint">{LEVELS.find((l) => l.value === level)?.hint}</span>
            </div>
          </section>

          <section className="settings-card">
            <div className="settings-card-head">
              <h3>Correction strictness</h3>
              <p>How picky the AI Tutor is when correcting your writing.</p>
            </div>
            <div className="pill-select">
              {STRICTNESS.map((s) => (
                <button
                  key={s.value}
                  type="button"
                  className={`neutral ${strictness === s.value ? "active" : ""}`}
                  onClick={() => setStrictness(s.value)}
                >
                  {s.label}
                </button>
              ))}
              <span className="hint">{STRICTNESS.find((s) => s.value === strictness)?.hint}</span>
            </div>
          </section>

          <section className="settings-card">
            <div className="settings-card-head">
              <h3>Conversation logs</h3>
              <p>Save Conversation-mode transcripts locally on this device. Off by default.</p>
            </div>
            <label className="settings-checkbox">
              <input type="checkbox" checked={logging} onChange={(e) => setLogging(e.currentTarget.checked)} />
              Save conversation logs locally
            </label>
          </section>
        </div>

        <div className="settings-actions">
          <button type="submit" className="btn-primary">
            Save changes
          </button>
          {saved && <span className="settings-saved">Saved.</span>}
        </div>
      </form>

      <button type="button" className="link" onClick={onDone}>
        Back to dashboard
      </button>
    </div>
  );
}
