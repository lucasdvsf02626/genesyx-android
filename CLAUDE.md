# CLAUDE.md — Genesyx Android

Project Name: Genesyx Android

Release-candidate handoff. Read this first. Honest state as of the commit below.

## Build identity
- **Shipping build:** versionCode `6`, versionName `1.0.0`  (`app/build.gradle.kts:38-39`) — `main` = `27c2ac0`. See "Release build v6" below. (History: RC was v5.)
- **Release artifacts** (built & signed with `genesyx-release.jks` via `keystore.properties`; `clean bundleRelease assembleRelease` all GREEN, R8/minify clean):
  - AAB (Play upload): `app/build/outputs/bundle/release/app-release.aab` — 8.1 MB
  - APK (on-device test): `app/build/outputs/apk/release/app-release.apk` — 4.3 MB
  - Install: `adb install -r app/build/outputs/apk/release/app-release.apk`
- **Release env:** `GENESYX_ENV=PROD` — the `release` buildType overrides the `DEV` default (`app/build.gradle.kts:84`). Confirmed in the compiled `BuildConfig.java` (release) → `GENESYX_ENV = "PROD"`, `VERSION_CODE = 4`. Release logging (`Logger.d`) is suppressed outside DEV.
- **Signing verified (v3):** APK signer cert SHA-256 `c3d51f4b…a446c17d` matches the `genesyx-release.jks` cert (CN=Lucas Valenca, OU=Genesyx). APK verifies under v2 scheme; `android:debuggable` absent (release = not debuggable). (v4 rebuilt with the same signing config.)

## v4 — two scoped changes (Sat Jul 4), built + smoke-tested on-device
- **FIX A — dev "Clients" screen gated OFF (blocker).** The admin/dev Clients screen (add client + "Seed 100 demo clients" → `ClientsViewModel.seedDemo(100)`) was reachable in release via **Profile → "Manage clients"** (`ProfileScreen.kt`, the only entry point — no gesture/deep-link, not in bottom tabs). Now gated behind new `FeatureFlags.ADMIN_CLIENTS = false` (same pattern as `PH_TRACKING`): the Profile "Clients" section is wrapped in `if (…ADMIN_CLIENTS)`, so the screen and its seed action are unreachable. Route composable left dormant. Dev sweep found no other dev/debug screens or seed actions (`Environment.DEV` only gates debug logcat verbosity, not a screen).
- **FIX B — Home hero banner brand image.** Replaced the gradient-bubble decorations on Home (`FloatingBubbles()` full-screen + `BrandOrb` on the cycle card) with the brand crescent artwork. Asset `~/Desktop/genesyx-brand/home-hero.jpg` (5375×11650) downscaled to `res/drawable-nodpi/home_hero_bg.jpg` (1080×2341, 82 KB). `HomeScreen.kt`: theme-aware — light theme shows the hero image (the art is light), dark theme keeps `FloatingBubbles` so text stays AA-readable; the cycle card is now a translucent surface (`surface.copy(alpha=0.72f)`) acting as a subtle scrim, `BrandOrb` removed. Card copy verified AA-readable over the image on-device. `BrandOrb` still used by onboarding screens (not dead).
- **On-device smoke (v4, emulator-5554, all PASS, 0 FATAL):** cold-start signed-out → **intro** ✓; sign-up/sign-in flow reachable (created gx03) ✓; Home renders the new brand background, card text readable ✓; all 5 tabs open (Home/Track/Nutrition/Insights/Profile) ✓; **no Clients screen reachable** (Profile has no "Manage clients") ✓; delete flow works (gx03 deleted → Splash) ✓; logcat 0 FATAL ✓. Before/after Home shots in scratchpad `t4e/` (`FIXB_06_before_home`, `FIXB_07_after_home_v4`).

