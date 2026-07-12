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
import type { Manifest } from "@polyglotai/shared-types";
import type { LearnerProfile, Severity } from "@polyglotai/shared-types";
import { listBundledPackIds, loadPackManifest } from "../app/bootstrap";

type Tab = "vocabulary" | "grammar" | "slang" | "culture" | "microPacks";

interface Props {
  repos: Repos;
  profile: LearnerProfile;
  pack: LoadedPack;
  /** Every existing profile for this account — used to tell an already-started sibling
   * micro-pack (show "Continue") from one the learner hasn't tried yet (show "Start"). */
  allProfiles: LearnerProfile[];
  /** Switches straight into an existing profile, same as the language picker's "Continue". */
  onContinuePack: (profile: LearnerProfile) => void;
  /** Begins onboarding into a brand-new profile for a not-yet-started sibling micro-pack. */
  onStartPack: (packId: string) => void;
  onDone: () => void;
}

/** A micro-pack sharing this pack's language (same basePack, or this pack itself is the
 * basePack) — the "More from <language>" section lives in Library precisely so these never
 * show up as their own tile on the top-level language picker (a micro-pack isn't a language). */
function useSiblingMicroPacks(pack: LoadedPack) {
  const [state, setState] = useState<{ siblings: { id: string; manifest: Manifest }[]; languageName: string } | null>(
    null,
  );
  const languageRootId = pack.manifest.basePack ?? pack.manifest.id;

  useEffect(() => {
    let active = true;
    Promise.all(
      listBundledPackIds()
        .filter((id) => id !== pack.manifest.id)
        .map((id) => loadPackManifest(id).then((manifest) => ({ id, manifest }))),
    ).then((all) => {
      if (!active) return;
      const root = all.find((p) => p.id === languageRootId);
      setState({
        siblings: all.filter((p) => p.manifest.basePack === languageRootId),
        languageName: root?.manifest.name ?? pack.manifest.name,
      });
    });
    return () => {
      active = false;
    };
  }, [languageRootId, pack.manifest.id, pack.manifest.name]);

  return state;
}

interface Data {
  vocabulary: VocabularyEntry[];
  grammar: GrammarEntry[];
  realSpeech: RealSpeechEntry[];
  slangEnabled: boolean;
  ceiling: Severity;
}

/** "food-drink" -> "Food & Drink"; a lone word just gets capitalized. Generic and data-driven —
 * works for any pack's tag vocabulary without a per-language label table to maintain. */
function prettyTopic(tag: string): string {
  const words = tag.split(/[-_]/).map((w) => w.charAt(0).toUpperCase() + w.slice(1));
  return words.join(tag.includes("-") ? " & " : " ");
}

/** Groups items by their first tag (an item's primary topic), preserving each group's first-
 * appearance order — that order already follows the pack's authored curriculum sequence (e.g.
 * greetings before numbers before food), so it reads as a sensible table of contents for free. */
function groupByTopic<T extends { tags: string[] }>(items: T[]): { topic: string; items: T[] }[] {
  const groups = new Map<string, T[]>();
  for (const item of items) {
    const topic = item.tags[0] ?? "other";
    if (!groups.has(topic)) groups.set(topic, []);
    groups.get(topic)!.push(item);
  }
  return [...groups.entries()].map(([topic, items]) => ({ topic, items }));
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

export function Library({ repos, profile, pack, allProfiles, onContinuePack, onStartPack, onDone }: Props) {
  const [tab, setTab] = useState<Tab>("vocabulary");
  const [data, setData] = useState<Data | null>(null);
  const packId = profile.activePackId;
  const microPacksState = useSiblingMicroPacks(pack);
  const profileByPackId = new Map(allProfiles.map((p) => [p.activePackId, p]));

  // Which topic groups are collapsed, keyed "vocabulary:greeting" / "culture:etiquette" so the
  // two tabs' collapse state don't collide. Groups start expanded — collapsing hides nothing by
  // default, it's just a way to compact a topic once you've already seen it.
  const [collapsedTopics, setCollapsedTopics] = useState<Set<string>>(new Set());
  const toggleTopic = (id: string) =>
    setCollapsedTopics((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });

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
        {microPacksState && microPacksState.siblings.length > 0 && (
          <button type="button" className={tab === "microPacks" ? "active" : ""} onClick={() => setTab("microPacks")}>
            More from {microPacksState.languageName} <span className="mono">{microPacksState.siblings.length}</span>
          </button>
        )}
      </nav>

      {tab === "vocabulary" &&
        groupByTopic(data.vocabulary).map(({ topic, items }) => {
          const groupId = `vocabulary:${topic}`;
          const collapsed = collapsedTopics.has(groupId);
          return (
            <section key={topic} className="lib-topic-group">
              <button type="button" className="lib-topic-header" onClick={() => toggleTopic(groupId)}>
                <span className={collapsed ? "chevron collapsed" : "chevron"} aria-hidden="true" />
                {prettyTopic(topic)} <span className="mono">{items.length}</span>
              </button>
              {!collapsed && (
                <div className="lib-list">
                  {items.map((v) => (
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
            </section>
          );
        })}

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

      {tab === "culture" &&
        groupByTopic(pack.culture).map(({ topic, items }) => {
          const groupId = `culture:${topic}`;
          const collapsed = collapsedTopics.has(groupId);
          return (
            <section key={topic} className="lib-topic-group">
              <button type="button" className="lib-topic-header" onClick={() => toggleTopic(groupId)}>
                <span className={collapsed ? "chevron collapsed" : "chevron"} aria-hidden="true" />
                {prettyTopic(topic)} <span className="mono">{items.length}</span>
              </button>
              {!collapsed && (
                <div className="culture-list">
                  {items.map((note) => (
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
            </section>
          );
        })}

      {tab === "microPacks" && microPacksState && (
        <div className="lib-list">
          {microPacksState.siblings.map(({ id, manifest }) => {
            const existing = profileByPackId.get(id);
            return (
              <div key={id} className="lib-row">
                <div className="word">
                  <span className="p">{manifest.name}</span>
                  <span className="m">Micro-pack · part of {microPacksState.languageName}</span>
                </div>
                <div />
                <button
                  type="button"
                  className="btn-primary"
                  onClick={() => (existing ? onContinuePack(existing) : onStartPack(id))}
                >
                  {existing ? "Continue" : "Start"}
                </button>
              </div>
            );
          })}
        </div>
      )}

      <button type="button" className="link" onClick={onDone}>
        Back to dashboard
      </button>
    </div>
  );
}
