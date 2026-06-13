# Genesyx — Pixel-Level Screen Layouts

> Part 3 of the Lovable extraction — exact per-screen layout for pixel-faithful native rebuild.
> Tokens: [`DESIGN_TOKENS.md`](DESIGN_TOKENS.md) · Inventory/features/states: [`UIUX_SPEC.md`](UIUX_SPEC.md).
> Canonical `flow` names (from source audit): `splash · intro · quiz · results · waitlist · app · log · pregnancy`
> (the extraction batches occasionally used `onboarding`/`conversion` for `intro`/`waitlist` — same screens).

**Coverage tracker:**
- [x] Splash · [x] Onboarding Intro · [x] Quiz · [x] Quiz Results · [x] Waitlist
- [x] Home · [x] Track · [x] Nutrition
- [x] Auth · [x] Partner Invite · [x] core modals/components (below)
- [~] Insights · Profile · Log Today · Pregnancy — **element-map level** in [`UIUX_SPEC.md`](UIUX_SPEC.md) §9 (pixel-level layouts not separately delivered; element maps are detailed enough to build from).
- [x] Dialogs: CycleSettings, PhLog, PartnerSection, BottomTabBar, AppShell, ThemeToggle (below). Sleep/Water/Supplements/EditName/ChangePassword/Delete covered inline in UIUX_SPEC §9.

> Shell: AppShell = 420px max, `100dvh` mobile / `860px rounded-[48px]` desktop. Top safe-area `max(env(safe-area-inset-top),12px)`. Bottom tab only in `app` flow (adds ~`pb-28`).

---

## Splash — flow=`splash`
Fixed full-screen, no scroll, min-h 760px, `px-6 pt-4 pb-10`, bg zenith. Background: **8 floating Egg PNGs** (4 male/blue, 4 female/pink, 70–170px, rotated 0–70°, `gx-float` ±12/6px 10–14s staggered 0–3s, pointer-events-none, opacity .85–1).
- **Top:** BrandLogo `genesyx-logo.svg` 64px, centered, `pt-2`, `dark:invert`, z-10.
- **Center (flex-1, centered):** eyebrow "Step into the future of fertility" (Outfit 13px/500 UPPERCASE tracking-[0.22em] electric-lavender) · H1 "Feel informed, supported and ready for your conception journey." (Outfit 32px/600 leading-[1.05] tracking-tight max-w-[16ch], `mt-4`) · sub "A premium, gently-guided companion blending cycle awareness, nutrition and supplement support." (15px leading-relaxed muted-fg max-w-[28ch], `mt-5`).
- **Bottom (space-y-3):** primary CTA "Start Your Personalised Quiz" (h-14 full rounded-2xl, bg electric-lavender, white 16/600, shadow `0 10px 30px -12px rgba(77,77,170,.55)`, trailing ChevronRight 20px) → `quiz` · "Sign in" ghost (text-sm/500 fg/80) → `/auth` · footer row Sparkles 14px + "Educational fertility wellness support, tailored to you." (12px muted-fg).

## Onboarding Intro — flow=`intro`
Scrollable column `px-6 pt-2 pb-10`, bg zenith. Back button top-left (ChevronRight rotated 180°, 44×44).
- **Heading:** H1 "Your fertility preparation, gently guided" (Outfit 30px/600 leading-[1.1] max-w-[16ch]) · sub "Genesyx blends cycle awareness, nutrition, and supportive insights into one calm space." (15px muted-fg `mt-3`).
- **Benefits (3 cards, space-y-3, mt-8):** each `rounded-3xl bg-card p-4 gx-soft-shadow+gx-card-shadow`, flex gap-4, icon tile 48×48 `rounded-2xl`:
  1. Heart, bg `color-mix(electric-lavender 12%, white)`, icon lavender — "Understand your cycle" / "Recognise patterns with calm, clear guidance."
  2. Leaf, bg `color-mix(powder-blue 30%, white)`, icon electric-blue — "Support fertility nutrition" / "Cycle-aware food and supplement focus."
  3. BarChart3, bg `color-mix(powder-pink 30%, white)`, icon electric-pink — "Receive tailored insights" / "Gentle observations based on your tracking."
  Title Outfit base/600; desc 13.5px leading-relaxed muted-fg.
