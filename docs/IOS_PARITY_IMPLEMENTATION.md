# iOS → Android parity implementation

**Purpose.** Make Android feel and work like the current iOS app (`genesxy_apple.V1.02`, `main`, 1.2.0 build 19, HEAD `b6907c5`, audited 17 Aug 2026). This file is the build list. Do not implement from the stale 13 Aug remote, from `docs/FEATURES.md` on iOS (wrong pH range), or from `docs/UIUX_SPEC.md` here (that describes the old web/Capacitor app).

**iOS source of truth (read these, in this order):**

| File | Why |
|---|---|
| `App/Genesyx/UI/MainTabView.swift` | 7-tab shell, order, state kept alive |
| `App/Genesyx/UI/Learn/LearnModels.swift` + `LearnContent.swift` + `LearnViews.swift` | Library, drip dates, How-to hub, 12-week plan, help slugs |
| `Sources/GenesyxCore/Content/RecipeContent.swift` + `NutritionContent.swift` | Recipes, food groups, copy |
| `Sources/GenesyxCore/Ph/PhCopy.swift` + `PhStatus.swift` | pH range, education, citations |
| `App/Genesyx/UI/Home/HomeView.swift` and every `HowThisWorksLink` | In-context help |
| `docs/HOW_THE_APP_WORKS.md` | What a customer is supposed to understand |

**Android already has** (do not rebuild): 7 tabs in the same order, auth hard-gate, email + Google, **Forgot password on Auth**, account delete, cycle engine, daily log + offline queue, vaginal pH 3.8–7.0, Learn drip with the **same 32 slugs and Sunday dates**, recipes **content + photos** (ported 17 Aug), quiz answers persisted, notifications (Android is ahead), Sign-out wipe.

**Photos.** Recipe plates are in `app/src/main/res/drawable-nodpi/recipe_*.jpg`. Extra / unmatched iOS rasters sit in `ios-asset-transfer/` — see that README before adding more images.

---

## UI/UX rules (this is how the iOS app succeeds)

These are product rules, not polish.

1. **Help at the point of use.** Every main tab except Profile ends with a quiet text link into a Learn article she can open in one tap. The articles already exist. A Learn library nobody can find from the screen they are stuck on is a failed Learn library.
2. **Never strand her.** A Learn deep link must land *inside* the article, not on the Learn list. iOS does `pendingLearnSlug` then switch to tab 5. Getting only the tab switch leaves her thinking the app is broken.
3. **Never open a date-gated article.** `LearnDrip.published(today)` is the only resolver. A slug with a future `publishedAt` compiles, navigates, and shows “unavailable”. That looks fine in a screenshot and fails in review. The 12-week index may *name* future weeks; it must not open them.
4. **Predictions stay predictions.** Home must say **“Predicted ovulation”**, not “Ovulation”. Fertile window / ovulation copy uses “predicted” or “estimated”.
5. **A food log is a record, not a scoreboard.** Six chips. Nothing is scored. A blank day costs her nothing. Do not mix this with the existing free-text meal log’s nutrient tags.
6. **Recipes cook a reviewed focus food.** They do not make a new health claim. `RecipeContentTest` already enforces the foreign key. Do not add “boosts iron” or “supports fertility” to a method step.
7. **Empty states stay honest.** No fake charts, no mock streaks, no “coming soon” for something that already ships on iOS (recipes no longer qualify).
8. **Tone.** British English, calm, short. One primary action per card. 48 dp touch targets. Keep the warm/premium palette already approved — do not invent a new visual language.

---

## Already matched — leave alone

| Area | Evidence |
|---|---|
| 7 tabs, same order | `Screen.kt` / iOS `MainTabView.swift` |
| Auth gate (no Home without account) | `AppViewModel` / iOS `RootRouting.swift` |
| Email + Google | `AuthScreen.kt` |
| Forgot password (signed out) | `AuthScreen.kt` “Forgot password?” → `sendPasswordReset` |
| pH range 3.8–7.0, classify `>4.5` | `PhStatus` both platforms |
| Learn slugs + Sunday drip dates | `LearnContent.kt` matches iOS `LearnContent.swift` |
| pH guide slug | both `guide-vaginal-ph-tracker` |
| 8 recipes + photos | `RecipeContent.kt` + `drawable-nodpi/recipe_*.jpg` |
| Quiz answers saved + synced | `QuizAnswersRepository` |
| Partner / Pregnancy | both stubs / flag-off |
| Notifications | Android has *more* kinds — do not cut them to match iOS |

