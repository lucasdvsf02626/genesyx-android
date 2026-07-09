# Learn / Blog — iOS Parity Handoff (source of truth)

**Source:** `genesyx-android`, branch `feature/learn-section`, HEAD `659de4d`.
**Extracted:** 2026-07-09. **No code was changed to produce this document.**
**Companion payload:** [`docs/learn/articles.json`](learn/articles.json) — machine-readable, extracted
programmatically from `LearnContent.kt` with a round-trip check (every emitted character traced back to
a source string literal). **Prefer the JSON over this markdown when copying content.**

Confirmed = read from code. **[INFERRED]** = deduced, marked in place.

---

## A. Executive summary

**Status: production-ready for the scope it covers; MVP-limited as a feature.** Everything visible
works and is backed by real, hand-written content. Nothing is faked, stubbed, or lorem. There are
**zero `TODO`/`FIXME` markers** in the Learn sources (verified). But the feature is deliberately narrow:
no bookmarks, no reading progress, no deep links, no backend.

**Recent work:** the whole feature was built today in one commit, `659de4d` (26 files, +2598/−53). It
replaced a stub in which three "Learn more" tiles on the Nutrition screen all opened the *same
hardcoded paragraph* in an `AlertDialog`.

**Confirmed for your iOS assumptions — all four hold:**

| Claim | Verdict |
|---|---|
| Learn uses **no backend** | ✅ Confirmed. No Supabase table, no `articles` in `docs/schema.sql`. |
| Learn uses **no network** | ✅ Confirmed. Zero `http` refs; no image loader in the project. |
| Learn uses **no Room** | ✅ Confirmed. No entity, DAO, DTO, or migration. |
| Learn has **no repository-persisted article state** | ✅ Confirmed. `grep` for `bookmark\|isBookmarked\|savedArticle\|completion` → zero hits. |
| **Only** persisted state is `learn_intro_seen` | ✅ Confirmed. DataStore boolean. Nothing else. |

**"Blog" is not a separate feature.** The word `blog` appears exactly once in the entire tracked
codebase — in a comment in `core/AppLinks.kt:10` explaining why the share sheet does *not* link to
`/blog/{slug}`. There is no blog screen, route, model, or content. Learn *is* the blog. Moving on.

**Top risks / gaps (specific):**

1. **The banned-phrase test does not scan `tags`.** It concatenates `title + excerpt + body` only. A
   prohibited term in a tag ships silently. See §B.4.
2. **The disclaimer test only covers `NUTRITION` and `WELLNESS`.** Articles `a8` and `a10` are
   `INSIGHTS` and set `disclaimerRequired = true`, but **nothing enforces that**. Someone can delete
   those flags and the build stays green.
3. **`CtaType.OPEN_ARTICLE` is a latent crash.** `ArticleCta.route()` calls
   `requireNotNull(targetSlug)`. No article uses `OPEN_ARTICLE` today, so it never fires — but the
   enum case is reachable from content. Dead control, unsafe if used.
4. **`tags` are never rendered.** Searched only. Not a bug; know it before you build a tag UI.
5. **Share sends the site root**, not a per-article URL. `https://genesyx.co.uk/blog/{slug}` is not
   confirmed to exist.
6. **Slugs are a permanent public contract.** They are route keys now and share/deep-link keys later.
   If iOS invents its own, cross-platform links break forever.

---

## B. Content payload

### B.1 Where the content lives (confirmed)

**`app/src/main/kotlin/com/genesyx/app/domain/content/LearnContent.kt`** — a single Kotlin file, ~430
lines. Content is a top-level `val learnArticles: List<Article>` initialised at class-load. No JSON, no
markdown, no assets, no seed file, no admin tool, no network. This matches the pre-existing convention
of `CycleContent.kt`, `NutritionContent.kt`, `QuizContent.kt`.

Model, verbatim:

```kotlin
data class Article(
    val id: String,
    /** Stable — used in the `learn/article/{slug}` route. Never change one after release. */
    val slug: String,
    val title: String,
    val excerpt: String,
    val body: List<ArticleBlock>,
    val category: ArticleCategory,
    val tags: List<String>,
    val readingTime: String,
    @DrawableRes val heroImage: Int? = null,
    val featured: Boolean = false,
    val relatedArticleIds: List<String> = emptyList(),
    val cta: ArticleCta? = null,
    /** Renders the medical disclaimer above the footer. */
    val disclaimerRequired: Boolean = false,
)

sealed interface ArticleBlock {
    data class Heading(val text: String) : ArticleBlock
    data class Paragraph(val text: String) : ArticleBlock
    data class BulletList(val items: List<String>) : ArticleBlock
    data class Callout(val text: String) : ArticleBlock
}

enum class CtaType { OPEN_LOG, OPEN_TRACK, OPEN_NUTRITION, OPEN_INSIGHTS, OPEN_ARTICLE }

data class ArticleCta(val type: CtaType, val label: String, val targetSlug: String? = null)
```

### B.2 The five categories (confirmed)

Declaration order in `enum class ArticleCategory(val label: String)`. **This order drives the filter
chip row** (`ArticleCategory.entries`), preceded by an "All" chip.

| # | id (enum case) | label (user-visible) | Articles | Accent colour | Colour source |
|---|---|---|---|---|---|
| 1 | `GETTING_STARTED` | `Getting started` | 1 | `ElectricBlue` `#57A1CE` | `ui/theme/Color.kt:12` |
| 2 | `TRACKING` | `Tracking` | 2 | `ElectricBlue` `#57A1CE` | " |
| 3 | `NUTRITION` | `Nutrition` | 3 | `ElectricLavender` `#4D4DAA` | `Color.kt:7` |
| 4 | `INSIGHTS` | `Insights` | 3 | `ElectricPink` `#C782D8` | `Color.kt:14` |
| 5 | `WELLNESS` | `Wellness` | 1 | `ElectricLavender` `#4D4DAA` | " |

There is **no icon** per category. The accent is used for the eyebrow text colour, the selected-chip
tint, and the gradient fallback. Note `GETTING_STARTED` and `TRACKING` share a colour, as do
`NUTRITION` and `WELLNESS` — two pairs are visually indistinguishable. **[Confirmed, and probably
unintentional.]**

Accent map, verbatim (`ui/learn/LearnScreen.kt`):

```kotlin
internal fun ArticleCategory.accent(): Color = when (this) {
    ArticleCategory.GETTING_STARTED -> ElectricBlue
    ArticleCategory.NUTRITION -> ElectricLavender
    ArticleCategory.TRACKING -> ElectricBlue
    ArticleCategory.INSIGHTS -> ElectricPink
    ArticleCategory.WELLNESS -> ElectricLavender
}
```

### B.3 The ten articles, verbatim

> Rendered from `docs/learn/articles.json`, which was extracted mechanically from `LearnContent.kt` and
> round-trip verified (10 articles, 84 body blocks, 10,246 body characters; every string traced to a
> source literal). **Paste from the JSON, not from this markdown** — the JSON is the machine truth and
> has no wrapping artefacts.
>
> Rendering order in the UI is **declaration order** = the order below. There is no sort key.

#### a1 — Your first week with Genesyx

| field | value |
|---|---|
| `id` | `a1` |
| `slug` | `getting-started-first-week` |
| `title` | Your first week with Genesyx |
| `excerpt` | What to do on day one, day three, and day seven — and what to ignore until later. |
| `category` | `GETTING_STARTED` (Getting started) |
| `tags` | `onboarding`, `basics`, `getting started` |
| `readingTime` | `5 min read` |
| `heroImage` | `R.drawable.learn_hero_first_week` → `res/drawable-nodpi/learn_hero_first_week.jpg` |
| `featured` | `true` |
| `disclaimerRequired` | `false` |
| `relatedArticleIds` | `a2`, `a9` |
| `cta` | type=`OPEN_LOG`, label=“Open today's log” |

