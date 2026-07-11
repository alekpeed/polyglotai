import { useEffect, useState, type ReactNode } from "react";
import type { Repos } from "@polyglotai/core-learning";
import type { LoadedPack } from "@polyglotai/language-pack-sdk";
import type { LearnerProfile } from "@polyglotai/shared-types";

export type NavKey =
  | "dashboard"
  | "review"
  | "drill"
  | "library"
  | "tutor"
  | "conversation"
  | "interpreter"
  | "pronunciation"
  | "settings";

interface NavItem {
  key: NavKey;
  label: string;
}

const PRIMARY_NAV: NavItem[] = [
  { key: "dashboard", label: "Home" },
  { key: "review", label: "Review" },
  { key: "library", label: "Library" },
];

const PRACTICE_NAV: NavItem[] = [
  { key: "tutor", label: "AI Tutor" },
  { key: "conversation", label: "Conversation" },
  { key: "interpreter", label: "Live Interpreter" },
  { key: "drill", label: "Substitution Drills" },
  { key: "pronunciation", label: "Pronunciation" },
];

interface Props {
  repos: Repos;
  profile: LearnerProfile;
  pack: LoadedPack;
  active: NavKey;
  onNavigate: (view: NavKey) => void;
  children: ReactNode;
}

/** Persistent sidebar shell (design handoff §03) — same nav and tokens on every screen once a
 * profile exists; only the sidebar's country badge would change for a future non-Brazil theme. */
export function AppShell({ repos, profile, pack, active, onNavigate, children }: Props) {
  const [dueCount, setDueCount] = useState<number | null>(null);

  useEffect(() => {
    let alive = true;
    repos.reviews.countDue(profile.id).then((n) => alive && setDueCount(n));
    return () => {
      alive = false;
    };
  }, [repos, profile.id, active]);

  const initial = profile.displayName.trim().charAt(0).toUpperCase() || "?";

  return (
    <div className="app-shell">
      <aside className="app-sidebar">
        <div className="app-brand">
          <span className="mark">P</span>
          <span className="name">PolyglotAI</span>
        </div>

        <div className="theme-picker">
          <div className="flag-badge" aria-hidden="true" />
          <div className="tp-text">
            <span className="tp-lang">{pack.manifest.name}</span>
            <span className="tp-swap">{pack.manifest.languageCode}</span>
          </div>
        </div>

        <nav className="app-nav">
          {PRIMARY_NAV.map((item) => (
            <button
              key={item.key}
              type="button"
              className={active === item.key ? "active" : ""}
              onClick={() => onNavigate(item.key)}
            >
              <span>{item.label}</span>
              {item.key === "review" && dueCount !== null && dueCount > 0 && (
                <span className="nav-badge mono">{dueCount}</span>
              )}
            </button>
          ))}

          <div className="nav-group-label">Practice</div>
          {PRACTICE_NAV.map((item) => (
            <button
              key={item.key}
              type="button"
              className={active === item.key ? "active" : ""}
              onClick={() => onNavigate(item.key)}
            >
              <span>{item.label}</span>
            </button>
          ))}

          <div className="nav-group-label">Account</div>
          <button
            type="button"
            className={active === "settings" ? "active" : ""}
            onClick={() => onNavigate("settings")}
          >
            <span>Settings</span>
          </button>
        </nav>

        <div className="app-profile">
          <div className="av">{initial}</div>
          <div>
            <div className="an">{profile.displayName}</div>
            <div className="ag">
              {profile.goal ?? "learning"} · {profile.cefrEstimate ?? "new"}
            </div>
          </div>
        </div>
      </aside>

      <main className="app-main">{children}</main>
    </div>
  );
}
