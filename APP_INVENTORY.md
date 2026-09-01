grok# Genesyx Android — Repository and App Inventory

**Purpose:** the code-backed map of the current Genesyx Android checkout: app surfaces, tracked
data, derived logic, persistence and sync, notifications, backend gates, privacy and verification.

This inventory was refreshed from the repository on **12 August 2026**. It describes the inspected
source, not a promise that every source feature is deployed to Supabase or published through Play.

| Repository snapshot | Current value |
|---|---|
| Branch / HEAD | `main` / `43b371c` |
| Gradle source identity | **1.4.0 (versionCode 14)** |
| Next-release warning | Codes 12 and 13 are superseded; never reuse them. 14 is unused on Play and is the identity to upload |
| Play state | Not live-verified by this inventory; historical repository notes are not current proof |
| Application ID | `com.genesyx.app` |
| SDK | compile/target 36, min 26 |
| Main stack | Kotlin, Jetpack Compose, Material 3, Hilt, Room, DataStore, WorkManager, Supabase Kotlin |
| Main navigation | Seven bottom tabs: Home, Track, pH, Nutrition, Insights, Learn, Profile |
| Local database | Room schema v9 |
| Preferences/session | Preferences DataStore (`genesyx_prefs`) |
| Cloud | Supabase Auth + PostgREST + RPCs where configured |
| Notifications | Local WorkManager reminders; no FCM/device token |
| Analytics | No-op logger; no Firebase/GA/Crashlytics sink |

## 1. Release identity and critical boundary

`app/build.gradle.kts` declares `versionName = "1.4.0"` and `versionCode = 14`, which matches the
work in the checkout—Room v9, private intimacy logging, meal logging, user supplements, seven tabs,
backdated logs, sync-status UI, the opt-in intimacy reminder and Learn additions. Code 14 has never
been uploaded to Play, so it remains a valid release identity.

Therefore:

- **1.4.0 (14) is both the current Gradle identity and a safe release identity.**
- Before shipping the present tree, build a fresh signed AAB/APK and verify their baked-in identity.
  The backend migrations this tree needed are applied and REST-verified (19 Aug 2026).
- `assembleRelease`/`bundleRelease` now **fail** when `keystore.properties` is absent rather than
  silently falling back to the debug key, so a green release build is signed with the upload key.
- Release tasks intentionally fail when Supabase credentials are missing, preventing the
  password-free local auth fallback from shipping.
- Upload validation, Internal publication, Play installation and Production availability are four
  separate evidence levels.

## 2. Product and truth model

Genesyx Android is a native fertility-preparation, cycle-awareness and wellbeing-tracking app. It
combines projected cycle context, daily logging, hydration and nutrition guidance, vaginal pH,
real-data summaries, bundled Learn content, local reminders and dormant partner/client seams.

The app should keep these concepts distinct:

- **Recorded:** mood, energy, symptoms, sleep, water, supplements, notes, private intimacy, cycle
  settings and pH readings.
- **Derived:** phase, predicted fertile window, estimated ovulation, streaks and summaries.
- **Guidance:** educational and nutrition content; not diagnosis or treatment.
- **Prediction:** cycle outputs based on one cycle-settings row. The app does not have period-event
  history and cannot truthfully claim cycle-to-cycle learning or measured fertility.

## 3. Architecture and data flow

```text
Single Activity
    ↓
Jetpack Compose routes/screens
    ↓
Hilt ViewModels + StateFlow
    ↓
Repositories
    ├─ Room source of truth
    ├─ Preferences DataStore
    ├─ Supabase Auth/PostgREST when configured
    └─ WorkManager offline queues and local reminders
```

This is pragmatic MVVM with `data`, `domain` and `ui` packages, not strict use-case-layer Clean
Architecture. `GenesyxApplication` initializes channels; `MainActivity` hosts Compose, resolves the
start state and re-arms sync/reminder work on foreground. Hilt modules provide Room, DataStore,
Supabase/stubs, repositories and scheduling dependencies.

