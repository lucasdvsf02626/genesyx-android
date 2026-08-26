package com.genesyx.app.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The bottom bar's routing contract. SFM-27 was not a missing route — it was a tab root pushed
 * on top of another tab and then restored — but the route half is what everyone checks first, so
 * it is pinned here: every tab navigates to a concrete, argument-free path, and the tab is
 * recognised as selected from the pattern the NavHost registers.
 */
class TabNavigationTest {

    @Test
    fun `every bottom tab navigates to a concrete route with no placeholders`() {
        Screen.bottomTabs.forEach { tab ->
            val route = TabNavigation.routeFor(tab)
            assertFalse("tab ${tab.route} navigates to a pattern: $route", route.contains("{"))
            assertFalse("tab ${tab.route} navigates with a query: $route", route.contains("?"))
            assertTrue(route.isNotBlank())
        }
    }

    @Test
    fun `the Nutrition tab navigates to the bare path its registered pattern accepts`() {
        // The NavHost registers `nutrition?plan={plan}` with `plan` nullable + defaulted, so
        // `nutrition` matches it. The tap must send that, never the pattern string itself.
        assertEquals("nutrition", TabNavigation.routeFor(Screen.Nutrition))
        assertEquals(Screen.Nutrition.create(), TabNavigation.routeFor(Screen.Nutrition))
        assertEquals("nutrition?plan={plan}", Screen.Nutrition.route)
    }

    @Test
    fun `a tab is selected from the destination pattern, not the concrete route`() {
        // `NavDestination.route` is the registered pattern; that is what the bar compares against.
        assertTrue(TabNavigation.isTabRoot("nutrition?plan={plan}", Screen.Nutrition))
        assertFalse(TabNavigation.isTabRoot("nutrition", Screen.Nutrition))
        assertTrue(TabNavigation.isTabRoot("tracker/ph", Screen.PhDetail))
        assertFalse(TabNavigation.isTabRoot("tracker/nutrition", Screen.Nutrition))
        assertFalse(TabNavigation.isTabRoot(null, Screen.Home))
    }

    @Test
    fun `the seven tabs are the seven the product asked for, in order`() {
        assertEquals(
            listOf("home", "track", "tracker/ph", "nutrition", "insights", "learn", "profile"),
            Screen.bottomTabs.map { TabNavigation.routeFor(it) },
        )
    }

    @Test
    fun `tab roots keep the bottom bar, so only a tab root can ever be restored on top of a tab`() {
        // The SFM-27 mechanism needs a bar-keeping screen pushed over a tab. Every non-tab
        // destination hides the bar, so cross-tab links are the only way to get there — and they
        // now go through navigateToTab. If a new bar-keeping route appears, revisit this.
        Screen.bottomTabs.forEach { assertFalse(it.route in Screen.noBottomNavRoutes) }
        assertTrue(Screen.NutritionDetail.route in Screen.noBottomNavRoutes)
        assertTrue(Screen.ArticleDetail.route in Screen.noBottomNavRoutes)
    }

    @Test
    fun `tabForRoute resolves every tab's own route, including the pH tab`() {
        Screen.bottomTabs.forEach { tab ->
            assertEquals(tab, TabNavigation.tabForRoute(TabNavigation.routeFor(tab)))
        }
        // The route at the heart of the Track → pH → Track regression: `tracker/ph` is a tab.
        assertEquals(Screen.PhDetail, TabNavigation.tabForRoute(Screen.PhDetail.route))
        assertEquals(Screen.PhDetail, TabNavigation.tabForRoute("tracker/ph"))
    }

    @Test
    fun `tabForRoute resolves tab patterns that carry optional arguments`() {
        // Nutrition's registered pattern is `nutrition?plan={plan}`; matching is on the path.
        assertEquals(Screen.Nutrition, TabNavigation.tabForRoute(Screen.Nutrition.route))
        assertEquals(Screen.Nutrition, TabNavigation.tabForRoute("nutrition?plan=1"))
    }

    @Test
    fun `tabForRoute rejects pushed destinations, so Track's detail rows keep their push behaviour`() {
        // Every other "Your trackers" row must keep opening as an immersive detail on top of
        // Track — only the pH row switches tabs.
        listOf(
            Screen.CycleDetail,
            Screen.NutritionDetail,
            Screen.SymptomsDetail,
            Screen.SleepDetail,
            Screen.HydrationDetail,
            Screen.Log,
            Screen.LogHistory,
            Screen.ArticleDetail,
        ).forEach { screen -> assertNull(screen.route, TabNavigation.tabForRoute(screen.route)) }
    }
}
