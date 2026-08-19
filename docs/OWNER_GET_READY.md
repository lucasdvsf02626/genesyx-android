# What you need to get Genesyx Android ready

**For:** you (owner)  
**Date:** 18 Aug 2026  
**Source in this tree:** `1.4.0` / versionCode **14**  
**Honest status:** the product change list is in the code. A **new signed 1.4.0 / code 14** AAB was built from this tree on **18 Aug 2026**. It is **not** on Play. You still have to disclose, phone-QA, and upload.

### Artifact built this session (18 Aug 2026)

| | |
|---|---|
| Folder | `~/Documents/Genesyx Releases/1.4.0-code14-20260818/` |
| AAB (Play) | `genesyx-1.4.0-code14-20260818.aab` |
| APK (phone) | `genesyx-1.4.0-code14-20260818.apk` |
| Identity | `com.genesyx.app` · versionCode **14** · versionName **1.4.0** · targetSdk **36** |
| AAB SHA-256 | `546a2815169402faf35be7fd2963a1a2b3356f140edbd4c01c4cdaa606bc3daf` |
| APK SHA-256 | `aa283943bc5edf2036be4b8d8c123cd36459a16593a21bc30e000dda7d7975fd` |
| Signing SHA-1 | `8deb47635f102ada7c93aa2715e337c649b2cc73` (matches the registered release key) |
| Lint / R8 | `:app:lintRelease` + `:app:assembleRelease` + `:app:bundleRelease` **BUILD SUCCESSFUL** |
| Launch check | Installed on emulator-5554 → splash: *Start Your Personalised Quiz* / *Sign in* |

This is **not** the 12 Aug archive in `1.4.0-code14/`. That older AAB does **not** include the later Learn / Free Guide / Medical Sources work. Upload **this** dated folder, not the 12 Aug one.

**Backend:** REST-probed 18 Aug. `user_supplements` and `genesyx_products` exist (HTTP 401 / permission denied for anon — **not** PGRST205 missing). The 12 Aug “tables missing” upload gate is closed. You still need the **deletion-with-pH** proof.

Engineering cannot close the items below. They are yours.

---

## Read this first

1. Code is ahead of the store. Last recorded Play check (28 Jul 2026): Internal **1.3.0 (11)**, Production **1.2.0 (9)**. Confirm live tracks in Play Console before you upload.
2. **Do not upload** any archived AAB older than this tree (codes 10, 11, 12, 13). Play will not reuse a versionCode.
3. **Do not upload code 14** until the `user_supplements` / `genesyx_products` migration is applied on production Supabase. Last REST probe (12 Aug) was **PGRST205 — tables missing**. Shipping the app against a missing table means the Genesyx supplement range stays empty or errors.
4. pH is **vaginal**, range **3.8–7.0**, Healthy **3.8–4.5**, Elevated **> 4.5**, and **it syncs** for signed-in users. Privacy copy and Data Safety must say that. Never restore “stored on this device only.”
5. Partner is **off**. Leave it off.
6. There is **no** genesyx.co.uk science / Shettles page. The app correctly hides those buttons. Do not invent URLs.

---

## Your list, in order

Do these in this order. Do not skip to upload.

### 1. Look at Play Console (15 minutes)

- [ ] Open Play Console → `com.genesyx.app`.
- [ ] Write down what Internal testing and Production actually serve today (versionName + versionCode).
- [ ] Confirm the next unused versionCode. If 14 is already used, tell engineering to bump before a new AAB.
- [ ] Confirm the Health apps declaration and Data Safety form are still **drafts**, not submitted.

### 2. Apply and prove the backend (blocks the AAB)

SQL to apply: `docs/migrations/2026-07-29_user_supplements.sql`  
Also confirm `daily_logs.sexual_activity` exists (iOS migration `20260810_daily_logs_sexual_activity.sql`).

- [x] Apply `user_supplements` + `genesyx_products` on **production** Supabase. *(done by 13 Aug; REST-confirmed 18 Aug — tables exist)*
- [x] REST-check: those tables return **not** `PGRST205`. *(401/RLS, tables present)*
- [ ] Confirm RLS: a user can only read their own `user_supplements`.
- [ ] Confirm `delete_current_user` deletes `ph_readings` **and** `user_supplements` (hard delete, not a tombstone).
- [ ] End-to-end: create a throwaway account, log a **vaginal pH** reading, delete the account in the app, then check SQL — **zero** rows left in `auth.users`, `profiles`, `daily_logs`, `cycle_settings`, `ph_readings`, `user_supplements`.

Until this is green, **do not upload**.

### 3. Legal / store copy (blocks review)

Drafts live locally in `docs/DATA_SAFETY_AND_PRIVACY_v1.1.md` (git-excluded). Fill every `[OWNER]` placeholder.

- [ ] Legal entity name.
- [ ] Supabase hosting region.
- [ ] Retention period.
- [ ] ICO / DPO / contact (`info@genesyx.co.uk` is the only public address).
- [ ] Publication date.

Then publish and submit:

