import { useEffect, useRef, useState } from "react";
import { createRepos, type Database, type Repos } from "@polyglotai/core-learning";
import type { LoadedPack } from "@polyglotai/language-pack-sdk";
import type { LearnerProfile } from "@polyglotai/shared-types";
import { bootstrap, loadPackForId } from "./app/bootstrap";
import { AppShell, type NavKey } from "./app/AppShell";
import { clearStoredActiveProfileId, getStoredActiveProfileId, setStoredActiveProfileId } from "./app/activeProfile";
import { Auth } from "./screens/Auth";
import { LanguagePicker } from "./screens/LanguagePicker";
import { Onboarding } from "./screens/Onboarding";
import { Dashboard } from "./screens/Dashboard";
import { Review } from "./screens/Review";
import { Drill } from "./screens/Drill";
import { Library } from "./screens/Library";
import { Tutor } from "./screens/Tutor";
import { Conversation } from "./screens/Conversation";
import { Interpreter } from "./screens/Interpreter";
import { Settings } from "./screens/Settings";
import { Pronunciation } from "./screens/Pronunciation";
import { useAuthSession } from "./auth/authContext";
import { supabase } from "./auth/supabaseClient";
import { createSupabaseRepos } from "./cloud/supabaseRepos";
import "./App.css";

