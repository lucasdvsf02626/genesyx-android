package com.genesyx.app.domain.content

import androidx.annotation.DrawableRes
import com.genesyx.app.R
import java.time.LocalDate

/**
 * Learn-section content. Bundled in the app (like [CycleContent] / [NutritionContent]) rather than
 * fetched: the set changes at an app-release cadence, works offline, and needs no backend table.
 * See docs/V1_1_NOTIFICATIONS_AND_LEARN.md §5.1.
 */

enum class ArticleCategory(val label: String) {
    GETTING_STARTED("Getting started"),
    TRACKING("Tracking"),
    NUTRITION("Nutrition"),
    INSIGHTS("Insights"),
    WELLNESS("Wellness"),
    GUIDES("Guides"),
}

/** Structured body — avoids a Markdown dependency and keeps articles previewable and type-safe. */
sealed interface ArticleBlock {
    data class Heading(val text: String) : ArticleBlock
    data class Paragraph(val text: String) : ArticleBlock
    data class BulletList(val items: List<String>) : ArticleBlock
    data class Callout(val text: String) : ArticleBlock
}

/** Where an article's closing call-to-action sends the reader. */
enum class CtaType { OPEN_LOG, OPEN_TRACK, OPEN_PH, OPEN_NUTRITION, OPEN_INSIGHTS, OPEN_ARTICLE }

data class ArticleCta(val type: CtaType, val label: String, val targetSlug: String? = null) {
    init {
        // OPEN_ARTICLE without a target is unrenderable. Fail at construction — [learnArticles] is a
        // compile-time constant, so a bad CTA blows up on class-init in tests, never in a user's hands.
        require(type != CtaType.OPEN_ARTICLE || targetSlug != null) {
            "ArticleCta(OPEN_ARTICLE) requires a targetSlug; label was \"$label\""
        }
    }
}

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
    /**
     * Hero artwork, 16:9, in `drawable-nodpi` (see `home_hero_bg.jpg` for the downscaling precedent).
     * Null until the art exists — the UI falls back to a brand gradient keyed to [category], so an
     * article without a picture still lays out correctly and the slot is visibly reserved.
     */
    @DrawableRes val heroImage: Int? = null,
    val featured: Boolean = false,
    val relatedArticleIds: List<String> = emptyList(),
    val cta: ArticleCta? = null,
    /** Renders the medical disclaimer above the footer. */
    val disclaimerRequired: Boolean = false,
    /**
     * The date this article becomes visible, or null for one that always has been. The weekly
     * series is compiled into the app like everything else and revealed on a fixed calendar date
     * (cross-platform contract with iOS `publishedAt`, agreed 12 Aug 2026) — twelve articles would
     * otherwise mean twelve Play submissions, and one rejected review would break the run. Everyone
     * sees a given article on the same real date, regardless of when they installed.
     */
    val publishedAt: LocalDate? = null,
)

/** Shown whenever [Article.disclaimerRequired]. */
const val MEDICAL_DISCLAIMER =
    "This is educational content, not medical advice. It can't account for your individual " +
        "circumstances, and it isn't a substitute for talking to a doctor, nurse, or pharmacist. " +
        "If something feels wrong, or you're worried, please speak to a healthcare professional."

fun articleBySlug(slug: String): Article? = learnArticles.firstOrNull { it.slug == slug }

fun relatedArticles(article: Article): List<Article> =
    article.relatedArticleIds.mapNotNull { id -> learnArticles.firstOrNull { it.id == id } }

/** Case-insensitive match over title, excerpt, and tags. No index needed at this size.
 *  [articles] lets callers search only what the drip has revealed. */
fun searchArticles(query: String, articles: List<Article> = learnArticles): List<Article> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return emptyList()
    return articles.filter { a ->
        a.title.lowercase().contains(q) ||
            a.excerpt.lowercase().contains(q) ||
            a.tags.any { it.lowercase().contains(q) }
    }
}

/**
 * The weekly reveal, by fixed calendar date (cross-platform contract with iOS, agreed 12 Aug 2026).
 * An article with a null [Article.publishedAt] has always been available; one dated in the future
 * is compiled into the binary but withheld until that date — everyone sees it on the same day,
 * regardless of when they installed. Every Learn surface (list, search, article, related, the Home
 * card, the NEW_ARTICLE nudge) resolves through here, so a withheld article is invisible everywhere
 * at once — including a deep link or notification naming one that hasn't landed yet.
 */
object LearnDrip {

    /** True when [article] has been revealed on or before [today]. */
    fun isPublished(article: Article, today: LocalDate): Boolean =
        article.publishedAt?.let { !it.isAfter(today) } ?: true

    /** Everything the user can see today. The gate every list/search/lookup surface goes through. */
    fun published(today: LocalDate): List<Article> =
        learnArticles.filter { isPublished(it, today) }

    /**
     * The most recently released dated article, if it landed within [withinDays] of [today] — what
     * the Home "new this week" card offers. Always-available (null-date) articles never count as
     * "new"; nor does one released more than a week ago.
     */
    fun newestReleased(today: LocalDate, withinDays: Long = 7): Article? =
        learnArticles
            .filter { a -> a.publishedAt?.let { !it.isAfter(today) && !it.isBefore(today.minusDays(withinDays - 1)) } ?: false }
            .maxByOrNull { it.publishedAt!! }

    /** Dated articles whose reveal day is exactly [today] — what the weekly notification fires on. */
    fun releasedOn(today: LocalDate): List<Article> =
        learnArticles.filter { it.publishedAt == today }
}

