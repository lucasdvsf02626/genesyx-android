package com.genesyx.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.genesyx.app.ui.components.PlaceholderScreen

// Main app + secondary screen stubs — built out one at a time per docs/SCREEN_LAYOUTS.md.

@Composable
fun ProfileScreen(navController: NavController) =
    PlaceholderScreen("Profile", "Focus toggle + partner + account + theme")

@Composable
fun LogScreen(onClose: () -> Unit) =
    PlaceholderScreen("Log Today", "Mood / energy / symptoms / sleep / water / supplements")

@Composable
fun AuthScreen(onSignedIn: () -> Unit, onBack: () -> Unit) =
    PlaceholderScreen("Welcome back", "Email + Google sign-in")

@Composable
fun InviteScreen(code: String, onAccepted: () -> Unit, onBack: () -> Unit) =
    PlaceholderScreen("Partner invite", "Accept invite • code: $code")