Current insight calculations are mostly separate pure objects under `ui/insights`. Track builds its
own summary layer. A future intelligent-partner programme should introduce one shared pure domain
engine so Track, Insights, Home and notifications cannot disagree about evidence thresholds.

## 4. Navigation and screens

### Seven bottom tabs

The bottom bar contains, in order:

1. Home
2. Track
3. pH
4. Nutrition
5. Insights
6. Learn
7. Profile

The dedicated pH tab reuses the canonical `tracker/ph` route. Home, Track, Insights and the pH deep
link reach that one surface. Bottom-tab navigation uses saved/restored state. Seven labels are tight
on 360 dp devices and require small-screen/font-scale QA.

### Onboarding and account routes

- Splash
- Introduction
- Five-question quiz
- Readiness Summary
- Waitlist
- Auth

Quiz answers remain in-memory and are discarded. The repository knows about the new owner-only
`quiz_answers` backend contract only as an audit constraint; Android does not yet persist or edit
those answers. Waitlist storage uses the `join_waitlist` RPC where Supabase is configured.

### Main and secondary destinations

| Surface | Route | Current purpose |
|---|---|---|
| Home | `home` | Cycle context, hydration, pH nudge, daily action, streak/new-article state |
| Track | `track` | Calendar, day markers, tracker summaries and date navigation |
| pH | `tracker/ph` | Canonical vaginal-pH entry, chart and history; also a bottom tab |
| Nutrition | `nutrition?plan={plan}` | Tappable plan chips (F O D Z + her own) logging straight to `daily_logs.supplements`, plan sheet with add-your-own form, hydration + 7-day challenge, food groups, meals, recipes, Genesyx-range empty state |
| Insights | `insights` | Real repository-derived cards and empty states |
| Learn | `learn` | Bundled articles, categories and search |
| Profile | `profile` | Account, profile/settings, reminders, sync state and legal/support |
| Log | `log?date={date}` | Today or backdated daily-log editor |
| Log history | `log_history` | Chronological recorded data |
| Cycle detail | `tracker/cycle` | Phase/projection details and settings |
| Hydration detail | `tracker/hydration` | Daily editor, goal, history and seven-day view |
| Sleep detail | `tracker/sleep` | Weekly duration summary and editor |
| Symptoms detail | `tracker/symptoms` | Four-week heatmap and dated history |
| Nutrition detail | `tracker/nutrition` | Today's supplements + food groups by name, a LOG SUPPLEMENTS checklist (essentials + her own, same toggle path as the Nutrition chips), a button into the shared supplement-plan sheet, Mon–Sun dot strips for both, footer only when the week is empty |
| Learn search | `learn/search` | Search available articles |
| Article | `learn/article/{slug}` | Article content, sources, related links and CTAs |
| Reminder settings | `reminder_settings` | Master, kind, schedule and quiet-hours controls |
| Pregnancy | `pregnancy` | Preview/static placeholder only |
| Invite | `invite/{code}` | Added only when partner feature flag is enabled |
| Clients | `clients` | Dormant admin surface |

Current notification/in-app deep links include Home, Track, Nutrition, Insights, Log, Learn, pH,
hydration and — as of 12 Aug (Phase 2) — `tracker/{cycle,sleep,symptoms,nutrition}`. Individual
intelligent-report destinations are still to come with the guidance programme (Phases 6–8).

## 5. Tracking model

### Daily log

One date-keyed `DailyLog` may contain:

- Mood
- Energy
- Symptoms
- Sleep minutes
- Supplement-name strings
- Notes
- Water millilitres
- Private intimacy (`sexualActivity`: nullable; null means not recorded)

The Log route accepts a date. Non-future Track days can add or edit that exact record. A first-load
guard prevents asynchronous database state from being overwritten by a blank editor.

The shared `isMeaningful()` chi ontract counts water, mood, energy, symptoms, sleep, supplements or a
note. Private intimacy is intentionally excluded to preserve the existing Android tracking-vector and
streak contract.

### Cycle