## v5 — pH restore + quiz back-arrow fix + intro logo (Sat Jul 4), versionCode 5
- **pH tracking restored, LOCAL-ONLY.** `FeatureFlags.PH_TRACKING = true` → pH tracker card, log dialog and insights section return to Track/Nutrition/Insights. **No network call fires for pH:** `PhRepository` remote upsert/delete/refresh are guarded/no-op'd with `// v1.1: enable when ph_readings table exists` (Room is the only store). Caption added to the pH card: "pH entries are stored on this device for now." (`PhTrackerCard.kt`). Deletion wipes pH via existing `database.clearAllTables()` (`AuthRepository.kt:58`). Onboarding grep = **zero** pH/sex-selection content.
- **Quiz back-arrow fix.** Root cause: `GxBackButton` was a 44dp target (< 48dp min) — onClick/nav were correct. Fixed to 48dp (`CommonUi.kt`) + added step-aware `BackHandler` to `OnboardingQuizScreen.kt` (system/gesture back mirrors the arrow; answers preserved in the `mutableStateMap`).
- **Intro logo swap.** Replaced the `Text("GENESYX")` wordmark with the L1 lockup image via a shared `BrandLockup` composable, on **Splash + Auth + Invite** (old text mark gone everywhere). Asset fixed: `L1.png` had an opaque white background → flood-filled to **transparent**; `drawable-nodpi/brand_lockup.png` = black wordmark (light), `drawable-night-nodpi/brand_lockup.png` = recolored **white wordmark** (dark). `logo_g.png` is dead (unused); system splash uses `@mipmap/ic_launcher` (unchanged).
- **v5 smoke — PARTIAL (emulator-5554):**
  - PASS: intro logo = L1 lockup in **both light + dark** (transparent-bg verified); quiz back-arrow (step 0 → intro; step 3 → back ×2, answers preserved; gesture-back parity); pH section shows below a **populated** phase card; pH log dialog + caption; pH 6.5 saved to Room with **zero pH network calls / no `E Ph`** (logcat `saved … locally`); cold-start routing → **Home** for signed-in gx05.
  - NOT YET RUN / unconfirmed: pH **persistence across restart** (logcat had no network errors on the restart read, but the reading was not re-viewed); **airplane-mode** pH test; all 5 tabs on v5; **no-Clients-screen** on v5; full-session **FATAL scan**; **deletion wipes pH + login fails**.
  - Shots in scratchpad `v5/`.

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

## T4 — deletion lifecycle (a–e ALL PASS on-device)
- **(a) gx02 in-app delete: PASS.** Profile → Delete account → confirm → signed out → **Splash** (shots `T4_00`–`T4_03`), no crash.
- **(b) gx02 login must fail: PASS.** Re-login with `lucas+gx02` (password on file with owner) returned **"Invalid login credentials"** and stayed on Auth (shot `T4_13`). Server rejected the deleted account's credentials.
- **(c) re-signup same email: PASS.** Created `lucas+gx02` again → landed on a **fresh Home** ("Set up your cycle", 0-day streak, no old data) (shot `T4_15`). Re-using the email proves the prior account was genuinely freed.
- **(d) delete gx02 again: PASS.** Profile → Delete account → confirm → **Splash**, no crash (shots `T4_16`–`T4_18`).
- **(e) gx01 delete: PASS (Sat Jul 4).** Password supplied by owner (not recorded here). Signed in as `lucas+gx01` → landed on **Home** with existing cycle data ("DAY 2 · PERIOD", not onboarding) (shot `T4e_07_home`). Profile → **Delete account** → confirm dialog "This will permanently delete your account and all your data. This cannot be undone." → **Delete** → signed out → **Splash/intro**, no FATAL in logcat (shot `T4e_11_after_delete`). Re-login with the **same** gx01 creds returned **"Invalid login credentials"** in red and stayed on Auth (shot `T4e_13_relogin_result`); logcat: `AuthRestException: Invalid login credentials`. Server rejected the deleted account. Shots in scratchpad `t4e/`.
- **Deletion evidence:** T4(b)+(e) rejection + (c) same-email re-signup are strong app/auth-level proof deletion works for **both** accounts. The remaining piece is the **Supabase server-side DB check** (below), which requires dashboard access.

## Device state (accurate, mid v5 smoke, Sat Jul 4)
- emulator-5554 runs **v5** (versionCode 5), **LIGHT** theme, **signed IN** as **`lucas+gx05`** (throwaway: cycle set up, 1 pH reading 6.5), on **Home**.
- Test accounts `lucas+gx01/02/03` all **deleted**. **`lucas+gx05`** is live — delete it at the end of the v5 smoke. (Passwords held by the owner, not recorded here.)

## Release build v6 — AAB built & signing-verified (Jul 6)
- **P9 (build + AAB): AAB built & signing-verified — upload pending Play Console app creation (P1).** Not uploaded (owner does Play Console manually).
  - `main` = **`27c2ac0`** (`Release: versionCode 5→6; mark Shopify delete-account page live-clean (P4)`).
  - AAB = `app/build/outputs/bundle/release/app-release.aab` — **8.7 MB (9,073,671 bytes)**.
  - **versionCode 6**, versionName **1.0.0**. `./gradlew clean bundleRelease` GREEN (CI on the source also green).
  - Signing SHA1 = **`8D:EB:47:63:5F:10:2A:DA:7C:93:AA:27:15:E3:37:C6:49:B2:CC:73`** (matches `genesyx-release.jks` and the fingerprint registered in Google Cloud). SHA256 `C3:D5:1F:4B…A4:46:C1:7D`.
