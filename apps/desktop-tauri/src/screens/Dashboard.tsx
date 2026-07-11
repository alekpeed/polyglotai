import { useEffect, useState } from "react";
import {
  listGrammar,
  listRealSpeech,
  listVocabulary,
  loadDashboard,
  effectiveSeverityCeiling,
  type DashboardData,
  type Repos,
} from "@polyglotai/core-learning";
import type { LearnerProfile } from "@polyglotai/shared-types";

interface Props {
  repos: Repos;
  profile: LearnerProfile;
  onStartReview: () => void;
  onStartDrill: () => void;
  onOpenLibrary: () => void;
  onOpenTutor: () => void;
  onOpenConversation: () => void;
  onOpenInterpreter: () => void;
  onOpenPronunciation: () => void;
  onOpenSettings: () => void;
}

interface ShelfItem {
  tag: string;
  title: string;
  body: string;
}

/** Pulls a small, real "this week" shelf straight from the active pack's content — one grammar
 * point, one phrase, and (if within the learner's comfort ceiling) one register/slang item —
 * instead of a curated-but-fake placeholder feed. */
async function loadShelf(repos: Repos, profile: LearnerProfile): Promise<ShelfItem[]> {
  const packId = profile.activePackId;
  if (!packId) return [];

  const [grammar, vocab, realSpeech] = await Promise.all([
    listGrammar(repos, packId),
    listVocabulary(repos, packId),
    listRealSpeech(repos, packId, effectiveSeverityCeiling(profile)),
  ]);

  const items: ShelfItem[] = [];
  const g = grammar[0];
  if (g) items.push({ tag: g.cefr ? `Grammar · ${g.cefr}` : "Grammar", title: g.title, body: g.explanationMd });

  const phrase = vocab.find((v) => v.entryType === "phrase") ?? vocab[0];
  if (phrase) items.push({ tag: "Phrase", title: phrase.lemma, body: phrase.translation });

  const speech = realSpeech.find((s) => s.withinComfort);
  if (speech) {
    items.push({
      tag: `Register · severity ${speech.severity}`,
      title: speech.phrase,
      body: speech.natural ?? speech.culturalWarning ?? speech.learnerShouldUse ?? "",
    });
  }
  return items.slice(0, 3);
}

export function Dashboard({
  repos,
  profile,
  onStartReview,
  onStartDrill,
  onOpenLibrary,
  onOpenTutor,
  onOpenConversation,
  onOpenInterpreter,
  onOpenPronunciation,
  onOpenSettings,
}: Props) {
  const [data, setData] = useState<DashboardData | null>(null);
  const [shelf, setShelf] = useState<ShelfItem[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    Promise.all([loadDashboard(repos, profile.id), loadShelf(repos, profile)])
      .then(([d, s]) => {
        if (!active) return;
        setData(d);
        setShelf(s);
      })
      .catch((e) => active && setError(String(e)));
    return () => {
      active = false;
    };
  }, [repos, profile]);

  if (error) return <p className="error">{error}</p>;
  if (!data) return <p>Loading…</p>;

  const firstName = data.profile.displayName.split(" ")[0];

  return (
    <div>
      <span className="eyebrow">Olá, {firstName}</span>
      <h1>{data.activePackName ?? "No pack installed"}</h1>

      <div className="hero-row">
        <section className="hero-card">
          <span className="eyebrow">Due now</span>
          <div className="hero-num mono">{data.dueCount}</div>
          <p className="hc-sub">
            {data.dueCount === 0
              ? "All caught up — check back later or explore the library."
              : "Vocabulary, grammar, and register — ready whenever you are."}
          </p>
          <button type="button" className="btn-primary" onClick={onStartReview} disabled={data.dueCount === 0}>
            {data.dueCount === 0 ? "Nothing due" : "Start review"} <span className="arrow">→</span>
          </button>
        </section>

        <section className="streak-card">
          <div>
            <span className="eyebrow">Streak</span>
            <div className="streak-num">
              {data.streakDays}
              <span className="unit"> day{data.streakDays === 1 ? "" : "s"}</span>
            </div>
          </div>
          <div className="streak-days">
            {data.streakLast7.map((on, i) => (
              <span key={i} className={on ? "on" : ""} />
            ))}
          </div>
        </section>
      </div>

      <div className="stat-strip">
        <div className="stat-cell">
          <div className="v mono">{data.totals.vocabulary}</div>
          <div className="l">vocabulary</div>
        </div>
        <div className="stat-cell">
          <div className="v mono">{data.totals.grammar}</div>
          <div className="l">grammar</div>
        </div>
        <div className="stat-cell">
          <div className="v mono">{data.totals.realSpeech}</div>
          <div className="l">slang & register</div>
        </div>
        <div className="stat-cell">
          <div className="v mono">{data.totals.dialogues}</div>
          <div className="l">dialogues</div>
        </div>
      </div>

      {shelf.length > 0 && (
        <>
          <div className="shelf-head">
            <h3>This week</h3>
            <button type="button" onClick={onOpenLibrary}>
              See library →
            </button>
          </div>
          <div className="shelf">
            {shelf.map((item) => (
              <div key={item.title} className="shelf-card">
                <span className="tag">{item.tag}</span>
                <h4>{item.title}</h4>
                <p>{item.body}</p>
              </div>
            ))}
          </div>
        </>
      )}

      <div className="shelf-head" style={{ marginTop: "2.4rem" }}>
        <h3>Practice</h3>
      </div>
      <div className="shelf">
        <button type="button" className="shelf-card" onClick={onOpenTutor} style={{ textAlign: "left" }}>
          <span className="tag">AI Tutor</span>
          <h4>Get a full correction</h4>
          <p>Write a sentence, get corrected/formal/casual/slang versions.</p>
        </button>
        <button type="button" className="shelf-card" onClick={onOpenConversation} style={{ textAlign: "left" }}>
          <span className="tag">Conversation</span>
          <h4>Roleplay a scenario</h4>
          <p>Café, job interview, first date — pick a scenario and go.</p>
        </button>
        <button type="button" className="shelf-card" onClick={onOpenInterpreter} style={{ textAlign: "left" }}>
          <span className="tag">Live Interpreter</span>
          <h4>Interpret on the clock</h4>
          <p>Two speakers, one live dialogue, a timer per line.</p>
        </button>
        <button type="button" className="shelf-card" onClick={onStartDrill} style={{ textAlign: "left" }}>
          <span className="tag">Substitution Drills</span>
          <h4>Fill the slot</h4>
          <p>Conjugation and pattern ladders, typed not flipped.</p>
        </button>
        <button type="button" className="shelf-card" onClick={onOpenPronunciation} style={{ textAlign: "left" }}>
          <span className="tag">Pronunciation</span>
          <h4>Record & score</h4>
          <p>Say it back, get scored against the target.</p>
        </button>
        <button type="button" className="shelf-card" onClick={onOpenSettings} style={{ textAlign: "left" }}>
          <span className="tag">Settings</span>
          <h4>API key & comfort level</h4>
          <p>OpenAI key, real-speech ceiling, correction strictness.</p>
        </button>
      </div>
    </div>
  );
}
