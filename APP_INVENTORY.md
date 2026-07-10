# Genesyx — Product Inventory

Derived from the codebase at `release/learn-v1` (`ac59b3a`), versionCode 7 / versionName 1.0.0 [`app/build.gradle.kts:38-39`].

Nineteen navigable destinations; six appear as bottom tabs [`ui/navigation/Screen.kt:46`]. Four compile-time feature gates control what a user can reach [`core/FeatureFlags.kt`].

---

## 1. SCREENS

| Screen | Route | Purpose | Key UI elements | Entry points |
|---|---|---|---|---|
| Splash | `splash` | Signed-out landing; brand statement and the two ways in. | Brand lockup, floating egg artwork, "Start Your Personalised Quiz", "Sign in" [`ui/onboarding/SplashScreen.kt:62-140`] | Launch destination when no session is stored [`ui/AppViewModel.kt:32`]; after account deletion [`ui/profile/ProfileScreen.kt:97`] |
| Onboarding Intro | `onboarding_intro` | Explains the three things the app does before asking anything. | Three benefit cards (cycle, nutrition, insights), "Continue" [`ui/onboarding/OnboardingIntroScreen.kt:57-126`] | "Start Your Personalised Quiz" on Splash [`ui/navigation/GenesyxNavGraph.kt:46`] |
| Onboarding Quiz | `onboarding_quiz` | Five questions establishing stage, cycle regularity, supplements, sex preference, support need. | Progress bar, step counter, option pills, "Did you know?" modal, back arrow [`ui/onboarding/OnboardingQuizScreen.kt:47-165`] | "Continue" on Intro [`ui/navigation/GenesyxNavGraph.kt:55`] |
| Readiness Summary | `readiness_summary` | Reassurance screen closing the quiz; routes to guide or account. | Three insight rows, "Suggested next steps" card, "Unlock My Free Guide", "Register / Login to continue" [`ui/onboarding/ReadinessSummaryScreen.kt:51-159`] | Answering the final quiz question [`ui/navigation/GenesyxNavGraph.kt:62`] |
| Waitlist | `waitlist` | Collects an email in exchange for a nutrition guide. | eBook hero, email field with validation, "Join the Waiting List", success state [`ui/onboarding/WaitlistScreen.kt:51-172`] | "Unlock My Free Guide" on Readiness Summary [`ui/navigation/GenesyxNavGraph.kt:71`] |
| Home | `home` | Daily dashboard: where you are in your cycle and what to do today. | Greeting + avatar menu, cycle hero card, "Today's focus", hydration and streak tiles, "Log today", pregnancy link [`ui/home/HomeScreen.kt:103-301`] | Bottom tab; launch destination when signed in [`ui/AppViewModel.kt:32`]; after sign-in [`ui/navigation/GenesyxNavGraph.kt:120`] |
| Track | `track` | Month calendar of cycle phases, plus today's phase. | Month grid with phase colouring, legend, day-detail dialog, current-phase card, "Add to today's log", pH section [`ui/track/TrackScreen.kt:91-282`] | Bottom tab; pH insight card on Insights [`ui/insights/InsightsScreen.kt:83`] |
| Nutrition | `nutrition` | Phase-aware food focus, hydration, supplements, article entry. | Hydration stepper card, focus-food accordion, supplement plan card + dialog, article list [`ui/nutrition/NutritionScreen.kt:63-156`] | Bottom tab |
| Insights | `insights` | Trend surfaces over cycle, symptoms, nutrition, pH. | "My logs" card, pH summary, cycle-regularity bars, symptom heatmap, nutrition bars [`ui/insights/InsightsScreen.kt:55-119`] | Bottom tab |
| Learn | `learn` | Landing for ten bundled articles. | Search icon, dismissible intro hint, category filter chips, featured hero card, article rows [`ui/learn/LearnScreen.kt:65-155`] | Bottom tab; "See all articles" on Nutrition [`ui/nutrition/NutritionScreen.kt:120`] |
| Learn Search | `learn/search` | Free-text search across the article set. | Auto-focused search field, clear button, results list, two empty states [`ui/learn/LearnSearchScreen.kt:46-112`] | Search icon on Learn [`ui/learn/LearnScreen.kt:101`] |
| Article Detail | `learn/article/{slug}` | Reads one article. | Hero image, heading/paragraph/bullet/callout blocks, optional CTA button, medical disclaimer, related articles, share [`ui/learn/ArticleDetailScreen.kt:49-158`] | Any article row on Learn, Learn Search, Nutrition, or the Related list [`ui/learn/LearnScreen.kt:149`, `ui/nutrition/NutritionScreen.kt:119`] |
| Log | `log` | Records how today went. | Mood picker, energy segments, symptom chips + custom add, sleep/water/supplement dialogs, notes, "Save log" [`ui/screens/LogScreen.kt:77-259`] | "Log today" on Home; "Add to today's log" on Track; article CTAs [`ui/home/HomeScreen.kt:282`, `ui/track/TrackScreen.kt:254`] |
| Log History | `log_history` | Everything tracked, newest first. | Per-day cards combining daily log and pH readings, empty state [`ui/history/LogHistoryScreen.kt:59-113`] | "My logs" card on Insights [`ui/insights/InsightsScreen.kt:77`] |
| Pregnancy | `pregnancy` | Preview of an unbuilt pregnancy mode. | Transition screen with two feature cards; after switching, a placeholder pregnancy home [`ui/screens/PregnancyScreen.kt:65-219`] | "Preview pregnancy pathway" on Home; "Pregnancy" focus segment on Profile [`ui/home/HomeScreen.kt:290`, `ui/profile/ProfileScreen.kt:148`] |
| Profile | `profile` | Account, preferences, theme, deletion. | User card, focus toggle, account rows, tracking rows, theme selector, privacy link, log out, delete account [`ui/profile/ProfileScreen.kt:79-329`] | Bottom tab; avatar menu on Home [`ui/home/HomeScreen.kt:171`] |
| Auth | `auth` | Sign in or create an account. | Email/password fields, mode toggle, error text, "Continue with Google" [`ui/screens/AuthScreen.kt:155-289`] | "Sign in" on Splash; Home sign-in banner; Readiness Summary; Waitlist; gated Profile rows [`ui/navigation/GenesyxNavGraph.kt:50,73,80`] |
| Invite | `invite/{code}` | Accepts a partner invite from a link. | Signed-out prompt, invalid-code state, "Accept invite" [`ui/screens/InviteScreen.kt:48-104`] | Deep link `genesyx://invite/{code}` or the equivalent https link [`ui/navigation/GenesyxNavGraph.kt:132-135`] |
| Clients | `clients` | Admin list of client records. | Add-client dialog, client cards with delete, "Seed 100 demo clients" [`ui/clients/ClientsScreen.kt:60-106`] | None — the Profile row that opened it is gated off [`ui/profile/ProfileScreen.kt:176`] |

