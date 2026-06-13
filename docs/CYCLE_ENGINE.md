# Genesyx — Cycle Engine, pH Logic & Static Content (port verbatim)

> The business logic + content to reimplement **exactly** in Kotlin. Source: `src/lib/cycle.ts`,
> `src/lib/cycleEngine.ts`, `src/hooks/use-ph.ts`, `src/components/genesyx/mockData.ts`.
> Port into `domain/usecase` + `domain/model` and **unit-test against these formulas** (watch DST / cycle boundaries).

## Cycle Math (`cycle.ts`)

**Types**
- `Phase = period | follicular | ovulatory | luteal`
- `DayType = period | follicular | fertile | ovulation | luteal` (calendar legend = Phase + `fertile` + `ovulation`)
- `CyclePhaseInfo = { dayOfCycle, phase, fertileWindow, ovulationDay, daysUntilNextPeriod }`
- `CalendarCell = { kind: "empty" } | { kind: "day", date, info, isToday }`

**Date helpers (TZ-safe — use local startOfDay, NOT UTC):**
- `parseDateOnly(v)` — Date or `"YYYY-MM-DD"` parsed as **local** (avoids UTC shift bug).
- `daysBetween(origin, target)` — whole-day diff. `formatDateOnly(d)` — local `YYYY-MM-DD`.

**`getCyclePhase(lastPeriodDate, cycleLength, periodLength, target = now)`:**
```
diff        = daysBetween(lastPeriodDate, target)
dayOfCycle  = ((diff % cycleLength) + cycleLength) % cycleLength + 1   // 1-based, handles negatives
ovulationDay = cycleLength - 14                                        // luteal fixed at 14d
fertileWindow = [ovulationDay - 5, ovulationDay + 1]  inclusive       // 7 days
daysUntilNextPeriod = cycleLength - dayOfCycle   (0 at end)

phase:
  dayOfCycle <= periodLength        → period
  dayOfCycle == ovulationDay        → ovulatory
  dayOfCycle <  ovulationDay        → follicular
  else                              → luteal
```
- `dayTypeFor(info)` — calendar mapping, priority **ovulation > fertile > phase**.
- `cycleNumberFor(...)` — `floor(diff / cycleLength) + 1`.
- `buildMonthGrid(monthAnchor, lastPeriod, cycleLen, periodLen)` — **Sunday-first**; leading empties = `first.getDay()`; trailing empties pad to multiple of 7.

**Defaults / ranges:** cycleLength default **28** (range 21–35); periodLength default **5** (range 1–10).

## Engine facade (`cycleEngine.ts`) — fertile-window overrides
- `getPhaseSubLabel(phase, fertile)` → `"Fertile window"` overrides label.
- `getPhaseHeroText(...)` → `"Fertile window is open"` overrides non-ovulatory phases.
- `getPhaseHeroSubtext(...)` → fertile copy: _"Conception chances are rising — stay hydrated and prioritise rest."_
- `getPhaseTags(...)` → prepends `"Fertile window"` to base tags.
- `getTodaysFocus(phase)` → `{ title, description }`.

## pH Logic (`use-ph.ts`)
```
phStatus(v):  v < 6.0 → acidic | v > 7.5 → alkaline | else → optimal
PH_STATUS_LABEL: { acidic:"Acidic", optimal:"Optimal", alkaline:"Alkaline" }
PH_STATUS_COLOR:
  acidic   → #D85A8A   (var --color-electric-pink)
  optimal  → #3FA37A   (hardcoded green — the only hardcoded color outside the root shell)
  alkaline → #4D4DAA   (var --color-electric-lavender)
```
- Reading range in UI: slider **4.5–9.0 step 0.1** (PhLogDialog says 4.5; some code paths reference 4.0 — use **4.5–9.0**, clamp+round to 0.1). DB constraint `ph_value` 4.5–9.0.
- Legend bands: Acidic `<6.0` · Optimal `6.0–7.5` · Alkaline `>7.5`.

## Static content (`cycle.ts` maps) — to bundle as resources/strings

**`phaseLabel`:** period "Period" · follicular "Follicular Phase" · ovulatory "Ovulatory Phase" · luteal "Luteal Phase".

**`phaseHeroCopy[phase]` = `{ hero, sub, tags[], focus:{title,body} }`:**
- **period** — hero "Rest and replenish your body" · tags `[Low estrogen, Restore iron]` · focus "Add a warm iron-rich meal"
- **follicular** — hero "Building energy for ovulation" · tags `[Estrogen rising, Building energy]` · focus "Add 2 cups of leafy greens"
- **ovulatory** — hero "High chance of conception today" · tags `[High estrogen, Peak energy]` · focus "Hydrate and prioritise protein"
- **luteal** — hero "Slow down and nourish" · tags `[Progesterone rising, Lower energy]` · focus "Try a magnesium-rich snack"

**`phaseFoods[phase]`** — 4 `{title, desc}` each (e.g. period: Lentils & beans · Dark leafy greens · Bone broth · Dark chocolate). _(Nutrition screen food-bullet accents: period `#F48FB1`, follicular `#A5D6A7`, ovulatory `#CE93D8`, luteal `#B39DDB`.)_

## Mock / seed data (`mockData.ts`)

**`quizQuestions` (5, `as const`):**
1. **stage** — "Where are you in your conception journey?" → exploring / preparing / trying / support
2. **cycle** — "How regular does your cycle usually feel?" → very / mostly / irregular / unsure · **fact:** _"Only about 13% of cycles are exactly 28 days. A healthy cycle can range from 21 to 35 days…"_
3. **supplements** — "Are you currently taking fertility supplements?" → yes / some / no / guidance
4. **gender** — "Do you have a gender preference for your baby?" → girl / boy / either / surprise · **fact:** _"Research suggests that timing, diet, and even pH balance can subtly influence…"_
5. **support** — "What would you like the most support with?" → nutrition / tracking / supplements / emotional

**`symptoms` (8):** Headache, Fatigue, Cramps, Nausea, Bloating, Acne, Backache, Tender breasts.
**`nutritionFocus` (4, tone):** Leafy greens (lavender), Complex carbs (blue), Omega-rich foods (pink), Zinc-rich foods (lavender).
**`articles` (3):** "Eating for your luteal phase" 4 min · "How hydration shapes fertility" 3 min · "A gentle guide to supplements" 6 min.
**`profileMenu`:** account → [Personal Details, Health Profile, Tracking Preferences]; about → [Privacy & Data, Help & Support].
**`cycleDays` (28-day viz):** days 1–5 period · 11–16 fertile (day 14 ovulation) · 17–28 luteal · rest follicular.
**Charts:** `insightBars = [82,78,90,85,88,80,92]` · `nutritionBars = [60,75,70,85,78,90,82]` (Insights — currently mock).

## Daily-log helper
- `todayISO(d=new Date())` — **local-tz** `YYYY-MM-DD`. Used as the `daily_logs.date` key (UNIQUE per user+date). Native must use device-local date, not UTC.