---

## Remaining work

Do these in the order below. Each item names the iOS file to copy from, the Android file to change, the customer-facing result, and when it is done.

### P0 — customer can use the app the way iOS teaches it

#### 1. Five in-context help links
**Why it matters.** This is the systematic iOS advantage. Without it Android looks like a tracker with a hidden manual.

| Tab | Label (copy exactly) | Slug |
|---|---|---|
| Home | New here? What your first week looks like → | `getting-started-first-week` |
| Track | How the log works, and what each entry is for → | `guide-how-the-log-works` |
| pH | Read: Understanding your vaginal pH → | `guide-understanding-vaginal-ph` |
| Nutrition | How your focus foods are chosen → | `guide-nutrition-focus` |
| Insights | Reading your trends without over-reading them → | `reading-your-trends` |

- **iOS:** `HowThisWorksLink` in `LearnViews.swift`; call sites in `HomeView`, `TrackView`, `PhTrackerSection`, `NutritionView`, `InsightsView`.
- **Android:** add one small composable (same as iOS: set pending slug, then navigate to Learn / article). Put it at the **bottom** of each tab’s scroll, after the last card, in the same muted-primary style.
- **Daily log** should also link `guide-how-the-log-works` (iOS reaches it via Track; Android’s log is its own full screen).
- **Done when:** tapping each link opens the article, not the Learn list. A unit/UI test per slug. Falsify once with a future-dated slug and confirm it does **not** open.

#### 2. Learn hub cards (permanent)
**Why it matters.** iOS Learn answers “how do I use this?” and “where is the 12-week plan?” without hunting.

- **iOS:** `HowToUseCard` + `TwelveWeekPlanCard` + `HowToUseView` + `TwelveWeekPlanView` in `LearnViews.swift`. Guides listed in `AppGuide` (`LearnModels.swift`).
- **Android:** `LearnScreen.kt` currently has one **dismissible** intro chip and then category chips. Keep the intro if you want, but add two **permanent** cards above the chips:
  1. **How to use Genesyx** — “Every feature, and what it is for”
  2. **Start your 12-week plan here** — “One new article each week. Read them as they arrive.”
- The How-to screen groups the existing guides by tab. Only slugs with **no** `publishedAt` appear.
- The 12-week screen lists all 12 weekly articles. Live ones open. Future ones show “Arrives 23 Aug” (etc.) and do not navigate.
- **Done when:** both cards survive dismissing the intro; every How-to row opens a live article; week 3 (`nutrition-before-conception`) is visible as a row on 17 Aug and does **not** open.

#### 3. Nutrition “Learn more” is nutrition
- **iOS:** `learnArticles.filter { $0.category == .nutrition }` (5 slugs; two date-gated).
- **Android:** `NutritionScreen.kt` `ArticlesSection` uses `LearnDrip.published(today).take(3)` — first-week / logging articles.
- **Change:** filter `category == NUTRITION` (or the Android equivalent), then take what is published. “See all” still goes to Learn.
- **Done when:** the three-or-so rows on Nutrition are hydration / eating-with-your-cycle / supplements, not “Your first week”.

#### 4. Home label: Predicted ovulation
- **iOS:** `HomeView.swift` `metric("Predicted ovulation", …)`
- **Android:** `HomeScreen.kt:439` `HeroMetric("Ovulation", …)`
- Rename the label. Do **not** port iOS Insights copy “the most likely time to conceive” — that is the riskier sentence. Keep Android’s calmer Insights ovulation card.

---

### P1 — Nutrition and logging feel like iOS

#### 5. “What you ate today” food-group chips
**Why it matters.** This is the largest remaining *visible* content gap. iOS screenshots lead with it. Android still shows a free-text “Today’s meals” card (`MealLogSection.kt`) that is a different product.

