# iOS → Android gap audit — 19 Aug 2026

**iOS reference:** `genesxy_apple.V1.02` (1.2.0 build 19, HEAD `b6907c5`, audited 17 Aug 2026 in
`docs/IOS_PARITY_IMPLEMENTATION.md`).
**Android tree checked:** source 1.4.0 (versionCode 14), Room v9, HEAD `edd8f2d` **plus uncommitted
changes** (~20 modified files incl. LearnScreen, NutritionScreen, InsightsScreen, ProfileScreen).
**Method:** every item in the 17 Aug parity build list re-verified against actual Android source, not
against the docs' claims.

---

## Verdict

Of the 11 parity work items, **9 are implemented in source and verified**. Two are still missing,
one is partial, and three Q&A decisions are unanswered. Nothing here is on Play yet — Internal
testing still serves 1.3.0 (11).

## ✅ Done — verified in source (do not rebuild)

| # | Item | Evidence |
|---|---|---|
| 1 | Five in-context help links | `ui/learn/HowThisWorksLink.kt` + `domain/content/AppGuide.kt` — all five slugs, wired into Home, Track, Log, pH, Nutrition, Insights |
| 2 | Learn hub cards (How-to + 12-week) | `HowToUseScreen.kt`, `TwelveWeekPlanScreen.kt`; `AzJourneyTest` asserts both cards + navigation; `QaParityTest` pins Sunday gating 23 Aug–8 Nov |
| 3 | Nutrition "Learn more" filtered to nutrition | `NutritionScreen.kt:509` — `LearnDrip.published(today).filter { category == NUTRITION }` |
| 4 | "Predicted ovulation" label | `HomeScreen.kt:453` |
| 5 | Food-group chips | `domain/content/FoodGroup.kt` + `ui/nutrition/FoodGroupSection.kt` — six groups, "What you ate today", exact "A record, not a target…" footnote |
| 6 | Recipe "Log \[groups]" additive action | `RecipesSection.kt:191` `RecipeCopy.logGroupsAction`; union write via `viewModel.logFoodGroups`; `RecipeContentTest` pins copy |
| 7 | Hydration "Why hydration?" expander | `HydrationCoach.WHY_TITLE/WHY_TEXT`, rendered on the Nutrition card |
| 8a | pH accuracy caveat + support section | `PhCopy.kt:59–64` `ACCURACY_*` / `SUPPORT_*`, inside the banned-phrase guard list |
| 10 | Insights "Days with meals" | `InsightsScreen.kt:325` |

Also confirmed: Profile "How to use Genesyx" row exists (`ProfileScreen.kt:252` — answers **Q11 = A**),
and off-scale legacy pH values are display-clamped in the chart (`PhTrackerCard.kt` `coerceIn`).

## ❌ Still missing — the actual to-develop list

### 1. Supplement plan live state (parity item 9, P1)
iOS Nutrition shows **"None logged yet today" / "N of M taken today"** on the plan card. Android's
`SupplementPlanCard` (`NutritionScreen.kt:431`) has no adherence line — no "taken today" string
exists anywhere in `main/`. Today's logged supplements are already available to the ViewModel
(the Log toggles write `daily_logs.supplements`), so this is one sentence of wiring.
**Copy from:** `NutritionView.swift`. **Touch:** `NutritionScreen.kt` plan card + `NutritionViewModel`.

### 2. Insights hydration: days-on-goal as a first-class number (parity item 11, P2)
iOS Insights shows 7-day total + **days on goal**. Android's `HydrationCard`
(`InsightsScreen.kt:432`) shows bars + delta only. Home already computes "N of 7 days on goal" —
reuse that logic, keep the delta (it's useful; the parity doc agrees).
**Touch:** `InsightsScreen.kt` / `InsightsViewModel`.

### 3. pH chart numeric axis labels (parity item 8, partial)
The chart has the two bands and boundary hairlines (`PhTrackerCard.kt:253–257`) but **no numeric
marks** — iOS labels 3.8 / 4.5 / max. Without numbers the hairlines are unexplained. Small Canvas
`drawText` (or overlaid `Text`) at the three y-positions.
**Touch:** `PhTrackerCard.kt`.

## ⚠️ Open decisions blocking sign-off (from the parity doc's Q&A table)

| Q | Question | State | My recommendation |
|---|---|---|---|
| Q10 | TalkBack reads only the visible tab? | **Unverified** — flagged "required before Play launch if broken" | Run TalkBack on a device this week; it's a launch gate, not polish |
| Q13 | Inbound pH < 3.8 from stale iOS builds | Display clamp exists in the chart; **classification/list behaviour undecided** | Adopt A (clamp on display, keep stored value, never reclassify) and record it in the doc |
| Q14 | Dead guest code paths | Unanswered | Leave dead (A); cleanup PR post-launch |

## Not gaps — deliberate divergence (already decided, don't "fix")

Sign in with Apple (skipped, Q6=A), waitlist/free-guide onboarding step (kept, Q7=A), cups/ml not
glasses (Q3=A), Android pH citations kept over iOS's condition-named ones (Q4=A), Android's extra pH
fertility section kept (Q5=A), vertical recipe cards kept over iOS's horizontal row (Q9=A),
Android's richer notifications kept. Android is **ahead** of iOS on: article-read streak input,
user-supplement Insights card, private Intimacy card, supplement why-expander.

## Process risks (not iOS gaps, but they'll bite the release)

1. **~20 files are modified and uncommitted** on top of `edd8f2d`, including the parity surfaces
   this audit verified. Commit before anything else — an audit of a dirty tree has a shelf life of
   one `git checkout`.
2. This 1.4.0/code-14 source is **behind several release gates** (Supabase `user_supplements`
   migration, Data Safety form, privacy copy, live pH-account deletion proof) — see
   `CHANGE_LIST_PROGRESS_ANDROID.md`. The three gaps above are small; the gates are the long pole.
3. On-device checks (cellular, airplane-mode, Profile row taps) remain unproven per the 18 Aug pass.

## Suggested order

1. Commit the dirty tree.
2. Supplement adherence line (30 min) + Insights days-on-goal (1 h) + pH axis labels (1 h) — all
   three are P1/P2 polish with existing data, no schema changes, no new copy approval needed
   (iOS strings are pre-signed-off per Q15).
3. TalkBack verification (Q10) — launch gate.
4. Record Q13/Q14 answers in `IOS_PARITY_IMPLEMENTATION.md` so the table stops looking half-done.