- One settings row per user: last-period date, typical cycle length and period length.
- Cycle length 21–35 days; period length 1–10 days.
- Estimated ovulation: `cycleLength - 14`.
- Predicted fertile window: five days before through one day after estimated ovulation.
- No period-event/cycle history, so no cycle-to-cycle regularity or multi-cycle pattern claim.

All customer-facing cycle copy should qualify projected fertility. Any “You’re in your fertile
window” wording without “predicted” or “estimated” is a copy defect. As of 12 Aug the fertile
overlay, Track current-phase card, calendar legend and ovulatory hero all carry the qualifier,
pinned by `NutritionContentTest`'s content-safety guards.

### Vaginal pH

- New input range **3.8–7.0**, step 0.1, default 4.2.
- Healthy **3.8–4.5**; Elevated **>4.5**.
- Legacy urine readings remain labelled and excluded from vaginal classification/insights.
- Repository rounds, validates, writes Room first and queues Supabase retries.
- Current insight can show status, direction, rolling 7/30-day averages and descriptive copy.
- Two vaginal readings in seven days are required for the written pH insight; charts need at least
  two readings in the selected range. The chart labels its axis at 3.8 / 4.5 / 7.0 (from
  `PhStatus`), in a left gutter the plot is inset past (19 Aug).
- Medical copy and source links are centralised; no diagnosis, dietary treatment or condition claim.

### Hydration

- `waterMl` is the canonical storage, sync, goal and calculation unit.
- Display choices are ML or cups; one Android cup is fixed at 250 ml.
- Goal range 1,000–5,000 ml; default 2,400 ml.
- Custom quick-add glass amount is stored separately and clamped 100–1,000 ml.
- Current Insights uses a rolling seven-day average, previous-seven comparison and goal scoring.
- The model stores one daily total, not drink timestamps. It cannot infer time-of-day drinking
  routines from existing data.

### Sleep

- Stored as minutes on the daily log.
- Current card is Monday–Sunday and unlocks with one positive logged night.
- The UI calls the entry “Last night,” but the value shares the selected daily row. Any connection
  from sleep to next-day energy needs an explicitly approved date-joining rule.
- No actual bedtime, wake time or sleep-event history exists.

### Symptoms

- Stored as a set of strings on the daily log.
- Four-week heatmap and dated history are implemented.
- The pattern-ready threshold is seven symptom days **within the same rolling 28-day window the
  grid displays** (corrected 12 Aug — it previously counted all-time history, letting weeks-old
  symptoms qualify a "pattern" the visible grid contradicted). The summary copy names the window.

### Supplements and nutrition

- Fixed logged vocabulary includes Folate, Omega-3, Vitamin D, Zinc and Iron; the scored default
  plan excludes Iron.
- User supplements have ID, name, optional dose, coarse time of day and optional product ID.
- Room + WorkManager sync infrastructure exists for user supplements.
- Daily adherence stores names in `daily_logs.supplements` (a cross-platform `text[]`). As of
  12 Aug the Log dialog can never *hide* a logged string — renamed/other-device entries render as
  orphan rows (`SupplementLogRows`). Robust *scoring* of custom supplements by stable ID remains a
  BLOCKED cross-platform contract change: the fixed plan (4 built-ins) is still the adherence
  denominator, and custom entries are recorded, not scored.
- The suggested-plan card carries a live adherence line — “None logged yet today” / “N of M taken
  today” — scored against the 4-item plan from today's Log toggles, matched trimmed and
  case-insensitively (`SupplementPlanProgress`, 19 Aug).
- `SupplementTime` is Morning/Afternoon/Evening/Anytime—not an exact reminder time.
- The `genesyx_products` catalogue renders a coming-soon state when the backend has no products.

## 6. Insights and present evidence thresholds

All current cards use actual Room/repository data and show empty states; there are no hardcoded
sample charts in production paths.

