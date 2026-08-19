# Genesyx Android — FEATURES.md

**Source-verified against this tree on 19 Aug 2026** (HEAD `77226c2`, source **1.4.0 /
versionCode 14**, Room **v9**, compile/target SDK 36, min 26). This is what the code does — not
what Play serves (Internal: 1.3.0 (11), Production: 1.2.0 (9) at last audit).

> ⚠️ Do not copy from iOS `docs/FEATURES.md` — it is flagged stale (wrong pH range) in
> `IOS_PARITY_IMPLEMENTATION.md`. When this file and the source disagree, the source wins;
> fix this file.

Feature flags: `PH_TRACKING = true`, `PUSH_NOTIFICATIONS = true` (local only),
`ADMIN_CLIENTS = false`, `PARTNER_INVITES = false`.

---

## Navigation

Seven bottom tabs, same order as iOS: **Home · Track · pH · Nutrition · Insights · Learn ·
Profile**. Auth hard-gate — no Home without an account.

## Onboarding & auth

- Splash (floating eggs) → intro → quiz → readiness summary → waitlist / "Unlock my free guide"
  (email capture via `join_waitlist` SECURITY DEFINER RPC — Android-only step, kept per Q7) → auth.
- Email + Google sign-in; **Forgot password** on the Auth screen; change password from Profile.
- Quiz: sex-preference question is optional (Girl / Boy / No preference / Prefer not to say);
  unanswered ≠ "Prefer not to say". Helper: "Genesyx does not predict or influence a baby's sex."
- Quiz answers persisted and synced (`QuizAnswersRepository`).
- Account deletion in-app via `delete_current_user()` RPC. Sign-out wipes local data.
- Guest paths exist but are dead/unreachable in release (Q14: leave dead).

## Home

- Named greeting, cycle-phase "Today's focus" card + "Learn about this phase →" link.
- Hero metrics incl. **"Predicted ovulation"** (predictions are always labelled predicted/estimated).
- Hydration-ring and pH-nudge cards with deep links into their tabs.
- New-article card when a weekly Learn drop is live; streak with milestones and "log yesterday to
  reconnect".
- Bottom help link → `getting-started-first-week`.

## Track & daily log

- Calendar: phase fill (period / fertile / ovulation) + day dots for pH, symptoms/notes, intimacy
  (`DayMarkers`). Tapping a day opens that day's Log; backdated logging supported with back-guard.
