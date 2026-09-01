# Changelog

What changed, when, and why. Newest first. One entry per working session.

Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Versions are `versionName (versionCode)`.

---

## [Unreleased] — consent sync matches the live schema — `1.4.2 (23)` (1 Sep 2026, night)

Fixes the smoke-test finding below. **Code 22 stays on Internal testing until this is uploaded;
its archives are untouched.** Artifacts at `~/Documents/Genesyx Releases/1.4.2-code23/`
(`genesyx-1.4.2-code23.aab`, SHA-256 `a9142e99…6eee80`; upload key SHA-1 `8D:EB…CC:73`).

### Fixed — consent events now speak the server's language

The live `consent_events` table (owned by iOS's `20260818_consent_events.sql`) uses
`occurred_at` and a NOT NULL `version`; code 22 sent `recorded_at` and no version, so every
push 400d and each fresh sign-in re-asked the consent question with all health-data pulls
skipped. The DTO now serialises `occurred_at` + `version` — the version is
`ConsentPolicy.WIRE_VERSION = "2026-08-18.v2"`, read from iOS's `ConsentPolicy.currentVersion`
so both platforms stamp one shared consent-copy version (bump them in lockstep). Pushes are
plain **INSERTs** (the table has no UPDATE policy; upsert's conflict branch is refused) and a
duplicate-key 409/23505 on the device-minted id reads as a replayed success — every other
failure still surfaces. The Room entity keeps its `recordedAt` name; only the wire changed.

### Fixed — the undecided-consent state survives process death and is user-scoped

`needsDecision` was an in-memory flag, so a restart forgot "undecided" (which read as
permitted). It is now persisted in DataStore as the uid that owes an answer — a different
account on the device inherits nothing, any recorded answer clears it immediately, and the
sign-out/deletion teardown wipes it with the rest of the prefs file.

### Fixed — the consent ask can only be answered

`ConsentDecisionDialog` no longer dismisses on an outside tap or Back (a stray tap used to
count as "Not now"). "Not now" is the explicit postpone and records nothing; "Allow" records
the grant and the dialog leaves only when the recorded grant flips `needsDecision` — a grant
that failed to record keeps the question on screen.

### Added — granting consent re-runs the skipped sign-in sync

When a sign-in found no consent answer, every health-data refresh was correctly skipped — but
then her server data only appeared after the NEXT sign-in. Granting (from the ask or Profile)
now re-runs the exact post-sign-in adopt-and-pull sequence via
`AuthRepository.resyncAfterConsentGranted()` — guarded to signed-in accounts with collection
actually permitted, and single-flight so a double tap cannot stack concurrent pulls.

### Gate (this tree, 1 Sep 2026)

`git diff --check` clean · **649 unit / 0 failures** (+14: DTO wire shape incl. no
`recorded_at`, insert-replay tolerance incl. schema/RLS/transport failures staying failures,
persisted/scoped/cleared needsDecision, resync guards) · `lintVitalRelease` pass ·
`connectedDebugAndroidTest` **44/44** (+3 `ConsentDecisionDialogTest`; the known
`DailyLogRepositoryTest` pull-vs-push flake tripped one interim run and passed the full one) ·
signed `bundleRelease` + `assembleRelease` green, aapt2 confirms `23 / 1.4.2 / target 36`.

### Verified against production (1 Sep 2026, ~22:10 — signed APK, emulator, throwaway account)

The full consent loop was walked live on the signed code-23 build: sign-up → the ask appeared
on Home; an outside tap and Back both left it standing; **Allow** recorded and pushed —
`consent_events` read back by SQL as `version "2026-08-18.v2", action "granted"` (the code-22
push 400d here); the dialog closed itself once the grant landed; a supplement logged and
pushed; sign-out → sign-in **adopted the server trail (no re-ask), ran every pull unrefused,
and restored the logged supplement from the server**; in-app deletion left every server count
at zero — consent_events included, so the updated `delete_current_user` covers it. The
code-22 loop (re-ask every sign-in, pulls skipped) is confirmed gone.

---

## Smoke test of the code-22 release build — `1.4.2 (22)` (1 Sep 2026, evening)

**Code 22 was uploaded to Play Internal testing earlier today**, then the release APK from
`~/Documents/Genesyx Releases/1.4.2-code22/` (hash-verified, same code Play re-signs) was
smoke-tested on the Pixel 8 / API 37 emulator with a real throwaway account against production
Supabase. The account was created, exercised, and deleted through the app; the full server-side
walk is recorded in the git-excluded `CHANGES.md` at the repo root ("Smoke test on the release
APK"), where working notes live because this repository is public.

### Verified working on the release build ✅

- **Launch** clean (R8/PROD), and the new minimum-version gate correctly **fails open** while
  `app_config` is undeployed.
- **Expired reset link** (`genesyx://reset-password#error=otp_expired…`) opens the new
  "Set a new password" screen with honest copy and a working "Request a new link" path — the exact
  fragment that crashes supabase-kt's own handler.
- **Sign-up → sign-in round trip**: `handle_new_user` still auto-creates the profile after the
  server-side RPC hardening; the typed display name survives a sign-out/sign-in.
- **Track → pH → Track** returns to Track (the code-17 defect stays fixed); pH shows the required
  sync-disclosure copy.
- **Clear-sync end-to-end (the codes 19–21 defect class)**: un-ticking the LAST logged supplement
  reached production as an explicit empty list — the row read back by SQL as
  `symptoms:[], water_ml:0, supplements:[], food_groups:[], notes:null`.
- **Account deletion**: in-app flow → `delete_current_user` → service-role counts all zero
  (auth user, daily_logs, profiles, ph_readings, quiz_answers, consent_events).

### Found broken 🔴 — fix scheduled as `1.4.2 (23)`

**Consent-event sync does not match the live `consent_events` schema.** The table (owned by the
iOS repo's `20260818_consent_events.sql`) has columns
`(id, user_id, version text NOT NULL, action, occurred_at)`; the Android DTO sends `recorded_at`
and omits `version`, so every push is rejected by PostgREST ("Could not find the 'recorded_at'
column"). Live effect on a fresh sign-in: the server trail reads empty → the first-time consent
ask appears **and all health-data pulls are skipped for that session** — and because the answer
never lands server-side, this repeats on every sign-in. Three smaller wrinkles to fix with it:
`needsDecision` is in-memory only (a process restart forgets "undecided"), the ask dialog
dismisses on an outside tap, and granting after sign-in does not re-run the skipped refreshes.
Client-side fix for code 23: `recorded_at`→`occurred_at`, send `version` (match iOS's constant),
and push with **insert + duplicate-key tolerance** instead of upsert — the table deliberately has
no UPDATE policy, so an upsert's conflict branch is refused (per iOS `RemoteModels.swift`).

**Not covered here** (needs a physical tester device / ops items): Google Sign-In, the reset
email end-to-end (the `genesyx://reset-password` Supabase allow-list entry is still not added),
and a true Play-track install.

---

## [Unreleased] — launch-readiness pass: legacy-pH correctness, logging hygiene, minimum-version gate — `1.4.2 (22)` (1 Sep 2026)

Release-preparation audit over the tree below, then the authorised bump. Two health-data
defects fixed, one defensive feature added, and the full release gate re-run green. File-by-file
breakdown in `CHANGES_ANDROID.md` (repo root). Unit suite 623 → **635, all green**; connected
41/41; lint-vital, debug and signed release builds all pass.

### Fixed — legacy urine pH rows are no longer re-validated against the vaginal range

`PhRepository` range-checked every edit against the vaginal 3.8–7.0 window regardless of the
row's `measurementType`, so even a notes-only edit to a legacy urine row (a different scale,
predating the vaginal-only slider) could be refused. Urine rows now skip the check entirely —
only vaginal readings are range-checked — and the class doc records the rule. Two tests added
to `PhRepositoryRangeTest`.

### Fixed — pH values and user UUIDs no longer reach release Logcat

`AndroidLogger.w` was not debug-gated, and several repositories interpolated the raw `userId` —
one logged the failing pH **value** itself — into lines that survive into release builds.
Failure logs now carry event names and aggregate counts only ("rejected an out-of-range vaginal
pH value (allowed 3.8..7.0)"), the rejected value is deliberately never logged, and the
DEV-gated detail path is unchanged. No tokens, emails, IDs or health values are written to
release Logcat.

### Added — minimum-version gate (fail-open)

At startup the app anonymously reads `app_config.min_supported_build` (explicit columns,
`limit 1`) and compares it numerically against `BuildConfig.VERSION_CODE`. Below it, a
non-dismissible `UpdateRequiredScreen` offers the exact production Play listing with an HTTPS
browser fallback. A missing table, missing row, network failure or malformed value all
**fail open** — the app launches normally, because a gate that blocks launch while its own
backend is absent is worse than no gate. Decision logic is unit-tested for
below/equal/above/missing/malformed/error (10 tests). **Ops: the `app_config` table is
deliberately NOT deployed yet — the gate is inert until it is, and it must not be populated
without deciding a real minimum first.**

### Changed — versionCode 21 → 22 (authorised); versionName stays 1.4.2

Play Console confirmed 21 exists and is Active (uploaded 26 Aug 2026), so 22 is the next upload
identity — as the entry below predicted. The only repo change for it is the one-line
`versionCode` bump in `app/build.gradle.kts`. Full gate re-run on the 22 tree:
`git diff --check` clean; **635 unit tests / 0 failures**; `lintVitalRelease` pass;
`connectedDebugAndroidTest` **41 / 0 / 0 / 0** (the `DailyLogRepositoryTest` timing flake did
not recur — no rerun, no test weakened); signed `bundleRelease` + `assembleRelease` pass.
AAB/APK archived as `genesyx-1.4.2-code22-launchready.{aab,apk}` with SHA256SUMS under
`~/Documents/Genesyx Releases/1.4.2-code22/`; the code-21 archives are untouched. Signing
certificate unchanged. **Note:** the machine's only AVD reports API 37, not 36 — the same
emulator every prior instrumented run used, and API 37 ≥ targetSdk 36.

### Deferred — recorded, deliberately not in this upload

Poison-row dead-lettering for the sync queues (needs a Room migration on a live app) and
incremental pull cursors (server `updated_at` monotonicity unverified from here). Both sit in
`CHANGES_ANDROID.md` with the rest of the audit state, alongside the outstanding owner items
(the Supabase redirect allow-list entry for `genesyx://reset-password`, the `app_config`
deployment, and a physical-device smoke test of the new gate).

---

## [Unreleased] — post-launch-audit patch: recovery, error kinds, teardown, consent sync, cycle queue (1 Sep 2026)

Five audit findings fixed in one pass, on top of the `1.4.2 (21)` tree. **The versionCode has NOT
been bumped — 21 is the already-built release-candidate identity, so the next upload from this
tree must be 22.** File-by-file breakdown per task in `CHANGES.md` (repo root). Unit suite
588 → **623, all green**; debug, androidTest and release variants all compile.

### Added — password reset now completes in-app (P0)

`resetPasswordForEmail` sends `redirectUrl = genesyx://reset-password` (the exact URL iOS uses, so
one Supabase allow-list entry covers both platforms), the manifest registers the deep link, and a
new `ResetPasswordScreen` imports the recovery session (`parseSessionFromUrl` → `retrieveUser` →
`importSession` — supabase-kt's own `handleDeeplinks` throws on an expired-link fragment, so the
link shape is checked first via `RecoveryLink.carriesSession`) and asks for the new password.
Expired/used links get honest copy and a "Request a new link" path. "Forgot password?" now carries
a 60 s cooldown (300 s once the server says rate-limited) — under the 2-emails/hour auth throttle,
the old "Please try again" with a hot button invited burning the whole budget.
**Ops: `genesyx://reset-password` must be on Supabase Auth → Redirect URLs or `redirect_to` is
silently dropped.**

### Changed — auth failures are mapped once, in the service layer (P1 #4/#5)

`AuthErrorKind` (RATE_LIMITED / OFFLINE / INVALID_CREDENTIALS / EMAIL_NOT_CONFIRMED / UNKNOWN) is
attached to every failed auth call; no `t.message` reaches a composable any more (the delete
dialog used to render raw Postgrest text, and a confirmation-gated sign-up leaked a developer
string). Re-auth failures in change-password/change-email only say "Current password is incorrect"
on an answered rejection — a throttled or offline re-auth keeps its own copy. All new copy in
`strings.xml`, British English.

### Fixed — account deletion teardown (P1 #9/#10)

A deletion retry that finds the account already gone (errcode 28000 "no authenticated user", or
401 from the dead refresh token) now reads as success, so the local wipe — previously unreachable
on that path forever — finally runs. The wipe itself is one awaited, sequential
`AuthRepository.wipeLocalState()` shared with sign-out: cancel reminder + supplement + sync-drain
workers (the drain schedulers gained `cancel()`), clear Room, then clear the ENTIRE
`genesyx_prefs` DataStore — focus mode, derived cycle phase, hydration goal, article history and
every `notif_*` key used to survive deletion and be inherited by the device's next user.

### Fixed — consent follows the account, not the install (P1 #8)

`consent_events` now syncs: pull-merge-push on sign-in (append-only both ways, newest event by
real timestamp wins), running FIRST in the post-sign-in sequence. A reinstall can no longer
silently reverse a withdrawal recorded on the server. When a completed pull confirms no answer
exists anywhere, Home asks (new `ConsentDecisionDialog`) instead of assuming permitted; "Not now"
records nothing. The daily-log drain is consent-gated like pH/supplements (it was the one drain
that kept uploading after a withdrawal). **Ops: assumes server table
`consent_events(id uuid PK, user_id uuid, action text, recorded_at timestamptz)` with owner RLS —
verify before release.**

### Fixed — cycle settings can no longer lose an offline edit; guest data follows her in (P1 #6/#7)

Cycle settings now carry the owed-write contract (DataStore flag, like quiz answers): a save whose
push failed is re-sent by the next refresh BEFORE the pull, and a failed re-send aborts the pull
instead of stamping the stale server copy over her edit. Chosen over a Room syncStatus column to
avoid a v10→v11 migration on a live app. Guest daily logs and guest cycle settings are adopted
onto the account at sign-in (before the refresh/clear that used to lose them), riding the ordinary
push queue. The three callers that discarded `SaveOutcome` (Home, Track, Cycle detail) now surface
it — the dialog closes only on a confirmed save.

---

## [Unreleased] — `food_groups` is live too — `1.4.2 (21)` (26 Aug 2026, night)

**Code 20 is superseded — never upload it.** A release-gate investigation before the code-20 upload
found that the one field 20 deliberately left unfixed, `food_groups`, had been left unfixed on a
premise that is false. This entry corrects it.

### Fixed — un-ticking the last food group never reached the server either

Same defect as the supplements bug in 20, one field later: `food_groups` sat at its `emptyList()`
default, the serializer dropped it, PostgREST's derived `columns=` never named it, the row was
still marked `SYNCED`, and the next pull brought the groups she had just un-ticked straight back.

Code 20 declined to force it on two grounds. Both were wrong.

**"The column might not exist."** The evidence for that was `docs/schema.sql` — which is a dated
Lovable extraction, not a live read, and is **not** the authority on server state. This repo
deliberately holds no executable migrations (`docs/worklog/2026-08-13.md`, "Carried constraints");
the shared-backend repo does, and it has
`supabase/migrations/20260812_daily_logs_food_groups.sql`:

```sql
alter table public.daily_logs
  add column if not exists food_groups text[] not null default '{}';
```

The applied-state audit (`TESTFLIGHT_B18.md`, pre-flight row 3) records it **applied to production
13 Aug 2026** and read back from `information_schema.columns` as `ARRAY / NO / '{}'::text[]` —
identical to `symptoms` and `supplements`, which is exactly what the migration's own verify block
demanded. Two further corroborations in `HANDOFF.md` (one of which explicitly warns that the stale
"verified MISSING" row in the same file is not the authority). Naming the column is therefore as
safe as naming the two beside it.

**"Android has no editor, so it can never clear it."** `ui/nutrition/FoodGroupSection.kt`'s chips
are `.clickable { onToggle(group.raw) }` and call `DailyLogRepository.toggleFoodGroup`, from both
the Nutrition tab and the Log form — since `edd8f2d`, 17 Aug 2026. The claim came from a stale KDoc
on `upsertPreservingWater` still saying "only iOS can log them"; that comment is now corrected.

The forced set is now five. `food_groups` is `text[] NOT NULL`, which is why the Kotlin type stays
non-nullable: it sends `[]`, never `null`. `sexual_activity` remains the one field that must never
be forced (`boolean NOT NULL DEFAULT false` — an explicit null fails the whole upsert).

### Tests

Two assertions **inverted** — both had the bug written down as a requirement, in `DailyLogDtoTest`
and `DataContractTest`. That makes two inversions across the pair of fixes: `water_ml` in 20,
`food_groups` in 21. The whole-body verbatim assertion was kept verbatim, not softened to a
`contains`, and now reads:

```json
{"user_id":"user-a","date":"2026-08-10","symptoms":[],"water_ml":0,"supplements":[],"food_groups":[],"notes":null}
```

588 unit / 0 failures / 76 classes, 41 instrumented, release build green. Mutation-tested: removing
the new annotation fails exactly four tests, then restored and re-run green.

### Release build (26 Aug, 19:44) — code 21, the upload

`versionCode` 20 → **21** (`versionName` 1.4.2), built from `3f724e3` after `:app:clean`. aapt2
`com.genesyx.app / 21 / 1.4.2 / targetSdk 36`; upload-key SHA-1 `8D:EB…CC:73` verified on the AAB
(`keytool`) and the APK (`apksigner`). Archived at `~/Documents/Genesyx Releases/1.4.2-code21/` —
AAB SHA-256 `2f24e509…ec6b9d`, 17,187,693 bytes; `SHA256SUMS.txt` verifies.

**Outstanding, and not closable from here:** the end-to-end round-trip on a real signed-in account
is still unproven for both fields. `SignInLocally`/`SeedTestData` live in `androidTest`, which
builds only against the debug variant, so they cannot seed a release APK, and the QA emulator
session's non-UUID user id 400s every push by design. The payload shape is proven; the server
acceptance is not. Walk it once on the Play-installed build — un-tick the last supplement **and**
the last food group, force-stop, relaunch, pull.

---

## [Unreleased] — the clear has to reach the column — `1.4.2 (20)` (26 Aug 2026, late evening)

**Code 19 is superseded — never upload it.** A read-only QA pass on the code-19 build found a
silent data-loss defect on the wire, so 19 never went to Play. This entry is the fix.

### Fixed — un-logging the last supplement never reached the server

The serializer runs with `encodeDefaults` off (supabase-kt's config), so a property equal to its
declared default is dropped from the JSON body — and PostgREST builds its `columns=` list from that
body. A dropped property is a column the upsert **never writes**. `DailyLogDto.supplements`
defaulted to `emptyList()`, so un-ticking her last supplement produced:

```
tick Zinc                          columns=user_id,date,supplements
untick Zinc, one still logged      columns=user_id,date,supplements
untick the last one                columns=user_id,date          ← supplements gone
```

The row was still marked `SYNCED`, so `refresh`'s "skip rows with unsynced local changes" guard did
not protect it, and the next pull brought the stale server list back: **supplements she un-logged
reappeared on her own device.** `symptoms`, `water_ml` and `notes` had the same defect.

The fix is per-property `@EncodeDefault(EncodeDefault.Mode.ALWAYS)` on those four — deliberately
**not** a global `encodeDefaults = true`, which would have changed all seven DTOs at once (the
Supabase client installs no custom serializer). The field-by-field reasoning now lives in
`DailyLogDto`'s KDoc, because the rule is not "force everything":

| field | forced? | why |
|---|---|---|
| `symptoms`, `water_ml`, `supplements`, `notes` | yes | each has a real clear-to-default path in the UI, and each is in `docs/schema.sql` — naming it can never fail |
| `mood`, `energy`, `sleep_minutes` | no | the form only ever *sets* these; with no deselect they cannot reach their `null` default |
| `food_groups` | **no — same defect, left unfixed on purpose** | ~~it is not in `docs/schema.sql`'s `CREATE TABLE`~~ — **this reasoning was wrong and was reversed in 21 (above): `docs/schema.sql` is a dated snapshot, and the column has been live since 13 Aug 2026** |
| `sexual_activity` | **never** | the column is `boolean NOT NULL DEFAULT false`; forcing the `null` default would send `"sexual_activity":null` and fail the whole row. Its clear is `false`, which already encodes on its own |

**Why no test caught it:** `DailyLogRepositoryToggleTest` asserts the empty list reaches a *fake*
remote, and it always did. The repository was right; the bug lived one layer below, in
serialization. `DailyLogDtoTest` now pins the actual JSON — including a `wireKeys()` assertion on
the exact column set a fully cleared row names — and `DataContractTest` had one assertion
**inverted**: it read `assertFalse(encoded.contains("water_ml"))`, which had written the bug down
as a requirement.

Verified on the wire, not just in tests: on-device, un-ticking the final supplement now sends
`columns=user_id,date,symptoms,water_ml,supplements,notes`, and the clear survives a force-stop.

### Fixed — "coq10" on top of "CoQ10" made a row she could never see

`SupplementToggleSet.build()` dedupes custom entries against the plan and each other, so a
duplicate name was saved to Room, pushed to the server, and then **rendered nowhere** — no chip, no
checklist row, no way to remove it. Writes now refuse a name already in the set via the same
canonical identity rule (`SupplementToggleSet.namesSomethingIn`, trimmed + case-insensitive), with
a new `SupplementWriteResult.Duplicate` and the sheet's existing error line: *"coq10" is already in
your list.* Typing a bundled essential's name ("folate") is refused for the same reason; "Iron",
which is outside the plan, still saves. The check reads the committed rows
(`UserSupplementDao.liveFor`), not a collected flow, so two fast taps of Add cannot race past it.

**Still open, deliberately not invented here:** custom entries have no edit or delete affordance in
the UI. `UserSupplementRepository.delete(id)` already exists and soft-deletes correctly, so this is
a UI-only gap — but destructive UI is a product decision, not a bug fix.

### Fixed — checkbox and label were glued together

The Track → Nutrition "LOG SUPPLEMENTS" rows had no horizontal gap between the tick box and the
supplement name.

### Tests

588 unit (was 579; +10 wire-format, +4 identity/guard, +4 duplicate), 41 instrumented, release build
green. Each new guard was mutation-tested — the annotation, the identity rule and the duplicate
check were each broken in turn to confirm the tests fail, then restored.

The tenth wire-format test asserts the **whole body verbatim**, not a `contains` that could pass on
a substring. A fully cleared row serializes through `DailyLogDto.serializer()` to exactly:

```json
{"user_id":"user-a","date":"2026-08-10","symptoms":[],"water_ml":0,"supplements":[],"notes":null}
```

Noted for a separate fix: on a cold emulator `DailyLogRepositoryTest` hangs exactly one test per
run at its 20-second flow-poll helper, a different test each time. It uses a fake in-memory remote
that never serializes a DTO, so it is a test-harness race, not a product defect.

---

## [Unreleased] — Track → Nutrition logs supplements again — `1.4.2 (19)` (26 Aug 2026, evening)

### Fixed — the Track → Nutrition tracker was a dead-end summary

Owner review before the code-18 upload: from Track → Nutrition she could *see* today's supplements
and the week's dots but could not log anything or reach the plan. That was PR #20 following the
brief's "§2 — read-only summary" wording literally; the pre-PR #20 screen had a "Log supplements"
button (opened the Log screen). Confirmed on the emulator: nothing tappable top to bottom.

The tracker now carries the logging surface itself, between the two TODAY cards:

- **"LOG SUPPLEMENTS"** — one row per entry **by name with its dose**, a checkbox each: the four
  essentials, then **"Your supplements"** (her own entries; an honest "None added yet — add your own
  from the supplement plan below" when there are none). Ticking a row calls the same
  `DailyLogRepository.toggleSupplement()` as the Nutrition tab's chips — one repository, one result
  contract — so today's card, the week dot, the Nutrition card and Insights all move at once, and
  refused / queued / failed saves surface in a snackbar (Retry on Failed). Status line is the
  shared "N of M logged today".
- **"Supplement plan"** button (+ "Review the essentials, set reminders, add your own") opens the
  **same `SupplementPlanSheet`** the Nutrition tab opens, in place — Back still returns to Track.
  `NutritionDetailViewModel` gains `UserSupplementRepository` + `SupplementReminderRepository` for
  the sheet's list, bells and add form; `TrackerDetailScaffold` gains a `snackbarHost` slot.
- Summary cards and week strips unchanged.

### Tests
- `NutritionInsightsSharedRepositoryTest` +1: the tracker's checklist toggle is read back by the
  tracker's own today card, the Nutrition tab and Insights over one repository.
- `TrackNutritionLoggingTest` (instrumented, real NavHost + bar): Track → Nutrition row → the four
  names present → tick Zinc → today's card names it → "Supplement plan" opens the sheet ("+ Add your
  own supplement") → un-tick → Back lands on Track. Green on emulator-5554 (2.5 s).
- Unit suite 570 tests. One pre-existing test (`PreferencesRepositoryTest.a glass below the floor is
  clamped`, from 12 Aug) timed out once while the emulator was booting alongside; passes alone.

### Release build (26 Aug, 17:02) — code 19, the upload
`versionCode` 18 → **19** (`versionName` 1.4.2). `:app:testDebugUnitTest` 570/0/0, then
`:app:bundleRelease :app:assembleRelease` from a clean tree. Checked against the built artifact:
aapt2 `com.genesyx.app / 19 / 1.4.2 / targetSdk 36`; upload-key SHA-1 `8D:EB…CC:73` (`apksigner` V2
on the APK, `keytool` on the AAB); R8 mapping retains `NutritionDetailViewModel` /
`SupplementToggleSet`. Archived at `~/Documents/Genesyx Releases/1.4.2-code19/` — AAB SHA-256
`a23886b5…baa18` (17,186,919 bytes), `SHA256SUMS.txt` verifies, `BUILD_NOTE.txt` beside it. Code
18 is superseded (read-only tracker); never upload it.

---

## [Unreleased] — Track → pH → Track navigation fix; release candidate becomes `1.4.2 (18)` (26 Aug 2026, 16:05)

**The bug (release-blocking, confirmed on the code-17 build before fixing):** Track → "Vaginal pH"
opened the pH screen (`tracker/ph`), but tapping **Track** in the bottom bar did nothing — pH stayed
visible with the pH tab highlighted; Android Back revealed Track, then Home. Root cause: `tracker/ph`
is a bottom-tab root (client-requested pH tab, 12 Aug), but Track's row (`TrackScreen.onNavigate`)
and Home's pH nudge card (`openTrackerDetail`) plain-pushed it: `home → track → tracker/ph`. The next
Track tap ran the shared tab switch (`popUpTo(Home) { saveState = true }` + `launchSingleTop` +
`restoreState`), which saved and restored that whole chain with pH on top — the SFM-27 mechanism, one
route over.

**The fix (`9af9501`):** every link that targets a tab root now goes through `navigateToTab`:
- `TabNavigation.tabForRoute(route)` — new pure lookup: the owning tab, `null` for pushed
  destinations (path match, so `nutrition?plan={plan}` still resolves).
- Track's row handler routes via `tabForRoute` → tabs switch, everything else stays a plain push —
  Cycle/Nutrition/Symptoms/Sleep/Hydration keep their pushed-detail behaviour (Back returns to Track).
- Home's pH nudge card calls `navigateToTab(Screen.PhDetail)`; `openTrackerDetail` now serves
  Hydration only.
- Insights → pH uses the same helper (was an inline copy of the options; behaviour identical).

**Deliberate behaviour note:** Back from the pH *tab* lands on Home (its parent in the graph),
matching Insights → pH and the article-CTA → pH paths. Pinned by
`PhTabNavigationTest.back_from_the_ph_tab_lands_on_home`.

**Tests:** `TabNavigationTest` +3 (`tabForRoute` contract — resolves all seven tabs incl. patterns,
rejects pushed detail routes). New `PhTabNavigationTest` (5 instrumented, real NavHost + bottom bar):
Track→pH→Track; Home→pH→Track (no `track` entry beneath pH); Insights→pH→Track; no duplicate pH roots
across repeated switches + reselect; the five detail rows still push and return; Back-from-pH lands
Home. Unit suite **569/569**; instrumented **40/40** on emulator-5554 (the pre-existing
`CycleSettingsDialogTest` flake did not appear).

**Release:** `versionCode` 17 → 18 (`versionName` stays "1.4.2"). Code 17 was consumed by the Play
internal-testing release flow, so Play requires a new, higher code — and code 17 carries this
confirmed defect, so it is **superseded: never upload `1.4.2-code17/`**. Code-18 artifacts archived
in `~/Documents/Genesyx Releases/1.4.2-code18/` (AAB SHA-256 `222fb746…9c5ae0`, 17,156,938 bytes;
upload-key SHA-256 `C3:D5…C1:7D` on both files; aapt2 `com.genesyx.app / 18 / 1.4.2 / target 36`;
`SHA256SUMS.txt` verifies). No Play upload, no Console changes.

---

## [Unreleased] — release candidate becomes `1.4.2 (17)` (26 Aug 2026, 14:08)

`84ebbc7` bumps `versionCode` 16 → 17 and nothing else. The AAB/APK in
`~/Documents/Genesyx Releases/1.4.2-code17/` were built at 14:04 from exactly this tree
(`2ec3fc5` + the bump): aapt2 `com.genesyx.app / 17 / 1.4.2 / targetSdk 36`; upload-key SHA-1
`8D:EB…CC:73` on both files (`keytool` on the AAB, `apksigner` V2 on the APK); archived AAB is
byte-identical to `app/build/outputs/bundle/release/app-release.aab` (SHA-256 `6f3a23a9…656df`,
17,160,086 bytes); `SHA256SUMS.txt` verifies.

**Why 17, when 16 never reached Play:** four different bundles carried the filename
`genesyx-1.4.2-code16.aab` on 26 Aug (00:03, 11:54, 12:12, 13:59), which had already produced one
wrong-artifact scare. A clean identity in its own directory removes the footgun; Play does not need
contiguous codes. The Play Console was **not** consulted — nothing in the repo can say whether a 16
bundle was uploaded, and 17 is safe either way. Every earlier "16 is the identity to upload" line in
this file is history.

---

## [Unreleased] — consent gate covers supplements; Auto Backup scoped explicitly — `1.4.2 (16)` rebuilt (26 Aug 2026, later)

Play-submission prep: making the code say plainly what the Data Safety and Health apps declarations
will have to claim, so the two can't drift apart.

**Version identity unchanged; the code-16 bundle was rebuilt a second time to carry this** (owner's
call). The alternative was holding these back for a code-17 Production build, which would have meant
Internal testing and Production running different apps while the declarations described only one of
them. Rebuilding in place is legitimate **only because code 16 has never been uploaded** — the moment
it lands on Play, every further change needs a version bump.

> **Three bundles have now held the filename `genesyx-1.4.2-code16.aab` in a single day.** Only the
> 12:12 one (`3ba7d6e1…`, from `67b224d`) is the upload; the earlier two are parked under
> `superseded/`. Check `SHA256SUMS.txt`, not the filename. Do not rebuild into `1.4.2-code16/` again.

### Changed — `user_supplements` writes now go through the Article 9 consent gate

`UserSupplementRepository` was the last of the five health stores still outside
`HealthDataCollectionGate`; its KDoc said it was "cloned from `PhRepository`'s proven shape", which
was true of the sync machinery but predated the gate. It now matches `PhRepository` on all four
collection paths: `write` refuses, `syncPending()` no-ops, `adoptGuestEntries()` adopts nothing, and
`refresh()` returns before pulling. PR #20 had just widened this surface with the sheet's "Add your
own supplement" form, so it was worth closing now rather than after the upload.

- **The refusal is visible, per `ba9380a`'s rule that nothing is ever fake-saved.** `create`/`update`
  return a new `SupplementWriteResult.Refused` alongside `Accepted` / `InvalidName`; the sheet shows
  "Not saved — health data collection is off. Turn it on under Profile → Health data consent." and
  **leaves the form fields filled**, so she can flip consent back on and tap Add without retyping.
- `write` became a suspend `scope.async { … }.await()` — the same application-scope write as before,
  but awaited, because a result the caller renders has to be the real one.
- `syncPending()` returns **`true`** when refused, not `false`: a queued row must not upload after a
  withdrawal, and reporting failure would just make WorkManager retry the same refusal forever.
- **`delete` stays ungated, on purpose.** Deleting is her exercising control; requiring consent to
  remove data would be backwards. `PhRepository` makes the same exception.
- Callers threaded through: `NutritionViewModel.saveSupplement` is suspend, `addFromCatalogue` wraps
  in `viewModelScope`, `SupplementPlanSheet` takes a suspend lambda and launches from
  `rememberCoroutineScope()`, `NutritionScreen` supplies the scope.

### Changed — Auto Backup rules are now written out instead of left to the defaults

`backup_rules.xml` (API ≤ 11) and `data_extraction_rules.xml` (API 12+) were both empty files. They
now state the intended scope explicitly, so "a guest's tracking stays on her device" is enforced by
the manifest rather than by convention.

- **Cloud backup** excludes the local health stores: `genesyx.db` — plus `-wal` and `-shm` **by
  name**, since SQLite keeps the write-ahead log beside the database and a copy of the `.db` alone
  would be an inconsistent snapshot — the DataStore prefs file, and the shared-prefs file that
  supabase-kt's default `SettingsSessionManager` owns. That last one is not ours and is easy to miss:
  multiplatform-settings' no-arg `Settings()` resolves to `"${packageName}_preferences"`. Confirmed
  by reading the library source out of the Gradle cache rather than assuming.
- **All three are listed together, and that is the point.** Scoping the data files without the
  session file would restore an app that believes it is signed in with nothing behind it. As written,
  a cloud restore is a clean install: signed out, and signing in pulls the account's data back from
  Supabase.
- **`<device-transfer/>` is deliberately left intact.** It is a direct handset-to-handset migration,
  nothing goes to a server, and restricting it would silently wipe a guest's whole history when she
  upgrades her phone with no way to get it back. `[OWNER]` — the two lists differing is a judgement
  call worth confirming.

Both files carry comments explaining why the lists differ, because otherwise it reads as an oversight
and someone will "tidy it up" into agreement.

### Tests

Unit: **565 passing, 0 failures, 0 skipped** (was 560; +5 in `ConsentGateTest`, whose doc now reads
"five health stores"). The new cases: a refused write says so, a refused refresh pulls nothing, a
queued row is not uploaded after withdrawal (asserts `syncPending()` returns `true` **and** that the
DAO's `pending()` is never called), guest adoption refuses, and — the deliberate exception — a
`delete` still reaches the server.

A green build alone does not prove a raw XML resource is well-formed for its schema, so both rule
files were read back out of the compiled artifact rather than trusted from source.

### Release build (26 Aug, 13:59) — code 16, fourth; supersedes the 12:12 build

Rebuilt from `2ec3fc5` (= `67b224d` + the Track-row toggle-set fix found while testing Track →
Nutrition on the emulator). Same checks as the 12:12 build: aapt2 `com.genesyx.app / 16 / 1.4.2 /
targetSdk 36`; upload-key SHA-1 `8D:EB…CC:73` on AAB and APK; R8 mapping retains
`SupplementToggleSet` and `TrackerSummaryLogic`. Archived as
`~/Documents/Genesyx Releases/1.4.2-code16/genesyx-1.4.2-code16.aab` — SHA-256 `dfee0b51…5ceb0`
(17,160,089 bytes), `SHA256SUMS.txt` verifies, `BUILD_NOTE.txt` names the commit. The 12:12 build
(`3ba7d6e1…`) is parked in `superseded/` as `*.pre-2ec3fc5.*`. **Upload by hash, not by name.**

### Release build (26 Aug, 12:12) — code 16, third (superseded at 13:59)

`:app:testDebugUnitTest` then `:app:bundleRelease :app:assembleRelease` from a clean tree at
`67b224d`. Everything below was checked against the built artifact, not the source:

| What | How | Result |
|---|---|---|
| Identity | `aapt2 dump badging` | `com.genesyx.app / 16 / 1.4.2 / targetSdk 36` |
| Signing | `apksigner verify --print-certs`; `keytool -printcert -jarfile` on the AAB | SHA-1 `8D:EB…CC:73`, SHA-256 `C3:D5:1F:4B…A4:46:C1:7D` — the registered upload key, on both files |
| Backup rules present | `aapt2 dump xmltree` on the obfuscated resources | Five `<exclude>` in `full-backup-content`, five in `cloud-backup`, `device-transfer` empty |
| Consent refusal present | `strings` on `classes.dex` | The refusal copy is there. One hit, not two — R8 folds the identical constant used by the sheet and by `SupplementSaveEvent` |
| PR #20 present | same | `SupplementToggleSet` |
| pH copy current | same | `"Vaginal pH tracking"` absent, `"legacy reading"` present |
| Archive | `shasum -a 256 -c` | AAB `3ba7d6e1…9a480` (17,158,664 bytes), APK `776ab211…5880d` |

Two traps worth writing down, both of which cost time today:

- **A release build obfuscates resource filenames.** `aapt2 dump xmltree <apk> --file
  res/xml/backup_rules.xml` returns *nothing* and reads as "the file isn't in there". Resolve the id
  first — `aapt2 dump resources <apk> | grep -A2 xml/backup_rules` → `res/Qq.xml` — then dump that.
- **`SHA256SUMS.txt` is the identity, the filename is not.** Rebuilding in place at code 16 twice
  left three distinct bundles that have all been called `genesyx-1.4.2-code16.aab`. The two dead ones
  are now under `superseded/` with descriptive suffixes so the Play Console file picker can't offer
  them, and `BUILD_NOTE.txt` names which is which.

---

## [Unreleased] — Nutrition tab fix + inline supplement logging (SFM-27 / SFM-28) — still `1.4.2 (16)` (26 Aug 2026)

Version identity deliberately **not** bumped: code 16 is built and signature-verified but not yet on
Play (Internal serves 1.3.0 (11)). Whether this lands as a rebuilt 16 or as 17 depends on whether 16
is uploaded first — the owner's call, checked against the Play Console, not guessed here.

### Fixed — the Nutrition bottom tab looked dead (SFM-27)

- **Root cause, reproduced on the emulator before touching code.** Not a missing or mismatched
  route: a fresh process → Home → Nutrition worked. Nutrition's *"See all articles"* did a plain
  `navigate(Screen.Learn.route)`, pushing the **Learn tab root on top of Nutrition**
  (`[home, nutrition, learn]`). The next tab switch popped that chain with `saveState`, keyed under
  Nutrition's destination, and every later Nutrition tap `restoreState`d it **with Learn on top** —
  "nothing happens" from Learn, a jump to Learn from anywhere else, and it stuck for the life of the
  process. Back from that state revealed Nutrition underneath, then Home.
- **`ui/navigation/TabNavigation.kt`** — one `NavController.navigateToTab(tab)` (popUpTo Home +
  saveState, launchSingleTop, restoreState) used by the bottom bar **and** by any in-screen link that
  targets another tab; `routeFor()` strips a tab's optional-argument pattern to the bare path;
  `isTabRoot()` selects from the registered pattern. "See all articles" now switches tabs properly.
- **Re-tapping the selected tab scrolls to the top** instead of being a no-op: the bar bumps a
  counter in the tab entry's `SavedStateHandle` (`TabNavigation.RESELECT_KEY`); Nutrition collects it
  and `animateScrollTo(0)`. Never stacks a duplicate.
- Pinned by `TabNavigationTest` (JVM) and `NutritionTabNavigationTest` (instrumented, walks the exact
  path above against the real NavHost + bar; asserts the route, no Back, one Nutrition entry).

### Added — inline supplement logging on the Nutrition tab (SFM-28a)

- **"Your supplement plan" card is now first** under the header and no longer gated on a cycle being
  set up. Four tappable chips F O D Z (44dp, `toggleable` with Checkbox semantics: "Folate, logged
  today"); a tap logs/un-logs that supplement for **today** and the chip fills from the row Room
  emits. Status line is iOS's: "None logged yet today" / "N of M logged today".
- **`DailyLogRepository.toggleSupplement()`** — the first *reporting* daily-log write. Returns
  `LogWriteResult`: `Saved` (server confirmed / guest local), `Queued` (Room written, push failed →
  WorkManager retry), `Refused` (consent withdrawn, nothing written), `Failed` (local write threw).
  Runs in the application scope so a screen leaving mid-toggle can't strand a PENDING row. Un-logging
  the last item writes and pushes an **empty list** — an explicit clear, the iOS §5 lesson.
- **Snackbar on anything but a clean save** (`SupplementSaveEvent`): refused → consent copy; queued →
  "saved on this device — it'll sync when you're back online"; failed → "Couldn't save X. Nothing was
  changed." with **Retry**. Nothing is ever fake-saved.
- **Supplement Plan sheet** (`SupplementPlanSheet.kt`, replaces the AlertDialog): GENESYX ESSENTIALS
  (name + dose range, benefit line, a **bell** that sets a daily local reminder — stored under
  `plan:<id>` in `SupplementReminderRepository`, which `reconcile()` now keeps) and YOUR SUPPLEMENTS
  (list + Name / Dose / Time form, "+ Add your own supplement" disabled until Name is non-empty,
  saves to `user_supplements`). Added entries **join the chips and the denominator**: "N of 5", and
  each of her own rows carries **the same reminder bell** as the essentials (owner request, later
  the same day) — keyed by the entry's id in `SupplementReminderRepository`, so it is the one
  reminder the tab's "Your supplements" card already shows for that entry.
- **`SupplementToggleSet`** (`domain/model`) — the one definition of "the set and its counts" for
  the plan card, the Track summary and Insights; custom entries dedupe against plan wire/display
  names. Replaces `SupplementPlanProgress`. `Supplement.fromWire` is now trimmed + case-insensitive
  so every reader of `daily_logs.supplements` matches the same rows; `Supplement.chipInitial` keeps
  "D" for Vitamin D.
- Hydration card gains **"Track ›"** (opens the Hydration detail); the **7-day hydration challenge**
  card (extracted to `ui/components/HydrationChallengeCard.kt`, shared with Home) sits under it.

### Changed — Track → Nutrition tracker is iOS's read-only summary (SFM-28 §2)

`NutritionDetailScreen` now shows TODAY / Supplements from today's log (names), TODAY / Food groups
from today's log, SUPPLEMENTS THIS WEEK and FOOD GROUPS THIS WEEK (Mon–Sun dots with counts), and
the "No supplements logged yet this week…" footer **only** when the week is genuinely empty. The
"Log supplements" button is gone — logging lives on the tab. Kept as a pushed screen with Back
(Android's sheet), per `ANDROID_PARITY.md`'s don't-port-literally rule. Logic in
`NutritionTrackerLogic` (pure, tested).

### Changed — Insights "Nutrition consistency" reflects the Nutrition tab (SFM-28b)

`InsightsViewModel.supplementInsights` combines `DailyLogRepository.logByDate` with
`UserSupplementRepository.supplements` through the same `SupplementToggleSet`; the card shows
"Today · N of M logged" and a ticked-name row above the week bars. Empty copy only when the whole
week is empty. `NutritionInsightsSharedRepositoryTest` drives NutritionViewModel, InsightsViewModel
and NutritionDetailViewModel over **one** real repository and a fake table.

### Tests

Unit: **560 passing, 0 failures, 0 skipped** (was 531; +29 across `TabNavigationTest`,
`SupplementToggleSetTest`, `DailyLogRepositoryToggleTest`, `NutritionTrackerLogicTest`,
`NutritionInsightsSharedRepositoryTest`, `SupplementReminderPlanIdTest`, extensions to
`SupplementTest` / `SupplementInsightLogicTest`; `SupplementPlanProgressTest` retired with its
object). Instrumented: `NutritionTabNavigationTest` green on the Pixel 8 API 36 emulator.
`SignInLocally` (a `@SeedOnly` manual utility) puts the installed app into a local signed-in state
for on-device QA. Manual checklist: `QA_CHECKLIST_ANDROID.md`.

### Verification record — what was actually checked on 26 Aug 2026

Branch `lucasvsf026/sfm-27-28-nutrition-tab-inline-supplement-logging`, commit `c948ec5`
(+ this changelog note). Debug build installed on the Pixel 8 / API 36 emulator, signed in locally
via `SignInLocally` and seeded with `SeedTestData`.

| What | How | Result |
|---|---|---|
| SFM-27 root cause | Emulator: from Learn, tap Nutrition → no change; BACK → Nutrition underneath, BACK → Home. Fresh process → Home → Nutrition → works | Cross-tab push + `restoreState`, not a route mismatch |
| SFM-27 fix | Nutrition → "See all articles" → Learn → Track → Nutrition, ×3 | Lands on "Your nutrition focus", 0 "Back" nodes, tab highlighted |
| Re-tap scroll-to-top | Scroll Nutrition down, tap Nutrition tab | Header back at top; back stack holds one Nutrition entry |
| Chip logging | Tap Zinc → "4 of 4 logged today"; tap Folate → "3 of 4" | Live, no dialog, chips fill/unfill from the stored row |
| Plan sheet | Review Plan | Essentials (name + dose, benefit, bell) and Your supplements form; Add disabled until Name set |
| Add your own | Name "Magnesium", Dose "300 mg", Time Evening → Add → Got it | Listed "300 mg · Evening"; five chips; "3 of 5" → tap M → "4 of 5" |
| Track tracker | Track → "Nutrition. 3 of 4 supplements today" row | Today's names "Vitamin D · Omega-3 · Zinc", week dots M:1 W:3, food groups empty state |
| Insights | Insights → Nutrition consistency | "Today · 3 of 4 logged" → "of 5 a day" / "Today · 4 of 5 logged" after Magnesium |
| Track row "N of M" | Track → "Your trackers" Nutrition row vs Nutrition card with two custom entries | Was "1 of 4" vs "1 of 6" — `TrackerSummaryLogic` scored the plan only; now scores the shared toggle set (fixed same day, `TrackerSummaryLogicTest`) |
| Tracker after logging | Chip (Folate) + Log screen (Iron, Fruit) → Track → Nutrition row | "Folate · Iron", "Fruit", Wednesday 2 supplements / 1 food group, footer gone |
| Custom reminder bell | Plan sheet → bell on a "Your supplements" row → OK → bell again | "Reminder at 9:00 AM" under the entry, then cleared |
| Unit suite | `./gradlew :app:testDebugUnitTest` | 560 tests, 0 failures, 0 skipped |
| Instrumented | `NutritionTabNavigationTest` via `connectedDebugAndroidTest` | 1/1 green (3.1 s) |

Not verified today (needs a real account / network): the Supabase rows behind 5.9 and 10.x in
`QA_CHECKLIST_ANDROID.md` (offline queue drain, consent-withdrawn snackbar, reminder firing).

### Release build (26 Aug, 11:53)

Code 16 **rebuilt from `main` (`1aae2d4`)** so the upload identity contains this work — 16 never
reached Play, so no bump. `:app:bundleRelease` + `:app:assembleRelease` green; aapt2
`com.genesyx.app / 16 / 1.4.2 / target 36`; upload-key SHA-1 `8D:EB…CC:73` on both files; R8
mapping shows `SupplementToggleSet` retained (PR #20 is in). Archived at
`~/Documents/Genesyx Releases/1.4.2-code16/` — AAB SHA-256 `117370ae…2ff13` (17,154,680 bytes),
`SHA256SUMS.txt` verifies. The earlier same-day build is kept as `*.pre-pr20.*` and must not be
uploaded.

### Not touched, on purpose

- Track rows are pushed screens, not modal bottom sheets (all six already open something).
- Food-group chips still use the fire-and-forget `toggleFoodGroup` (refusal shown by the banner,
  not a snackbar); `HomeScreen` / `InsightsScreen` keep their inline tab-navigation blocks (correct
  as written, just not yet on the shared helper).
- The Nutrition tab's own "Your supplements" card (with per-entry reminders) stays alongside the
  sheet's list — both read the same repository.

---

## [Unreleased] — iOS parity hand-off — `1.4.2 (16)` (25 Aug 2026)

Worked from `ANDROID_PARITY.md` at the root of the iOS repo (`2cd61d7`): six sections, each with the
iOS symptom, the Android files where the same defect lives, and the design intent to preserve. Each
was checked against the Android source rather than ported on the assumption of symmetry.

Version identity: **`versionName 1.4.2`, `versionCode 16`**, targetSdk 36, Room **v10**. Code 15 was
consumed by the previous batch; Play rejects a duplicate code, so this is 16.

### Added — Article 9 health-data consent gate (parity item 3)

The largest piece, and it gated items 2 and 4, so it went first. Android had **no consent gate
anywhere** — grepped for it rather than assumed. Now:

- **`consent_events`, an append-only trail (Room v9 → v10, `MIGRATION_9_10`).** Grants and
  withdrawals are appended, never updated in place, so the decision history is auditable rather than
  a single mutable boolean. The DAO reads back on **`rowid DESC`**, not `recordedAt`: `recordedAt` is
  ISO text and `LocalDateTime.toString()` omits zero seconds, so two events in the same minute sort
  wrong as strings. `rowid` is true insertion order.
- **`ConsentRepository` + `HealthDataCollectionGate`**, a **suspend** `fun interface` on purpose —
  the gate reads Room, and making callers suspend is what stops a synchronous default sneaking in.
- **An empty trail reads as permitted.** Deliberate, and matched to iOS: every install upgrading
  into this build was never asked, and fail-closed would silently stop their tracking mid-cycle with
  no explanation. Consent is captured going forward, not retroactively assumed absent.
- **Writes *and* reads are gated** in the four health repositories (cycle, daily log, pH, quiz
  answers). Gating writes alone would leave withdrawn users still seeing their data pulled from the
  server.
- **Guest decisions are adopted on sign-in** (`ConsentRepository.adoptGuestDecision`, called from
  `AuthRepository` *before* the refreshes), so a decision made before registering isn't discarded by
  the first authenticated pull.

### Added — refusal is now visible, never silent (parity items 4 and 2)

The iOS bug was a refused write dismissing its dialog exactly like a successful one. The same shape
existed here once the gate landed, so the reporting went in with it.

- **`SaveOutcome`** (Saved / Refused / Failed) and **`PhWriteResult`** (Accepted / Refused /
  OutOfRange) replace `Unit` returns on the health writes. Dialogs no longer dismiss on a refusal —
  they stay open and say why.
- **`ConsentWithdrawnBanner` now renders on every health write surface**, not just the two settings
  dialogs it started in. A withdrawn user tapping water quick-add, saving a daily log or adding
  hydration would otherwise get a control that does nothing with no explanation, which is the first
  thing an internal tester will hit given the toggle is new in this build. Wired into
  `LogScreen`, `HydrationDetailScreen` and `NutritionScreen`.
- Banner and dialog copy reviewed against the `PhCopyBannedPhraseTest` term list — clean: no
  condition named, no treatment or diagnosis implied.

### Fixed — Home greeting reads `profiles.display_name` (parity item 1)

The greeting sourced the name locally, so a name corrected on one device never reached the other.
Home now reads through `profiles.display_name`, and the name push is **owed and persisted**: the
push is attempted before any pull, and if it fails the flag survives process death and retries. The
flag is cleared on sign-out and, belt-and-braces, on sign-in.

### Fixed — quiz answers push before they pull (parity item 5, deliberately *not* a literal port)

The iOS fix is built around `QuizAnswersRow.init?` refusing an empty set — an iOS-only guard. Android
already does an unconditional empty-map upsert, so **the iOS bug does not exist here**; copying the
tombstone across would have fixed nothing and introduced a defect Android didn't have.

What did need porting is the discipline underneath it: `record()` was fire-and-forget, so an offline
edit was lost and then overwritten by `refresh()` pulling the stale server copy. Quiz answers now
carry the same persisted owed-push flag as the display name, and push before pulling.

### Changed — the legacy pH marker no longer names the sample type

`PhCopy.LEGACY_MARKER` is now **`"legacy reading"`**, was `"urine (legacy)"`. Owner request, 25 Aug.

**Only the wording changed — the mechanism it labels is untouched, and deliberately so.** Pre-migration
readings sit on a different scale (urine pH runs roughly 5.0–8.0; vaginal Elevated is anything above
4.5), so they remain excluded from insights, drawn muted and off the trend line, and shown with a
neutral marker *instead of* a Healthy/Elevated status. Dropping the distinction rather than renaming
it would render a stored 6.5 as "Elevated" — a false health signal on Article 9 data.

The stored wire value `PhMeasurement.URINE = "urine"` is unchanged: it is data, it is never rendered,
and the Room and Supabase defaults depend on it.

`ChangeListContractTest` was tightened from "no *urine pH* in customer copy" to **no `"urine"` at
all**, so the wording cannot be reintroduced silently. **iOS must make the matching change** —
`QaParityTest` pins this string as part of the cross-platform contract.

### Removed — the one-time "Vaginal pH tracking" notice dialog

The migration notice that interrupted the pH tab on first open is gone. Owner request, 25 Aug: it
shipped with 1.3.0 to announce the urine → vaginal switch, and by now it lands on people who never
logged a urine reading and have nothing to reconcile.

Removed as a whole chain rather than just hidden — the `AlertDialog` and its `PhTrackerViewModel`
members, `PhCopy.NOTICE_TITLE` / `NOTICE_BODY` / `NOTICE_DISMISS` (and their entry in
`PhCopy.all()`, so the banned-phrase guard no longer scans copy that cannot render), the
`ph_vaginal_notice_seen` DataStore key and its `PreferencesRepository` accessors. `PhTrackerViewModel`
no longer takes `PreferencesRepository` at all. Three tests covering the notice went with it: 534 → 531.

The dismissal flag surviving on existing devices is harmless — DataStore ignores keys nothing reads.

**Note for the pH copy review:** the notice was the only screen that explained what `"legacy reading"`
means. The marker still renders, still suppresses classification, and readings are still kept — but a
user with pre-migration rows now has no in-app definition of the term. Worth a line in the pH detail
screen if any of those rows are still out there.

### Not changed — products read + empty state (parity item 6)

Already correct on Android. Also confirmed: the **`genesyx_products` seed migration is cancelled, not
deferred** — the four bundled supplements are nutrition-pathway content, not SKUs, and the production
table should not be seeded.

### Verification

- Unit tests **534 passing, 0 failures, 0 errors, 0 skipped**.
- Lint **0 errors** (53 warnings, all pre-existing).
- `:app:bundleRelease` + `:app:assembleRelease` **GREEN** under R8/minify. AAB 17,071,622 bytes, APK
  12,332,045 bytes. `aapt2` reports `16 / 1.4.2 / SDK 36 / minSdk 26`; `apksigner` reports SHA-1
  `8D:EB…CC:73`, SHA-256 `C3:D5:1F:4B…A4:46:C1:7D` — the recorded upload key.
- **Migration proven twice.** `ConsentMigrationTest > migrate9To10_addsConsentEvents_andPreservesExistingLogs`
  passes via `MigrationTestHelper` against the versioned schema JSONs, alongside all six older
  migration tests; and a real APK-over-APK upgrade from the live Play build **1.4.0 (14)** to
  **1.4.2 (16)** installed clean, retained data, launched and rendered, with logcat clear of
  `AndroidRuntime`, `FATAL`, `SQLiteException` and any Room migration error.
- pH band boundary re-confirmed against the approved spec: `4.5` exactly classifies **Healthy**;
  `> 4.5` is **Elevated** (`PhStatus.classify`).

### Known, tracked, not fixed in this build

- `DailyLogRepositoryTest` flakes on a **rotating** test name (~25–50% per class run, passes in
  isolation, and HEAD flakes identically) — pre-existing, not a product defect, but it does mask
  regressions in the offline-safety suite.
- Cycle settings still have no offline retry queue: a failed remote push is only logged, and a later
  refresh can overwrite a never-pushed offline edit (`data/CycleRepository.kt`).

---

## [Unreleased] — Single-source-of-truth bug batch (28 Jul device walkthrough)

### Fixed (25 Aug 2026) — the five bugs the client reported on iOS build 22 — `1.4.1 (15)`

Android identity for this batch: **`versionName 1.4.1`, `versionCode 15`, `applicationId
com.genesyx.app`, targetSdk 36.**

This entry originally claimed code 14 could be reused because it "has never been uploaded to Play".
That was read off the repo, and it was wrong: the Play Console shows **14 (1.4.0) on the internal
testing track since 19 Aug 2026**, full rollout, live to internal testers. Play rejects a duplicate
version code, so reusing 14 would have failed at upload — and worse, it would have meant two
different binaries sharing one code, which is the code-9 collision this note was trying to avoid.
Hence 15. Play state is not derivable from this repo; check the console before choosing a code.

The client reported five bugs against **iOS build 22**. Each was investigated against the Android
source independently rather than ported on the assumption of symmetry — two were real here, three
were not, and the reasons differ per bug.

- **1. pH "See your supplement plan" opened the Nutrition tracker, not the plan.** Real on Android,
  and a different mechanism from iOS. `onOpenPlan` navigated to `Screen.NutritionDetail.route`. The
  Nutrition route is now parameterised (`nutrition?plan={plan}`) with a `Screen.Nutrition.create()`
  builder, and `NutritionScreen` seeds its plan dialog from the argument. Two traps handled: the
  navigation deliberately omits `restoreState`, because restoring a saved Nutrition back-stack entry
  discards the new argument and reproduces the exact reported bug; and `ArticleDetailScreen`'s
  tab-reuse check now compares on `substringBefore("?")`, which would otherwise have silently
  stopped matching the moment the route took an argument. `GenesyxBottomNav` navigates via a
  separate `navRoute` so the tab bar cannot navigate to a literal `{plan}`. (`5fb6872`)
- **2. Nutrition "See this phase's foods" did nothing.** *Not* a defect on Android. The iOS cause was
  a CTA firing from inside the tab it targets, which is a no-op there because Nutrition pushes
  `ArticleDetailView` onto its own stack. On Android `ArticleDetail` is a separate nav destination,
  so the CTA's `popUpTo`/`restoreState` genuinely lands on Nutrition. Verified by reading the graph,
  not inferred. No change.
- **3. Home greeted her by a misspelt name.** Real on Android, and deeper than on iOS.
  `SupabaseAuthService.signUp` discards its `displayName` parameter outright, and `toAuthSession`
  manufactures a `displayName` from the email localpart — so the name she typed was lost between the
  form and the greeting, and Home rendered `lucianne.valenca`. `AuthRepository.persist` now carries
  the typed name past the provider (`typedName`), and `SessionRepository` treats a name equal to the
  localpart as no name at all, sending it through the same presentable transform as an absent one.
  `SessionRepository.nameFromAddress` splits on `. - _ +` and title-cases, touching **only the first
  character of each word so accents survive**. Fixed entirely in files outside the in-flight auth
  work, which sits on `wip/auth-2026-08-25`. (`bc8541c`) A follow-up gave the sign-up Name field
  `KeyboardCapitalization.Words`; it had inherited `None` from the `Field` composable it shares with
  the email and password fields, where `None` is correct. (`bea1f99`)
- **4. Learn "How to use Genesyx" showed a missing image.** *Not* a defect on Android. That entry is
  a `HubCard` — title, subtitle, chevron — with no image slot at all, so there is no frame to render
  blank. `ArticleHero` already falls back to a category-keyed brand gradient when `heroImage` is
  null, which is the placeholder behaviour the fix asked for. No change.
- **5. Profile sub-tabs were read-only.** *Not* reproducible on Android, and the iOS root cause
  cannot occur here. On iOS the fields were never disabled either: `recordQuizAnswers` returned
  `Void` behind the Article 9 consent guard while Save dismissed unconditionally, so a refused write
  closed exactly like a successful one. **Android has no consent gate anywhere in the source** —
  grepped for it rather than assumed — so no write can be refused. All four dialogs write through:
  `updateName` → `SessionRepository.updateDisplayName` + `ProfileRepository.setDisplayName`;
  Tracking Preferences → `QuizAnswersRepository.record`; cycle settings → `CycleRepository.upsert`;
  Change Password → `AuthRepository.changePassword` with real loading and error state.
  `record()` writes to DataStore **before** any network call, so the edit persists regardless of
  connectivity and the dialog's dismissal is honest. No change.

**One latent issue found while verifying bug 5, deliberately not fixed in this batch.**
`QuizAnswersRepository.record` discards the `DataResult` from `remote.upsert`, and there is no retry
queue — by design, documented at the class header: a queued write could fire under the wrong JWT
after a sign-out. The consequence is that a save made while offline is correct on the device but
never reaches the server, and the next sign-in's `refresh()` takes the "server wins when it has
answers" branch and overwrites her local answers with the stale server copy. It needs a sign-out and
sign-in to surface, so it is **not** what the client saw, and fixing it means designing an owed-push
mechanism like iOS's `pendingPush` — out of scope for a bug-fix batch two days before a demo.

**CI, red on `main` since 19 Aug, is green again.** The cause was neither the code nor the runner:
every push failed at `:app:compileDebugKotlin` with `CommonUi.kt:81 Unresolved reference
'page_background'`, because the drawable was listed in `.git/info/exclude` — a local, unshared file
— and had therefore never entered the repo. It compiled on the machine it was added from and could
not compile anywhere else. Committed the asset (`fd3454a`). Verified by cloning `main` into a fresh
directory and running the exact two CI steps against the clone rather than a warm working tree:
`testDebugUnitTest` + `assembleDebug` **BUILD SUCCESSFUL**, **488 tests, 0 failures, 0 errors**.
Every other `R.drawable.*` reference in the source was checked against the git tree; none is missing.

**The in-flight sign-up/confirm-email work was moved off `main` to `wip/auth-2026-08-25`** so this
batch stays separable from it. `main` was returned to clean without stashing.

### Release prep for code 14 (19 Aug 2026) — signing hardened, deletion leak closed, docs de-drifted

- **A release build can no longer be signed with the debug key.** `app/build.gradle.kts` previously
  fell back to `signingConfigs.getByName("debug")` whenever `keystore.properties` was absent, so a
  machine without the keystore produced a green `assembleRelease` and an installable APK that looked
  like a release. Play rejects a debug-signed AAB, but the APK installs and runs — so the artifact
  could be smoke-tested and mistaken for the real thing. `signingConfig` is now `null` without a
  keystore, and the existing `gradle.taskGraph.whenReady` guard (which already failed release builds
  missing Supabase creds) now also fails them when `keystore.properties` or the `.jks` it points at
  is missing. Debug builds are untouched. Verified both ways: with the keystore moved aside the
  build fails with an explicit message; restored, it signs with upload key SHA-1 `8D:EB…CC:73`.
- **`deleteAccount()` left supplement reminders on the device.** It called `reminderScheduler
  .cancelAll()`, `database.clearAllTables()` and `quizAnswersRepository.clearLocal()`, but not
  `supplementReminderRepository.clearAll()` — which `signOut()` does call. Those reminders live in
  DataStore on a separate scheduler, so neither of the other wipes reaches them, and they kept
  firing notifications naming her supplements after the account was gone. One-line fix mirroring
  `signOut`, pinned by a new regression test in `AuthRepositoryTest`.
- **Artifacts rebuilt and identity-verified:** aapt2 reports `com.genesyx.app 14 / 1.4.0 / SDK 36`,
  APK and AAB both signed with the upload key. Unit tests **484 passing, 0 failures**. versionCode
  stays 14 — it has never been uploaded, so burning it for a no-op would repeat the code-9 collision.
- **Doc drift corrected** in the files most likely to mislead the next session: `CLAUDE.md` (header,
  STOPPED-HERE block, identity table, Room v6→v9), `APP_INVENTORY.md` (claimed 1.3.2/13 was the
  Gradle identity), `ARCHITECTURE.md` + `README.md` (claimed targetSdk 35), the release runbook's
  stale identity line and its now-wrong debug-signing note, and `docs/V1_1_NOTIFICATIONS_AND_LEARN.md`.

### Verified (19 Aug 2026) — `daily_logs.sexual_activity` is LIVE; code-14 backend gate CLOSED
- **Proven over REST with the anon key, not inferred from the catalogue.** A single probe was
  inconclusive — anon has no SELECT grant on `daily_logs`, so `select=sexual_activity` returns
  `42501 permission denied for table` (401) regardless of whether the column exists. A **differential
  probe** settles it: a deliberately bogus column returns `42703 column ... does not exist` (400),
  because PostgREST resolves the column name *before* Postgres applies the table grant. Since
  `sexual_activity` reached the grant check instead of failing name resolution, the column exists
  **and is in PostgREST's schema cache** — which is the exact skew a catalogue check cannot rule out.
- **Corrected two stale landmines that nearly caused harm.** CLAUDE.md still claimed the
  `user_supplements`/`genesyx_products` tables did not exist (PGRST205, 29 Jul) and that code-14
  must not ship without them — both superseded on 13/19 Aug. Acting on it, this session recommended
  running `docs/migrations/2026-07-29_user_supplements.sql`, which the 19 Aug verify doc marks
  **"⛔ Do NOT run — superseded"** and harmful. It was never executed. CLAUDE.md now says so loudly.
- **`docs/schema.sql:85` fixed.** It documented `sexual_activity` as nullable and "NOT YET IN
  PRODUCTION". Production is `NOT NULL DEFAULT false`, deliberately: the log sheet is a single
  toggle, so "she said no" and "she was never asked" have nowhere to diverge — the client omits the
  field when unset (`encodeDefaults = false`) and the server fills `false`. Not a schema conflict
  with iOS, just a wrong comment that made two people derive the same false alarm.
- **A2 stays closed.** One `FOR ALL` owner policy on `user_supplements` is the project convention;
  the four-policy expectation is the obsolete 29 Jul draft.

### Added (19 Aug 2026) — Intimacy reminder (opt-in, schedule-driven, zero backend)
- **New `ReminderKind.INTIMACY`**, riding the existing WorkManager reminder chain — no new
  framework, no new channel, no backend. She picks day(s) and a time (default 20:00, every day);
  it is **deliberately absent from `DEFAULT_ENABLED`**, so nobody is ever reminded about their sex
  life without asking. Files: `ReminderKind.kt`, `ReminderContent.kt`, `ReminderPolicy.kt`
  (nextOccurrence + inScheduledWindow + kindGate), `ReminderScheduler.kt` (`SCHEDULABLE`),
  `NotificationSettings.kt`, `NotificationSettingsRepository.kt` (two new DataStore keys),
  `ReminderSettingsViewModel.kt`, `ReminderSettingsScreen.kt` (new "Private" section).
- **Schedule-driven on purpose, not cycle-driven.** `FERTILE_WINDOW` already speaks for the
  predicted window; a second cycle-derived nudge would double up on it and drag the copy toward a
  fertility claim. `kindGate` for INTIMACY reads nothing from the cycle or the log — pinned by
  `intimacy reads nothing from the cycle or the log`.
- **Privacy is enforced in three places, not just the copy.** The words name nothing ("A private
  reminder" / "The reminder you set. Open when it suits you."); the notification posts
  `VISIBILITY_PRIVATE` so its contents stay off the lock screen; and it reuses the generic
  **Tracking** channel, so Android's own notification settings never display a channel named after
  it. New `ReminderContentTest` fails the build if the copy ever leaks "sex", "intima", "fertile",
  "ovulat", "conceiv", "partner", "period" or "cycle".
- **Fixed a pre-existing ANR hazard while adding the second user-controlled day set.**
  `ReminderPolicy.nextAt` looped forever on an empty `allowedDays`, on the caller's thread — now
  falls back to every day (`allowedDays.ifEmpty { ALL_DAYS }`), and `setIntimacyDays` refuses to
  clear the last day at the ViewModel. **Still open:** `setDailyDays` has no such guard, so
  deselecting all seven daily-log chips silently means "every day" rather than "off". Left alone
  deliberately (out of scope); worth a follow-up.
- Suite **483 passing, 0 failures**; `:app:compileReleaseKotlin` green.

### Verified (19 Aug 2026) — Intimacy in Track and Insights was already shipped
- The parity brief assumed both were missing. Neither is. Track already renders intimacy as the
  purple `DayMarker.ACTIVITY` dot (`DayMarkers.kt:32` → `TrackScreen.kt:500`, `MarkerIntimacy`),
  day detail already shows "Intimacy — Logged" (`LogDaySummary.kt:49`), and Insights already
  renders `IntimacyCard` gated on `hasData` (`InsightsScreen.kt:150`). No code was needed.
- **The brief's "blocked by the missing Supabase field" premise is also wrong.**
  `DailyLogDto.sexualActivity` is nullable and the shared serializer runs `encodeDefaults = false`,
  so a null intimacy field never reaches the wire — logs without intimacy sync cleanly against a
  server that predates the column. Only a row that actually carries one needs the migration, and
  that row queues and retries like any other failed push. `DailyLogDtoTest` pins all three cases.
  Nothing fakes cross-device persistence.
- **Two facts still unresolved:** whether `daily_logs.sexual_activity` is live in production (REST
  probe outstanding), and that the iOS migration declares it `not null default false` while this
  repo's `docs/schema.sql` documents it nullable. One of the two is wrong; they must agree before
  the code-14 migration batch is applied.

### Verified (19 Aug 2026) — Symptom patterns walkthrough, iOS doc fix pushed
- **Insights "Symptom patterns" traced end to end and confirmed working.** Entry is the Log's eight
  preset chips plus free text (`LogScreen.kt`); `SymptomPatternLogic.compute` is pure over
  `logsByDate`; `InsightsViewModel.symptomInsights` is a `StateFlow` off `dailyLogRepository.logByDate`,
  so the card is live. Behaviour worth restating: the 28-day grid runs oldest→newest with today in
  the bottom-right; the top symptom is counted in **days**, grouped case-insensitively and shown
  under its alphabetically-first spelling; and below `MIN_DAYS_FOR_PATTERN = 7` symptom days
  **inside the window** the card names no pattern. Qualification and display use the same 28 days
  on purpose. `SymptomPatternLogicTest`: **10 tests, 0 failures**.
- **iOS repo (`genesyx_apple`) pushed and one doc bug fixed** (`0381449`): `docs/FEATURES.md` said
  the vaginal pH scale ran 3.5–7.0; `PhStatus.swift` pins `min = 3.8` and records it as
  client-signed-off. The document was wrong, not the code — this now matches Android's floor.
- **Noted for the code-14 migration work:** the authoritative table definitions live in this repo's
  `docs/schema.sql`. The iOS repo carries ALTER migrations only, so it is a backstop for deltas,
  not for base schema.

### Added (19 Aug 2026) — Last three iOS parity gaps closed (per the 19 Aug audit)
- **Committed the dirty tree first** (`283ec5b`): the ~59 uncommitted files sitting on `edd8f2d`
  (medical sources, free guide, Learn source map, debug source set, contract tests, docs, iOS
  images) went in as one snapshot before any new work, with the suite green at 467.
- **Nutrition plan card live state** (audit gap 1): "None logged yet today" / "N of M taken today",
  live from today's Log toggles — iOS's `NutritionView` strings verbatim. `SupplementPlanProgress`
  scores today's names against the suggested plan, trimmed and case-insensitive; Iron and her own
  supplements are recorded, not scored. The "Why is this important?" expander stays.
- **Insights hydration: days on goal** (audit gap 2): the card now leads with "N of 7 days on
  goal" — delegated to `StreakEngine.daysOnGoal`, the same number Home renders, with a test pinning
  the two surfaces agree across goals. The ml/day delta and weekly bars stay.
- **pH chart numeric axis marks** (audit gap 3): muted labels at 3.8 / 4.5 / 7.0 (values from
  `PhStatus`, no new literals), right-aligned in a left gutter the plot is inset past so dots never
  overlap. Classification, bands and legacy-urine rendering untouched.
- **Q&A table recorded**: Q11 = A (Profile row exists), Q13 = A (display clamp, keep stored value,
  never reclassify), Q14 = A (guest paths stay dead; cleanup post-launch). Q10 (TalkBack) left
  OPEN — BLOCKED-ON-DEVICE, a human launch gate.
- Verified: **475 unit tests, 0 failures** (467 baseline + 8 new); `:app:assembleRelease` green
  (R8 clean); **33/33 instrumented tests** on the `test_Pixel8.1` emulator (the known
  `CycleSettingsDialogTest` flake passed this run). Not verified: a human look at the pH axis
  labels in light/dark on a physical device.

### Status after 19 Aug 2026 — done, blocking, missing

**Done.** The 17 Aug iOS parity build list is now **11/11 in source** (9 were already in, the last
3 landed today). Source sits at 1.4.0 (versionCode 14), Room v9; the working tree is clean and
everything is pushed (`edd8f2d..9555390`, five commits: dirty-tree snapshot `283ec5b`, plan-card
live state `586dfb2`, Insights days-on-goal `5736e3f`, pH axis marks `7fc2970`, docs/Q&A
`9555390`). Q11/Q13/Q14 are recorded in the parity Q&A table.

**Blocking — the release gates, in order (none are code):**
1. **Play catch-up first:** Internal testing still serves 1.3.0 (11) and Production 1.2.0 (9); the
   verified 1.3.2 (13) artifact (`~/Documents/Genesyx Releases/1.3.2-code13/`) still awaits upload
   and on-device smoke test.
2. **Console review:** the corrected Health apps declaration and Data Safety form are saved as
   drafts, not sent; the Play URL-validator 429 warning on the deletion/privacy routes is open.
3. ~~Supabase verification~~ **CLOSED 19 Aug (evening):** the full verify pass ran against
   production — schema checks all pass (the "4 policies" expectation was the obsolete draft's;
   the applied migration uses one `FOR ALL` owner policy by design), the **S6 deletion re-proof
   is complete** (throwaway seeded pH + log + supplement rows, RPC 204, JWT `user_not_found`,
   service-role counts all zero), the backstop SQL turned out to be versioned in the iOS repo
   already, and Supabase's DPA is incorporated into its ToS (no signature needed). Bonus:
   production `delete_current_user()` has pinned `search_path=''` since 13 Aug — the old TODO
   is obsolete. Details: `docs/PROMPT_SUPABASE_VERIFY_2026-08-19.md`.
4. **Backend/legal remainder:** `genesyx.co.uk` privacy wording (vaginal pH + waitlist email);
   `git push` the iOS repo's local `main` (ahead of origin — it holds the only versioned copy
   of the 13 Aug backstop migration besides production).
5. **Owner approval**, then promote to Production.

**Missing / not yet verified:**
- **Q10 TalkBack pass** — needs a physical device; the one human launch gate left in the parity doc.
- Human light/dark look at the new pH axis labels on a real screen.
- The 18 Aug on-device checks (cellular, airplane-mode, Profile row taps) remain unproven.
- Known pre-existing gap: cycle settings have no offline retry queue (`data/CycleRepository.kt`).
- iOS-side parity fixes (labels, two-band thresholds, `measurement_type`, copy, logged-days
  hydration average, waitlist RPC) live in the iOS repo's queue, not this one.

### Added/Changed (13 Aug 2026) — Audit quick-win punch list (4 items)
- **pH → fertility explanation** (1A): new "How this relates to fertility" section on the pH detail
  screen — cautious copy ("may", "background context, not a fertility test", GP/pharmacist
  signposting) that passes the banned-phrase guard. **Flagged for medical-reviewer sign-off.**
  (`PhCopy.FERTILITY_*`, rendered in `PhDetailScreen`.)
- **Phase card → article link** (2D): the Home "Today's focus" card now shows "Learn about this
  phase →", opening the always-available "How your cycle and phases work" guide
  (`HomeUiState.phaseArticleSlug`). Verified on-device (link opens the article).
- **Article reads count toward the streak** (3A): opening a Learn article now records the date, and
  the streak engine counts it as a meaningful action (`StreakEngine.compute(articleReadDates=…)`,
  stored in DataStore `read_article_dates`, joined in `StreakRepository`). **Additive & optional** —
  the shared `tracking_test_vectors.json` contract is untouched (`TrackingVectorTest` green). iOS
  parity: iOS won't count article reads until it adds the same input.
- **Personal Details editing obvious** (1B): the Personal Details dialog now surfaces **Change
  password** alongside Edit name / Change email — all three amend actions in one place. (The profile
  model holds only name/email/theme, so there were no further personal fields to add.)
- Verified: `StreakEngineTest` (+1) + `PhCopyBannedPhraseTest` (new copy clean) + `TrackingVectorTest`
  green — **376 unit tests** (lone failure is the known `PreferencesRepositoryTest` flake, passes on
  rerun); all four confirmed on the emulator; `:app:assembleRelease` green.

### Added (13 Aug 2026) — Insights: your own supplements + a private intimacy card
- **"Your supplements this week"** — the supplements a user adds herself ("Your supplements" in
  Nutrition) now surface in Insights, each with an N/7-days count and a Mon–Sun dot row, matched by
  name against `daily_logs.supplements`. Sits below the existing plan-based adherence card, which is
  **unchanged** (the owner chose a separate section over folding them into the score). The Log already
  offered these supplements as toggles (`LogViewModel.customSupplementNames`), so this closes the
  Insights half. (`UserSupplementInsightLogic`, `a73c280`.)
- **Private "Intimacy" card** — intimacy was logged (the "Private to you" toggle →
  `DailyLog.sexualActivity`) but never surfaced afterwards. A private card now shows this week's
  recorded days (dot row + plain count). It stays **hidden until she has recorded intimacy at least
  once**, is labelled "Private to you", carries no timing/fertility framing or judgement, and is still
  never shared with any partner feature. (`IntimacyInsightLogic`, `8474d9f`.)
- Both are **client-only** — no Supabase schema change, iOS unaffected. Verified:
  `UserSupplementInsightLogicTest` (4) + `IntimacyInsightLogicTest` (3) green; on-device both cards
  render with the right day filled after logging (supplement 1/7, intimacy 1/7);
  `:app:assembleRelease` green. `APP_INVENTORY.md` §6 updated with both.

### Changed (13 Aug 2026) — Distinct art for the 3 previously-shared heroes
- The three heroes that were shared between sister topics now each have their own image: **What your
  vaginal pH is telling you** (`learn_hero_ph_explained` — pH strip + colour chart), **Understanding
  ovulation tests** (`learn_hero_ovulation_tests` — ovulation test sticks), and **The Shettles
  Method** (`learn_hero_shettles` — a neutral notebook + calendar still life, deliberately no
  sex-selection imagery). Generated to match the existing pastel-wellness style, cropped to the
  1080 × 602 spec, replacing the shared files in place (no code change — the article references were
  already distinct filenames).
- Every Learn article now has a **unique** hero (verified: the 3 pairs are byte-distinct).
  `LearnHeroImagesTest` still green on the emulator (all heroes decode at 1080 × 602);
  `:app:assembleRelease` green. `APP_INVENTORY.md` §7 updated (no shared images).

### Added (13 Aug 2026) — Learn hero images wired for all 22 remaining articles
- **Every Learn article now has a hero photo (32 of 32).** The 22 that were missing art received
  client-supplied stock, each matched to its topic (pH scale/strips → the pH guides & explainer;
  cycle-phase dots → the cycle guide; a woman at a wall calendar → fertile window; couple's hands →
  timing sex; beds → the sleep articles; water → the hydration articles; nut/seed & food flatlays →
  nutrition/sperm-health; capsules → the supplements explainer; and so on).
- **Converted to the shipping spec**: each source PNG cropped/scaled to **1080 × 602 JPG** (16:9) in
  `drawable-nodpi/`, named `learn_hero_<name>`, and wired with one `heroImage = R.drawable.<name>`
  line per article. The invalid-named source PNGs (hyphenated UUIDs — illegal as Android resource
  names, would have broken the build) were removed. Payload: ~3 MB for all 32 heroes.
- Three photos are intentionally shared between sister topics (pH-scale, test-strip, cycle-phase) —
  noted in `APP_INVENTORY.md` §7 so distinct art can be dropped in later with a one-line repoint.
- Verified: **`LearnHeroImagesTest` (2 instrumented tests) passes on the emulator** — asserts no
  article has a null hero and that **every hero drawable decodes to a real 1080 × 602 bitmap** on
  device (catches a missing/corrupt/wrong-size asset that a compile alone wouldn't); 368 unit tests
  green; `:app:assembleRelease` green (R8 clean). `APP_INVENTORY.md` §7 updated to "complete (32 of
  32)".

### Docs (13 Aug 2026) — Learn hero-image inventory (22 of 32 articles need art)
- Audited every Learn article for a hero photo: **10 of 32 have art, 22 don't** (they currently fall
  back to a category-keyed brand gradient — the layout is correct, just no photo). Recorded the full
  gap in `APP_INVENTORY.md` §7 with the **exact spec to supply** (1080 × 602 px JPG, 16:9,
  `drawable-nodpi/`, ~50–150 KB) and a per-article **target filename** so delivered art wires in with
  a one-line `heroImage = R.drawable.<name>` each. The 22 break down as 10 always-available "guides"
  + the 12 dated weekly-series editorials.
- Same edit corrected two now-stale inventory claims: the article count (31 → **32**) and the Shettles
  piece (was "deliberately absent" → now **present** on Android as week-12 explicitly-unproven theory,
  8 Nov 2026, guard-railed language, pending medical-reviewer sign-off).

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
