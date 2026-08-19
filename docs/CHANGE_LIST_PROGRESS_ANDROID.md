# Genesyx change-list progress — Android

**App:** Genesyx Android source `1.4.0` (versionCode **14**), Room **v9**  
**Checked:** 18 Aug 2026 against `~/genesyx-android` (this tree, including uncommitted 0–13 parity work)  
**This is not a Play upload and not an App Store submission.**

This pass was asked to implement the consolidated client change list on **Android**, not iOS.  
Work was verify-first: existing surfaces were not rebuilt.

## Verdict

**Not ready for Play Production.**

The in-scope product change list (sections 1–3) is **implemented in source** and pinned by unit tests. That is not the same as “ready to ship”:

1. This tree has **not** been uploaded. Last recorded Play audit (28 Jul 2026) had Internal **1.3.0 (11)** and Production **1.2.0 (9)**. Codes 12–14 were not verified live today.
2. **Owner-only gates** still sit in front of any release: Data Safety form, privacy copy (vaginal pH + waitlist email), live pH-account deletion proof, Health apps declaration.
3. **Cellular / airplane-mode / Profile-edit** checks need a physical phone. They are not proven by this session.
4. There is **no genesyx.co.uk science or Shettles page**. In-app buttons that would open the homepage are **hidden** (`AppLinks.isConfiguredWebPage`). The in-app Shettles article (drip-gated **8 Nov 2026**) is the canonical theory copy.

Do not treat unit-green as “a customer tapped every screen on a phone.”

---

## Test evidence (this pass)

```
./gradlew :app:testDebugUnitTest
BUILD SUCCESSFUL
467 tests, 0 failures, 0 errors
```

Includes `ChangeListContractTest` (this pass), `QaParityTest` A–Z, `QuizContentTest`, `DataContractTest`, `TrackingVectorTest`, `PhCopyBannedPhraseTest`.

Last full on-device run on `emulator-5554` (same tree, 18 Aug):

```
./gradlew :app:connectedDebugAndroidTest
33 tests, 0 failures
```

That run included `AzJourneyTest` (splash → auth gate; signed-in Home / Track / pH / Nutrition / Insights / Learn hub / Medical sources), migrations, DailyLog queue (`a_pull_must_not_overwrite_an_unsynced_local_edit`), Learn heroes, MealLog, recipes.

**Not run this pass:** signed release AAB, Play Console, a real SIM / mobile-data phone.

---

## Score against sections 1–3

| Group | Status | Notes |
|---|---|---|
| 1A Vaginal pH | **Done in code** except website | Tab, copy, history, Learn, expandable disclaimer. No real science URL. |
| 1B Track / calendar / Profile | **Done in code** | Device tap of Profile rows still unproven this session. |
| 1C Onboarding question | **Done** | Four options; question `optional = true`; Continue works with no tap. |
| 1D Connectivity | **Done in code** | No Wi-Fi-only flag; offline queue live. Cellular unproven on a phone. |
| 2A Design | **Done** with one polish leftover | Light default; splash eggs; `page_background` on light tabs. No extra egg sprites on Track. |
| 2B Nutrition | **Done** | pH gone; recipes; food chips; why-expanders for supplements **and** hydration. |
| 2C Hydration | **Done** | Cups/ml, custom glass, progress, minus to correct. Storage stays ml. |
| 2D Cycle guidance | **Done** | Phase card + article link + named greeting. |
| 3A Streak | **Done** | Meaningful logs + pH days + article-read dates. Intimacy excluded. |
| 3B Education | **Done** | 12-week Sunday drip 23 Aug–8 Nov; Home card; opt-in local reminders. |
| 4 Scope separately | **Out of scope** | Partner off. No Health Connect, widgets, barcode. |

---

## Implemented (code + tests)

### 1A — Vaginal pH
- Customer copy is **vaginal pH**. The only “urine” strings are the **legacy marker** for pre-migration rows (`urine (legacy)`), which is required honesty, not a tracker name.
- pH is **not** on Nutrition (`NutritionScreen` comment + no pH UI).
- Dedicated bottom tab: Home · Track · **pH** · Nutrition · Insights · Learn · Profile (`Screen.bottomTabs`, `GenesyxBottomNav`).
- Add / edit / history / Healthy 3.8–4.5 / Elevated >4.5 on `PhDetailScreen`.
- Fertility context: `PhCopy.FERTILITY_*`. Support + seek-help copy on the same screen.
- Disclaimer lives in `ExpandableInfo("About this tracker")`, not a full-screen wall.
- Shettles is framed as **not a proven method**. The in-app article is drip-gated to 8 Nov. Website Science / Shettles buttons **do not show** while URLs equal the site root.

