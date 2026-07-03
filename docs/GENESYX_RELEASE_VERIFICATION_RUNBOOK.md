# Genesyx — Release Verification Runbook

_Generated 2 Jul 2026. Package `com.genesyx.app`, versionCode 1 / versionName 1.0.0, compileSdk 35, minSdk 26._

This runbook reflects what the **actual codebase** does, not the original checklist assumptions. Read the "Corrections to the original plan" box first — two of your five steps were based on premises that don't hold in this repo.

---

## ⚠️ Corrections to the original plan (read first)

| # | Original assumption | Reality in this repo | What to do instead |
|---|---|---|---|
| 2 | Supabase keys live in `local.properties` under `SUPABASE_URL` / `SUPABASE_ANON_KEY` | The build reads **Gradle properties** `genesyx.supabaseUrl` / `genesyx.supabaseAnonKey` (`app/build.gradle.kts:36–45`) → injected as `BuildConfig.*`. `local.properties` is only read for `sdk.dir`. Putting Supabase names there does **nothing**. | Set the two `genesyx.*` props — but **not** in the repo's `gradle.properties` (it's git-tracked). Use `~/.gradle/gradle.properties` (see Step 2). |
| 1 | `./gradlew assembleDebug` is the fast local check | Correct approach, but must run in **your** Android Studio (JDK 17). I could not run it here: the sandbox has JDK 11 and no Android SDK. | Run locally per Step 1. |
| 4 | App uses Google OAuth (needs Web + Android clients + SHA-1) | **"Continue with Google" is a non-functional stub** — `AuthScreen.kt:158` just calls `onSignIn("you@genesyx.app", …)`. No idToken / real Google flow exists anywhere. | The Android OAuth client currently powers nothing. Decide: ship without Google, or implement it (see Step 4). Don't block release chasing SHA-1 for a feature that isn't wired. |

---

## Step 1 — Local debug build (in your Android Studio)

Goal: confirm the app compiles and is testable.

1. Android Studio → **Settings → Build Tools → Gradle → Gradle JDK = 17** (project uses AGP/Kotlin toolchains that expect 17; JDK 11 will fail).
2. Terminal at project root:
   ```bash
   ./gradlew assembleDebug --stacktrace
   ```
3. Expected: `BUILD SUCCESSFUL`, APK at `app/build/outputs/apk/debug/app-debug.apk`.
4. If it fails, paste the first error to me. Most likely causes here: wrong Gradle JDK, or missing `sdk.dir` (yours is set: `/Users/lucasvalenca_sf/Library/Android/sdk`).

> Note: the app will **build** with empty Supabase keys, but auth/data calls will fail at runtime until Step 2 is done. So "builds" ≠ "testable end-to-end".

---

## Step 2 — Supabase keys (the real fix)

