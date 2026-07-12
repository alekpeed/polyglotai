-- Multi-user learner accounts. Cloud-syncs everything a person's account owns (profile, SRS
-- review state, conversation history, pronunciation attempts, feature flags) so it survives
-- clearing browser storage and follows them across devices/browsers, scoped strictly per
-- Supabase Auth user via row-level security.
--
-- Deliberately NOT included: pack content (vocabulary/grammar/dialogues/etc.) — that's static,
-- identical for every learner, and stays bundled client-side (packages/language-pack-sdk) the
-- same way it already works today. Only a content_id reference (packId::itemKey, computed
-- client-side) is stored here, matching the local SQLite build's existing scheme — porting the
-- content tables too would just be duplicating a static asset per account for no benefit.

create table learner_profiles (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null unique references auth.users(id) on delete cascade,
  display_name text not null,
  active_pack_id text,
  goal text,
  target_dialect text,
  real_speech_level text not null default 'informal',
  slang_severity_override smallint,
  cefr_estimate text,
  correction_strictness text not null default 'balanced',
  settings jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table review_items (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  item_type text not null,
  content_id text not null,
  difficulty real,
  stability real,
  retrievability real,
  state text not null default 'new',
  due_at timestamptz,
  last_reviewed_at timestamptz,
  lapses integer not null default 0,
  reps integer not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (user_id, item_type, content_id)
);
create index idx_review_items_due on review_items (user_id, due_at);

create table review_results (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  review_item_id uuid not null references review_items(id) on delete cascade,
  rating smallint not null,
  response_ms integer,
  confidence smallint,
  reviewed_at timestamptz not null default now(),
  created_at timestamptz not null default now()
);
create index idx_review_results_item on review_results (review_item_id);

create table conversations (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  mode text not null,
  scenario text,
  title text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table ai_messages (
  id uuid primary key default gen_random_uuid(),
  conversation_id uuid not null references conversations(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  role text not null,
  content text not null,
  correction_json jsonb,
  tokens integer,
  created_at timestamptz not null default now()
);
create index idx_ai_messages_conversation on ai_messages (conversation_id);

create table pronunciation_attempts (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  target_text text not null,
  transcript text,
  score real,
  created_at timestamptz not null default now()
);
create index idx_pronunciation_attempts_user on pronunciation_attempts (user_id, created_at desc);

-- Per-user, unlike the app's original global feature_flags table (which stays as-is for the
-- local-only build) — each account gets its own toggles, e.g. conversation_logging opt-in.
create table user_feature_flags (
  user_id uuid not null references auth.users(id) on delete cascade,
  key text not null,
  enabled boolean not null default false,
  updated_at timestamptz not null default now(),
  primary key (user_id, key)
);

alter table learner_profiles enable row level security;
alter table review_items enable row level security;
alter table review_results enable row level security;
alter table conversations enable row level security;
alter table ai_messages enable row level security;
alter table pronunciation_attempts enable row level security;
alter table user_feature_flags enable row level security;

-- Every policy is the same shape: a row is only ever visible to, or writable by, its owner.
create policy "own profile" on learner_profiles for all
  using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "own reviews" on review_items for all
  using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "own review results" on review_results for all
  using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "own conversations" on conversations for all
  using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "own messages" on ai_messages for all
  using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "own pronunciation" on pronunciation_attempts for all
  using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "own flags" on user_feature_flags for all
  using (auth.uid() = user_id) with check (auth.uid() = user_id);
