package com.genesyx.app

import android.content.Intent
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.genesyx.app.data.AppUpdateGate
import com.genesyx.app.data.NotificationSettingsRepository
import com.genesyx.app.data.PreferencesRepository
import com.genesyx.app.data.SyncStatusRepository
import com.genesyx.app.domain.model.ThemeMode
import com.genesyx.app.notifications.AppForegroundTracker
import com.genesyx.app.notifications.ReminderScheduler
import com.genesyx.app.ui.AppViewModel
import com.genesyx.app.ui.components.GenesyxBottomNav
import com.genesyx.app.ui.navigation.GenesyxNavGraph
import com.genesyx.app.ui.navigation.Screen
import com.genesyx.app.ui.screens.UpdateRequiredScreen
import com.genesyx.app.ui.theme.GenesyxTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()

    @Inject lateinit var settingsRepository: NotificationSettingsRepository
    @Inject lateinit var reminderScheduler: ReminderScheduler
    @Inject lateinit var syncStatusRepository: SyncStatusRepository
    @Inject lateinit var preferencesRepository: PreferencesRepository
    @Inject lateinit var appUpdateGate: AppUpdateGate

    // Hoisted out of setContent so a notification tap arriving while the app is already foregrounded
    // (onNewIntent) can route through the live NavController instead of spawning a second graph.
    private var navController: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Hold the system splash until the persisted session resolves, so a signed-in user routes
        // straight to Home instead of being forced back through onboarding on cold start.
        splash.setKeepOnScreenCondition { appViewModel.startRoute.value == null }
        enableEdgeToEdge()
        // Minimum-version gate: one anonymous check per process. Fail-open — only a confirmed,
        // well-formed minimum above this build replaces the UI with the update screen.
        appUpdateGate.check(BuildConfig.VERSION_CODE)
        setContent {
            val themeMode by appViewModel.themeMode.collectAsState()
            val startRoute by appViewModel.startRoute.collectAsState()
            val updateRequired by appUpdateGate.updateRequired.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            GenesyxTheme(darkTheme = darkTheme) {
                val route = startRoute
                if (updateRequired) {
                    // Rendered INSTEAD of the nav graph: no route behind it, so nothing to dismiss.
                    UpdateRequiredScreen()
                } else if (route != null) {
                    val nav = rememberNavController()
                    navController = nav
                    val backStackEntry by nav.currentBackStackEntryAsState()
                    val currentRoute = backStackEntry?.destination?.route
                    val showBottomNav = currentRoute != null &&
                        currentRoute !in Screen.noBottomNavRoutes

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            if (showBottomNav) GenesyxBottomNav(nav)
                        },
                    ) { innerPadding ->
                        GenesyxNavGraph(
                            navController = nav,
                            startDestination = route,
                            modifier = Modifier.padding(innerPadding),
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Route a reminder tap that arrived while we were already open.
        navController?.handleDeepLink(intent)
    }

    override fun onStart() {
        super.onStart()
        AppForegroundTracker.onEnterForeground()
        // "Opened" powers re-engagement pacing; re-arming on open is the cheap self-heal for any
        // chain an OEM task-killer dropped while we were away.
        lifecycleScope.launch {
            settingsRepository.markOpened(LocalDate.now().toEpochDay())
            reminderScheduler.rescheduleAll(settingsRepository.current())
        }
        // Same self-heal for the sync queues: a drain an OEM task-killer dropped stays PENDING
        // forever otherwise. No-op when nothing is queued.
        syncStatusRepository.scheduleDrain()
        // Anchor for the Learn weekly drip — set once, on the first open ever, then a no-op.
        preferencesRepository.ensureFirstOpenRecorded(LocalDate.now().toEpochDay())
    }

    override fun onStop() {
        super.onStop()
        AppForegroundTracker.onLeaveForeground()
    }
}