- **Bottom (mt-auto pt-8):** Continue primary h-14 rounded-2xl → `quiz`.

## Quiz — flow=`quiz`
Scrollable, one question/step (5 steps), bg zenith, `px-6 pt-2 pb-8`.
- **Top bar (~56px, in-screen):** ChevronLeft back (44×44) → prev question / intro; center optional "2 / 5".
- **Progress:** shadcn Progress, track muted, fill primary, `h-1` rounded-full, value `(step+1)/total*100`.
- **Question:** H1 Outfit 26px/600 leading-tight `mt-8`; optional support copy 14px muted-fg `mt-2`.
- **Options (stack space-y-3, mt-8):** option pill buttons, full-width `h-14`, `rounded-2xl`, text-[15px]/500 left-aligned px-5; unselected `bg-card gx-hairline`, selected `bg-primary/10 border border-primary` (Check icon when selected); select → enable Continue (or auto-advance).
- **Footer CTA (sticky):** Continue primary h-14 rounded-2xl, disabled until selection ("Continue"/"See My Summary").
- **Modal:** "Did you know?" Dialog after Q2 & Q4 (rounded-3xl, Sparkles tile, title + body + Continue).
- **Loading:** pulsing BrandOrb + "Preparing your personalised summary…" 1.6s → `results`. Data: `quizQuestions` (5) from mockData; answers in-memory only.

## Quiz Results — flow=`results`
Scrollable column `px-6 pt-2 pb-8`, bg zenith. ChevronLeft back → last question.
- **Hero (centered):** BrandOrb h-20 w-20 · Badge "Your readiness summary" (rounded-full, bg lavender@10%, primary, 11px UPPERCASE tracking-wider, mt-5) · H1 "A thoughtful starting point" (Outfit 26/600 mt-3) · sub max-w-[30ch] 14px muted-fg mt-2.
- **Insights card (rounded-3xl bg-card p-5 gx-card-shadow, mt-7):** 3 rows (space-y-4), each icon tile 44×44 rounded-2xl bg lavender@10% (CalendarDays/Leaf/Sparkles) + eyebrow label (12px UPPERCASE tracking-wider muted-fg) + value (15px/500 fg); `border-b border-border/50` between rows.
- **Suggested next steps (rounded-3xl border bg `color-mix(powder-blue 18%, white)` p-5, mt-5):** title "Suggested next steps" Outfit 15/600 + 3 Check-bulleted items (14px fg/85).
- **CTA stack (mt-auto pt-7 space-y-2):** "Unlock My Free Guide" primary h-14 rounded-2xl (BookOpen) → `waitlist` · "Continue to dashboard" ghost → `app`.

## Waitlist — flow=`waitlist`
Scrollable `px-6`, bg zenith with subtle BrandOrb behind hero.
- **Hero:** eBook card (~h-56 w-44, gradient, spine accent, "GENESYX", "The Fertility Nutrition Guide", "EDITION 01", BrandOrb h-7) · eyebrow "FREE WITH EARLY ACCESS" · H1 "A gentle guide to fertility nutrition" (Outfit 26/600) · sub max-w-[32ch] 14px muted-fg.
- **Benefits card (rounded-3xl bg-card p-5 gx-card-shadow):** 4 Check items (14px fg): "Early access to Genesyx", "Personalised fertility-prep tools", "Nutrition and supplement guidance" (+1).
- **Email capture:** Input with leading Mail icon, full-width `h-12 rounded-2xl bg-card border-border`; regex-validated; inline destructive error on invalid.
- **CTA:** "Join the Waiting List" primary h-14 rounded-2xl → success state (Check badge, "You're on the list", paragraph, "Continue to app") + Sonner toast. Privacy note (Lock). **Email NOT persisted.**

