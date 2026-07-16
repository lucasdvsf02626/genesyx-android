package com.genesyx.app.ui.navigation

/** All navigable destinations. Mirrors the web app's `flow` + `tab` state machine. */
sealed class Screen(val route: String) {
    // Onboarding flow
    data object Splash : Screen("splash")
    data object OnboardingIntro : Screen("onboarding_intro")
    data object OnboardingQuiz : Screen("onboarding_quiz")
    data object ReadinessSummary : Screen("readiness_summary")
    data object Waitlist : Screen("waitlist")

    // Main tabs
    data object Home : Screen("home")
    data object Track : Screen("track")
    data object Nutrition : Screen("nutrition")
    data object Insights : Screen("insights")
    data object Profile : Screen("profile")

    // Modal / secondary destinations
    data object Log : Screen("log")
    data object LogHistory : Screen("log_history")
    data object Pregnancy : Screen("pregnancy")
    data object Auth : Screen("auth")
    data object Clients : Screen("clients")
    data object ReminderSettings : Screen("reminder_settings")

    // Tracker detail destinations, reached from Track's "Your Trackers" list and Home deep links.
    data object CycleDetail : Screen("tracker/cycle")
    data object HydrationDetail : Screen("tracker/hydration")
    data object PhDetail : Screen("tracker/ph")
    data object SleepDetail : Screen("tracker/sleep")
    data object SymptomsDetail : Screen("tracker/symptoms")
    data object NutritionDetail : Screen("tracker/nutrition")

    // Learn. Bottom tab (took Profile's slot); also entered from Nutrition's "Learn more" section.
    data object Learn : Screen("learn")
    data object LearnSearch : Screen("learn/search")
    data object ArticleDetail : Screen("learn/article/{slug}") {
        fun create(slug: String) = "learn/article/$slug"
        const val ARG_SLUG = "slug"
    }

    // Deep link: genesyx://invite/{code}
    data object Invite : Screen("invite/{code}") {
        fun create(code: String) = "invite/$code"
        const val ARG_CODE = "code"
    }

    companion object {
        /**
         * Tabs shown in the bottom navigation bar. Six, by product decision — one past the Material 3
         * recommended maximum of five. Labels are tight at 360dp; check any label change on a small
         * screen before shipping it.
         */
        val bottomTabs by lazy { listOf(Home, Track, Nutrition, Insights, Learn, Profile) }

        /** Routes where the bottom navigation bar is hidden. */
        val noBottomNavRoutes by lazy {
            setOf(
            Splash.route,
            OnboardingIntro.route,
            OnboardingQuiz.route,
            ReadinessSummary.route,
            Waitlist.route,
            Log.route,
            LogHistory.route,
            Pregnancy.route,
            Auth.route,
            Invite.route,
            Clients.route,
            ReminderSettings.route,
            // Tracker details are immersive editors reached from Track; the bar is hidden so back
            // returns to Track (or to Home when deep-linked from a Home summary card).
            CycleDetail.route,
            HydrationDetail.route,
            PhDetail.route,
            SleepDetail.route,
            SymptomsDetail.route,
            NutritionDetail.route,
            // Reading and searching are immersive. `Learn` itself keeps the bar (it's a tab).
            ArticleDetail.route,
            LearnSearch.route,
            )
        }
    }
}
