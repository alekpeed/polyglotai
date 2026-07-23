-- Cloud sync target for the native Android app. The native app uses SM-2 scheduling (easiness /
-- interval_days), which does not fit the FSRS-shaped public.review_items table, and it has no
-- per-language profile rows — so it gets its own table keyed directly by (user_id, content_id).
-- content_id is "packId::itemKey", matching the on-device Room primary key.
create table if not exists public.native_review_state (
  user_id uuid not null references auth.users(id) on delete cascade,
  content_id text not null,
  pack_id text not null,
  item_type text not null,
  front text not null,
  back text not null,
  reading text,
  easiness double precision not null,
  interval_days integer not null,
  reps integer not null,
  lapses integer not null,
  due_at_millis bigint not null,
  last_reviewed_at_millis bigint,
  updated_at timestamptz not null default now(),
  primary key (user_id, content_id)
);

create index if not exists native_review_state_user_pack_idx
  on public.native_review_state (user_id, pack_id);

alter table public.native_review_state enable row level security;

-- One account only ever touches its own rows. auth.uid() comes from the signed-in user's JWT.
create policy native_review_state_select on public.native_review_state
  for select using (auth.uid() = user_id);
create policy native_review_state_insert on public.native_review_state
  for insert with check (auth.uid() = user_id);
create policy native_review_state_update on public.native_review_state
  for update using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy native_review_state_delete on public.native_review_state
  for delete using (auth.uid() = user_id);
