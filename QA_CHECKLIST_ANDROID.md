# QA checklist — Android, Nutrition parity sprint (SFM-27 / SFM-28)

Run on a real device or the emulator with the **debug** build from `main` (or the release AAB once
uploaded). Tick Pass/Fail per row; note the build (`versionName (versionCode)`) and device at the top.

| Build | Device / OS | Tester | Date |
|---|---|---|---|
| | | | |

**Conventions.** "Chip" = one of the round F / O / D / Z (+ your own) buttons on the Nutrition tab's
"Your supplement plan" card. "Kill the app" = swipe it away from Recents (not just Back), so the
process dies and the next launch reads from storage.

---

## 0. Setup

| # | Step | Expected | Pass / Fail |
|---|---|---|---|
| 0.1 | Fresh install, sign in with a test account (email/password). | Home opens. No crash, no blank screen. | |
| 0.2 | Profile → Health data consent is **on** (grant it if the screen asks). | Nutrition tab shows no red "Health data collection is off" banner. | |

## 1. All seven bottom tabs navigate (SFM-27)

| # | Step | Expected | Pass / Fail |
|---|---|---|---|
| 1.1 | From Home tap **Track**. | Track screen ("Your trackers" / calendar). Track highlighted. | |
| 1.2 | Tap **pH**. | Vaginal pH screen, no Back arrow. pH highlighted. | |
| 1.3 | Tap **Nutrition**. | "Your nutrition focus" page, **no Back arrow**, Nutrition highlighted. | |
| 1.4 | Tap **Insights**. | "Your Insights". | |
| 1.5 | Tap **Learn**. | Learn hub ("Short reads" / "How to use Genesyx"). | |
| 1.6 | Tap **Profile**. | Profile. | |
| 1.7 | Tap **Home**. | Home. | |
| 1.8 | **The regression path:** Nutrition → scroll to bottom → "See all articles" → (Learn opens, Learn highlighted) → tap **Track** → tap **Nutrition**. | Lands on "Your nutrition focus" (not Learn), no Back arrow, Nutrition highlighted. | |
| 1.9 | Repeat 1.8 three times in a row without killing the app. | Same result every time (the old bug stuck for the life of the process). | |
| 1.10 | On Nutrition, scroll halfway down, then tap the **Nutrition** tab again. | Page scrolls smoothly back to the top. Nothing is pushed; Back from here exits to Home/previous tab, not to a second Nutrition. | |
| 1.11 | Learn → open any article → Back. Then tap Nutrition. | Nutrition root, no article on top. | |
| 1.12 | **The dead-button path:** Insights → scroll to "Reading your trends without over-reading them →" → open it → tap **See your insights**. | The Insights dashboard, no Back arrow, Insights highlighted. (It used to sit there doing nothing.) | |
| 1.13 | Repeat 1.12, then tap **Learn**, then **Insights**. | Insights dashboard both times — never the article restored on top. | |
| 1.14 | pH → "Read: Understanding your vaginal pH →" → tap **Open the pH tracker**. | The pH tab. Same shape as 1.12. | |
| 1.15 | Learn → open any article with a tab button (e.g. "See your insights") → tap it → tap **Learn**. | The button lands on that tab; Learn returns to the hub list, not to the article. | |

## 2. Track — every tracker row opens its screen

