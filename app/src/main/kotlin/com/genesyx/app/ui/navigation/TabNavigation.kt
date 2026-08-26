package com.genesyx.app.ui.navigation

import androidx.navigation.NavController

/**
 * The one way to land on a bottom-tab root.
 *
 * Every tab switch must pop to Home with `saveState` and navigate with `launchSingleTop` +
 * `restoreState`, and — the part that bit us — so must every in-screen link that targets another
 * tab. A plain `navigate(Screen.Learn.route)` from inside Nutrition pushed the Learn root *on top
 * of* Nutrition; the next tab switch saved that whole chain under Nutrition's destination, and
 * every later tap on the Nutrition tab restored it with Learn on top. From Learn the tab looked
 * dead; from anywhere else it jumped to Learn; and it stuck for the life of the process (SFM-27).
 */
object TabNavigation {
    /** Key in a tab entry's SavedStateHandle: bumped when its tab is tapped while already selected. */
    const val RESELECT_KEY = "tab_reselect"

    /**
     * The concrete route a tab tap navigates to. A tab's [Screen.route] may be a pattern carrying
     * optional arguments (`nutrition?plan={plan}`); the tap wants the bare path.
     */
    fun routeFor(tab: Screen): String = tab.route.substringBefore("?")

    /** True when [destinationRoute] (a `NavDestination.route` pattern) is [tab]'s own root. */
    fun isTabRoot(destinationRoute: String?, tab: Screen): Boolean = destinationRoute == tab.route
}

/** Select [tab] the way the bottom bar does. Use this for any link that targets another tab. */
fun NavController.navigateToTab(tab: Screen) {
    navigate(TabNavigation.routeFor(tab)) {
        popUpTo(Screen.Home.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
