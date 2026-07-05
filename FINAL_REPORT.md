# v1.1 Sync Hardening — Final Report

Branch: `feature/v1.1-sync-hardening` · 7 commits · base `main` (`0798d45`)

Delivered in ordered phases: test toolchain, data-loss/integrity fixes, a regression net,
pH offline-first sync, and Google sign-in. Every phase gate was `./gradlew :app:testDebugUnitTest`
+ `:app:assembleDebug` green; Phases 3–4 also `:app:assembleRelease` (R8/minify) green.

---

## Per-phase changes

### Phase 0 — Test toolchain + deps + dead-code (`5aeb478`)
- `gradle/libs.versions.toml`, `app/build.gradle.kts` — added `kotlinx-serialization-json` (explicit
  pin, was transitive only), MockK, Turbine, `coroutines-test`, `room-testing`, and the androidTest
  stack (ext-junit, espresso, compose ui-test, hilt-android-testing).
- `app/src/androidTest/.../HiltTestRunner.kt` + `ToolchainSmokeTest.kt` — runner swaps in
  `HiltTestApplication`; proves the instrumented toolchain compiles.
- Deleted `PlaceholderScreen.kt` (verified zero references). No Kotlin/AGP/Compose/Hilt bumps.

### Phase 1 — Data-loss & integrity fixes (`71f7ac8`, follow-up `62655b5`)
- `GenesyxDatabase` `exportSchema=true`; `DatabaseModule` dropped `fallbackToDestructiveMigration()`
  → `.addMigrations(*GENESYX_MIGRATIONS)` + `fallbackToDestructiveMigrationOnDowngrade()` (downgrade
  only). Baseline `schemas/…/2.json` committed. Upgrades now preserve local (esp. LOCAL-ONLY pH) data.
- `PhRepository` enforces pH range `[4.5, 9.0]` in the data layer via new `PhWriteResult.OutOfRange`
  (rejected, never persisted). +4 boundary unit tests.
- `docs/schema.sql` — added the **verbatim deployed** `delete_current_user()` RPC + grants
  (owner-provided). Hard delete (bypasses soft-delete) is intentional for GDPR/Play erasure.

### Phase 2 — Priority regression tests (`b521c36`)
- `AuthRepositoryTest` — delete wipes local DB **only after** confirmed server delete
  (`coVerifyOrder`), no wipe on failure, sign-in persists the auth uid.
- `PhRepositoryTest` — create scoping, delete, and readings StateFlow re-scoping (Turbine).
- `AppViewModelTest` — start-route Home/Splash. `LogViewModelOnlineTest` — isOnline() gate.
- `MainDispatcherRule` test util. (These are the net Phase 3 was built against.)

### Phase 3 — pH offline-first sync (`6b8a8c7`)
- Schema **v2→v3** (`MIGRATION_2_3`, `3.json` committed): `ph_readings` gains `syncStatus`,
  `updatedAt`, `deletedAt`; existing rows → `SYNCED`, `updatedAt` seeded from `recordedAt`. No wipe.
- `PhReadingDto` maps `updated_at` + `deleted_at` (+ `created_at`); entity↔dto mappers with tz parse.
- Write path: Room first (`PENDING_UPSERT`, instant UI) → push; on failure stays PENDING and
  `WorkManagerPhSyncScheduler` retries (unique work, CONNECTED, exponential backoff). Offline **queues,
  never blocks**. `PhSyncWorker` drains via a Hilt EntryPoint.
- Delete = soft delete (`deletedAt` tombstone); `observeAll` hides tombstones; `list()` returns them so
  server deletes propagate. `refresh()` = pull-merge by id (no dupes), last-write-wins on `updatedAt`,
  never clobbers locally-pending rows. Un-guarded the `// v1.1` remote calls.
- `PhMigrationTest` (androidTest) proves 2→3 preserves rows. Dep: `androidx.work:work-runtime-ktx 2.9.1`.
- `.github/workflows/supabase-keepalive.yml` — free-tier keep-alive (owner-provided, verbatim).

### Phase 4 — Google sign-in (`f233bc2`)
- `GoogleCredentialClient` — Credential Manager + `GetSignInWithGoogleOption(serverClientId =
  BuildConfig.GOOGLE_WEB_CLIENT_ID)` → Google ID token.
- `AuthRepository.signInWithGoogle(idToken)` → `authService.signInWithIdToken` → `persist()`.
- `AuthScreen` — "or" divider + "Continue with Google" button (design-system match). **No fake
  success**: unconfigured → "Google sign-in isn't configured"; cancelled → silent; network/airplane →
  friendly error; never crashes. +2 Auth tests. Deps: `androidx.credentials(+play-services-auth) 1.3.0`,
  `googleid 1.1.1`.

---

## Test results

| Milestone | Unit tests |
|---|---|
| Baseline (main) | 32 |
| Phase 1 | 36 (+4 pH range) |
| Phase 2 | 46 (+10 regression net) |
| Phase 3 | 48 (+2 pH sync) |
| Phase 4 | **50** (+2 Google) |

Plus instrumented (androidTest, compile-verified — run on a device): `ToolchainSmokeTest`,
`PhMigrationTest`.

Commands run each phase (all green): `./gradlew :app:testDebugUnitTest`, `:app:assembleDebug`.
Phase 0 also `:app:assembleDebugAndroidTest`. Phases 3–4 also `:app:assembleRelease` (R8/minify clean,
only the pre-existing `MenuBook` deprecation warning).