## Home — flow=`app` tab=`home`
Scrollable `px-5 pt-3 pb-4` (+pb-28 from shell), bg zenith.
- **Header (px-1, flex justify-between):** left greeting "Good morning/afternoon/evening" (13px muted-fg) + H1 displayName (`user_metadata.full_name || display_name || email-prefix || "Guest"`, Outfit 26/600 leading-tight); right Avatar button 40×40 rounded-full bg-card, initial uppercase 13/600, `gx-hairline` → Profile.
- **Cycle hero (mt-6):** loading = `h-[220px] rounded-[28px] bg-muted` + shimmer overlay. Loaded = full-width button `rounded-[28px] bg-card p-6 gx-card-shadow`:
  - BrandOrb h-44 w-44 absolute `-right-10 -top-12` opacity-70.
  - eyebrow `DAY {n} · {PHASE}` (11px UPPERCASE tracking-[0.16em]/500 primary) · headline phase-specific (Outfit 26/600 leading-[1.1] max-w-[14ch] mt-3) · sub (13.5px leading-relaxed muted-fg max-w-[24ch] mt-2) · tags row (mt-5 flex-wrap gap-1.5): first tag bg lavender@8% text primary, rest bg powder-blue@22% text fg/75, each rounded-full px-2.5 py-1 11.5px.
  - empty (no settings): eyebrow "TODAY", headline "Set up your cycle", sub "Add your last period date to get personalised insights.", no tags.
  - → opens CycleSettingsDialog.
- **Today's focus (mt-3, rounded-[24px] bg-card p-5 gx-soft-shadow):** eyebrow "Today's focus" (11px UPPERCASE tracking-[0.14em] muted-fg) · loaded title (Outfit 17/600 mt-1.5) + desc (13px muted-fg) · empty "Complete your cycle setup to see focus foods."
- **Stat grid (mt-3 grid-cols-2 gap-3):** Hydration card (rounded-[20px] bg-card p-4 gx-soft-shadow, Droplets 16px electric-blue, eyebrow "Hydration" 10.5px UPPERCASE, value Outfit 18/600 e.g. "1.2L" + "/ 2.4L" 12px muted-fg; "—" when no log) · Streak card (Leaf 16px primary, eyebrow "Streak", value "{n} days").
- **Log CTA (mt-5):** "Log today" primary h-14 rounded-2xl 15/600 + Plus 20px → `log`.
- **Pregnancy preview (mt-3):** full-width button px-2 py-3 flex justify-between — "Preview pregnancy pathway" 13px muted-fg + ArrowRight 16px → `pregnancy`.
- Bottom: BottomTabBar active=home. Modal: CycleSettingsDialog (last period date, cycle length, period length, Save).

## Track — flow=`app` tab=`track`
Scrollable `px-5` (+pb-28), bg zenith.
- **ScreenHeader:** title current month "June 2026" (Outfit text-xl/600) · subtitle `Cycle {n} · Day {d}` (text-sm muted-fg; or "Loading…"/"Set up your cycle") · trailing 36×36 rounded-full bg-card gx-hairline Pencil → CycleSettingsDialog.
- **Calendar card (rounded-[28px] bg-card p-5 gx-card-shadow):**
  - month nav (mb-3): prev 32×32 rounded-full bg-muted ChevronLeft · month label 12.5px muted-fg · next ChevronRight.
  - weekday row (grid-cols-7 gap-1.5): S M T W T F S (10px UPPERCASE tracking-[0.14em]/500 muted-fg).
  - loading: 35 circular skeletons aspect-square rounded-full bg-muted/70 animate-pulse.
  - empty: dashed-border button "Add your cycle" (Outfit 15/600) + sub "Tell us when your last period started to see your phases here." (12.5px muted-fg) → CycleSettingsDialog.
  - grid (35–42 cells, grid-cols-7 gap-1.5): each aspect-square rounded-xl 13/500 `active:scale-95`; colors — period `color-mix(powder-pink 55%, white)`, follicular `bg-card border border-border`, fertile `color-mix(powder-blue 55%, white)`, ovulation `bg-primary text-primary-foreground ring-2 ring-offset-2 ring-primary/30`, luteal `color-mix(baby-lavender 25%, white)`; today adds `ring-2 ring-foreground ring-offset-2 ring-offset-card`. Tap → day-detail Dialog.
  - legend (mt-5 grid-cols-2 gap-x-3 gap-y-2, 11.5px): 14×14 rounded-md swatch + label for Period/Fertile/Ovulation/Luteal.
