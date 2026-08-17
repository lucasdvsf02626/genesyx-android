# Genesyx change-list progress — Android

**App:** Genesyx Android `1.4.0` (versionCode 14)  
**Checked:** 17 Aug 2026 against `~/genesyx-android`  
**Test evidence:** `./gradlew :app:testDebugUnitTest` — **406 tests, 1 flake, 0 real failures**  
(`PreferencesRepositoryTest.a glass above the ceiling is clamped` failed once with `UncompletedCoroutinesError`, passed on rerun — known DataStore flake, not from this work.)  
**This is not a Play / App Store submission.** This file answers: *did Android get the client change list?*

**Score against sections 1–3 (the in-scope product work):** **about 92–95% done in code.**  
Food-group chips, Nutrition Learn-more filter, in-tab help links, Learn hub cards, Forgot password, and Predicted ovulation all landed this session.  
What is left on the *client list* is **a dedicated genesyx.co.uk science/Shettles page** (in-app theory copy + site-root fallback is wired), **device QA**, and optional extra egg treatment on Track.

Section 4 (partner, Health, widgets, barcode) was explicitly “scope separately” — not counted against “ready”.

---

## How close, by group

| Group | Done | Partial | To do / device | Verdict |
|---|---:|---:|---:|---|
| 1A Vaginal pH | 7 | 1 | 0 | Almost — website link still missing |
| 1B Track / calendar / Profile | 9 | 0 | 0* | Done in code (*Profile edit rows need a 30s device check) |
| 1C Onboarding question | 1 | 1 | 0 | Options correct; not skippable without a tap |
| 1D Connectivity | 3 | 0 | 0* | Queue is live (*cellular still needs a phone) |
| 2A Design | 3 | 1 | 0 | Light default + splash eggs; no subtle eggs on Home/Track |
| 2B Nutrition | 5 | 1 | 0 | Recipes + **6 food-group chips** ship; hydration why-expander still missing |
| 2C Hydration | 3 | 0 | 0 | Done (cups/ml, custom glass, progress) |
| 2D Cycle guidance | 3 | 0 | 0 | Phase card + article link + named greeting |
| 3A Streak | 4 | 0 | 0 | Done, including “log yesterday to reconnect” |
| 3B Education | 3 | 0 | 0 | 12-week drip scheduled; Home card; opt-in push |
| 4 Scope separately | 0 | 0 | 5 | Correctly not built |

---

## Checklist

Statuses: **Done** · **Partial** · **To do** · **In review** (needs a human on a device) · **Out of scope**

### 1A — Vaginal pH (critical)

| Task | Status | Evidence |
|---|---|---|
| Replace all ‘urine pH’ with ‘vaginal pH’ (homepage + guidance) | **Done** | Customer copy is vaginal. `urine (legacy)` is only the marker for pre-migration rows. No `urine pH` string in `app/src/main`. |
| Remove pH tracker from Nutrition | **Done** | Zero pH UI under `ui/nutrition`. Track row + dedicated tab only. |
| Dedicated pH icon + tab in bottom nav | **Done** | `GenesyxBottomNav.kt`: Home · Track · **pH** · Nutrition · Insights · Learn · Profile |
| Add result, view history, explain readings | **Done** | `PhTrackerSection` + `PhLogDialog` + history + Healthy/Elevated copy |
| Short explanation of pH / vaginal health vs fertility | **Done** | `PhCopy.FERTILITY_TITLE/BODY` on `PhDetailScreen` — Android has this; iOS does not |
| Expand Learn: supporting health + when to seek help | **Done** | Guides + weekly `vaginal-ph-explained`; pH detail has support/next-step copy |
| Move disclaimer into info / expandable | **Done** | `ExpandableInfo("About this tracker")` on detail + log dialog |
| Link to website science + Shettles (theory, not proven) | **Partial** | In-app Shettles article exists (`shettles-method-theory-vs-evidence`), date-gated **8 Nov 2026**, clearly a theory. **No link to genesyx.co.uk science pages.** |

### 1B — Tracking, calendar & Profile (critical)

