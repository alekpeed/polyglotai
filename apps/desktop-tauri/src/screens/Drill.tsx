import { useCallback, useEffect, useRef, useState } from "react";
import { loadReviewCard, type ReviewCard, type ReviewItem, type Repos } from "@polyglotai/core-learning";
import type { LearnerProfile } from "@polyglotai/shared-types";
import { createSubmissionGuard } from "../reviewSubmissionGuard";

interface Props {
  repos: Repos;
  profile: LearnerProfile;
  onDone: () => void;
}

/** Loose match: trims, lowercases, and strips accents — a learner who nails the conjugation
 * but skips a diacritic (easy to do on an unfamiliar keyboard) still passes the drill. */
function normalize(s: string): string {
  return s
    .trim()
    .toLowerCase()
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "");
}

/** Fill-the-slot substitution-ladder drills (rock-solid idea #9): each step is its own SRS
 * card, but the interaction is typing the answer rather than flipping a card, since that's
 * what actually drills production of a pattern. */
export function Drill({ repos, profile, onDone }: Props) {
  const [queue, setQueue] = useState<ReviewItem[]>([]);
  const [card, setCard] = useState<ReviewCard | null>(null);
  const [input, setInput] = useState("");
  const [checked, setChecked] = useState<{ correct: boolean } | null>(null);
  const [loading, setLoading] = useState(true);
  const [reviewed, setReviewed] = useState(0);
  const [tally, setTally] = useState<Record<1 | 2 | 3, number>>({ 1: 0, 2: 0, 3: 0 });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const submitGuard = useRef(createSubmissionGuard());

  const showNext = useCallback(
    async (items: ReviewItem[]) => {
      const next = items[0];
      if (!next) {
        setCard(null);
        return;
      }
      setCard(await loadReviewCard(repos, next));
      setInput("");
      setChecked(null);
    },
    [repos],
  );

  useEffect(() => {
    let active = true;
    const packId = profile.activePackId;
    (async () => {
      if (packId) await repos.reviews.generateForPack(profile.id, packId, ["grammar_ladder"]);
      const items = await repos.reviews.listDue(profile.id, 50, ["grammar_ladder"]);
      if (!active) return;
      setQueue(items);
      await showNext(items);
      setLoading(false);
    })();
    return () => {
      active = false;
    };
  }, [repos, profile.id, profile.activePackId, showNext]);

  function check() {
    if (!card || !input.trim()) return;
    setChecked({ correct: normalize(input) === normalize(card.back) });
  }

  async function next(rating: 1 | 2 | 3) {
    const current = queue[0];
    if (!current) return;
    await submitGuard.current.run(async () => {
      setSaving(true);
      setError(null);
      try {
        await repos.reviews.recordReview(current.id, { rating });
        const rest = queue.slice(1);
        setQueue(rest);
        setReviewed((n) => n + 1);
        setTally((t) => ({ ...t, [rating]: t[rating] + 1 }));
        await showNext(rest);
      } catch (err) {
        setError(`Could not save this drill: ${String(err)}`);
      } finally {
        setSaving(false);
      }
    });
  }

  if (loading) return <p>Loading ladder drills…</p>;

  if (!card) {
    const clean = reviewed - tally[1];
    const cleanPct = reviewed ? Math.round((clean / reviewed) * 100) : 0;
    const breakdown = [
      { rating: 3 as const, label: "Correct", g: "good" },
      { rating: 2 as const, label: "Typo", g: "hard" },
      { rating: 1 as const, label: "Missed", g: "again" },
    ];
    return (
      <div className="session-recap">
        <span className="eyebrow">Substitution Drills</span>
        <h1>{reviewed === 0 ? "No ladders due" : `${reviewed} drilled`}</h1>
        {reviewed > 0 && (
          <>
            <div className="recap-rate">
              <span className="recap-num mono">{cleanPct}%</span>
              <span className="recap-sub">clean</span>
            </div>
            <ul className="recap-breakdown">
              {breakdown.map((b) => (
                <li key={b.rating} data-g={b.g}>
                  <span className="recap-count mono">{tally[b.rating]}</span>
                  <span className="recap-glabel">{b.label}</span>
                </li>
              ))}
            </ul>
          </>
        )}
        <button type="button" className="btn-primary" onClick={onDone}>
          Back to dashboard <span className="arrow">→</span>
        </button>
      </div>
    );
  }

  return (
    <div className="review-wrap">
      <p className="progress">{queue.length} left</p>
      <section className="review-card">
        <div className="review-front">{card.front}</div>
        {card.note && <div className="review-note">{card.note}</div>}

        {!checked ? (
          <form
            onSubmit={(e) => {
              e.preventDefault();
              check();
            }}
          >
            <input
              value={input}
              onChange={(e) => setInput(e.currentTarget.value)}
              placeholder="Fill in the blank…"
              autoFocus
            />
            <button type="submit" disabled={!input.trim()}>
              Check
            </button>
          </form>
        ) : (
          <>
            <hr />
            <div className={checked.correct ? "review-back correct" : "review-back incorrect"}>
              {checked.correct ? "Correct! " : "Not quite. "}
              Answer: {card.back}
            </div>
            <div className="grades">
              {checked.correct ? (
                <button type="button" onClick={() => next(3)} disabled={saving}>
                  Next
                </button>
              ) : (
                <>
                  <button type="button" onClick={() => next(1)} disabled={saving}>
                    Next (missed it)
                  </button>
                  <button type="button" onClick={() => next(2)} disabled={saving}>
                    Next (knew it, typo)
                  </button>
                </>
              )}
            </div>
          </>
        )}
      </section>

      {error && <p className="error">{error}</p>}

      <button type="button" className="link" onClick={onDone}>
        Stop for now
      </button>
    </div>
  );
}