- **pH Tracker card (PhTrackerCard, rounded-[28px] bg-card p-5 gx-card-shadow):**
  - header: eyebrow "TRACK YOUR PH" (11px UPPERCASE tracking-[0.16em] primary) + title "Urine Tracker" (Outfit 18/600); right "Log pH" pill (h-9 rounded-full bg-primary px-4 13/600 + Plus).
  - latest reading panel (rounded-2xl bg-muted/40 p-4): icon tile 48×48 rounded-2xl tinted by status + Droplet; eyebrow "Latest reading" + value Outfit 22/600 tabular-nums (e.g. "6.5"); date+time 11.5px muted-fg; status pill rounded-full px-3 py-1 11/600 UPPERCASE tracking-wider.
  - range selector (mt-4 grid-cols-4 gap-1 rounded-2xl bg-muted p-1): "7d/30d/90d/All" — active bg-card fg shadow-sm, inactive muted-fg, rounded-xl py-2 12/600.
  - chart (h-[200px]): Recharts LineChart, primary stroke, CartesianGrid muted, ReferenceArea bands, Tooltip, XAxis date / YAxis pH; loading shimmer; empty "No readings yet" + "Log pH" CTA.
- **Modals:** Day-detail Dialog (date title + phase desc; past "No log yet…", future "Predicted: {phase} · Fertile window"), CycleSettingsDialog, PhLogDialog (datetime, pH slider 4.0–9.0 step 0.1 color-tinted, notes, Save/Delete).

## Nutrition — flow=`app` tab=`nutrition`
Scrollable (+pb-28), bg zenith.
- **Header (px-6 pt-3 pb-6):** eyebrow `TODAY · {PHASE}` (12px UPPERCASE tracking-[0.16em]/500 primary) · H1 "Your nutrition focus" (Outfit 32/600 leading-[1.05] mt-2) · sub phase-specific (14.5px leading-relaxed muted-fg mt-3). Empty/loading: "TODAY · LOADING…"/"TODAY · SET UP YOUR CYCLE" + "Set up your cycle to get personalised nutrition guidance."
- **Hydration card (rounded-[28px] bg-card p-5 gx-card-shadow):** top row — left eyebrow "Hydration" (11px UPPERCASE) + value Outfit 28/600 (e.g. "1.6") + "/ 2.4 L" (13px muted-fg); right stepper Minus 36×36 rounded-full bg-muted + Plus 36×36 rounded-full bg-primary (±200ml, clamp 0–10000, **debounced 500ms save**). Progress h-1.5 bg-muted fill bg-foreground (mt-4). Footnote Droplets 14px + "{remaining}ml to go"/"Target reached — nice work" (12px muted-fg).
- **Focus foods card (rounded-[28px] bg-card overflow-hidden gx-card-shadow):** per-phase FoodItem rows (single-expand accordion) — period (3) accent `#F48FB1`, follicular (3) `#A5D6A7`, ovulatory (4) `#CE93D8`, luteal (4) `#B39DDB`. Collapsed = colored dot + name (Outfit/600) + shortDesc (13px muted-fg) + ChevronRight; expanded reveals expandedDesc; hairline dividers.
- **Supplements card (rounded-[28px] bg-card p-5 gx-card-shadow):** Pill icon + "Supplements" (Outfit 17/600) + recommended items (folate, omega-3, vit D, zinc…) with rationale. _(Part 1 also describes a "supplement plan" summary variant with F/O/D/Z avatar stack + "3 of 4 taken today" + "Review Plan" — mock data.)_
- **Articles (from `mockData.articles`):** article tiles rounded-3xl bg-card overflow-hidden, image header + category eyebrow + Outfit title + excerpt + "Read more" (non-functional).
- Bottom: BottomTabBar active=nutrition.

