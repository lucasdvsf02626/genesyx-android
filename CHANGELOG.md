# Changelog

What changed, when, and why. Newest first. One entry per working session.

Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Versions are `versionName (versionCode)`.

---

## [Unreleased] — Single-source-of-truth bug batch (28 Jul device walkthrough)

### Added (13 Aug 2026) — Recipe cards (scaffolding, content pending)
- **New "Recipes for your cycle" section on Nutrition.** Each recipe is an expandable card — accent
  header band (phase-tinted, matching that phase's focus foods), title, one-line hook, prep time and
  phase tag — opening to nutrient chips, ingredients, and a numbered method.
- **Content-driven, empty for now:** the library (`domain/content/RecipeContent.kt`, `recipeContent`)
  ships empty, so the section shows a graceful **"Cycle-friendly recipes are coming soon"** card until
  recipes are added — the same pattern as the Genesyx range catalogue. Adding `Recipe` entries there
  needs no other code change. Recipes are curated local content (no backend, no cross-platform
  contract), phase-tagged (`recipesFor(phase)` shows that phase's recipes plus always-shown general
  ones; before a cycle is set up, only general ones appear).
- Verified: `RecipesSectionTest` — **2 Compose UI tests pass on the emulator** (empty state shows the
  coming-soon copy; a populated recipe renders its title/hook and reveals ingredient bullets, the
  numbered method and nutrient chips when expanded, and tapping toggles it); 368 unit tests green;
  `:app:assembleRelease` green.
- **Next:** paste recipe content into `recipeContent` (keep the health copy within the banned-phrase
  guard-rails — no condition names or dosing claims).

### Changed (13 Aug 2026) — Meal log: tap a meal to edit it
- A logged meal is now **tappable to edit** (sitting, description, nutrient tags), reusing the same
  dialog pre-filled — closing the gap where meals could only be added or deleted (supplements were
  already editable). Editing is a plain upsert: the meal keeps its id, date and original logged-at
  time, so list order is stable. The dialog titles itself "Log a meal" vs "Edit meal".
- Verified: `MealLogCardTest` — **3 Compose UI tests pass on the emulator** (empty-state invite;
  tapping a meal opens the edit dialog pre-filled and a round-trip Save preserves the id/date/fields;
  the add button opens the log dialog); `:app:assembleRelease` green.

### Added (13 Aug 2026) — Meal logging (local-only) with nutrient tags
- **New "Today's meals" card on Nutrition.** "Log a meal" opens a dialog: pick the sitting
  (Breakfast/Lunch/Dinner/Snack), type what you ate (free text, ≤120 chars), and optionally tag
  nutrients from a curated fertility-prep set (Protein, Iron, Folate, Omega-3, Fibre, Calcium,
  Vitamin C). Logged meals list newest-first with their time and tags; each can be deleted.
- **Deliberately local-only** (owner-approved scope): meals live only in Room — **no Supabase table,
  no sync, no shared-schema change**, so the iOS build on the same project is untouched. The card copy
  says "Kept on this device" — the local-only nature is disclosed, never implied as synced.
- **Room schema v7 → v8**: new `meal_entries` table (`MIGRATION_7_8`, additive, no destructive
  fallback), scoped by `userId` like every other table so accounts stay isolated on a shared device;
  `database.clearAllTables()` on sign-out / account deletion already wipes it, so no extra teardown.
- Verified: `MealEntryEntityTest` (wire-value mapping + unknown-value fallback) — **368 unit tests
  green** (the lone failure was the known `PreferencesRepositoryTest` coroutine flake, passes on
  isolated rerun); **`MealLogMigrationTest` passes on the emulator** (`runMigrationsAndValidate`
  confirms the DDL matches Room's generated v8 schema and existing daily logs survive the upgrade);
  `:app:assembleRelease` green (R8 clean) and the release APK launches on the fresh v8 schema with no
  Room/migration errors.

### Added (12 Aug 2026) — Per-supplement daily reminders
- **Each supplement can now carry its own daily reminder.** In the Nutrition "Your supplements"
  editor a supplement gains a *Daily reminder* switch; turning it on opens the system time picker and
  schedules a local notification at that time, every day. Off cancels it. Default time 09:00.
- **On-device, private, self-rescheduling** — mirrors the proven `ReminderWorker`/`ReminderScheduler`
  pattern: a one-time WorkManager job fires `SupplementReminderWorker`, which posts the notification
  and re-arms itself for the next day (no exact-alarm permission, no FCM). The notification title is
  *"Time for your {name}"* on the Nutrition channel, tapping deep-links to Nutrition. It is
  `VISIBILITY_PRIVATE` with a **name-free public version** (*"Supplement reminder"*) so the lock
  screen never shows what she takes.
- **No shared-schema change:** reminder times live only in DataStore (`supplement_reminders`, a
  supplement-id → minutes-of-day map). Reminders reconcile when the supplement list changes (a
  removed supplement's reminder is cancelled) and are **cleared on sign-out and account deletion**,
  so they never bleed across accounts on a shared device.
- Verified: `SupplementReminderSchedulerTest` (next-occurrence maths) — 365 unit tests green;
  **`SupplementReminderWorkerInstrumentedTest` passes on the emulator**, proving the notification
  actually fires with the correct title and the nameless lock-screen version; `:app:assembleRelease`
  green (R8/minify clean, worker survives shrinking).

### Added (12 Aug 2026) — Nutrition meal cards; weekly streak + 7-day hydration challenge
- **Focus foods are now attractive cards**, not a flat text list: each food gets its own rounded
  card with an accent header band, an icon, name + benefit, and a "Why this helps" expandable
  (client's "replace text-only food suggestions with meal/recipe cards"). Content unchanged
  (`PhaseFood`) — presentation only.
- **Weekly streak shown beside the daily streak** on Home (a second chip: "N-week streak", from the
  existing `StreakEngine.weeklyStreak`, the 4-of-7-days-a-week contract). Each chip appears only once
  it means something.
- **7-day hydration challenge** on Home: "log water 7 days running", progress = the hydration streak
  capped at 7, shown as a filled dot row with encouraging non-guilt copy (start / N-of-7 / complete).
  Rolls forward on its own; no new storage — derived from the streak engine.
- Verified: 360 unit tests green; `:app:assembleRelease` green.

### Fixed (12 Aug 2026) — connectivity: the sync indicator reads "not synced yet", never "offline"
- The lingering sync indicator was being read as "you're offline" when it actually means "this row
  hasn't reached the server yet" — a reassuring state, since the change is already safe on the
  device. There is no `isOnline()` gate or offline banner in the app (v1.0's was removed with the
  offline queue); the only surface is the Profile sync row. Reworded it to
  **"N changes not synced yet · Saved on your device — they'll sync to your account automatically"**
  (was "N changes waiting to sync"), and fixed the Hydration-detail line that implied offline
  ("…sync when you're online" → "…sync to your account automatically"). No copy now implies the app
  is offline.
- **Log loss verified solved on-device:** `DailyLogRepositoryTest` — **11/11 instrumented tests pass**
  on the emulator, covering the offline write → PENDING queue, past-date persistence, and the
  `a_pull_must_not_overwrite_an_unsynced_local_edit` rule (the one that guarantees a connection drop
  never loses a log). Sync uses `NetworkType.CONNECTED` (any network incl. mobile data) and the
  drains self-heal on app start.
- Verified: 360 unit tests green; connected DailyLog suite 11/11.

### Fixed (12 Aug 2026) — Google sign-in errors now name the real cause
- The catch-all Google failure message ("Couldn't reach Google. Check your connection.") hid the most
  common real cause — the app's **signing certificate isn't registered** as an Android OAuth client
  in Google Cloud (works on the release/Play build, fails on debug/other builds). `AuthViewModel` now
  maps failures to specific copy: a developer-config signal (Google Identity code `10`, "developer",
  "whitelist", "audience", or a provider-configuration exception) → "This build isn't registered for
  Google sign-in…"; no account on device → "No Google account is available…"; genuine network signals
  → the connection message; else a plain retry. The Supabase-rejected-token case (provider disabled /
  audience mismatch) gets its own message and is logged. Raw exception `type`/message is logged for
  diagnosis. Pure mapping extracted to `AuthViewModel.googleErrorText` + 6 unit tests.
- **Not a code bug in the sign-in flow itself** — verified live on the emulator: the account picker
  shows, caller verification succeeds, and the flow fails only at the token step (the emulator's GMS
  returned `NETWORK_ERROR`), handled gracefully with no crash. The actual fix for Google sign-in on a
  given build is registering that build's signing SHA-1 as an Android OAuth client (debug
  `D3:8A:DB…A0:69`; **Play app-signing key** for the Play build) — an owner action in Google Cloud.
- Verified: **360 unit tests green** (+6); `:app:lintDebug` + `:app:assembleRelease` green.

### Added (12 Aug 2026) — quiz-answer persistence + Tracking-Preferences editor; the 12-week plan completed
- **Onboarding answers now persist and sync.** New `QuizAnswersRepository` mirrors the answers to
  the shared owner-only `quiz_answers` table (cross-platform contract with iOS: a `jsonb` map of
  question-id → option-id). Recorded locally on quiz completion (`OnboardingQuizViewModel`), pushed
  as an owed write on sign-in, server-wins-else-adopt on refresh (`AuthRepository.persist`), and
  cleared from DataStore on sign-out/delete so one account's answers never reach the next. New
  `QuizAnswersDto` + Supabase/stub remote sources + DI binding. **No schema change** — the table
  already exists in production (REST-verified) and iOS authored it; Android is now a compatible
  second client.
- **Profile → Tracking Preferences is a real editor** (matches iOS): opens the onboarding questions
  with the current answers, edits round-trip through `quiz_answers`. Hydration settings moved to
  their own "Hydration" row so nothing was lost.
- **The 12-week Learn plan is complete (32 articles).** Added the client's topic #7, "The Shettles
  Method: theory versus evidence" — included ONLY as an unproven theory the evidence does not
  support, never as guidance. Written to pass the banned-phrase guard **by construction** (a
  debunking piece avoids the endorsement phrasing) — no guard weakening. Scheduled 2026-11-08 (the
  12th Sunday iOS reserved), keeping the other 11 dates cross-platform-identical.
- Tests: `QuizAnswersRepositoryTest` (5 — server-wins/adopt/clear/cross-account-isolation/guest),
  `QuizAnswersDtoTest` (3 — wire shape), DataStore round-trip + clear, `AuthRepositoryTest` updated.
  **354 unit tests green** (+10); `:app:lintDebug` + `:app:assembleRelease` + `:app:bundleRelease`
  green.

### Verified on emulator (12 Aug 2026) — real in-app run + instrumented suite
- **`connectedDebugAndroidTest`: 19 tests, 0 failures** on `test_Pixel8.1` (API-level Pixel 8),
  including the previously-flaky `CycleSettingsDialogTest` and the Room v7 migration tests.
- **Live app run** (installed debug build, driven via adb): Splash renders in **light mode with the
  floating egg graphics**; onboarding flow works (progress, "Did you know?" modal); the **gender
  question shows "Do you have a preference for your baby's sex?" with Girl / Boy / No preference /
  Prefer not to say**; Readiness Summary and Auth render. Signed-in tabs were not visually driven —
  the dashboard requires an account and a throwaway account was deliberately not created in the
  shared production DB; those surfaces are covered by the instrumented + unit suites.

### Release ops (12 Aug 2026) — code 14 built and archived; migrations reconciled; upload gated
- **Version bumped 1.3.2 (13) → 1.4.0 (14)** (`app/build.gradle.kts`). Signed release AAB + APK built
  (R8/lint clean), identity verified `com.genesyx.app 14 / 1.4.0 / SDK 36`, signing cert SHA-1
  `8DEB4763…B2CC73` / SHA-256 `C3D51F4B…A446C17D` = registered release key. Archived at
  `~/Documents/Genesyx Releases/1.4.0-code14/` with `SHA256SUMS.txt`
  (AAB `c82479bc…a395f4`).
- **Migrations reconciled against the shared DB (no SQL change needed):**
  - `daily_logs.sexual_activity` — use iOS's `supabase/migrations/20260810_daily_logs_sexual_activity.sql`
    (shared source of truth, idempotent `add column if not exists`).
  - `user_supplements` + `genesyx_products` — Android's `docs/migrations/2026-07-29_user_supplements.sql`
    (Android-only feature; additive; iOS ignores it). Its `delete_current_user()` update adds the
    `user_supplements` delete on top of the current 5-delete body — confirmed no iOS migration
    modifies that function, and `quiz_answers` is covered by its own FK cascade on `auth.users`, so
    nothing is clobbered.
  - Android DTO wire-names verified against every migration column (`sexual_activity`, `time_of_day`,
    `product_id`, `sort_order`, tombstones).
- **Production state (REST-probed 12 Aug, anon key):** `user_supplements` + `genesyx_products` = 404
  PGRST205 (NOT applied); `quiz_answers` = 401 (exists — iOS applied); `daily_logs.sexual_activity`
  = inconclusive via anon (table-grant blocks column resolution — verify with service/authenticated).
- **UPLOAD IS GATED:** the AAB must NOT go to Play until the migrations are applied and verified in
  production (owner-only — no service-role/CLI access this session). Order: apply migrations → verify
  RLS/tombstones/deletion → upload code 14. Runbook handed to owner.

### Daily digest — 12 Aug 2026
One session, all committed to `main` as **code-14 source** (not on Play; Play internal still serves
1.3.0 (11)); unit suite **344 green**, `:app:lintDebug` + `:app:assembleRelease` + `:app:bundleRelease`
green. In order:
- **Phase 1 correctness baseline** — every fertility statement carries predicted/estimated;
  consistency copy built from the engine's own 4-day contract; symptom qualification scoped to the
  displayed 28-day window; weekly-summary supplement delta gated on real evidence; one sleep night
  reads as an observation, not an average.
- **Phase 2 tracking** — `genesyx://tracker/{cycle,sleep,symptoms,nutrition}` deep links; Log
  Supplements dialog never hides a logged string (`SupplementLogRows`).
- **Item 7 education** — `LearnDrip` switched to fixed calendar dates (cross-platform contract with
  iOS); **31 articles** now ported from iOS 1.2.0 (18) (10 launch + 10 guides + 11 dated weekly
  series, 23 Aug→1 Nov); banned-phrase guard reconciled to iOS's reviewed list (removed bare
  `diagnos`/`douch`/`treat`/`cure` false-positives — flag for medical reviewer).
- **Home** — the last block is a persistent "A read for your week".
- **Track** — calendar matched to the iOS design: 3 markers (pH/symptoms/intimacy) with iOS's exact
  dot colours, "Fertile window" capsule badge, plain legend labels.
- **Onboarding** — gender preference options are Girl / Boy / No preference / Prefer not to say
  (persistence to `quiz_answers` still to do).
- **No shared-Supabase changes this session** — all client-side; iOS needs no matching change.

### Changed (12 Aug 2026) — Track calendar matched to the iOS design
- **Calendar markers reduced from six to iOS's three**: pH ("pH test"), symptoms/notes
  ("Symptoms / notes"), intimacy ("Intimacy") — with iOS's exact dot colours (pH teal-blue
  `#1F6E93`, symptoms brown/gold `#9A5B12`, intimacy purple `#8E3FA3`, new tokens in `Color.kt`).
  Everyday logging (mood/energy/sleep, water, supplements) is still recorded and shown in the
  day-detail dialog; it no longer crowds the cell with a dot each. `DayMarkers` order and rules now
  mirror iOS `DayMarkers.markers`. `DayMarkersTest` rewritten for the three-marker model.
- **Current-phase card gains the "Fertile window" capsule badge** beside the phase name (pale-cyan
  `#C0E6EF` / teal `#1B6C80`), matching iOS.
- **Legend labels back to iOS's plain wording** ("Fertile window", "Ovulation" — the "(predicted)"
  qualifier from Phase 1 stays on the claim *sentences* in the phase card and day dialog, and the
  "Faded days are predictions" caption still discloses the prediction; the legend only names a
  calendar colour).