- **iOS:** `FoodGroup` + `FoodLogCopy` in `NutritionContent.swift`; chips in `NutritionView.swift`; `DailyLog.foodGroups`; `toggleFoodGroup` / `logFoodGroups` in `DailyLogRepository.swift`.
- **Android data already exists:** `DailyLog.foodGroups`, Room v9, `daily_logs.food_groups` DTO. **No chip UI. No repository toggle.** `upsertPreservingWater` already preserves the set.
- **Build:**
  - Port the six groups (same raw values, so sync stays compatible): `vegetables`, `fruit`, `starchyCarbs`, `protein`, `dairy`, `oilsAndFats`.
  - Labels: Vegetables · Fruit · Starchy carbs · Protein · Dairy & alternatives · Oils & fats.
  - Title: **What you ate today**. Counter `0/6`. Footnote: **“A record, not a target. Nothing here is scored, and a blank day costs you nothing.”**
  - “What counts as what?” expander lists the iOS examples.
  - Toggle writes through `DailyLogRepository` and syncs. Recipe “log these groups” is additive (union), never a toggle-off.
- **Do not delete** the free-text meal log in the same PR unless Q1 below is answered. Default: keep meals *below* the chips, or behind a quieter “Add a note about a meal” until the client chooses.
- **Done when:** six chips persist, sync, and survive process death; a recipe tap unions groups; Insights can count “days with meals” from the same set.

#### 6. Recipe cards — finish the iOS shape
Content and photos are in. Remaining UX:

- iOS is a **horizontal** photo row for the current phase, tap opens a sheet (photo, uses-line, time · serves, ingredients, method, additive “Log vegetables, protein…”).
- Android is a **vertical** expand-in-place list (`RecipesSection.kt`). Acceptable for v1 of the port if the photo, uses-line, and time · serves stay visible when collapsed.
- Add the additive **Log [groups]** action once item 5 exists.
- **Done when:** a woman on her period sees the dal and the soup with plates, not a wall of text; logging groups does not un-tick anything she already recorded.

#### 7. Hydration “Why hydration?” on the card
- **iOS:** `HydrationCoach.whyText` + Journal of Nutrition / EFSA citations, expander on the Nutrition hydration card.
- **Android:** the card has goal, steppers, coaching, days-on-goal. The explainer lives only as a Learn article.
- Port `whyText` onto the card as a collapsed expander. Cite the same sources already in `LearnSourceMap` / Android citations. Do not invent new claims.
- Vocabulary: iOS often says “glasses”; Android says “cups”/ml. Storage stays ml. See Q3.

---

### P1 — pH education

#### 8. Testing-accuracy + supporting-health + chart labels
Android pH already has more *sections* than iOS (including “How this relates to fertility”, which iOS does not). It is missing three things iOS screenshots show:

| Piece | iOS source | Android |
|---|---|---|
| Accuracy caveat | `PhCopy.accuracyCaveat` | **Missing** |
| “Supporting your vaginal health” | `PhCopy.spineSupportTitle/Body/Signpost` | Partial one-liner in `DO_HEALTHY` |
| Chart axis marks | 3.8 / 4.5 / 5.7 (or domain min / 4.5 / domain max) | Unlabelled fixed axis |
| Learn link | “Read: Understanding your vaginal pH →” | Missing (covered by item 1) |

- **Copy the iOS accuracy and support strings verbatim.** They already passed the banned-phrase guard.
- **Do not replace** Android’s fertility section with iOS’s silence unless Q5 says so. Keep it behind the existing medical-reviewer flag if still open.
- Sources: iOS cites *Bacterial vaginosis (NHS)* + *Vaginitis (StatPearls)*. Android deliberately uses non-condition-named sources. **Do not change citations without Q4.**

#### 9. Supplement plan live state
- **iOS:** “None logged yet today” / “N of M taken today” on Nutrition.
- **Android:** plan card has a “Why is this important?” expander (keep it — Android is ahead) but no adherence line.
- Wire today’s log supplements into that one sentence.

---

### P2 — Insights polish

#### 10. Days with meals
- **iOS:** `InsightsView.swift` tile `"Days with meals"`, `foodGroupDays / 7`.
- **Android:** Insights never reads `foodGroups`. Add the tile once item 5 writes data.

#### 11. Hydration metric shape
- **iOS Insights:** 7-day total + days on goal.
- **Android Insights:** average ml/day + delta.
- Show days-on-goal as a first-class number (Home already has “N of 7 days on goal”). Keep the delta; it is useful. Do not drop Android’s extra cards (Intimacy, Your supplements).

