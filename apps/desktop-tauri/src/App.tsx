import { useEffect, useState } from "react";
import type { Repos } from "@polyglotai/core-learning";
import type { LoadedPack } from "@polyglotai/language-pack-sdk";
import type { LearnerProfile } from "@polyglotai/shared-types";
import { bootstrap } from "./app/bootstrap";
import { AppShell, type NavKey } from "./app/AppShell";
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
import "./App.css";

function App() {
  const [ready, setReady] = useState<{ repos: Repos; pack: LoadedPack } | null>(null);
  const [profile, setProfile] = useState<LearnerProfile | null>(null);
  const [view, setView] = useState<NavKey>("dashboard");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    bootstrap()
      .then(({ repos, pack, profile }) => {
        setReady({ repos, pack });
        setProfile(profile);
      })
      .catch((e) => setError(String(e)));
  }, []);

  if (error) {
    return (
      <main className="container">
        <h1>PolyglotAI</h1>
        <p className="error">Failed to start: {error}</p>
      </main>
    );
  }

  if (!ready) {
    return (
      <main className="container">
        <h1>PolyglotAI</h1>
        <p>Loading…</p>
      </main>
    );
  }

  if (!profile) {
    return <Onboarding repos={ready.repos} pack={ready.pack} onComplete={setProfile} />;
  }

  const goHome = () => setView("dashboard");
  const openSettings = () => setView("settings");

  let screen: React.ReactNode;
  switch (view) {
    case "review":
      screen = <Review repos={ready.repos} profile={profile} onDone={goHome} />;
      break;
    case "drill":
      screen = <Drill repos={ready.repos} profile={profile} onDone={goHome} />;
      break;
    case "library":
      screen = <Library repos={ready.repos} profile={profile} onDone={goHome} />;
      break;
    case "tutor":
      screen = (
        <Tutor repos={ready.repos} profile={profile} pack={ready.pack} onDone={goHome} onOpenSettings={openSettings} />
      );
      break;
    case "conversation":
      screen = (
        <Conversation
          repos={ready.repos}
          profile={profile}
          pack={ready.pack}
          onDone={goHome}
          onOpenSettings={openSettings}
        />
      );
      break;
    case "interpreter":
      screen = (
        <Interpreter
          repos={ready.repos}
          profile={profile}
          pack={ready.pack}
          onDone={goHome}
          onOpenSettings={openSettings}
        />
      );
      break;
    case "pronunciation":
      screen = (
        <Pronunciation
          repos={ready.repos}
          profile={profile}
          pack={ready.pack}
          onDone={goHome}
          onOpenSettings={openSettings}
        />
      );
      break;
    case "settings":
      screen = <Settings repos={ready.repos} profile={profile} onSaved={setProfile} onDone={goHome} />;
      break;
    default:
      screen = (
        <Dashboard
          repos={ready.repos}
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
    <AppShell repos={ready.repos} profile={profile} pack={ready.pack} active={view} onNavigate={setView}>
      {screen}
    </AppShell>
  );
}

export default App;
