# Genesyx — Native Android (Kotlin) Architecture

> **Source of truth** for the native Kotlin rebuild of Genesyx.
> Combines the `genesyxandroidblueprint.pdf` (target native architecture) with a
> **ground-truth audit of the actual web source** in this repo (the Lovable app).
> Where the two disagree, the **real web source wins** (the screenshots are the real app).
> Update this file as the app evolves — every Claude session should start by reading it.

- **Package / appId:** `com.genesyx.app` (web/Capacitor id is `com.genesyx.fertilityprep`)
- **UI toolkit:** Jetpack Compose + Material 3
- **Architecture:** Single-Activity + Compose Navigation + **MVVM** over **Clean Architecture** (`data` / `domain` / `ui`)
- **DI:** Hilt · **Local:** Room + DataStore · **Remote:** Supabase (see [Backend](#backend--data-layer))
- **SDK:** `compileSdk = 35`, `targetSdk = 35`, `minSdk = 26`, Java 17
- **Versioning:** `versionCode = 1`, `versionName = "1.0.0"`
- **No ads.** AdMob is intentionally excluded.

**App:** *Genesyx — Fertility Prep, Gently Guided.* A premium, mobile-first fertility-prep
& cycle-tracking companion: cycle awareness, nutrition guidance, daily logging, pH tracking,
partner coordination, and personalised insights.

---

## ⚠️ Blueprint vs. Real App — reconciliation

The PDF blueprint was an early approximation. The **real web app** (audited from source) differs in two big ways:

**1. Design tokens are different.** Blueprint guessed `#5B4FCF` + Inter only. Real app uses an
**electric-lavender** primary, **Outfit + Inter** fonts, the **oklch** color space, and a **full dark mode**. → Use the real values below.

**2. The real app has more screens/features than the blueprint.** Blueprint covered: Splash,
OnboardingIntro, OnboardingQuiz, ReadinessSummary, Home, Track, Nutrition, Insights, Profile.
The real app **also** has: a **Conversion/Waitlist** step, **Daily Log** modal, **pH tracker** (chart + history),
**Partner invite/linking**, **Pregnancy mode** (preview/stub), **Cycle Settings editor**, and a **theme toggle**.
→ These must be added to the native plan. See [Feature Parity](#feature-parity-matrix).

---

## Design Tokens — VERIFIED (from `src/styles.css`)

Real app uses **oklch** (authoritative); approximate hex in parentheses. Define in `ui/theme/Color.kt`.

### Light mode
| Token | oklch (authoritative) | ~Hex | Use |
|---|---|---|---|
| `background` / zenith | `0.961 0 0` | ~`#F2F2F2` | Page background |
| `card` | `1 0 0` | `#FFFFFF` | Cards, dialogs, tab bar |
| `foreground` | `0.13 0 0` | ~`#1A1A1A` | Primary text |
| `muted-foreground` | `0.45 0.02 285` | ~`#6E6B78` | Secondary/helper text |
| **`primary`** electric-lavender | `0.435 0.154 285` | ~`#4D4DAA` | Buttons, active tab, links, focus ring |
| `primary-foreground` | `0.99 0 0` | ~`#FCFCFC` | Text on primary |
| `border` / `input` | `0.91 0.005 285` | ~`#E6E5EA` | Dividers, input borders |
| `destructive` | `0.6 0.2 25` | ~`#C8412E` | Delete, errors, sign-out |
| Accent — electric-blue | `0.685 0.110 240` | ~`#57A1CE` | Hydration / chart accent |
| Accent — powder-blue / baby-blue | `0.853 0.069 220` | ~`#8DD2E2` | Fertile-window tint |
| Accent — powder-pink | `0.760 0.110 330` | ~`#DDA4D3` | Period tint |
| Accent — baby-lavender | `0.640 0.115 290` | ~`#8888D3` | Luteal tint / avatar gradient |
| Accent — electric-pink | `0.700 0.158 330` | ~`#C782D8` | Avatar gradient end |

### Dark mode (`.dark`) — **must implement**
| Token | oklch | ~Hex |
|---|---|---|
| `background` | `0 0 0` | `#000000` |
| `card` | `0.14 0 0` | ~`#242424` |
| `foreground` | `1 0 0` | `#FFFFFF` |
| `primary` (brighter for legibility) | `0.62 0.16 285` | ~`#8A7DE0` |
| `border` | `1 0 0 / 10%` | white @10% |
| `input` | `1 0 0 / 14%` | white @14% |

### Shadows / effects (custom, from styles.css)
- `gx-card-shadow`: `0 0 0 0.5px rgba(0,0,0,.04), 0 1px 2px rgba(0,0,0,.025), 0 6px 18px -10px rgba(20,20,40,.06)` (iOS-style hairline + lift)
- `gx-soft-shadow`: `0 0 0 0.5px rgba(0,0,0,.04)` · `gx-hairline`: `0 0 0 0.5px rgba(0,0,0,.06)`
- `gx-orb`: radial gradient (electric-lavender → baby-lavender → powder-pink) — decorative "BrandOrb" blob
- Animations: `gx-fade-up` (translateY 8px→0, 320ms), `gx-float` (looping float for splash eggs)

### Typography — VERIFIED
- **Display:** **Outfit** (Google Fonts, weights 100–900), letter-spacing `-0.025em` → all headings/titles/brand. *(Blueprint said Inter for headings — wrong; use Outfit.)*
- **Body:** **Inter**, letter-spacing `-0.005em` → body, paragraphs, metadata.
- Add `outfit_*.ttf` and `inter_*.ttf` to `res/font/`. Map Outfit→`displayLarge/headlineMedium/titleLarge`, Inter→`bodyLarge/bodyMedium/labelSmall`.

| Role | Size (real usage) | Weight |
|---|---|---|
| Splash CTA / Nutrition title | 32px | semibold |
| Screen title (Home greeting, Quiz Q) | 26px | semibold |
| Card heading | 17–18px | semibold |
| Section label (ALL CAPS) | 11px | medium, tracked |
| Body | 13.5–15px | normal |
| pH value display | 48px (`text-5xl`) | semibold |

### Spacing & shape — VERIFIED
- Base radius `--radius: 1rem (16px)`; scale: sm 12 · md 14 · lg 16 · xl 20 · 2xl 24 · 3xl 28 · 4xl 32.
- Applied: **cards/dialogs `28px`**, buttons/inputs `16–24px`, pills `full`. Phone frame (web only) `48px` — ignore on native.
- Spacing 4px base; cards usually `p-5`(20)/`p-6`(24). Honor **safe-area insets** (status bar / nav bar) — native handles via `enableEdgeToEdge` + insets.

---

## Project Structure

```
genesyx-android/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── kotlin/com/genesyx/app/
│   │   │   ├── MainActivity.kt              ← Single-activity host (Scaffold + bottom nav)
│   │   │   ├── GenesyxApplication.kt        ← @HiltAndroidApp
│   │   │   │
│   │   │   ├── di/                          ← Hilt modules (Database, Network/Supabase, Repository)
│   │   │   │
│   │   │   ├── data/
│   │   │   │   ├── local/
│   │   │   │   │   ├── GenesyxDatabase.kt    ← Room (offline cache)
│   │   │   │   │   ├── dao/  { CycleDao, LogDao, PhDao, UserDao }
│   │   │   │   │   └── entity/ { CycleEntity, DailyLogEntity, PhReadingEntity, UserEntity }
│   │   │   │   ├── remote/
│   │   │   │   │   ├── SupabaseClient.kt     ← supabase-kt (auth + postgrest)
│   │   │   │   │   └── dto/
│   │   │   │   └── repository/
│   │   │   │       ├── AuthRepository.kt
│   │   │   │       ├── CycleRepository.kt
│   │   │   │       ├── DailyLogRepository.kt
│   │   │   │       ├── PhRepository.kt
│   │   │   │       ├── PartnerRepository.kt
│   │   │   │       ├── NutritionRepository.kt
│   │   │   │       └── ProfileRepository.kt
│   │   │   │
│   │   │   ├── domain/
│   │   │   │   ├── model/  { User, Profile, CycleSettings, CyclePhase, DailyLog,
│   │   │   │   │             PhReading, PartnerInvite, NutritionFocus, Supplement }
│   │   │   │   └── usecase/ { GetCycleInsights, GetNutritionFocus, LogDailyEntry,
│   │   │   │                  ComputeCyclePhase, ComputeStreak, ClassifyPh }
│   │   │   │
│   │   │   ├── ui/
│   │   │   │   ├── theme/  { Color.kt, Theme.kt (light+dark), Type.kt }
│   │   │   │   ├── navigation/ { GenesyxNavGraph.kt, Screen.kt }
│   │   │   │   ├── components/  ← Shared composables
│   │   │   │   │   { GenesyxBottomNav, BrandLogo, BrandOrb, PurpleCard, ScreenHeader,
│   │   │   │   │     BarChart, LineChart, DidYouKnowModal, SupplementBadge,
│   │   │   │   │     CycleSettingsSheet, PhLogSheet }
│   │   │   │   ├── home/        { HomeScreen, HomeViewModel }
│   │   │   │   ├── track/       { TrackScreen, TrackViewModel }      ← month calendar
│   │   │   │   ├── nutrition/   { NutritionScreen, NutritionViewModel } ← foods + hydration
│   │   │   │   ├── insights/    { InsightsScreen, InsightsViewModel }   ← charts + pH insights
│   │   │   │   ├── profile/     { ProfileScreen, ProfileViewModel, PartnerSection }
│   │   │   │   ├── log/         { LogScreen, LogViewModel }          ← daily log modal
│   │   │   │   ├── ph/          { PhTrackerScreen, PhViewModel }     ← pH chart + history
│   │   │   │   ├── pregnancy/   { PregnancyScreen }                  ← preview/stub
│   │   │   │   ├── auth/        { AuthScreen, AuthViewModel }        ← email + Google OAuth
│   │   │   │   ├── invite/      { InviteAcceptScreen, InviteViewModel } ← deep link /invite/{code}
│   │   │   │   └── onboarding/  { SplashScreen, OnboardingIntroScreen, OnboardingScreen(Quiz),
│   │   │   │                     OnboardingViewModel, ReadinessSummaryScreen, WaitlistScreen }
│   │   │   │
│   │   │   └── util/ { DateUtils, Extensions }
│   │   │
│   │   └── res/ { drawable/ (egg images, splash bg), font/ (Outfit+Inter),
│   │             values/ (strings, themes), xml/ (backup_rules) }
│   └── build.gradle.kts
├── build.gradle.kts
└── settings.gradle.kts
```

---

## Dependencies (`app/build.gradle.kts`)

Compose BOM pins Compose. Version catalog (`libs.versions.toml`). Plugins: `android.application`, `kotlin.android`, `kotlin.compose`, `hilt.android`, `ksp`.

- **Compose:** bom, ui, ui.graphics, material3, ui.tooling.preview (+ debug tooling)
- **Navigation:** navigation.compose · **Lifecycle:** lifecycle.runtime.ktx, lifecycle.viewmodel.compose
- **Hilt:** hilt.android, hilt.compiler (ksp), hilt.navigation.compose
- **Room:** room.runtime, room.ktx, room.compiler (ksp) — offline cache
- **Supabase (remote):** `supabase-kt` (auth + postgrest) **+** OkHttp/Ktor — *replaces blueprint's generic Retrofit; see Open Decisions*
- **DataStore:** datastore.preferences (session/theme/onboarding flags)
- **Splash:** core.splashscreen · **Coroutines:** kotlinx.coroutines.android
- **Charts:** Compose-native (custom `BarChart`/`LineChart`) or Vico — for cycle regularity, nutrition, pH chart
- **Google Sign-In:** Credential Manager / Play Services Auth (native Google OAuth → Supabase session)

---

## Navigation

Single `NavHost`, start `Splash`. Routes as sealed `Screen`.

**Onboarding flow** (web uses a `flow` state machine; native = nav routes):
`Splash → OnboardingIntro → OnboardingQuiz → ReadinessSummary → (Waitlist, optional) → Home`
Each onboarding step pops itself off the back stack.

**Auth:** `Auth` route (email/password + Google). Main tabs are auth-gated; onboarding is public.
**Deep link:** `genesyx://invite/{code}` → `InviteAccept` (sign-in required, email must match).

**Bottom nav** (`GenesyxBottomNav`) — main tabs only; hidden on splash/onboarding/auth/invite/modals.

| Tab | Route | Icon (real app uses Lucide → Material equiv.) |
|---|---|---|
| Home | `home` | `Home` |
| Track | `track` | `CalendarMonth` (Lucide CalendarDays) |
| Nutrition | `nutrition` | `Eco` / leaf (Lucide Leaf) |
| Insights | `insights` | `BarChart` (Lucide BarChart3) |
| Profile | `profile` | `Person` (Lucide User) |

Modals/sheets (not tabs): **LogScreen** (from Home "Log today"), **PhLogSheet**, **CycleSettingsSheet**, **PregnancyScreen** (from Home/Profile).

---

## Backend / Data Layer

**Real backend = Supabase (Postgres + RLS + Auth).** Web runs all writes through TanStack
`createServerFn` server functions; native will call Supabase directly (RLS enforces isolation) via `supabase-kt`, with Room as an offline cache.

### Tables (with RLS = owner-only unless noted)
| Table | Key columns |
|---|---|
| `profiles` | id (FK auth.users), display_name, avatar_url, **partner_id** (FK profiles), theme (`light`/`dark`). *SELECT allowed for self or linked partner.* |
| `cycle_settings` | user_id (unique), cycle_length (21–35), period_length (1–10), last_period_date |
| `daily_logs` | user_id, date, mood, energy (`low`/`normal`/`high`), symptoms[], sleep_minutes, water_ml, supplements[], notes — **UNIQUE(user_id, date)** |
| `partner_invites` | inviter_id, invitee_email, code (unique), status (`pending`/`accepted`/`revoked`/`expired`), expires_at (+14d), accepted_by/at |
| `ph_readings` | user_id, ph_value (NUMERIC 4.5–9.0), recorded_at, notes |

### Operations the native app needs (from `src/lib/*.functions.ts`)
- **cycle:** `getCycleSettings`, `upsertCycleSettings`
- **daily-log:** `getDailyLog(date)`, `upsertDailyLog(partial)`, `getStreak` (count back from today until first gap)
- **ph:** `listPhReadings(sinceDays?)`, `create/update/deletePhReading` (validate 4.5–9.0)
- **partner:** `sendPartnerInvite(email)`, `revokePartnerInvite(id)`, `acceptPartnerInvite(code)` (bidirectional `partner_id`; needs privileged path), `unlinkPartner`
- **account:** `getProfilePrefs`, `updateDisplayName`, `updateTheme`, `deleteAccount` (cascade delete all user rows + auth user)

> `acceptPartnerInvite`, `unlinkPartner`, `deleteAccount` use the **service role** on the web (privileged).
> Native cannot hold a service-role key. **Decision needed:** expose these as Supabase **Edge Functions** the app calls, or keep the existing server endpoints. See Open Decisions.

### Auth
- Email + password (`signUp`/`signInWithPassword`, password 8–72).
- Google OAuth — web via Lovable wrapper; **native → Google Credential Manager → Supabase `signInWithIdToken`**.
- Session: store JWT in **encrypted DataStore / Keystore** (not plain prefs). `email_verified` in JWT gates partner accept.

---

## Cycle Engine (port from `src/lib/cycle.ts` / `cycleEngine.ts`)

Pure functions, no backend. Given `last_period_date`, `cycle_length`, `period_length`, compute:
- current **day-of-cycle**, **phase** (`period` / `follicular` / `fertile` / `ovulation` / `luteal`), and **fertile window**.
- Phase colors (Track calendar): period→powder-pink, follicular→card/border, fertile→powder-blue, ovulation→primary (ring), luteal→baby-lavender. Today→ring on foreground.
Port verbatim into `domain/usecase/ComputeCyclePhase` + `util/DateUtils` and unit-test against the TS logic.

---

## Screen Build Order

| # | Screen | In blueprint? | Status | Source (web) |
|---|---|---|---|---|
| — | Theme (Color/Type/Theme + **dark mode**) | partial | ⬜ correct tokens | `styles.css` |
| — | Navigation (Screen, NavGraph) | ✅ | ⬜ add modifier param + onboarding/auth/invite routes | — |
| — | MainActivity + GenesyxBottomNav | ✅ | ⬜ | `AppShell`, `BottomTabBar` |
| 1 | Splash + OnboardingIntro | ✅ | ⬜ | `Onboarding.tsx` (splash/intro, floating eggs) |
| 2 | OnboardingQuiz (5 Q + DidYouKnow modals) | ✅ | ⬜ | `Quiz.tsx` |
| 3 | ReadinessSummary + Waitlist | partial | ⬜ | `Conversion.tsx` (results + waitlist) |
| 4 | Home (cycle hero, hydration, streak, focus, log CTA) | ✅ | ⬜ | `Home.tsx` |
| 5 | Track (month calendar + phase colors) + CycleSettings sheet | ✅ | ⬜ | `Track.tsx`, `CycleSettingsDialog.tsx` |
| 6 | Nutrition (phase foods + hydration ± tracker) | ✅ | ⬜ | `Nutrition.tsx` |
| 7 | Insights (cycle regularity, symptom heatmap, nutrition, pH insights) | ✅ | ⬜ | `Insights.tsx`, `PhInsightsSection.tsx` |
| 8 | **pH Tracker** (line chart, 7/30/90/all, history, edit/delete) | ❌ add | ⬜ | `PhTrackerCard.tsx`, `PhLogDialog.tsx` |
| 9 | **Daily Log** modal (mood/energy/symptoms/sleep/water/supplements/notes) | ❌ add | ⬜ | `Log.tsx` |
| 10 | Profile (focus toggle, account, prefs, theme, sign-out) | ✅ | ⬜ | `Profile.tsx` |
| 11 | **Partner** (invite form, pending list, linked display) + invite-accept screen | ❌ add | ⬜ | `PartnerSection.tsx`, `invite.$code.tsx` |
| 12 | **Auth** (email + Google) | ❌ add | ⬜ | `auth.tsx` |
| 13 | **Pregnancy** preview (stub) | ❌ add | ⬜ | `Pregnancy.tsx` |
| — | Theme toggle (light/dark) wired to profile/DataStore | ❌ add | ⬜ | `ThemeToggle.tsx` |

### Per-screen prompt template
```
Build [ScreenName].kt in Jetpack Compose for the Genesyx app.
Match this screenshot exactly.
Design tokens: primary electric-lavender ~#4D4DAA, background ~#F2F2F2, card #FFFFFF,
text ~#1A1A1A, muted ~#6E6B78; cards 28px radius; Outfit (display) + Inter (body); support dark mode.
Use Material3, MVVM with a companion ViewModel, and our shared GenesyxTheme + GenesyxBottomNav.
```

---

## Feature Parity Matrix

| Feature | Web → backend | In blueprint | Native plan |
|---|---|---|---|
| Onboarding quiz (5 Q + facts) | client-only | ✅ | Build; persist answers locally (DataStore) |
| Cycle tracking + phase math | Supabase + pure math | ✅ | Port engine; `cycle_settings` |
| Cycle settings editor | Supabase | ⚠️ implied | Add edit sheet |
| Daily logging | Supabase | ❌ | **Add** Log modal + `daily_logs` |
| Hydration tracker (± water) | Supabase (`water_ml`) | ❌ | **Add** on Home + Nutrition |
| Streak counter | Supabase (computed) | ✅ (stub) | Compute from logs |
| Nutrition guidance (phase foods) | hardcoded | ✅ | Port `PHASE_FOODS` map |
| **pH tracking** + chart + insights | Supabase | ❌ | **Add** full feature |
| **Partner** invite/link | Supabase (+ service role) | ❌ | **Add**; needs Edge Fn for accept/unlink |
| Insights dashboard (charts) | mocked in `mockData.ts` | ✅ | Build; wire real data where available |
| Theme light/dark | Supabase + classList | ❌ | **Add** dark mode + toggle |
| Account mgmt (name/password/delete) | Supabase (+ service role) | ❌ | **Add**; delete via Edge Fn |
| Pregnancy mode | stub | ❌ | **Add** preview screen only (defer full mode) |
| Google OAuth | Lovable wrapper | ❌ | **Add** native Google → Supabase |

---

## Known Gaps / Fixes when scaffolding

1. **`GenesyxNavGraph` signature** — blueprint defines `GenesyxNavGraph(navController)` but `MainActivity` passes `modifier = Modifier.padding(innerPadding)`. Add `modifier: Modifier = Modifier`, apply to `NavHost`.
2. **`OnboardingIntroScreen`** is referenced by the nav graph but missing from the blueprint file tree — create it (feature-list intro).
3. **HomeViewModel** ships stubbed; inject real repositories (`Profile`, `Cycle`, `DailyLog`).
4. **Home buttons** ("Log today", "Preview pregnancy pathway") are stubs — wire to Log modal + Pregnancy route.
5. **Charts** — blueprint has no chart impl; build Compose chart components for cycle regularity, nutrition, symptom heatmap, and the pH line chart (with acidic/optimal/alkaline reference bands, y-domain 4.5–9.0).

---

## Open Decisions

1. **Privileged ops (partner accept/unlink, account delete).** Web uses Supabase service role server-side. Native can't hold that key. → Use **Supabase Edge Functions** (recommended) or keep calling the existing web endpoints. *Pending.*
2. **Google OAuth path.** Recommend **native Google (Credential Manager) → `supabase.auth.signInWithIdToken`**, dropping the Lovable web wrapper. Confirm Lovable/Supabase project allows this. *Pending.*
3. **Offline support.** Web is online-only. Native plan adds Room cache + sync-on-reconnect. Confirm scope for v1 (online-only first is acceptable). *Pending.*
4. **Onboarding persistence.** Web doesn't persist quiz answers to Supabase. Keep client-only (DataStore) or add a table? *Pending.*
5. **Design source of truth.** Confirm we match the **real app's** tokens (Outfit/Inter, electric-lavender, oklch, dark mode) — NOT the blueprint's `#5B4FCF`/Inter. *Recommended: real app.*

---

## Native Port Notes (from source deep-dive)

**Architecture facts to honor:**
- **State machine → NavGraph + saved ViewModel.** Web keeps `flow` (`splash·intro·quiz·results·waitlist·app·log·pregnancy`) + `tab` in plain `useState` with **no persistence** (refresh → splash). On native, model onboarding as a nested NavGraph backed by a `SavedStateHandle`/DataStore `OnboardingViewModel` so it **survives process death** and remembers completion (a real improvement over web).
- **Bottom nav** shows only in `app` flow (hidden during onboarding/log/pregnancy) — match.
- **Auth bridge.** Web attaches `Authorization: Bearer <access_token>` to every server-fn call via a global middleware (`auth-attacher.ts`); `AuthProvider` registers `onAuthStateChange` **before** `getSession()` to avoid a race. Native equivalent: a single Supabase client holding the session; repositories call Postgrest directly (RLS enforces scope). Mirror the listener-first bootstrap.
- **Hooks use a pub/sub `Set<Listener>`** (`emitLogChange()`, pH `emit()`) for cross-screen refresh after a mutation. Native equivalent: a shared repository exposing `Flow`/`StateFlow`; collectors update automatically — no manual emit.
- **Server functions are thin wrappers over Supabase** (`requireSupabaseAuth`, Zod-validated). Native calls Supabase directly; **port the Zod validation** into the repository/use-case layer. Privileged fns (`acceptPartnerInvite`, `unlinkPartner`, `deleteAccount`) use the service role → must become **Supabase Edge Functions** the app calls.
- **CSP/connect:** web allows `connect-src 'self' https://*.supabase.co wss://*.supabase.co` — confirms Supabase is the only network dependency.
- **404 & errors exist:** `__root.tsx` has a `NotFoundComponent` (404 "Page not found" + Go home); router has `DefaultErrorComponent` ("Something went wrong", Try again / Go home). Provide native equivalents (nav fallback + error surface).

**Web → native mapping cheatsheet:**
- Recharts pH chart → Compose chart (Vico/MPAndroidChart): recreate colored reference bands (acidic/optimal/alkaline), tooltip, range pills (7d/30d/90d/All).
- oklch + `color-mix(in oklab,…)` → **pre-compute to ARGB** `Color(0xFF…)`; never compute at runtime.
- BrandOrb (`gx-orb` radial gradient + inset shadows) → `Brush.radialGradient` + layered `drawBehind`, or ship a pre-rendered asset. Floating splash eggs → looping `gx-float` offset animation on the PNG assets in `src/assets/`.
- Outfit variable font from gstatic → **bundle the .ttf**; Inter likewise (web relied on system fallback).
- Sonner toasts → Material 3 `Snackbar` (anchor bottom-center above nav, not bottom-right).
- shadcn `Sheet` (Log, PhLog, CycleSettings) → `ModalBottomSheet` (keep `rounded-t-[28px]`). Dialogs → `AlertDialog`/`Dialog`.
- `<input type=date/datetime-local>` → Material `DatePicker`/`TimePicker`. `100dvh` + `env(safe-area-inset-*)` → `WindowInsets.systemBars` + `enableEdgeToEdge`.
- Google OAuth → Credential Manager → `supabase.auth.signInWithIdToken(Google)` (drop Lovable web wrapper).
- Partner deep link `/invite/$code` → App Link intent filter on `genesis-cycle-guide.lovable.app/invite/:code` → `acceptPartnerInvite`.
- Theme toggle → DataStore + respect system dark mode by default.
- Capacitor config → use only for app id/name/icons; drop the WebView. Desktop phone-frame → drop (native is device-bound).
- **Offline:** web is online-only (no realtime, no SW). Native adds Room cache + sync-on-reconnect (net-new; scope per Open Decisions).

**Not implemented in web (decide for native):** password reset / forgot-password (no `resetPasswordForEmail`), email-confirmation landing page, persistent onboarding-complete flag, realtime subscriptions, offline, full pregnancy mode (only the transition/sell screen), push notifications (Capacitor present, no plugin wired).

## Conventions

- Complete files only — no partial snippets; all imports at the top. Kotlin only, no Java.
- ViewModel + `StateFlow<UiState>` per screen; `collectAsState()`.
- Compose-first; XML only for `themes.xml`, `backup_rules.xml`, splash.
- One screen per build step, matched to its screenshot.
- Light + dark mode parity from day one (the real app ships both).
