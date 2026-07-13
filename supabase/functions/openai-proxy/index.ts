import { createClient } from "jsr:@supabase/supabase-js@2";

const OPENAI_API_KEY = Deno.env.get("OPENAI_API_KEY");
const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const ACCESS_PASSCODE = Deno.env.get("ACCESS_PASSCODE");
const OPENAI_BASE = "https://api.openai.com/v1";

const REQUESTS_PER_WINDOW = Number(Deno.env.get("PROXY_REQUESTS_PER_WINDOW") ?? "20");
const WINDOW_SECONDS = Number(Deno.env.get("PROXY_WINDOW_SECONDS") ?? "60");
const REQUESTS_PER_DAY = Number(Deno.env.get("PROXY_REQUESTS_PER_DAY") ?? "200");
const REGISTRATIONS_PER_WINDOW = Number(Deno.env.get("PROXY_REGISTRATIONS_PER_WINDOW") ?? "10");
const REGISTRATION_WINDOW_SECONDS = Number(Deno.env.get("PROXY_REGISTRATION_WINDOW_SECONDS") ?? "3600");
const MAX_CHAT_BYTES = Number(Deno.env.get("PROXY_MAX_CHAT_BYTES") ?? "32768");
const MAX_TTS_BYTES = Number(Deno.env.get("PROXY_MAX_TTS_BYTES") ?? "8192");
const MAX_AUDIO_BYTES = Number(Deno.env.get("PROXY_MAX_AUDIO_BYTES") ?? "26214400");
const CHAT_MODELS = new Set((Deno.env.get("PROXY_CHAT_MODELS") ?? "gpt-4o-mini").split(",").map((m) => m.trim()).filter(Boolean));
const TTS_MODELS = new Set((Deno.env.get("PROXY_TTS_MODELS") ?? "gpt-4o-mini-tts").split(",").map((m) => m.trim()).filter(Boolean));
const TRANSCRIPTION_MODELS = new Set((Deno.env.get("PROXY_TRANSCRIPTION_MODELS") ?? "whisper-1").split(",").map((m) => m.trim()).filter(Boolean));

const supabase = createClient(SUPABASE_URL, SERVICE_ROLE_KEY);
const CORS_HEADERS = {
  "access-control-allow-origin": "*",
  "access-control-allow-headers": "authorization, content-type",
  "access-control-allow-methods": "POST, OPTIONS",
};

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { ...CORS_HEADERS, "content-type": "application/json" } });
}

function tooLarge(req: Request, maxBytes: number): boolean {
  const value = Number(req.headers.get("content-length") ?? "0");
  return Number.isFinite(value) && value > maxBytes;
}

function validLimit(value: number): boolean {
  return Number.isSafeInteger(value) && value > 0;
}

async function sha256Hex(input: string): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(input));
  return Array.from(new Uint8Array(digest)).map((b) => b.toString(16).padStart(2, "0")).join("");
}

function routePath(url: URL): string {
  return url.pathname.replace(/^\/(functions\/v1\/)?openai-proxy/, "") || "/";
}

async function registerDevice(req: Request): Promise<Response> {
  if (!ACCESS_PASSCODE) return json({ error: "registration is disabled until ACCESS_PASSCODE is configured" }, 503);
  let passcode = "";
  try {
    const body = (await req.json()) as { passcode?: string };
    passcode = typeof body.passcode === "string" ? body.passcode : "";
  } catch { /* rejected below */ }
  if (passcode !== ACCESS_PASSCODE) return json({ error: "invalid passcode" }, 401);
  if (!validLimit(REGISTRATIONS_PER_WINDOW) || !validLimit(REGISTRATION_WINDOW_SECONDS)) return json({ error: "server quota misconfigured" }, 500);

  const token = crypto.randomUUID() + crypto.randomUUID();
  const { data, error } = await supabase.rpc("register_device_with_rate_limit", {
    p_token_hash: await sha256Hex(token),
    p_max_registrations: REGISTRATIONS_PER_WINDOW,
    p_window_seconds: REGISTRATION_WINDOW_SECONDS,
  });
  if (error) return json({ error: "registration failed" }, 500);
  if (!data) return json({ error: "registration rate limit exceeded" }, 429);
  return json({ token });
}

