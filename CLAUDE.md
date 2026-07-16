# CLAUDE.md — Genesyx Android

Project Name: Genesyx Android

**v1.2 is merged to `main` (PR #10).** Read this first. Honest state, verified against the tree on **2026-07-16**.

> **⚠️ If you read an older copy of this file, three things it told you are now FALSE:**
> 1. **pH is NOT local-only.** pH readings sync to Supabase. Never restore "stored on this device"
>    copy — see "pH sync is live" below. It is the single most dangerous stale claim here.
> 2. **`main` is at versionCode 9, versionName "1.2.0".** Both v1.1 (PR #9) and v1.2 (PR #10) are
>    merged. There is no active feature branch — `feature/v1.2-supplement-card` was merged and deleted.
> 3. **Reminders are LIVE and local-only.** `FeatureFlags.PUSH_NOTIFICATIONS = true` on `main`:
>    WorkManager-scheduled on-device reminders (no FCM/token). See "What landed in v1.2" below.

## 🔖 STOPPED HERE — resume from this (2026-07-16)

**v1.2 is merged to `main` via PR #10** (merge commit `ee739c9`), versionCode 9 / versionName 1.2.0.
The feature branch is deleted. Nothing is uploaded to Play yet.

**Session-by-session history lives in `CHANGELOG.md`.** Read it before anything else.

### What landed in v1.2 (all on `main`)
- **Local reminders** — `FeatureFlags.PUSH_NOTIFICATIONS = true`. On-device only (WorkManager, no
  FCM/token): 6 reminder kinds, 4 channels, self-rescheduling chain, Profile → Reminders screen,
  pre-permission sheet, full `POST_NOTIFICATIONS` handling. Pure/tested `ReminderPolicy` +
  `NotificationPermission` (`notifications/`). `cancelAll()` on sign-out/delete. Verified firing
  on-device. **No new server surface — strictly local.**
- **Home / Track / Insights iOS parity** — Track "Your Trackers" list + six detail screens
  (`ui/track/detail/`); Home hydration-ring + pH-nudge cards deep-linking into Track detail; first-run
  setup card; cycle-hero metrics; gradient avatar; pregnancy entry hidden (route/feature kept). All
  Insights cards are real-data now (sleep, cycle regularity, symptom patterns, ovulation, consistency,
  pH, hydration, nutrition-consistency) plus a Weekly Summary extra.
- **Supplement adherence card**, loggable Zinc, one supplement vocabulary, one week-bucketing impl
  (`domain/time/WeekBuckets`), hydration scored against the user-set goal, intraday hydration coaching.
- **`SeedTestData`** — a manual emulator seeder guarded by `@SeedOnly` (gradle/CI never runs it; run
  by hand via `am instrument`).

**Next actions, in order (release prep — the OWNER gates in "Pre-release checks" still apply):**
1. Re-check Play Data Safety + `genesyx.co.uk/pages/privacy-policy` against what v1.2 stores
   server-side (pH still syncs; reminders add nothing — on-device only).
2. Finish the pH server-side deletion proof — see "Pre-release checks" #3.
3. Build/sign the AAB (versionCode 9), upload to Internal testing, promote.

## Where the code actually is

| | |
|---|---|
| `main` | `ee739c9` (merge of PR #10). versionCode **9**, versionName **"1.2.0"** |
| Working branch | none — v1.2 merged and the branch deleted |
| Unit tests | **233 passing, 0 failures** (`./gradlew :app:testDebugUnitTest`) |
| Instrumented | GREEN (`./gradlew :app:connectedDebugAndroidTest`); the `@SeedOnly` seeder is excluded. Pre-existing `CycleSettingsDialogTest` timing flake still noted below |
| Release build | `./gradlew :app:assembleRelease` GREEN, R8/minify clean |

Unit tests + builds verified 2026-07-16.

### On `main` (shipped or shippable)
- pH tracking + **sync** (see below), theme follows system + Profile toggle (PR #6), the PR #5
  ghost-session sign-in fix.
- `FeatureFlags` on `main`: `PH_TRACKING = true`, `PUSH_NOTIFICATIONS = true` (local reminders, v1.2),
  `ADMIN_CLIENTS = false`, `PARTNER_INVITES = false`.

### Now on `main` (v1.1 via PR #9, v1.2 via PR #10 — all merged)
- **Daily-log offline sync queue (the headline v1.1 item — DONE).** Offline log saves now QUEUE
  instead of being refused. See "The offline queue" below.
- **Log-screen Back no longer discards unsaved edits** — confirm dialog + `BackHandler`.
- **Streak engine v2** + the cross-platform vector contract (below).
- **User-set hydration goal** — persisted in DataStore, read by the engine, Nutrition and Home.
- Learn section (10 articles), auth hardening, Track/calendar fixes, brand/launcher fixes.
- **v1.2 (PR #10):** local reminders (`PUSH_NOTIFICATIONS = true`), Home/Track/Insights iOS parity
  (Track "Your Trackers" + six detail screens, Home hydration-ring + pH-nudge cards, deep links),
  real-data Insights + Weekly Summary, supplement adherence card, intraday hydration coaching. See
  "What landed in v1.2" above. `PARTNER_INVITES` remains gated **off** (still a UI-only stub).
- `docs/APP_INVENTORY.md` — screens, features, journeys, gaps.

## The offline queue (daily logs) — Room schema v4

v1.0 refused to save a log while offline ("You're offline — reconnect to save your log") because an
offline write would be silently overwritten by the server on the next read-through. That gate is
**gone**, replaced by the same shape pH already used:

- `daily_logs.syncStatus` (`LogSyncStatus`, Room **v4**, `MIGRATION_3_4`) — an offline write lands as
  `PENDING_UPSERT`; `DailyLogSyncWorker` drains it with WorkManager backoff.
- **`DailyLogRepository.refresh` skips rows with unsynced local changes.** That one rule is what makes
  offline writes safe — without it the pull stamps the server's copy over her edit. It is covered by
  `a_pull_must_not_overwrite_an_unsynced_local_edit`; deleting the guard fails that test.
- **Guest writes are never queued** (no server target under RLS) — they are written `SYNCED`.
- Verified on-device 2026-07-13: airplane-mode save → `push failed — queued for retry` → network back
  → `WM-WorkerWrapper: Worker result SUCCESS for … DailyLogSyncWorker`. No FATAL.

## pH sync is live — the most important thing on this page

`FeatureFlags.PH_TRACKING = true`, and pH is **no longer local-only**. `PhRepository` write-throughs
to the Supabase `ph_readings` table, with a WorkManager retry queue (`data/sync/PhSyncWorker.kt`,
`PhSyncScheduler.kt`) and pull-merge on sign-in. Guests (`LOCAL_USER_ID`) stay on-device.

**Consequences you must not get wrong:**
- The pH card copy (`ui/components/PhTrackerCard.kt:124`) says *"pH entries sync to your Genesyx
  account."* **Keep it.** pH is intimate health data; the sync has to be disclosed, not buried.
  Restoring the old "stored on this device for now" line would make the app lie to users.
- pH syncing changes what the app stores server-side, so the store disclosures and privacy copy must
  be reviewed against it before release. See "Pre-release checks" — owner work, and it gates release.

## The tracking contract (cross-platform)

`domain/tracking/tracking_test_vectors.json` — 16 cases, **mirrored verbatim into the iOS repo**.
`TrackingVectorTest` runs them against the real `StreakEngine`, so a metric that drifts on either
platform fails the build. The JSON is put on the unit-test classpath from where it lives beside the
engine (`app/build.gradle.kts`, `test` sourceSet), so there is one canonical copy that cannot drift.

If a vector and the engine disagree, **the spec wins and the engine changes.**

Contract decisions baked in: a week counts at **4 of 7 logged days** (`WEEK_COMPLETE_DAYS`); the
hydration goal is **user-set** (`GOAL_RANGE_ML = 1000..5000`, default `DEFAULT_GOAL_ML = 2400`);
`daysOnGoal` (days she actually hit the goal) is deliberately **not** `daysLoggedThisWeek` (days she
logged anything). `PreferencesRepository` is the only writer of the goal and clamps to range there —
so no reader can ever see a goal of zero and divide by it.

## Pre-release checks — what stands between here and shipping

**OWNER-ONLY (these gate release; no code change can close them). v1.1 changes what the app stores
server-side, so each of these needs a fresh review against the current behaviour — do not assume the
v1.0 answers still hold:**
1. **Store data-disclosure review.** Re-check the Play Console Data Safety answers against what v1.1
   actually syncs (pH readings now go to Supabase; see "pH sync is live").
2. **Privacy copy review.** Re-check `genesyx.co.uk/pages/privacy-policy` against the same.
3. **Re-run the server-side deletion check (S6).** `docs/schema.sql` hard-deletes `ph_readings` on
   `delete_current_user` (GDPR erase must remove rows, not tombstone them). The last server-side
   verification predates pH sync, so confirm the deployed function body matches the schema file and
   that a deleted account leaves no rows behind.

Detailed status for these lives with the owner, not in this repo.

**CODE — the v1.1 backlog is now DONE:**
- ~~No offline sync queue for daily logs~~ → **built and verified on-device** (see "The offline queue").
- ~~Log-screen Back discards unsaved edits~~ → **fixed** (confirm dialog + `BackHandler`).
- `PUSH_NOTIFICATIONS` is now **`true`** — local reminders shipped in v1.2 (WorkManager, on-device,
  no FCM). `PARTNER_INVITES` remains gated **off**: still a UI-only stub (no invite email, no
  cross-account linking). Shipping partner invites off is fine.

**KNOWN FLAKE (pre-existing, not from this work):** `CycleSettingsDialogTest.an_untouched_dialog_cannot_save`
failed once in ~6 full instrumented runs and passes 3/3 in isolation. It is a Compose UI timing flake;
nothing on this branch touches `CycleSettings*`.

## Build identity & release ops
- **Release env:** `GENESYX_ENV=PROD` — the `release` buildType overrides the `DEV` default
  (`app/build.gradle.kts`). Release logging (`Logger.d`) is suppressed outside DEV.
- **Signing:** `genesyx-release.jks` via `keystore.properties` → `storeFile=/Users/lucasvalenca_sf/Documents/genesyx-release.jks`.
  SHA1 `8D:EB:47:63:5F:10:2A:DA:7C:93:AA:27:15:E3:37:C6:49:B2:CC:73`, SHA256 `C3:D5:1F:4B…A4:46:C1:7D`
  (matches the fingerprint registered in Google Cloud; Google's Play app-signing key is **E0:CE**).
- **Artifacts:** AAB `app/build/outputs/bundle/release/app-release.aab` (Play upload); APK
  `app/build/outputs/apk/release/app-release.apk` (on-device). Install:
  `adb install -r app/build/outputs/apk/release/app-release.apk`.
- **Java:** `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`.
- **Secrets** in `local.properties` (git-ignored): `genesyx.supabaseUrl`, `genesyx.supabaseAnonKey`,
  `genesyx.googleWebClientId`, `genesyx.apiBaseUrl`. Never commit real values.
- **Architecture** is local-first: Room = source of truth, Supabase read-through on sign-in +
  write-through. See `ARCHITECTURE.md`, `docs/DATA_LAYER.md`, `docs/schema.sql`.

## v1.0 release history (context — all of this is DONE, don't redo it)
- **v6 AAB was uploaded + published to Internal testing** on `com.genesyx.app` (versionCode 6, from
  `main` = `1da07f9`, Mon Jul 6). Testers attached; all three OAuth clients registered.
  *Whether v6 was ever promoted to Production is not recorded here — confirm in Play Console.*
- **Account deletion proven (T4 a–e, on-device):** delete → Splash, no FATAL; re-login rejected with
  "Invalid login credentials"; same-email re-signup lands a fresh account. **S6 verified 0+0**
  server-side (rows gone from `auth.users`/`profiles`/`daily_logs`/`cycle_settings`). That check
  predates pH sync — see pre-release check #3.
- **Shopify pages LIVE:** `/pages/privacy-policy` and `/pages/delete-account` (H1 correct, no
  `[CONTACT_EMAIL]` placeholders, no `genesxy` typos, only `info@genesyx.co.uk`). Content to be
  re-reviewed against v1.1 — see pre-release check #2.
- Earlier RC fixes (all shipped): offline-save block, don't-re-onboard-signed-in-users, the dev
  "Clients" screen gated off, Home hero brand image, quiz back-arrow, intro logo lockup.
- P0 on-device script: `docs/GENESYX_P0_TEST_SCRIPT.md`. Release runbook:
  `docs/GENESYX_RELEASE_VERIFICATION_RUNBOOK.md`.

## Notes
- `delete_current_user` RPC is deployed and REST-verified (see blocker #3 for the pH caveat). No Edge
  Function needed.
- In-app "Privacy & Data" row opens `AppLinks.PRIVACY_POLICY_URL` via `ACTION_VIEW`.
- A `TODO(post-launch)` remains on `delete_current_user`: pin `set search_path = public, auth, pg_temp`
  to prevent search_path hijacking on the SECURITY DEFINER function. Deliberately not applied for the
  launch redeploy; worth doing in v1.1.
- Stale comment worth fixing sometime: `StreakEngineTest`'s class doc says "5-of-7 weekly streak" but
  `WEEK_COMPLETE_DAYS` is 4. The code is right; the comment is wrong.