**Body** — 10 blocks, in order:

1. **`Paragraph`** — The first week isn't about completeness. It's about proving to yourself that this takes a minute, not an evening.
2. **`Heading`** — Day one
3. **`Paragraph`** — Set your cycle, then log once. That's it. The log can be almost empty — a mood and nothing else is a real entry.
4. **`Heading`** — Days two and three
5. **`Paragraph`** — Notice how little time it takes. Most people spend under thirty seconds. If you're spending longer, you're logging more than you need to.
6. **`Heading`** — Day seven
7. **`Paragraph`** — Your first patterns appear. They will also be mostly noise — seven days is not enough to conclude anything, and the app will say so. Look anyway. Seeing your own week written down is the point.
8. **`Heading`** — What to ignore for now
9. **`BulletList`** —
    - Perfect streaks. Missing a day costs you almost nothing.
    - Every optional field. Fill in what you notice, skip what you don't.
    - Comparing your numbers to anyone else's.
10. **`Callout`** — If you only do one thing this week: log on the day you feel worst. That's the entry you'll be glad to have in three months.

---

#### a2 — Why logging beats remembering

| field | value |
|---|---|
| `id` | `a2` |
| `slug` | `why-logging-beats-remembering` |
| `title` | Why logging beats remembering |
| `excerpt` | Memory rewrites the past to fit the present. A log doesn't. |
| `category` | `TRACKING` (Tracking) |
| `tags` | `tracking`, `habits`, `basics`, `memory` |
| `readingTime` | `4 min read` |
| `heroImage` | `R.drawable.learn_hero_logging` → `res/drawable-nodpi/learn_hero_logging.jpg` |
| `featured` | `false` |
| `disclaimerRequired` | `false` |
| `relatedArticleIds` | `a3`, `a8` |
| `cta` | type=`OPEN_LOG`, label=“Log how you feel today” |

**Body** — 7 blocks, in order:

1. **`Paragraph`** — Memory is reconstructive. You don't replay the past — you rebuild it, every time, out of whatever material is nearest to hand. And the nearest material is how you feel right now.
2. **`Paragraph`** — This is why "I felt terrible all week" is so often untrue. You felt terrible today, and today has quietly colonised the other six days.
3. **`Heading`** — What a log actually does
4. **`Paragraph`** — It's not a diary and it's not a report card. It's a set of small, boring, unrevised facts written by someone who didn't yet know how the story ended — you, on a Tuesday.
5. **`Heading`** — Boring entries beat detailed ones
6. **`BulletList`** —
    - A one-tap mood, logged for sixty days, is worth more than a paragraph logged four times.
    - The entries you'll want later are the ordinary ones. Extremes you remember anyway.
    - "Nothing to report" is data. Log it.
7. **`Callout`** — The goal isn't a complete record. It's a record honest enough to disagree with you.

---

#### a3 — Symptoms and meals: what's worth writing down

| field | value |
|---|---|
| `id` | `a3` |
| `slug` | `what-to-log` |
| `title` | Symptoms and meals: what's worth writing down |
| `excerpt` | You don't need to log everything. Here's the short list. |
| `category` | `TRACKING` (Tracking) |
| `tags` | `tracking`, `symptoms`, `nutrition`, `logging` |
| `readingTime` | `4 min read` |
| `heroImage` | `R.drawable.learn_hero_what_to_log` → `res/drawable-nodpi/learn_hero_what_to_log.jpg` |
| `featured` | `false` |
| `disclaimerRequired` | `false` |
| `relatedArticleIds` | `a2`, `a9` |
| `cta` | type=`OPEN_LOG`, label=“Open today's log” |

**Body** — 8 blocks, in order:

1. **`Paragraph`** — The completeness trap goes like this: you decide to track properly, you build an elaborate routine, you sustain it for nine days, and then you stop entirely. A smaller habit you keep beats a thorough one you abandon.
2. **`Heading`** — Worth logging daily
3. **`BulletList`** —
    - Mood — one tap, no elaboration.
    - Energy — the thing you'll most want to correlate later.
    - Anything unusual. If you noticed it, write it.
    - Whether you ate roughly normally. Not what. Whether.
4. **`Heading`** — Worth logging weekly
5. **`BulletList`** —
    - Sleep, in broad strokes — good week, bad week.
    - Anything that changed: new medication, travel, illness, unusual stress.
6. **`Heading`** — Not worth logging at all
7. **`Paragraph`** — Anything you'd only be recording to feel virtuous about recording it. If you can't imagine a question it would help answer, skip it.
8. **`Callout`** — Consistency beats detail. It isn't close.

---

#### a4 — Hydration, without the eight-glass myth

| field | value |
|---|---|
| `id` | `a4` |
| `slug` | `hydration-basics` |
| `title` | Hydration, without the eight-glass myth |
| `excerpt` | Where "eight glasses a day" came from, and what to do instead. |
| `category` | `NUTRITION` (Nutrition) |
| `tags` | `hydration`, `nutrition`, `myths`, `water` |
| `readingTime` | `3 min read` |
| `heroImage` | `R.drawable.learn_hero_hydration` → `res/drawable-nodpi/learn_hero_hydration.jpg` |
| `featured` | `false` |
| `disclaimerRequired` | `true` |
| `relatedArticleIds` | `a5`, `a9` |
| `cta` | type=`OPEN_TRACK`, label=“Track today's water” |

**Body** — 6 blocks, in order:

1. **`Paragraph`** — The eight-glasses rule appears to trace back to a 1945 recommendation that adults need roughly 2.5 litres of water a day. The very next sentence noted that most of it already comes from food. That sentence got dropped, and the number outlived it.
2. **`Heading`** — What's actually true
3. **`BulletList`** —
    - For most healthy adults, thirst is a reasonable guide.
    - Food contributes a real share of your water. Fruit, vegetables, soup, and yoghurt all count.
    - Tea and coffee count too. The idea that they dehydrate you doesn't survive contact with the evidence at normal intake.
    - Needs vary with heat, altitude, exercise, illness, and pregnancy. A fixed target can't account for any of that.
4. **`Heading`** — Something more useful than a number
5. **`Paragraph`** — Anchor water to things you already do. A glass with each meal. A glass when you sit down at your desk. This works because it removes the decision, and decisions are what habits die of.
6. **`Callout`** — Persistent, unusual thirst — especially alongside fatigue or frequent urination — is worth mentioning to a doctor. It's not something to solve by drinking more.

---

#### a5 — Eating with your cycle, not against it

| field | value |
|---|---|
| `id` | `a5` |
| `slug` | `eating-with-your-cycle` |
| `title` | Eating with your cycle, not against it |
| `excerpt` | What changes across the four phases — and what genuinely doesn't. |
| `category` | `NUTRITION` (Nutrition) |
| `tags` | `nutrition`, `cycle`, `phases`, `food` |
| `readingTime` | `6 min read` |
| `heroImage` | `R.drawable.learn_hero_eating_cycle` → `res/drawable-nodpi/learn_hero_eating_cycle.jpg` |
| `featured` | `false` |
| `disclaimerRequired` | `true` |
| `relatedArticleIds` | `a4`, `a6` |
| `cta` | type=`OPEN_NUTRITION`, label=“See this phase's foods” |

**Body** — 10 blocks, in order:

1. **`Paragraph`** — Your appetite, energy, and cravings genuinely do shift across a cycle. That much is well established. What's less well established is almost everything else you'll read about it online.
2. **`Paragraph`** — So here's an honest version: a short list of what actually changes, a shorter list of what to do about it, and a clear note about the claims that don't hold up.
3. **`Heading`** — What actually shifts
4. **`BulletList`** —
    - Iron. Menstruation loses iron. Foods rich in it — lentils, dark leafy greens, red meat — are worth leaning on during your period.
    - Energy. Many people feel steadier in the follicular phase and flatter in the late luteal phase. Eating regularly matters more than eating perfectly.
    - Appetite. A modest increase in the luteal phase is normal and well documented. It is not a failure of discipline.