- **Kept deliberately (platform idiom):** tracker rows open as Android nav routes, not iOS sheets —
  they power the `genesyx://tracker/*` deep links. Section order was already identical to iOS.
- **One flagged Android/iOS difference:** the phase-card sentence stays "You're in your **predicted**
  fertile window" (the implementation programme's content-safety rule), whereas the iOS screenshot
  shows it unqualified. Reconcile with the medical reviewer.
- Verified: 344 unit tests green, `:app:lintDebug` + `:app:assembleRelease` + `:app:bundleRelease`
  green.

### Added (12 Aug 2026) — weekly series ported (11 articles) + Home "a read for your week"
- **The 11-article dated weekly series ported from iOS 1.2.0 (18)** (`fertile-window` 23 Aug →
  `when-to-ask-for-support` 1 Nov, consecutive Sundays), same slugs/ids/publish-dates as iOS. Learn
  is now **31 articles** (10 launch + 10 guides + 11 weekly). Each reveals on its own calendar date
  via `LearnDrip`; future-dated ones stay hidden on every surface (list, search, article, related,
  Home card, `NEW_ARTICLE` reminder). The `g7→w1`/`g8→w7` related links held out last commit are
  restored. The client's week-7 Shettles piece stays absent (banned sex-selection claim language) —
  same as iOS. `LearnDripTest` now exercises real dated behaviour (hidden-before-date, released-on,
  newest-within-window).
