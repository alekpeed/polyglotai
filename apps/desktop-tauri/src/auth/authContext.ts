import { useEffect, useState } from "react";
import type { Session, User } from "@supabase/supabase-js";
import { supabase } from "./supabaseClient";

export interface SignUpInput {
  email: string;
  password: string;
}

export async function signUp({ email, password }: SignUpInput): Promise<{ error: string | null }> {
  if (!supabase) return { error: "Accounts aren't configured for this build." };
  const { error } = await supabase.auth.signUp({ email, password });
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

interface AuthState {
  session: Session | null;
  user: User | null;
  loading: boolean;
}

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
