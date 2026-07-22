# Changelog

What changed, when, and why. Newest first. One entry per working session.

Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Versions are `versionName (versionCode)`.

---

## [1.2.1] — versionCode 11 — API 35 → 36 target migration (not yet uploaded to Play)

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
