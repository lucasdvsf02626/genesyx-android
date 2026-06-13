# Genesyx — UI/UX Spec (from Lovable extraction)

> Detailed UI/UX reference for the native rebuild — the **per-screen build contract**.
> Source: Lovable Chat-Mode extraction of the live web app, cross-checked against the
> actual web source in this repo. High-level architecture lives in [`../ARCHITECTURE.md`](../ARCHITECTURE.md);
> this file holds the exhaustive screen/feature/state detail we feed screen-by-screen.

**Extraction parts (all received):**
- [x] **Part 1 — Full app audit** (this doc, below)
- [x] Part 2 — Design tokens deep dive → [`DESIGN_TOKENS.md`](DESIGN_TOKENS.md)
- [x] Part 3 — Pixel-level screen layouts → [`SCREEN_LAYOUTS.md`](SCREEN_LAYOUTS.md)
- [x] Part 4 — Completeness checklist + summary (folded into ARCHITECTURE.md notes)
- [x] Part 5 — Data/hooks/cycle-math/theming deep dive → [`CYCLE_ENGINE.md`](CYCLE_ENGINE.md) + ARCHITECTURE.md "Native Port Notes"

> **Summary:** 14 screens · 10 server fns · 9 dialogs · ~55 components · dark mode YES · roles NO (partner-link only) · realtime NO · offline NO · rebuild complexity MEDIUM.

**Reconciliation notes vs ARCHITECTURE.md / blueprint:**
- Confirms verified tokens: electric-lavender `~#4D4DAA`, Outfit (display) + Inter (body), oklch, full dark mode.
- **Flow states are full screens, not modals:** the web app's `flow` state machine has `splash · intro · quiz · results · waitlist · app · log · pregnancy`. So **Log Today** and **Pregnancy Transition** are full-screen flows (native = routes), while Cycle Settings / pH Log / Day Detail / Sleep / Water / Supplements / Edit Name / Change Password / Delete / Quiz-fact are **dialogs**.
- **Not persisted to backend:** quiz answers (in-memory only) and the waitlist email (no write). Push-notifications toggle is local-only. Several Profile rows (Tracking/About) are non-functional placeholders.
- **Mock/synthetic data:** Insights cycle-regularity bars, symptom heatmap (sine-wave generated), nutrition-consistency bars, supplement plan card, and Nutrition articles are all static/mock — only the pH insights are live.
- **Open color items:** pH status hexes (acidic/optimal/alkaline) live in `src/hooks/use-ph.ts` (not yet captured); Nutrition food-bullet colors are hardcoded `#F48FB1` (period), `#A5D6A7` (follicular), `#CE93D8` (ovulatory), `#B39DDB` (luteal).

---

## 1. App Overview
- **Name:** Genesyx. **Purpose:** premium, calm fertility-prep & conception-support companion — cycle awareness, nutrition, supplements, urine-pH tracking, partner linking, future pregnancy pathway.
- **Target user:** adults (primarily women) planning/trying to conceive, plus partners.
- **Stack:** React 19 + TS + Vite 7 + TanStack Start v1 (file routing, `createServerFn`); TanStack Query; Tailwind v4 (oklch tokens in `styles.css`); shadcn/ui (Radix); lucide-react; Recharts; Supabase (Lovable Cloud) Postgres+Auth+RLS; Supabase Auth (email/password + Google via `@lovable.dev/cloud-auth-js`). Deploy: Cloudflare Workers + Capacitor Android wrap.

## 2. Screen Inventory (14 surfaces)
Single-page shell at `/` swapping `flow` states, plus 2 standalone routes.

