# Supabase prompt — verify the supplements backend + close the deletion gates

> **✅ RESOLVED 19 Aug 2026 — all checks run, gate closed.**
> - **A:** all pass. One expectation in A2 was wrong, not production: the applied migration
>   deliberately created **one `FOR ALL` owner policy** (`user_supplements_owner`, USING +
>   WITH CHECK on `auth.uid() = user_id`) matching the project convention, not the obsolete
>   draft's four-policy split. Same effect; REST behaviour tests confirm it.
> - **B:** full deletion re-proof done — client-side steps 1–4 by agent (RPC 204, JWT
>   `user_not_found`), service-role counts by owner in the dashboard: **six zeros**.
>   Recorded in the locally-excluded `DATA_SAFETY_AND_PRIVACY_v1.1.md`.
> - **C:** recovery unnecessary — the premise was stale. The 13 Aug migration IS versioned:
>   `supabase/migrations/20260813_user_supplements_delete_backstop_and_push_default_false.sql`
>   in the iOS repo, whose local `main` is **ahead of origin** — the remaining H1 action is
>   simply `git push` there. Production matches it (spliced backstop line present once,
>   `search_path=''`, push_enabled default false).
> - **DPA:** Supabase's DPA is now incorporated into their Terms of Service — no signature
>   flow exists; the gate is satisfied by default. Owner to download the DPA + TIA PDFs.
> - Bonus: production `delete_current_user()` already pins `SET search_path TO ''` (since the
>   13 Aug hardening) — the old "pin search_path" TODO is obsolete.

**⛔ Do NOT run `docs/migrations/2026-07-29_user_supplements.sql`.** It is superseded; its own
banner says so. The real migration (`20260813_android_supplements_backend.sql`) was applied to the
live project `epltxklawpcxxbaleswg` on 13 Aug 2026. Re-applying the old draft would try to recreate
existing objects and would revert the 13 Aug TRUNCATE-privilege fix.

What's actually needed in Supabase is threefold: **A)** verify the 13 Aug state is what the docs
claim, **B)** re-prove account deletion end-to-end now that pH + supplements sync (the old S6 proof
predates both), **C)** recover the un-versioned backstop SQL from production (open half of H1).

Paste the prompt below into your AI agent, or run the SQL/curl yourself.

---

You are verifying the Genesyx Supabase backend (project `epltxklawpcxxbaleswg`). Do not create,
alter, or drop anything. Run checks A and B, report results as a table, and produce the file
described in C. If ANY check fails, stop and report — do not fix.

## A. Schema state (SQL editor, read-only)

```sql
-- A1. Both tables exist with RLS enabled
SELECT relname, relrowsecurity
FROM pg_class
WHERE relname IN ('user_supplements', 'genesyx_products');
-- Expect: 2 rows, relrowsecurity = true on both.

-- A2. Policies: 4 owner-only on user_supplements, exactly 1 read-only on genesyx_products
SELECT tablename, policyname, cmd
FROM pg_policies
WHERE tablename IN ('user_supplements', 'genesyx_products')
ORDER BY tablename, cmd;
-- Expect: genesyx_products SELECT only; user_supplements SELECT/INSERT/UPDATE/DELETE,
-- all scoped to auth.uid() = user_id.

-- A3. Grants — the 13 Aug fix: authenticated must NOT hold TRUNCATE
SELECT table_name, grantee, privilege_type
FROM information_schema.table_privileges
WHERE table_name IN ('user_supplements', 'genesyx_products')
  AND grantee = 'authenticated'
ORDER BY table_name, privilege_type;
-- Expect: user_supplements → SELECT, INSERT, UPDATE, DELETE only (no TRUNCATE).
--         genesyx_products → SELECT only.

-- A4. FK shapes
SELECT conname, confdeltype
FROM pg_constraint
WHERE conrelid = 'public.user_supplements'::regclass AND contype = 'f';
-- Expect: user_id → auth.users confdeltype 'c' (CASCADE);
--         product_id → genesyx_products confdeltype 'n' (SET NULL).

-- A5. delete_current_user() covers supplements
SELECT pg_get_functiondef(oid)
FROM pg_proc
WHERE proname = 'delete_current_user';
-- Expect: SECURITY DEFINER; body deletes from user_supplements, ph_readings, daily_logs,
-- cycle_settings, profiles, then auth.users. It must NOT touch genesyx_products.
```

## B. REST + deletion proof (anon key, then a throwaway account)