async function authenticateAndConsume(req: Request): Promise<{ id: string } | null> {
  const token = (req.headers.get("authorization") ?? "").startsWith("Bearer ")
    ? (req.headers.get("authorization") ?? "").slice(7) : "";
  if (!token || !validLimit(REQUESTS_PER_WINDOW) || !validLimit(WINDOW_SECONDS) || !validLimit(REQUESTS_PER_DAY)) return null;
  const { data } = await supabase.from("devices").select("id").eq("token_hash", await sha256Hex(token)).maybeSingle();
  if (!data) return null;
  const { data: allowed, error } = await supabase.rpc("consume_device_proxy_quota", {
    p_device_id: data.id,
    p_max_requests_per_window: REQUESTS_PER_WINDOW,
    p_window_seconds: WINDOW_SECONDS,
    p_max_requests_per_day: REQUESTS_PER_DAY,
  });
  if (error || !allowed) return null;
  return { id: data.id as string };
}

async function logUsage(deviceId: string, endpoint: string, model: string | null, tokensUsed: number | null): Promise<void> {
  await supabase.from("usage_log").insert({ device_id: deviceId, endpoint, model, tokens_used: tokensUsed });
}

async function proxyJson(req: Request, deviceId: string, path: "/chat/completions" | "/audio/speech"): Promise<Response> {
  const maxBytes = path === "/chat/completions" ? MAX_CHAT_BYTES : MAX_TTS_BYTES;
  if (tooLarge(req, maxBytes)) return json({ error: "request body too large" }, 413);
  const bodyText = await req.text();
  if (new TextEncoder().encode(bodyText).length > maxBytes) return json({ error: "request body too large" }, 413);
  let body: { model?: unknown };
  try { body = JSON.parse(bodyText) as { model?: unknown }; } catch { return json({ error: "invalid JSON" }, 400); }
  const allowedModels = path === "/chat/completions" ? CHAT_MODELS : TTS_MODELS;
  if (typeof body.model !== "string" || !allowedModels.has(body.model)) return json({ error: "unsupported model" }, 400);

  const upstream = await fetch(`${OPENAI_BASE}${path}`, { method: "POST", headers: { "content-type": "application/json", authorization: `Bearer ${OPENAI_API_KEY}` }, body: bodyText });
  if (path === "/audio/speech" && upstream.ok) {
    const audio = await upstream.arrayBuffer();
    await logUsage(deviceId, "audio.speech", body.model, null);
    return new Response(audio, { status: upstream.status, headers: { ...CORS_HEADERS, "content-type": upstream.headers.get("content-type") ?? "audio/mpeg" } });
  }
  const resultText = await upstream.text();
  let tokens: number | null = null;
  try { tokens = (JSON.parse(resultText) as { usage?: { total_tokens?: number } }).usage?.total_tokens ?? null; } catch { /* upstream error body */ }
  await logUsage(deviceId, path.slice(1).replace("/", "."), body.model, tokens);
  return new Response(resultText, { status: upstream.status, headers: { ...CORS_HEADERS, "content-type": "application/json" } });
}

async function proxyTranscriptions(req: Request, deviceId: string): Promise<Response> {
  if (tooLarge(req, MAX_AUDIO_BYTES)) return json({ error: "audio upload too large" }, 413);
  const form = await req.formData();
  const model = form.get("model");
  const file = form.get("file");
  if (typeof model !== "string" || !TRANSCRIPTION_MODELS.has(model)) return json({ error: "unsupported model" }, 400);
  if (!(file instanceof File) || file.size > MAX_AUDIO_BYTES) return json({ error: "invalid or oversized audio upload" }, 413);
  const upstream = await fetch(`${OPENAI_BASE}/audio/transcriptions`, { method: "POST", headers: { authorization: `Bearer ${OPENAI_API_KEY}` }, body: form });
  const resultText = await upstream.text();
  await logUsage(deviceId, "audio.transcriptions", model, null);
  return new Response(resultText, { status: upstream.status, headers: { ...CORS_HEADERS, "content-type": "application/json" } });
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response(null, { headers: CORS_HEADERS });
  const path = routePath(new URL(req.url));
  if (path === "/register" && req.method === "POST") return registerDevice(req);
  if (req.method !== "POST") return json({ error: "not found" }, 404);
  if (!OPENAI_API_KEY) return json({ error: "server misconfigured" }, 500);
  const device = await authenticateAndConsume(req);
  if (!device) return json({ error: "invalid token or quota exceeded" }, 429);
  if (path === "/chat/completions") return proxyJson(req, device.id, path);
  if (path === "/audio/speech") return proxyJson(req, device.id, path);
  if (path === "/audio/transcriptions") return proxyTranscriptions(req, device.id);
  return json({ error: "not found" }, 404);
});
