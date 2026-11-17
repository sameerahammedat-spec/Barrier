package com.rushgames.barrier.ui.screens.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rushgames.barrier.ui.components.BarrierSectionHeader
import com.rushgames.barrier.ui.theme.BarrierGreen
import com.rushgames.barrier.ui.theme.BarrierPurple
import com.rushgames.barrier.ui.theme.BarrierRed
import com.rushgames.barrier.util.DeviceAdminManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigatePermissions: () -> Unit,
    onNavigateProtection: () -> Unit,
    onNavigateAuth: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showThemeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text("Customize Barrier", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))

        // === User Account & Cloud Sync Banner ===
        BarrierSectionHeader("Account & Cloud Backup")
        Spacer(Modifier.height(8.dp))
        SettingsCard {
            val user = uiState.currentUser
            if (user != null) {
                SettingsNavigationRow(
                    icon = Icons.Outlined.AccountCircle,
                    iconTint = BarrierGreen,
                    title = if (user.isGuest) "Guest Account (Offline)" else "Unverified local profile",
                    description = if (user.isGuest) "Online sign-in is not configured" else "This legacy profile is not authenticated or synced.",
                    onClick = onNavigateAuth
                )
                SettingsDivider()
                SettingsNavigationRow(
                    icon = Icons.Outlined.CloudSync,
                    iconTint = BarrierPurple,
                    title = "Cloud Backup Unavailable",
                    description = uiState.syncMessage ?: "Firebase authentication and backup are not configured.",
                    onClick = { viewModel.syncCloudData() }
                )
                SettingsDivider()
                SettingsNavigationRow(
                    icon = Icons.Outlined.Logout,
                    iconTint = BarrierRed,
                    title = "Sign Out",
                    description = "Clear this locally stored profile",
                    onClick = { viewModel.signOut() }
                )
            } else {
                SettingsNavigationRow(
                    icon = Icons.Outlined.Login,
                    iconTint = BarrierGreen,
                    title = "Sign In / Sign Up",
                    description = "Online sign-in is unavailable until Firebase is configured.",
                    onClick = onNavigateAuth
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // === Protection Center Banner Row ===
        SettingsCard {
            SettingsNavigationRow(
                icon = Icons.Outlined.Shield,
                iconTint = BarrierGreen,
                title = "Protection Center",
                description = "Inappropriate content shield & adult domain filters",
                onClick = onNavigateProtection
            )
        }

        Spacer(Modifier.height(24.dp))

        // === Blocking Settings ===
        BarrierSectionHeader("Blocking")
        Spacer(Modifier.height(8.dp))
        SettingsCard {
            SettingsToggleRow(
                icon = Icons.Outlined.Security,
                iconTint = BarrierGreen,
                title = "Strict Mode for New Sessions",
                description = "Start new sessions with in-app cancellation locked. Android can still stop or uninstall Barrier.",
                checked = uiState.strictModeGlobal,
                onToggle = viewModel::toggleStrictMode
            )
            SettingsDivider()
            SettingsToggleRow(
                icon = Icons.Outlined.Block,
                iconTint = BarrierRed,
                title = "Adult Content Protection",
                description = "Check URLs and visible browser content when Accessibility is enabled; coverage is limited.",
                checked = uiState.adultProtection,
                onToggle = viewModel::toggleAdultProtection
            )
        }

        Spacer(Modifier.height(24.dp))

        // === Permissions ===
        BarrierSectionHeader("Permissions")
        Spacer(Modifier.height(8.dp))
        SettingsCard {
            SettingsNavigationRow(
                icon = Icons.Outlined.AdminPanelSettings,
                iconTint = BarrierPurple,
                title = "Usage Access",
                description = "Required to monitor app usage",
                onClick = onNavigatePermissions
            )
            SettingsDivider()
            SettingsNavigationRow(
                icon = Icons.Outlined.Accessibility,
                iconTint = BarrierPurple,
                title = "Accessibility Service",
                description = "Required for stronger app blocking",
                onClick = onNavigatePermissions
            )
            SettingsDivider()
            SettingsNavigationRow(
                icon = Icons.Outlined.NotificationsActive,
                iconTint = BarrierPurple,
                title = "Notification Access",
                description = "Required for session notifications",
                onClick = onNavigatePermissions
            )
        }

        Spacer(Modifier.height(24.dp))

        // === Appearance ===
        BarrierSectionHeader("Appearance")
        Spacer(Modifier.height(8.dp))
        SettingsCard {
            SettingsNavigationRow(
                icon = Icons.Outlined.Palette,
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                title = "Theme",
                description = uiState.theme.replaceFirstChar { it.uppercase() },
                onClick = { showThemeDialog = true }
            )
        }

        Spacer(Modifier.height(24.dp))

        // === Security & Recovery ===
        BarrierSectionHeader("Security & Recovery")
        Spacer(Modifier.height(8.dp))
        SettingsCard {
            SettingsInfoRow(title = "Recovery PIN", value = "Unavailable")
            Text(
                text = "Barrier has no recovery flow and does not use a PIN for account or session recovery.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            SettingsDivider()
            SettingsNavigationRow(
                icon = Icons.Outlined.AdminPanelSettings,
                iconTint = BarrierRed,
                title = "Device Administrator",
                description = if (DeviceAdminManager.isDeviceAdminActive(context)) "Enabled; does not prevent uninstall on standard devices." else "Optional; does not provide anti-uninstall protection.",
                onClick = { DeviceAdminManager.openDeviceAdminSettings(context) }
            )
        }

        Spacer(Modifier.height(24.dp))

        // === About ===
        BarrierSectionHeader("About")
        Spacer(Modifier.height(8.dp))
        SettingsCard {
            SettingsInfoRow(title = "Version", value = "1.0.0 (Phase 1)")
            SettingsDivider()
            SettingsInfoRow(title = "App", value = "Barrier — Focus & Protection")
            SettingsDivider()
            SettingsInfoRow(title = "Minimum Android", value = "Android 8.0 (API 26)")
        }

        Spacer(Modifier.height(100.dp))
    }

    // Theme dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Theme") },
            text = {
                Column {
                    listOf("system", "light", "dark").forEach { theme ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                viewModel.setTheme(theme)
                                showThemeDialog = false
                            }.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(theme.replaceFirstChar { it.uppercase() })
                            if (uiState.theme == theme) Icon(Icons.Outlined.Check, contentDescription = null, tint = BarrierGreen)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Close") }
            }
        )
    }

}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp), content = content)
    }
}

@Composable
private fun SettingsDivider() {
    Divider(
        modifier = Modifier.padding(start = 46.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    )
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    description: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = iconTint)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(checkedTrackColor = BarrierGreen)
        )
    }
}

@Composable
private fun SettingsNavigationRow(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = iconTint)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
    }
}

@Composable
private fun SettingsInfoRow(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