| # | Step | Expected | Pass / Fail |
|---|---|---|---|
| 2.1 | Track → "Your trackers" → **Cycle**. | Cycle detail opens (Back arrow, bottom bar hidden). Back returns to Track. | |
| 2.2 | **Vaginal pH** row. | pH screen (this one is a tab, so bottom bar stays). | |
| 2.3 | **Nutrition** row. | Nutrition tracker: **TODAY / Supplements from today's log**, then **LOG SUPPLEMENTS** (checkbox rows by name + dose: Folate, Omega-3, Vitamin D, Zinc, then "Your supplements"), **Supplement plan** button, **TODAY / Food groups from today's log**, **SUPPLEMENTS THIS WEEK** (7 dots M–S with counts), **FOOD GROUPS THIS WEEK**. | |
| 2.3a | Tick **Zinc** in LOG SUPPLEMENTS. | Row checks immediately; status "1 of 4 logged today"; the TODAY card above reads "Zinc"; today's dot in SUPPLEMENTS THIS WEEK fills with "1". Nutrition tab's Z chip is filled too. | |
| 2.3b | Un-tick Zinc. | Row clears; TODAY card back to "No entries yet — log today to start"; dot hollow. Kill + relaunch → still clear. | |
| 2.3c | Tap **Supplement plan**. | The same sheet as Nutrition → Review Plan: GENESYX ESSENTIALS with bells, YOUR SUPPLEMENTS with Name / Dose / Time / "+ Add your own supplement". Add "Magnesium" → Got it → it appears under "Your supplements" in the checklist with its own checkbox; ticking it reads "1 of 5". | |
| 2.3d | Back from the tracker. | Returns to Track with Track highlighted. | |
| 2.4 | With nothing logged this week, read the Nutrition tracker. | Both TODAY cards say "No entries yet — log today to start"; LOG SUPPLEMENTS reads "None logged yet today"; all 14 dots hollow with "–"; footer "No supplements logged yet this week — even one, whenever you remember, is a gentle start." | |
| 2.5 | **Symptoms**, **Sleep**, **Hydration** rows. | Each opens its own detail; none is a dead row. | |
| 2.6 | Every row shows icon, title, one-line status, 7-dot strip, chevron. | Present on all six. | |

## 3. Inline supplement logging — Nutrition tab (SFM-28a)

| # | Step | Expected | Pass / Fail |
|---|---|---|---|
| 3.1 | Nutrition tab, top card. | Title "Your supplement plan", copy "Folate, Omega-3, Vitamin D, and Zinc — taken with breakfast.", four chips **F O D Z** with names under them, status "None logged yet today", "Why is this important?" expander, **Review Plan** button. Card is visible even if no cycle is set up. | |
| 3.2 | Tap **F**. | Chip fills solid (white letter) immediately. Status reads "1 of 4 logged today". No dialog, no new screen. | |
| 3.3 | Tap **D**. | Two filled chips, "2 of 4 logged today". | |
| 3.4 | Tap **F** again (un-log). | F hollow again, "1 of 4 logged today". | |
| 3.5 | Tap **D** again → nothing logged. | All hollow, "None logged yet today" (not stuck at "1 of 4"). | |
| 3.6 | Log **F** and **Z**, then **kill the app** and relaunch → Nutrition. | F and Z still filled, "2 of 4 logged today". | |
| 3.7 | Un-log both, kill the app, relaunch. | All hollow, "None logged yet today" — the clear persisted (explicit empty write, not a skipped one). | |
| 3.8 | With F logged, open Track → Nutrition row. | "Supplements from today's log: Folate"; today's dot filled with "1"; footer note absent. | |
| 3.9 | Log F via a chip, then open the Log screen (Track → "Log today") → Supplements. | Folate is ticked there too (same row, same column). Un-tick it there, Save, return to Nutrition. | |
| 3.10 | …after 3.9. | The F chip is hollow — the two writers agree. | |
| 3.11 | TalkBack on: focus a chip. | Announces e.g. "Folate, not logged today, checkbox" / "…logged today". | |

## 4. Food groups — "What you ate today"

| # | Step | Expected | Pass / Fail |
|---|---|---|---|
| 4.1 | Nutrition tab → "WHAT YOU ATE TODAY" card. | "0/6", prompt "Tap a group when you have eaten something from it.", six chips: Vegetables, Fruit, Starchy carbs, Protein, Dairy & alternatives, Oils & fats. | |
| 4.2 | Tap Vegetables and Protein. | Both highlighted, counter "2/6" live. | |
| 4.3 | Tap Vegetables again. | "1/6". | |
| 4.4 | Kill the app, relaunch. | Protein still selected, "1/6". | |
| 4.5 | Expand "What counts as what?". | Examples per group. Disclaimer "A record, not a target. Nothing here is scored, and a blank day costs you nothing." visible. | |
| 4.6 | Track → Nutrition row. | "Food groups from today's log: Protein"; today's food-group dot filled with "1". | |