5. **`Heading`** — What doesn't
6. **`Paragraph`** — Your cycle length, your ovulation timing, and the sex of any future child are not influenced by what you eat. No food, supplement, or eating pattern changes them. Claims otherwise are common, confidently stated, and unsupported.
7. **`Callout`** — If an article promises that a food will change your cycle or influence conception, that's the moment to close the tab.
8. **`Heading`** — A practical idea per phase
9. **`BulletList`** —
    - Period — iron with a source of vitamin C alongside it, which helps absorption.
    - Follicular — you likely have energy. This is a good week for meals that take effort.
    - Ovulatory — nothing special is required. Keep eating.
    - Luteal — more frequent, smaller meals help some people with energy dips. Try it, keep it if it works, drop it if it doesn't.
10. **`Paragraph`** — That last instruction applies to this whole article. Log how you feel, look back after a couple of cycles, and keep only what your own data supports.

---

#### a6 — A gentle guide to supplements

| field | value |
|---|---|
| `id` | `a6` |
| `slug` | `gentle-guide-supplements` |
| `title` | A gentle guide to supplements |
| `excerpt` | What the evidence supports, what it doesn't, and why to talk to someone first. |
| `category` | `NUTRITION` (Nutrition) |
| `tags` | `supplements`, `nutrition`, `folate`, `evidence` |
| `readingTime` | `6 min read` |
| `heroImage` | `R.drawable.learn_hero_supplements` → `res/drawable-nodpi/learn_hero_supplements.jpg` |
| `featured` | `false` |
| `disclaimerRequired` | `true` |
| `relatedArticleIds` | `a5`, `a4` |
| `cta` | type=`OPEN_LOG`, label=“Log how you're feeling” |

**Body** — 11 blocks, in order:

1. **`Paragraph`** — The word supplement is doing a lot of quiet work. It means: in addition to. Not instead of, and not as insurance against, a diet that's already broadly fine.
2. **`Heading`** — The one with strong consensus
3. **`Paragraph`** — Folate — folic acid in its supplement form — is recommended by mainstream health bodies for anyone who might become pregnant, ideally starting before conception. The evidence for reduced neural tube defects is unusually strong. This is the one worth raising with a doctor or pharmacist rather than reading about.
4. **`Heading`** — Everything else: ask, don't assume
5. **`Paragraph`** — Vitamin D, omega-3, zinc, and the rest all have a real evidence base for specific people in specific circumstances — and a much larger marketing base aimed at everyone else. Whether any of them is useful for you depends on your diet, your bloodwork, where you live, and what else you take.
6. **`Paragraph`** — We're deliberately naming no doses and no brands here. A dose that's sensible for one person is pointless or harmful for another, and that judgement needs someone who can see your actual situation.
7. **`Heading`** — Interactions are real
8. **`Paragraph`** — Supplements interact with medication and with each other. Some affect how other things are absorbed. "Natural" carries no information about safety.
9. **`Heading`** — How to raise it
10. **`Paragraph`** — A pharmacist is free to talk to, knows about interactions, and doesn't need an appointment. Bring a list of everything you take, including anything you'd think of as "just a vitamin". That's usually the whole conversation.
11. **`Callout`** — If you're taking prescription medication, check before starting anything new. This is the single highest-value thing in this article.

---

#### a7 — What "insights" actually means

| field | value |
|---|---|
| `id` | `a7` |
| `slug` | `what-insights-mean` |
| `title` | What "insights" actually means |
| `excerpt` | Your app can spot correlations. It cannot spot causes. That distinction is the whole game. |
| `category` | `INSIGHTS` (Insights) |
| `tags` | `insights`, `data`, `basics`, `correlation` |
| `readingTime` | `5 min read` |
| `heroImage` | `R.drawable.learn_hero_insights` → `res/drawable-nodpi/learn_hero_insights.jpg` |
| `featured` | `false` |
| `disclaimerRequired` | `false` |
| `relatedArticleIds` | `a8`, `a10` |
| `cta` | type=`OPEN_INSIGHTS`, label=“See your insights” |

**Body** — 7 blocks, in order:

1. **`Paragraph`** — An insight in this app is a pattern found by comparing things you logged. Nothing more mystical than that, and nothing more authoritative either.
2. **`Heading`** — Correlation, concretely
3. **`Paragraph`** — Suppose your logs show low energy on the days you slept badly. Three explanations fit equally well: bad sleep drained you; whatever drained you also wrecked your sleep; or a third thing — illness, stress, a hard week — caused both. The data cannot distinguish between them. Only you can, and only sometimes.
4. **`Heading`** — How much data is enough
5. **`BulletList`** —
    - Three weeks: almost nothing. Treat every pattern as coincidence.
    - Three months: something. Patterns that survive this long are worth noticing.
    - A year: enough to see how you change across seasons and circumstances.
6. **`Heading`** — Holding an insight lightly
7. **`Paragraph`** — A good insight makes you curious, not certain. If reading one makes you want to change three things at once, that's a sign you've believed it too hard.

---

#### a8 — Reading your trends without over-reading them

| field | value |
|---|---|
| `id` | `a8` |
| `slug` | `reading-your-trends` |
| `title` | Reading your trends without over-reading them |
| `excerpt` | One bad day is noise. Six weeks is a signal. Here's how to tell. |
| `category` | `INSIGHTS` (Insights) |
| `tags` | `insights`, `trends`, `patterns`, `variance` |
| `readingTime` | `5 min read` |
| `heroImage` | `R.drawable.learn_hero_trends` → `res/drawable-nodpi/learn_hero_trends.jpg` |
| `featured` | `false` |
| `disclaimerRequired` | `true` |
| `relatedArticleIds` | `a7`, `a10` |
| `cta` | type=`OPEN_INSIGHTS`, label=“See your insights” |

**Body** — 8 blocks, in order:

1. **`Paragraph`** — Bodies vary. Not as a failure of the body, but as a basic property of it. Two days with identical sleep, food, and stress will still produce different numbers, and nothing has gone wrong.
2. **`Heading`** — What noise looks like
3. **`BulletList`** —
    - A single outlier in an otherwise flat run.
    - A dramatic value on a day something unusual happened.
    - Any pattern you can only see if you squint.
4. **`Heading`** — What a signal looks like
5. **`BulletList`** —
    - It persists across several cycles, not several days.
    - It shows up whether or not you were looking for it.
    - It doesn't disappear when you exclude the most dramatic week.
6. **`Heading`** — The trap
7. **`Paragraph`** — When you feel worse, you track harder. More tracking finds more patterns. More patterns feel like confirmation that something is wrong. This loop is worth knowing about because it feels exactly like diligence.
8. **`Callout`** — A trend that is persistent, new for you, and unexplained is worth raising with a clinician — not worth resolving alone with more data.

---

#### a9 — Small habits that hold

| field | value |
|---|---|
| `id` | `a9` |
| `slug` | `small-habits-that-hold` |
| `title` | Small habits that hold |
| `excerpt` | The habit research says: make it smaller than feels worthwhile. |
| `category` | `WELLNESS` (Wellness) |
| `tags` | `habits`, `wellness`, `consistency`, `routine` |
| `readingTime` | `4 min read` |
| `heroImage` | `R.drawable.learn_hero_habits` → `res/drawable-nodpi/learn_hero_habits.jpg` |
| `featured` | `false` |
| `disclaimerRequired` | `true` |
| `relatedArticleIds` | `a3`, `a10` |
| `cta` | type=`OPEN_LOG`, label=“Start with one log” |

**Body** — 9 blocks, in order:

1. **`Paragraph`** — Ambitious habits fail in week two, and they fail for a structural reason: they were designed by a version of you who had unusual amounts of energy and optimism. That person does not show up on Wednesdays.
2. **`Heading`** — Anchor it to something that already happens
3. **`Paragraph`** — You already brush your teeth. You already sit down at a desk, or on a bus. Attach the new thing to the old thing and you never have to remember it, only notice it.
4. **`Heading`** — The two-minute version
5. **`Paragraph`** — Whatever you want to do, define a version that takes two minutes and do that. Not as a stepping stone to the real habit — as the habit. Scale later, if you want to, or don't.
6. **`Heading`** — Missing a day
7. **`Paragraph`** — Missing one day changes nothing measurable. Missing two is the pattern worth watching, because the second miss is where the habit stops being automatic.
8. **`Heading`** — On streaks
9. **`Paragraph`** — Streaks motivate some people and quietly torment others. If a broken streak makes you want to abandon the whole thing, the streak is costing you more than it's paying. Ignore the number.

---

#### a10 — Using what you learn

| field | value |
|---|---|
| `id` | `a10` |
| `slug` | `using-what-you-learn` |
| `title` | Using what you learn |
| `excerpt` | Data is only useful if it changes something. Here's how to close the loop. |
| `category` | `INSIGHTS` (Insights) |
| `tags` | `insights`, `decisions`, `wellness`, `experiments` |
| `readingTime` | `4 min read` |
| `heroImage` | `R.drawable.learn_hero_using` → `res/drawable-nodpi/learn_hero_using.jpg` |
| `featured` | `false` |
| `disclaimerRequired` | `true` |
| `relatedArticleIds` | `a7`, `a8` |
| `cta` | type=`OPEN_INSIGHTS`, label=“See your insights” |

**Body** — 8 blocks, in order:

1. **`Paragraph`** — Tracking that never changes a decision is a hobby. A pleasant one — but let's be honest about which it is.
2. **`Heading`** — The loop
3. **`BulletList`** —
    - Notice something in your logs.
    - Guess at why, out loud, in one sentence.
    - Change exactly one thing.
    - Wait a full cycle. Not a week.
    - Look again.
4. **`Heading`** — Why one thing
5. **`Paragraph`** — Change three things and you've learned nothing, whatever the result. You won't know which one moved the needle, or whether two of them cancelled out. This is the single most common way people waste months of careful tracking.
6. **`Heading`** — When to stop experimenting
7. **`Paragraph`** — If something is getting worse, or it's new, or it frightens you, stop running experiments and talk to a professional. Your logs will make that conversation better. They are not a substitute for it.
8. **`Callout`** — Bring your logs to the appointment. "Here's six weeks of data" is a much better opening than "I've been feeling off."

---


### B.4 Blocked terms — verbatim from `LearnContentTest.kt`

```kotlin
    /** Release blocker, not a lint warning. Sex-selection and pH-"balancing" framing must never return. */
    @Test
    fun `no banned health claims appear in any article`() {
        val banned = listOf(
            "boy or girl", "sex-selection", "sway", "alkaline diet", "alkaline water",
            "douch", "optimize your ph", "balance your ph", "conceiving a boy", "conceiving a girl",
        )
        learnArticles.forEach { article ->
            val text = (
                article.title + " " + article.excerpt + " " +
                    article.body.joinToString(" ") { block ->
                        when (block) {
                            is ArticleBlock.Heading -> block.text
                            is ArticleBlock.Paragraph -> block.text
                            is ArticleBlock.Callout -> block.text
                            is ArticleBlock.BulletList -> block.items.joinToString(" ")
                        }
                    }
                ).lowercase()
            banned.forEach { phrase ->
                assertTrue("${article.slug} contains banned phrase: $phrase", !text.contains(phrase))
            }
        }
    }
```

**Matching semantics, precisely:**

| Property | Value |
|---|---|
| Terms | `boy or girl` · `sex-selection` · `sway` · `alkaline diet` · `alkaline water` · `douch` · `optimize your ph` · `balance your ph` · `conceiving a boy` · `conceiving a girl` |
| Match type | **plain substring** (`String.contains`), **not** regex, **not** word-boundary |
| Case | insensitive — the haystack is `.lowercase()`d; the needles are already lowercase |
| Fields scanned | `title`, `excerpt`, and **all** body block text (`Heading`, `Paragraph`, `Callout`, and each `BulletList` item, space-joined) |
| Fields **NOT** scanned | **`tags`**, `readingTime`, `cta.label`, `slug`, `category.label` |
| Failure mode | `assertTrue` → **test failure → build failure** |

**Two consequences you must not inherit blindly:**

- **`tags` are unscanned.** Adding `tags = listOf("sway")` passes the build today. Close this on iOS.
- **`sway` is an unanchored substring.** It matches `swayed`, `sways`, and would match inside a longer
  word. No current article trips it. If iOS uses word boundaries instead, behaviour diverges — match
  the Android semantics or tighten *both* platforms together.

**Why this exists (context you need):** a prior Genesyx release shipped a claim in the onboarding quiz
that pH balance "can subtly influence the likelihood of conceiving a boy or girl." It is unsupported,
and sex-selection framing in a fertility app is an ethical and regulatory liability. It was caught by a
human, on a device, the night before release. This test exists so that never depends on a human again.
**Port it as a build-failing test, not a lint warning.**

The disclaimer test, verbatim — note its narrower scope:

```kotlin
    @Test
    fun `nutrition and wellness articles require the disclaimer`() {
        learnArticles
            .filter { it.category == ArticleCategory.NUTRITION || it.category == ArticleCategory.WELLNESS }
            .forEach { assertTrue("${it.slug} must set disclaimerRequired", it.disclaimerRequired) }
    }
```

`a8` (`reading-your-trends`) and `a10` (`using-what-you-learn`) are `INSIGHTS`, set
`disclaimerRequired = true`, and are **not covered by this assertion**. **[Confirmed gap.]**

### B.5 The disclaimer string, verbatim

`LearnContent.kt`, `const val MEDICAL_DISCLAIMER`:

> This is educational content, not medical advice. It can't account for your individual circumstances,
> and it isn't a substitute for talking to a doctor, nurse, or pharmacist. If something feels wrong, or
> you're worried, please speak to a healthcare professional.

Rendered **only** when `disclaimerRequired == true`, positioned after the CTA and before Related,
separated by a 1dp hairline rule, in `bodySmall` / `onSurfaceVariant`.

### B.6 `learn_intro_seen` — exact mechanism

| Property | Value |
|---|---|
| Mechanism | **Jetpack DataStore (Preferences)** — *not* SharedPreferences |
| Key type | `booleanPreferencesKey` |
| **Key string** | **`"learn_intro_seen"`** |
| Default | **`false`** (`it[Keys.LEARN_INTRO_SEEN] ?: false`) |
| Declared | `data/local/datastore/GenesyxPreferencesDataStore.kt:28` |
| Read (Flow) | `GenesyxPreferencesDataStore.kt:45` → `PreferencesRepository.kt:34-35` (`stateIn(scope, SharingStarted.Eagerly, false)`) |
| Written | `GenesyxPreferencesDataStore.kt:56` `setLearnIntroSeen(v)` ← `PreferencesRepository.setLearnIntroSeen` ← `LearnViewModel.dismissIntro()` |
| Consumed | `ui/learn/LearnScreen.kt:70` `val introSeen by viewModel.introSeen.collectAsState()`; gate at `:117` `if (!introSeen)` |
| Cleared on sign-out / account delete? | **No.** It is a device preference, not user data. `AuthRepository` clears session keys + `database.clearAllTables()`; `learn_intro_seen` survives. **[Confirmed. Deliberate? Unverified — see §G.]** |

Hint card copy, verbatim: `New here? Start with “Your first week with Genesyx”. Everything else can wait.`
(Note the **typographic** quotation marks `“ ”`, U+201C/U+201D.)

---

## C. Behaviour spec

### C.0 Routes and entry points (confirmed, `ui/navigation/Screen.kt`)

