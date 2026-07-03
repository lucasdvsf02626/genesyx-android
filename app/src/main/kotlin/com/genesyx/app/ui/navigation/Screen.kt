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
    data object Pregnancy : Screen("pregnancy")
    data object Auth : Screen("auth")
    data object Clients : Screen("clients")

    // Deep link: genesyx://invite/{code}
    data object Invite : Screen("invite/{code}") {
        fun create(code: String) = "invite/$code"
        const val ARG_CODE = "code"
    }

    companion object {
        /** Tabs shown in the bottom navigation bar. */
        val bottomTabs by lazy { listOf(Home, Track, Nutrition, Insights, Profile) }

        /** Routes where the bottom navigation bar is hidden. */
        val noBottomNavRoutes by lazy {
            setOf(
            Splash.route,
            OnboardingIntro.route,
            OnboardingQuiz.route,
            ReadinessSummary.route,
            Waitlist.route,
            Log.route,
            Pregnancy.route,
            Auth.route,
            Invite.route,
            Clients.route,
            )
        }
    }
}