- **Banned-phrase guard: `"treat"`/`"cure"` removed** alongside the earlier `"diagnos"`/`"douch"` —
  the same substring false-positives on responsible clinical-signposting the series carries ("treat
  sleep as the thing you can change", "treatment for cancer", "a commitment to treatment"). Every
  condition-name and pseudoscience ban stays; flagged for medical-reviewer confirmation.
- **Home: the last block is now a persistent "A read for your week"** (was "New this week", which
  only appeared the week a drip landed). It shows the freshly released weekly article when one
  dropped in the last 7 days, otherwise a deterministic weekly rotation over the published editorial
  articles (guides excluded), so the block is always present and always points at real content.
- Verified: **347 unit tests green**, `:app:lintDebug` + `:app:assembleRelease` + `:app:bundleRelease`
  green.

### Added (12 Aug 2026) — weekly-education programme: date-based drip + 10 guides ported (item 7)
- **Learn drip switched from weeks-since-first-open to fixed calendar dates** (`Article.publishedAt:
  LocalDate?`), the agreed cross-platform contract with iOS 1.2.0 (18): everyone sees a given
  article on the same real date. `LearnDrip` is now `isPublished` / `published(today)` /
  `newestReleased(today)` / `releasedOn(today)`; all six call sites (Learn list, search, article,
  related, Home "new this week" card, `NEW_ARTICLE` reminder) updated; the unused `firstOpenEpochDay`
  drip plumbing dropped from the Learn/Home read paths (the DataStore key is retained). New
  `LearnDripTest` (4) — the engine was previously untested.
- **10 "guide" how-to articles ported from iOS** (`guide-vaginal-ph-tracker` … `guide-understanding-
  vaginal-ph`), always-available, slugs/ids matched to iOS for cross-platform read-state and
  deep-link parity. Adds a `GUIDES` category and an `OPEN_PH` article CTA. Learn grows 10 → 20 with
  reviewed content — no invented placeholders. Hero art falls back to the category gradient (no new
  drawables). `g7`/`g8` related links to the not-yet-ported weekly series (`w1`/`w7`) are held out
  until that series lands (the Android integrity test rejects a dangling related id).
- **Cross-platform banned-phrase-guard reconciliation (flag for medical reviewer):** the bare
  substrings `"diagnos"` and `"douch"` were removed from `LearnContentTest`'s banned list — they are
  false positives on the *responsible disclaimer/caveat* language iOS ships and this app must carry
  ("it isn't a diagnosis", listing "douches" among times a reading is less representative). Every
  condition-name ban (bacterial vaginosis, bv, infection, thrush, candida, yeast) and pseudoscience
  ban (alkaline, balance/optimize your ph) is unchanged, so diagnosis *claims* and pH pseudoscience
  stay blocked. Matches iOS's reviewed list intent.
- **Streak (item 7a) confirmed COMPLETE, unchanged**: Home flame chip, one-shot milestone dialog,
  restore-streak card, new-article card all shipped in `43b371c`.
- **Still BLOCKED:** the 11-article dated weekly series (needs the same port pass, now unblocked by
  the date-based mechanism) and the client's week-7 Shettles-method piece (deliberately withheld on
  iOS too — its subject can't be written without banned sex-selection claims; a medical-reviewer
  decision).