---

### P2 — small product diffs (do not treat as bugs)

| Topic | iOS | Android | Action |
|---|---|---|---|
| Sign in with Apple | Required on iOS | Not required on Play | Optional later (Q6) |
| Waitlist / free guide step | Not in the iOS onboarding path | Extra Android step | Confirm (Q7) |
| Profile password-reset email | Yes (signed in) | Change-password only | Nice-to-have; Auth already recovers lock-out |
| Guest mode | None in Release | Dead / unreachable | Delete or leave; do not enable |
| Widgets / Health Connect / camera | None | None | Out of scope |
| Localization | English hardcoded | English hardcoded | Out of scope |

---

## Suggested build order

```
1. Help links + Learn hub cards + Nutrition Learn-more filter + “Predicted ovulation”
   → she can be taught the app
2. Food-group chips + repository toggle + Insights tile
   → the last big content surface
3. Recipe “log groups” + hydration why-expander + pH accuracy/support/axis + supplement line
   → education on the screens that already exist
4. Q&A answers that change scope (meals vs chips, citations, Apple, waitlist)
```

Do not start Partner, Pregnancy, Health Connect, or a redesign of the tab bar.

---

## Done-when (release bar)

A reviewer with both phones open should see:

- Same seven tabs, same order.
- Same help sentence at the bottom of Home, Track, pH, Nutrition, Insights.
- Learn opens with How-to + 12-week cards that are still there after a restart.
- Nutrition shows photo recipes and six food chips, not “coming soon” and not a calorie log.
- Home says Predicted ovulation.
- A locked-out user can still tap Forgot password (already true).
- No new medical claim that is not already on iOS or already flagged for review on Android.

---

## Q & A — decide these so implementation does not stall

Answer in this file (or in the PR) before the matching item ships. Recommended answers are marked.

### Product

**Q1. Food-group chips vs the existing “Today’s meals” free-text log?**
- A) Replace meals with chips (closest to iOS).
- B) Ship chips as the primary card; keep meals as an optional note underneath. **(Recommended.)**
- C) Keep meals only; skip chips.

**Q2. Should a recipe’s “Log vegetables, protein…” button appear before chips exist?**
- A) No — land it in the same PR as chips. **(Recommended.)**
- B) Yes — write `foodGroups` invisibly.

**Q3. Hydration unit word: “glasses” (iOS screenshots) vs “cups” (Android)?**
- A) Keep Android cups/ml; do not rename. **(Recommended — storage is already ml.)**
- B) Offer glasses as a display alias of the existing glass size.
- C) Rename everything to glasses.

**Q4. pH citation titles: iOS names *Bacterial vaginosis*; Android avoided condition names on purpose.**
- A) Keep Android’s current sources. **(Recommended until a clinician says otherwise.)**
- B) Switch to the iOS NHS BV + StatPearls pair.
- C) Ask the medical reviewer and do not ship the support section until they pick.

**Q5. Android’s “How this relates to fertility” pH section (not on iOS).**
- A) Keep it. **(Recommended — Android is ahead, and the copy is already cautious.)**
- B) Hide it to match iOS.
- C) Hold until the medical reviewer clears the open flag in `PhCopy.kt`.

**Q6. Sign in with Apple on Android?**
- A) Not for this parity pass. **(Recommended.)**
- B) Add it because some women have Apple-only accounts.

**Q7. Android waitlist / “Unlock my free guide” extra onboarding step?**
- A) Keep it; iOS does not have it in the path, but the guide content exists on both. **(Recommended if the email capture still works.)**
- B) Remove it to match iOS Splash → Intro → Quiz → Readiness → Auth.
- C) Keep the guide, drop the waitlist email.

**Q8. Dismissible Learn intro chip once the two permanent cards exist?**
- A) Remove it; the How-to card replaces it. **(Recommended.)**
- B) Keep both.
- C) Keep the chip, skip the How-to card.

### UX / success

**Q9. Recipe layout: horizontal photo row + sheet (iOS) vs vertical expand (current Android)?**
- A) Keep vertical expand now; add the log-groups button. Revisit row+sheet if the tab feels long. **(Recommended for speed.)**
- B) Rebuild as a horizontal pager before release.

