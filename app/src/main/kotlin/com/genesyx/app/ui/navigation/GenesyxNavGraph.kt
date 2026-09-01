package com.genesyx.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.genesyx.app.core.FeatureFlags
import com.genesyx.app.ui.clients.ClientsScreen
import com.genesyx.app.ui.history.LogHistoryScreen
import com.genesyx.app.ui.home.HomeScreen
import com.genesyx.app.ui.insights.InsightsScreen
import com.genesyx.app.ui.learn.ArticleDetailScreen
import com.genesyx.app.ui.learn.HowToUseScreen
import com.genesyx.app.ui.learn.LearnScreen
import com.genesyx.app.ui.learn.LearnSearchScreen
import com.genesyx.app.ui.learn.TwelveWeekPlanScreen
import com.genesyx.app.ui.onboarding.OnboardingIntroScreen
import com.genesyx.app.ui.onboarding.OnboardingQuizScreen
import com.genesyx.app.ui.onboarding.ReadinessSummaryScreen
import com.genesyx.app.ui.onboarding.SplashScreen
import com.genesyx.app.ui.onboarding.WaitlistScreen
import com.genesyx.app.ui.screens.AuthScreen
import com.genesyx.app.ui.screens.InviteScreen
import com.genesyx.app.ui.screens.LogScreen
import com.genesyx.app.ui.screens.ResetPasswordScreen
import com.genesyx.app.ui.nutrition.NutritionScreen
import com.genesyx.app.ui.settings.ReminderSettingsScreen
import com.genesyx.app.ui.track.detail.CycleDetailScreen
import com.genesyx.app.ui.track.detail.HydrationDetailScreen
import com.genesyx.app.ui.track.detail.NutritionDetailScreen
import com.genesyx.app.ui.track.detail.PhDetailScreen
import com.genesyx.app.ui.track.detail.SleepDetailScreen
import com.genesyx.app.ui.track.detail.SymptomsDetailScreen
import com.genesyx.app.ui.profile.ProfileScreen
import com.genesyx.app.ui.screens.PregnancyScreen
import com.genesyx.app.ui.track.TrackScreen

