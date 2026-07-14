# Learn Feature — Verification & Parity Handoff

**Scope:** the Learn (educational content) feature only.
**Repo:** `genesyx-android`, branch `feature/learn-section`, HEAD `659de4d`.
**Date:** 2026-07-09.
**Method:** every claim below is grounded in a file, a line, a git object, or a build output. Findings
that are *inferred* rather than *verified* are labelled **[INFERRED]**. Nothing here is filled in from
generic mobile best practice.

---

## A. Executive summary

**What the Learn feature is.** A read-only, offline-first educational section: ten bundled articles,
each with a hero image, reachable from a bottom-nav tab. It has category filters, client-side search,
related-article links, share, and per-article calls-to-action that navigate back into the app's
tracking surfaces. There is no backend, no network call, no account state, and no user-generated data
of any kind.

**What was done.** The entire feature was built today, in one session, in three steps. Before it, the
app had a stub: `NutritionContent.kt` defined `data class Article(val title: String, val read: String)`
with three fake entries, and all three tiles opened an `AlertDialog` containing *the same hardcoded
paragraph*. That stub is deleted. It is now ten real articles across five categories, three screens,
ten hero images, and a test suite. Committed as `659de4d` (26 files, +2598/−53).

**Is it real and complete?** It is **real and shipping-quality for what it covers**, and it is
**deliberately narrower** than the design brief it was built from. Everything visible works and is
backed by actual content. Nothing is faked. But four things the brief specifies do **not** exist yet:
bookmarks/saved articles, deep links into articles, the pH article cluster, and any per-article
persisted state. Those are unbuilt, not broken.

**Correction you need before reading further.** There is **no "Higgisfield" feature in this app, and
never has been.** Higgsfield is the third-party AI image-generation service I called via MCP earlier
today to produce the ten hero JPEGs. It is a tool that ran on my machine, not a screen, module,
article, route, asset, or concept inside Genesyx. The only two strings matching `higg` in the entire
repository are (1) a provenance sentence I wrote into the design doc an hour ago and (2) the commit
message of `659de4d`. Full evidence in **§F**. Nothing needs to be ported to the other platform for
it. I have not invented a feature to satisfy the question.

**Key parity risks**, in order of how likely they are to cause silent drift:

1. **Article slugs are a public contract.** They are route keys today (`learn/article/{slug}`) and
   will be deep-link and share-URL keys tomorrow. If the other platform invents its own slugs, links
   break across platforms permanently. Freeze the ten slugs in §E.
2. **The banned-phrase test does not exist on the other platform.** A build-failing guard
   (`LearnContentTest.kt`) blocks `boy or girl`, `sex-selection`, `sway`, `alkaline diet`, `douch`,
   `balance your ph`, `optimize your ph`. This exists because a pH sex-selection claim *did ship* once
   and was caught by a human the night before release. Without this test on the other platform, that
   regression can recur there. **This is the single highest-severity item in this document.**
3. **The disclaimer rule is enforced by test, not by convention.** Every `NUTRITION` and `WELLNESS`
   article must set `disclaimerRequired = true`. Port the assertion, not just the flag.
4. **Image prompt provenance was not stored in the repo** until this document. The exact prompts are
   now recorded in §G so the other platform can generate a visually matching set.
5. **The content is duplicated, not shared.** Both platforms will hold their own copy of ten articles.
   Any edit must be applied twice, or the content must move to a shared source. See §H.

---

## B. Learn feature map

| Screen / component | Purpose | Source file | Data source | Status | Notes |
|---|---|---|---|---|---|
| `LearnScreen` | Landing: featured hero, category chips, article list, search entry, first-time hint | `ui/learn/LearnScreen.kt` | `learnArticles` (compile-time const) + DataStore for hint | **Complete** | Bottom nav visible. Featured hero suppressed when a category filter is active. |
| `LearnViewModel` | Holds only the persisted "hint dismissed" flag | `ui/learn/LearnViewModel.kt` | `PreferencesRepository.learnIntroSeen` | **Complete** | Category filter is transient `rememberSaveable`, deliberately not in the VM. |
| `ArticleDetailScreen` | Renders one article: hero, structured body, CTA, disclaimer, related | `ui/learn/ArticleDetailScreen.kt` | `articleBySlug(slug)` | **Complete** | Bottom nav hidden. Handles unknown slug via `ArticleNotFound`. |
| `LearnSearchScreen` | Client-side search over title + excerpt + tags | `ui/learn/LearnSearchScreen.kt` | `searchArticles(query)` | **Complete** | Bottom nav hidden. No debounce (ten string compares). |
| `ArticleHero` (composable) | Renders `heroImage`, or a category-tinted gradient if null | `ui/learn/LearnScreen.kt` | `@DrawableRes Int?` | **Complete** | The fallback is the "space for pics" guarantee. |
| `ArticleRow` (composable) | 64dp thumbnail list row, shared by landing / search / related | `ui/learn/LearnScreen.kt` | — | **Complete** | `internal`, reused in three places. |
| `IntroCard` (composable) | One dismissible first-visit hint | `ui/learn/LearnScreen.kt` | DataStore `learn_intro_seen` | **Complete** | Not a coach-mark tour. One card, one dismiss. |
| `CategoryChips` (composable) | `FilterChip` row, All + 5 categories | `ui/learn/LearnScreen.kt` | `ArticleCategory.entries` | **Complete** | Filters in place; no navigation. |
| `ArticlesSection` (composable) | Nutrition-screen entry point: 3 tiles + "See all articles" | `ui/nutrition/NutritionScreen.kt:355` | `learnArticles` | **Complete** | Was the dead-end stub. Now navigates. |
| `LearnContent.kt` | The content itself + model + search/related helpers | `domain/content/LearnContent.kt` | — | **Complete** | 10 articles, ~430 lines. |
| `LearnContentTest.kt` | Content invariants incl. the banned-phrase guard | `test/.../LearnContentTest.kt` | — | **Complete** | 14 tests, all green. |
| Bookmarks / saved articles | — | — | — | **DOES NOT EXIST** | Specified in the brief (§5.1, §6.5). Not built. |
| Article deep links | — | — | — | **DOES NOT EXIST** | Specified in the brief (§2.6). Not built. See §D. |
| Progress / completion tracking | — | — | — | **DOES NOT EXIST** | Never specified, never built. No lessons, no modules. |
| pH article cluster | — | — | — | **DOES NOT EXIST** | Specified in the brief (§8), gated on clinical review. |