The bottom bar hides on every screen except the six tabs [`ui/navigation/Screen.kt:49-65`].

---

## 2. FEATURES

| Feature | Screens involved | Data source | Feature-flagged? | Status |
|---|---|---|---|---|
| Cycle phase engine | Home, Track, Nutrition | Derived — computed from cycle settings, nothing stored [`domain/cycle/CycleEngine.kt:32-95`] | No | live |
| Cycle settings (last period, cycle length, period length) | Home, Track | Both — Room is truth, written through to Supabase [`data/CycleRepository.kt:44-66`] | No | live |
| Daily log (mood, energy, symptoms, sleep, water, supplements, notes) | Log, Home, Nutrition, Insights, Log History | Both — Room is truth, written through to Supabase [`data/DailyLogRepository.kt:49-96`] | No | live |
| Hydration tracking + streak | Home, Nutrition, Log | Both — a facet of the daily log [`data/DailyLogRepository.kt:62-82`] | No | live |
| Log history | Insights → Log History | Both — merges daily logs and pH readings by day [`ui/history/LogHistoryViewModel.kt:25-29`] | No | live |
| Urine pH tracking | Track, Nutrition, Insights, Log History | Both — Room is truth, syncs to Supabase with a retry queue [`data/PhRepository.kt:61-148`] | Yes — `PH_TRACKING`, **on** [`core/FeatureFlags.kt:11`] | live |
| pH background sync | (none — invisible) | Both — drains pending writes when the network returns [`data/sync/PhSyncWorker.kt:22-26`] | Follows `PH_TRACKING` | live |
| Learn articles (10, bundled) | Learn, Learn Search, Article Detail, Nutrition | Neither — compiled into the app [`domain/content/LearnContent.kt:86`] | No | live |
| Article search | Learn Search | Neither — matches title, excerpt, tags in memory [`domain/content/LearnContent.kt:76-84`] | No | live |
| Article category filter | Learn | Neither — transient UI state [`ui/learn/LearnScreen.kt:71-73`] | No | live |
| Related articles | Article Detail | Neither — hand-authored id lists [`domain/content/LearnContent.kt:72-73`] | No | live |
| Article share | Article Detail | Neither — plain text to the system share sheet [`ui/learn/ArticleDetailScreen.kt:243-251`] | No | live |
| Medical disclaimer | Article Detail | Neither — shown on 6 of 10 articles [`ui/learn/ArticleDetailScreen.kt:121-130`] | No | live |
| Email/password auth | Auth | Supabase, with a local fallback when unconfigured [`di/NetworkModule.kt:60-64`] | No | live |
| Google sign-in | Auth | Supabase, via a Google ID token [`ui/screens/AuthScreen.kt:86-112`] | Self-gating — hidden effect if no client ID is compiled in [`ui/screens/AuthScreen.kt:79`] | live |
| Session persistence + launch routing | Splash, Home | Local — stored session decides the first screen [`ui/AppViewModel.kt:29-34`] | No | live |
| Account deletion | Profile | Both — deletes the remote account, then wipes local data [`auth/AuthRepository.kt:59-71`] | No | live |
| Display-name editing | Profile | Both — persists locally and to the profile record [`data/ProfileRepository.kt:52`] | No | live |
| Theme (system/light/dark) | Profile, all screens | Local — persisted preference [`data/PreferencesRepository.kt:26,37`] | No | live |
| Focus mode (prep/pregnancy) | Profile, Pregnancy | Local — persisted preference [`data/PreferencesRepository.kt:30,39`] | No | live |
| Learn intro hint dismissal | Learn | Local — persisted preference [`ui/learn/LearnViewModel.kt:18-20`] | No | live |
| Waitlist email capture | Waitlist | Neither — validated, then discarded [`ui/onboarding/WaitlistScreen.kt:166`] | No | dormant |
| Onboarding quiz answers | Onboarding Quiz | Neither — held in memory, discarded on completion [`ui/onboarding/OnboardingQuizScreen.kt:53`] | No | dormant |
| Pregnancy mode | Pregnancy | Neither — static placeholder copy [`ui/screens/PregnancyScreen.kt:150-219`] | No | dormant |
| Cycle regularity / symptom / nutrition charts | Insights | Neither — fixed sample values [`ui/insights/InsightsScreen.kt:51-52,306`] | No | dormant |
| Password change | Profile | Neither — validates, then closes [`ui/profile/ProfileScreen.kt:519-525`] | No | dormant |
| Partner invites and linking | Profile, Invite | Local only — writes a Room row, sends no email, links a placeholder [`data/PartnerRepository.kt:49-69`] | Yes — `PARTNER_INVITES`, **off** [`core/FeatureFlags.kt:27`] | dormant |
| Push notifications toggle | Profile | Local only — nothing consumes the stored value [`core/FeatureFlags.kt:29-36`] | Yes — `PUSH_NOTIFICATIONS`, **off** [`core/FeatureFlags.kt:36`] | dormant |
| Client management + demo seeding | Clients | Local only — the remote source is a stub [`di/BindingsModule.kt:32`] | Yes — `ADMIN_CLIENTS`, **off** [`core/FeatureFlags.kt:18`] | dormant |