## 5. Supplement plan sheet + "Add your own supplement"

| # | Step | Expected | Pass / Fail |
|---|---|---|---|
| 5.1 | Nutrition → **Review Plan**. | Bottom sheet "Your supplement plan". Section **GENESYX ESSENTIALS**, subtitle "Gentle, evidence-informed essentials for fertility prep.", four rows with chip, name + dose range, one-line benefit, bell icon. | |
| 5.2 | Check the four benefit lines. | Folate: "Supports egg quality and early cell development." · Omega-3: "Hormone balance and reduced inflammation." · Vitamin D: "Supports ovulation and overall wellbeing." · Zinc: "Supports the LH surge that triggers ovulation." | |
| 5.3 | Tap Folate's bell → pick 09:00 → OK. | Bell fills, "Reminder at 9:00 AM" under Folate. Tap the bell again → reminder off. | |
| 5.4 | Section **YOUR SUPPLEMENTS**. | Subtitle "Add your own supplements to keep everything in one place." Fields Name, Dose, Time dropdown (Morning / Afternoon / Evening / Anytime). Button "+ Add your own supplement" is **disabled** while Name is empty. | |
| 5.5 | Type Name "Magnesium", Dose "300 mg", Time "Evening", tap Add. | Row "Magnesium — 300 mg · Evening" appears in the sheet's list; form clears. | |
| 5.6 | Tap **Got it**. | Sheet closes. Plan card now shows **five** chips (F O D Z M); status "None logged yet today". "Your supplements" card lower on the page lists Magnesium too. | |
| 5.7 | Tap **M**. | "1 of 5 logged today". | |
| 5.8 | Kill the app, relaunch → Nutrition. | Magnesium chip still there and still logged. | |
| 5.9 | (Supabase) `select name, dose, time_of_day from user_supplements where user_id = <you>`. | Row `Magnesium / 300 mg / evening`. `select supplements from daily_logs where date = today` → contains `Magnesium`. | |
| 5.10 | Add a supplement named "Folic acid". | No fifth/sixth chip — it is the same as Folate; only the list entry appears. | |
| 5.11 | Review Plan → tap the bell on **Magnesium** (your own entry) → pick 09:00 → OK. | Bell fills, "Reminder at 9:00 AM" under Magnesium. The "Your supplements" card on the tab shows the same reminder for Magnesium. Tap the bell again → off in both places. | |

## 6. Insights "Nutrition consistency" reflects Nutrition (SFM-28b)

| # | Step | Expected | Pass / Fail |
|---|---|---|---|
| 6.1 | Nothing logged this week → Insights. | Card shows only the gentle empty copy "No supplements logged yet this week — even one, whenever you remember, is a gentle start." | |
| 6.2 | Nutrition → tap **F** and **Z** → Insights (no restart). | Card shows "Today · 2 of 4 logged", chips Folate ✓ and Zinc ✓ (others plain), today's bar at 50 %, "Days logged 1/7", "Supplements taken 2". | |
| 6.3 | Nutrition → un-log both → Insights. | Back to the empty copy (the week is genuinely empty again). | |
| 6.4 | With Magnesium added (5.5) and logged → Insights. | "Today · 1 of 5 logged", Magnesium ✓ chip. | |
| 6.5 | Log something yesterday via Track calendar → Insights. | Two bars, "Days logged 2/7". Copy never uses "missed" / "failed" / "forgot". | |

## 7. Greeting shows `profiles.display_name`