- Verified: **346 unit tests green** (+4), `:app:lintDebug` clean, `:app:assembleRelease` +
  `:app:bundleRelease` green.

### Added (12 Aug 2026) — tracking-system completion (missing-features programme, Phase 2)
- **Exact tracker deep links.** `genesyx://tracker/{cycle,sleep,symptoms,nutrition}` now resolve
  to their detail screens (hydration and pH already had theirs), so a notification can route to the
  exact tracker. Groundwork for Phase 5's per-tracker reminders; opened cold, the nav host builds a
  synthetic back stack to the start destination, same as the existing hydration/pH links.
- **No logged supplement is ever hidden.** The Log's Supplements dialog now renders, in order,
  built-ins → the user's current custom entries → **orphan rows** (any string already stored on
  that day that no longer matches a current entry — a custom supplement since renamed, or one
  written by another device/build). Previously such a string stayed in the stored set but showed
  no row, invisible and un-untoggleable. New pure `SupplementLogRows.forDay` + 5 tests; storage
  and the cross-platform `daily_logs.supplements` contract are unchanged (every row still toggles
  the exact string it reads).
- **Verified IMPLEMENTED, left unchanged** (evidence in APP_INVENTORY §5): cycle setup/projection +
  predicted-fertile calendar, pH create/edit/delete/history/chart with legacy exclusion, hydration
  ml-canonical + display units + coaching, sleep editor + weekly coverage, symptom heatmap +
  rolling-window qualification (Phase 1), private-intimacy owner-only field + calendar marker +
  streak exclusion.
- **BLOCKED, flagged for decision:** robust custom-supplement *adherence scoring* by stable ID (not
  name) requires changing the shared `daily_logs.supplements` `text[]` contract — a cross-platform
  decision, not implemented. Today's fix keeps identity safe at the display layer only.
- Verified: **342 unit tests green** (+5), `:app:assembleRelease` green.

### Fixed (12 Aug 2026) — correctness baseline (missing-features programme, Phase 1)
- **Every fertility statement now carries predicted/estimated.** The fertile overlay
  (`CycleContent.kt`) reads "Your predicted fertile window is open" / "estimated higher-fertility
  days"; the ovulatory hero "High chance of conception today" became "Predicted peak fertility
  today"; Track's current-phase card says "predicted fertile window"; the calendar legend labels
  fertile/ovulation "(predicted)". Two new content-safety tests pin the qualifier on every fertile
  sentence and ban certainty phrases ("high chance of conception", "confirmed ovulation").
- **Consistency copy matched to the engine contract**: "Five days makes the week count" said five
  while `WEEK_COMPLETE_DAYS` is four — the sentence is now built FROM the constant, so it cannot
  drift again. `StreakEngineTest`'s stale "5-of-7" class doc corrected too.
- **Symptom-pattern qualification scoped to the displayed window**: the summary and the
  7-day pattern threshold now count the same rolling 28 days the heatmap shows (previously
  all-time — weeks-old symptoms could qualify a "pattern" the visible grid contradicted). Copy
  says "in the last four weeks". Two regression tests pin the scoping.
- **Weekly summary supplement delta gated on real evidence**: a last week with no meaningful log
  is missing data, not a week of zero supplements — the delta is now null in that case (water and
  sleep already had this guard). Regression tests cover both the suppressed and the genuine-zero
  comparison.
- **One sleep entry is an observation, not an average**: the single-night card no longer says
  "averaging … across the one night"; it reports the value and what to log next.
- Left deliberately unchanged, verified honest: `HydrationInsightLogic` (rolling 7v7, logged-days
  denominator), `OvulationLogic` (fully qualified copy), `WeeklySummaryLogic` water/sleep guards,
  `DailyLog.isMeaningful()`, `StreakEngine`, tracking vectors, pH contracts.
- Verified: **337 unit tests green** (+7), `:app:lintDebug` clean, `:app:assembleRelease` +
  `:app:bundleRelease` green.

