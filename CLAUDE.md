# CLAUDE.md — Genesyx Android

Project Name: Genesyx Android

**`main` is at v1.3.2 (versionCode 13), but Play does NOT serve it yet:** a live Play Console
audit (2026-07-28) found Internal testing on **1.3.0 (11)** and Production on **1.2.0 (9)** —
codes 12 and 13 were never uploaded. The verified 1.3.2 artifact awaits upload at
`~/Documents/Genesyx Releases/1.3.2-code13/`.
Read this first. Honest state, verified against the tree on **2026-07-30**.

> **⚠️ If you read an older copy of this file, four things it told you are now FALSE:**
> 1. **pH is NOT local-only.** pH readings sync to Supabase. Never restore "stored on this device"
>    copy — see "pH sync is live" below.
> 2. **pH is VAGINAL pH now, not urine pH.** Two-band model (Healthy 3.8–4.5 / Elevated > 4.5),
>    pre-migration readings shown as "urine (legacy)". Ranges are **client-approved (28 Jul
>    2026)**: input 3.8–7.0, step 0.1, default 4.2 — no longer provisional.
> 3. **`main` is at versionCode 13, versionName "1.3.2"**, targeting Android 16 (SDK 36), Room
>    **v6** — and it is AHEAD of the archived code-13 binaries: manual supplement entry + the
>    Genesyx range (`aecac1a`, Room v5→v6) is source-only, targets the NEXT versionCode (14), and
>    **must not ship** before the `user_supplements`/`genesyx_products` Supabase migration is
>    applied (REST-probed 29 Jul: tables don't exist yet, PGRST205). The archived `1.2.1-code10`,
>    `1.3.0-code11` and `1.3.1-code12` builds are all **superseded — never upload them** (code12's
>    waitlist flow is broken — see CHANGELOG 1.3.2).
> 4. **Play does NOT serve 1.3.2.** Internal testing = **1.3.0 (11)** (rolled out 27 Jul, Health
>    apps declaration re-submitted same day); Production = **1.2.0 (9)**. The code-13 upload was
>    started 28 Jul but never completed; the corrected Health apps + Data Safety console drafts
>    are saved but **not sent for review**.

## 🔖 STOPPED HERE — resume from this (2026-07-30)

**The 1.3.2 (versionCode 13) release artifact is built, verified and archived
(`~/Documents/Genesyx Releases/1.3.2-code13/`) but NOT yet on Play** — Internal testing still
serves 1.3.0 (11). Since the 27 Jul release day: the client approved the vaginal-pH ranges
(28 Jul); the cross-screen consistency bug batch, ml/cups toggle, pH citations, cycle-phase
timeline card and calendar clipping fixes landed; and manual supplement entry + the Genesyx range
(Room v6) shipped **in source only** — it targets versionCode 14 and is blocked on its Supabase
migration. The `join_waitlist` SECURITY DEFINER RPC is deployed to production Supabase and
REST-verified (trim/lowercase, duplicate no-op, table unreadable/unwritable to clients).

**Session-by-session history lives in `CHANGELOG.md`.** Read it before anything else.
**Product state lives in `APP_INVENTORY.md`** (repo root).

**Next actions, in order:**
1. **Finish the code-13 upload to Internal testing**, then smoke-test the Play-installed build
   on-device: change password (incl. wrong current password), waitlist join (incl. duplicate),
   guest pH reading → sign-in → reading appears; plus the 1.3.0 checks — vaginal pH two-band
   display, "urine (legacy)" markers, Home deep links, a reminder firing.
2. **Send the corrected Play console drafts for review** — the Health apps declaration and Data
   Safety form (pH sync + waitlist email declared) are saved as drafts only. Resolve the Play
   URL-validator 429 warning on the deletion/privacy routes.
3. **Backend gates — mostly CLOSED 19 Aug 2026:** the live deletion re-proof is done (S6 gate
   closed, service-role counts all zero) and Supabase's DPA is incorporated into its ToS (no
   signature flow exists). Remaining: `genesyx.co.uk` privacy-policy wording (vaginal pH +
   waitlist email).
