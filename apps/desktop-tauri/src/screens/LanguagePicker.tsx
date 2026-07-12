import { useEffect, useState } from "react";
import type { Manifest } from "@polyglotai/shared-types";
import type { LearnerProfile } from "@polyglotai/shared-types";
import { listBundledPackIds, loadPackManifest } from "../app/bootstrap";

interface Props {
  existingProfiles: LearnerProfile[];
  onContinue: (profile: LearnerProfile) => void;
  onStartNew: (packId: string) => void;
}

/** packId -> manifest, for every bundled pack (not just installed ones) — used to label both
 * "continue" cards (existing profile, need the pack's display name) and "start new" cards
 * (no profile yet at all). */
function useBundledManifests() {
  const [manifests, setManifests] = useState<Record<string, Manifest> | null>(null);

  useEffect(() => {
    let active = true;
    Promise.all(listBundledPackIds().map((id) => loadPackManifest(id).then((m) => [id, m] as const))).then(
      (pairs) => {
        if (active) setManifests(Object.fromEntries(pairs));
      },
    );
    return () => {
      active = false;
    };
  }, []);

  return manifests;
}

/** Shown when an account has zero profiles (first-ever run) or more than one and none chosen
 * yet — a Duolingo-style course switcher: pick up an existing language or start a new one. */
export function LanguagePicker({ existingProfiles, onContinue, onStartNew }: Props) {
  const manifests = useBundledManifests();

  if (!manifests) {
    return (
      <main className="container">
        <h1>PolyglotAI</h1>
        <p>Loading…</p>
      </main>
    );
  }

  const startedPackIds = new Set(existingProfiles.map((p) => p.activePackId).filter((id): id is string => !!id));
  const newPackIds = listBundledPackIds().filter((id) => !startedPackIds.has(id));

  return (
    <div className="onboard-shell">
      <aside className="onboard-hero">
        <div className="onboard-sunburst" aria-hidden="true" />
        <div className="onboard-hero-content">
          <span className="eyebrow">PolyglotAI</span>
          <h1 className="onboard-headline">Which language?</h1>
          <p>Every language gets its own progress, review queue, and pace — pick up where you left off, or start something new.</p>
        </div>
        <div className="onboard-skyline" aria-hidden="true">
          <span className="a1" />
          <span className="a2" />
          <span className="a3" />
        </div>
      </aside>

      <main className="onboard-form-panel">
        <div className="onboarding onboard-form" style={{ maxWidth: 440 }}>
          {existingProfiles.length > 0 && (
            <>
              <h2 className="onboard-title">Continue learning</h2>
              <div className="lang-picker-list">
                {existingProfiles.map((p) => {
                  const manifest = p.activePackId ? manifests[p.activePackId] : undefined;
                  return (
                    <button
                      key={p.id}
                      type="button"
                      className="lang-picker-card"
                      onClick={() => onContinue(p)}
                    >
                      <span className="name">{manifest?.name ?? p.activePackId ?? "Unknown"}</span>
                      <span className="sub">{manifest?.languageCode ?? ""}</span>
                    </button>
                  );
                })}
              </div>
            </>
          )}

          {newPackIds.length > 0 && (
            <>
              <h2 className="onboard-title">{existingProfiles.length > 0 ? "Start a new language" : "Start learning"}</h2>
              <div className="lang-picker-list">
                {newPackIds.map((id) => {
                  const manifest = manifests[id];
                  return (
                    <button key={id} type="button" className="lang-picker-card new" onClick={() => onStartNew(id)}>
                      <span className="name">{manifest?.name ?? id}</span>
                      <span className="sub">{manifest?.languageCode ?? ""}</span>
                    </button>
                  );
                })}
              </div>
            </>
          )}
        </div>
      </main>
    </div>
  );
}