---

## 3. USER JOURNEYS

### A. First run to a working account

1. App opens on Splash because no session is stored [`ui/AppViewModel.kt:32`].
2. Tapping "Start Your Personalised Quiz" opens the Intro, which names the three benefits [`ui/onboarding/OnboardingIntroScreen.kt:57-64`].
3. "Continue" starts the five-question quiz. Each question needs an answer before "Continue" enables [`ui/onboarding/OnboardingQuizScreen.kt:126`].
4. Two questions raise a "Did you know?" modal that must be dismissed before advancing [`ui/onboarding/OnboardingQuizScreen.kt:63-66`].
5. The back arrow and the system back gesture both step to the previous question, preserving answers; from the first question they exit to the Intro [`ui/onboarding/OnboardingQuizScreen.kt:70`].
6. The final answer opens the Readiness Summary — three insights and three suggested next steps [`ui/onboarding/ReadinessSummaryScreen.kt:53-62`].
7. Two exits: "Unlock My Free Guide" opens the Waitlist; "Register / Login to continue" opens Auth [`ui/navigation/GenesyxNavGraph.kt:71-73`].
8. The Waitlist validates an email, shows a confirmation, then also routes to Auth. The dashboard is reachable only through an account [`ui/navigation/GenesyxNavGraph.kt:80`].
9. On Auth, creating an account or signing in clears the entire onboarding stack and lands on Home; back cannot return to the gate [`ui/navigation/GenesyxNavGraph.kt:120-122`].
10. Cycle settings, daily logs, and pH readings are pulled from the server in the background [`auth/AuthRepository.kt:80-85`].
11. Every later cold start goes straight to Home while the session persists [`ui/AppViewModel.kt:32`].

