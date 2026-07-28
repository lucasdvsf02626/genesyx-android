# Changelog

What changed, when, and why. Newest first. One entry per working session.

Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Versions are `versionName (versionCode)`.

---

## [Unreleased] — Single-source-of-truth bug batch (28 Jul device walkthrough)

### Fixed (28 Jul 2026) — cross-screen values that disagreed with each other
A device walkthrough caught the same number rendering differently on different screens minutes
apart. Six commits (`5cd4784..e7c90f2`), each phase committed with the unit suite green — now
**262 passing, 0 failures** (+15). Session detail in `docs/worklog/2026-07-28.md`.

- **Cycle phase/length (P0-1, P0-2).** The settings modal froze stale values in un-keyed `remember`
  state (said 33 while the card said 32; one day of length moves ovulation day and flips day 19
  Luteal↔Ovulatory). The modal now re-seeds from the latest emission; Home's never-cleared settings
  snapshot removed; `today` un-frozen on Track/Cycle screens. Phase math was already single-sourced
  in `CycleEngine` — only inputs diverged.
- **Hydration total (P0-3).** Four values at once, one column: Log Today's full-row save clobbered
  quick-adds with a stale snapshot; Nutrition side-read `.value` instead of its emitted logs; rapid
  +200 taps raced an in-memory snapshot. `DailyLogRepository` now serializes read-modify-writes
  against the DAO row; the form's Water field is a live read-through that saves on Done.
- **Sleep editor (P0-4).** Stepper seeded before Room answered and collapsed null/0 — "LAST NIGHT
  1h 15m" over a real 8h entry was the user rebuilding on a phantom zero. Editor gates on `loaded`;
  `todayMinutes` nullable; save was already an upsert (no duplicates possible — verified).
- **pH latest (P0-5).** Card picked "latest" by list position while Track/Home pick by timestamp;
  aligned on `maxByOrNull { recordedAt }`.
- **Pace copy (P1-6).** New WELL_BEHIND tier + a dedicated nothing-logged-yet line; 0.0 L at 16:00
  no longer wears "A LITTLE BEHIND". Stray M3 stop-indicator dot suppressed on empty bars.
- **7-day average (P1-7).** Now averages over logged days ("Avg on logged days"), killing the
  meaningless sparse-data "142ml". **Cross-platform semantic change** — vectors added.
- **Volume formatting (P1-8).** One `HydrationFormat` rule everywhere: sub-litre in ml, else litres
  to 1 dp. Ends "0.0 / 2.4 L goal" beside "2400ml to go" on one card.
- **pH chart/slider polish (P2).** dp-scaled chart marks, inset plot (edge dots were half-clipped),
  band-boundary hairlines; conventional round slider thumb. "urine (legacy)" confirmed intended.

### Cross-platform (iOS pickup)
`tracking_test_vectors.json` gains additive `cycleCases` (8) and `hydrationAverageCases` (4);
spec stays `genesyx-tracking-v1`. iOS must mirror the file, add both runners, and adopt the
logged-days average.

### Pending
On-device sweep + `connectedDebugAndroidTest` (two new instrumented hydration regressions compiled,
not yet executed); release build unrun this session.

## [1.3.2 (13)] — Waitlist joins via RPC (1.3.1's waitlist was broken)

### Fixed (27 Jul 2026) — every waitlist join failed in 1.3.1, not just duplicates
Server-side verification of the 1.3.1 waitlist found that PostgREST's targeted-conflict upsert
(`on_conflict=email` + ignore-duplicates) requires SELECT privilege on the table — which is
deliberately withheld so the email list can never be read or enumerated with the anon key. Result:
**every** join through the 1.3.1 client returned 401 (shown as the graceful "couldn't join" error).
The two requirements — silent duplicate no-op and an unreadable list — cannot both be met through
direct table access.

- Client now calls the **`join_waitlist(p_email)` SECURITY DEFINER RPC** (execute-only for
  anon/authenticated, `search_path` pinned); the function does an untargeted
  `on conflict do nothing` insert, lowercased/trimmed server-side.
