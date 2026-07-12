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
import type { LoadedPack } from "@polyglotai/language-pack-sdk";
import type { LearnerProfile, Severity } from "@polyglotai/shared-types";

type Tab = "vocabulary" | "grammar" | "slang" | "culture";

interface Props {
  repos: Repos;
  profile: LearnerProfile;
  pack: LoadedPack;
  onDone: () => void;
}

interface Data {
  vocabulary: VocabularyEntry[];
  grammar: GrammarEntry[];
  realSpeech: RealSpeechEntry[];
  slangEnabled: boolean;
  ceiling: Severity;
}

const HEAT = ["var(--heat-1)", "var(--heat-2)", "var(--heat-3)", "var(--heat-4)", "var(--heat-5)", "var(--heat-6)", "var(--heat-7)"];

/** A 7-segment heat meter reused for both the legend and each row — filled up to `severity`
 * in that severity's own heat color, so the shape reads at a glance even when locked. */
function HeatBar({ severity }: { severity: Severity }) {
  return (
    <div className="bar">
      {HEAT.map((color, i) => (
        <i key={i} style={i < severity ? { background: color } : undefined} />
      ))}
    </div>
  );
}

export function Library({ repos, profile, pack, onDone }: Props) {
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
      if (active) setData({ vocabulary, grammar, realSpeech, slangEnabled, ceiling });
    });
    return () => {
      active = false;
    };
  }, [repos, profile, packId]);

  if (!packId) return <p className="container">No active pack.</p>;
  if (!data) return <p className="container">Loading library…</p>;

  return (
    <div>
      <h1>Library</h1>

      <nav className="lib-tabs">
        <button type="button" className={tab === "vocabulary" ? "active" : ""} onClick={() => setTab("vocabulary")}>
          Vocabulary <span className="mono">{data.vocabulary.length}</span>
        </button>
        <button type="button" className={tab === "grammar" ? "active" : ""} onClick={() => setTab("grammar")}>
          Grammar <span className="mono">{data.grammar.length}</span>
        </button>
        {data.slangEnabled && (
          <button type="button" className={tab === "slang" ? "active" : ""} onClick={() => setTab("slang")}>
            Slang & Register <span className="mono">{data.realSpeech.length}</span>
          </button>
        )}
        {pack.culture.length > 0 && (
          <button type="button" className={tab === "culture" ? "active" : ""} onClick={() => setTab("culture")}>
            Culture <span className="mono">{pack.culture.length}</span>
          </button>
        )}
      </nav>

      {tab === "vocabulary" && (
        <div className="lib-list">
          {data.vocabulary.map((v) => (
            <div key={v.key} className="lib-row">
              <div className="word">
                <span className="p">{v.lemma}</span>
                {(v.reading || v.romaji) && (
                  <span className="r">{[v.reading, v.romaji].filter(Boolean).join(" · ")}</span>
                )}
                <span className="m">{v.translation}</span>
              </div>
              <span className="register-chip">{v.register ?? v.entryType}</span>
              <div />
            </div>
          ))}
        </div>
      )}

      {tab === "grammar" && (
        <div className="lib-list">
          {data.grammar.map((g) => (
            <div key={g.key} className="lib-row">
              <div className="word">
                <span className="p">{g.title}</span>
                <span className="m">{g.explanationMd}</span>
              </div>
              <span className="register-chip">{g.cefr ?? "—"}</span>
              <div />
            </div>
          ))}
        </div>
      )}

      {tab === "slang" && data.slangEnabled && (
        <>
          <div className="heat-legend">
            <span className="txt">Heat:</span>
            <HeatBar severity={7} />
            <span className="txt">mild → severe · your ceiling is set to {data.ceiling} in Settings</span>
          </div>
          <div className="lib-list">
            {data.realSpeech.map((s) => (
              <div key={s.key} className={s.withinComfort ? "lib-row" : "lib-row locked"}>
                <div className="word">
                  <span className="p">{s.phrase}</span>
                  <span className="m">{s.withinComfort ? (s.natural ?? "") : "raise your real-speech level in Settings to reveal"}</span>
                </div>
                <span className="register-chip">{s.register}</span>
                <div className="heat-meter">
                  <HeatBar severity={s.severity} />
                  {s.withinComfort ? (
                    <span className="num">{s.severity} / 7</span>
                  ) : (
                    <span className="lock-note">
                      <span className="lock-icon" />
                      {s.severity} / 7
                    </span>
                  )}
                </div>
              </div>
            ))}
          </div>
        </>
      )}

      {tab === "culture" && (
        <div className="culture-list">
          {pack.culture.map((note) => (
            <article key={note.key} className="culture-note">
              <h3>{note.title}</h3>
              {note.bodyMd.split(/\n\n+/).map((para, i) => (
                <p key={i}>{para}</p>
              ))}
              {note.tags.length > 0 && (
                <div className="culture-tags">
                  {note.tags.map((t) => (
                    <span key={t} className="register-chip">
                      {t}
                    </span>
                  ))}
                </div>
              )}
            </article>
          ))}
        </div>
      )}

      <button type="button" className="link" onClick={onDone}>
        Back to dashboard
      </button>
    </div>
  );
}
