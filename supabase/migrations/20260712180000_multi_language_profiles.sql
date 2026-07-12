-- Multi-language accounts: one account now carries one learner_profiles row PER language pack
-- (a course-switcher, like Duolingo) instead of exactly one profile total. Drops the
-- one-profile-per-account constraint and gives review/conversation/pronunciation data an
-- explicit profile_id so a Japanese review queue and a Portuguese review queue never blend —
-- mirrors the local SQLite build, which already scoped these tables by profile_id from the
-- start (see 0001_initial.sql). RLS stays user_id-scoped (still correct, just coarser); the app
-- layer filters by profile_id for the actual per-language separation.

alter table learner_profiles drop constraint learner_profiles_user_id_key;
alter table learner_profiles add constraint learner_profiles_user_id_pack_key unique (user_id, active_pack_id);

alter table review_items add column profile_id uuid references learner_profiles(id) on delete cascade;
create index idx_review_items_profile on review_items (profile_id, due_at);

alter table conversations add column profile_id uuid references learner_profiles(id) on delete cascade;
create index idx_conversations_profile on conversations (profile_id, created_at desc);

alter table pronunciation_attempts add column profile_id uuid references learner_profiles(id) on delete cascade;
create index idx_pronunciation_attempts_profile on pronunciation_attempts (profile_id, created_at desc);
