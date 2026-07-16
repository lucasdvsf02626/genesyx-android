package com.genesyx.app.ui.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import android.content.res.Configuration
import com.genesyx.app.R
import com.genesyx.app.domain.model.CycleSettings
import com.genesyx.app.ui.components.CycleSettingsDialog
import com.genesyx.app.ui.components.Eyebrow
import com.genesyx.app.ui.components.GxPrimaryButton
import com.genesyx.app.ui.components.tintOnWhite
import com.genesyx.app.ui.navigation.Screen
import com.genesyx.app.ui.theme.ElectricBlue
import com.genesyx.app.ui.theme.ElectricLavender
import com.genesyx.app.ui.theme.GenesyxTheme
import com.genesyx.app.ui.theme.PowderBlue
import java.time.LocalDate

private data class Bubble(val x: Int, val y: Int, val size: Int, val a: Color, val b: Color, val durationMs: Int, val drift: Float)

private val bubbles = listOf(
    Bubble(300, 96, 112, Color(0xFFC4B5FD), Color(0xFFA78BFA), 5000, -14f),
    Bubble(-32, 224, 80, Color(0xFFBAE6FD), Color(0xFF7DD3FC), 7000, -10f),
    Bubble(280, 360, 96, Color(0xFFE9D5FF), Color(0xFFC084FC), 6000, -18f),
    Bubble(16, 420, 64, Color(0xFFFBCFE8), Color(0xFFF472B6), 4500, -12f),
    Bubble(300, 540, 56, Color(0xFFBFDBFE), Color(0xFF60A5FA), 8000, -10f),
)

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    HomeContent(
        state = state,
        onNavigate = { navController.navigate(it) },
        onSaveCycle = { viewModel.saveCycleSettings(it) },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeContent(
    state: HomeUiState,
    onNavigate: (String) -> Unit,
    onSaveCycle: (CycleSettings) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var showCycleDialog by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        // Brand hero background. The artwork is light, so it only reads on a light surface —
        // in dark theme fall back to the animated bubbles so text stays AA-readable.
        if (colors.background.luminance() > 0.5f) {
            Image(
                painter = painterResource(R.drawable.home_hero_bg),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
            )
        } else {
            FloatingBubbles()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(20.dp))

            // ── Greeting header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(state.greeting, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    Text(state.userName, style = MaterialTheme.typography.headlineMedium, color = colors.onBackground)
                }
                Box {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(colors.surface)
                            .clickable { menuOpen = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            state.userName.firstOrNull()?.uppercase() ?: "G",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.onSurface,
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        if (!state.signedIn) {
                            DropdownMenuItem(
                                text = { Text("Sign in or create account") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Login, null) },
                                onClick = { menuOpen = false; onNavigate(Screen.Auth.route) },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Profile") },
                            leadingIcon = { Icon(Icons.Filled.Person, null) },
                            onClick = { menuOpen = false; onNavigate(Screen.Profile.route) },
                        )
                        DropdownMenuItem(
                            text = { Text("Cycle setup") },
                            leadingIcon = { Icon(Icons.Filled.Settings, null) },
                            onClick = { menuOpen = false; showCycleDialog = true },
                        )
                    }
                }
            }

            // ── Sign-in banner (signed-out)
            if (!state.signedIn) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ElectricLavender)
                        .clickable { onNavigate(Screen.Auth.route) }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Sign in to save your journey", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Cycle setup, daily logs, and profile sync.", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                    }
                    Icon(Icons.AutoMirrored.Filled.Login, null, tint = Color.White)
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Cycle hero card. Translucent surface acts as a subtle scrim so the brand hero
            //    reads through behind the copy while keeping the dynamic text AA-readable.
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.72f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                onClick = { showCycleDialog = true },
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text(state.cycleEyebrow, style = MaterialTheme.typography.labelSmall, color = ElectricLavender)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        state.cycleHeadline,
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(state.cycleSub, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    if (state.cycleTags.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            state.cycleTags.forEachIndexed { i, tag -> TagChip(tag, primary = i == 0) }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Today's focus card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Eyebrow("Today's focus", color = colors.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    if (state.todayFocusTitle != null) {
                        Text(state.todayFocusTitle!!, style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
                        Spacer(Modifier.height(4.dp))
                        Text(state.todayFocusBody.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    } else {
                        Text(
                            "Complete your cycle setup to see focus foods.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Stat grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = { Icon(Icons.Outlined.WaterDrop, null, tint = ElectricBlue) },
                    label = "Hydration",
                    value = state.hydrationLitres?.let { "%.1fL".format(it) } ?: "—",
                    suffix = "/ ${"%.1f".format(state.hydrationGoalLitres)}L",
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = { Icon(Icons.Outlined.Eco, null, tint = ElectricLavender) },
                    label = "Streak",
                    value = state.streakDays?.toString() ?: "—",
                    suffix = "days",
                )
            }

            // Intraday hydration pacing — how today is going, framed by the time of day.
            state.hydrationCoaching?.let { coaching ->
                Spacer(Modifier.height(10.dp))
                Text(
                    coaching,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            Spacer(Modifier.height(20.dp))

            GxPrimaryButton(text = "Log today", onClick = { onNavigate(Screen.Log.route) }, leadingIcon = Icons.Filled.Add)

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onNavigate(Screen.Pregnancy.route) }
                    .padding(horizontal = 4.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Preview pregnancy pathway", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(16.dp))
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showCycleDialog) {
        CycleSettingsDialog(
            current = state.settings,
            onDismiss = { showCycleDialog = false },
            onSave = { onSaveCycle(it); showCycleDialog = false },
        )
    }
}

@Composable
private fun FloatingBubbles() {
    val transition = rememberInfiniteTransition(label = "bubbles")
    Box(Modifier.fillMaxSize()) {
        bubbles.forEach { b ->
            val dy by transition.animateFloat(
                initialValue = 0f,
                targetValue = b.drift,
                animationSpec = infiniteRepeatable(tween(b.durationMs), RepeatMode.Reverse),
                label = "bubble",
            )
            Box(
                modifier = Modifier
                    .offset(x = b.x.dp, y = (b.y + dy).dp)
                    .size(b.size.dp)
                    .blur(18.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(b.a, b.b))),
            )
        }
    }
}

@Composable
private fun TagChip(text: String, primary: Boolean) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (primary) ElectricLavender.tintOnWhite(0.08f) else PowderBlue.tintOnWhite(0.22f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            fontSize = 11.5.sp,
            color = if (primary) ElectricLavender else colors.onSurface.copy(alpha = 0.75f),
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    suffix: String,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            icon()
            Spacer(Modifier.height(8.dp))
            Eyebrow(label, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
                Spacer(Modifier.size(4.dp))
                Text(suffix, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(bottom = 2.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Compose Previews — render the Home UI live in Android Studio, no emulator needed.
// HomeContent is stateless, so we feed it sample data and no-op callbacks.
// ─────────────────────────────────────────────────────────────────────────────

private val sampleHomeState = HomeUiState(
    userName = "Lucas",
    signedIn = true,
    greeting = "Good afternoon",
    settings = CycleSettings(lastPeriodDate = LocalDate.now().minusDays(8)),
    cycleSetUp = true,
    cycleEyebrow = "DAY 9 · FOLLICULAR",
    cycleHeadline = "Your body is preparing",
    cycleSub = "Energy is climbing toward your fertile window.",
    cycleTags = listOf("Follicular", "Rising energy", "Prep foods"),
    todayFocusTitle = "Leafy greens & folate",
    todayFocusBody = "Spinach, lentils and citrus support the follicular build-up.",
    hydrationLitres = 1.6f,
    streakDays = 4,
)

@Preview(name = "Home — light", showBackground = true, showSystemUi = true)
@Composable
private fun HomeContentLightPreview() {
    GenesyxTheme(darkTheme = false) {
        HomeContent(state = sampleHomeState, onNavigate = {}, onSaveCycle = {})
    }
}

@Preview(
    name = "Home — dark",
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun HomeContentDarkPreview() {
    GenesyxTheme(darkTheme = true) {
        HomeContent(state = sampleHomeState, onNavigate = {}, onSaveCycle = {})
    }
}

@Preview(name = "Home — not set up", showBackground = true, showSystemUi = true)
@Composable
private fun HomeContentEmptyPreview() {
    GenesyxTheme(darkTheme = false) {
        HomeContent(state = HomeUiState(), onNavigate = {}, onSaveCycle = {})
    }
}
