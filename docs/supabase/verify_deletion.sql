-- ============================================================================
-- Genesyx — verify account deletion still erases everything, now that pH syncs
--
-- WHY THIS EXISTS
--   "Delete my account" must physically remove the user's rows. The app calls the
--   delete_current_user() RPC to do that. The last time we PROVED that against the live
--   database, pH readings had not yet started syncing to the server. pH is now stored
--   server-side, so the proof is out of date: if the DEPLOYED function body is an older
--   version that predates the ph_readings line, a deleted account could leave pH rows behind.
--
--   Google Play's Data Safety form and the privacy policy both assert that deletion erases
--   this data. This script is how we make that assertion true before we submit it.
--
-- HOW TO USE
--   Supabase Dashboard → SQL Editor → paste → run STEP 1 ONLY.
--   Read the results. Do not run STEP 2 or STEP 3 until you have.
--
--   STEP 1 is READ-ONLY. It changes nothing. It is safe to run on production.
--   STEP 2 REPLACES A FUNCTION — only run it if STEP 1 says the deployed body is wrong.
--   STEP 3 DELETES ROWS — only run it if STEP 1 finds orphans, and read the warning first.
-- ============================================================================


-- ============================================================================
-- STEP 1 — READ-ONLY CHECKS.  Run this first. Changes nothing.
-- ============================================================================

-- 1a. What is ACTUALLY deployed right now?
--     Look at the returned source. It MUST contain this line:
--         delete from public.ph_readings    where user_id = uid;
--     The `covers_*` booleans below do that check for you.
select
  p.proname                                            as function_name,
  p.prosecdef                                          as is_security_definer,   -- expect: true
  pg_get_functiondef(p.oid) ilike '%ph_readings%'      as covers_ph_readings,    -- expect: true  ← THE ONE THAT MATTERS
  pg_get_functiondef(p.oid) ilike '%daily_logs%'       as covers_daily_logs,     -- expect: true
  pg_get_functiondef(p.oid) ilike '%cycle_settings%'   as covers_cycle_settings, -- expect: true
  pg_get_functiondef(p.oid) ilike '%profiles%'         as covers_profiles,       -- expect: true
  pg_get_functiondef(p.oid) ilike '%auth.users%'       as covers_auth_user,      -- expect: true
  pg_get_functiondef(p.oid)                            as deployed_source
from pg_proc p
join pg_namespace n on n.oid = p.pronamespace
where n.nspname = 'public'
  and p.proname = 'delete_current_user';
-- If this returns NO ROWS, the function is not deployed at all — go to STEP 2.
-- If covers_ph_readings = false, the deployed body is stale     — go to STEP 2.


-- 1b. Who is allowed to execute it?
--     Expect: authenticated = true, anon = false, public = false.
select
  has_function_privilege('authenticated', 'public.delete_current_user()', 'EXECUTE') as authenticated_can_execute,
  has_function_privilege('anon',          'public.delete_current_user()', 'EXECUTE') as anon_can_execute,
  has_function_privilege('public',        'public.delete_current_user()', 'EXECUTE') as public_can_execute;


-- 1c. ORPHANS: rows whose owner no longer exists in auth.users.
--     Every count MUST be 0. A non-zero ph_readings count is the exact failure this
--     script is looking for: data from an already-deleted account still sitting on the server.
select 'ph_readings'    as table_name, count(*) as orphan_rows
  from public.ph_readings    t left join auth.users u on u.id = t.user_id where u.id is null
union all
select 'daily_logs',     count(*)
  from public.daily_logs     t left join auth.users u on u.id = t.user_id where u.id is null
union all
select 'cycle_settings', count(*)
  from public.cycle_settings t left join auth.users u on u.id = t.user_id where u.id is null
union all
select 'profiles',       count(*)
  from public.profiles       t left join auth.users u on u.id = t.id      where u.id is null;


