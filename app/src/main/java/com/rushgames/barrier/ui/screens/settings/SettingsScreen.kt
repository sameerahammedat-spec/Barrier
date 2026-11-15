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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rushgames.barrier.ui.components.BarrierSectionHeader
import com.rushgames.barrier.ui.theme.BarrierGreen
import com.rushgames.barrier.ui.theme.BarrierPurple
import com.rushgames.barrier.ui.theme.BarrierRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigatePermissions: () -> Unit,
    onNavigateProtection: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }

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
                title = "Strict Mode",
                description = "Maximum enforcement. No exceptions during sessions.",
                checked = uiState.strictModeGlobal,
                onToggle = viewModel::toggleStrictMode
            )
            SettingsDivider()
            SettingsToggleRow(
                icon = Icons.Outlined.Block,
                iconTint = BarrierRed,
                title = "Adult Content Protection",
                description = "Block adult websites and explicit content globally.",
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
                icon = Icons.Outlined.VpnKey,
                iconTint = BarrierPurple,
                title = "VPN Permission",
                description = "Required for website filtering",
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
            SettingsNavigationRow(
                icon = Icons.Outlined.Pin,
                iconTint = BarrierRed,
                title = "Recovery PIN",
                description = if (uiState.recoveryPin.isEmpty()) "Not set" else "••••",
                onClick = { showPinDialog = true }
            )
            SettingsDivider()
            SettingsNavigationRow(
                icon = Icons.Outlined.AdminPanelSettings,
                iconTint = BarrierRed,
                title = "Device Administrator",
                description = "Advanced management mode",
                onClick = { }
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

    // PIN dialog
    if (showPinDialog) {
        var pin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Set Recovery PIN") },
            text = {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6) pin = it.filter { c -> c.isDigit() } },
                    label = { Text("4–6 digit PIN") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.setRecoveryPin(pin); showPinDialog = false },
                    enabled = pin.length >= 4,
                    colors = ButtonDefaults.buttonColors(containerColor = BarrierGreen)
                ) { Text("Save PIN") }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) { Text("Cancel") }
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