4. **Owner approval, then promote to Production.**
5. Before any code-14 build: apply + REST-verify the `user_supplements`/`genesyx_products`
   migration. Schedule the iOS parity fix (labels, two-band thresholds, `measurement_type`, copy,
   logged-days hydration average — and the waitlist RPC if iOS gains the screen).

## Where the code actually is

| | |
|---|---|
| `main` | versionCode **13**, versionName **"1.3.2"**, compile/targetSdk **36**, Room **v6** |
| Working branch | none |
| Unit tests | **304 passing, 0 failures** (`./gradlew :app:testDebugUnitTest`, 29 Jul) |
| Release build | `:app:bundleRelease` + `:app:assembleRelease` GREEN (2026-07-29), R8/minify clean |
| Play status | Internal testing **1.3.0 (11)**, Production **1.2.0 (9)** (Console audit 28 Jul); code-13 upload pending |

`FeatureFlags` on `main`: `PH_TRACKING = true`, `PUSH_NOTIFICATIONS = true` (local reminders),
`ADMIN_CLIENTS = false`, `PARTNER_INVITES = false`.

### Shipped feature summary (details in `APP_INVENTORY.md` and `CHANGELOG.md`)
- **v1.1:** daily-log offline sync queue; streak engine v2 + cross-platform vector contract;
  user-set hydration goal; log back-guard; Learn (10 articles).
- **v1.2:** local reminders (WorkManager, on-device, no FCM); Track "Your Trackers" + six detail
  screens; Home hydration-ring/pH-nudge cards + deep links; all-real-data Insights + Weekly Summary;
  supplement adherence; intraday hydration coaching.
- **1.3.0:** Android 16 target; **Vaginal pH migration** — two-band model, Room v4→v5
  (`measurement_type`, legacy rows stamped `urine`), Supabase production migration applied 22 Jul,
  neutral `PhCopy` insight copy with banned-phrase guards.

## The offline queue (daily logs) — Room schema v6

Offline log saves QUEUE instead of being refused: `daily_logs.syncStatus` (`LogSyncStatus`) — an
offline write lands as `PENDING_UPSERT`; `DailyLogSyncWorker` drains it with WorkManager backoff.
**`DailyLogRepository.refresh` skips rows with unsynced local changes** — that one rule makes
offline writes safe; it is covered by `a_pull_must_not_overwrite_an_unsynced_local_edit`. Guest
writes are never queued (no server target under RLS) — written `SYNCED`. Verified on-device
2026-07-13.

Known remaining offline gap: **cycle settings** have no retry queue — a failed remote push is only
logged, and a later refresh can overwrite a never-pushed offline edit (`data/CycleRepository.kt`).

## pH sync is live — do not get this wrong

`FeatureFlags.PH_TRACKING = true`; pH is **not local-only**. `PhRepository` write-throughs to the
Supabase `ph_readings` table, with a WorkManager retry queue (`data/sync/PhSyncWorker.kt`) and
pull-merge on sign-in. Guests (`LOCAL_USER_ID`) stay on-device (and do not migrate on sign-in).

- The pH card copy (`ui/components/PhTrackerCard.kt`) says *"pH entries sync to your Genesyx
  account."* **Keep it** — the sync must be disclosed, not buried.
- The feature is **Vaginal pH**: input 3.8–7.0, step 0.1, default 4.2; Healthy 3.8–4.5, Elevated
  > 4.5 (`domain/ph/PhStatus.kt` — client-approved 28 Jul 2026). Copy lives in
  `domain/ph/PhCopy.kt` and is pinned by verbatim + banned-phrase tests — no condition names, no
  dietary advice, GP/pharmacist signposting only. Genesyx is a wellness app, not a medical device.

## The tracking contract (cross-platform)

