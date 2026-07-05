# Manual Test Checklist — v1.1 (pH sync + Google sign-in)

On-device verification for the `feature/v1.1-sync-hardening` branch. **Non-Google tests come first** so
you can validate the whole app even if the Google config has an issue. Google scenarios are last.

## Setup
- Build/install debug: `./gradlew :app:installDebug` (debug SHA-1 is registered, so Google works on
  debug too). For a true "fresh install", `adb uninstall com.genesyx.app` first.
- Have a throwaway email/password account (or create one in-app).
- **Supabase dashboard → SQL editor** open for verification. Get your test user's id once:
  ```sql
  select id, email from auth.users where email = '<your test email>';
  ```
  Then reuse `<uid>` below. pH check query:
  ```sql
  select id, ph_value, recorded_at, updated_at, deleted_at
  from public.ph_readings where user_id = '<uid>' order by recorded_at desc;
  ```

---

## A. pH log online → row appears in Supabase
1. Sign in (email/password). Go to the pH tracker; log a reading, e.g. **6.5**.
2. It shows immediately in the app (Track/Insights).
- **Expect (Supabase):** one `ph_readings` row for `<uid>`, `ph_value = 6.5`, `deleted_at` null,
  `updated_at` set. **No `E Ph` / crash** in `adb logcat`.

## B. Offline log must QUEUE, never block
1. Enable **airplane mode**.
2. Log a reading, e.g. **7.2**.
- **Expect (app):** the reading saves and is visible **immediately** — no "you're offline" block, no
  spinner hang. (This is the key change vs the daily-log flow, which still blocks offline.)
- **Expect (Supabase):** row **not** there yet.
3. Turn airplane mode **off**. Wait ~30–60s (WorkManager backoff) or background/foreground the app.
- **Expect (Supabase):** the 7.2 row now appears (`deleted_at` null). No duplicate of it.

## C. Edit a reading → syncs
1. Online, edit an existing reading's value (e.g. 6.5 → 6.8).
- **Expect (Supabase):** same row `id`, `ph_value = 6.8`, `updated_at` advanced. **No new row.**

## D. Delete a reading → soft-delete syncs
1. Delete a reading in the app.
- **Expect (app):** it disappears from the list.
- **Expect (Supabase):** the row still exists but `deleted_at` is now **set** (tombstone), not a hard
  delete.

## E. Sign out / sign in → pull-merge, no duplicates
1. Sign out, then sign back in with the same account.
- **Expect (app):** your non-deleted readings reappear (pulled from server); deleted ones stay gone.
- **Expect:** counts match Supabase (non-deleted rows). **No duplicated readings.**

## F. Account deletion → data physically gone, re-login fails
1. Profile → Delete account → confirm.
- **Expect (app):** signed out → Splash, no crash.
2. Re-login with the same credentials.
- **Expect (app):** "Invalid login credentials", stays on Auth.
- **Expect (Supabase):** `<uid>` gone from `auth.users`, and **zero** rows in `ph_readings`,
  `profiles`, `daily_logs`, `cycle_settings` for `<uid>` (hard delete — GDPR erasure):
  ```sql
  select count(*) from public.ph_readings   where user_id = '<uid>';  -- expect 0
  select count(*) from auth.users           where id      = '<uid>';  -- expect 0
  ```

## G. (Regression) Migration preserves existing pH data
- If you have a device already on the **old** build with a pH reading, install this build **over it**
  (no uninstall). **Expect:** the old reading is still there (migration 2→3 did not wipe it).

---

## Google sign-in (LAST — needs the Google config to be healthy)
> Debug builds work with the registered **debug** SHA-1. **Release builds will fail** until the
> release keystore is created and its SHA-1 registered (see FINAL_REPORT.md). If any step here fails,
> everything above still stands.

## H. Fresh-install Google sign-in
1. `adb uninstall com.genesyx.app`, reinstall, open, go to Auth.
2. Tap **Continue with Google**, pick an account.
- **Expect:** account sheet appears → lands on **Home** signed in. A `profiles` row exists for the new
  uid; no crash.

## I. Returning-user Google sign-in
1. Sign out, tap **Continue with Google**, pick the same account.
- **Expect:** back to Home, same account (no duplicate profile).

## J. Cancelled dialogue
1. Tap **Continue with Google**, then **dismiss** the sheet (back/tap-away).
- **Expect:** returns to the Auth screen quietly — **no error banner**, no crash, button re-enabled.

## K. Airplane-mode failure is friendly
1. Enable airplane mode, tap **Continue with Google**.
- **Expect:** a friendly inline error ("Couldn't reach Google. Check your connection and try again.")
  — **not a crash**, not a fake success. Button re-enabled.

## L. (Only if release config is set up) Not-configured guard
- If a build ships without `GOOGLE_WEB_CLIENT_ID`, the button shows **"Google sign-in isn't
  configured."** and does nothing else. (With config present, you won't see this.)

---

### Log capture
Run `adb logcat -s Ph DailyLog Auth AndroidRuntime` during A–F; **zero `AndroidRuntime` (FATAL)**
lines is part of pass. Note any `Ph` warnings ("queued for retry" is expected offline).