| Task | Status | Evidence |
|---|---|---|
| Log symptoms and nutrition from the tracker | **Done** | Track → day → `Screen.Log`; Nutrition from Track list + Nutrition tab |
| Private sexual-activity logging for TTC | **Done** | `DailyLog.sexualActivity`; Log screen intimacy; excluded from streaks/notifications |
| Entries persist on the correct date | **Done** | Logs keyed by date; calendar re-opens that day’s record |
| Colour markers: period, fertile, ovulation, sex, pH, symptoms/notes | **Done** | Phase fill for period/fertile/ovulation; dots for pH, symptoms/notes, intimacy (`DayMarkers.kt`) |
| Notification / highlight for most fertile stage | **Done** | Fertile ring on calendar + `ReminderKind.FERTILE_WINDOW` (opt-in after grant; default-on for that kind) |
| Audit Profile — every edit works | **In review** | Rows exist: Personal Details, Health Profile, Tracking Preferences, password. Same XCUITest/Compose tap risk as iOS Profile `rowItem`. **Tap all four on a device.** |
| Edit name, password, personal details | **Done** | `ProfileScreen` + `changePassword` / name editor |
| Amend Health Profile and Tracking Preferences | **Done** | Cycle settings + quiz answers dialogs, synced |
| Edit controls obvious; previous entries updatable | **Done** | Calendar day opens that date’s log; pH history rows edit |

### 1C — Onboarding question (critical)

| Task | Status | Evidence |
|---|---|---|
| Girl / Boy / No preference / Prefer not to say | **Done** | `QuizContent.kt` `gender` options |
| Question optional; no sex-can-be-guaranteed claim | **Partial** | Helper is gentle; no guarantee copy. Continue is **disabled until she taps one option**. “Prefer not to say” is the decline. There is **no Skip** that leaves the answer blank. |

### 1D — Connectivity (critical)

| Task | Status | Evidence |
|---|---|---|
| Works over mobile data as well as Wi-Fi | **In review** | No Wi-Fi-only flag. Uses HTTPS to Supabase. **Confirm on a phone with Wi-Fi off.** |
| Investigate false offline symbol | **Done** (code) | Old `isOnline()` save-gate removed (`LogViewModel`). Offline writes queue instead of showing “you’re offline”. |
| Prevent log loss on a dropped connection | **Done** | `PENDING_UPSERT` + `DailyLogSyncWorker`. Pull will not overwrite an unsynced local edit. |

### 2A — Restore intended design

| Task | Status | Evidence |
|---|---|---|
| Light mode default; dark optional | **Done** | `ThemeMode.LIGHT` default; Profile Light/Dark/System |
| Restore egg graphics incl. subtle background eggs | **Partial** | Floating eggs on splash; `home_hero_bg` on Home. `page_background.jpg` is **not used** on main tabs. |
| Warm, premium presentation | **Done** | Approved warm/premium direction already shipped (same decision as iOS 14 Aug) |
| Reduce text; cards, visuals, icons, expandables | **Done** | Nutrition expanders, pH expandable disclaimer, recipe photos |

### 2B — Simplify Nutrition

| Task | Status | Evidence |
|---|---|---|
| Hide greyed-out explanatory text | **Done** | Long grey copy pulled into expanders / Learn |
| Secondary info in ‘Why is this important?’ / ‘Learn more’ | **Partial** | Supplement “Why is this important?” exists. Nutrition Learn-more is now nutrition-only. Hydration **why-expander** still missing. |
| Main screen focused on actions | **Done** | Hydration steppers, chips, meals, recipes, supplements |
| Expand: meals, food-group/nutrient, suggestions, recipes, supplement reminders | **Done** | Meals (optional note) + **6 food-group chips** + nutrient tags + 8 photo recipes + supplement plan + reminders |
| Replace text-only food suggestions with recipe cards | **Done** | `RecipeContent.kt` + `drawable-nodpi/recipe_*.jpg` (17 Aug) |

### 2C — Hydration logging

| Task | Status | Evidence |
|---|---|---|
| Add water by glasses or millilitres | **Done** | `HydrationUnit.ML` / `CUPS` (250 ml cup). Storage stays ml. |
| Custom glass size + correct a wrong entry | **Done** | `hydrationGlassMl` + minus stepper / log edit |
| Progress towards daily target | **Done** | Nutrition + Home progress + “N of 7 days on goal” |