| Surface | Current evidence and limitation |
|---|---|
| Weekly summary | Current Mon–Sun week; unlocks with one meaningful day; comparisons need one relevant point in each week |
| Consistency | Shared streak engine; four meaningful days make a qualifying week |
| pH | Vaginal-only; status/latest-two direction and rolling averages |
| Hydration | Rolling seven days; can appear after one positive water entry. Leads with “N of 7 days on goal” — StreakEngine's number, so it always matches Home (19 Aug) |
| Supplements (plan) | Current week; adherence vs the fixed 4-item starter plan; one built-in scored entry can unlock |
| Your supplements this week | Current week; each of her own "Your supplements" (Nutrition) with an N/7-days count + Mon–Sun dot row, matched by name against `daily_logs.supplements`; hidden when she has none. Separate from the plan card, which is unchanged (13 Aug) |
| Sleep | Current Mon–Sun week; one positive night can unlock |
| Cycle | Describes current settings, not historical regularity |
| Symptoms | 28-day display; qualification now uses the same rolling window (12 Aug) |
| Intimacy (private) | Current week; days she recorded the private `DailyLog.sexualActivity` flag, with a dot row + plain count. Hidden until first recorded, labelled "Private to you", no timing/fertility framing, never shared with any partner feature (13 Aug) |
| Ovulation | Estimate from current cycle settings |
| My logs | Full chronological owner history |

The current source does **not** implement:

- A unified Evidence State model.
- One shared Guidance/Intelligence engine.
- 7/14/21/30-day reports or “Your picture” programme.
- Cross-signal observation contracts.
- Per-insight evidence denominators and contributing-date explanations.
- Data signatures, reviewed/dismissed state or material-change deduplication.
- Multi-cycle learning.

Customer-facing copy must not turn low coverage into “a pattern.” One useful rule already exists:
four meaningful days qualify a week. The intelligent programme should add signal-specific coverage
instead of weakening that shared contract.

## 7. Learn and content

- **Thirty-two** bundled Android articles ship with categories, search, related links, share and
  CTAs: 10 launch + 10 "guide" how-tos + the 12-article dated weekly series, all ported from iOS
  1.2.0 (18) on 12 Aug with matching slugs/ids (and, for the series, matching publish dates
  23 Aug → 8 Nov). Slugs/ids match iOS for cross-platform read-state/deep-link parity.
- `LearnDrip` reveals by **fixed calendar date** (`Article.publishedAt`, cross-platform contract
  with iOS, 12 Aug) — everyone sees a dated article on the same real day. Every Learn surface plus
  the Home card and `NEW_ARTICLE` reminder resolve through it, so a future-dated article is hidden
  everywhere at once, including a stale deep link.
- The 12 weekly-series articles carry `publishedAt` dates (23 Aug → 8 Nov 2026, consecutive
  Sundays); the other 20 are always-available. The week-12 Shettles piece (8 Nov,
  `shettles-method-theory-vs-evidence`) is now included on Android, framed as **explicitly-unproven
  theory** with guard-railed language — kept within the banned-phrase rules (no sex-selection
  claims); flag for the medical reviewer before it goes live.
- `NEW_ARTICLE` reminder exists and is opt-in; it stays silent while no dated article is due today.
- Home's last block is a persistent **"A read for your week"**: the freshly released weekly article
  when one dropped in the last 7 days, else a deterministic weekly rotation over published editorial
  articles.
- Medical/disclaimer copy and pH citations are compiled into the app. The `LearnContentTest`
  banned-phrase guard dropped bare `"diagnos"`/`"douch"` (substring false-positives on responsible
  disclaimer language) on 12 Aug — condition-name and pseudoscience bans are unchanged; flagged for
  medical-reviewer confirmation.

Do not create placeholder articles simply to fill the weekly series; port only reviewed content.

### Hero images — complete (32 of 32)

Every article has a 16:9 hero (`Article.heroImage`, `@DrawableRes`). The 22 that were missing art were
supplied and wired on **13 Aug 2026** (client-provided stock, converted to the shipping spec). All
heroes are **1080 × 602 px JPG in `drawable-nodpi/`**, named `learn_hero_<name>`. The gradient
fallback (category-keyed) remains in code for any future article added without art.