---

## Auth — route `/auth` (standalone, no AppShell frame)
`min-h-screen flex-col items-center justify-center px-6 py-10`, bg `--background`, Toaster top-center. Redirect: when `user` truthy → `navigate({to:"/", replace:true})`. Inner card `w-full max-w-sm`.
- BrandLogo size=32 centered mb-8.
- **Heading:** H1 (Outfit text-3xl/600 centered) "Welcome back" / "Create your account"; sub (text-sm muted-fg mt-2) "Sign in to sync your journey across devices." / "Save your cycle, nutrition, and partner info securely."
- **Form (mt-8 space-y-4):** Name (signup only, maxLength 80, "Your name", h-12 rounded-xl) · Email (type=email autoComplete=email required) · Password (type=password autoComplete current/new) · Submit primary h-12 full rounded-xl text-base/600 ("Sign in"/"Create account", Loader2 busy).
  - **Zod:** email trim+format+max 255 → toast "Enter a valid email"; password min 8 max 72 → toast "Password must be at least 8 characters".
  - signup → `signUp({ emailRedirectTo: origin+"/", data:{ display_name } })` → toast "Check your email to confirm your account." · signin → `signInWithPassword` → `/`.
- **Divider:** rule with centered "OR" pill (border-t, span bg-background px-2 text-xs uppercase tracking-wider muted-fg).
- **Google:** outline h-12 full rounded-xl text-base/500 "Continue with Google" → `lovable.auth.signInWithOAuth("google", { redirect_uri: origin })`.
- **Mode toggle:** footer text-sm muted-fg mt-8 "New here? Create account" / "Already have an account? Sign in" (button 600 text-primary). Below: `<Link to="/">Back to app</Link>` (text-xs mt-4).

## Partner Invite — route `/invite/$code` (standalone)
`min-h-screen flex-col items-center justify-center px-6`, bg `--background`, Toaster top-center. **3 states:**
1. **Loading:** Loader2 24px muted-fg spin (centered).
2. **Not signed in:** BrandLogo 32 · H1 "You've been invited" (Outfit 2xl/600 mt-6) · sub "Sign in or create an account to accept this partner invite." (text-sm mt-2) · primary "Sign in to continue" h-12 rounded-xl px-8 mt-6 → Link `/auth`.
3. **Signed in:** BrandLogo 32 · Heart 40px primary mt-8 · H1 "Partner invite" (2xl/600 mt-4).
   - **error branch** (not-found / not-pending / expired / wrong-email): message (text-sm muted-fg max-w-xs mt-3) + outline "Back to app" h-12 rounded-xl px-8 mt-6 → Link `/`.
   - **valid branch:** sub "{inviter} has invited you to share your Genesyx journey." + primary "Accept invite" h-12 rounded-xl px-8 (Loader2) → `acceptPartnerInvite({code})` → toast "You're linked!" → `/` · ghost "Not now" → Link `/` mt-2.
   - **client validation (preview; server re-validates):** invite exists · status pending · `expires_at > now` · `invitee_email === user.email` (case-insensitive).

---

## Modal & Component Deep Dives

**CycleSettingsDialog** (Dialog `sm:max-w-[420px]`) — title "Your cycle" / desc "We use this to predict your phases and fertile window."
- Fields: First day of last period `<Input type="date" max={today} required>` · Cycle length `type=number min21 max35 inputMode=numeric default28` · Period length `type=number min1 max10 default5`.
- Footer: Cancel (bg-muted rounded-xl min-h-44) / Save (bg-primary, Loader2).
- Validation: date regex `^\d{4}-\d{2}-\d{2}$` → "Please pick a valid date"; cycle 21–35 → "Cycle length must be 21–35 days"; period 1–10 → "Period length must be 1–10 days". → `upsertCycleSettings` → toast "Cycle updated". Locked while saving.