| # | Screen | Route / Flow | Purpose | Reached via |
|---|---|---|---|---|
| 1 | Splash | `/` flow=`splash` (default) | Brand entry, floating eggs, CTA | App load |
| 2 | Onboarding Intro | flow=`intro` | 3 benefits | Splash → Start Quiz |
| 3 | Quiz | flow=`quiz` | 5-Q quiz + "Did you know?" after Q2/Q4 | Intro → Continue |
| 4 | Quiz Results | flow=`results` | Readiness summary + CTAs | Quiz complete (1.6s delay) |
| 5 | Waitlist | flow=`waitlist` | Email capture (not persisted) | Results → Unlock Guide |
| 6 | Home (Today) | flow=`app` tab=`home` | Greeting, cycle hero, focus, hydration/streak, Log CTA | Results/Waitlist → Continue |
| 7 | Track | tab=`track` | Month calendar + phases, current-phase card, pH card, day dialog | Bottom tab |
| 8 | Nutrition | tab=`nutrition` | Hydration tracker, focus foods, supplements, articles | Bottom tab |
| 9 | Insights | tab=`insights` | pH insights, cycle regularity, symptom heatmap, nutrition bars | Bottom tab |
| 10 | Profile | tab=`profile` | Avatar, focus toggle, Partner, Account, Prefs, About, Sign out, Delete | Bottom tab |
| 11 | Log Today | flow=`log` | Mood, energy, symptoms, sleep, water, supplements, notes | Home/Track → Log |
| 12 | Pregnancy Transition | flow=`pregnancy` | Sell pregnancy mode | Home preview / Profile toggle |
| 13 | Auth | `/auth` (route) | Sign in/up + Google | Splash/Profile/auth-gated action |
| 14 | Partner Invite | `/invite/$code` (route) | Accept invite by code | External link |

**Secondary dialogs:** Cycle Settings, pH Log, Day Detail, Sleep, Water, Supplements, Edit Name, Change Password, Delete Account (AlertDialog), Quiz "Did you know?".

## 3. Navigation
- **Pattern:** bottom tab bar (Home · Track · Nutrition · Insights · Profile) in app mode; otherwise single-screen flow with in-screen back buttons. `/auth` & `/invite/$code` render outside the phone shell.
- **Entry:** `/` → Splash. **Post-login:** `/auth` success → `/` (lands on Splash unless flow already `app`).
- **Public:** `/` (all onboarding + app-tab UI), `/auth`. **Protected (data-level via RLS, not routing):** all saves (logs, cycle, pH, partner, account) via `requireSupabaseAuth`. `/invite/$code` shows sign-in prompt if unauthenticated.

```
Splash ──Sign in──► /auth ──signed in──► /
  │ Start Quiz
  ▼
Intro ◄─back─ Quiz (5 Qs; dialogs after Q2 & Q4)
  │ Continue   │ Complete (1.6s)
  └────────────┘
               ▼
            Results ──Continue───────────────┐
               │ Unlock Guide                │
               ▼                             │
            Waitlist ──Continue──────────────┤
                                             ▼
   App Shell (tabs):
     Home → Log Today (save→Home); → Pregnancy; → Profile(avatar)
     Track → Log; Cycle Settings; Day Detail; pH card → pH Log
     Nutrition → in-place water adjuster, expandable foods
     Insights → pH section → opens Track tab
     Profile → Partner / Auth dialogs / Pregnancy / Logout
   External: /invite/$code → (sign-in) → accept → /
```

## 4–6. Design System
Tokens/typography/spacing are captured in **[`../ARCHITECTURE.md`](../ARCHITECTURE.md)** (Design Tokens — VERIFIED). Highlights confirmed by Part 1:
- **Brand palette:** zenith `#F2F2F2`, electric-lavender `#4D4DAA` (primary), powder-blue/baby-blue `#8DD2E2` (fertile), powder-pink `#DDA4D3` (period), electric-blue `#57A1CE` (hydration), baby-lavender `#8888D3` (luteal), electric-pink `#C782D8` (pregnancy/avatar), baby-pink `#DEBED2` (rare).
- **Fonts:** Outfit (display, `@font-face` from Google) tracking `-0.025em`; Inter (body, fallback Vend Sans/system) tracking `-0.005em`. Sizes applied via `text-[Npx]` not h1/h2 globals (see size table in ARCHITECTURE.md).
- **Radius:** base `1rem`; cards `24–28px`, buttons `16px` (pills `full`), inputs/dialog buttons `12px`, quiz fact dialog `3xl`. Max content width `420px` (phone frame `rounded-[48px]`, desktop only).
- **Shadows:** `gx-card-shadow`, `gx-soft-shadow`, `gx-hairline` (+ dark inverts); special CTA/eBook/orb shadows. `gx-orb` radial brand pearl.
- **Spacing:** 4px base; screen edges `px-5` (onboarding `px-6`); cards `p-4/p-5`, hero `p-6`; vertical rhythm `mt-3/4/5`, `space-y-3/4`, grid `gap-2/3`.