Verified on-device: `LearnHeroImagesTest` (androidTest) asserts no article has a null hero and that
**every hero drawable decodes to a real 1080 × 602 bitmap** — so a missing/corrupt/wrong-size asset
fails CI, not just a screenshot.

The 22 added, with their drawables:

| Article | Drawable |
|---|---|
| Using the vaginal pH tracker | `learn_hero_guide_ph_tracker` |
| How the daily log works | `learn_hero_guide_log` |
| How to log a pH reading | `learn_hero_guide_log_ph` |
| How your nutrition focus works | `learn_hero_guide_nutrition` |
| How the Hydration Coach works | `learn_hero_guide_hydration` |
| Reading your pH trend | `learn_hero_guide_ph_trend` |
| How your cycle and phases work | `learn_hero_guide_cycle` |
| How sleep tracking works | `learn_hero_guide_sleep` |
| How to log how you feel | `learn_hero_guide_symptoms` |
| Understanding your vaginal pH | `learn_hero_guide_understanding_ph` |
| Understanding your fertile window | `learn_hero_fertile_window` |
| What your vaginal pH is actually telling you | `learn_hero_ph_explained` |
| Eating in the months before conception | `learn_hero_before_conception` |
| What cervical mucus can tell you | `learn_hero_cervical_mucus` |
| Hydration and reproductive health | `learn_hero_hydration_repro` |
| Timing sex when you are trying to conceive | `learn_hero_timing_sex` |
| Sleep, stress and your cycle | `learn_hero_sleep_stress` |
| Understanding ovulation tests | `learn_hero_ovulation_tests` |
| Supporting sperm health | `learn_hero_sperm_health` |
| Fertility supplements, and what the evidence says | `learn_hero_supplements_evidence` |
| When to ask for fertility support | `learn_hero_ask_for_support` |
| The Shettles Method: theory versus evidence | `learn_hero_shettles` |

Every article has its own distinct hero — no photo is shared. The three sister topics that initially
reused a source photo were given dedicated art on 13 Aug (generated to match the pastel style):
`learn_hero_ph_explained` (pH strip + colour chart), `learn_hero_ovulation_tests` (ovulation test
sticks), and `learn_hero_shettles` (a neutral notebook + calendar still life, deliberately no
sex-selection imagery).

**The 10 articles that already had art** (unchanged): Your first week with Genesyx, Why logging beats
remembering, Symptoms and meals: what's worth writing down, Hydration without the eight-glass myth,
Eating with your cycle not against it, A gentle guide to supplements, What "insights" actually means,
Reading your trends without over-reading them, Small habits that hold, Using what you learn.

## 8. Notifications

Notifications are entirely local:

- No Firebase Cloud Messaging dependency, token or remote-push pipeline.
- Four channels: tracking, nutrition/wellness, insights and re-engagement.
- Independent self-rescheduling one-time WorkManager chains.
- Re-armed on app foreground and reboot; cancelled on sign-out/delete.
- Android 13+ permission requested only after the customer enables reminders.
- Quiet hours and a global cap of two per day; re-engagement is exempt.

Current kinds:

1. Daily log
2. Missed log
3. Hydration
4. Nutrition (reserved/not scheduled)
5. Weekly insights
6. Re-engagement
7. Predicted fertile-window start
8. New article (opt-in)

Current gaps against the Android intelligent-partner goal:

- No sleep, pH, symptom-pattern or per-supplement reminder kinds.
- No exact per-supplement time model.
- Hydration policy does not yet inspect goal completion strongly enough.
- Weekly Insights currently unlocks from insufficient general evidence.
- No 7/14/21/30-day report-ready notification.
- No account-scoped candidate/signature/review ledger.
- Independent workers can race against the daily counter; intelligent candidates need one serialized
  priority arbiter.
- Fertile-window lock-screen copy exposes sensitive context; there is no discreet/descriptive mode,
  `VISIBILITY_PRIVATE` public fallback or copy-privacy scan.
