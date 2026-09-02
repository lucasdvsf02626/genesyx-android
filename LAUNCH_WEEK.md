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