| Route string | `Screen` object | Bottom bar | Reached from |
|---|---|---|---|
| `learn` | `Screen.Learn` | **visible** | Bottom-nav "Learn" tab; Nutrition → "See all articles" |
| `learn/search` | `Screen.LearnSearch` | hidden | Search icon in Learn header |
| `learn/article/{slug}` | `Screen.ArticleDetail` | hidden | Featured card, list row, Nutrition tile, Related row |

`Screen.bottomTabs = listOf(Home, Track, Nutrition, Insights, Learn, Profile)` — **six** tabs, one past
the Material 3 maximum, by owner decision. `noBottomNavRoutes` contains `ArticleDetail.route` and
`LearnSearch.route` — registered as the **route pattern** (`"learn/article/{slug}"`), because
`destination.route` yields the pattern, not the resolved path.

**No deep links.** `GenesyxNavGraph.kt:109-116` declares `composable(route, arguments)` with **no
`navDeepLink`**, and `AndroidManifest.xml` registers only `host="invite"`. Confirmed.

Nutrition entry point (`ui/nutrition/NutritionScreen.kt:117-121`): renders `learnArticles.forEach` — i.e.
**all ten**, not three — as compact rows, plus a "See all articles" `TextButton`. It sits **outside** the
`if (state.cycleSetUp)` gate, so users without a configured cycle still reach Learn.

### C.1 Landing — `LearnScreen`

Section order, top to bottom:

1. **Header row** — `Eyebrow("Learn")` in `ElectricLavender`; `Text("Short reads")` `displayLarge`
   `SemiBold`; trailing `IconButton` (48dp) with `Icons.Outlined.Search` → navigates to `learn/search`.
2. **Subtitle** — `Tracking, nutrition, and what your patterns mean.`
3. **Intro card** — *only if* `!introSeen`. Lavender-tinted (`alpha 0.08`) rounded row, body text +
   48dp `Close` `IconButton` → `dismissIntro()`. Never returns once dismissed.
4. **Category chips** — `LazyRow` of `FilterChip`. First chip `All`; then the five categories in enum
   order.
5. **Featured card** — hero image at `aspectRatio(16f/9f)`, then eyebrow (category label, in accent
   colour), title `titleLarge/SemiBold`, excerpt `bodyMedium`, reading time `11.5.sp`.
6. **Article rows** — every remaining visible article.
7. 24dp trailing spacer.

**Filter + featured selection, verbatim (`LearnScreen.kt:72-76`):**

```kotlin
    val visible = learnArticles.filter { selectedCategory == null || it.category == selectedCategory }
    // The featured hero only leads the unfiltered list; inside a filter it's just another article.
    val featured = if (selectedCategory == null) visible.firstOrNull { it.featured } else null
    val rest = visible.filter { it != featured }
```

- **Single-select**, nullable. Tapping the active chip **deselects** it
  (`onSelect(if (selected == category) null else category)`); tapping `All` sets `null`.
- **Does not compose with search** — search is a separate screen with no category state.
- Selection is `rememberSaveable` (survives rotation/process death), **not** in the ViewModel, **not**
  persisted across app launches.
- **When a filter is active the featured hero is suppressed** and `a1` appears as an ordinary row.
- `items(rest, key = { it.id })` — stable keys, so filter changes don't lose scroll position.

**Card contents:** featured = hero + category label + title + excerpt + reading time. Row = 64dp square
thumbnail + title + `"${category.label} · ${readingTime}"` + chevron. **The row shows no excerpt.**

### C.2 Detail — `ArticleDetailScreen`

