# AI backend proxy

The app never ships an OpenAI key. It registers an opaque, per-install device token with this
Supabase Edge Function, which holds the real key as a server secret.

The proxy now requires a registration passcode and enforces server-side limits by default:

- 10 registrations per hour globally.
- 20 requests per device per minute.
- 200 requests per device per day.
- 32 KiB chat bodies, 8 KiB TTS bodies, and 25 MiB audio uploads.
- Explicit model allow-lists (`gpt-4o-mini`, `gpt-4o-mini-tts`, and `whisper-1` by default).

Adjust any of these with `PROXY_*` Supabase secrets; do not move them into client code.

## Setup

```sh
supabase login
supabase link --project-ref YOUR-PROJECT-REF
supabase db push
supabase secrets set OPENAI_API_KEY=sk-...
supabase secrets set ACCESS_PASSCODE=use-a-strong-secret
supabase functions deploy openai-proxy --no-verify-jwt
```

`ACCESS_PASSCODE` is required. New devices cannot register until it is configured. Existing
device tokens can be revoked by deleting their row from `devices` in the Supabase dashboard.

## Point the app at the proxy

Set this in `apps/desktop-tauri/.env.local`:

```text
VITE_AI_PROXY_URL=https://YOUR-PROJECT-REF.supabase.co/functions/v1/openai-proxy
```

The app will prompt for the registration passcode only when a device has not already received a
token. The token is not an OpenAI credential, but it should still be treated like a session
token.

## Verify

```sh
curl -X POST https://YOUR-PROJECT-REF.supabase.co/functions/v1/openai-proxy/register \
  -H "content-type: application/json" \
  -d '{"passcode":"YOUR-ACCESS-PASSCODE"}'
```

The response should contain a device token. Use it only to test the documented proxy routes.