### B. Setting up a cycle and logging a day

1. Before setup, Home reads "Set up your cycle" and Today's focus is empty [`ui/home/HomeViewModel.kt:32-33`].
2. Tapping the cycle card — or the avatar menu's "Cycle setup", or the edit button on Track — opens the cycle dialog [`ui/home/HomeScreen.kt:212`, `ui/track/TrackScreen.kt:139`].
3. Saving a last-period date, cycle length, and period length stores the settings locally and pushes them to the server [`data/CycleRepository.kt:44-54`].
4. Home immediately shows the cycle day, phase name, a headline, and phase tags; Today's focus fills with phase-appropriate foods [`ui/home/HomeViewModel.kt:79-90`].
5. Track paints a month calendar colour-coded for period, fertile window, ovulation, and luteal days, with today outlined [`ui/track/TrackScreen.kt:304-316`].
6. Tapping any calendar day opens a detail dialog; future days read as predictions [`ui/track/TrackScreen.kt:405-410`].
7. "Log today" opens the Log screen, pre-filled with anything already recorded for today [`ui/screens/LogScreen.kt:79`].
8. Mood, energy, symptoms (including custom ones), sleep, water, supplements, and a note are all optional [`ui/screens/LogScreen.kt:107-224`].
9. Tapping "Save log" while offline does **not** save or close — it shows "You're offline — reconnect to save your log." [`ui/screens/LogScreen.kt:237-244`].
10. Online, the log saves locally, pushes to the server, and the screen closes [`data/DailyLogRepository.kt:49-59`].
11. Home's hydration tile and streak update; the streak counts consecutive days back from today with any water logged [`data/DailyLogRepository.kt:73-82`].

### C. Reading the Learn section

1. The Learn tab lists ten articles: one featured hero, the rest as rows [`ui/learn/LearnScreen.kt:75-152`].
2. On first visit only, a dismissible card points at "Your first week with Genesyx"; dismissal is remembered [`ui/learn/LearnScreen.kt:117-125`].
3. Five category chips — Getting started, Tracking, Nutrition, Insights, Wellness — filter the list. Inside a filter the featured hero becomes an ordinary row [`ui/learn/LearnScreen.kt:75`].
4. The search icon opens a screen whose field is focused immediately; matching runs over title, excerpt, and tags [`domain/content/LearnContent.kt:79-83`].
5. Opening an article shows a hero image, then headings, paragraphs, bullet lists, and callouts [`ui/learn/ArticleDetailScreen.kt:174-225`].
6. Six of the ten articles close with a medical disclaimer above the footer [`ui/learn/ArticleDetailScreen.kt:121-130`].
7. Some articles end with a call-to-action that jumps into Log, Track, Nutrition, Insights, or another article. A CTA into a tab reuses that tab rather than stacking a second copy [`ui/learn/ArticleDetailScreen.kt:110-118`].
8. Related articles replace the current one rather than stacking, so three taps through Related need one back press, not three [`ui/learn/ArticleDetailScreen.kt:146-150`].
9. Share sends the title, excerpt, and the site root — not a per-article link, which does not exist [`core/AppLinks.kt`].

### D. Tracking urine pH