Pinned by `ChangeListContractTest` + `PhCopyBannedPhraseTest` + `QaParityTest`.

### 1B — Track, calendar, Profile
- Track day → `Log` (symptoms, water, food groups, notes, supplements, sleep).
- Private intimacy switch on Log. **Not** in streaks (`DailyLog.isMeaningful` ignores `sexualActivity`).
- Logs keyed by date; calendar re-opens that day.
- Calendar: phase fill (period / fertile / ovulation) + dots for pH, symptoms/notes, intimacy (`DayMarkers`).
- Fertile highlight + `ReminderKind.FERTILE_WINDOW` (opt-in after notification grant).
- Profile rows: Personal Details, Health Profile, Tracking Preferences, Change password.

Pinned by `DayMarkersTest`, `DataContractTest`, `DailyLogRepositoryTest` (on-device).

### 1C — Preference question
- Options: **Girl / Boy / No preference / Prefer not to say**.
- `optional = true`: Continue works with **no selection**. Unanswered ≠ “Prefer not to say”.
- Helper: “Genesyx does not predict or influence a baby's sex.” No guarantee language.

Pinned by `QuizContentTest` + `ChangeListContractTest`.

### 1D — Connectivity
- No Wi-Fi-only permission or transport check.
- Offline daily-log writes queue (`PENDING_UPSERT` + `DailyLogSyncWorker`).
- Pull must not overwrite an unsynced local edit (instrumented).

### 2A–2D, 3A–3B
- Default theme **LIGHT**; Profile Light / Dark / System.
- Splash floating eggs; light-mode `page_background` via `GenesyxPage`.
- Nutrition: hydration steppers, food-group chips, meal notes, 8 photo recipes, supplement plan + why-expander, **Why hydration?** expander (`HydrationCoach.WHY_*`).
- Hydration cups or ml; custom glass; minus to correct; progress bar.
- Home phase card + “Learn about this phase” + display-name headline.
- Streak engine + milestones + “log yesterday to reconnect”.
- 32 Learn articles / 20 live on 18 Aug / 12 drip-gated. Home new-article card. Local reminders only if opted in.

---

## Skipped / out of scope (section 4)

| Item | Why |
|---|---|
| Partner / Add Partner | `FeatureFlags.PARTNER_INVITES = false`. UI hidden. Do not flip the flag. |
| Partner sharing controls | No cross-account linking. |
| Apple Health / Watch / Oura / Health Connect | Not built. |
| Home-screen widget | Not built. |
| Barcode / meal-photo logging | Future. Chips are the v1 food surface. |
| Invented science / Shettles website pages | Forbidden. There is no confirmed `/pages/science`. |

---

## Partial / blocked

| Item | Status | Why |
|---|---|---|
| Science + Shettles **website** links | **Partial by design** | URLs are `https://genesyx.co.uk`. Buttons stay hidden until a real page exists. |
| Extra egg sprites on Track | **Polish leftover** | Splash eggs + page art are enough for this list. Not a launch blocker. |
| Mobile data only | **Blocked on a phone** | No code path is Wi-Fi-only. Still needs a SIM. |
| Airplane-mode → reconnect | **Code proven, phone unproven this session** | Queue tests exist; last on-device proof was earlier (13 Jul notes). Re-run before upload. |
| Profile edit taps | **Code exists, device tap unproven this session** | Rows and dialogs are in `ProfileScreen`. |
| Play Console / privacy / deletion-with-pH | **Owner-blocked** | No code change closes these. |

---

## What was changed in this pass

- **Verified** the change list against current source instead of rebuilding it.
- Added `ChangeListContractTest` so “Done” rows fail the build if someone reverts them.
- Did **not** invent website pages, enable Partner, add Health Connect, widgets, or barcode logging.
- Did **not** touch iOS or `graphify-out`.

Earlier uncommitted work still in the tree (not re-done here): Shettles slug alias, hidden homepage science buttons, Free Guide, Medical Sources, `GenesyxPage`, Q&A/DATA tests, `AzJourneyTest`, `BootReceiver` Hilt guard.

---

## App Store / Play readiness

**Not ready.**

This is an Android Play app. Calling it “App Store ready” would be false.

To become upload-ready for Internal testing, still needed:

1. Owner fills Data Safety + privacy copy for **vaginal pH sync** and waitlist email.
2. Live deletion of an account that contains a pH reading.
3. Phone QA: airplane-mode queue, mobile data, Profile four edit rows, Forgot password, food chips after process death, fertile reminder with permission.
4. Commit, bump if Play already has code 14, signed AAB, upload. **Do not upload an older archived binary.**

Until those four are done, the honest status is: **change list implemented in source; release is blocked on owner + device proof + upload.**