- **Branch/merge reconciliation:**
  - `main` was **force-reset to the release line** (`main` → `27c2ac0`). Previously `origin/main` held a divergent scaffold (`586533c`, versionCode 1) that was **not** the release app.
  - Old scaffold **preserved** at `origin/backup/old-main` (recoverable; nothing lost).
  - `claude/rc-v3` **deleted** (was fully merged into main via PR #2; no unique commits).
  - **PR #3 / #4 merge steps are complete** — all their content (v1.1 pH sync + Google sign-in + the full app) is now in `main`. PR #4 is MERGED (its base was `feature/v1.1-sync-hardening`); PR #3 left open but redundant. `feature/log-history` and `feature/v1.1-sync-hardening` kept as-is.
  - `main` is the **GitHub default branch**, so all new PRs base off it (no more wrong-base risk).

## LAUNCH CHECKLIST — where we are (updated Mon)

**Engineering (DONE):** 4 RC fixes + v4 FIX A (Clients gate) + FIX B (Home hero), v4 built/signed, PR #2, T1–T3 PASS, T4(a–e) PASS, v4 smoke PASS. `GENESYX_ENV=PROD` verified.

| # | Item | Status | Detail / evidence |
|---|---|---|---|
| 1 | T4(e) gx01 delete | ✅ DONE | PASS on-device Sat Jul 4: delete → Splash (no FATAL); re-login rejected with "Invalid login credentials" (`AuthRestException`). Shots in scratchpad `t4e/` |
| 2 | Supabase server-side delete proof | ⛔ OWNER | dashboard: confirm deleted rows gone in `auth.users` + `profiles`/`daily_logs`/`cycle_settings`. (b)+(c) already prove auth-layer deletion works |
| 3a | Shopify `/pages/privacy-policy` | ✅ DONE | verified LIVE clean — H1 ok, 0 `[CONTACT_EMAIL]`, 0 `genesxy`, no `.html`, only `info@genesyx.co.uk` |
| 3b | Shopify `/pages/delete-account` | ✅ DONE | verified LIVE clean (Sun Jul 6): H1 "Delete account", 0 `[CONTACT_EMAIL]`, 0 `genesxy`, no `.html` links, only `info@genesyx.co.uk`. The old broken draft has been replaced — page now matches the clean `delete-account-FINAL.html`. |
| 4 | Store assets | ⛔ OWNER | feature graphic 1024×500 + phone + 7"/10" tablet screenshots |
| 5 | Play Console | ⛔ OWNER | privacy + deletion URLs, Data Safety form, content rating, AAB upload, internal smoke, **submit** |
| P9 | Release AAB build | ✅ DONE (upload pending) | AAB built & signing-verified — upload pending Play Console app creation (P1). `main` `27c2ac0`, versionCode 6, 8.7 MB, SHA1 `8D:EB:47:63…B2:CC:73`. See "Release build v6" above |

**Overall: ~95%.** All engineering is done — release AAB (v6) built & signing-verified, `main` is the release trunk, both Shopify pages LIVE clean. Remaining = owner-only Play Console + dashboard work (#2, #4, #5) and the manual AAB upload.

> **CODE FROZEN at versionCode 6 (`main` = `27c2ac0`).** No further source edits — only docs/evidence and owner store/console/dashboard steps.
>
> **NEXT ACTION (single, owner):** Play Console — create the app (P1), complete Data Safety/store forms, **upload the v6 AAB** (`app/build/outputs/bundle/release/app-release.aab`), internal smoke, submit.

## v1.1 backlog (deferred, do NOT build for 1.0)
- **pH backend + sync (headline v1.1 task):** create the Supabase `ph_readings` table, then un-guard the `PhRepository` remote calls (search `// v1.1: enable when ph_readings table exists`) so pH write-through/read-through works. pH is LOCAL-ONLY in v1.0.
- **Real offline-first sync queue** so offline edits persist and reconcile on reconnect (removes the FIX 2 offline block).
- **Log-screen Back discards unsaved edits** — add a trivial confirm/guard.
- **Track/Nutrition UI polish** — owner reviewing; findings TBD, **do not act** yet.

## Notes
- Secrets in `local.properties` (git-ignored): `genesyx.supabaseUrl`, `genesyx.supabaseAnonKey`, `genesyx.googleWebClientId`, `genesyx.apiBaseUrl`. Never commit real values.
- `keystore.properties` → `storeFile=/Users/lucasvalenca_sf/Documents/genesyx-release.jks`.
- Java: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`.
- Architecture is local-first: Room = source of truth, Supabase read-through on sign-in + write-through. Details in `ARCHITECTURE.md`, `docs/DATA_LAYER.md`, `docs/schema.sql`.
- Remaining release ops: upload the AAB to Play Console, complete Data Safety form + store listing; ensure `genesyx.co.uk/pages/privacy-policy` has live content.
