# Launch Week — consolidated checklist

Every deferred / pre-launch item from `FINAL_REPORT.md`, with exact locations. Supabase project ref:
`epltxklawpcxxbaleswg`. Google Cloud project: the one holding the Genesyx OAuth clients.

Legend: 🔴 blocks launch · 🟠 should-do before launch · 🟢 post-launch follow-up · 👤 owner · 🛠️ engineering

---

## 🔴 Blockers

### 1. 👤 Release keystore + release SHA-1 (release Google sign-in is dead until done)
Only the **debug** SHA-1 is registered; **no release keystore exists**. `assembleRelease` passing is
build-only — release *signing* isn't set up, and Google sign-in fails on release builds.
1. Create the keystore:
   `keytool -genkeypair -v -keystore genesyx-release.jks -alias genesyx -keyalg RSA -keysize 2048 -validity 10000`
2. Get its SHA-1: `keytool -list -v -keystore genesyx-release.jks -alias genesyx` → copy `SHA1:`.
3. Register it: **Google Cloud Console → APIs & Services → Credentials → OAuth 2.0 Client IDs →
   [Android client] → "Add fingerprint" → paste SHA-1 → Save.**
4. Fill `keystore.properties` (repo root, git-ignored): `storeFile`, `storePassword`, `keyAlias`,
   `keyPassword`. (Path already referenced by `app/build.gradle.kts`.)

### 2. 👤 Re-enable "Confirm email" in Supabase (currently OFF — QA left it off)
Leaving it off in production lets anyone create accounts with unverified/typo'd emails (spam/abuse,
undeliverable resets).
- **Supabase Dashboard → Authentication → Providers → Email → toggle "Confirm email" ON → Save.**
  (`https://supabase.com/dashboard/project/epltxklawpcxxbaleswg/auth/providers`)

---

## 🟠 Should-do before launch

### 3. 👤 Publish the OAuth consent screen to production
Currently in "Testing" → only allowlisted Google accounts can sign in.
- **Google Cloud Console → APIs & Services → OAuth consent screen → "Publish app" → confirm.**

### 4. 👤 Delete the old July-1 OAuth client secret (Google Cloud)
Rotate/remove the stale secret dated July 1 so only the current secret is live.
- **Google Cloud Console → APIs & Services → Credentials → [Web OAuth client] → "Client secrets"
  section → delete the July-1 secret** (keep the current one). Confirm nothing in use references it.

### 5. 👤 Supabase Pro upgrade
Removes free-tier auto-pause risk. The keep-alive workflow
(`.github/workflows/supabase-keepalive.yml`) is the interim mitigation only.
- **Supabase Dashboard → Settings → Billing → Upgrade to Pro.**
  (`https://supabase.com/dashboard/project/epltxklawpcxxbaleswg/settings/billing`)

### 6. 🛠️ Fix the stale pH caption (found during the autonomous run)
`PhTrackerCard.kt` shows **"pH entries are stored on this device for now."** — true when pH was
local-only, but **Phase 3 made pH sync** (it's now local-only *only for guests*). The copy is
misleading for signed-in users. Options: make it conditional on sign-in, or reword neutrally
(e.g. "pH entries sync to your account when you're signed in."). Product decision on wording — left
un-edited deliberately.

---

## 🟢 Post-launch follow-ups (engineering)

### 7. 🛠️ Daily-log offline sync queue (retires the FIX 2 band-aid)
Offline daily-log saves are still **blocked** via `LogViewModel.isOnline()`. Apply the same
offline-first queue pattern pH now uses (WorkManager + PENDING states) to `DailyLogRepository`.

### 8. 🛠️ Guest → signed-in data migration
pH rows written while signed out are scoped to `local-user` and are **stranded** on sign-in
(not adopted by the new `user_id`). Adopt local guest rows into the account on first sign-in.

### 9. 🛠️ FCM / push notifications
Not started — no `firebase-messaging`, no service, no `POST_NOTIFICATIONS`. Needs Firebase project
wiring + code.

### 10. 🛠️ Deep-link host
Manifest + nav still use `genesis-cycle-guide.lovable.app` (Lovable staging, `autoVerify=false`).
Move to the brand domain and add `assetlinks.json` before relying on web→app links.

### 11. 🛠️ Dependency bump pass
Kotlin/AGP/Compose/Hilt/Room/supabase/ktor are ~1–2 minors behind. Bump after this PR merges, with
the now-larger test suite as the net. (Explicitly NOT done in this PR.)

---

## Quick status
- Engineering for v1.1 (sync + Google sign-in + integrity): **done, on `feature/v1.1-sync-hardening`,
  PR #3.** 51 unit + 2 instrumented tests green; pH sync validated end-to-end on-device.
- Remaining to ship: items 1–5 (owner) + on-device Google/account-deletion manual checks
  (`MANUAL_TEST_CHECKLIST.md`).