### Added (12 Aug 2026) — client-feedback batch, phase 4 COMPLETE: streak UI, drip gate, new-article reminder
- **Home streak UI wired** (the 10 Aug groundwork's pending half): a flame streak chip under the
  greeting (any-activity streak, hidden below 2 days), the one-shot **milestone celebration
  dialog** (dismiss = `celebrateMilestones()`, so it cannot re-fire), the **"your streak paused
  yesterday" restore card** (opens yesterday's log via the `?date=` route), and the **"New this
  week" article card** (drip articles only; tap marks seen + opens the article).
- **Drip gate is now enforced** on every article surface: Learn landing, Learn search, article
  detail (an unrevealed slug renders as not-found — stale deep links can't tunnel in), related
  links, and Nutrition's 3-article taster all filter through `LearnDrip.available`. Opening an
  article records it in `read_article_slugs` (mark-as-read). Still a visual no-op until articles
  with `dripWeek ≥ 1` ship — the 12-week programme remains a content task gated on copy review.
- **`ReminderKind.NEW_ARTICLE`** (insights channel, `genesyx://learn`, request code 8): daily
  10:00 check gated on `LearnDrip.releasedOn` — fires at most once per drip week by construction.
  **Opt-in** (not in `DEFAULT_ENABLED`); toggle added to Reminder settings under Insights; the
  `genesyx://learn` deep link registered on the Learn tab. +3 `ReminderPolicyTest` cases.

### Changed (12 Aug 2026) — partner loose ends closed (client item 8, code half)
- The invite destination + its two deep links (`genesyx://invite/{code}`, https app link) are now
  registered **only when `PARTNER_INVITES` is on** — previously the screen stayed reachable by
  deep link with the flag off and would "link" a placeholder partner. An invite link now just
  opens the app.
- Auth sign-up subtitle no longer promises "partner info" (feature is dormant): now "Save your
  cycle and nutrition info securely."

### Changed (12 Aug 2026) — Splash eggs float again (client item 5)
- Restored the web app's `gx-float` drift the Android port dropped: each of the 8 Splash eggs
  wanders ±12dp vertically / ±6dp horizontally on independent 10–14s eased loops, staggered 0–3s,
  with different x/y periods so the path reads as a wander, not a diagonal. Offsets apply in the
  layout lambda — no per-frame recomposition.

### Changed (12 Aug 2026) — pH gets its own bottom tab; out of Nutrition (client item 1)
- The bottom bar grows to **seven tabs**: Home, Track, **pH**, Nutrition, Insights, Learn,
  Profile (client-chosen layout — add, don't displace). The canonical `tracker/ph` destination is
  promoted to the tab, so the Home nudge card, Track "Your Trackers" row, Insights card and the
  `genesyx://tracker/ph` deep link all land on the same screen — no duplicate pH surface.
- `TrackerDetailScaffold.onBack` is now nullable: the pH screen passes null (a tab has no back
  arrow); the other five tracker details are unchanged. `PhDetail.route` left `noBottomNavRoutes`
  so the bar shows.
- **`PhTrackerSection` removed from the Nutrition tab** — pH no longer appears under Nutrition;
  its slot in the action-first ordering closes up (hydration → your supplements → …). Insights'
  "Open tracker" now navigates to the pH tab instead of Track.
- Seven labels are tight at 360dp (~51dp per item); "pH" is the shortest label on the bar. Check
  on a small screen before release.
- Verified: 327 unit tests green, `:app:assembleRelease` green (R8 + lint).

### Changed (12 Aug 2026) — preference question reworded to the client's four options
- The onboarding quiz's sex-preference question (`QuizContent.kt`, id `gender`) now asks
  "Do you have a preference for your baby's sex?" with the client-requested options
  **Girl / Boy / No preference / Prefer not to say** (was the three softened options
  "I have a hope in mind" / "I'm happy either way" / "I'd rather not say"). Neutral preference
  capture only — no sway/selection claims anywhere; the banned-phrase guards on pH/Learn copy
  are untouched. Answers remain in-memory and unpersisted (wiring to the server-side
  `quiz_answers` table is still a separate, open item).
- Verified: 327 unit tests green.

### Added (10 Aug 2026) — client-feedback batch, phase 1: backdated logging, sync visibility, Profile editors
- **Backdated log editing.** `log` route gained an optional `?date=` argument; `LogViewModel`
  resolves it (malformed/future → today, `LogViewModelTest`) and every write path — form seed,
  water mini-card, save — targets that date. The Track calendar's day dialog gained
  "Edit this day" / "Add a log" on non-future days, so entries land on, and stay on, the day the
  user tapped. The repository layer already supported arbitrary dates; only the UI pinned "today".
- **Sync status made visible.** New `SyncStatusRepository` sums unsynced rows across daily logs,
  pH readings and supplements (new `observePendingCount` DAO queries). Profile shows
  "N changes waiting to sync" with **Sync now** only when something is actually queued; each sync
  scheduler gained `syncNow()` (REPLACE-enqueue so a drain stuck behind a mis-evaluated network
  constraint re-evaluates), and `MainActivity.onStart` re-enqueues the drains (KEEP) as the same
  self-heal already applied to reminder chains. Guards: `SyncStatusRepositoryTest`.
- **Profile dead ends are now editors.** "Health Profile" opens the shared `CycleSettingsDialog`;
  "Tracking Preferences" opens the shared `HydrationGoalDialog` (goal + ml/cups unit);
  "Personal Details" links straight to the name and email editors. New **Change email** flow
  (Account group + dialog): re-auth with current password, then Supabase sends a confirmation
  link — the persisted session deliberately keeps the old address until the link is followed
  (`AuthService.changeEmail` on both implementations; guarded by the new `AuthRepositoryTest`
  case). The static `detailCopy` paragraphs are gone.
- Verified: 313 unit tests green (was 304), `:app:assembleRelease` green.

### Verified (10 Aug 2026) — audited against the tightened `profiles` column grants (no code change needed)
- The shared production database moved `profiles` UPDATE to a per-column grant (managed from the
  backend side) and relocated onboarding quiz answers to a new owner-only `quiz_answers` table.
  Audited every `profiles` write in this app against the new grants, checking the *serialized*
  payload, not just call sites (encodeDefaults is off in supabase-kt 3.0.3, verified from its
  sources: `SupabaseClientBuilder.kt:67`):
  - `updateDisplayName` / `updateTheme` (`SupabaseProfileRemoteDataSource.kt:52/61`) are partial
    updates naming only `display_name` / `theme` — both granted.
  - `upsertProfile` (`:33`, sole caller `ProfileRepository.createMissing:74`) serializes only
    `{id, display_name}` in practice — the DTO's other fields equal their defaults and are
    omitted from the wire. Compatible, including the upsert's conflict-update arm.
  - This app never writes `partner_id` (`PartnerRepository` is entirely local Room; the feature
    flag has been off since v1.0), never touches `profiles.quiz_answers` (quiz answers are not
    persisted anywhere yet), and never sends `created_at`/`updated_at`.
- Follow-ups noted, not actioned: harden `upsertProfile` so `partner_id` can never re-enter the
  write path; if the onboarding-preference work is unblocked it must target the new
  `quiz_answers` table (not a `profiles` column); `docs/schema.sql` needs a sync pass once the
  backend changes settle.

### In progress (10 Aug 2026) — client-feedback batch, phase 4: streaks + article-drip groundwork (completed 12 Aug — see above)
- Groundwork landed, tree green (327 tests): `Article.dripWeek` + pure `LearnDrip` reveal logic
  (weeks since first open; a no-op while every bundled article is week 0); DataStore keys
  `first_open_epoch_day` (seeded set-if-absent on app start — existing users anchor to today),
  `read_article_slugs`, `last_seen_article_slug`; `HomeViewModel` now computes earned-but-
  uncelebrated milestones, the "log yesterday to reconnect your streak" restore date, and the
  "new article this week" card state.
- Still to come before phase 4 is done: Home UI (streak chip, milestone celebration dialog,
  restore prompt, new-article card), the drip gate on the Learn/search/article/Nutrition
  surfaces, mark-as-read, and the opt-in weekly `NEW_ARTICLE` reminder + `genesyx://learn` link.

### Added (10 Aug 2026) — client-feedback batch, phase 3: pH history, tucked disclaimers, action-first Nutrition, custom glass
- **pH history list.** The Vaginal pH screen now lists every reading, newest first, grouped by day
  ("Previous readings"); tapping one opens the existing log dialog prefilled for edit/delete.
  Legacy "urine (legacy)" rows render exactly as elsewhere.
- **Disclaimers moved behind a tap.** New shared `ExpandableInfo` component ("About this tracker"
  row with an info icon) replaces the always-visible disclaimer paragraph on the pH screen and in
  the pH log dialog. The disclaimer string itself is untouched — `PhCopy` verbatim/banned-phrase
  guards stay green. Deleted the three orphaned urine-strip PNGs (~200 KB, zero references).
- **Nutrition reads action-first.** Order is now hydration → pH → your supplements → Genesyx range
  → focus foods → suggested plan → articles; the two faded hydration stat lines merged into one
  compact "N days on goal · N steady weeks" line; the Learn section shows three articles + "See
  all" instead of all ten.
- **Custom glass size.** New `hydration_glass_ml` preference (default 250 ml, clamped 100–1000,
  single writer in `PreferencesRepository`, pinned by new clamp tests). The goal dialog gained a
  "Glass size" stepper (shared by Nutrition, Hydration detail and Profile → Tracking Preferences);
  the Nutrition card's ± buttons and the Hydration detail's first quick-add pair now pour her
  glass instead of a fixed 200 ml. The cups *display* unit stays the fixed 250 ml metric cup.
- Verified: 327 unit tests green, `:app:assembleRelease` green.

### Added (10 Aug 2026) — client-feedback batch, phase 2: intimacy logging, richer calendar markers, fertile-window reminder
- **Private intimacy logging (Room v6→v7).** `daily_logs` gained a nullable `sexualActivity`
  column (`MIGRATION_6_7`; null = not recorded — the truthful state of every existing row, pinned
  by a new 6→7 migration test). A quiet switch on the log form ("Intimacy · Private to you")
  records it; it shows in the day summary and calendar only to the owner and is structurally
  untouched by partner code. Deliberately NOT a streak qualifying action — the cross-platform
  tracking contract stays unchanged. **Server gate:** `daily_logs.sexual_activity boolean` must
  join the pending pre-code-14 Supabase batch; until it is applied, a log carrying an intimacy
  record queues and retries (a null one syncs fine — the field is omitted from the wire while
  null, pinned by `DailyLogDtoTest`). `docs/schema.sql` documents the column as not-yet-applied.
- **Calendar markers now distinguish six signals.** Symptoms & notes split out of the catch-all
  "how you felt" dot, and intimacy earned its own; dots render in rows of three so six still fit
  a cell. Legend and TalkBack descriptions follow automatically. `DayMarkersTest` extended.
- **Fertile days carry a ring** on top of the tint (faded on predicted days) so the window reads
  at a glance, and a new **fertile-window reminder** (`ReminderKind.FERTILE_WINDOW`, tracking
  channel) posts a morning heads-up on the predicted window's first day — a daily 09:00 check
  gated in `ReminderPolicy` (needs cycle settings, respects quiet hours and the daily cap;
  "predicted" wording throughout). Toggle added to Reminder settings; default-on for fresh
  permission grants only. Guards: four new `ReminderPolicyTest` cases.
- Verified: 324 unit tests green, androidTest compiles, `:app:assembleRelease` green,
  Room schema `7.json` exported and matching the migration DDL.
- **Toolchain note:** the working tree carried an uncommitted, half-finished AGP 9.3.1 /
  Kotlin 2.2.10 / Gradle 9.5 upgrade that failed configuration (Hilt/KSP plugin classloader).
  Stashed (`git stash`, alongside the older 9.2.1 attempt) and also saved as a patch in the
  session scratchpad; the tree builds on the committed toolchain again.

### Fixed (30 Jul 2026) — production `ph_value_range` constraint aligned with the vaginal model
- A read-only production audit found the urine-era `CHECK (ph_value BETWEEN 4.5 AND 9.0)` still
  live on `ph_readings` — it had survived the 22 Jul vaginal migration and rejected every vaginal
  reading below 4.5 (the entire lower Healthy band): 57 rows in the table, minimum exactly 4.5,
  zero below. Affected readings were never lost — they queue as `PENDING_UPSERT` on-device and
  drain automatically now the constraint accepts them.
- Replaced in production (single transaction, pre-checked against all existing rows) with a
  measurement-type-aware constraint: vaginal **3.8–7.0**, urine (legacy) **4.5–9.0**.
  `docs/schema.sql` updated to match. Old prod clients (≤ code 10) omit `measurement_type`, take
  the column's `'urine'` default, and are unaffected.
- Known edges, accepted: readings 3.5–3.7 entered on 1.3.0 (its floor predated the approved 3.8)
  stay device-local; a legacy urine row edited below 4.5 keeps type `urine` and won't sync
  (same as before the change). Also catalogued in prod: `partner_invites` (v1.0, feature-flagged
  off) — cascade covers inviter rows on account deletion; invitee-side rows are a code-14
  deletion-function consideration.

### Changed (30 Jul 2026) — CLAUDE.md corrected to the verified release state
- CLAUDE.md's header no longer claims 1.3.2 (13) is live on Play. It now records the audited
  state: Internal testing serves **1.3.0 (11)**, Production **1.2.0 (9)**, and the verified
  code-13 archive awaits upload. Also corrected: Room **v6**, 304 unit tests, the client-approved
  pH input floor (3.8, no longer provisional), and that the supplements feature is source-only,
  targeting code 14 behind its unapplied Supabase migration.
- Archive identity re-verified: aapt2 reports `com.genesyx.app` 13 / 1.3.2 / SDK 36; apksigner
  cert SHA-1 matches the registered release key. The archive binaries (29 Jul 22:12, built at
  `fd15178`) predate `b488519` — **the calendar clipping fixes are NOT in code 13**; they ship
  with code 14.

### Changed (29 Jul 2026, night) — code-13 re-archive; calendar clipping fixes; supplements build starts
- **`1.3.2-code13` archive refreshed** (`~/Documents/Genesyx Releases/1.3.2-code13/`, new
  `SHA256SUMS.txt`): the binaries now include the day-marker/phase-timeline session and the 29 Jul
  wording work, committed as `fd15178` so the archive matches git exactly. Release build green
  (`:app:bundleRelease` + `:app:assembleRelease`, R8 + lintVital clean); aapt2 confirms
  `versionCode 13 / versionName 1.3.2 / SDK 36`. Awaiting Play upload — Internal still serves
  code 11.
- **Client punch-list triaged against the tree** for the upcoming meeting: Track reorder ✅ shipped
  in source; ml/cups toggle ✅ shipped in source (28 Jul); Genesyx-range visibility ❌ proposal only;
  manual supplement entry ❌ proposal only; the reported "… calendar truncation" matched no code —
  the app never sets `TextOverflow.Ellipsis`; a layout audit instead found real clip/squeeze bugs
  on the calendar surfaces, fixed below.
- **Calendar clipping fixes** (`TrackScreen.kt`, `LogDaySummary.kt`): the marker legend under the
  month grid is now a `FlowRow` (four dot+label pairs need ~240dp — a 320dp screen or raised font
  scale was clipping the trailing "supplements"/"pH" labels); the Track header column and the
  month-nav label take `weight(1f)` so a long month title squeezes before the edit/nav controls are
  pushed off-screen; the day-detail dialog's label column is `widthIn(min 96.dp)` (was fixed
  96dp, wrapping "Supplements" mid-word at large font scale) and `PhReadingRow` weights the status
  label so the trailing time can no longer be clipped at the dialog edge.
- `docs/migrations/2026-07-29_user_supplements.sql` gained the **`genesyx_products` catalogue**
  (read-only for signed-in users, writes via service_role only, `product_id` FK on
  `user_supplements` with `ON DELETE SET NULL`) — the client's "Genesyx visibility in supplements"
  as data, with a coming-soon empty state while the range has zero SKUs (`3e15fb1`). Application to
  production was attempted this session and **REST verification shows the tables do not exist yet**
  (`PGRST205` on both) — the migration is still NOT APPLIED; re-verify before any client that
  writes these tables ships.
- **Manual supplement entry + Genesyx range shipped in source** (client items 3 and 4):
  - Room v5→v6: `user_supplements` mirror table (client-minted UUID id, `updatedAt` LWW clock,
    `deletedAt` tombstone, `syncStatus`), `MIGRATION_5_6` DDL verified identical to the exported
    `6.json` schema.
  - `UserSupplementRepository` cloned from the proven `PhRepository` shape: writes land in Room
    instantly as PENDING and push-or-queue (WorkManager backoff, `user-supplement-sync`); deletes
    are soft; guests stay local with no doomed retries; sign-in adopts guest entries **before** the
    pull so they survive the merge (`AuthRepository.persist`). Name trimmed + 1..60 enforced in the
    data layer (server contract), whitespace-only dose collapses to null.
  - Nutrition tab gains **"Your supplements"** (add/edit/delete dialog — name, optional dose,
    optional morning/afternoon/evening/anytime) and **"The Genesyx range"** reading the
    `genesyx_products` catalogue with a "coming soon" empty state — seeded products appear with no
    app update. Guests/offline/empty all collapse to coming-soon (RLS is signed-in-only).
  - The Log screen's Supplements dialog now lists the user's own entries under the built-in five
    and stores them by name (compatible with the existing `Set<String>` storage; unknown names
    simply don't score against the 4-item default plan). Custom names colliding with a built-in
    row are filtered out of the dialog; the mini-card denominator now counts customs.
  - Tests: `UserSupplementRepositoryTest` (9 — push/queue/guest/validation/trim/merge/adoption) +
    `UserSupplementTest` (3 — wire contract). Unit suite **304 passing, 0 failures**; release build
    + R8 green. NOT yet released — ships in the next versionCode, and MUST NOT ship before the
    Supabase migration is applied and REST-verified.

### Changed (29 Jul 2026, evening) — Insights "Cycle regularity" card becomes "Your cycle phases"
- The Insights cycle card no longer plots one dot on the 21–35 range: it now shows a proportional
  phase timeline (Period / Follicular / Ovulatory / Luteal) with a "Today · Day N" marker, current
  and next phase lines, one per-phase wellbeing sentence, and the estimate disclaimer. The two
  summary tiles (Your cycle / Typical) stay.
- Segments are **run-length-encoded from `CycleEngine.getCyclePhase`** — never a restated
  when-ladder — so the bar cannot disagree with the phase Home, Track, and the Cycle detail screen
  report. The engine's single-day Ovulatory phase is widened only *visually* (`MIN_VISUAL_DAYS`);
  the marker is placed in the same widened coordinate space, so it always sits inside the segment
  named as the current phase. Degenerate case handled: a 10-day period in a 21-day cycle swallows
  the ovulation day and the card renders the two segments the engine returns, not an assumed four.
- Client decisions: segment boundaries mirror the engine exactly, and the first phase is labelled
  **"Period"** (the app's word everywhere), not "Menstrual". The honesty line survives, trimmed:
  the timeline comes from the saved setup, not a measurement of past cycles.
- New copy (four phase lines + disclaimer) is pinned and guarded by a banned-phrase test extending
  the card's anti-diagnosis list with fertility/conception terms.
- `CycleRegularityLogic.compute` now takes `today` (defaulted, `OvulationLogic` pattern);
  `CycleRegularityInsights` gains segments/day/phase/marker fields. Files:
  `CycleRegularityLogic.kt`, `InsightsViewModel.kt`, `InsightsScreen.kt`
  (`CycleRegularityCard` → `CyclePhaseTimelineCard`), `CycleRegularityLogicTest.kt` (6 → 15 tests).
- Unit suite: **292 passing, 0 failures**.

### Changed (29 Jul 2026) — client wording + Track order; legacy-pH audit came back clean
- Home pH nudge card retitled **"Vaginal pH"** (was "Check your pH"), including the screen-reader
  description, matching the tracker's name everywhere else.
- Track "Your Trackers" reordered to the client-requested **Cycle → Vaginal pH → Nutrition →
  Symptoms → Sleep → Hydration** (was Cycle → Hydration → pH → Sleep → Symptoms → Nutrition).
  Pure row permutation; routes and summaries unchanged.
- A full audit for legacy urine-pH UI found **zero reachable urine-pH screens, strings, or routes**
  in the tree — the 22 Jul migration (`3713374`) converted the model in place. A tester report of a
  "Urine pH" 4.5–9.0 page is explained by an out-of-date installed build, not by this source.
  Follow-ups noted: several docs (`CYCLE_ENGINE.md`, `UIUX_SPEC.md`, `SCREEN_LAYOUTS.md`,
  `DATA_LAYER.md`, `ARCHITECTURE.md`) still describe the old urine model and need updating.
- Unit suite after each change: **265 passing, 0 failures**.

### Added (29 Jul 2026) — pH detail screen answers the four questions, with citations
- New shared `Citation` type + `CitationList` component: dated, named, tappable sources that open the
  publisher's own page. Compile-time constants, so a citation can never fail to load. This is the
  citation surface that was logged as a TODO during the 1.3.0 pH migration.
- `PhCopy` gains four sections — why pH matters, what your reading means, what to do next, how the
  Genesyx plan relates. The two band-dependent sections resolve via `meansFor`/`doNextFor` off the
  same `PhInsightLogic` the Insights screen uses, so a reading classifies identically everywhere and
  legacy urine rows are correctly excluded.
- Sources: StatPearls *Physiology, Vaginal Structure and Function* (updated 5 Jul 2026) for the
  3.8–4.5 range and the lactobacilli/lactic-acid mechanism; NHS *Vaginal discharge* (reviewed
  15 Feb 2024) for the when-to-speak-to-someone list and everyday care advice.
- The supplements section explicitly disclaims any pH effect and links to the plan without selling it.
- All new copy passes the banned-phrase guard unchanged. Citation titles are deliberately outside
  that guard — it governs claims Genesyx writes, not the names of the sources it cites.
- `docs/migrations/2026-07-29_user_supplements.sql` — **PROPOSED, NOT APPLIED**: the shared
  `user_supplements` table for manual supplement entry, shaped after `ph_readings` (soft delete,
  `updated_at` LWW, four RLS policies), including the mandatory `delete_current_user()` addition
  without which deleted accounts would leave orphaned rows.
- Unit tests: **272 passing, 0 failures** (was 265; +7).

### Release readiness (28–29 Jul 2026) — code 13 verified; Play drafts corrected
- Play Console was checked live. Production remains **1.2.0 (9)** and Internal testing remains
  **1.3.0 (11)**; the earlier changelog statement that code 13 was already on Internal was wrong.
- The Health apps declaration was corrected and saved: Period tracking, Nutrition and weight
  management, Sleep management, and Reproductive and sexual health are selected; Mental and
  behavioural health is removed; Genesyx is declared as a wellness app, not a medical device.
- Data Safety was updated and saved as a
  draft. It now declares Name, Email address, User IDs and Health info as collected but not shared,
  encrypted in transit, optional because guest mode is available, with account/data deletion URLs.
  These console changes are **not yet sent for review**.
- `docs/DATA_SAFETY_AND_PRIVACY_v1.1.md` now records the v1.3.2 facts, including waitlist email
  storage through `join_waitlist`, the verified Supabase region (EU West / Ireland), adults 18+,
  and the live Genesyx Ltd identity. Supabase DPA status and the production pH deletion proof remain
  owner/backend verification gates.
- Unit tests: **265 passing, 0 failures**. Connected tests: **18 passing, 0 failures** on the API 37
  emulator after fixing a Room test teardown race by waiting for repository observers to stop before
  closing their in-memory database.
- Signed release AAB and APK built successfully with lint/R8 clean. APK identity verified as
  `com.genesyx.app`, **1.3.2 (13)**. AAB SHA-256:
  `4dd38d1953261ffe1c2291b37cff94c3794a16ed363530152da53f41b575701c`.
- The code-13 Internal upload was started but **not completed or rolled out** in this session. No
  Production release was created or submitted.

### Changed (28 Jul 2026) — approved vaginal-pH range
Client approved the release contract: input **3.8–7.0**, default **4.2**, Healthy **3.8–4.5**,
Elevated **>4.5**, with the existing neutral disclaimer and GP/nurse/pharmacist signposting. The
old 3.5 input floor contradicted the Healthy lower bound and could label 3.5–3.7 as Healthy; the
input floor, repository validation, tests, UI copy and inventory are now aligned at 3.8.

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

### Added (28 Jul 2026, later the same day) — ml/cups display choice for hydration
Client request: the customer chooses how water reads. New `HydrationUnit` preference (DataStore,
default ml) with an ml / cups toggle inside the shared water-goal dialog — which Nutrition now
reuses instead of its private duplicate. One cup = **250ml (metric, client-confirmed)**. The chosen
unit applies display-wide: Home card, Nutrition card, Track row, Hydration detail (headline, avg
tile, history), Log Today mini-card, My logs, the calendar day dialog, and the coaching/insight
copy ("about 4.8 cups to go"). **Storage, sync, goals and streaks stay in ml** — the toggle changes
nothing but presentation; quick-add buttons keep their ml labels. Per-day deltas stay in ml (a rate,
not a pour). Unit suite **265 passing, 0 failures**. iOS parity: needs the same preference +
`CUP_ML = 250` if the feature ships there.

### Verified on-device (28 Jul 2026)
`connectedDebugAndroidTest` on emulator (API 36): **18 tests, 0 failures** — including the two new
hydration regressions (rapid quick-adds sum; form save preserves water) and the previously flaky
`CycleSettingsDialogTest`.

### Note — Google sign-in on debug builds
Running the instrumented suite (and Studio deploys) put the **debug-signed** build on the test
emulator. "Continue with Google" requires the app's signing certificate to be registered in the
Google Cloud OAuth config, and only the **release** certificate is registered — so Google sign-in
fails on debug builds by design, and reinstalls wipe local app data (hence being signed out).
Not an app defect; nothing in today's changes touched auth. Options: register the debug
keystore's SHA-1 as an additional Android OAuth client (one-time, Cloud Console), or test Google
sign-in on release-signed builds only. Email/password is unaffected by signing.

### Pending
Upload code 13 to Internal testing and verify a Play-installed build; resolve the Play URL validator
429 warning for the deletion/privacy routes; verify the Supabase DPA and live end-to-end deletion of
an account containing a pH reading; then request explicit owner approval before Production rollout.

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
- 1.3.1 (12) was built for the broken flow, but the live Play bundle/track audit on 28 Jul found
  neither code 12 nor code 13 uploaded. Internal testing still serves **1.3.0 (11)**. Code 13 is the
  verified replacement artifact and still needs upload and Play-installed verification.

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

The vaginal-pH range and thresholds were approved on 28 Jul 2026: healthy band **3.8–4.5**,
elevated **> 4.5**, input range **MIN 3.8 / MAX 7.0**, step 0.1, slider default 4.2. Genesyx is a
wellness app, not a medical device. See the **Release gates** table below for the pre-ship checklist.

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
1. ~~**Client sign-off of the ranges**~~ — **DONE 28 Jul 2026:** healthy 3.8–4.5, elevated >4.5,
   input 3.8–7.0, default 4.2.
2. ~~**Client sign-off of the user-visible copy**~~ (`domain/ph/PhCopy.kt`) — **DONE 28 Jul 2026.**
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
