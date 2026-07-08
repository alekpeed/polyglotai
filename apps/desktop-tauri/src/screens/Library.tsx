import { useEffect, useState } from "react";
import {
  effectiveSeverityCeiling,
  listGrammar,
  listRealSpeech,
  listVocabulary,
  type GrammarEntry,
  type RealSpeechEntry,
  type Repos,
  type VocabularyEntry,
} from "@polyglotai/core-learning";
import type { LearnerProfile } from "@polyglotai/shared-types";

type Tab = "vocabulary" | "grammar" | "slang";

interface Props {
  repos: Repos;
  profile: LearnerProfile;
  onDone: () => void;
}

interface Data {
  vocabulary: VocabularyEntry[];
  grammar: GrammarEntry[];
  realSpeech: RealSpeechEntry[];
  slangEnabled: boolean;
}

export function Library({ repos, profile, onDone }: Props) {
  const [tab, setTab] = useState<Tab>("vocabulary");
  const [data, setData] = useState<Data | null>(null);
  const packId = profile.activePackId;

  useEffect(() => {
    if (!packId) return;
    let active = true;
    const ceiling = effectiveSeverityCeiling(profile);
    Promise.all([
      listVocabulary(repos, packId),
      listGrammar(repos, packId),
      listRealSpeech(repos, packId, ceiling),
      repos.flags.isEnabled("slang_mode"),
    ]).then(([vocabulary, grammar, realSpeech, slangEnabled]) => {
      if (active) setData({ vocabulary, grammar, realSpeech, slangEnabled });
    });
    return () => {
      active = false;
    };
  }, [repos, profile, packId]);

  if (!packId) return <p className="container">No active pack.</p>;
  if (!data) return <p className="container">Loading library…</p>;

  return (
    <main className="container">
      <h1>Library</h1>
      <nav className="tabs">
        <button type="button" className={tab === "vocabulary" ? "active" : ""} onClick={() => setTab("vocabulary")}>
          Vocabulary ({data.vocabulary.length})
        </button>
        <button type="button" className={tab === "grammar" ? "active" : ""} onClick={() => setTab("grammar")}>
          Grammar ({data.grammar.length})
        </button>
        {data.slangEnabled && (
          <button type="button" className={tab === "slang" ? "active" : ""} onClick={() => setTab("slang")}>
            Slang & Register ({data.realSpeech.length})
          </button>
        )}
      </nav>

      {tab === "vocabulary" && (
        <ul className="entry-list">
          {data.vocabulary.map((v) => (
            <li key={v.key}>
              <span className="entry-front">{v.lemma}</span>
              <span className="entry-back">{v.translation}</span>
              <span className="entry-tag">{v.register ?? ""}</span>
            </li>
          ))}
        </ul>
      )}

      {tab === "grammar" && (
        <ul className="entry-list">
          {data.grammar.map((g) => (
            <li key={g.key} className="grammar-entry">
              <span className="entry-front">
                {g.title} {g.cefr && <em>({g.cefr})</em>}
              </span>
              <span className="entry-back">{g.explanationMd}</span>
            </li>
          ))}
        </ul>
      )}

      {tab === "slang" && data.slangEnabled && (
        <ul className="entry-list">
          {data.realSpeech.map((s) =>
            s.withinComfort ? (
              <li key={s.key}>
                <span className="entry-front">{s.phrase}</span>
                <span className="entry-back">{s.natural}</span>
                <span className="entry-tag">
                  {s.register} · sev {s.severity}/7 · {s.learnerShouldUse}
                </span>
                {s.culturalWarning && <span className="review-note">{s.culturalWarning}</span>}
              </li>
            ) : (
              <li key={s.key} className="locked">
                <span className="entry-front">🔒 hidden</span>
                <span className="entry-tag">
                  {s.register} · severity {s.severity}/7 — raise your real-speech level in Settings to reveal
                </span>
              </li>
            ),
          )}
        </ul>
      )}

      <button type="button" className="link" onClick={onDone}>
        Back to dashboard
      </button>
    </main>
  );
}
