# Implementation prompt — close the last 3 iOS parity gaps

Paste everything below this line into the agent in Android Studio, run from
`/Users/lucasvalenca_sf/genesyx-android`.

---

You are completing the final iOS-parity gaps in the Genesyx Android app
(`/Users/lucasvalenca_sf/genesyx-android`, source 1.4.0 / versionCode 14, Room v9).

Read first, in this order:
1. `docs/IOS_PARITY_GAP_AUDIT_2026-08-19.md` — the audit this prompt comes from.
2. `docs/IOS_PARITY_IMPLEMENTATION.md` — product rules, Q&A decisions, file cheat sheet.
3. `CLAUDE.md` / `AGENTS.md` — repo rules.

Ground rules — non-negotiable:
- **Step 0: commit the dirty tree first.** ~20 files are modified and uncommitted on top of
  `edd8f2d`. Run the unit tests, then commit as-is with a descriptive message BEFORE any new work.
  Each task below is its own commit.
- Do NOT rebuild anything the audit marked ✅ done. No duplicate screens, repositories, or rules.
- No schema changes, no Supabase changes, no new Room migration. All three tasks use data the app
  already has.
- No new health claims. Copy that exists on iOS is pre-approved (Q15) — port verbatim. Any new
  sentence must pass the existing banned-phrase tests (`PhCopyBannedPhraseTest` pattern).
- British English. Do not touch `FeatureFlags`, the tab bar, or `tracking_test_vectors.json`.
- Verify with `./gradlew :app:testDebugUnitTest` (baseline: 467 passing) after every task. Add
  tests for each change; never delete or weaken an existing test to get green.

## Task 1 — Supplement plan live state (Nutrition)

iOS Nutrition shows a live adherence line on the supplement plan card: **"None logged yet today"**
or **"N of M taken today"** (source: `NutritionView.swift` in the iOS repo; if unavailable, use
those exact strings).

- Touch: `ui/nutrition/NutritionScreen.kt` (`SupplementPlanCard`, ~line 431) and
  `ui/nutrition/NutritionViewModel.kt`.
- Data already exists: today's logged supplements are in `daily_logs.supplements` (the Log screen's
  toggles write it); the plan is `domain/content/supplementPlan`. Count today's logged names that
  match plan items → N; M = plan size. Match names the same way
  `UserSupplementInsightLogic` matches (reuse it, don't reinvent).
- Keep the existing "Why is this important?" expander — Android is ahead there.
- Done when: with nothing logged today the card says "None logged yet today"; after toggling one
  plan supplement in the Log it says "1 of M taken today" without restart; unit test covers both
  states and the name-matching edge case (case/whitespace).

## Task 2 — Insights hydration: days on goal (Insights)

iOS Insights shows days-on-goal as a first-class number. Android's `HydrationCard`
(`ui/insights/InsightsScreen.kt`, ~line 432) shows bars + ml/day delta only.

- Touch: `InsightsScreen.kt` `HydrationCard` and the ViewModel/logic that builds
  `HydrationInsights`.
- Home already computes "N of 7 days on goal" — reuse that exact logic/source (goal comes from
  `PreferencesRepository`, the only writer of the goal). Do not create a second on-goal rule.
- Show "N of 7 days on goal" alongside the card title or as the leading stat. KEEP the existing
  delta and bars — the parity doc says the delta is useful; do not drop it.
- Done when: the card shows the same N as Home for the same week; unit test pins that the two
  surfaces agree (same input → same N).

## Task 3 — pH chart numeric axis labels

The pH chart (`ui/components/PhTrackerCard.kt`) draws the healthy/elevated bands and boundary
hairlines but no numbers. iOS labels the axis 3.8 / 4.5 / axis max.

- Touch: `PhTrackerCard.kt` Canvas block (~lines 240–290).
- Draw small muted labels at y-positions of 3.8, 4.5, and the axis max (use the existing
  `yFor()`; values from `PhStatus`, not new literals). Use `drawText` with an rememberered
  `TextMeasurer`, `MaterialTheme` onSurfaceVariant at ~60% alpha, right-aligned inside the left
  inset. Inset the plot horizontally so labels never overlap dots.
- Do not change classification, band values, or the legacy-urine rendering. `PhStatus` stays the
  single source of the thresholds.
- Done when: labels render in light and dark theme without clipping; existing pH tests stay green.

## Task 4 — record the open decisions (docs only)

In `docs/IOS_PARITY_IMPLEMENTATION.md` "Answers" table fill in:
- Q11: A — Profile row exists (`ProfileScreen.kt:252`). Date 19 Aug 2026.
- Q13: A — clamp on display, keep stored value, never reclassify (already the chart's behaviour).
- Q14: A — leave guest paths dead; cleanup post-launch.
Leave Q10 (TalkBack) open — it needs a physical device and is a human launch gate, not yours.
Note it as BLOCKED-ON-DEVICE.

## Final verification

1. `./gradlew :app:testDebugUnitTest` — 467+ passing, 0 failures.
2. `./gradlew :app:assembleRelease` — green, R8 clean.
3. If an emulator is available: `./gradlew :app:connectedDebugAndroidTest`.
4. Append a `CHANGELOG.md` entry (newest first, existing format) and update
   `APP_INVENTORY.md` where these surfaces are described.
5. Report: per task — files changed, tests added, and anything you could NOT verify.

Do not upload anything to Play. Do not bump versionCode/versionName.