## 7. Component Library
shadcn/ui (full set installed). **Used:** Button (lavender primary + outline/ghost + custom rounded), Input, Textarea, Label, Badge, Progress, Switch, Slider, Dialog, AlertDialog, Sonner toaster (top-center). **Custom (`components/genesyx/`):** BrandLogo, BrandOrb, AppShell (phone frame), BottomTabBar, ScreenHeader, ThemeToggle + `useTheme`, CycleSettingsDialog, PartnerSection, PhTrackerCard, PhLogDialog, PhInsightsSection, + 14 screen components.

## 8. Feature List (condensed — full detail in source audit)
- **Onboarding/Conversion:** floating eggs (decor), start quiz, sign-in shortcut, 3-benefit intro, 5-Q quiz (in-memory), "Did you know?" facts (Q2/Q4), 1.6s loading orb, readiness summary, unlock-guide CTA, continue-to-dashboard, waitlist email (validated, **not persisted**).
- **Home:** time-aware greeting (`user.user_metadata`), avatar→Profile, cycle hero (`getCycleSettings`), cycle settings dialog (`upsertCycleSettings`), today's focus (computed), hydration (`getDailyLog`), streak (`getStreak`), Log CTA, pregnancy preview.
- **Track:** month calendar w/ phase coloring (computed), prev/next month, day-cell→day dialog, phase legend, current-phase card, edit cycle settings, add-to-log, embedded pH card.
- **pH (Track + Insights):** latest badge, range filter 7/30/90/All, Recharts line chart w/ status bands, history list, log/edit/delete (`create/update/deletePhReading`, `listPhReadings`).
- **Nutrition:** phase header, hydration ±200ml stepper (`upsertDailyLog` debounced 500ms), expandable focus foods (static `PHASE_FOODS`), supplement summary (mock), 3 article cards (mock).
- **Insights:** pH insights summary (live, `listPhReadings(90)` — value, status, trend, 7/30-day avg, insight+recommendation), cycle regularity bars (mock), symptom heatmap (synthetic), nutrition consistency bars (mock); `empty` hardcoded false.
- **Log:** mood (4), energy (3-seg), symptom chips (8 + custom), sleep dialog, water dialog (0–10000 step 100), supplements (4 checkboxes), notes (max 2000), auth-gated save (`upsertDailyLog`).
- **Profile:** avatar+name+email+Premium badge, focus toggle (Prep/Pregnancy), PartnerSection, edit name (`updateDisplayName`), change password (`supabase.auth.updateUser`), tracking links (non-functional), push toggle (local), dark-mode switch (`useTheme`), about links (non-functional), sign in/out, delete account (`deleteAccount`).
- **Pregnancy:** 2 feature cards (Trimester tracking, Prenatal nutrition), "Switch to pregnancy mode" (no persistence yet), "Not yet".
- **Auth:** mode toggle, email/password sign-in/up (Zod email + min-8), Google OAuth (Lovable wrapper), auto-redirect when signed in.
- **Partner Invite:** code validation preview, sign-in gate, accept (`acceptPartnerInvite`), error states (not found/used/expired/wrong email).

## 9. Screen-by-Screen Element Maps

> Phone shell: `AppShell` centers a 420px-max frame on desktop (`h-[860px] rounded-[48px]`), full-bleed on mobile (`h-[100dvh]`). Top safe-area `max(env(safe-area-inset-top),12px)`. Bottom tab only in `app` flow.

**Splash** — full-height column, bg `--background`. 8 floating brand eggs (70–170px, `gx-float`); BrandLogo (64) center-top; eyebrow "STEP INTO THE FUTURE OF FERTILITY" (lavender, tracking 0.22em); H1 "Feel informed, supported and ready for your conception journey." (Outfit 32/600); subhead 15px muted; primary CTA "Start Your Personalised Quiz" + ChevronRight (h-14, lavender, lifted shadow) → `intro`; "Sign in" text button → `/auth`; Sparkles + footer microcopy. Static.

**Onboarding Intro** — back chevron → splash; H1 "Your fertility preparation, gently guided" (Outfit 30/600); subhead; 3 benefit cards (Heart/lavender "Understand your cycle"; Leaf/blue "Support fertility nutrition"; BarChart3/pink "Receive tailored insights"); Continue CTA (h-14) → `quiz`. Static `benefits`.