| # | Step | Expected | Pass / Fail |
|---|---|---|---|
| 7.1 | Sign in as an account whose `profiles.display_name` is "Chezelle Madekwe" but whose email is `chezelle.madekwe@…`. | Home greeting reads "Chezelle Madekwe" — not "chezelle.madekwe". | |
| 7.2 | Profile → Personal details → change name to "Chez" → Save. | Dialog closes only on success; Home greeting updates to "Chez". Kill + relaunch → still "Chez". `profiles.display_name` = "Chez". | |
| 7.3 | Account with `display_name` NULL. | Greeting falls back to a tidy name from the address (e.g. "Chezelle Madekwe"), and that fallback is **not** written to `profiles` (column stays NULL). | |

## 8. Profile tabs save and survive restart

| # | Step | Expected | Pass / Fail |
|---|---|---|---|
| 8.1 | Profile → Health profile → change cycle length → Save. | Dialog closes; value shown. Kill + relaunch → value kept. | |
| 8.2 | Profile → Tracking preferences → change an answer → Save. | Same. Clearing all answers also sticks after relaunch. | |
| 8.3 | Turn **off** Health data consent, then try 8.1. | Save is disabled / the dialog stays open and says consent is withdrawn — it does **not** close as if saved. Personal details (name) still saves — it is not health data. | |
| 8.4 | Airplane mode → Health profile → Save. | Dialog stays open with a "couldn't reach the server" style message — never a silent close. | |

## 9. pH → "See your supplement plan"

| # | Step | Expected | Pass / Fail |
|---|---|---|---|
| 9.1 | pH tab → "See your supplement plan". | Nutrition tab opens **with the plan sheet already open** (not the Track "Nutrition" tracker, not the tab with nothing open). | |
| 9.2 | Dismiss with "Got it", rotate the device. | Sheet does not re-open. | |

## 10. Offline / failure behaviour of a chip toggle

| # | Step | Expected | Pass / Fail |
|---|---|---|---|
| 10.1 | Airplane mode ON → Nutrition → tap **F**. | Chip fills; snackbar "Folate saved on this device — it'll sync when you're back online." (Queued, not "saved"). Profile → sync status shows 1 pending. | |
| 10.2 | Kill the app while still offline, relaunch. | F still filled (it is in Room). | |
| 10.3 | Airplane mode OFF, wait ≤ 1 min (or open Profile). | Pending count returns to 0; Supabase `daily_logs.supplements` for today contains "Folic acid". | |
| 10.4 | Offline: un-log F (the only item). | Snackbar again; chip hollow; after reconnecting the server row has `supplements = {}` — the clear reached the server. | |
| 10.5 | Profile → Health data consent **off** → Nutrition → tap a chip. | Red banner at top of Nutrition; chip does **not** fill; snackbar "Not saved — health data collection is off. Turn it on under Profile → Health data consent." Nothing written (Insights unchanged). | |
| 10.6 | Consent back on → tap the chip. | Works again. | |
| 10.7 | (If reproducible) any local write failure. | Snackbar "Couldn't save Folate. Nothing was changed." with a **Retry** action; the chip stays as it was. No fake-saved state anywhere. | |

## 11. Nothing else regressed

| # | Step | Expected | Pass / Fail |
|---|---|---|---|
| 11.1 | Hydration card on Nutrition: + / −, "Edit goal", **"Track ›"**. | Water changes; goal dialog; "Track ›" opens the Hydration detail (Back returns to Nutrition). | |
| 11.2 | "7-day hydration challenge" card on Nutrition. | Same day count as the Home card; tapping opens Hydration detail. | |
| 11.3 | Home → hydration challenge card. | Unchanged from before this sprint. | |
| 11.4 | Reminder fires for a plan supplement (5.3 set to one minute ahead). | Notification "Time for your Folate"; tapping opens Nutrition. | |
| 11.5 | Delete a custom supplement from the "Your supplements" card. | Its chip disappears from the plan card; counts go back to "of 4". Its reminder (if any) stops. | |
