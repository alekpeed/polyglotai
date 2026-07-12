import { createClient, type SupabaseClient } from "@supabase/supabase-js";

const SUPABASE_URL = (import.meta.env.VITE_SUPABASE_URL as string | undefined)?.replace(/\/$/, "");
const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY as string | undefined;

/** Null when accounts aren't configured for this build — same "just unavailable, not broken"
 * pattern as the AI proxy (VITE_AI_PROXY_URL unset). The anon key is meant to be public; row
 * level security (see supabase/migrations) is what actually keeps one account's data away from
 * another's, not keeping this key secret. */
export const supabase: SupabaseClient | null =
  SUPABASE_URL && SUPABASE_ANON_KEY ? createClient(SUPABASE_URL, SUPABASE_ANON_KEY) : null;

/** The auth-helper edge function lives in the same Supabase project as the AI proxy, so its URL
 * is derived from the same project URL rather than needing its own env var. */
export const AUTH_HELPER_URL = SUPABASE_URL ? `${SUPABASE_URL}/functions/v1/auth-helper` : undefined;