**Q10. Should VoiceOver / TalkBack hide inactive tabs?**
- iOS still relies on `opacity(0)` rather than a working `.accessibilityHidden` through `NavigationStack`. Android’s Compose pager/scaffold usually does this correctly.
- A) Verify TalkBack only reads the visible tab; fix if it reads all seven. **(Required before Play launch if broken.)**
- B) Ignore until after parity content ships.

**Q11. Profile “How to use Genesyx” row?**
- iOS has it; XCUITest cannot tap Profile `rowItem`s (manual check still needed).
- A) Add the same row, plus the Learn card, so there are two routes. **(Recommended.)**
- B) Learn card only.

### Engineering / data

**Q12. Food groups and the streak.**
- Both platforms now count `foodGroups` toward a meaningful day (H4). Do not put chips behind a “does not count” footnote that contradicts `TrackingEngine`.
- Confirm: **chips count.** The “not a target” line is about scoring the *day’s diet*, not about the logging streak.

**Q13. Inbound pH of 3.6 from an old iOS build.**
- Current iOS min is 3.8 (same as Android). A stale 3.5-era row could still arrive.
- A) Clamp on display; keep the stored value; never reclassify urine. **(Recommended.)**
- B) Reject inbound rows below 3.8.

**Q14. Guest / `LOCAL_USER_ID` on Android?**
- A) Leave dead. **(Recommended.)**
- B) Delete the unused paths in a cleanup PR.
- C) Enable guest (would *diverge* from iOS).

**Q15. Who signs off copy that already exists on iOS?**
- In-context help labels, food-group footnote, hydration `whyText`, pH accuracy/support: **already signed off on iOS.** Port verbatim. New sentences need the same banned-phrase tests as Learn/pH.

---

## Answers (fill in)

| # | Decision | Date | Who |
|---|---|---|---|
| Q1 | B — chips primary; meals stay as an optional note | 17 Aug 2026 | owner |
| Q2 | A — log-groups button lands with chips | 17 Aug 2026 | owner |
| Q3 | A — keep cups/ml | 17 Aug 2026 | owner |
| Q4 | A — keep Android pH sources until a clinician says otherwise | 17 Aug 2026 | owner |
| Q5 | A — keep the fertility pH section | 17 Aug 2026 | owner |
| Q6 | A — no Sign in with Apple this pass | 17 Aug 2026 | owner |
| Q7 | A — keep waitlist / free-guide (email capture still present) | 17 Aug 2026 | owner |
| Q8 | A — drop the dismissible Learn intro; How-to card replaces it | 17 Aug 2026 | owner |
| Q9 | A — keep vertical recipe cards | 17 Aug 2026 | owner |
| Q10 | | | |
| Q11 | | | |
| Q12 | chips count toward the streak (already in both engines) | 17 Aug 2026 | code |
| Q13 | | | |
| Q14 | | | |
| Q15 | port iOS-signed strings verbatim | 17 Aug 2026 | code |

---

## File cheat sheet

| Implement | Touch on Android | Copy from iOS |
|---|---|---|
| Help links | New `HowThisWorksLink.kt`; Home, Track, Log, Ph, Nutrition, Insights | `LearnViews.swift` `HowThisWorksLink` |
| How-to + 12-week | `LearnScreen.kt`, new screens | `LearnViews.swift` `HowToUse*` / `TwelveWeekPlan*` |
| Learn-more filter | `NutritionScreen.kt` `ArticlesSection` | `NutritionView.swift` ~509 |
| Predicted ovulation | `HomeScreen.kt:439` | `HomeView.swift:152` |
| Food chips | New `FoodGroup` + card; `DailyLogRepository` toggle | `NutritionContent.swift` `FoodGroup` / `FoodLogCopy` |
| Recipe log-groups | `RecipesSection.kt` | `RecipeCopy.logGroupsAction` |
| Hydration why | `NutritionScreen.kt` hydration card | `HydrationCoach.whyText` |
| pH education | `PhCopy.kt`, tracker card/chart | `PhCopy.swift`, `PhTrackerSection.swift` |
| Days with meals | `InsightsScreen.kt` / VM | `InsightsView.swift` `currentWeekFoodGroupDays` |
| Supplement line | Nutrition plan card | `NutritionView.swift` “None logged yet today” |
