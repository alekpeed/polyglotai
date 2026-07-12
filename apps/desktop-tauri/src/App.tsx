import { useEffect, useState } from "react";
import { createRepos, type Database, type Repos } from "@polyglotai/core-learning";
import type { LoadedPack } from "@polyglotai/language-pack-sdk";
import type { LearnerProfile } from "@polyglotai/shared-types";
import { bootstrap } from "./app/bootstrap";
import { AppShell, type NavKey } from "./app/AppShell";
import { Auth } from "./screens/Auth";
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
  const [boot, setBoot] = useState<{ db: Database; pack: LoadedPack } | null>(null);
  const [repos, setRepos] = useState<Repos | null>(null);
  const [profile, setProfile] = useState<LearnerProfile | null>(null);
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
      setProfile(null);
      return;
    }
    const activeRepos = supabase && user ? createSupabaseRepos(supabase, user.id, boot.db) : createRepos(boot.db);
    setRepos(activeRepos);
    activeRepos.profiles
      .getFirst()
      .then(setProfile)
      .catch((e) => setError(String(e)));
  }, [boot, user]);

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

  if (!repos) {
    return (
      <main className="container">
        <h1>PolyglotAI</h1>
        <p>Loading…</p>
      </main>
    );
  }

  if (!profile) {
    return <Onboarding repos={repos} pack={boot.pack} onComplete={setProfile} />;
  }

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
      screen = <Library repos={repos} profile={profile} onDone={goHome} />;
      break;
    case "tutor":
      screen = (
        <Tutor repos={repos} profile={profile} pack={boot.pack} onDone={goHome} onOpenSettings={openSettings} />
      );
      break;
    case "conversation":
      screen = (
        <Conversation
          repos={repos}
          profile={profile}
          pack={boot.pack}
          onDone={goHome}
          onOpenSettings={openSettings}
        />
      );
      break;
    case "interpreter":
      screen = (
        <Interpreter
          repos={repos}
          profile={profile}
          pack={boot.pack}
          onDone={goHome}
          onOpenSettings={openSettings}
        />
      );
      break;
    case "pronunciation":
      screen = (
        <Pronunciation
          repos={repos}
          profile={profile}
          pack={boot.pack}
          onDone={goHome}
          onOpenSettings={openSettings}
        />
      );
      break;
    case "settings":
      screen = <Settings repos={repos} profile={profile} onSaved={setProfile} onDone={goHome} />;
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
    <AppShell repos={repos} profile={profile} pack={boot.pack} active={view} onNavigate={setView}>
      {screen}
    </AppShell>
  );
}

export default App;