**PhLogDialog** (Dialog `sm:max-w-[400px]`) — title "Log/Edit pH reading" / desc "Track your urine pH from 4.5 to 9.0."
- Big value tile (rounded-2xl bg-card p-4 gx-soft-shadow, center): Outfit text-5xl/600 tabular-nums colored by status; status pill rounded-full px-2.5 py-1 11px uppercase, bg `color-mix(status 18%, white)`.
- Slider row: Minus 44×44 rounded-full bg-muted `active:scale-95` · shadcn Slider min4.5 max9.0 step0.1 · Plus 44×44 (clamp+round 0.1).
- "When" datetime-local Input (label 12px muted-fg) · Notes Textarea (optional).
- Footer: Save (Loader2); editing also Delete (Trash2 destructive, Loader2). → `create/update/deletePhReading` → toasts "pH logged"/"Reading updated"/"Reading deleted".

**PartnerSection** (inline in Profile) — eyebrow "Partner" (12px uppercase muted-fg); card rounded-2xl bg-card gx-soft-shadow p-5 space-y-4; loading Loader2.
- signed-out: Heart 24px + "Add your partner" 14/600 + sub 12.5 + "Sign in" → `/auth`.
- linked: avatar 44×44 lavender→pink gradient + initial; name + "Linked partner"; "Remove" text-destructive → `unlinkPartner` (after `confirm()`).
- unlinked signed-in: Heart + "Add your partner" + sub; Email Input (h-11 rounded-xl type=email maxLength255 "partner@example.com"); "Send invite" primary h-11 rounded-xl + Mail (Zod email; "You can't invite yourself" guard) → clear + toast "Invite created — copy the link to share". Pending list (border-t pt-3 space-y-2, eyebrow "Pending invites" 11/600 uppercase): email truncated + Copy (clipboard `${origin}/invite/${code}`) + Revoke (X destructive).

**BottomTabBar** — `absolute bottom-0 inset-x-0 border-t border-border/60 bg-card/95 backdrop-blur-xl`; pad bottom `max(env(safe-area-inset-bottom),12px)`. 5 equal-flex columns; each button min-h-52 rounded-xl py-1 flex-col gap-1. Icons (lucide) Home/CalendarDays/Leaf/BarChart3/User 20px, active `stroke-[2.4]`. Label 11px/500 active→primary, inactive→muted-fg (hover fg). `aria-current="page"` + `aria-label` per tab.

**AppShell** — outer `min-h-screen` bg `oklch(0.93 0.005 280)`/black, centered, `p-0` mobile / `sm:p-8`. Frame `max-w-[420px] bg-background overflow-hidden`, `h-[100dvh]` / `sm:h-[860px] sm:rounded-[48px]`, desktop shadow `0 40px 100px -30px rgba(20,20,40,.35), 0 0 0 1px rgba(0,0,0,.06)`. Scroll `gx-scroll h-full overflow-y-auto`, top pad `max(env(safe-area-inset-top),12px)`; `pb-28` when tabBar present. `ThemeToggle` pinned top-right z-50 when enabled.

**ThemeToggle** — round 36–40px bg-card gx-hairline icon button; Sun/Moon swap; persists theme; respects system default.

> **Insights / Profile / Log / Pregnancy:** detailed element maps in [`UIUX_SPEC.md`](UIUX_SPEC.md) §9 (Insights: pH summary + 3 mock charts; Profile: user card, focus toggle, PartnerSection, Account/Tracking/Prefs/About groups, sign-out, delete; Log: mood/energy/symptoms + Sleep/Water/Supplements mini-cards + notes; Pregnancy: BrandOrb + 2 feature cards + 2 CTAs). Request Lovable Part-3 batch-3 if pixel-level is needed.