**Quiz** — header row (back, Progress h-1.5 = `(step+1)/total*100`, step counter "{n}/5"); question H2 (Outfit 26/600); helper 14px muted; 4 option pills each label + radio (Check when selected); Continue CTA "Continue"/"See My Summary" (disabled w/o selection). Loading: pulsing BrandOrb + "Preparing your personalised summary…" 1.6s. Modal: "Did you know?" Dialog after Q2 & Q4 (title+body+Continue). Data: `quizQuestions` (5) from `mockData.ts`; answers in-memory.

**Quiz Results** — back; BrandOrb (h-20); badge "Your readiness summary"; H1 "A thoughtful starting point"; subhead; insights card (3 rows CalendarDays/Leaf/Sparkles, label+value); suggested-steps card (powder-blue tint, 3 Check items); primary CTA "Unlock My Free Guide" + BookOpen → `waitlist`; text "Continue to dashboard" → `app`. Static `insights`.

**Waitlist** — back; eBook card (~h-56 w-44, gradient, spine, "GENESYX", "The Fertility Nutrition Guide", "EDITION 01", BrandOrb h-7); eyebrow "FREE WITH EARLY ACCESS"; H1 "A gentle guide to fertility nutrition"; subhead; 3 check-pill benefits; email field (Label + Mail icon + Input, regex-validated); inline destructive error; CTA "Join the Waiting List" → success state (Check badge, "You're on the list", "Continue to app"); privacy note (Lock). **Not persisted.**

**Home** — greeting (muted) + display name (Outfit 26/600) + avatar button (40px initial → Profile); hero card (button): shimmer while loading, else BrandOrb decor (h-44 top-right), eyebrow "DAY {n} · {PHASE}", phase heading 26/600, phase sub (muted), tag pills (lavender + powder-blue tints), onClick → CycleSettingsDialog; Today's focus card (eyebrow + title + desc, fallback "Complete your cycle setup…"); 2-col stats — Hydration (Droplets/blue, "{L}/2.4L"), Streak (Leaf/primary, "{n} days"); Log today CTA (h-14, Plus) → `log`; pregnancy preview row ("Preview pregnancy pathway" + ArrowRight) → `pregnancy`. Data: `getCycleSettings`, `getDailyLog`, `getStreak`.

**Track** — ScreenHeader (month "June 2026", subtitle "Cycle {n} · Day {d}"/"Set up your cycle", edit pencil → CycleSettingsDialog); calendar card (prev/label/next; weekday header S M T W T F S; 7×N day grid w/ phase colors per `dayClass`, today ring; loading = 35 pulsing circles; empty = dashed "Add your cycle"; 4-item legend Period/Fertile/Ovulation/Luteal); current-phase card (eyebrow "CURRENT PHASE", phase name 22/600, fertile/days-to-period sentence); add-to-log CTA (h-14, Plus); embedded PhTrackerCard. Modals: CycleSettingsDialog, Day-detail Dialog (weekday/date, "Day X · {phase}", "No log yet"/"Predicted: …"), PhLogDialog.

**pH Tracker Card** — eyebrow "TRACK YOUR PH" + "Urine Tracker" + "Log pH" (Plus); latest reading (color droplet tile + value font-display 22/600 tabular + date/time + status pill); range chips 7d/30d/90d/All in `bg-muted` rail; Recharts LineChart (X time, Y 4.5–9.0, ReferenceAreas acidic/optimal/alkaline, tooltip, lavender line+dots); empty states (sign-in prompt OR "No readings yet" + "Log your first pH"); legend dots (Acidic <6.0 / Optimal 6.0–7.5 / Alkaline >7.5); history list (max-h 260, value tile + date/time + status + notes preview + ChevronRight → edit). Modal: PhLogDialog (Slider + ± buttons + datetime-local + Notes textarea + Save/Cancel + Delete when editing).

**Nutrition** — eyebrow "TODAY · {PHASE}"; H1 "Your nutrition focus" (Outfit 32/600); phase desc; hydration card (eyebrow + L/2.4L + ±200ml round buttons + Progress + "{ml}ml to go"/"Target reached — nice work"); focus-foods card (expandable 3–4 items: colored dot + name + short desc + animated long desc + ChevronRight rotator); supplement plan card (Pill tile + "Your supplement plan" + "Folate, Omega-3, Vitamin D, and Zinc…" + 4 overlapping F/O/D/Z avatars + "3 of 4 taken today" + "Review Plan"); articles (3 buttons w/ read time + ChevronRight). Data: `getCycleSettings`, `useDailyLog` (water); rest static.