1. A pH section sits on Track and Nutrition; a summary card sits on Insights [`ui/track/TrackScreen.kt:261-264`, `ui/nutrition/NutritionScreen.kt:101-104`].
2. "Log pH" opens a dialog to record a value; readings outside 4.5–9.0 are rejected and never stored [`data/PhRepository.kt:64-68`, `domain/ph/PhStatus.kt:15-16`].
3. Values round to one decimal place, then save to the device immediately [`data/PhRepository.kt:72-80`].
4. For a signed-in user the reading also pushes to the server; if that fails the reading stays queued and a background job retries when the network returns [`data/PhRepository.kt:99-108`].
5. For a signed-out user nothing is queued and nothing is pushed [`data/PhRepository.kt:76`].
6. Deleting a reading marks it deleted rather than removing it, so the deletion syncs safely [`data/PhRepository.kt:86-96`].
7. Insights shows the current value with a status colour, a trend marker, 7- and 30-day averages, and a written observation; with no readings it invites a first entry [`ui/insights/InsightsScreen.kt:185-222`].
8. Tapping the pH card on Insights opens the tracker on Track [`ui/insights/InsightsScreen.kt:82-88`].
9. Readings appear alongside daily logs, timestamped, in Log History [`ui/history/LogHistoryScreen.kt:143-163`].

### E. Deleting an account

1. "Delete account" appears on Profile only while signed in [`ui/profile/ProfileScreen.kt:233`].
2. Tapping it raises a confirmation: "This will permanently delete your account and all your data. This cannot be undone." [`ui/profile/ProfileScreen.kt:302-305`].
3. Confirming disables both buttons and shows "Deleting…" while the request is in flight [`ui/profile/ProfileScreen.kt:314-317`].
4. The server deletes the account; only then is every local table wiped and the session cleared [`auth/AuthRepository.kt:60-65`].
5. A failure keeps the user signed in and shows the error inside the dialog [`ui/profile/ProfileScreen.kt:307-310`].
6. On success the entire back stack is cleared and the app returns to Splash [`ui/profile/ProfileScreen.kt:95-99`].

---

## 4. DATA & SYNC

**Stored on the device.** A local database holds cycle settings, daily logs, pH readings, profile records, client records, partner invites, and partner links — seven tables, scoped per user so a guest's rows stay separate from an account's [`data/local/GenesyxDatabase.kt:25-44`]. A separate preference store holds the session (signed-in flag, user id, email, display name) and the app preferences: theme, push toggle, focus mode, onboarding-complete flag, and whether the Learn intro was dismissed [`data/SessionRepository.kt:34-41`, `data/PreferencesRepository.kt:26-35`].

**The device is the source of truth.** Every write lands locally first and the UI updates from local state; the server is a mirror [`data/CycleRepository.kt:44-54`].

**What syncs.** Profile records, cycle settings, daily logs, and pH readings sync to Supabase — but only when the app was built with server credentials. Without them, every remote call is replaced by a no-op stub and the app runs entirely on-device [`di/NetworkModule.kt:64-82`]. Client records never sync; their remote source is always a stub [`di/BindingsModule.kt:32`]. Partner invites and links never leave the device [`data/PartnerRepository.kt:49-69`].

**When it syncs.** Writes push immediately after the local save. Reads pull once, in the background, right after sign-in — profile, then cycle, then daily logs, then pH — so sign-in never blocks on a slow table [`auth/AuthRepository.kt:80-85`].

**Offline behaviour differs by data type, and this is the sharpest edge in the product.**

- *Daily logs* refuse to save while offline. The Save button checks connectivity and, if there is none, blocks the save and shows an error rather than storing something the server will later overwrite [`ui/screens/LogScreen.kt:237-244`, `ui/screens/LogViewModel.kt:24-33`].
- *pH readings* do the opposite: an offline write is stored, marked pending, and a background job retries it with backoff once the network returns [`data/PhRepository.kt:99-108`, `data/sync/PhSyncScheduler.kt:24-27`].
- *Cycle settings* take a third path: the local write succeeds, the remote push fails quietly and is logged, and nothing retries it [`data/CycleRepository.kt:48-52`].

**Conflict resolution.** Only pH has any. A pull merges by record id, prefers whichever copy was updated last, and never overwrites a row with unsynced local edits [`data/PhRepository.kt:127-148`]. Cycle settings and daily logs let the server copy win on the next read [`data/DailyLogRepository.kt:85-96`].

**Deletion.** Account deletion is remote-first: the server call must succeed before any local data is cleared, so a failed delete leaves the user intact and signed in [`auth/AuthRepository.kt:59-71`]. pH deletions are tombstoned rather than removed, so they propagate [`data/PhRepository.kt:86-96`].

---

## 5. RECENT ADDITIONS

Commits from 2026-07-07 to 2026-07-09 (`git log --since="3 days ago"`). The Learn section is the headline: it did not exist three days ago.

