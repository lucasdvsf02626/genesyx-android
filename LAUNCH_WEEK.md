# Launch task list — path to Production

**Refreshed 2026-09-02.** This supersedes the v1.1-era checklist (July 2026 — see git history);
its engineering items are all closed or re-listed below with current status.

**Where we are:** `1.4.2 (23)` is live on Play **Internal testing** (1 Sep) and was verified on the
signed build against production Supabase — auth, password reset end-to-end, consent loop,
clear-sync, deletion to zero counts. Production still serves **1.2.0 (9), targetSdk 35**, and the
Play targetSdk-36 requirement took effect **31 Aug 2026** — only a Production publish clears it.
Truth sources: `CLAUDE.md` (STOPPED HERE box) and the top of `CHANGELOG.md`.

Legend: 🔴 blocks launch · 🟠 do before/with the promotion · 🟢 post-launch · 👤 owner/console · 🛠️ engineering

---

## 🔴 Blockers — the Production publish itself

### 1. 👤 Submit the Play Console drafts + buy time on the targetSdk policy
The Health apps declaration and Data Safety form are saved as **drafts, not submitted** — they gate
the Production publish, and *their review time, not the upload, is the long pole*. On **Policy
status → Issue details**, use **"Request more time"** for the targetSdk-36 requirement if not
already done (the date has passed).

### 2. 👤 Privacy-policy wording on `genesyx.co.uk`
The live policy must reflect vaginal-pH sync and the waitlist email before the Data Safety form it
backs is reviewed. Also resolve the Play URL-validator 429 warning on the deletion/privacy routes.

### 3. 👤 Owner approval → promote to Production
Promote the reviewed build from Internal testing. **If any code change lands first, the next
identity is versionCode 24** with its own `~/Documents/Genesyx Releases/1.4.2-code24/` directory —
codes 21/22 (and everything older) are superseded; never re-upload them.

---

## 🟠 Before / with the promotion

### 4. 👤🛠️ Physical-device QA on the Play-track install
`QA_CHECKLIST_ANDROID.md` rows that no emulator run can close:
- **Google Sign-In on the Play-delivered build** — Play re-signs with its app-signing key
  (`E0:CE…`); verify that fingerprint is registered on the Android OAuth client, not just the
  upload key (`8D:EB…CC:73`).
- The reset email end-to-end on a real handset (emulator pass is green, 1 Sep).
- A reminder firing, Home deep links, the Track → pH → Track walk.

### 5. 👤 Ten-minute console verification pass
Old checklist items never re-verified — confirm, don't assume:
- Supabase Auth → Email → **"Confirm email" is ON** (QA once left it off).
- Google Cloud → **OAuth consent screen is Published** (not Testing).
- Web OAuth client has only the current secret (rotate out the stale July-1 one if still present).
- Supabase **Pro upgrade decision** (the keep-alive workflow is interim mitigation only).
- Android OAuth client carries **both** SHA-1s: upload key `8D:EB…CC:73` AND Play's app-signing
  key `E0:CE…` (without the latter, Google Sign-In fails only on Play-installed builds).

A ready-to-paste prompt for a browser agent to run this whole pass is below —
see **"Browser-agent prompt — console verification pass"**.

---

## Browser-agent prompt — console verification pass

Paste everything in the block below into a browser-driving agent session signed in to the
Supabase dashboard and Google Cloud Console. It runs item 5 (and the flip in item 5a) end to end.

