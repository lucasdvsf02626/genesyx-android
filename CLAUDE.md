# CLAUDE.md — Genesyx Android

Project Name: Genesyx Android

**`main` is at v1.4.2 (versionCode 19), and Play serves nothing newer than 1.3.0:** a live Play
Console audit (2026-07-28) found Internal testing on **1.3.0 (11)** and Production on **1.2.0 (9)**.
**18 is the identity to upload**
(`~/Documents/Genesyx Releases/1.4.2-code18/genesyx-1.4.2-code18.aab`, SHA-256 `222fb746…9c5ae0`).
Built and signature-verified 2026-08-26 16:05 from `9af9501` (see "Where the code actually is").
**Code 17 is superseded — never upload it:** it was consumed by the internal-testing release flow
and carries the confirmed Track → pH → Track navigation defect (the pH tab was plain-pushed from
Track/Home, so the next Track tap restored pH on top — fixed in `9af9501`). Codes 12–16 remain
superseded; 16 was skipped on purpose: four different bundles carried the `code16` filename on
26 Aug — never upload any of them.
Read this first. Honest state, verified against the tree on **2026-08-26**.

> **🔴 DEADLINE: Google Play requires targetSdk 36 in PRODUCTION by 2026-08-31.** Production still
> serves **1.2.0 (9), which targets API 35** — that is the non-compliant build Play names. An
> Internal-testing upload does **not** clear it; only a Production publish does. After 31 Aug, app
> updates are blocked until it is fixed. A "Request more time" button exists on Policy status →
> Issue details if the date cannot be met.