| Item | What it does | Where it lives |
|---|---|---|
| **Learn section** (`659de4d`, Jul 9) | Ten illustrated articles with search, category filters, related-article links, share, and per-article calls-to-action. Took Profile's bottom-tab slot, making six tabs. | [`ui/learn/LearnScreen.kt`, `ui/learn/LearnSearchScreen.kt`, `ui/learn/ArticleDetailScreen.kt`, `ui/learn/LearnViewModel.kt`, `domain/content/LearnContent.kt`] |
| Article content set | Ten articles across five categories, one featured, six carrying a medical disclaimer: `getting-started-first-week` (featured, 5 min), `why-logging-beats-remembering` (4 min), `what-to-log` (4 min), `hydration-basics` (3 min), `eating-with-your-cycle` (6 min), `gentle-guide-supplements` (6 min), `what-insights-mean` (5 min), `reading-your-trends` (5 min), `small-habits-that-hold` (4 min), `using-what-you-learn` (4 min). | [`domain/content/LearnContent.kt:86-505`] |
| Ten article hero images | One illustration per article, shown on the landing hero, list rows, and article headers. Missing art falls back to a category-tinted gradient. | [`res/drawable-nodpi/learn_hero_*.jpg`, `ui/learn/LearnScreen.kt:230-247`] |
| Six-tab bottom navigation | Learn joined Home, Track, Nutrition, Insights, and Profile — one past the platform's recommended maximum, a deliberate product call. | [`ui/navigation/Screen.kt:41-46`, `ui/components/GenesyxBottomNav.kt:30-35`] |
| Learn entry point on Nutrition | The old "Learn more" article list now opens real articles and gained a "See all articles" link into the Learn tab. | [`ui/nutrition/NutritionScreen.kt:333-361`] |
| Persisted Learn intro hint | A one-time dismissible card on the Learn landing; dismissal survives restart. | [`data/PreferencesRepository.kt:34,41`, `ui/learn/LearnViewModel.kt`] |
| Share link target | Article shares point at the site root, not a per-article URL, because no per-article page is confirmed to exist. | [`core/AppLinks.kt`] |
| Learn audit + iOS parity handoff (`e6f279e`, Jul 9) | Documentation only: a verification audit of the Learn feature, an iOS parity handoff, and the article set exported as JSON. | [`docs/LEARN_FEATURE_AUDIT.md`, `docs/IOS_LEARN_PARITY_HANDOFF.md`, `docs/learn/articles.json`] |
| Three content-safety fixes (`ac59b3a`, Jul 9) | Behaviour unchanged; the guards around content changed. The banned-phrase scan now covers tags, CTA labels, and every body block — previously it read only title, excerpt, and body, so a banned tag could ship. The disclaimer check now pins an exact set of six article slugs rather than inferring from category. A call-to-action that opens another article now fails at construction if it names no target, rather than crashing a reader. | [`domain/content/LearnContent.kt:31-39`, `ui/learn/ArticleDetailScreen.kt:167-168`, `domain/content/LearnContentTest.kt`] |
| Dashboard gated behind an account (`49d07f2`, Jul 7) | Both onboarding exits — the readiness summary and the waitlist — now route to register/login instead of straight to Home. | [`ui/navigation/GenesyxNavGraph.kt:71-81`] |
| Brand lockup on readiness screen (`49d07f2`, Jul 7) | Replaced a text wordmark with the real logo. | [`ui/onboarding/ReadinessSummaryScreen.kt:74`] |
| Push-notification toggle gated off (`ff8d82c`, Jul 7) | The Profile switch is hidden; it persisted a value nothing consumed. | [`core/FeatureFlags.kt:36`, `ui/profile/ProfileScreen.kt:195-201`] |
| Partner section gated off (`f9966bc`, Jul 7) | Hidden: the invite flow sends no email and links no real accounts. | [`core/FeatureFlags.kt:27`, `ui/profile/ProfileScreen.kt:154-165`] |
| Support email changed (`7c3d13d`, Jul 7) | Now `info@genesyx.co.uk`. | [`core/AppLinks.kt`] |
| Profile tracking rows + splash icon (`04bf55c`, Jul 7) | Three tracking rows now open explanatory dialogs; the system splash uses a branded icon. | [`ui/profile/ProfileScreen.kt:69-76`, `res/drawable/splash_icon.xml`] |
| Brand launcher icon (`f568180`, Jul 7) | Replaced the vector placeholder with the real logo. | [`res/drawable-nodpi/ic_launcher_foreground.png`] |
| Theme follows system + user toggle (`fcdd8d1`, Jul 7) | Theme choice is System, Light, or Dark, and persists. Bumped versionCode to 7. | [`ui/profile/ProfileScreen.kt:204-209`, `app/build.gradle.kts:38`] |

