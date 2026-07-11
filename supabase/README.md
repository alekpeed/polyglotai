# AI backend proxy

The desktop app never holds your OpenAI key. It talks to this Supabase Edge Function, which
holds the key as a server secret and forwards requests to OpenAI. Each app install registers
once for an opaque device token (stored locally, not an OpenAI credential) — nothing for a
user to type in.

- `migrations/0001_device_proxy.sql` — `devices` (hashed tokens) and `usage_log` (per-request
  logging; no cap is enforced yet, by design — see the code comments if you want to add one).
- `functions/openai-proxy/index.ts` — the proxy itself: `POST /register`, `POST
  /chat/completions`, `POST /audio/transcriptions`, `POST /audio/speech`.

## One-time setup

You'll need a Supabase account/project and the Supabase CLI
(`npm install -g supabase` or see supabase.com/docs/guides/cli).

```sh
# From the repo root:
supabase login
supabase link --project-ref YOUR-PROJECT-REF   # find this in your Supabase project's URL/settings

# Apply the schema:
supabase db push

# Set your real OpenAI key as a server-only secret — never committed, never shipped in the app:
supabase secrets set OPENAI_API_KEY=sk-...

# Deploy the function:
supabase functions deploy openai-proxy --no-verify-jwt
```

`--no-verify-jwt` disables Supabase's own auth layer for this function — we're doing our own
lightweight auth (the device token) instead, since there's no user-login system here.

## Point the app at it

In `apps/desktop-tauri/`, copy `.env.example` to `.env.local` and set:

```
VITE_AI_PROXY_URL=https://YOUR-PROJECT-REF.supabase.co/functions/v1/openai-proxy
```

Then build/run the app as usual (`pnpm dev` / `pnpm build`). Without this set, AI screens
(Tutor, Conversation, Live Interpreter, Pronunciation) show "AI features aren't available right
now" — everything else works normally.

Conversation and Live Interpreter also speak AI turns aloud (`gpt-4o-mini-tts`, ~$0.015/min)
and accept a spoken reply via the mic button (Whisper, same as Pronunciation) — both ride the
same device token, no separate setup.

## Verify it's working

```sh
# Should return {"token":"..."}
curl -X POST https://YOUR-PROJECT-REF.supabase.co/functions/v1/openai-proxy/register

# Should return a normal OpenAI chat-completion response (swap in the token from above):
curl -X POST https://YOUR-PROJECT-REF.supabase.co/functions/v1/openai-proxy/chat/completions \
  -H "authorization: Bearer YOUR-DEVICE-TOKEN" \
  -H "content-type: application/json" \
  -d '{"model":"gpt-4o-mini","messages":[{"role":"user","content":"say hi in one word"}]}'
```

## Redeploying after changes

Only `functions/openai-proxy/index.ts` changed:

```sh
supabase functions deploy openai-proxy --no-verify-jwt
```

Only the schema changed (new migration file):

```sh
supabase db push
```

## What this does and doesn't protect against

- **Does:** keep the real OpenAI key off every user's machine — it only ever exists as a
  Supabase secret and in OpenAI's own systems.
- **Does not (yet):** cap usage per device. Per your choice, `usage_log` currently just
  records every request (device, endpoint, model, tokens) without rejecting anything over a
  threshold. If this ever needs a real budget cap, add a `SELECT sum(tokens_used) ... WHERE
  device_id = ? AND created_at > now() - interval '1 day'` check in `index.ts` before
  forwarding to OpenAI, and reject with a 429 over the limit.
- A device token is exactly as sensitive as a login session token for this proxy — if the app
  is ever handed to genuinely untrusted parties at scale, revisit this.
