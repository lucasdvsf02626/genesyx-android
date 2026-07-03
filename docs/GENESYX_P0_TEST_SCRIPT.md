# Genesyx Android — P0 On-Device Test Script (RC 1.0.0 / versionCode 2)

Run this against the **release APK** on a real device (or a clean emulator). It is the go/no-go
gate for the Play upload. Every step has an explicit **Expected** result — a single ✗ is a release
blocker. **Do the steps in order; DELETE ACCOUNT is intentionally last.**

## Under test
- APK: `app/build/outputs/apk/release/app-release.apk` (3.9 MB, signed, minified)
- Package / launcher: `com.genesyx.app` / `com.genesyx.app.MainActivity`
- Backend: Supabase (real email/password auth + Room→Supabase sync). pH is gated OFF (`FeatureFlags.PH_TRACKING=false`) — **it must be invisible everywhere**.

## Preconditions
- Device on Android 8.0+ (minSdk 26). Network on.
- A **fresh** e-mail you control, using a plus-address so it can be reused after deletion, e.g.
  `you+p0a@yourdomain.com`. Keep the inbox open in case email confirmation is ON.
- Uninstall any prior build:
  ```
  adb uninstall com.genesyx.app
  adb install -r app/build/outputs/apk/release/app-release.apk
  ```
- Optional live log tail in a second terminal: `adb logcat -c && adb logcat | grep -i genesyx`

---

## Test steps

### 1. Clean install — cold start
- **Action:** `adb shell am start -n com.genesyx.app/.MainActivity` (or tap the launcher icon).
- **Expected:** Splash → onboarding intro appears. **No crash, no ANR.** Process stays alive
  (`adb shell pidof com.genesyx.app` returns a PID after 5 s).

### 2. Onboarding
- **Action:** Walk the full onboarding (intro → quiz → readiness summary → auth/waitlist as designed).
- **Expected:** Every screen renders; forward/back work; no pH copy anywhere; lands on the auth screen.

### 3. Fresh sign-up (plus-address)
- **Action:** Choose sign-up, enter `you+p0a@yourdomain.com` + a password, submit.
- **Expected:**
  - If email confirmation is **OFF** (current intended state): account is created and you are signed
    in, landing on Home.
  - If confirmation is **ON**: app says to confirm; open the inbox, click the link, return, sign in.
    (Note this in results — it affects the Data Safety / UX story.)
- **Negative check:** re-open auth, try the **wrong** password → clear error, no crash.

### 4. Cycle setup
- **Action:** Complete cycle setup (last period date, cycle length, period length).
- **Expected:** Values save; Home reflects the cycle (day/phase). No pH fields present.

### 5. Daily log — create + edit
- **Action:** Create today's log (mood/energy/symptoms/water/notes), save. Re-open and **edit** it.
- **Expected:** First save persists; edit **overwrites the same day** (no duplicate row); values reload
  correctly when you revisit.

### 6. Offline log + reconnect sync
- **Action:** Enable **airplane mode**. Create/edit a log while offline. Re-enable network; wait ~15 s.
- **Expected:** The offline write is accepted locally (UI updates immediately — Room is source of
  truth). After reconnect it syncs to Supabase with no crash and no data loss. Confirm the row landed
  server-side (Supabase dashboard `daily_logs`, filtered to this user, or `logcat` sync line).

### 7. Theme toggle
- **Action:** Profile → toggle dark/light.
- **Expected:** Theme flips live across the app; no relayout crash.

### 8. Rotation mid-screen
- **Action:** On a data screen (Home or a log), rotate the device (or `adb shell settings put system
  accelerometer_rotation 1` then rotate).
- **Expected:** No crash; state/scroll position preserved; content intact.

### 9. Sign out / sign in
- **Action:** Profile → sign out. Then sign back in with the same credentials.
- **Expected:** Sign-out returns to start. Sign-in restores the account and **read-syncs** the cycle +
  logs created above (they reappear from Supabase, proving remote persistence).

### 10. Force-close → reopen (session survives)
- **Action:** `adb shell am force-stop com.genesyx.app`, then relaunch.
- **Expected:** App reopens **already signed in** (session persisted in DataStore) — no re-login. Data
  present.

### 11. In-app privacy link opens the live page
- **Action:** Profile → **Privacy & Data**.
- **Expected:** Opens a browser to `https://genesyx.co.uk/pages/privacy-policy` showing **real content**
  (not a 404 / placeholder). Back returns to the app cleanly.
  - ⚠️ If the Shopify page is empty/404, that is a **Play submission blocker** (open item #3), not an
    app-code bug — record it.

### 12. DELETE ACCOUNT — **do this last**
- **Action:** Profile → delete account → confirm in the dialog.
- **Expected, in order:**
  1. **Progress** state shows while deleting (button disabled / spinner).
  2. On success the app **signs out and navigates to the start** (Splash), back stack cleared —
     pressing back does **not** return to a signed-in screen.
  3. On failure a clear error is shown and you stay put (no half-deleted limbo).
- **Post-delete verification:** try to sign in again with the **same** email/password →
  **rejected** (account no longer exists). If sign-in still succeeds, deletion did **not** remove the
  `auth.users` row → open item #2 (RPC likely needs an Edge Function). Record it.

### 13. Re-signup same email succeeds
- **Action:** After a confirmed deletion, sign **up** again with the **same** `you+p0a@...` address.
- **Expected:** Sign-up succeeds (email fully freed). Fresh, empty account — none of the old data
  returns.

---

## Result log
| # | Step | Pass/Fail | Notes |
|---|------|-----------|-------|
| 1 | Clean install / cold start | | |
| 2 | Onboarding | | |
| 3 | Fresh sign-up | | |
| 4 | Cycle setup | | |
| 5 | Daily log create + edit | | |
| 6 | Offline log + reconnect sync | | |
| 7 | Theme toggle | | |
| 8 | Rotation | | |
| 9 | Sign out / sign in | | |
| 10 | Force-close reopen (session survives) | | |
| 11 | Privacy link → live page | | |
| 12 | Delete account (progress→out→start→cannot re-login) | | |
| 13 | Re-signup same email | | |

**Go/no-go:** all 13 ✓ = ship-ready on the app side. Steps **11** and **12/13** also depend on the
two backend open items (Shopify page content; `delete_current_user` actually removing the auth user) —
a fail there is a backend/config blocker, still release-blocking, tracked in `CLAUDE.md`.