### 2D — Contextual cycle guidance

| Task | Status | Evidence |
|---|---|---|
| Visual card when entering a new cycle phase | **Done** | Home cycle hero + Today’s focus from `phaseHeroCopy` |
| Link phase card to a relevant article | **Done** | “Learn about this phase →” (`phaseArticleSlug`) |
| Personalise homepage greeting with name | **Done** | Time-of-day line + **display name as the headline** (`state.userName`) |

### 3A — Daily logging streak

| Task | Status | Evidence |
|---|---|---|
| Streak on meaningful actions | **Done** | `StreakEngine` / `DailyLog.isMeaningful()` — mood, energy, symptoms, sleep, water, supplements, notes, food groups, pH days, article-read dates. Intimacy excluded on purpose. |
| Show streak + milestone celebrations | **Done** | Home chips (≥2 days) + `MilestoneDialog` |
| Encouraging language (no guilt) | **Done** | Copy + banned-phrase tests on streak/insights |
| Occasional streak restore | **Done** | `RestoreStreakCard`: “Log yesterday to reconnect it.” |

### 3B — Education section

| Task | Status | Evidence |
|---|---|---|
| One new article a week + in-app card | **Done** | `publishedAt` Sundays 23 Aug–8 Nov; Home `NewArticleCard` |
| Push only where opted in | **Done** | Notification master + per-kind switches |
| 12-week plan scheduled (articles 1–12) | **Done** | All 12 compiled. Client topic #7 Shettles is week **12** (8 Nov) so it is framed as theory, not week-7 guidance. |

### 4 — Clarify / scope separately

| Task | Status | Notes |
|---|---|---|
| Partner: confirm current Add Partner behaviour | **Out of scope** | `FeatureFlags.PARTNER_INVITES = false`. No Add Partner in the UI. Same as iOS 1.2.0 public release. |
| Partner sharing controls | **Out of scope** | Do not enable the flag as a shortcut. |
| Apple Health / Watch / Oura | **Out of scope** | No Health Connect / sensors. |
| Home-screen widget | **Out of scope** | Not built. |
| Barcode / meal-photo logging | **Out of scope** | Chips are the intended v1 food surface, not a food database. |

---

## What to do before calling Android “finished” on this list

1. **Hydration “Why hydration?” expander** on the Nutrition water card (iOS has it; Android still only has the Learn article).  
2. **Website links** for science + Shettles (or formally accept the in-app 8 Nov article as enough — there is no genesyx.co.uk science URL in this repo).  
3. **Optional sex-preference:** add Skip, or formally accept “Prefer not to say” as the optional path.  
4. **Subtle egg / page background** on Home/Track if design still feels flat (`page_background.jpg` unused).  
5. **Device QA (cannot be done from the repo):**
   - Airplane mode → log → reconnect → row still there  
   - Mobile data only  
   - Profile: Personal Details, Health Profile, Tracking Preferences, password, Forgot password from Auth  
   - Food chips persist after kill  
   - Help links open the article, not the Learn list  
   - Fertile-window highlight + notification with permission on  
   - TalkBack only reads the visible tab  

Shipped this session vs *current iOS* (see [`IOS_PARITY_IMPLEMENTATION.md`](IOS_PARITY_IMPLEMENTATION.md)): Forgot password on Auth, 8 photo recipes, food-group chips, help links on five tabs + Log, Learn hub cards, Nutrition Learn-more filter, Predicted ovulation.

Still extra vs iOS (not on the client list, but they make the two phones match): pH accuracy/support/axis labels, supplement “N of M taken today”, Insights hydration days-on-goal as a first-class number.

---

## Test run (this session)

```
./gradlew :app:testDebugUnitTest
406 tests completed, 1 failed (PreferencesRepositoryTest glass-clamp — UncompletedCoroutinesError)
rerun PreferencesRepositoryTest → BUILD SUCCESSFUL
```

Instrumented / on-device UI tests were **not** run here (no emulator attached). Do not treat unit green as “I tapped the app.” This is also **not** an App Store / Play upload.