- `docs/schema.sql` updated: table grants are now REVOKE ALL for clients (Supabase's default
  wide public-schema grants must be explicitly revoked — they had silently granted
  SELECT/UPDATE/DELETE, held back only by RLS), function DDL added.
- Deployed to production Supabase and verified over the anon REST path 27 Jul 2026: fresh join 204
  (trim/lowercase confirmed server-side), duplicate join silent no-op collapsing onto one row,
  direct table POST/GET/DELETE all 401. Final table state: RLS on, **zero policies, no client
  grants** — only the execute-only RPC touches it.
- 1.3.1 (12) was uploaded to Internal testing with the broken flow; **1.3.2 (13) supersedes it and
  is on Internal testing since 27 Jul 2026** — no other behaviour change.

## [1.3.1 (12)] — Three dormant features made real

### Fixed (27 Jul 2026) — change password, guest pH adoption, waitlist storage
Closes three of the six §6 gaps recorded in `APP_INVENTORY.md`. Unit suite **247 passing, 0
failures** (+2). The other three (quiz personalization, pregnancy mode, partner invites) are
deliberate deferrals needing product/server decisions, not fixes.

- **Change password now changes the password.** `AuthService.changePassword` re-verifies the
  current password by re-signing-in (so a borrowed unlocked phone can't take over the account),
  then updates via Supabase Auth (`SupabaseAuthService`). The Profile dialog gains
  loading/error/success states and password masking; local mode reports the feature needs the
  online service instead of pretending.
- **Guest pH readings now survive sign-in.** `PhReadingDao.adoptGuestRows` +
  `PhRepository.adoptGuestReadings`: on sign-in/sign-up the guest bucket's visible readings are
  reassigned to the account and queued `PENDING_UPSERT`, running before the pull so the merge
  can't clobber them; the WorkManager queue pushes them even if the sign-in refresh fails. Guest
  tombstones are skipped (nothing to propagate).
- **The waitlist email is now stored.** New `waitlist_emails` Supabase table (insert-only RLS for
  anon — clients can add but never read the list; UNIQUE(email) + `on conflict do nothing` absorbs
  duplicates; DDL in `docs/schema.sql`). `WaitlistViewModel` submits before showing success, with
  offline error handling. ⚠️ **Server + compliance follow-ups:** the table must be created in
  Supabase before this ships, and Data Safety / privacy policy must disclose email collection.

## [1.3.0 (11)] — Vaginal pH migration (Urine pH → Vaginal pH)

⚠️ The vaginal-pH range and thresholds below are
**PROVISIONAL** (marked in `domain/ph/PhStatus.kt`): healthy band **3.8–4.5**, elevated **> 4.5**,
input range **MIN 3.5 / MAX 7.0**, step 0.1, slider default 4.2 — the standard published healthy
vaginal-pH range. Genesyx is a wellness app, not a medical device. See the **Release gates** table
below for the pre-ship checklist status.

### Play rollout (27 Jul 2026) — 1.3.0 (11) live on Internal testing
- `genesyx-1.3.0-code11.aab` uploaded to Play Console and **rolled out to Internal testing**.
- **Health apps declaration re-submitted** (Policy → App content) — wellness / menstrual health,
  not Medical — as required for the changed health feature under the Jan 2026 enforcement. This
  clears Release-gates item 6(d) below.
- `APP_INVENTORY.md` fully rewritten against the tree: the stale v1.0 baseline (§1–§6) is gone;
  every section now describes current `main` (26 routes, tracker matrix, real-data Insights, the
  offline queues, updated gaps list). Verified by a two-agent code audit — 7 previously-listed gaps
  are closed (offline log save, back-guard, sample-data Insights, supplement counter, pH caption,
  unconsumed notifications toggle, inert Nutrition tile), 6 remain and are listed in §6.

### Release build (27 Jul 2026) — 1.3.0 (versionCode 11) built and archived
- `versionName` bumped `1.2.1` → `1.3.0` (versionCode was already 11); reverted a stray-keystroke
  corruption on line 1 of `app/build.gradle.kts` that broke the build.
- Unit suite green (245 passing). `:app:bundleRelease` + `:app:assembleRelease` GREEN.
- Signed AAB/APK archived at `~/Documents/Genesyx Releases/1.3.0-code11/` with `SHA256SUMS.txt`.
  This supersedes the `1.2.1-code10` archive, which predates the API-36 target and the vaginal-pH
  migration and must not be uploaded.
- Play Console upload + the owner items in the Release gates table are handled outside this repo.

### Audit follow-up (22 Jul 2026) — vaginal-pH audit flags closed
Closes the five non-blocking flags raised in the 22 Jul vaginal-pH audit. Docs/tests + a small
constant extraction; no behaviour change beyond the marker casing. Unit suite **245 passing, 0
failures / 0 errors** (was 236; +9). Not pushed.
- **Legacy marker is now one canonical constant** — `PhCopy.LEGACY_MARKER = "urine (legacy)"`,
  rendered verbatim (lowercase) on every surface. The card pill no longer uppercases it to
  "URINE (LEGACY)" and the Home nudge/accessibility strings no longer say "urine, legacy"
  (`PhTrackerCard`, `LogDaySummary`, `TrackerSummaryLogic`, `HomeScreen`).
- **Verbatim copy assertions** — new `PhCopyTest` pins the Healthy/Elevated/signpost/disclaimer/notice
  strings to hardcoded expected text (not via `PhCopy` constants), so an accidental copy edit fails
  the build.
- **Banned-phrase guard extended** with `leafy greens`, `whole grains`, `mineral water`
  (`PhCopyBannedPhraseTest`) — no dietary advice may appear in pH copy.
- **Entry default extracted + tested** — `PhStatus.DEFAULT = 4.2` (was an inline literal in
  `PhLogDialog`); `PhInsightLogicTest` pins it to 4.2, in-range, and Healthy.
- **One-time notice covered** — `GenesyxPreferencesDataStoreTest` now asserts the vaginal-notice flag
  defaults to unseen (fires once) and that dismissal persists (no re-fire).
- **Stale comment removed** — `Color.kt` `ElectricBlue` no longer references the retired "pH alkaline"
  scheme.

### Changed — the pH feature is now Vaginal pH, not Urine pH
- **Two-band model (Healthy / Elevated)** replaces urine acidic/optimal/alkaline (`PhStatus`). The
  duplicated hardcoded chart-band literals (`6.0f`/`7.5f`) were removed and the chart now derives its
  bands from `PhStatus` constants.
- **All user-facing copy** relabelled to "Vaginal pH" (British English): tracker card, log dialog,
  detail screen, Track "Your Trackers" row, Insights section, log-day rows.
- **Home "Check your pH" card made legacy-aware** — a pre-migration urine reading now surfaces as
  "urine (legacy)" and nudges a fresh vaginal reading, instead of presenting the old value as current
  (`ui/home/HomeScreen.kt`, `HomeViewModel.kt`). Found in a post-implementation Home audit.
- **Insight copy rewritten (neutral, not re-ranged):** two states only. Elevated copy is neutral and
  descriptive, names no condition, gives no dietary advice, and signposts a GP or pharmacist for
  persistently elevated readings. Healthy copy is brief and factual. All new copy lives in
  `domain/ph/PhCopy.kt` and passes the extended banned-phrase guard.
- **Banned-phrase guard extended** (`LearnContentTest`) with: bacterial vaginosis, bv, infection,
  thrush, candida, yeast, treat, cure, diagnos — and a new `PhCopyBannedPhraseTest` scans all pH copy.
  One benign Learn article line ("**Treat** every pattern as coincidence") was reworded to "Take …"
  so the extended `treat` term stays green — unrelated to pH, flagged for review.

### Historical data (E1 + E2, no silent relabelling)
- **Room v4 → v5** (`MIGRATION_4_5`): adds `measurementType TEXT NOT NULL DEFAULT 'urine'` to
  `ph_readings`; every existing row is stamped `urine` (legacy). New writes are `vaginal`.
- **Legacy readings stay distinguishable:** pre-migration entries show a neutral "urine (legacy)"
  marker (latest panel, log-day rows, Track summary) and are **excluded** from vaginal insight/status
  computation; on the chart they render muted/hollow and don't join the line. The y-axis/bands are the
  vaginal scale only.
- **One-time notice** (dismissible, DataStore-gated) explains the switch on first open of the tracker.
- **Supabase — DONE (production, 22 Jul 2026).** `measurement_type` column added to
  `public.ph_readings`; all **31** existing rows stamped `'urine'`; CHECK constraint
  `ph_measurement_type_check` (`'urine'`, `'vaginal'`) applied. The DTO sends/reads `measurement_type`;
  rows without it decode as legacy urine. Applied via:
  ```sql
  ALTER TABLE public.ph_readings
    ADD COLUMN IF NOT EXISTS measurement_type text NOT NULL DEFAULT 'urine';
  ALTER TABLE public.ph_readings
    ADD CONSTRAINT ph_measurement_type_check CHECK (measurement_type IN ('urine', 'vaginal'));
  ```
  The `DEFAULT 'urine'` stamps existing rows on add (no separate backfill). To re-run against a DB
  that lacks the constraint, guard with `... DROP CONSTRAINT IF EXISTS ph_measurement_type_check;`
  first, since `ADD CONSTRAINT` is not idempotent.

### Disclaimer
- The existing `MEDICAL_DISCLAIMER` mechanism is echoed by a pH-specific `PhCopy.DISCLAIMER` on the pH
  detail screen and the log dialog. **No citation infrastructure** in this release.
- **TODO:** Android citation surface — separate task.

### Release gates (all must clear before this ships)
1. **Client sign-off of the ranges** — healthy 3.8–4.5, elevated >4.5, input 3.5–7.0, default 4.2. **OPEN.**
2. **Client sign-off of the user-visible copy** (`domain/ph/PhCopy.kt`). **OPEN.**
3. **Supabase migration** — **DONE 22 Jul 2026** (production): `measurement_type` added to
   `public.ph_readings`, all 31 existing rows stamped `'urine'`, CHECK constraint
   `ph_measurement_type_check` (`'urine'`, `'vaginal'`) applied.
4. **`PhMigrationTest` v4→v5 on-device run** — **DONE 22 Jul 2026** (emulator-5554, 2/2 pass).
5. **iOS parity fix scheduled** — labels, two-band range/thresholds, `measurement_type`, copy. **OPEN.**
6. **Store / compliance updates:** (a) Play Data Safety form review — **OPEN**; (b) `docs/DATA_SAFETY_AND_PRIVACY`
   "Urine pH" → "Vaginal pH" (draft updated locally — gitignored; owner to review/submit) — **OPEN**; (c) `genesyx.co.uk`
   privacy-policy wording — **OPEN**; (d) ~~re-submit the Play Console Health apps declaration~~ —
   **DONE 27 Jul 2026** (wellness / menstrual health, not Medical).

### Verified (commit `3713374`, 33 files)
- Unit suite **236 passing, 0 failures / 0 errors** (`./gradlew :app:testDebugUnitTest`).
- **On-device (emulator-5554):** `PhMigrationTest` v4→v5 **2/2 pass** (rows preserved, stamped
  `urine`); legacy display confirmed live — Track shows "Vaginal pH · Last reading 6.8 · urine (legacy)".
- Banned-phrase guards green: `LearnContentTest` (extended) + new `PhCopyBannedPhraseTest`.

---

## [1.2.1] — versionCode 11 — API 35 → 36 target migration (not yet uploaded to Play)

**Status:** AAB built and verified (targetSdk 36, versionCode 11) — pending emulator edge-to-edge pass
and Play Console upload. Committed `b713937` (on `main`).

### Why
Google Play requires apps to target Android 16 (API 36) or higher to keep publishing updates after
**31 Aug 2026**. This bumps `compileSdk`/`targetSdk` 35 → 36 for compliance. `versionCode 10` (the
archived `1.2.1-code10` build) was never uploaded, so the API-36 build supersedes it as
`versionCode 11`; `versionName` stays `1.2.1` (no user-facing feature change).

### Changed
- `compileSdk = 35 → 36`, `targetSdk = 35 → 36`, `versionCode = 10 → 11` (`app/build.gradle.kts`).
  `minSdk` unchanged at 26.
- Fixed a deprecation surfaced by the build: `Icons.Outlined.MenuBook` →
  `Icons.AutoMirrored.Outlined.MenuBook` (`ui/onboarding/ReadinessSummaryScreen.kt`). No new
  dependency (AutoMirrored ships in `material-icons-extended`).

### Deferred
- `LocalLifecycleOwner` (`ui/settings/ReminderSettingsScreen.kt`) is deprecated in favour of
  `androidx.lifecycle.compose.LocalLifecycleOwner`, which requires adding the
  `lifecycle-runtime-compose` dependency. The current API still works; left for a dedicated
  Compose-library upgrade rather than bundling a dependency change into a compliance release.

### Toolchain
- **Unchanged** — built on committed **AGP 8.13.2 / Gradle 8.13** (which already fully support
  `compileSdk 36`; no AGP/Gradle bump is needed for API 36). API 36 platform (`android-36`) was
  already installed.
- An exploratory **AGP 9.2.1 / Gradle 9.4.1 / Kotlin 2.2.10** upgrade (with associated `gradle.properties`
  opt-out flags) was deliberately kept **out** of this release to keep the compliance change
  isolated and trivially attributable. It is preserved in a local git stash on this machine
  (`agp-9.2.1-gradle-9.4.1-kotlin-2.2.10-upgrade`) for a separate branch/session.

### Verified
- Unit tests **233 passing, 0 failures / 0 errors / 0 skipped** (`./gradlew :app:testDebugUnitTest`).
- `bundleRelease` GREEN, `lintVitalRelease` clean, R8/minify clean.
- Packaged release manifest reports `targetSdkVersion="36"`, `versionCode="11"`, `minSdkVersion="26"`,
  `versionName="1.2.1"` (`app/build/outputs/bundle/release/app-release.aab`).

### Next
- On-device test on an API 36 device, focused on **edge-to-edge enforcement** (the target-36 opt-out
  is gone) across all screens + IME insets, and predictive-back on the log-screen confirm dialog.
- Then upload `app-release.aab` (versionCode 11) to Play Internal testing. Nothing uploaded yet.

---

## [1.2.1] — versionCode 10 — merged to `main` (PR #14), not yet uploaded to Play

### Why
The versionCode 9 / 1.2.0 binary that reached Google Play Production was an **earlier** build than
the current `main` source: it predates the completed Track work. The current source is the intended
1.2 UI — the "Your trackers" list (Cycle, Hydration, Urine pH, Sleep, Symptoms, Nutrition, activity
dots) and its six tracker-detail screens (`ui/track/detail/`). Because Play will not accept another
artifact at an already-used versionCode, the fixed build cannot ship as code 9. This release bumps
the version so the complete current build can be published.

### Changed
- `versionCode 9 → 10`, `versionName "1.2.0" → "1.2.1"` (`app/build.gradle.kts`). No code or UI
  changes — this publishes the existing `main` source, which already contains the full Track
  implementation that the Play-served code-9 binary was missing.

### Verified
- Unit tests **233 passing, 0 failures**. `bundleRelease` + `assembleRelease` GREEN, R8 clean.
- Release APK `apksigner verify` → **Verifies** (release keystore SHA-1 `8DEB4763…B2CC73`), not
  debuggable, `com.genesyx.app`, versionCode **10** / versionName **1.2.1**.
- Installed + launched on emulator-5554 (fresh install — the prior code-9 copy was signed with a
  different key, forcing an uninstall): no FATAL/ANR/ClassNotFound; onboarding renders. The Track
  "Your trackers" walk-through is to be verified from the Play Internal-testing install.
- Artifacts archived outside the build dir: `~/Documents/Genesyx Releases/1.2.1-code10/`
  (`genesyx-1.2.1-code10.aab`, `genesyx-1.2.1-code10.apk`, `SHA256SUMS.txt`).

### Next
Upload `genesyx-1.2.1-code10.aab` to Play Internal testing, verify Track from the Play install,
then promote to Production. Nothing uploaded yet.

---

## [Unreleased] — 1.2.0

Branch `feature/v1.2-supplement-card`, off `main`. Not merged, not uploaded to Play.

### Added
- **Weekly summary card on Insights** (iOS parity) — opens the Insights screen with the current
  Mon–Sun week set against the one before: days logged vs last week, a mood/energy tally, and
  hydration/sleep/supplement deltas shown *only* when both weeks hold the data to compare (last
  week's silence is not treated as a week of zeros). Copy never scolds a quieter week. The
  "meaningful log" definition was extracted from `StreakEngine` into one shared `DailyLog.isMeaningful()`
  so the summary and the streak engine count days identically. `WeeklySummaryLogicTest` (9 cases);
  `StreakEngineTest` + `TrackingVectorTest` unchanged, so the cross-platform contract is intact.
- **Local reminders** (iOS parity; `FeatureFlags.PUSH_NOTIFICATIONS` on) — WorkManager-scheduled,
  strictly on-device (no FCM, no server push, no token). Six reminder kinds (daily log, missed-log,
  hydration, weekly insights, re-engagement; nutrition reserved) across four notification channels,
  a self-rescheduling one-time-work chain, a Profile → **Reminders** settings screen (per-category
  toggles, time pickers, day chips, quiet hours), a pre-permission sheet, and full `POST_NOTIFICATIONS`
  handling incl. the Android-13 "dialog shows twice" trap. All scheduling and suppression logic is a
  pure, tested `ReminderPolicy` (quiet-hours overnight wrap, already-logged-today, daily cap,
  re-engagement pacing) + `NotificationPermission` state machine — 27 tests. Reminders `cancelAll()`
  on sign-out and account deletion, so one can never deep-link a signed-out user past the auth gate.
- **Intraday hydration coaching** (iOS parity) — a time-of-day pacing line on the Home hydration tile
  and the Nutrition hydration card: it compares how much you've drunk with how much of the day has
  passed and frames it by morning/afternoon/evening, never as a grade. Pure `HydrationCoach`
  (8 tests); a fresh morning is never "behind", and being behind reads as an invitation, not a miss.
- **Supplement adherence card on Insights** — the current Monday-to-Sunday week, one bar per day
  showing how much of your supplement plan you took, with tiles for days logged and supplements
  taken. Live from your own logs. Sits directly beneath Hydration.
- **Zinc is now loggable.** The Log screen offers five supplements: Folic acid, Vitamin D, Omega-3,
  Zinc and Iron.

### Fixed
- **Your hydration goal is now used on the Insights card.** If you set a goal of 3000 ml, the
  Insights bars were still scored against 2400 ml and read higher than they should have. They now
  follow the goal you set. (Home, Nutrition and the streak engine were already correct.)

### Notes
- Iron can be logged but is not part of the four-item plan the card scores against, so taking it is
  recorded without inflating adherence — and not taking it is not counted against you.

---

## [1.1.0] — versionCode 8 — merged to `main` (PR #9), not yet uploaded to Play

### Added
- **Offline sync queue for daily logs** — the headline v1.1 item. A log saved offline now lands in
  Room as `PENDING_UPSERT` (schema **v4**, `MIGRATION_3_4`) and is drained by `DailyLogSyncWorker`
  with WorkManager backoff. `DailyLogRepository.refresh()` skips rows with unsynced local changes, so
  a pull can no longer overwrite an offline edit. Guest writes are never queued (no server row exists
  for them under RLS).
- **User-set hydration goal** — persisted in DataStore, read by the streak engine, Nutrition and Home.
  Editable from the Nutrition hydration card. `PreferencesRepository` is the only writer and clamps to
  `StreakEngine.GOAL_RANGE_ML` (1000–5000 ml), so no reader can see a goal of zero and divide by it.
- **`daysOnGoal`** — days this week she actually reached the goal, which is deliberately not the same
  as days she logged anything. Shown on the hydration card.
- **Cross-platform tracking contract** — `domain/tracking/tracking_test_vectors.json`, 16 cases,
  mirrored verbatim into the iOS repo and run against the real `StreakEngine` by `TrackingVectorTest`.
  A metric that drifts on either platform now fails the build.
- **Confirm-before-discard on the log screen** — leaving with unsaved edits asks first (dialog +
  `BackHandler`), instead of silently binning them.

### Changed
- **Log saves are no longer blocked offline.** v1.0 refused the save ("You're offline — reconnect to
  save your log") because an offline write would be silently overwritten by the server on the next
  read-through. The queue removes the reason for that gate, so the gate is gone.
- `versionCode 7 → 8`, `versionName 1.0.0 → 1.1.0`.
- **CLAUDE.md rewritten to match the tree.** It still described pH as local-only and the code as
  frozen at versionCode 6 — both long out of date. pH has synced to Supabase since the Phase 3 work.

### Removed
- `LogViewModel.isOnline()` and its test — the connectivity check existed only to power the offline
  save gate.

### Verified
- Unit tests **132 passing**, instrumented **14 passing**, 0 failures.
- `clean testDebugUnitTest bundleRelease assembleRelease` GREEN, R8 clean. Release AAB signed with
  `genesyx-release.jks` (SHA-1 `8D:EB:47:63…B2:CC:73`), not debuggable.
- **On-device (emulator-5554):** airplane-mode save → `push failed — queued for retry` → network
  restored → `WM-WorkerWrapper: Worker result SUCCESS for … DailyLogSyncWorker`. No FATAL.
- The safety net bites: deleting the `refresh()` guard fails
  `a_pull_must_not_overwrite_an_unsynced_local_edit`; changing `>=` to `>` in the goal comparison
  fails a tracking vector.
- **Server-side deletion re-checked (2026-07-13, owner ran `docs/supabase/verify_deletion.sql`
  step 1):** the deployed `delete_current_user` is SECURITY DEFINER and covers `ph_readings`,
  `daily_logs`, `cycle_settings`, `profiles` and `auth.users`. Orphan rows: **0 / 0 / 0 / 0**. No
  fix needed. Still outstanding: the end-to-end pass with a pH reading in play (step 4).

### Known issues
- `CycleSettingsDialogTest.an_untouched_dialog_cannot_save` is a **pre-existing flake** — failed once
  in ~6 full instrumented runs, passes 3/3 in isolation. Nothing on this branch touches
  `CycleSettings*`.
- `PARTNER_INVITES` and `PUSH_NOTIFICATIONS` remain gated off; their code is UI-only stubs (no FCM,
  no invite email, no cross-account linking). Their "until v1.1" comments are now aspirational.

### Before this can ship
Owner steps only — see **"Pre-release checks"** in `CLAUDE.md`. v1.1 changes what the app stores
server-side, so the store data disclosures and the privacy copy each need a fresh review against
current behaviour. Deletion check: `docs/supabase/verify_deletion.sql` (step 1 run 2026-07-13, clean).

### Stopped here (2026-07-13)
1. Fill the `[OWNER]` placeholders in `docs/DATA_SAFETY_AND_PRIVACY_v1.1.md` — **local-only, excluded
   from git** via `.git/info/exclude`. Holds the drafted Play Data Safety answers + privacy copy.
2. Publish the privacy copy; submit the Data Safety answers.
3. Finish the deletion proof **for pH** (step 4 of the SQL script). The daily-log half is already
   proven: a synced log from a throwaway account was erased by an in-app delete — which is why the
   orphan counts came back 0.
4. Merge PR #9, upload the AAB, promote.

---

## [1.0.0] — versionCode 6 · released to Play Internal testing (2026-07-06)

`main` = `d7be924` (versionCode 7 includes the theme toggle, PR #6).

### Added
- Full v1.0 app: onboarding quiz, cycle engine, Track, Nutrition, Insights, Learn (10 articles),
  Profile.
- pH tracking with **server sync** (Supabase `ph_readings`, WorkManager retry queue, pull-merge).
- Account deletion via the `delete_current_user` RPC — hard-deletes user rows, then the auth user.
- Theme follows system, with a Profile override (PR #6).

### Fixed
- Sign-in could fall through to a stale ambient session and seat one user in another's account (PR #5).
- Streaks counted only water and reset at midnight; the calendar drew predictions as if they were
  logged fact; the log dialog invented a period date.

### Security / privacy
- `GENESYX_ENV=PROD` in release; debug logging suppressed outside DEV.
- Dev "Clients" screen (with its demo-seed action) gated off for release.
