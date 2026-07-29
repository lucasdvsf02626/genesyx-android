-- ============================================================================
-- PROPOSED — NOT YET APPLIED TO ANY SUPABASE ENVIRONMENT
--
-- Custom supplement entries ("Add your own supplement": name, dose, time).
-- Shared schema: Android and iOS both read/write this table, so the column
-- names below are a cross-platform contract — changing one after either client
-- ships is a breaking change.
--
-- Shape follows public.ph_readings deliberately: uuid PK minted client-side,
-- updated_at for last-write-wins merge, deleted_at tombstone so a delete can
-- sync rather than silently reappearing on the next pull. Same four RLS
-- policies, same grants.
--
-- Apply order: staging -> verify over the anon/authenticated REST path ->
-- production. Do not deploy a client that writes this table before the table
-- exists in that client's environment.
-- ============================================================================

CREATE TABLE public.user_supplements (
  id           uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id      uuid        NOT NULL,
  name         text        NOT NULL,     -- user-entered display name, trimmed client-side, 1..60 chars
  dose         text,                     -- free text ("400 mcg", "2 capsules"); nullable, never parsed
  time_of_day  text        CONSTRAINT user_supplements_time_of_day_check
                           CHECK (time_of_day IN ('morning', 'afternoon', 'evening', 'anytime')),
  created_at   timestamptz NOT NULL DEFAULT now(),
  updated_at   timestamptz NOT NULL DEFAULT now(),
  deleted_at   timestamptz               -- soft-delete tombstone: null = live, set = deleted
);

CREATE INDEX user_supplements_user_idx ON public.user_supplements (user_id, created_at DESC);

GRANT SELECT, INSERT, UPDATE, DELETE ON public.user_supplements TO authenticated;
GRANT ALL ON public.user_supplements TO service_role;
ALTER TABLE public.user_supplements ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users select own supplements" ON public.user_supplements
  FOR SELECT TO authenticated USING (auth.uid() = user_id);
CREATE POLICY "Users insert own supplements" ON public.user_supplements
  FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users update own supplements" ON public.user_supplements
  FOR UPDATE TO authenticated USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users delete own supplements" ON public.user_supplements
  FOR DELETE TO authenticated USING (auth.uid() = user_id);

-- ----------------------------------------------------------------------------
-- MANDATORY, not optional: this DB has no FK cascades (see schema.sql line 2),
-- so account deletion is the explicit list inside delete_current_user(). Ship
-- the table without this and every deleted account leaves orphaned supplement
-- rows behind — a GDPR erasure failure and a Play Data Safety contradiction.
--
-- Replace the existing function body, adding the one marked line.
-- ----------------------------------------------------------------------------
create or replace function public.delete_current_user()
returns void
language plpgsql
security definer
as $$
declare
  uid uuid := auth.uid();
begin
  if uid is null then
    raise exception 'no authenticated user';
  end if;

  delete from public.user_supplements where user_id = uid;   -- <-- added 2026-07-29
  delete from public.ph_readings       where user_id = uid;
  delete from public.daily_logs        where user_id = uid;
  delete from public.cycle_settings    where user_id = uid;
  delete from public.profiles          where id      = uid;

  delete from auth.users where id = uid;
end;
$$;

revoke execute on function public.delete_current_user() from public, anon;
grant execute on function public.delete_current_user() to authenticated;

-- ----------------------------------------------------------------------------
-- Post-apply verification (run as an authenticated user, then as a second user):
--   1. INSERT a row; confirm it returns and carries your user_id.
--   2. SELECT as the other user; confirm zero rows (RLS isolation).
--   3. UPDATE the other user's row by id; confirm zero rows affected.
--   4. Call delete_current_user(); confirm 0 rows remain for that user_id
--      (this is the S6 re-check, which must now cover supplements as well as pH).
-- ----------------------------------------------------------------------------
