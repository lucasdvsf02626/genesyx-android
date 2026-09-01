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

    /**
     * The tab that owns [route], or null when [route] is an ordinary pushed destination. Matches
     * on the path, so a tab pattern carrying optional arguments (`nutrition?plan={plan}`) still
     * resolves. `tracker/ph` is a tab root: any in-screen link that plain-pushes it stacks the pH
     * tab on top of the current tab, and the next tab switch saves/restores that whole chain —
     * the Track → pH → Track regression, same mechanism as SFM-27.
     */
    fun tabForRoute(route: String): Screen? =
        Screen.bottomTabs.firstOrNull { routeFor(it) == route.substringBefore("?") }
}

/** Select [tab] the way the bottom bar does. Use this for any link that targets another tab. */
fun NavController.navigateToTab(tab: Screen) {
    // Fired from a screen pushed onto a tab root — an article, say — the first move is back down to
    // that root: the chain we are standing in must never be saved. Saved under the tab this link
    // points at, `restoreState` brings it straight back and nothing on screen changes: that is the
    // "See your insights" dead button, tapped in `reading-your-trends`, which is the Insights tab's
    // own "how this works" article (the pH tab's, `guide-understanding-vaginal-ph`, was dead the
    // same way, as was any article opened from the tab its CTA points back at). Saved under
    // any other tab, it is restored later with a bar-less article on top — how a tab comes to look
    // dead (SFM-27).
    val beneath = tabRootBeneath()
    if (beneath != null && popBackStack(beneath.route, inclusive = false) && beneath == tab) {
        // The link pointed at the tab we were sitting on: the pop was the whole navigation.
        return
    }
    navigate(TabNavigation.routeFor(tab)) {
        popUpTo(Screen.Home.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * The tab whose root this screen was pushed onto, or null when we are on a tab root ourselves (the
 * bottom bar's case — there the switch must save the tab's state, not pop it away) or on something
 * deeper than one push.
 */
private fun NavController.tabRootBeneath(): Screen? {
    val here = currentDestination?.route ?: return null
    if (TabNavigation.tabForRoute(here) != null) return null
    val beneath = previousBackStackEntry?.destination?.route ?: return null
    return TabNavigation.tabForRoute(beneath)
}
