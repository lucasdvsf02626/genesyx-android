# CLAUDE.md — Genesyx Android

Project Name: Genesyx Android

Release-candidate handoff. Read this first. Honest state as of the commit below.

## Build identity
- **RC build:** versionCode `3`, versionName `1.0.0`  (`app/build.gradle.kts:38-39`)
- **Release artifacts** (built & signed with `genesyx-release.jks` via `keystore.properties`; `clean bundleRelease assembleRelease` all GREEN, R8/minify clean):
  - AAB (Play upload): `app/build/outputs/bundle/release/app-release.aab` — 7.5 MB
  - APK (on-device test): `app/build/outputs/apk/release/app-release.apk` — 3.9 MB
  - Install: `adb install -r app/build/outputs/apk/release/app-release.apk`
- **Signing verified:** APK signer cert SHA-256 `c3d51f4b…a446c17d` matches the `genesyx-release.jks` cert (CN=Lucas Valenca, OU=Genesyx). APK verifies under v2 scheme; `android:debuggable` absent (release = not debuggable).

## What THIS session fixed (device test found 4 blockers; all fixed, verified in source + compiled into the RC)
- **FIX 1 — pH sex-selection claim in onboarding quiz (blocker, content).** The "Did you know?" modal on quiz Q4 claimed "…even pH balance can subtly influence the likelihood of conceiving a boy or girl." Removed the entire `fact = DidYouKnow(...)` block. `domain/content/QuizContent.kt` (gender question, ~line 58). The earlier pH audit only covered Home/Profile/Track/Nutrition/Insights and missed onboarding. Repo-wide grep for `boy or girl` / `conceiving a boy|girl` / `sex-selection` / `sway` now returns **zero** user-visible hits.
- **FIX 2 — offline save must not lie (blocker, data-loss).** There is no sync queue in v1.0: an offline daily-log write lands in Room but is silently overwritten by the server on the next read-through (data loss). Now the log Save is gated on connectivity — offline, it does **not** save/close; it shows `"You're offline — reconnect to save your log."`
  - `ui/screens/LogViewModel.kt` — added `isOnline()` (ConnectivityManager point-in-time check; injects `@ApplicationContext`). `ACCESS_NETWORK_STATE` already in the manifest.
  - `ui/screens/LogScreen.kt:227-243` — Save `onClick` calls `viewModel.isOnline()`; offline → sets `offline=true` (shows the error string), online → saves + `onClose()`.
- **FIX 3 — don't re-onboard signed-in users (high, UX).** Cold start forced a signed-in user through intro → 5-question quiz → readiness summary before Home. Now the start destination is resolved from the persisted session before the graph is built; onboarding shows only for new/signed-out users. Quiz screens left dormant, not deleted.
  - `data/SessionRepository.kt:48` — `suspend fun awaitSignedIn() = store.signedIn.first()` (reads the real persisted value; avoids the eagerly-seeded `isSignedIn` StateFlow that reads `false` until DataStore loads).
  - `ui/AppViewModel.kt` — `startRoute: StateFlow<String?>` (null until resolved); `init` awaits `awaitSignedIn()` → `Home` or `Splash`.
  - `MainActivity.kt` — activity-scoped `AppViewModel`; `splash.setKeepOnScreenCondition { startRoute.value == null }` holds the system splash until resolved; NavHost built only when `route != null`, passing `startDestination = route`.
  - `ui/navigation/GenesyxNavGraph.kt` — added `startDestination: String` param (defaults to `Splash.route`).
- **FIX 4 — softened gender question copy (content).** `domain/content/QuizContent.kt` gender question → "When it comes to your baby's sex, what feels right for you?"; options "I have a hope in mind" / "I'm happy either way" / "I'd rather not say". Same `id = "gender"`.
- `app/build.gradle.kts:38` — `versionCode 2 → 3`.

## Prior session (context, already true before tonight)
- pH tracking is compile-time gated OFF for 1.0 via `core/FeatureFlags.PH_TRACKING = false`. To re-enable later: flip the single flag.
- `ph_readings` is **irrelevant** to v1.0 (pH hidden, no table needed). Do not chase it.
- `delete_current_user` RPC is **deployed and REST-verified**; no Edge Function needed. Delete flow (`SupabaseAuthService`/`AuthRepository`) calls RPC → clears Room → signs out → navigates to Splash.
- In-app "Privacy & Data" row opens `AppLinks.PRIVACY_POLICY_URL` (`https://genesyx.co.uk/pages/privacy-policy`) via `ACTION_VIEW`.
- P0 on-device script previously ran steps 1–11 clean (see `docs/GENESYX_P0_TEST_SCRIPT.md`).

## Verified this session
- All 4 fixes present in source (greps + pasted lines).
- `./gradlew clean bundleRelease assembleRelease` → BUILD SUCCESSFUL, R8/minify clean (only a pre-existing `MenuBook` deprecation warning, unrelated).
- APK signed with the release keystore (cert SHA-256 match) and not debuggable.

## On-device re-test — versionCode 3 APK on emulator-5554 (all PASS, no FATAL in the whole run)
1. **Cold-start routing (FIX 3): PASS.** Force-stop cold start → signed-in `lucas+gx01` landed on **Home**, no onboarding; second force-close→reopen still Home. Signed-out relaunch showed **Splash**. New gx02 signup → Home.
2. **Offline save (FIX 2): PASS.** `svc` offline (`Active default network: none`) → Save did **not** close and showed exact copy "You're offline — reconnect to save your log." Reconnect → Save closed to Home; logcat `DailyLog: synced daily log 2026-07-04`.
3. **No pH claim in quiz (FIX 1): PASS.** Walked onboarding Q1–Q5; only "Did you know?" shown was the legit cycle-length fact on Q2. Answering the gender question advanced **straight to Q5** — no pH modal.
4. **Softened gender copy (FIX 4): PASS.** Q4 shows "When it comes to your baby's sex, what feels right for you?" + helper + the 3 new options; old boy/girl options gone.
- Note: after each sign-in `PhRepository.refresh()` queries the absent `ph_readings` table and logs a non-fatal `E Ph` error — expected, irrelevant (pH flagged off), does not crash.

## Left in this state for the next steps
- Signed in on-device as **`lucas+gx02@mysupplementfactory.com`** (throwaway password `Gx02!verify7k`), on Home.

## SINGLE NEXT ACTION — delete-account steps (owner-run, steps 12–13)
Delete account (progress → signed out → returns to start) → confirm cannot log back in → re-signup same email succeeds → delete again. Then the RC is clear for Play Console (upload AAB, Data Safety form, store listing).

## v1.1 backlog (deferred, do NOT build for 1.0)
- **Real offline-first sync queue** so offline edits persist and reconcile on reconnect (removes the FIX 2 offline block).
- **Log-screen Back discards unsaved edits** — add a trivial confirm/guard.

## Notes
- Secrets in `local.properties` (git-ignored): `genesyx.supabaseUrl`, `genesyx.supabaseAnonKey`, `genesyx.googleWebClientId`, `genesyx.apiBaseUrl`. Never commit real values.
- `keystore.properties` → `storeFile=/Users/lucasvalenca_sf/Documents/genesyx-release.jks`.
- Java: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`.
- Architecture is local-first: Room = source of truth, Supabase read-through on sign-in + write-through. Details in `ARCHITECTURE.md`, `docs/DATA_LAYER.md`, `docs/schema.sql`.
- Remaining release ops: upload the AAB to Play Console, complete Data Safety form + store listing; ensure `genesyx.co.uk/pages/privacy-policy` has live content.
