import { useState, type FormEvent } from "react";
import { sendPasswordReset, signIn, signUp } from "../auth/authContext";
import { supabase } from "../auth/supabaseClient";

type Mode = "signin" | "signup" | "forgot";

export function Auth() {
  const [mode, setMode] = useState<Mode>("signin");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  if (!supabase) {
    return <div className="onboard-shell"><main className="onboard-form-panel"><div className="onboarding onboard-form"><h2 className="onboard-title">Accounts aren't set up for this build</h2><p className="subtitle">This build has no Supabase URL/key configured.</p></div></main></div>;
  }

  function switchMode(next: Mode) {
    setMode(next);
    setError(null);
    setNotice(null);
  }

  async function handleSignIn(e: FormEvent) {
    e.preventDefault();
    setBusy(true); setError(null);
    const result = await signIn(email.trim(), password);
    setBusy(false);
    if (result.error) setError(result.error);
  }

  async function handleSignUp(e: FormEvent) {
    e.preventDefault();
    setBusy(true); setError(null);
    const result = await signUp({ email: email.trim(), password });
    setBusy(false);
    if (result.error) return setError(result.error);
    setNotice("Account created - check your email to confirm, then sign in.");
    setMode("signin");
  }

  async function handleSendReset() {
    setBusy(true); setError(null);
    const result = await sendPasswordReset(email.trim());
    setBusy(false);
    if (result.error) return setError(result.error);
    setNotice("Reset email sent - check your inbox.");
  }

  const intro = <aside className="onboard-hero"><div className="onboard-sunburst" aria-hidden="true" /><div className="onboard-hero-content"><span className="eyebrow">PolyglotAI</span><h1 className="onboard-headline">Fala ai.</h1><p>Your account, your progress - synced wherever you sign in.</p></div><div className="onboard-skyline" aria-hidden="true"><span className="a1" /><span className="a2" /><span className="a3" /></div></aside>;

  return (
    <div className="onboard-shell">
      {intro}
      <main className="onboard-form-panel">
        {mode === "signin" && <form className="onboarding onboard-form" onSubmit={handleSignIn}>
          <h2 className="onboard-title">Sign in</h2>
          <label>Email<input type="email" value={email} onChange={(e) => setEmail(e.currentTarget.value)} autoFocus required /></label>
          <label>Password<input type="password" value={password} onChange={(e) => setPassword(e.currentTarget.value)} required /></label>
          {error && <p className="error">{error}</p>}{notice && <p className="auth-notice">{notice}</p>}
          <button type="submit" className="btn-primary" disabled={busy}>{busy ? "Signing in..." : "Sign in"} <span className="arrow">-&gt;</span></button>
          <div className="auth-links"><button type="button" className="link" onClick={() => switchMode("signup")}>Need an account? Sign up</button><button type="button" className="link" onClick={() => switchMode("forgot")}>Forgot password?</button></div>
        </form>}
        {mode === "signup" && <form className="onboarding onboard-form" onSubmit={handleSignUp}>
          <h2 className="onboard-title">Create your account</h2>
          <label>Email<input type="email" value={email} onChange={(e) => setEmail(e.currentTarget.value)} autoFocus required /></label>
          <label>Password<input type="password" value={password} onChange={(e) => setPassword(e.currentTarget.value)} minLength={6} required /></label>
          {error && <p className="error">{error}</p>}
          <button type="submit" className="btn-primary" disabled={busy}>{busy ? "Creating..." : "Create account"} <span className="arrow">-&gt;</span></button>
          <div className="auth-links"><button type="button" className="link" onClick={() => switchMode("signin")}>Already have an account? Sign in</button></div>
        </form>}
        {mode === "forgot" && <form className="onboarding onboard-form" onSubmit={(e) => { e.preventDefault(); void handleSendReset(); }}>
          <h2 className="onboard-title">Reset password</h2>
          <p className="subtitle">Enter your email and we will send a reset link.</p>
          <label>Email<input type="email" value={email} onChange={(e) => setEmail(e.currentTarget.value)} autoFocus required /></label>
          {error && <p className="error">{error}</p>}{notice && <p className="auth-notice">{notice}</p>}
          <button type="submit" className="btn-primary" disabled={busy || !email.trim()}>{busy ? "Sending..." : "Email me a reset link"}</button>
          <div className="auth-links"><button type="button" className="link" onClick={() => switchMode("signin")}>Back to sign in</button></div>
        </form>}
      </main>
    </div>
  );
}
