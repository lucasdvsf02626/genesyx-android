package com.genesyx.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.genesyx.app.core.AppLinks
import com.genesyx.app.domain.model.FocusMode
import com.genesyx.app.domain.model.PartnerInvite
import com.genesyx.app.domain.model.ThemeMode
import com.genesyx.app.ui.components.Eyebrow
import com.genesyx.app.ui.components.ScreenHeader
import com.genesyx.app.ui.components.isValidEmail
import com.genesyx.app.ui.navigation.Screen
import com.genesyx.app.ui.theme.BabyLavender
import com.genesyx.app.ui.theme.ElectricLavender
import com.genesyx.app.ui.theme.ElectricPink

private val detailCopy = mapOf(
    "Personal Details" to "Manage your display name, email sign-in, and account details from this screen.",
    "Health Profile" to "Your cycle settings, daily logs, and partner connection shape your personalised guidance.",
    "Tracking Preferences" to "Keep notifications on and update your cycle settings any time your rhythm changes.",
    "Privacy & Data" to "Your saved data is private to your account. You can log out or delete your account from Profile.",
    "Help & Support" to "For best results, complete cycle setup and log today.",
)

@Composable
fun ProfileScreen(navController: NavController, viewModel: ProfileViewModel = hiltViewModel()) {
    val colors = MaterialTheme.colorScheme
    val signedIn by viewModel.isSignedIn.collectAsState()
    val displayName by viewModel.displayName.collectAsState()
    val email by viewModel.email.collectAsState()
    val dark by viewModel.themeMode.collectAsState()
    val push by viewModel.pushEnabled.collectAsState()
    val focus by viewModel.focusMode.collectAsState()
    val partner by viewModel.partner.collectAsState()
    val invites by viewModel.invites.collectAsState()
    val deleting by viewModel.deleting.collectAsState()
    val deleteError by viewModel.deleteError.collectAsState()
    val accountDeleted by viewModel.deleted.collectAsState()
    val context = LocalContext.current

    // Account deleted → clear the back stack and return to the start (splash/auth).
    LaunchedEffect(accountDeleted) {
        if (accountDeleted) {
            navController.navigate(Screen.Splash.route) { popUpTo(0) { inclusive = true } }
        }
    }

    var nameOpen by remember { mutableStateOf(false) }
    var pwOpen by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<String?>(null) }
    var delOpen by remember { mutableStateOf(false) }

    val name = displayName ?: "Guest"
    val goSignIn = { navController.navigate(Screen.Auth.route) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader(title = "Profile")

        Column(Modifier.padding(horizontal = 20.dp)) {
            // User card
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(colors.surface).padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape).background(Brush.linearGradient(listOf(BabyLavender, ElectricPink))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(name.firstOrNull()?.uppercase() ?: "G", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                }
                Spacer(Modifier.size(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
                    Text(email ?: "Sign in to sync your data", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
                if (signedIn) {
                    Box(Modifier.clip(CircleShape).background(ElectricLavender.copy(alpha = 0.10f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text("PREMIUM", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = ElectricLavender)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            // Current focus
            Eyebrow("Current focus", color = colors.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surfaceVariant).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FocusSeg("Fertility Prep", focus == FocusMode.PREP, Modifier.weight(1f)) { viewModel.setFocus(FocusMode.PREP) }
                FocusSeg("Pregnancy", focus == FocusMode.PREGNANCY, Modifier.weight(1f)) {
                    viewModel.setFocus(FocusMode.PREGNANCY)
                    navController.navigate(Screen.Pregnancy.route)
                }
            }

            Spacer(Modifier.height(16.dp))
            PartnerSection(
                signedIn = signedIn,
                partnerName = partner?.name,
                pending = invites.filter { it.status == com.genesyx.app.domain.model.InviteStatus.PENDING },
                onSignIn = { goSignIn() },
                onSend = { viewModel.sendInvite(it) },
                onRevoke = { viewModel.revokeInvite(it) },
                onUnlink = { viewModel.unlinkPartner() },
            )

            Spacer(Modifier.height(16.dp))
            GroupLabel("Account")
            CardGroup {
                RowItem("Edit name", onClick = { if (signedIn) nameOpen = true else goSignIn() })
                Divider()
                RowItem("Change password", onClick = { if (signedIn) pwOpen = true else goSignIn() })
            }

            // ── Clients (admin/dev tool) — gated off for 1.0 (FeatureFlags.ADMIN_CLIENTS)
            if (com.genesyx.app.core.FeatureFlags.ADMIN_CLIENTS) {
                Spacer(Modifier.height(16.dp))
                GroupLabel("Clients")
                CardGroup {
                    RowItem("Manage clients", onClick = { navController.navigate(Screen.Clients.route) })
                }
            }

            Spacer(Modifier.height(16.dp))
            GroupLabel("Tracking")
            CardGroup {
                listOf("Personal Details", "Health Profile", "Tracking Preferences").forEachIndexed { i, label ->
                    RowItem(label, onClick = { detail = label })
                    if (i < 2) Divider()
                }
            }

            Spacer(Modifier.height(16.dp))
            GroupLabel("Preferences")
            CardGroup {
                SwitchRow("Push Notifications", push) { viewModel.setPush(it) }
            }

            Spacer(Modifier.height(12.dp))
            Eyebrow("Theme", color = colors.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surfaceVariant).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FocusSeg("System", dark == ThemeMode.SYSTEM, Modifier.weight(1f)) { viewModel.setTheme(ThemeMode.SYSTEM) }
                FocusSeg("Light", dark == ThemeMode.LIGHT, Modifier.weight(1f)) { viewModel.setTheme(ThemeMode.LIGHT) }
                FocusSeg("Dark", dark == ThemeMode.DARK, Modifier.weight(1f)) { viewModel.setTheme(ThemeMode.DARK) }
            }

            Spacer(Modifier.height(16.dp))
            GroupLabel("About")
            CardGroup {
                RowItem("Privacy & Data", onClick = {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppLinks.PRIVACY_POLICY_URL))) }
                })
                Divider()
                RowItem("Help & Support", onClick = { detail = "Help & Support" })
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surface)
                    .clickable { if (signedIn) viewModel.signOut() else goSignIn() }.padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, null, tint = colors.error, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(8.dp))
                Text(if (signedIn) "Log out" else "Sign in", fontWeight = FontWeight.SemiBold, color = colors.error)
            }

            if (signedIn) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .border(1.dp, colors.error.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .clickable { delOpen = true }.padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.DeleteOutline, null, tint = colors.error, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Delete account", fontWeight = FontWeight.SemiBold, color = colors.error)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (nameOpen) {
        EditNameDialog(initial = name, onDismiss = { nameOpen = false }, onSave = { viewModel.updateName(it); nameOpen = false })
    }
    if (pwOpen) {
        ChangePasswordDialog(onDismiss = { pwOpen = false })
    }
    detail?.let { d ->
        AlertDialog(
            onDismissRequest = { detail = null },
            shape = RoundedCornerShape(20.dp),
            containerColor = colors.surface,
            title = { Text(d, style = MaterialTheme.typography.titleLarge, color = colors.onSurface) },
            text = { Text(detailCopy[d] ?: "This section is ready for your saved app settings.", style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant) },
            confirmButton = { TextButton(onClick = { detail = null }) { Text("Done", color = ElectricLavender) } },
        )
    }
    if (delOpen) {
        // On successful deletion the session is cleared → signedIn flips false → close the dialog.
        LaunchedEffect(signedIn) { if (!signedIn) delOpen = false }
        AlertDialog(
            onDismissRequest = { if (!deleting) { delOpen = false; viewModel.clearDeleteError() } },
            shape = RoundedCornerShape(20.dp),
            containerColor = colors.surface,
            title = { Text("Delete your account?", style = MaterialTheme.typography.titleLarge, color = colors.onSurface) },
            text = {
                Column {
                    Text(
                        "This will permanently delete your account and all your data. This cannot be undone.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurfaceVariant,
                    )
                    if (deleteError != null) {
                        Spacer(Modifier.height(12.dp))
                        Text(deleteError!!, style = MaterialTheme.typography.bodyMedium, color = colors.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteAccount() }, enabled = !deleting) {
                    Text(
                        if (deleting) "Deleting…" else "Delete",
                        color = colors.error,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { delOpen = false; viewModel.clearDeleteError() }, enabled = !deleting) {
                    Text("Cancel", color = colors.onSurfaceVariant)
                }
            },
        )
    }
}

@Composable
private fun FocusSeg(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(12.dp)).background(if (selected) colors.surface else Color.Transparent).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (selected) colors.onSurface else colors.onSurfaceVariant) }
}