-- 1d. Is RLS actually on? Expect rls_enabled = true for all four.
select relname as table_name, relrowsecurity as rls_enabled
from pg_class
where relnamespace = 'public'::regnamespace
  and relname in ('ph_readings', 'daily_logs', 'cycle_settings', 'profiles')
order by relname;


-- ============================================================================
-- STEP 2 — FIX.  Run ONLY if STEP 1 showed covers_ph_readings = false (or no rows).
--
--   This REPLACES the deployed function with the canonical body from docs/schema.sql.
--   It does not touch any user data. It is idempotent — safe to run twice.
--   Everything below is commented out on purpose. Uncomment to run.
-- ============================================================================

-- create or replace function public.delete_current_user()
-- returns void
-- language plpgsql
-- security definer
-- as $$
-- declare
--   uid uuid := auth.uid();
-- begin
--   if uid is null then
--     raise exception 'no authenticated user';
--   end if;
--
--   -- explicit deletes of user-owned rows (backstop to the FK cascades)
--   delete from public.ph_readings    where user_id = uid;
--   delete from public.daily_logs     where user_id = uid;
--   delete from public.cycle_settings where user_id = uid;
--   delete from public.profiles       where id      = uid;
--
--   -- finally remove the auth account
--   delete from auth.users where id = uid;
-- end;
-- $$;
--
-- revoke execute on function public.delete_current_user() from public, anon;
-- grant  execute on function public.delete_current_user() to authenticated;

-- After running STEP 2, re-run STEP 1a. covers_ph_readings must now be true.


-- ============================================================================
-- STEP 3 — CLEAN UP EXISTING ORPHANS.  Run ONLY if 1c found orphan rows.
--
--   ⚠️ THIS DELETES DATA AND CANNOT BE UNDONE.
--   It removes rows belonging to accounts that no longer exist. Those users asked to be
--   deleted, so this is finishing a job that was left half-done — but read 1c's output and
--   satisfy yourself the counts are what you expect BEFORE uncommenting.
--   Take a backup first if you are at all unsure.
-- ============================================================================

-- delete from public.ph_readings    t where not exists (select 1 from auth.users u where u.id = t.user_id);
-- delete from public.daily_logs     t where not exists (select 1 from auth.users u where u.id = t.user_id);
-- delete from public.cycle_settings t where not exists (select 1 from auth.users u where u.id = t.user_id);
-- delete from public.profiles       t where not exists (select 1 from auth.users u where u.id = t.id);

-- Then re-run 1c. Every count must be 0.


-- ============================================================================
-- STEP 4 — END-TO-END PROOF (do this in the app, not here)
--
--   The queries above prove the function is right. This proves the whole path is right.
--
--   1. In the app: create a throwaway account (e.g. lucas+gxNN@…).
--   2. Log a daily entry AND a pH reading. Confirm both appear.
--   3. Run this to capture the user id and confirm the rows exist server-side:
--
--        select u.id, u.email,
--               (select count(*) from public.ph_readings    where user_id = u.id) as ph_rows,
--               (select count(*) from public.daily_logs     where user_id = u.id) as log_rows,
--               (select count(*) from public.cycle_settings where user_id = u.id) as cycle_rows,
--               (select count(*) from public.profiles       where id      = u.id) as profile_rows
--        from auth.users u
--        where u.email = 'lucas+gxNN@mysupplementfactory.com';
--
--      Expect: ph_rows >= 1, log_rows >= 1.  (If ph_rows is 0, pH did not reach the server —
--      that is a different bug, and worth knowing about before you file the Data Safety form.)
--
--   4. In the app: Profile → Delete account → confirm.
--
--   5. Re-run the query from (3). Expect ZERO ROWS — the auth user is gone.
--      Then re-run 1c. Expect every orphan count to be 0.
--
--   That is the "S6 = 0 + 0" proof, redone with pH in the picture. Record the date you ran it.
-- ============================================================================
