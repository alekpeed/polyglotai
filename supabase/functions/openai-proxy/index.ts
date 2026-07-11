// Deno Edge Function — the only place OPENAI_API_KEY ever lives. The desktop app never talks
// to OpenAI directly; it talks here, authenticated by a per-install device token (opaque,
// registered once via POST /register, never an OpenAI credential). Mirrors the exact wire
// shape OpenAIProvider/WhisperProvider already speak (packages/ai-orchestration,
// packages/pronunciation) so the client only needed a baseUrl swap, not a rewrite.
//
// Deploy: supabase functions deploy openai-proxy
// Secrets required: OPENAI_API_KEY, SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY
// (the latter two are auto-injected by the Supabase platform at runtime; only OPENAI_API_KEY
// needs to be set explicitly — see supabase/README.md.)

import { createClient } from "jsr:@supabase/supabase-js@2";

const OPENAI_API_KEY = Deno.env.get("OPENAI_API_KEY");
const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const OPENAI_BASE = "https://api.openai.com/v1";

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

async function sha256Hex(input: string): Promise<string> {
  const bytes = new TextEncoder().encode(input);
  const digest = await crypto.subtle.digest("SHA-256", bytes);
  return Array.from(new Uint8Array(digest))
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");
}

/** Strips the function's own routing prefix so path matching below doesn't care whether
 * Supabase invoked us via /functions/v1/openai-proxy/... or a bare /openai-proxy/... */
function routePath(url: URL): string {
  return url.pathname.replace(/^\/(functions\/v1\/)?openai-proxy/, "") || "/";
}

async function registerDevice(): Promise<Response> {
  const token = crypto.randomUUID() + crypto.randomUUID();
  const tokenHash = await sha256Hex(token);
  const { error } = await supabase.from("devices").insert({ token_hash: tokenHash });
  if (error) return json({ error: "registration failed" }, 500);
  return json({ token });
}

async function authenticateDevice(req: Request): Promise<string | null> {
  const auth = req.headers.get("authorization") ?? "";
  const token = auth.startsWith("Bearer ") ? auth.slice(7) : "";
  if (!token) return null;
  const tokenHash = await sha256Hex(token);
  const { data } = await supabase.from("devices").select("id").eq("token_hash", tokenHash).maybeSingle();
  if (!data) return null;
  // Awaited, not fire-and-forget: edge runtimes can tear down the isolate right after the
  // response is sent, silently dropping un-awaited background writes.
  await supabase.from("devices").update({ last_seen_at: new Date().toISOString() }).eq("id", data.id);
  return data.id as string;
}

async function logUsage(deviceId: string, endpoint: string, model: string | null, tokensUsed: number | null): Promise<void> {
  await supabase.from("usage_log").insert({ device_id: deviceId, endpoint, model, tokens_used: tokensUsed });
}

async function proxyChatCompletions(req: Request, deviceId: string): Promise<Response> {
  const bodyText = await req.text();
  const upstream = await fetch(`${OPENAI_BASE}/chat/completions`, {
    method: "POST",
    headers: { "content-type": "application/json", authorization: `Bearer ${OPENAI_API_KEY}` },
    body: bodyText,
  });
  const resultText = await upstream.text();

  try {
    const reqBody = JSON.parse(bodyText) as { model?: string };
    const resBody = JSON.parse(resultText) as { usage?: { total_tokens?: number } };
    await logUsage(deviceId, "chat.completions", reqBody.model ?? null, resBody.usage?.total_tokens ?? null);
  } catch {
    // Logging is best-effort in the sense that a malformed body shouldn't break the response —
    // but the write itself is awaited so it isn't dropped by the runtime tearing down early.
  }

  return new Response(resultText, { status: upstream.status, headers: { ...CORS_HEADERS, "content-type": "application/json" } });
}

async function proxyTranscriptions(req: Request, deviceId: string): Promise<Response> {
  const form = await req.formData();
  const model = form.get("model");
  const upstream = await fetch(`${OPENAI_BASE}/audio/transcriptions`, {
    method: "POST",
    headers: { authorization: `Bearer ${OPENAI_API_KEY}` },
    body: form,
  });
  const resultText = await upstream.text();
  await logUsage(deviceId, "audio.transcriptions", typeof model === "string" ? model : null, null);
  return new Response(resultText, { status: upstream.status, headers: { ...CORS_HEADERS, "content-type": "application/json" } });
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response(null, { headers: CORS_HEADERS });

  const path = routePath(new URL(req.url));

  // Registration never touches OpenAI, so it must not be gated behind OPENAI_API_KEY — a
  // fresh deploy (secret not set yet) should still let devices register.
  if (path === "/register" && req.method === "POST") return registerDevice();

  if (req.method !== "POST") return json({ error: "not found" }, 404);

  if (!OPENAI_API_KEY) return json({ error: "server misconfigured: OPENAI_API_KEY not set" }, 500);

  const deviceId = await authenticateDevice(req);
  if (!deviceId) return json({ error: "invalid or missing device token" }, 401);

  if (path === "/chat/completions") return proxyChatCompletions(req, deviceId);
  if (path === "/audio/transcriptions") return proxyTranscriptions(req, deviceId);

  return json({ error: "not found" }, 404);
});