> **⚠️ If you read an older copy of this file, four things it told you are now FALSE:**
> 1. **pH is NOT local-only.** pH readings sync to Supabase. Never restore "stored on this device"
>    copy — see "pH sync is live" below.
> 2. **pH is VAGINAL pH now, not urine pH.** Two-band model (Healthy 3.8–4.5 / Elevated > 4.5),
>    pre-migration readings shown as "urine (legacy)". Ranges are **client-approved (28 Jul
>    2026)**: input 3.8–7.0, step 0.1, default 4.2 — no longer provisional.
> 3. **`main` is at versionCode 19, versionName "1.4.2"**, targeting Android 16 (SDK 36), Room
>    **v10** (migrations 6→7→8→9→10 each have an instrumented test). The archived `1.2.1-code10`,
>    `1.3.0-code11`, `1.3.1-code12`, `1.3.2-code13` and `1.4.0-code14` builds — and the
>    **`1.4.2-code17` build** (Track → pH → Track navigation defect) — are all **superseded —
>    never upload them** (code12's waitlist flow is broken — see CHANGELOG 1.3.2).
>    **The old "code-14 is blocked on a Supabase migration" warning is DEAD — do not act on it.**
>    `user_supplements` and `genesyx_products` went live 13 Aug and were REST-verified 19 Aug;
>    `daily_logs.sexual_activity` was REST-verified live 19 Aug. **Never run
>    `docs/migrations/2026-07-29_user_supplements.sql`** — it is superseded and harmful, per
>    `docs/PROMPT_SUPABASE_VERIFY_2026-08-19.md`.
> 4. **Play does NOT serve 1.3.2 or 1.4.0.** Internal testing = **1.3.0 (11)** (rolled out 27 Jul,
>    Health apps declaration re-submitted same day); Production = **1.2.0 (9)**. The code-13 upload
>    was started 28 Jul but never completed and code 13 is now superseded by 14; the corrected
>    Health apps + Data Safety console drafts are saved but **not sent for review**.

## 🔖 STOPPED HERE — resume from this (2026-08-26)

**Latest, 26 Aug 19:09 — the release candidate is `1.4.2 (20)`, commit `0288fda`.** A read-only QA
pass on code 19 found a silent data-loss defect on the wire: un-ticking the **last** supplement sent
`columns=user_id,date` — the column was never written, the row was still marked `SYNCED` (so
`refresh`'s unsynced-changes guard did not protect it), and the next pull resurrected the
supplements she had just removed. Cause: supabase-kt serializes with `encodeDefaults = false`, so a
property sitting at its declared default is dropped from the body, and PostgREST derives `columns=`
from that body. Fixed with per-property `@EncodeDefault(Mode.ALWAYS)` on `symptoms`, `water_ml`,
`supplements`, `notes` — **never** on `sexual_activity` (`NOT NULL DEFAULT false`; an explicit null
fails the whole upsert), and deliberately **not yet** on `food_groups` (same defect, but it is
absent from `docs/schema.sql` and naming a column that does not exist breaks every daily-log push —
probe production first). Also: duplicate custom supplements are refused via
`SupplementToggleSet.namesSomethingIn`, and the Track → Nutrition checkbox/label spacing is fixed.
**Code 19 is superseded — never upload it.** Upload
`1.4.2-code20/genesyx-1.4.2-code20.aab` (SHA-256 `86c5c161…3c049`). Everything below this paragraph
about 19 is history.

**Previously, 26 Aug 17:02 — the release candidate was `1.4.2 (19)`.** An owner review before the code-18
upload found Track → Nutrition had become a read-only dead end for supplements (PR #20 took the
brief's "§2 read-only summary" literally; the old screen had a "Log supplements" button). The
tracker now carries a **LOG SUPPLEMENTS** checklist by name + dose (essentials, then her own) on the
same `toggleSupplement()` path as the Nutrition chips, plus a **"Supplement plan"** button opening
the shared sheet. Upload `1.4.2-code19/genesyx-1.4.2-code19.aab` (SHA-256 `a23886b5…baa18`). Code
18 is superseded. Everything below this paragraph about 18 is history.

**The release candidate is now 1.4.2 (versionCode 18) — built 16:05 from `9af9501`, and NOT yet on
Play.** `9af9501` fixes the confirmed Track → pH → Track navigation regression: `tracker/ph` is a
bottom-tab root, but Track's pH row and Home's pH card plain-pushed it, so the next Track tap
restored pH on top and the tab looked dead. All pH entries now switch tabs via `navigateToTab` /
`TabNavigation.tabForRoute`. Unit 569/569, instrumented 40/40 (incl. the new 5-test
`PhTabNavigationTest`), release build green, code-18 artifacts archived under
`~/Documents/Genesyx Releases/1.4.2-code18/`. **The code-17 AAB is superseded — never upload it.**
Everything below about "code 16"/"code 17" is history: 16 was rebuilt in place four times on 26 Aug
and then abandoned for a clean identity (see `1.4.2-code17/BUILD_NOTE.txt`); 17 was consumed by the
internal-testing release flow while carrying the navigation bug.
**The 1.4.2 (versionCode 16) release artifact was built and signature-verified, but never reached
Play** — Internal testing still serves 1.3.0 (11), Production 1.2.0 (9). The upload was attempted
26 Aug and did not complete; nothing on Play changed. The Supabase gate is **CLOSED**:
`user_supplements` and `genesyx_products` went live 13 Aug and `daily_logs.sexual_activity` was
REST-verified live 19 Aug, so nothing backend-side blocks the upload. Landed since 1.3.0: the
vaginal-pH range approval (28 Jul), the cross-screen consistency batch, ml/cups toggle, pH
citations, cycle-phase timeline card, calendar clipping fixes, manual supplement entry + the
Genesyx range, private intimacy logging, meal logging (Room v9), and the opt-in intimacy reminder.
The `join_waitlist` SECURITY DEFINER RPC is deployed to production Supabase and REST-verified
(trim/lowercase, duplicate no-op, table unreadable/unwritable to clients).

**Later on 26 Aug: PR #20 merged to `main` (`b5a98c6`) — SFM-27 + SFM-28.** The Nutrition bottom
tab is fixed (root cause below), the plan card logs supplements inline with tappable chips, the
plan is a bottom sheet with an add-your-own form and reminder bells, the Track → Nutrition tracker
and the Insights "Nutrition consistency" card read the same repository as the chips.

**Then, 26 Aug 12:12 — `67b224d`, and the code-16 AAB rebuilt AGAIN from it.** `user_supplements`
now goes through the Article 9 `HealthDataCollectionGate` like the other four health stores (the
refusal is visible; `delete` stays ungated on purpose), and `backup_rules.xml` /
`data_extraction_rules.xml` state the Auto Backup scope explicitly instead of leaving it to the
platform default — health stores out of cloud backup, `device-transfer` deliberately intact.

**Then, 26 Aug 13:59 — `2ec3fc5`, and the code-16 AAB rebuilt a FOURTH time from it.** Testing
Track → Nutrition on the emulator showed the Track row saying "1 of 4" while the Nutrition card
said "1 of 6" (two custom entries) — `TrackerSummaryLogic` scored the plan alone. It now scores the
shared `SupplementToggleSet`. `2ec3fc5` descends from `67b224d`, so this artifact carries
everything above. **This is the AAB to upload.** The 12:12 build is parked in `superseded/` as
`*.pre-2ec3fc5.*`.

> **🔴 THREE bundles have held the name `genesyx-1.4.2-code16.aab` in one day.** Only the **12:12**
> one (SHA-256 `3ba7d6e1…9a480`, from `67b224d`) is the upload. The other two are parked in
> `~/Documents/Genesyx Releases/1.4.2-code16/superseded/` and must never be uploaded. **Check
> `SHA256SUMS.txt` before you pick a file — the filename has been reused twice.**
>
> Rebuilding in place was only legitimate because 16 has never been uploaded. **Once it is on Play,
> every further change needs versionCode 17** and its own release directory.

Verification record (what was proven on the emulator, and what was not) is in `CHANGELOG.md`;
the manual script is `QA_CHECKLIST_ANDROID.md`.

**Session-by-session history lives in `CHANGELOG.md`.** Read it before anything else.
**Product state lives in `APP_INVENTORY.md`** (repo root).

**Next actions, in order:**
0. **⏰ API-36 deadline work (31 Aug) runs in parallel and outranks everything.** Production must
   publish a targetSdk-36 build. The Health apps declaration and Data Safety form are still
   **drafts, not submitted** — they gate the Production publish, and their review time, not the
   upload, is the long pole. Submit them first or request more time.
0b. **DONE — superseded by the 17 bump (`84ebbc7`, 14:08). History: 26 Aug 13:59 — code 16 rebuilt from `2ec3fc5` (= `67b224d` + the Track-row fix), carrying PR #20 plus the consent-gate and
   Auto-Backup changes.** Still to do: run `QA_CHECKLIST_ANDROID.md` §5.9/§10 (Supabase rows,
   offline queue, consent snackbar) against a real account on the Play-installed build — those rows
   were not verifiable on the emulator. Add two: adding a supplement with health-data consent OFF
   must show the refusal and keep the form filled; and a cloud restore must come back signed out.
1. **Upload the code-20 AAB to Internal testing** (`1.4.2-code20/genesyx-1.4.2-code20.aab`, SHA-256 `86c5c161…3c049`) — verify the hash first, then smoke-test the
   Play-installed build on-device:
   Google Sign-In (needs the release SHA-1 registered), change password (incl. wrong current
   password), waitlist join (incl. duplicate), guest pH reading → sign-in → reading appears; plus
   the 1.3.0 checks — vaginal pH two-band display, "urine (legacy)" markers, Home deep links, a
   reminder firing — and the new opt-in intimacy reminder. **Add the code-19 round-trip, which the
   emulator could NOT prove (the QA session's non-UUID user id 400s every push by design): on a real
   account, log a supplement, un-tick the last one, force-stop, relaunch, and pull — it must stay
   un-logged.** That is the half of DEFECT-1 no unit test can close. **Add the navigation walk: Track →
   Vaginal pH → Track tab must return to Track** (the code-17 defect; pinned in code by
   `PhTabNavigationTest`, but walk it once on the Play-installed build).
2. **Send the corrected Play console drafts for review** — the Health apps declaration and Data
   Safety form (pH sync + waitlist email declared) are saved as drafts only. Resolve the Play
   URL-validator 429 warning on the deletion/privacy routes.
3. **Backend gates — mostly CLOSED 19 Aug 2026:** the live deletion re-proof is done (S6 gate
   closed, service-role counts all zero) and Supabase's DPA is incorporated into its ToS (no
   signature flow exists). Remaining: `genesyx.co.uk` privacy-policy wording (vaginal pH +
   waitlist email).
4. **Owner approval, then promote to Production.**
5. Schedule the iOS parity fix (labels, two-band thresholds, `measurement_type`, copy,
   logged-days hydration average — and the waitlist RPC if iOS gains the screen).

## Where the code actually is

| | |
|---|---|
| `main` | versionCode **20**, versionName **"1.4.2"**, compile/targetSdk **36**, minSdk **26**, Room **v10** |
| Working branch | none. The fix commit is `0288fda`; the code-20 artifacts were built from exactly that tree, clean |
| Unit tests | **588 passing, 0 failures, 0 errors, 0 skipped** across 76 classes (`./gradlew :app:testDebugUnitTest --rerun-tasks`, exit 0, 26 Aug 19:05); instrumented **41/41, exit 0** on the Pixel 8 / API 36 emulator |
| Release build | GREEN (2026-08-26 19:09) after `:app:clean`, R8/minify clean; AAB + APK signed with upload key SHA-1 `8D:EB…CC:73` (aapt2 reports `com.genesyx.app / 20 / 1.4.2 / target 36`) |
| Artifact | **`~/Documents/Genesyx Releases/1.4.2-code20/genesyx-1.4.2-code20.aab`** — SHA-256 `86c5c161…3c049`, 17,188,803 bytes; `SHA256SUMS.txt` verifies; `BUILD_NOTE.txt` beside it. `1.4.2-code19/` (empty-list sync defect), `1.4.2-code18/` (read-only tracker) and `1.4.2-code17/` (Track → pH bug; reached Play) are superseded — never upload from them |
| Play status | Internal testing **1.3.0 (11)**, Production **1.2.0 (9) — targets API 35, the build Play flags**; code-20 upload pending (17 reached the Play release flow with the Track → pH defect — see `9af9501`) |

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
- **26 Aug 2026 (PR #20, unreleased):** Nutrition tab fix (SFM-27); inline supplement logging via
  tappable plan chips + reporting `toggleSupplement`; Supplement Plan bottom sheet (essentials with
  reminder bells, add-your-own form, custom entries get bells too); Track → Nutrition tracker as
  iOS's read-only summary; Insights "Nutrition consistency" on the shared toggle set.

## Nutrition & supplements — rules that must survive (26 Aug 2026)

- **Cross-tab links go through `navigateToTab()`** (`ui/navigation/TabNavigation.kt`), never a
  plain `navigate(Screen.Learn.route)`. SFM-27's cause: Nutrition's "See all articles" pushed the
  Learn *tab root* on top of Nutrition; the next tab switch saved that chain under Nutrition and
  every later Nutrition tap restored it with Learn on top — the tab looked dead for the life of the
  process. It was **not** a route mismatch (`nutrition?plan={plan}` matches `nutrition`). Re-tapping
  the selected tab bumps `TabNavigation.RESELECT_KEY` in the entry's `SavedStateHandle` → scroll to
  top. Same mechanism bit twice: `tracker/ph` was plain-pushed from Track/Home (fixed 26 Aug,
  `9af9501`) — links whose target might be a tab route through `TabNavigation.tabForRoute` →
  `navigateToTab`. `TabNavigationTest` + `NutritionTabNavigationTest` + `PhTabNavigationTest` pin this.
- **`SupplementToggleSet` (`domain/model`) is the one definition of "the set and its counts"** —
  the four bundled essentials (a constant; `genesyx_products` is intentionally empty, never seed it)
  plus her `user_supplements`, deduped against plan wire/display names. The plan card, the Track
  tracker (`NutritionTrackerLogic`) and Insights (`SupplementInsightLogic`) all score through it, so
  "N of M" is the same N and M everywhere; adding an entry makes it "N of 5". `Supplement.fromWire`
  matches trimmed + case-insensitive — do not reintroduce an exact-match reader.
- **`DailyLogRepository.toggleSupplement()` reports** (`LogWriteResult`: Saved / Queued / Refused /
  Failed) and the screen shows a snackbar for anything but Saved. Un-logging the last item writes
  and pushes an **empty list** — never skip an "empty" write (ANDROID_PARITY.md §5). The other
  daily-log mutators are still fire-and-forget; if you make one report, use the same shape.
- Plan-item reminders live in `SupplementReminderRepository` under `plan:<supplement.id>`;
  `reconcile()` keeps those ids on purpose. Custom entries use their own id — one reminder per
  entry, shown in both the sheet and the tab's "Your supplements" card.
- Track → Nutrition is a **pushed screen with Back**, not a modal sheet — Android's equivalent of
  iOS's sheet (ANDROID_PARITY.md: don't port literally). All six tracker rows open a screen.
- **`ANDROID_PARITY.md` lives in the iOS repo** (`~/genesxy_apple.V1.02/`), not here.
- On-device QA without a Supabase account: `adb shell am instrument -w -e class
  com.genesyx.app.SignInLocally com.genesyx.app.test/com.genesyx.app.HiltTestRunner` (a `@SeedOnly`
  utility, never run by gradle) puts the installed app into a local signed-in state; pair with
  `SeedTestData`. Gradle's `connectedAndroidTest` **uninstalls the app afterwards** (data gone) unless
  `-Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true`.

## The offline queue (daily logs) — Room schema v9

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