### Verified absences

I searched for each of these explicitly. All returned zero hits in `app/src/main/kotlin`:

```
bookmark | isBookmarked | savedArticle | completion | progress.*article   → NONE
article  (in data/, domain/model/, docs/schema.sql)                       → NONE
```

Articles **never touch the data layer**. No Room entity, no DAO, no DTO, no Supabase table, no
repository. The only persisted byte in the whole feature is the boolean `learn_intro_seen`.

---

## C. What was done

### C.1 Founder-readable

Before today, the Nutrition tab had a section called "Learn more" with three tiles: *Eating for your
luteal phase*, *How hydration shapes fertility*, *A gentle guide to supplements*. Tapping any of the
three opened a small popup containing the same 40-word paragraph. It was scaffolding that looked like
a feature. Two of those three titles were also quietly a problem: *"How hydration shapes fertility"*
asserts a causal claim the evidence doesn't support.

Now: there's a Learn tab in the bottom bar. It opens on a featured article with a photograph, a row of
category filters, and a list of the other nine, each with its own thumbnail. Tap any one and you get a
real article — headings, bullets, pull-out callouts — ending in a button that takes you back into the
app to do the thing the article just described. Health articles carry a plain-English disclaimer.
There's a search, and every article suggests two related reads.

The three original tiles still exist in Nutrition, now pointing at three genuinely different articles,
with a "See all articles" link. That was the whole point of building it in that order: the visible bug
became the feature's front door.

What it deliberately does not do: you cannot save an article, there is no reading progress, no
"lessons" or "modules", and nothing syncs to a server. It works on a plane.

### C.2 Technical implementation

**Content model** — `domain/content/LearnContent.kt`, following the existing convention of
`CycleContent.kt` / `NutritionContent.kt` / `QuizContent.kt` (top-level `val`s, no injection):

```kotlin
data class Article(
    val id: String,                    // "a1".."a10", stable, used by relatedArticleIds
    val slug: String,                  // route key — never change after release
    val title: String,
    val excerpt: String,
    val body: List<ArticleBlock>,      // sealed interface, not Markdown
    val category: ArticleCategory,     // GETTING_STARTED|TRACKING|NUTRITION|INSIGHTS|WELLNESS
    val tags: List<String>,            // search corpus only; never rendered
    val readingTime: String,           // editorial string, e.g. "4 min read" — NOT computed
    @DrawableRes val heroImage: Int? = null,
    val featured: Boolean = false,
    val relatedArticleIds: List<String> = emptyList(),
    val cta: ArticleCta? = null,
    val disclaimerRequired: Boolean = false,
)

sealed interface ArticleBlock { Heading; Paragraph; BulletList; Callout }
enum class CtaType { OPEN_LOG, OPEN_TRACK, OPEN_NUTRITION, OPEN_INSIGHTS, OPEN_ARTICLE }
```

Deliberate omissions from the model, each with a reason:

- **No Markdown dependency.** `ArticleBlock` is a sealed interface rendered by an exhaustive `when`.
  Type-safe, previewable, zero deps, and serialisable as JSON unchanged if content ever moves
  server-side.
- **No `isBookmarked` field.** It would put mutable per-user state on an immutable compile-time
  constant. When bookmarks land, the ViewModel combines the static list with a Room `Flow`.
- **No `relatedArticles: List<Article>`.** A self-referencing `data class` graph cannot be built as a
  `val` list without `lateinit`/`lazy` backdoors. IDs are resolved at render time by
  `relatedArticles(article)`.
- **`readingTime` is a string, not `wordCount / 200`.** A bulleted checklist reads faster than dense
  prose; the arithmetic would lie.
- **No `author` / `publishDate` / `difficulty`.** Nothing reads them. They were in the design brief and
  were cut, per the repo's "no speculative code" rule.

**Navigation** — three routes added to `ui/navigation/Screen.kt`:

| Route | Object | Bottom nav |
|---|---|---|
| `learn` | `Screen.Learn` | **visible** (it's a tab) |
| `learn/search` | `Screen.LearnSearch` | hidden |
| `learn/article/{slug}` | `Screen.ArticleDetail` | hidden |

`noBottomNavRoutes` compares against `backStackEntry.destination.route`, which for a parameterised
destination is the **pattern** (`"learn/article/{slug}"`), not the resolved path — so the literal
pattern string is what's registered, matching the pre-existing `Invite.route` precedent.

**Bottom nav** — `Screen.bottomTabs` went from 5 → 6 (`Home / Track / Nutrition / Insights / Learn /
Profile`), one past the Material 3 recommended maximum. This was an explicit owner decision, made after
two intermediate designs (Learn off-bar; Learn swapped in for Profile). Six items leave ~60dp each at
360dp, which wrapped "Nutrition" onto a second line at the default `labelSmall`. `GenesyxBottomNav.kt`
now pins labels to `fontSize = 9.sp`, `lineHeight = 12.sp`, `maxLines = 1`, `softWrap = false`. **There
is no headroom left**: a seventh tab, or a longer label, breaks the bar again.

**Sorting and selection** — there is no sort. `learnArticles` renders in **declaration order**. Featured
selection is `visible.firstOrNull { it.featured }`, and a test asserts exactly one article sets
`featured = true`. When a category chip is active, `featured` is forced to `null` so the hero doesn't
duplicate a row — inside a filter, the featured article is just another article.

**Search** — `searchArticles(query)` (`LearnContent.kt`): trim, lowercase, `contains` over
`title | excerpt | tags`. Blank query returns `emptyList()` (the UI shows a prompt state, not the full
list). No index, no debounce, no fuzzy matching. Ten articles, ten string compares.

**Related navigation** replaces rather than stacks:

```kotlin
navController.navigate(Screen.ArticleDetail.create(other.slug)) {
    popUpTo(Screen.ArticleDetail.route) { inclusive = true }
}
```

Three taps through Related should not require three back-presses.

**CTA navigation** reuses tabs rather than stacking a second copy:

```kotlin
if (Screen.bottomTabs.any { it.route == route }) {
    popUpTo(Screen.Home.route) { saveState = true }; restoreState = true
}
launchSingleTop = true
```

**Share** — `Intent.ACTION_SEND`, `text/plain`, wrapped in `createChooser`, in `runCatching`. It sends
title + excerpt + **`AppLinks.SITE_URL` (`https://genesyx.co.uk`)**, *not* a per-article URL. No
`/blog/{slug}` page is confirmed to exist, and a shared 404 is worse than a shared homepage. The
constant carries a comment saying exactly this.

**Images** — ten `learn_hero_*.jpg` in `res/drawable-nodpi/`, loaded with `painterResource` +
`ContentScale.Crop`. No Coil/Glide/Picasso anywhere in the project (verified against
`gradle/libs.versions.toml` and `app/build.gradle.kts`); bundled drawables need none.

**Tests** — `LearnContentTest.kt`, 14 tests, all green under `:app:testDebugUnitTest`. The banned-phrase and disclaimer guards are **mutation-verified**: reintroducing each defect fails the build.


| Test | Guards against |
|---|---|
| `every article has copy` | an empty title/excerpt/body |
| `ten launch articles, each tagged` | silent content loss |
| `slugs are unique` | a duplicate slug silently shadowing an article |
| `articles have distinct titles and bodies` | **the exact stub bug that existed before** |
| `articleBySlug resolves … rejects unknown slug` | bad deep links |
| `exactly one article is featured` | two heroes / no hero |
| `related article ids resolve and never self-reference` | a related link vanishing silently |
| `search matches title, excerpt and tags, ignores case` | tag-only matches regressing |
| `article CTAs resolve` | a CTA into a nonexistent article |
| `nutrition and wellness articles require the disclaimer` | shipping health copy uncovered |
| `no banned health claims appear in any article` | **the pH sex-selection regression** |

### C.3 Recent-work evidence (git-verified, not inferred)

Git history **is** available. Everything below is from `git show`/`git log`, not inference.

- Single commit: **`659de4d`** "Add Learn section: 10 illustrated articles, search, filters, related",
  **26 files changed, 2598 insertions(+), 53 deletions(-)**, on branch `feature/learn-section`,
  branched from `fix/app-icon` (`49d07f2`).
- **Added:** `domain/content/LearnContent.kt`; `ui/learn/{LearnScreen,ArticleDetailScreen,
  LearnSearchScreen,LearnViewModel}.kt`; 10 × `res/drawable-nodpi/learn_hero_*.jpg`;
  `test/.../LearnContentTest.kt`; `docs/V1_1_NOTIFICATIONS_AND_LEARN.md`.
- **Removed:** `NutritionContent.Article` and `nutritionArticles` (the 3-entry stub), and the
  `AlertDialog` at the old `NutritionScreen.kt:154-173` that rendered one shared paragraph for all
  three tiles. The now-stale KDoc reference to `mockData.articles` was corrected in the same file.
- **Modified:** `Screen.kt` (3 routes, `bottomTabs` 5→6, `noBottomNavRoutes` +2);
  `GenesyxNavGraph.kt` (3 composables); `GenesyxBottomNav.kt` (Learn item, Profile restored, 9sp
  labels); `NutritionScreen.kt` (tiles navigate, "See all articles" added, section moved **out** of
  the `if (state.cycleSetUp)` gate); `GenesyxPreferencesDataStore.kt` + `PreferencesRepository.kt`
  (`learn_intro_seen`); `AppLinks.kt` (`SITE_URL`); `NutritionContentTest.kt` (stub test removed).
- **On-device verification** (emulator-5554, `installDebug`): all three Nutrition tiles open three
  *different* articles; category filter yields exactly the 3 Insights articles and suppresses the
  hero; search for `"memory"` — a term present **only** in `a2`'s tags — returns `a2`; unknown query
  shows the no-results state with the query echoed; article detail hides the bottom nav; CTA from
  `gentle-guide-supplements` lands on Log Today; disclaimer renders at the foot of health articles;
  all six nav labels fit on one line.

**One caveat on the device evidence:** the emulator is severely throttled — its clock advanced ~12
minutes between consecutive `adb` commands and it repeatedly queued and replayed taps. Several
intermediate screenshots landed on screens I had not navigated to. Every result above was
re-confirmed via `uiautomator dump` text assertions, not screenshot appearance alone. **No
Learn-related bug was observed**, but the device is not a reliable timing harness.

### C.4 Parity explanation

For the other platform, the load-bearing facts are: **ten articles, five categories, one featured,
declaration-order rendering, structured (non-Markdown) bodies, tags searched but never displayed,
editorial reading-time strings, disclaimer on nutrition+wellness, related-as-replace, CTA-as-tab-reuse,
and zero backend.** Everything else is Compose detail. Full spec in **§H**.

---

## D. Learn roadmap

### D.1 Confirmed current scope (phase 1 — shipped)

Ten articles · five categories · featured hero · category filters · client-side search · article detail
with structured body, CTA and disclaimer · related articles · share · dismissible first-time hint · ten
bundled hero images with gradient fallback · Nutrition-screen entry point · Learn bottom-nav tab.

### D.2 Confirmed roadmap evidence (written down, not built)

These are **stated in the design doc** `docs/V1_1_NOTIFICATIONS_AND_LEARN.md`, which was authored
alongside the code. They are commitments, not guesses:

| Item | Where specified | Why it isn't built |
|---|---|---|
| **Bookmarks / saved articles** | §5.1, §6.5, §11.1 step 4 | Requires Room `version = 3 → 4`, an `article_bookmarks` entity keyed `(userId, articleId)`, a migration + `MigrationTestHelper` test. Deliberately deferred as its own step. Note: it must be **Room**, not DataStore — `AuthRepository.kt:58` calls `database.clearAllTables()` on account deletion, so Room bookmarks are wiped for free, while a DataStore `stringSet` would silently survive account deletion. That is a compliance bug, and it is why the field was left off the model. |
| **Article deep links** (`genesyx://learn/article/{slug}`) | §2.6, table | The route exists but declares **no `navDeepLink`**, and `AndroidManifest.xml` has only `host="invite"`. Verified. Deep links were scheduled with the notification work (which owns `MainActivity` `singleTop` + `onNewIntent`), so they land together. |
| **pH article cluster** (4 articles, `PH_WELLNESS` category) | §8 | **Hard-gated on clinical review by a named reviewer.** The doc says: if review can't happen, ship the ten and drop the cluster. `ArticleCategory` therefore has **no** `PH_WELLNESS` member today. |
| **Per-article `/blog/{slug}` share URL** | §6.3, `AppLinks.SITE_URL` comment, Open Question 1 | The marketing site is not confirmed to host per-article pages. |
| **Push notifications** deep-linking into Learn | §2.6 | Entire notifications feature unbuilt. |

### D.3 Inferred roadmap clues **[INFERRED]**

Read off the shape of the code, not from any TODO (there are **zero** `TODO`/`FIXME` markers in the
Learn sources — verified):

- **`heroImage` is nullable.** [INFERRED] This exists so an eleventh article can be added before its
  artwork lands. Content growth is anticipated.
- **`ArticleBlock` is a sealed interface, explicitly documented as "stays serializable".** [INFERRED]
  Deliberate preparation for a future server-side content source, where the same hierarchy becomes
  JSON. `LearnContent.kt`'s header comment says content is bundled because "the set changes at an
  app-release cadence" — i.e. this holds until it doesn't.
- **`CtaType.OPEN_ARTICLE` exists and is tested, but no article uses it.** [INFERRED] Reserved for
  article-to-article funnels; the pH cluster's `when-to-seek-support` article is the specified
  consumer (doc §8.2).
- **`tags` are searched but never rendered.** [INFERRED] A tag chip UI, or tag-based related-article
  inference (replacing hand-curated `relatedArticleIds`), is the natural next use.
- **`LearnViewModel` exists to hold a single boolean.** [INFERRED] It is over-provisioned for its job —
  it's the seam where bookmark state will be combined in.
- **No `LearnCategory` route**, though `Screen` naming and the doc contemplate one. [INFERRED] Category
  browsing was collapsed into in-place chip filtering; a dedicated route returns only when deep links
  need `genesyx://learn/category/{c}`.

### D.4 Suggested phasing

- **Phase 1 — done.** Read-only Learn, bundled content, ten illustrated articles.
- **Phase 2 — bookmarks.** Room 3→4 + migration test + saved-articles screen + Profile row. Self-
  contained; no other feature depends on it.
- **Phase 3 — deep links.** Lands with notifications (`singleTop`, `onNewIntent`, manifest hosts). Only
  then does the share URL become a per-article link.
- **Phase 4 — pH cluster.** Gated on clinical review. Adds `ArticleCategory.PH_WELLNESS` and four
  articles. Independent of 2 and 3.
- **Later — server-side content.** Only if content velocity demands it. The documented approach is a
  Supabase `articles` table behind a read-through cache **with the bundled set as offline fallback** —
  bundled content becomes the seed, not dead weight.

### D.5 Blockers / gaps

| Gap | Blocking | Owner |
|---|---|---|
| No named clinical reviewer | pH cluster | Owner |
| `/blog/{slug}` pages don't exist | per-article share URL | Owner / marketing |
| Bookmarks need a Room migration | phase 2 | Engineering |
| The supplements article sits adjacent to a supplements business | ongoing editorial risk | Owner |

On that last point: `gentle-guide-supplements` names **no doses and no brands**, says so in the body,
and its hero image is a bare open palm — no pills, no bottles, no packaging. That was a deliberate
constraint, because Genesyx is commercially adjacent to a supplements business. Note that
`NutritionContent.kt`'s pre-existing `supplementPlan` **does** name doses ("Folate (400–800 mcg)") — that
is older code, outside Learn's scope, and was not touched. **[Flagged, not changed.]**

---

## E. Article inventory

All ten are **real, hand-written, unique content**. None is seeded, duplicated, lorem, or placeholder.
Source for every one: `app/src/main/kotlin/com/genesyx/app/domain/content/LearnContent.kt`.
Every article has a detail screen (one screen, parameterised by slug) and a hero image.
Metadata present on every article: `readingTime`, `tags`, `category`, `relatedArticleIds`, `cta`.
Metadata **absent from all** articles: author, publish date, difficulty. (`author` was cut from the
model — see §C.2.)

| # | Title | Slug | Category | Read | Feat | Disc | CTA | Related | Hero |
|---|---|---|---|---|---|---|---|---|---|
| a1 | Your first week with Genesyx | `getting-started-first-week` | GETTING_STARTED | 5 min | **✔** | — | OPEN_LOG | a2, a9 | `learn_hero_first_week.jpg` |
| a2 | Why logging beats remembering | `why-logging-beats-remembering` | TRACKING | 4 min | — | — | OPEN_LOG | a3, a8 | `learn_hero_logging.jpg` |
| a3 | Symptoms and meals: what's worth writing down | `what-to-log` | TRACKING | 4 min | — | — | OPEN_LOG | a2, a9 | `learn_hero_what_to_log.jpg` |
| a4 | Hydration, without the eight-glass myth | `hydration-basics` | NUTRITION | 3 min | — | **✔** | OPEN_TRACK | a5, a9 | `learn_hero_hydration.jpg` |
| a5 | Eating with your cycle, not against it | `eating-with-your-cycle` | NUTRITION | 6 min | — | **✔** | OPEN_NUTRITION | a4, a6 | `learn_hero_eating_cycle.jpg` |
| a6 | A gentle guide to supplements | `gentle-guide-supplements` | NUTRITION | 6 min | — | **✔** | OPEN_LOG | a5, a4 | `learn_hero_supplements.jpg` |
| a7 | What "insights" actually means | `what-insights-mean` | INSIGHTS | 5 min | — | — | OPEN_INSIGHTS | a8, a10 | `learn_hero_insights.jpg` |
| a8 | Reading your trends without over-reading them | `reading-your-trends` | INSIGHTS | 5 min | — | **✔** | OPEN_INSIGHTS | a7, a10 | `learn_hero_trends.jpg` |
| a9 | Small habits that hold | `small-habits-that-hold` | WELLNESS | 4 min | — | **✔** | OPEN_LOG | a3, a10 | `learn_hero_habits.jpg` |
| a10 | Using what you learn | `using-what-you-learn` | INSIGHTS | 4 min | — | **✔** | OPEN_INSIGHTS | a7, a8 | `learn_hero_using.jpg` |

**Categories (5):** `GETTING_STARTED` "Getting started" (1) · `TRACKING` "Tracking" (2) ·
`NUTRITION` "Nutrition" (3) · `INSIGHTS` "Insights" (3) · `WELLNESS` "Wellness" (1).

**Tag vocabulary (searched, never displayed):** onboarding, basics, getting started, tracking, habits,
memory, symptoms, nutrition, logging, hydration, water, myths, cycle, phases, food, supplements,
folate, evidence, insights, data, correlation, trends, patterns, variance, consistency, routine,
decisions, wellness, experiments.

**Ordering / priority.** No sort field, no `order` column, no priority. Rendering is **declaration
order** in `learnArticles`. Featured is chosen by `firstOrNull { it.featured }`, asserted unique.
Under a category filter, `featured` is nulled out.

**Locked / unlocked / roadmap indicators in content:** none. There is no gating, no premium tier, no
"coming soon" article, no lesson or module concept. Articles are flat.

**Loading pipeline (there isn't one).** `learnArticles` is a `val` on a top-level object, resolved at
class-init. `articleBySlug()` is a linear `firstOrNull`. `LearnScreen` reads the list directly in
composition; there is no repository, no `Flow`, no `suspend`, no loading state and — per the design doc
§10.1 — **deliberately no skeleton or spinner**, because a shimmer over a compile-time constant would
be a lie about latency. There is likewise **no error state**, because there is no operation that can
fail.

**Persisted article state:** none. `learn_intro_seen` (Boolean, DataStore) is the only persisted value
in the feature, and it is about the hint card, not any article.

---

## F. Higgsfield report

**1. Does it exist in the app?** **No.** Definitively not. It is not a feature, screen, module,
article, category, route, asset, constant, or content concept anywhere in Genesyx.

**2. What is it, actually?** Higgsfield (`higgsfield.ai`) is a **third-party AI image-generation
service**, exposed to me as an MCP tool (`mcp__claude_ai_https_higgsfield_ai_mcp__generate_image`). I
called it earlier today, from this machine, to generate the ten `learn_hero_*.jpg` files. It ran in my
tooling. It has no presence in your product, your codebase, your build, your APK, or your runtime. The
shipped app makes **zero** network calls to it and contains **zero** references to it.

**3. Evidence.** Exhaustive search of the repository:

```
$ git grep -in -E "higg?is?field|higgs" -- .
docs/V1_1_NOTIFICATIONS_AND_LEARN.md:663:  … The current set is **AI-generated** (Higgsfield, Jul 2026) …

$ git log --all --oneline --grep="higg" -i
659de4d Add Learn section: 10 illustrated articles, search, filters, related

$ grep -ril "higg" --exclude-dir=build --exclude-dir=.git .
docs/V1_1_NOTIFICATIONS_AND_LEARN.md
```

Both hits are **provenance notes I wrote today**, roughly an hour before this audit, recording *how the
images were made*. That is their entire extent. There is no `Higgisfield.kt`, no `higgisfield` route,
no such string in `strings.xml`, no asset with that name.

**4. Spelling.** You wrote "Higgisfield" (extra *i*). The service is spelled **Higgsfield**. I searched
for both, plus `higgs`, case-insensitively, across tracked files, untracked files, and all commit
messages on all branches. The three hits above are the complete result set.

**5. Learn-only, or shared?** Neither. It is not in the app at all. Its *output* — ten JPEGs — is
Learn-only.

**6. Complete / partial / placeholder?** Not applicable. Nothing to complete.

**7. Images used for it.** None belong to it. The ten hero images were **produced by** it and are now
ordinary bundled drawables with no runtime relationship to the service.

**8. Prompts.** The prompts I sent to Higgsfield were **not stored in the repository** at the time of
this audit. They existed only in my tool-call history. That was a genuine provenance gap, and it is why
you could not find them. **They are now recorded verbatim in §G** so the other platform can reproduce a
matching set.

**9. Confidence.** **Certain**, on all points above. This is a negative finding established by
exhaustive search, not an inference. If you were told, or believed, that Genesyx contains a
"Higgisfield" section, that belief does not correspond to anything in this codebase. I would rather
tell you that plainly than manufacture a section to match the question.

---

## G. Image + prompt inventory

### G.1 Image facts (verified)

- **All ten:** bundled local drawables, `app/src/main/res/drawable-nodpi/learn_hero_*.jpg`.
- **Nothing remote.** No `http` image URL anywhere in Learn. No image-loading library in the project
  (no Coil/Glide/Picasso in `libs.versions.toml` or `app/build.gradle.kts`) — none is needed.
- **Actual dimensions: 1080 × 602 px** each (aspect **1.794:1**). Source generations were 1376 × 768
  (also 1.792:1). They are rendered into a **`aspectRatio(16f/9f)` = 1.778** box with
  `ContentScale.Crop`, so ~1% is cropped horizontally. Not visible; noted for exactness.
- **Downscaled** with `sips --resampleWidth 1080 -s formatOptions 72`, following the
  `home_hero_bg.jpg` precedent (1080 px wide, ~82 KB).
- **Total payload 864 KB** across ten files (46–148 KB each), against a release APK of 4.74 MB.
- **Two purposes, one asset each:** hero (16:9, on featured card + article detail) and thumbnail
  (64dp square, `ContentScale.Crop`, in `ArticleRow`). There is no separate thumbnail asset.
- **Fallback:** if `heroImage == null`, `ArticleHero` draws a `Brush.linearGradient` of the category
  accent at 28%→6% alpha. This is the "space for pics" guarantee — layout is identical with or without
  art. No article currently uses the fallback.
- **Category accents** (`LearnScreen.kt`): GETTING_STARTED → `ElectricBlue`; TRACKING → `ElectricBlue`;
  NUTRITION → `ElectricLavender`; INSIGHTS → `ElectricPink`; WELLNESS → `ElectricLavender`.

| Content item | Image path | Type | Where used | Bytes | Prompt found in repo? |
|---|---|---|---|---|---|
| a1 first week | `drawable-nodpi/learn_hero_first_week.jpg` | photo | featured hero + detail + thumb | 76 K | **No** → §G.3 |
| a2 logging | `drawable-nodpi/learn_hero_logging.jpg` | photo | detail + thumb | 88 K | **No** → §G.3 |
| a3 what to log | `drawable-nodpi/learn_hero_what_to_log.jpg` | photo | detail + thumb | 76 K | **No** → §G.3 |
| a4 hydration | `drawable-nodpi/learn_hero_hydration.jpg` | photo | detail + thumb | 76 K | **No** → §G.3 |
| a5 eating w/ cycle | `drawable-nodpi/learn_hero_eating_cycle.jpg` | photo | detail + thumb | 140 K | **No** → §G.3 |
| a6 supplements | `drawable-nodpi/learn_hero_supplements.jpg` | photo | detail + thumb | 48 K | **No** → §G.3 |
| a7 what insights means | `drawable-nodpi/learn_hero_insights.jpg` | **abstract** | detail + thumb | 100 K | **No** → §G.3 |
| a8 reading trends | `drawable-nodpi/learn_hero_trends.jpg` | **abstract** | detail + thumb | 108 K | **No** → §G.3 |
| a9 small habits | `drawable-nodpi/learn_hero_habits.jpg` | photo | detail + thumb | 148 K | **No** → §G.3 |
| a10 using what you learn | `drawable-nodpi/learn_hero_using.jpg` | photo | detail + thumb | 84 K | **No** → §G.3 |

### G.2 Prompt provenance — honest statement

**Before this document, the repository contained zero image prompts.** No prompt file, no JSON seed
field, no markdown note, no code comment, no admin tool, no generation script, no issue text. The
commit message for `659de4d` records only that the images are AI-generated and which service made
them. `docs/V1_1_NOTIFICATIONS_AND_LEARN.md:663` says the same. Neither contains a prompt.

The prompts below are **not reconstructed or guessed**. They are the **exact strings** I sent to
`generate_image` today, recovered from this session's tool calls, transcribed verbatim. Model:
`nano_banana_pro` (resolved server-side to `nano_banana_2`), `aspect_ratio: "16:9"`, `count: 1`,
resolution `1k` (service default).

### G.3 The ten prompts, verbatim

**Shared style contract** (present in every prompt, in some form): muted airy palette of warm
off-white, pale lavender and faint cool blue · soft diffused natural light · shallow depth of field ·
calm, quiet, editorial · **no people's faces, no text, no logos, no clinical objects**.

**a1 — `learn_hero_first_week`**
> Editorial hero image for a calm women's health app article titled "Your first week with Genesyx".
> Soft diffused morning light falling across a pale bedside table; a phone lies face-down beside a
> simple glass of water and a small sprig of greenery. Shot from a low three-quarter angle, shallow
> depth of field. Muted, airy palette of warm off-white and pale lavender, with faint cool blue in the
> shadows. Calm, quiet, unhurried, editorial photography. No people, no faces, no text, no logos, no
> medical or clinical objects, no pills or supplement bottles. Generous negative space in the upper
> right.

**a2 — `learn_hero_logging`**
> Editorial hero image: an open notebook on a pale wooden surface, photographed from directly
> overhead, with a single short line of handwriting on an otherwise blank page. A pen rests beside it.
> Soft diffused natural light, shallow depth of field. Muted airy palette of warm off-white, pale
> lavender and faint cool blue. Calm, quiet editorial photography. No people, no faces, no readable
> text, no logos, no clinical objects.

**a3 — `learn_hero_what_to_log`**
> Editorial hero image: a minimal flat-lay of exactly three simple objects on a pale surface — a glass
> of water, a wooden pen, and a single piece of fresh fruit. Generous negative space between them.
> Soft diffused natural light from one side. Muted airy palette of warm off-white, pale lavender and
> faint cool blue. Calm, quiet editorial photography. No people, no faces, no text, no logos, no
> clinical objects.

**a4 — `learn_hero_hydration`**
> Editorial hero image: water being poured into a clear glass, captured close up and side-lit so the
> stream catches the light. Sense of movement and freshness. Pale background, shallow depth of field.
> Muted airy palette of warm off-white, pale lavender and faint cool blue. Calm, quiet editorial
> photography. No people, no faces, no text, no logos, no clinical objects.

**a5 — `learn_hero_eating_cycle`**
> Editorial hero image: four small ceramic plates arranged in a soft grid on a pale surface,
> photographed from above. Each plate holds a different simple whole food — dark leafy greens, lentils,
> berries, nuts. Soft diffused natural light. Muted airy palette of warm off-white, pale lavender, soft
> sage green and faint cool blue. Calm, quiet editorial photography. No people, no faces, no text, no
> logos, no clinical objects.

**a6 — `learn_hero_supplements`** *(note the unusually long negative list — deliberate; see §D.5)*
> Editorial hero image: a single open human palm, held upward, softly out of focus against a pale
> neutral background. Minimal, quiet, unhurried. Warm diffused light. Muted airy palette of warm
> off-white, pale lavender and faint cool blue. Calm editorial photography. No face, no pills, no
> capsules, no bottles, no packaging, no medical or clinical objects, no text, no logos.

**a7 — `learn_hero_insights`** *(second generation — see §G.4)*
> Minimal abstract editorial graphic on a warm off-white paper field: two clearly visible hand-drawn
> line traces running loosely parallel, gently rising and falling, never touching. Lines are confident
> and legible with clear contrast — one in saturated deep lavender-purple, one in medium slate blue,
> each roughly 6px thick. The lines fill most of the frame. Subtle paper grain. No axes, no grid, no
> numbers, no text, no labels, no logos, no people. Calm, editorial, high clarity even when viewed very
> small.

**a8 — `learn_hero_trends`** *(second generation — see §G.4)*
> Minimal abstract editorial graphic on a warm off-white paper field: a scatter of clearly visible
> small dots spread across the frame, resolving into a discernible upward diagonal trend. Dots in
> saturated deep lavender-purple and dusty rose, with good contrast against the pale background. A soft
> slate-blue trend line runs through them. The composition fills the frame. Subtle paper grain. No axes,
> no grid, no numbers, no text, no labels, no logos, no people. Calm, editorial, high clarity even when
> viewed very small.

**a9 — `learn_hero_habits`**
> Editorial hero image: a worn dirt desire-line path curving across soft green grass, made by many
> footsteps over time. Photographed low, soft overcast morning light, quiet and human. Muted palette of
> pale sage green, warm off-white and faint cool blue. Calm editorial photography, shallow depth of
> field. No people, no faces, no text, no signage, no logos, no buildings.

**a10 — `learn_hero_using`**
> Editorial hero image: a hand reaching in to turn a simple, unmarked brass dial, close up, in warm
> side light. Sense of quiet agency and adjustment. Pale neutral background, shallow depth of field.
> Muted airy palette of warm off-white, pale lavender and faint cool blue. Calm editorial photography.
> No face, no text, no numbers, no markings, no logos, no clinical objects.

### G.4 The thumbnail-legibility lesson (important for parity)

The **first** generations of a7 and a8 asked for "pale lavender", "faint", "barely visible", "lots of
negative space". At full size they were elegant. Shrunk to the 64dp `ArticleRow` thumbnail they
rendered as **blank white squares** — observed on device, not theorised. Both were regenerated with
"saturated deep lavender-purple", "clear contrast", "fills the frame", "high clarity even when viewed
very small". The prompts in §G.3 for a7/a8 are the **second, shipped** versions.

**Rule for the other platform:** any abstract hero must survive being cropped to a small square. Test
it at thumbnail size before accepting it. Photographic subjects tolerate the shrink; low-contrast
abstract line-work does not.

### G.5 Missing pieces

- Prompts were absent from the repo until this file. **Recommendation:** commit §G.3 (this document
  does that) so provenance travels with the code.
- No source-resolution originals are stored in the repo — only the 1080px derivatives. The 1376×768
  PNGs exist solely in a session scratchpad and **will be lost**. If you want re-crops or higher-DPI
  variants later, regenerate from §G.3 rather than upscaling.
- No licence/provenance metadata is embedded in the JPEGs themselves (no EXIF note that they are
  AI-generated). **[Flagged.]** Some jurisdictions and some app stores are beginning to ask.

---

## H. Other-version implementation spec

> **Which platform?** Not stated in the request and not determinable from this repo — no iOS or web
> source is present here. This spec is written platform-agnostically. `docs/schema.sql`,
> `ARCHITECTURE.md` and the `genesis-cycle-guide.lovable.app` App Link in `AndroidManifest.xml:44`
> suggest a **web** counterpart exists. **[INFERRED — confirm before use.]**

### H.1 Required screens (3)

1. **Learn landing** — featured hero card (image + category eyebrow + title + excerpt + reading time),
   horizontally scrolling category filter chips (`All` + 5), vertical list of remaining articles as
   thumbnail rows, search affordance, dismissible first-visit hint.
2. **Article detail** — hero image, `CATEGORY · N MIN READ` eyebrow, title, structured body, CTA block,
   conditional medical disclaimer, related-articles list. Share action.
3. **Search** — text field, results as thumbnail rows, distinct prompt state (blank query) and
   no-results state (echo the query back).

### H.2 Required model (field-for-field)

```
Article
  id            string     stable, referenced by relatedArticleIds
  slug          string     ROUTE KEY — must match Android exactly (see §E)
  title         string
  excerpt       string
  body          ArticleBlock[]
  category      enum       GETTING_STARTED | TRACKING | NUTRITION | INSIGHTS | WELLNESS
  tags          string[]   searched, never rendered
  readingTime   string     editorial, e.g. "4 min read" — DO NOT COMPUTE
  heroImage     ref?       nullable → gradient fallback
  featured      bool       exactly one true across the set
  relatedIds    string[]
  cta           { type, label, targetSlug? }
  disclaimerRequired bool

ArticleBlock = Heading{text} | Paragraph{text} | BulletList{items[]} | Callout{text}
CtaType      = OPEN_LOG | OPEN_TRACK | OPEN_NUTRITION | OPEN_INSIGHTS | OPEN_ARTICLE
```

**Do not** add `isBookmarked` to this model (see §C.2). **Do not** add `author`/`publishDate`/
`difficulty` unless you render them. **Do not** substitute Markdown for `ArticleBlock` — the structure
is what keeps content consistent and lets it serialise unchanged later.

### H.3 Required content structure

Port all ten articles from §E **verbatim**, including slugs, ids, category assignments, reading-time
strings, tag lists, related-id pairs, CTA types, and `disclaimerRequired` flags. `a1` is the featured
article. Render in declaration order. Body text must be copied, not re-written — the copy has been
through an editorial pass with specific safety constraints (§H.6).

### H.4 Required image structure

- One hero per article, 16:9, ~1080px wide, ~50–150 KB.
- The same asset serves both hero and thumbnail; crop-to-fill for the square thumbnail.
- **Nullable hero with a category-tinted gradient fallback is mandatory**, not optional. It is the
  contract that lets an article ship before its artwork.
- Regenerate from the prompts in §G.3 for visual consistency. Honour §G.4: verify abstract heroes at
  thumbnail size.
- Keep the filename convention `learn_hero_<short_name>.jpg` so the two platforms' assets are
  cross-referenceable.

### H.5 Behaviour parity (matters more than pixels)

| Behaviour | Rule |
|---|---|
| Featured under filter | When a category is selected, the featured hero is **suppressed**; the article appears as an ordinary row. |
| Search corpus | title + excerpt + **tags**. Case-insensitive substring. Blank query → prompt state, **not** the full list. |
| Related navigation | **Replaces** the current article in history. Three related-taps ⇒ one back-press returns to the list. |
| CTA into a tab | Reuses the existing tab; never stacks a second copy behind the article. |
| Unknown slug | Renders a graceful "That article isn't available" screen with a way back. **Never blank, never crash.** |
| Disclaimer | Rendered iff `disclaimerRequired`. Enforced by test, not by reviewer diligence. |
| Loading state | **None.** Content is local and synchronous. Do not add a skeleton or spinner. |
| Error state | **None.** No operation can fail. Do not invent one. |
| Hint card | Shows once, persists dismissal, never returns. |
| Share | Title + excerpt + site root. **Not** a per-article URL until those pages exist on both platforms. |

### H.6 Content-safety contract (non-negotiable)

Port `LearnContentTest.kt`'s `no banned health claims appear in any article` as a **build-failing
test**, not a lint warning. Banned substrings (case-insensitive, over title + excerpt + all body text):

```
boy or girl · sex-selection · sway · alkaline diet · alkaline water
douch · optimize your ph · balance your ph · conceiving a boy · conceiving a girl
```

Also port: `nutrition and wellness articles require the disclaimer`; `slugs are unique`;
`related article ids resolve and never self-reference`; `article CTAs resolve`;
`articles have distinct titles and bodies`; `exactly one article is featured`.

**Why this is stated so forcefully.** A previous Genesyx release shipped a claim in the onboarding quiz
that "pH balance can subtly influence the likelihood of conceiving a boy or girl." It is unsupported,
and sex-selection framing in a fertility app is an ethical and regulatory liability. It was caught **by a
human, on a device, the night before release**. The test exists so that never depends on a human again.
A second platform without this test reintroduces the exposure.

Additionally, `gentle-guide-supplements` must remain scrupulously non-promotional: **no doses, no
brands**, no pills/bottles/packaging in its hero image. Genesyx is commercially adjacent to a
supplements business.

### H.7 Allowed platform-specific adaptation

Free to differ: navigation container (tab bar vs. drawer vs. sidebar), scroll and transition physics,
typography scale, chip and card styling, search affordance (inline vs. dedicated screen), share sheet
mechanics, image codec (WebP/AVIF instead of JPEG), and how the gradient fallback is drawn.

Must **not** differ: slugs, ids, category names and their labels, article body text, reading-time
strings, featured selection, related pairs, CTA targets, disclaimer text and its trigger rule, search
corpus, and the banned-phrase guard.

### H.8 Drift risks

| Risk | Mitigation |
|---|---|
| Content edited on one platform only | Single source of truth. Short term: this document + code review. Long term: the shared `articles` table described in §D.4. |
| Slugs diverge | Freeze §E. Slugs are permanent once a share URL or deep link exists. |
| Other platform adds Markdown bodies | Blocks the future shared JSON contract. Port `ArticleBlock`. |
| Other platform computes reading time | Produces different numbers for the same article. Copy the strings. |
| Banned-phrase test not ported | Silent reintroduction of the pH claim. **Highest severity.** |
| Bookmarks built on the wrong store | Must be wiped on account deletion. On Android that means Room (`clearAllTables()`), not DataStore. Whatever the other platform's equivalent, it must be covered by its account-deletion path. |
| AI-image provenance lost | §G.3 travels with the repo. Do not strip it. |

---

## I. File evidence

Every conclusion above traces to one of these.

**Learn source (added, commit `659de4d`)**
- `app/src/main/kotlin/com/genesyx/app/domain/content/LearnContent.kt` — model, 10 articles, `articleBySlug`, `relatedArticles`, `searchArticles`, `MEDICAL_DISCLAIMER`
- `app/src/main/kotlin/com/genesyx/app/ui/learn/LearnScreen.kt` — `LearnScreen`, `CategoryChips`, `IntroCard`, `ArticleHero`, `FeaturedCard`, `ArticleRow`, `ArticleCategory.accent()`
- `app/src/main/kotlin/com/genesyx/app/ui/learn/ArticleDetailScreen.kt` — `ArticleDetailScreen`, `ArticleBlockView`, `CtaCard`, `ArticleNotFound`, `Context.shareArticle`, `ArticleCta.route()`
- `app/src/main/kotlin/com/genesyx/app/ui/learn/LearnSearchScreen.kt` — `LearnSearchScreen`, `EmptyState`
- `app/src/main/kotlin/com/genesyx/app/ui/learn/LearnViewModel.kt` — `introSeen`, `dismissIntro()`
- `app/src/test/kotlin/com/genesyx/app/domain/content/LearnContentTest.kt` — 14 invariants

**Learn assets** — `app/src/main/res/drawable-nodpi/learn_hero_{first_week,logging,what_to_log,hydration,eating_cycle,supplements,insights,trends,habits,using}.jpg` (10 × 1080×602)

**Modified**
- `app/src/main/kotlin/com/genesyx/app/ui/navigation/Screen.kt` — `Learn`, `LearnSearch`, `ArticleDetail` (+`create()`, `ARG_SLUG`); `bottomTabs` 5→6; `noBottomNavRoutes` +`ArticleDetail`, +`LearnSearch`
- `app/src/main/kotlin/com/genesyx/app/ui/navigation/GenesyxNavGraph.kt:106-116` — three `composable`s; **no `navDeepLink`**
- `app/src/main/kotlin/com/genesyx/app/ui/components/GenesyxBottomNav.kt` — `Learn` item (`Icons.AutoMirrored.Outlined.MenuBook`), `Profile` restored, labels 9sp/1-line
- `app/src/main/kotlin/com/genesyx/app/ui/nutrition/NutritionScreen.kt:117-121, 355-380` — `ArticlesSection(onOpen, onSeeAll)`, moved outside the `cycleSetUp` gate; `AlertDialog` removed
- `app/src/main/kotlin/com/genesyx/app/domain/content/NutritionContent.kt` — `Article` + `nutritionArticles` **deleted**
- `app/src/main/kotlin/com/genesyx/app/data/local/datastore/GenesyxPreferencesDataStore.kt:28,45` — `LEARN_INTRO_SEEN`
- `app/src/main/kotlin/com/genesyx/app/data/PreferencesRepository.kt:34-35` — `learnIntroSeen`
- `app/src/main/kotlin/com/genesyx/app/core/AppLinks.kt` — `SITE_URL` + rationale comment
- `app/src/test/kotlin/com/genesyx/app/domain/content/NutritionContentTest.kt` — stub test removed

**Consulted for absence** — `docs/schema.sql` (no `articles` table), `app/src/main/AndroidManifest.xml`
(hosts: `invite`, `genesis-cycle-guide.lovable.app` only), `gradle/libs.versions.toml` +
`app/build.gradle.kts` (no image loader), `data/` + `domain/model/` (no article types).

**Design doc** — `docs/V1_1_NOTIFICATIONS_AND_LEARN.md` (§5.1 content-source decision, §5.2 nav,
§6 screens, §8 pH cluster, §10 states, §11 handoff). Note this doc also specifies the **unbuilt**
notifications feature; do not read it as a description of shipped code.

**Git** — `659de4d` on `feature/learn-section`, parent `49d07f2` (`fix/app-icon`). 26 files,
+2598/−53.

---

## J. Open questions

**Cannot be verified from this repo — needs you.**

1. **Which "other version"?** No iOS or web source is in this repository. An App Link to
   `genesis-cycle-guide.lovable.app` (`AndroidManifest.xml:44`) implies a web app exists. **[INFERRED.]**
   §H is written platform-agnostically; confirm the target before handing it over.
2. **Does `genesyx.co.uk/blog/{slug}` exist, or will it?** Share currently sends the site root. Until
   these pages exist, no per-article URL can be shared and no HTTPS App Link can be registered.
3. **Who clinically reviews the pH cluster?** Without a named reviewer it does not ship on either
   platform. The other ten articles are unaffected.
4. **Is AI-generated imagery acceptable for the store listing / brand?** You approved it for the app.
   Some app stores are beginning to require disclosure. No provenance metadata is embedded in the JPEGs.
5. **`supplementPlan` in `NutritionContent.kt` names specific doses** ("Folate (400–800 mcg)", "Zinc
   (8–11 mg)"). That predates Learn and is out of scope here, but it sits in tension with the
   no-doses rule the Learn supplements article follows. Worth a decision. **[Flagged, unchanged.]**

**Inferred, not confirmed** — every claim in §D.3; the existence of a web counterpart (§J.1); the
intended future use of `tags` and `CtaType.OPEN_ARTICLE`. All are marked **[INFERRED]** in place.

**Verified negative** — there is no Higgsfield/"Higgisfield" feature (§F); there are no bookmarks, no
progress or completion tracking, no lessons or modules, no article deep links, no article backend, and
no `TODO`/`FIXME` markers anywhere in the Learn sources.