**Insights** — ScreenHeader (large) "Your Insights" + subtitle; PhInsightsSection (title "Urine pH" + "Open tracker" → Track; current value+color+status pill; trend Up/Down/Flat; 2-up 7-day/30-day avg; insight + recommendation paragraphs); cycle regularity card (title + "Last 7 cycles" + 7 lavender-gradient bars + insight); symptom patterns card (7×5 mood-tinted grid, synthetic, + insight); nutrition consistency card (7 blue-gradient bars Mon–Sun + insight). pH live; rest mock.

**Profile** — ScreenHeader "Profile"; user card (gradient avatar initial + name + email + "Premium" badge); focus segmented control (Fertility Prep / Pregnancy → triggers pregnancy flow); PartnerSection (unsigned→sign-in; no partner→email input + "Send invite" + pending list w/ copy+revoke X; linked→avatar + name + "Linked partner" + "Remove"); Account group (Edit name, Change password rows + ChevronRight); Tracking group (Personal Details, Health Profile, Tracking Preferences — non-functional); Preferences (Push Notifications Switch local, Dark Mode Switch); About (Privacy & Data, Help & Support — non-functional); Sign out/in (full-width destructive); Delete account (outlined destructive, signed-in only). Modals: EditName, ChangePassword, Delete AlertDialog.

**Log Today** — ScreenHeader "Log Today" + subtitle + back (`onClose`); Mood (4 cells Heart/Smile/Meh/Frown = Great/Good/Okay/Low); Energy (3-seg low/normal/high); Symptoms (8 chips Headache/Fatigue/Cramps/Nausea/Bloating/Acne/Backache/Tender breasts + dashed "Add" → inline input); MiniCard 2×2 — Sleep (Moon/lavender → hours+minutes dialog), Water (Droplets/blue → ml dialog), Supplements (Pill/lavender → Folic acid/Vit D/Iron/Omega-3 checkboxes), Nutrition (Apple/pink, display "On track"); Notes textarea (3 rows); Save log CTA (h-14, session check → "Saving…"). Auth-gated, Zod-validated server-side.

**Pregnancy Transition** — back; BrandOrb (24); H1 "Support for the next chapter" (26/600); subhead; FeatureCard "Trimester tracking" (Baby/pink), FeatureCard "Prenatal nutrition" (Apple/pink); primary CTA "Switch to pregnancy mode" (h-14, returns to app, no persistence); text "Not yet, keep tracking".

**Auth** (`/auth`) — centered max-w-sm card; BrandLogo (32); H1 "Welcome back"/"Create your account" (font-display 3xl/600); subhead; fields Name (sign-up, max 80) / Email / Password; submit (h-12 rounded-xl, Loader2); "OR" divider; Continue with Google (outline h-12); toggle paragraph; Back to app Link. Zod email + min-8 (max 72); errors via sonner.

**Partner Invite** (`/invite/$code`) — centered column (outside shell); BrandLogo (32); Heart icon (h-10 primary); H1 "Partner invite"/"You've been invited"; description (error or invite); Accept button or Sign-in link; "Not now"/"Back to app".

## 10. API & Data Layer
**Backend:** Supabase (Lovable Cloud) — Postgres+Auth+RLS. Env: `VITE_SUPABASE_URL`, `VITE_SUPABASE_PUBLISHABLE_KEY`.

**Tables** (`public`, from `integrations/supabase/types.ts`):
- `profiles` (id FK auth.users, display_name?, avatar_url?, theme, partner_id? FK→profiles, created/updated)
- `cycle_settings` (id, user_id unique, last_period_date, cycle_length 21–35, period_length 1–10, timestamps)
- `daily_logs` (id, user_id, date, mood?, energy?, symptoms[], sleep_minutes?, water_ml default 0, supplements[], notes?, timestamps; UNIQUE(user_id,date))
- `ph_readings` (id, user_id, ph_value numeric 4.5–9.0, recorded_at, notes?, timestamps)
- `partner_invites` (id, inviter_id, invitee_email, code unique 16-char, status pending/accepted/revoked, expires_at, accepted_at, accepted_by, created)
- RLS SQL not in repo (managed via Lovable migrations) — **UNCLEAR**; code assumes owner-scoped `auth.uid()`, service-role only for partner link/unlink + account delete.

