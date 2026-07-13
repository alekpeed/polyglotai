-- Repair multi-profile ownership, make review advancement atomic, and add proxy quotas.

-- The original cloud schema made content unique per user. That breaks as soon as the user has
-- more than one profile, because one content card needs one independent SRS state per profile.
update review_items ri
set profile_id = coalesce(
  (
    select lp.id
    from learner_profiles lp
    where lp.user_id = ri.user_id
      and lp.active_pack_id = split_part(ri.content_id, '::', 1)
    limit 1
  ),
  (
    select lp.id
    from learner_profiles lp
    where lp.user_id = ri.user_id
    order by lp.created_at asc
    limit 1
  )
)
where ri.profile_id is null;

update conversations c
set profile_id = (
  select lp.id from learner_profiles lp where lp.user_id = c.user_id order by lp.created_at asc limit 1
)
where c.profile_id is null;

update pronunciation_attempts pa
set profile_id = (
  select lp.id from learner_profiles lp where lp.user_id = pa.user_id order by lp.created_at asc limit 1
)
where pa.profile_id is null;

alter table review_items alter column profile_id set not null;
alter table conversations alter column profile_id set not null;
alter table pronunciation_attempts alter column profile_id set not null;

alter table review_items drop constraint if exists review_items_user_id_item_type_content_id_key;
alter table review_items add constraint review_items_profile_id_item_type_content_id_key
  unique (profile_id, item_type, content_id);

-- Child rows must belong to a parent row owned by the caller, not merely carry the caller's id.
drop policy if exists "own review results" on review_results;
create policy "own review results" on review_results for all
  using (
    auth.uid() = user_id
    and exists (select 1 from review_items ri where ri.id = review_item_id and ri.user_id = auth.uid())
  )
  with check (
    auth.uid() = user_id
    and exists (select 1 from review_items ri where ri.id = review_item_id and ri.user_id = auth.uid())
  );

drop policy if exists "own messages" on ai_messages;
create policy "own messages" on ai_messages for all
  using (
    auth.uid() = user_id
    and exists (select 1 from conversations c where c.id = conversation_id and c.user_id = auth.uid())
  )
  with check (
    auth.uid() = user_id
    and exists (select 1 from conversations c where c.id = conversation_id and c.user_id = auth.uid())
  );

-- A review schedule update and its history event must succeed or fail together.
create or replace function record_review_and_result(
  p_review_item_id uuid,
  p_difficulty real,
  p_stability real,
  p_retrievability real,
  p_state text,
  p_due_at timestamptz,
  p_last_reviewed_at timestamptz,
  p_lapses integer,
  p_reps integer,
  p_rating smallint,
  p_response_ms integer,
  p_confidence smallint,
  p_reviewed_at timestamptz
) returns void
language plpgsql
security invoker
set search_path = public
as $$
begin
  update review_items
  set difficulty = p_difficulty,
      stability = p_stability,
      retrievability = p_retrievability,
      state = p_state,
      due_at = p_due_at,
      last_reviewed_at = p_last_reviewed_at,
      lapses = p_lapses,
      reps = p_reps,
      updated_at = p_reviewed_at
  where id = p_review_item_id and user_id = auth.uid();

  if not found then
    raise exception 'review item not found';
  end if;

  insert into review_results (user_id, review_item_id, rating, response_ms, confidence, reviewed_at)
  values (auth.uid(), p_review_item_id, p_rating, p_response_ms, p_confidence, p_reviewed_at);
end;
$$;
grant execute on function record_review_and_result(uuid, real, real, real, text, timestamptz, timestamptz, integer, integer, smallint, integer, smallint, timestamptz) to authenticated;
revoke execute on function record_review_and_result(uuid, real, real, real, text, timestamptz, timestamptz, integer, integer, smallint, integer, smallint, timestamptz) from public;

-- Server-side proxy quotas. The service-role-backed functions below serialize quota updates so
-- concurrent requests cannot race through a check-then-increment gap.
alter table devices add column if not exists request_window_started_at timestamptz;
alter table devices add column if not exists request_window_count integer not null default 0;
alter table devices add column if not exists daily_request_date date;
alter table devices add column if not exists daily_request_count integer not null default 0;

create table if not exists proxy_registration_window (
  singleton boolean primary key default true check (singleton),
  started_at timestamptz not null default now(),
  count integer not null default 0
);
insert into proxy_registration_window (singleton) values (true) on conflict do nothing;

create or replace function register_device_with_rate_limit(
  p_token_hash text,
  p_max_registrations integer,
  p_window_seconds integer
) returns boolean
language plpgsql
security definer
set search_path = public
as $$
declare
  w proxy_registration_window%rowtype;
  now_ts timestamptz := clock_timestamp();
begin
  select * into w from proxy_registration_window where singleton = true for update;
  if w.started_at <= now_ts - make_interval(secs => p_window_seconds) then
    update proxy_registration_window set started_at = now_ts, count = 1 where singleton = true;
  elsif w.count >= p_max_registrations then
    return false;
  else
    update proxy_registration_window set count = count + 1 where singleton = true;
  end if;
  insert into devices (token_hash, last_seen_at) values (p_token_hash, now_ts);
  return true;
end;
$$;

create or replace function consume_device_proxy_quota(
  p_device_id uuid,
  p_max_requests_per_window integer,
  p_window_seconds integer,
  p_max_requests_per_day integer
) returns boolean
language plpgsql
security definer
set search_path = public
as $$
declare
  d devices%rowtype;
  now_ts timestamptz := clock_timestamp();
  next_window_count integer;
  next_daily_count integer;
  next_window_start timestamptz;
begin
  select * into d from devices where id = p_device_id for update;
  if not found then return false; end if;

  next_window_start := case
    when d.request_window_started_at is null or d.request_window_started_at <= now_ts - make_interval(secs => p_window_seconds)
      then now_ts else d.request_window_started_at end;
  next_window_count := case when next_window_start = now_ts then 1 else d.request_window_count + 1 end;
  next_daily_count := case when d.daily_request_date is null or d.daily_request_date <> current_date then 1 else d.daily_request_count + 1 end;

  if next_window_count > p_max_requests_per_window or next_daily_count > p_max_requests_per_day then
    return false;
  end if;

  update devices
  set request_window_started_at = next_window_start,
      request_window_count = next_window_count,
      daily_request_date = current_date,
      daily_request_count = next_daily_count,
      last_seen_at = now_ts
  where id = p_device_id;
  return true;
end;
$$;

revoke execute on function register_device_with_rate_limit(text, integer, integer) from public;
revoke execute on function consume_device_proxy_quota(uuid, integer, integer, integer) from public;
grant execute on function register_device_with_rate_limit(text, integer, integer) to service_role;
grant execute on function consume_device_proxy_quota(uuid, integer, integer, integer) to service_role;
