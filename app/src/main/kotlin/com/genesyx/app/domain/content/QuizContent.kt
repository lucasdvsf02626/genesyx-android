package com.genesyx.app.domain.content

/** Onboarding quiz content — verbatim from the web app (mockData.ts) + screenshots. */

data class QuizOption(val id: String, val label: String)

data class DidYouKnow(val title: String, val body: String)

data class QuizQuestion(
    val id: String,
    val question: String,
    val helper: String,
    val options: List<QuizOption>,
    /** Shown after answering this question, before advancing. */
    val fact: DidYouKnow? = null,
    /**
     * Skippable. Only the baby-sex preference is: it is the one question here that is nobody's
     * business but hers, and the client's brief requires it be optional. Everything else feeds the
     * personalisation the summary promises, so it stays required.
     */
    val optional: Boolean = false,
)

val quizQuestions: List<QuizQuestion> = listOf(
    QuizQuestion(
        id = "stage",
        question = "Where are you in your conception journey?",
        helper = "There's no wrong answer — we'll tailor your experience.",
        options = listOf(
            QuizOption("exploring", "Just starting to think about it"),
            QuizOption("preparing", "Actively preparing my body"),
            QuizOption("trying", "Trying to conceive now"),
            QuizOption("support", "Looking for extra support"),
        ),
    ),
    QuizQuestion(
        id = "cycle",
        question = "How regular does your cycle usually feel?",
        helper = "An honest answer helps us personalise your insights.",
        options = listOf(
            QuizOption("very", "Very regular, predictable"),
            QuizOption("mostly", "Mostly regular with small shifts"),
            QuizOption("irregular", "Often irregular"),
            QuizOption("unsure", "I'm not sure yet"),
        ),
        fact = DidYouKnow(
            title = "Did you know?",
            body = "Only about 13% of cycles are exactly 28 days. A healthy cycle can range " +
                "from 21 to 35 days — your rhythm is uniquely yours, and tracking it reveals " +
                "your most fertile window.",
        ),
    ),
    QuizQuestion(
        id = "supplements",
        question = "Are you currently taking fertility supplements?",
        helper = "We'll build a plan that fits where you are.",
        options = listOf(
            QuizOption("yes", "Yes, a full routine"),
            QuizOption("some", "A few key ones"),
            QuizOption("no", "Not yet"),
            QuizOption("guidance", "I'd love guidance on this"),
        ),
    ),
    QuizQuestion(
        id = "gender",
        question = "Do you have a preference for your baby's sex?",
        helper = "This is just for you — we keep it gentle and private.",
        options = listOf(
            QuizOption("girl", "Girl"),
            QuizOption("boy", "Boy"),
            QuizOption("no_preference", "No preference"),
            QuizOption("prefer_not_to_say", "Prefer not to say"),
        ),
        optional = true,
    ),
    QuizQuestion(
        id = "support",
        question = "What would you like the most support with?",
        helper = "Choose what feels most important right now.",
        options = listOf(
            QuizOption("nutrition", "Fertility nutrition guidance"),
            QuizOption("tracking", "Understanding my cycle"),
            QuizOption("supplements", "Supplement support"),
            QuizOption("emotional", "Feeling calm and informed"),
        ),
    ),
)
