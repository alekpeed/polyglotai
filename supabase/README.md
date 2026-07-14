# AI backend proxy

The app never ships an OpenAI key. It registers an opaque, per-install device token with this
Supabase Edge Function, which holds the real key as a server secret and forwards requests to
OpenAI. The token is stored locally on the device; it is not an OpenAI credential.

- `migrations/` — `devices` (hashed tokens + per-device quota counters) and `usage_log`
  (per-request logging), plus the cloud-account schema (`learner_profiles`, `review_items`, …)
  and the `record_review_and_result` / quota RPCs.
- `functions/openai-proxy/index.ts` — the proxy itself: `POST /register`, `POST
  /chat/completions`, `POST /audio/transcriptions`, `POST /audio/speech`.

## Server-side limits (enforced by default)

The proxy requires a registration passcode and rejects anything over these bounds:

- 10 registrations per hour, globally.
- 20 requests per device per minute.
- 200 requests per device per day.
- 32 KiB chat bodies, 8 KiB TTS bodies, 25 MiB audio uploads.
- Explicit model allow-lists (`gpt-4o-mini`, `gpt-4o-mini-tts`, `whisper-1` by default).

Each limit is overridable via a `PROXY_*` Supabase secret (see the constants at the top of
`index.ts`) — keep them server-side; do not move them into client code where a user could edit
them. The rate-limit bookkeeping runs in `SECURITY DEFINER` Postgres functions
(`register_device_with_rate_limit`, `consume_device_proxy_quota`) so concurrent requests can't
race through a check-then-increment gap.

## One-time setup

You'll need a Supabase account/project and the Supabase CLI
(`npm install -g supabase` or see supabase.com/docs/guides/cli).

```sh
# From the repo root:
supabase login
supabase link --project-ref YOUR-PROJECT-REF   # find this in your Supabase project's URL/settings

# Apply the schema:
supabase db push

# Server-only secrets — never committed, never shipped in the app:
supabase secrets set OPENAI_API_KEY=sk-...
supabase secrets set ACCESS_PASSCODE=use-a-strong-secret

# Deploy the function:
supabase functions deploy openai-proxy --no-verify-jwt
```

`--no-verify-jwt` disables Supabase's own auth layer for this function — we do our own
lightweight auth (the device token) instead, since there's no user-login system on the proxy.
`ACCESS_PASSCODE` is required: new devices cannot register until it is set.

## Point the app at it

In `apps/desktop-tauri/`, copy `.env.example` to `.env.local` and set:

```text
VITE_AI_PROXY_URL=https://YOUR-PROJECT-REF.supabase.co/functions/v1/openai-proxy
```

Then build/run the app as usual (`pnpm dev` / `pnpm build`). Without this set, the app falls
back to the shared production proxy baked into `src/ai/aiContext.ts`. The app prompts for the
registration passcode the first time a device registers; after that the cached device token is
reused. Treat that token like a session token.

Conversation and Live Interpreter also speak AI turns aloud (`gpt-4o-mini-tts`, ~$0.015/min)
and accept a spoken reply via the mic button (Whisper, same as Pronunciation) — both ride the
same device token, no separate setup.

## Verify it's working

```sh
# Should return {"token":"..."} — the passcode is required.
curl -X POST https://YOUR-PROJECT-REF.supabase.co/functions/v1/openai-proxy/register \
  -H "content-type: application/json" \
  -d '{"passcode":"YOUR-ACCESS-PASSCODE"}'

# Should return a normal OpenAI chat-completion response (swap in the token from above):
curl -X POST https://YOUR-PROJECT-REF.supabase.co/functions/v1/openai-proxy/chat/completions \
  -H "authorization: Bearer YOUR-DEVICE-TOKEN" \
  -H "content-type: application/json" \
  -d '{"model":"gpt-4o-mini","messages":[{"role":"user","content":"say hi in one word"}]}'
```

## Redeploying after changes

Automatic: `.github/workflows/deploy-supabase.yml` runs `supabase db push` +
`supabase functions deploy` on every push to `main` that touches `supabase/**`, using the
`SUPABASE_ACCESS_TOKEN` and `ACCESS_PASSCODE` repo secrets. Just push — nothing to run by hand.

Manual (e.g. after rotating a secret, with no code change): open the repo's Actions tab →
"Deploy Supabase" → "Run workflow." Or from the CLI:

```sh
supabase functions deploy openai-proxy --no-verify-jwt   # function only
supabase db push                                         # schema only
```

## Managing access

- **Let someone in:** make sure `ACCESS_PASSCODE` is set (repo secret + deployed), and tell
  them the passcode out of band (not in the link itself).
- **Stop new registrations:** rotate or delete the `ACCESS_PASSCODE` secret and redeploy. This
  only blocks *new* devices — anyone already registered keeps their token (the passcode is a
  one-time handshake, not re-checked per request).
- **Cut off a device that already registered:** open the Supabase dashboard → Table Editor →
  `devices`, find its row (by `created_at`/`last_seen_at`), and delete it. The cached token
  stops authenticating on its next request — immediate, no redeploy needed.

## What this does and doesn't protect against

- **Does:** keep the real OpenAI key off every user's machine (it only ever exists as a
  Supabase secret); cap per-device and global usage so a leaked token can't run up an unbounded
  bill; restrict which models and payload sizes reach OpenAI.
- **Does not:** distinguish individual humans — a device token is exactly as sensitive as a
  login session for this proxy. If the app is ever handed to genuinely untrusted parties at
  scale, revisit the token model.

> **Note:** there used to be a second `auth-helper` function backing a "password hint" feature.
> That feature was removed (it disclosed a stored hint to any unauthenticated caller who knew an
> email address). The function slug has been decommissioned; password recovery is the standard
> Supabase reset-email flow only.
