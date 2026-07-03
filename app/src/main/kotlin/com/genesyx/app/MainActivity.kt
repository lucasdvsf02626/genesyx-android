package com.genesyx.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.genesyx.app.domain.model.ThemeMode
import com.genesyx.app.ui.AppViewModel
import com.genesyx.app.ui.components.GenesyxBottomNav
import com.genesyx.app.ui.navigation.GenesyxNavGraph
import com.genesyx.app.ui.navigation.Screen
import com.genesyx.app.ui.theme.GenesyxTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Hold the system splash until the persisted session resolves, so a signed-in user routes
        // straight to Home instead of being forced back through onboarding on cold start.
        splash.setKeepOnScreenCondition { appViewModel.startRoute.value == null }
        enableEdgeToEdge()
        setContent {
            val themeMode by appViewModel.themeMode.collectAsState()
            val startRoute by appViewModel.startRoute.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            GenesyxTheme(darkTheme = darkTheme) {
                val route = startRoute
                if (route != null) {
                    val navController = rememberNavController()
                    val backStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = backStackEntry?.destination?.route
                    val showBottomNav = currentRoute != null &&
                        currentRoute !in Screen.noBottomNavRoutes

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            if (showBottomNav) GenesyxBottomNav(navController)
                        },
                    ) { innerPadding ->
                        GenesyxNavGraph(
                            navController = navController,
                            startDestination = route,
                            modifier = Modifier.padding(innerPadding),
                        )
                    }
                }
            }
        }
    }
}