- Log records: mood, energy, symptoms, sleep, water, food groups, supplements (plan + user's own),
  notes, and a **private intimacy toggle** ("Private to you" — never in streaks, never shared).
- Offline queue: offline saves land `PENDING_UPSERT`; `DailyLogSyncWorker` drains with backoff.
  A pull never overwrites an unsynced local edit. Known gap: cycle settings have no retry queue.
- Bottom help link → `guide-how-the-log-works` (on Track and on the Log screen).

## Vaginal pH (dedicated tab)

- **Vaginal pH**, two-band model: input 3.8–7.0, step 0.1, default 4.2; **Healthy 3.8–4.5,
  Elevated > 4.5** (client-approved 28 Jul 2026; `PhStatus` is the single source of thresholds).
- Add / edit / history; chart with band fills, boundary hairlines and **numeric axis marks at
  3.8 / 4.5 / 7.0**; pre-migration readings marked **"urine (legacy)"** — muted hollow dots,
  excluded from the trend line, display-clamped to the axis, never reclassified (Q13).
- **Syncs to Supabase** (`ph_readings`, write-through + retry queue + pull-merge on sign-in); the
  card discloses "pH entries sync to your Genesyx account" — keep that line. Guests stay on-device.
- Education on the detail screen: why it matters, accuracy caveat ("For a reading you can trust"),
  "Supporting your vaginal health", **"How this relates to fertility"** (Android-only, cautious,
  pending medical-reviewer sign-off), expandable "About this tracker" disclaimer, citations.
- Copy in `PhCopy.kt` is pinned by verbatim + banned-phrase tests: no condition names, no dietary
  advice, GP/pharmacist signposting only. Wellness app, not a medical device.
- Bottom help link → `guide-understanding-vaginal-ph`.

## Nutrition

- **Hydration card**: user-set goal (1000–5000 ml, default 2400, step 200), cups or ml display
  (storage always ml — Q3), custom glass size, steppers with minus-to-correct, progress bar,
  intraday coaching, days-on-goal, collapsed **"Why hydration?"** expander (`HydrationCoach`).
- **"What you ate today"** — six food-group chips (vegetables, fruit, starchy carbs, protein,
  dairy & alternatives, oils & fats; raw values shared with iOS). Counter 0/6, "What counts as
  what?" expander, footnote: "A record, not a target…". Nothing is scored; chips DO count toward
  the streak (Q12). Free-text meal notes remain below as an optional card (Q1 = B).
- **Recipes for your cycle** — 8 photographed recipes, phase-tagged, vertical expandable cards
  (Q9): photo, uses-line, time · serves, ingredients, method, and an additive **"Log \[groups]"**
  action (union — never un-ticks). Recipes cook a reviewed focus food; no new health claims.
- **Suggested supplements plan card** with live adherence: **"None logged yet today" / "N of M
  taken today"** (iOS strings verbatim), plus a "Why is this important?" expander (Android-only).
- **Your supplements** — user-added entries (name, dose, time) with optional local reminders;
  synced via `user_supplements`; Genesyx range catalogue reads `genesyx_products` and renders
  "coming soon" while empty.
- Learn-more rows filtered to **nutrition-category** articles (published only).
- Bottom help link → `guide-nutrition-focus`.

## Insights (all real data, no mocks)

- Weekly summary; hydration card leading with **"N of 7 days on goal"** (same `StreakEngine`
  figure as Home) plus ml/day delta and weekday bars; **"Days with meals N/7"** from food groups.
- pH 7/30-day averages; sleep; plan-based supplement consistency; **"Your supplements this week"**
  (per-supplement N/7 dot rows); private **Intimacy** card (hidden until first recorded, no
  fertility framing).
- **Symptom patterns** — 4×7 (28-day) heatmap + most-logged symptom (`SymptomPatternLogic`);
  summary counts the same 28 days the grid shows.
- **Ovulation** — predicted ovulation + fertile window on a current-cycle timeline
  (`OvulationCard`); position in the one saved setup, never claimed as history.
- **Cycle length** — configured length vs the typical range (`CycleRegularityLogic`); explicitly
  not a regularity measure (no per-cycle history exists).
- **My Logs** — full daily-entry history (`LogHistoryScreen`).
- Bottom help link → `reading-your-trends`.

## Learn

- **32 articles**: 10 launch reads + 10 always-available guides + 12-week Sunday drip
  (23 Aug – 8 Nov 2026). Every article has a unique 1080×602 hero photo (32/32).
- Permanent hub cards: **"How to use Genesyx"** (guides grouped by tab) and **"Start your 12-week
  plan here"** (future weeks named but not openable — "Arrives …").
- **Search** over articles (`Screen.LearnSearch` from the Learn top bar).
- `LearnDrip.published(today)` is the only date-gate resolver; deep links land inside the article
  (the `learn/article/{slug}` route), never on the list.
- Week 12 is the Shettles Method, framed explicitly as not a proven method (drip-gated 8 Nov).
- Article reads count toward the streak (Android-only input; contract vectors untouched).
- Medical sources / citations screen (`LearnSourceMap`); pH citations deliberately avoid
  condition-named sources (Q4).

## Profile

- Personal Details (edit name / change email / change password in one dialog), Health Profile,
  Tracking Preferences, **focus mode** (`FocusMode` via `PreferencesRepository`),
  **"How to use Genesyx"** row (Q11), theme Light / Dark / System (default **Light**),
  notification settings, Privacy & Data (opens `PRIVACY_POLICY_URL`), sign out, delete account.

## Notifications

- **Local only** (WorkManager; no FCM, no device token). Opt-in kinds incl. daily-log reminder,
  supplement reminders, fertile-window (`ReminderKind.FERTILE_WINDOW`), new-article. Android has
  more kinds than iOS — deliberate; do not cut to match.

## Streak & tracking contract

- `StreakEngine` v2; shared cross-platform spec `tracking_test_vectors.json` (16 cases, mirrored
  in the iOS repo) — **the spec wins over the engine**. Weeks complete at 4-of-7 logged days;
  `daysOnGoal` ≠ `daysLoggedThisWeek`. Meaningful day = logs / pH / article read; intimacy never
  counts. `PreferencesRepository` is the only writer of the hydration goal.

## Architecture & backend (summary)

- Local-first: Room (v9) is source of truth; Supabase read-through on sign-in + write-through.
  Kotlin, Compose, Material 3, Hilt, DataStore, WorkManager. No analytics sink, no Crashlytics.
- Supabase: Auth, `profiles`, `daily_logs`, `cycle_settings`, `ph_readings`, `user_supplements`,
  `genesyx_products` (read-only catalogue), `join_waitlist` + `delete_current_user` RPCs. RLS
  owner-only throughout. Details: `ARCHITECTURE.md`, `docs/DATA_LAYER.md`, `docs/schema.sql`.

## Not built / deliberately absent

Partner invites & sharing (flag off), pregnancy mode (stub), Sign in with Apple (Q6), Health
Connect / wearables, home-screen widget, barcode or photo meal logging, FCM push, localisation
(English hardcoded), cycle-to-cycle learning claims (the app has one cycle-settings row, not
period history — and says so).