- No immediate time/timezone-change receiver.
- WorkManager is deferrable delivery, not an exact-alarm guarantee.

## 9. Persistence and sync

### Room v7

Eight entity types:

1. `cycle_settings`
2. `daily_logs`
3. `ph_readings`
4. `profiles`
5. `clients`
6. `partner_invites`
7. `partner_links`
8. `user_supplements`

Upgrade migrations preserve data:

- 2→3: pH sync metadata/tombstones
- 3→4: daily-log sync status
- 4→5: pH measurement type
- 5→6: user supplements
- 6→7: nullable private intimacy field

Only downgrade allows destructive fallback.

### DataStore

Stores theme, focus, push master, hydration goal/unit/glass size, Learn/read/drip state,
onboarding/notices, streak celebrations, reminder schedules/counters and the local session mirror.
The session mirror contains signed-in state, user ID, email and display name.

### Offline-first queues

Daily logs, pH and user supplements follow the strong path:

1. Write Room immediately.
2. Mark signed-in work pending.
3. Attempt Supabase push.
4. On failure, enqueue unique connected WorkManager work with exponential backoff.
5. Pull/merge without overwriting unsynced local rows.
6. Re-arm on foreground.
7. Show combined pending count in Profile with Sync now.

Cycle settings and profile writes do not have equivalent durable pending queues. A failed offline
cycle/profile push is a remaining asymmetry. Guest pH and supplements are adopted on sign-in;
guest daily logs are not.

## 10. Supabase/backend boundary

Current source directly addresses:

- `profiles`
- `cycle_settings`
- `daily_logs`
- `ph_readings`
- `user_supplements`
- `genesyx_products`
- `join_waitlist` RPC
- `delete_current_user` RPC

RLS is the server privacy boundary; client queries additionally filter the current user ID.

### Blocking backend gates

Two source features are ahead of the documented production schema:

- `docs/migrations/2026-07-29_user_supplements.sql` is marked **PROPOSED — NOT YET APPLIED**.
- `daily_logs.sexual_activity` is marked **NOT YET IN PRODUCTION**.

Before shipping a client that writes either field/table:

1. Apply in staging.
2. Verify authenticated REST read/write and cross-account RLS isolation.
3. Verify tombstones/merge and offline retry.
4. Verify `delete_current_user` removes user supplements and intimacy-bearing logs while preserving
   the shared product catalogue.
5. Apply in production and repeat verification.
6. Only then build and upload the new Android version.

Partner invites remain a local placeholder and `PARTNER_INVITES` is off. Android must not present
partner invitation or health-data sharing as an implemented customer feature.

## 11. Authentication, privacy and account lifecycle

- Supabase email/password authentication.
- Google Credential Manager → ID token → Supabase.
- Password and email changes with reauthentication.
- Sign-out.
- Remote-first account deletion through `delete_current_user`.
- Newly minted-token/email checks defend against a stale ambient session.
- Sign-out cancels reminders, clears every Room table and clears local session state.
- Local permissive auth exists only when Supabase config is absent; release builds are guarded
  against missing credentials.

Privacy caveats that require explicit product/release treatment:

- Room uses ordinary SQLite; no SQLCipher/encrypted Room layer is configured.
- Preferences DataStore is not encrypted.
- Android backup is enabled and current backup rules do not explicitly exclude Room/DataStore
  health data.
- Production suppresses debug logs, but info/warning/error may still reach Logcat.
- Notification lock-screen privacy is incomplete for sensitive cycle content.
- Private intimacy is owner-scoped and excluded from partner code and streak qualification.
- Analytics is currently a no-op logger, not a production analytics service.

## 12. Feature flags and dormant capabilities

| Capability | Flag/state |
|---|---|
| Vaginal pH | `PH_TRACKING` on |
| Local reminders | `PUSH_NOTIFICATIONS` on |
| Partner invite/linking | `PARTNER_INVITES` off; placeholder/local behaviour only |
| Admin Clients | `ADMIN_CLIENTS` off; remote source incomplete |
| Pregnancy tracking | Preview/static only |
| Quiz-answer persistence | Not implemented on Android |
| Unified 7/14/21/30 intelligent partner | Not implemented |
| User supplements | Implemented in source; production schema gate outstanding |
| Private intimacy | Implemented in source; production column gate outstanding |

