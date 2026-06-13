# Genesyx — Design Tokens & Component Styles

> The theming contract for the native rebuild (Part 2 of the Lovable extraction).
> Source of truth = `src/styles.css` (oklch). Hex values are converted equivalents —
> **convert oklch → ARGB once** and store as Compose `Color(0xFF…)`; don't compute oklch at runtime.
> High-level: [`../ARCHITECTURE.md`](../ARCHITECTURE.md) · Screens: [`UIUX_SPEC.md`](UIUX_SPEC.md), [`SCREEN_LAYOUTS.md`](SCREEN_LAYOUTS.md).

## Colors — Light

| Role | Hex (≈ from oklch) | Use |
|---|---|---|
| primary | `#4D4DAA` (electric-lavender) | buttons, links, active tab, accents, focus ring |
| primary-hover | `rgba(77,77,170,0.9)` (`bg-primary/90`) | primary button hover |
| primary-active | `#4D4DAA` (`/80` on press) | pressed |
| secondary | `~#F2EFF6` | secondary buttons, chips |
| background (zenith) | `#F2F2F2` | page background |
| surface / card | `#FFFFFF` | cards, panels, bottom tab bar |
| surface-elevated | `#FFFFFF` | popover, dialog, dropdown |
| border | `~#E6E4EC` (oklch 0.91 .005 285) | dividers, hairlines, input borders |
| input bg | transparent | input fill |
| text-primary (fg) | `#1F1F1F` (oklch 0.13) | body + headings |
| text-secondary (muted-fg) | `~#6B6878` | subtitles, captions, helpers, placeholders |
| text-disabled | fg @ opacity-50 | disabled |
| error (destructive) | `~#D93636` (oklch 0.6 .2 25) | validation errors, destructive |
| muted | `~#EEEBF1` | muted surfaces, skeletons |
| ring | `#4D4DAA` | focus ring |
| success / warning | _not tokenized_ | success uses electric/powder-blue locally |

**Brand palette (utility tokens):** zenith `#F2F2F2` · electric-lavender `#4D4DAA` · powder-blue/baby-blue `#8DD2E2` · powder-pink `#DDA4D3` · electric-blue `#57A1CE` (pH alkaline) · baby-lavender `#8888D3` · electric-pink `#C782D8` · baby-pink `#DEBED2`.

**Feature-specific (hardcoded):**
- Nutrition food-bullet accents: period `#F48FB1` · follicular `#A5D6A7` · ovulatory `#CE93D8` · luteal `#B39DDB`
- pH status colors live in `src/hooks/use-ph.ts` (acidic/optimal/alkaline) — _still to capture exact hex_; electric-blue `#57A1CE` is the alkaline tint.
- Symptom heatmap: `color-mix(in oklab, electric-lavender X%, white)` at 5/15/30/50% → **pre-compute per tint**, don't color-mix at runtime.

## Colors — Dark (`.dark`, implemented; `ThemeToggle` in AppShell top-right)

| Role | Value |
|---|---|
| background | `#000000` |
| foreground | `#FFFFFF` |
| card / popover | `~#1F1F1F` (oklch 0.14) |
| primary | `~#9B7BD8` (oklch 0.62 .16 285) — brighter lavender |
| primary-foreground | `#000000` |
| secondary / accent / muted | `~#2A2730` (oklch 0.18–0.20) |
| muted-foreground | `~#B8B5C4` (oklch 0.72) |
| border | `rgba(255,255,255,0.10)` |
| input | `rgba(255,255,255,0.14)` |
| ring | `~#9B7BD8` |
| destructive | `~#E0463A` |

Brand palette tokens are **reused unchanged** in dark mode (not redefined).

## Typography

- **font-display:** `"Outfit"` (variable 100–900), `@font-face` from gstatic → **bundle the .ttf as an asset**, don't fetch at runtime. Tracking `-0.025em`.
- **font-body:** `"Inter", "Vend Sans", system-ui` (Inter not actually loaded — host fallback). Tracking `-0.005em`.

| Role | Size | Weight | Family / notes |
|---|---|---|---|
| h1 large header | `text-3xl` 30px | 600 | Outfit, `-0.025em` |
| h1 default header | `text-xl` 20px | 600 | Outfit |
| h1 greeting/hero | 26px | 600 | leading-tight |
| h2 section card title | 16–18px | 600 | Outfit |
| h3 subsection | 15px | 600 | |
| body | 16px (inputs) / 14px (prose) | 400 | line-height ~1.5 |
| small / caption | 13–13.5px | 400 | muted-fg |
| micro / metadata | 11.5–12px | 400 | |
| label / eyebrow | 11px | 500 | UPPERCASE, tracking 0.14–0.22em |
| button default | 14px | 500 | no transform |
| button lg primary | 16px | 600 | |
| badge | 12px | 600 | |
| tab label | 11px | 500 | |

## Spacing (4px base)
xs 4 (`p-1`) · sm 8 (`p-2`) · md 12–16 (`p-3/4`) · lg 20 (`p-5`, most cards) · xl 24 (`p-6`, screen edges/auth) · 2xl 28–32 (section gaps).
Screen horizontal: `px-5` standard, `px-6` hero/auth/onboarding. Vertical rhythm between cards: `space-y-3/4`.