@Composable
fun GenesyxNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Splash.route,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        // ── Onboarding flow (each step pops itself off the back stack)
        composable(Screen.Splash.route) {
            SplashScreen(
                onStart = {
                    navController.navigate(Screen.OnboardingIntro.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onSignIn = { navController.navigate(Screen.Auth.route) },
            )
        }
        composable(Screen.OnboardingIntro.route) {
            OnboardingIntroScreen(
                onContinue = { navController.navigate(Screen.OnboardingQuiz.route) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.OnboardingQuiz.route) {
            OnboardingQuizScreen(
                onComplete = {
                    navController.navigate(Screen.ReadinessSummary.route) {
                        popUpTo(Screen.OnboardingQuiz.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.ReadinessSummary.route) {
            ReadinessSummaryScreen(
                onUnlockGuide = { navController.navigate(Screen.FreeGuide.route) },
                // Dashboard is gated behind an account: send guests to register/login, not Home.
                onContinue = { navController.navigate(Screen.Auth.route) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.Waitlist.route) {
            WaitlistScreen(
                // Same gate: the free-guide path must also register/login before the dashboard.
                onContinue = { navController.navigate(Screen.Auth.route) },
                onBack = { navController.popBackStack() },
            )
        }

        // ── Main tabs. Reminder taps deep-link straight to the relevant tab (genesyx://<host>).
        composable(
            Screen.Home.route,
            deepLinks = listOf(navDeepLink { uriPattern = "genesyx://home" }),
        ) { HomeScreen(navController) }
        composable(
            Screen.Track.route,
            deepLinks = listOf(navDeepLink { uriPattern = "genesyx://track" }),
        ) { TrackScreen(navController) }
        composable(
            Screen.Nutrition.route,
            // Optional: present when we were sent here to show the supplement plan (pH's card).
            arguments = listOf(
                navArgument(Screen.Nutrition.ARG_PLAN) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
            deepLinks = listOf(navDeepLink { uriPattern = "genesyx://nutrition" }),
        ) { entry ->
            NutritionScreen(
                navController,
                openPlan = entry.arguments?.getString(Screen.Nutrition.ARG_PLAN) != null,
                reselects = entry.savedStateHandle.getStateFlow(TabNavigation.RESELECT_KEY, 0),
            )
        }
        composable(
            Screen.Insights.route,
            deepLinks = listOf(navDeepLink { uriPattern = "genesyx://insights" }),
        ) { InsightsScreen(navController) }
        composable(Screen.Profile.route) { ProfileScreen(navController) }

        // ── Secondary / modal destinations
        composable(
            Screen.Log.route,
            // Optional: an ISO date to edit a past day (Track calendar). Absent = today.
            arguments = listOf(
                navArgument(Screen.Log.ARG_DATE) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
            deepLinks = listOf(navDeepLink { uriPattern = "genesyx://log" }),
        ) {
            LogScreen(
                onClose = { navController.popBackStack() },
                onOpenArticle = { slug -> navController.navigate(Screen.ArticleDetail.create(slug)) },
            )
        }
        composable(Screen.ReminderSettings.route) {
            ReminderSettingsScreen(onBack = { navController.popBackStack() })
        }

        // ── Tracker details (reached from Track's "Your Trackers" list + Home/notification deep
        // links). Each carries genesyx://tracker/{name} so a notification can route to the exact
        // tracker; opened cold, the nav host builds a synthetic back stack to the start destination.
        composable(
            Screen.CycleDetail.route,
            deepLinks = listOf(navDeepLink { uriPattern = "genesyx://tracker/cycle" }),
        ) {
            CycleDetailScreen(onBack = { navController.popBackStack() })
        }
        composable(
            Screen.HydrationDetail.route,
            deepLinks = listOf(navDeepLink { uriPattern = "genesyx://tracker/hydration" }),
        ) {
            HydrationDetailScreen(onBack = { navController.popBackStack() })
        }
        composable(
            Screen.PhDetail.route,
            deepLinks = listOf(navDeepLink { uriPattern = "genesyx://tracker/ph" }),
        ) {
            PhDetailScreen(
                // "See your supplement plan" means the plan, not the Nutrition tab it lives on.
                // No `restoreState`: restoring a saved Nutrition entry would drop the `plan`
                // argument and land her on the tab with nothing open — the reported bug.
                onOpenPlan = {
                    navController.navigate(Screen.Nutrition.create(openPlan = true)) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                    }
                },
                onOpenArticle = { slug -> navController.navigate(Screen.ArticleDetail.create(slug)) },
            )
        }
        composable(
            Screen.SleepDetail.route,
            deepLinks = listOf(navDeepLink { uriPattern = "genesyx://tracker/sleep" }),
        ) {
            SleepDetailScreen(onBack = { navController.popBackStack() })
        }
        composable(
            Screen.SymptomsDetail.route,
            deepLinks = listOf(navDeepLink { uriPattern = "genesyx://tracker/symptoms" }),
        ) {
            SymptomsDetailScreen(
                onBack = { navController.popBackStack() },
                onEditToday = { navController.navigate(Screen.Log.create()) },
                onOpenHistory = { navController.navigate(Screen.LogHistory.route) },
            )
        }
        composable(
            Screen.NutritionDetail.route,
            deepLinks = listOf(navDeepLink { uriPattern = "genesyx://tracker/nutrition" }),
        ) {
            NutritionDetailScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.LogHistory.route) {
            LogHistoryScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Pregnancy.route) {
            PregnancyScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Clients.route) {
            ClientsScreen(onBack = { navController.popBackStack() })
        }

        // ── Learn
        composable(
            Screen.Learn.route,
            // Fallback when no dated article released today.
            deepLinks = listOf(navDeepLink { uriPattern = "genesyx://learn" }),
        ) { LearnScreen(navController) }
        composable(Screen.HowToUse.route) { HowToUseScreen(navController) }
        composable(Screen.TwelveWeekPlan.route) { TwelveWeekPlanScreen(navController) }
        composable(Screen.FreeGuide.route) { com.genesyx.app.ui.learn.FreeGuideScreen(navController) }
        composable(Screen.MedicalSources.route) { com.genesyx.app.ui.profile.MedicalSourcesScreen(navController) }
        composable(Screen.LearnSearch.route) { LearnSearchScreen(navController) }
        composable(
            route = Screen.ArticleDetail.route,
            arguments = listOf(navArgument(Screen.ArticleDetail.ARG_SLUG) { type = NavType.StringType }),
            deepLinks = listOf(navDeepLink { uriPattern = "genesyx://learn/article/{slug}" }),
        ) { backStackEntry ->
            val slug = backStackEntry.arguments?.getString(Screen.ArticleDetail.ARG_SLUG).orEmpty()
            ArticleDetailScreen(slug = slug, navController = navController)
        }
        composable(Screen.Auth.route) {
            AuthScreen(
                onSignedIn = {
                    // Clear the whole onboarding/auth stack so back can't return to the gate.
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        // ── Password-recovery deep link: genesyx://reset-password (the Supabase recovery email's
        // redirect target; see auth/RecoveryLink.kt). The screen imports the session the link
        // carries, then asks for the new password.
        composable(
            Screen.ResetPassword.route,
            deepLinks = listOf(navDeepLink { uriPattern = "genesyx://reset-password" }),
        ) {
            ResetPasswordScreen(
                onDone = {
                    // She is signed in — same stack clear as AuthScreen's onSignedIn.
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onRequestNewLink = {
                    // Back to the gate, where "Forgot password?" lives.
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        // ── Partner invite deep link: genesyx://invite/{code} + https app link.
        // Registered only while the feature flag is on — with it off, the destination previously
        // stayed reachable by deep link and would "link" a placeholder partner. An incoming invite
        // link with the flag off now just opens the app at its start destination.
        if (FeatureFlags.PARTNER_INVITES) {
            composable(
                route = Screen.Invite.route,
                arguments = listOf(navArgument(Screen.Invite.ARG_CODE) { type = NavType.StringType }),
                deepLinks = listOf(
                    navDeepLink { uriPattern = "genesyx://invite/{code}" },
                    navDeepLink { uriPattern = "https://genesis-cycle-guide.lovable.app/invite/{code}" },
                ),
            ) { backStackEntry ->
                val code = backStackEntry.arguments?.getString(Screen.Invite.ARG_CODE).orEmpty()
                InviteScreen(
                    code = code,
                    onAccepted = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                    onBack = { navController.navigate(Screen.Splash.route) },
                    onSignIn = { navController.navigate(Screen.Auth.route) },
                )
            }
        }
    }
}
