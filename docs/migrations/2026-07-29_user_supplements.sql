-- ============================================================================
-- ⛔ SUPERSEDED — DO NOT APPLY. KEPT FOR AUDIT HISTORY ONLY.
--
-- This draft was never applied anywhere. Production got a different file, and this one is now
-- historical: it records what was proposed on 29 Jul 2026, not what runs.
--
-- WHAT ACTUALLY SHIPPED, on live project `epltxklawpcxxbaleswg`, 13 Aug 2026:
--   * `20260813_android_supplements_backend.sql` (migration B) created `user_supplements` and
--     `genesyx_products` with owner-only RLS, `user_id -> auth.users(id) ON DELETE CASCADE` and
--     `product_id -> genesyx_products(id) ON DELETE SET NULL`.
--   * `20260813_user_supplements_delete_backstop_and_push_default_false.sql` then added the
--     explicit deletion backstop to `delete_current_user()` and set the `profiles.push_enabled`
--     default to `false`.
--
-- The canonical, applied SQL lives in the iOS / shared-backend repository
-- `lucasdvsf02626/genesyx_apple`, under:
--     supabase/migrations/20260813_android_supplements_backend.sql                        (migration B)
-- (local checkout on the author's machine: /Users/lucasvalenca_sf/genesxy_apple.V1.02/)
--
-- ⚠️ The SECOND file above — the `delete_current_user()` backstop plus the `push_enabled` default —
-- was applied to production but is NOT checked into that repo yet, under any name. Verified
-- 13 Aug 2026. Do not go looking for it, and do not reconstruct it from the summary above: the
-- applied text has to be recovered verbatim from the session that ran it. Until that happens
-- production is the only copy, and
-- `supabase/migrations/20260813_delete_current_user_hardening.sql` over there carries a ⛔ banner,
-- because re-running *it* would silently revert the backstop. This is the open half of H1.
-- Android has no `supabase/migrations/` directory and must not grow one — SQL records belong
-- here, under `docs/migrations/`, and the backend is applied from the shared-backend repo.
--
-- Applying this file now would be actively harmful: it would attempt to recreate objects that
-- already exist, and its RLS/grant shape predates the 13 Aug TRUNCATE-privilege fix (the schema
-- default privileges on this project hand `authenticated` the full `arwdDxtm` at CREATE TABLE
-- time, and TRUNCATE is not subject to RLS).
--
-- ---------------------------------------------------------------------------
-- ORIGINAL HEADER, PRESERVED VERBATIM BELOW THIS LINE
-- ---------------------------------------------------------------------------
--
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

-- ============================================================================
-- 1. genesyx_products — the Genesyx range, as DATA not code.
--
-- Zero SKUs exist yet (Shopify store confirmed empty, all statuses); the line
-- is not finalised. The app therefore ships reading this table and rendering a
-- "coming soon" empty state when it returns nothing. Seeding real rows later
-- needs NO app change on either platform.
--
-- NOT user-owned: this is a shared catalogue. RLS is on with a read-only policy
-- for signed-in users, and no INSERT/UPDATE/DELETE grants to clients at all —
-- only service_role (i.e. the dashboard or a seed script) can write it. It must
-- NOT appear in delete_current_user(): deleting an account must never delete
-- the product catalogue.
-- ============================================================================

CREATE TABLE public.genesyx_products (
  id          uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
  name        text        NOT NULL,
  dose        text,                                  -- label as printed on the product
  sort_order  integer     NOT NULL DEFAULT 0,        -- display order; ties break on name
  active      boolean     NOT NULL DEFAULT true,     -- false = retired, keep the row for history
  created_at  timestamptz NOT NULL DEFAULT now(),
  updated_at  timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX genesyx_products_active_idx ON public.genesyx_products (active, sort_order);

GRANT SELECT ON public.genesyx_products TO authenticated;
GRANT ALL    ON public.genesyx_products TO service_role;
ALTER TABLE public.genesyx_products ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Signed-in users read the catalogue" ON public.genesyx_products
  FOR SELECT TO authenticated USING (true);
-- Deliberately no write policies: clients cannot create or edit products.

-- ============================================================================
-- 2. user_supplements — the user's own entries
-- ============================================================================

CREATE TABLE public.user_supplements (
  id           uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id      uuid        NOT NULL,
  name         text        NOT NULL,     -- user-entered display name, trimmed client-side, 1..60 chars
  dose         text,                     -- free text ("400 mcg", "2 capsules"); nullable, never parsed
  time_of_day  text        CONSTRAINT user_supplements_time_of_day_check
                           CHECK (time_of_day IN ('morning', 'afternoon', 'evening', 'anytime')),
  product_id   uuid        REFERENCES public.genesyx_products (id) ON DELETE SET NULL,
                           -- set when the entry came from the Genesyx catalogue, null for a
                           -- user's own. SET NULL, not CASCADE: retiring a product must never
                           -- delete the user's adherence history.
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
--   4. Call delete_current_user(); confirm 0 rows remain in user_supplements for
--      that user_id (this is the S6 re-check, which must now cover supplements as
--      well as pH), AND that genesyx_products still has all its rows — deleting an
--      account must never touch the catalogue.
--   5. As an authenticated user, attempt INSERT/UPDATE/DELETE on genesyx_products;
--      all three must fail. SELECT must succeed.
-- ----------------------------------------------------------------------------

-- ============================================================================
-- ROLLBACK
--
-- Safe while no client build that writes these tables has shipped. Once one has,
-- rolling back destroys user-entered supplement data — export user_supplements
-- first, and prefer leaving the tables in place (an unused table costs nothing)
-- over dropping them.
--
-- Run in this order; the FK means user_supplements must go first.
-- ============================================================================
--
-- DROP TABLE IF EXISTS public.user_supplements;
-- DROP TABLE IF EXISTS public.genesyx_products;
--
-- -- Restore delete_current_user() to its pre-migration body (drop the
-- -- user_supplements line). Leaving the line in place against a dropped table
-- -- would make the function raise, breaking account deletion entirely — so this
-- -- half of the rollback is mandatory, not optional.
-- create or replace function public.delete_current_user()
-- returns void language plpgsql security definer as $$
-- declare uid uuid := auth.uid();
-- begin
--   if uid is null then raise exception 'no authenticated user'; end if;
--   delete from public.ph_readings    where user_id = uid;
--   delete from public.daily_logs     where user_id = uid;
--   delete from public.cycle_settings where user_id = uid;
--   delete from public.profiles       where id      = uid;
--   delete from auth.users where id = uid;
-- end; $$;
-- revoke execute on function public.delete_current_user() from public, anon;
-- grant execute on function public.delete_current_user() to authenticated;