## 13. Verification state

Repository `CHANGELOG.md` records, for the 12 August source work:

- **327 unit tests green**.
- `:app:assembleRelease` green, including R8/lint for the recorded runs.
- Room v7 migration/schema checks added for private intimacy.

Those results were not rerun during this inventory edit. Before release, independently run at least:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleRelease :app:bundleRelease
```

Then verify:

- Connected/instrumented tests on an emulator/device.
- Backdated edits and sync queue recovery.
- pH, intimacy and supplement round-trips against the deployed Supabase schema.
- Sign-out and deletion clearing.
- Notification permission, schedule, privacy and cold/warm deep-link routing.
- Seven-tab layout on a small device and large font scale.
- Signed APK/AAB application ID, versionName/versionCode, certificate and SHA-256.
- Play Internal publication and Play-installed package identity separately.

## 14. Android implementation status

| Desired Android capability | Current Android source |
|---|---|
| Seven tabs with pH | Implemented |
| Backdated daily logging | Implemented |
| Private intimacy | Implemented locally; production column gate |
| Calendar signal markers | Implemented |
| pH 3.8–7.0 + legacy handling | Implemented |
| Hydration display preferences | Implemented: ML/cups display with millilitres as canonical storage |
| Custom supplements | Stronger Room/sync source exists, but backend migration is unapplied |
| Per-supplement clock reminders | Not implemented |
| Full Android notification controls and evidence-aware planner | Partially implemented |
| Quiz-answer persistence + Tracking Preferences | Not implemented |
| Profile Health/Personal editors | Mostly implemented; verify every row is repository-backed |
| Weekly Learn programme | Date-based drip live; 31 articles ported from iOS (10 launch + 10 guides + 11 dated weekly series) |
| Partner invite/accept/decline/unlink | Not implemented; feature remains off |
| Unified intelligent 7/14/21/30 programme | Not implemented |

## 15. Repository map

```text
app/src/main/kotlin/com/genesyx/app/
  auth/                  authentication and account lifecycle
  core/                  config, feature flags, logging and DI primitives
  data/                  repositories, Room/DataStore/remote/sync
  di/                    Hilt modules
  domain/                pure models, cycle, pH, hydration, streak and content logic
  notifications/         kinds, policy, scheduler, workers, notifier and channels
  ui/                    Compose screens, ViewModels, components and navigation

app/src/test/             JVM unit tests
app/src/androidTest/      instrumented/Compose/database tests
app/schemas/              exported Room schemas
docs/migrations/          proposed/deployment SQL
docs/schema.sql           documented backend shape
app/build.gradle.kts      build identity and release guards
CHANGELOG.md              newest source/release evidence
```

## 16. Maintenance rules

Update this file whenever a change adds or removes:

1. A screen, tab, route, deep link or notification destination.
2. A tracked field, threshold, metric, prediction or insight.
3. A Room entity/version/migration, DataStore key, repository or WorkManager queue.
4. A Supabase table/column/RLS policy/RPC or deletion obligation.
5. An authentication, notification, privacy, partner or analytics behaviour.
6. A capability moving between dormant, source-only, backend-blocked, tested or Play-released.
7. A versionName/versionCode or compiled/uploaded/published artifact state.

Always check current source and `CHANGELOG.md`. Keep **source identity**, **tested source**,
**backend-deployed state**, **signed artifact**, **Play-upload validation**, **Play publication** and
**Play-installed identity** separate. Never turn a prediction into a measurement or sparse data into
a customer-facing pattern.

---

_Last audited from the Android repository on 12 August 2026: `main` / `43b371c`; release identity
refreshed 19 August 2026. Current Gradle identity: **1.4.0 (14)**, which is unused on Play and is
the identity to upload._
