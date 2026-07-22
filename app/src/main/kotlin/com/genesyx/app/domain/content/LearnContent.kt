package com.genesyx.app.domain.content

import androidx.annotation.DrawableRes
import com.genesyx.app.R

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
}

/** Structured body — avoids a Markdown dependency and keeps articles previewable and type-safe. */
sealed interface ArticleBlock {
    data class Heading(val text: String) : ArticleBlock
    data class Paragraph(val text: String) : ArticleBlock
    data class BulletList(val items: List<String>) : ArticleBlock
    data class Callout(val text: String) : ArticleBlock
}

/** Where an article's closing call-to-action sends the reader. */
enum class CtaType { OPEN_LOG, OPEN_TRACK, OPEN_NUTRITION, OPEN_INSIGHTS, OPEN_ARTICLE }

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
)

/** Shown whenever [Article.disclaimerRequired]. */
const val MEDICAL_DISCLAIMER =
    "This is educational content, not medical advice. It can't account for your individual " +
        "circumstances, and it isn't a substitute for talking to a doctor, nurse, or pharmacist. " +
        "If something feels wrong, or you're worried, please speak to a healthcare professional."

fun articleBySlug(slug: String): Article? = learnArticles.firstOrNull { it.slug == slug }

fun relatedArticles(article: Article): List<Article> =
    article.relatedArticleIds.mapNotNull { id -> learnArticles.firstOrNull { it.id == id } }

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
)