```bash
# B1. Tables are visible over REST (no more PGRST205)
curl -s "https://epltxklawpcxxbaleswg.supabase.co/rest/v1/genesyx_products?select=id&limit=1" \
  -H "apikey: $ANON_KEY" -H "Authorization: Bearer $ANON_KEY"
# Expect: [] or rows — NOT a PGRST205 "table not found" error.

# B2. Anon cannot read user_supplements
curl -s "https://epltxklawpcxxbaleswg.supabase.co/rest/v1/user_supplements?select=id&limit=1" \
  -H "apikey: $ANON_KEY" -H "Authorization: Bearer $ANON_KEY"
# Expect: [] (RLS filters everything) or a permission error — never data.
```

Then the live deletion re-proof (this is the S6 re-check the release is gated on):

1. Sign up a **throwaway account** in the app. Log one daily log, one pH reading, and add one
   custom supplement (that covers all three synced user tables).
2. As that user (its JWT), confirm each row exists over REST.
3. As a **second** user, confirm SELECT on the first user's rows returns zero (RLS isolation), and
   an authenticated INSERT/UPDATE/DELETE on `genesyx_products` all fail while SELECT succeeds.
4. In the app: Profile → delete account (calls `delete_current_user()`).
5. As service_role, count rows for that user_id in `user_supplements`, `ph_readings`,
   `daily_logs`, `cycle_settings`, `profiles`, and `auth.users`: **all must be 0**, and
   `genesyx_products` must still have all its rows.

   > Steps 1–4 were run live on **19 Aug 2026** (throwaway user
   > `1605cedb-314a-4479-9730-e4b82a22b52a`; RPC returned 204, JWT now `user_not_found`) —
   > see `DATA_SAFETY_AND_PRIVACY_v1.1.md`. Only this service-role count remains; paste into
   > the dashboard SQL editor:
   >
   > ```sql
   > SELECT 'user_supplements' t, count(*) FROM public.user_supplements WHERE user_id = '1605cedb-314a-4479-9730-e4b82a22b52a'
   > UNION ALL SELECT 'ph_readings', count(*) FROM public.ph_readings WHERE user_id = '1605cedb-314a-4479-9730-e4b82a22b52a'
   > UNION ALL SELECT 'daily_logs', count(*) FROM public.daily_logs WHERE user_id = '1605cedb-314a-4479-9730-e4b82a22b52a'
   > UNION ALL SELECT 'cycle_settings', count(*) FROM public.cycle_settings WHERE user_id = '1605cedb-314a-4479-9730-e4b82a22b52a'
   > UNION ALL SELECT 'profiles', count(*) FROM public.profiles WHERE id = '1605cedb-314a-4479-9730-e4b82a22b52a'
   > UNION ALL SELECT 'auth.users', count(*) FROM auth.users WHERE id = '1605cedb-314a-4479-9730-e4b82a22b52a';
   > -- Expect: six rows, all 0.
   > ```
6. Record the result with date + counts in `docs/DATA_SAFETY_AND_PRIVACY_v1.1.md` (or the runbook)
   — this proof is what the Play Data Safety form relies on.

## C. Recover the un-versioned backstop SQL (open half of H1)

The 13 Aug `delete_current_user()` backstop + `profiles.push_enabled` default-false change was
applied to production but is checked into NO repo. Production is the only copy. Recover it:

```sql
-- C1. The function, verbatim as it runs
SELECT pg_get_functiondef(oid) FROM pg_proc WHERE proname = 'delete_current_user';

-- C2. The column default
SELECT column_name, column_default
FROM information_schema.columns
WHERE table_name = 'profiles' AND column_name = 'push_enabled';
-- Expect default: false
```

Save both outputs verbatim as
`supabase/migrations/20260813_user_supplements_delete_backstop_and_push_default_false.sql` in the
**shared-backend (iOS) repo** `lucasdvsf02626/genesyx_apple` — NOT in the Android repo (Android
must not grow a `supabase/migrations/` directory; its SQL records live under `docs/migrations/`).
Mark the file "recovered from production 19 Aug 2026, do not re-run" and remove/replace the ⛔
banner situation described in the Android draft's header.

## Out of scope — do not do

- Do not apply `docs/migrations/2026-07-29_user_supplements.sql` (superseded, harmful).
- Do not seed `genesyx_products` — zero SKUs is the intended state; the app renders "coming soon".
- Do not pin `set search_path` on `delete_current_user()` yet — that is a recorded
  TODO(post-launch), and changing the function now would invalidate the deletion proof you just ran.
- The Supabase **DPA check** is a dashboard/legal task (Settings → Legal), not SQL — do it in the
  same sitting, but it needs the account owner.
```