---

## Supabase SQL

**Already run & verified by owner (nothing pending server-side):**
- `ph_readings` table — RLS on, 4 policies, `CHECK ph_value BETWEEN 4.5 AND 9.0` (rejects 12.0 →
  `23514`), `updated_at` + `deleted_at` columns, `updated_at` trigger, index `(user_id, recorded_at
  desc)`. Mirrored in `docs/schema.sql`.
- `delete_current_user()` RPC + grants (`revoke … from public, anon; grant execute … to authenticated`).
  Verbatim in `docs/schema.sql`.

**Still to run:** none for this workstream.

---

## Remaining manual steps (owner)

### 🔴 Launch-week (blocks release Google sign-in)
- **Create `genesyx-release.jks`, register its SHA-1 in the Google Cloud Android OAuth client, and
  fill `keystore.properties`.** Only the **debug** SHA-1 is registered today; **no release keystore
  exists**. `assembleRelease` passing is **build-only** — release *signing* is not set up, and Google
  sign-in will fail on release builds until this is done.
- **Publish the OAuth consent screen to production** (currently testing) so non-allowlisted users can
  sign in with Google.
- **Re-enable "Confirm email" in Supabase → Auth → Providers → Email.** It is currently **OFF**
  (pre-existing config, used for QA sign-in). Leaving it off in production lets anyone create accounts
  with unverified/typo'd emails (spam/abuse, undeliverable password resets). Turn it back on before launch.

### Google Cloud / Supabase dashboard
- Google: confirm the release SHA-1 (above) is on the Android client; consent screen published.
- Supabase: server-side delete proof — after an in-app account delete, confirm rows gone in
  `auth.users` + `profiles`/`daily_logs`/`cycle_settings`/`ph_readings` (launch-checklist item #2).
- Supabase **Pro upgrade** (removes free-tier pause risk; keep-alive workflow is the interim mitigation).

### Play Console (unchanged, owner)
- Store assets, Data Safety form, privacy + deletion URLs, AAB upload, internal smoke, submit.

---

## Deferred (deliberately not built)
- **Daily-log sync queue** — the FIX 2 band-aid (offline log save is *blocked* via `isOnline()`) is
  **still in place**. Only pH got the offline-first queue this cycle; applying the same pattern to daily
  logs is the follow-up that retires FIX 2.
- **Guest→signed-in data migration** — pH rows written while signed out are scoped to `local-user` and
  stay local-only (see the guest guard in `PhRepository`). On sign-in they are **stranded**: not
  re-assigned to the new `user_id`, not synced, not visible under the account. Adopting local guest rows
  into the account on first sign-in is a deliberate follow-up.
- **FCM** (push) — not started.
- **`storage-kt` / `functions-kt`** — add only when a feature needs them.
- **Dependency bump pass** — Kotlin/AGP/Compose/Hilt/Room/supabase/ktor are ~1–2 minors behind; bump
  after this branch, with tests as the net.
- **OAuth consent screen publish to production** (see launch-week).
- **Supabase Pro upgrade**.

## Risks still open
- **Release Google sign-in is non-functional until the release keystore + SHA-1 are registered**
  (see launch-week). Debug builds work (debug SHA-1 registered).
- **pH sync is new and unverified on-device** — run `MANUAL_TEST_CHECKLIST.md` before shipping.
- **LWW is last-write-wins, not field-merge** — fine for single-user pH; revisit if partner-shared.
- **Deep-link host** `genesis-cycle-guide.lovable.app` (Manifest + nav) is a Lovable staging domain,
  `autoVerify=false`. **Unchanged this cycle** — flagged only; move to the brand domain before relying
  on web→app links.

---

## Suggested squash-merge commit message

```
v1.1: pH offline-first sync + Google sign-in + migration/integrity hardening

- Room: remove destructive migration (upgrades preserve LOCAL-ONLY pH data), exportSchema,
  MIGRATION_2_3, downgrade-only fallback.
- pH: data-layer 4.5–9.0 enforcement; offline-first sync (write-through + WorkManager queue,
  soft-delete, pull-merge LWW) against the now-deployed ph_readings table.
- Google sign-in via Credential Manager on the existing Supabase IDToken seam (no fake success).
- Test toolchain (MockK/Turbine/coroutines-test/room-testing + androidTest+Hilt); 32→50 unit tests.
- Docs: verbatim delete_current_user RPC; CI Supabase keep-alive.

Release signing / release SHA-1 not yet set up — see FINAL_REPORT.md launch-week tasks.
```

## Suggested PR description

**What** — Ships v1.1 pH offline-first sync and Google sign-in, and hardens data integrity
(non-destructive Room migrations, data-layer pH range enforcement, verbatim delete RPC docs). Adds a
real test toolchain and grows the suite 32→50.

**Why** — pH was local-only with a data-loss risk on schema bumps; this makes it sync safely and
removes the destructive-migration footgun. Google sign-in was a dead code path; it's now a working,
fail-safe flow.

**Testing** — `testDebugUnitTest` 50/50, `assembleDebug` + `assembleRelease` (R8) green.
Instrumented `PhMigrationTest` proves 2→3 preserves rows. On-device verification pending via
`MANUAL_TEST_CHECKLIST.md`.

**Not included / follow-ups** — daily-log sync queue (FIX 2 still in place), FCM, dependency bump,
release keystore + release SHA-1 registration, OAuth consent production publish, Supabase Pro. See
`FINAL_REPORT.md`.
