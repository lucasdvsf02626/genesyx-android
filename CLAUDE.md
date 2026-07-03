# CLAUDE.md — Genesyx Android

Project Name: Genesyx Android

Release-candidate handoff. Read this first. Honest state as of the commit below.

## Build identity
- **RC build:** versionCode `2`, versionName `1.0.0`  (`app/build.gradle.kts`)
- **Last commit:** `c066424` — "RC prep: FeatureFlags pH gate, AppLinks privacy URL, delete flow success/error/nav, versionCode bump"
- **Release artifacts** (built & signed with `genesyx-release.jks` via `keystore.properties`; `assembleDebug`, `assembleRelease`, `bundleRelease` all green):
  - AAB (Play upload): `app/build/outputs/bundle/release/app-release.aab` — 7.5 MB
  - APK (on-device test): `app/build/outputs/apk/release/app-release.apk` — 3.9 MB
  - Install: `adb install -r app/build/outputs/apk/release/app-release.apk`

## What THIS session actually fixed (file by file, all verified compiling)
- `core/FeatureFlags.kt` **(NEW)** — `PH_TRACKING = false`. pH is compile-time gated OFF for 1.0.
- `core/AppLinks.kt` **(NEW)** — `PRIVACY_POLICY_URL = https://genesyx.co.uk/pages/privacy-policy`, `DELETE_ACCOUNT_URL = https://genesyx.co.uk/pages/delete-account`.
- `ui/track/TrackScreen.kt` — `PhTrackerSection()` wrapped in `if (FeatureFlags.PH_TRACKING)`.
- `ui/nutrition/NutritionScreen.kt` — pH section wrapped in `if (FeatureFlags.PH_TRACKING)`.
- `ui/insights/InsightsScreen.kt` — `PhInsightsSection` wrapped in `if (FeatureFlags.PH_TRACKING)`.
- `ui/home/HomeScreen.kt` — pH removed from subtitle copy ("Cycle setup, daily logs, and profile sync.").
- `ui/profile/ProfileScreen.kt` — removed "pH readings" from detail copy; **"Privacy & Data" row now opens `PRIVACY_POLICY_URL`** via `ACTION_VIEW` intent; added `LaunchedEffect(accountDeleted)` that navigates to `Screen.Splash` with `popUpTo(0){inclusive=true}` after delete.
- `ui/profile/ProfileViewModel.kt` — added `deleted: StateFlow<Boolean>`; `deleteAccount()` now sets `deleted=true` on `DataResult.Success`, surfaces message on `Error` (alongside existing `deleting`/`deleteError`).
- `app/build.gradle.kts` — `versionCode 1 → 2`.

**Audit (grep):** no user-visible pH strings remain reachable outside the gated components. No dead/placeholder privacy URLs.

## Task 0 findings (state found at session start, now resolved)
- FeatureFlags did not exist → created, pH gated OFF. ✅
- pH was user-visible on Track/Nutrition/Insights/Home/Profile → gated + strings cleaned. ✅
- `versionCode` was still `1` → bumped to `2`. ✅
- In-app privacy link was a dead in-app detail pane, not a real URL → wired to live page. ✅
- Delete-account flow had success/error but **no nav-to-start** → added. ✅
- No project `CLAUDE.md` → this file. ✅
- Delete RPC (`delete_current_user`) wiring is present in `SupabaseAuthService`/`AuthRepository` (calls RPC, clears Room, signs out). Remote deployment NOT yet verified on device — see open items.

## Remaining (fresh session)
1. **Write & run the on-device P0 test script** (Task 3 — not yet written). Clean install cold start → onboarding → fresh sign-up (plus-address) → cycle setup → daily log create+edit → airplane-mode log + reconnect sync → theme toggle → rotation → sign out/in → force-close reopen (session survives) → tap in-app privacy link (opens live page) → **DELETE ACCOUNT last** (progress → signed out → returns to start → cannot log back in) → re-signup same email succeeds.
2. **Supabase:** verify `ph_readings` table + `delete_current_user` RPC are deployed and in the PostgREST schema cache (recurring PGRST205/PGRST202; fix via Dashboard "Reload schema cache"). Account deletion from `auth.users` likely needs an Edge Function, not a plain SECURITY DEFINER RPC.
3. **Shopify pages content:** ensure `genesyx.co.uk/pages/privacy-policy` and `/pages/delete-account` have real, live content.
4. **Play Console:** upload the AAB, complete Data Safety form + store listing.

## SINGLE NEXT ACTION
Write the numbered on-device P0 test script (Task 3), then run it against the release APK. Everything else waits on a clean P0 pass.

## Notes for whoever continues
- Secrets in `local.properties` (git-ignored): `genesyx.supabaseUrl`, `genesyx.supabaseAnonKey`, `genesyx.googleWebClientId`, `genesyx.apiBaseUrl`. Never commit real values.
- Java: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`.
- To re-enable pH later: flip `FeatureFlags.PH_TRACKING = true` (single switch, no other code changes).
- Architecture is local-first: Room = source of truth, Supabase read-through on sign-in + write-through. Details in `ARCHITECTURE.md`, `docs/DATA_LAYER.md`, `docs/schema.sql`.