function App() {
  const [boot, setBoot] = useState<{ db: Database } | null>(null);
  const [repos, setRepos] = useState<Repos | null>(null);
  const [profiles, setProfiles] = useState<LearnerProfile[] | null>(null);
  const [activeProfile, setActiveProfile] = useState<LearnerProfile | null>(null);
  const [activePack, setActivePack] = useState<LoadedPack | null>(null);
  const [view, setView] = useState<NavKey>("dashboard");
  const [error, setError] = useState<string | null>(null);
  const { user, loading: authLoading } = useAuthSession();

  useEffect(() => {
    bootstrap()
      .then(setBoot)
      .catch((e) => setError(String(e)));
  }, []);

  // Accounts builds (supabase configured) gate on the signed-in user; local-only builds keep
  // the original behavior of one profile straight off the local DB, no auth required.
  useEffect(() => {
    if (!boot) return;
    if (supabase && !user) {
      setRepos(null);
      setProfiles(null);
      setActiveProfile(null);
      setActivePack(null);
      return;
    }
    const activeRepos = supabase && user ? createSupabaseRepos(supabase, user.id, boot.db) : createRepos(boot.db);
    setRepos(activeRepos);
    activeRepos.profiles
      .listAll()
      .then(setProfiles)
      .catch((e) => setError(String(e)));
  }, [boot, user]);

  // Auto-pick an active profile: the one remembered from last time, or the only one there is —
  // otherwise the language picker shows and the learner chooses (or starts something new). Only
  // attempted once per session (the ref, not just "no active profile") — otherwise this would
  // immediately re-select the same lone profile right after an explicit "switch language" put
  // activeProfile back to null, making the switcher impossible to use with a single language.
  const autoSelectAttempted = useRef(false);
  useEffect(() => {
    if (!profiles || activeProfile || autoSelectAttempted.current) return;
    autoSelectAttempted.current = true;
    const storedId = getStoredActiveProfileId();
    const remembered = profiles.find((p) => p.id === storedId);
    if (remembered) {
      setActiveProfile(remembered);
    } else if (profiles.length === 1) {
      setActiveProfile(profiles[0]);
      setStoredActiveProfileId(profiles[0].id);
    }
  }, [profiles, activeProfile]);

  // Loads the active profile's full pack content once it's known — skipped if a "start new
  // language" pick already loaded that exact pack for the onboarding screen below.
  useEffect(() => {
    if (!activeProfile?.activePackId) return;
    if (activePack?.manifest.id === activeProfile.activePackId) return;
    loadPackForId(activeProfile.activePackId)
      .then(setActivePack)
      .catch((e) => setError(String(e)));
  }, [activeProfile, activePack]);

  // The pack theme (palette/hero treatment) is derived state, not a persisted preference —
  // recomputed every session from whichever pack is actually active (see App.css "Pack theme").
  // A micro-pack (manifest.basePack set — see bootstrap.ts loadPackForId) has no CSS of its own;
  // it inherits its parent language's visual identity by falling back to basePack here.
  useEffect(() => {
    if (activePack) document.documentElement.dataset.pack = activePack.manifest.basePack ?? activePack.manifest.id;
    else delete document.documentElement.dataset.pack;
  }, [activePack]);

  function handleContinue(profile: LearnerProfile) {
    setActiveProfile(profile);
    setStoredActiveProfileId(profile.id);
  }

  function handleStartNew(packId: string) {
    loadPackForId(packId)
      .then(setActivePack)
      .catch((e) => setError(String(e)));
  }

  function handleOnboardingComplete(profile: LearnerProfile) {
    setProfiles((prev) => [...(prev ?? []), profile]);
    setActiveProfile(profile);
    setStoredActiveProfileId(profile.id);
  }

  function handleSwitchLanguage() {
    clearStoredActiveProfileId();
    setActiveProfile(null);
    setActivePack(null);
  }

  if (error) {
    return (
      <main className="container">
        <h1>PolyglotAI</h1>
        <p className="error">Failed to start: {error}</p>
      </main>
    );
  }

  if (!boot || (supabase && authLoading)) {
    return (
      <main className="container">
        <h1>PolyglotAI</h1>
        <p>Loading…</p>
      </main>
    );
  }

  if (supabase && !user) {
    return <Auth />;
  }

  if (!repos || !profiles) {
    return (
      <main className="container">
        <h1>PolyglotAI</h1>
        <p>Loading…</p>
      </main>
    );
  }

  if (!activeProfile) {
    if (activePack) {
      return <Onboarding repos={repos} pack={activePack} onComplete={handleOnboardingComplete} />;
    }
    return <LanguagePicker existingProfiles={profiles} onContinue={handleContinue} onStartNew={handleStartNew} />;
  }

  if (!activePack) {
    return (
      <main className="container">
        <h1>PolyglotAI</h1>
        <p>Loading…</p>
      </main>
    );
  }

  const profile = activeProfile;
  const pack = activePack;
  const goHome = () => setView("dashboard");
  const openSettings = () => setView("settings");

  let screen: React.ReactNode;
  switch (view) {
    case "review":
      screen = <Review repos={repos} profile={profile} onDone={goHome} />;
      break;
    case "drill":
      screen = <Drill repos={repos} profile={profile} onDone={goHome} />;
      break;
    case "library":
      screen = <Library repos={repos} profile={profile} pack={pack} onDone={goHome} />;
      break;
    case "tutor":
      screen = <Tutor repos={repos} profile={profile} pack={pack} onDone={goHome} onOpenSettings={openSettings} />;
      break;
    case "conversation":
      screen = (
        <Conversation repos={repos} profile={profile} pack={pack} onDone={goHome} onOpenSettings={openSettings} />
      );
      break;
    case "interpreter":
      screen = (
        <Interpreter repos={repos} profile={profile} pack={pack} onDone={goHome} onOpenSettings={openSettings} />
      );
      break;
    case "pronunciation":
      screen = (
        <Pronunciation repos={repos} profile={profile} pack={pack} onDone={goHome} onOpenSettings={openSettings} />
      );
      break;
    case "settings":
      screen = (
        <Settings
          repos={repos}
          profile={profile}
          onSaved={setActiveProfile}
          onDone={goHome}
          onSwitchLanguage={handleSwitchLanguage}
        />
      );
      break;
    default:
      screen = (
        <Dashboard
          repos={repos}
          profile={profile}
          onStartReview={() => setView("review")}
          onStartDrill={() => setView("drill")}
          onOpenLibrary={() => setView("library")}
          onOpenTutor={() => setView("tutor")}
          onOpenConversation={() => setView("conversation")}
          onOpenInterpreter={() => setView("interpreter")}
          onOpenPronunciation={() => setView("pronunciation")}
          onOpenSettings={openSettings}
        />
      );
  }

  return (
    <AppShell repos={repos} profile={profile} pack={pack} active={view} onNavigate={setView}>
      {screen}
    </AppShell>
  );
}

export default App;