val learnArticles: List<Article> = listOf(
    Article(
        id = "a1",
        slug = "getting-started-first-week",
        title = "Your first week with Genesyx",
        excerpt = "What to do on day one, day three, and day seven — and what to ignore until later.",
        category = ArticleCategory.GETTING_STARTED,
        tags = listOf("onboarding", "basics", "getting started"),
        readingTime = "5 min read",
        heroImage = R.drawable.learn_hero_first_week,
        featured = true,
        relatedArticleIds = listOf("a2", "a9"),
        cta = ArticleCta(CtaType.OPEN_LOG, "Open today's log"),
        body = listOf(
            ArticleBlock.Paragraph(
                "The first week isn't about completeness. It's about proving to yourself that this " +
                    "takes a minute, not an evening.",
            ),
            ArticleBlock.Heading("Day one"),
            ArticleBlock.Paragraph(
                "Set your cycle, then log once. That's it. The log can be almost empty — a mood and " +
                    "nothing else is a real entry.",
            ),
            ArticleBlock.Heading("Days two and three"),
            ArticleBlock.Paragraph(
                "Notice how little time it takes. Most people spend under thirty seconds. If you're " +
                    "spending longer, you're logging more than you need to.",
            ),
            ArticleBlock.Heading("Day seven"),
            ArticleBlock.Paragraph(
                "Your first patterns appear. They will also be mostly noise — seven days is not enough " +
                    "to conclude anything, and the app will say so. Look anyway. Seeing your own week " +
                    "written down is the point.",
            ),
            ArticleBlock.Heading("What to ignore for now"),
            ArticleBlock.BulletList(
                listOf(
                    "Perfect streaks. Missing a day costs you almost nothing.",
                    "Every optional field. Fill in what you notice, skip what you don't.",
                    "Comparing your numbers to anyone else's.",
                ),
            ),
            ArticleBlock.Callout(
                "If you only do one thing this week: log on the day you feel worst. That's the entry " +
                    "you'll be glad to have in three months.",
            ),
        ),
    ),
    Article(
        id = "a2",
        slug = "why-logging-beats-remembering",
        title = "Why logging beats remembering",
        excerpt = "Memory rewrites the past to fit the present. A log doesn't.",
        category = ArticleCategory.TRACKING,
        tags = listOf("tracking", "habits", "basics", "memory"),
        readingTime = "4 min read",
        heroImage = R.drawable.learn_hero_logging,
        relatedArticleIds = listOf("a3", "a8"),
        cta = ArticleCta(CtaType.OPEN_LOG, "Log how you feel today"),
        body = listOf(
            ArticleBlock.Paragraph(
                "Memory is reconstructive. You don't replay the past — you rebuild it, every time, out " +
                    "of whatever material is nearest to hand. And the nearest material is how you feel " +
                    "right now.",
            ),
            ArticleBlock.Paragraph(
                "This is why \"I felt terrible all week\" is so often untrue. You felt terrible today, " +
                    "and today has quietly colonised the other six days.",
            ),
            ArticleBlock.Heading("What a log actually does"),
            ArticleBlock.Paragraph(
                "It's not a diary and it's not a report card. It's a set of small, boring, unrevised " +
                    "facts written by someone who didn't yet know how the story ended — you, on a " +
                    "Tuesday.",
            ),
            ArticleBlock.Heading("Boring entries beat detailed ones"),
            ArticleBlock.BulletList(
                listOf(
                    "A one-tap mood, logged for sixty days, is worth more than a paragraph logged " +
                        "four times.",
                    "The entries you'll want later are the ordinary ones. Extremes you remember anyway.",
                    "\"Nothing to report\" is data. Log it.",
                ),
            ),
            ArticleBlock.Callout(
                "The goal isn't a complete record. It's a record honest enough to disagree with you.",
            ),
        ),
    ),
    Article(
        id = "a3",
        slug = "what-to-log",
        title = "Symptoms and meals: what's worth writing down",
        excerpt = "You don't need to log everything. Here's the short list.",
        category = ArticleCategory.TRACKING,
        tags = listOf("tracking", "symptoms", "nutrition", "logging"),
        readingTime = "4 min read",
        heroImage = R.drawable.learn_hero_what_to_log,
        relatedArticleIds = listOf("a2", "a9"),
        cta = ArticleCta(CtaType.OPEN_LOG, "Open today's log"),
        body = listOf(
            ArticleBlock.Paragraph(
                "The completeness trap goes like this: you decide to track properly, you build an " +
                    "elaborate routine, you sustain it for nine days, and then you stop entirely. A " +
                    "smaller habit you keep beats a thorough one you abandon.",
            ),
            ArticleBlock.Heading("Worth logging daily"),
            ArticleBlock.BulletList(
                listOf(
                    "Mood — one tap, no elaboration.",
                    "Energy — the thing you'll most want to correlate later.",
                    "Anything unusual. If you noticed it, write it.",
                    "Whether you ate roughly normally. Not what. Whether.",
                ),
            ),
            ArticleBlock.Heading("Worth logging weekly"),
            ArticleBlock.BulletList(
                listOf(
                    "Sleep, in broad strokes — good week, bad week.",
                    "Anything that changed: new medication, travel, illness, unusual stress.",
                ),
            ),
            ArticleBlock.Heading("Not worth logging at all"),
            ArticleBlock.Paragraph(
                "Anything you'd only be recording to feel virtuous about recording it. If you can't " +
                    "imagine a question it would help answer, skip it.",
            ),
            ArticleBlock.Callout("Consistency beats detail. It isn't close."),
        ),
    ),
    Article(
        id = "a4",
        slug = "hydration-basics",
        title = "Hydration, without the eight-glass myth",
        excerpt = "Where \"eight glasses a day\" came from, and what to do instead.",
        category = ArticleCategory.NUTRITION,
        tags = listOf("hydration", "nutrition", "myths", "water"),
        readingTime = "3 min read",
        heroImage = R.drawable.learn_hero_hydration,
        disclaimerRequired = true,
        relatedArticleIds = listOf("a5", "a9"),
        cta = ArticleCta(CtaType.OPEN_TRACK, "Track today's water"),
        body = listOf(
            ArticleBlock.Paragraph(
                "The eight-glasses rule appears to trace back to a 1945 recommendation that adults need " +
                    "roughly 2.5 litres of water a day. The very next sentence noted that most of it " +
                    "already comes from food. That sentence got dropped, and the number outlived it.",
            ),
            ArticleBlock.Heading("What's actually true"),
            ArticleBlock.BulletList(
                listOf(
                    "For most healthy adults, thirst is a reasonable guide.",
                    "Food contributes a real share of your water. Fruit, vegetables, soup, and yoghurt " +
                        "all count.",
                    "Tea and coffee count too. The idea that they dehydrate you doesn't survive contact " +
                        "with the evidence at normal intake.",
                    "Needs vary with heat, altitude, exercise, illness, and pregnancy. A fixed target " +
                        "can't account for any of that.",
                ),
            ),
            ArticleBlock.Heading("Something more useful than a number"),
            ArticleBlock.Paragraph(
                "Anchor water to things you already do. A glass with each meal. A glass when you sit " +
                    "down at your desk. This works because it removes the decision, and decisions are " +
                    "what habits die of.",
            ),
            ArticleBlock.Callout(
                "Persistent, unusual thirst — especially alongside fatigue or frequent urination — is " +
                    "worth mentioning to a doctor. It's not something to solve by drinking more.",
            ),
        ),
    ),
    Article(
        id = "a5",
        slug = "eating-with-your-cycle",
        title = "Eating with your cycle, not against it",
        excerpt = "What changes across the four phases — and what genuinely doesn't.",
        category = ArticleCategory.NUTRITION,
        tags = listOf("nutrition", "cycle", "phases", "food"),
        readingTime = "6 min read",
        heroImage = R.drawable.learn_hero_eating_cycle,
        disclaimerRequired = true,
        relatedArticleIds = listOf("a4", "a6"),
        cta = ArticleCta(CtaType.OPEN_NUTRITION, "See this phase's foods"),
        body = listOf(
            ArticleBlock.Paragraph(
                "Your appetite, energy, and cravings genuinely do shift across a cycle. That much is " +
                    "well established. What's less well established is almost everything else you'll " +
                    "read about it online.",
            ),
            ArticleBlock.Paragraph(
                "So here's an honest version: a short list of what actually changes, a shorter list of " +
                    "what to do about it, and a clear note about the claims that don't hold up.",
            ),
            ArticleBlock.Heading("What actually shifts"),
            ArticleBlock.BulletList(
                listOf(
                    "Iron. Menstruation loses iron. Foods rich in it — lentils, dark leafy greens, red " +
                        "meat — are worth leaning on during your period.",
                    "Energy. Many people feel steadier in the follicular phase and flatter in the late " +
                        "luteal phase. Eating regularly matters more than eating perfectly.",
                    "Appetite. A modest increase in the luteal phase is normal and well documented. It " +
                        "is not a failure of discipline.",
                ),
            ),
            ArticleBlock.Heading("What doesn't"),
            ArticleBlock.Paragraph(
                "Your cycle length, your ovulation timing, and the sex of any future child are not " +
                    "influenced by what you eat. No food, supplement, or eating pattern changes them. " +
                    "Claims otherwise are common, confidently stated, and unsupported.",
            ),
            ArticleBlock.Callout(
                "If an article promises that a food will change your cycle or influence conception, " +
                    "that's the moment to close the tab.",
            ),
            ArticleBlock.Heading("A practical idea per phase"),
            ArticleBlock.BulletList(
                listOf(
                    "Period — iron with a source of vitamin C alongside it, which helps absorption.",
                    "Follicular — you likely have energy. This is a good week for meals that take effort.",
                    "Ovulatory — nothing special is required. Keep eating.",
                    "Luteal — more frequent, smaller meals help some people with energy dips. Try it, " +
                        "keep it if it works, drop it if it doesn't.",
                ),
            ),
            ArticleBlock.Paragraph(
                "That last instruction applies to this whole article. Log how you feel, look back after " +
                    "a couple of cycles, and keep only what your own data supports.",
            ),
        ),
    ),
    Article(
        id = "a6",
        slug = "gentle-guide-supplements",
        title = "A gentle guide to supplements",
        excerpt = "What the evidence supports, what it doesn't, and why to talk to someone first.",
        category = ArticleCategory.NUTRITION,
        tags = listOf("supplements", "nutrition", "folate", "evidence"),
        readingTime = "6 min read",
        heroImage = R.drawable.learn_hero_supplements,
        disclaimerRequired = true,
        relatedArticleIds = listOf("a5", "a4"),
        cta = ArticleCta(CtaType.OPEN_LOG, "Log how you're feeling"),
        body = listOf(
            ArticleBlock.Paragraph(
                "The word supplement is doing a lot of quiet work. It means: in addition to. Not " +
                    "instead of, and not as insurance against, a diet that's already broadly fine.",
            ),
            ArticleBlock.Heading("The one with strong consensus"),
            ArticleBlock.Paragraph(
                "Folate — folic acid in its supplement form — is recommended by mainstream health " +
                    "bodies for anyone who might become pregnant, ideally starting before conception. " +
                    "The evidence for reduced neural tube defects is unusually strong. This is the one " +
                    "worth raising with a doctor or pharmacist rather than reading about.",
            ),
            ArticleBlock.Heading("Everything else: ask, don't assume"),
            ArticleBlock.Paragraph(
                "Vitamin D, omega-3, zinc, and the rest all have a real evidence base for specific " +
                    "people in specific circumstances — and a much larger marketing base aimed at " +
                    "everyone else. Whether any of them is useful for you depends on your diet, your " +
                    "bloodwork, where you live, and what else you take.",
            ),
            ArticleBlock.Paragraph(
                "We're deliberately naming no doses and no brands here. A dose that's sensible for one " +
                    "person is pointless or harmful for another, and that judgement needs someone who " +
                    "can see your actual situation.",
            ),
            ArticleBlock.Heading("Interactions are real"),
            ArticleBlock.Paragraph(
                "Supplements interact with medication and with each other. Some affect how other things " +
                    "are absorbed. \"Natural\" carries no information about safety.",
            ),
            ArticleBlock.Heading("How to raise it"),
            ArticleBlock.Paragraph(
                "A pharmacist is free to talk to, knows about interactions, and doesn't need an " +
                    "appointment. Bring a list of everything you take, including anything you'd think of " +
                    "as \"just a vitamin\". That's usually the whole conversation.",
            ),
            ArticleBlock.Callout(
                "If you're taking prescription medication, check before starting anything new. This is " +
                    "the single highest-value thing in this article.",
            ),
        ),
    ),
    Article(
        id = "a7",
        slug = "what-insights-mean",
        title = "What \"insights\" actually means",
        excerpt = "Your app can spot correlations. It cannot spot causes. That distinction is the whole game.",
        category = ArticleCategory.INSIGHTS,
        tags = listOf("insights", "data", "basics", "correlation"),
        readingTime = "5 min read",
        heroImage = R.drawable.learn_hero_insights,
        relatedArticleIds = listOf("a8", "a10"),
        cta = ArticleCta(CtaType.OPEN_INSIGHTS, "See your insights"),
        body = listOf(
            ArticleBlock.Paragraph(
                "An insight in this app is a pattern found by comparing things you logged. Nothing more " +
                    "mystical than that, and nothing more authoritative either.",
            ),
            ArticleBlock.Heading("Correlation, concretely"),
            ArticleBlock.Paragraph(
                "Suppose your logs show low energy on the days you slept badly. Three explanations fit " +
                    "equally well: bad sleep drained you; whatever drained you also wrecked your sleep; " +
                    "or a third thing — illness, stress, a hard week — caused both. The data cannot " +
                    "distinguish between them. Only you can, and only sometimes.",
            ),
            ArticleBlock.Heading("How much data is enough"),
            ArticleBlock.BulletList(
                listOf(
                    "Three weeks: almost nothing. Take every pattern as coincidence.",
                    "Three months: something. Patterns that survive this long are worth noticing.",
                    "A year: enough to see how you change across seasons and circumstances.",
                ),
            ),
            ArticleBlock.Heading("Holding an insight lightly"),
            ArticleBlock.Paragraph(
                "A good insight makes you curious, not certain. If reading one makes you want to " +
                    "change three things at once, that's a sign you've believed it too hard.",
            ),
        ),
    ),
    Article(
        id = "a8",
        slug = "reading-your-trends",
        title = "Reading your trends without over-reading them",
        excerpt = "One bad day is noise. Six weeks is a signal. Here's how to tell.",
        category = ArticleCategory.INSIGHTS,
        tags = listOf("insights", "trends", "patterns", "variance"),
        readingTime = "5 min read",
        heroImage = R.drawable.learn_hero_trends,
        disclaimerRequired = true,
        relatedArticleIds = listOf("a7", "a10"),
        cta = ArticleCta(CtaType.OPEN_INSIGHTS, "See your insights"),
        body = listOf(
            ArticleBlock.Paragraph(
                "Bodies vary. Not as a failure of the body, but as a basic property of it. Two days " +
                    "with identical sleep, food, and stress will still produce different numbers, and " +
                    "nothing has gone wrong.",
            ),
            ArticleBlock.Heading("What noise looks like"),
            ArticleBlock.BulletList(
                listOf(
                    "A single outlier in an otherwise flat run.",
                    "A dramatic value on a day something unusual happened.",
                    "Any pattern you can only see if you squint.",
                ),
            ),
            ArticleBlock.Heading("What a signal looks like"),
            ArticleBlock.BulletList(
                listOf(
                    "It persists across several cycles, not several days.",
                    "It shows up whether or not you were looking for it.",
                    "It doesn't disappear when you exclude the most dramatic week.",
                ),
            ),
            ArticleBlock.Heading("The trap"),
            ArticleBlock.Paragraph(
                "When you feel worse, you track harder. More tracking finds more patterns. More " +
                    "patterns feel like confirmation that something is wrong. This loop is worth " +
                    "knowing about because it feels exactly like diligence.",
            ),
            ArticleBlock.Callout(
                "A trend that is persistent, new for you, and unexplained is worth raising with a " +
                    "clinician — not worth resolving alone with more data.",
            ),
        ),
    ),
    Article(
        id = "a9",
        slug = "small-habits-that-hold",
        title = "Small habits that hold",
        excerpt = "The habit research says: make it smaller than feels worthwhile.",
        category = ArticleCategory.WELLNESS,
        tags = listOf("habits", "wellness", "consistency", "routine"),
        readingTime = "4 min read",
        heroImage = R.drawable.learn_hero_habits,
        disclaimerRequired = true,
        relatedArticleIds = listOf("a3", "a10"),
        cta = ArticleCta(CtaType.OPEN_LOG, "Start with one log"),
        body = listOf(
            ArticleBlock.Paragraph(
                "Ambitious habits fail in week two, and they fail for a structural reason: they were " +
                    "designed by a version of you who had unusual amounts of energy and optimism. That " +
                    "person does not show up on Wednesdays.",
            ),
            ArticleBlock.Heading("Anchor it to something that already happens"),
            ArticleBlock.Paragraph(
                "You already brush your teeth. You already sit down at a desk, or on a bus. Attach the " +
                    "new thing to the old thing and you never have to remember it, only notice it.",
            ),
            ArticleBlock.Heading("The two-minute version"),
            ArticleBlock.Paragraph(
                "Whatever you want to do, define a version that takes two minutes and do that. Not as a " +
                    "stepping stone to the real habit — as the habit. Scale later, if you want to, or " +
                    "don't.",
            ),
            ArticleBlock.Heading("Missing a day"),
            ArticleBlock.Paragraph(
                "Missing one day changes nothing measurable. Missing two is the pattern worth watching, " +
                    "because the second miss is where the habit stops being automatic.",
            ),
            ArticleBlock.Heading("On streaks"),
            ArticleBlock.Paragraph(
                "Streaks motivate some people and quietly torment others. If a broken streak makes you " +
                    "want to abandon the whole thing, the streak is costing you more than it's paying. " +
                    "Ignore the number.",
            ),
        ),
    ),
    Article(
        id = "a10",
        slug = "using-what-you-learn",
        title = "Using what you learn",
        excerpt = "Data is only useful if it changes something. Here's how to close the loop.",
        category = ArticleCategory.INSIGHTS,
        tags = listOf("insights", "decisions", "wellness", "experiments"),
        readingTime = "4 min read",
        heroImage = R.drawable.learn_hero_using,
        disclaimerRequired = true,
        relatedArticleIds = listOf("a7", "a8"),
        cta = ArticleCta(CtaType.OPEN_INSIGHTS, "See your insights"),
        body = listOf(
            ArticleBlock.Paragraph(
                "Tracking that never changes a decision is a hobby. A pleasant one — but let's be " +
                    "honest about which it is.",
            ),
            ArticleBlock.Heading("The loop"),
            ArticleBlock.BulletList(
                listOf(
                    "Notice something in your logs.",
                    "Guess at why, out loud, in one sentence.",
                    "Change exactly one thing.",
                    "Wait a full cycle. Not a week.",
                    "Look again.",
                ),
            ),
            ArticleBlock.Heading("Why one thing"),
            ArticleBlock.Paragraph(
                "Change three things and you've learned nothing, whatever the result. You won't know " +
                    "which one moved the needle, or whether two of them cancelled out. This is the " +
                    "single most common way people waste months of careful tracking.",
            ),
            ArticleBlock.Heading("When to stop experimenting"),
            ArticleBlock.Paragraph(
                "If something is getting worse, or it's new, or it frightens you, stop running " +
                    "experiments and talk to a professional. Your logs will make that conversation " +
                    "better. They are not a substitute for it.",
            ),
            ArticleBlock.Callout(
                "Bring your logs to the appointment. \"Here's six weeks of data\" is a much better " +
                    "opening than \"I've been feeling off.\"",
            ),
        ),
    ),

    // ── Guides (in-app how-to manuals) — ported from iOS 1.2.0 (18), 12 Aug 2026. Always available
    // (no publishedAt). Slugs/ids match iOS for cross-platform read-state and deep-link parity.
    Article(
        id = "g1",
        slug = "guide-vaginal-ph-tracker",
        title = "Using the vaginal pH tracker",
        excerpt = "How to read a pH strip, match it to a value, and log it so your trend is honest.",
        body = listOf(
            ArticleBlock.Heading("What this feature is for"),
            ArticleBlock.Paragraph(
                "The vaginal pH tracker turns a paper pH strip into a trend you can actually use. A " +
                    "single reading tells you almost nothing — a strip read once is a moment, not a " +
                    "pattern. The tracker holds your readings over weeks so you can see whether your " +
                    "vaginal pH sits in its typical range (about 3.8–4.5) over time, and whether " +
                    "anything shifts it — it varies naturally across your cycle.",
            ),
            ArticleBlock.Paragraph(
                "Genesyx does not sell strips. Any standard pH test strip from a pharmacy works, as " +
                    "long as it reads across the range the app accepts.",
            ),
            ArticleBlock.Heading("How to use it"),
            ArticleBlock.BulletList(
                listOf(
                    "Use the strip exactly as its packaging instructs, and read it within the time the instructions state.",
                    "Match the colour pad on the strip to the colour chart that came with it. Note the number it points to (e.g. 4.2).",
                    "Open the pH tab and tap \"Log pH\".",
                    "Move the slider to the value on your strip, or use the ± buttons. The app rounds to one decimal place.",
                    "Pick the time you took the reading (it defaults to now), add a note if you want, and Save.",
                ),
            ),
            ArticleBlock.Heading("For a trend you can trust"),
            ArticleBlock.BulletList(
                listOf(
                    "Log at a similar point in your routine each time, and note your cycle day alongside it — vaginal pH shifts naturally through the cycle, so that context makes the trend readable.",
                    "One reading a week is enough to see a trend over a couple of months. More is fine, but not required.",
                    "If a strip looks like it didn't read properly, read it again rather than saving a doubtful value.",
                ),
            ),
            ArticleBlock.Heading("When to speak to someone"),
            ArticleBlock.Paragraph(
                "If your readings stay above the typical healthy range over several days, or a " +
                    "pattern worries you, a GP, nurse, or pharmacist can talk it through with you. " +
                    "This tracker is a record for your own use — it isn't a diagnosis.",
            ),
            ArticleBlock.Heading("Conclusion"),
            ArticleBlock.Paragraph(
                "The tracker is a low-effort, weekly habit that turns a paper strip into a real " +
                    "trend. Log once a week, note your cycle day, and let the app hold the history. " +
                    "Don't chase a single number — chase the pattern.",
            ),
        ),
        category = ArticleCategory.GUIDES,
        tags = listOf("guide", "ph", "vaginal ph", "how to"),
        readingTime = "4 min read",
        relatedArticleIds = listOf("g3", "g6", "a4"),
        cta = ArticleCta(CtaType.OPEN_PH, "Open the pH tracker"),
        disclaimerRequired = true,
    ),
    Article(
        id = "g2",
        slug = "guide-how-the-log-works",
        title = "How the daily log works",
        excerpt = "What to log, what to skip, and why a boring one-tap entry beats a detailed one you abandon.",
        body = listOf(
            ArticleBlock.Heading("What this feature is for"),
            ArticleBlock.Paragraph(
                "The daily log is the heart of Genesyx. It's a short, honest record of how today " +
                    "went — written by a version of you who didn't yet know how the story ends. " +
                    "Memory rewrites the past to fit how you feel today; a log doesn't. That's the " +
                    "whole point.",
            ),
            ArticleBlock.Heading("How to use it"),
            ArticleBlock.BulletList(
                listOf(
                    "Tap \"Log today\" on Home, or \"Add to today's log\" on Track.",
                    "Everything is optional. Tap a mood, an energy level, any symptoms you noticed.",
                    "Add a custom symptom if yours isn't listed — type it once and it's there next time.",
                    "Tap sleep, water, or supplements to adjust those if you want — or skip them entirely.",
                    "Add a note if something specific happened (new medication, travel, illness, stress).",
                    "Tap \"Save log\". It saves on your device straight away and closes — even with no signal. If you're offline, it syncs by itself the next time you're connected, so nothing is lost.",
                ),
            ),
            ArticleBlock.Heading("What's worth logging"),
            ArticleBlock.BulletList(
                listOf(
                    "Mood — one tap, no elaboration.",
                    "Energy — the thing you'll most want to correlate later.",
                    "Anything unusual. If you noticed it, write it.",
                    "\"Nothing to report\" is a real entry. Log it.",
                ),
            ),
            ArticleBlock.Heading("Benefits if you use it"),
            ArticleBlock.Paragraph(
                "A one-tap mood logged for sixty days is worth more than a paragraph logged four " +
                    "times. The entries you'll want later are the ordinary ones — extremes you " +
                    "remember anyway. After a few cycles, your Insights tab starts to surface " +
                    "patterns: energy dips in the luteal phase, symptoms that cluster, hydration " +
                    "that slips on certain days. None of that exists without the boring, consistent log.",
            ),
            ArticleBlock.Heading("Conclusion"),
            ArticleBlock.Paragraph(
                "Log in under a minute, most days. Skip what you don't notice. The goal isn't a " +
                    "complete record — it's a record honest enough to disagree with you later. That's " +
                    "the habit that makes every other feature in the app work.",
            ),
        ),
        category = ArticleCategory.GUIDES,
        tags = listOf("guide", "log", "how to", "basics"),
        readingTime = "4 min read",
        relatedArticleIds = listOf("a2", "a3", "g5"),
        cta = ArticleCta(CtaType.OPEN_LOG, "Open today's log"),
        disclaimerRequired = false,
    ),
    Article(
        id = "g3",
        slug = "guide-how-to-log-ph",
        title = "How to log a pH reading",
        excerpt = "The exact taps to record a vaginal pH value, and how to keep your readings comparable.",
        body = listOf(
            ArticleBlock.Heading("What this feature is for"),
            ArticleBlock.Paragraph(
                "Logging a pH reading is how you feed the vaginal pH tracker. Each reading you save " +
                    "becomes a point on your trend chart and feeds your 7- and 30-day averages on " +
                    "Insights. The log screen rounds your value to one decimal place and keeps it " +
                    "within the scale the app accepts, so you can't accidentally save a misread strip.",
            ),
            ArticleBlock.Heading("How to use it"),
            ArticleBlock.BulletList(
                listOf(
                    "Open the pH tab and find the \"Vaginal pH\" card.",
                    "Tap \"Log pH\".",
                    "Move the slider to your strip's value, or use the ± buttons (each step is 0.1).",
                    "The big number and its colour update live — healthy (up to 4.5) or elevated (above 4.5).",
                    "If you want, change the time — useful if you're logging a reading you took earlier today.",
                    "Add a note if something was different (e.g. \"day 12 of my cycle\").",
                    "Tap \"Save\". The reading stores on your device immediately and syncs to your account if you're signed in.",
                ),
            ),
            ArticleBlock.Heading("Editing or deleting"),
            ArticleBlock.Paragraph(
                "To fix a mistake, open the pH card, tap the latest reading, and edit or delete it. " +
                    "Deletions are tombstoned — marked deleted rather than removed — so the change " +
                    "syncs safely if you're signed in.",
            ),
            ArticleBlock.Heading("Benefits if you use it"),
            ArticleBlock.Paragraph(
                "Each reading adds to a trend you can actually read. After a few weeks, the Insights " +
                    "pH card shows your current value with a status colour, a trend arrow (up, down, " +
                    "or flat vs your previous reading), and 7- and 30-day averages. That's a real " +
                    "signal — not a single number in isolation. It's also something concrete to bring " +
                    "to a clinician if a pattern persists.",
            ),
            ArticleBlock.Heading("Conclusion"),
            ArticleBlock.Paragraph(
                "Log once a week at a similar point in your routine, noting your cycle day. The " +
                    "slider makes it a ten-second task. Let the app hold the history and do the math — " +
                    "your job is just the consistent tap.",
            ),
        ),
        category = ArticleCategory.GUIDES,
        tags = listOf("guide", "ph", "how to", "vaginal ph"),
        readingTime = "3 min read",
        relatedArticleIds = listOf("g1", "g6", "a7"),
        cta = ArticleCta(CtaType.OPEN_PH, "Log a pH reading"),
        disclaimerRequired = true,
    ),
    Article(
        id = "g4",
        slug = "guide-nutrition-focus",
        title = "How your nutrition focus works",
        excerpt = "Why your focus foods change through your cycle, and what to actually do with them.",
        body = listOf(
            ArticleBlock.Heading("What this feature is for"),
            ArticleBlock.Paragraph(
                "Nutrition focus is phase-aware food guidance. As you move through your cycle — " +
                    "period, follicular, ovulatory, luteal — your body's needs shift slightly. The " +
                    "Nutrition tab shows focus foods and a short rationale tuned to whichever phase " +
                    "you're in, so you're not eating from a static list. It's gentle and specific, " +
                    "not a diet.",
            ),
            ArticleBlock.Heading("How to use it"),
            ArticleBlock.BulletList(
                listOf(
                    "Make sure your cycle is set up (Home → set your last period start). Without it, the tab can't pick a phase.",
                    "Open the Nutrition tab. The header shows today's phase.",
                    "The \"Focus foods\" card lists a few foods worth leaning into this phase, each with a short reason. Tap one to expand and see more detail.",
                    "The supplement plan card shows the evidence-informed essentials (folate, omega-3, vitamin D, zinc) — review it any time.",
                    "The \"Learn more\" section links to real nutrition articles; \"See all articles\" jumps to the full Learn library.",
                ),
            ),
            ArticleBlock.Heading("What it does and doesn't claim"),
            ArticleBlock.Paragraph(
                "Your appetite, energy, and cravings genuinely shift across a cycle — that's well " +
                    "established. But no food, supplement, or eating pattern changes your cycle " +
                    "length, your ovulation timing, or the sex of a future child. Claims otherwise " +
                    "are common, confidently stated, and unsupported. The focus foods are about " +
                    "working with your cycle, not manipulating it.",
            ),
            ArticleBlock.Heading("Benefits if you use it"),
            ArticleBlock.Paragraph(
                "Instead of a generic fertility diet, you get small, doable, phase-appropriate " +
                    "shifts — iron-rich foods during your period, steady energy in the follicular " +
                    "phase, smaller frequent meals if luteal energy dips. It removes the guesswork " +
                    "without overwhelming you. Over a couple of cycles, you'll notice which foods " +
                    "actually help you feel steady, and keep only what your own data supports.",
            ),
            ArticleBlock.Heading("Conclusion"),
            ArticleBlock.Paragraph(
                "Check Nutrition when you change phase, not every day. Pick one focus food to lean " +
                    "into, keep it if it works, drop it if it doesn't. The point is steady, " +
                    "phase-aware eating — not perfection.",
            ),
        ),
        category = ArticleCategory.GUIDES,
        tags = listOf("guide", "nutrition", "how to", "cycle"),
        readingTime = "4 min read",
        relatedArticleIds = listOf("a5", "a6", "g5"),
        cta = ArticleCta(CtaType.OPEN_NUTRITION, "See this phase's foods"),
        disclaimerRequired = true,
    ),
    Article(
        id = "g5",
        slug = "guide-how-hydration-works",
        title = "How the Hydration Coach works",
        excerpt = "Why the card changes through the day, and what the streak means.",
        body = listOf(
            ArticleBlock.Heading("What this feature is for"),
            ArticleBlock.Paragraph(
                "The Hydration Coach is a small card at the top of the Nutrition tab that helps you " +
                    "drink water steadily without thinking about it. It's not a nag — it's a live, " +
                    "time-of-day-aware nudge that tells you what to do right now. It's in-app only: " +
                    "no notifications, no buzzing, no permission.",
            ),
            ArticleBlock.Heading("How to use it"),
            ArticleBlock.BulletList(
                listOf(
                    "Open the Nutrition tab. The Hydration card sits at the top.",
                    "You'll see today's water total (e.g. 0.8 / 2.4 L), a progress bar, and + / − buttons.",
                    "Tap + each time you drink a glass (about 200 ml). Tap − if you over-counted.",
                    "Below the number is a bold coach line naming the part of day — \"Morning — start with a glass now\" in the morning, \"Midday — a glass with lunch\" around noon, \"Evening — small sips only\" at night. It changes through the day.",
                    "When you hit the 2.4 L goal, the line switches to a \"target reached\" message.",
                    "A small flame shows your daily streak — consecutive days you've logged any water.",
                ),
            ),
            ArticleBlock.Heading("Why it works this way"),
            ArticleBlock.Paragraph(
                "The most effective hydration habit is anchoring a glass to something you already " +
                    "do — a glass with each meal, one at your desk. The coach line suggests an anchor " +
                    "based on the time of day, so the decision is already made. Decisions are what " +
                    "habits die of; removing them is the whole trick.",
            ),
            ArticleBlock.Heading("Benefits if you use it"),
            ArticleBlock.Paragraph(
                "Steady hydration supports your energy and mood. The streak is there for " +
                    "motivation, but the app deliberately doesn't shame a broken one — missing a day " +
                    "costs almost nothing. The goal is consistency, not perfection.",
            ),
            ArticleBlock.Heading("Conclusion"),
            ArticleBlock.Paragraph(
                "Tap + when you drink. Let the coach line tell you what part of day it is and what " +
                    "to do. Don't chase the streak — chase the steady habit. A glass anchored to a " +
                    "meal, most days, is the whole job.",
            ),
        ),
        category = ArticleCategory.GUIDES,
        tags = listOf("guide", "hydration", "how to", "coach"),
        readingTime = "4 min read",
        relatedArticleIds = listOf("a4", "g3", "g6"),
        cta = ArticleCta(CtaType.OPEN_NUTRITION, "Open the Hydration Coach"),
        disclaimerRequired = false,
    ),
    Article(
        id = "g6",
        // Slug kept from when this article covered the pH card's old home on Nutrition. pH has its
        // own tab now and the piece was rewritten around the trend chart, but a slug is a route and
        // a read-history key, so it stays.
        slug = "guide-track-ph-in-nutrition",
        title = "Reading your pH trend",
        excerpt = "What the chart, the range selector, and the status colour are actually telling you.",
        body = listOf(
            ArticleBlock.Heading("What this feature is for"),
            ArticleBlock.Paragraph(
                "Logging a reading is the easy half. The chart on the pH tab is the half that pays " +
                    "you back: it holds every reading you've saved and draws the shape they make. A " +
                    "shape is something you can read. A single number isn't.",
            ),
            ArticleBlock.Heading("How to read the card"),
            ArticleBlock.BulletList(
                listOf(
                    "Open the pH tab. Your latest reading sits at the top with the date and time you took it.",
                    "The colour beside it is its status — healthy up to 4.5, elevated above that.",
                    "Below is the chart. The shaded bands mark the same two ranges, so you can see where each point falls without reading the axis.",
                    "The range selector (7d, 30d, 90d, All) changes how much history the chart draws. It needs at least two readings in the range to draw a line.",
                ),
            ),
            ArticleBlock.Heading("What the shape is worth"),
            ArticleBlock.BulletList(
                listOf(
                    "A steady band you return to is your normal. That's the useful thing to know.",
                    "One point outside it is noise. Vaginal pH moves with your cycle, with your period, and with the time of day you measured.",
                    "The same movement at the same point across two or three cycles is a pattern — and that is worth writing down alongside your cycle day.",
                ),
            ),
            ArticleBlock.Heading("Where it goes next"),
            ArticleBlock.Paragraph(
                "The Insights tab reads the same readings and adds your 7- and 30-day averages plus " +
                    "a trend arrow against your previous reading. Nothing is recalculated differently " +
                    "there — it's the same history, summarised.",
            ),
            ArticleBlock.Heading("When to speak to someone"),
            ArticleBlock.Paragraph(
                "If your readings sit above the usual range for several days, or the pattern worries " +
                    "you, a GP, nurse, or pharmacist can talk it through. The chart is a record for " +
                    "your own use — it isn't a diagnosis.",
            ),
            ArticleBlock.Heading("Conclusion"),
            ArticleBlock.Paragraph(
                "Log weekly, note your cycle day, and look at the 90-day view after a couple of " +
                    "months rather than the number after every strip. Read the shape, not the point.",
            ),
        ),
        category = ArticleCategory.GUIDES,
        tags = listOf("guide", "ph", "trends", "how to"),
        readingTime = "3 min read",
        relatedArticleIds = listOf("g1", "g3", "g5"),
        cta = ArticleCta(CtaType.OPEN_PH, "Open the pH tab"),
        disclaimerRequired = true,
    ),
    Article(
        id = "g7",
        slug = "guide-cycle-and-phases",
        title = "How your cycle and phases work",
        excerpt = "Where the phase names come from, what the app is predicting, and how much to trust it.",
        body = listOf(
            ArticleBlock.Heading("What this feature is for"),
            ArticleBlock.Paragraph(
                "Almost everything else in the app is built on top of your cycle. Your phase decides " +
                    "which focus foods Nutrition shows you, which colours the calendar draws, and " +
                    "when the fertile-window nudge fires. Set it up once and the rest follows. Skip " +
                    "it and the app can still log, but it cannot tell you where you are.",
            ),
            ArticleBlock.Heading("Setting it up"),
            ArticleBlock.BulletList(
                listOf(
                    "On Home, tap \"Start tracking\" on the welcome card. (It's the same editor as Profile → Health Profile, if you come back to it later.)",
                    "Enter the first day of your last period. This one is required — nothing is guessed for you, and Save stays greyed out until you pick a date.",
                    "Confirm your cycle length. The default is 28 days; anything from 21 to 35 is accepted.",
                    "Confirm your period length. The default is 5 days; 1 to 10 is accepted.",
                    "The welcome card is replaced by your phase card as soon as you save.",
                ),
            ),
            ArticleBlock.Heading("The four phases"),
            ArticleBlock.BulletList(
                listOf(
                    "Period — from day 1 for as many days as you set your period length.",
                    "Follicular — from the end of your period up to the day before ovulation.",
                    "Ovulatory — the single predicted ovulation day.",
                    "Luteal — from the day after ovulation until your next period starts.",
                ),
            ),
            ArticleBlock.Heading("Where the predicted dates come from"),
            ArticleBlock.Paragraph(
                "The app places ovulation 14 days before your next period is due, and draws the " +
                    "fertile window from five days before that day to one day after. On a 28-day " +
                    "cycle that puts ovulation on day 14 and the window across days 9 to 15. Change " +
                    "your cycle length and both move with it.",
            ),
            ArticleBlock.Paragraph(
                "The 14-day figure is a convention, not a measurement. The luteal phase is the more " +
                    "consistent half of the cycle for most people, which is why the estimate is " +
                    "anchored there — but it is still an average applied to you, not an observation " +
                    "of this particular cycle.",
            ),
            ArticleBlock.Heading("Where your phase shows up"),
            ArticleBlock.BulletList(
                listOf(
                    "Home — the phase name, the day of your cycle, days until your next period, and your predicted ovulation day.",
                    "Track — the calendar tints each day by what it is. The legend underneath names the five: Period, Fertile window, Ovulation, Luteal, Follicular. A card below it repeats your current phase in words.",
                    "Nutrition — the header carries today's phase, and a card announces the day it changes.",
                    "Insights — a timeline of the whole cycle with your ovulation day, your fertile window, and where today sits on it.",
                    "A notification — on the morning your predicted window opens, if you've turned that one on. It is deliberately vague (\"A note about your cycle\") because a lock screen is read by whoever is holding the phone.",
                ),
            ),
            ArticleBlock.Heading("How much to trust it"),
            ArticleBlock.Paragraph(
                "These are predictions from your averages, so they are a starting point rather than " +
                    "an answer. Cycles vary, and the first month after you set the app up is the " +
                    "least reliable one — it has nothing but your defaults to work from. What makes " +
                    "it better is logging: the more of your own data sits behind the estimate, the " +
                    "more it describes you rather than a textbook.",
            ),
            ArticleBlock.Paragraph(
                "This is not contraception, and it should not be used as any part of it. If your " +
                    "cycles are very irregular, or something changes and stays changed, a GP or nurse " +
                    "is the right place to take that.",
            ),
            ArticleBlock.Heading("Conclusion"),
            ArticleBlock.Paragraph(
                "Set your last period date honestly, correct it when a period arrives early or late, " +
                    "and read the fertile window as a likely range rather than a date. The " +
                    "predictions are only ever as good as the dates you give them.",
            ),
        ),
        category = ArticleCategory.GUIDES,
        tags = listOf("guide", "cycle", "phases", "how to"),
        readingTime = "5 min read",
        // "w1" (weekly series) re-added when that series is ported — the Android integrity test
        // rejects a related id that doesn't resolve, so only landed articles are listed.
        relatedArticleIds = listOf("g4", "g2"),
        cta = ArticleCta(CtaType.OPEN_TRACK, "Open your calendar"),
        disclaimerRequired = true,
    ),
    Article(
        id = "g8",
        slug = "guide-sleep-tracking",
        title = "How sleep tracking works",
        excerpt = "A duration you enter yourself, what the week's chart adds up to, and what it deliberately doesn't do.",
        body = listOf(
            ArticleBlock.Heading("What this feature is for"),
            ArticleBlock.Paragraph(
                "Sleep here is one number a day: how long you slept. You enter it — nothing is " +
                    "measured, and the app never touches your phone's sensors or your watch. That " +
                    "sounds like a limitation and mostly isn't: a rough figure you actually record " +
                    "every day is worth more than a precise one you stop looking at.",
            ),
            ArticleBlock.Heading("Logging a night"),
            ArticleBlock.BulletList(
                listOf(
                    "Open the Track tab and find Sleep under \"Your trackers\". The row shows what you've entered for today, or a dash if you haven't entered anything, with the last seven nights sketched beside it.",
                    "Tap it. The sheet opens on 7h 0m for a day you haven't logged — a starting point to nudge, not a recommendation.",
                    "Set hours (up to 12) and minutes (in fives).",
                    "Tap Save. Tap the row again any time to change it, or Clear to remove the entry entirely.",
                    "A small badge tells you where the entry is: saved on the device, synced to your account, or waiting for a connection.",
                ),
            ),
            ArticleBlock.Heading("What the week adds up to"),
            ArticleBlock.BulletList(
                listOf(
                    "The Insights tab draws this week as bars, Monday to Sunday. It is the calendar week, not your last seven nights — so on a Wednesday you'll see three bars and four empty ones waiting.",
                    "Beside them: your nightly average, and \"nights logged\" out of seven.",
                    "A line underneath reads the two together — how much you're averaging, over how many nights.",
                ),
            ),
            ArticleBlock.Paragraph(
                "The average deliberately ignores the nights you skipped rather than counting them " +
                    "as zero, so a missed entry lowers your \"nights logged\" and leaves the average " +
                    "honest. The bars for those nights are drawn flat and grey rather than left out, " +
                    "because a gap you can see is more useful than one you can't.",
            ),
            ArticleBlock.Heading("What it counts towards"),
            ArticleBlock.Paragraph(
                "A night of sleep on its own is enough to make a day count as logged, so it keeps " +
                    "your streak alive by itself. It also feeds your weekly summary, which compares " +
                    "this week's average against last week's — but only when both weeks have sleep in " +
                    "them, because a comparison against nothing is not a comparison.",
            ),
            ArticleBlock.Heading("What it doesn't do"),
            ArticleBlock.Paragraph(
                "There's no quality rating, no bedtime or wake time, no stages, and no attempt to " +
                    "relate your sleep to your cycle phase. If you want the relationship between rest, " +
                    "stress and your cycle, that's an article rather than a feature — the app records " +
                    "the hours and leaves the reading to you.",
            ),
            ArticleBlock.Heading("Conclusion"),
            ArticleBlock.Paragraph(
                "Enter last night roughly, first thing, and don't agonise over fifteen minutes " +
                    "either way. Seven entries that are approximately right will tell you more about " +
                    "your week than two that are exact.",
            ),
        ),
        category = ArticleCategory.GUIDES,
        tags = listOf("guide", "sleep", "how to", "tracking"),
        readingTime = "3 min read",
        // "w7" (weekly series) re-added when that series is ported (see g7).
        relatedArticleIds = listOf("g2", "a7"),
        cta = ArticleCta(CtaType.OPEN_TRACK, "Log last night's sleep"),
        disclaimerRequired = false,
    ),
    Article(
        id = "g9",
        slug = "guide-logging-symptoms",
        title = "How to log how you feel",
        excerpt = "The daily log, what's in it, and what a month of symptoms can show you.",
        body = listOf(
            ArticleBlock.Heading("What this feature is for"),
            ArticleBlock.Paragraph(
                "Symptoms are the part of a cycle you notice and then forget. Logging them takes " +
                    "about fifteen seconds and turns \"I think I get headaches before my period\" " +
                    "into something you can actually check.",
            ),
            ArticleBlock.Heading("Opening the log"),
            ArticleBlock.BulletList(
                listOf(
                    "From Home, tap \"Log today\".",
                    "Or from Track, tap \"Add to today's log\".",
                    "For a day you missed, tap it in the calendar and choose \"Add a log\" — or \"Edit this day\" if something's already there. The sheet titles itself with the day so you always know which one you're writing on.",
                    "Future days can't be logged. There's nothing to record about a day that hasn't happened.",
                ),
            ),
            ArticleBlock.Heading("What's in the sheet, in order"),
            ArticleBlock.BulletList(
                listOf(
                    "Mood — Great, Good, Okay, or Low.",
                    "Energy — Low, Normal, or High.",
                    "Symptoms — headache, fatigue, cramps, nausea, bloating, acne, backache, tender breasts. Tap as many as apply; tap again to remove one.",
                    "Intimacy — a single \"Sex\" chip. It is private to you: a linked partner sees your name, never your logs.",
                    "Sleep, water and supplements — three small cards that open their own sheets.",
                    "Notes — anything the chips don't cover.",
                    "Then \"Save log\".",
                ),
            ),
            ArticleBlock.Heading("Symptoms that aren't on the list"),
            ArticleBlock.Paragraph(
                "Tap \"Add\" at the end of the chips and type your own. It's kept and offered " +
                    "alongside the standard ones from then on, so name it the way you'll recognise " +
                    "it — the same wording every time is what lets a pattern show up later.",
            ),
            ArticleBlock.Heading("What happens to it"),
            ArticleBlock.BulletList(
                listOf(
                    "The day gets a dot on the calendar, under \"Symptoms / notes\" in the legend.",
                    "Insights builds a four-week view of how many symptoms you logged each day, and names the one you recorded most often with a count.",
                    "Anything at all in the log keeps your daily streak going — symptoms alone are enough.",
                ),
            ),
            ArticleBlock.Paragraph(
                "Until you've logged something on seven separate days, Insights says \"too soon to " +
                    "read patterns\" rather than guessing. A pattern needs something to be a pattern " +
                    "across, and a confident-sounding summary drawn from three days would be worse " +
                    "than an honest blank.",
            ),
            ArticleBlock.Heading("What it is and isn't"),
            ArticleBlock.Paragraph(
                "This is a record you keep, not an interpretation the app makes. Nothing here " +
                    "decides that a symptom belongs to a phase or means something about your health — " +
                    "it counts what you entered and shows you the count. That record is genuinely " +
                    "useful to bring to a GP appointment, where the interpreting belongs.",
            ),
            ArticleBlock.Heading("Conclusion"),
            ArticleBlock.Paragraph(
                "Log on the bad days especially — those are the ones worth being able to look back " +
                    "at. Use the same words each time, and give it a month before you expect the " +
                    "picture to say anything.",
            ),
        ),
        category = ArticleCategory.GUIDES,
        tags = listOf("guide", "symptoms", "logging", "how to"),
        readingTime = "4 min read",
        relatedArticleIds = listOf("g2", "a3", "a8"),
        cta = ArticleCta(CtaType.OPEN_LOG, "Log how you feel today"),
        disclaimerRequired = false,
    ),
    Article(
        id = "g10",
        slug = "guide-understanding-vaginal-ph",
        title = "Understanding your vaginal pH",
        excerpt = "What vaginal pH is, what a typical healthy range looks like, and how to take a reading you can trust.",
        body = listOf(
            ArticleBlock.Heading("What vaginal pH is"),
            ArticleBlock.Paragraph(
                "Vaginal pH is a simple, everyday signal of intimate wellbeing. For most adults it " +
                    "typically sits within a healthy range of about 3.8 to 4.5. It shifts naturally " +
                    "across your cycle, so your own pattern over time tells you more than any single " +
                    "reading.",
            ),
            ArticleBlock.Heading("How this tracker reads it"),
            ArticleBlock.Paragraph(
                "You log each reading from 3.8 to 7.0, to one decimal place. Readings at or below " +
                    "4.5 are shown as within the typical healthy range; readings above 4.5 are shown " +
                    "as above it. That's all it does — it records your trend, it doesn't interpret it " +
                    "for you.",
            ),
            ArticleBlock.Heading("Why timing matters"),
            ArticleBlock.Paragraph(
                "A few everyday things can temporarily shift a reading and make it less " +
                    "representative of your usual pattern.",
            ),
            ArticleBlock.BulletList(
                listOf(
                    "During or just after your period",
                    "Within 24 hours of sex",
                    "After using lubricants or douches",
                    "During or soon after a course of antibiotics",
                ),
            ),
            ArticleBlock.Paragraph(
                "For the most reliable picture, take your reading away from these times. If one " +
                    "reading looks unusual, log a few more over the following days and watch the " +
                    "trend rather than the one number.",
            ),
            ArticleBlock.Heading("Tracking, not diagnosis"),
            ArticleBlock.Paragraph(
                "This is a wellness tracker to help you notice your own patterns — it isn't medical " +
                    "advice and doesn't diagnose anything. If your readings stay above the typical " +
                    "range over several days and you also notice symptoms that concern you, a GP, " +
                    "nurse, or pharmacist can talk it through with you.",
            ),
        ),
        category = ArticleCategory.GUIDES,
        tags = listOf("ph"),
        readingTime = "3 min read",
        relatedArticleIds = emptyList(),
        cta = ArticleCta(CtaType.OPEN_PH, "Open the pH tracker"),
        disclaimerRequired = true,
    ),
)