Resolution: `articleBySlug(slug)`; if `null` → `ArticleNotFound` (back button, "That article isn't
available.", "Back to Learn"). **Never blank, never crash.**

Section order:

1. Top row: 48dp `GxBackButton`, spacer, 48dp `Icons.Filled.Share` button.
2. Hero — `aspectRatio(16f/9f)`, 20dp horizontal padding, `RoundedCornerShape(24.dp)`.
3. Eyebrow — `"${category.label} · ${readingTime}"`, uppercased by `Eyebrow`, accent-coloured. So it
   renders **`NUTRITION · 6 MIN READ`**.
4. Title — `headlineMedium`, `SemiBold`.
5. Body — `article.body.forEach { ArticleBlockView(it) }`, 16dp spacer after each block.
6. CTA card — *if* `cta != null`. `surfaceVariant` rounded card wrapping a full-width `GxPrimaryButton`.
7. Disclaimer — *if* `disclaimerRequired`. Hairline rule, then `MEDICAL_DISCLAIMER` in `bodySmall`.
8. Related — *if* `relatedArticles(article).isNotEmpty()`. `Text("Related")` `titleLarge`, then
   `ArticleRow`s.
9. 32dp trailing spacer.

**Body rendering format** (`ArticleBlockView`, exhaustive `when`):

| Block | Style |
|---|---|
| `Heading` | `titleLarge`, `SemiBold`, `onBackground`, 8dp top padding |
| `Paragraph` | `bodyLarge`, `lineHeight = 26.sp`, `onSurfaceVariant` |
| `BulletList` | per item: 5dp circular dot (`onSurfaceVariant`, 9dp top padding) + 14dp gap + `bodyLarge`, `lineHeight = 24.sp`; 10dp between items |
| `Callout` | full-width `surface` card, `RoundedCornerShape(20.dp)`, 18dp padding, `bodyMedium`, `lineHeight = 22.sp`, `onSurface` |

**Related selection is NOT a rule — it is hardcoded ids.** Verbatim (`LearnContent.kt`):

```kotlin
fun relatedArticles(article: Article): List<Article> =
    article.relatedArticleIds.mapNotNull { id -> learnArticles.firstOrNull { it.id == id } }
```

No tag similarity, no category match, no recency. Hand-curated pairs, listed in §B.3. `mapNotNull`
means an unresolvable id **silently disappears** — a unit test guards this.

**Related navigation replaces, not stacks** (`ArticleDetailScreen.kt:146-150`):

```kotlin
navController.navigate(Screen.ArticleDetail.create(other.slug)) {
    popUpTo(Screen.ArticleDetail.route) { inclusive = true }
}
```

Three related-taps ⇒ **one** back-press returns to the list.

**CTA wiring**, verbatim:

```kotlin
private fun ArticleCta.route(): String = when (type) {
    CtaType.OPEN_LOG -> Screen.Log.route
    CtaType.OPEN_TRACK -> Screen.Track.route
    CtaType.OPEN_NUTRITION -> Screen.Nutrition.route
    CtaType.OPEN_INSIGHTS -> Screen.Insights.route
    CtaType.OPEN_ARTICLE -> Screen.ArticleDetail.create(requireNotNull(targetSlug))
}
```

…and the navigation, which reuses a tab rather than stacking a second copy:

```kotlin
val route = cta.route()
navController.navigate(route) {
    // A CTA into a tab must reuse that tab, not stack a second copy behind us.
    if (Screen.bottomTabs.any { it.route == route }) {
        popUpTo(Screen.Home.route) { saveState = true }
        restoreState = true
    }
    launchSingleTop = true
}
```

`OPEN_LOG` targets `Screen.Log` — a **modal** destination, not a tab — so it stacks normally.
**`OPEN_ARTICLE` is unused by all ten articles and would crash on a null `targetSlug`.**

**Share action**, verbatim:

```kotlin
private fun Context.shareArticle(article: Article) {
    val text = "${article.title}\n\n${article.excerpt}\n\nFrom the Genesyx app — ${AppLinks.SITE_URL}"
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, article.title)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    runCatching { startActivity(Intent.createChooser(send, null)) }
}
```

Shared payload = **title + blank line + excerpt + blank line + `From the Genesyx app — https://genesyx.co.uk`**.
Subject = title. **No per-article URL.** `AppLinks.SITE_URL = "https://genesyx.co.uk"`, and its comment
says the site root is deliberate because no `/blog/{slug}` page is confirmed to exist.

### C.3 Search — `LearnSearchScreen`

- **Live, not submit.** `val results = searchArticles(query)` is computed in composition on every
  keystroke (`onValueChange = { query = it }`). `ImeAction.Search` is set but **no `onSearch` handler
  exists** — pressing the key does nothing beyond dismissing focus. No debounce.
- Field auto-focuses via `LaunchedEffect(Unit) { focusRequester.requestFocus() }`.
- Trailing clear (`✕`) appears only when `query.isNotEmpty()`.
- `query` is `rememberSaveable`.

**Filtering logic, verbatim (`LearnContent.kt`):**

```kotlin
/** Case-insensitive match over title, excerpt, and tags. Ten articles — no index needed. */
fun searchArticles(query: String): List<Article> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return emptyList()
    return learnArticles.filter { a ->
        a.title.lowercase().contains(q) ||
            a.excerpt.lowercase().contains(q) ||
            a.tags.any { it.lowercase().contains(q) }
    }
}
```

- Fields: **title, excerpt, tags**. **Body is NOT searched.**
- **No tokenization.** The whole trimmed query is one substring needle. `"logging beats"` matches;
  `"beats logging"` matches nothing.
- Case-insensitive both sides. Blank/whitespace query → `emptyList()`.
- Results render in **declaration order** (filter preserves it), as `ArticleRow`s. No relevance rank.

**Two empty states, both real:**

| Condition | Title | Body |
|---|---|---|
| `query.isBlank()` | `Search the Learn section` | `Try "hydration", "habits", or "insights".` |
| `results.isEmpty()` | `No articles match “{query}”` | `Try a shorter word, or browse everything from the Learn tab.` |

Both show a 40dp `Search` icon at 50% `onSurfaceVariant`. The no-results title uses typographic quotes
and **echoes the raw query**.

### C.4 Loading / empty / error states — what exists

| State | Exists? | Why |
|---|---|---|
| Loading / skeleton / spinner | ❌ **Deliberately absent** | Content is a compile-time constant. A shimmer would be a lie about latency. |
| Error state | ❌ **Deliberately absent** | No operation can fail. There is nothing to retry. |
| Empty search (blank query) | ✅ | §C.3 |
| No search results | ✅ | §C.3 |
| Article not found (bad slug) | ✅ `ArticleNotFound` | Guards against a bad deep link once deep links exist |
| Empty category | ❌ **Unreachable** | Every category has ≥1 article; a chip can never yield zero |
| Empty article list | ❌ | Impossible with bundled content |

**Do not port a loading state to iOS.** It would be theatre.

### C.5 Image rendering + the blank-white-square bug

**Assets:** ten JPEGs, `app/src/main/res/drawable-nodpi/learn_hero_*.jpg`. Bundled local. No remote
URLs. No image-loading library in the project (verified against `gradle/libs.versions.toml` and
`app/build.gradle.kts`) — `painterResource` needs none.

**Actual pixel dimensions: 1080 × 602** (aspect **1.794:1**). Sizes 46–148 KB; **864 KB total**.

| Usage | Box | Scaling |
|---|---|---|
| Featured card hero | `fillMaxWidth().aspectRatio(16f/9f)` (**1.778**) | `ContentScale.Crop` |
| Detail hero | `fillMaxWidth().aspectRatio(16f/9f)`, `RoundedCornerShape(24.dp)` | `ContentScale.Crop` |
| List/related/search thumbnail | `size(64.dp)` **square**, `RoundedCornerShape(14.dp)` | `ContentScale.Crop` |

The assets are 1.794:1 into a 1.778:1 box, so ~1% is cropped horizontally. Invisible; stated for exactness.
**There is no separate thumbnail asset** — the same 1080px hero is centre-cropped to a 64dp square.

**Fallback when the asset is missing**, verbatim:

```kotlin
@Composable
internal fun ArticleHero(article: Article, modifier: Modifier = Modifier) {
    val hero = article.heroImage
    if (hero != null) {
        Image(
            painter = painterResource(hero),
            contentDescription = null, // Decorative — the title carries the meaning.
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        val accent = article.category.accent()
        Box(
            modifier = modifier.background(
                Brush.linearGradient(listOf(accent.copy(alpha = 0.28f), accent.copy(alpha = 0.06f))),
            ),
        )
    }
}
```

`heroImage` is `@DrawableRes Int?`. When null, a linear gradient of the category accent (28% → 6% alpha)
fills the identical box. **Layout is unchanged with or without art.** All ten currently supply an image,
so the fallback is never hit in production — it exists so an eleventh article can ship before its
artwork does. `contentDescription = null` marks the hero decorative for TalkBack.

**The blank-white-square bug — what it actually was.** Not a code bug. A *content* bug, caught on device.
The two abstract heroes (`learn_hero_insights.jpg`, `learn_hero_trends.jpg`) were first generated from
prompts asking for "pale lavender", "faint", "barely visible", "lots of negative space". At full width
they were elegant. Centre-cropped to the **64dp square thumbnail**, they rendered as **blank white
squares** — nothing legible survived the shrink. Both were regenerated with "saturated deep
lavender-purple", "clear contrast", "fills the frame", "high clarity even when viewed very small", and
verified on device. **The fix was new artwork, not new code.** `ArticleHero` was never at fault.

**iOS consequence:** any abstract hero must survive a centre-crop to a small square. Verify at thumbnail
size before accepting it. Photographic subjects tolerate the shrink; low-contrast line-work does not.

---

## D. Recent work report

### D.1 Files changed — commit `659de4d` (git-confirmed, not inferred)

`git log`/`git show` are available. Single commit, branch `feature/learn-section`, parent `49d07f2`
(`fix/app-icon`). **26 files, +2598 / −53.**

**Added (16)**
```
app/src/main/kotlin/com/genesyx/app/domain/content/LearnContent.kt
app/src/main/kotlin/com/genesyx/app/ui/learn/LearnScreen.kt
app/src/main/kotlin/com/genesyx/app/ui/learn/ArticleDetailScreen.kt
app/src/main/kotlin/com/genesyx/app/ui/learn/LearnSearchScreen.kt
app/src/main/kotlin/com/genesyx/app/ui/learn/LearnViewModel.kt
app/src/test/kotlin/com/genesyx/app/domain/content/LearnContentTest.kt
docs/V1_1_NOTIFICATIONS_AND_LEARN.md
app/src/main/res/drawable-nodpi/learn_hero_{first_week,logging,what_to_log,hydration,
                                          eating_cycle,supplements,insights,trends,
                                          habits,using}.jpg          (10 files)
```

**Modified (9)**
```
ui/navigation/Screen.kt                       + Learn, LearnSearch, ArticleDetail routes;
                                                bottomTabs 5→6; noBottomNavRoutes +2
ui/navigation/GenesyxNavGraph.kt              + 3 composables (no deep links)
ui/components/GenesyxBottomNav.kt             + Learn tab; Profile restored; labels 9sp/1-line
ui/nutrition/NutritionScreen.kt               tiles navigate; "See all articles";
                                                section moved out of `if (state.cycleSetUp)`
domain/content/NutritionContent.kt            stub Article + nutritionArticles DELETED
data/local/datastore/GenesyxPreferencesDataStore.kt   + learn_intro_seen
data/PreferencesRepository.kt                 + learnIntroSeen / setLearnIntroSeen
core/AppLinks.kt                              + SITE_URL (+ rationale comment)
test/.../NutritionContentTest.kt              stub article test removed
```

**Removed**
- `NutritionContent.Article` (`data class Article(val title: String, val read: String)`) and
  `val nutritionArticles` — the three-entry stub.
- The `AlertDialog` at the old `NutritionScreen.kt:154-173` that rendered **one shared hardcoded
  paragraph** for all three tiles.
- The stale KDoc reference to `mockData.articles` in `NutritionContent.kt`'s header.

### D.2 Plain-English narrative

**What became functional that wasn't.** Before: three tiles titled *Eating for your luteal phase*, *How
hydration shapes fertility*, *A gentle guide to supplements*. All three opened the same 40-word popup.
It was scaffolding shaped like a feature. (Two of those titles were also editorially unsound — *"How
hydration shapes fertility"* asserts a causal claim the evidence doesn't support.)

After: a Learn tab; ten real articles with photographs; category filters; live search; related reads;
share; per-article CTAs back into Log/Track/Nutrition/Insights; and a disclaimer on health content. The
three Nutrition tiles now open three genuinely different articles.

**UI-only vs fully wired.** Everything user-visible is fully wired. Nothing renders against fake data.
The only *unwired* things are: `CtaType.OPEN_ARTICLE` (declared, tested, unused, latent crash), `tags`
(populated and searched, never displayed), and the `heroImage == null` gradient path (correct, never
exercised).

**Static vs dynamic.** 100% static. `learnArticles` is a `val` on a top-level object. There is no
repository, no `Flow<List<Article>>`, no `suspend`, no cache, no refresh. `LearnScreen` reads the list
directly in composition.

**What remains incomplete.** No bookmarks. No reading progress or completion. No lessons or modules
(the concept doesn't exist). No deep links. No pH article cluster (gated on clinical review, per
`docs/V1_1_NOTIFICATIONS_AND_LEARN.md` §8). No per-article share URL.

### D.3 Backend / network / Room / persistence — confirmed

| Assertion | Method | Result |
|---|---|---|
| No backend | `git grep -in article -- data/ domain/model/ docs/schema.sql` | zero hits |
| No network | no `http` in Learn sources; no Coil/Glide/Picasso in `libs.versions.toml` | confirmed |
| No Room | no entity/DAO/DTO/migration for articles | confirmed |
| No repository-persisted article state | `git grep -in "bookmark\|isBookmarked\|savedArticle\|completion\|progress.*article" -- app/src/main/kotlin` | **NONE** |
| Only persisted state is `learn_intro_seen` | see §B.6 | confirmed |

**Nothing contradicts your iOS assumption.** All four hold. One nuance worth stating explicitly:
`learn_intro_seen` is a **device preference**, not user data — it is *not* cleared on sign-out or
account deletion. If iOS stores it per-account, the platforms will behave differently on re-login.

### D.4 Blog

**Not a separate feature.** `blog` appears once in the tracked codebase, in a comment at
`core/AppLinks.kt:10`. No screen, route, model, asset, or content. Learn is the blog. No further
treatment required.

---

## E. Status verdict

### **MVP-limited — but honest.** Not shallow, not fake.

Production-ready *within its scope*: real content, real navigation, real tests, no placeholders. It is
"MVP-limited" only because the feature set is small (read-only, no saving, no progress, no deep links),
which was a deliberate, documented scoping decision — not incompleteness.

**Evidence for "not shallow":** 10,246 characters of hand-written body text across 84 structured blocks;
11 passing content-invariant tests including a build-failing safety guard; three screens with real empty
states and a not-found path; ten bundled hero images; on-device verification of every claim.

**Complete list of placeholders / dead controls / fake content in Learn as it ships:**

| Item | File | Severity | Note |
|---|---|---|---|
| `CtaType.OPEN_ARTICLE` unused; `requireNotNull(targetSlug)` | `ArticleDetailScreen.kt` `route()` | **Latent crash** | Reachable from content, never used. Would crash on null slug. |
| `tags` never rendered | `LearnScreen.kt`, `ArticleDetailScreen.kt` | Cosmetic | Searched only. Intentional. |
| `heroImage == null` gradient path | `LearnScreen.kt` `ArticleHero` | None | Correct, never exercised — all ten have art. |
| `ImeAction.Search` with no `onSearch` handler | `LearnSearchScreen.kt:80` | Cosmetic | Search is live; the key is decorative. |
| Banned-phrase test skips `tags` | `LearnContentTest.kt` | **Real gap** | A prohibited term in a tag ships silently. |
| Disclaimer test skips `INSIGHTS` | `LearnContentTest.kt` | **Real gap** | `a8`/`a10` flags unenforced. |
| `GETTING_STARTED` / `TRACKING` share an accent; `NUTRITION` / `WELLNESS` share one | `LearnScreen.kt` `accent()` | Cosmetic | Two colour collisions across five categories. |
| Share links to site root, not article | `AppLinks.kt` | Known | `/blog/{slug}` unconfirmed. |
| No deep links declared | `GenesyxNavGraph.kt` | Known | Scheduled with notifications. |

**`TODO` / `FIXME` count in Learn sources: 0.** (Verified: `grep -rn "TODO\|FIXME" ui/learn/ LearnContent.kt`.)

**Fake content: none.** Every article is original prose written for this app.

---

## F. iOS parity handoff

### F.1 Must replicate exactly

**Content.** All ten articles, verbatim, from `docs/learn/articles.json`. Do not re-write, re-order, or
"improve" the copy — it has been through an editorial pass with specific safety constraints.

**Identity.** `id` (`a1`…`a10`) and `slug`. Slugs are permanent public keys — they are route keys today
and share/deep-link keys tomorrow. Inventing new ones breaks cross-platform links forever.

**Taxonomy.** Five categories, these enum cases, these labels, **this order** (it drives the chip row).

**Suggested Swift shape:**

```swift
enum ArticleCategory: String, CaseIterable, Codable {
    case gettingStarted = "GETTING_STARTED"
    case tracking       = "TRACKING"
    case nutrition      = "NUTRITION"
    case insights       = "INSIGHTS"
    case wellness       = "WELLNESS"

    var label: String {
        switch self {
        case .gettingStarted: "Getting started"
        case .tracking:       "Tracking"
        case .nutrition:      "Nutrition"
        case .insights:       "Insights"
        case .wellness:       "Wellness"
        }
    }
    var accent: Color {           // hex from ui/theme/Color.kt
        switch self {
        case .gettingStarted, .tracking: Color(hex: 0x57A1CE)  // ElectricBlue
        case .nutrition, .wellness:      Color(hex: 0x4D4DAA)  // ElectricLavender
        case .insights:                  Color(hex: 0xC782D8)  // ElectricPink
        }
    }
}

enum ArticleBlock: Codable {
    case heading(String)
    case paragraph(String)
    case bulletList([String])
    case callout(String)
}

enum CtaType: String, Codable {
    case openLog = "OPEN_LOG", openTrack = "OPEN_TRACK", openNutrition = "OPEN_NUTRITION"
    case openInsights = "OPEN_INSIGHTS", openArticle = "OPEN_ARTICLE"
}

struct ArticleCta: Codable { let type: CtaType; let label: String; let targetSlug: String? }

struct Article: Codable, Identifiable {
    let id: String
    let slug: String
    let title: String
    let excerpt: String
    let body: [ArticleBlock]
    let category: ArticleCategory
    let tags: [String]
    let readingTime: String        // editorial string — DO NOT COMPUTE
    let heroImage: String?         // asset name; nil ⇒ gradient fallback
    let featured: Bool
    let relatedArticleIds: [String]
    let cta: ArticleCta?
    let disclaimerRequired: Bool
}
```

**Search — port the semantics, not just the intent:**

```swift
func searchArticles(_ query: String) -> [Article] {
    let q = query.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
    guard !q.isEmpty else { return [] }
    return articles.filter {
        $0.title.lowercased().contains(q)
            || $0.excerpt.lowercased().contains(q)
            || $0.tags.contains { $0.lowercased().contains(q) }
    }
}
```

Substring, not tokenized. Body **not** searched. Blank ⇒ empty (prompt state, **not** the full list).

**Filter:** single-select, nullable, tapping the active chip deselects. Featured hero **suppressed** when
a filter is active. Not persisted across launches. Does not compose with search.

**Related:** hardcoded `relatedArticleIds`, resolved by id, unresolvable ids dropped silently. **No
similarity algorithm.** Navigation **replaces** the current article (on iOS: swap the top of the
`NavigationStack` path, don't push).

**Featured:** `first(where: \.featured)`; exactly one across the set; assert it.

**Ordering:** declaration order everywhere — landing, search results, related. No sort key exists.

**Reading time:** copy the strings (`"4 min read"`). Do **not** compute from word count; a bulleted
checklist reads faster than dense prose and the arithmetic will disagree with Android.

**Safety list — port as a build-failing test.** Same ten substrings, same case-insensitive `contains`,
same fields. **And close the two gaps:** scan `tags` as well, and extend the disclaimer assertion to
`INSIGHTS`. Reference: §B.4.

**Disclaimer:** exact string (§B.5), rendered iff `disclaimerRequired`, positioned after CTA, before
Related.

**`learn_intro_seen` semantics:** key `learn_intro_seen`, default `false`, set `true` on dismiss, never
reset. Device-scoped, **not** account-scoped — survives sign-out. If you scope it per-account, you have
introduced a divergence. `UserDefaults` is the natural equivalent; use the same key string.

### F.2 May adapt natively

| Android | iOS |
|---|---|
| `NavHost` + route strings | `NavigationStack` + typed path. Keep the *slug* as the identifier. |
| Bottom `NavigationBar` (6 tabs) | `TabView`. **iOS collapses >5 tabs into "More"** — decide deliberately whether Learn or Profile lives there. This is a genuine platform divergence, not a bug. |
| `Intent.ACTION_SEND` | `ShareLink` / `UIActivityViewController`. Same payload (§C.2). |
| `painterResource` + `ContentScale.Crop` | `Image(...).resizable().scaledToFill().clipped()`. Same 16:9 hero, same 64pt square thumb. |
| `FilterChip` `LazyRow` | horizontal `ScrollView` of capsule buttons |
| `AlertDialog`-free detail | plain `ScrollView` |
| DataStore | `UserDefaults` / `@AppStorage("learn_intro_seen")` |
| `ArticleBlock` sealed interface | `enum ArticleBlock` with associated values. **Do not substitute Markdown** — the structure is what keeps the two platforms' rendering consistent and lets content serialise unchanged if it ever moves server-side. |

Typography, spacing, transitions, chip styling, haptics, and the gradient's exact gradient stops are all
free to feel native.

### F.3 Drift risks if ignored

| Risk | Consequence |
|---|---|
| Slugs invented on iOS | Cross-platform links break permanently. Unrecoverable once shared. |
| Banned-phrase test not ported | Silent reintroduction of the pH sex-selection claim. **Highest severity.** |
| Markdown bodies instead of `ArticleBlock` | Blocks the future shared JSON contract; rendering diverges. |
| Reading time computed | Different numbers for identical articles. |
| Search tokenized on iOS | `"beats logging"` returns results on iOS, nothing on Android. |
| Related computed by similarity | Different "Related" lists per platform for the same article. |
| Body added to the search corpus | iOS returns far more results for the same query. |
| Featured hero shown under a filter | `a1` appears twice on one screen. |
| `learn_intro_seen` scoped per-account | Hint reappears on iOS after re-login, not on Android. |
| Content edited on one platform | Ten articles silently diverge. **No shared source exists today.** |
| Abstract hero art not checked at thumbnail size | Blank white squares. See §C.5. |

---

## G. File evidence

**Content + model**
- `app/src/main/kotlin/com/genesyx/app/domain/content/LearnContent.kt` — `Article`, `ArticleBlock`,
  `ArticleCategory`, `ArticleCta`, `CtaType`, `MEDICAL_DISCLAIMER`, `learnArticles`, `articleBySlug`,
  `relatedArticles`, `searchArticles`

**Screens / composables**
- `ui/learn/LearnScreen.kt` — `LearnScreen`, `CategoryChips`, `IntroCard`, `ArticleHero`, `FeaturedCard`,
  `ArticleRow`, `ArticleCategory.accent()`
- `ui/learn/ArticleDetailScreen.kt` — `ArticleDetailScreen`, `ArticleBlockView`, `CtaCard`,
  `ArticleNotFound`, `Context.shareArticle`, `ArticleCta.route()`
- `ui/learn/LearnSearchScreen.kt` — `LearnSearchScreen`, `EmptyState`
- `ui/learn/LearnViewModel.kt` — `introSeen`, `dismissIntro()`
- `ui/nutrition/NutritionScreen.kt:117-121, 355-380` — `ArticlesSection(onOpen, onSeeAll)`

**Navigation**
- `ui/navigation/Screen.kt:27-31` (routes), `:46` (`bottomTabs`), `:63-64` (`noBottomNavRoutes`)
- `ui/navigation/GenesyxNavGraph.kt:106-116` — three `composable`s, **no `navDeepLink`**
- `ui/components/GenesyxBottomNav.kt` — Learn tab, 9sp labels
- `AndroidManifest.xml:34,44` — only `genesyx://invite` and the lovable.app App Link

**Persistence**
- `data/local/datastore/GenesyxPreferencesDataStore.kt:28,45,56`
- `data/PreferencesRepository.kt:34-35, setLearnIntroSeen`

**Theme**
- `ui/theme/Color.kt:7,12,14` — `ElectricLavender #4D4DAA`, `ElectricBlue #57A1CE`, `ElectricPink #C782D8`

**Tests**
- `app/src/test/kotlin/com/genesyx/app/domain/content/LearnContentTest.kt` — 11 invariants

**Assets**
- `app/src/main/res/drawable-nodpi/learn_hero_*.jpg` — 10 × 1080×602, 864 KB total

**Other**
- `core/AppLinks.kt:10` — the only `blog` reference in the codebase; `SITE_URL`
- `docs/V1_1_NOTIFICATIONS_AND_LEARN.md` — design brief (**also specifies an unbuilt notifications
  feature — do not read it as shipped code**)
- `docs/learn/articles.json` — extracted payload, round-trip verified
- Git: `659de4d`, branch `feature/learn-section`, parent `49d07f2`

---

## H. Open questions

1. **Is `learn_intro_seen` surviving account deletion intentional?** It is a device preference and is
   **not** cleared by `AuthRepository`'s `clearAllTables()` + session wipe. Defensible, but unverified as
   a decision. If iOS scopes it per-account, the platforms diverge. **[Needs a call.]**
2. **Does `genesyx.co.uk/blog/{slug}` exist, or will it?** Until it does, neither platform can share a
   per-article URL or register an HTTPS deep link.
3. **iOS tab bar with six tabs.** iOS collapses more than five into an automatic "More" tab. Which of
   Learn or Profile goes there — or does iOS use a different navigation idiom entirely? Android's
   six-tab bar was an explicit owner decision that iOS cannot copy literally.
4. **Should the two colour collisions be fixed?** `GETTING_STARTED`/`TRACKING` and
   `NUTRITION`/`WELLNESS` share accents. If iOS assigns five distinct colours, chips and eyebrows will
   not match Android. **[Confirmed collision; intent unverified.]**
5. **Should `CtaType.OPEN_ARTICLE` be removed or made safe?** Unused, and `requireNotNull` makes it a
   crash if ever used with a null slug. Out of scope for this audit (no code changes) — flagged.
6. **`supplementPlan` in `NutritionContent.kt` names doses** ("Folate (400–800 mcg)", "Zinc (8–11 mg)")
   while the Learn supplements article deliberately names none, in an app commercially adjacent to a
   supplements business. Pre-existing, outside Learn, unchanged. **[Flagged.]**
7. **No source-resolution originals of the hero images are in the repo** — only the 1080px derivatives.
   The 1376×768 generations exist only in a session scratchpad and will be lost. Regenerate from the
   prompts in `docs/LEARN_FEATURE_AUDIT.md` §G.3 rather than upscaling.