```
You are running a production console verification pass for the Genesyx apps (Android
com.genesyx.app + iOS, one shared Supabase project: epltxklawpcxxbaleswg). Work item by
item, in order. For every toggle or state you report, verify it visually AND in the DOM
(aria-checked / data-state) — never trust a label alone. Change NOTHING except what a task
explicitly tells you to change. If a screen doesn't match what a task describes, stop that
task, report what you actually see, and move on.

GOAL: every task below ends ✅ verified-correct, ✅ fixed-and-verified, or ❌ blocked with
the exact reason and a screenshot-level description of what was on screen.

TASK 1 — Supabase "Confirm email" (the one change you are authorised to make)
  Where: Supabase Dashboard → project epltxklawpcxxbaleswg → Authentication →
  Sign In / Providers → User Signups.
  Check: the "Confirm email" toggle state.
  Achieve: it must be ON. If OFF, flip it ON and Save, then re-read the toggle from the
  DOM after the save round-trips to confirm it stuck.
  Also record (do not change): Allow new users to sign up / manual linking / anonymous
  sign-ins states.

TASK 2 — Supabase confirmation redirect sanity
  Where: same project → Authentication → URL Configuration.
  Check: the Site URL and the Redirect URLs allow-list.
  Achieve: report them verbatim. The allow-list must still contain genesyx://reset-password.
  The Site URL must be a real, live page (a genesyx.co.uk address, not a staging/lovable
  URL) — email-confirmation links land there. Flag, don't fix, anything off.

TASK 3 — Supabase unconfirmed-accounts census (read-only)
  Where: same project → SQL editor.
  Run: select count(*) from auth.users where email_confirmed_at is null;
  Achieve: report the count only. Do NOT delete or modify any user — the project is shared
  with iOS.

TASK 4 — Google Cloud OAuth consent screen
  Where: Google Cloud Console → the project holding the Genesyx OAuth clients →
  APIs & Services → OAuth consent screen.
  Check: Publishing status.
  Achieve: it must read "In production" / Published. If it reads "Testing", publish it
  (button: "Publish app") and confirm the status changed. Record the user type and any
  verification warnings shown.

TASK 5 — Web OAuth client secrets
  Where: APIs & Services → Credentials → the Web application OAuth 2.0 client →
  Client secrets.
  Check: how many secrets exist and their creation dates.
  Achieve: exactly one current secret. If a stale secret dated ~July 1 2026 is still
  listed alongside a newer one, report both dates and STOP — do not delete it yourself;
  deletion needs the owner to confirm nothing still references it.

TASK 6 — Android OAuth client fingerprints
  Where: APIs & Services → Credentials → the Android OAuth 2.0 client
  (package com.genesyx.app).
  Check: the registered SHA-1 certificate fingerprints.
  Achieve: BOTH of these present — upload key 8D:EB:47:63:5F:10:2A:DA:7C:93:AA:27:15:E3:37:C6:49:B2:CC:73
  and the Google Play app-signing key (starts E0:CE — read its full value from Play
  Console → Setup → App signing if needed). If the E0:CE one is missing, add it via
  "Add fingerprint" and save. This is what makes Google Sign-In work on Play-installed
  builds.

TASK 7 — Supabase billing tier (read-only)
  Where: Supabase Dashboard → project settings → Billing.
  Check: current plan.
  Achieve: report the plan name. If Free, note that auto-pause risk stands and the
  keep-alive GitHub workflow is the only mitigation — the Pro decision is the owner's,
  not yours.

REPORT: finish with a numbered list, one line per task: state found → action taken →
state after. Anything you could not verify goes under "Blocked", with the reason.
```

**After the agent reports back:** re-run one fresh sign-up end to end on the app
(confirmation email arrives via the custom SMTP, link confirms, sign-in succeeds, and an
unconfirmed sign-in shows the in-app "confirm your email" copy — the client handles this via
its typed auth-error mapping). Then tick this item off and update the ledger below.

### 6. 👤🛠️ Crash-reporting decision
`core/log/Logger.kt` + `core/log/Analytics.kt` are deliberate no-ops — production launches blind to
field crashes. Either wire Crashlytics (and amend the Data Safety form to match) or explicitly
accept blind launch for v1. Decide before promotion; wiring after review means re-review.

### 7. 👤 `app_config` deployment decision
The minimum-version gate ships in code 23 but is **inert and fail-open** until the `app_config`
table exists with a real `min_supported_build`. Deploy it with a deliberate minimum, or leave it
inert — do not populate it casually.

---

## 🟢 Post-launch follow-ups

### 8. 🛠️ Serialise `pushOrQueue` with `refresh` in `data/DailyLogRepository.kt`
Closes the known pull-vs-push race: the documented test flake, plus the narrow production window
where a stale pull briefly shows locally. Keep the existing test assertion; do not weaken it.

### 9. 🛠️ Deep-link host
Invite links still use `genesis-cycle-guide.lovable.app` (staging, `autoVerify=false`) in
`AndroidManifest.xml` + `GenesyxNavGraph.kt`. Harmless while `PARTNER_INVITES = false`; move to the
brand domain + `assetlinks.json` before partner invites ship. (`genesyx://reset-password` is live
and separate.)

### 10. 🛠️ Dependency bump pass
Kotlin/AGP/Compose/Hilt/Room/supabase/ktor drift — bump with the 649-test suite as the net.

### 11. 🛠️ iOS parity batch
Labels, two-band thresholds, `measurement_type`, copy, logged-days hydration average — and the
waitlist RPC if iOS gains the screen. (`ANDROID_PARITY.md` lives in the iOS repo.)

### 12. Content / product (owner)
Article 7 (Shettles) + website-science links (blocked on medical/content sign-off), "background
eggs" beyond Splash (asset decision), Nutrition "Why important?" dropdowns, phase→specific-article
link, partner-sharing build (after backend contract + email provider), false-offline repro on
cellular.

---

## Done ledger (from the July checklist — do not redo)

- ✅ Release keystore + SHA-1 (`genesyx-release.jks`, registered; release tasks fail without it).
- ✅ Stale pH caption — card now discloses sync (*"pH entries sync to your Genesyx account."* — keep it).
- ✅ Daily-log offline queue (Room `syncStatus` + WorkManager drain; verified on-device 13 Jul).
- ✅ Guest → signed-in adoption (1 Sep launch-audit patch: pH, daily logs, cycle settings,
  supplements, consent — `AuthRepository.syncHealthStores`).
- ✅ Password reset end-to-end (custom SMTP via Resend, deep link, in-app completion — 1 Sep).
- ✅ FCM: **decided against** — both platforms are local-notifications-only by design.