- [ ] Update `genesyx.co.uk/pages/privacy-policy` so it names **vaginal pH sync** and **waitlist email**.
- [ ] Confirm `genesyx.co.uk/pages/delete-account` still works (Play URL checker has 429’d this before — retry).
- [ ] Submit Play **Data Safety**: health data, pH sync, waitlist email, account info.
- [ ] Submit Play **Health apps** declaration.
- [ ] Optional: a real `/pages/science` (and only then we can show the in-app Science button). Not required to ship.

### 4. Commit and build the artifact you will actually upload

This tree still has **uncommitted** change-list / Learn-parity work. Do not upload an AAB built from a dirty or half-committed tree.

- [ ] Review `git status`. Commit what you want in 1.4.0 (or ask engineering to commit).
- [ ] Confirm `local.properties` has real `genesyx.supabaseUrl`, `genesyx.supabaseAnonKey`, `genesyx.googleWebClientId`. Never commit those.
- [ ] Build signed release:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
cd ~/genesyx-android
./gradlew :app:bundleRelease :app:assembleRelease
```

- [ ] Check the AAB identity is `com.genesyx.app` / **14** / **1.4.0** (or the bumped code).
- [ ] Archive under `~/Documents/Genesyx Releases/1.4.0-code14/` with `SHA256SUMS.txt`.
- [ ] Do **not** reuse an older folder’s AAB.

### 5. Phone QA (you, on a real phone)

Install the **same** APK you will upload (`adb install -r app/build/outputs/apk/release/app-release.apk`), or the Play Internal build after upload. Emulator green is not enough.

Walk this once. Tick only what you saw.

**Account**
- [ ] Create account (email + password).
- [ ] Sign out, sign in.
- [ ] Forgot password (needs network + real inbox).
- [ ] Change password from Profile (wrong current password is rejected).
- [ ] Edit name / Personal Details. Kill the app. Name is still there.
- [ ] Health Profile (cycle) save. Reopen — values stick.
- [ ] Tracking Preferences save. Reopen — answers stick.

**pH**
- [ ] Bottom tab is **pH**, not inside Nutrition.
- [ ] Copy says **vaginal pH**, never “urine pH” (legacy rows may say `urine (legacy)`).
- [ ] Log 4.2 → Healthy. Log 4.6 → Elevated.
- [ ] Disclaimer is behind “About this tracker”, not a wall of text.
- [ ] Guest pH stays on device. Sign in — reading is still there (adoption).

**Track / log**
- [ ] Open a past calendar day. Log symptoms + a food-group chip. Leave. Come back — same date, same data.
- [ ] Intimacy switch is quiet and does **not** grow the streak.
- [ ] Fertile days are highlighted. (Notification only if you granted permission.)

**Offline**
- [ ] Airplane mode → save today’s log → you are **not** blocked.
- [ ] Turn network back on → log is still there after a refresh.
- [ ] Repeat on **mobile data only** (Wi-Fi off).

**Nutrition / Learn**
- [ ] Water: add/remove a glass, switch cups/ml, edit goal.
- [ ] Food chips persist after killing the app.
- [ ] A recipe opens with a photo.
- [ ] Learn: “7-day nutrition starter guide” and “How to use Genesyx” open.
- [ ] Profile → Medical Sources & Disclaimer opens.

**Delete**
- [ ] Throwaway account with a pH reading → Delete account → land on splash.
- [ ] Same email cannot sign in. Re-signup is a **new** empty account.

### 6. Upload, then test the Play install

- [ ] Upload the new AAB to **Internal testing** only.
- [ ] Wait until Play serves it to testers.
- [ ] Install **from Play**, not from `adb`.
- [ ] Repeat the phone QA on that Play build (at least: sign-in, pH, offline save, delete).
- [ ] Only then promote to Production.

---

## What you do **not** need to rebuild

These are already in the Android source and covered by tests (`467` unit, `33` instrumented on emulator):

- Vaginal pH tab, bands, copy, expandable disclaimer
- Offline daily-log queue
- Girl / Boy / No preference / Prefer not to say (optional)
- Light mode default
- Recipes, food chips, hydration cups/ml
- Streaks (intimacy excluded)
- 12-week Learn drip (first Sunday 23 Aug 2026)
- Partner hidden
- Science / Shettles website buttons hidden until a real page exists

Do not ask for Partner, Apple Health, Watch, Oura, widgets, or barcode logging for this release.

---

## One-page scoreboard

| Gate | Who | Blocks upload? |
|---|---|---|
| Product change list in code | Engineering — done | No |
| `user_supplements` / `genesyx_products` on production | You | **Yes** |
| `delete_current_user` includes pH + supplements, proven with a pH row | You | **Yes** |
| Privacy page + Data Safety + Health apps submitted | You | **Yes** (blocks review / Production) |
| Clean commit + signed code-14 (or next unused code) AAB | You + engineering | **Yes** |
| Phone QA on that artifact | You | **Yes** before Production |
| Internal testing Play install | You | **Yes** before Production |
| Partner / Health / widgets / barcode | Nobody this release | No — out of scope |

---

## After you finish a box

Write the date and what you saw (pass / fail + one line). If a box fails, stop. Do not upload past a red backend or a red deletion proof.

The detailed code map is `docs/CHANGE_LIST_PROGRESS_ANDROID.md`.