## Border Radius (base `--radius: 1rem` = 16px)
button default `rounded-md` 6 · **CTA/pill `rounded-2xl` 16 / `rounded-full`** · input `rounded-md` 6 · shadcn card `rounded-xl` 12 · **Genesyx card `rounded-[28px]` / `rounded-3xl` 24** · badge `rounded-md` 6 (brand chips `rounded-full`) · avatar `rounded-full` · Dialog `rounded-lg` 8 · bottom sheet `rounded-t-[28px]` · tooltip `rounded-md` 6 · icon tile `rounded-2xl` 16 · pill segment `rounded-xl` 12 inside `rounded-2xl` track · phone frame `rounded-[48px]` (desktop only — drop on native).

## Shadows
```
gx-card-shadow:  0 0 0 .5px rgba(0,0,0,.04), 0 1px 2px rgba(0,0,0,.025), 0 6px 18px -10px rgba(20,20,40,.06)
gx-soft-shadow:  0 0 0 .5px rgba(0,0,0,.04)
gx-hairline:     0 0 0 .5px rgba(0,0,0,.06)
modal/dialog:    shadow-lg  (0 10px 15px -3px rgba(0,0,0,.1), 0 4px 6px -4px rgba(0,0,0,.1))
dropdown/popover:shadow-md
button primary:  0 1px 3px rgba(0,0,0,.1), 0 1px 2px -1px rgba(0,0,0,.1)   (outline/secondary: shadow-sm; ghost/link: none)
phone frame:     0 40px 100px -30px rgba(20,20,40,.35), 0 0 0 1px rgba(0,0,0,.06)   (desktop only)
dark card:       inset 0 0 0 .5px rgba(255,255,255,.08), 0 1px 2px rgba(0,0,0,.4), 0 10px 24px -14px rgba(0,0,0,.6)
gx-orb:          0 24px 50px -24px color-mix(electric-lavender 28%, transparent),
                 inset -8px -14px 32px color-mix(powder-pink 22%, transparent),
                 inset 6px 8px 22px color-mix(white 80%, transparent)
splash CTA glow: 0 10px 30px -12px rgba(77,77,170,.55)
eBook card:      0 30px 60px -22px rgba(77,77,170,.45), 0 0 0 1px rgba(0,0,0,.05)
```

## Transitions & Animations
- `.gx-screen` mount: fade + 8px translateY-up, **320ms**, cubic-bezier(.22,1,.36,1) — on route/flow change.
- Skeleton shimmer: translateX(-100%→100%), **1.6s** linear infinite, on loading cards.
- `gx-float` (orb/eggs): translateY/X ±12/6px, 10–14s ease-in-out infinite, staggered delays — decorative.
- Tab icon/text color ~150ms; button bg hover (shadcn default); range pill `transition-all` 150ms; Dialog fade+zoom 150ms; Sheet slide 300–500ms; Tabs/Switch/Progress shadcn defaults.

## Component Styles

**Primary Button** — bg `--primary` `#4D4DAA`, text near-white, no border; radius `rounded-md` (hero CTAs `rounded-2xl`, inline "Log pH" `rounded-full`); sizes default `h-9 px-4 py-2`, lg `h-10 px-8`, hero `h-14 w-full`; font 14/500 (hero 16/600); `shadow`; hover `bg-primary/90`; disabled opacity-50 + pointer-events-none.
**Secondary** — bg `~#F2EFF6`, deep-lavender text, `rounded-md`, `shadow-sm`, hover `/80`.
**Ghost/Link** — transparent; ghost text fg + hover `bg-accent`; link text primary + hover underline; no shadow.
**Outline** — bg `--background`, 1px `--input` border, `rounded-md`, `shadow-sm`, hover `bg-accent`.
**Input** — transparent bg, 1px `--input` border `~#E6E4EC`, `rounded-md`, `h-9`, `px-3 py-1`, text 16/md:14 fg, placeholder muted-fg, focus `ring-1 ring-ring #4D4DAA`, no built-in error variant (use `border-destructive`), `shadow-sm`, disabled opacity-50.
**Card** — bg `--card`, 1px `--border`, shadcn `rounded-xl` (Genesyx wraps `rounded-[28px]`), `shadow`/`gx-card-shadow`, header+content `p-6` (brand cards `p-5`).
**Badge** — variant bg (primary/secondary/destructive/outline), `rounded-md`, `px-2.5 py-0.5`, 12/600; brand variant `rounded-full px-3 py-1` 11px uppercase tracking-wider, bg `color-mix(lavender 10%, white)` text primary.
**Dialog** — bg `--background`, overlay `bg-black/80`, `rounded-lg`, `p-6`, `shadow-lg`, max-w `lg` (32rem), fade+zoom 95→100 150ms, close X top-4 right-4.
**Sheet (bottom)** — bg `--background`, overlay `bg-black/80`, `rounded-t-[28px]`, `p-6`, `shadow-lg`, slide-from-bottom 500ms / out 300ms. Used for Log, PhLog, CycleSettings.
**Toast (Sonner)** — bg popover/background, fg text, 1px border, `rounded-md`, `shadow-lg`, position bottom-right (web) → native should anchor **bottom-center above the nav bar** (Material Snackbar).