@Composable
private fun GroupLabel(text: String) =
    Eyebrow(text, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))

@Composable
private fun CardGroup(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface), content = content)
}

@Composable
private fun RowItem(label: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 14.5.sp, color = colors.onSurface)
        Icon(Icons.Filled.ChevronRight, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 14.5.sp, color = colors.onSurface)
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedTrackColor = ElectricLavender))
    }
}

@Composable
private fun Divider() = Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)))

@Composable
private fun PartnerSection(
    signedIn: Boolean,
    partnerName: String?,
    pending: List<PartnerInvite>,
    onSignIn: () -> Unit,
    onSend: (String) -> Unit,
    onRevoke: (String) -> Unit,
    onUnlink: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    GroupLabel("Partner")
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surface).padding(20.dp)) {
        when {
            !signedIn -> {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Favorite, null, tint = ElectricLavender, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Add your partner", style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                    Text("Sign in to invite a partner to join your journey.", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = onSignIn) { Text("Sign in", color = ElectricLavender, fontWeight = FontWeight.SemiBold) }
                }
            }
            partnerName != null -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(Brush.linearGradient(listOf(BabyLavender, ElectricPink))), contentAlignment = Alignment.Center) {
                        Text(partnerName.firstOrNull()?.uppercase() ?: "P", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(partnerName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                        Text("Linked partner", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                    TextButton(onClick = onUnlink) { Text("Remove", color = colors.error) }
                }
            }
            else -> {
                var email by remember { mutableStateOf("") }
                var err by remember { mutableStateOf<String?>(null) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Favorite, null, tint = ElectricLavender, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Add your partner", style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                }
                Spacer(Modifier.height(4.dp))
                Text("Send an invite — when they accept, you'll be linked and can share your journey.", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; err = null },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    placeholder = { Text("partner@example.com") },
                    singleLine = true,
                    isError = err != null,
                    shape = RoundedCornerShape(12.dp),
                )
                if (err != null) Text(err!!, style = MaterialTheme.typography.bodyMedium, color = colors.error)
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { if (isValidEmail(email)) { onSend(email.trim()); email = "" } else err = "Enter a valid email" },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Mail, null, tint = ElectricLavender, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Send invite", color = ElectricLavender, fontWeight = FontWeight.SemiBold)
                }
                if (pending.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.outline.copy(alpha = 0.5f)))
                    Spacer(Modifier.height(8.dp))
                    Eyebrow("Pending invites", color = colors.onSurfaceVariant)
                    pending.forEach { inv ->
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(inv.email, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface, modifier = Modifier.weight(1f))
                            Icon(Icons.Filled.ContentCopy, "Copy", tint = colors.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(12.dp))
                            Icon(Icons.Filled.Close, "Revoke", tint = colors.error, modifier = Modifier.size(16.dp).clickable { onRevoke(inv.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditNameDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = colors.surface,
        title = { Text("Edit name", style = MaterialTheme.typography.titleLarge, color = colors.onSurface) },
        text = {
            Column {
                Text("This is how you'll appear across the app.", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(name, { if (it.length <= 80) name = it }, label = { Text("Display name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onSave(name.trim()) }) { Text("Save", color = ElectricLavender, fontWeight = FontWeight.SemiBold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = colors.onSurfaceVariant) } },
    )
}

@Composable
private fun ChangePasswordDialog(onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var current by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = colors.surface,
        title = { Text("Change password", style = MaterialTheme.typography.titleLarge, color = colors.onSurface) },
        text = {
            Column {
                Text("Choose a new password of at least 8 characters.", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(current, { current = it }, label = { Text("Current password") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(next, { next = it }, label = { Text("New password") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(confirm, { confirm = it }, label = { Text("Confirm new password") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (err != null) { Spacer(Modifier.height(6.dp)); Text(err!!, style = MaterialTheme.typography.bodyMedium, color = colors.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    next.length < 8 -> err = "New password must be at least 8 characters"
                    next != confirm -> err = "Passwords don't match"
                    else -> onDismiss()
                }
            }) { Text("Update", color = ElectricLavender, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = colors.onSurfaceVariant) } },
    )
}