`domain/tracking/tracking_test_vectors.json` — 16 cases, **mirrored verbatim into the iOS repo**.
`TrackingVectorTest` runs them against the real `StreakEngine`. If a vector and the engine disagree,
**the spec wins and the engine changes.** Baked in: weeks count at **4 of 7 logged days**; hydration
goal is **user-set** (`GOAL_RANGE_ML = 1000..5000`, default 2400, step 200); `daysOnGoal` ≠
`daysLoggedThisWeek`. `PreferencesRepository` is the only writer of the goal and clamps to range.

## Build identity & release ops
- **Release env:** `GENESYX_ENV=PROD` — the `release` buildType overrides the `DEV` default.
  Release logging (`Logger.d`) suppressed outside DEV.
- **Signing:** `genesyx-release.jks` via `keystore.properties` → `storeFile=/Users/lucasvalenca_sf/Documents/genesyx-release.jks`.
  SHA1 `8D:EB:47:63:5F:10:2A:DA:7C:93:AA:27:15:E3:37:C6:49:B2:CC:73`, SHA256 `C3:D5:1F:4B…A4:46:C1:7D`
  (matches the fingerprint registered in Google Cloud; Google's Play app-signing key is **E0:CE**).
- **Artifacts:** raw at `app/build/outputs/bundle/release/app-release.aab` (Play) and
  `app/build/outputs/apk/release/app-release.apk` (on-device, `adb install -r …`); release archives
  under `~/Documents/Genesyx Releases/<version>-code<N>/` with `SHA256SUMS.txt`.
- **Java:** `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`.
- **Secrets** in `local.properties` (git-ignored): `genesyx.supabaseUrl`, `genesyx.supabaseAnonKey`,
  `genesyx.googleWebClientId`, `genesyx.apiBaseUrl`. Never commit real values.
- **Architecture** is local-first: Room = source of truth, Supabase read-through on sign-in +
  write-through. See `ARCHITECTURE.md`, `docs/DATA_LAYER.md`, `docs/schema.sql`.

## Release history (context — DONE, don't redo)
- **v1.0:** v6 AAB published to Internal testing Jul 6 (`com.genesyx.app`); account deletion proven
  on-device (T4 a–e) and server-side (S6, 0+0 — predates pH sync, hence the re-check in Next
  actions); Shopify privacy/delete-account pages LIVE (`info@genesyx.co.uk`).
- **versionCode 9 collision:** a 1.2.0/code-9 binary reached Play Production before the Track work
  completed; Play won't reuse a versionCode, forcing the corrective 1.2.1/code-10 → superseded in
  turn by 1.3.0/code-11 (API 36 + vaginal pH).
- **1.3.0 (11) uploaded + rolled out to Internal testing 2026-07-27**; Health apps declaration
  re-submitted the same day.
- P0 on-device script: `docs/GENESYX_P0_TEST_SCRIPT.md`. Release runbook:
  `docs/GENESYX_RELEASE_VERIFICATION_RUNBOOK.md`.

## Notes
- `delete_current_user` RPC: **re-verified end-to-end 19 Aug 2026** post pH + supplements sync
  (throwaway account, RPC 204, JWT `user_not_found`, service-role counts all zero — S6 gate
  closed; see `docs/PROMPT_SUPABASE_VERIFY_2026-08-19.md`). The old "pin search_path" TODO is
  obsolete: production has pinned `search_path=''` since the 13 Aug hardening.
- In-app "Privacy & Data" row opens `AppLinks.PRIVACY_POLICY_URL` via `ACTION_VIEW`.
- **KNOWN FLAKE (pre-existing):** `CycleSettingsDialogTest.an_untouched_dialog_cannot_save` —
  Compose UI timing flake in instrumented runs; passes in isolation.
- Stale comment worth fixing sometime: `StreakEngineTest`'s class doc says "5-of-7 weekly streak"
  but `WEEK_COMPLETE_DAYS` is 4. The code is right; the comment is wrong.