**Auth:** Supabase (email/password + Google via Lovable). Session in `localStorage` (`persistSession`, `autoRefreshToken`). `AuthProvider` (`hooks/use-auth.tsx`) → `onAuthStateChange` + `getSession`.

**Server functions** (`createServerFn`, all `requireSupabaseAuth`; privileged load `supabaseAdmin`):
`getCycleSettings`/`upsertCycleSettings` · `getDailyLog`/`upsertDailyLog`/`getStreak` · `listPhReadings`/`create`/`update`/`deletePhReading` · `updateDisplayName`/`updateTheme`(call site unclear)/`getProfilePrefs`(unclear)/`deleteAccount` · `sendPartnerInvite`/`revokePartnerInvite`/`acceptPartnerInvite`/`unlinkPartner`.
**Direct client Supabase:** auth methods (signUp/signInWithPassword/signInWithOAuth/signOut/getSession/getUser/updateUser/onAuthStateChange); `partner_invites` & `profiles` selects (PartnerSection, invite preview). **No public REST endpoints.**

## 11. User Flows
1. **New user:** Splash → Intro → Quiz (facts Q2/Q4) → 1.6s orb → Results → Waitlist (email, not saved) → Home (anon; saves prompt sign-in) → `/auth` create account → confirm email → sign in → persists.
2. **Daily log:** Home/Track → Log → mood/energy/symptoms → sleep/water/supplements dialogs → notes → Save (session check → `upsertDailyLog` → toast → Home).
3. **Cycle setup:** Home hero / Track pencil → Cycle Settings (last period date, cycle 21–35, period 1–10) → save → recompute → hero/calendar/nutrition update.
4. **pH:** Track → pH card → Log pH → slider 4.5–9.0 / ±0.1 + datetime + notes → save → chart/history/Insights update.
5. **Partner:** Profile → Partner → email → Send invite (16-char code, pending w/ copy+revoke) → share `…/invite/{code}` → partner opens → sign-in → Accept → both `partner_id` set via service role → mutual display.
6. **Pregnancy preview:** Home preview / Profile toggle → Pregnancy → "Switch" (no persistence) / "Not yet".
7. **Delete account:** Profile → Delete → AlertDialog → `deleteAccount` (unlink partner; clear daily_logs/cycle_settings/partner_links?/partner_invites/profiles; delete auth user) → sign out → toast → `/auth`.

## 12. States
| Screen | Loading | Empty | Error |
|---|---|---|---|
| Quiz | 1.6s BrandOrb pulse + "Preparing…" | — | — |
| Waitlist | sync | — | destructive text under email on invalid |
| Home | hero shimmer (220px); stats "—" | hero→"Set up your cycle"; focus fallback; stats "—" | toast only |
| Track | calendar 5×7 pulsing circles; subtitle "Loading…" | dashed "Add your cycle"; "—" phase | toast |
| Nutrition | header "TODAY · LOADING…"; foods "Set up your cycle…" | phase fallback | water-debounce errors swallowed |
| Insights | pH summary skeleton; rest static | `empty=false` (unreachable); pH "No pH readings yet…" | — |
| Profile | PartnerSection Loader2 | partner "Add your partner" | sonner toasts |
| Log | Save "Saving…" disabled | empty fields default | toast "Please sign in to save…"/"Could not save log" |
| Auth | submit + Google Loader2 | — | sonner validation/auth errors |
| Invite | full-screen Loader2 | error branch + "Back to app" | inline error in place of accept |

## 13. Responsive
Mobile-first. AppShell: `<sm` full-bleed `w-full h-[100dvh]` no radius/shadow; `≥sm` centered 420×860 `rounded-[48px]` + ambient shadow on zenith/black canvas (iPhone-mockup look). Single-column ~390px; no wide re-layout. Bottom tab only in `app` flow w/ `env(safe-area-inset-bottom)`. `/auth` & `/invite` outside shell (own max-w-sm). Touch targets 44–76px (`min-h-[44/52/60/76px]`). Numeric inputs `inputMode="numeric"`; email `type=email`; native `datetime-local`. Capacitor Android wrapper present → maps to native WebView at device width.

---

_Open follow-ups to capture from later parts / source:_ pH status hex values (`use-ph.ts`), exact `ThemeToggle` storage, RLS SQL, supplement-plan real data model (currently mock).
