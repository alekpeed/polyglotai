-- Backend AI proxy (see supabase/functions/openai-proxy) — lets the desktop app use AI
-- features without every install needing its own OpenAI key. Each app install registers once
-- and gets an opaque device token; only its SHA-256 hash is ever stored, so a database leak
-- doesn't hand out usable tokens. usage_log is informational only (per the owner's choice, no
-- per-device cap is enforced yet) — it exists so usage is visible and a cap can be added later
-- without a schema change.

create table if not exists devices (
  id uuid primary key default gen_random_uuid(),
  token_hash text not null unique,
  created_at timestamptz not null default now(),
  last_seen_at timestamptz not null default now(),
  label text
);

create table if not exists usage_log (
  id bigint generated always as identity primary key,
  device_id uuid not null references devices(id) on delete cascade,
  endpoint text not null,
  model text,
  tokens_used integer,
  created_at timestamptz not null default now()
);

create index if not exists usage_log_device_idx on usage_log (device_id, created_at);

-- Edge Functions call this table only via the service-role key (bypasses RLS), so no
-- application role should ever read/write it directly. Locking it down with RLS + no policies
-- means even a leaked anon/public key can't touch device tokens or usage history.
alter table devices enable row level security;
alter table usage_log enable row level security;