---

## 6. GAPS & DORMANT

**Gated off by a compile-time flag — code present, unreachable.**

| What | Flag | Why it is off |
|---|---|---|
| Client management screen, including "Seed 100 demo clients" | `ADMIN_CLIENTS` [`core/FeatureFlags.kt:18`] | An admin tool, not a user feature. Its route still exists in the navigation graph but nothing links to it [`ui/navigation/GenesyxNavGraph.kt:102-104`]. |
| Partner invites and linking | `PARTNER_INVITES` [`core/FeatureFlags.kt:27`] | Sends no email and links no real account. The Invite deep link `genesyx://invite/{code}` remains registered and reachable, and accepting one silently links a placeholder partner named "Your partner" [`data/PartnerRepository.kt:63-65`]. |
| Push notifications toggle | `PUSH_NOTIFICATIONS` [`core/FeatureFlags.kt:36`] | No notification infrastructure exists. The stored value has no consumer. |

**Reachable, but not doing what the interface implies.**

- **Change password validates and then does nothing.** The dialog checks length and confirmation, then closes without changing any password [`ui/profile/ProfileScreen.kt:519-525`].
- **The waitlist email is discarded.** It is validated, a success screen promises "We'll send your free fertility nutrition guide to {email} shortly", and the address is never stored or transmitted [`ui/onboarding/WaitlistScreen.kt:166`].
- **Three of four Insights charts are fixed sample data.** Cycle regularity and nutrition consistency are hard-coded numbers; the symptom heatmap is generated from a sine wave, not from logged symptoms. Their accompanying prose reads as personalised. Only the pH card reflects real data [`ui/insights/InsightsScreen.kt:51-52,306`].
- **The supplement plan reports "3 of 4 taken today" unconditionally.** The count is static text, unconnected to the supplements recorded in the daily log [`ui/nutrition/NutritionScreen.kt:303`].
- **The Log screen's "Nutrition — On track" tile is inert.** It has an empty tap handler and a fixed value [`ui/screens/LogScreen.kt:213`].
- **Pregnancy mode is a placeholder.** Switching in shows static prenatal copy, a "—" trimester, and no due-date entry despite the copy promising "once you confirm your due date" [`ui/screens/PregnancyScreen.kt:150-208`].
- **The pH card's caption contradicts the code.** It reads "pH entries are stored on this device for now", but the repository pushes readings to the server and queues retries [`ui/components/PhTrackerCard.kt:124`, `data/PhRepository.kt:99-108`]. One of the two is wrong.
- **Quiz answers are never used.** All five are collected into memory and discarded when the summary opens. The readiness summary and Today's focus are identical for every answer set [`ui/onboarding/OnboardingQuizScreen.kt:53`, `ui/onboarding/ReadinessSummaryScreen.kt:53-62`].
- **The onboarding-complete preference is never written or read.** It exists in the preference store with no caller [`data/PreferencesRepository.kt:32`].
- **Guest pH readings never sync.** A signed-out user's readings are marked synced without ever reaching a server, and signing in later does not migrate them [`data/PhRepository.kt:76`].
- **An offline cycle-settings change is silently lost.** The local write succeeds, the remote push fails, a warning is logged, nothing retries, and the next sign-in pulls the server's older copy over it. Unlike the daily log, the user is not told [`data/CycleRepository.kt:48-52`].
- **"PREMIUM" is a label, not a tier.** It appears on Profile for anyone signed in; no subscription, billing, or entitlement code exists [`ui/profile/ProfileScreen.kt:134-138`].

**Environment-dependent.**

- Without compiled-in server credentials the entire app runs offline against stub remote sources, and authentication falls back to a local service that does not verify passwords [`di/NetworkModule.kt:60-64`, `auth/LocalAuthService.kt`].
- Google sign-in shows its button unconditionally but reports "Google sign-in isn't configured" when no client ID was compiled in [`ui/screens/AuthScreen.kt:87-89`].
- The Article Detail screen handles an unknown slug with a dedicated "That article isn't available" screen, but no deep link can currently deliver one — article routes are reachable only from inside the app [`ui/learn/ArticleDetailScreen.kt:254-274`].
