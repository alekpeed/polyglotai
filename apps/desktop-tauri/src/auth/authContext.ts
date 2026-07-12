import { useEffect, useState } from "react";
import type { Session, User } from "@supabase/supabase-js";
import { AUTH_HELPER_URL, supabase } from "./supabaseClient";

export interface SignUpInput {
  email: string;
  password: string;
  /** Optional — stored as auth user_metadata, surfaced by the forgot-password screen via
   * auth-helper. Never anything sensitive; it's shown to anyone who enters this email. */
  passwordHint?: string;
}

export async function signUp({ email, password, passwordHint }: SignUpInput): Promise<{ error: string | null }> {
  if (!supabase) return { error: "Accounts aren't configured for this build." };
  const { error } = await supabase.auth.signUp({
    email,
    password,
    ...(passwordHint ? { options: { data: { passwordHint } } } : {}),
  });
  return { error: error?.message ?? null };
}

export async function signIn(email: string, password: string): Promise<{ error: string | null }> {
  if (!supabase) return { error: "Accounts aren't configured for this build." };
  const { error } = await supabase.auth.signInWithPassword({ email, password });
  return { error: error?.message ?? null };
}

export async function signOut(): Promise<void> {
  await supabase?.auth.signOut();
}

export async function sendPasswordReset(email: string): Promise<{ error: string | null }> {
  if (!supabase) return { error: "Accounts aren't configured for this build." };
  const { error } = await supabase.auth.resetPasswordForEmail(email);
  return { error: error?.message ?? null };
}

export async function updatePassword(newPassword: string): Promise<{ error: string | null }> {
  if (!supabase) return { error: "Accounts aren't configured for this build." };
  const { error } = await supabase.auth.updateUser({ password: newPassword });
  return { error: error?.message ?? null };
}

/** Looks up a stored password hint by email — returns null if none is set (or the account
 * doesn't exist; the server intentionally doesn't distinguish the two, see auth-helper). */
export async function fetchPasswordHint(email: string): Promise<string | null> {
  if (!AUTH_HELPER_URL) return null;
  try {
    const res = await fetch(`${AUTH_HELPER_URL}/password-hint`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ email }),
    });
    if (!res.ok) return null;
    const data = (await res.json()) as { hint?: string | null };
    return data.hint ?? null;
  } catch {
    return null;
  }
}

interface AuthState {
  session: Session | null;
  user: User | null;
  loading: boolean;
}

/** Tracks the current Supabase session live — updates on sign-in/out/token refresh anywhere in
 * the app. `loading` is only ever true for the initial session check on mount. */
export function useAuthSession(): AuthState {
  const [state, setState] = useState<AuthState>({ session: null, user: null, loading: true });

  useEffect(() => {
    if (!supabase) {
      setState({ session: null, user: null, loading: false });
      return;
    }
    let alive = true;
    supabase.auth.getSession().then(({ data }) => {
      if (alive) setState({ session: data.session, user: data.session?.user ?? null, loading: false });
    });
    const { data: sub } = supabase.auth.onAuthStateChange((_event, session) => {
      if (alive) setState({ session, user: session?.user ?? null, loading: false });
    });
    return () => {
      alive = false;
      sub.subscription.unsubscribe();
    };
  }, []);

  return state;
}
