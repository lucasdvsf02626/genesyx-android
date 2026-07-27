# Genesyx — Product Inventory

Describes **current `main` at 1.3.1 (versionCode 12)**, targeting **Android 16**
(compileSdk/targetSdk 36, minSdk 26), **Room schema v5**. Every claim below was verified against the
tree on **2026-07-27** (file:line references throughout). Source root:
`app/src/main/kotlin/com/genesyx/app/`.

`FeatureFlags` [`core/FeatureFlags.kt`]: `PH_TRACKING` **on** (:11), `PUSH_NOTIFICATIONS` **on**
(:37, local reminders), `ADMIN_CLIENTS` off (:18), `PARTNER_INVITES` off (:27).

Six bottom tabs — **Home, Track, Nutrition, Insights, Learn, Profile** — in that order
[`ui/navigation/Screen.kt:55`]; deliberately one over the Material 3 maximum of five (Learn took
Profile's slot, :35). The bottom bar hides on every other screen [`ui/navigation/Screen.kt:58-84`].

---

## 1. SCREENS — 26 routes

Start destination is Splash [`ui/navigation/GenesyxNavGraph.kt:41`].

| Screen | Route | Purpose | Entry points |
|---|---|---|---|
| Splash | `splash` | Signed-out landing; brand statement and the two ways in. | Launch destination with no session; after account deletion |
| Onboarding Intro | `onboarding_intro` | Explains the three things the app does before asking anything. | "Start Your Personalised Quiz" on Splash |
| Onboarding Quiz | `onboarding_quiz` | Five questions (stage, cycle regularity, supplements, sex preference, support need). | "Continue" on Intro |
| Readiness Summary | `readiness_summary` | Closes the quiz; routes to guide or account. | Final quiz answer |
| Waitlist | `waitlist` | Collects an email for a nutrition guide. | "Unlock My Free Guide" on Readiness Summary |
| Home | `home` | Daily dashboard. First-run setup card when no cycle is set [`ui/home/HomeScreen.kt:462-515`]; otherwise cycle hero + 3 metrics (Cycle day / Next period / Ovulation, :251-283), Today's focus (:296-316), hydration ring card (:318-407), pH nudge card (:414-459, flag-gated), "Log today", signed-out sign-in banner. Avatar menu: Sign in / Profile / Cycle setup (:169-187). **No pregnancy link on Home.** | Bottom tab; launch destination when signed in |
| Track | `track` | Month calendar of cycle phases + "Your Trackers" list of six rows with real per-signal summaries (`TrackerSummaryLogic`), each opening a detail screen. | Bottom tab; pH card on Insights |
| Cycle Detail | `tracker/cycle` | Phase + metrics + explicitly-estimated fertile window / ovulation; edit settings. | Track "Your Trackers" |
| Hydration Detail | `tracker/hydration` | The canonical hydration editor: quick-add + manual entry (clamped), 7-day bars, days-on-goal, streak, daily history. Writes only through `DailyLogRepository`. | Track "Your Trackers"; Home hydration card (`genesyx://tracker/hydration`) |
| Vaginal pH Detail | `tracker/ph` | The pH tracker section (validation, chart, history). | Track "Your Trackers"; Home pH card (`genesyx://tracker/ph`) |
| Sleep Detail | `tracker/sleep` | Week summary + hours/minutes editor. | Track "Your Trackers" |
| Symptoms Detail | `tracker/symptoms` | 4-week heatmap + dated history into the log flow. | Track "Your Trackers" |
| Nutrition Detail | `tracker/nutrition` | Supplement summary into the logging flow. | Track "Your Trackers" |
| Nutrition | `nutrition` | Phase-aware food focus, hydration stepper + goal dialog, supplement plan card, article entry. | Bottom tab |
| Insights | `insights` | All-real-data trend cards — see §3. | Bottom tab |
| Learn | `learn` | Landing for ten bundled articles: featured hero, category chips, search. | Bottom tab; "See all articles" on Nutrition |
| Learn Search | `learn/search` | Free-text search over title, excerpt, tags. | Search icon on Learn |
| Article Detail | `learn/article/{slug}` | Reads one article; CTA buttons, related articles, share, medical disclaimer on 6 of 10. | Article rows anywhere |
| Log | `log` | Records the day: mood, energy, symptom chips + custom add, Sleep / Water / Supplements dialog tiles [`ui/screens/LogScreen.kt:231-238`], notes. Saves online **or offline** (offline queues — see §5). Back with unsaved edits raises "Discard your changes?" (`BackHandler`, :120-121, :264-288). | "Log today" on Home; "Add to today's log" on Track; article CTAs; `genesyx://log` |
| Log History | `log_history` | Everything tracked, newest first (daily logs + pH readings per day). | "My logs" card on Insights |
| Reminder Settings | `reminder_settings` | Master toggle + per-reminder toggles for the local reminder engine. | Profile → Reminders [`ui/profile/ProfileScreen.kt:203`] |
| Pregnancy | `pregnancy` | Placeholder pregnancy mode (static copy, "—" trimester). | Profile focus toggle only [`ui/profile/ProfileScreen.kt:149-152`] |
| Profile | `profile` | Account, focus toggle, Reminders row, tracking rows, theme selector, privacy link, log out, delete account. | Bottom tab; avatar menu on Home |
| Auth | `auth` | Email/password sign-in / sign-up, "Continue with Google". | Splash; Home banner; Readiness Summary; Waitlist |
| Invite | `invite/{code}` | Accepts a partner invite from a link (feature gated off). | Deep link only |
| Clients | `clients` | Admin client list. **Unreachable** — the Profile row is gated off. | None |

**Deep links.** In-app / notification navigation (nav graph only): `genesyx://{home,track,nutrition,insights,log}`, `genesyx://tracker/{hydration,ph}`. Externally launchable (manifest intent-filters): only the invite pair — `genesyx://invite/{code}` and `https://genesis-cycle-guide.lovable.app/invite/{code}` [`AndroidManifest.xml:37-53`].

---

## 2. TRACKERS — what the app records

| Tracker | What she records | Constraints / model | Logged from | Detail screen |
|---|---|---|---|---|
| Cycle | Last period date, cycle length, period length | Phases, fertile window and ovulation are **derived estimates** (`domain/cycle/CycleEngine.kt`), nothing per-day stored | Cycle dialog (Home / Track / Cycle Detail) | `tracker/cycle` |
| Mood | One of the mood options | Facet of the daily log | Log screen | — |
| Energy | Segmented level | Facet of the daily log | Log screen | — |
| Symptoms | Preset chips + free-text custom symptoms | Facet of the daily log | Log screen | `tracker/symptoms` |
| Sleep | Hours + minutes | Facet of the daily log | Log screen; Sleep Detail | `tracker/sleep` |
| Hydration | Water in ml | Scored against the **user-set goal** — range 1000–5000 ml, default 2400, step 200 [`domain/streaks/StreakEngine.kt:62-70`], clamped at the single writer [`data/PreferencesRepository.kt:43-62`] | Log screen; Hydration Detail (canonical editor); Nutrition stepper | `tracker/hydration` |
| Supplements | Folate, Omega-3, Vitamin D, Zinc | One vocabulary across log, Nutrition and Insights | Log screen; Nutrition Detail | `tracker/nutrition` |
| Vaginal pH | A pH value | Input **3.5–7.0**, step 0.1, default 4.2; two bands — Healthy **3.8–4.5**, Elevated **> 4.5** [`domain/ph/PhStatus.kt:23-35`] — **PROVISIONAL, pending client sign-off**. Out-of-range writes are rejected in the data layer [`data/PhRepository.kt:65-69`]. Pre-migration readings persist as **"urine (legacy)"** (`PhCopy.LEGACY_MARKER`): excluded from vaginal insights, muted/hollow on the chart. | pH dialog (Track / pH Detail) | `tracker/ph` |
| Notes | Free text | Facet of the daily log | Log screen | — |

---

## 3. FEATURES

| Feature | Data source | Flagged? | Status |
|---|---|---|---|
| Cycle phase engine (derived, nothing stored) | Computed from settings [`domain/cycle/CycleEngine.kt`] | No | live |
| Cycle settings | Room is truth, written through to Supabase [`data/CycleRepository.kt:44-54`] | No | live |
| Daily log (mood, energy, symptoms, sleep, water, supplements, notes) | Room is truth, written through with an **offline queue** [`data/DailyLogRepository.kt:76-98`] | No | live |
| Daily-log offline sync queue | `PENDING_UPSERT` + `DailyLogSyncWorker` with WorkManager backoff; refresh skips unsynced rows | No | live |
| Log back-guard | Confirm-discard dialog + `BackHandler` [`ui/screens/LogScreen.kt:120-121,264-288`] | No | live |
| Hydration tracking, user-set goal, streaks, week buckets | Daily-log facet + DataStore goal; one week-bucketing impl (`domain/time/WeekBuckets`) | No | live |
| Intraday hydration coaching | `HydrationCoach` on Home + Nutrition | No | live |
| Vaginal pH tracking + **sync** | Room is truth, syncs to Supabase with a retry queue [`data/PhRepository.kt`, `data/sync/PhSyncWorker.kt`]; card copy discloses the sync [`ui/components/PhTrackerCard.kt:126-130`] | `PH_TRACKING` **on** | live |
| **Local reminders** | Strictly on-device — WorkManager, no FCM/token. 6 `ReminderKind`s (daily log, missed-log, hydration, weekly insights, re-engagement; nutrition reserved) across 4 channels; self-rescheduling chain; pure/tested `ReminderPolicy` (quiet-hours wrap, already-logged-today, daily cap, re-engagement pacing); `BootReceiver`; `cancelAll()` on sign-out/delete. The Profile master toggle is genuinely consumed [`notifications/ReminderPolicy.kt:72`]. Default-enabled: daily log, missed-log, weekly insights, re-engagement. | `PUSH_NOTIFICATIONS` **on** | live |
| Insights — **all real data** | `InsightsViewModel` combines the real repos; every card has a genuine empty state. Cards: Log-history entry, Weekly summary, Consistency, Vaginal pH (flag-gated), Hydration, Nutrition consistency, Supplement adherence, Sleep, Cycle regularity, Symptom patterns, Ovulation [`ui/insights/InsightsScreen.kt:91-131`] | No | live |
| Streak engine v2 + cross-platform contract | `domain/tracking/tracking_test_vectors.json` — 16 vectors mirrored into the iOS repo; `TrackingVectorTest` runs them against the real engine | No | live |
| Learn (10 bundled articles, search, filters, related, share, CTAs) | Compiled into the app [`domain/content/LearnContent.kt`] | No | live |
| Email/password auth + Google sign-in | Supabase (local fallback when unconfigured) | Google self-gates on client ID | live |
| Session persistence + launch routing | Stored session decides the first screen | No | live |
| Account deletion (remote-first) | Server delete must succeed before local wipe [`auth/AuthRepository.kt`] | No | live |
| Theme (system/light/dark), display name, focus mode | Persisted preferences | No | live |
| Waitlist email capture | Stored server-side (`waitlist_emails`, insert-only RLS) before the success screen shows; offline shows an error [`ui/onboarding/WaitlistViewModel.kt`] | No | live (1.3.1) |
| Password change | Re-verifies the current password, then updates via Supabase Auth; loading/error/success in the dialog [`auth/SupabaseAuthService.kt`] | No | live (1.3.1) |
| Guest pH adoption on sign-in | Guest readings are reassigned to the account and queued for push [`data/PhRepository.kt` `adoptGuestReadings`] | Follows `PH_TRACKING` | live (1.3.1) |
| Onboarding quiz answers | Held in memory, discarded on completion [`ui/onboarding/OnboardingQuizScreen.kt:53`] | No | dormant |
| Pregnancy mode | Static placeholder copy | No | dormant |
| Partner invites and linking | Writes a Room row, sends no email, links a placeholder | `PARTNER_INVITES` **off** | dormant |
| Client management + demo seeding | Remote source is a stub | `ADMIN_CLIENTS` **off** | dormant |

---

## 4. USER JOURNEYS

### A. First run to a working account

Splash → Intro (three benefits) → five-question quiz (answers required per step; two "Did you know?"
modals; back preserves answers) → Readiness Summary → "Unlock My Free Guide" (Waitlist) or
"Register / Login". The dashboard is reachable only through an account; signing in clears the
onboarding stack and lands on Home. Cycle settings, daily logs and pH readings pull from the server
in the background. Later cold starts go straight to Home while the session persists.

### B. Setting up a cycle and logging a day

1. Before setup, Home shows the first-run setup card (last-period date picker + "Start tracking").
2. Saving cycle settings stores locally and pushes to the server; Home fills with the cycle hero,
   three metrics and Today's focus; Track paints the phase-coloured month calendar.
3. "Log today" opens the Log screen pre-filled with anything already recorded for today. Everything
   is optional.
4. **Saving works online or offline.** An offline save lands in Room as `PENDING_UPSERT` and a
   WorkManager job retries with backoff until it reaches the server ("push failed — queued for
   retry" → `Worker result SUCCESS`). Verified on-device 2026-07-13.
5. Back with unsaved edits asks "Discard your changes?" instead of silently dropping them.
6. Home's hydration ring, streak and week dots update; the streak counts consecutive days with any
   water logged; "days on goal" counts days the user-set goal was actually hit.

### C. Reading the Learn section

Ten articles: one featured hero, five category chips, auto-focused search, related-article links
that replace rather than stack, share (title + excerpt + site root), medical disclaimer on six of
ten, CTAs that jump into Log/Track/Nutrition/Insights or another article.

### D. Tracking vaginal pH

*(Relabelled from urine pH in the post-v1.2 migration. Range/thresholds PROVISIONAL, pending client
sign-off.)*

1. Entry points: Track "Your Trackers" → Vaginal pH detail; Home pH-nudge card (deep link); Insights
   pH card.
2. "Log pH" records a value; out-of-range (outside 3.5–7.0) is rejected and never stored.
3. Values round to one decimal and save to the device immediately; a signed-in user's reading also
   pushes to Supabase, with a queued retry on failure. Guests stay entirely on-device.
4. Deleting a reading tombstones it so the deletion syncs; the server hard-deletes on account
   erasure.
5. Pre-migration readings surface as "urine (legacy)": excluded from vaginal insight/status
   computation, muted/hollow on the chart, and the Home nudge asks for a fresh vaginal reading
   rather than presenting the old value as current.
6. Insights shows current value, status, trend, 7- and 30-day averages, and neutral written copy
   (`domain/ph/PhCopy.kt` — no condition names, no dietary advice, GP/pharmacist signposting;
   enforced by banned-phrase tests).

### E. Deleting an account

Profile → "Delete account" (signed-in only) → confirmation → server deletes the account **first**;
only then is local data wiped and the session cleared; failure keeps the user signed in with the
error shown. Success clears the back stack to Splash. Reminders are cancelled on sign-out/delete.

---

## 5. DATA & SYNC

**Stored on the device.** Room v5 [`data/local/GenesyxDatabase.kt:34`], seven tables scoped per
user: `cycle_settings`, `daily_logs`, `ph_readings`, `profiles`, `clients`, `partner_invites`,
`partner_links`. DataStore holds the session and preferences: theme, reminders master + per-kind
toggles, focus mode, hydration goal, Learn-hint and pH-notice dismissals.

**The device is the source of truth.** Every write lands locally first; the server is a mirror.

**What syncs to Supabase** (only when built with server credentials; otherwise stub no-ops):
profiles, cycle settings, daily logs, **and pH readings**. Client records and partner rows never
leave the device. Reminders add no server surface — strictly local.

**Offline behaviour by data type:**

- *Daily logs* — offline writes **queue** (`PENDING_UPSERT`, `DailyLogSyncWorker`, WorkManager
  backoff). `DailyLogRepository.refresh` skips rows with unsynced local changes — the rule that
  makes offline writes safe, covered by `a_pull_must_not_overwrite_an_unsynced_local_edit`.
- *pH readings* — same shape: pending status + `PhSyncWorker` retry; pull-merge by record id
  prefers whichever copy was updated last and never overwrites unsynced local edits.
- *Cycle settings* — the local write persists and drives the UI, but a failed remote push is only
  logged; **nothing retries it**, and a later refresh can overwrite a never-pushed offline edit
  with the server's older copy [`data/CycleRepository.kt:44-66`]. The one remaining offline gap.

**Guests** (`LOCAL_USER_ID`): daily-log and pH writes are marked `SYNCED` without a push (no server
target under RLS). Since 1.3.1, guest **pH readings** are adopted into the account on sign-in
(reassigned + queued for push); guest **daily logs** still are not — that migration remains open.

**Deletion.** Account deletion is remote-first. pH deletions tombstone locally so they propagate;
the server-side `delete_current_user` hard-deletes `ph_readings` rows (GDPR erase removes rows, not
tombstones).

---

## 6. GAPS & DORMANT

**Gated off by a compile-time flag — code present, unreachable.**

| What | Flag | Why it is off |
|---|---|---|
| Client management screen | `ADMIN_CLIENTS` | Admin tool, not a user feature. Route exists; nothing links to it. |
| Partner invites and linking | `PARTNER_INVITES` | Sends no email, links no real account. The invite deep links remain registered; accepting one links a placeholder partner. |

**Reachable, but not doing what the interface implies.**

- **Quiz answers are never used** — the readiness summary is identical for every answer set.
- **Pregnancy mode is a placeholder** — static copy, "—" trimester, no due-date entry.
- **The onboarding-complete preference has no callers** — persisted plumbing exists, nothing writes
  or reads it.
- **Guest daily logs don't migrate on sign-in** (guest pH readings do, since 1.3.1 — the daily-log
  equivalent remains open).
- **An offline cycle-settings change can still be lost server-side** — see §5; unlike daily logs
  there is no pending status or retry.
- **"PREMIUM" is a label, not a tier** — no subscription, billing, or entitlement code exists.

**Environment-dependent.**

- Without compiled-in server credentials the app runs fully offline against stubs, and auth falls
  back to a local service that does not verify passwords.
- Google sign-in shows unconditionally but reports "isn't configured" without a compiled-in client
  ID.

---

## 7. RELEASE HISTORY (condensed — details in `CHANGELOG.md`)

| Version | Highlights |
|---|---|
| v1.0 (code 6–7) | Core app: cycle engine, daily log, Track calendar, Nutrition, pH (urine, flag-on), Learn (10 articles), auth, account deletion. Uploaded to Play Internal testing Jul 2026. |
| v1.1 (PR #9) | Daily-log offline queue; streak engine v2 + cross-platform vector contract; user-set hydration goal; log back-guard; auth hardening. |
| v1.2 (PR #10) | Local reminders (`PUSH_NOTIFICATIONS` on); Track "Your Trackers" + six detail screens; Home hydration-ring/pH-nudge cards + deep links; all-real-data Insights + Weekly Summary; supplement adherence; intraday hydration coaching. |
| 1.2.1 (code 10) | Corrective versionCode bump after the code-9 Play collision. Archived, never uploaded — **superseded, do not upload**. |
| 1.3.0 (code 11) | Android 16 target (SDK 36); Vaginal pH migration (two-band model, Room v5, legacy "urine (legacy)" handling, Supabase `measurement_type`). Uploaded to Play **Internal testing** 2026-07-27; Health apps declaration re-submitted. |
| **1.3.1 (code 12)** | **Current.** Change password works (Supabase Auth + re-verification); guest pH readings adopted on sign-in; waitlist email stored server-side (`waitlist_emails`, insert-only RLS — table must exist in Supabase before rollout). |