The build pulls `genesyx.supabaseUrl` / `genesyx.supabaseAnonKey`. Both are currently **empty** in the tracked `gradle.properties`. Do **not** paste secrets into that file (it's committed to git).

**Recommended — user-level Gradle props (outside the repo, not in git):**

Edit (create if absent) `~/.gradle/gradle.properties` and add:
```properties
genesyx.supabaseUrl=https://<your-project-ref>.supabase.co
genesyx.supabaseAnonKey=<your-anon-public-key>
```
Gradle auto-merges these as project properties for every build. Get both values from Supabase Dashboard → **Project Settings → API** (Project URL + `anon` `public` key).

Verify after setting:
```bash
./gradlew :app:assembleDebug -q
# then confirm they landed in BuildConfig:
cat app/build/generated/source/buildConfig/debug/com/genesyx/app/BuildConfig.java | grep SUPABASE
```
Both fields should be non-empty strings.

Alternatives (pick one, don't combine): pass `-Pgenesyx.supabaseUrl=… -Pgenesyx.supabaseAnonKey=…` on the CLI, or set CI env `ORG_GRADLE_PROJECT_genesyx.supabaseUrl` etc.

---

## Step 3 — Supabase dashboard review

The repo's source of truth is `docs/schema.sql`. Verify the live project matches.

**3a. Tables (exactly 5, schema `public`)** — Table Editor:
- [ ] `profiles`  (PK `id` = `auth.users.id`; has `partner_id`, `display_name`, `avatar_url`)
- [ ] `cycle_settings`  (`user_id`)
- [ ] `daily_logs`  (`user_id`)
- [ ] `ph_readings`  (`user_id`)
- [ ] `partner_invites`  (inviter/invitee by email; drives the `genesyx://invite/{code}` deep link)

**3b. RLS enabled on all 5** — Authentication → Policies (or Table Editor → shield icon). Every table above must show **RLS enabled**. Expected policy shape from schema:
- [ ] `cycle_settings`, `daily_logs`, `ph_readings`: 4 policies each (SELECT/INSERT/UPDATE/DELETE), all keyed on `auth.uid() = user_id`.
- [ ] `profiles`: SELECT (own **or** partner), INSERT (own), UPDATE (own, no partner_id write). No DELETE policy — confirm that's intended.
- [ ] `partner_invites`: inviter-sees-own, invitee-sees-by-email, inviter-creates, inviter-revokes. No DELETE policy by design.
- [ ] Spot-check: signed-in user A cannot read user B's `daily_logs` (SQL editor with `set role`/JWT, or two test accounts).

**3c. `handle_new_user` trigger** — Database → Functions & Triggers:
- [ ] Function `public.handle_new_user()` exists, `SECURITY DEFINER`, `search_path = public`, inserts into `profiles` with `ON CONFLICT (id) DO NOTHING`.
- [ ] A trigger **AFTER INSERT ON `auth.users`** actually calls it. ⚠️ The schema comment says this is "attached by Lovable managed setup" but the trigger itself is **not in `schema.sql`** — confirm it truly exists in the live DB, else new signups won't get a profile row. Test: create a throwaway user → a matching `profiles` row should appear.
- [ ] `touch_updated_at()` exists but is intentionally **not attached** to any table — fine, no action.

**3d. Auth providers** — Authentication → Providers:
- [ ] **Email** enabled. Decide on "Confirm email" — if ON, testers must verify before login; for internal testing you may want it OFF or use pre-confirmed users.
- [ ] **Google** provider: only needed if you actually implement Google sign-in (see Step 4). As-is, leave it, but it's not exercised by the app.

**3e. API values** — Project Settings → API:
- [ ] Project URL and `anon` key match what you put in `~/.gradle/gradle.properties` (Step 2).
- [ ] Never ship the `service_role` key in the app.

---

## Step 4 — Google Cloud OAuth review

**Reality check:** the app does **not** perform Google OAuth today (`AuthScreen.kt:158` is a stub). So the Android OAuth client is currently unused. Two paths:

**Path A — ship without Google now (fastest):** Remove or disable the "Continue with Google" button so testers aren't shown a control that fakes a login. Skip the OAuth client work. Revisit later.

**Path B — actually implement Google sign-in:** then verify in Google Cloud Console → APIs & Services → Credentials:
- [ ] **Web application** OAuth client exists. Its client ID is the `serverClientId` you pass to the Credential Manager / Supabase `signInWith(IDToken)` flow. Supabase → Auth → Providers → Google also needs this Web client ID + secret.
- [ ] **Android** OAuth client exists with package `com.genesyx.app` and the correct **SHA-1**.
- [ ] SHA-1s registered (add all that apply):
  - Debug: `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android`
  - Release/upload + Play App Signing: get from `./gradlew signingReport`, and from Play Console → Setup → **App signing** (Google-managed signing key SHA-1 must also be registered or Google login breaks in production).
- [ ] Wire the button: replace the stub onClick with a real Credential Manager Google flow → pass the returned idToken to Supabase Auth. (I can implement this incrementally — say the word.)

> Your `keystore.properties` exists locally (release signing configured), so `signingReport` will show a real release SHA-1. When keystore.properties is absent, the release build falls back to debug signing (`app/build.gradle.kts:66–70`).

---

## Step 5 — Play Console review (internal testing)

- [ ] **Internal testing** track: an artifact is uploaded and status is **Released** (not Draft). Note: build is `versionCode 1` — every re-upload needs a higher `versionCode` (bump in `app/build.gradle.kts:30`).
- [ ] **App signing**: enrolled in Play App Signing; record the app-signing SHA-1 (feeds Step 4 Path B).
- [ ] **App content** (Policy → App content) — all required declarations complete: Privacy policy URL, Data safety, Ads, Content rating, Target audience, News app, Health apps declaration (Genesyx tracks cycle/health data → expect the **health data** and sensitive-data questions), Government apps, Financial features (N/A).
  - ⚠️ Cycle/pH/health logging = sensitive personal data. Data safety form must disclose collection + Supabase storage, and you need a real **privacy policy URL**.
- [ ] **Store listing**: app name, short + full description, app icon (512×512), feature graphic (1024×500), ≥2 phone screenshots, category, contact email.
- [ ] **Testers**: internal tester list/email added; opt-in URL shared with testers.
- [ ] **Dashboard blockers**: resolve any "Errors"/"To do" items on the Dashboard and Publishing overview. Confirm no pending review or policy flags.
- [ ] Deep-link sanity: the manifest also verifies `https://genesis-cycle-guide.lovable.app` (autoVerify=false) and handles `genesyx://invite/{code}`. Confirm that Lovable host is intended for the shipped app.

---

## Quick status of what I verified locally

| Item | Result |
|---|---|
| `local.properties` has `sdk.dir` | ✅ present, points to your SDK |
| `local.properties` has Supabase keys | ❌ absent — **and not read from here anyway** |
| Supabase keys in `gradle.properties` | ⚠️ present but **empty** (`genesyx.supabaseUrl=` / `…AnonKey=`) |
| `gradle.properties` git-tracked | ⚠️ yes — don't put secrets in it |
| 5 tables + RLS + policies in `docs/schema.sql` | ✅ all present |
| `handle_new_user` function | ✅ defined; ⚠️ its `auth.users` trigger is **not** in the repo — verify live |
| Google sign-in | ❌ stub only, no real OAuth in code |
| Local build in sandbox | ❌ not possible (JDK 11, no Android SDK) → run in your Android Studio |
