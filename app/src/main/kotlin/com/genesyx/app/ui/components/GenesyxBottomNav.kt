package com.genesyx.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.genesyx.app.ui.navigation.Screen
import com.genesyx.app.ui.theme.ElectricLavender

private data class BottomNavItem(val screen: Screen, val label: String, val icon: ImageVector)

private val items = listOf(
    BottomNavItem(Screen.Home, "Home", Icons.Outlined.Home),
    BottomNavItem(Screen.Track, "Track", Icons.Outlined.CalendarMonth),
    BottomNavItem(Screen.Nutrition, "Nutrition", Icons.Outlined.Eco),
    BottomNavItem(Screen.Insights, "Insights", Icons.Outlined.BarChart),
    BottomNavItem(Screen.Learn, "Learn", Icons.AutoMirrored.Outlined.MenuBook),
    BottomNavItem(Screen.Profile, "Profile", Icons.Outlined.Person),
)

@Composable
fun GenesyxBottomNav(navController: NavController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.screen.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(item.screen.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = {
                    // Six tabs leave ~60dp per item at 360dp. labelSmall wraps "Nutrition" onto a
                    // second line, so shrink and pin to one line.
                    Text(
                        item.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        lineHeight = 12.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Visible,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ElectricLavender,
                    selectedTextColor = ElectricLavender,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.surface,
                ),
            )
        }
    }
}
