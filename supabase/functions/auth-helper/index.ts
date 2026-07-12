// Small helper alongside openai-proxy: looks up a password hint by email for the "forgot
// password" screen. Has to run server-side with the service-role key — signup/login/password
// reset itself all go straight from the client to Supabase Auth (no proxy needed there), but a
// user's own metadata (where the hint lives) can only be read by an authenticated admin client,
// and you're logged out by definition when you've forgotten your password.

import { createClient } from "jsr:@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

const supabase = createClient(SUPABASE_URL, SERVICE_ROLE_KEY);

const CORS_HEADERS = {
  "access-control-allow-origin": "*",
  "access-control-allow-headers": "authorization, content-type",
  "access-control-allow-methods": "POST, OPTIONS",
};

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...CORS_HEADERS, "content-type": "application/json" },
  });
}

/** Strips the function's own routing prefix, mirroring openai-proxy's routePath. */
function routePath(url: URL): string {
  return url.pathname.replace(/^\/(functions\/v1\/)?auth-helper/, "") || "/";
}

async function passwordHint(req: Request): Promise<Response> {
  let email = "";
  try {
    const body = (await req.json()) as { email?: string };
    email = typeof body.email === "string" ? body.email.trim().toLowerCase() : "";
  } catch {
    // falls through to the empty-email response below
  }
  if (!email) return json({ hint: null });

  // No direct getUserByEmail on the admin API — list and match. Fine at this app's user scale;
  // revisit with a paginated/filtered lookup if that ever stops being true.
  const { data, error } = await supabase.auth.admin.listUsers({ perPage: 1000 });
  if (error) return json({ hint: null });

  const match = data.users.find((u) => u.email?.toLowerCase() === email);
  const hint = (match?.user_metadata as Record<string, unknown> | undefined)?.passwordHint;

  // Always 200 with the same shape whether the account exists or just has no hint set — the
  // point of doing the lookup server-side in the first place is to avoid leaking which emails
  // have accounts via a distinguishable response.
  return json({ hint: typeof hint === "string" && hint ? hint : null });
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response(null, { headers: CORS_HEADERS });
  if (req.method !== "POST") return json({ error: "not found" }, 404);

  const path = routePath(new URL(req.url));
  if (path === "/password-hint") return passwordHint(req);

  return json({ error: "not found" }, 404);
});
